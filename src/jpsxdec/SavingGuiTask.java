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

package jpsxdec;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jpsxdec.SavingGuiTable.Row;
import jpsxdec.util.UserFriendlyHandler;
import jpsxdec.util.ProgressListener;
import jpsxdec.util.TaskCanceledException;
import org.jdesktop.swingworker.SwingWorker;

public class SavingGuiTask extends SwingWorker<Void, SavingGuiTask.Event_Message> 
        implements ProgressListener
{
    public static final String ALL_DONE = "alldone";

    private final ArrayList<Row> _rows;
    private final File _dir;
    private Row _currentRow;

    private Logger _errLog;
    
    public UserFriendlyHandler _handler = new UserFriendlyHandler("save") {
        protected void onWarn(LogRecord record) {
            EventQueue.invokeLater(new Event_Warning(_currentRow));
        }
        protected void onErr(LogRecord record) {
            EventQueue.invokeLater(new Event_Error(_currentRow));
        }
    };

    public SavingGuiTask(ArrayList<Row> rows, File dir) {
        _rows = rows;
        _dir = dir;
        _errLog = Logger.getLogger("save");
    }

    @Override
    protected Void doInBackground() throws Exception {
        _errLog.addHandler(_handler);
        for (Row row : _rows) {
            _currentRow = row;
            try {
                _handler.setSubheader(row._saver.getInput());
                row._saver.startSave(this, _dir);
            } catch (TaskCanceledException ex) {
                // cool
                EventQueue.invokeLater(new Event_Progress(row, SavingGuiTable.PROGRESS_CANCELED));
                break;
            } catch (Exception ex) {
                // uncool
                _errLog.log(Level.SEVERE, "Unhandled error", ex);
                EventQueue.invokeLater(new Event_Progress(row, SavingGuiTable.PROGRESS_FAILED));
                if (ex instanceof InterruptedException)
                    break;
                else
                    continue;
            } catch (Throwable ex) {
                // VERY uncool
                _errLog.log(Level.SEVERE, "Unhandled error", ex);
                EventQueue.invokeLater(new Event_Progress(row, SavingGuiTable.PROGRESS_FAILED));
                continue;
            }
            EventQueue.invokeLater(new Event_Progress(row, SavingGuiTable.PROGRESS_DONE));
        }
        firePropertyChange(ALL_DONE, null, null);
        _handler.close();
        _errLog.removeHandler(_handler);

        return null;
    }

    // -- Event types -------------------------------------------------------

    private abstract static class Event implements Runnable {
        protected final Row _row;
        public Event(Row row) { _row = row; }
        abstract public void run();
    }
    private class Event_Warning extends Event {
        public Event_Warning(Row row) { super(row); }
        public void run() { _row.incWarn(); }
    }
    private class Event_Error extends Event {
        public Event_Error(Row row) { super(row); }
        public void run() { _row.incErr(); }
    }
    private static class Event_Progress extends Event {
        private final int _val;
        public Event_Progress(Row row, int val) { super(row); _val = val; }
        public void run() { _row.setProgress(_val); }
    }
    public static class Event_Message extends Event {
        private final String _val;
        public Event_Message(Row row, String val) { super(row); _val = val; }
        public void run() { _row.setMessage(_val); }
    }


    final protected void process(List<Event_Message> chunks) {
        chunks.get(chunks.size()-1).run();
    }

    public void progressStart(String s) throws TaskCanceledException {
        publish(new Event_Message(_currentRow, s));
        setProgress(0);
    }

    public void progressStart() throws TaskCanceledException {
        if (isCancelled())
            throw new TaskCanceledException();
        EventQueue.invokeLater(new Event_Progress(_currentRow, SavingGuiTable.PROGRESS_STARTED));
    }

    public void progressEnd() throws TaskCanceledException {
        EventQueue.invokeLater(new Event_Progress(_currentRow, SavingGuiTable.PROGRESS_DONE));
    }

    public void progressUpdate(double dblPercentComplete) throws TaskCanceledException {
        if (isCancelled())
            throw new TaskCanceledException();
        EventQueue.invokeLater(new Event_Progress(_currentRow, (int)Math.round(dblPercentComplete * 100)));
    }

    public void event(String sDescription) {
        publish(new Event_Message(_currentRow, sDescription));
    }

    public boolean seekingEvent() {
        // TODO: only seek event after so many seconds
        return true;
    }

    public void info(String s) {
        // ignored
    }

    public Logger getLog() {
        return _errLog;
    }

}
