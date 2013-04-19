/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.ProgressListenerLogger;
import jpsxdec.util.TaskCanceledException;
import jpsxdec.util.UserFriendlyLogger;
import org.jdesktop.swingworker.SwingWorker;


public class IndexingGui extends javax.swing.JDialog implements PropertyChangeListener {

    private static final Logger LOG = Logger.getLogger(IndexingGui.class.getName());

    /** The task to perform. */
    private ProgresGuiTask _task;
    /** Holds any exception thrown by the task. */
    private Throwable _exception;

    private int _iWarningCount;
    private int _iErrorCount;

    public DiscIndex _index;
    public CdFileSectorReader _cd;

    private State _eState = State.NOT_STARTED;

    private enum State {
        NOT_STARTED,
        RUNNING,
        CANCELING,
        ENDED;
    }



    /** Creates new form Progress */
    public IndexingGui(java.awt.Dialog parent, CdFileSectorReader cd) {
        super(parent, true);
        sharedConstructor(parent, cd);
    }

    /** Creates new form Progress */
    public IndexingGui(java.awt.Frame parent, CdFileSectorReader cd) {
        super(parent, true);
        sharedConstructor(parent, cd);
    }

    private void sharedConstructor(java.awt.Window parent, CdFileSectorReader cd)
    {
        initComponents();

        // lock the current dialog size before we change anything
        setMinimumSize(new Dimension(getWidth(), getHeight()));
        setPreferredSize(getMinimumSize());

        setLocationRelativeTo(parent); // center on parent

        _cd = cd;
        _guiItemName.setText(cd.getSourceFile().getPath());
        _guiResultLbl.setText("");

        _task = new ProgresGuiTask();
        _task.addPropertyChangeListener(this);
        _guiCancelBtnActionPerformed(null);
    }



    public void propertyChange(PropertyChangeEvent evt) {
        // update the progress bar
        if (ProgresGuiTask.PROGRESS_VALUE.equals(evt.getPropertyName())) {
            _guiProgress.setValue((Integer)evt.getNewValue());
        } else if (ProgresGuiTask.EXCEPTION.equals(evt.getPropertyName()) ) {
            // fatal/unhandled exception
            _exception = (Throwable)evt.getNewValue();
            JOptionPane.showMessageDialog(this, _exception.toString(), "Exception", JOptionPane.ERROR_MESSAGE);
            _exception.printStackTrace(System.err); // debug
            taskComplete();
        } else if (ProgresGuiTask.DONE.equals(evt.getPropertyName()) ) {
            taskComplete();
        }
    }

    private void taskComplete() {
        _eState = State.ENDED;
        _guiCancelBtn.setEnabled(true);
        _guiCancelBtn.setText("Close");
        if (wasCanceled()) {
            _guiResultLbl.setText("Canceled");
            _guiResultLbl.setForeground(Color.orange);
        } else if (getException() != null) {
            _guiResultLbl.setText("Failure - See " + _task.__progressLog.getFileName() + " for details");
            _guiResultLbl.setForeground(Color.red);
        } else if (_iWarningCount > 0 || _iErrorCount > 0) {
            _guiResultLbl.setText("Success with messages - See " + _task.__progressLog.getFileName() + " for details");
        } else {
            _guiResultLbl.setText("Success!");
        }
    }


    /** Returns if the cancel button was pressed before the task completed. */
    public boolean wasCanceled() {
        return _task.isCancelled();
    }

    /** Returns the exception thrown by the task (or null if none). */
    public Throwable getException() {
        return _exception;
    }

    public DiscIndex getIndex() {
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

        setTitle("Progress...");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        _guiMarginPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        _guiMarginPanel.setLayout(new java.awt.GridBagLayout());

        _guiSavingLbl.setText("Indexing:");
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
        _guiWarningsLbl.setText("Warnings:");
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
        _guiErrorsLbl.setText("Errors:");
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

        _guiCancelBtn.setText("Start");
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

        _guiResultLbl.setText("Success!");
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
                _guiCancelBtn.setText("Cancel");
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

    private class ProgresGuiTask extends SwingWorker<Void, String>
    {

        public static final String PROGRESS_VALUE = "progress";
        public static final String EXCEPTION = "exception";
        public static final String DONE = "done";

        private final ProgressListenerLogger __progressLog = new ProgressListenerLogger("index") {
            public void progressStart(String s) throws TaskCanceledException {
                if (isCancelled())
                    throw new TaskCanceledException();
                publish(s);
                setProgress(0);
            }

            public void progressStart() throws TaskCanceledException {
                if (isCancelled())
                    throw new TaskCanceledException();
                setProgress(0);
            }

            public void progressEnd() throws TaskCanceledException {
                setProgress(100);
            }

            public void progressUpdate(double dblPercentComplete) throws TaskCanceledException {
                if (isCancelled())
                    throw new TaskCanceledException();
                setProgress((int)Math.round(dblPercentComplete * 100));
            }

            public void event(String sDescription) {
                publish(sDescription);
            }

            public boolean seekingEvent() {
                // TODO: only seek event after so many seconds
                return true;
            }

            public void progressInfo(String s) {
            }
        };

        public ProgresGuiTask() {
            __progressLog.setListener(new UserFriendlyLogger.OnWarnErr() {
                public void onWarn(LogRecord record) {
                    EventQueue.invokeLater(new ExceptionLater(true));
                }
                public void onErr(LogRecord record) {
                    EventQueue.invokeLater(new ExceptionLater(false));
                }
            });
            
            __progressLog.setHeader(1, "Indexing " + _cd.getSourceFile().toString());
        }

        @Override
        final protected void process(List<String> chunks) {
            _guiProgressDescription.setText(chunks.get(chunks.size()-1));
        }

        @Override
        final protected Void doInBackground() {
            try {
                _index = new DiscIndex(_cd, __progressLog);
            } catch (TaskCanceledException ex) {
                // cool
            } catch (Throwable ex) {
                // uncool
                _exception = ex;
                firePropertyChange(EXCEPTION, null, ex); // calls IndexingGui#propertyChange()
                LOG.log(Level.SEVERE, "Unhandled error", ex);
                __progressLog.log(Level.SEVERE, "Unhandled error", ex);
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
