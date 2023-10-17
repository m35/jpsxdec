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

package jpsxdec.psxvideo.bitstreams;

import jpsxdec.psxvideo.mdec.MdecCode;


public class ZeroRunLengthAcLookup_STR {

    public static final ZeroRunLengthAc ESCAPE_CODE = new ZeroRunLengthAc(BitStreamCode._000001___________, true, false);
    public static final ZeroRunLengthAc END_OF_BLOCK = new ZeroRunLengthAc(BitStreamCode._10_______________,
            MdecCode.MDEC_END_OF_DATA_TOP6, MdecCode.MDEC_END_OF_DATA_BOTTOM10, false, true);

    /** The standard bit-stream Huffman variable length codes used in almost
     * all PlayStation games. Conveniently identical to MPEG1. */
    public static final ZeroRunLengthAcLookup AC_VARIABLE_LENGTH_CODES_MPEG1 = new ZeroRunLengthAcLookup.Builder()
                 //  Code        "Run" "Level"
                ._11s              (0 ,  1)
                ._011s             (1 ,  1)
                ._0100s            (0 ,  2)
                ._0101s            (2 ,  1)
                ._00101s           (0 ,  3)
                ._00110s           (4 ,  1)
                ._00111s           (3 ,  1)
                ._000100s          (7 ,  1)
                ._000101s          (6 ,  1)
                ._000110s          (1 ,  2)
                ._000111s          (5 ,  1)
                ._0000100s         (2 ,  2)
                ._0000101s         (9 ,  1)
                ._0000110s         (0 ,  4)
                ._0000111s         (8 ,  1)
                ._00100000s        (13,  1)
                ._00100001s        (0 ,  6)
                ._00100010s        (12,  1)
                ._00100011s        (11,  1)
                ._00100100s        (3 ,  2)
                ._00100101s        (1 ,  3)
                ._00100110s        (0 ,  5)
                ._00100111s        (10,  1)
                ._0000001000s      (16,  1)
                ._0000001001s      (5 ,  2)
                ._0000001010s      (0 ,  7)
                ._0000001011s      (2 ,  3)
                ._0000001100s      (1 ,  4)
                ._0000001101s      (15,  1)
                ._0000001110s      (14,  1)
                ._0000001111s      (4 ,  2)
                ._000000010000s    (0 , 11)
                ._000000010001s    (8 ,  2)
                ._000000010010s    (4 ,  3)
                ._000000010011s    (0 , 10)
                ._000000010100s    (2 ,  4)
                ._000000010101s    (7 ,  2)
                ._000000010110s    (21,  1)
                ._000000010111s    (20,  1)
                ._000000011000s    (0 ,  9)
                ._000000011001s    (19,  1)
                ._000000011010s    (18,  1)
                ._000000011011s    (1 ,  5)
                ._000000011100s    (3 ,  3)
                ._000000011101s    (0 ,  8)
                ._000000011110s    (6 ,  2)
                ._000000011111s    (17,  1)
                ._0000000010000s   (10,  2)
                ._0000000010001s   (9 ,  2)
                ._0000000010010s   (5 ,  3)
                ._0000000010011s   (3 ,  4)
                ._0000000010100s   (2 ,  5)
                ._0000000010101s   (1 ,  7)
                ._0000000010110s   (1 ,  6)
                ._0000000010111s   (0 , 15)
                ._0000000011000s   (0 , 14)
                ._0000000011001s   (0 , 13)
                ._0000000011010s   (0 , 12)
                ._0000000011011s   (26,  1)
                ._0000000011100s   (25,  1)
                ._0000000011101s   (24,  1)
                ._0000000011110s   (23,  1)
                ._0000000011111s   (22,  1)
                ._00000000010000s  (0 , 31)
                ._00000000010001s  (0 , 30)
                ._00000000010010s  (0 , 29)
                ._00000000010011s  (0 , 28)
                ._00000000010100s  (0 , 27)
                ._00000000010101s  (0 , 26)
                ._00000000010110s  (0 , 25)
                ._00000000010111s  (0 , 24)
                ._00000000011000s  (0 , 23)
                ._00000000011001s  (0 , 22)
                ._00000000011010s  (0 , 21)
                ._00000000011011s  (0 , 20)
                ._00000000011100s  (0 , 19)
                ._00000000011101s  (0 , 18)
                ._00000000011110s  (0 , 17)
                ._00000000011111s  (0 , 16)
                ._000000000010000s (0 , 40)
                ._000000000010001s (0 , 39)
                ._000000000010010s (0 , 38)
                ._000000000010011s (0 , 37)
                ._000000000010100s (0 , 36)
                ._000000000010101s (0 , 35)
                ._000000000010110s (0 , 34)
                ._000000000010111s (0 , 33)
                ._000000000011000s (0 , 32)
                ._000000000011001s (1 , 14)
                ._000000000011010s (1 , 13)
                ._000000000011011s (1 , 12)
                ._000000000011100s (1 , 11)
                ._000000000011101s (1 , 10)
                ._000000000011110s (1 ,  9)
                ._000000000011111s (1 ,  8)
                ._0000000000010000s(1 , 18)
                ._0000000000010001s(1 , 17)
                ._0000000000010010s(1 , 16)
                ._0000000000010011s(1 , 15)
                ._0000000000010100s(6 ,  3)
                ._0000000000010101s(16,  2)
                ._0000000000010110s(15,  2)
                ._0000000000010111s(14,  2)
                ._0000000000011000s(13,  2)
                ._0000000000011001s(12,  2)
                ._0000000000011010s(11,  2)
                ._0000000000011011s(31,  1)
                ._0000000000011100s(30,  1)
                ._0000000000011101s(29,  1)
                ._0000000000011110s(28,  1)
                ._0000000000011111s(27,  1)
                .add(ESCAPE_CODE)
                .add(END_OF_BLOCK)
                .build();

    /** Debug */
    public static void main(String[] args) {
        AC_VARIABLE_LENGTH_CODES_MPEG1.printAllLookupTables(System.out);
    }

}
