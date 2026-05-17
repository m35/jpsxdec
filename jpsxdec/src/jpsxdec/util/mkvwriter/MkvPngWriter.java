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
import jpsxdec.util.aviwriter.AVIstruct;
import jpsxdec.util.aviwriter.BITMAPINFOHEADER;

/**
 * Frames encoded as PNG images.
 *
 * Codec ID "V_MS/VFW/FOURCC" (with FOURCC "png ")
 *
 * Codec Name: Microsoft (TM) Video Codec Manager (VCM)
 *
 * Description: The private data contains the VCM structure {@code BITMAPINFOHEADER}
 * including the extra private bytes, as defined by Microsoft. The data are stored in
 * little-endian format (like on IA32 machines)...
 *
 * Initialization: Private Data contains the VCM structure {@code BITMAPINFOHEADER}
 * including the extra private bytes, as defined by Microsoft in
 * https://msdn.microsoft.com/en-us/library/windows/desktop/dd183376(v=vs.85).aspx.
 *
 * https://www.matroska.org/technical/codec_specs.html#v_msvfwfourcc
 *
 * @see IMkvWriter
 */
public class MkvPngWriter implements IMkvWriter {

    @Nonnull
    private final FastImageIOwriteToBytes _img2PngBytes;
    @Nonnull
    private final MkvVideoFormat _videoFormat;
    @Nonnull
    private final BasicVideoAudioMuxer _muxer;

    private static @Nonnull MkvVideoFormat makeVf(int iWidth, int iHeight, @CheckForNull Fraction constantFramesPerSecond) {
        BITMAPINFOHEADER bih = new BITMAPINFOHEADER();
        bih.biWidth = iWidth;
        bih.biHeight = iHeight;
        bih.biBitCount = 24;
        bih.biCompression = AVIstruct.string2int("png ");
        bih.biSizeImage = iWidth * iHeight * 3; // this is the value ffmpeg uses

        return new MkvVideoFormat(iWidth, iHeight, constantFramesPerSecond, "V_MS/VFW/FOURCC", null, false, bih.toBytes());
    }

    /**
     * Creates an mkv file without audio.
     * @see #MkvPngWriter(File, int, int, Fraction)
     * */
    public MkvPngWriter(@Nonnull File mkvFile, int iWidth, int iHeight,
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
    public MkvPngWriter(@Nonnull File mkvFile, int iWidth, int iHeight,
                        @CheckForNull Fraction constantFramesPerSecond,
                        boolean blnStereo, int iSampleRate)
            throws FileNotFoundException, IOException
    {
        this(mkvFile, makeVf(iWidth, iHeight, constantFramesPerSecond),
             new MkvAudioFormat(blnStereo, iSampleRate));
    }

    private MkvPngWriter(@Nonnull File mkvFile,
                         @Nonnull MkvVideoFormat videoFormat,
                         @CheckForNull MkvAudioFormat audioFormat)
            throws FileNotFoundException, IOException
    {
        _img2PngBytes = new FastImageIOwriteToBytes("png");
        _videoFormat = videoFormat;

        BasicMkvWriter mkvWriter = new BasicMkvWriter(mkvFile, videoFormat, audioFormat);
        _muxer = new BasicVideoAudioMuxer(mkvWriter, videoFormat, audioFormat);
    }

    @Override
    public void writeImage(@Nonnull BufferedImage bi, @Nonnull Fraction timestampInSeconds) throws IOException {
        _videoFormat.assertDimensionsMatch(bi);

        FastImageIOwriteToBytes.ensureOpaque(bi); // TODO remove any alpha channel first?

        byte[] abPng = _img2PngBytes.toBytes(bi);
        _muxer.addFrame(abPng, timestampInSeconds);
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
