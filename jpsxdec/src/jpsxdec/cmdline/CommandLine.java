/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2016  Michael Sabin
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.Version;
import jpsxdec.cdreaders.CdFileNotFoundException;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.MiscResources;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.ConsoleProgressListenerLogger;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.TaskCanceledException;


public class CommandLine {
    
    private static final Logger LOG = Logger.getLogger(CommandLine.class.getName());

    public static int main(String[] asArgs) {

        FeedbackStream Feedback = new FeedbackStream(System.out, FeedbackStream.NORM);

        asArgs = checkVerbosity(asArgs, Feedback);

        Feedback.println(I.JPSXDEC_VERSION_NON_COMMERCIAL(Version.Version));

        ArgParser ap = new ArgParser("", false);

        StringHolder inputFileArg = new StringHolder();
        ap.addOption("-f,-file %s", inputFileArg);

        StringHolder indexFileArg = new StringHolder();
        ap.addOption("-x,-index %s", indexFileArg);

        Command[] aoCommands = {
            new Command_CopySect(),
            new Command_SectorDump(),
            new Command_Static(),
            new Command_Visualize(),
            new Command_Items.Command_Item(),
            new Command_Items.Command_All(),
        };

        for (Command command : aoCommands) {
            command.init(ap, inputFileArg, indexFileArg, Feedback);
        }

        asArgs = ap.matchAllArgs(asArgs, 0, 0);

        String sErr = ap.getErrorMessage();
        if (sErr != null) {
            Feedback.printlnErr(I.CMD_ARGPARSE_ERR(sErr));
            Feedback.printlnErr(I.CMD_TRY_HELP());
            return 1;
        }

        Command mainCommand = null;
        for (Command command : aoCommands) {
            if(command.found()) {
                if (mainCommand != null) {
                    Feedback.printlnErr(I.CMD_TOO_MANY_MAIN_COMMANDS());
                    Feedback.printlnErr(I.CMD_TRY_HELP());
                    return 1;
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
                        createAndSaveIndex(inputFileArg.value, indexFileArg.value, Feedback);
                    } else {
                        Feedback.printlnErr(I.CMD_NEED_MAIN_COMMAND());
                        Feedback.printlnErr(I.CMD_TRY_HELP());
                        return 1;
                    }
                }
            } else {
                ILocalizedMessage errMsg = mainCommand.validate();
                if (errMsg != null) {
                    Feedback.printlnErr(errMsg);
                    Feedback.printlnErr(I.CMD_TRY_HELP());
                    return 1;
                } else {
                    mainCommand.execute(asArgs);
                }
            }
        } catch (CommandLineException ex) {
            Feedback.printlnErr(ex.getLocalizedMessage());
            ILocalizedMessage msg = ex.getSourceMessage();
            if (msg == null) // TODO: find way to not log unhandled exceptions twice in debug.log
                LOG.log(Level.SEVERE, null, ex);
            else
                msg.log(LOG, Level.SEVERE, ex);
            return 1;
        } catch (Throwable ex) {
            Feedback.printlnErr(I.CMD_ERR_EX_CLASS(ex, ex.getClass().getSimpleName()));
            LOG.log(Level.SEVERE, "Unhandled exception", ex);
            return 1;
        }
        return 0;
    }

    // -------------------------------------------------------------
    
    private static @CheckForNull String[] checkVerbosity(@Nonnull String[] asArgs,
                                                         @Nonnull FeedbackStream fbs)
    {
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
                    fbs.printlnWarn(I.CMD_VERBOSE_LVL_INVALID_NUM(iValue));
            } catch (NumberFormatException ex) {
                fbs.printlnWarn(I.CMD_VERBOSE_LVL_INVALID_STR(verbose.value));
            }
        }

        return asArgs;
    }
    
    public static boolean checkForMainHelp(@CheckForNull String[] asArgs) {
        if (asArgs == null)
            return false;

        ArgParser ap = new ArgParser("", false);

        BooleanHolder help = new BooleanHolder();
        ap.addOption("-?,-h,-help %v", help);
        ap.matchAllArgs(asArgs, 0, 0);

        return help.value;
    }

    private static void printMainHelp(@Nonnull PrintStream ps) {
        BufferedReader br = new BufferedReader(MiscResources.main_cmdline_help());
        try {
            String sLine;
            while ((sLine = br.readLine()) != null) {
                ps.println(sLine);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    // -------------------------------------------------------------

    private static void createAndSaveIndex(@CheckForNull String sDiscFile,
                                           @Nonnull String sIndexFile,
                                           @Nonnull FeedbackStream Feedback)
            throws CommandLineException
    {
        CdFileSectorReader cd = loadDisc(sDiscFile, Feedback);
        try {
            DiscIndex index = buildIndex(cd, Feedback);
            saveIndex(index, sIndexFile, Feedback);
        } finally {
            try {
                cd.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Error closing CD", ex);
            }
        }
    }

    static @Nonnull CdFileSectorReader loadDisc(@CheckForNull String sDiscFile,
                                                @Nonnull FeedbackStream Feedback)
            throws CommandLineException
    {
        if (sDiscFile == null)
            throw new CommandLineException(I.CMD_COMMAND_NEEDS_DISC());
        Feedback.println(I.CMD_OPENING(sDiscFile));
        try {
            CdFileSectorReader cd = new CdFileSectorReader(new File(sDiscFile));
            Feedback.println(I.CMD_DISC_IDENTIFIED(cd.getTypeDescription()));
            return cd;
        } catch (CdFileNotFoundException ex) {
            throw new CommandLineException(I.CMD_FILE_NOT_FOUND_FILE(ex.getFile()), ex);
        } catch (IOException ex) {
            throw new CommandLineException(I.CMD_DISC_READ_ERROR(), ex);
        }
    }

    static DiscIndex buildIndex(@Nonnull CdFileSectorReader cd,
                                @Nonnull FeedbackStream Feedback)
    {
        Feedback.println(I.CMD_BUILDING_INDEX());
        DiscIndex index = null;
        ConsoleProgressListenerLogger cpll = new ConsoleProgressListenerLogger(I.INDEX_LOG_FILE_BASE_NAME().getLocalizedMessage(), Feedback);
        try {
            I.CMD_GUI_INDEXING(cd).log(cpll, Level.INFO);
            index = new DiscIndex(cd, cpll);
        } catch (TaskCanceledException ex) {
            throw new RuntimeException("Impossible TaskCanceledException during commandline indexing", ex);
        } finally {
            cpll.close();
        }
        Feedback.println(I.CMD_NUM_ITEMS_FOUND(index.size()));
        return index;
    }

    static void saveIndex(@Nonnull DiscIndex index, @Nonnull String sIndexFile,
                          @Nonnull FeedbackStream Feedback)
            throws CommandLineException
    {
        if (index.size() < 1) {
            Feedback.println(I.CMD_NOT_SAVING_EMPTY_INDEX());
        } else {
            Feedback.println(I.CMD_SAVING_INDEX(sIndexFile));
            try {
                index.serializeIndex(new File(sIndexFile));
            } catch (FileNotFoundException ex) {
                throw new CommandLineException(I.CMD_SAVE_OPEN_ERR(), ex);
            } catch (IOException ex) {
                throw new CommandLineException(I.WRITING_INDEX_FILE_ERR(), ex);
            }
        }
    }


}
