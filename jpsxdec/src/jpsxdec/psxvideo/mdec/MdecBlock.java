/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.psxvideo.mdec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;


/** Macro-block sub blocks, listed in the order accepted by the MDEC chip. */
public enum MdecBlock {
    Cr(true),
    Cb(true),
    Y1(false),
    Y2(false),
    Y3(false),
    Y4(false);

    private final boolean _blnIsChroma;
    private MdecBlock _nextBlock;

    private MdecBlock(boolean blnIsChroma) {
        _blnIsChroma = blnIsChroma;
    }

    public boolean isChroma() {
        return _blnIsChroma;
    }

    public boolean isLuma() {
        return !_blnIsChroma;
    }

    /** Never returns null, loops from the last block to the first. */
    public @Nonnull MdecBlock next() {
        return _nextBlock;
    }

    // -------------------------------------------------------------------------

    private static final List<MdecBlock> VALUES =
            Collections.unmodifiableList(Arrays.asList(values()));

    static {
        Cr._nextBlock = Cb;
        Cb._nextBlock = Y1;
        Y1._nextBlock = Y2;
        Y2._nextBlock = Y3;
        Y3._nextBlock = Y4;
        Y4._nextBlock = Cr;
    }

    public static @Nonnull List<MdecBlock> list() {
        return VALUES;
    }

    public static int count() {
        return VALUES.size();
    }

    public static @Nonnull MdecBlock first() {
        return Cr;
    }
}
