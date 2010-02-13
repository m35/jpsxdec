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

import jpsxdec.plugins.psx.video.PsxYuvImage;
import jpsxdec.formats.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import javax.imageio.ImageIO;
import jpsxdec.plugins.psx.video.DemuxImage;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;
import jpsxdec.plugins.psx.video.decode.UncompressionException;
import jpsxdec.util.IO;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor_STRv2;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor_STRv3;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder_double;
import jpsxdec.plugins.psx.video.mdec.MdecInputStream;
import jpsxdec.plugins.psx.video.mdec.idct.StephensIDCT;
import static jpsxdec.plugins.psx.video.mdec.MdecInputStream.REVERSE_ZIG_ZAG_SCAN_MATRIX;

public class MdecEncoder {

    private static final boolean DEBUG = false;

    private int[][] m_aaiYBlocks;
    private int[][] m_aaiCbBlocks;
    private int[][] m_aaiCrBlocks;
    private int m_iMacBlockWidth;
    private int m_iMacBlockHeight;
    private int m_iQscale;
    private StephensIDCT m_DCT = new StephensIDCT();

    private static final int[] ZIG_ZAG_SCAN_MATRIX = new int[REVERSE_ZIG_ZAG_SCAN_MATRIX.length];
    static {
        for (int i = 0; i < ZIG_ZAG_SCAN_MATRIX.length; i++) {
            ZIG_ZAG_SCAN_MATRIX[REVERSE_ZIG_ZAG_SCAN_MATRIX[i]] = i;
        }
        if (DEBUG) {
            for (int y = 0; y < 8; y++) {
                System.out.print("[ ");
                for (int x = 0; x < 8; x++) {
                    System.out.print(ZIG_ZAG_SCAN_MATRIX[x + y * 8] + " ");
                }
                System.out.println("]");
            }
        }
    }
    private static int[] PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX =
            MdecInputStream.getDefaultPsxQuantMatrixCopy();


    public MdecEncoder(PsxYuvImage oYuv, int iQscale) {

        m_iMacBlockWidth = oYuv.getLuminWidth() / 16;
        m_iMacBlockHeight = oYuv.getLuminHeight() / 16;
        m_iQscale = iQscale;

        m_aaiYBlocks = new int[m_iMacBlockWidth * m_iMacBlockHeight * 4][];
        m_aaiCbBlocks = new int[m_iMacBlockWidth * m_iMacBlockHeight][];
        m_aaiCrBlocks = new int[m_iMacBlockWidth * m_iMacBlockHeight][];

        int iBlock = 0;
        for (int iBlockX = 0; iBlockX < m_iMacBlockWidth*2; iBlockX++) {
            for (int iBlockY = 0; iBlockY < m_iMacBlockHeight*2; iBlockY++) {
                double[] adblBlock = oYuv.get8x8blockY(iBlockX*8, iBlockY*8);
                m_aaiYBlocks[iBlockX + iBlockY * m_iMacBlockWidth*2] = encodeBlock(adblBlock);
                iBlock++;
            }
        }
        iBlock = 0;
        for (int iBlockX = 0; iBlockX < m_iMacBlockWidth; iBlockX++) {
            for (int iBlockY = 0; iBlockY < m_iMacBlockHeight; iBlockY++) {
                double[] adblBlock = oYuv.get8x8blockCb(iBlockX*8, iBlockY*8);
                if (DEBUG)
                    System.out.println("Encoding macroblock " + iBlock + " Cb");
                m_aaiCbBlocks[iBlockX + iBlockY * m_iMacBlockWidth] = encodeBlock(adblBlock);
                if (DEBUG)
                    System.out.println("Encoding macroblock " + iBlock + " Cr");
                adblBlock = oYuv.get8x8blockCr(iBlockX*8, iBlockY*8);
                m_aaiCrBlocks[iBlockX + iBlockY * m_iMacBlockWidth] = encodeBlock(adblBlock);
                iBlock++;
            }
        }

    }

    private int[] encodeBlock(double[] adblBlock) {
        if (DEBUG) {
            System.out.println("Pre DCT");
            for (int y = 0; y < 8; y++) {
                System.out.print("[ ");
                for (int x = 0; x < 8; x++) {
                    System.out.format("%1.3f ", adblBlock[x + y * 8]);
                }
                System.out.println("]");
            }
        }

        // perform the discrete cosine transform
        m_DCT.forwardDCT(adblBlock);

        if (DEBUG) {
            System.out.println("Post DCT (Pre zig-zag & quant)");
            for (int y = 0; y < 8; y++) {
                System.out.print("[ ");
                for (int x = 0; x < 8; x++) {
                    System.out.format("%1.3f ", adblBlock[x + y * 8]);
                }
                System.out.println("]");
            }
        }

        int[] aiBlock = new int[8*8];
        // quantize it
        if (DEBUG)
            System.out.format("[0] = [0]%1.3f / %d = ", adblBlock[0], PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[0]);
        aiBlock[0] = (int)Math.round(adblBlock[0]
                     / (double)PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[0]);
        if (DEBUG)
            System.out.println(aiBlock[0]);
        for (int i = 1; i < ZIG_ZAG_SCAN_MATRIX.length; i++) {
            int iZigZagPos = REVERSE_ZIG_ZAG_SCAN_MATRIX[i];
            if (DEBUG) {
                System.out.format("[%d] = [%d]%1.3f / (%d * %d) * 8 = ",
                        i, iZigZagPos, adblBlock[iZigZagPos],
                        PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[iZigZagPos],
                        m_iQscale);
            }
            aiBlock[i] = (int)Math.round(adblBlock[iZigZagPos] * 8.0 /
                         (PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[iZigZagPos] * m_iQscale));
            if (DEBUG)
                System.out.println(aiBlock[i]);
        }

        if (DEBUG) {
            System.out.println("Final block");
            for (int y = 0; y < 8; y++) {
                System.out.print("[ ");
                for (int x = 0; x < 8; x++) {
                    System.out.print(aiBlock[x + y * 8] + " ");
                }
                System.out.println("]");
            }
        }

        return aiBlock;
    }

    public MdecInputStream getStream() {
        return new EncodedMdecInputStream();
    }

    private class EncodedMdecInputStream extends MdecInputStream {

        private int _iMacroBlockX=0, _iMacroBlockY=0, _iBlock=0, _iVectorPos=0;
        
        public boolean readMdecCode(MdecCode oCode) throws UncompressionException, EOFException {
            int[] aiBlock;
            switch (_iBlock) {
                case 0: aiBlock = m_aaiCrBlocks[ _iMacroBlockX +  _iMacroBlockY * m_iMacBlockWidth]; break;
                case 1: aiBlock = m_aaiCbBlocks[ _iMacroBlockX +  _iMacroBlockY * m_iMacBlockWidth]; break;
                case 2: aiBlock = m_aaiYBlocks[_iMacroBlockX * 2    +  _iMacroBlockY * 2     * m_iMacBlockWidth * 2]; break;
                case 3: aiBlock = m_aaiYBlocks[_iMacroBlockX * 2 +1 +  _iMacroBlockY * 2     * m_iMacBlockWidth * 2]; break;
                case 4: aiBlock = m_aaiYBlocks[_iMacroBlockX * 2    + (_iMacroBlockY * 2 +1) * m_iMacBlockWidth * 2]; break;
                case 5: aiBlock = m_aaiYBlocks[_iMacroBlockX * 2 +1 + (_iMacroBlockY * 2 +1) * m_iMacBlockWidth * 2]; break;
                default: throw new IllegalStateException();
            }

            if (_iVectorPos == 0) { // qscale & dc
                oCode.Top6Bits = m_iQscale;
                oCode.Bottom10Bits = aiBlock[0];
                _iVectorPos++;
                if (DEBUG)
                    System.out.println(oCode);
                return false;
            } else {
                int iZeroCount = 0;
                while (_iVectorPos < aiBlock.length && aiBlock[_iVectorPos] == 0) {
                    iZeroCount++;
                    _iVectorPos++;
                }

                if (_iVectorPos < aiBlock.length) {
                    oCode.Top6Bits = iZeroCount;
                    oCode.Bottom10Bits = aiBlock[_iVectorPos];
                    _iVectorPos++;
                    if (DEBUG)
                        System.out.println(oCode);
                    return false;
                } else { // end of block
                    oCode.Top6Bits = MdecInputStream.MDEC_END_OF_DATA_TOP6;
                    oCode.Bottom10Bits = MdecInputStream.MDEC_END_OF_DATA_BOTTOM10;
                    _iVectorPos = 0;

                    // increment indexes
                    _iBlock++;
                    if (_iBlock >= 6) {
                        _iBlock = 0;
                        _iMacroBlockY++;
                        if (_iMacroBlockY >= m_iMacBlockHeight) {
                            _iMacroBlockY = 0;
                            _iMacroBlockX++;
                        }
                    }
                    if (DEBUG)
                        System.out.println(oCode);
                    return true;
                }
            }
        }

    }

    public static void main(String args[]) throws Throwable {
        PsxYuvImage oYuv = convert();
        final int QSCALE = 3;
        BufferedImage bi = ImageIO.read(new File("abc000[0]0300.png"));

        BufferedImage bi2 = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi2.createGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        bi = bi2;

        RgbIntImage rgb = new RgbIntImage(bi);
        ImageIO.write(rgb.toBufferedImage(), "png", new File("rgbsteve.png"));

        //oYuv = new PsxYuvImage(bi);
        ImageIO.write(oYuv.toRgb().toBufferedImage(), "jpg", new File("yuvsteve.jpg"));

        MdecEncoder oEncoder = new MdecEncoder(oYuv, QSCALE);

        ParsedMdecImage oUncompressed = new ParsedMdecImage(bi.getWidth(), bi.getHeight());

        oUncompressed.readFrom(oEncoder.getStream());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitStreamWriter bsw = new BitStreamWriter(baos);
        DemuxFrameUncompressor_STRv2.FrameRecompressor_STRv2 compressor = new DemuxFrameUncompressor_STRv2.FrameRecompressor_STRv2();
        compressor.compressToDemux(bsw, QSCALE, oUncompressed.getRunLengthCodeCount());
        compressor.write(oUncompressed.getStream());
        bsw.close();
        IO.writeFile(String.format("newsteve%dx%d-v2.demux", oYuv.getLuminWidth(), oYuv.getLuminHeight()), baos.toByteArray());

        DemuxFrameUncompressor uncompressor = new DemuxFrameUncompressor_STRv2(baos.toByteArray(), 0);
        MdecDecoder_double decoder = new MdecDecoder_double(new StephensIDCT(), bi.getWidth(), bi.getHeight());
        decoder.decode(uncompressor);
        RgbIntImage oRgb = new RgbIntImage(bi.getWidth(), bi.getHeight());
        decoder.readDecodedRGB(oRgb);
        ImageIO.write(oRgb.toBufferedImage(), "png", new File("newsteve.png"));
    }

    private static PsxYuvImage convert() throws Throwable {
        DemuxImage oDemux = new DemuxImage(320, 160, new File("abc000[0]0300.demux"));

        DemuxFrameUncompressor_STRv3 oUncompress = new DemuxFrameUncompressor_STRv3(oDemux.getData(), 0);
        MdecDecoder_double decoder = new MdecDecoder_double(new StephensIDCT(), oDemux.getWidth(), oDemux.getHeight());
        decoder.decode(oUncompress);
        RgbIntImage rgb = new RgbIntImage(oDemux.getWidth(), oDemux.getHeight());
        decoder.readDecodedRGB(rgb);
        ImageIO.write(rgb.toBufferedImage(), "png", new File("abc000[0]0300.png"));

        PsxYuvImage oYuv = new PsxYuvImage(oDemux.getWidth(), oDemux.getHeight());
        //decoder.readDecodedPsxYuv(oYuv);
        return oYuv;
    }
}
