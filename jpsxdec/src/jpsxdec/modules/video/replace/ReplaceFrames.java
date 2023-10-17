/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.video.framenumber.FrameCompareIs;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.sectorbased.DiscItemSectorBasedVideoStream;
import jpsxdec.modules.video.sectorbased.ISectorBasedDemuxedFrame;
import jpsxdec.modules.video.sectorbased.SectorClaimToSectorBasedDemuxedFrame;
import jpsxdec.util.IO;
import jpsxdec.util.TaskCanceledException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
<?xml version="1.0"?>
<str-replace version="0.3">

    <replace frame="14" format="bmp">newframe14.bmp</replace>

    <partial-replace frame="3" tolerance="5" mask="test.png" format="png" rect="20,15,200,150">
        test.png
    </partial-replace>

</str-replace>
*/
public class ReplaceFrames {

    public static class XmlFileNotFoundException extends FileNotFoundException {

        public XmlFileNotFoundException(FileNotFoundException cause) {
            initCause(cause);
        }

    }

    public static class XmlReadException extends IOException {

        public XmlReadException(IOException cause) {
            super(cause);
        }

    }

    private static final String VERSION = "0.3";

    private final ArrayList<ReplaceFrameFull> _replacers = new ArrayList<ReplaceFrameFull>();

    public ReplaceFrames() {}

    public ReplaceFrames(@Nonnull String sXmlConfig)
            throws XmlFileNotFoundException, XmlReadException, LocalizedDeserializationFail
    {
        this(new File(sXmlConfig));
    }
    public ReplaceFrames(@Nonnull File xmlConfig)
            throws XmlFileNotFoundException, XmlReadException, LocalizedDeserializationFail
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
        Document doc;
        try {
            FileInputStream fis;
            try {
                fis = new FileInputStream(xmlConfig);
            } catch (FileNotFoundException ex) {
                throw new XmlFileNotFoundException(ex);
            }

            try {
                doc = db.parse(fis); // .parse() treats File like URL, must open manually
            } catch (IOException ex) {
                throw new XmlReadException(ex);
            } finally {
                IO.closeSilently(fis, Logger.getLogger(ReplaceFrames.class.getName()));
            }
        } catch (SAXException ex) {
            // Unforunately it seems SAXException does not localize their messages
            // https://netbeans.org/bugzilla/show_bug.cgi?id=102370
            throw new LocalizedDeserializationFail(I.REPLACE_FRAME_XML_ERROR(ex.getLocalizedMessage()), ex);
        }

        Element root = doc.getDocumentElement();
        root.normalize();
        if (!"str-replace".equals(root.getNodeName()))
            throw new LocalizedDeserializationFail(I.CMD_REPLACE_XML_INVALID_ROOT_NODE(root.getNodeName()));
        if (!VERSION.equals(root.getAttribute("version")))
            throw new LocalizedDeserializationFail(I.CMD_REPLACE_XML_INVALID_VERSION(root.getAttribute("version")));

        NodeList nodeLst = root.getChildNodes();

        for (int i = 0; i < nodeLst.getLength(); i++) {

            Node node = nodeLst.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            ReplaceFrameFull replace = null;
            Element element = (Element)node;
            if (ReplaceFrameFull.XML_TAG_NAME.equals(element.getNodeName())) {
                replace = new ReplaceFrameFull(element);
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
            for (ReplaceFrameFull replacer : _replacers) {
                root.appendChild(replacer.serialize(document));
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File(sFile));
            transformer.transform(source, result);
        } catch (ParserConfigurationException | TransformerException ex) {
            throw new IOException(ex);
        }
    }

    /** Returns null if no match. */
    protected @CheckForNull ReplaceFrameFull getFrameToReplace(@Nonnull FrameNumber frame) {
        for (ReplaceFrameFull replacer : _replacers) {
            if (replacer.getFrameLookup().compareTo(frame) == FrameCompareIs.EQUAL)
                return replacer;
        }
        return null;
    }

    public void addFrameToReplace(@Nonnull ReplaceFrameFull replace) {
        _replacers.add(replace);
    }

    public void replaceFrames(@Nonnull DiscItemSectorBasedVideoStream vidItem,
                              final @Nonnull DiscPatcher patcher,
                              final @Nonnull ProgressLogger pl)
            throws LoggedFailure, TaskCanceledException
    {
        SectorClaimToSectorBasedDemuxedFrame demuxer = vidItem.makeDemuxer();
        ReplaceFrameListener replaceListener = new ReplaceFrameListener(pl, patcher);
        demuxer.setFrameListener(replaceListener);

        pl.progressStart(vidItem.getSectorLength());
        SectorClaimSystem it = vidItem.createClaimSystem();
        demuxer.attachToSectorClaimer(it);
        for (int iSector = 0; it.hasNext(); iSector++) {
            try {
                IIdentifiedSector sector = it.next(pl);
            } catch (CdException.Read ex) {
                throw new LoggedFailure(pl, Level.SEVERE,
                        I.IO_READING_FROM_FILE_ERROR_NAME(ex.getFile().toString()), ex);
            }
            pl.progressUpdate(iSector);
            if (pl.isSeekingEvent() && replaceListener.currentFrameNum != null)
                pl.event(replaceListener.currentFrameNum.getIndexDescription());

            if (replaceListener.exception != null)
                throw replaceListener.exception;
        }
        it.flush(pl);
        pl.progressEnd();
    }

    private class ReplaceFrameListener implements ISectorBasedDemuxedFrame.Listener {

        @Nonnull
        private final ProgressLogger _pl;
        @Nonnull
        private final DiscPatcher _patcher;

        @CheckForNull
        public FrameNumber currentFrameNum;

        @CheckForNull
        public LoggedFailure exception;

        public ReplaceFrameListener(@Nonnull ProgressLogger pl, @Nonnull DiscPatcher patcher) {
            _pl = pl;
            _patcher = patcher;
        }

        @Override
        public void frameComplete(@Nonnull ISectorBasedDemuxedFrame frame) {
            currentFrameNum = frame.getFrame();

            ReplaceFrameFull replacer = getFrameToReplace(frame.getFrame());
            if (replacer != null) {
                try {
                    _pl.log(Level.INFO, I.CMD_REPLACING_FRAME_WITH_FILE(replacer.getFrameLookup().toString(),
                                                                        replacer.getImageFile()));
                    replacer.replace(frame, _patcher, _pl);
                } catch (LoggedFailure ex) {
                    exception = ex;
                }
            }

        }
    }

}

