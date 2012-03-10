/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import jpsxdec.Main;

/**
 * Specially designed logging Handler for logging long-progress errors to a
 * file. It includes the following features:
 * <ul>
 * <li>Specifying of log file name
 * <li>Simple handling when log file is locked
 * <li>Adds header with basic system info
 * <li>File is only created when an error is reported
 * <li>Can add 'sub-header' lines that are only injected when an error
 *     occurs under that sub-header
 * <li>Subclasses can respond to error/warning events
 *     (to count them, or notify user in interface).
 * </ul>
 * It also automatically uses a very simple formatter to make it easier for
 * uses to read, to reduce overhead, and keep log file size down.
 */
public abstract class UserFriendlyHandler extends StreamHandler {

    private final static String LINE_SEPARATOR = System.getProperty("line.separator");

    private final CustomFormatter _formatter;
    private String _sSubHeader;
    private String _sBaseName;
    private File _file;
    private PrintStream _ps;

    public UserFriendlyHandler(String sBaseName) {
        _sBaseName = sBaseName;
        _formatter = new CustomFormatter();
        setFormatter(_formatter);
    }

    abstract protected void onWarn(LogRecord record);
    abstract protected void onErr(LogRecord record);

    /** Returns the file name of the log file. */
    public String getFileName() {
        return _file == null ? null : _file.toString();
    }

    /** Inserts a sub-header in the log file, but only if something gets logged. 
     * This prevents log files being written with no useful info. */
    public void setSubheader(String s) {
        _formatter._sSubHeaderLine = s;
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (_file == null) {
            _file = new File(_sBaseName + ".log");
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(_file));
                setOutputStream(bos);
                _ps = new PrintStream(bos, true);
            } catch (FileNotFoundException fail) {
                try {
                    _file = File.createTempFile(_sBaseName, ".log", new File("."));
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(_file));
                    setOutputStream(bos);
                    _ps = new PrintStream(bos, true);
                } catch (IOException ex) {
                    Logger.getLogger(UserFriendlyHandler.class.getName()).warning("Unable to create custom logger file " + _file);
                }
            }
        }
        
        Level lvl = record.getLevel();
        if (lvl == Level.WARNING)
            onWarn(record);
        else if (lvl == Level.SEVERE)
            onErr(record);

        if (_sSubHeader != null && _ps != null) {
            _ps.println(_sSubHeader);
            _ps.flush();
            _sSubHeader = null;
        }

        super.publish(record);
    }

    private static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static class CustomFormatter extends Formatter {

        private String _sSubHeaderLine;

        public CustomFormatter() {
        }

        @Override
        public String getHead(Handler h) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos, true);
            ps.println(Main.VerString);
            ps.print(System.getProperty("os.name")); ps.print(' '); ps.println(System.getProperty("os.version"));
            ps.print("Java "); ps.println(System.getProperty("java.version"));
            ps.println(FORMAT.format(Calendar.getInstance().getTime()));
            ps.close();
            return baos.toString();
        }

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            if (_sSubHeaderLine != null) {
                sb.append(_sSubHeaderLine).append(LINE_SEPARATOR);
                _sSubHeaderLine = null;
            }
            Level lvl = record.getLevel();
            if (lvl == Level.WARNING) {
                sb.append("[WARN] ");
            } else if (lvl == Level.SEVERE) {
                sb.append("[ERR] ");
            } else {
                sb.append('[').append(lvl.getName()).append("] ");
            }
            if (record.getMessage() != null) {
                sb.append(record.getMessage());
            }
            sb.append(LINE_SEPARATOR);
            if (record.getThrown() != null) {
                sb.append("[^EX^] ");
	        StringWriter sw = new StringWriter();
	        PrintWriter pw = new PrintWriter(sw);
	        record.getThrown().printStackTrace(pw);
	        pw.close();
		sb.append(sw.toString());
            }
            return sb.toString();
        }
    }

}
