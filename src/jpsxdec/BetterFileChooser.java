/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * BetterFileChooser.java
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
class BetterFileChooser extends JFileChooser {
    
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
        putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
        super.updateUI();
    }
}
