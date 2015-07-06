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

package jpsxdec.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import javax.annotation.Nonnull;
import jpsxdec.i18n.LocalizedMessage;

/** Filters outputting text based on verbosity level. */
public class FeedbackStream extends PrintStream {

    public static final int MORE = 4;
    public static final int NORM = 3;
    public static final int WARN = 2;
    public static final int ERR = 1;
    public static final int NONE = 0;

    private static final int INDENT_AMOUNT = 2;

    private int _iIndentLevel = 0;
    boolean _blnNewLine = true;
    private int _iVerboseLevel;
    private int _iPrintDefault = NORM;

    public FeedbackStream() {
        this(System.out, NORM);
    }

    public FeedbackStream(@Nonnull OutputStream out, int iVerboseLevel) {
        super(out);
        _iVerboseLevel = iVerboseLevel;
    }

    //<editor-fold defaultstate="collapsed" desc="PrintStream overrides">
    @Override
    public void print(boolean b) { 
        if (_iVerboseLevel >= _iPrintDefault)  {
            printIndentIfAny();
            super.print(b);
        }
    }
    @Override
    public void print(char c) { 
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.print(c);
        }
    }
    @Override
    public void print(int i) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.print(i);
        }
    }
    @Override
    public void print(long l) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.print(l);
        }
    }
    @Override
    public void print(float f) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.print(f);
        }
    }
    @Override
    public void print(double d) {  
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.print(d);
        }
    }
    @Override
    public void print(char[] s) {  
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.print(s);
        }
    }
    @Override
    public void print(String s) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.print(s);
        }
    }
    @Override
    public void print(Object obj) {  
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.print(obj);
        }
    }
    @Override
    public void println() {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println();
            _blnNewLine = true;
        }
    }
    @Override
    public void println(boolean x) {   
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println(x);
            _blnNewLine = true;
        }
    }
    @Override
    public void println(char x) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println(x);
            _blnNewLine = true;
        }
    }
    @Override
    public void println(int x) {   
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println(x);
            _blnNewLine = true;
        }
    }
    @Override
    public void println(long x) {   
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println(x);
            _blnNewLine = true;
        }
    }
    @Override
    public void println(float x) {   
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println(x);
            _blnNewLine = true;
        }
    }
    @Override
    public void println(double x) {   
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println(x);
            _blnNewLine = true;
        }
    }
    @Override
    public void println(char[] x) {   
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println(x);
            _blnNewLine = true;
        }
    }
    @Override
    public void println(String x) {   
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println(x);
            _blnNewLine = true;
        }
    }
    @Override
    public void println(Object x) {   
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            super.println(x);
            _blnNewLine = true;
        }
    }
    /*
    @Override
    public void write(int b) {   
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndent();
            super.write(b);
        }
    }
    @Override
    public void write(byte[] buf, int off, int len) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndent();
            super.write(buf, off, len);
        }
    }
    */
    //.........................................................................
    @Override
    public FeedbackStream printf(String format, Object... args) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            return (FeedbackStream) super.printf(format, args);
        }
        return this;
    }
    @Override
    public FeedbackStream printf(Locale l, String format, Object... args) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            return (FeedbackStream) super.printf(l, format, args);
        }
        return this;
    }
    @Override
    public FeedbackStream append(CharSequence csq) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            return (FeedbackStream) super.append(csq);
        }
        return this;
    }
    @Override
    public FeedbackStream append(CharSequence csq, int start, int end) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            return (FeedbackStream) super.append(csq, start, end);
        }
        return this;
    }
    @Override
    public FeedbackStream append(char c) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            return (FeedbackStream) super.append(c);
        }
        return this;
    }
    @Override
    public FeedbackStream format(String format, Object... args) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            return (FeedbackStream) super.format(format, args);
        }
        return this;
    }
    @Override
    public FeedbackStream format(Locale l, String format, Object... args) {
        if (_iVerboseLevel >= _iPrintDefault) {
            printIndentIfAny();
            return (FeedbackStream) super.format(l, format, args);
        }
        return this;
    }
    // </editor-fold>

    //========================================================================

    public void printMore(@Nonnull LocalizedMessage s) {
        if (_iVerboseLevel >= MORE) {
            printIndentIfAny();
            super.print(s);
        }
    }
    public void printlnMore(@Nonnull LocalizedMessage s) {
        if (_iVerboseLevel >= MORE) {
            printIndentIfAny();
            super.println(s);
            _blnNewLine = true;
        }
    }
    public void printWarn(@Nonnull LocalizedMessage s) {
        if (_iVerboseLevel >= WARN) {
            printIndentIfAny();
            super.print(s);
        }
    }
    public void printlnWarn(@Nonnull LocalizedMessage s) {
        if (_iVerboseLevel >= WARN) {
            printIndentIfAny();
            super.println(s);
            _blnNewLine = true;
        }
    }

    public void printErr(@Nonnull LocalizedMessage s)  {
        if (_iVerboseLevel >= ERR) {
            printIndentIfAny();
            super.print(s);
        }
    }
    public void printlnErr(@Nonnull String s)  {
        if (_iVerboseLevel >= ERR) {
            printIndentIfAny();
            super.println(s);
            _blnNewLine = true;
        }
    }
    public void printlnErr(@Nonnull LocalizedMessage s)  {
        if (_iVerboseLevel >= ERR) {
            printIndentIfAny();
            super.println(s);
            _blnNewLine = true;
        }
    }
    public void printlnErr(@Nonnull Throwable ex)  {
        if (_iVerboseLevel >= MORE) {
            // if MORE verbosity is wanted, then print the entire stack trace
            ex.printStackTrace(this);
        } else if (_iVerboseLevel >= ERR) {
            printIndentIfAny();
            String sMsg = ex.getMessage();
            if (sMsg == null)
                sMsg = ex.toString();
            super.println(sMsg);
            _blnNewLine = true;
        }
    }

    private void printIndentIfAny() {
        if (_blnNewLine && _iIndentLevel > 0) {
            super.print(Misc.dup("  ", _iIndentLevel));
            _blnNewLine = false;
        }
    }

    public @Nonnull FeedbackStream indent() {
        _iIndentLevel += INDENT_AMOUNT;
        return this;
    }

    public @Nonnull FeedbackStream indent1() {
        super.print("  ");
        _blnNewLine = true;
        return this;
    }

    public @Nonnull FeedbackStream indent(int iAmount) {
        _iIndentLevel += iAmount * INDENT_AMOUNT;
        return this;
    }

    public @Nonnull FeedbackStream outdent() {
        _iIndentLevel -= INDENT_AMOUNT;
        if (_iIndentLevel < 0)
            _iIndentLevel = 0;
        return this;
    }

    public @Nonnull FeedbackStream outdent(int iAmount) {
        _iIndentLevel -= iAmount * INDENT_AMOUNT;
        if (_iIndentLevel < 0)
            _iIndentLevel = 0;
        return this;
    }

    public boolean printMore() {
        return _iVerboseLevel == MORE;
    }

    public void setLevel(int iMore) {
        _iVerboseLevel = iMore;
    }

}
