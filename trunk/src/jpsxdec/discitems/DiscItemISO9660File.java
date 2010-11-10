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

package jpsxdec.discitems;

import argparser.ArgParser;
import argparser.BooleanHolder;
import com.jhlabs.awt.ParagraphLayout;
import com.l2fprod.common.swing.JDirectoryChooser;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import jpsxdec.BetterFileChooser;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.ProgressListener;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.TaskCanceledException;

/** Represents an ISO9660 file on the disc. */
public class DiscItemISO9660File extends DiscItem {

    private static final Logger log = Logger.getLogger(DiscItemISO9660File.class.getName());
    public static final String TYPE_ID = "File";

    private final File _path;
    private final long _lngSize;
    private int _iContainedItemCount;

    public DiscItemISO9660File(int iStartSector, int iEndSector, File path, long lngSize) {
        super(iStartSector, iEndSector);
        _path = path;
        _lngSize = lngSize;
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
                log.warning(other + " breaks this file boundaries " + this);
            }
        }
        return iOverlap;
    }


    public File getPath() {
        return _path;
    }

    public String getBaseName() {
        return Misc.getBaseName(_path.getName());
    }

    void incrementContainedItems() {
        _iContainedItemCount++;
    }

    int getContainedItemCount() {
        return _iContainedItemCount;
    }

    public DiscItemSerialization serialize() {
        DiscItemSerialization fields = super.superSerial(TYPE_ID);
        fields.addNumber("Size", _lngSize);
        fields.addString("Path", forwardSlashPath(_path));
        return fields;

    }

    private static final URI CURRENT_URI = new File(".").toURI();
    private static final String forwardSlashPath(File f) {
        return CURRENT_URI.relativize(f.toURI()).toString();
    }

    public DiscItemISO9660File(DiscItemSerialization fields) throws NotThisTypeException {
        super(fields);
        _lngSize = fields.getLong("Size");
        _path = new File(fields.getString("Path"));
    }

    public DiscItemSaverBuilder makeSaverBuilder() {
        return new ISO9660SaverBuilder();
    }

    public String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getInterestingDescription() {
        return DecimalFormat.getInstance().format(_lngSize) + " bytes";
    }

    private class ISO9660SaverBuilder extends DiscItemSaverBuilder {

        public ISO9660SaverBuilder() {
            resetToDefaults();
        }

        public void resetToDefaults() {
            setSaveRaw(true);
        }

        @Override
        public boolean copySettings(DiscItemSaverBuilder other) {
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
            return getSourceCD().hasSectorHeader();
        }

        // ............................................
        
        public DiscItemSaverBuilderGui getOptionPane() {
            return new ISO9660FileSaverBuilderGui(this);
        }

        public IDiscItemSaver makeSaver() {
            return new ISO9660FileSaver(__blnSaveRaw);
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
            if (getSourceCD().hasSectorHeader())
                fbs.println("-iso   save as 2048 sectors (default raw 2352 sectors)");
            else
                fbs.println("[no options available]");
            fbs.outdent();
        }

        public void printSelectedOptions(PrintStream ps) {
            if (getSaveRaw())
                ps.println("Saving with raw sectors");
            else
                ps.println("Saving with iso sectors");
        }

    }

    private static class ISO9660FileSaverBuilderGui extends DiscItemSaverBuilderGui {
        private ISO9660SaverBuilder _sourceBldr;
        private final ChangeListener[] _aoControls;

        /** Use just 1 change listener to notify all the controls so it's
         * easier to swap out when the builder is changed. */
        private ChangeListener _listenerWrapper = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                for (ChangeListener control : _aoControls) {
                    control.stateChanged(e);
                }
            }
        };

        public ISO9660FileSaverBuilderGui(ISO9660SaverBuilder sourceBldr) {
            super(new ParagraphLayout());
            _sourceBldr = sourceBldr;
            _aoControls = new ChangeListener[] {
                new FileName(),
                new SaveRaw()
            };

            _sourceBldr.addChangeListener(_listenerWrapper);
        }

        @Override
        public boolean useSaverBuilder(DiscItemSaverBuilder saverBuilder) {
            if (saverBuilder instanceof ISO9660SaverBuilder) {
                DiscItemSaverBuilder oldBuilder = _sourceBldr;
                _sourceBldr =  (ISO9660SaverBuilder) saverBuilder;
                oldBuilder.removeChangeListener(_listenerWrapper);
                _sourceBldr.addChangeListener(_listenerWrapper);
                _listenerWrapper.stateChanged(null);
                revalidate();
                return true;
            } else {
                return false;
            }
        }

        private class FileName implements ChangeListener {
            JLabel __label = new JLabel("Save as:");
            JLabel __name;
            public FileName() {
                __name = new JLabel(_sourceBldr.getFileName());
                add(__label, ParagraphLayout.NEW_PARAGRAPH);
                add(__name);
            }
            public void stateChanged(ChangeEvent e) {
                if (!__name.getText().equals(_sourceBldr.getFileName()))
                    __name.setText(_sourceBldr.getFileName());
            }
        }

        private class SaveRaw extends AbstractCheck {

            public SaveRaw() { super("Save raw:"); }
            public boolean isSelected() { return _sourceBldr.getSaveRaw(); }
            public void setSelected(boolean b) { _sourceBldr.setSaveRaw(b); }
            public boolean isEnabled() { return _sourceBldr.getSaveRaw_enabled(); }

        }
        
    }

    private class ISO9660FileSaver implements IDiscItemSaver {
        private final boolean _blnSaveRaw;

        public ISO9660FileSaver(boolean blnSaveRaw) {
            _blnSaveRaw = blnSaveRaw;
        }

        public String getInput() {
            return getIndexId().toString();
        }

        public String getOutput() {
            return getPath().getPath();
        }

        public void startSave(ProgressListener pl, File dir) throws IOException, TaskCanceledException {
            File outputFile = new File(dir, _path.getPath());

            if (outputFile.getParentFile() != null) {
                if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
                    throw new IOException("Unable to create directory " + outputFile.getParentFile());
                } else if (!outputFile.getParentFile().isDirectory()) {
                    throw new IOException("Cannot create directory over a file " + outputFile.getParentFile());
                }
            }

            final double iSectLen = getSectorLength();
            final int iStartSect = getStartSector();
            final int iEndSect = getEndSector();
            FileOutputStream fos = new FileOutputStream(outputFile);
            pl.progressStart();
            for (int iSector = iStartSect; iSector <= iEndSect; iSector++) {
                CdSector cdSector = getSourceCD().getSector(iSector);
                if (_blnSaveRaw)
                    fos.write(cdSector.getRawSectorDataCopy());
                else
                    fos.write(cdSector.getCdUserDataCopy());
                pl.progressUpdate((iSector - iStartSect) / iSectLen);
            }
            pl.progressEnd();
        }

    }

}
