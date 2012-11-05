/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.discitems;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import jpsxdec.audio.SquareAdpcmDecoder;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorCrusader;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.IO;

/** Demultiplexes audio and video from Crusader: No Remorse movies.
 * Can be used for decoding or for discovery of when movies start and end.
 * The object's life ends at the end of a movie and should be discarded. */
public class CrusaderDemuxer implements ISectorFrameDemuxer, ISectorAudioDecoder {

    private static final Logger log = Logger.getLogger(CrusaderDemuxer.class.getName());

    private static final boolean DEBUG = false;

    private static final long AUDIO_ID = 0x08000200L;
    
    // TODO: verify format
    private static final int CRUSADER_SAMPLES_PER_SECOND = 22050;
    private static final int SAMPLES_PER_SECTOR = CRUSADER_SAMPLES_PER_SECOND / 150;
    {
        if (CRUSADER_SAMPLES_PER_SECOND % 150 != 0)
            throw new RuntimeException("Crusader sample rate doesn't cleanly divide by sector rate");
    }
    private static final int BYTES_PER_SAMPLE = 4;

    private static enum ReadState {
        MAGIC,
        LENGTH,
        PAYLOAD,
        HEADER1,
        HEADER2,
    }
    
    private static enum PayloadType {
        MDEC,
        AD20,
        AD21,
    }
    
    private ReadState _state = ReadState.MAGIC;

    // .. info about the movie ..

    private final boolean _blnIndexing;
    /** Will be dynamic if indexing. */
    private int _iWidth, _iHeight;
    /** Will be dynamic if indexing. */
    private int _iStartSector, _iEndSector;

    // .. payload parsing ..

    private PayloadType _ePayloadType;
    /** The starting offset in the sector of the first payload. */
    private int _iPayloadStartOffset;
    /** Size of the payload as read from the payload header. */
    private int _iPayloadSize;
    /** Remaining payload to read before it is finished. */
    private int _iRemainingPayload = -1;

    /** Saves payload header. */
    private final byte[] _abHeader = new byte[8];

    // .. stateful tracking ..

    /** Crusader identified sector number of the last seen Crusader sector.
     * Important for determining if any sectors were skipped. */
    private int _iPreviousCrusaderSectorNumber = -1;
    /** Tracks if a at least 1 payload was discovered. */
    private boolean _blnFoundAPayload = false;
    /** All presentation sectors are adjusted by this offset.
     * Initialized upon receiving the first payload.
     * This could be initialized differently depending on if someone is
     * listening for audio or video. Not needed for indexing. */
    private int _iInitialPresentationSector = -1;

    /** Saved payload sectors. */
    private final ArrayList<SectorCrusader> _sectors = new ArrayList<SectorCrusader>();
    
    private ICompletedFrameListener _frameListener;
    private ISectorTimedAudioWriter _audioListener;
    
    private final AudioFormat _audioFmt;
    
    public CrusaderDemuxer(int iWidth, int iHeight, int iStartSector, int iEndSector) {
        _iWidth = iWidth;
        _iHeight = iHeight;
        _iStartSector = iStartSector;
        _iEndSector = iEndSector;
        _blnIndexing = false;
        _audioFmt = new AudioFormat(CRUSADER_SAMPLES_PER_SECOND, 16, 2, true, false);
    }

    /** Constructor used for indexing. It will discover the dimensions and
     * start/end sector when it finds it. Use
     * {@link #indexFeedSector(jpsxdec.sectors.IdentifiedSector)} to know
     * if sectors are accepted. */
    public CrusaderDemuxer() {
        _iWidth = -1;
        _iHeight = -1;
        _iStartSector = -1;
        _iEndSector = -1;
        _blnIndexing = true;
        _audioFmt = null;
    }
    
    public int getStartSector() {
        return _iStartSector;
    }

    public int getEndSector() {
        return _iEndSector;
    }

    /** Returns if a at least 1 payload was discovered. Even if Crusader
     * sectors are found, there's no need to create a disc item if there's
     * never a payload. */
    public boolean foundAPayload() {
        return _blnFoundAPayload;
    }

    public void feedSector(IdentifiedSector identifiedSector) throws IOException {
        indexFeedSector(identifiedSector);
    }
    
    /** Used for indexing.
     * @return if the sector was accepted by the current movie. */
    public boolean indexFeedSector(IdentifiedSector identifiedSector) throws IOException {
        
        if (!(identifiedSector instanceof SectorCrusader))
            return false;
        
        if (_blnIndexing && _iStartSector == -1)
            _iStartSector = identifiedSector.getSectorNumber();
        else if (identifiedSector.getSectorNumber() < _iStartSector)
            return false;
        if (!_blnIndexing && identifiedSector.getSectorNumber() > _iEndSector)
            return false;
        
        SectorCrusader cru = (SectorCrusader) identifiedSector;

        // make sure the Crusader identified sector number is part of the same movie
        if (_iPreviousCrusaderSectorNumber != -1 && cru.getCrusaderSectorNumber() < _iPreviousCrusaderSectorNumber)
            return false; // new disc item

        // check for skipped sectors
        else if (_iPreviousCrusaderSectorNumber + 1 != cru.getCrusaderSectorNumber()) {
            for (int iMissingSector = _iPreviousCrusaderSectorNumber + 1; 
                 iMissingSector < cru.getCrusaderSectorNumber(); 
                 iMissingSector++)
            {
                log.warning("Missing sector while demuxing Crusader " + iMissingSector);
                feedSectorIteration(null);
            }
        }
        
        feedSectorIteration(cru);
        
        if (_blnIndexing)
            _iEndSector = identifiedSector.getSectorNumber();
        _iPreviousCrusaderSectorNumber = cru.getCrusaderSectorNumber();
        return true;
    }

    /** Feed either a real sector or null if a sector is missing. */
    private void feedSectorIteration(SectorCrusader cru) throws IOException {
        int iCurSectorOffset = 0;
        
        while (iCurSectorOffset < SectorCrusader.CRUSADER_IDENTIFIED_USER_DATA_SIZE) {
            switch (_state) {
                case MAGIC:
                    if (cru == null)
                        return;
                    String sBlockType = cru.readMagic(iCurSectorOffset);
                    iCurSectorOffset += 4;
                    _state = ReadState.LENGTH;
                    if ("MDEC".equals(sBlockType))
                        _ePayloadType = PayloadType.MDEC;
                    else if ("ad20".equals(sBlockType))
                        _ePayloadType = PayloadType.AD20;
                    else if ("ad21".equals(sBlockType))
                        _ePayloadType = PayloadType.AD21;
                    else
                        _state = ReadState.MAGIC;
                    break;
                case LENGTH:
                    if (cru == null) {
                        _state = ReadState.MAGIC;
                        return;
                    }
                    _iPayloadSize = cru.readSInt32BE(iCurSectorOffset);
                    _iRemainingPayload = _iPayloadSize - 8;
                    iCurSectorOffset += 4;
                    _state = ReadState.HEADER1;
                    break;
                case HEADER1:
                    if (cru == null) {
                        _state = ReadState.MAGIC;
                        return;
                    }
                    cru.copyIdentifiedUserData(iCurSectorOffset, _abHeader, 0, 4);
                    _iRemainingPayload -= 4;
                    iCurSectorOffset += 4;
                    _state = ReadState.HEADER2;
                    break;
                case HEADER2:
                    if (cru == null) {
                        _state = ReadState.MAGIC;
                        return;
                    }
                    cru.copyIdentifiedUserData(iCurSectorOffset, _abHeader, 4, 4);
                    _iRemainingPayload -= 4;
                    iCurSectorOffset += 4;
                    _state = ReadState.PAYLOAD;
                    break;
                case PAYLOAD:
                    _blnFoundAPayload = true;
                    
                    if (_sectors.isEmpty())
                        _iPayloadStartOffset = iCurSectorOffset;
                    _sectors.add(cru); // add the sector even if it's null. letting the payload handle null sectors
                    
                    int iDataLeftInSector = SectorCrusader.CRUSADER_IDENTIFIED_USER_DATA_SIZE - iCurSectorOffset;
                    if (_iRemainingPayload <= iDataLeftInSector) {
                        // payload is done
                        
                        if (_ePayloadType == PayloadType.MDEC) {
                            if (_frameListener != null)
                                videoPayload(_iPayloadSize - 16);
                        } else if (_audioListener != null) {
                            audioPayload(_iPayloadSize - 16);
                        }
                        _sectors.clear();
                        
                        iCurSectorOffset += _iRemainingPayload;
                        _iRemainingPayload = -1;
                        _state = ReadState.MAGIC;
                    } else {
                        _iRemainingPayload -= iDataLeftInSector;
                        iCurSectorOffset += iDataLeftInSector;
                    }
                    break;
            }
        }
    }
    
    public void flush() throws IOException {
        if (!_sectors.isEmpty() && _state == ReadState.PAYLOAD) {
            _blnFoundAPayload = true;
            if (_ePayloadType == PayloadType.MDEC) {
                if (_frameListener != null)
                    videoPayload(_iPayloadSize - 16 - _iRemainingPayload);
            } else if (_audioListener != null) { // ad20, ad21
                audioPayload(_iPayloadSize - 16 - _iRemainingPayload);
                _audioListener.write(_audioFmt, _decodedAudBuffer.getBuffer(), 0, _decodedAudBuffer.size(), _iCurrentAudioPresSector);
                _iCurrentAudioPresSector = -1;
            }
            _sectors.clear();
        }
        if (_decodedAudBuffer.size() > 0)
        _iRemainingPayload = -1;
        _state = ReadState.MAGIC;
    }
    
    //-- Video stuff ---------------------------
    
    private void videoPayload(int iSize) throws IOException {
        
        int iWidth = IO.readSInt16BE(_abHeader, 0);
        int iHeight = IO.readSInt16BE(_abHeader, 2);
        int iFrame = IO.readSInt32BE(_abHeader, 4);
        
        if (_blnIndexing && _iWidth == -1)
            _iWidth = iWidth;
        else if (_iWidth != iWidth)
            throw new IOException("Inconsistent width " + _iWidth + " != " + iWidth);
        if (_blnIndexing && _iHeight == -1)
            _iHeight = iHeight;
        else if (_iHeight != iHeight)
            throw new IOException("Inconsistent height " + _iHeight + " != " + iHeight);
        if (iFrame < 0)
            throw new IOException("Invalid frame number " + iFrame);

        if (_iInitialPresentationSector < 0) {
            _iInitialPresentationSector = iFrame * 10;
            if (_iInitialPresentationSector != 0)
                log.warning("[Video] Setting initial presentation sector " + _iInitialPresentationSector);
        }

        int iPresentationSector = iFrame * 10 - _iInitialPresentationSector;

        if (DEBUG)
            System.out.format( "[Frame %d] Presentation sector %d Size %d Start %d.%d End %d", 
                    iFrame, iPresentationSector, iSize,
                    _sectors.get(0).getSectorNumber(), _iPayloadStartOffset,
                    _sectors.get(_sectors.size()-1).getSectorNumber() ).println();
        
        SectorCrusader[] aoSects = _sectors.toArray(new SectorCrusader[_sectors.size()]);

        if (DEBUG)
            System.out.format("Writing frame %d to be presented at sector %d", iFrame, _iStartSector + iPresentationSector).println();
        _frameListener.frameComplete(new DemuxedCrusaderFrame(_iWidth, _iHeight, 
                                                              aoSects, iSize, 
                                                              _iPayloadStartOffset, 
                                                              iFrame, _iStartSector + iPresentationSector));
    }
    
    public int getHeight() {
        return _iHeight;
    }

    public int getWidth() {
        return _iWidth;
    }

    public void setFrameListener(ICompletedFrameListener listener) {
        _frameListener = listener;
    }

    //-- Audio stuff ---------------------------
    
    private byte[] _abAudioDemuxBuffer;
    private SquareAdpcmDecoder _audDecoder = new SquareAdpcmDecoder(1.0);
    private final ExposedBAOS _decodedAudBuffer = new ExposedBAOS();
    private int _iCurrentAudioPresSector = 0;
    
    private void audioPayload(final int iSize) throws IOException {

        if (iSize % 2 != 0)
            throw new IllegalArgumentException("Uneven Crusader audio payload size " + iSize);
        
        // .. read the header values ...............................
        final long lngPresentationSample = IO.readSInt32BE(_abHeader, 0);
        if (lngPresentationSample < 0)
            throw new IOException("Invalid presentation sample " + lngPresentationSample);
        final long lngAudioId = IO.readUInt32BE(_abHeader, 4);
        if (lngAudioId != AUDIO_ID)
            throw new IOException("Invalid Crusader audio id " + lngAudioId);

        // .. copy the audio data out of the sectors ...............
        if (_abAudioDemuxBuffer == null || _abAudioDemuxBuffer.length < iSize)
            _abAudioDemuxBuffer = new byte[iSize];
        else
            // pre-fill the buffer with 0 in case we are missing sectors at end
            Arrays.fill(_abAudioDemuxBuffer, 0, iSize, (byte)0);
        for (int iBufferPos = 0, iSect = 0; iSect < _sectors.size(); iSect++) {
            int iBytesToCopy;
            if (iSect == 0)
                iBytesToCopy = SectorCrusader.CRUSADER_IDENTIFIED_USER_DATA_SIZE - _iPayloadStartOffset;
            else
                iBytesToCopy = SectorCrusader.CRUSADER_IDENTIFIED_USER_DATA_SIZE;
            
            // only copy as much as we need from the last sector(s)
            if (iBufferPos + iBytesToCopy > iSize)
                iBytesToCopy = iSize - iBufferPos;
            if (iBytesToCopy == 0)
                break;
            
            SectorCrusader chunk = _sectors.get(iSect);
            if (chunk != null) {
                if (iSect == 0)
                    chunk.copyIdentifiedUserData(_iPayloadStartOffset, _abAudioDemuxBuffer, iBufferPos, iBytesToCopy);
                else
                    chunk.copyIdentifiedUserData(0, _abAudioDemuxBuffer, iBufferPos, iBytesToCopy);
            } else {
                log.warning("Missing sector " + iSect + " from Crusader audio data");
                // just skip the bytes that would have been copied
                // they were already previously set to 0
            }
            iBufferPos += iBytesToCopy;
        }

        // .. check for missing data ............................
        {
            if (_iCurrentAudioPresSector < 0)
                throw new IllegalStateException("Crusader audio decoding was finished but still got more");
            
            int iBufferedSamples = _decodedAudBuffer.size() / BYTES_PER_SAMPLE;
            long lngCurrentPresSample = (long)_iCurrentAudioPresSector * SAMPLES_PER_SECTOR + iBufferedSamples;
            if (lngPresentationSample != lngCurrentPresSample) {
                long lngMissingSamples = lngPresentationSample - lngCurrentPresSample;
                log.warning("Missing "+ lngMissingSamples +" samples of Crusader audio, writing silence");

                if (_decodedAudBuffer.size() > 0) {
                    // 1) add silence to fill up the current sector
                    int iSamplesPad = SAMPLES_PER_SECTOR - iBufferedSamples;
                    IO.writeZeros(_decodedAudBuffer, iSamplesPad * BYTES_PER_SAMPLE);
                    // 2) write that
                    _audioListener.write(_audioFmt, _decodedAudBuffer.getBuffer(), 0, SAMPLES_PER_SECTOR, _iCurrentAudioPresSector);
                    _decodedAudBuffer.reset();
                }
                // 3) again add silence to align the incoming data to sector boundary
                _iCurrentAudioPresSector = (int) (lngPresentationSample / SAMPLES_PER_SECTOR);
                int iSamplesPad = (int) (lngPresentationSample - (_iCurrentAudioPresSector * SAMPLES_PER_SECTOR));
                IO.writeZeros(_decodedAudBuffer, iSamplesPad * BYTES_PER_SAMPLE);
            }
        }

        // if the initial portion of a movie is missing, and audio is the first
        // payload found, we need to adjust the initial presentation offset
        // so we don't write a ton silence initially in an effort to catch up.
        // after analyzing a crusader movie, it seems audio payload presentation
        // sectors run about 40 sectors ahead of the video presentation sectors.
        // so pick an initial presentation sector a little before when the next
        // frame should be presented
        if (_iInitialPresentationSector < 0) {
            _iInitialPresentationSector = _iCurrentAudioPresSector - 60;
            if (_iInitialPresentationSector < 0) // don't start before the start of the movie
                _iInitialPresentationSector = 0;
            if (_iInitialPresentationSector > 0)
                log.warning("[Audio] Setting initial presentation sector " + _iInitialPresentationSector);
        }

        // .. decode the audio data .............................
        {
            int iChannelSize = iSize / 2; // size is already confirmed to be divisible by 2
            _audDecoder.decode(new ByteArrayInputStream(_abAudioDemuxBuffer, 0, iChannelSize),
                               new ByteArrayInputStream(_abAudioDemuxBuffer, iChannelSize, iChannelSize),
                               iChannelSize, _decodedAudBuffer);
        }

        // .. write it as sectors worth of samples ..............
        final int iDataToPlay;
        {
            int iPresentationSector = _iCurrentAudioPresSector - _iInitialPresentationSector;
            if (_decodedAudBuffer.size() % BYTES_PER_SAMPLE != 0)
                throw new RuntimeException("Buffer size not divisible by sample size");
            if (_ePayloadType == PayloadType.AD20) {
                int iSamples = _decodedAudBuffer.size() / BYTES_PER_SAMPLE;
                // this will trim off samples that don't cleanly divide into sectors
                int iSectorsToPlay = iSamples / SAMPLES_PER_SECTOR;
                iDataToPlay = iSectorsToPlay * SAMPLES_PER_SECTOR * BYTES_PER_SAMPLE;
                _iCurrentAudioPresSector += iSectorsToPlay;
            } else { // _ePayloadType == BLOCKTYPE.AD21
                // it's the last audio chunk, play it all now because we won't get another chance
                iDataToPlay = _decodedAudBuffer.size();
                _iCurrentAudioPresSector = -1;
            }

            if (DEBUG)
                System.out.format("Writing %d bytes of audio to be presented at sector %d", iDataToPlay, _iStartSector + iPresentationSector).println();
            _audioListener.write(_audioFmt, _decodedAudBuffer.getBuffer(), 0, iDataToPlay, _iStartSector + iPresentationSector);
        }

        // .. copy any remaing data to the head of the decoded buffer .....
        {
            int iRemainingData = _decodedAudBuffer.size() - iDataToPlay;
            if (iRemainingData % BYTES_PER_SAMPLE != 0)
                throw new RuntimeException("Remaining data not divisible by sample size");
            _decodedAudBuffer.reset();
            if (iRemainingData > 0) {
                _decodedAudBuffer.write(_decodedAudBuffer.getBuffer(), iDataToPlay, iRemainingData);
            }
        }
        
    }
    
    
    public void setAudioListener(ISectorTimedAudioWriter audioFeeder) {
        _audioListener = audioFeeder;
    }

    public int getPresentationStartSector() {
        return _iStartSector; 
   }

    public AudioFormat getOutputFormat() {
        return _audioFmt;
    }

    public int getSamplesPerSecond() {
        return CRUSADER_SAMPLES_PER_SECOND;
    }
    
    public int getDiscSpeed() {
        return 2;
    }

    public double getVolume() {
        return _audDecoder.getVolume();
    }

    public void setVolume(double dblVolume) {
        _audDecoder.setVolume(dblVolume);
    }

    public void reset() {
        _audDecoder.resetContext();
    }
    
    public void printAudioDetails(PrintStream ps) {
        ps.println("Embedded Crusader audio " + CRUSADER_SAMPLES_PER_SECOND + "Hz");
    }
    
}
