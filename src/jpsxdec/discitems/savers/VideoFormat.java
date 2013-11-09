/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2013  Michael Sabin
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

package jpsxdec.discitems.savers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.formats.JavaImageFormat;


public enum VideoFormat {
    AVI_MJPG    ("AVI: Compressed (MJPG)"  , "avi:mjpg") {
        public String makePostfixFormat(int iWidth, int iHeight) {
            return ".avi";
        }
        public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame) {
            return ".avi";
        }
        public boolean isAvi() { return true; }
        public int getDecodeQualityCount() { return 0; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return null; }
    },
    AVI_RGB     ("AVI: Uncompressed RGB"   , "avi:rgb") {
        public String makePostfixFormat(int iWidth, int iHeight) {
            return ".avi";
        }
        public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame) {
            return ".avi";
        }
        public boolean isAvi() { return true; }
    },
    AVI_YUV     ("AVI: YUV"                , "avi:yuv") {
        public String makePostfixFormat(int iWidth, int iHeight) {
            return ".avi";
        }
        public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame) {
            return ".avi";
        }
        public boolean isAvi() { return true; }
        public int getDecodeQualityCount() { return 1; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return MdecDecodeQuality.HIGH_PLUS; }
    },
    AVI_JYUV    ("AVI: YUV with [0-255] range", "avi:jyuv") {
        public String makePostfixFormat(int iWidth, int iHeight) {
            return ".avi";
        }
        public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame) {
            return ".avi";
        }
        public boolean isAvi() { return true; }
        public int getDecodeQualityCount() { return 1; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return MdecDecodeQuality.HIGH_PLUS; }
    },
    IMGSEQ_PNG  ("Image sequence: png"     , "png", JavaImageFormat.PNG) {
        public String makePostfixFormat(int iWidth, int iHeight) {
            return "." + getImgFmt().getExtension();
        }
        public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame) {
            return frameFormat(iEndFrame) + "." + getImgFmt().getExtension();
        }
    },
    IMGSEQ_JPG  ("Image sequence: jpg"     , "jpg") {
        public String makePostfixFormat(int iWidth, int iHeight) {
            return ".jpg";
        }
        public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame) {
            return frameFormat(iEndFrame) + ".jpg";
        }
        public int getDecodeQualityCount() { return 0; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return null; }
    },
    IMGSEQ_BMP  ("Image sequence: bmp"     , "bmp", JavaImageFormat.BMP) {
        public String makePostfixFormat(int iWidth, int iHeight) {
            return "." + getImgFmt().getExtension();
        }
        public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame) {
            return frameFormat(iEndFrame) + "." + getImgFmt().getExtension();
        }
    },
    IMGSEQ_BITSTREAM("Image sequence: bitstream"   , "bs") {
        public String makePostfixFormat(int iWidth, int iHeight) {
            return "_" + iWidth + "x" + iHeight + ".bs";
        }
        public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame) {
            return "_" + iWidth + "x" + iHeight + frameFormat(iEndFrame) + ".bs";
        }
        public int getDecodeQualityCount() { return 0; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return null; }
        public boolean isCroppable() { return false; }
    },
    IMGSEQ_MDEC ("Image sequence: mdec"    , "mdec") {
        public String makePostfixFormat(int iWidth, int iHeight) {
            return "_" + iWidth + "x" + iHeight + ".mdec";
        }
        public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame) {
            return "_" + iWidth + "x" + iHeight + frameFormat(iEndFrame) + ".mdec";
        }
        public int getDecodeQualityCount() { return 0; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return null; }
        public boolean isCroppable() { return false; }
    },
    ;

    private final String _sGui;
    private final String _sCmdLine;
    private final JavaImageFormat _eImgFmt;

    VideoFormat(String sGui, String sCmdLine) {
        this(sGui, sCmdLine, null);
    }

    VideoFormat(String sGui, String sCmdLine, JavaImageFormat imgFormat) {
        _sGui = sGui;
        _sCmdLine = sCmdLine;
        _eImgFmt = imgFormat;
    }

    public String toString() { return _sGui; }
    public String getCmdLine() { return _sCmdLine; }
    public boolean isAvailable() {
        return _eImgFmt == null ? true : _eImgFmt.isAvailable();
    }

    public boolean isCroppable() { return true; }

    public int getDecodeQualityCount() { return MdecDecodeQuality.values().length; }
    public MdecDecodeQuality getMdecDecodeQuality(int i) { return MdecDecodeQuality.values()[i]; }

    /** If AVI, it means it can save audio, otherwise it is an image sequence. */
    public boolean isAvi() { return false; }

    public JavaImageFormat getImgFmt() { return _eImgFmt; }

    final public String makeFormat(DiscItemVideoStream vid) {
        File baseName = vid.getSuggestedBaseName();
        String sName = baseName.getName().replace("%", "%%") +
                       makePostfixFormat(vid.getWidth(), vid.getHeight(), vid.getEndFrame());
        String sBaseNameParent = baseName.getParent();
        if (sBaseNameParent == null)
            return sName;
        else
            return new File(baseName.getParent().replace("%", "%%"), sName).toString();
    }
    abstract public String makePostfixFormat(int iWidth, int iHeight);
    abstract public String makePostfixFormat(int iWidth, int iHeight, int iEndFrame);

    /////////////////////////////////////////////////////////

    private static String frameFormat(int iEndFrame) {
        int iDigitCount = String.valueOf(iEndFrame).length();
        return "[%0" + String.valueOf(iDigitCount) + "d]";
    }

    public static VideoFormat fromCmdLine(String sCmdLine) {
        for (VideoFormat fmt : getAvailable()) {
            if (fmt.getCmdLine().equalsIgnoreCase(sCmdLine))
                return fmt;
        }
        return null;
    }

    public static String getCmdLineList() {
        StringBuilder sb = new StringBuilder();
        for (VideoFormat fmt : values()) {
            if (fmt.isAvailable()) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(fmt.getCmdLine());
            }
        }
        return sb.toString();
    }

    public static List<VideoFormat> getAvailable() {
        ArrayList<VideoFormat> avalable = new ArrayList<VideoFormat>();
        for (VideoFormat fmt : values()) {
            if (fmt.isAvailable())
                avalable.add(fmt);
        }
        return avalable;
    }
}
