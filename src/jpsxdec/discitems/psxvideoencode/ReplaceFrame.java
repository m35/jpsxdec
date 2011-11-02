/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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


import java.util.Iterator;
import jpsxdec.psxvideo.encode.ParsedMdecImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.savers.DemuxedFrame;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.PsxYCbCrImage;
import jpsxdec.psxvideo.bitstreams.BitStreamCompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReplaceFrame {
	private final int _iFrame;
    private String _sFormat;
    private File _imageFile;

    public static final String XML_TAG_NAME = "replace";

    public ReplaceFrame(Element element) {
        this(element.getAttribute("frame"));
        setImageFile(element.getFirstChild().getNodeValue());
        setFormat(element.getAttribute("format"));
    }
    public Element serialize(Document document) {
        Element node = document.createElement(XML_TAG_NAME);
        node.setAttribute("frame", String.valueOf(getFrame()));
        node.setTextContent(getImageFile().toString());
        if (getFormat() != null)
            node.setAttribute("format", getFormat());
        return node;
    }

    public ReplaceFrame(String sFrameNumber) {
        this(Integer.parseInt(sFrameNumber.trim()));
    }
    public ReplaceFrame(int iFrame) {
        _iFrame = iFrame;
    }

    public int getFrame() {
        return _iFrame;
    }

    public File getImageFile() {
        return _imageFile;
    }

    public void setImageFile(String sImageFile) {
        setImageFile(new File(sImageFile.trim()));
    }
    public void setImageFile(File imageFile) {
        _imageFile = imageFile;
        if (!_imageFile.exists())
            throw new IllegalArgumentException("Unable to find " + _imageFile);
    }

    public String getFormat() {
        return _sFormat;
    }

    public void setFormat(String sFormat) {
        _sFormat = sFormat;
    }

    public void replace(DemuxedFrame frame, CdFileSectorReader cd, FeedbackStream fbs) throws IOException, NotThisTypeException, MdecException {
        // identify existing frame bs format
        byte[] abExistingFrame = frame.copyDemuxData(null);
        BitStreamUncompressor uncompressor = BitStreamUncompressor.identifyUncompressor(abExistingFrame);

        byte[] newFrame;

        if ("bs".equals(_sFormat)) {
            // glean the necessary info from already compressed frame
            newFrame = IO.readFile(_imageFile);

            if (newFrame.length > frame.getDemuxSize()) {
                throw new RuntimeException(String.format(
                        "Demux data does fit in frame %d!! Available size %d, needed size %d",
                        getFrame(), frame.getDemuxSize(), newFrame.length));
            }
        } else if ("mdec".equals(_sFormat)) {
            // compress frame
            ParsedMdecImage parsed = new ParsedMdecImage(frame.getWidth(), frame.getHeight());
            parsed.readFrom(new MdecInputStreamReader(_imageFile));

            BitStreamCompressor compressor = uncompressor.makeCompressor();
            newFrame = compressor.compress(parsed.getStream(), parsed.getMdecCodeCount());

        } else {
            newFrame = compressReplacement(frame, uncompressor, fbs);
        }

        if (newFrame.length > frame.getDemuxSize())
            throw new RuntimeException(String.format(
                    "Demux data does fit in frame %d!! Available size %d, needed size %d",
                    getFrame(), frame.getDemuxSize(), newFrame.length));

        BitStreamUncompressor bsu = BitStreamUncompressor.identifyUncompressor(newFrame);
        bsu.reset(newFrame);
        bsu.readToEnd(frame.getWidth(), frame.getHeight());
        bsu.skipPaddingBits();

        // +2 because getStreamPosition() returns the active word, not the next word to be read
        frame.writeToSectors(newFrame, bsu.getStreamPosition()+2, bsu.getMdecCodeCount(), cd, fbs);
    }

    private byte[] compressReplacement(DemuxedFrame frame, BitStreamUncompressor uncompressor, FeedbackStream fbs) throws IOException, NotThisTypeException, MdecException {

        BufferedImage bi = ImageIO.read(_imageFile);
        
        if (bi.getWidth()  != ((frame.getWidth() +15)& ~15) ||
            bi.getHeight() != ((frame.getHeight()+15)& ~15))
            throw new IllegalArgumentException("Replacement frame dimensions do not match frame to replace: " +
                    bi.getWidth() + "x" + bi.getHeight() + " != " + frame.getWidth() + "x" + frame.getHeight());
        
        PsxYCbCrImage psxImage = new PsxYCbCrImage(bi);

        ParsedMdecImage parsedNew;
        byte[] abNewDemux = null;
        MdecEncoder encoded = new MdecEncoder(psxImage);
        BitStreamCompressor compressor = uncompressor.makeCompressor();
        byte[] abOriginal = new byte[frame.getDemuxSize()];
        frame.copyDemuxData(abOriginal);
        uncompressor.reset(abOriginal);
        uncompressor.readToEnd(frame.getWidth(), frame.getHeight());
        Iterator<int[]> qscales = uncompressor.qscaleIterator(true);
        while (qscales.hasNext()) {
            int[] qs = qscales.next();
            fbs.println("Trying " + Arrays.toString(qs));

            parsedNew = new ParsedMdecImage(frame.getWidth(), frame.getHeight());
            parsedNew.readFrom(encoded.getStream(qs));

            try {
                abNewDemux = compressor.compress(parsedNew.getStream(), parsedNew.getMdecCodeCount());
                int iNewDemuxSize = abNewDemux.length;
                if (iNewDemuxSize <= frame.getDemuxSize()) {
                    fbs.println(String.format("  New frame %d demux size %d <= max source %d ",
                                    frame.getFrame(), iNewDemuxSize, frame.getDemuxSize()));
                    break;
                } else {
                    fbs.println(String.format("  >>> New frame %d demux size %d > max source %d <<<",
                                    frame.getFrame(), iNewDemuxSize, frame.getDemuxSize()));
                }
            } catch (MdecException.TooMuchEnergyToCompress ex) {
                fbs.printlnWarn(ex.getMessage());
            }
        }

        return abNewDemux;
    }



}

