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


import argparser.ArgParser;
import argparser.BooleanHolder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowEvent;
import jpsxdec.modules.IdentifiedSectorRangeIterator;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.cdreaders.CdSector;
import java.io.*;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.cdreaders.CdxaRiffHeader;
import jpsxdec.player.PlayController;
import jpsxdec.util.ConsoleProgressListener;
import jpsxdec.modules.DiscIndex;
import jpsxdec.modules.DiscItem;
import jpsxdec.modules.DiscItemSaver;
import jpsxdec.modules.JPSXModule;
import jpsxdec.modules.UnidentifiedSector;
import jpsxdec.modules.psx.str.DiscItemSTRVideo;
import jpsxdec.modules.psx.str.IVideoSector;
import jpsxdec.modules.psx.video.encode.ReplaceFrames;
import jpsxdec.modules.xa.DiscItemAudioStream;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** Main entry point to the jPSXdec program. */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static FeedbackStream Feedback;

    public final static String Version = "0.93.0 (alpha)";
    public final static String VerString = "jPSXdec: PSX media decoder, v" + Version;
    public final static String VerStringNonCommercial = "jPSXdec: PSX media decoder (non-commercial), v" + Version;
    private static MainCommandLineParser _mainSettings;

    /** Main entry point to the jPSXdec program. */
    public static void main(String[] args) {
        try { // load the logger configuration
            InputStream is = Main.class.getResourceAsStream("LogToFile.properties");
            if (is != null)
                java.util.logging.LogManager.getLogManager().readConfiguration(is);
        } catch (IOException ex) {
            log.log(Level.WARNING, null, ex);
        }

        _mainSettings = new MainCommandLineParser(args);

        if (_mainSettings.getMainCommand() == MainCommandLineParser.MAIN_CMD_STARTGUI) {
            /* disableded
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    new Gui().setVisible(true);
                }
            });
            */
            return;
        }
        
        Feedback = new FeedbackStream(System.out, _mainSettings.getVerbose());
                
        Feedback.println(VerStringNonCommercial);

        int iExitCode = 0;

        switch (_mainSettings.getMainCommand()) {
            case MainCommandLineParser.MAIN_CMD_INDEX:
                iExitCode = indexOnly();
                break;
            case MainCommandLineParser.MAIN_CMD_DECODE:
                iExitCode = normalDecode();
                break;
            case MainCommandLineParser.MAIN_CMD_DECODE_ALL_TYPE:
                iExitCode = decodeAllType();
                break;
            case MainCommandLineParser.MAIN_CMD_STATICFILE:
                iExitCode = decodeStaticFile();
                break;
            case MainCommandLineParser.MAIN_CMD_SECTORLIST:
                iExitCode = sectorDump();
                break;
            case MainCommandLineParser.MAIN_CMD_COPYSECT:
                iExitCode = copySectors();
                break;
            case MainCommandLineParser.MAIN_CMD_FPS_DUMP:
                iExitCode = fpsDump();
                break;
            case MainCommandLineParser.MAIN_CMD_ITEM_HELP:
                iExitCode = itemHelp();
                break;
            case MainCommandLineParser.MAIN_CMD_PLAY:
                iExitCode = player();
                break;
            case MainCommandLineParser.MAIN_CMD_ENCODE:
                iExitCode = frameReplacer();
                break;
            case MainCommandLineParser.MAIN_CMD_VISUALIZE:
                iExitCode = visualize();
        }

        // display details of logging configuration right before program exits
        //System.out.println(au.com.forward.logging.Logging.currentConfiguration());

        System.exit(iExitCode);
    }


    //--------------------------------------------------------------------------

    private static int itemHelp() {
        CDFileSectorReader cdReader;
        try {
            cdReader = new CDFileSectorReader(_mainSettings.getInputFile());
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error opening CDFileSectorReader", ex);
            Feedback.printlnErr(ex);
            return -1;
        }

        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        if (!discIndex.hasIndex(_mainSettings.getDiscItemIndex())) {
            Feedback.printlnErr("Can't find index " + _mainSettings.getDiscItemIndex());
            return -1;
        }

        DiscItem item = discIndex.getByIndex(_mainSettings.getDiscItemIndex());

        Feedback.println();

        Feedback.println("Help for");
        Feedback.println(item.toString());

        DiscItemSaver saver = item.getSaver();
        saver.printHelp(Feedback);

        return 0;
    }

    // =========================================================================

    private static CDFileSectorReader openCD(boolean blnCheckHasHeaders) {
        return openCD(blnCheckHasHeaders, false);
    }
    private static CDFileSectorReader openCD(boolean blnCheckHasHeaders, boolean blnWritable) {
        //open input file
        CDFileSectorReader cdReader;
        try {
            Feedback.println("Opening " + _mainSettings.getInputFile());
            if (blnWritable)
                cdReader = new CDFileSectorReader(_mainSettings.getInputFile(), true);
            else
                cdReader = new CDFileSectorReader(_mainSettings.getInputFile());
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error opening input file", ex);
            Feedback.printlnErr(ex);
            return null;
        }

        Feedback.println("Identified as " + cdReader.getTypeDescription());

        if (blnCheckHasHeaders) {
            if (!cdReader.hasSectorHeader()) {
                Feedback.printlnWarn("Warning: Input file does not contain entire raw CD sectors.");
                Feedback.printlnWarn("         Audio cannot be decoded.");
            }
        }

        return cdReader;
    }

    private static DiscIndex doTheIndex(CDFileSectorReader cdReader, boolean blnIndexOnly) {
        // index the STR file
        DiscIndex discIndex;
        boolean blnSaveIndexFile = false;
        String sIndexFile = _mainSettings.getIndexFile();
        if (sIndexFile  != null) {
            if (blnIndexOnly || !new File(_mainSettings.getIndexFile()).exists()) {
                Feedback.println("Building index");
                discIndex = new DiscIndex(cdReader, new ConsoleProgressListener(Feedback));
                Feedback.println(discIndex.size() + " items found.");
                blnSaveIndexFile = true;
            } else {
                Feedback.println("Reading index file " + sIndexFile);
                try {
                    discIndex = new DiscIndex(sIndexFile, cdReader, Feedback);
                    Feedback.println(discIndex.size() + " items loaded.");
                } catch (NotThisTypeException ex) {
                    log.log(Level.SEVERE, "Reading index error", ex);
                    Feedback.printlnErr("Invalid index file");
                    return null;
                } catch (IOException ex) {
                    log.log(Level.SEVERE, "IO error", ex);
                    Feedback.printlnErr(ex);
                    return null;
                }
            }
        } else {
            Feedback.println("Building index");
            discIndex = new DiscIndex(cdReader, new ConsoleProgressListener(Feedback));
            Feedback.println(discIndex.size() + " items found.");
        }

        // save index file if necessary
        if (blnSaveIndexFile) {

            PrintStream printer = null;
            try {
                if (_mainSettings.getIndexFile().equals("-")) {
                    printer = System.out;
                    Feedback.println("Writing index to stdout.");
                } else {
                    Feedback.println("Saving index as " + _mainSettings.getIndexFile());
                    printer = new PrintStream(_mainSettings.getIndexFile());
                }
                discIndex.serializeIndex(printer);
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Error writing index file", ex);
                Feedback.printlnErr(ex);
                return null;
            } finally {
                if (printer != null && printer != System.out)
                    printer.close();
            }
        }

        // print the index
        if (false) {
            for (int i=0; i < discIndex.getTopLevelItemCount(); i++) {
                printItem("", discIndex.getTopLevelItem(i));
            }
        }

        return discIndex;
    }

    private static void printItem(String sSpaces, DiscItem item) {
        Feedback.println(sSpaces + item);
        Feedback.println(sSpaces + item.getSuggestedBaseName());
        for (int i = 0; i < item.getChildCount(); i++) {
            printItem(sSpaces + "  ", item.getChild(i));
        }
    }

    // =========================================================================

    private static int copySectors() {
        //open input file
        CDFileSectorReader cdReader;
        try {
            cdReader = new CDFileSectorReader(_mainSettings.getInputFile());

            int[] aiSectors = _mainSettings.getSectorsToCopy();

            Feedback.printlnErr("Copying sectors " + aiSectors[0] + " to " + aiSectors[1]);

            FileOutputStream fos = new FileOutputStream(
                    _mainSettings.constructOutPath(_mainSettings.getInputFileBase()
                    + aiSectors[0] + "-" + aiSectors[1] + ".dat"));

            if (cdReader.getSectorSize() == CDFileSectorReader.SECTOR_SIZE_2352_BIN && _mainSettings.getRemainingArgs() != null) {
                ArgParser parser = new ArgParser("", false);
                BooleanHolder noCdxaHeader = new BooleanHolder(false);
                parser.addOption("-nocdxa %v", noCdxaHeader);
                parser.matchAllArgs(_mainSettings.getRemainingArgs(), 0, 0);
                if (!noCdxaHeader.value) {
                    long lngFileSize = (aiSectors[1] - aiSectors[0] + 1) * (long)2352;
                    CdxaRiffHeader.write(fos, lngFileSize);
                }
            }

            for (int i = aiSectors[0]; i <= aiSectors[1]; i++) {
                CdSector sector = cdReader.getSector(i);
                if (sector == null) {
                    Feedback.printlnErr("Error reading sector " + i);
                    return -1;
                    // TODO: err
                } else {
                    fos.write(sector.getRawSectorDataCopy());
                }
            }

            fos.close();

            return 0;

        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error copying sectors.", ex);
            Feedback.printlnErr(ex);
            return -1;
        }
    }
    
    //--------------------------------------------------------------------------
    
    private static int indexOnly() {
        //open input file
        CDFileSectorReader cdReader = openCD(false);
        if (cdReader == null)
            return -1;
        
        if (doTheIndex(cdReader, true) == null)
            return -1;
        else
            return 0;
    }

    //--------------------------------------------------------------------------
    
    private static int normalDecode() {
        //open input file
        CDFileSectorReader cdReader = openCD(true);
        if (cdReader == null)
            return -1;
        
        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        Feedback.println();
        
        // decode/extract the desired disc item
        try {
            
            int iIndex = _mainSettings.getDiscItemIndex();
            if (discIndex.hasIndex(iIndex)) {
                DiscItem item = discIndex.getByIndex(iIndex);
                decodeDiscItem(item);
                Feedback.println("Disc decoding/extracting complete.");
            } else {
                Feedback.printlnWarn("Sorry, couldn't find disc item " + iIndex);
            }

        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO error", ex);
            Feedback.printlnErr(ex);
            return -1;
        }
            
        return 0;
    }

    // .........................................................................

    private static int decodeAllType() {
        //open input file
        CDFileSectorReader cdReader = openCD(true);
        if (cdReader == null)
            return -1;

        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        // extract/decode all desired disc items
        try {

            boolean blnFound = false;

            for (DiscItem item : discIndex) {
                if (item.getTypeId().equalsIgnoreCase(_mainSettings.getDecodeAllType())) {
                    blnFound = true;
                    decodeDiscItem(item);
                    Feedback.println("Item complete.");
                    Feedback.println();
                }
            }

            if (!blnFound) {
                Feedback.println("Sorry, couldn't find any disc items of type " +
                        _mainSettings.getDecodeAllType());
            }

            Feedback.println("Disc decoding/extracting complete.");
        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO error", ex);
            Feedback.printlnErr(ex);
            return -1;
        }

        return 0;
    }
    
    
    private static void decodeDiscItem(DiscItem item) throws IOException {

        DiscItemSaver saver = item.getSaver();

        Feedback.println("Saving " + item.toString());

        saver.commandLineOptions(_mainSettings.getRemainingArgs(), Feedback);
        
        long lngStart, lngEnd;
        lngStart = System.currentTimeMillis();
        saver.startSave(new ConsoleProgressListener(Feedback));
        lngEnd = System.currentTimeMillis();
        Feedback.format("Time: %1.2f sec", (lngEnd - lngStart) / 1000.0);
        Feedback.println();
    }
    
    //--------------------------------------------------------------------------

    private static int decodeStaticFile() {
        
        try {
            
            Feedback.println("Reading static file");
            
            final FileInputStream is;
            is = new FileInputStream(_mainSettings.getInputFile());
            
            InputStream isfp = new IO.InputStreamWithFP(is);

            JPSXModule.identifyStatic(isfp);

        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO error", ex);
            Feedback.printlnErr(ex);
            return -1;
        }
        
        return 0;
    }

    //--------------------------------------------------------------------------
   
    private static int sectorDump() {
        CDFileSectorReader cdReader = openCD(false);
        if (cdReader == null)
            return -1;
        
        Feedback.println("Generating sector list");
        
        PrintStream ps;
        try {
            if (_mainSettings.getOutFile().equals("-"))
                ps = System.out;
            else
                ps = new PrintStream(_mainSettings.getOutFile());
            
            IdentifiedSectorRangeIterator oIter = new IdentifiedSectorRangeIterator(cdReader);
            while (oIter.hasNext()) {
                IdentifiedSector oSect = oIter.next();
                if (oSect != null) {
                    String s = oSect.toString();
                    if (s != null) ps.println(s);
                } else {
                    // probably unknown sector
                }
            }
            
            ps.flush();
            if (!ps.equals(System.out)) ps.close();
            return 0;
            
        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO error", ex);
            Feedback.printlnErr(ex);
            return -1;
        }
    }

    //--------------------------------------------------------------------------

    private static int fpsDump() {
        CDFileSectorReader cdReader = openCD(false);
        if (cdReader == null)
            return -1;

        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        try {
            Feedback.println("Generating fps dump.");

            int iIndex = _mainSettings.getFpsDumpItem();
            if (!discIndex.hasIndex(iIndex)) {
                Feedback.printlnWarn("Sorry, couldn't find disc item " + iIndex);
                return -1;
            } else {
                
                DiscItem item = discIndex.getByIndex(iIndex);

                if (!(item instanceof DiscItemSTRVideo)) {
                    Feedback.printlnErr("Disc item isn't a video.");
                } else {
                    PrintStream ps = new PrintStream("fps.txt");

                    DiscItemSTRVideo vid = (DiscItemSTRVideo)item;
                    final int LENGTH = vid.getSectorLength();
                    for (int iSector = 0; iSector < LENGTH; iSector++) {
                        CdSector sector = vid.getRelativeSector(iSector);
                        IdentifiedSector isect = JPSXModule.identifyModuleSector(sector);
                        if (isect instanceof IVideoSector) {
                            IVideoSector vidSect = (IVideoSector) isect;
                            ps.println(String.format(
                                    "%-5d %-4d %d/%d",
                                    iSector,
                                    vidSect.getFrameNumber(),
                                    vidSect.getChunkNumber(),
                                    vidSect.getChunksInFrame()
                                    ));
                        } else {
                            ps.println(String.format(
                                    "%-5d X",
                                    iSector));
                        }

                    }
                    ps.close();
                }
            }

        } catch (IOException ex) {
            Feedback.printlnErr(ex);
            return -1;
        }

        return 0;
    }

    //--------------------------------------------------------------------------

    private static int player() {
        CDFileSectorReader cdReader = openCD(true);
        if (cdReader == null)
            return -1;

        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        DiscItem item = discIndex.getByIndex(_mainSettings.getDiscItemIndex());
        if (item == null) {
            Feedback.printlnErr("Item " + _mainSettings.getDiscItemIndex() + " not found.");
            return -1;
        }

        try {

            final PlayController controller;

            if (item instanceof DiscItemSTRVideo) {
                Feedback.println("Creating player for");
                Feedback.println(item.toString());

                DiscItemSTRVideo video = (DiscItemSTRVideo) item;
                if (video.hasAudio()) {
                    DiscItemAudioStream audio = video.getParallelAudioStream(0);
                    int iStartSector = Math.min(video.getStartSector(), audio.getStartSector());
                    int iEndSector = Math.max(video.getEndSector(), audio.getEndSector());
                    controller = new PlayController(new MediaPlayer(video, audio.makeDecoder(true, 1.0), iStartSector, iEndSector));
                } else {
                    controller = new PlayController(new MediaPlayer(video));
                }
            } else if (item instanceof DiscItemAudioStream) {
                Feedback.println("Creating player for");
                Feedback.println(item.toString());

                DiscItemAudioStream audio = (DiscItemAudioStream) item;

                controller = new PlayController(new MediaPlayer(audio));
            } else {
                Feedback.printlnErr(item.toString());
                Feedback.printlnErr("is not audio or video. Cannot create player.");
                return -1;
            }

            final JFrame window = new JFrame(Main.VerStringNonCommercial + " - Player");

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
                    controller.play();
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

        } catch (Throwable ex) {
            Feedback.printlnErr(ex);
            return -1;
        }

        return 0;
    }

    //--------------------------------------------------------------------------

    private static int frameReplacer() {
        CDFileSectorReader cdReader = openCD(true, true);
        if (cdReader == null)
            return -1;

        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        DiscItem item = discIndex.getByIndex(_mainSettings.getDiscItemIndex());
        if (item == null) {
            Feedback.printlnErr("Item " + _mainSettings.getDiscItemIndex() + " not found.");
            return -1;
        } else if (!(item instanceof DiscItemSTRVideo)) {
            Feedback.printlnErr("Item " + _mainSettings.getDiscItemIndex() + " is not video.");
            return -1;
        }

        String[] asArgs = _mainSettings.getRemainingArgs();
        if (asArgs == null) {
            Feedback.printlnErr("Need to specify frame replacer xml.");
            return -1;
        }

        try {
            ReplaceFrames replacers = new ReplaceFrames(asArgs[0]);
            replacers.replaceFrames((DiscItemSTRVideo) item, cdReader, Feedback);
            cdReader.close();
        } catch (Throwable ex) {
            Feedback.printlnErr(ex);
        }

        return 0;
    }


    private static int visualize() { try {

        CDFileSectorReader cd = openCD(true);
        if (cd == null)
            return -1;

        DiscIndex index = doTheIndex(cd, false);
        if (index == null)
            return -1;

        final int SECTOR_SECTION_SIZE = 32;
        final int TEXT_LINE_HEIGHT = 16;
        final int BOX_AREA_HEIGHT = 16;
        final int BOX_MARGIN_TOP = 2;
        final int BOX_MARGIN_BOTTOM = 2;
        final int BOX_HEIGHT = BOX_AREA_HEIGHT - (BOX_MARGIN_BOTTOM + BOX_MARGIN_TOP);
        final double SCALE = (200.0*72.0 - 18.0) / cd.size();

        /* priority:
         * ISO file
         * video
         * audio
         * tim
         *
         * summarize to just the important data-points
         */

        Feedback.println("Generating visualization.");

        int[] aiDataPoints = extractDataPoints(index);

        // pre-determine the tree-area width based on max point of overalpping items
        int iMaxOverlap = findMaxOverlap(aiDataPoints, index);

        //########################################################

        int iWidth = cd.size();
        int iHeight = SECTOR_SECTION_SIZE
                + iMaxOverlap * TEXT_LINE_HEIGHT
                + iMaxOverlap * BOX_AREA_HEIGHT;


        FileOutputStream pdfStream = new FileOutputStream(_mainSettings.getOutFile());

        /*
        com.pdfjet.PDF pdf = new com.pdfjet.PDF(pdfStream);
        com.pdfjet.Font pdfFont = new com.pdfjet.Font(pdf, "Helvetica");
        pdfFont.setSize(6*SCALE);

        com.pdfjet.Page pdfPage = new com.pdfjet.Page(pdf,
            new double[] {
                iWidth * SCALE,
                iHeight * SCALE
            }
        );

        for (int iSector = 0; iSector < cd.size(); iSector++) {
            try {
                IdentifiedSector sector = JPSXModule.identifyModuleSector(cd.getSector(iSector));
                
                Color c;
                if (sector == null) {
                    c = classToColor(UnidentifiedSector.class);
                } else {
                    c = classToColor(sector.getClass());
                }
                com.pdfjet.Box pdfBox = new com.pdfjet.Box(iSector*SCALE, 0*SCALE, 1*SCALE, SECTOR_SECTION_SIZE*SCALE);
                int[] aiRgb = { c.getRed(), c.getGreen(), c.getBlue() };
                pdfBox.setFillShape(true);
                pdfBox.setLineWidth(0);
                pdfBox.setColor(aiRgb);
                pdfBox.drawOn(pdfPage);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        */

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
        /*
        for (int iDataPoint : aiDataPoints) {

            // open
            for (DiscItem item : index) {
                if (item.getStartSector() == iDataPoint) {
                    int i = findFree(aoRunningItems);
                    aoRunningItems[i] = item;

                    double x = item.getStartSector()*SCALE,
                           y = (SECTOR_SECTION_SIZE + i * BOX_AREA_HEIGHT + BOX_MARGIN_TOP)*SCALE,
                           w = item.getSectorLength()*SCALE,
                           h = BOX_HEIGHT*SCALE;

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
                    pdfText.setPosition(x, y + (BOX_HEIGHT * 0.6) * SCALE);
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

         *
         */
        return 0;
        
        } catch (Exception ex) {
            Feedback.printlnErr(ex);
            return -1;
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
        // TODO: optimize this
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
