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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


public class LocalizedMessage {
    private final String _sKey;
    private final Object[] _aoArguments;

    public LocalizedMessage(String sKey, Object ... aoArguments) {
        _sKey = sKey;
        _aoArguments = aoArguments;
    }

    public LocalizedMessage(String sKey) {
        _sKey = sKey;
        _aoArguments = null;
    }
    
    public void warn(Logger log) {
        log(log, Level.WARNING, null);
    }
    public void warn(Logger log, Throwable ex) {
        log(log, Level.WARNING, ex);
    }
    
    private void log(Logger log, Level level, Throwable ex) {
        if (ex == null) {
            if (_aoArguments == null)
                log.log(level, _sKey);
            else if (_aoArguments.length == 1)
                log.log(level, _sKey, _aoArguments[0]);
            else
                log.log(level, _sKey, _aoArguments);
        } else {
            if (_aoArguments == null)
                log.log(level, _sKey, ex);
            else
                I18N.log(log, level, ex, _sKey, _aoArguments);
        }
    }

    public String getEnglishMessage() {
        if (_aoArguments == null)
            return _sKey;
        else
            return MessageFormat.format(_sKey, _aoArguments);
    }
    
    public String getLocalizedMessage() {
        if (_aoArguments == null)
            return I18N.S(_sKey);
        else
            return I18N.S(_sKey, _aoArguments);
    }
    
    @Override
    public String toString() {
        return getLocalizedMessage();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + (this._sKey != null ? this._sKey.hashCode() : 0);
        hash = 71 * hash + Arrays.deepHashCode(this._aoArguments);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        
        final LocalizedMessage other = (LocalizedMessage) obj;
        if ((_sKey == null) ? (other._sKey != null) : !_sKey.equals(other._sKey)) {
            return false;
        }
        if (!Arrays.deepEquals(_aoArguments, other._aoArguments)) {
            return false;
        }
        return true;
    }
}
