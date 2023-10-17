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

package jpsxdec.gui;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.SwingWorker;
import jpsxdec.gui.SavingGuiTable.Row;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.i18n.log.UserFriendlyLogger;
import jpsxdec.util.TaskCanceledException;

public class SavingGuiTask extends SwingWorker<Void, SavingGuiTask.Event_Message>
        implements UserFriendlyLogger.OnWarnErr
{
    public static final String ALL_DONE = "alldone";

    @Nonnull
    private final ArrayList<Row> _rows;
    @CheckForNull
    private final File _outputDir;
    @CheckForNull
    private Row _currentRow;


    final ProgressLogger _progressLog = new ProgressLogger(I.SAVE_LOG_FILE_BASE_NAME().getLocalizedMessage())
    {

        @Override
        protected void handleProgressStart() throws TaskCanceledException {
            if (isCancelled())
                throw new TaskCanceledException();
            if (_currentRow != null)
                EventQueue.invokeLater(new Event_Progress(_currentRow, SavingGuiTable.PROGRESS_STARTED));
        }

        @Override
        protected void handleProgressEnd() throws TaskCanceledException {
            if (_currentRow != null)
                EventQueue.invokeLater(new Event_Progress(_currentRow, SavingGuiTable.PROGRESS_DONE));
        }

        @Override
        protected void handleProgressUpdate(double dblPercentComplete) throws TaskCanceledException {
            if (isCancelled())
                throw new TaskCanceledException();
            if (_currentRow != null)
                EventQueue.invokeLater(new Event_Progress(_currentRow,
                                                          (int)Math.round(dblPercentComplete * 100)));
        }

        @Override
        public void event(@Nonnull ILocalizedMessage msg) {
            if (_currentRow != null)
                publish(new Event_Message(_currentRow, msg));
        }

        @Override
        public boolean isSeekingEvent() {
            // TODO: only seek event after so many seconds
            return true;
        }
    };


    public SavingGuiTask(@Nonnull ArrayList<Row> rows, @Nonnull String sCd,
                         @CheckForNull File outputDir)
    {
        _rows = rows;
        _outputDir = outputDir;
        _progressLog.log(Level.INFO, new UnlocalizedMessage(sCd));
        _progressLog.setListener(this);
    }

    @Override
    public void onWarn(@Nonnull ILocalizedMessage msg) {
        if (_currentRow != null)
            EventQueue.invokeLater(new Event_Warning(_currentRow));
    }
    @Override
    public void onErr(@Nonnull ILocalizedMessage msg) {
        if (_currentRow != null)
            EventQueue.invokeLater(new Event_Error(_currentRow));
    }

    @Override
    protected Void doInBackground() {
        for (Row row : _rows) {
            _currentRow = row;
            try {
                _progressLog.log(Level.INFO, new UnlocalizedMessage(row._builder.getDiscItem().toString()));
                row._builder.startSave(_progressLog, _outputDir);
            } catch (TaskCanceledException ex) {
                // cool
                EventQueue.invokeLater(new Event_Progress(row, SavingGuiTable.PROGRESS_CANCELED));
                break;
            } catch (LoggedFailure ex) {
                // uncool
                EventQueue.invokeLater(new Event_Progress(row, SavingGuiTable.PROGRESS_FAILED));
                continue;
            } catch (Throwable ex) {
                // uh oh...
                _progressLog.log(Level.SEVERE, I.GUI_UNHANDLED_ERROR(), ex);
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

    @Override
    final protected void process(@Nonnull List<Event_Message> events) {
        // only process the last event
        // XXX TODO BUG This can skip the 'Done' event so the last operation is left in the cell
        events.get(events.size()-1).run();
    }

    // -- Event types -------------------------------------------------------

    private abstract static class Event implements Runnable {
        @Nonnull
        protected final Row _row;
        public Event(@Nonnull Row row) { _row = row; }
        @Override
        abstract public void run();
    }
    private class Event_Warning extends Event {
        public Event_Warning(@Nonnull Row row) { super(row); }
        @Override
        public void run() { _row.incWarn(); }
    }
    private class Event_Error extends Event {
        public Event_Error(@Nonnull Row row) { super(row); }
        @Override
        public void run() { _row.incErr(); }
    }
    private static class Event_Progress extends Event {
        private final int _val;
        public Event_Progress(@Nonnull Row row, int val) { super(row); _val = val; }
        @Override
        public void run() { _row.setProgress(_val); }
    }
    public static class Event_Message extends Event {
        @Nonnull
        private final ILocalizedMessage _val;
        public Event_Message(@Nonnull Row row, @Nonnull ILocalizedMessage val) { super(row); _val = val; }
        @Override
        public void run() { _row.setMessage(_val.getLocalizedMessage()); }
    }


}
