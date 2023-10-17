/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

package jpsxdec.modules.aconcagua;

import javax.annotation.Nonnull;

public class TableLookup {

    public interface IBits {
        @Nonnull String getBits();
    }

    /** This is a very common way to read bit streams. All possible combinations
     * are expanded into a single array so every read will be O(1), kind of like a HashMap.
     * Invalid combinations will have a null value. */
    public static void buildLookup(int iBitMask, @Nonnull IBits[] table,
                                   @Nonnull IBits[] lookupTable)
    {
        for (IBits code : table) {
            String sBits = code.getBits();
            int iBits = Integer.parseInt(sBits, 2);

            if ((iBits & ~iBitMask) != 0)
                throw new IllegalArgumentException("Bit string does not fit in mask");

            int iMaxLeadingBits = iBitMask >>> sBits.length();

            for (int iLeadingBits = 0; iLeadingBits <= iMaxLeadingBits; iLeadingBits++) {
                int iLookupIdx = (iLeadingBits << sBits.length()) | iBits;
                if (lookupTable[iLookupIdx] != null) // sanity check
                    throw new RuntimeException("Bad logic");
                lookupTable[iLookupIdx] = code;
            }
        }
    }

}
