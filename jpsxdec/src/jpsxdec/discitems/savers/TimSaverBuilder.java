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

package jpsxdec.discitems.savers;


import argparser.ArgParser;
import argparser.StringHolder;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.DiscItemTim;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.tim.Tim;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.ProgressListenerLogger;
import jpsxdec.util.TabularFeedback;
import jpsxdec.util.TaskCanceledException;


/** Manages the Tim saving options. */
public class TimSaverBuilder extends DiscItemSaverBuilder {

    /** Valid formats for saving Tim images. */
    public static enum TimSaveFormat {
        PNG(JavaImageFormat.PNG),
        GIF(JavaImageFormat.GIF),
        BMP(JavaImageFormat.BMP),
        TIM;

        private static final List<TimSaveFormat> TRUE_COLOR_FORMAT_LIST;
        private static final List<TimSaveFormat> TRUE_COLOR_ALPHA_FORMAT_LIST;
        private static final List<TimSaveFormat> PALETTE_FORMAT_LIST;
        static {
            ArrayList<TimSaveFormat> trueColors = new ArrayList<TimSaveFormat>();
            ArrayList<TimSaveFormat> trueColorsAlpha = new ArrayList<TimSaveFormat>();
            ArrayList<TimSaveFormat> paletted = new ArrayList<TimSaveFormat>();
            for (TimSaveFormat fmt : TimSaveFormat.values()) {
                if (fmt.isAvailable()) {
                    if (fmt.hasTrueColor()) {
                        if (fmt.hasAlpha())
                            trueColorsAlpha.add(fmt);
                        else
                            trueColors.add(fmt);
                    }
                    paletted.add(fmt);
                }
            }
            TRUE_COLOR_FORMAT_LIST = trueColors;
            TRUE_COLOR_ALPHA_FORMAT_LIST = trueColorsAlpha;
            PALETTE_FORMAT_LIST = paletted;
        }
        
        @CheckForNull
        private final JavaImageFormat _javaFmt;
        
        private TimSaveFormat() {
            _javaFmt = null;
        }
        private TimSaveFormat(@Nonnull JavaImageFormat eJavaFmt) {
            _javaFmt = eJavaFmt;
        }

        private boolean isAvailable() {
            return (_javaFmt == null) || _javaFmt.isAvailable();
        }

        private boolean hasTrueColor() {
            return (_javaFmt == null) || _javaFmt.hasTrueColor();
        }

        private boolean hasAlpha() {
            return (_javaFmt == null) || _javaFmt.hasAlpha();
        }

        private @Nonnull String getId() {
            if (_javaFmt == null)
                return "tim";
            else
                return _javaFmt.getId();
        }

        /** @throws UnsupportedOperationException if this is {@link #TIM}. */
        public @Nonnull JavaImageFormat getJavaFormat() {
            if (_javaFmt == null)
                throw new UnsupportedOperationException("TIM does not have a Java image format");
            return _javaFmt;
        }
        
        public String toString() {
            if (_javaFmt == null)
                return "tim";
            else
                return _javaFmt.toString();
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

        public String getExtension() {
            if (_javaFmt == null)
                return "tim";
            else
                return _javaFmt.getExtension();
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
        _validFormats = TimSaveFormat.getValidFormats(_timItem.getBitsPerPixel());
        resetToDefaults();
    }

    @Nonnull Tim readTim() throws IOException, NotThisTypeException {
        return _timItem.readTim();
    }

    public void resetToDefaults() {
        Arrays.fill(_ablnSavePalette, true);
        _saveFormat = _validFormats.get(0); // will be PNG if available
        firePossibleChange();
    }

    @Override
    public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder other) {
        if (!(other instanceof TimSaverBuilder))
            return false;
        TimSaverBuilder otherTim = (TimSaverBuilder) other;
        otherTim.setImageFormat(getImageFormat());
        return true;
    }

    public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
        return new TimSaverBuilderGui(this);
    }

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
        if (getImageFormat() == TimSaveFormat.TIM)
            return true;
        return _ablnSavePalette[iPalette];
    }
    public boolean getPaletteSelection_enabled() {
        return _saveFormat != TimSaveFormat.TIM;
    }

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
        if (_saveFormat == TimSaveFormat.TIM)
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

    private @Nonnull String makeTimFileName() {
        return _timItem.getSuggestedBaseName() + ".tim";
    }


    // .................................................................


    private @CheckForNull TimSaveFormat fromCmdLine(@Nonnull String sCmdLine) {
        for (TimSaveFormat fmt : _validFormats) {
            if (fmt.getId().equalsIgnoreCase(sCmdLine))
                return fmt;
        }
        return null;
    }

    public @CheckForNull String[] commandLineOptions(@CheckForNull String[] asArgs, 
                                                     @Nonnull FeedbackStream fbs)
    {
        if (asArgs == null) return null;

        ArgParser parser = new ArgParser("", false);

        StringHolder timpalettes = new StringHolder();
        parser.addOption("-pal %s", timpalettes);

        StringHolder format = new StringHolder();
        parser.addOption("-imgfmt,-if %s", format);

        String[] asRemain = parser.matchAllArgs(asArgs, 0, 0);

        // parse args for which palettes to save
        if (timpalettes.value != null) {
            boolean[] ablnNewValues = parseNumberListRange(timpalettes.value, getPaletteCount());
            if (ablnNewValues == null) {
                fbs.printlnWarn(I.CMD_PALETTE_LIST_INVALID(timpalettes.value));
            } else {
                System.arraycopy(ablnNewValues, 0, _ablnSavePalette, 0, getPaletteCount());
            }
        }

        if (format.value != null) {
            TimSaveFormat fmt = fromCmdLine(format.value);
            if (fmt == null) {
                fbs.printlnWarn(I.CMD_TIM_SAVE_FORMAT_INVALID(format.value));
            } else {
                setImageFormat(fmt);
            }
        }

        return asRemain;
    }


    /** Parse a string of comma-delimited numbers and ranges, then creates an
     *  array with indexes toggled based on the numbers.
     *  e.g. 3,6-9,15 */
    private static boolean[] parseNumberListRange(@Nonnull String s, int iMax) {
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
        } catch (NumberFormatException ex) {
            return null;
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }


    public void printHelp(@Nonnull FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();
        tfb.setRowSpacing(1);

        tfb.print(I.CMD_TIM_PAL()).tab().print(I.CMD_TIM_PAL_HELP());
        tfb.newRow();
        tfb.print(I.CMD_TIM_IF()).tab().print(I.CMD_TIM_IF_HELP(_validFormats.get(0).getExtension()));
        tfb.indent();
        for (TimSaveFormat fmt : _validFormats) {
            tfb.ln().print(new UnlocalizedMessage(fmt.getExtension()));
        }

        tfb.write(fbs);
    }

    public @Nonnull IDiscItemSaver makeSaver(@CheckForNull File directory) {
        if (_saveFormat == TimSaveFormat.TIM)
            return new TimRawSaver(_timItem, directory, makeTimFileName());
        else {
            // _saveFormat.getJavaFormat() should != null for non-Tim formats
            String[] asOutputFiles = new String[_ablnSavePalette.length];
            for (int i = 0; i < asOutputFiles.length; i++) {
                if (_ablnSavePalette[i])
                    asOutputFiles[i] = makePaletteFileName(i, _saveFormat.getJavaFormat());
            }
            return new TimImageSaver(_saveFormat, _timItem,
                                     directory, asOutputFiles, getOutputFilesSummary());
        }
    }

    // ------------------------------------------------------------------------

    private static class TimRawSaver implements IDiscItemSaver {

        @Nonnull
        private final DiscItemTim _timItem;
        @Nonnull
        private final File _outputFile;

        public TimRawSaver(@Nonnull DiscItemTim tim, @CheckForNull File outputDir, @Nonnull String sOutputFile) {
            _timItem = tim;
            _outputFile = new File(outputDir, sOutputFile);
        }
        
        public void startSave(@Nonnull ProgressListenerLogger pll) throws IOException, TaskCanceledException {
            OutputStream os = null;
            try {
                pll.progressStart();
                Tim tim = _timItem.readTim();
                pll.event(I.CMD_WRITING(_outputFile.getName()));
                IO.makeDirsForFile(_outputFile);
                os = new BufferedOutputStream(new FileOutputStream(_outputFile));
                tim.write(os);
                pll.progressEnd();
            } catch (NotThisTypeException ex) {
                pll.log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                pll.log(Level.SEVERE, null, ex);
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    pll.log(Level.SEVERE, null, ex);
                }
            }
        }

        public @Nonnull String getInput() {
            return _timItem.getIndexId().serialize();
        }

        public @Nonnull DiscItemTim getDiscItem() {
            return _timItem;
        }

        public @Nonnull ILocalizedMessage getOutputSummary() {
            return new UnlocalizedMessage(_outputFile.getName());
        }

        public void printSelectedOptions(@Nonnull PrintStream ps) {
            ps.println(I.CMD_TIM_SAVE_FORMAT(TimSaveFormat.TIM.getExtension()));
        }

        public @Nonnull File[] getGeneratedFiles() {
            return new File[] {_outputFile};
        }
    }
    
    
    private static class TimImageSaver implements IDiscItemSaver {

        @Nonnull
        private final TimSaveFormat _timFormat;
        @Nonnull
        private final JavaImageFormat _imageFormat;
        @Nonnull
        private final DiscItemTim _timItem;
        @CheckForNull
        private final File _outputDir;
        @Nonnull
        private final String[] _asOutputFiles;
        @Nonnull
        private final ILocalizedMessage _outputSummary;
        @CheckForNull
        private ArrayList<File> _generatedFiles;

        public TimImageSaver(@Nonnull TimSaveFormat timFormat, @Nonnull DiscItemTim timItem,
                             @CheckForNull File outputDir, @Nonnull String[] asOutputFiles,
                             @Nonnull ILocalizedMessage outputSummary)
        {
            _timFormat = timFormat;
            _imageFormat = _timFormat.getJavaFormat();
            _timItem = timItem;
            _outputDir = outputDir;
            _asOutputFiles = asOutputFiles;
            _outputSummary = outputSummary;
        }

        public @Nonnull String getInput() {
            return _timItem.getIndexId().serialize();
        }

        public @Nonnull DiscItemTim getDiscItem() {
            return _timItem;
        }

        public @Nonnull ILocalizedMessage getOutputSummary() {
            return _outputSummary;
        }

        public void startSave(@Nonnull ProgressListenerLogger pll) throws TaskCanceledException {

            try {
                Tim tim = _timItem.readTim();
                _generatedFiles = new ArrayList<File>();

                pll.progressStart();
                for (int i = 0; i < _asOutputFiles.length; i++) {
                    if (_asOutputFiles[i] != null) {
                        String sFile = _asOutputFiles[i];
                        pll.event(I.CMD_WRITING(sFile));
                        BufferedImage bi = tim.toBufferedImage(i);
                        File f = new File(_outputDir, sFile);
                        IO.makeDirsForFile(f);
                        boolean blnOk = ImageIO.write(bi, _imageFormat.getId(), f);
                        if (blnOk)
                            _generatedFiles.add(f);
                        else
                            I.CMD_PALETTE_IMAGE_SAVE_FAIL(f, i).log(pll, Level.SEVERE);
                        pll.progressUpdate((double)i / _timItem.getPaletteCount());
                    }
                }
                pll.progressEnd();
            } catch (NotThisTypeException ex) {
                pll.log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                pll.log(Level.SEVERE, null, ex);
            }
        }

        public void printSelectedOptions(@Nonnull PrintStream ps) {
            ps.println(I.CMD_PALETTE_FILES(_outputSummary));
            ps.println(I.CMD_TIM_SAVE_FORMAT(_timFormat.getExtension()));
        }

        public @CheckForNull File[] getGeneratedFiles() {
            if (_generatedFiles == null)
                return null;
            else
                return _generatedFiles.toArray(new File[_generatedFiles.size()]);
        }
    }

}

