/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * Gui.java
 */

package jpsxdec;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import jpsxdec.Progress.SimpleWorker.TaskInfo;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.media.MediaHandler;
import jpsxdec.media.PSXMedia;
import jpsxdec.media.PSXMediaFF9;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.nativeclass.VideoForWindows;
import jpsxdec.util.IProgressListener;

public class Gui extends javax.swing.JFrame {
    private final File INI_FILE = new File("jpsxdec.ini");
    
    private CDSectorReader m_oCD;
    private MediaHandler m_oMediaList;
    private String m_sIndexFile;
    private File m_oLastBrowseFolder;
    private File m_oLastSaveFolder;
   
    /** Creates new form Gui */
    public Gui() {
        initComponents();
        
        // use the system's L&F if available (for great justice!)
        try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
                ex.printStackTrace();
        }
        // center the gui
        this.setLocationRelativeTo(null);
        
        guiMediaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        if (INI_FILE.exists()) {
            try {
                BufferedReader oReader = new BufferedReader(new FileReader(INI_FILE));
                m_oLastBrowseFolder = new File(oReader.readLine());
                m_oLastSaveFolder = new File(oReader.readLine());
                oReader.close();
            } catch (IOException ex) {
                Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
    }
    
    private boolean VerifyInputFile() {
        if (new File(guiInputFile.getText()).exists()) {
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Input file does not exist");
            return false;
        }
            
    }

    private void PopulateList() {
        guiMediaList.setModel(m_oMediaList);
    }

    private void DisableIndexButtons() {
        guiGenerateIdx.setEnabled(false);
        guiLoadIdx.setEnabled(false);
        guiBrowseBtn.setEnabled(false);
        guiInputFile.setEnabled(false);
    }
    
    private void PromptToSaveIndex() {
        int iOpt = JOptionPane.showConfirmDialog(this, 
                                      "Would you like to save the index?", 
                                      "Indexing complete", 
                                      JOptionPane.YES_NO_OPTION);
        if (iOpt == 0) {
            FileDialog fd = new FileDialog(this, "Save index", FileDialog.SAVE);
            if (m_oLastBrowseFolder != null && m_oLastBrowseFolder.exists())
                fd.setDirectory(m_oLastBrowseFolder.getPath());
            fd.setVisible(true);
            m_oLastBrowseFolder = new File(fd.getDirectory());
            if (fd.getFile() != null) {
                m_sIndexFile = new File(fd.getDirectory(), fd.getFile()).getPath();
                // save the index
                try {
                    PrintStream oPrinter = new PrintStream(m_sIndexFile);
                    m_oMediaList.SerializeMediaList(oPrinter);
                    oPrinter.close();
                    guiIndexFile.setText(m_sIndexFile);
                } catch (IOException ex) {
                    Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    
    private void DecodeMediaItem(final PSXMedia oMedia, String sFile) {

        
        Progress oSaveTask = new Progress(this, "Saving " + oMedia.toString(), new Progress.SimpleWorker<Void>() {
            @Override
            Void task(final TaskInfo task) {
                return null;
            }
        });
            
        oSaveTask.setVisible(true);
        if (oSaveTask.threwException()) {

        } else {
            if (oSaveTask.wasCanceled()) {

            }
        }
        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        guiInputFile = new javax.swing.JTextField();
        guiMediaListPanel = new javax.swing.JScrollPane();
        guiMediaList = new javax.swing.JList();
        guiBrowseBtn = new javax.swing.JButton();
        guiGenerateIdx = new javax.swing.JButton();
        guiLoadIdx = new javax.swing.JButton();
        guiIndexFileLbl = new javax.swing.JLabel();
        guiIndexFile = new javax.swing.JLabel();
        guiSave = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("jPSXdec");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        guiMediaList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                guiMediaListValueChanged(evt);
            }
        });
        guiMediaListPanel.setViewportView(guiMediaList);

        guiBrowseBtn.setText("Browse...");
        guiBrowseBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiBrowseBtnActionPerformed(evt);
            }
        });

        guiGenerateIdx.setText("Generate Index");
        guiGenerateIdx.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiGenerateIdxActionPerformed(evt);
            }
        });

        guiLoadIdx.setText("Load Index");
        guiLoadIdx.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiLoadIdxActionPerformed(evt);
            }
        });

        guiIndexFileLbl.setText("Index File:");
        guiIndexFileLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                guiIndexFileLblMouseClicked(evt);
            }
        });

        guiIndexFile.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        guiIndexFile.setText("None");
        guiIndexFile.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        guiIndexFile.setFocusable(false);

        guiSave.setText("Save...");
        guiSave.setEnabled(false);
        guiSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiSaveActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(guiMediaListPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, guiLoadIdx, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(guiGenerateIdx, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, guiSave, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .add(layout.createSequentialGroup()
                        .add(guiInputFile, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(guiBrowseBtn))
                    .add(layout.createSequentialGroup()
                        .add(guiIndexFileLbl)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(guiIndexFile, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 339, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(guiBrowseBtn)
                    .add(guiInputFile, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(guiIndexFileLbl)
                    .add(guiIndexFile))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(guiGenerateIdx)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(guiLoadIdx)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 162, Short.MAX_VALUE)
                        .add(guiSave))
                    .add(guiMediaListPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void guiSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiSaveActionPerformed

        final PSXMedia oMedia = (PSXMedia)guiMediaList.getSelectedValue();
        if (oMedia instanceof PSXMediaStreaming) 
        {
            SaveMedia ve = new SaveMedia(this, (PSXMediaStreaming)oMedia);
            ve.setVisible(true);
        }

    }//GEN-LAST:event_guiSaveActionPerformed

    
    private void guiMediaListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_guiMediaListValueChanged
        guiSave.setEnabled(true);
    }//GEN-LAST:event_guiMediaListValueChanged

    private void guiLoadIdxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiLoadIdxActionPerformed
        if (VerifyInputFile()) {
            try {
                m_oCD = new CDSectorReader(guiInputFile.getText());
                FileDialog fd = new FileDialog(this, "Load Index", FileDialog.LOAD);
                if (m_oLastBrowseFolder != null && m_oLastBrowseFolder.exists())
                    fd.setDirectory(m_oLastBrowseFolder.getPath());
                fd.setVisible(true);
                if (fd.getFile() != null) {
                    m_sIndexFile = new File(fd.getDirectory(), fd.getFile()).getPath();
                    if (new File(m_sIndexFile).exists()) {
                        // if everything went swimmingly
                        // load index
                        m_oMediaList = new MediaHandler(m_oCD, m_sIndexFile);
                        if (m_oMediaList.size() > 0) {
                            DisableIndexButtons();
                            PopulateList();
                            guiIndexFile.setText(m_sIndexFile);
                        } else {
                            JOptionPane.showMessageDialog(this, "No data found. Are you sure this is an index file?");
                            m_oMediaList = null;
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_guiLoadIdxActionPerformed

    private void guiGenerateIdxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiGenerateIdxActionPerformed
        if (VerifyInputFile()) {
            try {

                m_oCD = new CDSectorReader(guiInputFile.getText());
                if (!m_oCD.HasSectorHeader())
                    JOptionPane.showMessageDialog(this, 
                            "This file does not contain entire raw CD sectors.\n" +
                            "Audio cannot be decoded.");
                // generate index
                //"This could take a long time";
                Progress oIndexTask = new Progress(this, "Indexing " + m_oCD.getSourceFile(), new Progress.SimpleWorker<Void>() {

                    @Override
                    Void task(final TaskInfo task) {
                        task.updateEvent("This could take a very long time.");
                        try {
                            m_oMediaList = new MediaHandler(m_oCD, new IProgressListener.IProgressEventErrorListener() {

                                public boolean ProgressUpdate(String sEvent) {
                                    task.updateEvent(sEvent);
                                    return !task.cancelPressed();
                                }

                                public boolean ProgressUpdate(String sWhatDoing, double dblPercentComplete) {
                                    task.updateProgress((int) (dblPercentComplete*100));
                                    task.updateNote(sWhatDoing);
                                    return !task.cancelPressed();
                                }

                                public void ProgressUpdate(Exception e) {
                                    task.showError(e);
                                }
                            });
                            return null;
                        } catch (IOException ex) {
                            task.showError(ex);
                        }
                        return null;
                    }
                });
                oIndexTask.setVisible(true);
                if (!oIndexTask.wasCanceled() && !oIndexTask.threwException() && m_oMediaList.size() > 0) {
                    this.DisableIndexButtons();
                    this.PopulateList();
                    PromptToSaveIndex();
                }
                
            } catch (IOException ex) {
                Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, null, ex);
            }
                
                
        }
    }//GEN-LAST:event_guiGenerateIdxActionPerformed

    
    
    
    private void guiBrowseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiBrowseBtnActionPerformed
        FileDialog fd = new FileDialog(this, "Open", FileDialog.LOAD);
        if (m_oLastBrowseFolder != null)
            fd.setDirectory(m_oLastBrowseFolder.getPath());
        fd.setVisible(true);
        m_oLastBrowseFolder = new File(fd.getDirectory());
        if (fd.getFile() != null)
            guiInputFile.setText(new File(fd.getDirectory(), fd.getFile()).getPath());
    }//GEN-LAST:event_guiBrowseBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        try {
            if (m_oCD != null) m_oCD.close();
            PrintWriter oFOS = new PrintWriter(INI_FILE);
            oFOS.println(m_oLastBrowseFolder != null ? m_oLastBrowseFolder.toString() : "");
            oFOS.println(m_oLastSaveFolder != null ? m_oLastSaveFolder.toString() : "");
            oFOS.close();
        } catch (IOException ex) {
            Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }//GEN-LAST:event_formWindowClosing

    private void guiIndexFileLblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_guiIndexFileLblMouseClicked
        
        /*
        PSXMedia oMedia = m_oMediaList.getBySting(guiMediaList.getSelectedValue().toString());
            
        if (oMedia instanceof PSXMediaSTR) {
            PSXMediaSTR oStrMedia = (PSXMediaSTR)oMedia;

            //JOptionPane.showMessageDialog(this, oStrMedia.CalculateFrameLength());
            JOptionPane.showMessageDialog(this, oStrMedia.CalculateFrameRateBase() + "\n" + oStrMedia.CalculateFrameRateWacked());
        }
        */  
        
        //JOptionPane.showMessageDialog(this, this.getClass().getName() +"\n"+ this.getTitle());
        
        /*
        JOptionPane.showMessageDialog(this, 
                ovfw.FindWindow(this.getClass().getName(), this.getTitle())
        );
        */
        
        if (guiMediaList.getSelectedValue() != null) {
            PSXMedia oMedia = m_oMediaList.getBySting(guiMediaList.getSelectedValue().toString());

            if (oMedia instanceof PSXMediaFF9) {
            }
        }
    }//GEN-LAST:event_guiIndexFileLblMouseClicked
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton guiBrowseBtn;
    private javax.swing.JButton guiGenerateIdx;
    private javax.swing.JLabel guiIndexFile;
    private javax.swing.JLabel guiIndexFileLbl;
    private javax.swing.JTextField guiInputFile;
    private javax.swing.JButton guiLoadIdx;
    private javax.swing.JList guiMediaList;
    private javax.swing.JScrollPane guiMediaListPanel;
    private javax.swing.JButton guiSave;
    // End of variables declaration//GEN-END:variables
    
    

}
