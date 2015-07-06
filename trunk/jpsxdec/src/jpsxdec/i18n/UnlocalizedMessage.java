/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015  Michael Sabin
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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/** A localized string that doesn't need to be localized.
 * <p>
 * Some strings (primarily file paths) need to be presented to the user, but
 * cannot or should not be localized. This represents those kinds of strings.
 */
public class UnlocalizedMessage extends LocalizedMessage {

    @Nonnull
    private final String _sMessage;

    public UnlocalizedMessage(@Nonnull String sMessage) {
        super(null, null);
        _sMessage = sMessage;
    }

    @Override
    public boolean equalsIgnoreCase(String s) {
        return _sMessage.equalsIgnoreCase(s);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        return _sMessage.equals(((UnlocalizedMessage)obj)._sMessage);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this._sMessage != null ? this._sMessage.hashCode() : 0);
        return hash;
    }


    @Override
    public String toString() {
        return _sMessage;
    }

    @Override
    public String getLocalizedMessage() {
        return _sMessage;
    }

    @Override
    public String getEnglishMessage() {
        return _sMessage;
    }

    @Override
    public void log(Logger log, Level level, Throwable ex) {
        log.log(level, _sMessage, ex);
    }

    @Override
    public void log(Logger log, Level level) {
        log.log(level, _sMessage);
    }

}
