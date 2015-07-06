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

package jpsxdec.gui;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedMessage;
import jpsxdec.gui.SavingGuiTable.Row;
import jpsxdec.util.ProgressListenerLogger;
import jpsxdec.util.TaskCanceledException;
import jpsxdec.util.UserFriendlyLogger;
import org.jdesktop.swingworker.SwingWorker;

public class SavingGuiTask extends SwingWorker<Void, SavingGuiTask.Event_Message> 
{
    public static final String ALL_DONE = "alldone";

    @Nonnull
    private final ArrayList<Row> _rows;
    @CheckForNull
    private Row _currentRow;

    
    final ProgressListenerLogger _progressLog = new ProgressListenerLogger("save")
    {

        public void progressStart(@CheckForNull LocalizedMessage msg) throws TaskCanceledException {
            if (_currentRow != null && msg != null)
                publish(new Event_Message(_currentRow, msg));
            setProgress(0);
        }

        public void progressStart() throws TaskCanceledException {
            if (isCancelled())
                throw new TaskCanceledException();
            if (_currentRow != null)
                EventQueue.invokeLater(new Event_Progress(_currentRow, SavingGuiTable.PROGRESS_STARTED));
        }

        public void progressEnd() throws TaskCanceledException {
            if (_currentRow != null)
                EventQueue.invokeLater(new Event_Progress(_currentRow, SavingGuiTable.PROGRESS_DONE));
        }

        public void progressUpdate(double dblPercentComplete) throws TaskCanceledException {
            if (isCancelled())
                throw new TaskCanceledException();
            if (_currentRow != null)
                EventQueue.invokeLater(new Event_Progress(_currentRow,
                                                          (int)Math.round(dblPercentComplete * 100)));
        }

        public void event(@Nonnull LocalizedMessage msg) {
            if (_currentRow != null)
                publish(new Event_Message(_currentRow, msg));
        }

        public boolean seekingEvent() {
            // TODO: only seek event after so many seconds
            return true;
        }

        public void progressInfo(LocalizedMessage msg) {
            // ignored
        }
    };


    public SavingGuiTask(@Nonnull ArrayList<Row> rows, @Nonnull String sCd) {
        _rows = rows;

        _progressLog.info(sCd);

        _progressLog.setListener(new UserFriendlyLogger.OnWarnErr() {
            public void onWarn(LogRecord record) {
                if (_currentRow != null)
                    EventQueue.invokeLater(new Event_Warning(_currentRow));
            }
            public void onErr(LogRecord record) {
                if (_currentRow != null)
                    EventQueue.invokeLater(new Event_Error(_currentRow));
            }
        });
    }

    @Override
    protected Void doInBackground() throws Exception {
        for (Row row : _rows) {
            _currentRow = row;
            try {
                _progressLog.info(row._saver.getDiscItem().toString());
                row._saver.startSave(_progressLog);
            } catch (TaskCanceledException ex) {
                // cool
                EventQueue.invokeLater(new Event_Progress(row, SavingGuiTable.PROGRESS_CANCELED));
                break;
            } catch (Throwable ex) {
                // uncool
                I.GUI_UNHANDLED_ERROR().log(_progressLog, Level.SEVERE, ex);
                EventQueue.invokeLater(new Event_Progress(row, SavingGuiTable.PROGRESS_FAILED));
                if (ex instanceof InterruptedException)
                    break;
                else
                    continue;
            }
            EventQueue.invokeLater(new Event_Progress(row, SavingGuiTable.PROGRESS_DONE));
        }
        firePropertyChange(ALL_DONE, null, null);
        _progressLog.close();

        return null;
    }

    final protected void process(@Nonnull List<Event_Message> events) {
        // only process the last event
        events.get(events.size()-1).run();
    }

    // -- Event types -------------------------------------------------------

    private abstract static class Event implements Runnable {
        @Nonnull
        protected final Row _row;
        public Event(@Nonnull Row row) { _row = row; }
        abstract public void run();
    }
    private class Event_Warning extends Event {
        public Event_Warning(@Nonnull Row row) { super(row); }
        public void run() { _row.incWarn(); }
    }
    private class Event_Error extends Event {
        public Event_Error(@Nonnull Row row) { super(row); }
        public void run() { _row.incErr(); }
    }
    private static class Event_Progress extends Event {
        private final int _val;
        public Event_Progress(@Nonnull Row row, int val) { super(row); _val = val; }
        public void run() { _row.setProgress(_val); }
    }
    public static class Event_Message extends Event {
        @Nonnull
        private final LocalizedMessage _val;
        public Event_Message(@Nonnull Row row, @Nonnull LocalizedMessage val) { super(row); _val = val; }
        public void run() { _row.setMessage(_val.getLocalizedMessage()); }
    }

}
