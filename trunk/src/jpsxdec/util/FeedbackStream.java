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

package jpsxdec.util;

import java.io.PrintStream;

/** Filters outputting text based on verbosity level. */
public class FeedbackStream {

    public static final int MORE = 4;
    public static final int NORM = 3;
    public static final int WARN = 2;
    public static final int ERR = 1;
    public static final int NONE = 0;

    private static final int INDENT_AMOUNT = 2;

    private int _iIndentLevel = 0;
    private PrintStream _out;
    boolean _blnNewLine;
    private int _iVerboseLevel;

    private int _iLastLevel;

    public FeedbackStream(PrintStream out, int iVerboseLevel) {
        _out = out;
        _iLastLevel = _iVerboseLevel = iVerboseLevel;
    }

    public FeedbackStream printMore(String s) { return print(MORE, s); }
    public FeedbackStream printNorm(String s) { return print(NORM, s); }
    public FeedbackStream printWarn(String s) { return print(WARN, s); }
    public FeedbackStream printErr(String s)  { return print(ERR,  s); }

    public FeedbackStream printlnMore(String s) { return print(MORE, s).nl(MORE); }
    public FeedbackStream printlnNorm(String s) { return print(NORM, s).nl(NORM); }
    public FeedbackStream printlnWarn(String s) { return print(WARN, s).nl(WARN); }
    public FeedbackStream printlnErr(String s)  { return print(ERR,  s).nl(ERR); }

    public FeedbackStream print(int iLevel, String s) {
        if (iLevel <= _iVerboseLevel) {
            writeIndent();
            _out.print(s);
        }
        _iLastLevel = iLevel;
        return this;
    }

    public FeedbackStream format(int iLevel, String s, Object ... args) {
        if (iLevel <= _iVerboseLevel) {
            writeIndent();
            _out.format(s, args);
        }
        _iLastLevel = iLevel;
        return this;
    }

    private void writeIndent() {
        if (_blnNewLine) {
            if (_iIndentLevel > 0) {
                _out.print(Misc.dup(' ', _iIndentLevel));
            }
            _blnNewLine = false;
        }
    }

    public FeedbackStream nl(int iLevel) {
        if (iLevel <= _iVerboseLevel) {
            _out.println();
            _blnNewLine = true;
        }
        _iLastLevel = iLevel;
        return this;
    }

    public FeedbackStream nl() {
        return nl(_iLastLevel);
    }

    public FeedbackStream indent() {
        _iIndentLevel += INDENT_AMOUNT;
        return this;
    }

    public FeedbackStream indent(int iAmount) {
        _iIndentLevel += iAmount * INDENT_AMOUNT;
        return this;
    }

    public FeedbackStream outdent() {
        _iIndentLevel -= INDENT_AMOUNT;
        if (_iIndentLevel < 0)
            _iIndentLevel = 0;
        return this;
    }

    public FeedbackStream outdent(int iAmount) {
        _iIndentLevel -= iAmount * INDENT_AMOUNT;
        if (_iIndentLevel < 0)
            _iIndentLevel = 0;
        return this;
    }

}
