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

package jpsxdec;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

/** The JFileChooser is pretty crappy, so this sub-class attempts to fix
 *  some of the problems with it. */
public class BetterFileChooser extends JFileChooser {
    
    /** My own 'All files' filter as a workaround for Linux bug. */
    public static final FileFilter ALL_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return true;
        }

        @Override
        public String getDescription() {
            return "All files";
        }

        @Override
        public String toString() {
            // Java is so awesome that the Linux version uses toString() instead of
            // getDescription() in the list of file extention filters.
            return getDescription();
        }
    };

    /** Creates a new BetterFileChooser. */
    public BetterFileChooser() {
        super();
        init();
    }

    public BetterFileChooser(String currentDirectoryPath, FileSystemView fsv) {
        super(currentDirectoryPath, fsv);
        init();
    }

    public BetterFileChooser(File currentDirectory, FileSystemView fsv) {
        super(currentDirectory, fsv);
        init();
    }

    public BetterFileChooser(FileSystemView fsv) {
        super(fsv);
        init();
    }

    public BetterFileChooser(File currentDirectory) {
        super(currentDirectory);
        init();
    }

    public BetterFileChooser(String currentDirectoryPath) {
        super(currentDirectoryPath);
        init();
    }

    private void init() {
        // Set the look and feel for the platform
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
        }
        // and fix the stupid 'all' file filter for Linux by using my own
        super.removeChoosableFileFilter(super.getAcceptAllFileFilter());
        super.addChoosableFileFilter(ALL_FILE_FILTER);
    }

    @Override
    public void updateUI() {
        // Fix the possible 5 second delay that can occur on Windows
        // by disabling some feature.
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6372808
        putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
        super.updateUI();
    }
}
