/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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
import java.io.IOException;
import javax.imageio.ImageIO;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.savers.FrameLookup;
import jpsxdec.psxvideo.bitstreams.BitStreamCompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.encode.PsxYCbCrImage;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReplaceFrame {
    private final FrameLookup _frameNum;
    private String _sFormat;
    private File _imageFile;

    public static final String XML_TAG_NAME = "replace";

    public ReplaceFrame(Element element) throws NotThisTypeException {
        this(element.getAttribute("frame"));
        setImageFile(element.getFirstChild().getNodeValue());
        setFormat(element.getAttribute("format"));
    }
    public Element serialize(Document document) {
        Element node = document.createElement(XML_TAG_NAME);
        node.setAttribute("frame", getFrame().toString());
        node.setTextContent(getImageFile().toString());
        if (getFormat() != null)
            node.setAttribute("format", getFormat());
        return node;
    }

    public ReplaceFrame(FrameLookup frameNumber) {
        _frameNum = frameNumber;
    }

    public ReplaceFrame(String sFrameNumber) throws NotThisTypeException {
        _frameNum = FrameLookup.deserialize(sFrameNumber.trim());
    }

    public FrameLookup getFrame() {
        return _frameNum;
    }

    public File getImageFile() {
        return _imageFile;
    }

    final public void setImageFile(String sImageFile) {
        setImageFile(new File(sImageFile.trim()));
    }
    public void setImageFile(File imageFile) {
        _imageFile = imageFile;
        if (!_imageFile.exists())
            throw new IllegalArgumentException("Unable to find " + _imageFile); // I18N
    }

    public String getFormat() {
        return _sFormat;
    }

    final public void setFormat(String sFormat) {
        _sFormat = sFormat;
    }

    public void replace(IDemuxedFrame frame, CdFileSectorReader cd, FeedbackStream fbs) 
            throws IOException, NotThisTypeException, MdecException
    {
        // identify existing frame bs format
        byte[] abExistingFrame = frame.copyDemuxData(null);
        BitStreamCompressor compressor = BitStreamUncompressor.identifyCompressor(abExistingFrame);

        byte[] abNewFrame;

        if ("bs".equals(_sFormat)) {
            abNewFrame = IO.readFile(_imageFile);
        } else if ("mdec".equals(_sFormat)) {
            abNewFrame = compressor.compress(new MdecInputStreamReader(_imageFile),
                                             frame.getWidth(), frame.getHeight());
        } else {
            BufferedImage bi = ImageIO.read(_imageFile);
            if (bi == null)
                throw new IllegalArgumentException("Unable to read " + _imageFile + " as an image. Did you forget 'format'?"); // I18N

            if (bi.getWidth()  != Calc.fullDimension(frame.getWidth()) ||
                bi.getHeight() != Calc.fullDimension(frame.getHeight()))
                throw new IllegalArgumentException("Replacement frame dimensions do not match frame to replace: " + // I18N
                        bi.getWidth() + "x" + bi.getHeight() + " != " + frame.getWidth() + "x" + frame.getHeight());

            PsxYCbCrImage psxImage = new PsxYCbCrImage(bi);
            MdecEncoder encoder = new MdecEncoder(psxImage, frame.getWidth(), frame.getHeight());
            abNewFrame = compressor.compressFull(abExistingFrame, frame.getFrame(), encoder, fbs);
        }

        if (abNewFrame.length > frame.getDemuxSize())
            throw new MdecException.Compress("Demux data does fit in frame {0}!! Available size {1,number,#}, needed size {2,number,#}", // I18N
                    getFrame(), frame.getDemuxSize(), abNewFrame.length);

        // find out how many bytes and mdec codes are used by the new frame
        BitStreamUncompressor bsu = BitStreamUncompressor.identifyUncompressor(abNewFrame);
        bsu.reset(abNewFrame, abNewFrame.length);
        bsu.readToEnd(frame.getWidth(), frame.getHeight());
        bsu.skipPaddingBits();

        frame.writeToSectors(abNewFrame, ((bsu.getBitPosition() + 15) / 16) * 2, bsu.getMdecCodeCount(), cd, fbs);
    }

}

