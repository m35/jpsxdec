/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013  Michael Sabin
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jpsxdec.Version;


public class UserFriendlyLogger extends Logger {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final int HEADER_LEVEL_COUNT = 4;
    
    public static interface OnWarnErr {
        void onWarn(LogRecord record);
        void onErr(LogRecord record);
    }

    private final String[] _asHeaders = new String[HEADER_LEVEL_COUNT];
    private boolean _blnHasHeader = false;

    /** Filename of the logger file. Null if logging to a PrintStream. */
    private File _file;
    /** Stream where all logging goes to. */
    protected PrintStream _ps;
    /** Name of the log. */
    private final String _sBaseName;

    /** Listener for warnings and errors. */
    private OnWarnErr _listener;

    /** Logger will create a logging file upon first log. */
    public UserFriendlyLogger(String sBaseName) {
        super(sBaseName, null);
        _sBaseName = sBaseName;
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
        return _file == null ? null : _file.toString();
    }

    /** Only written if something is logged. */
    public void setHeader(int iHeaderLevel, String s) {
        _asHeaders[iHeaderLevel] = s;
        _blnHasHeader = true;
    }

    @Override
    public void log(LogRecord record) {
        if (_ps == null)
            openOutputFile();

        if (_blnHasHeader) {
            for (int i = 0; i < _asHeaders.length; i++) {
                if (_asHeaders[i] == null)
                    continue;
                _ps.println(_asHeaders[i]);
                _asHeaders[i] = null;
            }
            _blnHasHeader = false;
        }

        Level lvl = record.getLevel();

        if (lvl == Level.WARNING) {
            if (_listener != null) _listener.onWarn(record);
            _ps.print("[WARN] ");
        } else if (lvl == Level.SEVERE) {
            if (_listener != null) _listener.onErr(record);
            _ps.print("[ERR] ");
        } else {
            _ps.print('[');
            _ps.print(lvl.getName());
            _ps.print("] ");
        }
        if (record.getMessage() != null) {
            _ps.print(record.getMessage());
        }
        _ps.println();
        if (record.getThrown() != null) {
            _ps.print("[^EX^] ");
            record.getThrown().printStackTrace(_ps);
        }
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
                Logger.getLogger(UserFriendlyLogger.class.getName()).severe(
                        "Unable to create custom logger file " + _file);
                _ps = System.err;
            }
        }
        if (fos != null) {
            _file = file;
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            _ps = new PrintStream(bos, true);
            writeFileHeader(_ps);
        }
    }

    private static void writeFileHeader(PrintStream ps) {
        ps.println(Version.VerString);
        ps.print(System.getProperty("os.name")); ps.print(' '); ps.println(System.getProperty("os.version"));
        ps.print("Java "); ps.println(System.getProperty("java.version"));
        ps.println(FORMAT.format(Calendar.getInstance().getTime()));
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
