/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import argparser.BooleanHolder;
import com.jhlabs.awt.ParagraphLayout;
import java.awt.BorderLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import jpsxdec.discitems.savers.CombinedBuilderListener;
import jpsxdec.discitems.savers.ParagraphPanel;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.LocalizedFileNotFoundException;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.util.ArgParser;
import jpsxdec.util.DeserializationFail;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.LoggedFailure;
import jpsxdec.util.Misc;
import jpsxdec.util.ProgressLogger;
import jpsxdec.util.TaskCanceledException;

/** Represents an ISO9660 file on the disc. */
public class DiscItemISO9660File extends DiscItem {

    private static final Logger LOG = Logger.getLogger(DiscItemISO9660File.class.getName());
    
    public static final String TYPE_ID = "File";

    private final static String PATH_KEY = "Path";
    @Nonnull
    private final File _path;
    private final static String SIZE_KEY = "Size";
    private final long _lngSize;
    private final static String HAS_FORM2_KEY = "Has mode 2 form 2";
    private final boolean _blnHasMode2Form2;
    private final static String HAS_CD_AUDIO_KEY = "Has CD audio";
    private final boolean _blnHasCdAudio;

    private final SortedSet<DiscItem> _children = new TreeSet<DiscItem>();
    
    public DiscItemISO9660File(@Nonnull CdFileSectorReader cd,
                               int iStartSector, int iEndSector,
                               @Nonnull File path, long lngSize,
                               boolean hasMode2Form2, boolean blnHasCdAudio)
    {
        super(cd, iStartSector, iEndSector);
        _path = path;
        _lngSize = lngSize;
        _blnHasMode2Form2 = hasMode2Form2;
        _blnHasCdAudio = blnHasCdAudio;
        // we already know the IndexId, so set it now
        super.setIndexId(new IndexId(path));
    }

    public DiscItemISO9660File(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields) 
            throws DeserializationFail
    {
        super(cd, fields);
        _path = new File(fields.getString(PATH_KEY));
        _lngSize = fields.getLong(SIZE_KEY);
        _blnHasMode2Form2 = fields.getYesNo(HAS_FORM2_KEY);
        _blnHasCdAudio = fields.getYesNo(HAS_CD_AUDIO_KEY);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem fields = super.serialize();
        fields.addNumber(SIZE_KEY, _lngSize);
        fields.addString(PATH_KEY, Misc.forwardSlashPath(_path));
        fields.addYesNo(HAS_FORM2_KEY, _blnHasMode2Form2);
        fields.addYesNo(HAS_CD_AUDIO_KEY, _blnHasCdAudio);
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
        if (!_children.add(child))
            throw new RuntimeException("Disc item "+child+" already exists in ISO file set " + this);
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
                LOG.log(Level.WARNING, "Broke file boundary: {0} breaks {1}", new Object[]{other, this});
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
            firePossibleChange();
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
            if (getSaveRaw_enabled())
                return __blnSaveRaw;
            else {
                if (_blnHasCdAudio || _blnHasMode2Form2)
                    return true;
                else // does not have sector header
                    return false;
            }
        }
        public void setSaveRaw(boolean blnSaveRaw) {
            __blnSaveRaw = blnSaveRaw;
            firePossibleChange();
        }
        public boolean getSaveRaw_enabled() {
            return getSourceCd().hasSectorHeader() && !_blnHasCdAudio && !_blnHasMode2Form2;
        }

        // ............................................
        
        public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
            return new ISO9660FileSaverBuilderGui(this);
        }

        public @Nonnull IDiscItemSaver makeSaver(@CheckForNull File directory) {
            return new ISO9660FileSaver(DiscItemISO9660File.this, getSaveRaw(), directory);
        }

        public void commandLineOptions(@Nonnull ArgParser ap,
                                       @Nonnull FeedbackStream infoStream)
        {
            if (!ap.hasRemaining())
                return;

            BooleanHolder save2048 = ap.addBoolOption("-iso");

            ap.match();

            setSaveRaw(!save2048.value);
        }

        public void printHelp(@Nonnull FeedbackStream fbs) {
            CdFileSectorReader cd = getSourceCd();
            if (cd.getRawSectorSize() == CdFileSectorReader.SECTOR_SIZE_2048_ISO)
                fbs.println(I.CMD_ISOFILE_HELP_NO_OPTIONS());
            else
                fbs.println(I.CMD_ISOFILE_ISO_HELP(cd.getRawSectorSize()));
        }

    }

    private static class ISO9660FileSaverBuilderGui extends DiscItemSaverBuilderGui {

        @Nonnull
        private final CombinedBuilderListener<ISO9660SaverBuilder> _bh;

        public ISO9660FileSaverBuilderGui(@Nonnull ISO9660SaverBuilder sourceBldr) {
            super(new BorderLayout());
            _bh = new CombinedBuilderListener<ISO9660SaverBuilder>(sourceBldr);
            add(new PPanel(_bh), BorderLayout.NORTH);
        }

        @Override
        public boolean useSaverBuilder(@Nonnull DiscItemSaverBuilder saverBuilder) {
            return _bh.changeSourceBuilder(saverBuilder);
        }

        private static class PPanel extends ParagraphPanel {
            @Nonnull
            private final CombinedBuilderListener<ISO9660SaverBuilder> _bh;

            public PPanel(@Nonnull CombinedBuilderListener<ISO9660SaverBuilder> bh) {
                _bh = bh;
                _bh.addListeners(
                    new FileName(),
                    new SaveRaw()
                );
            }

            private class FileName implements ChangeListener {
                final JLabel __label = new JLabel(I.GUI_SAVE_AS_LABEL().getLocalizedMessage());
                @Nonnull final JLabel __name;
                public FileName() {
                    __name = new JLabel(_bh.getBuilder().getFileName());
                    add(__label, ParagraphLayout.NEW_PARAGRAPH);
                    add(__name);
                }
                public void stateChanged(ChangeEvent e) {
                    if (!__name.getText().equals(_bh.getBuilder().getFileName()))
                        __name.setText(_bh.getBuilder().getFileName());
                }
            }

            private class SaveRaw extends AbstractCheck {

                public SaveRaw() { super(I.GUI_ISOFILE_SAVE_RAW_LABEL()); }
                public boolean isSelected() { return _bh.getBuilder().getSaveRaw(); }
                public void setSelected(boolean b) { _bh.getBuilder().setSaveRaw(b); }
                public boolean isEnabled() { return _bh.getBuilder().getSaveRaw_enabled(); }

            }
        }
    }

    private static class ISO9660FileSaver implements IDiscItemSaver {
        @Nonnull
        private final DiscItemISO9660File _item;
        private final boolean __blnSaveRaw;
        @CheckForNull
        private final File __outputDir;
        @CheckForNull
        private File __generatedFile;

        public ISO9660FileSaver(@Nonnull DiscItemISO9660File item,
                                boolean blnSaveRaw, @CheckForNull File outputDir)
        {
            _item = item;
            __blnSaveRaw = blnSaveRaw;
            __outputDir = outputDir;
        }

        public @Nonnull String getInput() {
            return _item.getIndexId().toString();
        }

        public @Nonnull DiscItemISO9660File getDiscItem() {
            return _item;
        }

        public @Nonnull ILocalizedMessage getOutputSummary() {
            return new UnlocalizedMessage(_item.getPath().getPath());
        }

        public void startSave(@Nonnull ProgressLogger pl) throws LoggedFailure, TaskCanceledException {
            File outputFile = new File(__outputDir, _item.getPath().getPath());

            try {
                IO.makeDirsForFile(outputFile);
            } catch (LocalizedFileNotFoundException ex) {
                throw new LoggedFailure(pl, Level.SEVERE, ex.getSourceMessage(), ex);
            }

            FileOutputStream fos;
            try {
                fos = new FileOutputStream(outputFile);
            } catch (FileNotFoundException ex) {
                throw new LoggedFailure(pl, Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(outputFile.toString()), ex);
            }
            try {
                __generatedFile = outputFile;
                int iSectorLength = _item.getSectorLength();
                pl.progressStart(iSectorLength);
                // TODO: only save the bytes associated with this file from the last sector?
                for (int iSector = 0; iSector < iSectorLength; iSector++) {
                    CdSector cdSector;
                    try {
                        cdSector = _item.getRelativeSector(iSector);
                    } catch (IOException ex) {
                        throw new LoggedFailure(pl, Level.SEVERE, I.IO_READING_FROM_FILE_ERROR_NAME(
                                                _item.getSourceCd().getSourceFile().toString()), ex);
                    }

                    try {
                        if (__blnSaveRaw)
                            fos.write(cdSector.getRawSectorDataCopy());
                        else
                            fos.write(cdSector.getCdUserDataCopy());
                    } catch (IOException ex) {
                        throw new LoggedFailure(pl, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(outputFile.toString()), ex);
                    }
                    
                    pl.progressUpdate(iSector);
                }
            } finally {
                IO.closeSilently(fos, LOG);
            }
            pl.progressEnd();
        }

        public void printSelectedOptions(@Nonnull FeedbackStream fbs) {
            if (__blnSaveRaw)
                fbs.println(I.CMD_ISOFILE_SAVING_RAW());
            else
                fbs.println(I.CMD_ISOFILE_SAVING_ISO());
        }

        public @CheckForNull File[] getGeneratedFiles() {
            if (__generatedFile == null)
                return null;
            else
                return new File[] {__generatedFile};
        }
    }

}
