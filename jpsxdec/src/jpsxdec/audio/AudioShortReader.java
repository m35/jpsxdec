/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2017  Michael Sabin
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

package jpsxdec.audio;

import java.io.Closeable;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.util.IO;

/** Wraps {@link AudioInputStream} to read mono or stereo 16-bit
 * {@link AudioFormat.Encoding.PCM_SIGNED} samples as arrays of shorts. */
public class AudioShortReader implements Closeable {
    
    @Nonnull
    private final AudioInputStream _sourceAudio;

    /** If source stream is ended. */
    private boolean _blnIsEof = false;

    /** @throws UnsupportedOperationException if the audio format is not valid. */
    public AudioShortReader(@Nonnull AudioInputStream sourceAudio) {
        AudioFormat fmt = sourceAudio.getFormat();
        int iChannels = fmt.getChannels();
        if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
            throw new UnsupportedOperationException("Encoding must be PCM_SIGNED");
        if (fmt.getSampleSizeInBits() != 16)
            throw new UnsupportedOperationException("Bit size must be 16 " + fmt.getSampleRate());
        if (iChannels < 1 || iChannels > 2)
            throw new UnsupportedOperationException("Channels must be 1 or 2");

        _sourceAudio = sourceAudio;
    }

    /** If source stream is ended. */
    public boolean isEof() {
        return _blnIsEof;
    }

    /** Closes the underlying audio stream. */
    public void close() throws IOException {
        _sourceAudio.close();
    }

    /** Read the requested number of sample frames from the input source stream
     * and convert to 1 (mono) or 2 (stereo) arrays of shorts.
     * If end of stream is reached, fills remaining
     * samples with 0 and {@link #isEof()} will return true.
     *
     * @param iSampleFrameCount The number of sample frames to read
     *                          (1 sample frame = 1 short for mono or 2 shorts for stereo).
     *
     * @return 1 or 2 arrays of {@code iSampleFrameCount} shorts. */
    public @Nonnull short[][] readSoundUnitSamples(int iSampleFrameCount) throws IOException {
        int iChannels = _sourceAudio.getFormat().getChannels();
        byte[] abReadPcmSoundUnitSamples = new byte[iSampleFrameCount*2*iChannels];
        short[][] aasiPcmSoundUnitChannelSamples = new short[iChannels][iSampleFrameCount];

        int iStart = 0;
        int iLen = abReadPcmSoundUnitSamples.length;
        while (iLen > 0) {
            int iRead = _sourceAudio.read(abReadPcmSoundUnitSamples, iStart, iLen);
            if (iRead < 0) {
                _blnIsEof = true;
                break;
            }
            iLen -= iRead;
        }

        boolean blnIsBigEndian = _sourceAudio.getFormat().isBigEndian();
        for (int iInByte = 0, iOutSample = 0;
             iInByte < abReadPcmSoundUnitSamples.length;
             iOutSample++)
        {
            for (int iChannel = 0; iChannel < iChannels; iChannel++, iInByte+=2) {
                aasiPcmSoundUnitChannelSamples[iChannel][iOutSample] =
                        blnIsBigEndian ?
                        IO.readSInt16BE(abReadPcmSoundUnitSamples, iInByte) :
                        IO.readSInt16LE(abReadPcmSoundUnitSamples, iInByte);
            }
        }
        return aasiPcmSoundUnitChannelSamples;
    }

}
