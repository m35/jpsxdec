/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2015  Michael Sabin
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

import javax.annotation.CheckForNull;

public class LocalizedException extends Exception {

    @CheckForNull
    private final LocalizedMessage _msg;

    public LocalizedException() {
        _msg = null;
    }

    public LocalizedException(@CheckForNull LocalizedMessage msg) {
        super(msg == null ? null : msg.getEnglishMessage());
        _msg = msg;
    }

    public LocalizedException(@CheckForNull Throwable cause) {
        super(cause);
        _msg = null;
    }

    public LocalizedException(@CheckForNull LocalizedMessage msg, @CheckForNull Throwable cause) {
        super(msg == null ? null : msg.getEnglishMessage(), cause);
        _msg = msg;
    }

    public @CheckForNull LocalizedMessage getSourceMessage() {
        return _msg;
    }

    @Override
    public @CheckForNull String getLocalizedMessage() {
        if (_msg == null)
            return super.getLocalizedMessage();
        else
            return _msg.getLocalizedMessage();
    }

    @Override
    public @CheckForNull String getMessage() {
        if (_msg == null)
            return super.getMessage();
        else
            return _msg.getEnglishMessage();
    }
    
}
