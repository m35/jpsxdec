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

import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate;
import jpsxdec.psxvideo.mdec.MdecDecoder_int;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_double;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_int;
import jpsxdec.psxvideo.mdec.idct.simple_idct;


public enum MdecDecodeQuality {
    LOW("Fast (lower quality)", "low") {
        public MdecDecoder makeDecoder(int iWidth, int iHeight) {
            return new MdecDecoder_int(new simple_idct(), iWidth, iHeight);
        }
    },
    HIGH("High quality (slower)", "high") {
        public MdecDecoder makeDecoder(int iWidth, int iHeight) {
            return new MdecDecoder_double(new PsxMdecIDCT_double(), iWidth, iHeight);
        }
    },
    HIGH_PLUS("High quality + interpolation (slowest)", "high+") {
        public MdecDecoder makeDecoder(int iWidth, int iHeight) {
            return new MdecDecoder_double_interpolate(new PsxMdecIDCT_double(), iWidth, iHeight);
        }
        public boolean canUpsample() { return true; }
    },
    PSX("Emulate PSX (low) quality", "psx") {
        public MdecDecoder makeDecoder(int iWidth, int iHeight) {
            return new MdecDecoder_int(new PsxMdecIDCT_int(), iWidth, iHeight);
        }
    };

    public boolean canUpsample() { return false; }

    public static String getCmdLineList() {
        StringBuilder sb = new StringBuilder();
        for (MdecDecodeQuality dq : MdecDecodeQuality.values()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(dq.getCmdLine());
        }
        return sb.toString();
    }

    public static MdecDecodeQuality fromCmdLine(String sCmdLine) {
        for (MdecDecodeQuality dq : MdecDecodeQuality.values()) {
            if (dq.getCmdLine().equals(sCmdLine))
                return dq;
        }
        return null;
    }

    private final String _sGui;
    private final String _sCmdLine;

    private MdecDecodeQuality(String sDescription, String sCmdLine) {
        _sGui = sDescription;
        _sCmdLine = sCmdLine;
    }

    abstract public MdecDecoder makeDecoder(int iWidth, int iHeight);

    public String getCmdLine() { return _sCmdLine; }
    public String toString() { return _sGui; }
}
