/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;


/** Represents a 16-bit code readable by the PlayStation MDEC chip.
 *  If the MDEC code is the first of a block, the top 6 bits indicate
 *  the block's quantization scale, and the bottom 10 bits indicate
 *  the "direct current" (DC) coefficient.
 *  If the MDEC code is not the first of a block, and it is
 *  not a {@link #MDEC_END_OF_DATA} code (0xFE00), then the top 6 bits indicate
 *  the number of zeros preceding an "alternating current" (AC) coefficient,
 *  with the bottom 10 bits indicating a (usually) non-zero AC coefficient.  */
public class MdecCode implements Comparable<MdecCode> {

    private static final Logger LOG = Logger.getLogger(MdecCode.class.getName());

    /** 16-bit MDEC code indicating the end of a block.
     * The equivalent MDEC value is (63, -512). */
    public final static int MDEC_END_OF_DATA = 0xFE00;
    /** Top 6 bits of {@link #MDEC_END_OF_DATA}. */
    public final static int MDEC_END_OF_DATA_TOP6 = (MDEC_END_OF_DATA >> 10) & 63;
    /** Bottom 10 bits of {@link #MDEC_END_OF_DATA}. */
    public final static int MDEC_END_OF_DATA_BOTTOM10 = (short)(MDEC_END_OF_DATA | 0xFC00);

    /** Most significant 6 bits of the 16-bit MDEC code.
     * Holds either a block's quantization scale or the
     * count of zero AC coefficients leading up to a non-zero
     * AC coefficient. Unsigned. */
    private int _iTop6Bits;

    /** Least significant 10 bits of the 16-bit MDEC code.
     * Holds either the DC coefficient of a block or
     * a non-zero AC coefficient. Signed. */
    private int _iBottom10Bits;

    /** Initializes to (0, 0). */
    public MdecCode() {
        _iTop6Bits = 0;
        _iBottom10Bits = 0;
    }

    public MdecCode(int iTop6Bits, int iBottom10Bits) {
        if (!validTop(iTop6Bits))
            throw new IllegalArgumentException("Invalid top 6 bits " + iTop6Bits);
        if (!validBottom(iBottom10Bits))
            throw new IllegalArgumentException("Invalid bottom 10 bits " + iBottom10Bits);
        _iTop6Bits = iTop6Bits;
        _iBottom10Bits = iBottom10Bits;
    }

    /** Extract the top 6 bit and bottom 10 bit values from 16 bits */
    public MdecCode(int iMdecWord) {
        set(iMdecWord);
    }

    public void setFrom(@Nonnull MdecCode other) {
        _iTop6Bits = other._iTop6Bits;
        _iBottom10Bits = other._iBottom10Bits;
    }

    public int getBottom10Bits() {
        return _iBottom10Bits;
    }

    /** By default does not verify the value for performance. */
    public void setBottom10Bits(int iBottom10Bits) {
        assert validBottom(iBottom10Bits);
        _iBottom10Bits = iBottom10Bits;
    }

    public int getTop6Bits() {
        return _iTop6Bits;
    }

    /** By default does not verify the value for performance. */
    public void setTop6Bits(int iTop6Bits) {
        assert validTop(iTop6Bits);
        _iTop6Bits = iTop6Bits;
    }

    /** By default does not verify the values for performance. */
    public void setBits(int iTop6, int iBottom10) {
        assert validTop(_iTop6Bits) && validBottom(_iBottom10Bits);
        _iTop6Bits = iTop6;
        _iBottom10Bits = iBottom10;
    }

    public void set(int iMdecWord) {
        _iTop6Bits = ((iMdecWord >> 10) & 63);
        _iBottom10Bits = (iMdecWord & 0x3FF);
        if ((_iBottom10Bits & 0x200) == 0x200) { // is it negative?
            _iBottom10Bits -= 0x400;
        }
    }

    /** Combines the top 6 bits and bottom 10 bits into an unsigned 16 bit value. */
    public int toMdecShort() {
        if (isEOD())
            return MDEC_END_OF_DATA;
        if (!validTop(_iTop6Bits))
            throw new IllegalStateException("MDEC code has invalid top 6 bits " + _iTop6Bits);
        if (!validBottom(_iBottom10Bits))
            throw new IllegalStateException("MDEC code has invalid bottom 10 bits " + _iBottom10Bits);
        return ((_iTop6Bits & 63) << 10) | (_iBottom10Bits & 0x3FF);
    }

    /** Set the MDEC code to the special "End of Data" (EOD) value,
     * indicating the end of a block.
     * @see MdecInputStream#MDEC_END_OF_DATA */
    public @Nonnull MdecCode setToEndOfData() {
        _iTop6Bits = MDEC_END_OF_DATA_TOP6;
        _iBottom10Bits = MDEC_END_OF_DATA_BOTTOM10;
        return this;
    }

    /** Returns if this MDEC code is setFrom to the special "End of Data" (EOD) value.
     * @see MdecInputStream#MDEC_END_OF_DATA */
    public boolean isEOD() {
        return (_iTop6Bits == MDEC_END_OF_DATA_TOP6 &&
                _iBottom10Bits == MDEC_END_OF_DATA_BOTTOM10);
    }

    /** Returns if this MDEC code has valid values.
     * As an optimization, many parameter checks are disabled, so
     * this MDEC code could hold values that are be invalid. */
    public boolean isValid() {
        return validTop(_iTop6Bits) && validBottom(_iBottom10Bits);
    }

    /** Checks if the top 6 bits of an MDEC code are valid. */
    private static boolean validTop(int iTop6Bits) {
        boolean blnValid = iTop6Bits >= 0 && iTop6Bits <= 63;
        if (!blnValid)
            LOG.log(Level.FINE, "Invalid top 6 bit value {0,number,#}", iTop6Bits);
        return blnValid;
    }
    /** Checks if the bottom 10 bits of an MDEC code are valid. */
    private static boolean validBottom(int iBottom10Bits) {
        boolean blnValid = iBottom10Bits >= -512 && iBottom10Bits <= 511;
        if (!blnValid)
            LOG.log(Level.FINE, "Invalid bottom 10 bit value {0,number,#}", iBottom10Bits);
        return blnValid;
    }

    public @Nonnull MdecCode copy() {
        return new MdecCode(_iTop6Bits, _iBottom10Bits);
    }

    @Override
    public String toString() {
        String s = String.format("%04x (%d, %d)", toMdecShort(), _iTop6Bits, _iBottom10Bits);
        if (isEOD())
            return s + " EOD";
        else
            return s;
    }

    @Override
    public int compareTo(MdecCode o) {
        int i = Integer.compare(_iTop6Bits, o._iTop6Bits);
        if (i != 0)
            return i;
        return Integer.compare(_iBottom10Bits, o._iBottom10Bits);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MdecCode other = (MdecCode) obj;
        return _iTop6Bits == other._iTop6Bits &&
               _iBottom10Bits == other._iBottom10Bits;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + _iTop6Bits;
        hash = 97 * hash + _iBottom10Bits;
        return hash;
    }

}
