/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2015  Michael Sabin
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedMessage;
import jpsxdec.Version;


/** Logger that creates a file with a known name, meant for user consumption. */
public class UserFriendlyLogger extends Logger {

    private final DateFormat _dateFormat = DateFormat.getDateTimeInstance();

    /** Listener will be notified of any {@link Level#WARNING} or
     * {@link Level#SEVERE} (errors). */
    public static interface OnWarnErr {
        void onWarn(@Nonnull LogRecord record);
        void onErr(@Nonnull LogRecord record);
    }
    
    private static final class FriendlyFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            throw new UnsupportedOperationException("Should never be called");
        }

        private final static int HAS_MESSAGE = 1;
        private final static int HAS_EXCEPTION = 2;
        private final static int HAS_EXCEPTION_MSG = 4;

        public void print(@Nonnull LogRecord record, @Nonnull PrintStream ps) {

            int iFlags = 0;
            if (record.getMessage() != null)
                iFlags |= HAS_MESSAGE;
            
            Throwable ex = record.getThrown();
            String sExMsg = null;
            if (ex != null) {
                iFlags |= HAS_EXCEPTION;
                sExMsg = ex.getLocalizedMessage();
                if (sExMsg != null)
                    iFlags |= HAS_EXCEPTION_MSG;
            }

            switch (iFlags) {
                default:
                case 0:
                    // ???
                    ps.println("Unexpected log record state " + iFlags);
                    break;
                case HAS_MESSAGE:
                    ps.println(I.USER_LOG_MESSAGE(
                               record.getLevel().getLocalizedName(),
                               formatMessage(record)));
                    break;

                case HAS_EXCEPTION:
                    ps.println(I.USER_LOG_EXCEPTION(
                               record.getLevel().getLocalizedName(),
                               ex.getClass().getSimpleName()));
                    break;
                case HAS_EXCEPTION | HAS_EXCEPTION_MSG:
                    ps.println(I.USER_LOG_EXCEPTION_MSG(
                               record.getLevel().getLocalizedName(),
                               ex.getClass().getSimpleName(),
                               sExMsg));
                    break;
                    
                case HAS_MESSAGE | HAS_EXCEPTION:
                    ps.println(I.USER_LOG_MESSAGE_EXCEPTION(
                               record.getLevel().getLocalizedName(),
                               formatMessage(record),
                               ex.getClass().getSimpleName()));
                    break;
                case HAS_MESSAGE | HAS_EXCEPTION | HAS_EXCEPTION_MSG:
                    ps.println(I.USER_LOG_MESSAGE_EXCEPTION_MSG(
                               record.getLevel().getLocalizedName(),
                               formatMessage(record),
                               ex.getClass().getSimpleName(),
                               sExMsg));
                    break;
                    
            }
        }
    }
    private static final FriendlyFormatter FORMATTER = new FriendlyFormatter();

    /** Filename of the logger file. Null if logging to a PrintStream. */
    @CheckForNull
    private File _file;
    /** Stream where all logging goes to. */
    @CheckForNull
    private PrintStream _ps;
    /** Name of the log. */
    @Nonnull
    private final String _sBaseName;

    @Nonnull
    private final Logger _globalLogger;
    
    /** Listener for warnings and errors. */
    @CheckForNull
    private OnWarnErr _listener;

    /** Logger will create a logging file upon first log. */
    public UserFriendlyLogger(@Nonnull String sBaseName) {
        super(sBaseName, null);
        _sBaseName = sBaseName;
        _globalLogger = Logger.getLogger(_sBaseName);
    }

    /** Logger will not create a file but write to supplied PrintStream. */
    public UserFriendlyLogger(@Nonnull String sBaseName, @Nonnull PrintStream ps) {
        this(sBaseName);
        _ps = ps;
    }

    public void setListener(@CheckForNull OnWarnErr listener) {
        _listener = listener;
    }

    /** Returns the file name of the log file. */
    public @Nonnull String getFileName() {
        return _file == null ? "stderr" : _file.toString();
    }

    @Override
    public void log(@Nonnull LogRecord record) {
        _globalLogger.log(record); // also log to normal logging
        if (_ps == null)
            openOutputFile();

        Level lvl = record.getLevel();

        if (lvl == Level.WARNING) {
            if (_listener != null) _listener.onWarn(record);
        } else if (lvl == Level.SEVERE) {
            if (_listener != null) _listener.onErr(record);
        }
        FORMATTER.print(record, _ps);
        _ps.flush();
    }

    /** Attempts to open the target log file.
     * If fails, tries to create a temp file with the same base name.
     * If that fails, logs to System.err.  */
    private void openOutputFile() {
        File file = new File(_sBaseName + ".log");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException fail) {
            try {
                file = File.createTempFile(_sBaseName, ".log", new File("."));
                fos = new FileOutputStream(file);
            } catch (IOException ex) {
                Misc.log(Logger.getLogger(UserFriendlyLogger.class.getName()),
                        Level.SEVERE, ex, "Unable to create custom logger file {0}", file);
                _ps = System.err;
            }
        }
        if (fos != null) {
            _file = file;
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            try {
                _ps = new PrintStream(bos, true, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(UserFriendlyLogger.class.getName()).log(Level.SEVERE, null, ex);
                _ps = new PrintStream(bos, true);
            }
            writeFileHeader(_ps);
        }
    }

    private void writeFileHeader(@Nonnull PrintStream ps) {
        ps.println(I.JPSXDEC_VERSION_NON_COMMERCIAL(Version.Version).getLocalizedMessage());
        ps.print(System.getProperty("os.name")); ps.print(' '); ps.println(System.getProperty("os.version"));
        ps.print("Java "); ps.println(System.getProperty("java.version"));
        ps.println(_dateFormat.format(Calendar.getInstance().getTime()));
    }

    public void close() {
        if (_file != null)
            _ps.close();
    }

    // -----------------------------------------------------------------------
    // -- Not currently supported --------------------------------------------
    // -----------------------------------------------------------------------

    @Override
    public synchronized boolean getUseParentHandlers() {
        return false;
    }

    @Override
    public synchronized void addHandler(Handler hndlr) throws SecurityException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void removeHandler(Handler hndlr) throws SecurityException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Handler[] getHandlers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFilter(Filter filter) throws SecurityException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setParent(Logger logger) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void setUseParentHandlers(boolean bln) {
        throw new UnsupportedOperationException();
    }

}
