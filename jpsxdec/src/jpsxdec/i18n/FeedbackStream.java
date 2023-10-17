/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.log.ILocalizedLogger;

/**
 * Filters outputting text based on verbosity level.
 */
public class FeedbackStream {

    private static final Logger LOG = Logger.getLogger(FeedbackStream.class.getName());

    public static final int MORE = 4;
    public static final int NORM = 3;
    public static final int WARN = 2;
    public static final int ERR = 1;
    public static final int NONE = 0;

    private int _iVerboseLevel;

    private final PrintStream _ps;

    public FeedbackStream() {
        this(System.out, NORM);
    }

    public FeedbackStream(@Nonnull PrintStream ps, int iVerboseLevel) {
        _ps = ps;
        _iVerboseLevel = iVerboseLevel;
    }

    public void println() {
        if (_iVerboseLevel >= NORM) {
            _ps.println();
        }
    }

    public void printSpaces(int iCount) {
        if (_iVerboseLevel >= NORM) {
            for (int i = 0; i < iCount; i++) {
                _ps.print(' ');
            }
        }
    }

    public void print(@Nonnull ILocalizedMessage s) {
        if (_iVerboseLevel >= NORM) {
            _ps.print(s.getLocalizedMessage());
        }
    }

    public void println(@Nonnull ILocalizedMessage s) {
        if (_iVerboseLevel >= NORM) {
            _ps.println(s.getLocalizedMessage());
        }
    }

    public void printMore(@Nonnull ILocalizedMessage s) {
        if (_iVerboseLevel >= MORE) {
            _ps.print(s.getLocalizedMessage());
        }
    }

    public void printlnMore(@Nonnull ILocalizedMessage s) {
        if (_iVerboseLevel >= MORE) {
            _ps.println(s.getLocalizedMessage());
        }
    }

    public void printWarn(@Nonnull ILocalizedMessage s) {
        if (_iVerboseLevel >= WARN) {
            _ps.print(s.getLocalizedMessage());
        }
    }

    public void printlnWarn(@Nonnull ILocalizedMessage s) {
        if (_iVerboseLevel >= WARN) {
            _ps.println(s.getLocalizedMessage());
        }
    }

    public void printErr(@Nonnull ILocalizedMessage s) {
        if (_iVerboseLevel >= ERR) {
            _ps.print(s.getLocalizedMessage());
        }
    }

    public void printlnErr(@Nonnull ILocalizedMessage s) {
        if (_iVerboseLevel >= ERR) {
            _ps.println(s.getLocalizedMessage());
        }
    }

    public void setLevel(int iMore) {
        _iVerboseLevel = iMore;
    }

    public int getLevel() {
        return _iVerboseLevel;
    }

    public @Nonnull PrintStream getUnderlyingStream() {
        return _ps;
    }

    public @Nonnull ILocalizedLogger makeLogger() {
        return new FbsLogger();
    }

    private class FbsLogger implements ILocalizedLogger {

        @Override
        public void log(@Nonnull Level level, @Nonnull ILocalizedMessage msg) {
            log(level, msg, null);
        }

        @Override
        public void log(@Nonnull Level level, @Nonnull ILocalizedMessage msg,
                        @CheckForNull Throwable debugException)
        {
            LOG.log(level, msg.getEnglishMessage(), debugException);
            if (level.intValue() < Level.INFO.intValue() ||
                level.intValue() == Level.ALL.intValue() ||
                level.intValue() == Level.CONFIG.intValue())
                printlnMore(msg);
            else if (level.intValue() < Level.WARNING.intValue())
                println(msg);
            else if (level.intValue() < Level.SEVERE.intValue())
                printlnWarn(msg);
            else if (level.intValue() >= Level.SEVERE.intValue() &&
                     level.intValue() != Level.OFF.intValue())
                printlnErr(msg);
        }
    }
}
