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
import javax.annotation.Nonnull;
import javax.swing.filechooser.FileFilter;
import jpsxdec.gui.BetterFileChooser.SaveFileFilter;
import jpsxdec.i18n.I;


public class GuiFileFilters {

    public static final SaveFileFilter INDEX_FILE_FILTER = new SaveFileFilter() {
        @Override
        public boolean accept(@Nonnull File f) {
            return !f.isFile() || f.getName().toLowerCase().endsWith(getExtension().toLowerCase());
        }
        @Override
        public @Nonnull String getDescription() {
            return I.GUI_INDEX_EXTENSION().getLocalizedMessage();
        }
        @Override
        public @Nonnull String getExtension() {
            return ".idx";
        }
    };


    static final FileFilter[] DISC_OPEN_FILTERS = {
        new FileFilter() {
            @Override
            public String getDescription() { return I.GUI_ALL_COMPATIBLE_EXTENSIONS().getLocalizedMessage(); }
            @Override
            public boolean accept(File f) {
                String s = f.getName().toLowerCase();
                return f.isDirectory() ||
                       s.endsWith(".iso") ||
                       s.endsWith(".bin") ||
                       s.endsWith(".img") ||
                       s.endsWith(".mdf") ||
                       s.endsWith(".str") ||
                       s.endsWith(".iki") ||
                       s.endsWith(".xa");
            }
        },
        new FileFilter() {
            @Override
            public String getDescription() { return I.GUI_CD_IMAGE_EXTENSIONS().getLocalizedMessage(); }
            @Override
            public boolean accept(File f) {
                String s = f.getName().toLowerCase();
                return f.isDirectory() ||
                       s.endsWith(".iso") ||
                       s.endsWith(".bin") ||
                       s.endsWith(".img") ||
                       s.endsWith(".mdf");
            }
        },
        new FileFilter() {
            @Override
            public String getDescription() { return I.GUI_PSX_VIDEO_EXTENSIONS().getLocalizedMessage(); }
            @Override
            public boolean accept(File f) {
                String s = f.getName().toLowerCase();
                return f.isDirectory() ||
                       s.endsWith(".str") ||
                       s.endsWith(".mov") ||
                       s.endsWith(".iki") ||
                       s.endsWith(".ik2");
            }
        },
        new FileFilter() {
            @Override
            public String getDescription() { return I.GUI_XA_EXTENSION().getLocalizedMessage(); }
            @Override
            public boolean accept(File f) {
                String s = f.getName().toLowerCase();
                return f.isDirectory() ||
                       s.endsWith(".xa");
            }
        },
    };

}
