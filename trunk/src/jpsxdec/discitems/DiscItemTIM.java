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
import argparser.StringHolder;
import java.io.PrintStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.sectors.IdentifiedSectorRangeIterator;
import jpsxdec.tim.Tim;
import jpsxdec.util.ProgressListener;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.TabularFeedback;
import jpsxdec.util.TaskCanceledException;


/** Represents a TIM file found in a PSX disc. Currently only searches at the
 *  start of sectors for TIM files, but TIM files could be found anywhere
 *  in a sector.
 *  TODO: Search everywhere */
public class DiscItemTIM extends DiscItem {
    
    public static final String TYPE_ID = "Tim";

    private static final String START_OFFSET_KEY = "Start Offset";
    private final int _iStartOffset;
    private static final String PALETTE_COUNT_KEY = "Palette Count";
    private final int _iPaletteCount;
    private static final String BITSPERPIXEL_KEY = "Bis-per-pixel";
    private final int _iBitsPerPixel;
    
    public DiscItemTIM(int iStartSector, int iEndSector, 
                       int iStartOffset, int iPaletteCount, int iBitsPerPixel)
            throws NotThisTypeException
    {
        super(iStartSector, iEndSector);
        _iStartOffset = iStartOffset;
        _iPaletteCount = iPaletteCount;
        _iBitsPerPixel = iBitsPerPixel;
    }
    
    public DiscItemTIM(DiscItemSerialization fields) throws NotThisTypeException {
        super(fields);
        _iStartOffset = fields.getInt(START_OFFSET_KEY);
        _iPaletteCount = fields.getInt(PALETTE_COUNT_KEY);
        _iBitsPerPixel = fields.getInt(BITSPERPIXEL_KEY);
    }
    
    public DiscItemSerialization serialize() {
        DiscItemSerialization oFields = super.superSerial(TYPE_ID);
        oFields.addNumber(START_OFFSET_KEY, _iStartOffset);
        oFields.addNumber(PALETTE_COUNT_KEY, _iPaletteCount);
        oFields.addNumber(BITSPERPIXEL_KEY, _iBitsPerPixel);
        return oFields;
    }
    
    public String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getInterestingDescription() {
        return "Palettes: " + _iPaletteCount;
    }

    public Tim readTIM() throws IOException {
        
        IdentifiedSectorRangeIterator sectIter = getSectorIterator();
        
        try {
            UnidentifiedDataPullDemuxerIS inStream = new UnidentifiedDataPullDemuxerIS(sectIter);
            return Tim.read(inStream);
        } catch (NotThisTypeException ex) {
            throw new IOException("This is not actually a TIM file?");
        }
    }

    public DiscItemSaverBuilder makeSaverBuilder() {
        return new TimSaverBuilder(this);
    }

    /** Returns the number of palettes if the TIM file is paletted 
     * (with a CLUT). 1 could mean a paletted image with 1 palette, or a
     * true color image. It is possible for paletted images to not have a CLUT,
     * in that case a paletted image with a gray scale palette is created. */
    public int getPaletteCount() {
        return _iPaletteCount;
    }

    public int getBitsPerPixel() {
        return _iBitsPerPixel;
    }
    
    private static class TimSaverBuilder extends DiscItemSaverBuilder {

        private Tim _tim;
        private boolean[] _ablnSavePalette;
        private JavaImageFormat _imageFormat;
        private DiscItemTIM _timItem;

        public void resetToDefaults() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean copySettings(DiscItemSaverBuilder other) {
            if (other instanceof TimSaverBuilder) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            return false;
        }

        public static String getCmdLineList(boolean blnTrueColor) {
            StringBuilder sb = new StringBuilder();
            for (JavaImageFormat fmt : JavaImageFormat.getAvailable()) {
                if (blnTrueColor == fmt.hasTrueColor() ||
                    (!blnTrueColor) == fmt.paletted())
                {
                    if (sb.length() > 0)
                        sb.append(", ");
                    sb.append(fmt.getId());
                }
            }
            return sb.toString();
        }

        public static JavaImageFormat fromCmdLine(String sCmdLine) {
            for (JavaImageFormat fmt : JavaImageFormat.getAvailable()) {
                if (fmt.getId().equals(sCmdLine))
                    return fmt;
            }
            return null;
        }

        public TimSaverBuilder(DiscItemTIM timItem) {
            _timItem = timItem;
            _ablnSavePalette = new boolean[_timItem.getPaletteCount()];
            Arrays.fill(_ablnSavePalette, true);
            _imageFormat = JavaImageFormat.PNG;
        }

        private boolean timIsTrueColor() {
            return _timItem.getBitsPerPixel() == 16 ||
                   _timItem.getBitsPerPixel() == 24;
        }

        public DiscItemSaverBuilderGui getOptionPane() {
            return null;
        }

        public int getPaletteCount() {
            return _tim.getPaletteCount();
        }
        public void setSavePalette(int iPalette, boolean blnSave) {
            _ablnSavePalette[iPalette] = blnSave;
        }
        public void toggleSavePalette(int iPalette) {
            _ablnSavePalette[iPalette] = !_ablnSavePalette[iPalette];
        }
        public boolean getSavePalette(int iPalette) {
            return _ablnSavePalette[iPalette];
        }

        public void setImageFormat(JavaImageFormat fmt) {
            _imageFormat = fmt;
        }

        public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs) {
            if (asArgs == null) return null;

            ArgParser parser = new ArgParser("", false);

            StringHolder timpalettes = new StringHolder();
            parser.addOption("-pal %s", timpalettes);

            StringHolder format = new StringHolder();
            parser.addOption("-imgfmt,-if %s {"+ getCmdLineList(timIsTrueColor()) +"}", format);

            String[] asRemain = null;
            asRemain = parser.matchAllArgs(asArgs, 0, 0);

            // parse args for which palettes to save
            if (timpalettes.value != null) {
                // TODO: finish this
                _ablnSavePalette = parseNumberListRange(timpalettes.value, getPaletteCount());
            }

            if (format.value != null) {
                JavaImageFormat fmt = fromCmdLine(format.value);
                if (fmt == null) {
                    fbs.printlnWarn("Invalid format " + format.value);
                } else {
                    fbs.printlnWarn("Using format " + fmt.getExtension());
                    setImageFormat(fmt);
                }
            }

            return asRemain;
        }


        /** Parse a string of comma-delimited numbers and ranges, then creates an
         *  array with indexes toggled based on the numbers.
         *  e.g. 3,6-9,15 */
        private static boolean[] parseNumberListRange(String s, int iMax) {
            boolean[] abln = new boolean[iMax];
            Arrays.fill(abln, false);
            try {
                for (String num : s.split(",")) {
                    if (s.indexOf('-') > 0) {
                        // TODO: Finish
                        throw new UnsupportedOperationException("Not finished yet.");
                    } else {
                        abln[Integer.parseInt(num)] = true;
                    }
                }
            } catch (NumberFormatException ex) {
                return null;
            } catch (IndexOutOfBoundsException ex) {
                return null;
            }
            return abln;
        }
        

        public void printHelp(FeedbackStream fbs) {
            TabularFeedback tfb = new TabularFeedback();
            tfb.setRowSpacing(1);

            tfb.print("-pal <#,#-#>").tab().print("Palettes to save (default all).");
            tfb.newRow();
            tfb.print("-imgfmt,-if <format>").tab().println("Output image format (default png). Options:");
            tfb.indent().print(getCmdLineList(timIsTrueColor()));

            tfb.write(fbs);
        }

        public IDiscItemSaver makeSaver() {
            return new TimItemSaver(_tim, _imageFormat, _timItem);
        }

        public void printSelectedOptions(PrintStream ps) {
            // TODO: Print tim options
        }



    }


    private static class TimItemSaver implements IDiscItemSaver {
        private Tim _tim;
        private JavaImageFormat _imageFormat;
        private DiscItemTIM _timItem;

        public TimItemSaver(Tim tim, JavaImageFormat imageFormat, DiscItemTIM timItem) {
            _tim = tim;
            _imageFormat = imageFormat;
            _timItem = timItem;
        }

        public String getInput() {
            return _timItem.getIndexId().serialize();
        }

        public String getOutput() {
            return makeFileName(0) + " - " + makeFileName(_tim.getPaletteCount()-1);
        }

        public void startSave(ProgressListener pl, File dir) throws TaskCanceledException {

            try {
                pl.progressStart();
                for (int i = 0; i < _tim.getPaletteCount(); i++) {
                    String sFile = makeFileName(i);
                    pl.event("Writing " + sFile);
                    BufferedImage bi = _tim.toBufferedImage(i);
                    File f = new File(sFile);
                    ImageIO.write(bi, _imageFormat.getId(), f);
                    pl.progressUpdate((double)i / _tim.getPaletteCount());
                }
                pl.progressEnd();
            } catch (IOException ex) {
                pl.getLog().log(Level.SEVERE, null, ex);
            }
        }

        private String makeFileName(int iFile) {
            return String.format("%s_p%02d.%s",
                    _timItem.getSuggestedBaseName(),
                    iFile,
                    _imageFormat.getExtension());
        }
    }


}