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

package jpsxdec.plugins.psx.video.encode;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.plugins.psx.video.decode.UncompressionException;
import jpsxdec.util.IO;
import jpsxdec.plugins.psx.video.mdec.MdecInputStream;
import jpsxdec.plugins.psx.video.mdec.MdecInputStream.MdecCode;

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
        
        public void setBlock(Block oBlk) {
            switch (oBlk.getIndex()) {
                case 0: Cr = oBlk; break;
                case 1: Cb = oBlk; break;
                case 2: Y1 = oBlk; break;
                case 3: Y2 = oBlk; break;
                case 4: Y3 = oBlk; break;
                case 5: Y4 = oBlk; break;
            }
        }
        
        public Block getBlock(String sBlk) {
            for (int i = 0; i < Block.BLOCK_NAMES.length; i++) {
                if (Block.BLOCK_NAMES[i].equals(sBlk))
                    return getBlock(i);
            }
            throw new IllegalArgumentException("Invalid block name " + sBlk);
        }
        
        public Block getBlock(int iBlk) {
            switch (iBlk) {
                case 0: return Cr;
                case 1: return Cb;
                case 2: return Y1;
                case 3: return Y2;
                case 4: return Y3;
                case 5: return Y4;
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
                    return getBlock(m_iBlk++);
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
            oNew.DCCoefficient = (MdecCode)DCCoefficient.clone();
            oNew.ACCoefficients = new MdecCode[ACCoefficients.length];
            for (int i = 0; i < ACCoefficients.length; i++)
                oNew.ACCoefficients[i] = (MdecCode)ACCoefficients[i].clone();
            oNew.EndOfBlock = (MdecCode)EndOfBlock.clone();
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
            final int iBlksCurScale = DCCoefficient.Top6Bits;
            // update the block's Qscale
            DCCoefficient.Top6Bits = iNewScale;

            // we don't need to scale the DC too because it's not multiplied by the qscale...right?
            // DCCoefficient.Bottom10Bits = (int)
            //         Math.round(DCCoefficient.Bottom10Bits * iBlksCurScale / (double)iNewScale);

            // copy array into arraylist
            ArrayList<MdecCode> oACCodes =
                    new ArrayList<MdecCode>(ACCoefficients.length);
            for (MdecCode oMdec : ACCoefficients) {
                oACCodes.add(oMdec);
            }

            int i = 0;
            while (i < oACCodes.size()) {
                MdecCode oMdecCode = oACCodes.get(i);

                // scale the AC coefficient
                oMdecCode.Bottom10Bits = (int)(
                    Math.round(oMdecCode.Bottom10Bits * iBlksCurScale / (double)iNewScale ));

                // if the AC coefficient becomes zero
                // (i.e. if code becomes (#, 0) ), we should remove this code
                if (oMdecCode.Bottom10Bits == 0) {
                    oACCodes.remove(i);
                    // update the next code (if any) with the removed code's run + 1
                    if (i < oACCodes.size())
                        oACCodes.get(i).Top6Bits += oMdecCode.Top6Bits + 1;
                } else {
                    // next code
                    i++;
                }
            }

            // update the AC coefficients in the block
            ACCoefficients = oACCodes.toArray(new MdecCode[oACCodes.size()]);
        }

        public int getQscale() {
            return DCCoefficient.Top6Bits;
        }

    }


    private class MdecReader extends MdecInputStream {

        int m_iCurrentMacroBlock;
        int m_iCurrentBlock = 0;
        int m_iCurrentMdecCode = 0;

        private MdecReader(int iStartMacroBlock) {
            m_iCurrentMacroBlock = iStartMacroBlock;
        }

        public boolean readMdecCode(MdecCode oCode) throws EOFException  {

            MacroBlock oCurMacBlk;
            try {
                oCurMacBlk = m_oMdecList[m_iCurrentMacroBlock];
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException();
            }

            Block oCurBlk = oCurMacBlk.getBlock(m_iCurrentBlock);

            MdecCode c = oCurBlk.getMdecCode(m_iCurrentMdecCode);
            oCode.Bottom10Bits = c.Bottom10Bits;
            oCode.Top6Bits = c.Top6Bits;

            m_iCurrentMdecCode++;

            // end of block?
            boolean eob = false;
            if (m_iCurrentMdecCode == oCurBlk.getMdecCodeCount()) {
                m_iCurrentMdecCode = 0;
                m_iCurrentBlock++;
                eob = true;
                // end of macroblock?
                if (m_iCurrentBlock == 6) {
                    m_iCurrentBlock = 0;
                    m_iCurrentMacroBlock++;
                }
            }
            return eob;
        }

    }
    
    /*########################################################################*/
    /*## Beginning of instance ###############################################*/
    /*########################################################################*/
    
    /** An array to store the uncompressed data as MacroBlock structures. */
    protected MacroBlock[] m_oMdecList;
    
    /** Width of the frame in pixels */
    protected int m_iWidth;
    /** Height of the frame in pixels */
    protected int m_iHeight;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public ParsedMdecImage(int iWidth, int iHeight) {
        
        // Save width and height
        m_iWidth = iWidth;
        m_iHeight = iHeight;

        // Calculate number of macro-blocks in the frame
        int iMacroBlockCount = calculateMacroBlocks(iWidth, iHeight);

        // Set the array to match
        m_oMdecList = new MacroBlock[iMacroBlockCount];

        if (log.isLoggable(Level.FINE))
            log.fine("Expecting " + iMacroBlockCount + " macroblocks");

    }
    

    protected static int calculateMacroBlocks(int iWidth, int iHeight) {
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

    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public int getWidth() {
        return m_iWidth;
    }

    public int getHeight() {
        return m_iHeight;
    }
        
    public MacroBlock getMacroBlock(int x, int y) {
        int iMacBlkHeight = (m_iHeight + 15) / 16;
        return m_oMdecList[y + x * iMacBlkHeight];
    }

    public MdecInputStream getStream() {
        return new MdecReader(0);
    }
    public MdecInputStream getStream(int x, int y) {
        int iMacBlkHeight = (m_iHeight + 15) / 16;
        return new MdecReader(y + x * iMacBlkHeight);
    }
    
    public int getRunLengthCodeCount() {
        int lngMacroBlocks = 0;
        for (int i=0; i < m_oMdecList.length; i++) {
            lngMacroBlocks += m_oMdecList[i].getMdecCodeCount();
        }
        return lngMacroBlocks;
    }

    public int getFirstChromQscale() {
        return m_oMdecList[0].Cr.getQscale();
    }

    public void readFrom(MdecInputStream oUncompress) 
            throws UncompressionException, EOFException
    {

        ArrayList<MdecCode> oACCoefficients = new ArrayList<MdecCode>();

        int iMacBlockWidth = (m_iWidth + 15) / 16;
        int iMacBlockHeight = (m_iHeight + 15) / 16;

        int iMacroBlockIndex = 0;

        for (int iMacBlkX = 0; iMacBlkX < iMacBlockWidth; iMacBlkX ++)
        {
            for (int iMacBlkY = 0; iMacBlkY < iMacBlockHeight; iMacBlkY ++)
            {
                if (log.isLoggable(Level.FINE))
                    log.fine("Decoding macroblock " + iMacroBlockIndex);

                MacroBlock oThisMacBlk;
                m_oMdecList[iMacroBlockIndex] = oThisMacBlk = new MacroBlock();
                
                for (int iBlock = 0; iBlock < 6; iBlock++) {
                    Block oThisBlk;
                    oThisMacBlk.setBlock(oThisBlk = new Block(iBlock));
                    
                    MdecCode oQscaleDC = new MdecCode();
                    oUncompress.readMdecCode(oQscaleDC);
                    oThisBlk.DCCoefficient = oQscaleDC;

                    oACCoefficients.clear();
                    MdecCode oCode;
                    while (!oUncompress.readMdecCode(oCode = new MdecCode())) {
                        oACCoefficients.add(oCode);
                    }
                    oThisBlk.ACCoefficients = oACCoefficients.toArray(new MdecCode[oACCoefficients.size()]);

                    oThisBlk.EndOfBlock = oCode;

                    log.finest("EOB");
                }

                iMacroBlockIndex++;
            }
        }
    }

}
