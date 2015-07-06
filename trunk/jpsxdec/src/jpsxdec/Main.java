/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.cmdline.CommandLine;
import jpsxdec.gui.Gui;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void loadDefaultLogger() {
        loadLogger("LogToFile.properties");
    }

    public static void loadLogger(@Nonnull String sLogFileResource) {
        InputStream is = Main.class.getResourceAsStream(sLogFileResource);
        if (is != null) {
            try { // load the logger configuration
                java.util.logging.LogManager.getLogManager().readConfiguration(is);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, null, ex);
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /** Main entry point to the jPSXdec program. */
    public static void main(final String[] asArgs) {

        loadDefaultLogger();

        boolean blnShowGui = false;
        if (asArgs.length == 0) {
            blnShowGui = true;
        } else if (asArgs.length == 1) {
            blnShowGui = !CommandLine.checkForMainHelp(asArgs);
        } 
        
        if (blnShowGui) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Gui gui = new Gui();
                    if (asArgs.length > 0)
                        gui.setCommandLineFile(asArgs[0]);
                    gui.setVisible(true);
                }
            });
        } else {
            System.exit(CommandLine.main(asArgs));
        }
    }
}
