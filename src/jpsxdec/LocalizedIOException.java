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

import java.io.IOException;
import java.text.MessageFormat;

public class LocalizedIOException extends IOException {

    private final Object[] _aoArguments;
    
    public LocalizedIOException() {
        _aoArguments = null;
    }

    public LocalizedIOException(String sMessage) {
        super(sMessage);
        _aoArguments = null;
    }

    public LocalizedIOException(String sMessage, Object ... aoArguments) {
        super(sMessage);
        _aoArguments = aoArguments;
    }

    public LocalizedIOException(Throwable cause) {
        super();
        initCause(cause);
        _aoArguments = null;
    }
    
    public LocalizedIOException(Throwable cause, String sMessage, Object ... aoArguments) {
        super(sMessage);
        initCause(cause);
        _aoArguments = aoArguments;
    }

    @Override
    public String getLocalizedMessage() {
        String sSuperMessage = super.getMessage();
        if (sSuperMessage == null)
            return null;
        else {
            if (_aoArguments == null)
                return I18N.S(sSuperMessage);
            else
                return I18N.S(sSuperMessage, _aoArguments);
        }
    }

    @Override
    public String getMessage() {
        String sSuperMessage = super.getMessage();
        if (_aoArguments == null)
            return sSuperMessage;
        else if (sSuperMessage != null)
            return MessageFormat.format(sSuperMessage, _aoArguments);
        else
            return null;
    }
    
}
