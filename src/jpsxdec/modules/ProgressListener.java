/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.modules;

public abstract class ProgressListener {

    private static ProgressListener IGNORE;
    
    public static ProgressListener ignore() {
        if (IGNORE == null) {
            IGNORE = new ProgressListener() {
                @Override public void error(Throwable ex) {}
                @Override public void error(String sDescription) {}
                @Override public void event(String sDescription) {}
                @Override public void info(String s) {}
                @Override public void progressEnd() {}
                @Override public void progressStart() {}
                @Override public void progressUpdate(double dblPercentComplete) {}
                @Override public void warning(Throwable ex) {}
                @Override public void warning(String sDescription) {}
                @Override public void progressStart(String s) {}
                @Override public void more(String s) {}
            };
        }
        return IGNORE;
    }

    abstract public void progressStart(String s);

    public void progressStart() { progressStart(null); }

    public void progressEnd() {}

    public void progressUpdate(double dblPercentComplete) {}

    public void event(String sDescription) {}

    public void warning(String sMessage, Throwable cause) { 
        warning(sMessage + " " + cause.getMessage());
    }
    public void warning(Throwable ex) { warning(ex.getMessage()); }
    public void warning(String sDescription) {}

    public void error(String sMessage, Throwable ex) {
        error(sMessage + " " + ex.getMessage());
    }
    public void error(Throwable ex) { error(ex.getMessage()); }
    public void error(String sDescription) {}

    public void info(String s) {}

    abstract public void more(String s);
}
