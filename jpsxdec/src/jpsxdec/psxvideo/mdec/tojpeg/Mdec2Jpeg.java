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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.Version;
import jpsxdec.i18n.I;
import jpsxdec.psxvideo.mdec.Ac0Checker;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecBlock;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecContext;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/**
 * Directly translate MDEC stream into JPEG image with near lossless quality.
 *<p>
 * The generated JPEG is extremely similar to the original MDEC image.
 * There are 3 points where there are minor quality/accuracy loss.
 *<ol>
 * <li>The way MDEC (and MPEG-1) do quantization is different from JPEG,
 * so up to 1/8 of a value is lost. However this lost value is common in most
 * MPEG1 decoders, so it could be considered in acceptable range.
 * <li>The PSX YCbCr -> RGB conversion is very slightly different for PSX.
 * ffmpeg uses the standard JPEG conversion, so this class will generate
 * at least equal quality to ffmpeg.
 * <li>The PSX IDCT is unique. It appears using a different IDCT can have an
 * effect on output quality. jPSXdec is the only decoder that tries to match
 * the PSX IDCT. By translating to JPEG, you're at the mercy of whatever
 * implementation other programs use.
 *</ol>
 * Now by converting to JPEG, you can take advantage of other JPEG tools that
 * could help improve JPEG artifacts. These JPEGS also maximize the quality
 * to size ratio. While other conversion options can have better quality/
 * accuracy, they also generate larger files.
 *<p>
 * The generated JPEGs have the following properties:
 *<ul>
 * <li>The JPEG generated has 4:2:0 sampling, like the PSX does. In JPEG terms,
 * this is also known as 2x1 sampling.
 * <li>The JPEG has the default Huffman tables: 1 DC and 1 AC Huffman table for
 * Luma (Y), and 1 DC and 1 AC Huffman table shared between the two Chromas (CbCr).
 * The default tables must be used because VirtualDub has decreed it as such
 * (other MJPEG players don't complain).
 * <li>The JPEG has 1 quantization table for both luma and chroma.
 *</ul>
 * Translating from JPEG to MDEC could be doable, but not as simple or lossless.
 *<p>
 * This class could easily be adapted to translate MPEG-1 I-frames. That
 * usefulness is questionable since it's uncommon for MPEG-1 movies to
 * contain only I-frames, and MPEG-1 uses the limited YCbCr ranges while
 * JPEG/JFIF use the full [0-255] range.
 */
public class Mdec2Jpeg {

    private static final Logger LOG = Logger.getLogger(Mdec2Jpeg.class.getName());

    //--------------------------------------------------------------------------
    //-- CONSTANTS
    //--------------------------------------------------------------------------

    /** Special JPEG quantization table.
     *
     * The MDEC quantization table.
     * <pre>
     * [  2, 16, 19, 22, 26, 27, 29, 34 ]
     * [ 16, 16, 22, 24, 27, 29, 34, 37 ]
     * [ 19, 22, 26, 27, 29, 34, 34, 38 ]
     * [ 22, 22, 26, 27, 29, 34, 37, 40 ]
     * [ 22, 26, 27, 29, 32, 35, 40, 48 ]
     * [ 26, 27, 29, 32, 35, 40, 48, 58 ]
     * [ 26, 27, 29, 34, 38, 46, 56, 69 ]
     * [ 27, 29, 35, 38, 46, 56, 69, 83 ]
     *</pre>
     *
     * MPEG1/MDEC use fixed-point math, where 3 bits are the fractional part.
     * So after multiplying by the above table, the value is then divided by 8
     * to get the integer part.
     * JPEG by design just uses integer values, so we're faced with a dilemma.
     * Are we stuck chopping off those bits so we can start using integer values?
     *
     * Unfortunately yes.
     *
     * So using a JPEG quantization table of all 1's would be perfectly reasonable
     * since that's the best we're going to get quality wise.
     *
     * In the lucky cases where values are evenly divisible by 8, there will
     * be no loss in precision. Otherwise, farewell bits...
     *
     * But while we can't improve the quality of the resulting JPEG, we can
     * slightly reduce its size.
     * Note some values in the MPEG1/MDEC quantization table are already
     * divisible by 8.
     * <pre>
     * [  2, 16* 19, 22, 26, 27, 29, 34 ]
     * [ 16* 16* 22, 24, 27, 29, 34, 37 ]
     * [ 19, 22, 26, 27, 29, 34, 34, 38 ]
     * [ 22, 22, 26, 27, 29, 34, 37, 40*]
     * [ 22, 26, 27, 29, 32* 35, 40* 48*]
     * [ 26, 27, 29, 32* 35, 40* 48* 58 ]
     * [ 26, 27, 29, 34, 38, 46, 56* 69 ]
     * [ 27, 29, 35, 38, 46, 56* 69, 83 ]
     *</pre>
     * If we use a JPEG quantization of 1, in these cases we already know the
     * bottom 3 bits will be zero. So why not use a better JPEG quantization
     * than 1?
     *
     * So for most, the quantization value will be 1, but for those lucky values,
     * the quantization value will be MDEC/8. Which leaves us this:
     * <pre>
     *  [ 2* 2, 1, 1, 1, 1, 1, 1 ]
     *  [ 2, 2, 1, 3, 1, 1, 1, 1 ]
     *  [ 1, 1, 1, 1, 1, 1, 1, 1 ]
     *  [ 1, 1, 1, 1, 1, 1, 1, 5 ]
     *  [ 1, 1, 1, 1, 4, 1, 5, 6 ]
     *  [ 1, 1, 1, 4, 1, 5, 6, 1 ]
     *  [ 1, 1, 1, 1, 1, 1, 7, 1 ]
     *  [ 1, 1, 1, 1, 1, 7, 1, 1 ]
     * </pre>
     * Note that the DC coefficient in the top left is not divided by 8 so
     * remains the same.
     *
     * The math below does all these checks and calculates this table.
     *
     * During the quantization conversion, we can check if the value is 1,
     * then calculate normally and cut off those 3 bits. If not 1,
     * then skip the division by 8 since it's already done.
     */

    /*
    TODO: can optimize output size even more by checking all values through
    the image and see if their greatest common denominator is larger than 1.
    If so, we can use that instead of just 1.
    (along the way we could also check for data errors ahead of time)
    But that is a ton of work with likely only a tiny reduction in size.
    */
    private static final int[] JPEG_QUANTIZATION_TABLE_ZIGZAG = new int[64];

    /** PSX standard quantization table in zig-zag order. */
    private static final int[] PSX_QUANTIZATION_TABLE_ZIGZAG = new int[64];
    static {
        for (int i = 0; i < PSX_QUANTIZATION_TABLE_ZIGZAG.length; i++) {
            int iQVal = MdecInputStream.PSX_DEFAULT_QUANTIZATION_MATRIX[MdecInputStream.REVERSE_ZIG_ZAG_LOOKUP_LIST[i]];
            PSX_QUANTIZATION_TABLE_ZIGZAG[i] = iQVal;
            if (i == 0)
                JPEG_QUANTIZATION_TABLE_ZIGZAG[i] = iQVal;
            else if ((iQVal % 8) == 0)
                JPEG_QUANTIZATION_TABLE_ZIGZAG[i] = iQVal / 8;
            else
                JPEG_QUANTIZATION_TABLE_ZIGZAG[i] = 1;
        }
        assert verifyTable(JPEG_QUANTIZATION_TABLE_ZIGZAG);
    }

    /** Because I don't trust myself. */
    private static boolean verifyTable(int[] aiActual) {
        int[] aiExpected = {
            2, 2, 1, 1, 1, 1, 1, 1,
            2, 2, 1, 3, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 5,
            1, 1, 1, 1, 4, 1, 5, 6,
            1, 1, 1, 4, 1, 5, 6, 1,
            1, 1, 1, 1, 1, 1, 7, 1,
            1, 1, 1, 1, 1, 7, 1, 1,
        };
        for (int i = 0; i < aiExpected.length; i++) {
            if (aiExpected[MdecInputStream.REVERSE_ZIG_ZAG_LOOKUP_LIST[i]] != aiActual[i])
                return false;
        }
        return aiExpected.length == aiActual.length;
    }

    private static final byte[] COMMENT_BYTES;
    static {
        String comment = "Generated by "+I.JPSXDEC_VERSION_NON_COMMERCIAL(Version.Version).getEnglishMessage()+" MDEC to JPEG translator";
        COMMENT_BYTES = Misc.stringToAscii(comment);
    }

    private static @Nonnull byte[] getCommentBytes() {
        String sMeta = System.getProperty("meta");
        if (sMeta == null)
            return COMMENT_BYTES;
        else
            return Misc.stringToAscii("Generated by "+sMeta+" MDEC to JPEG translator");
    }

    /** Start of image */
    private static final int SOI  = 0xD8;
    /** Reserved for application segments */
    private static final int APP0 = 0xE0;
    /** Comment */
    private static final int COM = 0xFE;
    /** Define quantization table(s) */
    private static final int DQT  = 0xDB;
    /** Define huffman table(s) */
    static final int DHT = 0xC4;
    /** Start of frame */
    private static final int SOF0 = 0xC0;
    /** Start of scan */
    private static final int SOS  = 0xDA;
    /** End of image */
    private static final int EOI  = 0xD9;

    private static final int PRECISION = 8;

    private static final int NUM_COMPONENTS = 3;
    private static final int JPEG_Y_COMPONENT  = 0;
    private static final int JPEG_CB_COMPONENT = 1;
    private static final int JPEG_CR_COMPONENT = 2;

    //--------------------------------------------------------------------------
    //-- FIELDS
    //--------------------------------------------------------------------------

    private final Component[] _aoComponents = new Component[NUM_COMPONENTS];

    private final int _iPixelWidth, _iPixelHeight;
    private final int _iMacBlockWidth, _iMacBlockHeight;
    private final int _iTotalMacBlocks;

    /** Huffman tables as they will be written to the DHT block. */
    private final HuffmanTable[] _aoDhtTables = {
        HuffmanTable.DEFAULT_DC_LUMA_HUFFMAN,
        HuffmanTable.DEFAULT_DC_CHROMA_HUFFMAN,
        HuffmanTable.DEFAULT_AC_LUMA_HUFFMAN,
        HuffmanTable.DEFAULT_AC_CHROMA_HUFFMAN
    };
    /** DC huffman tables in order of index. */
    private final HuffmanTable[] _aoDcHuffmanTables = new HuffmanTable[2];
    /** AC huffman tables in order of index. */
    private final HuffmanTable[] _aoAcHuffmanTables = new HuffmanTable[2];

    /** Bit stream to temporarily wrap the output stream. */
    private final JpegBitOutputStream _jpegStream = new JpegBitOutputStream();

    public Mdec2Jpeg(int iPixelWidth, int iPixelHeight) {
        _iPixelWidth  = iPixelWidth;
        _iPixelHeight = iPixelHeight;
        _iMacBlockWidth  = Calc.macroblockDim(iPixelWidth);
        _iMacBlockHeight = Calc.macroblockDim(iPixelHeight);
        _iTotalMacBlocks = _iMacBlockWidth * _iMacBlockHeight;

        _aoComponents[JPEG_Y_COMPONENT]  = new Component(1, 0, 2, 2, 0, 0, _iMacBlockWidth, _iMacBlockHeight);
        _aoComponents[JPEG_CB_COMPONENT] = new Component(2, 0, 1, 1, 1, 1, _iMacBlockWidth, _iMacBlockHeight);
        _aoComponents[JPEG_CR_COMPONENT] = new Component(3, 0, 1, 1, 1, 1, _iMacBlockWidth, _iMacBlockHeight);

        HuffmanTable.initializeHuffmanTables(_aoDhtTables, _aoDcHuffmanTables, _aoAcHuffmanTables);
    }


    /** Reads and buffers the MDEC data.
     * @throws MdecException.TooMuchEnergy if the source stream has too much energy
     *                                     to save with this current implementation.
     */
    public void readMdec(MdecInputStream mdecInStream)
            throws MdecException.TooMuchEnergy, MdecException.ReadCorruption,
            MdecException.EndOfStream
    {
        // while jpgs with AC=0 codes seem to be fine, still would like to avoid it
        Ac0Checker cleanStream = Ac0Checker.wrapWithChecker(mdecInStream, true);

        final MdecCode code = new MdecCode();

        for (Component comp : _aoComponents) {
            Arrays.fill(comp.DctCoffZZ, 0);
            comp.WriteIndex = 0;
        }

        MdecContext context = new MdecContext(_iMacBlockHeight);

        // decode all the macro blocks of the image
        while (context.getTotalMacroBlocksRead() < _iTotalMacBlocks) {

            for (MdecBlock block : MdecBlock.list()) {
                Component comp;
                if (block == MdecBlock.Cr)
                    comp = _aoComponents[JPEG_CR_COMPONENT];
                else if (block == MdecBlock.Cb)
                    comp = _aoComponents[JPEG_CB_COMPONENT];
                else
                    comp = _aoComponents[JPEG_Y_COMPONENT];

                cleanStream.readMdecCode(code);

                // normally would multiply by PSX_QUANTIZATION_TABLE_ZIGZAG[0]
                // but JPEG_QUANTIZATION_TABLE_ZIGZAG[0] will take care of that
                comp.DctCoffZZ[comp.WriteIndex] = code.getBottom10Bits();
                // note that so long as the MDEC codes are valid,
                // the DC diff can never overflow the 11 bits it must fit in
                //      MDEC DC 10 bit: -512 to 511
                //      max diff = 511 + 512 = 1023
                //      11 bits: +/- 2047

                final int iCurrentBlockQscale = code.getTop6Bits();
                int iCurrentBlockVectorPosition = 0;

                while (!cleanStream.readMdecCode(code)) {

                    ////////////////////////////////////////////////////////
                    iCurrentBlockVectorPosition += code.getTop6Bits() + 1;

                    if (iCurrentBlockVectorPosition >= 64) {
                        MdecContext.MacroBlockPixel macBlkXY = context.getMacroBlockPixel();
                        throw new MdecException.ReadCorruption(MdecException.RLC_OOB_IN_MB_XY_BLOCK(
                                       iCurrentBlockVectorPosition,
                                       context.getTotalMacroBlocksRead(), macBlkXY.x, macBlkXY.y, context.getCurrentBlock().ordinal()));
                    }

                    // Dequantize
                    int iJpegQScale = JPEG_QUANTIZATION_TABLE_ZIGZAG[iCurrentBlockVectorPosition];
                    int iVal;
                    if (iJpegQScale == 1) {
                        // The JPEG quantization scale is 1, so simply do the
                        // math and cut off the bottom 3 bits :(
                        iVal = (code.getBottom10Bits()
                                * PSX_QUANTIZATION_TABLE_ZIGZAG[iCurrentBlockVectorPosition]
                                * iCurrentBlockQscale + 4) >> 3;
                    } else {
                        // normally would multiply by
                        // PSX_QUANTIZATION_TABLE_ZIGZAG[iCurrentBlockVectorPosition]
                        // and divide by 8 (like above), but the
                        // JPEG_QUANTIZATION_TABLE_ZIGZAG[iCurrentBlockVectorPosition]
                        // has already been divided by 8, so it will handle that
                        iVal = code.getBottom10Bits() * iCurrentBlockQscale;
                    }

                    if (iVal < -1023 || iVal > 1023) {
                        // if this happens we would need to go back and
                        // increase values in the quantization table.
                        // however that would deviate even more from the
                        // original frame quality, and be a huge pain
                        // to implement
                        // thankfully this doesn't seem to happen for
                        // normal (non-corrupted) frames
                        MdecContext.MacroBlockPixel macBlkXY = context.getMacroBlockPixel();
                        String msg = String.format(
                                "[JPG] Too much energy to encode %d in macroblock %d (%d, %d) block %d",
                                iVal, context.getTotalMacroBlocksRead(), macBlkXY.x, macBlkXY.y, context.getCurrentBlock().ordinal());
                        LOG.log(Level.WARNING, msg);
                        throw new MdecException.TooMuchEnergy(msg);
                    }
                    comp.DctCoffZZ[comp.WriteIndex+iCurrentBlockVectorPosition] = iVal;

                    ////////////////////////////////////////////////////////
                    context.nextCode();
                }
                context.nextCodeEndBlock();

                comp.WriteIndex += 64;
            }

        }

        cleanStream.logIfAny0AcCoefficient();
    }

    /** Writes the translated JPEG to the output. */
    public void writeJpeg(OutputStream os) throws IOException  {

        // write headers
        writeMarker(os, SOI);
        writeAPP(os, APP0, 1, 1, 0, 1, 1, 0, 0);
        writeCOM(os, getCommentBytes());
        writeDQT(os, 0, JPEG_QUANTIZATION_TABLE_ZIGZAG);
        writeSOF(os, SOF0, PRECISION, _iPixelWidth, _iPixelHeight);
        for (int i = 0; i < _aoDhtTables.length; i++) {
            HuffmanTable dhtTable = _aoDhtTables[i];
            dhtTable.writeDHT(os);
        }
        writeSOS(os, SOS, 0, 63);

        for (Component component : _aoComponents) {
            component.PreviousDC = 0;
        }

        _jpegStream.innerStream = os;
        _jpegStream.reset();
        // write the payload
        try {
            for (int iMbY = 0; iMbY < _iMacBlockHeight; iMbY++) {
                for (int iMbX = 0; iMbX < _iMacBlockWidth; iMbX++) {

                    for (Component comp : _aoComponents) {
                        int iMbBlockSize = comp.HSampling * comp.VSampling;
                        int iBlockStart = (iMbX * _iMacBlockHeight + iMbY) * iMbBlockSize * 64;
                        int[] aiBlocks = comp.DctCoffZZ;
                        for (int iBlock = 0; iBlock < iMbBlockSize; iBlock++) {
                            _aoDcHuffmanTables[comp.DcHuffTableIndex].encodeDcCoefficient(aiBlocks[iBlockStart], comp, _jpegStream);
                            _aoAcHuffmanTables[comp.AcHuffTableIndex].encodeAcCoefficients(aiBlocks, iBlockStart, _jpegStream);
                            iBlockStart += 64;
                        }
                    }

                }
            }

            _jpegStream.flush();
        } finally {
            _jpegStream.innerStream = null;
        }

        writeMarker(os, EOI);
    }

    /** Write a JPEG marker. */
    static void writeMarker(OutputStream os, int iMarker) throws IOException {
        os.write(0xff);
        os.write(iMarker);
    }

    private static void writeAPP(OutputStream os, int iAppMarker,
                                 int iMajorVersion, int iMinorVersion,
                                 int iUnits, int iHDpp, int iVDpp,
                                 int iThumbW, int iThumbH)
            throws IOException
    {
        writeMarker(os, iAppMarker);
        IO.writeInt16BE(os, 16);
        os.write(new byte[]{'J', 'F', 'I', 'F', '\0'});
        os.write(iMajorVersion);
        os.write(iMinorVersion);
        os.write(iUnits);
        IO.writeInt16BE(os, iHDpp);
        IO.writeInt16BE(os, iVDpp);
        os.write(iThumbH);
        os.write(iThumbW);
    }

    private void writeSOF(OutputStream os, int iSofMarker,
                          int iSamplePrecision, int iWidth, int iHeight)
            throws IOException
    {
        writeMarker(os, iSofMarker);
        IO.writeInt16BE(os, 2 + 6 + _aoComponents.length * 3);

        os.write(iSamplePrecision);
        IO.writeInt16BE(os, iHeight);
        IO.writeInt16BE(os, iWidth);
        os.write(_aoComponents.length);
        for (Component comp : _aoComponents) {
            os.write(comp.ComponentIndex);
            os.write((comp.HSampling << 4) | comp.VSampling);
            os.write(comp.QuantizationTableIndex);
        }

    }

    private static void writeDQT(OutputStream os, int iIndex, int[] aiQtable)
            throws IOException
    {
        writeMarker(os, DQT);
        IO.writeInt16BE(os, 2 + 1 + 64);
        if ((iIndex & ~15) != 0 || aiQtable.length != 64) {
            throw new UnsupportedOperationException("Only 8-bit precision implemented");
        }
        os.write(iIndex);
        for (int i : aiQtable) {
            if ((i & ~255) != 0) {
                throw new UnsupportedOperationException("Only 8-bit precision implemented");
            }
            os.write(i);
        }
    }

    private void writeSOS(OutputStream os, int iSosMarker,
                          int iSpectralSelectionStart, int iSpectralSelectionEnd)
            throws IOException
    {
        writeMarker(os, iSosMarker);
        IO.writeInt16BE(os, 2 + 1 + _aoComponents.length * 2 + 3);
        os.write(_aoComponents.length);
        for (Component comp : _aoComponents) {
            os.write(comp.ComponentIndex);
            os.write((comp.DcHuffTableIndex << 4) | comp.AcHuffTableIndex);
        }
        os.write(iSpectralSelectionStart);
        os.write(iSpectralSelectionEnd);
        os.write(0);
    }

    /** Insert comment. */
    private static void writeCOM(OutputStream out, byte[] abComment) throws IOException {
        writeMarker(out, COM);
        IO.writeInt16BE(out, 2 + abComment.length);
        // Comment text
        out.write(abComment);
    }

}
