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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** MDEC conversion and bitstream exceptions. */
public abstract class MdecException  {

    /** Error related to decoding an MDEC stream. */
    public static class ReadCorruption extends Exception {

        public ReadCorruption(@Nonnull String message) {
            super(message);
        }

        public ReadCorruption(@Nonnull Throwable cause) {
            super(cause);
        }

        public ReadCorruption(@Nonnull String message, @CheckForNull Throwable cause) {
            super(message, cause);
        }
    }
    public static String RLC_OOB_IN_MB_BLOCK(int outOfBoundsValue, int macroBlockIndex, int blockIndex) {
        return String.format("[MDEC] Run length out of bounds [%d] in macroblock %d block %d", outOfBoundsValue, macroBlockIndex, blockIndex);
    }

    public static String RLC_OOB_IN_BLOCK_NAME(int outOfBoundsIndex, int macroBlockIndex, int macroBlockX, int macroBlockY, int blockIndex, @Nonnull String blockName) {
        return String.format("[MDEC] Run length out of bounds [%d] in macroblock %d (%d, %d) block %d (%s)", outOfBoundsIndex, macroBlockIndex, macroBlockX, macroBlockY, blockIndex, blockName);
    }


    public static String STRV3_BLOCK_UNCOMPRESS_ERR_CHROMA_DC_OOB(int macroBlockIndex, int blockIndex, int outOfBoundsValue) {
        return String.format("Error uncompressing macro block %d.%d: Chroma DC out of bounds: %d", macroBlockIndex, blockIndex, outOfBoundsValue);
    }

    public static String STRV3_BLOCK_UNCOMPRESS_ERR_LUMA_DC_OOB(int macroBlockIndex, int blockIndex, int outOfBoundsValue) {
        return String.format("Error uncompressing macro block %d.%d: Luma DC out of bounds: %d", macroBlockIndex, blockIndex, outOfBoundsValue);
    }

    public static String STRV3_BLOCK_UNCOMPRESS_ERR_UNKNOWN_LUMA_DC_VLC(int macroBlockIndex, int blockIndex, @Nonnull String variableLengthCodeBits) {
        return String.format("Error uncompressing macro block %d.%d: Unknown luma DC variable length code %s", macroBlockIndex, blockIndex, variableLengthCodeBits);
    }

    // =========================================================

    /** Exception thrown at the end of an MDEC stream. */
    public static class EndOfStream extends Exception {

        public EndOfStream() {
        }

        public EndOfStream(@Nonnull String message) {
            super(message);
        }

        public EndOfStream(@Nonnull Throwable cause) {
            super(cause);
        }

        public EndOfStream(@Nonnull String message, @Nonnull Throwable cause) {
            super(message, cause);
        }

    }
    public static String UNEXPECTED_STREAM_END_IN_BLOCK(int iBlockNumber) {
        return String.format("Unexpected end of stream in block %s", iBlockNumber);
    }

    public static String RLC_OOB_IN_MB_XY_BLOCK(int outOfBoundsIndex, int macroBlockIndex, int macroBlockX, int macroBlockY, int blockIndex) {
        return String.format("[MDEC] Run length out of bounds [%d] in macroblock %d (%d, %d) block %d", outOfBoundsIndex, macroBlockIndex, macroBlockX, macroBlockY, blockIndex);
    }

    public static String END_OF_BITSTREAM(int bitstreamOffset) {
        return String.format("Unexpected end of bitstream at %d", bitstreamOffset);
    }

    public static String inBlockOfBlocks(int iCurrentBlock, int iBlockCount) {
        return String.format("Unexpected end of block %d out of %d blocks", iCurrentBlock, iBlockCount);
    }

    // =========================================================

    /** Exception thrown during encoding when the source mdec stream contains
     * too much energy to compress with that particular encoder.
     * "Too much energy" may seem like a weird way to describe it, but
     * is accurate given the nature of the DCT and working in the frequency domain.
     */
    public static class TooMuchEnergy extends Exception {

        public TooMuchEnergy(String message) {
            super(message);
        }

    }
}
