/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2023  Michael Sabin
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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.Misc;

/** Concrete localized string for display to the user.
 * <p>
 * This class waits until the last moment to query for the localized version
 * of the string, allowing it to dynamically change with the default locale.
 * This also provides access to the original English for debugging and
 * presenting messages to the developer.
 */
class LocalizedMessage implements ILocalizedMessage {

    private static final Logger LOG = Logger.getLogger(LocalizedMessage.class.getName());

    @Nonnull
    private final String _sKey;
    @Nonnull
    private final String _sEnglishDefault;
    @CheckForNull
    private final Object[] _aoArguments;

    LocalizedMessage(@Nonnull String sKey, @Nonnull String sEnglishDefault, Object ... aoArguments) {
        _sKey = sKey;
        _sEnglishDefault = sEnglishDefault;
        _aoArguments = aoArguments;
    }

    LocalizedMessage(@Nonnull String sKey, @Nonnull String sEnglishDefault) {
        _sKey = sKey;
        _sEnglishDefault = sEnglishDefault;
        _aoArguments = null;
    }

    @Override
    public void logEnglish(@Nonnull Logger log, @Nonnull Level level) {
        logEnglish(log, level, null);
    }

    @Override
    public void logEnglish(@Nonnull Logger log, @Nonnull Level level, @CheckForNull Throwable ex) {
        LogRecord lr = new LogRecord(level, _sEnglishDefault);
        lr.setLoggerName(log.getName());
        if (_aoArguments != null)
            lr.setParameters(_aoArguments);
        if (ex != null)
            lr.setThrown(ex);
        log.log(lr);
    }

    @Override
    public @Nonnull String getEnglishMessage() {
        if (_aoArguments == null)
            return _sEnglishDefault;
        else {
            Object[] aoArgCopy = new Object[_aoArguments.length];
            for (int i = 0; i < _aoArguments.length; i++) {
                Object arg = _aoArguments[i];
                if (arg instanceof ILocalizedMessage)
                    aoArgCopy[i] = ((ILocalizedMessage)arg).getEnglishMessage(); // recursively get all the English
                else
                    aoArgCopy[i] = arg;
            }

            return MessageFormat.format(_sEnglishDefault, aoArgCopy);
        }
    }

    @Override
    public @Nonnull String getLocalizedMessage() {
        String sMessage = lookupValue();
        if (sMessage == null)
            sMessage = _sEnglishDefault;

        if (_aoArguments == null)
            return sMessage;
        else
            return MessageFormat.format(sMessage, _aoArguments);
    }

    private @CheckForNull String lookupValue() {
        ResourceBundle rb = Bundle.getResourceBundle();
        try {
            if (rb != null)
                return rb.getString(_sKey);
        } catch (MissingResourceException ex) {
            LOG.log(Level.WARNING, "Missing I18N for {0}", _sKey);
        }
        return null;
    }

    @Override
    public String toString() {
        return getLocalizedMessage();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this._sKey != null ? this._sKey.hashCode() : 0);
        hash = 97 * hash + (this._sEnglishDefault != null ? this._sEnglishDefault.hashCode() : 0);
        hash = 97 * hash + Arrays.deepHashCode(this._aoArguments);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final LocalizedMessage other = (LocalizedMessage) obj;
        if (!Misc.objectEquals(this._sKey, other._sKey))
            return false;
        if (!Misc.objectEquals(this._sEnglishDefault, other._sEnglishDefault))
            return false;
        if (!Arrays.deepEquals(this._aoArguments, other._aoArguments))
            return false;

        return true;
    }

    @Override
    public boolean equalsIgnoreCase(String s) {
        String sMessage = lookupValue();
        return _sEnglishDefault.equalsIgnoreCase(s) || (sMessage != null && sMessage.equalsIgnoreCase(s));
    }

}
