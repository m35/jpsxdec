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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.Version;
import jpsxdec.i18n.I;
import jpsxdec.util.IO;

/** Maintains GUI settings persistent between program runs. */
public class GuiSettings {

    private static final Logger LOG = Logger.getLogger(GuiSettings.class.getName());

    private static final String INI_FILE_NAME = "jpsxdec.ini";

    private static final String SAVING_DIR_KEY = "SavingDir";
    @CheckForNull
    private String _sSavingDir;

    private static final String IMAGE_DIR_KEY = "ImageDir";
    @CheckForNull
    private String _sImageDir;

    private static final String INDEX_DIR_KEY = "IndexDir";
    @CheckForNull
    private String _sIndexDir;

    private static final String PREVIOUS_IMAGE_KEY = "PreviousImage";
    private final LinkedList<String> _previousImages = new LinkedList<String>();

    private static final String PREVIOUS_IMAGE_COUNT_KEY = "PreviousImageCount";
    private int _iPreviousImageCount;

    private static final String PREVIOUS_INDEX_KEY = "PreviousIndex";
    private final LinkedList<String> _previousIndexes = new LinkedList<String>();

    private static final String PREVIOUS_INDEX_COUNT_KEY = "PreviousIndexCount";
    private int _iPreviousIndexCount;

    public void load() {
        Properties prop = new Properties();
        FileInputStream propFile = null;
        try {
            prop.load(propFile = new FileInputStream(INI_FILE_NAME));
        } catch (FileNotFoundException ex) {
            LOG.log(Level.INFO, "ini file not found", ex);
        } catch (Throwable ex) {
            LOG.log(Level.WARNING, "Error loading ini file", ex);
        } finally {
            IO.closeSilently(propFile, LOG);
        }
        _sSavingDir = prop.getProperty(SAVING_DIR_KEY, new File("").getAbsolutePath());
        _sImageDir = prop.getProperty(IMAGE_DIR_KEY, new File("").getAbsolutePath());
        _sIndexDir = prop.getProperty(INDEX_DIR_KEY, new File("").getAbsolutePath());
        try {
            _iPreviousImageCount = Integer.parseInt(prop.getProperty(PREVIOUS_IMAGE_COUNT_KEY, "10"));
            if (_iPreviousImageCount < 0)
                _iPreviousImageCount = 10;
        } catch (NumberFormatException ex) {
            _iPreviousImageCount = 10;
        }
        try {
            _iPreviousIndexCount = Integer.parseInt(prop.getProperty(PREVIOUS_INDEX_COUNT_KEY, "10"));
            if (_iPreviousIndexCount < 0)
                _iPreviousIndexCount = 10;
        } catch (NumberFormatException ex) {
            _iPreviousIndexCount = 10;
        }
        for (int i=_iPreviousImageCount-1; i >= 0; i--) {
            String s = prop.getProperty(PREVIOUS_IMAGE_KEY + i);
            if (s != null)
                addPreviousImage(s);
        }
        for (int i=_iPreviousIndexCount-1; i >= 0; i--) {
            String s = prop.getProperty(PREVIOUS_INDEX_KEY + i);
            if (s != null)
                addPreviousIndex(s);
        }
    }

    public void save() throws IOException {
        Properties prop = new Properties();
        if (_sSavingDir != null)
            prop.setProperty(SAVING_DIR_KEY, _sSavingDir);
        if (_sImageDir != null)
            prop.setProperty(IMAGE_DIR_KEY, _sImageDir);
        if (_sIndexDir != null)
            prop.setProperty(INDEX_DIR_KEY, _sIndexDir);
        prop.setProperty(PREVIOUS_IMAGE_COUNT_KEY, String.valueOf(_iPreviousImageCount));
        for (int i=0; i < _previousImages.size(); i++) {
            prop.setProperty(PREVIOUS_IMAGE_KEY + i, _previousImages.get(i));
        }
        prop.setProperty(PREVIOUS_INDEX_COUNT_KEY, String.valueOf(_iPreviousIndexCount));
        for (int i=0; i < _previousIndexes.size(); i++) {
            prop.setProperty(PREVIOUS_INDEX_KEY + i, _previousIndexes.get(i));
        }
        FileOutputStream fos = new FileOutputStream(INI_FILE_NAME);
        boolean blnException = true;
        try {
            prop.store(fos, I.JPSXDEC_VERSION_NON_COMMERCIAL(Version.Version).getEnglishMessage());
            blnException = false;
        } finally {
            if (blnException)
                IO.closeSilently(fos, LOG);
            else
                fos.close();
        }
    }

    public int getPreviousImageCount() {
        return _iPreviousImageCount;
    }

    public void setPreviousImageCount(int iPreviousImageCount) {
        _iPreviousImageCount = iPreviousImageCount;
    }

    public int getPreviousIndexCount() {
        return _iPreviousIndexCount;
    }

    public void setPreviousIndexCount(int iPreviousIndexCount) {
        _iPreviousIndexCount = iPreviousIndexCount;
    }

    public @Nonnull List<String> getPreviousImages() {
        return _previousImages;
    }

    public void addPreviousImage(@Nonnull String sImagePath) {
        _previousImages.remove(sImagePath);
        _previousImages.addFirst(sImagePath);
        while (_previousImages.size() >= _iPreviousImageCount)
            _previousImages.removeLast();
    }

    public void removePreviousImage(@Nonnull String sImagePath) {
        _previousImages.remove(sImagePath);
    }

    public @Nonnull List<String> getPreviousIndexes() {
        return _previousIndexes;
    }

    public void addPreviousIndex(@Nonnull String sIndexPath) {
        _previousIndexes.remove(sIndexPath);
        _previousIndexes.addFirst(sIndexPath);
        while (_previousIndexes.size() >= _iPreviousIndexCount)
            _previousIndexes.removeLast();
    }

    public void removePreviousIndex(@Nonnull String sIndexPath) {
        _previousIndexes.remove(sIndexPath);
    }

    public @CheckForNull String getImageDir() {
        return _sImageDir;
    }

    public void setImageDir(@Nonnull String sImageDir) {
        this._sImageDir = sImageDir;
    }

    public @CheckForNull String getIndexDir() {
        return _sIndexDir;
    }

    public void setIndexDir(@Nonnull String sIndexDir) {
        _sIndexDir = sIndexDir;
    }

    public @CheckForNull String getSavingDir() {
        return _sSavingDir;
    }

    public void setSavingDir(@Nonnull String sSavingDir) {
        _sSavingDir = sSavingDir;
    }


}
