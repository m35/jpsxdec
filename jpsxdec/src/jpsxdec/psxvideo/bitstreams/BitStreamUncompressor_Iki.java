/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.psxvideo.encode.MacroBlockEncoder;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.Misc;

/** A bitstream in the 'iki' format. 
 * These streams store the DC value and quantization scale of each
 * block in a lzss compressed header. Unlike other bitstreams,
 * this takes full advantage of the MDEC chip by allowing each block
 * to have its own quantization scale, instead of a single scale for the
 * entire frame. */
public class BitStreamUncompressor_Iki extends BitStreamUncompressor {

    private static final Logger LOG = Logger.getLogger(BitStreamUncompressor_Iki.class.getName());

    public static class IkiHeader {
        private int _iMdecCodeCount = -1;
        private int _iWidth = -1;
        private int _iHeight = -1;
        private int _iCompressedDataSize = -1;

        private int _iBlockCount = -1;
        @Nonnull
        private byte[] _abQscaleDcLookupTable = null;

        private final boolean _blnIsValid;
        public IkiHeader(@Nonnull byte[] abFrameData, int iDataSize) {
            if (iDataSize < 10) {
                _blnIsValid = false;
                return;
            }

            int iMdecCodeCount      = IO.readUInt16LE(abFrameData, 0);
            int iMagic3800          = IO.readUInt16LE(abFrameData, 2);
            int iWidth              = IO.readSInt16LE(abFrameData, 4);
            int iHeight             = IO.readSInt16LE(abFrameData, 6);
            int iCompressedDataSize = IO.readUInt16LE(abFrameData, 8);

            if (iMdecCodeCount < 0 || iMagic3800 != 0x3800 || iWidth < 1 || iHeight < 1 || iCompressedDataSize < 1) {
                _blnIsValid = false;
                return;
            }

            if (iDataSize < 10 + iCompressedDataSize) {
                LOG.log(Level.WARNING, "Incomplete iki frame header");
                _blnIsValid = false;
                return;
            }

            int iBlockCount = Calc.blocks(iWidth, iHeight);
            int iQscaleDcLookupTableSize = iBlockCount * 2; // 2 bytes per block

            byte[] abQscaleDcLookupTable = new byte[iQscaleDcLookupTableSize];

            try {
                ikiLzssUncompress(abFrameData, 10, abQscaleDcLookupTable, iQscaleDcLookupTableSize);
            } catch (ArrayIndexOutOfBoundsException ex) {
                _blnIsValid = false;
                return;
            }

            _iMdecCodeCount = iMdecCodeCount;
            _iWidth = iWidth;
            _iHeight = iHeight;
            _iCompressedDataSize = iCompressedDataSize;
            _iBlockCount = iBlockCount;
            _abQscaleDcLookupTable = abQscaleDcLookupTable;

            _blnIsValid = true;
        }

        public int getiMdecCodeCount() {
            return _iMdecCodeCount;
        }

        public boolean isValid() {
            return _blnIsValid;
        }

        public int getWidth() {
            if (!_blnIsValid) throw new IllegalStateException();
            return _iWidth;
        }

        public int getHeight() {
            if (!_blnIsValid) throw new IllegalStateException();
            return _iHeight;
        }

        public int getCompressedDataSize() {
            if (!_blnIsValid) throw new IllegalStateException();
            return _iCompressedDataSize;
        }

        public int getFrameBlockCount() {
            if (!_blnIsValid) throw new IllegalStateException();
            return _iBlockCount;
        }

        public int getBlockQscale(int iBlock) {
            if (!_blnIsValid) throw new IllegalStateException();
            int b1 = _abQscaleDcLookupTable[iBlock] & 0xff;
            int b2 = _abQscaleDcLookupTable[iBlock+_iBlockCount] & 0xff;
            return (b1 << 8) | b2;
        }
    }

    public static @Nonnull BitStreamUncompressor_Iki makeIki(@Nonnull byte[] abFrameData)
            throws BinaryDataNotRecognized
    {
        return makeIki(abFrameData, abFrameData.length);
    }
    public static @Nonnull BitStreamUncompressor_Iki makeIki(@Nonnull byte[] abFrameData, int iDataSize)
            throws BinaryDataNotRecognized
    {
        BitStreamUncompressor_Iki bsu = makeIkiNoThrow(abFrameData, iDataSize);
        if (bsu == null)
            throw new BinaryDataNotRecognized();
        return bsu;
    }

    static @CheckForNull BitStreamUncompressor_Iki makeIkiNoThrow(@Nonnull byte[] abFrameData, int iDataSize)
            throws BinaryDataNotRecognized
    {
        IkiHeader header = new IkiHeader(abFrameData, iDataSize);
        if (!header.isValid())
            return null;

        ArrayBitReader bitReader = new ArrayBitReader(abFrameData, iDataSize, true, 10 + header.getCompressedDataSize());

        return new BitStreamUncompressor_Iki(header, bitReader);
    }

    @Nonnull
    private final IkiHeader _header;

    private int _iCurrentBlock = 0;

    private BitStreamUncompressor_Iki(@Nonnull IkiHeader header,
                                      @Nonnull ArrayBitReader bitReader)
    {
        super(BitStreamUncompressor_STRv2.AC_VARIABLE_LENGTH_CODES_MPEG1, bitReader);
        _header = header;
    }

    /** Read the quantization scale and DC coefficient from the iki lzss
     * compressed header. */
    @Override
    protected void readQscaleAndDC(@Nonnull MdecCode code) throws MdecException.EndOfStream {
        if (_iCurrentBlock >= _header.getFrameBlockCount())
            throw new MdecException.EndOfStream(MdecException.inBlockOfBlocks(_iCurrentBlock, _header.getFrameBlockCount()));
        readBlockQscaleAndDC(code, _iCurrentBlock);
        _iCurrentBlock++;
    }

    /** Looks up the given block's quantization scale and DC coefficient. */
    private void readBlockQscaleAndDC(@Nonnull MdecCode code, int iBlock) {
        code.set(_header.getBlockQscale(iBlock));
    }

    @Override
    protected void readEscapeAcCode(MdecCode code) throws MdecException.EndOfStream {
        BitStreamUncompressor_STRv2.readStrEscapeAcCode(_bitReader, code, _debug, LOG);
    }

    @Override
    public void skipPaddingBits() {
    }

    /** .iki videos utilize yet another LZSS compression format that is
     * different from both FF7 and Lain.
     *<p>
     * Note that if the ArrayIndexOutOfBoundsException is thrown a lot,
     * it may stop having stack trace or index.
     * This is due to a Sun VM optimization.
     * See VM option OmitStackTraceInFastThrow.
     * @throws ArrayIndexOutOfBoundsException 
     *              if there was an error uncompressing the data. */
    private static void ikiLzssUncompress(@Nonnull byte[] abSrc, int iSrcPosition,
                                          @Nonnull byte[] abDest, int iUncompressedSize)
            throws ArrayIndexOutOfBoundsException
    {
        int iDestPosition = 0;

        while (iDestPosition < iUncompressedSize) {

            int iFlags = abSrc[iSrcPosition++] & 0xff;

            if (DEBUG)
                System.err.println("Flags " + Misc.bitsToString(iFlags, 8));

            for (int iBit = 0; iBit < 8; iBit++, iFlags >>= 1) {

                if (DEBUG)
                    System.err.format("[InPos: %d OutPos: %d] bit %02x: ",
                                      iSrcPosition, iDestPosition, 1 << iBit );

                if ((iFlags & 1) == 0) {
                    byte b = abSrc[iSrcPosition++];

                    if (DEBUG)
                        System.err.println(String.format("{Byte %02x}", b));

                    abDest[iDestPosition++] = b;
                } else {
                    int iCopySize = (abSrc[iSrcPosition++] & 0xff) + 3;

                    int iCopyOffset = (abSrc[iSrcPosition++] & 0xff);
                    if ((iCopyOffset & 0x80) != 0) {
                        iCopyOffset = ((iCopyOffset & 0x7f) << 8) | (abSrc[iSrcPosition++] & 0xff);
                    }
                    iCopyOffset++;

                    if (DEBUG)
                        System.err.println(
                                "Copy " + iCopySize + " bytes from " + (iDestPosition - (iCopyOffset + 1)) + "(-"+iCopyOffset+")");

                    for (; iCopySize > 0; iCopySize--) {
                        abDest[iDestPosition] = abDest[iDestPosition - iCopyOffset];
                        iDestPosition++;
                    }
                }

                if (iDestPosition >= iUncompressedSize)
                    break;
            }
        }
        if (DEBUG)
            System.err.println("Src pos at end: " + iSrcPosition);
    }

    private static class IkiLzssCompressor {

        private int _iFlags;
        private int _iFlagBit;
        private final ByteArrayOutputStream _buffer = new ByteArrayOutputStream();
        private final ByteArrayOutputStream _baosLogger = new ByteArrayOutputStream();
        private final PrintStream _logger = new PrintStream(_baosLogger, true);

        /** Find the longest run of bytes that match the current position. */
        public void compress(@Nonnull byte[] abSrcData, @Nonnull ByteArrayOutputStream out) {
            reset();

            for (int iSrcPos = 0; iSrcPos < abSrcData.length;) {

                if (DEBUG)
                    _logger.format("[InPos: %d OutPos: %d]: bit %02x: ",
                                      out.size()+1+_buffer.size(), iSrcPos, 1 << _iFlagBit );
                
                int iLongestRunPos = 0;
                int iLongestRunLen = 0;

                // iki is weird because it won't compress the last 3 bytes
                // with a run even if it would save space
                if (iSrcPos < abSrcData.length - 3) {
                    int iFarthestBack = iSrcPos - (0x7fff + 1);
                    if (iFarthestBack < 0) iFarthestBack = 0;
                    for (int iMatchStart = iSrcPos-1; iMatchStart >= iFarthestBack; iMatchStart--) {
                        int iMatchLen = matchLength(abSrcData, iMatchStart, iSrcPos);
                        if (iMatchLen > iLongestRunLen) {
                            int iNegOffsetMin1 = iSrcPos - iMatchStart - 1;
                            if ((iNegOffsetMin1 <  0x80) && (iMatchLen >= 3) ||
                                (iNegOffsetMin1 >= 0x80) && (iMatchLen >= 4))
                            {
                                iLongestRunLen = iMatchLen;
                                iLongestRunPos = iMatchStart;
                            }
                        }
                    }
                }
                if (iLongestRunLen > 0) {
                    addRun(iSrcPos - iLongestRunPos, iLongestRunLen, iSrcPos);
                    iSrcPos += iLongestRunLen;
                } else {
                    if (DEBUG) _logger.format("{Byte %02x}", abSrcData[iSrcPos]&0xff).println();
                    addCopy(abSrcData[iSrcPos]);
                    iSrcPos++;
                }
                incFlag(out);
            }

            if (_iFlagBit > 0) {
                if (DEBUG) _logger.println("Flags " + Misc.bitsToString(_iFlags, 8));
                out.write(_iFlags);
                byte[] ab = _buffer.toByteArray();
                out.write(ab, 0, ab.length);
            }

        }
        
        private void addRun(int iPosition, int iLength, int iSrcPos) {
            assert iPosition > 0;
            if (DEBUG) _logger.format("Copy %d bytes from %d(%d)", iLength, iSrcPos-iPosition, -iPosition).println();
            _iFlags |= (1 << _iFlagBit);
            _buffer.write(iLength - 3);
            iPosition--;
            if (iPosition < 0x80) {
                _buffer.write(iPosition);
            } else {
                _buffer.write((iPosition >> 8) | 0x80);
                _buffer.write(iPosition & 0xff);
            }
        }

        private void addCopy(byte b) {
            _buffer.write(b);
        }

        private void incFlag(@Nonnull ByteArrayOutputStream out) {
            _iFlagBit++;
            if (_iFlagBit >= 8) {
                if (DEBUG) { 
                    System.err.println("Flags " + Misc.bitsToString(_iFlags, 8));
                    _logger.flush();
                    System.err.print(_baosLogger.toString());
                    _baosLogger.reset();
                }
                out.write(_iFlags);
                byte[] ab = _buffer.toByteArray();
                out.write(ab, 0, ab.length);
                reset();
            }
        }

        private void reset() {
            _iFlagBit = 0;
            _iFlags = 0;
            _buffer.reset();
        }

        /** Count how many bytes match the current position. */
        private static int matchLength(@Nonnull byte[] abData, int iMatchStart, int iEndPos) {
            int iLen = 0;
            while ((iEndPos + iLen < abData.length)
                   && iLen < (255 + 3)
                   && abData[iMatchStart+iLen] == abData[iEndPos+iLen])
            {
                iLen++;
            }
            return iLen;
        }
    }


    @Override
    public @Nonnull Type getType() {
        return Type.Iki;
    }

    public String toString() {
        if (_header.isValid()) {
            // find the minimum and maximum quantization scales used
            int iMinQscale = 64, iMaxQscale = 0;
            MdecCode code = new MdecCode();
            for (int i = 0; i < _header.getFrameBlockCount(); i++) {
                readBlockQscaleAndDC(code, i);
                int iQscale = code.getTop6Bits();
                if (iQscale < iMinQscale)
                    iMinQscale = iQscale;
                if(iQscale > iMaxQscale)
                    iMaxQscale = iQscale;
            }
            return String.format("%s Qscale=%d-%d Offset=%d MB=%d.%d Mdec count=%d %dx%d",
                    getType(), iMinQscale, iMaxQscale,
                    _bitReader.getWordPosition(),
                    getFullMacroBlocksRead(), getCurrentMacroBlockSubBlock(),
                    getReadMdecCodeCount(),
                    _header.getWidth(), _header.getHeight());
        } else {
            return "Invalid IKI";
        }
    }

    @Override
    public @Nonnull BitStreamCompressor_Iki makeCompressor() {
        return new BitStreamCompressor_Iki(_header.getWidth(), _header.getHeight());
    }

    // =========================================================================

    public static class BitStreamCompressor_Iki extends BitStreamUncompressor_STRv2.BitStreamCompressor_STRv2 {
        
        private final int _iWidth, _iHeight;

        private BitStreamCompressor_Iki(int iWidth, int iHeight) {
            super(Calc.macroblocks(iWidth, iHeight));
            _iWidth = iWidth;
            _iHeight = iHeight;
        }


        @Override
        public @CheckForNull byte[] compressFull(@Nonnull byte[] abOriginal,
                                                 @Nonnull String sFrameDescription,
                                                 @Nonnull MdecEncoder encoder,
                                                 @Nonnull ILocalizedLogger log)
                throws MdecException.EndOfStream, MdecException.ReadCorruption
        {
            // TODO: verify original bitstream is iki?
            
            // STEP 1: Find the minimum Qscale for all blocks that will fit frame
            byte[] abNewDemux = null;
            int iQscale;
            for (iQscale = 1; iQscale < 64; iQscale++) {
                log.log(Level.INFO, I.TRYING_QSCALE(iQscale));

                int[] aiNewQscale = { iQscale, iQscale, iQscale,
                                      iQscale, iQscale, iQscale };

                for (MacroBlockEncoder macblk : encoder) {
                    macblk.setToFullEncode(aiNewQscale);
                }

                try {
                    abNewDemux = compress(encoder.getStream());
                } catch (IncompatibleException ex) {
                    throw new RuntimeException("The encoder should be compatible here", ex);
                }
                int iNewDemuxSize = abNewDemux.length;
                if (iNewDemuxSize <= abOriginal.length) {
                    log.log(Level.INFO, I.NEW_FRAME_FITS(sFrameDescription, iNewDemuxSize, abOriginal.length));
                    break;
                } else {
                    log.log(Level.INFO, I.NEW_FRAME_DOES_NOT_FIT(sFrameDescription, iNewDemuxSize, abOriginal.length));
                    abNewDemux = null;
                }
            }

            if (abNewDemux != null && abNewDemux.length < abOriginal.length && iQscale > 1) {
                // STEP 2: decrease the qscale of blocks with high energy
                //         until we run out of space
                abNewDemux = reduceQscaleForHighEnergyMacroBlocks(
                             abNewDemux,
                             abOriginal.length, sFrameDescription, iQscale-1, encoder, log);
            }

            return abNewDemux;
        }

        /** It is clear the original iki encoder did something like this.
         * While this doesn't produce identical results, it does appear to be
         * in the right direction. It should be quite sufficient for
         * partially replacing frames, and pretty good for full frame replace. */
        private @Nonnull byte[] reduceQscaleForHighEnergyMacroBlocks(
                                                @Nonnull byte[] abLastGoodDemux,
                                                int iOriginalLength,
                                                @Nonnull String sFrameDescription,
                                                int iNewQscale,
                                                @Nonnull MdecEncoder encoder,
                                                @Nonnull ILocalizedLogger log)
                throws MdecException.EndOfStream, MdecException.ReadCorruption
        {
            // sort the macroblocks by energy and distance from center of frame
            final int iMbCenterX = encoder.getMacroBlockWidth()  / 2,
                      iMbCenterY = encoder.getMacroBlockHeight() / 2;
            TreeSet<MacroBlockEncoder> macblocks = new TreeSet<MacroBlockEncoder>(new Comparator<MacroBlockEncoder>() {
                public int compare(MacroBlockEncoder o1, MacroBlockEncoder o2) {
                    // put macroblocks with bigger energy first
                    if (o1.getEnergy() > o2.getEnergy())
                        return -1;
                    if (o1.getEnergy() < o2.getEnergy())
                        return 1;
                    // calculate macroblock's distance from the center of the frame
                    int iDistX, iDistY;
                    iDistX = o1.X - iMbCenterX;
                    iDistY = o1.Y - iMbCenterY;
                    int o1dist = iDistX*iDistX + iDistY*iDistY;
                    iDistX = o2.X - iMbCenterX;
                    iDistY = o2.Y - iMbCenterY;
                    int o2dist = iDistX*iDistX + iDistY*iDistY;
                    // put those closer to the center first
                    return Misc.intCompare(o1dist, o2dist);
                }
            });
            for (MacroBlockEncoder macblk : encoder) {
                macblocks.add(macblk);
            }

            // decrease the qscale of each macroblock until we run out of room
            int[] aiNewQscale = { iNewQscale, iNewQscale, iNewQscale,
                                  iNewQscale, iNewQscale, iNewQscale };
            for (MacroBlockEncoder macblk : macblocks) {
                log.log(Level.INFO, I.IKI_REDUCING_QSCALE_OF_MB_TO_VAL(macblk.X, macblk.Y, iNewQscale));
                macblk.setToFullEncode(aiNewQscale);
                byte[] abNewDemux;
                try {
                    abNewDemux = compress(encoder.getStream());
                } catch (IncompatibleException ex) {
                    throw new RuntimeException("The encoder should be compatible here", ex);
                }
                int iNewDemuxSize = abNewDemux.length;
                if (iNewDemuxSize <= iOriginalLength) {
                    log.log(Level.INFO, I.NEW_FRAME_FITS(sFrameDescription, iNewDemuxSize, iOriginalLength));
                } else {
                    log.log(Level.INFO, I.IKI_NEW_FRAME_GT_SRC_STOPPING(sFrameDescription, iNewDemuxSize, iOriginalLength));
                    break;
                }
                abLastGoodDemux = abNewDemux;
            }

            return abLastGoodDemux;
        }
        
        @Override
        public @CheckForNull byte[] compressPartial(@Nonnull byte[] abOriginal,
                                                    @Nonnull String sFrameDescription,
                                                    @Nonnull MdecEncoder encoder,
                                                    @Nonnull ILocalizedLogger log)
                throws LocalizedIncompatibleException, MdecException.EndOfStream, MdecException.ReadCorruption
        {
            // all blocks to replace are full replaced
            return compressFull(abOriginal, sFrameDescription, encoder, log);
        }



        private final ByteArrayOutputStream _top8 = new ByteArrayOutputStream();
        private final ByteArrayOutputStream _bottom8 = new ByteArrayOutputStream();
        private final MdecCode _currentBlockQscaleDc = new MdecCode();
        private final IkiLzssCompressor _lzs = new IkiLzssCompressor();

        @Override
        public @Nonnull byte[] compress(@Nonnull MdecInputStream inStream)
                throws IncompatibleException, MdecException.EndOfStream,
                       MdecException.ReadCorruption
        {
            _top8.reset();
            _bottom8.reset();
            try {
                return super.compress(inStream);
            } catch (MdecException.TooMuchEnergy ex) {
                throw new RuntimeException("This should not happen with Iki", ex);
            }
        }

        @Override
        protected void setBlockQscale(int iBlock, int iQscale) {
            _currentBlockQscaleDc.setTop6Bits(iQscale);
        }


        @Override
        protected @Nonnull String encodeDC(int iDC, int iBlock) {
            _currentBlockQscaleDc.setBottom10Bits(iDC);
            int iMdec = _currentBlockQscaleDc.toMdecWord();
            _top8.write(iMdec >> 8);
            _bottom8.write(iMdec & 0xff);
            return "";
        }

        @Override
        protected @Nonnull byte[] createHeader(int iMdecCodeCount) {
            assert _top8.size() == _bottom8.size();

            byte[] ab = _bottom8.toByteArray();
            _top8.write(ab, 0, ab.length);
            ab = _top8.toByteArray();
            _top8.reset();
            _bottom8.reset();
            _lzs.compress(ab, _bottom8);
            if (_bottom8.size() % 2 != 0)
                _bottom8.write(0);
            ab = _bottom8.toByteArray();

            byte[] abHdr = new byte[10];
            IO.writeInt16LE(abHdr, 0, (short)calculateHalfCeiling32(iMdecCodeCount));
            IO.writeInt16LE(abHdr, 2, (short)0x3800);
            IO.writeInt16LE(abHdr, 4, (short)_iWidth);
            IO.writeInt16LE(abHdr, 6, (short)_iHeight);
            IO.writeInt16LE(abHdr, 8, (short)ab.length);
            _top8.write(abHdr, 0, abHdr.length);
            _top8.write(ab, 0, ab.length);

            return _top8.toByteArray();
        }

        @Override
        protected void addTrailingBits(BitStreamWriter bitStream) {
        }
    }

    /** For testing. */
    static byte[] ikiLzssCompress(byte[] ab) {
        IkiLzssCompressor compressor = new IkiLzssCompressor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        compressor.compress(ab, baos);
        return baos.toByteArray();
    }

    /** For testing. */
    static byte[] ikiLzssUncompress(byte[] ab, int iUncompressSize) {
        byte[] abUncompressed = new byte[iUncompressSize];
        ikiLzssUncompress(ab, 0, abUncompressed, iUncompressSize);
        return abUncompressed;
    }

}
