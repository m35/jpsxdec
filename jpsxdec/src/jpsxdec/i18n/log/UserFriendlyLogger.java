/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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

package jpsxdec.i18n.log;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.Version;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.util.Misc;


/** Logger that creates a file with a known name, meant for user consumption. */
public class UserFriendlyLogger implements ILocalizedLogger, Closeable {

    private static final Logger LOG = Logger.getLogger(UserFriendlyLogger.class.getName());

    private final DateFormat _dateFormat = DateFormat.getDateTimeInstance();

    /** Listener will be notified of any {@link Level#WARNING} or
     * {@link Level#SEVERE} (errors). */
    public interface OnWarnErr {
        void onWarn(@Nonnull ILocalizedMessage msg);
        void onErr(@Nonnull ILocalizedMessage msg);
    }
    /** Simple implementation of {@link OnWarnErr} that just counts the
     * warnings and errors. */
    public static class WarnErrCounter implements OnWarnErr {
        private int _iWarnCount = 0;
        private int _iErrCount = 0;
        @Override
        public void onWarn(@Nonnull ILocalizedMessage msg) {
            _iWarnCount++;
        }
        @Override
        public void onErr(@Nonnull ILocalizedMessage msg) {
            _iErrCount++;
        }
        public int getWarnCount() {
            return _iWarnCount;
        }
        public int getErrCount() {
            return _iErrCount;
        }
    }

    /** Filename of the logger file. Null if logging to a PrintStream. */
    @CheckForNull
    private File _file;
    /** Stream where all logging goes to. */
    @CheckForNull
    private PrintStream _logStream;
    /** Name of the log. */
    @Nonnull
    private final String _sBaseName;

    @Nonnull
    private final Logger _javaLogger;

    /** Listener for warnings and errors. */
    @CheckForNull
    private OnWarnErr _listener;

    /** Logger will create a logging file upon first log. */
    public UserFriendlyLogger(@Nonnull String sBaseName) {
        _sBaseName = sBaseName;
        _javaLogger = Logger.getLogger(_sBaseName);
    }

    /** Logger will not create a file but write to supplied PrintStream. */
    public UserFriendlyLogger(@Nonnull String sBaseName, @Nonnull PrintStream ps) {
        this(sBaseName);
        _logStream = ps;
    }

    public void setListener(@CheckForNull OnWarnErr listener) {
        _listener = listener;
    }

    /** Returns the file name of the log file. */
    public @Nonnull String getFileName() {
        if (_file == null) {
            if (_logStream == System.out)
                return "<stdout>";
            else if (_logStream == System.err)
                return "<stderr>";
            else
                return "<stream>";
        } else {
            return _file.toString();
        }
    }

    @Override
    public void log(@Nonnull Level level, @Nonnull ILocalizedMessage msg) {
        log(level, msg, null);
    }
    @Override
    public void log(@Nonnull Level level, @Nonnull ILocalizedMessage msg, @CheckForNull Throwable debugException) {
        msg.logEnglish(_javaLogger, level, debugException); // also log to normal logging
        if (_logStream == null)
            openOutputFile();

        if (level == Level.WARNING) {
            if (_listener != null) _listener.onWarn(msg);
        } else if (level == Level.SEVERE) {
            if (_listener != null) _listener.onErr(msg);
        }

        // don't log the exception for the user as it most likely will just be useless or confusing
        _logStream.println(I.USER_LOG_MESSAGE(
                           level.getLocalizedName(),
                           msg.getLocalizedMessage()));
        _logStream.flush();
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
            Misc.log(LOG, Level.SEVERE, fail, "Unable to open log file {0}", file);
            try {
                file = File.createTempFile(_sBaseName, ".log", new File("."));
                fos = new FileOutputStream(file);
            } catch (IOException ex) {
                Misc.log(LOG, Level.SEVERE, ex, "Unable to open log file {0}", file);
                _logStream = System.err;
            }
        }
        if (fos != null) {
            _file = file;
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            try {
                _logStream = new PrintStream(bos, true, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                LOG.log(Level.SEVERE, null, ex);
                _logStream = new PrintStream(bos, true);
            }
            writeFileHeader(_logStream);
        }
    }

    private void writeFileHeader(@Nonnull PrintStream ps) {
        ps.println(I.JPSXDEC_VERSION_NON_COMMERCIAL(Version.Version).getLocalizedMessage());
        ps.print(System.getProperty("os.name")); ps.print(' '); ps.println(System.getProperty("os.version"));
        ps.print("Java "); ps.println(System.getProperty("java.version"));
        ps.println(_dateFormat.format(Calendar.getInstance().getTime()));
    }

    @Override
    public void close() {
        if (_file != null) // only close the stream if it's pointing to a file (don't close stdout/stderr)
            _logStream.close();
    }

}
