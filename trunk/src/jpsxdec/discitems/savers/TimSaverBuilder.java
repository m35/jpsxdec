/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.DiscItemTim;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.tim.Tim;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.ProgressListener;
import jpsxdec.util.TabularFeedback;
import jpsxdec.util.TaskCanceledException;


/** Manages the Tim saving options. */
public class TimSaverBuilder extends DiscItemSaverBuilder {

    private static final Logger log = Logger.getLogger(TimSaverBuilder.class.getName());

    private static final List<TimSaveFormat> TRUE_COLOR_FORMAT_LIST;
    private static final List<TimSaveFormat> PALETTE_FORMAT_LIST;
    static {
        ArrayList<TimSaveFormat> trueColors = new ArrayList<TimSaveFormat>();
        ArrayList<TimSaveFormat> paletted = new ArrayList<TimSaveFormat>();
        // only check for png, gif, and bmp availability (all paletted)
        for (TimSaveFormat fmt : TimSaveFormat.values()) {
            if (fmt.isAvailable()) {
                if (fmt.hasTrueColor())
                    trueColors.add(fmt);
                paletted.add(fmt);
            }
        }
        TRUE_COLOR_FORMAT_LIST = trueColors;
        PALETTE_FORMAT_LIST = paletted;
    }
    
    
    public static enum TimSaveFormat {
        PNG(JavaImageFormat.PNG),
        GIF(JavaImageFormat.GIF),
        BMP(JavaImageFormat.BMP),
        TIM;
        
        private final JavaImageFormat _javaFmt;
        
        private TimSaveFormat() {
            _javaFmt = null;
        }
        private TimSaveFormat(JavaImageFormat eJavaFmt) {
            _javaFmt = eJavaFmt;
        }

        private boolean isAvailable() {
            return (_javaFmt == null) || _javaFmt.isAvailable();
        }

        private boolean hasTrueColor() {
            return (_javaFmt == null) || _javaFmt.hasTrueColor();
        }

        private String getId() {
            if (_javaFmt == null)
                return "tim";
            else
                return _javaFmt.getId();
        }

        private JavaImageFormat getJavaFormat() {
            return _javaFmt;
        }
        
        public String toString() {
            if (_javaFmt == null)
                return "tim";
            else
                return _javaFmt.toString();
        }
    }

    // -----------------------------------------------------------------------

    private final DiscItemTim _timItem;
    private final List<TimSaveFormat> _validFormats;

    private final boolean[] _ablnSavePalette;
    private TimSaveFormat _imageFormat;

    public TimSaverBuilder(DiscItemTim timItem) {
        _timItem = timItem;
        _ablnSavePalette = new boolean[_timItem.getPaletteCount()];
        boolean blnTimIsTrueColor = _timItem.getBitsPerPixel() == 16 ||
                                    _timItem.getBitsPerPixel() == 24;
        _validFormats = blnTimIsTrueColor ? TRUE_COLOR_FORMAT_LIST :
                                            PALETTE_FORMAT_LIST;
        resetToDefaults();
    }

    Tim readTim() throws IOException, NotThisTypeException {
        return _timItem.readTim();
    }

    public void resetToDefaults() {
        Arrays.fill(_ablnSavePalette, true);
        _imageFormat = _validFormats.get(0); // will be PNG if available
        firePossibleChange();
    }

    @Override
    public boolean copySettingsTo(DiscItemSaverBuilder other) {
        if (other instanceof TimSaverBuilder) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return false;
    }

    public DiscItemSaverBuilderGui getOptionPane() {
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
        return _imageFormat != TimSaveFormat.TIM;
    }

    public void setImageFormat(TimSaveFormat fmt) {
        _imageFormat = fmt;
        firePossibleChange();
    }
    public TimSaveFormat getImageFormat() {
        return _imageFormat;
    }
    public int getImageFormat_listSize() {
        return _validFormats.size();
    }
    public TimSaveFormat getImageFormat_listItem(int i) {
        return _validFormats.get(i);
    }

    // .................................................................

    private String getCmdLineList() {
        StringBuilder sb = new StringBuilder();
        for (TimSaveFormat fmt : _validFormats) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(fmt.getId());
        }
        return sb.toString();
    }

    private TimSaveFormat fromCmdLine(String sCmdLine) {
        for (TimSaveFormat fmt : _validFormats) {
            if (fmt.getId().equals(sCmdLine))
                return fmt;
        }
        return null;
    }

    public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs) {
        if (asArgs == null) return null;

        ArgParser parser = new ArgParser("", false);

        StringHolder timpalettes = new StringHolder();
        parser.addOption("-pal %s", timpalettes);

        StringHolder format = new StringHolder();
        parser.addOption("-imgfmt,-if %s", format);

        String[] asRemain = null;
        asRemain = parser.matchAllArgs(asArgs, 0, 0);

        // parse args for which palettes to save
        if (timpalettes.value != null) {
            boolean[] ablnNewValues = parseNumberListRange(timpalettes.value, getPaletteCount());
            if (ablnNewValues == null) {
                fbs.printlnWarn("Invalid list of palettes " + timpalettes.value);
            } else {
                System.arraycopy(ablnNewValues, 0, _ablnSavePalette, 0, getPaletteCount());
            }
        }

        if (format.value != null) {
            TimSaveFormat fmt = fromCmdLine(format.value);
            if (fmt == null) {
                fbs.printlnWarn("Invalid format " + format.value);
            } else {
                setImageFormat(fmt);
            }
        }

        return asRemain;
    }


    /** Parse a string of comma-delimited numbers and ranges, then creates an
     *  array with indexes toggled based on the numbers.
     *  e.g. 3,6-9,15 */
    private static boolean[] parseNumberListRange(String s, int iMax) {
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


    public void printHelp(FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();
        tfb.setRowSpacing(1);

        tfb.print("-pal <#,#-#>").tab().print("Palettes to save (default all).");
        tfb.newRow();
        tfb.print("-imgfmt,-if <format>").tab().println("Output image format (default "+_validFormats.get(0)+"). Options:");
        tfb.indent().print(getCmdLineList());

        tfb.write(fbs);
    }

    public IDiscItemSaver makeSaver() {
        if (_imageFormat == TimSaveFormat.TIM)
            return new TimRawSaver(_timItem);
        else
            return new TimImageSaver(_imageFormat.getJavaFormat(), _timItem, _ablnSavePalette.clone());
    }

    // ------------------------------------------------------------------------

    private static class TimRawSaver implements IDiscItemSaver {

        private final DiscItemTim _timItem;
        private final File _outputFile;

        public TimRawSaver(DiscItemTim tim) {
            _timItem = tim;
            _outputFile = new File(_timItem.getSuggestedBaseName() + ".tim");
        }
        
        public void startSave(ProgressListener pl, File dir) throws IOException, TaskCanceledException {
            OutputStream os = null;
            try {
                pl.progressStart();
                Tim tim = _timItem.readTim();
                pl.event("Writing " + _outputFile.getName());
                File f = new File(dir, _outputFile.getPath());
                makeDir(f.getParentFile());
                os = new BufferedOutputStream(new FileOutputStream(f));
                tim.write(os);
                pl.progressEnd();
            } catch (NotThisTypeException ex) {
                pl.getLog().log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                pl.getLog().log(Level.SEVERE, null, ex);
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    pl.getLog().log(Level.SEVERE, null, ex);
                }
            }
        }

        public String getInput() {
            return _timItem.getIndexId().serialize();
        }

        public DiscItemTim getDiscItem() {
            return _timItem;
        }

        public String getOutputSummary() {
            return _outputFile.getName();
        }

        public int getOutputFileCount() {
            return 1;
        }

        public File getOutputFile(int i) {
            return _outputFile;
        }

        public void printSelectedOptions(PrintStream ps) {
            ps.println("Format: " + TimSaveFormat.TIM);
        }
        
    }
    
    
    private static class TimImageSaver implements IDiscItemSaver {

        private final JavaImageFormat _imageFormat;
        private final DiscItemTim _timItem;
        private final boolean[] _ablnPalettes;

        public TimImageSaver(JavaImageFormat imageFormat, DiscItemTim timItem, boolean[] ablnPalettes) {
            _imageFormat = imageFormat;
            _timItem = timItem;
            _ablnPalettes = ablnPalettes;
        }

        public String getInput() {
            return _timItem.getIndexId().serialize();
        }

        public DiscItemTim getDiscItem() {
            return _timItem;
        }

        public String getOutputSummary() {
            return makeSelectedFileList();
        }

        public File getOutputFile(int i) {
            int iCount = 0;
            for (boolean b : _ablnPalettes) {
                if (b) {
                    if (iCount == i)
                        return new File(makePaletteFileName(i));
                    iCount++;
                }
            }
            throw new IllegalArgumentException();
        }

        public int getOutputFileCount() {
            int iCount = 0;
            for (boolean b : _ablnPalettes) {
                if (b) iCount++;
            }
            return iCount;
        }

        
        public void startSave(ProgressListener pl, File dir) throws TaskCanceledException {

            try {
                Tim tim = _timItem.readTim();

                pl.progressStart();
                for (int i = 0; i < _ablnPalettes.length; i++) {
                    if (_ablnPalettes[i]) {
                        String sFile = makePaletteFileName(i);
                        pl.event("Writing " + sFile);
                        BufferedImage bi = tim.toBufferedImage(i);
                        File f = new File(dir, sFile);
                        makeDir(f.getParentFile());
                        boolean blnOk = ImageIO.write(bi, _imageFormat.getId(), f);
                        pl.progressUpdate((double)i / _timItem.getPaletteCount());
                    }
                }
                pl.progressEnd();
            } catch (NotThisTypeException ex) {
                pl.getLog().log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                pl.getLog().log(Level.SEVERE, null, ex);
            }
        }

        public void printSelectedOptions(PrintStream ps) {
            ps.println("Palette files: " + makeSelectedFileList());
            ps.println("Format: " + _imageFormat.getExtension());
        }

        private String makeSelectedFileList() {
            StringBuilder sb = new StringBuilder();
            int iRunStart;
            int iCurrentImage = 0;
            while (iCurrentImage < _ablnPalettes.length) {
                for (; iCurrentImage < _ablnPalettes.length && !_ablnPalettes[iCurrentImage]; iCurrentImage++) {
                }
                if (iCurrentImage >= _ablnPalettes.length)
                    break;
                iRunStart = iCurrentImage;
                iCurrentImage++;
                for (; iCurrentImage < _ablnPalettes.length && _ablnPalettes[iCurrentImage]; iCurrentImage++) {
                }
                if (sb.length() > 0) sb.append(", ");
                if (iRunStart == iCurrentImage - 1) {
                    sb.append(makePaletteFileName(iRunStart));
                } else {
                    sb.append(makePaletteFileName(iRunStart));
                    sb.append('-');
                    sb.append(makePaletteFileName(iCurrentImage - 1));
                }
                iCurrentImage++;
            }
            return sb.toString();
        }

        private String makePaletteFileName(int iFile) {
            return String.format("%s_p%02d.%s",
                    _timItem.getSuggestedBaseName(),
                    iFile,
                    _imageFormat.getExtension());
        }
    }

    /** Creates the directory or throws IOException if anything goes wrong. */
    private static void makeDir(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create directory " + dir);
        } else if (!dir.isDirectory()) {
            throw new IOException("Cannot create directory over a file " + dir);
        }
    }
}

