/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2023  Michael Sabin
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

package jpsxdec.adpcm;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.formats.Signed16bitLittleEndianLinearPcmAudioInputStream;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;

/** Encodes audio data into the PlayStation/CD-i/Green Book ADPCM format,
 *  one sector at a time.
 * <p>
 * The input is an audio stream at a matching quality
 * (16 bits/sample at 37800 Hz or 18900 Hz in mono or stereo).
 * The output is the "user data" for a Mode 2 Form 2 real-time XA audio sector.
 * more source data, silence is encoded. Check for EOF condition with. */
public class XaAdpcmEncoder implements Closeable {

    /** Source audio stream. */
    @Nonnull
    private final Signed16bitLittleEndianLinearPcmAudioInputStream _audioShortReader;

    /** Sample rate of source/output stream. Must be 18900 or 37800 Hz */
    private final int _iSampleFramesPerSecond;
    /** true=encode to 4 bits/sample, false=encode to 8 bits/sample. */
    private final boolean _blnEncode4BitsElse8Bits;

    /** Running ADPCM encoders(s), an encoder for each channel (1 or 2). */
    @Nonnull
    private final SoundUnitEncoder[] _aoEncoders;

    /** Keeps track of the state of the decoding process so loggers can
     * more clearly report the state. */
    private final LogContext _logContext = new LogContext();

    public long getSampleFramesRead() {
        return _audioShortReader.getSampleFramesRead();
    }

    public static class LogContext implements IContextCopier {
        /** The number of sectors that have been encoded. */
        public int iEncodedSectorCount = 0;
        /** The sound group being encoded. */
        public int iSoundGroup = -1;
        /** The sound unit being encoded. */
        public int iSoundUnit = -1;
        /** Left/right audio channel being encoded (0 or 1). */
        public int iChannel = -1;
        /** The number of PCM sample frames that have been read
         * Used to help find where in the input stream data was being encoded. */
        public long lngSamplesFramesRead = 0;

        private LogContext() {}

        @Override
        public @Nonnull LogContext copy() {
            LogContext cpy = new LogContext();
            cpy.iEncodedSectorCount = iEncodedSectorCount;
            cpy.iSoundGroup = iSoundGroup;
            cpy.iSoundUnit = iSoundUnit;
            cpy.iChannel = iChannel;
            cpy.lngSamplesFramesRead = lngSamplesFramesRead;
            return cpy;
        }

        @Override
        public String toString() {
            return String.format(
                    "Sector %d Sound Group.Unit %d.%d Channel %d after Sample Frame %d",
                    iEncodedSectorCount, iSoundGroup, iSoundUnit, iChannel, lngSamplesFramesRead);
        }
    }

    /** Manually provide the filter and range parameters for every Sound Unit
     *  via a stream of bytes. Set by {@link #setPresetParameters(java.io.InputStream)}.
     *  Primarily for development/testing purposes. */
    @CheckForNull
    private InputStream _presetParameters = null;

    /** Create a new encoder for the given audio stream, to be encoded with the
     * given bits.
     * @param ais Must be 16 bits/sample at 37800 Hz or 18900 Hz in mono or stereo.
     * @param iEncodeToAdpcmBitsPerSample Must be 4 or 8
     * @throws UnsupportedOperationException if source audio format is incorrect.
     */
    public XaAdpcmEncoder(@Nonnull Signed16bitLittleEndianLinearPcmAudioInputStream ais, int iEncodeToAdpcmBitsPerSample)
            throws IncompatibleException
    {
        if (iEncodeToAdpcmBitsPerSample == 4)
            _blnEncode4BitsElse8Bits = true;
        else if (iEncodeToAdpcmBitsPerSample == 8)
            _blnEncode4BitsElse8Bits = false;
        else // programmer error
            throw new IllegalArgumentException("Invalid encoding bits/sample "+iEncodeToAdpcmBitsPerSample+", must be 4 or 8");

        if (ais.getSampleFramesPerSecond() == 37800)
            _iSampleFramesPerSecond = 37800;
        else if (ais.getSampleFramesPerSecond() == 18900)
            _iSampleFramesPerSecond = 18900;
        else
            throw new IncompatibleException("Unsupported sample rate "+ais.getSampleFramesPerSecond()+", must be 18900 or 37800");

        _audioShortReader = ais;

        int iChannels = ais.isStereo() ? 2 : 1;
        _aoEncoders = new SoundUnitEncoder[iChannels];
        for (int i = 0; i < iChannels; i++) {
            _aoEncoders[i] = new SoundUnitEncoder(iEncodeToAdpcmBitsPerSample,
                                                  K0K1Filter.XA);
        }
    }

    public boolean isStereo() {
        return _aoEncoders.length == 2;
    }

    public int getSampleFramesPerSecond() {
        return _iSampleFramesPerSecond;
    }

    @Override
    public void close() throws IOException {
        _audioShortReader.close();
    }

    /** Manually provide the filter and range parameters for every Sound Unit
     *  via a stream of bytes. Primarily for development/testing purposes. */
    public void setPresetParameters(@CheckForNull InputStream presetParameters) {
        _presetParameters = presetParameters;
    }

    /** From the source audio stream, reads either 4032 sample frames
     * (for 4 bits/sample), or 2016 sample frames (for 8 bits/sample).
     * And returns 2304 bytes. Note that the
     * last 20 bytes of the sector are not included in the output.
     * @throws EOFException if the underlying audio input stream is exhausted
     * @throws IOException if there is an error reading from the underlying audio input stream
     */
    public @Nonnull byte[] encode1Sector() throws EOFException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2304);
        for (_logContext.iSoundGroup = 0;
             _logContext.iSoundGroup < XaAdpcmDecoder.ADPCM_SOUND_GROUPS_PER_SECTOR;
             _logContext.iSoundGroup++)
        {
            encodeSoundGroup(baos);
        }
        _logContext.iSoundGroup = -1;
        _logContext.iEncodedSectorCount++;

        return baos.toByteArray();
    }

    private void encodeSoundGroup(@Nonnull ByteArrayOutputStream os) throws EOFException, IOException {
        SoundUnitEncoder.EncodedUnit[] aoEncoded;
        if (_blnEncode4BitsElse8Bits)
            aoEncoded = new SoundUnitEncoder.EncodedUnit[XaAdpcmDecoder.SOUND_UNITS_IN_4_BIT_SOUND_GROUP];
        else
            aoEncoded = new SoundUnitEncoder.EncodedUnit[XaAdpcmDecoder.SOUND_UNITS_IN_8_BIT_SOUND_GROUP];

        for (_logContext.iSoundUnit = 0; _logContext.iSoundUnit < aoEncoded.length;) {
            short[][] aasiPcmSoundUnitChannelSamples =
                    _audioShortReader.readSampleFrames(SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT);

            for (_logContext.iChannel = 0;
                 _logContext.iChannel < _aoEncoders.length;
                 _logContext.iChannel++, _logContext.iSoundUnit++)
            {
                if (_presetParameters == null) {
                    aoEncoded[_logContext.iSoundUnit] = _aoEncoders[_logContext.iChannel].encodeSoundUnit(
                                aasiPcmSoundUnitChannelSamples[_logContext.iChannel], _logContext);
                } else {
                    int iParameter;
                    try {
                        iParameter = IO.readUInt8(_presetParameters);
                    } catch (IOException ex) {
                        throw new RuntimeException("Error reading encoding parameter", ex);
                    }
                    aoEncoded[_logContext.iSoundUnit] = _aoEncoders[_logContext.iChannel].encodeSoundUnit(
                                aasiPcmSoundUnitChannelSamples[_logContext.iChannel],
                                iParameter, _logContext);
                }
            }
            _logContext.iChannel = -1;
            _logContext.lngSamplesFramesRead += SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
        }
        _logContext.iSoundUnit = -1;

        // write the sound parameters and the encoded samples
        if (_blnEncode4BitsElse8Bits) {
            // aoEncoded.length == AdpcmSoundGroup.SOUND_UNITS_IN_4_BIT_SOUND_GROUP == 8

            // 0,1,2,3, 0,1,2,3, 4,5,6,7, 4,5,6,7
            for (int i = 0; i < 4; i++) {
                os.write(aoEncoded[i].getSoundParameter());
            }
            for (int i = 0; i < 4; i++) {
                os.write(aoEncoded[i].getSoundParameter());
            }
            for (int i = 4; i < 8; i++) {
                os.write(aoEncoded[i].getSoundParameter());
            }
            for (int i = 4; i < 8; i++) {
                os.write(aoEncoded[i].getSoundParameter());
            }

            // 1/0,3/2,5/4,7/6, 1/0,3/2,5/4,7/6, ...
            for (int i = 0; i < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT; i++) {
                writeInt4x2(os, aoEncoded[1].getByteOrNibble(i), aoEncoded[0].getByteOrNibble(i));
                writeInt4x2(os, aoEncoded[3].getByteOrNibble(i), aoEncoded[2].getByteOrNibble(i));
                writeInt4x2(os, aoEncoded[5].getByteOrNibble(i), aoEncoded[4].getByteOrNibble(i));
                writeInt4x2(os, aoEncoded[7].getByteOrNibble(i), aoEncoded[6].getByteOrNibble(i));
            }
        } else {
            // aoEncoded.length == AdpcmSoundGroup.SOUND_UNITS_IN_8_BIT_SOUND_GROUP == 4

            // 0,1,2,3, 0,1,2,3, 0,1,2,3, 0,1,2,3
            for (int iRepeat = 0; iRepeat < 4; iRepeat++) {
                for (int i = 0; i < 4; i++) {
                    os.write(aoEncoded[i].getSoundParameter());
                }
            }

            // 0,1,2,3, 0,1,2,3, 0,1,2,3 ...
            for (int i = 0; i < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT; i++) {
                os.write(aoEncoded[0].getByteOrNibble(i));
                os.write(aoEncoded[1].getByteOrNibble(i));
                os.write(aoEncoded[2].getByteOrNibble(i));
                os.write(aoEncoded[3].getByteOrNibble(i));
            }

        }
    }

    private static void writeInt4x2(@Nonnull ByteArrayOutputStream stream, byte bTop4bits, byte bBottom4bits) {
        stream.write(((bTop4bits&0xf)<<4) | (bBottom4bits&0xf));
    }

}
