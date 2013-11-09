/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013  Michael Sabin
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

package jpsxdec.cmdline;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.StringHolder;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import jpsxdec.Version;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemStrVideoStream;
import jpsxdec.discitems.DiscItemTim;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.DiscItemXaAudioStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.ConsoleProgressListenerLogger;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.TaskCanceledException;
import jpsxdec.util.player.PlayController;


class Command_Items {
    
    private static final Logger LOG = Logger.getLogger(Command_Items.class.getName());

    public static class Command_Item extends Command {

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
        public void execute(String[] asRemainingArgs) throws CommandLineException {
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

            ConsoleProgressListenerLogger cpll = new ConsoleProgressListenerLogger("save", _fbs);
            try {
                handleItem(item, asRemainingArgs, _fbs, cpll);
            } finally {
                cpll.close();
            }
        }
    }

    private static void player(final PlayController controller, DiscItem item, final FeedbackStream fbs)
            throws InterruptedException
    {

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
                    controller.start();
                } catch (Throwable ex) {
                    fbs.printlnErr(ex);
                    synchronized( window ) {
                        window.notifyAll();
                    }
                }
            }
        });
        window.add(startBtn, BorderLayout.SOUTH);

        if (controller.hasVideo()) {
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

    public static class Command_All extends Command {

        private String _sType;

        public Command_All() {
            super("-a,-all");
        }
        protected String validate(String s) {
            _sType = s;
            return null;
        }
        public void execute(String[] asRemainingArgs) throws CommandLineException {
            DiscIndex discIndex = getIndex();

            boolean blnFound = false;
            ConsoleProgressListenerLogger cpll = new ConsoleProgressListenerLogger("save", _fbs);

            try {
                for (DiscItem item : discIndex) {
                    if (item.getType().name().equalsIgnoreCase(_sType)) {
                        blnFound = true;
                        handleItem(item, asRemainingArgs, _fbs, cpll);
                        _fbs.println("Item complete.");
                        _fbs.println();
                    }
                }
            } finally {
                cpll.close();
            }

            if (!blnFound) {
                _fbs.println("Sorry, couldn't find any disc items of type " + _sType);
            } else {
                _fbs.println("All index items complete.");
            }
        }
    }

    private static void handleItem(DiscItem item, String[] asRemainingArgs, FeedbackStream fbs,
                                   ConsoleProgressListenerLogger cpll)
            throws CommandLineException
    {
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
                    throw new CommandLineException("Disc item isn't a video.");
                } else {
                    fbs.println("Generating fps dump.");
                    PrintStream ps = new PrintStream("fps.txt");
                    try {
                        ((DiscItemStrVideoStream)item).fpsDump(ps);
                    } finally {
                        ps.close();
                    }
                }

            } else if (itemHelpArg.value) {
                fbs.println("Detailed help for");
                fbs.println(item);
                item.makeSaverBuilder().printHelp(fbs);
            } else if (playArg.value) {
                if (item instanceof DiscItemVideoStream) {
                    fbs.println("Creating player for");
                    fbs.println(item);
                    try {
                        player(((DiscItemVideoStream)item).makePlayController(), item, fbs);
                    } catch (InterruptedException ex) {
                        throw new CommandLineException("Error with player", ex);
                    }
                } else if (item instanceof DiscItemAudioStream) {
                    try {
                        player(((DiscItemAudioStream)item).makePlayController(), item, fbs);
                    } catch (InterruptedException ex) {
                        throw new CommandLineException("Error with player", ex);
                    }
                } else {
                    throw new CommandLineException(item, "is not audio or video. Cannot create player.");
                }
            } else if (frameInfoArg.value) {
                if (!(item instanceof DiscItemVideoStream)) {
                    throw new CommandLineException(item, "Disc item isn't a video.");
                } else {
                    ((DiscItemVideoStream)item).frameInfoDump(fbs);
                }
            } else if (replaceFrames.value != null) {
                if (!(item instanceof DiscItemVideoStream)) {
                    throw new CommandLineException(item, "Disc item isn't a video.");
                } else {
                    fbs.printlnWarn("Hope your disc image is backed up because this is irreversable.");
                    fbs.printlnWarn("Reopening disc image with write access.");
                    item.getSourceCd().reopenForWriting();
                    try {
                        ((DiscItemVideoStream)item).replaceFrames(fbs, replaceFrames.value);
                    } catch (MdecException ex) {
                        Logger.getLogger(Command_Items.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else if (replaceTim.value != null) {
                if (!(item instanceof DiscItemTim)) {
                    throw new CommandLineException("Disc item isn't a TIM image.");
                } else {
                    DiscItemTim timItem = (DiscItemTim)item;
                    if (timItem.getPaletteCount() != 1) {
                        throw new CommandLineException("Unable to replace a TIM image that has multiple palettes.");
                    }
                    BufferedImage bi = ImageIO.read(new File(replaceTim.value));
                    fbs.printlnWarn("Hope your disc image is backed up because this is irreversable.");
                    fbs.printlnWarn("Reopening disc image with write access.");
                    item.getSourceCd().reopenForWriting();
                    timItem.replace(fbs, bi);
                }
            } else if (replaceXa.value != null) {
                if (!(item instanceof DiscItemXaAudioStream)) {
                    throw new CommandLineException("Disc item isn't a XA stream.");
                } else {
                    DiscItemXaAudioStream xaItem = (DiscItemXaAudioStream)item;
                    fbs.printlnWarn("Hope your disc image is backed up because this is irreversable.");
                    fbs.println("Opening patch index " + replaceXa.value);
                    DiscIndex patchIndex;
                    try {
                        patchIndex = new DiscIndex(replaceXa.value, LOG);
                    } catch (IOException ex) {
                        throw new CommandLineException(ex);
                    }

                    DiscItemXaAudioStream patchXa;
                    try {
                        int iPatchXaIndex = Integer.parseInt(xaNum.value);
                        patchXa = (DiscItemXaAudioStream) patchIndex.getByIndex(iPatchXaIndex);
                    } catch (Throwable ex) {
                        throw new CommandLineException("Invalid or missing XA item number " + xaNum.value);
                    }
                    fbs.printlnWarn("Reopening disc image with write access.");
                    item.getSourceCd().reopenForWriting();
                    xaItem.replaceXa(fbs, patchXa);
                }
            } else {
                File dir;
                if (directory.value != null)
                    dir = new File(directory.value);
                else
                    dir = null;
                // decode/extract the desired disc item
                decodeDiscItem(item, dir, asRemainingArgs, fbs, cpll);
                fbs.println("Disc decoding/extracting complete.");
            }

        } catch (NotThisTypeException ex) {
            throw new CommandLineException(ex);
        } catch (IOException ex) {
            throw new CommandLineException(ex);
        }
    }

    private static void decodeDiscItem(DiscItem item, File dir, String[] asRemainingArgs, 
                                       FeedbackStream fbs, ConsoleProgressListenerLogger cpll)
            throws IOException
    {

        DiscItemSaverBuilder builder = item.makeSaverBuilder();

        fbs.println("Saving " + item.toString());

        builder.commandLineOptions(asRemainingArgs, fbs);

        fbs.println();

        IDiscItemSaver saver = builder.makeSaver(dir);

        saver.printSelectedOptions(fbs);

        long lngStart, lngEnd;
        lngStart = System.currentTimeMillis();
        try {
            cpll.setHeader(1, item.getSourceCd().toString());
            cpll.setHeader(2, item.toString());
            saver.startSave(cpll);
        } catch (TaskCanceledException ex) {
            LOG.severe("SHOULD NEVER HAPPEN");
        }
        lngEnd = System.currentTimeMillis();
        fbs.format("Time: %1.2f sec", (lngEnd - lngStart) / 1000.0);
        fbs.println();
    }

}
