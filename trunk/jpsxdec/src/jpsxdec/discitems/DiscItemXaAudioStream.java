/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.audio.XaAdpcmDecoder;
import jpsxdec.audio.XaAdpcmEncoder;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.sectors.SectorXaAudio;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.NotThisTypeException;

/** Represents a series of XA ADPCM sectors that combine to make an audio stream. */
public class DiscItemXaAudioStream extends DiscItemAudioStream {
    /** Type identifier for this disc item. */
    public static final String TYPE_ID = "XA";

    /** Serialization key for Channel. */
    private final static String CHANNEL_KEY = "Channel";
    /** CD stream channel number. Should be between 0 and 31, inclusive. */
    private final int _iChannel;
    
    /** Serialization key for sample rate. */
    private static final String SAMPLES_PER_SEC_KEY = "Samples/Sec";
    /** Sample rate of the audio stream. Should be either 37800 or 18900. */
    private final int _iSamplesPerSecond;

    /** Serialization key for stereo. */
    private static final String STEREO_KEY = "Stereo?";
    /** If the audio is in stereo. */
    private final boolean _blnIsStereo;

    /** Serialization key for bits/sample. */
    private final static String BITSPERSAMPLE_KEY = "Bits/Sample";
    /** ADPCM bits per sample that the audio is encoded as. Should be 4 or 8. */
    private final int _iBitsPerSample;

    /** Serialization key for audio sector stride. */
    private final static String STRIDE_KEY = "Sector stride";
    /** Number of non-audio sectors between each audio sector. 
     *  Should be 1, 4, 8, 16, or 32, or -1 if unknown (only one sector of audio).*/
    private final int _iSectorStride;

    /** Serialization key for disc speed. */
    private final static String DISC_SPEED_KEY = "Disc speed";
    /** Speed that the disc must spin to properly play the audio stream.
     * <ul>
     * <li>1 for 1x (75 sectors/second)
     * <li>2 for 2x (150 sectors/second)
     * <li> -1 for unknown
     *     (unknown case should only occur if this audio
     *      stream is only one sector long)
     *      or invalid (sector stride is 1)
     * </ul>
     */
    private int _iDiscSpeed;

    public DiscItemXaAudioStream(@Nonnull CdFileSectorReader cd,
                                 int iStartSector, int iEndSector,
                                 int iChannel, int iSamplesPerSecond,
                                 boolean blnIsStereo, int iBitsPerSample,
                                 int iStride)
    {
        super(cd, iStartSector, iEndSector);
        
        if (iChannel < 0 || iChannel > SectorXaAudio.MAX_VALID_CHANNEL) throw new IllegalArgumentException(
                "Channel " + iChannel + " is not between 0 and " + SectorXaAudio.MAX_VALID_CHANNEL);
        if (iBitsPerSample != 4 && iBitsPerSample != 8)
            throw new IllegalArgumentException("Bits/sample " + iBitsPerSample + " is not 4 or 8");
        if (iSamplesPerSecond != 37800 && iSamplesPerSecond != 18900)
            throw new IllegalArgumentException();
        if (iStride != -1 && iStride != 1 && iStride != 2 &&
            iStride != 4 && iStride != 8 && iStride != 16 && iStride != 32)
            throw new IllegalArgumentException("Illegal audio sector stride " + iStride);
        
        _iSamplesPerSecond = iSamplesPerSecond;
        _blnIsStereo = blnIsStereo;
        _iChannel = iChannel;
        _iBitsPerSample = iBitsPerSample;
        _iSectorStride = iStride;

        // if there is no sector stride (iStride == -1, 1 sector long)
        // or the stride is only 1, then the disc speed is unknown/invalid
        if (_iSectorStride == -1 || _iSectorStride == 1) {
            _iDiscSpeed = -1;
        } else {
            _iDiscSpeed = SectorXaAudio.calculateDiscSpeed(_iSamplesPerSecond, _blnIsStereo, _iBitsPerSample, _iSectorStride);
            if (_iDiscSpeed < 1)
                throw new RuntimeException(String.format(
                        "Disc speed calc doesn't add up: Samples/sec %d Stereo %s Bits/sample %s Stride %d",
                        _iSamplesPerSecond, String.valueOf(_blnIsStereo),
                        _iBitsPerSample, _iSectorStride));
        }
    }

    public DiscItemXaAudioStream(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws NotThisTypeException
    {
        super(cd, fields);
        
        String sStereo = fields.getString(STEREO_KEY);
        if ("Yes".equals(sStereo))
            _blnIsStereo = true;
        else if ("No".equals(sStereo))
            _blnIsStereo = false;
        else throw new NotThisTypeException(I.FIELD_HAS_INVALID_VALUE_STR(STEREO_KEY, sStereo));
            
        _iSamplesPerSecond = fields.getInt(SAMPLES_PER_SEC_KEY);
        _iChannel = fields.getInt(CHANNEL_KEY);

        _iBitsPerSample = fields.getInt(BITSPERSAMPLE_KEY);
        _iSectorStride = fields.getInt(STRIDE_KEY);
        if (_iSectorStride != -1 && _iSectorStride != 1 && _iSectorStride != 2 &&
            _iSectorStride != 4 && _iSectorStride != 8 && _iSectorStride != 16 && _iSectorStride != 32)
            throw new NotThisTypeException(I.FIELD_HAS_INVALID_VALUE_NUM(STRIDE_KEY, _iSectorStride));

        String sDiscSpeed = fields.getString(DISC_SPEED_KEY);
        if ("1x".equals(sDiscSpeed))
            _iDiscSpeed = 1;
        else if ("2x".equals(sDiscSpeed))
            _iDiscSpeed = 2;
        else if ("?".equals(sDiscSpeed))
            _iDiscSpeed = -1;
        else throw new NotThisTypeException(I.FIELD_HAS_INVALID_VALUE_STR(DISC_SPEED_KEY, sDiscSpeed));
    }
    
    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem fields = super.serialize();

        fields.addNumber(CHANNEL_KEY, _iChannel);
        fields.addString(STEREO_KEY, _blnIsStereo ? "Yes" : "No");
        fields.addNumber(SAMPLES_PER_SEC_KEY, _iSamplesPerSecond);
        fields.addNumber(BITSPERSAMPLE_KEY, _iBitsPerSample);
        fields.addNumber(STRIDE_KEY, _iSectorStride);
        switch (_iDiscSpeed) {
            case 1:  fields.addString(DISC_SPEED_KEY, "1x"); break;
            case 2:  fields.addString(DISC_SPEED_KEY, "2x"); break;
            default: fields.addString(DISC_SPEED_KEY, "?"); break;
        }
        return fields;
    }

    public int getChannel() {
        return _iChannel;
    }
    
    public boolean isStereo() {
        return _blnIsStereo;
    }

    public int getSectorStride() {
        return _iSectorStride;
    }
    
    public int getSectorsPastEnd() {
        return _iSectorStride - 1;
    }

    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        Date secs = new Date(0, 0, 0, 0, 0, (int)Math.max(getApproxDuration(), 1));
        return I.GUI_XA_DESCRIPTION(secs, _iSamplesPerSecond, _blnIsStereo ? 2 : 1);
    }
    
    public int getDiscSpeed() {
        return _iDiscSpeed;
    }

    public int getSampleRate() {
        return _iSamplesPerSecond;
    }

    public int getAdpcmBitsPerSample() {
        return _iBitsPerSample;
    }

    public int getPresentationStartSector() {
        return getStartSector();
    }

    @Override
    public double getApproxDuration() {
        return getSampleCount() / (double)_iSamplesPerSecond;
    }

    public long getSampleCount() {
        int iAudioSectorCount;
        int iSectorLength = getSectorLength();
        if (iSectorLength == 1)
            iAudioSectorCount = 1;
        else {
            assert _iSectorStride > 0;
            iAudioSectorCount = getSectorLength() / _iSectorStride + 1;
        }
        long lngSampleCount = iAudioSectorCount *
                (long)XaAdpcmDecoder.pcmSamplesGeneratedFromXaAdpcmSector(_iBitsPerSample);
        if (_blnIsStereo)
            return lngSampleCount / 2;
        else
            return lngSampleCount;
    }

    public @Nonnull AudioFormat getAudioFormat(boolean blnBigEndian) {
        return new AudioFormat(_iSamplesPerSecond, 16, _blnIsStereo ? 2 : 1, true, blnBigEndian);
    }

    public @Nonnull ISectorAudioDecoder makeDecoder(double dblVolume) {
        return new XAConverter(dblVolume);
    }

    public @Nonnull DiscItemXaAudioStream[] split(int iBeforeSector) {
        if (iBeforeSector <= getStartSector() || iBeforeSector > getEndSector())
            throw new IllegalArgumentException("Split sector outside the bounds of XA audio stream");

        int iFirstEnd = iBeforeSector - ((iBeforeSector - getStartSector()-1) % _iSectorStride) - 1;
        int iSecondStart = iBeforeSector + (_iSectorStride - (iBeforeSector - getStartSector()-1) % _iSectorStride) - 1;
        DiscItemXaAudioStream first = new DiscItemXaAudioStream(getSourceCd(), getStartSector(), iFirstEnd,
                _iChannel, _iSamplesPerSecond, _blnIsStereo, _iBitsPerSample, _iSectorStride);
        DiscItemXaAudioStream second = new DiscItemXaAudioStream(getSourceCd(), iSecondStart, getEndSector(),
                _iChannel, _iSamplesPerSecond, _blnIsStereo, _iBitsPerSample, _iSectorStride);
        return new DiscItemXaAudioStream[] { first, second };
    }
    
    public void replaceXa(@Nonnull PrintStream ps, @Nonnull File audioFile)
            throws UnsupportedAudioFileException, IOException, IncompatibleException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
        try {
            AudioFormat fmt = ais.getFormat();
            if (Math.abs(fmt.getSampleRate() - _iSamplesPerSecond) > 0.1f ) {
                throw new IncompatibleException(I.XA_COPY_REPLACE_SAMPLE_RATE_MISMATCH(
                                                fmt.getSampleRate(), _iSamplesPerSecond));
            }
            if (( _blnIsStereo && (fmt.getChannels() != 2)) ||
                (!_blnIsStereo && (fmt.getChannels() != 1)))
            {
                throw new IncompatibleException(I.XA_COPY_REPLACE_CHANNEL_MISMATCH(
                                                fmt.getChannels(),
                                                _blnIsStereo ? 2 : 1));
            }
            XaAdpcmEncoder encoder = new XaAdpcmEncoder(ais, _iBitsPerSample);
            IdentifiedSectorIterator it = identifiedSectorIterator();
            while (it.hasNext()) {
                IdentifiedSector origIdSect = it.next();
                if (origIdSect instanceof SectorXaAudio && isPartOfStream((SectorXaAudio)origIdSect)) {
                    CdSector origSect = origIdSect.getCdSector();
                    byte[] abOrigData = origSect.getCdUserDataCopy();
                    ExposedBAOS baos = new ExposedBAOS(abOrigData.length);
                    encoder.encode1Sector(baos);
                    System.arraycopy(baos.getBuffer(), 0, abOrigData, 0, baos.size());
                    if (encoder.isEof()) {
                        ps.println(I.XA_ENCODE_REPLACE_SRC_AUDIO_EXHAUSTED());
                    }
                    ps.println(I.CMD_PATCHING_SECTOR());
                    ps.println(origIdSect);
                    getSourceCd().writeSector(origSect.getSectorNumberFromStart(), abOrigData);
                }
            }
        } finally {
            try {
                ais.close();
            } catch (IOException ex) {
                Logger.getLogger(DiscItemXaAudioStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public void replaceXa(@Nonnull PrintStream ps, @Nonnull DiscItemXaAudioStream other) 
            throws IOException, IncompatibleException
    {
        ps.println(I.CMD_PATCHING_DISC_ITEM());
        ps.println(this);
        ps.println(I.CMD_PATCHING_WITH_DISC_ITEM());
        ps.println(other);

        if (getSampleRate() != other.getSampleRate() ||
            _blnIsStereo    != other._blnIsStereo    ||
            _iBitsPerSample != other._iBitsPerSample)
            throw new IncompatibleException(I.XA_ENCODE_REPLACE_FORMAT_MISMATCH());

        IdentifiedSectorIterator origIt = identifiedSectorIterator();
        IdentifiedSectorIterator patchIt = other.identifiedSectorIterator();
        EndOfOther:
        while (origIt.hasNext()) {
            IdentifiedSector origIdSect = origIt.next();
            if (origIdSect instanceof SectorXaAudio && isPartOfStream((SectorXaAudio)origIdSect)) {
                SectorXaAudio origXaSect = (SectorXaAudio) origIdSect;
                // seek to the next other XA sector
                IdentifiedSector patchIdSect = null;
                do {
                    if (!patchIt.hasNext()) {
                        ps.println(I.XA_COPY_REPLACE_SRC_XA_EXHAUSTED());
                        break EndOfOther;
                    }
                    patchIdSect = patchIt.next();
                } while (!(patchIdSect instanceof SectorXaAudio && other.isPartOfStream((SectorXaAudio)patchIdSect)));
                SectorXaAudio patchXaSect = (SectorXaAudio) patchIdSect;
                ps.println(I.CMD_PATCHING_SECTOR());
                ps.println(origXaSect);
                ps.println(I.CMD_PATCHING_WITH_SECTOR());
                ps.println(patchXaSect);
                byte[] abPatchData = patchXaSect.getCdSector().getCdUserDataCopy();
                getSourceCd().writeSector(origXaSect.getSectorNumber(), abPatchData);
            }
        }
    }

    private class XAConverter implements ISectorAudioDecoder {

        @Nonnull
        private final XaAdpcmDecoder __decoder;
        /** Must be set before using this class. */
        @CheckForNull
        private ISectorTimedAudioWriter __outFeed;
        @Nonnull
        private final ExposedBAOS __tempBuffer;
        @CheckForNull
        private AudioFormat __format;

        public XAConverter(double dblVolume) {
            __decoder = XaAdpcmDecoder.create(DiscItemXaAudioStream.this.getAdpcmBitsPerSample(),
                                              DiscItemXaAudioStream.this.isStereo(), dblVolume);
            __tempBuffer = new ExposedBAOS(
                    XaAdpcmDecoder.bytesGeneratedFromXaAdpcmSector(
                    DiscItemXaAudioStream.this.getAdpcmBitsPerSample()));
        }

        public void setAudioListener(@Nonnull ISectorTimedAudioWriter audioFeed) {
            __outFeed = audioFeed;
        }

        public boolean feedSector(@Nonnull IdentifiedSector sector, @Nonnull Logger log) throws IOException {
            if (!(sector instanceof SectorXaAudio))
                return false;
            SectorXaAudio xaSector = (SectorXaAudio) sector;
            if (!isPartOfStream(xaSector))
                return false;

            __tempBuffer.reset();
            long lngSamplesWritten = __decoder.getSamplesWritten();
            __decoder.decode(xaSector.getIdentifiedUserDataStream(), __tempBuffer, xaSector.getSectorNumber());
            if (__decoder.hadCorruption())
                I.XA_AUDIO_CORRUPTED(xaSector.getSectorNumber(), lngSamplesWritten)
                        .log(log, Level.WARNING);

            if (__format == null)
                __format = __decoder.getOutputFormat(xaSector.getSamplesPerSecond());

            if (__outFeed == null)
                throw new IllegalStateException("Must set audio listener before feeding sectors.");
            __outFeed.write(__format, __tempBuffer.getBuffer(), 0, __tempBuffer.size(), xaSector.getSectorNumber());
            return true;
        }

        public double getVolume() {
            return __decoder.getVolume();
        }

        public int getSamplesPerSecond() {
            return _iSamplesPerSecond;
        }

        public void setVolume(double dblVolume) {
            __decoder.setVolume(dblVolume);
        }

        public @Nonnull AudioFormat getOutputFormat() {
            return __decoder.getOutputFormat(DiscItemXaAudioStream.this.getSampleRate());
        }

        public void reset() {
            __decoder.resetContext();
        }

        public int getEndSector() {
            return DiscItemXaAudioStream.this.getEndSector();
        }

        public int getStartSector() {
            return DiscItemXaAudioStream.this.getStartSector();
        }

        public int getPresentationStartSector() {
            return DiscItemXaAudioStream.this.getPresentationStartSector();
        }

        public int getDiscSpeed() {
            return DiscItemXaAudioStream.this.getDiscSpeed();
        }

        public @Nonnull ILocalizedMessage[] getAudioDetails() {
            return new ILocalizedMessage[] { DiscItemXaAudioStream.this.getDetails() };
        }

    }

    public boolean isPartOfStream(@Nonnull SectorXaAudio xaSector) {
        return xaSector.getSectorNumber() >= getStartSector() &&
               xaSector.getSectorNumber() <= getEndSector() &&
               xaSector.getBitsPerSample() == _iBitsPerSample &&
               xaSector.getChannel() == _iChannel &&
               xaSector.getBitsPerSample() == _iBitsPerSample &&
               xaSector.isStereo() == _blnIsStereo;
    }

}
