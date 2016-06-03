/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

package jpsxdec.discitems.psxvideoencode;


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
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.savers.FrameLookup;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedFileNotFoundException;
import jpsxdec.i18n.LocalizedIOException;
import jpsxdec.psxvideo.bitstreams.BitStreamCompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.encode.PsxYCbCrImage;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.NotThisTypeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReplaceFrame {

    private static final Logger LOG = Logger.getLogger(ReplaceFrame.class.getName());
    
    @Nonnull
    private final FrameLookup _frameNum;
    @CheckForNull
    private String _sFormat;
    @CheckForNull
    private File _imageFile;

    public static final String XML_TAG_NAME = "replace";

    public ReplaceFrame(@Nonnull Element element) throws NotThisTypeException, FileNotFoundException {
        this(element.getAttribute("frame"));
        setImageFile(element.getFirstChild().getNodeValue());
        setFormat(element.getAttribute("format"));
    }
    public @Nonnull Element serialize(@Nonnull Document document) {
        Element node = document.createElement(XML_TAG_NAME);
        node.setAttribute("frame", getFrameLookup().toString());
        File imgFile = getImageFile();
        if (imgFile != null)
            node.setTextContent(imgFile.toString());
        String fmt = getFormat();
        if (fmt != null)
            node.setAttribute("format", fmt);
        return node;
    }

    public ReplaceFrame(@Nonnull FrameLookup frameNumber) {
        _frameNum = frameNumber;
    }

    public ReplaceFrame(@Nonnull String sFrameNumber) throws NotThisTypeException {
        _frameNum = FrameLookup.deserialize(sFrameNumber.trim());
    }

    public @Nonnull FrameLookup getFrameLookup() {
        return _frameNum;
    }

    public @CheckForNull File getImageFile() {
        return _imageFile;
    }

    final public void setImageFile(@Nonnull String sImageFile) throws FileNotFoundException {
        setImageFile(new File(sImageFile.trim()));
    }
    public void setImageFile(@Nonnull File imageFile) throws FileNotFoundException {
        _imageFile = imageFile;
        if (!_imageFile.exists())
            throw new LocalizedFileNotFoundException(I.REPLACE_UNABLE_TO_FIND_FILE(_imageFile));
    }

    public @CheckForNull String getFormat() {
        return _sFormat;
    }

    final public void setFormat(@CheckForNull String sFormat) {
        _sFormat = sFormat;
    }

    public void replace(@Nonnull IDemuxedFrame frame, @Nonnull CdFileSectorReader cd,
                        @Nonnull FeedbackStream fbs)
            throws IOException, NotThisTypeException, MdecException, IncompatibleException
    {
        // identify existing frame bs format
        byte[] abExistingFrame = frame.copyDemuxData(null);
        BitStreamUncompressor bsu = BitStreamUncompressor.identifyUncompressor(abExistingFrame);
        if (bsu == null)
            throw new MdecException.Uncompress(I.CMD_UNABLE_TO_IDENTIFY_FRAME_TYPE());
        BitStreamCompressor compressor = bsu.makeCompressor();

        byte[] abNewFrame;

        if ("bs".equals(_sFormat)) {
            abNewFrame = IO.readFile(_imageFile);
        } else if ("mdec".equals(_sFormat)) {
            MdecInputStreamReader mdecIn = new MdecInputStreamReader(_imageFile);
            try {
                abNewFrame = compressor.compress(mdecIn, frame.getWidth(), frame.getHeight());
            } finally {
                try {
                    mdecIn.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        } else {
            BufferedImage bi = ImageIO.read(_imageFile);
            if (bi == null)
                throw new LocalizedIOException(I.REPLACE_FILE_NOT_JAVA_IMAGE(_imageFile));

            if (bi.getWidth()  != Calc.fullDimension(frame.getWidth()) ||
                bi.getHeight() != Calc.fullDimension(frame.getHeight()))
                throw new IncompatibleException(I.REPLACE_FRAME_DIMENSIONS_MISMATCH(bi.getWidth(), bi.getHeight(), frame.getWidth(), frame.getHeight()));

            PsxYCbCrImage psxImage = new PsxYCbCrImage(bi);
            MdecEncoder encoder = new MdecEncoder(psxImage, frame.getWidth(), frame.getHeight());
            abNewFrame = compressor.compressFull(abExistingFrame, frame.getFrame(), encoder, fbs);
        }

        if (abNewFrame == null)
            throw new MdecException.Compress(I.CMD_UNABLE_TO_COMPRESS_FRAME_SMALL_ENOUGH(
                    frame.getFrame(), frame.getDemuxSize()));
        else if (abNewFrame.length > frame.getDemuxSize()) // for bs or mdec formats
            throw new MdecException.Compress(I.NEW_FRAME_DOES_NOT_FIT(
                    frame.getFrame(), abNewFrame.length, frame.getDemuxSize()));

        // find out how many bytes and mdec codes are used by the new frame
        bsu.reset(abNewFrame, abNewFrame.length);
        bsu.skipMacroBlocks(frame.getWidth(), frame.getHeight());
        bsu.skipPaddingBits();

        int iUsedSize = ((bsu.getBitPosition() + 15) / 16) * 2; // rounded up to nearest word
        frame.writeToSectors(abNewFrame, iUsedSize, bsu.getMdecCodeCount(), cd, fbs);
    }

}

