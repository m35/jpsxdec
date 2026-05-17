/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2026  Michael Sabin
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

package jpsxdec.util.mkvwriter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.FastImageIOwriteToBytes;
import jpsxdec.util.Fraction;

/**
 * Frames stored as JPEG images.
 *
 * Codec ID: V_MJPEG
 *
 * Added in 2024
 * https://github.com/ietf-wg-cellar/matroska-specification/commit/c64e34b622a80cd5e9fa25bc55c4dab86355e305
 *
 * @see IMkvWriter
 */
public class MkvMjpegWriter implements IMkvWriter {

    @Nonnull
    private final MkvVideoFormat _videoFormat;
    @CheckForNull
    private FastImageIOwriteToBytes _img2Bytes;

    @Nonnull
    private final BasicVideoAudioMuxer _muxer;

    private static @Nonnull MkvVideoFormat makeVf(int iWidth, int iHeight, @CheckForNull Fraction constantFramesPerSecond) {
        return new MkvVideoFormat(iWidth, iHeight, constantFramesPerSecond, "V_MJPEG", null, true, null);
    }

    /**
     * Creates an mkv file without audio.
     * @see #MkvMjpegWriter(File, int, int, Fraction)
     * */
    public MkvMjpegWriter(@Nonnull File mkvFile, int iWidth, int iHeight,
                          @CheckForNull Fraction constantFramesPerSecond)
            throws IOException
    {
        this(mkvFile, makeVf(iWidth, iHeight, constantFramesPerSecond), null);
    }

    /**
     * @param constantFramesPerSecond If this is provided, the mkv file will report it has
     *                                a constant frame rate, and all frame timestamps MUST
     *                                be multiples of this value.
     *                                If null, the mkv file will report that it has a
     *                                variable frame rate
     */
    public MkvMjpegWriter(@Nonnull File mkvFile, int iWidth, int iHeight,
                          @CheckForNull Fraction constantFramesPerSecond,
                          boolean blnStereo, int iSampleRate)
            throws FileNotFoundException, IOException
    {
        this(mkvFile, makeVf(iWidth, iHeight, constantFramesPerSecond),
             new MkvAudioFormat(blnStereo, iSampleRate));
    }

    private MkvMjpegWriter(@Nonnull File mkvFile,
                           @Nonnull MkvVideoFormat videoFormat,
                           @CheckForNull MkvAudioFormat audioFormat)
            throws FileNotFoundException, IOException
    {
        _videoFormat = videoFormat;

        BasicMkvWriter mkvWriter = new BasicMkvWriter(mkvFile, videoFormat, audioFormat);
        _muxer = new BasicVideoAudioMuxer(mkvWriter, videoFormat, audioFormat);
    }

    @Override
    public void writeImage(@Nonnull BufferedImage bi, @Nonnull Fraction timestampInSeconds) throws IOException {
        _videoFormat.assertDimensionsMatch(bi);

        if (_img2Bytes == null)
            _img2Bytes = new FastImageIOwriteToBytes("jpeg");

        FastImageIOwriteToBytes.ensureOpaque(bi); // remove any alpha channel first to avoid jpg with alpha error (exists in some jdk implementations)
        byte[] abJpeg = _img2Bytes.toBytes(bi);

        writeJpegBytes(abJpeg, timestampInSeconds);
    }

    public void writeJpegBytes(@Nonnull byte[] abJpeg, @Nonnull Fraction timestamp) throws IOException {
        _muxer.addFrame(abJpeg, timestamp);
    }

    @Override
    public void writeAudio(@Nonnull byte[] abAudio) throws IOException {
        _muxer.addAudio(abAudio, getAudioTime());
    }

    @Override
    public void writeAudio(@Nonnull byte[] abAudio, @Nonnull Fraction timestampInSeconds) throws IOException {
        _muxer.addAudio(abAudio, timestampInSeconds);
    }

    @Override
    public void writeAudioSilence(int iSampleFrameCount, @Nonnull Fraction timestampInSeconds) throws IOException {
        _muxer.addSilence(iSampleFrameCount, timestampInSeconds);
    }

    @Override
    public @Nonnull Fraction getAudioTime() {
        return _muxer.getAudioTime();
    }

    @Override
    public void close() throws IOException {
        _muxer.close();
    }

}
