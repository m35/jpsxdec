/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2015  Michael Sabin
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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedMessage;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate;
import jpsxdec.psxvideo.mdec.MdecDecoder_int;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_double;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_int;
import jpsxdec.psxvideo.mdec.idct.SimpleIDCT;


public enum MdecDecodeQuality {
    LOW(I.QUALITY_FAST_DESCRIPTION(), I.QUALITY_FAST_COMMAND()) {
        public MdecDecoder makeDecoder(int iWidth, int iHeight) {
            return new MdecDecoder_int(new SimpleIDCT(), iWidth, iHeight);
        }
    },
    HIGH_PLUS(I.QUALITY_HIGH_DESCRIPTION(), I.QUALITY_HIGH_COMMAND()) {
        public MdecDecoder makeDecoder(int iWidth, int iHeight) {
            return new MdecDecoder_double_interpolate(new PsxMdecIDCT_double(), iWidth, iHeight);
        }
        public boolean canUpsample() { return true; }
    },
    PSX(I.QUALITY_PSX_DESCRIPTION(), I.QUALITY_PSX_COMMAND()) {
        public MdecDecoder makeDecoder(int iWidth, int iHeight) {
            return new MdecDecoder_int(new PsxMdecIDCT_int(), iWidth, iHeight);
        }
    };

    public boolean canUpsample() { return false; }

    public static @CheckForNull MdecDecodeQuality fromCmdLine(@Nonnull String sCmdLine) {
        for (MdecDecodeQuality dq : MdecDecodeQuality.values()) {
            if (dq.getCmdLine().equalsIgnoreCase(sCmdLine))
                return dq;
        }
        return null;
    }

    @Nonnull
    private final LocalizedMessage _guiDescription;
    @Nonnull
    private final LocalizedMessage _cmdLine;

    private MdecDecodeQuality(@Nonnull LocalizedMessage description, @Nonnull LocalizedMessage cmdLine) {
        _guiDescription = description;
        _cmdLine = cmdLine;
    }

    abstract public @Nonnull MdecDecoder makeDecoder(int iWidth, int iHeight);

    public @Nonnull LocalizedMessage getCmdLine() { return _cmdLine; }
    public String toString() { return _guiDescription.getLocalizedMessage(); }
}
