/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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

package jpsxdec.psxvideo.mdec.tojpeg;

import java.io.IOException;
import java.io.OutputStream;
import jpsxdec.util.IO;

/** JPEG huffman table. */
class HuffmanTable {

    public static void initializeHuffmanTables(HuffmanTable[] aoDhtTables,
                                               HuffmanTable[] aoDcHuffmanTables,
                                               HuffmanTable[] aoAcHuffmanTables)
    {
        for (HuffmanTable dhtTable : aoDhtTables) {
            if (dhtTable._iTableType == 0) {
                aoDcHuffmanTables[dhtTable.getIndex()] = dhtTable;
            } else {
                aoAcHuffmanTables[dhtTable.getIndex()] = dhtTable;
            }
        }
    }

    private static final int DC = 0;
    private static final int AC = 1;

    public static final HuffmanTable DEFAULT_DC_LUMA_HUFFMAN = new HuffmanTable(DC,0, new int[][]{
            null,
            {0},
            {1, 2, 3, 4, 5},
            {6},
            {7},
            {8},
            {9},
            {10},
            {11},
            null,
            null,
            null,
            null,
            null,
            null,
            null,
    });
    public static final HuffmanTable DEFAULT_DC_CHROMA_HUFFMAN = new HuffmanTable(DC,1, new int[][]{
            null,
            {0, 1, 2},
            {3},
            {4},
            {5},
            {6},
            {7},
            {8},
            {9},
            {10},
            {11},
            null,
            null,
            null,
            null,
            null,
    });
    public static final HuffmanTable DEFAULT_AC_LUMA_HUFFMAN = new HuffmanTable(AC,0, new int[][]{
            null,                                              // 0
            {1, 2},                                            // 2
            {3},                                               // 1
            {0, 4, 17},                                        // 3
            {5, 18, 33},                                       // 3
            {49, 65},                                          // 2
            {6, 19, 81, 97},                                   // 4
            {7, 34, 113},                                      // 3
            {20, 50, 129, 145, 161},                           // 5
            {8, 35, 66, 177, 193},                             // 5
            {21, 82, 209, 240},                                // 4
            {36, 51, 98, 114},                                 // 4
            null,                                              // 0
            null,                                              // 0
            {130},                                             // 1
            {  9,  10,  22,  23,  24,  25,  26,  37,  38,  39, // 125
              40,  41,  42,  52,  53,  54,  55,  56,  57,  58,
              67,  68,  69,  70,  71,  72,  73,  74,  83,  84,
              85,  86,  87,  88,  89,  90,  99, 100, 101, 102,
             103, 104, 105, 106, 115, 116, 117, 118, 119, 120,
             121, 122, 131, 132, 133, 134, 135, 136, 137, 138,
             146, 147, 148, 149, 150, 151, 152, 153, 154, 162,
             163, 164, 165, 166, 167, 168, 169, 170, 178, 179,
             180, 181, 182, 183, 184, 185, 186, 194, 195, 196,
             197, 198, 199, 200, 201, 202, 210, 211, 212, 213,
             214, 215, 216, 217, 218, 225, 226, 227, 228, 229,
             230, 231, 232, 233, 234, 241, 242, 243, 244, 245,
             246, 247, 248, 249, 250 },
    });
    public static final HuffmanTable DEFAULT_AC_CHROMA_HUFFMAN = new HuffmanTable(AC,1, new int[][]{
            null,                                              // 0
            {0, 1},                                            // 2
            {2},                                               // 1
            {3, 17},                                           // 2
            {4, 5, 33, 49},                                    // 4
            {6, 18, 65, 81},                                   // 4
            {7, 97, 113},                                      // 3
            {19, 34, 50, 129},                                 // 4
            {8, 20, 66, 145, 161, 177, 193},                   // 7
            {9, 35, 51, 82, 240},                              // 5
            {21, 98, 114, 209},                                // 4
            {10, 22, 36, 52},                                  // 4
            null,                                              // 0
            {225},                                             // 1
            {37, 241},                                         // 2
            { 23,  24,  25,  26,  38,  39,  40,  41,  42,  53, // 119
              54,  55,  56,  57,  58,  67,  68,  69,  70,  71,
              72,  73,  74,  83,  84,  85,  86,  87,  88,  89,
              90,  99, 100, 101, 102, 103, 104, 105, 106, 115,
             116, 117, 118, 119, 120, 121, 122, 130, 131, 132,
             133, 134, 135, 136, 137, 138, 146, 147, 148, 149,
             150, 151, 152, 153, 154, 162, 163, 164, 165, 166,
             167, 168, 169, 170, 178, 179, 180, 181, 182, 183,
             184, 185, 186, 194, 195, 196, 197, 198, 199, 200,
             201, 202, 210, 211, 212, 213, 214, 215, 216, 217,
             218, 226, 227, 228, 229, 230, 231, 232, 233, 234,
             242, 243, 244, 245, 246, 247, 248, 249, 250 },
    });


    /** Index of the EOB huffman code. */
    private static final int EOB_CODE_INDEX = 0;
    /** Index of the zero-run 16 huffman code. */
    private static final int RUN16_CODE_INDEX = 240;

    /** Either {@link #AC} or {@link #DC}. */
    private final int _iTableType;
    /** Index of this huffman table. */
    private final int _iTableIndex;
    /** Huffman table values for writing. */
    private final int[][] _aaiValuesForBitLen;

    /** Huffman table converted to codes used for encoding. */
    private final int[] _aiHuffCodes = new int[256];
    /** Huffman table converted to code lengths used for encoding. */
    private final int[] _aiHuffCodesSize = new int[256];

    /** Size of the DHT block needed to write this huffman table. */
    private final int _iDhtLength;

    private HuffmanTable(int iType, int iIndex, int[][] aaiValuesForBitLen) {
        if (aaiValuesForBitLen.length != 16)
            throw new IllegalArgumentException();

        _iTableType = iType;
        _iTableIndex = iIndex;
        _aaiValuesForBitLen = aaiValuesForBitLen;

        int iCode = 0;
        int iTableSize = 0;
        for (int iBitLen = 0; iBitLen < 16; iBitLen++) {
            int[] aiValues = _aaiValuesForBitLen[iBitLen];
            if (aiValues != null) {
                iTableSize += aiValues.length;

                for (int iValue : aiValues) {
                    _aiHuffCodesSize[iValue] = iBitLen + 1;
                    _aiHuffCodes[iValue] = iCode;
                    iCode++;
                }
            }
            iCode <<= 1;
        }

        _iDhtLength = 2 + 1 + 16 + iTableSize;
    }

    public int getIndex() {
        return _iTableIndex;
    }

    public void writeDHT(OutputStream out) throws IOException {
        Mdec2Jpeg.writeMarker(out, Mdec2Jpeg.DHT);
        IO.writeInt16BE(out, _iDhtLength);
        out.write((_iTableType << 4) | _iTableIndex);
        for (int[] i : _aaiValuesForBitLen) {
            out.write(i == null ? 0 : i.length);
        }
        for (int[] i : _aaiValuesForBitLen) {
            if (i != null) {
                for (int j : i) {
                    out.write(j);
                }
            }
        }
    }

    /** Only used for DC tables. */
    public void encodeDcCoefficient(int iDc, Component comp, JpegBitOutputStream out)
            throws IOException
    {
        int iDcDiff = iDc - comp.PreviousDC;
        comp.PreviousDC = iDc;
        if (iDcDiff < 0) {
            int iAbsDcDiff = -iDcDiff;
            int iBitSize = highest1bitPosition(iAbsDcDiff);
            assert iBitSize <= 11;
            out.write(_aiHuffCodes[iBitSize], _aiHuffCodesSize[iBitSize]);
            int iAbsDcDiffMask = ((1 << iBitSize) - 1);
            out.write((0xFFFFFF - iAbsDcDiff) & iAbsDcDiffMask, iBitSize);
        } else {
            int iBitSize = highest1bitPosition(iDcDiff);
            assert iBitSize <= 11;
            out.write(_aiHuffCodes[iBitSize], _aiHuffCodesSize[iBitSize]);
            if (iBitSize != 0) {
                out.write(iDcDiff, iBitSize);
            }
        }
    }

    /** Only used for AC tables. */
    public void encodeAcCoefficients(int[] aiDctCoffs, int iBlockStart, JpegBitOutputStream out)
            throws IOException
    {
        int iZeroRun = 0;

        for (int i = 1; i < 64; i++) {
            int iAc = aiDctCoffs[iBlockStart + i];
            if (iAc == 0) {
                if (i == 63) {
                    out.write(_aiHuffCodes[EOB_CODE_INDEX], _aiHuffCodesSize[EOB_CODE_INDEX]);
                } else {
                    iZeroRun++;
                }
            } else {
                while (iZeroRun > 15) {
                    out.write(_aiHuffCodes[RUN16_CODE_INDEX], _aiHuffCodesSize[RUN16_CODE_INDEX]);
                    iZeroRun -= 16;
                }

                int iBitCount;
                if (iAc < 0) {
                    iBitCount = highest1bitPosition(-iAc);
                    iAc = (0xFFFFFF + iAc) & ((1 << iBitCount) - 1);
                } else {
                    iBitCount = highest1bitPosition(iAc);
                }
                assert iBitCount <= 10; // should have been caught during MDEC read phase
                int iCode = (iZeroRun << 4) | iBitCount;
                out.write(_aiHuffCodes[iCode], _aiHuffCodesSize[iCode]);
                out.write(iAc, iBitCount);
                iZeroRun = 0;
            }
        }
    }

    /** Similar to 1/log(2) */
    private static int highest1bitPosition(int i) {
        return 32 - Integer.numberOfLeadingZeros(i);
    }
}
