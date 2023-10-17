/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2020-2023  Michael Sabin
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

package jpsxdec.modules.starwars;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;

/** Star Wars - Rebel Assault II - The Hidden Empire bitstream version lookup.
 *
 * The bitstream headers for all the frames in this game have a random
 * number where the 'version' value should be (i.e. STR version 2, version 3, etc.).
 *
 * Some videos use STRv2 bitstream style, and some use STRv3 style.
 *
 * So how do we identify that these frames use Star Wars bitstream?
 * And how do we know whether it's STRv2 or STRv3?
 *
 * Here we use a pre-made table that contains both the random 'version'
 * value in the header, combined with the 10th and 11th bytes in the
 * frame data. Those combined values are sufficient to identify a valid
 * Star Wars frame, and also know whether the frame uses STRv2 or STRv3
 * style bitstreams.
 *
 * Format of the frame lookup table data file:
 *
 * First 4 bytes little-endian: number of entries in the table (to allocate array ahead of time).
 *
 * The table is a sorted list of 4 byte little-endian values.
 * Each entry is a combination of bytes 6, 7, 10 and 11.
 *
 * <pre>
 * Bits:
 *
 * vvvv vvvv vvvv vvvv bbbb bbbb bbbb bbbt
 *
 *  v = the 2 bytes (16 bits) at offset 6-7 (a random value where the 'version' normally is)
 *  b = the top 15 bits of the 2 bytes at offset 10-11
 *  t = 0 if the frame type is STRv2, 1 if the frame type is STRv3.
 * </pre>
 * The table is searched using binary search for the top 31 bits.
 * If a value in the table starts with those 31 bits,
 * bit 32 in the table entry indicates the version type (0 for v2, 1 for v3).
 */
public class StarWarsFrameTypeLookup {

    private static final Logger LOG = Logger.getLogger(StarWarsFrameTypeLookup.class.getName());

    static final String LOOKUP_TABLE_FILE = "swra2bsheaderid.dat";

    @Nonnull
    static final int[] LOOKUP_TABLE;
    static {
        InputStream is = null;
        try {
            is = StarWarsFrameTypeLookup.class.getResourceAsStream(LOOKUP_TABLE_FILE);
            if (is == null) {
                LOG.severe("Star Wars: Rebel Assault II: The Hidden Empire frame type lookup table missing");
                LOOKUP_TABLE = new int[0];
            } else {
                LOOKUP_TABLE = readTable(is);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            IO.closeSilently(is, LOG);
        }
    }

    static @Nonnull int[] readTable(@Nonnull InputStream is) throws IOException {
        int iEntryCount = IO.readSInt32LE(is);
        int[] aiLookupTable = new int[iEntryCount];

        for (int i = 0; i < iEntryCount; i++) {
            aiLookupTable[i] = IO.readSInt32LE(is);
        }

        if (is.read() != -1)
            throw new RuntimeException("Unexpected extra data");

        return aiLookupTable;
    }

    static int extractLookupValue(@Nonnull byte[] abBitstream) {
        int iRandomVersion = IO.readUInt16LE(abBitstream, 6);
        // use bytes 10 and 11, but clear the bottom bit
        int iBytes10_11_lastBit0 = IO.readUInt16LE(abBitstream, 10) & ~1;
        return (iRandomVersion << 16) | iBytes10_11_lastBit0;
    }

    /** Returns 2 or 3 if it is a valid frame, or another value (like -1)
     * if the frame is not found. */
    static int getFrameType(@Nonnull byte[] abBitstream) {
        return getFrameType(LOOKUP_TABLE, abBitstream);
    }

    private static int getFrameType(@Nonnull int[] aiLookupTable, @Nonnull byte[] abBitstream) {
        int i32bit_lastBit0 = extractLookupValue(abBitstream);
        return getFrameType(aiLookupTable, i32bit_lastBit0);
    }

    static int getFrameType(@Nonnull int[] aiLookupTable, int i32bit_lastBit0) {
        assert (i32bit_lastBit0 & 1) == 0;
        int i32bit_lastBit1 = i32bit_lastBit0 | 1;
        assert i32bit_lastBit0 < i32bit_lastBit1;

        int iMin = 0;
        int iMax = aiLookupTable.length - 1;
        // binary search for a value between a range
        // continue searching while [min,max] is not empty
        while (iMax >= iMin) {
            int iMid = (iMin + iMax) / 2;

            int iMidValue = aiLookupTable[iMid];

            // determine which subarray to search
            if (iMidValue < i32bit_lastBit0) { // change min index to search upper subarray
                iMin = iMid + 1;
            } else if (iMidValue > i32bit_lastBit1) { // change max index to search lower subarray
                iMax = iMid - 1;
            } else { // key found within index mid
                assert iMidValue == i32bit_lastBit0 || iMidValue == i32bit_lastBit1;
                // Bottom bit indicates ver 2 or 3
                return (iMidValue & 1) == 0 ? 3 : 2;
            }
        }
        // key not found
        return -1;
    }

}
