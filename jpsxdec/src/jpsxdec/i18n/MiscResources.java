/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2023  Michael Sabin
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

package jpsxdec.i18n;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;


public class MiscResources extends ListResourceBundle {
    private static final Logger LOG = Logger.getLogger(MiscResources.class.getName());

    private static final ResourceBundle MISC_RESOURCES;
    static {
        ResourceBundle rb = null;
        try {
            rb = ResourceBundle.getBundle(MiscResources.class.getCanonicalName());
        } catch (MissingResourceException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        MISC_RESOURCES = rb;
    }
    private static String getResource(String sKey) throws MissingResourceException {
        if (MISC_RESOURCES == null)
            throw new MissingResourceException("MiscResources not loaded", MiscResources.class.getName(), sKey);
        return MISC_RESOURCES.getString(sKey);
    }

    //==========================================================================

    static final String MAIN_CMDLINE_HELP = "main_cmdline_help";
    public static @Nonnull Iterator<ILocalizedMessage> main_cmdline_help() throws MissingResourceException {
        String sFile = getResource(MAIN_CMDLINE_HELP);
        InputStream is = MiscResources.class.getResourceAsStream(sFile);
        if (is == null)
            throw new MissingResourceException("Failed to open main help resource " + sFile,
                                               MiscResources.class.getName(),
                                               MAIN_CMDLINE_HELP);
        try {
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            ArrayList<ILocalizedMessage> lines = new ArrayList<ILocalizedMessage>();
            String sLine;
            while ((sLine = br.readLine()) != null) {
                lines.add(new UnlocalizedMessage(sLine));
            }
            return lines.iterator();
        } catch (Throwable cause) {
            MissingResourceException ex = new MissingResourceException(
                    "Error loading resource",
                    MiscResources.class.getName(),
                    MAIN_CMDLINE_HELP);
            ex.initCause(cause);
            throw ex;
        } finally {
            IO.closeSilently(is, LOG);
        }
    }

    //==========================================================================

    @Override
    protected Object[][] getContents() {
        return new Object[][] {
            { MAIN_CMDLINE_HELP, "main_cmdline_help.dat" }
        };
    }

}
