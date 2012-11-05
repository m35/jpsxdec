/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012  Michael Sabin
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

import java.util.ArrayList;
import java.util.List;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.formats.JavaImageFormat;


public enum VideoFormat {
    AVI_MJPG    ("AVI: Compressed (MJPG)"  , "avi:mjpg", JavaImageFormat.JPG) {
        public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
            return ".avi";
        }
        public boolean canSaveAudio() { return true; }
    },
    AVI_RGB     ("AVI: Uncompressed RGB"   , "avi:rgb") {
        public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
            return ".avi";
        }
        public boolean canSaveAudio() { return true; }
    },
    AVI_YUV     ("AVI: YUV"                , "avi:yuv") {
        public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
            return ".avi";
        }
        public boolean canSaveAudio() { return true; }
        public int getDecodeQualityCount() { return 1; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return MdecDecodeQuality.HIGH; }
    },
    AVI_JYUV    ("AVI: YUV with [0-255] range", "avi:jyuv") {
        public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
            return ".avi";
        }
        public boolean canSaveAudio() { return true; }
        public int getDecodeQualityCount() { return 1; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return MdecDecodeQuality.HIGH; }
    },
    IMGSEQ_PNG  ("Image sequence: png"     , "png", JavaImageFormat.PNG) {
        public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
            return digitBlk(vid, iFrame) + ".png";
        }
    },
    IMGSEQ_JPG  ("Image sequence: jpg"     , "jpg", JavaImageFormat.JPG) {
        public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
            return digitBlk(vid, iFrame) + ".jpg";
        }
    },
    IMGSEQ_BMP  ("Image sequence: bmp"     , "bmp", JavaImageFormat.BMP) {
        public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
            return digitBlk(vid, iFrame) + ".bmp";
        }
    },
    IMGSEQ_DEMUX("Image sequence: bitstream"   , "bs") {
        public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
            return "_" + vid.getWidth() + "x" + vid.getHeight() + digitBlk(vid, iFrame) + ".bs";
        }
        public int getDecodeQualityCount() { return 0; }
        public MdecDecodeQuality getMdecDecodeQuality(int i) { return null; }
        public boolean isCroppable() { return false; }
    },
    IMGSEQ_MDEC ("Image sequence: mdec"    , "mdec") {
        public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
            return "_" + vid.getWidth() + "x" + vid.getHeight() + digitBlk(vid, iFrame) + ".mdec";
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

    public boolean canSaveAudio() { return false; }

    public JavaImageFormat getImgFmt() { return _eImgFmt; }

    public boolean hasCompression() {
        return _eImgFmt == null ? false : _eImgFmt.hasCompression();
    }

    abstract public String formatPostfix(DiscItemVideoStream vid, int iStartFrame);

    /////////////////////////////////////////////////////////

    private static String digitBlk(DiscItemVideoStream vid, int iFrame) {
        int iDigitCount = String.valueOf(vid.getEndFrame()).length();
        return String.format("[%0" + String.valueOf(iDigitCount) + "d]", iFrame);
    }

    public static VideoFormat fromCmdLine(String sCmdLine) {
        for (VideoFormat fmt : values()) {
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
