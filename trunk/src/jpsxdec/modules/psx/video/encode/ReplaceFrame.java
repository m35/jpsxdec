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


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.modules.JPSXModule;
import jpsxdec.modules.psx.str.FrameDemuxer;
import jpsxdec.modules.psx.str.IVideoSector;
import jpsxdec.modules.psx.video.PsxYCbCrImage;
import jpsxdec.modules.psx.video.bitstreams.BitStreamCompressor;
import jpsxdec.modules.psx.video.bitstreams.BitStreamUncompressor;
import jpsxdec.modules.psx.video.mdec.DecodingException;
import jpsxdec.modules.psx.video.mdec.MdecInputStreamReader;
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

    public void replace(FrameDemuxer demuxer, CDFileSectorReader cd, FeedbackStream fbs) throws IOException, DecodingException, NotThisTypeException {
        // identify existing frame bs format
        byte[] abExistingFrame = new byte[demuxer.getDemuxSize()];
        demuxer.copyDemuxData(abExistingFrame);
        BitStreamUncompressor uncompressor = JPSXModule.identifyUncompressor(abExistingFrame, 0, demuxer.getFrame());

        CompressedImage newFrame;

        if ("bs".equals(_sFormat)) {
            // glean the necessary info from already compressed frame
            byte[] abNewDemux = IO.readFile(_imageFile);

            if (abNewDemux.length > demuxer.getDemuxSize()) {
                throw new RuntimeException(String.format(
                        "Demux data does fit in frame %d!! Available size %d, needed size %d",
                        getFrame(), demuxer.getDemuxSize(), abNewDemux.length));
            }
            BitStreamUncompressor un = JPSXModule.identifyUncompressor(abNewDemux, 0, demuxer.getFrame());
            un.reset(abNewDemux);

            ParsedMdecImage parsed = new ParsedMdecImage(demuxer.getWidth(), demuxer.getHeight());
            parsed.readFrom(un);

            newFrame = new CompressedImage();
            newFrame.iMdecCodeCount = parsed.getMdecCodeCount();
            newFrame.iLuminQscale = un.getLuminQscale();
            newFrame.iChromQscale = un.getChromQscale();

        } else if ("mdec".equals(_sFormat)) {
            // compress frame
            ParsedMdecImage parsed = new ParsedMdecImage(demuxer.getWidth(), demuxer.getHeight());
            parsed.readFrom(new MdecInputStreamReader(_imageFile));

            newFrame = new CompressedImage();
            newFrame.iMdecCodeCount = parsed.getMdecCodeCount();
            newFrame.iLuminQscale = parsed.getLuminQscale();
            newFrame.iChromQscale = parsed.getChromQscale();

            BitStreamCompressor compressor = uncompressor.makeCompressor();
            byte[] abNewDemux = compressor.compress(parsed.getStream(),
                    newFrame.iLuminQscale, newFrame.iChromQscale,
                    newFrame.iMdecCodeCount);

            newFrame.abBitStream = abNewDemux;

        } else {
            BitStreamCompressor compressor = uncompressor.makeCompressor();
            newFrame = compressReplacement(demuxer, compressor, fbs);
        }

        if (newFrame.abBitStream.length > demuxer.getDemuxSize())
            throw new RuntimeException(String.format(
                    "Demux data does fit in frame %d!! Available size %d, needed size %d",
                    getFrame(), demuxer.getDemuxSize(), newFrame.abBitStream.length));

        int iDemuxOfs = 0;
        for (int i = 0; i < demuxer.getChunksInFrame(); i++) {
            IVideoSector vidSector = demuxer.getChunk(i);
            if (vidSector != null) {
                iDemuxOfs += vidSector.replaceFrameData(cd, newFrame.abBitStream, iDemuxOfs,
                        newFrame.iLuminQscale, newFrame.iChromQscale,
                        newFrame.iMdecCodeCount);
            } else {
                fbs.printlnWarn("Trying to replace a frame with missing chunks??");
            }
        }

    }

    private CompressedImage compressReplacement(FrameDemuxer demuxer, BitStreamCompressor compressor, FeedbackStream fbs) throws DecodingException, IOException {

        BufferedImage bi = ImageIO.read(_imageFile);
        
        if (bi.getWidth() !=  ((demuxer.getWidth() +15)& ~15) ||
            bi.getHeight() != ((demuxer.getHeight()+15)& ~15))
            throw new IllegalArgumentException("Replacement frame has the wrong dimensions.");
        
        PsxYCbCrImage psxImage = new PsxYCbCrImage(bi);

        ParsedMdecImage parsedNew;
        byte[] abNewDemux;
        int iLuminQscale = 1;
        int iChromQscale = 1;
        while (true) {
            fbs.println("Trying qscale lumin: " + iLuminQscale + " chrom: " + iChromQscale);
            MdecEncoder encoded = new MdecEncoder(psxImage, iLuminQscale, iChromQscale);

            parsedNew = new ParsedMdecImage(demuxer.getWidth(), demuxer.getHeight());
            parsedNew.readFrom(encoded.getStream());

            abNewDemux = compressor.compress(parsedNew.getStream(), iLuminQscale, iChromQscale, parsedNew.getMdecCodeCount());
            int iNewDemuxSize = abNewDemux.length;
            if (iNewDemuxSize <= demuxer.getDemuxSize()) {
                fbs.println(String.format("  New frame %d demux size %d <= max source %d ",
                                demuxer.getFrame(), iNewDemuxSize, demuxer.getDemuxSize()));
                break;
            } else {
                fbs.println(String.format("  >>> New frame %d demux size %d > max source %d <<<",
                                demuxer.getFrame(), iNewDemuxSize, demuxer.getDemuxSize()));
            }

            if (compressor.separateQscales()) {
                if (iLuminQscale == iChromQscale)
                    iChromQscale++;
                else if (iLuminQscale < iChromQscale) {
                    iLuminQscale++;
                    iChromQscale--;
                } else {
                    iChromQscale++;
                }
            } else {
                iLuminQscale++;
                iChromQscale++;
            }
        }

        CompressedImage newFrame = new CompressedImage();
        newFrame.abBitStream = abNewDemux;
        newFrame.iChromQscale = iChromQscale;
        newFrame.iLuminQscale = iLuminQscale;
        newFrame.iMdecCodeCount = parsedNew.getMdecCodeCount();
        
        return newFrame;
    }

    private static class CompressedImage {
        public byte[] abBitStream;
        public int iLuminQscale;
        public int iChromQscale;
        public int iMdecCodeCount;
    }

}

