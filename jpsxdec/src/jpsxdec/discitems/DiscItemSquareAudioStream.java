/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.audio.SpuAdpcmDecoder;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.sectors.ISquareAudioSector;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.DeserializationFail;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.Fraction;
import jpsxdec.util.ILocalizedLogger;
import jpsxdec.util.LoggedFailure;

/** Represents a series of Square ADPCM sectors that combine to make an audio stream.
 * These kinds of streams are found in Final Fantasy 8, 9, and Chrono Cross.  */
public class DiscItemSquareAudioStream extends DiscItemAudioStream {

    private static final Logger LOG = Logger.getLogger(DiscItemSquareAudioStream.class.getName());

    public static final String TYPE_ID = "SquareAudio";

    private static final String SAMPLE_COUNT_LEFT_KEY = "Left samples";
    private final long _lngLeftSampleCount;
    
    private static final String SAMPLE_COUNT_RIGHT_KEY = "Right samples";
    private final long _lngRightSampleCount;
    
    private static final String SAMPLES_PER_SEC_KEY = "Samples/Sec";
    private final int _iSamplesPerSecond;

    private static final String SECTORS_PAST_END_KEY = "Sectors past end";
    private final int _iSectorsPastEnd;
    
    public DiscItemSquareAudioStream(@Nonnull CdFileSectorReader cd,
                                     int iStartSector, int iEndSector,
                                     long lngLeftSampleCount, long lngRightSampleCount,
                                     int iSamplesPerSecond, int iSectorsPastEnd)
    {
        super(cd, iStartSector, iEndSector);

        _lngLeftSampleCount = lngLeftSampleCount;
        _lngRightSampleCount = lngRightSampleCount;
        _iSamplesPerSecond = iSamplesPerSecond;
        _iSectorsPastEnd = iSectorsPastEnd;

        if (_lngLeftSampleCount != _lngRightSampleCount)
            LOG.log(Level.WARNING, "Left & right sample count does not match: {0,number,#} != {1,number,#}",
                    new Object[]{_lngLeftSampleCount, _lngRightSampleCount});
    }
    
    public DiscItemSquareAudioStream(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws DeserializationFail
    {
        super(cd, fields);
        
        _iSamplesPerSecond = fields.getInt(SAMPLES_PER_SEC_KEY);
        _lngLeftSampleCount = fields.getLong(SAMPLE_COUNT_LEFT_KEY);
        _lngRightSampleCount = fields.getLong(SAMPLE_COUNT_RIGHT_KEY);
        _iSectorsPastEnd = fields.getInt(SECTORS_PAST_END_KEY);

        if (_lngLeftSampleCount != _lngRightSampleCount)
            LOG.log(Level.WARNING, "Left & right sample count does not match: {0,number,#} != {1,number,#}",
                    new Object[]{_lngLeftSampleCount, _lngRightSampleCount});
    }
    
    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem fields = super.serialize();
        fields.addNumber(SAMPLES_PER_SEC_KEY, _iSamplesPerSecond);
        fields.addNumber(SAMPLE_COUNT_LEFT_KEY, _lngLeftSampleCount);
        fields.addNumber(SAMPLE_COUNT_RIGHT_KEY, _lngRightSampleCount);
        fields.addNumber(SECTORS_PAST_END_KEY, _iSectorsPastEnd);
        return fields;
    }

    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    public boolean isStereo() {
        return true;
    }

    public int getSectorsPastEnd() {
        return _iSectorsPastEnd;
    }

    public int getDiscSpeed() {
        return 2;
    }

    @Override
    public int getPresentationStartSector() {
        return getStartSector() + 1;
    }

    @Override
    public double getApproxDuration() {
        long lngSampleCount = _lngLeftSampleCount > _lngRightSampleCount ?
                              _lngLeftSampleCount : _lngRightSampleCount;
        return lngSampleCount / (double)_iSamplesPerSecond;
    }

    public int getSampleRate() {
        return _iSamplesPerSecond;
    }

    @Override
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        long lngSampleCount = _lngLeftSampleCount > _lngRightSampleCount ?
                              _lngLeftSampleCount : _lngRightSampleCount;
        // unable to find ANY sources of info about how to localize durations
        Date secs = new Date(0, 0, 0, 0, 0, (int)Math.max(lngSampleCount / _iSamplesPerSecond, 1));
        return I.GUI_SQUARE_AUDIO_DETAILS(secs, _iSamplesPerSecond);
    }

    public @Nonnull ISectorAudioDecoder makeDecoder(double dblVolume) {
        return new SquareConverter(new SpuAdpcmDecoder.Stereo(dblVolume));
    }

    // -------------------------------------------------------------------------

    private class SquareConverter implements ISectorAudioDecoder {

        @Nonnull
        private final SpuAdpcmDecoder.Stereo __decoder;
        @Nonnull
        private final AudioFormat __format;
        @Nonnull
        private final ExposedBAOS __tempBuffer;
        @CheckForNull
        private ISectorTimedAudioWriter __audioWriter;
        @CheckForNull
        private ISquareAudioSector __leftAudioSector, __rightAudioSector;

        public SquareConverter(@Nonnull SpuAdpcmDecoder.Stereo decoder) {
            __decoder = decoder;
            // 1840 is the most # of ADPCM bytes found in any Square game sector
            // (FF8, FF9, Chrono Cross) multitplied by 2 since there are always
            // 2 sectors: one for left and one for right channels.
            __tempBuffer = new ExposedBAOS(SpuAdpcmDecoder.calculatePcmBytesGenerated(1840*2));
            __format = __decoder.getOutputFormat(_iSamplesPerSecond);
        }

        public void setAudioListener(@Nonnull ISectorTimedAudioWriter audioOut) {
            __audioWriter = audioOut;
        }

        public boolean feedSector(@Nonnull IdentifiedSector sector, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            if (!(sector instanceof ISquareAudioSector))
                return false;

            ISquareAudioSector audSector = (ISquareAudioSector) sector;
            if (audSector.getAudioChannel() == 0) {
                __leftAudioSector = audSector;
            } else if (audSector.getAudioChannel() == 1) {
                if (__leftAudioSector == null)
                    throw new IllegalStateException("Received right audio sector before left audio sector.");
                
                __rightAudioSector = audSector;

                if (__leftAudioSector.getAudioDataSize() != __rightAudioSector.getAudioDataSize() ||
                    __leftAudioSector.getSamplesPerSecond() != __rightAudioSector.getSamplesPerSecond())
                    throw new RuntimeException("Left/Right audio does not match.");

                __tempBuffer.reset();
                long lngSamplesWritten = __decoder.getSampleFramesWritten();
                try {
                    int iSize = __decoder.decode(__leftAudioSector.getIdentifiedUserDataStream(),
                                                 __rightAudioSector.getIdentifiedUserDataStream(),
                                                 __rightAudioSector.getAudioDataSize(), __tempBuffer);
                } catch (IOException ex) {
                    throw new RuntimeException("Should never happen", ex);
                }
                if (__decoder.hadCorruption())
                    log.log(Level.WARNING, I.SPU_ADPCM_CORRUPTED(__leftAudioSector.getSectorNumber(), lngSamplesWritten));

                if (__audioWriter == null)
                    throw new IllegalStateException("Must set audio listener before feeding sectors.");
                __audioWriter.write(__format, __tempBuffer.getBuffer(), 0, __tempBuffer.size(), new Fraction(__rightAudioSector.getSectorNumber()));
            } else {
                throw new RuntimeException("Invalid audio channel " + audSector.getAudioChannel());
            }
            return true;
        }

        public double getVolume() {
            return __decoder.getVolume();
        }

        public void setVolume(double dblVolume) {
            __decoder.setVolume(dblVolume);
        }

        public @Nonnull AudioFormat getOutputFormat() {
            return __format;
        }

        public int getSamplesPerSecond() {
            return _iSamplesPerSecond;
        }

        public int getDiscSpeed() {
            return 2;
        }

        public void reset() {
            __decoder.resetContext();
        }

        public int getEndSector() {
            return DiscItemSquareAudioStream.this.getEndSector();
        }

        public int getStartSector() {
            return DiscItemSquareAudioStream.this.getStartSector();
        }

        public int getPresentationStartSector() {
            return DiscItemSquareAudioStream.this.getPresentationStartSector();
        }

        public @Nonnull ILocalizedMessage[] getAudioDetails() {
            return new ILocalizedMessage[] { DiscItemSquareAudioStream.this.getDetails() };
        }
    }

}
