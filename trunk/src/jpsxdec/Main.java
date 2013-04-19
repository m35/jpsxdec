/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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

import jpsxdec.util.ConsoleProgressListenerLogger;
import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.StringHolder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaRiffHeader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemStrVideoStream;
import jpsxdec.discitems.DiscItemTim;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.DiscItemXaAudioStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.discitems.savers.MdecDecodeQuality;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.sectors.UnidentifiedSector;
import jpsxdec.tim.Tim;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.TaskCanceledException;
import jpsxdec.util.player.PlayController;

public class Main {


    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private static FeedbackStream Feedback = new FeedbackStream(System.out, FeedbackStream.NORM);

    public static void loadDefaultLogger() {
        loadLogger("LogToFile.properties");
    }

    public static void loadLogger(String sLogFileResource) {
        try { // load the logger configuration
            InputStream is = Main.class.getResourceAsStream(sLogFileResource);
            if (is != null)
                java.util.logging.LogManager.getLogManager().readConfiguration(is);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, null, ex);
        }
    }

    /** Main entry point to the jPSXdec program. */
    public static void main(String[] asArgs) {

        loadDefaultLogger();

        if (asArgs.length < 1) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new Gui().setVisible(true);
                }
            });
            return;
        }

        asArgs = checkVerbosity(asArgs, Feedback);

        Feedback.println(Version.VerStringNonCommercial);

        Command[] aoCommands = {
            new Command_CopySect(),
            new Command_SectorDump(),
            new Command_Static(),
            new Command_Visualize(),
            new Command_Item(),
            new Command_All(),
        };

        ArgParser ap = new ArgParser("", false);

        StringHolder inputFileArg = new StringHolder();
        ap.addOption("-f,-file %s", inputFileArg);

        StringHolder indexFileArg = new StringHolder();
        ap.addOption("-x,-index %s", indexFileArg);

        for (Command command : aoCommands) {
            command.init(ap);
        }

        asArgs = ap.matchAllArgs(asArgs, 0, 0);

        String sErr = ap.getErrorMessage();
        if (sErr != null) {
            Feedback.printlnErr("Error: " + sErr);
            Feedback.printlnErr("Try -? for help.");
            System.exit(1);
        }

        Command mainCommand = null;
        for (Command command : aoCommands) {
            if(command.found()) {
                if (mainCommand != null) {
                    Feedback.printlnErr("Too many main commands.");
                    Feedback.printlnErr("Try -? for help.");
                    System.exit(1);
                }
                mainCommand = command;
            }
        }

        try {
            if (mainCommand == null) {
                if (checkForMainHelp(asArgs)) {
                    printMainHelp(Feedback);
                } else {
                    if (inputFileArg.value != null && indexFileArg.value != null) {
                        createAndSaveIndex(inputFileArg.value, indexFileArg.value);
                    } else {
                        Feedback.printlnErr("Need a main command.");
                        Feedback.printlnErr("Try -? for help.");
                        System.exit(1);
                    }
                }
            } else {
                String sError = mainCommand.validate();
                if (sError != null) {
                    Feedback.printlnErr(sError);
                    Feedback.printlnErr("Try -? for help.");
                    System.exit(1);
                } else {

                    // TODO: let commands load what's needed via function calls
                    switch (mainCommand.getWhatsNeeded()) {
                        case Command.NEEDS_INFILE:
                            if (inputFileArg.value == null) {
                                Feedback.printlnErr("Input file required");
                                System.exit(1);
                            }
                            _INPUT_FILE = confirmFile(inputFileArg.value);
                            break;
                        case Command.NEEDS_INDISC:
                            if (inputFileArg.value == null) {
                                Feedback.printlnErr("Input file disc image required");
                                System.exit(1);
                            }
                            _INPUT_DISC = loadDisc(inputFileArg.value);
                            break;
                        case Command.NEEDS_INDEX:
                            _INPUT_INDEX = getIndexSomehow(inputFileArg.value, indexFileArg.value);
                            break;
                    }
                    mainCommand.execute(asArgs);
                }
            }
        } catch (Throwable ex) {
            Feedback.printlnErr("Error: " + ex.toString() + " (" + ex.getClass().getSimpleName() + ")");
            LOG.log(Level.SEVERE, "Unhandled exception", ex);
            System.exit(1);
        }
        System.exit(0);
    }

    // -------------------------------------------------------------
    
    private static File _INPUT_FILE;
    private static CdFileSectorReader _INPUT_DISC;
    private static DiscIndex _INPUT_INDEX;

    // -------------------------------------------------------------

    private static void createAndSaveIndex(String sDiscFile, String sIndexFile) throws IOException {
        CdFileSectorReader cd = loadDisc(sDiscFile);
        DiscIndex index = buildIndex(cd);
        saveIndex(index, sIndexFile);
        cd.close();
    }

    private static CdFileSectorReader loadDisc(String sDiscFile) throws IOException {
        if (sDiscFile == null) {
            throw new IllegalArgumentException("Need disc file.");
        }
        Feedback.println("Opening " + sDiscFile);
        CdFileSectorReader cd = new CdFileSectorReader(new File(sDiscFile));
        Feedback.println("Identified as " + cd.getTypeDescription());
        return cd;
    }

    private static File confirmFile(String sFile) throws FileNotFoundException {
        File file = new File(sFile);
        if (!file.exists())
            throw new FileNotFoundException();
        return file;
    }

    private static DiscIndex loadIndex(String sIndexFile) throws IOException, NotThisTypeException {
        Feedback.println("Reading index file " + sIndexFile);
        DiscIndex index = new DiscIndex(sIndexFile, false, Logger.getLogger("index"));
        Feedback.println("Opening source file " + index.getSourceCd().getSourceFile());
        Feedback.println(index.size() + " items loaded");
        return index;
    }

    private static DiscIndex buildIndex(CdFileSectorReader cd) {
        Feedback.println("Building index");
        DiscIndex index = null;
        try {
            ConsoleProgressListenerLogger cpll = new ConsoleProgressListenerLogger("index", Feedback);
            cpll.setHeader(1, "Indexing " + cd.toString());
            index = new DiscIndex(cd, cpll);
            cpll.close();
        } catch (TaskCanceledException ex) {
            LOG.severe("SHOULD NEVER HAPPEN");
        }
        Feedback.println(index.size() + " items found.");
        return index;
    }

    private static void saveIndex(DiscIndex index, String sIndexFile) throws IOException {
        if (index.size() < 1) {
            Feedback.println("No items found, not saving index file.");
        } else if (sIndexFile.equals("-")) {
            index.serializeIndex(System.out);
        } else {
            Feedback.println("Saving index as " + sIndexFile);
            PrintStream printer = new PrintStream(sIndexFile);
            try {
                index.serializeIndex(printer);
            } finally {
                printer.close();
            }
        }
    }

    private static DiscIndex getIndexSomehow(String sDiscFile, String sIndexFile) throws IOException, NotThisTypeException {
        final DiscIndex index;
        if (sIndexFile != null) {
            File idxFile = new File(sIndexFile);
            if (sDiscFile != null) {
                CdFileSectorReader cd = loadDisc(sDiscFile);
                if (idxFile.exists()) {
                    Feedback.println("Reading index file " + sIndexFile);
                    index = new DiscIndex(sIndexFile, cd, Logger.getLogger("index"));
                    Feedback.println(index.size() + " items loaded.");
                } else {
                    index = buildIndex(cd);
                    saveIndex(index, sIndexFile);
                }
            } else {
                index = loadIndex(sIndexFile);
            }
        } else {
            if (sDiscFile != null) {
                CdFileSectorReader cd = loadDisc(sDiscFile);
                index = buildIndex(cd);
            } else {
                throw new IllegalArgumentException("Need a input file and/or index file to load.");
            }
        }
        
        return index;
    }

    // -------------------------------------------------------------


    private static String[] MAIN_HELP = loadMainHelp();

    private static String[] loadMainHelp() {
        InputStream is = Main.class.getResourceAsStream("main_cmdline_help.dat");
        if (is == null)
            throw new RuntimeException("Unable to find help resource " +
                    Main.class.getResource("main_cmdline_help.dat"));
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        ArrayList<String> lines = new ArrayList<String>();
        try {
            String sLine;
            while ((sLine = br.readLine()) != null) {
                lines.add(sLine);
            }
            br.close();
            return lines.toArray(new String[lines.size()]);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void printMainHelp(PrintStream ps) {
        for (String sLine : MAIN_HELP) {
            ps.println(sLine);
        }
    }

    private static String[] checkVerbosity(String[] asArgs, FeedbackStream fbs) {
        ArgParser ap = new ArgParser("", false);

        StringHolder verbose = new StringHolder();
        ap.addOption("-v,-verbose %s", verbose);
        asArgs = ap.matchAllArgs(asArgs, 0, 0);

        if (verbose.value != null) {
            try {
                int iValue = Integer.parseInt(verbose.value);
                if (iValue >= FeedbackStream.NONE && iValue <= FeedbackStream.MORE)
                    fbs.setLevel(iValue);
                else
                    fbs.printlnWarn("Invalid verbosity level " + iValue);
            } catch (NumberFormatException ex) {
                fbs.printlnWarn("Invalid verbosity level " + verbose.value);
            }
        }

        return asArgs;
    }

    private static boolean checkForMainHelp(String[] asArgs) {
        if (asArgs == null)
            return false;

        ArgParser ap = new ArgParser("", false);

        BooleanHolder help = new BooleanHolder();
        ap.addOption("-?,-h,-help %v", help);
        ap.matchAllArgs(asArgs, 0, 0);

        return help.value;
    }

    // -------------------------------------------------------------

    // TODO: open and load these data when these functions are called
    private static CdFileSectorReader getCdReader() {
        return _INPUT_DISC;
    }

    private static DiscIndex getIndex() {
        return _INPUT_INDEX;
    }

    private static File getInFile() {
        return _INPUT_FILE;
    }


    // #########################################################################


    private static abstract class Command {
        protected final String _sArg;

        /** @param sArg  Option name. */
        public Command(String sArg) {
            _sArg = sArg;
        }

        abstract public void init(ArgParser ap);
        abstract public boolean found();
        abstract public String validate();
        abstract public int execute(String[] asRemainingArgs);
        abstract public int getWhatsNeeded();

        public static final int NEEDS_INFILE = 1;
        public static final int NEEDS_INDISC = 2;
        public static final int NEEDS_INDEX = 3;
    }

    private static abstract class StringCommand extends Command {

        final private StringHolder _receiver = new StringHolder();

        public StringCommand(String sArg) {
            super(sArg);
        }

        @Override
        public void init(ArgParser ap) {
            ap.addOption(_sArg + " %s", _receiver);
        }

        @Override
        final public boolean found() {
            return _receiver.value != null;
        }

        final public String validate() {
            return validate(_receiver.value);
        }

        /** Checks that the option value is valid.
         *  Returns {@code null} if OK, or error message if invalid. */
        abstract protected String validate(String sOptionValue);
    }

    // #########################################################################


    private static class Command_CopySect extends StringCommand {
        public Command_CopySect() {
            super("-copysect");
        }

        private int[] _aiStartEndSectors;

        protected String validate(String s) {
            _aiStartEndSectors = parseNumberRange(s);
            if (_aiStartEndSectors == null)
                return "Invalid sector range: " + s;
            else
                return null;
        }
        public int getWhatsNeeded() {
            return NEEDS_INDISC;
        }
        public int execute(String[] asRemainingArgs) {

            CdFileSectorReader cdReader = getCdReader();

            try {

                String sOutputFile = String.format("%s%d-%d.dat",
                            Misc.getBaseName(cdReader.getSourceFile().getName()),
                            _aiStartEndSectors[0],
                            _aiStartEndSectors[1]);

                Feedback.printlnErr("Copying sectors " + _aiStartEndSectors[0] + " - " + _aiStartEndSectors[1] + " to " + sOutputFile);

                OutputStream os = new BufferedOutputStream(new FileOutputStream(sOutputFile));

                if (cdReader.getSectorSize() == CdFileSectorReader.SECTOR_SIZE_2352_BIN && asRemainingArgs != null) {
                    ArgParser parser = new ArgParser("", false);
                    BooleanHolder noCdxaHeader = new BooleanHolder(false);
                    parser.addOption("-nocdxa %v", noCdxaHeader);
                    parser.matchAllArgs(asRemainingArgs, 0, 0);
                    if (!noCdxaHeader.value) {
                        long lngFileSize = (_aiStartEndSectors[1] - _aiStartEndSectors[0] + 1) * (long)2352;
                        CdxaRiffHeader.write(os, lngFileSize);
                    }
                }

                for (int i = _aiStartEndSectors[0]; i <= _aiStartEndSectors[1]; i++) {
                    CdSector sector = cdReader.getSector(i);
                    if (sector == null) {
                        Feedback.printlnErr("Error reading sector " + i);
                        return -1;
                    } else {
                        os.write(sector.getRawSectorDataCopy());
                    }
                }

                os.close();

                return 0;

            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Error copying sectors.", ex);
                Feedback.printlnErr(ex);
                return -1;
            }
        }
    }


    /** Parse a number range. e.g. 5-10
     * @return Array of 2 elements, or null on error. */
    public static int[] parseNumberRange(String s) {
        int iStart, iEnd;
        String[] split = s.split("-");
        try {

            if (split.length == 2) {
                iStart = Integer.parseInt(split[0]);
                iEnd = Integer.parseInt(split[1]);
            } else {
                iStart = iEnd = Integer.parseInt(s);
            }

            return new int[] {iStart, iEnd};

        } catch (NumberFormatException ex) {
            return null;
        }
    }


    private static class Command_Static extends StringCommand {

        public Command_Static() {
            super("-static");
        }

        private static enum StaticType { bs, mdec, tim }
        private StaticType _eStaticType;

        protected String validate(String s) {
            for (StaticType type : StaticType.values()) {
                if (s.equals(type.name())) {
                    _eStaticType = type;
                    return null;
                }
            }
            return "Invalid static type: " + s;
        }
        public int getWhatsNeeded() {
            return NEEDS_INFILE;
        }
        public int execute(String[] asRemainingArgs) {
            try {

                File inFile = getInFile();

                switch (_eStaticType) {
                    case bs:
                    case mdec:
                        
                        if (asRemainingArgs == null) {
                            Feedback.printlnErr("-dim option required");
                            return -1;
                        }

                        ArgParser parser = new ArgParser("", false);
                        BooleanHolder debug = new BooleanHolder(false);
                        parser.addOption("-debug %v", debug);
                        StringHolder dimentions = new StringHolder();
                        parser.addOption("-dim %s", dimentions);
                        StringHolder quality = new StringHolder("high");
                        parser.addOption("-quality,-q %s", quality);
                        StringHolder format = new StringHolder("png");
                        parser.addOption("-fmt %s", format);
                        StringHolder upsample = new StringHolder();
                        parser.addOption("-up %s", upsample);

                        //......
                        parser.matchAllArgs(asRemainingArgs, 0, 0);
                        //......

                        if (dimentions.value == null) {
                            Feedback.printlnErr("-dim option required");
                            return -1;
                        }

                        final int iWidth, iHeight;
                        int[] aiDim = Misc.splitInt(dimentions.value, "x");
                        iWidth = aiDim[0]; iHeight = aiDim[1];

                        if (debug.value) {
                            BitStreamUncompressor.DEBUG = true;
                            MdecDecoder.DEBUG = true;
                            boolean blnAssertsEnabled = false;
                            assert blnAssertsEnabled = true;
                            if (!blnAssertsEnabled) {
                                Feedback.printlnWarn("Unable to enable decoding debug because asserts are disabled.");
                                Feedback.printlnWarn("Start java using the -ea option.");
                            }
                        }

                        Feedback.println("Reading static file " + inFile);

                        MdecInputStream mdecIn;
                        byte[] abBitstream = IO.readFile(inFile);
                        if (_eStaticType == StaticType.bs) {
                            mdecIn = BitStreamUncompressor.identifyUncompressor(abBitstream);
                            if (mdecIn == null) {
                                Feedback.printlnErr("Unable to identify bitstream format.");
                                return -1;
                            }
                            ((BitStreamUncompressor)mdecIn).reset(abBitstream);
                        } else {
                            mdecIn = new MdecInputStreamReader(new ByteArrayInputStream(abBitstream));
                        }

                        String sFileBaseName = Misc.getBaseName(inFile.getName());

                        JavaImageFormat jImgFmt = null;
                        if (format.value.equals(StaticType.mdec.name())) {
                            FileOutputStream fos = new FileOutputStream(sFileBaseName + ".mdec");
                            try {
                                MdecInputStreamReader.writeMdecDims(mdecIn, fos, iWidth, iHeight);
                            } finally {
                                fos.close();
                            }
                        } else {
                            for (JavaImageFormat fmt : JavaImageFormat.getAvailable()) {
                                if (fmt.getExtension().equals(format.value)) {
                                    jImgFmt = fmt;
                                    break;
                                }
                            }

                            if (jImgFmt == null) {
                                Feedback.printlnErr("Invalid format type " + format.value);
                                return -1;
                            }

                            MdecDecodeQuality decQuality = MdecDecodeQuality.fromCmdLine(quality.value);
                            if (decQuality == null)
                                throw new RuntimeException("Invalid quality " + quality.value);

                            MdecDecoder vidDecoder = decQuality.makeDecoder(iWidth, iHeight);

                            if (vidDecoder instanceof MdecDecoder_double_interpolate) {
                                MdecDecoder_double_interpolate.Upsampler up;
                                if (upsample.value == null) {
                                    up = MdecDecoder_double_interpolate.Upsampler.Bicubic;
                                } else {
                                    up = MdecDecoder_double_interpolate.Upsampler.fromCmdLine(upsample.value);
                                    if (up == null) {
                                        Feedback.printlnErr("Invalid upsampling " + upsample.value);
                                        return -1;
                                    }
                                }
                                Feedback.println("Using upsampling " + up.name());
                                ((MdecDecoder_double_interpolate)vidDecoder).setResampler(up);
                            }

                            Feedback.println("Using quality " + quality.value);

                            try {
                                vidDecoder.decode(mdecIn);
                            } catch (MdecException.Decode ex) {
                                Feedback.printlnErr(ex);
                            }
                            int[] aiRgb = new int[iWidth * iHeight];
                            vidDecoder.readDecodedRgb(iWidth, iHeight, aiRgb);
                            RgbIntImage rgb = new RgbIntImage(iWidth, iHeight, aiRgb);
                            BufferedImage bi = rgb.toBufferedImage();

                            ImageIO.write(bi, jImgFmt.getId(), new File(sFileBaseName + "." + jImgFmt.getExtension()));

                        }

                        Feedback.println("Frame converted successfully.");

                        return 0;

                    case tim:
                        Feedback.println("Reading TIM file "+ inFile);
                        final FileInputStream is = new FileInputStream(inFile);
                        try {
                            String sOutBaseName = Misc.getBaseName(inFile.getName());

                            Tim tim = Tim.read(is);
                            
                            Feedback.println(tim);

                            int iDigitCount = String.valueOf(tim.getPaletteCount()).length();
                            for (int i = 0; i < tim.getPaletteCount(); i++) {
                                BufferedImage bi = tim.toBufferedImage(i);
                                String sFileName = String.format("%s_p%0"+iDigitCount+"d.png", sOutBaseName, i);
                                File file = new File(sFileName);
                                Feedback.println("Writing " + file.getPath());
                                ImageIO.write(bi, "png", file);
                            }

                            Feedback.println("Image converted successfully");

                        } catch (NotThisTypeException ex) {
                            Feedback.printlnErr("Error: not a Tim image");
                            return -1;
                        } finally {
                            is.close();
                        }

                        return 0;
                }

                throw new RuntimeException("Shouldn't happen");


            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "IO error", ex);
                Feedback.printlnErr(ex);
                return -1;
            } catch (NotThisTypeException ex) {
                LOG.log(Level.SEVERE, null, ex);
                Feedback.printlnErr(ex);
                return -1;
            } catch (MdecException ex) {
                LOG.log(Level.SEVERE, null, ex);
                Feedback.printlnErr(ex);
                return -1;
            }

        }
    }

    private static class Command_SectorDump extends StringCommand {

        private String _sOutfile;

        public Command_SectorDump() {
            super("-sectordump");
        }
        protected String validate(String s) {
            _sOutfile = s;
            return null;
        }
        public int getWhatsNeeded() {
            return NEEDS_INDISC;
        }
        public int execute(String[] asRemainingArgs) {
            CdFileSectorReader cdReader = getCdReader();

            Feedback.println("Generating sector list");

            PrintStream ps;
            try {
                if (_sOutfile.equals("-"))
                    ps = System.out;
                else
                    ps = new PrintStream(_sOutfile);

                for (int i = 0; i < cdReader.getLength(); i++) {
                    CdSector cdSect = cdReader.getSector(i);
                    IdentifiedSector idSect = IdentifiedSector.identifySector(cdSect);
                    if (idSect != null) {
                        String s = idSect.toString();
                        ps.println(s);
                    } else {
                        ps.println(cdSect.toString());
                    }
                }

                ps.flush();
                if (!ps.equals(System.out)) ps.close();
                return 0;

            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "IO error", ex);
                Feedback.printlnErr(ex);
                return -1;
            }
        }
    }

    private static class Command_Item extends StringCommand {

        private String _sItemId;
        private int _iItemNum;

        public Command_Item() {
            super("-i,-item");
        }
        protected String validate(String s) {
            try {
                _iItemNum = Integer.parseInt(s);
                if (_iItemNum < 0)
                    return "Invalid item number: " + s;
                else
                    return null;
            } catch (NumberFormatException ex) {
                if (s.contains(" "))
                    return "Invalid item identifier: " + s;
                _sItemId = s;
                return null;
            }
        }
        public int getWhatsNeeded() {
            return NEEDS_INDEX;
        }
        public int execute(String[] asRemainingArgs) {
            DiscIndex discIndex = getIndex();

            DiscItem item;
            if (_sItemId != null) {
                item = discIndex.getById(_sItemId);
                if (item == null)
                    throw new IllegalArgumentException("Could not find disc item " + _sItemId);
            } else {
                item = discIndex.getByIndex(_iItemNum);
                if (item == null)
                    throw new IllegalArgumentException("Could not find disc item " + _iItemNum);
            }

            return handleItem(item, asRemainingArgs);
        }
    }

    private static void player(final PlayController controller, DiscItem item) throws InterruptedException {

        final JFrame window = new JFrame(Version.VerStringNonCommercial + " - Player");

        window.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                synchronized( window ) {
                    window.notifyAll();
                }
            }

            @Override
            public void windowClosed( java.awt.event.WindowEvent e ) {
                synchronized( window ) {
                    window.notifyAll();
                }
            }
        });

        window.add(new JLabel(item.toString()), BorderLayout.NORTH);

        final JButton startBtn = new JButton("Play");
        startBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startBtn.setEnabled(false);
                try {
                    controller.play();
                } catch (Throwable ex) {
                    Feedback.printlnErr(ex);
                    synchronized( window ) {
                        window.notifyAll();
                    }
                }
            }
        });
        window.add(startBtn, BorderLayout.SOUTH);

        if (controller.hasVideo()) {
            controller.setVideoZoom(2);
            window.add(controller.getVideoScreen(), BorderLayout.CENTER);
        }

        window.pack();

        window.setLocationRelativeTo(null); // center window

        window.setVisible(true);

        synchronized (window) {
            window.wait();
        }

        controller.stop();

    }

    private static class Command_All extends StringCommand {

        private String _sType;

        public Command_All() {
            super("-a,-all");
        }
        protected String validate(String s) {
            _sType = s;
            return null;
        }
        public int getWhatsNeeded() {
            return NEEDS_INDEX;
        }
        public int execute(String[] asRemainingArgs) {
            DiscIndex discIndex = getIndex();
            
            boolean blnFound = false;

            for (DiscItem item : discIndex) {
                if (item.getType().name().equalsIgnoreCase(_sType)) {
                    blnFound = true;
                    int iRet = handleItem(item, asRemainingArgs);
                    if (iRet != 0)
                        return iRet;
                    Feedback.println("Item complete.");
                    Feedback.println();
                }
            }

            if (!blnFound) {
                Feedback.println("Sorry, couldn't find any disc items of type " + _sType);
            } else {
                Feedback.println("All index items complete.");
            }

            return 0;
            
        }
    }

    private static int handleItem(DiscItem item, String[] asRemainingArgs) {
        ArgParser ap = new ArgParser("", false);

        BooleanHolder fpsDumpArg = new BooleanHolder();
        ap.addOption("-fpsdump %v", fpsDumpArg);
        BooleanHolder itemHelpArg = new BooleanHolder();
        ap.addOption("-?,-h,-help %v", itemHelpArg);
        BooleanHolder playArg = new BooleanHolder();
        ap.addOption("-play %v", playArg);
        BooleanHolder frameInfoArg = new BooleanHolder();
        ap.addOption("-frameinfodump %v", frameInfoArg);
        StringHolder replaceFrames = new StringHolder();
        ap.addOption("-replaceframes %s", replaceFrames);
        StringHolder replaceTim = new StringHolder();
        ap.addOption("-replacetim %s", replaceTim);
        StringHolder replaceXa = new StringHolder();
        ap.addOption("-replacexa %s", replaceXa);
        StringHolder xaNum = new StringHolder();
        ap.addOption("-xa %s", xaNum);
        StringHolder directory = new StringHolder();
        ap.addOption("-dir %s", directory);

        if (asRemainingArgs != null)
            asRemainingArgs = ap.matchAllArgs(asRemainingArgs, 0, 0);

        try {
            if (fpsDumpArg.value) {

                if (!(item instanceof DiscItemStrVideoStream)) {
                    Feedback.printlnErr("Disc item isn't a video.");
                    return -1;
                } else {
                    Feedback.println("Generating fps dump.");
                    PrintStream ps = new PrintStream("fps.txt");
                    try {
                        ((DiscItemStrVideoStream)item).fpsDump(ps);
                    } finally {
                        ps.close();
                    }
                }

            } else if (itemHelpArg.value) {
                Feedback.println("Detailed help for");
                Feedback.println(item);
                item.makeSaverBuilder().printHelp(Feedback);
            } else if (playArg.value) {
                if (item instanceof DiscItemVideoStream) {
                    Feedback.println("Creating player for");
                    Feedback.println(item);
                    player(((DiscItemVideoStream)item).makePlayController(), item);
                } else if (item instanceof DiscItemAudioStream) {
                    player(((DiscItemAudioStream)item).makePlayController(), item);
                } else {
                    Feedback.printlnErr(item.toString());
                    Feedback.printlnErr("is not audio or video. Cannot create player.");
                    return -1;
                }
            } else if (frameInfoArg.value) {
                if (!(item instanceof DiscItemVideoStream)) {
                    Feedback.printlnErr("Disc item isn't a video.");
                    return -1;
                } else {
                    ((DiscItemVideoStream)item).frameInfoDump(Feedback);
                }
            } else if (replaceFrames.value != null) {
                if (!(item instanceof DiscItemVideoStream)) {
                    Feedback.printlnErr("Disc item isn't a video.");
                    return -1;
                } else {
                    Feedback.printlnWarn("Hope your disc image is backed up because this is irreversable.");
                    Feedback.printlnWarn("Reopening disc image with write access.");
                    item.getSourceCd().reopenForWriting();
                    ((DiscItemVideoStream)item).replaceFrames(Feedback, replaceFrames.value);
                }
            } else if (replaceTim.value != null) {
                if (!(item instanceof DiscItemTim)) {
                    Feedback.printlnErr("Disc item isn't a TIM image.");
                    return -1;
                } else {
                    DiscItemTim timItem = (DiscItemTim)item;
                    if (timItem.getPaletteCount() != 1) {
                        Feedback.printlnErr("Unable to replace a TIM image that has multiple palettes.");
                        return -1;
                    }
                    BufferedImage bi = ImageIO.read(new File(replaceTim.value));
                    Feedback.printlnWarn("Hope your disc image is backed up because this is irreversable.");
                    Feedback.printlnWarn("Reopening disc image with write access.");
                    item.getSourceCd().reopenForWriting();
                    timItem.replace(Feedback, bi);
                }
            } else if (replaceXa.value != null) {
                if (!(item instanceof DiscItemXaAudioStream)) {
                    Feedback.printlnErr("Disc item isn't a XA stream.");
                    return -1;
                } else {
                    DiscItemXaAudioStream xaItem = (DiscItemXaAudioStream)item;
                    Feedback.printlnWarn("Hope your disc image is backed up because this is irreversable.");
                    Feedback.println("Opening patch index " + replaceXa.value);
                    DiscIndex patchIndex;
                    try {
                        patchIndex = new DiscIndex(replaceXa.value, LOG);
                    } catch (IOException ex) {
                        Feedback.printlnErr(ex);
                        return -1;
                    }                    
                    
                    DiscItemXaAudioStream patchXa;
                    try {
                        int iPatchXaIndex = Integer.parseInt(xaNum.value);
                        patchXa = (DiscItemXaAudioStream) patchIndex.getByIndex(iPatchXaIndex);
                    } catch (Throwable ex) {
                        Feedback.printlnErr("Invalid or missing XA item number " + xaNum.value);
                        return -1;
                    }
                    Feedback.printlnWarn("Reopening disc image with write access.");
                    item.getSourceCd().reopenForWriting();
                    xaItem.replaceXa(Feedback, patchXa);
                }
            } else {
                File dir;
                if (directory.value != null)
                    dir = new File(directory.value);
                else
                    dir = new File(".");
                // decode/extract the desired disc item
                decodeDiscItem(item, dir, asRemainingArgs);
                Feedback.println("Disc decoding/extracting complete.");
            }

        } catch (Throwable ex) {
            LOG.log(Level.SEVERE, "Item error", ex);
            Feedback.printlnErr(ex);
            return -1;
        }

        return 0;
    }

    private static void decodeDiscItem(DiscItem item, File dir, String[] asRemainingArgs) throws IOException {

        DiscItemSaverBuilder builder = item.makeSaverBuilder();

        Feedback.println("Saving " + item.toString());

        builder.commandLineOptions(asRemainingArgs, Feedback);

        Feedback.println();
        
        IDiscItemSaver saver = builder.makeSaver();

        saver.printSelectedOptions(Feedback);

        long lngStart, lngEnd;
        lngStart = System.currentTimeMillis();
        try {
            ConsoleProgressListenerLogger cpll = new ConsoleProgressListenerLogger("save", Feedback);
            cpll.setHeader(1, item.getSourceCd().toString());
            cpll.setHeader(2, item.toString());
            saver.startSave(cpll, dir);
            cpll.close();
        } catch (TaskCanceledException ex) {
            LOG.severe("SHOULD NEVER HAPPEN");
        }
        lngEnd = System.currentTimeMillis();
        Feedback.format("Time: %1.2f sec", (lngEnd - lngStart) / 1000.0);
        Feedback.println();
    }


    private static class Command_Visualize extends StringCommand {
        private String _sOutfile;

        public Command_Visualize() {
            super("-visualize");
        }

        protected String validate(String s) {
            _sOutfile = s;
            return null;
        }
        
        public int getWhatsNeeded() {
            return NEEDS_INDEX;
        }
        public int execute(String[] asRemainingArgs) {
            DiscIndex index = getIndex();
            CdFileSectorReader cd = index.getSourceCd();

            try {

            final int SECTOR_SECTION_SIZE = 32;
            final int TEXT_LINE_HEIGHT = 16;
            final int BOX_AREA_WIDTH = 16;
            final int BOX_MARGIN_LEFT = 2;
            final int BOX_MARGIN_RIGHT = 2;
            final int BOX_WIDTH = BOX_AREA_WIDTH - (BOX_MARGIN_RIGHT + BOX_MARGIN_LEFT);
            final double MAX_PDF_SIZE = 200.0*72.0 - 18.0;

            /* priority:
             * ISO file
             * video
             * audio
             * tim
             *
             * summarize to just the important data-points
             */

            Feedback.println("Generating visualization");

            int[] aiDataPoints = extractDataPoints(index);

            // pre-determine the tree-area width based on max point of overalpping items
            int iMaxOverlap = findMaxOverlap(aiDataPoints, index);

            //########################################################

            int iWidth = SECTOR_SECTION_SIZE
                    + iMaxOverlap * TEXT_LINE_HEIGHT
                    + iMaxOverlap * BOX_AREA_WIDTH;
            int iHeight = cd.getLength()+1;

            final double SCALE;
            if (iHeight < MAX_PDF_SIZE)
                SCALE = 1;
            else
                SCALE = MAX_PDF_SIZE / iHeight;

            FileOutputStream pdfStream = new FileOutputStream(_sOutfile);

            com.pdfjet.PDF pdf = new com.pdfjet.PDF(pdfStream);
            com.pdfjet.Font pdfFont = new com.pdfjet.Font(pdf, "Helvetica");
            pdfFont.setSize(6*SCALE);

            com.pdfjet.Page pdfPage = new com.pdfjet.Page(pdf,
                new double[] {
                    iWidth * SCALE,
                    iHeight * SCALE
                }
            );

            for (int iSector = 0; iSector < cd.getLength(); iSector++) {
                try {
                    IdentifiedSector sector = IdentifiedSector.identifySector(cd.getSector(iSector));

                    Color c;
                    if (sector == null) {
                        c = classToColor(UnidentifiedSector.class);
                    } else {
                        c = classToColor(sector.getClass());
                    }
                    com.pdfjet.Box pdfBox = new com.pdfjet.Box(0*SCALE, iSector*SCALE, SECTOR_SECTION_SIZE*SCALE, 1*SCALE);
                    int[] aiRgb = { c.getRed(), c.getGreen(), c.getBlue() };
                    pdfBox.setFillShape(true);
                    pdfBox.setLineWidth(0);
                    pdfBox.setColor(aiRgb);
                    pdfBox.drawOn(pdfPage);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            DiscItem[] aoRunningItems = new DiscItem[iMaxOverlap];

            /*
             * at each datapoint, there are basically 3 different things that can happen
             * 1) 1 item begins
             * 2) 2 or more items begin
             * and
             * 3) one or more items end
             *
             * Also, disc items can begin and end at the same sector
             *
             */
            for (int iDataPoint : aiDataPoints) {

                // open
                for (DiscItem item : index) {
                    if (item.getStartSector() == iDataPoint) {
                        int i = findFree(aoRunningItems);
                        aoRunningItems[i] = item;

                        double x = (SECTOR_SECTION_SIZE + i * BOX_AREA_WIDTH + BOX_MARGIN_LEFT)*SCALE,
                               y = item.getStartSector()*SCALE,
                               w = BOX_WIDTH*SCALE,
                               h = item.getSectorLength()*SCALE;

                        // draw box
                        com.pdfjet.Box pdfBox = new com.pdfjet.Box(x, y, w, h);
                        Color c = classToColor(item.getClass());
                        int[] aiRgb = { c.getRed(), c.getGreen(), c.getBlue() };

                        pdfBox.setColor(aiRgb);
                        pdfBox.setFillShape(true);
                        pdfBox.setLineWidth(0);
                        pdfBox.drawOn(pdfPage);

                        pdfBox.setFillShape(false);
                        pdfBox.setColor(com.pdfjet.RGB.WHITE);
                        pdfBox.setLineWidth(0.3*SCALE);
                        pdfBox.drawOn(pdfPage);

                        com.pdfjet.TextLine pdfText = new com.pdfjet.TextLine(pdfFont, item.toString());
                        pdfText.setPosition(x, y);
                        pdfText.setColor(com.pdfjet.RGB.DARK_GRAY);
                        pdfText.drawOn(pdfPage);
                    }
                }

                for (int i = 0; i < aoRunningItems.length; i++) {
                    if (aoRunningItems[i] != null) {
                        if (iDataPoint >= aoRunningItems[i].getEndSector())
                            aoRunningItems[i] = null;
                    }
                }

            }


            pdf.flush();
            pdfStream.close();

            return 0;

            } catch (Exception ex) {
                Feedback.printlnErr(ex);
                return -1;
            }
            

        }
    }
    
    private static final HashMap<Class, Color> colorLookup = new HashMap<Class, Color>();

    private static Color classToColor(Class c) {
        Color color = colorLookup.get(c.getClass());
        if (color == null) {
            int iClr = c.getName().hashCode();
            color = new Color(iClr);
            colorLookup.put(c, color);
        }
        return color;
    }

    private static int findFree(Object[] ao) {
        for (int i = 0; i < ao.length; i++) {
            if (ao[i] == null)
                return i;
        }
        return -1;
    }

    private static int[] extractDataPoints(DiscIndex index) {
        TreeSet<Integer> dataPoints = new TreeSet<Integer>();
        for (DiscItem item : index) {
            dataPoints.add(item.getStartSector());
            dataPoints.add(item.getEndSector());
        }

        int[] aiDataPoints = new int[dataPoints.size()];
        int i = 0;
        for (Integer point : dataPoints) {
            aiDataPoints[i] = point.intValue();
            i++;
        }
        return aiDataPoints;
    }

    private static int findMaxOverlap(int[] aiDataPoints, DiscIndex index) {
        int iMaxOverlap = 0;
        for (int iSector : aiDataPoints) {
            int iSectorOverlap = 0;
            for (DiscItem item : index) {
                if (iSector >= item.getStartSector() && iSector <= item.getEndSector())
                    iSectorOverlap++;
            }
            if (iSectorOverlap > iMaxOverlap)
                iMaxOverlap = iSectorOverlap;
        }
        return iMaxOverlap;
    }


}
