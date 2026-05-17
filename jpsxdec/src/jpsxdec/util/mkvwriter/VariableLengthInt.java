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

import javax.annotation.Nonnull;

/**
 * Functions related to variable length integers, as defined in the EBML spec.
 * Sometimes referred to as "vint".
 * <p>
 * To understand mkv, you need to understand EBML. To understand EBML, you need
 * to understand variable length ints.
 * <p>
 * Variable length ints start with 1 byte indicating how many bytes are needed
 * to hold the integer. The (number of zeroes before the first set bit) + 1
 * indicates the number of bytes. Example: {@code 00100000} means the integer
 * will be 3 bytes long. This count includes the header byte.
 * <p>
 * After that initial bit, the rest of the bits can be used to hold the actual integer
 * value. So the value 33 would be encoded as {@code 10010110}: the top bit says the int
 * is 1 byte in size, and the remaining 7 bits equal 33. The smallest value
 * would be: {@code 10000000}: the top bit again says value takes 1 byte, and
 * the remaining bits are 0, so the value is 0. Since that header byte only has
 * 8 bits, that means the largest integer value could be 8 bytes. In that case
 * the header byte would be {@code 00000001}. The entire byte is used to
 * indicate the length of the integer, so couldn't be used for the actual integer
 * value. That leaves {@code (8 bytes - 1)=7} bytes, or 56 bits.
 */
class VariableLengthInt {

    /** Declared in the MKV EBML header {@code EBMLMaxSizeLength} meaning no values will
     *  be larger than this number of bytes. */
    public static final int EBML_MAX_SIZE_LENGTH = 8;

    public static @Nonnull byte[] encodeVariableUnsignedInt(long lngUnsignedValue) {
        assert EBML_MAX_SIZE_LENGTH == 8; // This function assumes this size.

        // Not the fastest implementation, but hopefully clearer
        final int iBitsNeeded = 64 - Long.numberOfLeadingZeros(lngUnsignedValue);
        final long lngLengthMask;
        final int iByteSize;
        if (iBitsNeeded <= 7)         { //  8 - 1
            iByteSize = 1;
            lngLengthMask = 1L << 7;
        } else if (iBitsNeeded <= 14) { // 16 - 2
            iByteSize = 2;
            lngLengthMask = 1L << 14;
        } else if (iBitsNeeded <= 21) { // 24 - 3
            iByteSize = 3;
            lngLengthMask = 1L << 21;
        } else if (iBitsNeeded <= 28) { // 32 - 4
            iByteSize = 4;
            lngLengthMask = 1L << 28;
        } else if (iBitsNeeded <= 35) { // 40 - 5
            iByteSize = 5;
            lngLengthMask = 1L << 35;
        } else if (iBitsNeeded <= 42) { // 48 - 6
            iByteSize = 6;
            lngLengthMask = 1L << 42;
        } else if (iBitsNeeded <= 49) { // 56 - 7
            iByteSize = 7;
            lngLengthMask = 1L << 49;
        } else if (iBitsNeeded <= 56) { // 64 - 8
            iByteSize = 8;
            lngLengthMask = 1L << 56;
        } else {
            throw new IllegalArgumentException(lngUnsignedValue + " too big to fit in a 8 byte vint");
        }

        lngUnsignedValue |= lngLengthMask;
        byte[] ab = numberToBEBytes(lngUnsignedValue, iByteSize);
        return ab;
    }

    public static @Nonnull byte[] minimalUnsignedIntToBEBytes(long lngUnsignedValue) {
        // Integer values can be written with any number of bytes.
        // A 0 could be written as 00000000, or 00000000 00000000
        // In most cases, you can just write the minimum number of bytes to hold
        // the value.
        // NOTE! While this int can be variable length, it is not
        // the same as the EBML variable length int. This variable length int
        // holds no information about how long the int is. That length value
        // would precede this as an EBML variable length int.

        // For example, the number 3 would be written as:
        // 10000001 00000011
        // aaaaaaaa bbbbbbbb
        // a = EBML vint with length of 1 byte holding the value 1
        //     (length of the actual int)
        // b = int of length 1 holding the value of 3

        // Look at the number of bits actually used...
        final int iBitsNeeded = 64 - Long.numberOfLeadingZeros(lngUnsignedValue);
        // ...to get the number of bytes needed
        int iBytesNeeded = (iBitsNeeded + 7) / 8;
        if (iBytesNeeded == 0)
            iBytesNeeded = 1; // but 0 still needs 1 byte

        byte[] ab = numberToBEBytes(lngUnsignedValue, iBytesNeeded);
        return ab;
    }

    /** Converts the value into big-endian bytes. */
    public static @Nonnull byte[] numberToBEBytes(long lngUnsignedValue, int iByteSize) {
        byte[] ab = new byte[iByteSize];
        for (int i = ab.length-1; i >= 0; i--) {
            ab[i] = (byte) (lngUnsignedValue & 0xff);
            lngUnsignedValue >>>= 8;
        }
        return ab;
    }

}
