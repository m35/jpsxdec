/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014  Michael Sabin
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

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** String localization for jPSXdec. */
public class I18N {
    
    private static final Logger LOG = Logger.getLogger(I18N.class.getName());
    
    private static final ResourceBundle _translationBundle;
    static {
        ResourceBundle rb = null;
        try {
            rb = ResourceBundle.getBundle(getResourceBundleName());
        } catch (MissingResourceException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        _translationBundle = rb;
    }
    
    public static String getResourceBundleName() {
        return "jpsxdec.Translations";
    }
    public static ResourceBundle getResourceBundle() {
        return _translationBundle;
    }

    /** Localize a string with arguments. */
    public static String S(String sKey, Object ... aoArgs) {
        if (aoArgs == null || aoArgs.length == 0)
            LOG.warning("I18N arguments == null || length == 0");
        return java.text.MessageFormat.format(S(sKey), aoArgs);
    }
    /** Localize a string.
     * If no key is found, the key is returned as the localized string. */
    public static String S(String sKey) {
        if (true) { // TODO: enable once keys are in bundle
            return sKey;
        } else {
            if (_translationBundle == null)
                return sKey;
            try {
                return _translationBundle.getString(sKey);
            } catch (MissingResourceException ex) {
                LOG.log(Level.WARNING, sKey, ex);
                return sKey;
            }
        }
    }
    
    /** Logging with message format AND exception. */
    public static void log(Logger log, Level level, Throwable exception, 
                           String sMessage, Object ... aoArguments)
    {
        LogRecord lr = new LogRecord(level, sMessage);
        lr.setLoggerName(log.getName());
        lr.setParameters(aoArguments);
        lr.setResourceBundle(_translationBundle);
        lr.setThrown(exception);
        log.log(lr);
    }
}
