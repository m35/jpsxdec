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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
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
    @Nonnull
    private final File _path;
    private final long _lngSize;

    private final SortedSet<DiscItem> _children = new TreeSet<DiscItem>();
    
    public DiscItemISO9660File(@Nonnull CdFileSectorReader cd,
                               int iStartSector, int iEndSector,
                               @Nonnull File path, long lngSize)
    {
        super(cd, iStartSector, iEndSector);
        _path = path;
        _lngSize = lngSize;
        // we already know the IndexId, so set it now
        super.setIndexId(new IndexId(path));
    }

    public DiscItemISO9660File(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields) 
            throws NotThisTypeException
    {
        super(cd, fields);
        _lngSize = fields.getLong(SIZE_KEY);
        _path = new File(fields.getString(PATH_KEY));
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem fields = super.serialize();
        fields.addNumber(SIZE_KEY, _lngSize);
        fields.addString(PATH_KEY, Misc.forwardSlashPath(_path));
        return fields;
    }

    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    /** {@inheritDoc}
     * <p>
     * Also set all the child {@link IndexId}s. */
    @Override
    public boolean setIndexId(@Nonnull IndexId id_ignored) {
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
    public int getParentRating(@Nonnull DiscItem child) {
        if (child instanceof DiscItemISO9660File)
            return 0;

        return getOverlap(child)*100 / child.getSectorLength();
    }
    
    @Override
    public boolean addChild(@Nonnull DiscItem child) {
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
    public @Nonnull Iterable<DiscItem> getChildren() {
        return _children;
    }

    public @Nonnull File getPath() {
        return _path;
    }

    public long getSize() {
        return _lngSize;
    }

    public @Nonnull ISO9660SaverBuilder makeSaverBuilder() {
        return new ISO9660SaverBuilder();
    }

    /** Stream of user data (not raw). 
     * Because a file could contain Form 2 sectors, that may make the number of
     * bytes readable from the stream different from the reported file size.
     * So the stream will end with the full data of the file's last sector
     * since it may be impossible to determine exactly how many bytes should be
     * read from the final sector.  */
    public @Nonnull InputStream getUserDataStream() {
        return new DemuxedSectorInputStream(getSourceCd(), getStartSector(), 0, getEndSector());
    }

    /** {@inheritDoc}
     * Also does special a bit of checking for items that break file boundaries.
     */
    @Override
    public int getOverlap(@Nonnull DiscItem other) {
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
    public @Nonnull GeneralType getType() {
        return GeneralType.File;
    }

    @Override
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        return I.GUI_ISOFILE_DETAILS(_lngSize);
    }

    public class ISO9660SaverBuilder extends DiscItemSaverBuilder {

        public ISO9660SaverBuilder() {
            resetToDefaults();
        }

        public void resetToDefaults() {
            setSaveRaw(true);
        }

        @Override
        public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder other) {
            if (other instanceof ISO9660SaverBuilder) {
                ISO9660SaverBuilder o = (ISO9660SaverBuilder) other;
                o.setSaveRaw(getSaveRaw());
                return true;
            }
            return false;
        }

        // ............................................

        public @Nonnull String getFileName() {
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
        
        public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
            return new ISO9660FileSaverBuilderGui(this);
        }

        public @Nonnull IDiscItemSaver makeSaver(@CheckForNull File directory) {
            return new ISO9660FileSaver(__blnSaveRaw, directory);
        }

        public @CheckForNull String[] commandLineOptions(@CheckForNull String[] asArgs, 
                                                         @Nonnull FeedbackStream infoStream)
        {
            if (asArgs == null) return null;

            ArgParser parser = new ArgParser("", false);

            BooleanHolder save2048 = new BooleanHolder();
            parser.addOption("-iso %v", save2048);

            String[] asRemain = parser.matchAllArgs(asArgs, 0, 0);

            setSaveRaw(!save2048.value);

            return asRemain;
        }

        public void printHelp(@Nonnull FeedbackStream fbs) {
            if (getSourceCd().hasSectorHeader())
                fbs.println(I.CMD_ISOFILE_ISO_HELP());
            else
                fbs.println(I.CMD_ISOFILE_HELP_NO_OPTIONS());
        }

    }

    private static class ISO9660FileSaverBuilderGui extends DiscItemSaverBuilderGui<ISO9660SaverBuilder> {

        public ISO9660FileSaverBuilderGui(@Nonnull ISO9660SaverBuilder sourceBldr) {
            super(sourceBldr, new ParagraphLayout());
            setParagraphLayoutPanel(this);
            addListeners(
                new FileName(),
                new SaveRaw()
            );
        }

        private class FileName implements ChangeListener {
            final JLabel __label = new JLabel(I.GUI_SAVE_AS_LABEL().getLocalizedMessage());
            @Nonnull final JLabel __name;
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

            public SaveRaw() { super(I.GUI_ISOFILE_SAVE_RAW_LABEL()); }
            public boolean isSelected() { return _writerBuilder.getSaveRaw(); }
            public void setSelected(boolean b) { _writerBuilder.setSaveRaw(b); }
            public boolean isEnabled() { return _writerBuilder.getSaveRaw_enabled(); }

        }
        
    }

    private class ISO9660FileSaver implements IDiscItemSaver {
        private final boolean __blnSaveRaw;
        @CheckForNull
        private final File __outputDir;
        @CheckForNull
        private File __generatedFile;

        public ISO9660FileSaver(boolean blnSaveRaw, @CheckForNull File outputDir) {
            __blnSaveRaw = blnSaveRaw;
            __outputDir = outputDir;
        }

        public @Nonnull String getInput() {
            return getIndexId().toString();
        }

        public @Nonnull DiscItemISO9660File getDiscItem() {
            return DiscItemISO9660File.this;
        }

        public @Nonnull ILocalizedMessage getOutputSummary() {
            return new UnlocalizedMessage(getPath().getPath());
        }

        public void startSave(@Nonnull ProgressListenerLogger pll) throws IOException, TaskCanceledException {
            File outputFile = new File(__outputDir, _path.getPath());

            IO.makeDirsForFile(outputFile);

            final double dblSectLen = getSectorLength();
            final int iStartSect = getStartSector();
            final int iEndSect = getEndSector();
            FileOutputStream fos = new FileOutputStream(outputFile);
            try {
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
            } finally {
                try {
                    fos.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
            pll.progressEnd();
        }

        public void printSelectedOptions(@Nonnull PrintStream ps) {
            if (__blnSaveRaw)
                ps.println(I.CMD_ISOFILE_SAVING_RAW());
            else
                ps.println(I.CMD_ISOFILE_SAVING_ISO());
        }

        public @CheckForNull File[] getGeneratedFiles() {
            if (__generatedFile == null)
                return null;
            else
                return new File[] {__generatedFile};
        }
    }

}
