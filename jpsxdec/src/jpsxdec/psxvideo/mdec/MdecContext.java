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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** Tracks MDEC codes, blocks, macro-blocks, and pixel coordinates during
 * the decoding process. One-time use. */
public class MdecContext {

    private static final Logger LOG = Logger.getLogger(MdecContext.class.getName());

    private int _iCurrentMacroBlock = 0;
    @Nonnull
    private MdecBlock _currentBlock = MdecBlock.first();
    private int _iCurrentMdecCodeInCurrentBlock = 0;

    private int _iCurrentTotalBlocks = 0;
    private int _iCurrentTotalMdecCode = 0;

    @CheckForNull
    private final Integer _oiMacroBlockHeight;

    /** By providing the height of the frame, it allows tracking of what pixel
     *  x/y is being decoded. */
    public MdecContext(int iFrameMacroBlockHeight) {
        _oiMacroBlockHeight = Integer.valueOf(iFrameMacroBlockHeight);
    }

    public MdecContext() {
        _oiMacroBlockHeight = null;
    }

    public int getTotalMacroBlocksRead() {
        return _iCurrentMacroBlock;
    }

    /** Total number of (sub) blocks that have been read thus far. */
    public int getTotalBlocksRead() {
        return _iCurrentTotalBlocks;
    }

    /** Total number of MDEC codes that have been read thus far. */
    public int getTotalMdecCodesRead() {
        return _iCurrentTotalMdecCode;
    }

    public @Nonnull MdecBlock getCurrentBlock() {
        return _currentBlock;
    }

    public int getMdecCodesReadInCurrentBlock() {
        return _iCurrentMdecCodeInCurrentBlock;
    }

    /** Indicates if the next read should be the Quantization Scale and DC Coefficient. */
    public boolean atStartOfBlock() {
        return _iCurrentMdecCodeInCurrentBlock == 0;
    }

    /** Increments the number of MDEC codes that have been read. */
    public void nextCode() {
        _iCurrentTotalMdecCode++;
        _iCurrentMdecCodeInCurrentBlock++;
        if (_iCurrentMdecCodeInCurrentBlock > 64)
            LOG.log(Level.WARNING, "Impossible number of codes in a block {0}", _iCurrentMdecCodeInCurrentBlock);
    }

    /** Increments the number of MDEC codes that have been read and ends the block. */
    public void nextCodeEndBlock() {
        _iCurrentTotalMdecCode++;
        _iCurrentMdecCodeInCurrentBlock = 0;
        _iCurrentTotalBlocks++;
        _currentBlock = _currentBlock.next();
        if (_currentBlock == MdecBlock.first())
            _iCurrentMacroBlock++;
    }

    /** The pixel of the top-left corner of a macro-block. */
    public static class MacroBlockPixel {
        public final int x;
        public final int y;

        private MacroBlockPixel(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /** Returns null if no dimensions were specified in constructor. */
    public @CheckForNull MacroBlockPixel getMacroBlockPixel() {
        if (_oiMacroBlockHeight == null)
            return null;
        int iMacroBlockHeight = _oiMacroBlockHeight.intValue();
        return new MacroBlockPixel((_iCurrentMacroBlock / iMacroBlockHeight) * 16, (_iCurrentMacroBlock % iMacroBlockHeight) * 16);
    }

    @Override
    public String toString() {
        String s = String.format("Macro.block.code %d.%s(%d).%d total blocks %d codes %d",
                                 _iCurrentMacroBlock, _currentBlock, _currentBlock.ordinal(),
                                 _iCurrentMdecCodeInCurrentBlock,
                                 _iCurrentTotalBlocks,
                                 _iCurrentTotalMdecCode);
        MacroBlockPixel pixel = getMacroBlockPixel();
        if (pixel == null)
            return s;
        else
            return s + " " + pixel;
    }
}
