/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jpsxdec.i18n.I;
import jpsxdec.util.IncompatibleException;
import jpsxdec.i18n.LocalizedIOException;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.NotThisTypeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
<?xml version="1.0"?>
<str-replace version="0.2">

    <replace frame="14" format="bmp">newframe14.bmp</replace>

    <partial-replace frame="3" tolerance="5" mask="test.png" format="png" rect="20,15,200,150">
        test.png
    </partial-replace>

</str-replace>
*/
public class ReplaceFrames {

    private static final String VERSION = "0.2";

    private final ArrayList<ReplaceFrame> _replacers =
            new ArrayList<ReplaceFrame>();

    public ReplaceFrames() {}

    public ReplaceFrames(@Nonnull String sXmlConfig) throws IOException, NotThisTypeException {
        this(new File(sXmlConfig));
    }
    public ReplaceFrames(@Nonnull File xmlConfig) throws IOException, NotThisTypeException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
        Document doc;
        try {
            FileInputStream fis = new FileInputStream(xmlConfig);
            try {
                doc = db.parse(fis); // .parse() treats File like URL, must open manually
            } finally {
                try {
                    fis.close();
                } catch (IOException ex) {
                    Logger.getLogger(ReplaceFrames.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (SAXException ex) {
            throw new NotThisTypeException(ex);
        }

        Element root = doc.getDocumentElement();
        root.normalize();
        if (!"str-replace".equals(root.getNodeName()))
            throw new NotThisTypeException(I.CMD_REPLACE_XML_INVALID_ROOT_NODE(root.getNodeName()));
        if (!VERSION.equals(root.getAttribute("version")))
            throw new NotThisTypeException(I.CMD_REPLACE_XML_INVALID_VERSION(root.getAttribute("version")));

        NodeList nodeLst = root.getChildNodes();

        for (int i = 0; i < nodeLst.getLength(); i++) {

            Node node = nodeLst.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            ReplaceFrame replace = null;
            Element element = (Element)node;
            if (ReplaceFrame.XML_TAG_NAME.equals(element.getNodeName())) {
                replace = new ReplaceFrame(element);
            } else if (ReplaceFramePartial.XML_TAG_NAME.equals(element.getNodeName())) {
                replace = new ReplaceFramePartial(element);
            }
            if (replace != null) {
                _replacers.add(replace);
            }
        }
            
    }

    public void save(@Nonnull String sFile) throws IOException {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = document.createElement("str-replace");
            root.setAttribute("version", VERSION);
            document.appendChild(root);
            for (ReplaceFrame replacer : _replacers) {
                root.appendChild(replacer.serialize(document));
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File(sFile));
            transformer.transform(source, result);
        } catch (TransformerException ex) {
            throw new LocalizedIOException(ex);
        } catch (ParserConfigurationException ex) {
            throw new LocalizedIOException(ex);
        }
    }

    /** Returns null if no match. */
    protected @CheckForNull ReplaceFrame getFrameToReplace(@Nonnull FrameNumber frame) {
        for (ReplaceFrame replacer : _replacers) {
            if (replacer.getFrame().compareTo(frame) == 0)
                return replacer;
        }
        return null;
    }

    public void addFrameToReplace(@Nonnull ReplaceFrame replace) {
        _replacers.add(replace);
    }

    public void replaceFrames(@Nonnull DiscItemVideoStream vidItem, 
                              final @Nonnull CdFileSectorReader cd,
                              final @Nonnull FeedbackStream fbs)
            throws IOException, NotThisTypeException, MdecException, IncompatibleException
    {
        final Throwable[] exception = new Throwable[1];
        
        ISectorFrameDemuxer demuxer;
        demuxer = vidItem.makeDemuxer();
        demuxer.setFrameListener(new ISectorFrameDemuxer.ICompletedFrameListener() {

            public void frameComplete(@Nonnull IDemuxedFrame frame) throws IOException {

                ReplaceFrame replacer = getFrameToReplace(frame.getFrame());
                if (replacer != null) {
                    fbs.println(I.CMD_REPLACING_FRAME_NUM(frame.getFrame()));
                    fbs.indent();
                    try {
                        fbs.println(I.CMD_REPLACING_FRAME_WITH_FILE(replacer.getImageFile()));
                        replacer.replace(frame, cd, fbs);
                    } catch (MdecException ex) {
                        exception[0] = ex;
                    } catch (NotThisTypeException ex) {
                        exception[0] = ex;
                    } catch (IncompatibleException ex) {
                        exception[0] = ex;
                    } finally {
                        fbs.outdent();
                    }
                }

            }
        });
        
        Logger log = Logger.getLogger("replace");

        for (int iSector = 0; iSector < vidItem.getSectorLength(); iSector++) {
            IdentifiedSector sector = vidItem.getRelativeIdentifiedSector(iSector);
            if (sector != null)
                demuxer.feedSector(sector, log);

            if (exception[0] != null) {
                if (exception[0] instanceof MdecException)
                    throw (MdecException)exception[0];
                else if (exception[0] instanceof NotThisTypeException)
                    throw (NotThisTypeException)exception[0];
                else if (exception[0] instanceof IncompatibleException)
                    throw (IncompatibleException)exception[0];
                else
                    throw new RuntimeException(exception[0]);
            }
        }

        demuxer.flush(log);
    }

}

