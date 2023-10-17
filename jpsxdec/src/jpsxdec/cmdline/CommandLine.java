/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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

import argparser.StringHolder;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.Version;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.MiscResources;
import jpsxdec.util.ArgParser;

/** Entry point to handle command line options. */
public class CommandLine {

    private static final Logger LOG = Logger.getLogger(CommandLine.class.getName());

    /** Entry point to handle command line options. */
    public static int main(@Nonnull ArgParser ap) {

        FeedbackStream Feedback = new FeedbackStream(System.out, FeedbackStream.NORM);

        checkVerbosity(ap, Feedback);

        Feedback.println(I.JPSXDEC_VERSION_NON_COMMERCIAL(Version.Version));

        InFileAndIndexArgs discAndIndexArgs = new InFileAndIndexArgs(ap, Feedback);

        Command[] aoCommands = {
            new Command_CopySect(),
            new Command_SectorDump(),
            new Command_Static(),
            new Command_Visualize(),
            new Command_Items.Command_Item(),
            new Command_Items.Command_All(),
        };

        for (Command command : aoCommands) {
            command.init(ap, discAndIndexArgs, Feedback);
        }

        ap.match();

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
                if (ap.hasHelp()) {
                    printMainHelp(Feedback);
                } else {
                    if (!discAndIndexArgs.createAndSaveIndexIfProvided()) {
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
                    mainCommand.execute(ap);
                }
            }
        } catch (CommandLineException ex) {
            ILocalizedMessage msg = ex.getSourceMessage();
            // TODO: find way to not log unhandled exceptions twice in debug.log
            msg.logEnglish(LOG, Level.SEVERE, ex);
            Feedback.printlnErr(msg);
            return 1;
        } catch (Throwable ex) {
            Feedback.printlnErr(I.CMD_ERR_EX_CLASS(ex, ex.getClass().getSimpleName()));
            LOG.log(Level.SEVERE, "Unhandled exception", ex);
            return 1;
        }
        return 0;
    }

    // -------------------------------------------------------------

    private static void checkVerbosity(@Nonnull ArgParser ap,
                                       @Nonnull FeedbackStream fbs)
    {
        StringHolder verbose = ap.addStringOption("-v","-verbose");
        ap.match();

        if (verbose.value != null) {
            try {
                int iValue = Integer.parseInt(verbose.value);
                if (iValue >= FeedbackStream.NONE && iValue <= FeedbackStream.MORE)
                    fbs.setLevel(iValue);
                else
                    fbs.printlnWarn(I.CMD_INVALID_VALUE_FOR_CMD(verbose.value, "-v,-verbose"));
            } catch (NumberFormatException ex) {
                fbs.printlnWarn(I.CMD_INVALID_VALUE_FOR_CMD(verbose.value, "-v,-verbose"));
            }
        }
    }

    private static void printMainHelp(@Nonnull FeedbackStream fbs) {
        Iterator<ILocalizedMessage> helpLines = MiscResources.main_cmdline_help();
        while (helpLines.hasNext()) {
            fbs.println(helpLines.next());
        }
    }

}
