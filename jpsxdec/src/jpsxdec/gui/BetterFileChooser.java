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

package jpsxdec.gui;

import java.io.File;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;

/** The JFileChooser is pretty lame, so this sub-class attempts to fix
 *  some of the problems with it. */
public class BetterFileChooser extends JFileChooser {

    /**
     * Special filter for saving so we can consider auto extension before saving.
     */
    public abstract static class SaveFileFilter extends FileFilter {
        abstract public @Nonnull String getExtension();
    }

    /** My own 'All files' filter as a workaround for Java 5 Linux bug. */
    private transient final FileFilter ALL_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return BetterFileChooser.super.getAcceptAllFileFilter().accept(f);
        }

        @Override
        public String getDescription() {
            return BetterFileChooser.super.getAcceptAllFileFilter().getDescription();
        }

        @Override
        public String toString() {
            // Java 5 on Linux uses toString() instead of
            // getDescription() in the list of file extension filters.
            return BetterFileChooser.super.getAcceptAllFileFilter().getDescription();
        }
    };

    /** Creates a new BetterFileChooser. */
    public BetterFileChooser() {
        super();
    }

    public BetterFileChooser(@Nonnull String currentDirectoryPath, @Nonnull FileSystemView fsv) {
        super(currentDirectoryPath, fsv);
    }

    public BetterFileChooser(@Nonnull File currentDirectory, @Nonnull FileSystemView fsv) {
        super(currentDirectory, fsv);
    }

    public BetterFileChooser(@Nonnull FileSystemView fsv) {
        super(fsv);
    }

    public BetterFileChooser(@Nonnull File currentDirectory) {
        super(currentDirectory);
    }

    public BetterFileChooser(@CheckForNull String currentDirectoryPath) {
        super(currentDirectoryPath);
    }

    @Override
    public @Nonnull FileFilter getAcceptAllFileFilter() {
        // fix the broken 'all' file filter for Java 5 Linux by wrapping it with my own
        return ALL_FILE_FILTER;
    }

    @Override
    public void updateUI() {
        // Fix the possible 5 second delay that can occur on Windows
        // by disabling some feature.
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6372808
        //putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
        super.updateUI();
    }

    @Override
    public void approveSelection() {
        if (getDialogType() == SAVE_DIALOG) {
            File f = getSelectedFile();

            // ensure file name has extension
            FileFilter filter = getFileFilter();
            if (filter instanceof SaveFileFilter) {
                SaveFileFilter saveFilter = (SaveFileFilter) filter;
                if (!f.getName().toLowerCase().endsWith(saveFilter.getExtension().toLowerCase())) {
                    f = new File(f.getParentFile(), f.getName()+saveFilter.getExtension());
                    setSelectedFile(f);
                }
            }

            // confirm overwrite
            if (f.exists()) {
                ILocalizedMessage msg = I.GUI_FILE_EXISTS_REPLACE(f.getName());
                String sTitle = getDialogTitle();
                int iOption = JOptionPane.showConfirmDialog(this, msg, sTitle, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (iOption != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        }

        super.approveSelection();
    }
}
