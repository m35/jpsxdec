/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2014  Michael Sabin
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
import jpsxdec.I18N;
import jpsxdec.Version;


/** Logger that creates a file with a known name, meant for user consumption. */
public class UserFriendlyLogger extends Logger {

    private final DateFormat _dateFormat = DateFormat.getDateTimeInstance();

    /** Listener will be notified of any {@link Level#WARNING} or
     * {@link Level#SEVERE} (errors). */
    public static interface OnWarnErr {
        void onWarn(LogRecord record);
        void onErr(LogRecord record);
    }
    
    private static final class FriendlyFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            throw new UnsupportedOperationException("Should never be called");
        }

        public void print(LogRecord record, PrintStream ps) {
            ps.print('[');
            ps.print(record.getLevel().getLocalizedName());
            ps.print("] ");
            if (record.getMessage() != null) {
                ps.print(formatMessage(record)); // localizes the message
            }
            ps.println();
            if (record.getThrown() != null) {
                ps.print("[^EX^] "); // I18N
                record.getThrown().printStackTrace(ps);
            }
        }
    }
    private static final FriendlyFormatter FORMATTER = new FriendlyFormatter();

    /** Filename of the logger file. Null if logging to a PrintStream. */
    private File _file;
    /** Stream where all logging goes to. */
    private PrintStream _ps;
    /** Name of the log. */
    private final String _sBaseName;

    private final Logger _globalLogger;
    
    /** Listener for warnings and errors. */
    private OnWarnErr _listener;

    /** Logger will create a logging file upon first log. */
    public UserFriendlyLogger(String sBaseName) {
        super(sBaseName, null);
        _sBaseName = sBaseName;
        _globalLogger = Logger.getLogger(_sBaseName);
    }

    /** Logger will not create a file but write to supplied PrintStream. */
    public UserFriendlyLogger(String sBaseName, PrintStream ps) {
        this(sBaseName);
        if (ps == null)
            throw new NullPointerException();
        _ps = ps;
    }

    public void setListener(OnWarnErr listener) {
        _listener = listener;
    }

    /** Returns the file name of the log file. */
    public String getFileName() {
        return String.valueOf(_file);
    }

    @Override
    public void log(LogRecord record) {
        _globalLogger.log(record);
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
                Logger.getLogger(UserFriendlyLogger.class.getName()).log(
                        Level.SEVERE, "Unable to create custom logger file {0}", _file);
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

    private void writeFileHeader(PrintStream ps) {
        ps.println(Version.VerString.getLocalizedMessage());
        ps.print(System.getProperty("os.name")); ps.print(' '); ps.println(System.getProperty("os.version"));
        ps.print("Java "); ps.println(System.getProperty("java.version"));
        ps.println(_dateFormat.format(Calendar.getInstance().getTime()));
    }

    public void close() {
        if (_file != null)
            _ps.close();
    }

    @Override
    public String getResourceBundleName() {
        return I18N.getResourceBundleName();
    }

    @Override
    public ResourceBundle getResourceBundle() {
        return I18N.getResourceBundle();
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
