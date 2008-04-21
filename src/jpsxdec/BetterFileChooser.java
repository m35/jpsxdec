/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
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
import javax.swing.filechooser.FileSystemView;

class BetterFileChooser extends JFileChooser {

    public BetterFileChooser() {
        super();
        setLF();
    }

    public BetterFileChooser(String currentDirectoryPath, FileSystemView fsv) {
        super(currentDirectoryPath, fsv);
        setLF();
    }

    public BetterFileChooser(File currentDirectory, FileSystemView fsv) {
        super(currentDirectory, fsv);
        setLF();
    }

    public BetterFileChooser(FileSystemView fsv) {
        super(fsv);
        setLF();
    }

    public BetterFileChooser(File currentDirectory) {
        super(currentDirectory);
        setLF();
    }

    public BetterFileChooser(String currentDirectoryPath) {
        super(currentDirectoryPath);
        setLF();
    }

    private void setLF() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
        }
    }

    @Override
    public void updateUI() {
        putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
        super.updateUI();
    }
}
