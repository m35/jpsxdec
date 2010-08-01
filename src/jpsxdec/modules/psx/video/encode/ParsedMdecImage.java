/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.modules.psx.video.encode;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.modules.psx.video.mdec.DecodingException;
import jpsxdec.modules.psx.video.mdec.MdecInputStream;
import jpsxdec.modules.psx.video.mdec.MdecInputStream.MdecCode;

/** Parses and stores a stream of MDEC 16-bit codes, in the process is analyzes
 *  it for information necessay for recompression. It also has methods to
 *  tweak individual macro-blocks, which is also useful for recompression. */
public class ParsedMdecImage  {
    
    private static final Logger log = Logger.getLogger(ParsedMdecImage.class.getName());

    public static class MacroBlock implements Iterable<Block> {
        public Block Cr;
        public Block Cb;
        public Block Y1;
        public Block Y2;
        public Block Y3;
        public Block Y4;
        
        public void replaceBlock(Block oBlk) {
            switch (oBlk.getIndex()) {
                case 0: Cr = oBlk; break;
                case 1: Cb = oBlk; break;
                case 2: Y1 = oBlk; break;
                case 3: Y2 = oBlk; break;
                case 4: Y3 = oBlk; break;
                case 5: Y4 = oBlk; break;
            }
        }
        
        public Block getBlockCopy(String sBlk) {
            for (int i = 0; i < Block.BLOCK_NAMES.length; i++) {
                if (Block.BLOCK_NAMES[i].equals(sBlk))
                    return getBlockCopy(i);
            }
            throw new IllegalArgumentException("Invalid block name " + sBlk);
        }
        
        public Block getBlockCopy(int iBlk) {
            switch (iBlk) {
                case 0: return Cr.clone();
                case 1: return Cb.clone();
                case 2: return Y1.clone();
                case 3: return Y2.clone();
                case 4: return Y3.clone();
                case 5: return Y4.clone();
                default: 
                    throw new IllegalArgumentException("Invalid block number " + iBlk);
            }
        }
        
        public long getMdecCodeCount() {
            return Cr.getMdecCodeCount() +
                   Cb.getMdecCodeCount() +
                   Y1.getMdecCodeCount() +
                   Y2.getMdecCodeCount() +
                   Y3.getMdecCodeCount() +
                   Y4.getMdecCodeCount();
        }

        @Override
        public MacroBlock clone() {
            MacroBlock oNew = new MacroBlock();
            oNew.Cr = (Block)Cr.clone();
            oNew.Cb = (Block)Cb.clone();
            oNew.Y1 = (Block)Y1.clone();
            oNew.Y2 = (Block)Y2.clone();
            oNew.Y3 = (Block)Y3.clone();
            oNew.Y4 = (Block)Y4.clone();
            return oNew;
        }

        public Iterator<Block> iterator() {
            return new Iterator<Block>() {

                private int m_iBlk = 0;
                
                public boolean hasNext() {
                    return m_iBlk < 6;
                }

                public Block next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return getBlockCopy(m_iBlk++);
                }

                public void remove() {
                    throw new UnsupportedOperationException("Can't remove macroblocks from list.");
                }
            };
        }

    }
    
    public static class Block {

        private static final String[] BLOCK_NAMES = {
            "Cr", "Cb", "Y1", "Y2", "Y3", "Y4"
        };

        public MdecCode DCCoefficient;
        public MdecCode[] ACCoefficients;
        public MdecCode EndOfBlock;
        private int _iIndex;

        public Block(int iIndex) {
            if (iIndex < 0 || iIndex >= 6)
                throw new IllegalArgumentException("Invalid block index " + iIndex);
            _iIndex = iIndex;
        }

        public String getName() {
            return BLOCK_NAMES[_iIndex];
        }

        public MdecCode getMdecCode(int i) {
            if (i == 0)
                return DCCoefficient;
            else if (i >= 1 && i <= ACCoefficients.length)
                return ACCoefficients[i-1];
            else if (i == ACCoefficients.length+1)
                return EndOfBlock;
            else
                throw new IllegalArgumentException("Invalid MDEC code index " + i);
        }
        
        public int getMdecCodeCount() {
            return ACCoefficients.length + 2;
        }
        
        @Override
        public Block clone() {
            Block oNew = new Block(_iIndex);
            oNew.DCCoefficient = DCCoefficient.clone();
            oNew.ACCoefficients = new MdecCode[ACCoefficients.length];
            for (int i = 0; i < ACCoefficients.length; i++)
                oNew.ACCoefficients[i] = ACCoefficients[i].clone();
            oNew.EndOfBlock = EndOfBlock.clone();
            return oNew;
        }

        public boolean isChrom() {
            return _iIndex < 2;
        }

        public boolean isLumin() {
            return _iIndex >= 2;
        }

        public int getIndex() {
            return _iIndex;
        }

        /** An exciting function that changes the quantization scale of a block
         *  in a macro block. */
        public void changeQuantizationScale(int iNewScale) {
            final int iBlksCurScale = DCCoefficient.getTop6Bits();
            // update the block's Qscale
            DCCoefficient.setTop6Bits(iNewScale);

            // we don't need to scale the DC too because it's not multiplied by the qscale
            // DCCoefficient.Bottom10Bits = (int)
            //         Math.round(DCCoefficient.Bottom10Bits * iBlksCurScale / (double)iNewScale);

            // copy array into arraylist
            ArrayList<MdecCode> acCodes =
                    new ArrayList<MdecCode>(ACCoefficients.length);
            for (MdecCode oMdec : ACCoefficients) {
                acCodes.add(oMdec);
            }

            int i = 0;
            while (i < acCodes.size()) {
                MdecCode code = acCodes.get(i);

                // scale the AC coefficient
                code.setBottom10Bits( (int)( Math.round(
                        code.getBottom10Bits() * iBlksCurScale / (double)iNewScale
                        )) );

                // if the AC coefficient becomes zero
                // (i.e. if code becomes (#, 0) ), we should remove this code
                if (code.getBottom10Bits() == 0) {
                    acCodes.remove(i);
                    // update the next code (if any) with the removed code's run + 1
                    if (i < acCodes.size()) {
                        MdecCode nextCode = acCodes.get(i);
                        nextCode.setTop6Bits(nextCode.getTop6Bits() + code.getTop6Bits() + 1);
                    }
                } else {
                    // next code
                    i++;
                }
            }

            // update the AC coefficients in the block
            ACCoefficients = acCodes.toArray(new MdecCode[acCodes.size()]);
        }

        public int getQscale() {
            return DCCoefficient.getTop6Bits();
        }

    }


    private class MdecReader extends MdecInputStream {

        int _iCurrentMacroBlock;
        int _iCurrentBlock = 0;
        int _iCurrentMdecCode = 0;

        private MdecReader(int iStartMacroBlock) {
            _iCurrentMacroBlock = iStartMacroBlock;
        }

        public boolean readMdecCode(MdecCode code) throws EOFException  {

            MacroBlock currentMacBlk;
            try {
                currentMacBlk = _mdecList[_iCurrentMacroBlock];
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException();
            }

            Block currentBlk = currentMacBlk.getBlockCopy(_iCurrentBlock);

            MdecCode c = currentBlk.getMdecCode(_iCurrentMdecCode);
            code.set(c);

            _iCurrentMdecCode++;

            // end of block?
            boolean eob = false;
            if (_iCurrentMdecCode == currentBlk.getMdecCodeCount()) {
                _iCurrentMdecCode = 0;
                _iCurrentBlock++;
                eob = true;
                // end of macroblock?
                if (_iCurrentBlock == 6) {
                    _iCurrentBlock = 0;
                    _iCurrentMacroBlock++;
                }
            }
            return eob;
        }

    }
    
    /*########################################################################*/
    /*## Beginning of instance ###############################################*/
    /*########################################################################*/
    
    /** An array to store the uncompressed data as MacroBlock structures. */
    protected MacroBlock[] _mdecList;
    
    /** Width of the frame in pixels */
    protected int _iWidth;
    /** Height of the frame in pixels */
    protected int _iHeight;

    protected int _iLuminQscale;

    protected int _iChromQscale;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public ParsedMdecImage(int iWidth, int iHeight) {
        
        // Save width and height
        _iWidth = iWidth;
        _iHeight = iHeight;

        // Calculate number of macro-blocks in the frame
        int iMacroBlockCount = calculateMacroBlocks(iWidth, iHeight);

        // Set the array to match
        _mdecList = new MacroBlock[iMacroBlockCount];

        if (log.isLoggable(Level.FINE))
            log.fine("Expecting " + iMacroBlockCount + " macroblocks");

    }
    

    public static int calculateMacroBlocks(int iWidth, int iHeight) {
        // Actual width/height in macroblocks 
        // (since you can't have a partial macroblock)
        int iActualWidth, iActualHeight;
        
        if ((iWidth % 16) > 0)
            iActualWidth = (iWidth / 16 + 1) * 16;
        else
            iActualWidth = iWidth;
        
        if ((iHeight % 16) > 0)
            iActualHeight = (iHeight / 16 + 1) * 16;
        else
            iActualHeight = iHeight;
        
        // Calculate number of macro-blocks in the frame
        return (iActualWidth / 16) * (iActualHeight / 16);
    }

    /** A strange value needed for video bitstreams and video sector headers.
     *  It's the number of MDEC codes, divided by two, then rounded up to the
     *  next closest multiple of 32 (if not already a multiple of 32).  */
    public static short calculateHalfCeiling32(int iMdecCodeCount) {
        return (short) ((((iMdecCodeCount + 1) / 2) + 31) & ~31);
    }

    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }
        
    public MacroBlock getMacroBlock(int x, int y) {
        int iMacBlkHeight = (_iHeight + 15) / 16;
        return _mdecList[y + x * iMacBlkHeight];
    }

    public MdecInputStream getStream() {
        return new MdecReader(0);
    }
    public MdecInputStream getStream(int iMacBlkX, int iMaxBlkY) {
        int iMacBlkHeight = (_iHeight + 15) / 16;
        return new MdecReader(iMaxBlkY + iMacBlkX * iMacBlkHeight);
    }
    
    public int getMdecCodeCount() {
        int iMdecCodeCount = 0;
        for (int i=0; i < _mdecList.length; i++) {
            iMdecCodeCount += _mdecList[i].getMdecCodeCount();
        }
        return iMdecCodeCount;
    }

    public int getChromQscale() {
        return _iChromQscale;
    }

    public int getLuminQscale() {
        return _iLuminQscale;
    }

    public void readFrom(MdecInputStream mdecIn)
            throws DecodingException, EOFException
    {
        _iChromQscale = -1;
        _iLuminQscale = -1;

        ArrayList<MdecCode> acCoefficients = new ArrayList<MdecCode>();

        int iMacBlockWidth = (_iWidth + 15) / 16;
        int iMacBlockHeight = (_iHeight + 15) / 16;

        int iMacroBlockIndex = 0;

        for (int iMacBlkX = 0; iMacBlkX < iMacBlockWidth; iMacBlkX ++)
        {
            for (int iMacBlkY = 0; iMacBlkY < iMacBlockHeight; iMacBlkY ++)
            {
                if (log.isLoggable(Level.FINE))
                    log.fine("Reading macroblock " + iMacroBlockIndex);

                MacroBlock thisMacBlk;
                _mdecList[iMacroBlockIndex] = thisMacBlk = new MacroBlock();
                
                for (int iBlock = 0; iBlock < 6; iBlock++) {
                    Block thisBlk;
                    thisMacBlk.replaceBlock(thisBlk = new Block(iBlock));
                    
                    MdecCode qscaleDC = new MdecCode();
                    mdecIn.readMdecCode(qscaleDC);
                    thisBlk.DCCoefficient = qscaleDC;
                    if (thisBlk.isChrom()) {
                        if (_iChromQscale < 0)
                            _iChromQscale = qscaleDC.getTop6Bits();
                        else if (_iChromQscale != qscaleDC.getTop6Bits())
                            log.warning("Chrominance q-scale changed mid frame!");
                    } else {
                        if (_iLuminQscale < 0)
                            _iLuminQscale = qscaleDC.getTop6Bits();
                        else if (_iLuminQscale != qscaleDC.getTop6Bits())
                            log.warning("Luminance q-scale changed mid frame!");
                    }

                    acCoefficients.clear();
                    MdecCode code;
                    while (!mdecIn.readMdecCode(code = new MdecCode())) {
                        acCoefficients.add(code);
                    }
                    thisBlk.ACCoefficients = acCoefficients.toArray(new MdecCode[acCoefficients.size()]);

                    thisBlk.EndOfBlock = code;

                    log.finest("EOB");
                }

                iMacroBlockIndex++;
            }
        }
    }

}
