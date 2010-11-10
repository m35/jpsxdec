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

package jpsxdec;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import jpsxdec.SavingGuiTable.Row;
import jpsxdec.util.ProgressListener;
import jpsxdec.util.TaskCanceledException;
import org.jdesktop.swingworker.SwingWorker;

/**
 *
 * @author Michael
 */
public class SavingGuiTask extends SwingWorker<Void, SavingGuiTask.Event_Message> implements ProgressListener {
    public static final String ALL_DONE = "alldone";

    private final ArrayList<Row> _rows;
    private final File _dir;
    private Row _currentRow;
    private boolean _blnFirstRowError;

    private PrintStream _logStream;
    public String _sErrorLog;

    private PrintStream getLogStream() {
        if (_logStream == null) {
            try {
                String sFile = "saving" + System.currentTimeMillis() + ".log";
                _logStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(sFile)));
                _sErrorLog = sFile;
                _logStream.println(Main.VerString);
                _logStream.println("Java " + System.getProperty("java.version"));
                _logStream.println(System.getProperty("os.name") + " " + System.getProperty("os.version"));
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        return _logStream;
    }


    public SavingGuiTask(ArrayList<Row> rows, File _dir) {
        this._rows = rows;
        this._dir = _dir;
    }

    @Override
    protected Void doInBackground() throws Exception {
        for (Row row : _rows) {
            _currentRow = row;
            _blnFirstRowError = true;
            try {
                row._saver.startSave(this, _dir);
            } catch (TaskCanceledException ex) {
                // cool
                java.awt.EventQueue.invokeLater(new Event_Progress(row, -1));
                break;
            } catch (Exception ex) {
                // uncool
                java.awt.EventQueue.invokeLater(new Event_Exception(row, ex));
                if (ex instanceof InterruptedException)
                    break;
                else
                    continue;
            } catch (Throwable ex) {
                // VERY uncool
                java.awt.EventQueue.invokeLater(new Event_Exception(row, ex));
                continue;
            }
            java.awt.EventQueue.invokeLater(new Event_Done(row));
        }
        firePropertyChange(ALL_DONE, null, null);
        if (_logStream != null) {
            _logStream.close();
        }
        return null;
    }


    private abstract static class Event implements Runnable {
        protected final Row _row;

        public Event(Row row) {
            _row = row;
        }

        abstract public void run();
    }
    private class Event_Warning extends Event {
        private final Throwable _val;
        private final String _msg;
        public Event_Warning(Row row, String msg, Throwable val) {
            super(row); _msg = msg; _val = val;
            PrintStream stream = getLogStream();
            if (stream != null) {
                if (_blnFirstRowError) {
                    stream.println(_row._saver.getInput());
                    _blnFirstRowError = false;
                }
                stream.print("Warning: ");
                if (_msg != null)
                    stream.println(_msg);
                if (_val != null) {
                    _val.printStackTrace(stream);
                }
            }
        }
        public void run() {
            _row.incWarn();
        }
    }
    private class Event_Error extends Event {
        private final Throwable _val;
        private final String _msg;
        public Event_Error(Row row, String msg, Throwable val) { 
            super(row); _msg = msg; _val = val;
            PrintStream stream = getLogStream();
            if (stream != null) {
                if (_blnFirstRowError) {
                    stream.println(_row._saver.getInput());
                    _blnFirstRowError = false;
                }
                stream.print("Warning: ");
                if (_msg != null)
                    stream.println(_msg);
                if (_val != null) {
                    _val.printStackTrace(stream);
                }
            }
        }
        public void run() {
            _row.incErr();
        }
    }
    private static class Event_Progress extends Event {
        private final int _val;
        public Event_Progress(Row row, int val) { super(row); _val = val; }
        public void run() {
            _row.setProgress(_val);
        }
    }
    private class Event_Exception extends Event {
        private final Throwable _val;
        public Event_Exception(Row row, Throwable val) { 
            super(row); _val = val;
            PrintStream stream = getLogStream();
            if (stream != null) {
                if (_blnFirstRowError) {
                    stream.println(_row._saver.getInput());
                    _blnFirstRowError = false;
                }
                stream.print("Failure: ");
                _val.printStackTrace(stream);
            }
            _row.setProgress(-2);
        }
        public void run() {
        }
    }
    private static class Event_Done extends Event {
        public Event_Done(Row row) { super(row); }
        public void run() {
            _row.setProgress(100);
        }
    }
    public static class Event_Message extends Event {
        private final String _val;
        public Event_Message(Row row, String val) { super(row); _val = val; }
        public void run() {
            _row.setMessage(_val);
        }
    }


    final protected void process(List<SavingGuiTask.Event_Message> chunks) {
        chunks.get(chunks.size()-1).run();
    }

    public void progressStart(String s) throws TaskCanceledException {
        publish(new Event_Message(_currentRow, s));
        setProgress(0);
    }

    public void progressStart() throws TaskCanceledException {
        java.awt.EventQueue.invokeLater(new Event_Progress(_currentRow, 0));
    }

    public void progressEnd() throws TaskCanceledException {
        java.awt.EventQueue.invokeLater(new Event_Progress(_currentRow, 100));
    }

    public void progressUpdate(double dblPercentComplete) throws TaskCanceledException {
        if (isCancelled())
            throw new TaskCanceledException();
        java.awt.EventQueue.invokeLater(new Event_Progress(_currentRow, (int)Math.round(dblPercentComplete * 100)));
    }

    public void event(String sDescription) {
        publish(new Event_Message(_currentRow, sDescription));
    }

    public boolean seekingEvent() {
        // TODO: only seek event after so many seconds
        return true;
    }

    public void warning(String sMessage, Throwable ex) {
        java.awt.EventQueue.invokeLater(new Event_Warning(_currentRow, sMessage, ex));
    }

    public void warning(Throwable ex) {
        java.awt.EventQueue.invokeLater(new Event_Warning(_currentRow, null, ex));
    }

    public void warning(String sMessage) {
        java.awt.EventQueue.invokeLater(new Event_Warning(_currentRow, sMessage, null));
    }

    public void error(String sMessage, Throwable ex) {
        java.awt.EventQueue.invokeLater(new Event_Error(_currentRow, sMessage, ex));
    }

    public void error(Throwable ex) {
        java.awt.EventQueue.invokeLater(new Event_Warning(_currentRow, null, ex));
    }

    public void error(String sMessage) {
        java.awt.EventQueue.invokeLater(new Event_Warning(_currentRow, sMessage, null));
    }

    public void info(String s) {
        // ignored
    }

    public void more(String s) {
        // ignored
    }

}
