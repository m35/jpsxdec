/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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

package jpsxdec.formats;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.Maths;

/** Wraps {@link AudioInputStream} to read mono or stereo 16-bit
 * {@link AudioFormat.Encoding.PCM_SIGNED} samples as arrays of shorts.
 * The sample rate also must be a whole number.
 * Signed 16-bit little-endian linear PCM audio is the same format used for
 * CD audio. CD audio is played back at 44100 Hz, but this allows for any
 * sample rate. */
public class Signed16bitLittleEndianLinearPcmAudioInputStream implements Closeable {

    private static final Logger LOG = Logger.getLogger(Signed16bitLittleEndianLinearPcmAudioInputStream.class.getName());

    /**
     * Opens an audio file using {@link AudioSystem#getAudioInputStream(java.io.File)}
     * and wraps it with {@link Signed16bitLittleEndianLinearPcmAudioInputStream}.
     * @throws IncompatibleException if the audio format is not signed 16-bit
     *                               little-endian linear PCM with at most 2 channels
     *                               and a whole number sample rate
     */
    public static @Nonnull Signed16bitLittleEndianLinearPcmAudioInputStream getAudioInputStream(@Nonnull File audioFile)
            throws UnsupportedAudioFileException, IOException, IncompatibleException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
        return new Signed16bitLittleEndianLinearPcmAudioInputStream(ais);
    }

    @Nonnull
    private final AudioInputStream _sourceAudio;
    @Nonnull
    private final AudioFormat _audioFormat;
    private final int _iSampleFramesPerSecond;

    private long _lngSampleFramesRead = 0;

    /** Ensures the audio stream is 16-bit little-endian linear PCM with at most 2 channels.
     * @throws IncompatibleException if not
     */
    public Signed16bitLittleEndianLinearPcmAudioInputStream(@Nonnull AudioInputStream sourceAudio)
            throws IncompatibleException
    {
        _audioFormat = sourceAudio.getFormat();

        int iChannels = _audioFormat.getChannels();
        if (iChannels < 1 || iChannels > 2)
            throw new IncompatibleException("Channels must be 1 or 2");

        if (_audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
            throw new IncompatibleException("Audio must be signed PCM");

        if (_audioFormat.isBigEndian())
            throw new IncompatibleException("Audio must be little endian");

        if (_audioFormat.getSampleSizeInBits() != 16)
            throw new IncompatibleException("Audio sample size must be 16-bit");

        if (_audioFormat.getFrameSize() != iChannels * 2)
            throw new IncompatibleException("Audio frame size must be 2 bytes per channel");

        _iSampleFramesPerSecond = Math.round(_audioFormat.getSampleRate());

        if (!Maths.floatEquals(_iSampleFramesPerSecond, _audioFormat.getSampleRate(), 1f))
            throw new IncompatibleException("Sample rate must be a whole number");

        _sourceAudio = sourceAudio;
    }

    /** Closes the underlying {@link AudioInputStream}. */
    @Override
    public void close() throws IOException {
        _sourceAudio.close();
    }

    public boolean isStereo() {
        return _audioFormat.getChannels() != 1;
    }

    public int getSampleFramesPerSecond() {
        return _iSampleFramesPerSecond;
    }

    /** The number of same frames that have been read from the underlying
     * {@link AudioInputStream}. */
    public long getSampleFramesRead() {
        return _lngSampleFramesRead;
    }

    /** Read the requested number of sample frames from the input source stream
     * and convert to 1 (mono) or 2 (stereo) arrays of shorts.
     *
     * @param iSampleFrameCount The number of sample frames to read
     *                          (1 sample frame = 1 short for mono or 2 shorts for stereo).
     *
     * @return 1 or 2 arrays of {@code iSampleFrameCount} shorts.
     *
     * @throws EOFException Out of data in the source audio stream.
     * @throws IOException  Error reading source audio stream.
     */
    public @Nonnull short[][] readSampleFrames(int iSampleFrameCount) throws EOFException, IOException {
        int iChannels = _audioFormat.getChannels();
        byte[] abReadPcmSoundUnitSamples = new byte[_audioFormat.getFrameSize() * iSampleFrameCount];
        IO.readByteArray(_sourceAudio, abReadPcmSoundUnitSamples);
        _lngSampleFramesRead += iSampleFrameCount;

        short[][] aasiPcmSoundUnitChannelSamples = new short[iChannels][iSampleFrameCount];
        for (int iInByte = 0, iOutSample = 0;
             iInByte < abReadPcmSoundUnitSamples.length;
             iOutSample++)
        {
            for (int iChannel = 0; iChannel < iChannels; iChannel++, iInByte+=2) {
                aasiPcmSoundUnitChannelSamples[iChannel][iOutSample] =
                        IO.readSInt16LE(abReadPcmSoundUnitSamples, iInByte);
            }
        }
        return aasiPcmSoundUnitChannelSamples;
    }

    /** Reads a small amount of data from the underlying {@link AudioInputStream}
     * to see if the stream is at the end. Note that this will throw away that data!
     * So make sure you're done with this stream before calling. */
    public boolean nextReadReturnsEndOfStream() {
        try {
            return _sourceAudio.read(new byte[_audioFormat.getFrameSize()]) < 0;
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Checking end of audio stream exception", ex);
            return true;
        }
    }
}
