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

package jpsxdec.modules.tim;


import argparser.StringHolder;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import jpsxdec.cdreaders.CdException;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.TabularFeedback;
import jpsxdec.i18n.TabularFeedback.Cell;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.exception.LocalizedFileNotFoundException;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.tim.Tim;
import jpsxdec.util.ArgParser;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.Md5OutputStream;
import jpsxdec.util.TaskCanceledException;


/** Manages the Tim saving options. */
public class TimSaverBuilder extends DiscItemSaverBuilder {

    public static boolean MOCK_WRITING = false;

    private static void makeDirsForFile(@Nonnull File f) throws LocalizedFileNotFoundException {
        if (!MOCK_WRITING)
            IO.makeDirsForFile(f);
    }

    private OutputStream newOutputStream(@Nonnull File outputFile) throws FileNotFoundException {
        OutputStream os;
        if (MOCK_WRITING) {
            os = Md5OutputStream.getThreadMd5OutputStream();
        } else {
            os = new FileOutputStream(outputFile);
            addGeneratedFile(outputFile);
        }
        return os;
    }

    private boolean writeImage(BufferedImage bi, String imageIOid, File f) throws IOException {
        boolean blnOk;
        if (MOCK_WRITING) {
            blnOk = true;
        } else {
            blnOk = ImageIO.write(bi, imageIOid, f);
            if (blnOk)
                addGeneratedFile(f);
        }
        return blnOk;
    }



    private static final Logger LOG = Logger.getLogger(TimSaverBuilder.class.getName());

    /** Valid formats for saving Tim images. */
    public static class TimSaveFormat {

        @CheckForNull
        private final JavaImageFormat _javaFmt;

        private TimSaveFormat() {
            _javaFmt = null;
        }
        private TimSaveFormat(@Nonnull JavaImageFormat eJavaFmt) {
            _javaFmt = eJavaFmt;
        }

        private @Nonnull String getUiId() {
            if (_javaFmt == null)
                return "tim";
            else
                return _javaFmt.getUiId();
        }

        /** @throws UnsupportedOperationException if this is {@link #TIM}. */
        public @Nonnull JavaImageFormat getJavaFormat() {
            if (_javaFmt == null)
                throw new UnsupportedOperationException("TIM does not have a Java image format");
            return _javaFmt;
        }

        @Override
        public String toString() {
            if (_javaFmt == null)
                return "tim";
            else
                return _javaFmt.toString();
        }

        public String getExtension() {
            if (_javaFmt == null)
                return "tim";
            else
                return _javaFmt.getExtension();
        }
    }

    public static final TimSaveFormat TIM = new TimSaveFormat();
    private static final ArrayList<TimSaveFormat> TRUE_COLOR_FORMAT_LIST;
    private static final ArrayList<TimSaveFormat> TRUE_COLOR_ALPHA_FORMAT_LIST;
    private static final ArrayList<TimSaveFormat> PALETTE_FORMAT_LIST;
    static {
        List<JavaImageFormat> availableFormats = JavaImageFormat.getAvailable();
        int iMaxListSize = availableFormats.size() + 1;
        TRUE_COLOR_FORMAT_LIST = new ArrayList<TimSaveFormat>(iMaxListSize);
        TRUE_COLOR_ALPHA_FORMAT_LIST = new ArrayList<TimSaveFormat>(iMaxListSize);
        PALETTE_FORMAT_LIST = new ArrayList<TimSaveFormat>(iMaxListSize);
        for (JavaImageFormat jif : availableFormats) {
            if (jif.isAvailable()) {
                if (jif.hasTrueColor()) {
                    TRUE_COLOR_FORMAT_LIST.add(new TimSaveFormat(jif));
                    if (jif.hasAlpha())
                        TRUE_COLOR_ALPHA_FORMAT_LIST.add(new TimSaveFormat(jif));
                }
                PALETTE_FORMAT_LIST.add(new TimSaveFormat(jif));
            }
        }
        TRUE_COLOR_FORMAT_LIST.add(TIM);
        TRUE_COLOR_ALPHA_FORMAT_LIST.add(TIM);
        PALETTE_FORMAT_LIST.add(TIM);
    }

    public static @Nonnull List<TimSaveFormat> getValidFormats(int iBpp) {
        switch (iBpp) {
            case 4:
            case 8:
                return PALETTE_FORMAT_LIST;
            case 16:
                return TRUE_COLOR_ALPHA_FORMAT_LIST;
            case 24:
                return TRUE_COLOR_FORMAT_LIST;
            default: throw new IllegalArgumentException("Impossible Tim bpp " + iBpp);
        }
    }


    // -----------------------------------------------------------------------

    @Nonnull
    private final DiscItemTim _timItem;
    @Nonnull
    private final List<TimSaveFormat> _validFormats;

    @Nonnull
    private final boolean[] _ablnSavePalette;
    @Nonnull
    private TimSaveFormat _saveFormat;

    public TimSaverBuilder(@Nonnull DiscItemTim timItem) {
        _timItem = timItem;
        _ablnSavePalette = new boolean[_timItem.getPaletteCount()];
        _validFormats = getValidFormats(_timItem.getBitsPerPixel());
        Arrays.fill(_ablnSavePalette, true);
        _saveFormat = _validFormats.get(0); // will be PNG if available
    }

    @Override
    public @Nonnull DiscItemTim getDiscItem() {
        return _timItem;
    }

    @Override
    public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder other) {
        if (!(other instanceof TimSaverBuilder))
            return false;
        TimSaverBuilder otherTim = (TimSaverBuilder) other;
        otherTim.setImageFormat(getImageFormat());
        return true;
    }

    // .................................................................

    public int getPaletteCount() {
        return _timItem.getPaletteCount();
    }
    public void setSavePalette(int iPalette, boolean blnSave) {
        _ablnSavePalette[iPalette] = blnSave;
        firePossibleChange();
    }
    public void toggleSavePalette(int iPalette) {
        _ablnSavePalette[iPalette] = !_ablnSavePalette[iPalette];
        firePossibleChange();
    }
    public boolean getSavePalette(int iPalette) {
        if (getImageFormat() == TIM)
            return true;
        return _ablnSavePalette[iPalette];
    }
    public boolean getPaletteSelection_enabled() {
        return _saveFormat != TIM;
    }

    // .................................................................

    public void setImageFormat(@Nonnull TimSaveFormat fmt) {
        _saveFormat = fmt;
        firePossibleChange();
    }
    public @Nonnull TimSaveFormat getImageFormat() {
        return _saveFormat;
    }
    public int getImageFormat_listSize() {
        return _validFormats.size();
    }
    public TimSaveFormat getImageFormat_listItem(int i) {
        return _validFormats.get(i);
    }

    // .................................................................

    public @Nonnull ILocalizedMessage getOutputFilesSummary() {
        if (_saveFormat == TIM)
            return new UnlocalizedMessage(makeTimFileName());

        // _saveFormat.getJavaFormat() should != null for non-Tim formats
        JavaImageFormat format = _saveFormat.getJavaFormat();

        int iCount = 0;
        String sStartFile = null, sEndFile = null;
        for (int iCurrentImage = 0; iCurrentImage < _ablnSavePalette.length; iCurrentImage++) {
            if (_ablnSavePalette[iCurrentImage]) {
                iCount++;
                String sFile = makePaletteFileName(iCurrentImage, format);
                if (sStartFile == null)
                    sStartFile = sFile;
                sEndFile = sFile;
            }
        }

        if (iCount == 0)
            return I.TIM_OUTPUT_FILES_NONE();
        else if (sStartFile == sEndFile)
            return new UnlocalizedMessage(sStartFile); // just 1 file
        else
            return I.TIM_OUTPUT_FILES(iCount, sStartFile, sEndFile);
    }

    private @Nonnull String makePaletteFileName(int iFile, @Nonnull JavaImageFormat format) {
        return String.format("%s_p%02d.%s",
                _timItem.getSuggestedBaseName(),
                iFile,
                format.getExtension());
    }

    // .................................................................

    @Nonnull Tim readTim() throws CdException.Read, BinaryDataNotRecognized {
        return _timItem.readTim();
    }

    private @CheckForNull TimSaveFormat fromCmdLine(@Nonnull String sCmdLine) {
        for (TimSaveFormat fmt : _validFormats) {
            if (fmt.getUiId().equalsIgnoreCase(sCmdLine))
                return fmt;
        }
        return null;
    }

    @Override
    public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs) {
        if (!ap.hasRemaining())
            return;

        StringHolder timpalettes = ap.addStringOption("-pal");
        StringHolder format = ap.addStringOption("-imgfmt","-if");
        ap.match();

        // parse args for which palettes to save
        if (timpalettes.value != null) {
            boolean[] ablnNewValues = parseNumberListRange(timpalettes.value, getPaletteCount());
            if (ablnNewValues == null) {
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VALUE_FOR_CMD(timpalettes.value, "-pal"));
            } else {
                System.arraycopy(ablnNewValues, 0, _ablnSavePalette, 0, getPaletteCount());
            }
        }

        if (format.value != null) {
            TimSaveFormat fmt = fromCmdLine(format.value);
            if (fmt == null) {
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VALUE_FOR_CMD(format.value, "-if,-imgfmt"));
            } else {
                setImageFormat(fmt);
            }
        }
    }


    /** Parse a string of comma-delimited numbers and ranges, then creates an
     *  array with indexes toggled based on the numbers.
     *  e.g. 3,6-9,15 */
    private static @CheckForNull boolean[] parseNumberListRange(@Nonnull String s, int iMax) {
        try {
            boolean[] abln = new boolean[iMax];
            Arrays.fill(abln, false);
            for (String num : s.split(",")) {
                if (num.indexOf('-') > 0) {
                    String[] asParts = num.split("-");
                    for (int i = Integer.parseInt(asParts[0]); i <= Integer.parseInt(asParts[1]); i++)
                        abln[i] = true;
                } else {
                    abln[Integer.parseInt(num)] = true;
                }
            }
            return abln;
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return null;
        }
    }

    @Override
    public void printHelp(@Nonnull FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();
        tfb.setRowSpacing(1);

        tfb.addCell(I.CMD_TIM_PAL()).addCell(I.CMD_TIM_PAL_HELP());
        tfb.newRow();
        tfb.addCell(I.CMD_TIM_IF());
        Cell c = new Cell(I.CMD_TIM_IF_HELP(_validFormats.get(0).getExtension()));
        for (TimSaveFormat fmt : _validFormats) {
            c.addLine(new UnlocalizedMessage(fmt.getExtension()), 2);
        }
        tfb.addCell(c);

        tfb.write(fbs.getUnderlyingStream());
    }

    @Override
    public void printSelectedOptions(@Nonnull
    ILocalizedLogger log) {
        log.log(Level.INFO, I.CMD_TIM_SAVE_FORMAT(getImageFormat().getExtension()));
        log.log(Level.INFO, getOutputFilesSummary());
    }

    private @Nonnull String makeTimFileName() {
        return _timItem.getSuggestedBaseName() + ".tim";
    }

    @Override
    public @Nonnull ILocalizedMessage getOutputSummary() {
        return getOutputFilesSummary();
    }

    @Override
    public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
        return new TimSaverBuilderGui(this);
    }

    // ------------------------------------------------------------------------

    @Override
    public void startSave(@Nonnull ProgressLogger pl, @CheckForNull File directory)
            throws LoggedFailure, TaskCanceledException
    {
        clearGeneratedFiles();
        printSelectedOptions(pl);
        if (getImageFormat() == TIM) {
            startSaveTim(pl, directory, makeTimFileName());
        } else {
            // _saveFormat.getJavaFormat() should != null for non-Tim formats
            String[] asOutputFiles = new String[_ablnSavePalette.length];
            for (int i = 0; i < asOutputFiles.length; i++) {
                if (_ablnSavePalette[i])
                    asOutputFiles[i] = makePaletteFileName(i, getImageFormat().getJavaFormat());
            }
            startSaveImage(pl, directory, asOutputFiles, getImageFormat().getJavaFormat());
        }
    }

    private void startSaveTim(@Nonnull ProgressLogger pl, @CheckForNull File outputDir,
                              @Nonnull String sOutputFile)
            throws LoggedFailure, TaskCanceledException
    {
        File outputFile = new File(outputDir, sOutputFile);
        pl.progressStart(1);

        Tim tim = readTim(pl);

        if (tim.timHasIssues()) {
            pl.log(Level.WARNING, I.TIM_HAS_ISSUES_CAN_BE_EXTRACTED(tim.toString()));
        }
        pl.event(I.IO_WRITING_FILE(outputFile.getName()));
        try {
            makeDirsForFile(outputFile);
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(newOutputStream(outputFile));
                tim.write(os);
            } catch (FileNotFoundException ex) {
                throw new LoggedFailure(pl, Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(outputFile.toString()), ex);
            } catch (IOException ex) {
                throw new LoggedFailure(pl, Level.SEVERE, I.IO_WRITING_FILE_ERROR_NAME(outputFile.toString()), ex);
            } finally {
                IO.closeSilently(os, LOG);
            }
        } catch (LocalizedFileNotFoundException ex) {
            throw new LoggedFailure(pl, Level.SEVERE, ex.getSourceMessage(), ex);
        }
        pl.progressEnd();
    }


    public void startSaveImage(@Nonnull ProgressLogger pl, @CheckForNull File outputDir,
                               @Nonnull String[] _asOutputFiles, @Nonnull JavaImageFormat _imageFormat)
            throws LoggedFailure, TaskCanceledException
    {
        pl.progressStart(_asOutputFiles.length);

        Tim tim = readTim(pl);

        for (int i = 0; i < _asOutputFiles.length; i++) {
            if (_asOutputFiles[i] != null) {
                String sFile = _asOutputFiles[i];
                BufferedImage bi = tim.toBufferedImage(i);
                File f = new File(outputDir, sFile);
                try {
                    pl.event(I.IO_WRITING_FILE(f.toString()));
                    makeDirsForFile(f);
                    try {
                        boolean blnOk = writeImage(bi, _imageFormat.getImageIOid(), f);
                        if (!blnOk)
                            pl.log(Level.SEVERE, I.CMD_PALETTE_IMAGE_SAVE_FAIL(f, i));
                    } catch (IOException ex) {
                        pl.log(Level.SEVERE, I.IO_WRITING_FILE_ERROR_NAME(f.toString()), ex);
                    }
                } catch (LocalizedFileNotFoundException ex) {
                    pl.log(Level.SEVERE, ex.getSourceMessage(), ex);
                }
            }
            pl.progressUpdate(i);
        }

        pl.progressEnd();
    }

    private Tim readTim(@Nonnull ProgressLogger pl) throws LoggedFailure {
        Tim tim;
        try {
            tim = _timItem.readTim();
        } catch (CdException.Read ex) {
            throw new LoggedFailure(pl, Level.SEVERE,
                   I.IO_READING_FROM_FILE_ERROR_NAME(ex.getFile().toString()), ex);
        } catch (BinaryDataNotRecognized ex) {
            throw new LoggedFailure(pl, Level.SEVERE, I.TIM_DATA_NOT_FOUND(), ex);
        }
        return tim;
    }

}
