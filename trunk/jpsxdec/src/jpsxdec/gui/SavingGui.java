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

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedMessage;
import jpsxdec.discitems.IDiscItemSaver;

public class SavingGui extends javax.swing.JDialog implements PropertyChangeListener {

    private STATES _eState = STATES.NotStarted;
    @Nonnull
    private SavingGuiTask _saveAll;

    public SavingGui(@Nonnull java.awt.Dialog parent, @Nonnull List<IDiscItemSaver> savers,
                     @Nonnull String sCd)
    {
        super(parent, true);
        sharedConstructor(parent, savers, sCd);
    }

    public SavingGui(@Nonnull java.awt.Frame parent, @Nonnull List<IDiscItemSaver> savers,
                     @Nonnull String sCd)
    {
        super(parent, true);
        sharedConstructor(parent, savers, sCd);
    }

    private void sharedConstructor(@Nonnull java.awt.Window parent,
                                   @Nonnull List<IDiscItemSaver> savers,
                                   @Nonnull String sCd)
    {
        initComponents();

        SavingGuiTable model = new SavingGuiTable(savers, jTable1);
        // pack now so we can use the table dimensions for the perferred size
        validate();
        pack();

        jScrollPane1.setPreferredSize(new Dimension(jTable1.getWidth() + 20, jScrollPane1.getHeight()));
        validate();
        pack();

        setLocationRelativeTo(parent); // center on parent

        _saveAll = new SavingGuiTask(model._rows, sCd);
        _saveAll.addPropertyChangeListener(this);
    }

    private enum STATES {
        NotStarted,
        Running,
        Canceling,
        Ended;
    }

    public void propertyChange(@Nonnull PropertyChangeEvent evt) {
        if (SavingGuiTask.ALL_DONE.equals(evt.getPropertyName())) {
            _eState = STATES.Ended;
            _guiStartCancelCloseBtn.setText(I.GUI_CLOSE_BTN().getLocalizedMessage());
            _guiStartCancelCloseBtn.setEnabled(true);
            LocalizedMessage result;
            if (_saveAll.isCancelled())
                result = I.GUI_SAVE_STATUS_OVERALL_CANCELED(_saveAll._progressLog.getFileName());
            else
                result = I.GUI_SAVE_STATUS_OVERALL_COMPLETE(_saveAll._progressLog.getFileName());
            _guiResultLbl.setText(result.getLocalizedMessage());
        }
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

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        _guiResultLbl = new javax.swing.JLabel();
        _guiStartCancelCloseBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTable1.setFocusable(false);
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable1.setShowHorizontalLines(false);
        jTable1.setShowVerticalLines(false);
        jScrollPane1.setViewportView(jTable1);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        _guiResultLbl.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(_guiResultLbl, gridBagConstraints);

        _guiStartCancelCloseBtn.setText(I.GUI_START_BTN().getLocalizedMessage()); // NOI18N
        _guiStartCancelCloseBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiStartCancelCloseBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(_guiStartCancelCloseBtn, gridBagConstraints);

        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void _guiStartCancelCloseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiStartCancelCloseBtnActionPerformed
        switch (_eState) {
            case NotStarted:
                _eState = STATES.Running;
                _saveAll.execute();
                _guiStartCancelCloseBtn.setText(I.GUI_CANCEL_BTN().getLocalizedMessage());
                break;
            case Running:
                _eState = STATES.Canceling;
                _saveAll.cancel(false);
                _guiStartCancelCloseBtn.setEnabled(false);
                break;
            case Canceling:
                break;
            case Ended:
                // close the window
                setVisible(false);
                dispose();
                break;
        }
    }//GEN-LAST:event__guiStartCancelCloseBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        _saveAll.cancel(true);
    }//GEN-LAST:event_formWindowClosing


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel _guiResultLbl;
    private javax.swing.JButton _guiStartCancelCloseBtn;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables


}
