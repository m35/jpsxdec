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

package jpsxdec.discitems;

import argparser.ArgParser;
import argparser.BooleanHolder;
import com.jhlabs.awt.ParagraphLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.I18N;
import jpsxdec.LocalizedMessage;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.ProgressListenerLogger;
import jpsxdec.util.TaskCanceledException;

/** Represents an ISO9660 file on the disc. */
public class DiscItemISO9660File extends DiscItem {

    private static final Logger LOG = Logger.getLogger(DiscItemISO9660File.class.getName());
    public static final String TYPE_ID = "File";

    private final static String SIZE_KEY = "Size";
    private final static String PATH_KEY = "Path";
    private final File _path;
    private final long _lngSize;

    private final SortedSet<DiscItem> _children = new TreeSet<DiscItem>();
    
    public DiscItemISO9660File(int iStartSector, int iEndSector, File path, long lngSize) {
        super(iStartSector, iEndSector);
        _path = path;
        _lngSize = lngSize;
        // we already know the IndexId, so set it now
        super.setIndexId(new IndexId(path));
    }

    public DiscItemISO9660File(SerializedDiscItem fields) throws NotThisTypeException {
        super(fields);
        _lngSize = fields.getLong(SIZE_KEY);
        _path = new File(fields.getString(PATH_KEY));
    }

    @Override
    public SerializedDiscItem serialize() {
        SerializedDiscItem fields = super.serialize();
        fields.addNumber(SIZE_KEY, _lngSize);
        fields.addString(PATH_KEY, Misc.forwardSlashPath(_path));
        return fields;
    }

    @Override
    public String getSerializationTypeId() {
        return TYPE_ID;
    }

    /** {@inheritDoc}
     * <p>
     * Also set all the child {@link IndexId}s. */
    @Override
    public boolean setIndexId(IndexId id) {
        IndexId childId = getIndexId().createChild();
        for (DiscItem child : _children) {
            if (child.setIndexId(childId))
                childId = childId.createNext();
            else
                LOG.log(Level.INFO, "Child rejected id {0}", child);
        }
        return false;
    }

    @Override
    public int getParentRating(DiscItem child) {
        if (child instanceof DiscItemISO9660File)
            return 0;

        return getOverlap(child)*100 / child.getSectorLength();
    }
    
    @Override
    public boolean addChild(DiscItem child) {
        if (getParentRating(child) == 0)
            return false;
        _children.add(child);
        return true;
    }

    @Override
    public int getChildCount() {
        return _children.size();
    }

    @Override
    public Iterable<DiscItem> getChildren() {
        return _children;
    }

    public File getPath() {
        return _path;
    }

    public long getSize() {
        return _lngSize;
    }

    public ISO9660SaverBuilder makeSaverBuilder() {
        return new ISO9660SaverBuilder();
    }

    /** Stream of user data (not raw). 
     * Because a file could contain Form 2 sectors, that may make the number of
     * bytes readable from the stream different from the reported file size.
     * So the stream will end with the full data of the file's last sector
     * since it may be impossible to determine exactly how many bytes should be
     * read from the final sector.  */
    public InputStream getUserDataStream() {
        return new DemuxedSectorInputStream(getSourceCd(), getStartSector(), 0, getEndSector());
    }

    /** {@inheritDoc}
     * Also does special a bit of checking for items that break file boundaries.
     */
    @Override
    public int getOverlap(DiscItem other) {
        int iOverlap = super.getOverlap(other);
        if (iOverlap > 0) {
            // if there is overlap, then the other item should technically fall
            // entirely within this iso9660 file, except they don't always...
            if (other.getStartSector() < getStartSector() || other.getEndSector() > getEndSector()) {
                LOG.log(Level.WARNING, "{0} breaks this file boundaries {1}", new Object[]{other, this});
            }
        }
        return iOverlap;
    }


    @Override
    public GeneralType getType() {
        return GeneralType.File;
    }

    @Override
    public LocalizedMessage getInterestingDescription() {
        return new LocalizedMessage("{0} bytes", _lngSize); // I18N
    }

    public class ISO9660SaverBuilder extends DiscItemSaverBuilder {

        public ISO9660SaverBuilder() {
            resetToDefaults();
        }

        public void resetToDefaults() {
            setSaveRaw(true);
        }

        @Override
        public boolean copySettingsTo(DiscItemSaverBuilder other) {
            if (other instanceof ISO9660SaverBuilder) {
                ISO9660SaverBuilder o = (ISO9660SaverBuilder) other;
                o.setSaveRaw(getSaveRaw());
                return true;
            }
            return false;
        }

        // ............................................

        public String getFileName() {
            return _path.getPath();
        }

        // ............................................

        private boolean __blnSaveRaw = true;
        public boolean getSaveRaw() {
            return getSaveRaw_enabled() && __blnSaveRaw;
        }
        public void setSaveRaw(boolean blnSaveRaw) {
            __blnSaveRaw = blnSaveRaw;
            firePossibleChange();
        }
        public boolean getSaveRaw_enabled() {
            return getSourceCd().hasSectorHeader();
        }

        // ............................................
        
        public DiscItemSaverBuilderGui getOptionPane() {
            return new ISO9660FileSaverBuilderGui(this);
        }

        public IDiscItemSaver makeSaver(File directory) {
            return new ISO9660FileSaver(__blnSaveRaw, directory);
        }

        public String[] commandLineOptions(String[] asArgs, FeedbackStream infoStream) {
            if (asArgs == null) return null;

            ArgParser parser = new ArgParser("", false);

            BooleanHolder save2048 = new BooleanHolder();
            parser.addOption("-iso %v", save2048);

            String[] asRemain = null;
            asRemain = parser.matchAllArgs(asArgs, 0, 0);

            setSaveRaw(!save2048.value);

            return asRemain;
        }

        public void printHelp(FeedbackStream fbs) {
            fbs.indent();
            if (getSourceCd().hasSectorHeader())
                fbs.println("-iso   save as 2048 sectors (default raw 2352 sectors)"); // I18N
            else
                fbs.println("[no options available]"); // I18N
            fbs.outdent();
        }

    }

    private static class ISO9660FileSaverBuilderGui extends DiscItemSaverBuilderGui<ISO9660SaverBuilder> {

        public ISO9660FileSaverBuilderGui(ISO9660SaverBuilder sourceBldr) {
            super(sourceBldr, new ParagraphLayout());
            setParagraphLayoutPanel(this);
            addListeners(
                new FileName(),
                new SaveRaw()
            );
        }

        private class FileName implements ChangeListener {
            JLabel __label = new JLabel(I18N.S("Save as:")); // I18N
            JLabel __name;
            public FileName() {
                __name = new JLabel(_writerBuilder.getFileName());
                add(__label, ParagraphLayout.NEW_PARAGRAPH);
                add(__name);
            }
            public void stateChanged(ChangeEvent e) {
                if (!__name.getText().equals(_writerBuilder.getFileName()))
                    __name.setText(_writerBuilder.getFileName());
            }
        }

        private class SaveRaw extends AbstractCheck {

            public SaveRaw() { super(I18N.S("Save raw:")); } // I18N
            public boolean isSelected() { return _writerBuilder.getSaveRaw(); }
            public void setSelected(boolean b) { _writerBuilder.setSaveRaw(b); }
            public boolean isEnabled() { return _writerBuilder.getSaveRaw_enabled(); }

        }
        
    }

    private class ISO9660FileSaver implements IDiscItemSaver {
        private final boolean __blnSaveRaw;
        private final File __outputDir;
        private File __generatedFile;

        public ISO9660FileSaver(boolean blnSaveRaw, File outputDir) {
            __blnSaveRaw = blnSaveRaw;
            __outputDir = outputDir;
        }

        public String getInput() {
            return getIndexId().toString();
        }

        public DiscItemISO9660File getDiscItem() {
            return DiscItemISO9660File.this;
        }

        public String getOutputSummary() {
            return getPath().getPath();
        }

        public void startSave(ProgressListenerLogger pll) throws IOException, TaskCanceledException {
            File outputFile = new File(__outputDir, _path.getPath());

            IO.makeDirsForFile(outputFile);

            final double dblSectLen = getSectorLength();
            final int iStartSect = getStartSector();
            final int iEndSect = getEndSector();
            FileOutputStream fos = new FileOutputStream(outputFile);
            __generatedFile = outputFile;
            pll.progressStart();
            for (int iSector = iStartSect; iSector <= iEndSect; iSector++) {
                CdSector cdSector = getSourceCd().getSector(iSector);
                if (__blnSaveRaw)
                    fos.write(cdSector.getRawSectorDataCopy());
                else
                    fos.write(cdSector.getCdUserDataCopy());
                pll.progressUpdate((iSector - iStartSect) / dblSectLen);
            }
            fos.close();
            pll.progressEnd();
        }

        public void printSelectedOptions(PrintStream ps) {
            if (__blnSaveRaw)
                ps.println(I18N.S("Saving with raw sectors")); // I18N
            else
                ps.println(I18N.S("Saving with iso sectors")); // I18N
        }

        public File[] getGeneratedFiles() {
            if (__generatedFile == null)
                return null;
            else
                return new File[] {__generatedFile};
        }
    }

}
