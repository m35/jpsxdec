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


import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import jpsxdec.plugins.IdentifiedSectorRangeIterator;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.cdreaders.CDSector;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import jpsxdec.cdreaders.CDSectorIterator;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.player.PlayController;
import jpsxdec.plugins.ConsoleProgressListener;
import jpsxdec.plugins.DiscIndex;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.DiscItemSaver;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.psx.str.DiscItemSTRVideo;
import jpsxdec.plugins.psx.str.IVideoSector;
import jpsxdec.plugins.xa.IDiscItemAudioStream;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** Main entry point to the jPSXdec program. */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static FeedbackStream Outputter;

    public final static String Version = "0.90.0 (alpha)";
    public final static String VerString = "jPSXdec: PSX media decoder, v" + Version;
    private static MainCommandLineParser _mainSettings;

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
        
        Outputter = new FeedbackStream(System.err, _mainSettings.getVerbose());
                
        Outputter.printlnNorm(VerString);

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
        }

        // display details of logging configuration right before program exits
        //System.out.println(au.com.forward.logging.Logging.currentConfiguration());

        System.exit(iExitCode);
    }


    //--------------------------------------------------------------------------

    private static int itemHelp() {
        CDSectorReader cdReader;
        try {
            cdReader = CDSectorReader.open(_mainSettings.getInputFile());
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error opening CDSectorReader", ex);
            Outputter.printlnErr(ex.getMessage());
            return -1;
        }

        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        if (!discIndex.hasIndex(_mainSettings.getDecodeIndex())) {
            Outputter.printlnErr("Can't find index " + _mainSettings.getDecodeIndex());
            return -1;
        }

        DiscItem item = discIndex.getByIndex(_mainSettings.getDecodeIndex());

        Outputter.nl();

        Outputter.printlnNorm("Help for");
        Outputter.printlnNorm(item.toString());

        DiscItemSaver saver = item.getSaver();
        saver.printHelp(Outputter);

        return 0;
    }

    // =========================================================================

    private static CDSectorReader openCD(boolean blnCheckHasHeaders) {
        //open input file
        CDSectorReader cdReader;
        try {
            Outputter.printlnNorm("Opening " + _mainSettings.getInputFile());
            cdReader = CDSectorReader.open(_mainSettings.getInputFile());
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error opening input file", ex);
            Outputter.printlnErr(ex.getMessage());
            return null;
        }

        Outputter.printlnNorm("Identified as " + cdReader.getTypeDescription());

        if (blnCheckHasHeaders) {
            if (!cdReader.hasSectorHeader()) {
                Outputter.printlnWarn("Warning: Input file does not contain entire raw CD sectors.");
                Outputter.printlnWarn("         Audio cannot be decoded.");
            }
        }

        return cdReader;
    }

    private static DiscIndex doTheIndex(CDSectorReader cdReader, boolean blnIndexOnly) {
        // index the STR file
        DiscIndex discIndex = new DiscIndex(cdReader);
        boolean blnSaveIndexFile = false;
        String sIndexFile = _mainSettings.getIndexFile();
        if (sIndexFile  != null) {
            if (blnIndexOnly || !new File(_mainSettings.getIndexFile()).exists()) {
                Outputter.printlnNorm("Building index");
                discIndex.indexDisc(new ConsoleProgressListener(Outputter));
                blnSaveIndexFile = true;
            } else {
                Outputter.printlnNorm("Reading index file " + sIndexFile);
                try {
                    discIndex.deserializeIndex(sIndexFile);
                } catch (NotThisTypeException ex) {
                    log.log(Level.SEVERE, "Reading index error", ex);
                    Outputter.printlnErr("Invalid index file");
                    return null;
                } catch (IOException ex) {
                    log.log(Level.SEVERE, "IO error", ex);
                    Outputter.printlnErr(ex.getMessage());
                    return null;
                }
            }
        } else {
            Outputter.printlnNorm("Building index");
            discIndex.indexDisc(new ConsoleProgressListener(Outputter));
        }

        // save index file if necessary
        if (blnSaveIndexFile) {

            PrintStream printer = null;
            try {
                if (_mainSettings.getIndexFile().equals("-")) {
                    printer = System.out;
                    Outputter.printlnNorm("Writing index to stdout.");
                } else {
                    Outputter.printlnNorm("Saving index as " + _mainSettings.getIndexFile());
                    printer = new PrintStream(_mainSettings.getIndexFile());
                }
                discIndex.serializeIndex(printer);
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Error writing index file", ex);
                Outputter.printlnErr(ex.getMessage());
                return null;
            } finally {
                if (printer != null && printer != System.out)
                    printer.close();
            }
        }

        // print the index
        for (DiscItem item : discIndex) {
            Outputter.printlnMore(item.toString());
        }

        return discIndex;
    }

    // =========================================================================

    private static int copySectors() {
        //open input file
        CDSectorReader oCD;
        try {
            oCD = CDSectorReader.open(_mainSettings.getInputFile());

            int[] aiSectors = _mainSettings.getSectorsToCopy();
            
            CDSectorIterator cdIter = new CDSectorIterator(oCD, aiSectors[0], aiSectors[1]);
            
            Outputter.printlnErr("Copying sectors " + aiSectors[0] + " to " + aiSectors[1]);
            
            FileOutputStream fos = new FileOutputStream(
                    _mainSettings.constructOutPath(_mainSettings.getInputFileBase()
                    + aiSectors[0] + "-" + aiSectors[1] + ".dat"));
            
            while (cdIter.hasNext()) {
                CDSector sector = cdIter.next();
                if (sector == null) {
                    Outputter.printlnErr("Error reading sector!");
                    return -1;
                    // TODO: err
                } else {
                    fos.write(sector.getRawSectorData());
                }
            }
            
            fos.close();
            
            return 0;
            
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error copying sectors.", ex);
            Outputter.printlnErr(ex.getMessage());
            return -1;
        }
    }
    
    //--------------------------------------------------------------------------
    
    private static int indexOnly() {
        //open input file
        CDSectorReader cdReader = openCD(false);
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
        CDSectorReader cdReader = openCD(true);
        if (cdReader == null)
            return -1;
        
        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        Outputter.nl();
        
        // decode/extract the desired disc item
        try {
            
            int iIndex = _mainSettings.getDecodeIndex();
            if (discIndex.hasIndex(iIndex)) {
                DiscItem item = discIndex.getByIndex(iIndex);
                decodeDiscItem(item);
                Outputter.printlnNorm("Disc decoding/extracting complete.");
            } else {
                Outputter.printlnWarn("Sorry, couldn't find disc item " + iIndex);
            }

        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO error", ex);
            Outputter.printlnErr(ex.getMessage());
            return -1;
        }
            
        return 0;
    }

    // .........................................................................

    private static int decodeAllType() {
        //open input file
        CDSectorReader cdReader = openCD(true);
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
                    Outputter.printlnNorm("Item complete.");
                    Outputter.nl();
                }
            }

            if (!blnFound) {
                Outputter.printlnNorm("Sorry, couldn't find any disc items of type " +
                        _mainSettings.getDecodeAllType());
            }

            Outputter.printlnNorm("Disc decoding/extracting complete.");
        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO error", ex);
            Outputter.printlnErr(ex.getMessage());
            return -1;
        }

        return 0;
    }
    
    
    private static void decodeDiscItem(DiscItem item) throws IOException {

        DiscItemSaver saver = item.getSaver();

        Outputter.printlnNorm("Saving " + item.toString());

        saver.commandLineOptions(_mainSettings.getRemainingArgs(), Outputter);
        
        saver.startSave(new ConsoleProgressListener(Outputter));
    }
    
    //--------------------------------------------------------------------------

    private static int decodeStaticFile() {
        
        try {
            
            Outputter.printlnNorm("Reading static file");
            
            final FileInputStream is;
            is = new FileInputStream(_mainSettings.getInputFile());
            
            InputStream isfp = new IO.InputStreamWithFP(is);

            JPSXPlugin.identifyStatic(isfp);

        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO error", ex);
            Outputter.printlnErr(ex.getMessage());
            return -1;
        }
        
        return 0;
    }

    //--------------------------------------------------------------------------
   
    private static int sectorDump() {
        CDSectorReader cdReader = openCD(false);
        if (cdReader == null)
            return -1;
        
        Outputter.printlnNorm("Generating sector list");
        
        PrintStream ps;
        try {
            if (_mainSettings.getSectorDumpFile().equals("-"))
                ps = System.out;
            else
                ps = new PrintStream(_mainSettings.getSectorDumpFile());
            
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
            Outputter.printlnErr(ex.getMessage());
            return -1;
        }
    }

    //--------------------------------------------------------------------------

    private static int fpsDump() {
        CDSectorReader cdReader = openCD(false);
        if (cdReader == null)
            return -1;

        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        try {

            int iIndex = _mainSettings.getFpsDumpItem();
            if (discIndex.hasIndex(iIndex)) {
                
                PrintStream ps = new PrintStream("fps.txt");

                DiscItem item = discIndex.getByIndex(iIndex);
                if (item instanceof DiscItemSTRVideo) {
                    DiscItemSTRVideo vid = (DiscItemSTRVideo)item;
                    final int LENGTH = vid.getSectorLength();
                    for (int iSector = 0; iSector < LENGTH; iSector++) {
                        CDSector sector = vid.getSector(iSector);
                        IdentifiedSector isect = JPSXPlugin.identifyPluginSector(sector);
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
                                    "%5d X",
                                    iSector));
                        }

                    }
                }
                ps.close();
            } else {
                Outputter.printlnWarn("Sorry, couldn't find disc item " + iIndex);
                return -1;
            }

        } catch (IOException ex) {
            ex.printStackTrace(); // TODO: fixme
            return -1;
        }

        return 0;
    }

    //--------------------------------------------------------------------------

    private static int player() {
        CDSectorReader cdReader = openCD(true);
        if (cdReader == null)
            return -1;

        DiscIndex discIndex = doTheIndex(cdReader, false);
        if (discIndex == null)
            return -1;

        DiscItem item = discIndex.getByIndex(_mainSettings.getPlayIndex());
        if (item == null) {
            Outputter.printlnErr("Item " + _mainSettings.getPlayIndex() + " not found.");
            return -1;
        }

        try {

            final PlayController controller;

            if (item instanceof DiscItemSTRVideo) {
                Outputter.printlnNorm("Creating player for");
                Outputter.printlnNorm(item.toString());

                DiscItemSTRVideo video = (DiscItemSTRVideo) item;
                if (video.hasAudio()) {
                    IDiscItemAudioStream audio = video.getParallelAudioStreams()[0];
                    int iStartSector = Math.min(video.getStartSector(), audio.getStartSector());
                    int iEndSector = Math.min(video.getEndSector(), audio.getEndSector());
                    controller = new PlayController(
                            new MediaPlayer.StrReader(video, iStartSector, iEndSector),
                            new MediaPlayer.XAAudioDecoder(audio),
                            new MediaPlayer.StrVideoDecoder(video));
                } else {
                    controller = new PlayController(
                            new MediaPlayer.StrReader(video),
                            null,
                            new MediaPlayer.StrVideoDecoder(video));
                }
            } else if (item instanceof IDiscItemAudioStream) {
                Outputter.printlnNorm("Creating player for");
                Outputter.printlnNorm(item.toString());

                IDiscItemAudioStream audio = (IDiscItemAudioStream) item;

                controller = new PlayController(
                        new MediaPlayer.StrReader(audio),
                        new MediaPlayer.XAAudioDecoder(audio),
                        null);
            } else {
                Outputter.printlnErr(item.toString());
                Outputter.printlnErr("is not audio or video. Cannot create player.");
                return -1;
            }

            final JFrame window = new JFrame(Main.VerString + " - Player");

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
            ex.printStackTrace();
            Outputter.printlnErr(ex.toString());
            return -1;
        }

        return 0;
    }

    
}
