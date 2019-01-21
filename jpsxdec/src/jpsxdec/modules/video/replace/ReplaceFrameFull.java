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

package jpsxdec.modules.video.replace;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.framenumber.FrameLookup;
import jpsxdec.psxvideo.bitstreams.BitStreamCompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.encode.PsxYCbCrImage;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReplaceFrameFull {

    private static final Logger LOG = Logger.getLogger(ReplaceFrameFull.class.getName());
    
    @Nonnull
    private final FrameLookup _frameNum;
    @Nonnull
    private final File _imageFile;
    @CheckForNull
    private ImageFormat _format;

    public static final String XML_TAG_NAME = "replace";

    public enum ImageFormat {
        BS, MDEC;
        public static @CheckForNull ImageFormat deserialize(@CheckForNull String sFormat) throws LocalizedDeserializationFail {
            if (sFormat == null || sFormat.length() == 0)
                return null;
            else if ("bs".equalsIgnoreCase(sFormat))
                return BS;
            else if ("mdec".equalsIgnoreCase(sFormat))
                return MDEC;
            else
                throw new LocalizedDeserializationFail(I.REPLACE_INVALID_IMAGE_FORMAT(sFormat));
        }
        public @Nonnull String serialize() {
            return name().toLowerCase();
        }
    }

    public ReplaceFrameFull(@Nonnull Element element) throws LocalizedDeserializationFail {
        this(element.getAttribute("frame").trim(), element.getFirstChild().getNodeValue().trim());
        setFormat(ImageFormat.deserialize(element.getAttribute("format")));
    }
    public @Nonnull Element serialize(@Nonnull Document document) {
        Element node = document.createElement(XML_TAG_NAME);
        node.setAttribute("frame", getFrameLookup().toString());
        node.setTextContent(getImageFile().toString());
        ImageFormat fmt = getFormat();
        if (fmt != null)
            node.setAttribute("format", fmt.serialize());
        return node;
    }

    public ReplaceFrameFull(@Nonnull String sFrameNumber, @Nonnull String sImageFile) throws LocalizedDeserializationFail {
        this(sFrameNumber, new File(sImageFile));
    }
    public ReplaceFrameFull(@Nonnull String sFrameNumber, @Nonnull File imageFile) throws LocalizedDeserializationFail {
        this(new FrameLookup(sFrameNumber), imageFile);
    }
    public ReplaceFrameFull(@Nonnull FrameLookup frameNumber, @Nonnull String sImageFile) {
        this(frameNumber, new File(sImageFile));
    }
    public ReplaceFrameFull(@Nonnull FrameLookup frameNumber, @Nonnull File imageFile) {
        _frameNum = frameNumber;
        _imageFile = imageFile;
    }

    final public @Nonnull FrameLookup getFrameLookup() {
        return _frameNum;
    }

    final public @Nonnull File getImageFile() {
        return _imageFile;
    }

    final public @CheckForNull ImageFormat getFormat() {
        return _format;
    }

    final public void setFormat(@CheckForNull ImageFormat format) {
        _format = format;
    }

    public void replace(@Nonnull IDemuxedFrame frame, @Nonnull CdFileSectorReader cd,
                        @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        // identify existing frame bs format
        byte[] abExistingFrame = frame.copyDemuxData();
        BitStreamUncompressor bsu;
        try {
            bsu = BitStreamUncompressor.identifyUncompressor(abExistingFrame);
        } catch (BinaryDataNotRecognized ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(getFrameLookup().toString()), ex);
        }

        try {
            // parse through the existing bitstream to ensure it is good
            // and to collect information about it
            bsu.skipMacroBlocks(frame.getWidth(), frame.getHeight());
        } catch (MdecException.EndOfStream ex) {
            // existing frame is incomplete
            throw new LoggedFailure(log, Level.SEVERE, I.FRAME_NUM_INCOMPLETE(getFrameLookup().toString()), ex);
        } catch (MdecException.ReadCorruption ex) {
            // existing frame is corrupted
            throw new LoggedFailure(log, Level.SEVERE, I.FRAME_NUM_CORRUPTED(getFrameLookup().toString()), ex);
        }
        byte[] abNewFrame;

        if (_format == ImageFormat.BS) {
            abNewFrame = readBitstreamFile(_imageFile, log);
        } else {
            BitStreamCompressor compressor = bsu.makeCompressor();
            if (_format == ImageFormat.MDEC) {
                abNewFrame = readMdecAndCompress(_imageFile, getFrameLookup(),
                                                 compressor, log);
            } else {
                abNewFrame = readJavaImage(_imageFile, getFrameLookup(),
                                           frame.getWidth(), frame.getHeight(),
                                           abExistingFrame, compressor, log);
            }
        }

        if (abNewFrame == null)
            throw new LoggedFailure(log, Level.SEVERE, I.CMD_UNABLE_TO_COMPRESS_FRAME_SMALL_ENOUGH(
                    getFrameLookup().toString(), frame.getDemuxSize()));
        else if (abNewFrame.length > frame.getDemuxSize()) // for bs or mdec formats
            throw new LoggedFailure(log, Level.SEVERE, I.NEW_FRAME_DOES_NOT_FIT(
                    getFrameLookup().toString(), abNewFrame.length, frame.getDemuxSize()));

        // find out how many bytes and mdec codes are used by the new frame
        BitStreamUncompressor verifyBsu;
        try {
            // also verify it is the same bitstream type (for bs format)
            verifyBsu = bsu.getType().makeNew(abNewFrame);
        } catch (BinaryDataNotRecognized ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.REPLACE_BITSTREAM_MISMATCH(_imageFile), ex);
        }

        try {
            verifyBsu.skipMacroBlocks(frame.getWidth(), frame.getHeight());
            verifyBsu.skipPaddingBits();
        } catch (MdecException.EndOfStream ex) {
            throw new RuntimeException("Can't decode a frame we just encoded?", ex);
        } catch (MdecException.ReadCorruption ex) {
            throw new RuntimeException("Can't decode a frame we just encoded?", ex);
        }

        int iUsedSize = ((verifyBsu.getBitPosition() + 15) / 16) * 2; // rounded up to nearest word
        frame.writeToSectors(abNewFrame, iUsedSize, verifyBsu.getReadMdecCodeCount(), cd, log);
    }

    private static byte[] readBitstreamFile(@Nonnull File imageFile, @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        try {
            return IO.readFile(imageFile);
        } catch (FileNotFoundException ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.IO_OPENING_FILE_NOT_FOUND_NAME(imageFile.toString()), ex);
        } catch (IOException ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.IO_READING_FILE_ERROR_NAME(imageFile.toString()), ex);
        }
    }

    private static byte[] readMdecAndCompress(@Nonnull File mdecImageFile, @Nonnull FrameLookup frameNum,
                                   @Nonnull BitStreamCompressor compressor,
                                   @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        try {
            MdecInputStreamReader mdecIn = new MdecInputStreamReader(IO.readFile(mdecImageFile));
            return compressor.compress(mdecIn);
        } catch (FileNotFoundException ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.IO_OPENING_FILE_NOT_FOUND_NAME(mdecImageFile.toString()), ex);
        } catch (IOException ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.IO_READING_FILE_ERROR_NAME(mdecImageFile.toString()), ex);
        } catch (IncompatibleException ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.REPLACE_INCOMPATIBLE_MDEC(mdecImageFile.toString(), frameNum.toString()), ex);
        } catch (MdecException.TooMuchEnergy ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.REPLACE_INCOMPATIBLE_MDEC(mdecImageFile.toString(), frameNum.toString()), ex);
        } catch (MdecException.EndOfStream ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.REPLACE_INCOMPLETE_MDEC(mdecImageFile.toString(), frameNum.toString()), ex);
        } catch (MdecException.ReadCorruption ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.REPLACE_CORRUPTED_MDEC(mdecImageFile.toString(), frameNum.toString()), ex);
        }
    }

    private static byte[] readJavaImage(@Nonnull File imageFile, @Nonnull FrameLookup frameNum,
                                        int iWidth, int iHeight,
                                        byte[] abExistingFrame,
                                        @Nonnull BitStreamCompressor compressor,
                                        @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        BufferedImage bi;
        try { bi = ImageIO.read(imageFile); } catch (IOException ex) {
            throw new LoggedFailure(log, Level.SEVERE, I.IO_READING_FILE_ERROR_NAME(imageFile.toString()), ex);
        }
        if (bi == null)
            throw new LoggedFailure(log, Level.SEVERE, I.REPLACE_FILE_NOT_JAVA_IMAGE(imageFile));

        if (bi.getWidth()  != Calc.fullDimension(iWidth) ||
            bi.getHeight() != Calc.fullDimension(iHeight))
            throw new LoggedFailure(log, Level.SEVERE,
                    I.REPLACE_FRAME_DIMENSIONS_MISMATCH(imageFile.toString(),
                    bi.getWidth(), bi.getHeight(), iWidth, iHeight));

        PsxYCbCrImage psxImage = new PsxYCbCrImage(bi);
        MdecEncoder encoder = new MdecEncoder(psxImage, iWidth, iHeight);
        try {
            return compressor.compressFull(abExistingFrame, frameNum.toString(), encoder, log);
        } catch (MdecException.EndOfStream ex) {
            // existing frame is incomplete
            throw new LoggedFailure(log, Level.SEVERE, I.FRAME_NUM_INCOMPLETE(frameNum.toString()), ex);
        } catch (MdecException.ReadCorruption ex) {
            // existing frame is corrupted
            throw new LoggedFailure(log, Level.SEVERE, I.FRAME_NUM_CORRUPTED(frameNum.toString()), ex);
        }
    }
}

