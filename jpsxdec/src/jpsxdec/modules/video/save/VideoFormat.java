/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2023  Michael Sabin
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

package jpsxdec.modules.video.save;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;


public enum VideoFormat {
    AVI_MJPG(I.VID_AVI_MJPG_DESCRIPTION(), "avi:mjpg") {
        public String getExtension() { return ".avi"; }
        public boolean isVideo() { return true; }
        public int getDecodeQualityCount() { return 0; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { throw new IndexOutOfBoundsException(); }
    },
    AVI_RGB(I.VID_AVI_RGB_DESCRIPTION(), "avi:rgb") {
        public String getExtension() { return ".avi"; }
        public boolean isVideo() { return true; }
    },
    AVI_YUV(I.VID_AVI_YUV_DESCRIPTION(), "avi:yuv") {
        public String getExtension() { return ".avi"; }
        public boolean isVideo() { return true; }
        public int getDecodeQualityCount() { return 1; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return MdecDecodeQuality.HIGH; }
        public boolean mustHaveEvenDims()  { return true; }
    },
    AVI_JYUV(I.VID_AVI_JYUV_DESCRIPTION(), "avi:jyuv") {
        public String getExtension() { return ".avi"; }
        public boolean isVideo() { return true; }
        public int getDecodeQualityCount() { return 1; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return MdecDecodeQuality.HIGH; }
        public boolean mustHaveEvenDims()  { return true; }
    },
    IMGSEQ_PNG(I.VID_IMG_SEQ_PNG_DESCRIPTION(), "png",
               JavaImageFormat.PNG)
    {
        public String getExtension() { return "." + JavaImageFormat.PNG.getExtension(); }
    },
    IMGSEQ_JPG(I.VID_IMG_SEQ_JPG_DESCRIPTION(), "jpg") {
        public String getExtension() { return ".jpg"; }
        public int getDecodeQualityCount() { return 0; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { throw new IndexOutOfBoundsException(); }
    },
    IMGSEQ_BMP(I.VID_IMG_SEQ_BMP_DESCRIPTION(), "bmp",
               JavaImageFormat.BMP)
    {
        public String getExtension() { return "." + JavaImageFormat.BMP.getExtension(); }
    },
    IMGSEQ_TIFF(I.VID_IMG_SEQ_TIFF_DESCRIPTION(), "tiff",
            JavaImageFormat.TIFF)
    {
        public String getExtension() { return "." + JavaImageFormat.TIFF.getExtension(); }
    },
    IMGSEQ_BITSTREAM(I.VID_IMG_SEQ_BS_DESCRIPTION(), "bs") {
        public String getExtension() { return ".bs"; }
        public int getDecodeQualityCount() { return 0; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { throw new IndexOutOfBoundsException(); }
        public boolean isCroppable() { return false; }
        public boolean needsDims() { return true; }
    },
    IMGSEQ_MDEC(I.VID_IMG_SEQ_MDEC_DESCRIPTION(), "mdec") {
        public String getExtension() { return ".mdec"; }
        public int getDecodeQualityCount() { return 0; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { throw new IndexOutOfBoundsException(); }
        public boolean isCroppable() { return false; }
        public boolean needsDims() { return true; }
    },
    ;

    /** How the format will be displayed in the GUI. */
    @Nonnull
    private final ILocalizedMessage _guiName;
    /** How the format will be displayed on the command line. */
    @Nonnull
    private final String _sCmdLineId;
    @CheckForNull
    private final JavaImageFormat _eImgFmt;

    private VideoFormat(@Nonnull ILocalizedMessage description, @Nonnull String sCmdLine) {
        this(description, sCmdLine, null);
    }

    private VideoFormat(@Nonnull ILocalizedMessage description, @Nonnull String sCmdLine,
                        @CheckForNull JavaImageFormat imgFormat)
    {
        _guiName = description;
        _sCmdLineId = sCmdLine;
        _eImgFmt = imgFormat;
    }

    /** {@inheritDoc}
     *<p>
     *  Must be localized because this object is used directly. */
    public String toString() { return _guiName.getLocalizedMessage(); }
    public @Nonnull String getCmdLine() { return _sCmdLineId; }
    public boolean isAvailable() {
        return _eImgFmt == null || _eImgFmt.isAvailable();
    }

    public boolean isCroppable() { return true; }
    public boolean mustHaveEvenDims()  { return false; }

    public int getDecodeQualityCount() { return MdecDecodeQuality.values().length; }
    public @Nonnull MdecDecodeQuality getMdecDecodeQuality(int i) { return MdecDecodeQuality.values()[i]; }

    /** If AVI, it means it can save audio, otherwise it is an image sequence. */
    public boolean isVideo() { return false; }
    public boolean isSequence() { return !isVideo(); }

    public @CheckForNull JavaImageFormat getImgFmt() { return _eImgFmt; }

    /** If the output filename should include frame dimensions. */
    public boolean needsDims() { return false; }
    /** Filename extension with '.'. */
    abstract public @Nonnull String getExtension();

    /////////////////////////////////////////////////////////

    public static @CheckForNull VideoFormat fromCmdLine(@Nonnull String sCmdLine) {
        for (VideoFormat fmt : getAvailable()) {
            if (fmt.getCmdLine().equalsIgnoreCase(sCmdLine))
                return fmt;
        }
        return null;
    }

    public static @Nonnull List<VideoFormat> getAvailable() {
        ArrayList<VideoFormat> available = new ArrayList<VideoFormat>();
        for (VideoFormat fmt : values()) {
            if (fmt.isAvailable())
                available.add(fmt);
        }
        return available;
    }
}
