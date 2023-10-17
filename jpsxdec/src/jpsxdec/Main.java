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

package jpsxdec;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.cmdline.CommandLine;
import jpsxdec.gui.Gui;
import jpsxdec.util.ArgParser;
import jpsxdec.util.IO;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());


    /** @see LogManager */
    private static boolean isLoggerConfiguredFromProperty() {
        String sLogConfigFile = System.getProperty("java.util.logging.config.file");
        if (sLogConfigFile != null)
            return true;
        String sLogConfigClass = System.getProperty("java.util.logging.config.class");
        if (sLogConfigClass != null)
            return true;

        return false;
    }

    public static void loadDefaultLogger() {
        loadLoggerConfigResource(Main.class, "LogToFile.properties");
    }

    public static void loadLoggerConfigResource(@Nonnull Class<?> referenceClass,
                                                @Nonnull String sLogFileResource)
    {
        InputStream is = referenceClass.getResourceAsStream(sLogFileResource);
        if (is != null) {
            try { // load the logger configuration
                java.util.logging.LogManager.getLogManager().readConfiguration(is);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, null, ex);
            } finally {
                IO.closeSilently(is, LOG);
            }
        }
    }

    /** Main entry point to the jPSXdec program. */
    public static void main(final String[] asArgs) {
        if (!isLoggerConfiguredFromProperty())
            loadDefaultLogger();

        ArgParser ap = new ArgParser(asArgs);

        boolean blnShowGui = false;
        if (asArgs.length == 0) {
            blnShowGui = true;
        } else if (asArgs.length == 1) {
            blnShowGui = !ap.hasHelp();
        }

        if (blnShowGui) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Gui gui = new Gui();
                    if (asArgs.length > 0)
                        gui.setCommandLineFile(asArgs[0]);
                    gui.setVisible(true);
                }
            });
        } else {
            System.exit(CommandLine.main(ap));
        }
    }
}
