/*
 * LainTools: PSX Serial Experiments Lain Hacking and Translation Tools
 * Copyright (C) 2011  Michael Sabin
 *
 * Redistribution and use of the LainTools code or any derivative works are
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

package texter.gui;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import laintools.ReplaceAudioImages;
import texter.models.RenderOptions;

public class Gui extends javax.swing.JFrame {
    
    private final ArrayList<RenderOptions> _guiModels = new ArrayList<RenderOptions>();
    private static final File DIR = new File("Ini");

    private class MediaListModel extends AbstractListModel implements PropertyChangeListener {

        public int getSize() {
            return _guiModels.size();
        }

        public Object getElementAt(int index) {
            return _guiModels.get(index);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            this.fireContentsChanged(this, 0, getSize());
        }
    }
    private final MediaListModel _listModel = new MediaListModel();

    /** Creates new form Gui */
    public Gui(String path) {
        // use the system's L&F if available (for great justice!)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        initComponents();
        this.setLocationRelativeTo(null); // center on screen

        System.out.println("Loading .ini files");
        
        for (ReplaceAudioImages.NodeInfo node : ReplaceAudioImages.NODES_WITH_IMAGES) {
            RenderOptions ro;
            try {
                ro = new RenderOptions(DIR, node.name(), node.sites());
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, ex);
                ro = new RenderOptions(node.name(), node.sites());
            }
            ro.addPropertyChangeListener("toString", _listModel);
            _guiModels.add(ro);
        }

        _gList.setModel(_listModel);

        _gList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;

                int i = _gList.getSelectedIndex();
                if (i < 0)
                    return;
                setModel(_guiModels.get(i));
            }
        });
    }

    private RenderOptions _selection;

    private void setModel(RenderOptions ro) {
        _selection = ro;
        _gSizeSpin.setModel(_selection.fontSize);
        _gText.setDocument(_selection.text);
        _gFont.setDocument(_selection.fontName);
        _gMask.setModel(_selection.overlayMask);
        _gOptions1.setOptions(_selection.getImgOpt(0));
        _gOptions2.setOptions(_selection.getImgOpt(1));
        _gOptions3.setOptions(_selection.getImgOpt(2));

    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        _gListPanel = new javax.swing.JPanel();
        _gListScroll = new javax.swing.JScrollPane();
        _gList = new javax.swing.JList();
        _gRevert = new javax.swing.JButton();
        _gSave = new javax.swing.JButton();
        _gSaveRender = new javax.swing.JButton();
        _gTextScroll = new javax.swing.JScrollPane();
        _gText = new javax.swing.JTextArea();
        _gImgOptPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        _gFontBtn = new javax.swing.JButton();
        _gFont = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        _gSizeLbl = new javax.swing.JLabel();
        _gSizeSpin = new javax.swing.JSpinner();
        jPanel4 = new javax.swing.JPanel();
        _gMaskLbl = new javax.swing.JLabel();
        _gMask = new javax.swing.JComboBox();
        _gRender = new javax.swing.JButton();
        _gOptions1 = new texter.gui.Options();
        _gOptions2 = new texter.gui.Options();
        _gOptions3 = new texter.gui.Options();
        _gAddOption = new javax.swing.JButton();
        _gRemoveOption = new javax.swing.JButton();
        _gImgScroll = new javax.swing.JScrollPane();
        _gRenderImage = new texter.gui.ImagePanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Texterizer");
        setMinimumSize(new java.awt.Dimension(800, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        _gListPanel.setLayout(new java.awt.GridBagLayout());

        _gList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        _gListScroll.setViewportView(_gList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        _gListPanel.add(_gListScroll, gridBagConstraints);

        _gRevert.setText("Revert");
        _gRevert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _gRevertActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        _gListPanel.add(_gRevert, gridBagConstraints);

        _gSave.setText("Save");
        _gSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _gSaveActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        _gListPanel.add(_gSave, gridBagConstraints);

        _gSaveRender.setText("Export Renders");
        _gSaveRender.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _gSaveRenderActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        _gListPanel.add(_gSaveRender, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipady = 255;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 5);
        getContentPane().add(_gListPanel, gridBagConstraints);

        _gText.setColumns(1);
        _gText.setLineWrap(true);
        _gText.setRows(5);
        _gText.setWrapStyleWord(true);
        _gTextScroll.setViewportView(_gText);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 258;
        gridBagConstraints.ipady = 347;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 10, 5);
        getContentPane().add(_gTextScroll, gridBagConstraints);

        _gImgOptPanel.setLayout(new javax.swing.BoxLayout(_gImgOptPanel, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        _gFontBtn.setText("Font");
        _gFontBtn.setAlignmentY(0.0F);
        _gFontBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _gFontBtnActionPerformed(evt);
            }
        });
        jPanel3.add(_gFontBtn);

        _gFont.setAlignmentY(0.0F);
        _gFont.setMaximumSize(new java.awt.Dimension(2147483647, 40));
        jPanel3.add(_gFont);

        _gImgOptPanel.add(jPanel3);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        _gSizeLbl.setText("Size");
        jPanel2.add(_gSizeLbl);
        jPanel2.add(_gSizeSpin);

        _gImgOptPanel.add(jPanel2);

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.LINE_AXIS));

        _gMaskLbl.setText("Mask");
        jPanel4.add(_gMaskLbl);

        _gMask.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jPanel4.add(_gMask);

        _gImgOptPanel.add(jPanel4);

        _gRender.setText("Render");
        _gRender.setAlignmentX(0.5F);
        _gRender.setMaximumSize(new java.awt.Dimension(500, 23));
        _gRender.setPreferredSize(new java.awt.Dimension(200, 30));
        _gRender.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _gRenderActionPerformed(evt);
            }
        });
        _gImgOptPanel.add(_gRender);
        _gImgOptPanel.add(_gOptions1);
        _gImgOptPanel.add(_gOptions2);
        _gImgOptPanel.add(_gOptions3);

        _gAddOption.setText("Add image option");
        _gAddOption.setAlignmentX(0.5F);
        _gAddOption.setMaximumSize(new java.awt.Dimension(500, 23));
        _gAddOption.setPreferredSize(new java.awt.Dimension(200, 30));
        _gAddOption.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _gAddOptionActionPerformed(evt);
            }
        });
        _gImgOptPanel.add(_gAddOption);

        _gRemoveOption.setText("Remove image option");
        _gRemoveOption.setAlignmentX(0.5F);
        _gRemoveOption.setMaximumSize(new java.awt.Dimension(500, 23));
        _gRemoveOption.setPreferredSize(new java.awt.Dimension(200, 30));
        _gRemoveOption.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _gRemoveOptionActionPerformed(evt);
            }
        });
        _gImgOptPanel.add(_gRemoveOption);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.ipady = 82;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 10, 5);
        getContentPane().add(_gImgOptPanel, gridBagConstraints);

        _gImgScroll.setMinimumSize(new java.awt.Dimension(320, 240));

        _gRenderImage.setMinimumSize(new java.awt.Dimension(320, 720));
        _gRenderImage.setPreferredSize(new java.awt.Dimension(320, 720));
        _gRenderImage.setLayout(new java.awt.BorderLayout());
        _gImgScroll.setViewportView(_gRenderImage);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 10, 10);
        getContentPane().add(_gImgScroll, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void _gRenderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__gRenderActionPerformed
        if (_selection == null)
            return;
        BufferedImage bi = _selection.renderTest();
        if (bi == null)
            return;

        _gRenderImage.setBackgroundImage(bi);
        

}//GEN-LAST:event__gRenderActionPerformed

    private void _gSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__gSaveActionPerformed
        if (_selection == null)
            return;
        try {
            _selection.save(DIR);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex);
        }
    }//GEN-LAST:event__gSaveActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        ArrayList<RenderOptions> dirty = new ArrayList<RenderOptions>();
        for (RenderOptions renderOptions : _guiModels) {
            if (renderOptions.dirty())
                dirty.add(renderOptions);
        }


        if (!dirty.isEmpty()) {
                int i = JOptionPane.showConfirmDialog(this, "Save the "+dirty.size()+" changed items before closing?", "Changed", JOptionPane.YES_NO_CANCEL_OPTION);
                switch (i) {
                    case JOptionPane.YES_OPTION:
                        for (RenderOptions renderOptions : dirty) {
                            try {
                                renderOptions.save(DIR);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(this, ex);
                            }
                        }
                        // pass through
                    case JOptionPane.NO_OPTION:
                        setVisible(false);
                        dispose();
                        System.exit(0);
                        break;
                }
        } else {
            setVisible(false);
            dispose();
            System.exit(0);
        }
    }//GEN-LAST:event_formWindowClosing

    private void _gRevertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__gRevertActionPerformed
        if (_selection == null)
            return;
        if (_selection.dirty()) {
            int i = JOptionPane.showConfirmDialog(this, "Revert " + _selection + "?", "Orly?", JOptionPane.OK_CANCEL_OPTION);
            if (i != JOptionPane.OK_OPTION)
                return;
            try {
                _selection.load(DIR);
                setModel(_selection);
            } catch (IOException ex) {
                Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, ex);
            }
        }
    }//GEN-LAST:event__gRevertActionPerformed

    private void _gSaveRenderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__gSaveRenderActionPerformed
        if (_selection == null)
            return;
        try {
            ArrayList<String> itemsSaved = new ArrayList<String>();
            for (int iIdx : _gList.getSelectedIndices()) {
                RenderOptions item = _guiModels.get(iIdx);
                for (int iSite = 0; iSite < item.sites().length(); iSite++) {
                    item.renderSave(new File("Site"+item.sites().charAt(iSite)));
                }
                itemsSaved.add(item.mediaName);
            }
            JOptionPane.showMessageDialog(this, "Saved " + itemsSaved.size() + " renders: " + itemsSaved);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }

    }//GEN-LAST:event__gSaveRenderActionPerformed

    private void _gFontBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__gFontBtnActionPerformed

        if (_selection == null)
            return;

        String sfont = FontChooser.showFontNameChooser(this);
        if (sfont != null) _selection.setFontName(sfont);

    }//GEN-LAST:event__gFontBtnActionPerformed

    private void _gAddOptionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__gAddOptionActionPerformed
        if (_selection == null)
            return;
        _selection.addImageOption();
        _gOptions2.setOptions(_selection.getImgOpt(1));
        _gOptions3.setOptions(_selection.getImgOpt(2));

    }//GEN-LAST:event__gAddOptionActionPerformed

    private void _gRemoveOptionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__gRemoveOptionActionPerformed
        if (_selection == null)
            return;
        _selection.removeImageOption();
        _gOptions2.setOptions(_selection.getImgOpt(1));
        _gOptions3.setOptions(_selection.getImgOpt(2));

    }//GEN-LAST:event__gRemoveOptionActionPerformed
    

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Gui("..\\texterwork\\cou001.ini").setVisible(true);
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton _gAddOption;
    private javax.swing.JTextField _gFont;
    private javax.swing.JButton _gFontBtn;
    private javax.swing.JPanel _gImgOptPanel;
    private javax.swing.JScrollPane _gImgScroll;
    private javax.swing.JList _gList;
    private javax.swing.JPanel _gListPanel;
    private javax.swing.JScrollPane _gListScroll;
    private javax.swing.JComboBox _gMask;
    private javax.swing.JLabel _gMaskLbl;
    private texter.gui.Options _gOptions1;
    private texter.gui.Options _gOptions2;
    private texter.gui.Options _gOptions3;
    private javax.swing.JButton _gRemoveOption;
    private javax.swing.JButton _gRender;
    private texter.gui.ImagePanel _gRenderImage;
    private javax.swing.JButton _gRevert;
    private javax.swing.JButton _gSave;
    private javax.swing.JButton _gSaveRender;
    private javax.swing.JLabel _gSizeLbl;
    private javax.swing.JSpinner _gSizeSpin;
    private javax.swing.JTextArea _gText;
    private javax.swing.JScrollPane _gTextScroll;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    // End of variables declaration//GEN-END:variables
    

    
}
