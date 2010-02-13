/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.player.AudioProcessor;
import jpsxdec.player.IAudioVideoReader;
import jpsxdec.player.IDecodableAudioChunk;
import jpsxdec.player.IDecodableFrame;
import jpsxdec.player.ObjectPool;
import jpsxdec.player.VideoProcessor;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.psx.str.DiscItemSTRVideo;
import jpsxdec.plugins.psx.str.IVideoSector;
import jpsxdec.plugins.psx.str.FrameDemuxer;
import jpsxdec.plugins.psx.str.IDemuxReceiver;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;
import jpsxdec.plugins.psx.video.decode.UncompressionException;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder_int;
import jpsxdec.plugins.psx.video.mdec.idct.simple_idct;
import jpsxdec.plugins.xa.IAudioSectorDecoder;
import jpsxdec.plugins.xa.DiscItemAudioStream;
import jpsxdec.util.NotThisTypeException;

/** Holds all the class implementations that the jpsxdec.player framework
 *  needs to playback PlayStation audio and/or video. */
public class MediaPlayer implements IAudioVideoReader {

    private static final boolean DEBUG = false;

    private final int _iMovieStartSector;
    private final int _iMovieEndSector;
    private int _iSector;
    private final CDSectorReader _cdReader;

    //----------------------------------------------------------

    private final MdecDecoder_int _decoder;
    private final DiscItemSTRVideo _vid;
    private final int _iSectorsPerSecond = 150;
    private final int[] _aiFrameIndexes;
    private DemuxFrameUncompressor _uncompressor;
    private FrameDemuxer _demuxer;

    public MediaPlayer(DiscItemSTRVideo vid)
            throws UnsupportedAudioFileException, IOException
    {
        this(vid, vid.getStartSector(), vid.getEndSector());
    }


    public MediaPlayer(DiscItemSTRVideo vid, int iSectorStart, int iSectorEnd)
            throws UnsupportedAudioFileException, IOException
    {
        _cdReader = vid.getSourceCD();
        _iSector = _iMovieStartSector = iSectorStart;
        _iMovieEndSector = iSectorEnd;

        _vid = vid;
        _decoder = new MdecDecoder_int(new simple_idct(),
                                       vid.getWidth(),
                                       vid.getHeight());

        _aiFrameIndexes = new int[_vid.getEndFrame() - _vid.getStartFrame() + 1];
    }

    //-----------------------------------------------------------------------

    private final static boolean AUDIO_BIGENDIAN = true;
    private IAudioSectorDecoder _audioDecoder;
    private SourceDataLineAudioReceiver _audioOut;

    public MediaPlayer(DiscItemAudioStream aud)
            throws FileNotFoundException, UnsupportedAudioFileException,
                   IOException
    {
        _cdReader = aud.getSourceCD();
        _iSector = _iMovieStartSector = aud.getStartSector();
        _iMovieEndSector = aud.getEndSector();

        _audioDecoder = aud.makeDecoder(AUDIO_BIGENDIAN, 1.0);

        // ignore video
        _decoder = null;
        _vid = null;
        _aiFrameIndexes = null;
    }
    
    //----------------------------------------------------------

    MediaPlayer(DiscItemSTRVideo vid, IAudioSectorDecoder audio, int iSectorStart, int iSectorEnd) throws UnsupportedAudioFileException, IOException {
        // do the video init
        this(vid, iSectorStart, iSectorEnd);

        // manually init the audio
        _audioDecoder = audio;
    }


    public int readNext(VideoProcessor vidProc, AudioProcessor audProc) {

        try {

            if (!(_iSector < _iMovieEndSector)) {
                return -1;
            }

            CDSector cdSector = _cdReader.getSector(_iSector);
            IdentifiedSector identifiedSector = JPSXPlugin.identifyPluginSector(cdSector);
            if (vidProc != null && identifiedSector instanceof IVideoSector) {
                if (_demuxer == null) {
                    _demuxer = new FrameDemuxer(
                            new DemuxReceiver(vidProc),
                            _iMovieStartSector, _iMovieEndSector);
                }
                _demuxer.feedSector((IVideoSector) identifiedSector);
            } else if (audProc != null && identifiedSector != null) {
                audProc.addDecodableAudioChunk(new XAAudioChunk(identifiedSector));
            }
            _iSector++;
            return (_iSector - _iMovieStartSector) * 100 / (_iMovieEndSector - _iMovieStartSector);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private class DemuxReceiver implements IDemuxReceiver {

        private VideoProcessor _vidProc;

        public DemuxReceiver(VideoProcessor vidProc) {
            _vidProc = vidProc;
        }

        @Override
        public int getWidth() {
            return _vid.getWidth();
        }

        @Override
        public int getHeight() {
            return _vid.getHeight();
        }

        @Override
        public void receive(byte[] abDemux, int iSize, int iFrameNumber, int iFrameEndSector) throws IOException {
            StrFrame strFrame = _framePool.borrow();
            strFrame.init(abDemux, iSize, iFrameNumber, iFrameEndSector - _iMovieStartSector);
            _vidProc.addFrame(strFrame);
        }
    }

    public void seekToTime(long lngTime) {
        if (_audioDecoder != null)
            _audioDecoder.reset();
        _iSector = _iMovieStartSector + (int)(lngTime * _iSectorsPerSecond / 1000);
        throw new RuntimeException();
        // TODO: either backup or move forward to the beginning of a frame (if there is video)
    }

    public void reset() {
        if (_audioDecoder != null)
            _audioDecoder.reset();
        _iSector = _iMovieStartSector;
    }

    // #########################################################################
    // #########################################################################

    @Override
    public AudioFormat getAudioFormat() {
        if (_audioDecoder == null)
            return null;
        return _audioDecoder.getOutputFormat();
    }

    private class XAAudioChunk implements IDecodableAudioChunk {

        private IdentifiedSector __sector;

        public XAAudioChunk(IdentifiedSector sector) {
            __sector = sector;
        }

        @Override
        public void decodeAudio(SourceDataLine dataLine) throws IOException {
            if (_audioOut == null) {
                _audioOut = new SourceDataLineAudioReceiver(dataLine, _iSectorsPerSecond, _iMovieStartSector);
                _audioDecoder.open(_audioOut);
            }
            _audioDecoder.feedSector(__sector);
        }

    }


    // #########################################################################
    // #########################################################################

    private class DecodableFramePool extends ObjectPool<StrFrame> {

        @Override
        protected StrFrame createExpensiveObject() {
            if (DEBUG) System.err.println("Creating new pool object.");
            return new StrFrame();
        }

    }
    private final DecodableFramePool _framePool = new DecodableFramePool();


    public void seekToFrame(int iFrame) {
        if (_aiFrameIndexes[iFrame - _vid.getStartFrame()] < 1) {
            try {
                _iSector = _vid.seek(iFrame).getSectorNumber();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            _iSector = _aiFrameIndexes[iFrame - _vid.getStartFrame()];
        }
        if (_audioDecoder != null)
            _audioDecoder.reset();
        // TODO? backup and get the audio for this frame?
    }

    @Override
    public boolean hasVideo() {
        return _vid != null;
    }

    @Override
    public int getVideoWidth() {
        return _vid.getWidth();
    }

    @Override
    public int getVideoHeight() {
        return _vid.getHeight();
    }


    private class StrFrame implements IDecodableFrame {

        private byte[] __abDemuxBuf;
        private int __iFrame;
        private int __iSectorFromStart;
        
        public void init(byte[] abDemuxBuf, int iSize, int iFrame, int iSectorFromStart) {
            if (__abDemuxBuf == null || __abDemuxBuf.length < iSize)
                __abDemuxBuf = new byte[iSize];
            System.arraycopy(abDemuxBuf, 0, __abDemuxBuf, 0, iSize);
            __iSectorFromStart = iSectorFromStart;
            __iFrame = iFrame;
        }

        public long getPresentationTime() {
            return (long)(__iSectorFromStart * 1000 / _iSectorsPerSecond);
        }

        public void decodeVideo(RgbIntImage drawHere) {
            if (_uncompressor == null) {
                _uncompressor = JPSXPlugin.identifyUncompressor(__abDemuxBuf, 0, __iFrame);
                if (_uncompressor == null) {
                    System.err.println("Unable to identify frame type.");
                    return;
                }
            }
            try {
                _uncompressor.reset(__abDemuxBuf, 0);
            } catch (NotThisTypeException ex) {
                _uncompressor = JPSXPlugin.identifyUncompressor(__abDemuxBuf, 0, __iFrame);
                if (_uncompressor == null) {
                    System.err.println("Unable to identify frame type.");
                    return;
                }
            }

            try {
                _decoder.decode(_uncompressor);
            } catch (UncompressionException ex) {
                ex.printStackTrace();
            }

            _decoder.readDecodedRGB(drawHere);
        }

        private long _lngContiguousId;
        @Override
        public void setContiguiousId(long lngId) { _lngContiguousId = lngId; }
        @Override
        public long getContigiousId() { return _lngContiguousId; }

        @Override
        public void returnToPool() {
            if (DEBUG) System.err.println("Returning object to pool.");
            _framePool.giveBack(this);
        }

    }

}
