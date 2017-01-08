/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

package jpsxdec.psxvideo.encode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;

/** Parses and stores a stream of MDEC 16-bit codes in a structure for easier analysis. */
public class ParsedMdecImage  {
    
    private static final Logger LOG = Logger.getLogger(ParsedMdecImage.class.getName());

    private static class MacroBlock implements Iterable<Block> {
        public final Block[] _aoBlocks = new Block[6];

        public MacroBlock(@Nonnull MdecInputStream mdecIn)
                throws MdecException.EndOfStream, MdecException.ReadCorruption
        {
            for (int iBlock = 0; iBlock < 6; iBlock++) {
                _aoBlocks[iBlock] = new Block(iBlock, mdecIn);
            }
        }

        public @Nonnull Block getBlock(int iBlk) {
            return _aoBlocks[iBlk];
        }

        public int getMdecCodeCount() {
            int iCount = 0;
            for (Block block : _aoBlocks) {
                iCount += block.getMdecCodeCount();
            }
            return iCount;
        }

        public ArrayList<MdecCode> getCodes() {
            ArrayList<MdecCode> c = new ArrayList<MdecCode>();
            for (Block b : this) {
                Collections.addAll(c, b._aoCodes);
            }
            return c;
        }

        public @Nonnull Iterator<Block> iterator() {
            return new Iterator<Block>() {

                private int _iBlk = 0;
                
                public boolean hasNext() {
                    return _iBlk < 6;
                }

                public @Nonnull Block next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return getBlock(_iBlk++);
                }

                public void remove() {
                    throw new UnsupportedOperationException("Can't remove blocks.");
                }
            };
        }

    }
    
    private static class Block {

        private static final String[] BLOCK_NAMES = {
            "Cr", "Cb", "Y1", "Y2", "Y3", "Y4"
        };

        private final int _iIndex;
        @Nonnull
        private final MdecCode[] _aoCodes;
        /** Only available if source data was a bitstream. -1 if unknown. */
        private final int _iBitSize;

        public Block(int iIndex, @Nonnull MdecInputStream mdecIn)
                throws MdecException.EndOfStream, MdecException.ReadCorruption
        {
            if (iIndex < 0 || iIndex >= 6)
                throw new IllegalArgumentException("Invalid block index " + iIndex);
            _iIndex = iIndex;

            ArrayList<MdecCode> codes = new ArrayList<MdecCode>();
            int iBitStart = -1;
            if (mdecIn instanceof BitStreamUncompressor)
                iBitStart = ((BitStreamUncompressor)mdecIn).getBitPosition();

            MdecCode code;
            mdecIn.readMdecCode(code = new MdecCode());
            codes.add(code);

            while (!mdecIn.readMdecCode(code = new MdecCode())) {
                codes.add(code);
            }
            codes.add(code);
            _aoCodes = codes.toArray(new MdecCode[codes.size()]);
            if (iBitStart == -1)
                _iBitSize = -1;
            else
                _iBitSize = ((BitStreamUncompressor)mdecIn).getBitPosition() - iBitStart;
        }

        public @Nonnull String getName() {
            return BLOCK_NAMES[_iIndex];
        }

        public @Nonnull MdecCode getMdecCode(int i) {
            return _aoCodes[i];
        }
        
        public int getMdecCodeCount() {
            return _aoCodes.length;
        }
        
        public boolean isChroma() {
            return _iIndex < 2;
        }

        public boolean isLuma() {
            return _iIndex >= 2;
        }

        public int getIndex() {
            return _iIndex;
        }

        public int getQscale() {
            return _aoCodes[0].getTop6Bits();
        }

        public int getBitSize() {
            return _iBitSize;
        }

        /** Returns shallow copy of the codes.
         * Modifying the codes will modify the source codes! */
        public @Nonnull ArrayList<MdecCode> getCodes() {
            ArrayList<MdecCode> c = new ArrayList<MdecCode>(_aoCodes.length);
            Collections.addAll(c, _aoCodes);
            return c;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getName()).append(" Q:").append(getQscale());
            if (_iBitSize != -1)
                sb.append(" Size=").append(_iBitSize);
            return sb.toString();
        }

    }


    private class MdecReader extends MdecInputStream {

        // TODO: convert to use iterator?
        private int __iCurrentMacroBlock;
        private int __iCurrentBlock = 0;
        private int __iCurrentMdecCode = 0;

        private MdecReader(int iStartMacroBlock) {
            __iCurrentMacroBlock = iStartMacroBlock;
        }

        public boolean readMdecCode(@Nonnull MdecCode code) throws MdecException.EndOfStream  {

            MacroBlock currentMacBlk;
            try {
                currentMacBlk = _aoMacroBlocks[__iCurrentMacroBlock];
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new MdecException.EndOfStream(MdecException.inBlockOfBlocks(__iCurrentBlock, _aoMacroBlocks.length), ex);
            }

            Block currentBlk = currentMacBlk.getBlock(__iCurrentBlock);

            MdecCode c = currentBlk.getMdecCode(__iCurrentMdecCode);
            code.set(c);

            __iCurrentMdecCode++;

            // at end of block?
            boolean eob = false;
            if (__iCurrentMdecCode == currentBlk.getMdecCodeCount()) {
                __iCurrentMdecCode = 0;
                __iCurrentBlock++;
                eob = true;
                // at end of macroblock?
                if (__iCurrentBlock == 6) {
                    __iCurrentBlock = 0;
                    __iCurrentMacroBlock++;
                }
            }
            return eob;
        }

    }
    
    /*########################################################################*/
    /*## Beginning of instance ###############################################*/
    /*########################################################################*/
    
    /** An array to store the uncompressed data as MacroBlock structures. */
    @Nonnull
    private final MacroBlock[] _aoMacroBlocks;
    
    /** Width of the frame in pixels */
    private final int _iWidth;
    /** Height of the frame in pixels */
    private final int _iHeight;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public ParsedMdecImage(@Nonnull MdecInputStream mdecIn, int iWidth, int iHeight) 
            throws MdecException.EndOfStream, MdecException.ReadCorruption
    {
        
        // Save width and height
        _iWidth = iWidth;
        _iHeight = iHeight;

        // Calculate number of macro-blocks in the frame
        int iMacroBlockCount = Calc.macroblocks(iWidth, iHeight);

        // Set the array to match
        _aoMacroBlocks = new MacroBlock[iMacroBlockCount];

        readFrom(mdecIn);
    }
    
    private void readFrom(@Nonnull MdecInputStream mdecIn)
            throws MdecException.EndOfStream, MdecException.ReadCorruption
    {

        int iMacBlockWidth = Calc.macroblockDim(_iWidth);
        int iMacBlockHeight = Calc.macroblockDim(_iHeight);

        int iMacroBlockIndex = 0;
        for (int iMacBlkX = 0; iMacBlkX < iMacBlockWidth; iMacBlkX ++) {
            for (int iMacBlkY = 0; iMacBlkY < iMacBlockHeight; iMacBlkY ++) {
                LOG.log(Level.FINE, "Reading macroblock {0,number,#}", iMacroBlockIndex);

                _aoMacroBlocks[iMacroBlockIndex] = new MacroBlock(mdecIn);
                iMacroBlockIndex++;
            }
        }
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getMacroBlockCount() {
        return _aoMacroBlocks.length;
    }

    public @Nonnull MdecInputStream getStream() {
        return new MdecReader(0);
    }
    /** Get a stream starting at the specified macroblock. */
    public @Nonnull MdecInputStream getStream(int iMacBlkX, int iMacBlkY) {
        int iMacBlkHeight = Calc.macroblockDim(_iHeight);
        return new MdecReader(iMacBlkY + iMacBlkX * iMacBlkHeight);
    }

    public ArrayList<MdecCode> getMacroBlockCodes(int iMacBlkX, int iMacBlkY) {
        int iMacBlkHeight = Calc.macroblockDim(_iHeight);
        return _aoMacroBlocks[iMacBlkY + iMacBlkX * iMacBlkHeight].getCodes();
    }

    /** Returns shallow copy of the codes for the given block.
     * Modifying the codes will modify the source codes! */
    public @Nonnull ArrayList<MdecCode> getBlockCodes(int iMacBlkX, int iMacBlkY, int iBlock) {
        int iMacBlkHeight = Calc.macroblockDim(_iHeight);
        return _aoMacroBlocks[iMacBlkY + iMacBlkX * iMacBlkHeight].getBlock(iBlock).getCodes();
    }
    
    public int getMdecCodeCount() {
        int iMdecCodeCount = 0;
        for (MacroBlock mb : _aoMacroBlocks) {
            iMdecCodeCount += mb.getMdecCodeCount();
        }
        return iMdecCodeCount;
    }

    public @Nonnull String getBlockInfo(int iMacBlkX, int iMacBlkY, int iBlock) {
        int iMacBlkHeight = Calc.macroblockDim(_iHeight);
        return _aoMacroBlocks[iMacBlkY + iMacBlkX * iMacBlkHeight].getBlock(iBlock).toString();
    }

    public @Nonnull int[] getMacroBlockQscales(int iMacBlkX, int iMacBlkY) {
        int[] aiQscales = new int[6];
        int iMacBlkHeight = Calc.macroblockDim(_iHeight);
        MacroBlock mb = _aoMacroBlocks[iMacBlkY + iMacBlkX * iMacBlkHeight];
        for (int i = 0; i < aiQscales.length; i++) {
            aiQscales[i] = mb.getBlock(i).getQscale();
        }
        return aiQscales;
    }

}
