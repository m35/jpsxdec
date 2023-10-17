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

package jpsxdec.psxvideo.bitstreams;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.mdec.MdecCode;

/** Assigns value to a {@link BitStreamCode}. The value can have a
 * zero-run-length and AC coefficient (i.e. {@link MdecCode})
 * and/or the escape code or end-of-block code. */
public class ZeroRunLengthAc {

    @Nonnull
    private final BitStreamCode _bitStreamCode;

    @CheckForNull
    private final MdecCode _mdecCode;

    private final boolean _blnIsEscapeCode;
    private final boolean _blnIsEndOfBlock;

    /** If this is a normal AC code with MDEC code equivalent. */
    public ZeroRunLengthAc(@Nonnull BitStreamCode bitStreamCode, int iZeroRunLength, int iAcCoefficient) {
        _bitStreamCode = bitStreamCode;
        _mdecCode = new MdecCode(iZeroRunLength, iAcCoefficient);
        _blnIsEscapeCode = false;
        _blnIsEndOfBlock = false;
    }

    /** If this is a special AC code without an MDEC equivalent. */
    public ZeroRunLengthAc(@Nonnull BitStreamCode bitStreamCode, boolean blnIsEscapeCode, boolean blnIsEndOfBlock) {
        this(bitStreamCode, null, blnIsEscapeCode, blnIsEndOfBlock);
    }

    /** If this is a special AC code with an MDEC equivalent. */
    public ZeroRunLengthAc(@Nonnull BitStreamCode bitStreamCode,
                           int iZeroRunLength, int iAcCoefficient,
                           boolean blnIsEscapeCode, boolean blnIsEndOfBlock)
    {
        this(bitStreamCode, new MdecCode(iZeroRunLength, iAcCoefficient), blnIsEscapeCode, blnIsEndOfBlock);
    }

    /** If this is a special AC code with an MDEC equivalent. */
    private ZeroRunLengthAc(@Nonnull BitStreamCode bitStreamCode,
                            @CheckForNull MdecCode mdecCode,
                            boolean blnIsEscapeCode, boolean blnIsEndOfBlock)
    {
        if (blnIsEscapeCode && blnIsEndOfBlock)
            throw new IllegalArgumentException("Only one of [escape code] or [end of block] can be true");
        _bitStreamCode = bitStreamCode;
        _mdecCode = mdecCode;
        _blnIsEscapeCode = blnIsEscapeCode;
        _blnIsEndOfBlock = blnIsEndOfBlock;
    }

    public void getMdecCode(@Nonnull MdecCode out) {
        if (_mdecCode == null) {
            throw new IllegalStateException("MDEC code requested from AC without code " + this);
        } else {
            out.setFrom(_mdecCode);
        }
    }

    public @CheckForNull MdecCode getMdecCodeCopy() {
        if (_mdecCode == null)
            return null;
        else
            return _mdecCode.copy();
    }

    public @Nonnull BitStreamCode getBitStreamCode() {
        return _bitStreamCode;
    }

    public @Nonnull String getBitString() {
        return _bitStreamCode.getString();
    }

    public int getBitLength() {
        return _bitStreamCode.getLength();
    }

    public boolean isIsEscapeCode() {
        return _blnIsEscapeCode;
    }

    public boolean isIsEndOfBlock() {
        return _blnIsEndOfBlock;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_bitStreamCode);
        if (_mdecCode != null)
            sb.append(' ').append(_mdecCode);
        if (_blnIsEscapeCode)
            sb.append(" ESCAPE_CODE");
        if (_blnIsEndOfBlock)
            sb.append(" END_OF_BLOCK");
        return sb.toString();
    }

    public boolean equalsMdec(@Nonnull MdecCode code) {
        return _mdecCode != null && _mdecCode.equals(code);
    }

}
