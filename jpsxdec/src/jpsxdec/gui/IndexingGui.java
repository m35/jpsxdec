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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.i18n.log.UserFriendlyLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.TaskCanceledException;


public class IndexingGui extends javax.swing.JDialog implements PropertyChangeListener {

    private static final Logger LOG = Logger.getLogger(IndexingGui.class.getName());

    /** The task to perform. */
    @Nonnull
    private ProgressGuiTask _task;
    /** Holds any exception thrown by the task. */
    @CheckForNull
    private Throwable _exception;

    private int _iWarningCount;
    private int _iErrorCount;

    @CheckForNull
    public DiscIndex _index;
    @Nonnull
    public ICdSectorReader _cd;

    @Nonnull
    private State _eState = State.NOT_STARTED;

    private enum State {
        NOT_STARTED,
        RUNNING,
        CANCELING,
        ENDED;
    }



    /** Creates new form Progress */
    public IndexingGui(@Nonnull java.awt.Dialog parent, @Nonnull ICdSectorReader cd) {
        super(parent, true);
        sharedConstructor(parent, cd);
    }

    /** Creates new form Progress */
    public IndexingGui(@Nonnull java.awt.Frame parent, @Nonnull ICdSectorReader cd) {
        super(parent, true);
        sharedConstructor(parent, cd);
    }

    private void sharedConstructor(@Nonnull java.awt.Window parent, @Nonnull ICdSectorReader cd)
    {
        initComponents();

        // lock the current dialog size before we change anything
        setMinimumSize(new Dimension(getWidth(), getHeight()));
        setPreferredSize(getMinimumSize());

        setLocationRelativeTo(parent); // center on parent

        _cd = cd;
        _guiItemName.setText(cd.getSourceFile().getPath());
        _guiResultLbl.setText("");

        _task = new ProgressGuiTask();
        _task.addPropertyChangeListener(this);
        _guiCancelBtnActionPerformed(null);
    }



    @Override
    public void propertyChange(@Nonnull PropertyChangeEvent evt) {
        // update the progress bar
        if (ProgressGuiTask.PROGRESS_VALUE.equals(evt.getPropertyName())) {
            _guiProgress.setValue((Integer)evt.getNewValue());
        } else if (ProgressGuiTask.EXCEPTION.equals(evt.getPropertyName()) ) {
            // fatal/unhandled exception
            // we know getNewValue() != null since we created the event
            _exception = (Throwable)evt.getNewValue();
            _exception.printStackTrace(System.err); // debug
            JOptionPane.showMessageDialog(this, _exception.toString(),
                    I.GUI_INDEX_EXCEPTION_DIALOG_TITLE().getLocalizedMessage(),
                    JOptionPane.ERROR_MESSAGE);
            taskComplete();
        } else if (ProgressGuiTask.DONE.equals(evt.getPropertyName()) ) {
            taskComplete();
        }
    }

    private void taskComplete() {
        _eState = State.ENDED;
        _guiCancelBtn.setEnabled(true);
        _guiCancelBtn.setText(I.GUI_CLOSE_BTN().getLocalizedMessage());
        if (wasCanceled()) {
            _guiResultLbl.setText(I.GUI_INDEX_RESULT_CANCELED().getLocalizedMessage());
            _guiResultLbl.setForeground(Color.orange);
        } else if (getException() != null) {
            _guiResultLbl.setText(I.GUI_INDEX_RESULT_FAILURE(_task.__progressLog.getFileName()).getLocalizedMessage());
            _guiResultLbl.setForeground(Color.red);
        } else if (_iWarningCount > 0 || _iErrorCount > 0) {
            _guiResultLbl.setText(I.GUI_INDEX_RESULT_OK_MSGS(_task.__progressLog.getFileName()).getLocalizedMessage());
        } else {
            _guiResultLbl.setText(I.GUI_INDEX_RESULT_SUCCESS().getLocalizedMessage());
        }
    }


    /** Returns if the cancel button was pressed before the task completed. */
    public boolean wasCanceled() {
        return _task.isCancelled();
    }

    /** Returns the exception thrown by the task (or null if none). */
    public @CheckForNull Throwable getException() {
        return _exception;
    }

    public @CheckForNull DiscIndex getIndex() {
        return _index;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        _guiMarginPanel = new javax.swing.JPanel();
        _guiSavingLbl = new javax.swing.JLabel();
        _guiItemName = new javax.swing.JLabel();
        _guiProgressDescription = new javax.swing.JLabel();
        _guiProgress = new javax.swing.JProgressBar();
        _guiBottomPanel = new javax.swing.JPanel();
        _guiWarningsLbl = new javax.swing.JLabel();
        _guiWarningsCount = new javax.swing.JLabel();
        _guiErrorsLbl = new javax.swing.JLabel();
        _guiErrorsCount = new javax.swing.JLabel();
        _guiCancelBtn = new javax.swing.JButton();
        _guiResultLbl = new javax.swing.JLabel();

        setTitle(I.GUI_INDEX_TITLE().getLocalizedMessage()); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        _guiMarginPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        _guiMarginPanel.setLayout(new java.awt.GridBagLayout());

        _guiSavingLbl.setText(I.GUI_INDEXING_LABEL().getLocalizedMessage()); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        _guiMarginPanel.add(_guiSavingLbl, gridBagConstraints);

        _guiItemName.setText("1123456789012345678901234567890123456789012345678901234567890234567890");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 0);
        _guiMarginPanel.add(_guiItemName, gridBagConstraints);

        _guiProgressDescription.setText("Doing something");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(17, 0, 5, 0);
        _guiMarginPanel.add(_guiProgressDescription, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        _guiMarginPanel.add(_guiProgress, gridBagConstraints);

        _guiBottomPanel.setLayout(new java.awt.GridBagLayout());

        _guiWarningsLbl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        _guiWarningsLbl.setText(I.GUI_INDEX_WARNINGS_LABEL().getLocalizedMessage()); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        _guiBottomPanel.add(_guiWarningsLbl, gridBagConstraints);

        _guiWarningsCount.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        _guiBottomPanel.add(_guiWarningsCount, gridBagConstraints);

        _guiErrorsLbl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        _guiErrorsLbl.setText(I.GUI_INDEX_ERRORS_LABEL().getLocalizedMessage()); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        _guiBottomPanel.add(_guiErrorsLbl, gridBagConstraints);

        _guiErrorsCount.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        _guiBottomPanel.add(_guiErrorsCount, gridBagConstraints);

        _guiCancelBtn.setText(I.GUI_START_BTN().getLocalizedMessage()); // NOI18N
        _guiCancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiCancelBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        _guiBottomPanel.add(_guiCancelBtn, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        _guiMarginPanel.add(_guiBottomPanel, gridBagConstraints);

        _guiResultLbl.setText(I.GUI_INDEX_RESULT_SUCCESS().getLocalizedMessage()); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        _guiMarginPanel.add(_guiResultLbl, gridBagConstraints);

        getContentPane().add(_guiMarginPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void _guiCancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiCancelBtnActionPerformed
        switch (_eState) {
            case NOT_STARTED:
                _guiCancelBtn.setText(I.GUI_CANCEL_BTN().getLocalizedMessage());
                _task.execute();
                _eState = State.RUNNING;
                break;
            case RUNNING:
                _task.cancel(false);
                _guiCancelBtn.setEnabled(false);
                // need to wait for task to finish
                _eState = State.CANCELING;
                break;
            case ENDED:
                setVisible(false);
                dispose();
                break;
        }

    }//GEN-LAST:event__guiCancelBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        _task.cancel(true);
    }//GEN-LAST:event_formWindowClosing


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel _guiBottomPanel;
    private javax.swing.JButton _guiCancelBtn;
    private javax.swing.JLabel _guiErrorsCount;
    private javax.swing.JLabel _guiErrorsLbl;
    private javax.swing.JLabel _guiItemName;
    private javax.swing.JPanel _guiMarginPanel;
    private javax.swing.JProgressBar _guiProgress;
    private javax.swing.JLabel _guiProgressDescription;
    private javax.swing.JLabel _guiResultLbl;
    private javax.swing.JLabel _guiSavingLbl;
    private javax.swing.JLabel _guiWarningsCount;
    private javax.swing.JLabel _guiWarningsLbl;
    // End of variables declaration//GEN-END:variables

    // run in separate thread

    private class ProgressGuiTask extends SwingWorker<Void, ILocalizedMessage>
        implements UserFriendlyLogger.OnWarnErr
    {

        public static final String PROGRESS_VALUE = "progress";
        public static final String EXCEPTION = "exception";
        public static final String DONE = "done";

        private final ProgressLogger __progressLog = new ProgressLogger("index") {
            @Override
            protected void handleProgressStart() throws TaskCanceledException {
                if (isCancelled())
                    throw new TaskCanceledException();
                setProgress(0);
            }

            @Override
            protected void handleProgressEnd() throws TaskCanceledException {
                setProgress(100);
            }

            @Override
            protected void handleProgressUpdate(double dblPercentComplete) throws TaskCanceledException {
                if (isCancelled())
                    throw new TaskCanceledException();
                setProgress((int)Math.round(dblPercentComplete * 100));
            }

            @Override
            public void event(@Nonnull ILocalizedMessage msg) {
                publish(msg);
            }

            @Override
            public boolean isSeekingEvent() {
                // TODO: only seek event after so many seconds
                return true;
            }
        };

        public ProgressGuiTask() {
            __progressLog.setListener(this);
            __progressLog.log(Level.INFO, I.CMD_GUI_INDEXING(_cd.toString()));
        }

        @Override
        public void onWarn(@Nonnull ILocalizedMessage msg) {
            EventQueue.invokeLater(new ExceptionLater(true));
        }
        @Override
        public void onErr(@Nonnull ILocalizedMessage msg) {
            EventQueue.invokeLater(new ExceptionLater(false));
        }

        @Override
        final protected void process(@Nonnull List<ILocalizedMessage> chunks) {
            _guiProgressDescription.setText(chunks.get(chunks.size()-1).getLocalizedMessage());
        }

        @Override
        final protected @CheckForNull Void doInBackground() {
            try {
                _index = new DiscIndex(_cd, __progressLog);
            } catch (TaskCanceledException ex) {
                // cool
            } catch (Throwable ex) {
                // uncool
                _exception = ex;
                firePropertyChange(EXCEPTION, null, ex); // calls IndexingGui#propertyChange()
                __progressLog.log(Level.SEVERE, I.GUI_UNHANDLED_ERROR(), ex);
                return null;
            }
            firePropertyChange(DONE, null, null); // calls IndexingGui#propertyChange()
            __progressLog.close();
            return null;
        }



        private class ExceptionLater implements Runnable {
            private final boolean __blnWarn;
            public ExceptionLater(boolean blnWarn) {
                __blnWarn = blnWarn;
            }
            @Override
            public void run() {
                if (__blnWarn) {
                    _iWarningCount++;
                    _guiWarningsCount.setText(String.valueOf(_iWarningCount));
                    _guiWarningsCount.setForeground(Color.red);
                } else {
                    _iErrorCount++;
                    _guiErrorsCount.setText(String.valueOf(_iErrorCount));
                    _guiErrorsCount.setForeground(Color.red);
                }
            }
        }

    }

}
