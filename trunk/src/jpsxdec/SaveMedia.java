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
 * SaveMedia.java
 */

package jpsxdec;

import java.io.File;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.media.StrFpsCalc.FramesPerSecond;
import jpsxdec.media.savers.SaverFactory;
import jpsxdec.media.savers.SavingOptions;
import jpsxdec.util.Fraction;
import jpsxdec.media.IProgressListener.IProgressEventErrorListener;
import jpsxdec.media.savers.Formats;
import jpsxdec.util.Misc;

public class SaveMedia extends javax.swing.JDialog {

    private static class DisplayValue {
        public Object Value;
        public String Display;

        public DisplayValue(String Display, Object Value) {
            this.Value = Value;
            this.Display = Display;
        }
        
        public String toString() {
            return Display;
        }
        
        public boolean equals(Object o) {
            return o.equals(Value);
        }
    }
    
    private static final String SAVE_AS_AVI = "AVI";
    private static final String SAVE_AS_IMAGES = "Seperate images";
    
    
    private DefaultComboBoxModel modelAviFormats;
    private DefaultComboBoxModel modelImageFormats;
    private DefaultComboBoxModel modelAudioFormats;
    private DefaultComboBoxModel modelAVIAudioFormats;
    
    private void CreateModels(FramesPerSecond[] afps) {
        modelAviFormats = 
            new DefaultComboBoxModel(Formats.getAviVidFormats());
        
        ////////////////////////////////////////////////////////////////////////
        
        modelImageFormats = new DefaultComboBoxModel( Misc.join(Formats.getVidCompatableImgFmts(), Formats.getExtendedSeqFormats()) );
                
        ////////////////////////////////////////////////////////////////////////

        modelAudioFormats = new DefaultComboBoxModel( Formats.getJavaAudFormats() );
        
        ////////////////////////////////////////////////////////////////////////
        
        modelAVIAudioFormats = new DefaultComboBoxModel( Misc.join(Formats.getAviAudFormats(), Formats.getJavaAudFormats()) );
        
        ////////////////////////////////////////////////////////////////////////
        
        cmbSaveAsVideo.setModel(new DefaultComboBoxModel(new String[] {
            SAVE_AS_AVI,
            SAVE_AS_IMAGES
        }));
        
        ////////////////////////////////////////////////////////////////////////
        
        if (afps != null) {
        
            DisplayValue[] adv = new DisplayValue[afps.length];
            for (int i = 0; i < adv.length; i++) {
                adv[i] = new DisplayValue(
                        String.format("%1.1f fps", afps[i].asDouble()), 
                        afps[i]);
            }

            cmbFrameRate.setModel(new DefaultComboBoxModel(adv));
        }
    }
    
    private String m_sVidBaseName;
    private String m_sAudBaseName;
    
    private long m_lngWidth;
    private long m_lngHeight;
    
    private PSXMediaStreaming m_oVidMedia;
    
    /** Creates new form SaveMedia */
    public SaveMedia(java.awt.Frame parent, PSXMediaStreaming oVidMedia) {
        super(parent, true);
        initComponents();
        
        // use the system's L&F if available (for great justice!)
        try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
                ex.printStackTrace();
        }
        // center the gui
        this.setLocationRelativeTo(parent);
        
        txtFolder.setText(new File("").getAbsolutePath());
        
        m_oVidMedia = oVidMedia;
        
        m_sVidBaseName = m_sAudBaseName = oVidMedia.getSuggestedName();
        
        if (oVidMedia.hasVideo()) {
            m_lngWidth = oVidMedia.getActualWidth();
            m_lngHeight = oVidMedia.getActualHeight();
        
            lblFrameSizeWidth.setText(Long.toString(m_lngWidth));
            lblFrameSizeHeight.setText(Long.toString(m_lngHeight));
        
            lblFramesStart.setText(Long.toString(oVidMedia.getStartFrame()));
            lblFramesEnd.setText(Long.toString(oVidMedia.getEndFrame()));
        
            CreateModels(oVidMedia.getPossibleFPS());
        } else {
            m_lngWidth = 0;
            m_lngHeight = 0;
            
            lblFrameSizeWidth.setText(Long.toString(m_lngWidth));
            lblFrameSizeHeight.setText(Long.toString(m_lngHeight));
            
            lblFramesStart.setText("0");
            lblFramesEnd.setText("0");
        
            chkVideo.setSelected(false);
            chkVideo.setEnabled(false);
            
            CreateModels(null);
        }
        
        if (oVidMedia.getAudioChannels() > 0){
            if (oVidMedia.getAudioChannels() == 1)
                lblChannelsValue.setText("Mono");
            else if (oVidMedia.getAudioChannels() == 2)
                lblChannelsValue.setText("Stereo");
        
            lblBitsPerSampleValue.setText("16");
            lblSampleRateValue.setText(Integer.toString(oVidMedia.getSamplesPerSecond()));
            cmbSaveAsAudio.setModel(modelAVIAudioFormats);
        
        } else {
            lblBitsPerSampleValue.setText("");
            lblChannelsValue.setText("");
            lblSampleRateValue.setText("");
            chkAudio.setSelected(false);
            chkAudio.setEnabled(false);
        }
        
        updateEnabling();
        
    }
    
    private void updateEnabling() {
        
        Object sel;
        
        boolean blnVidChked = chkVideo.getModel().isSelected();
        
        cmbSaveAsVideo.setEnabled(blnVidChked);
        
        cmbFrameRate.setEnabled(blnVidChked && cmbSaveAsVideo.getSelectedItem() == SAVE_AS_AVI);
        cmbDecodeQuality.setEnabled(blnVidChked);
        
        cmbFormat.setEnabled(blnVidChked);
        
        chkDontCrop.setEnabled(blnVidChked);
        
        txtFileNameVideo.setEnabled(blnVidChked);
        
        
        if ( cmbSaveAsVideo.getSelectedItem() == SAVE_AS_AVI) 
        {
            cmbFormat.setModel(modelAviFormats);
            txtFileNameVideo.setText(m_sVidBaseName + ".avi");
        } else {
            cmbFormat.setModel(modelImageFormats);
            
            sel = cmbFormat.getSelectedItem();
            Formats.Format vidfmt =(Formats.Format)sel;

            txtFileNameVideo.setText(m_sVidBaseName + "." + vidfmt.getExt());
        }
        
        sel = cmbFormat.getSelectedItem();
        boolean blnJpgEnabled = blnVidChked && 
                (sel instanceof Formats.HasJpeg) &&
                ((Formats.HasJpeg)sel).hasJpeg();
                
        slideJpegQuality.setEnabled(blnJpgEnabled);
        txtJpegQuality.setEnabled(blnJpgEnabled);
        
        //----------------------------------------------------------------------
        
        if ( blnVidChked && cmbSaveAsVideo.getSelectedItem() == SAVE_AS_AVI) 
        {
            cmbSaveAsAudio.setModel(modelAVIAudioFormats);
        } else {
            cmbSaveAsAudio.setModel(modelAudioFormats);
        }
        
        boolean blnAudChecked = chkAudio.getModel().isSelected();
        
        cmbSaveAsAudio.setEnabled(blnAudChecked);
        
        sel = cmbSaveAsAudio.getSelectedItem();
        Formats.Format aufmt = (Formats.Format)sel;
        txtFileNameAudio.setEnabled(blnAudChecked && !(aufmt == Formats.AVI_WAV));
        if (!(aufmt == Formats.AVI_WAV))
            txtFileNameAudio.setText(m_sAudBaseName + "." + aufmt.getExt());
        
        btnSave.setEnabled(blnVidChked || blnAudChecked);

    }
    
    private void DecodeMediaItem() {

        boolean decodeVideo = chkVideo.getModel().isSelected();
        boolean decodeAudio = chkAudio.getModel().isSelected();
        
        final SavingOptions oOptions = new SavingOptions(m_oVidMedia);
        oOptions.setDecodeVideo(decodeVideo);
        oOptions.setDecodeAudio(decodeAudio);

        oOptions.setFolder(new File(txtFolder.getText()));
        
        if (decodeVideo) {
            oOptions.setVidFilename(txtFileNameVideo.getText());
            //oOptions.setVideoFilenameBase(m_sVidBaseName);
            //oOptions.setVideoFilenameExt(Misc.getExt(txtFileNameVideo));
            oOptions.setDoNotCrop(chkDontCrop.getModel().isSelected());
            oOptions.setFps((Fraction)((DisplayValue)cmbFrameRate.getSelectedItem()).Value);
            oOptions.setJpegQuality(slideJpegQuality.getValue() / 100.0f);
            oOptions.setUseDefaultJpegQuality(false);
            
            oOptions.setVideoFormat((Formats.Format)cmbFormat.getSelectedItem());
            
        }
        
        if (decodeAudio) {
            oOptions.setAudioFilename(txtFileNameAudio.getText());
            //oOptions.setAudioFilenameBase(m_sAudBaseName);
            //oOptions.setAudioFilenameExt(Misc.getExt(txtFileNameAudio));
            oOptions.setAudioFormat((Formats.Format)cmbSaveAsAudio.getSelectedItem());
        }

        Progress oSaveTask = new Progress(
                this, 
                "Saving " + m_oVidMedia.toString(), 
                new Progress.SimpleWorker<Void>() {
                    @Override
                    Void task(final TaskInfo task) throws Exception {
                        long startTime =System.currentTimeMillis();
                        task.showMessage("Start: " + startTime);
                        
                        IProgressEventErrorListener oListen = 
                        new IProgressEventErrorListener() {

                            public boolean ProgressUpdate(String sEvent) {
                                task.updateEvent(sEvent);
                                return task.cancelPressed();
                            }

                            public boolean ProgressUpdate(String sWhatDoing, double dblPercentComplete) {
                                task.updateEvent(sWhatDoing);
                                task.updateProgress((int)(dblPercentComplete * 100));
                                return task.cancelPressed();
                            }

                            public void ProgressUpdate(Exception e) {
                                task.showError(e);
                            }
                        };
                        
                        SaverFactory.DecodeStreaming(
                                oOptions,
                                oListen
                        );
                        
                        long endTime = System.currentTimeMillis();
                        task.showMessage("End: " + endTime);
                        long elapsedTimeInSecond = (endTime - startTime) / 1000;                        
                        task.showMessage("Seconds: " + elapsedTimeInSecond);
                        
                        return null;
                    }
                }
        );
            
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

        modelJpegQuality = new jpsxdec.BoundedRangeDocumentModel();
        lblFolder = new javax.swing.JLabel();
        txtFolder = new javax.swing.JTextField();
        btnBrowse = new javax.swing.JButton();
        panelVideo = new javax.swing.JPanel();
        lblFrameSize = new javax.swing.JLabel();
        lblFrameSizeWidth = new javax.swing.JLabel();
        lblFrameSizeX = new javax.swing.JLabel();
        lblFrameSizeHeight = new javax.swing.JLabel();
        chkDontCrop = new javax.swing.JCheckBox();
        lblFrameRate = new javax.swing.JLabel();
        cmbFrameRate = new javax.swing.JComboBox();
        lblDecodeQuality = new javax.swing.JLabel();
        cmbDecodeQuality = new javax.swing.JComboBox();
        lblFormat = new javax.swing.JLabel();
        cmbFormat = new javax.swing.JComboBox();
        lblJpegQuality = new javax.swing.JLabel();
        slideJpegQuality = new javax.swing.JSlider();
        txtJpegQuality = new javax.swing.JTextField();
        lblFileNameVideo = new javax.swing.JLabel();
        txtFileNameVideo = new javax.swing.JTextField();
        lblFrames = new javax.swing.JLabel();
        lblFramesStart = new javax.swing.JLabel();
        lblFramesto = new javax.swing.JLabel();
        lblFramesEnd = new javax.swing.JLabel();
        chkVideo = new javax.swing.JCheckBox();
        lblSaveAsVideo = new javax.swing.JLabel();
        cmbSaveAsVideo = new javax.swing.JComboBox();
        btnCancel = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        panelAudio = new javax.swing.JPanel();
        lblChannels = new javax.swing.JLabel();
        lblChannelsValue = new javax.swing.JLabel();
        lblBitsPerSample = new javax.swing.JLabel();
        lblBitsPerSampleValue = new javax.swing.JLabel();
        lblSampleRate = new javax.swing.JLabel();
        lblSampleRateValue = new javax.swing.JLabel();
        lblFileNameAudio = new javax.swing.JLabel();
        txtFileNameAudio = new javax.swing.JTextField();
        chkAudio = new javax.swing.JCheckBox();
        lblSaveAsAudio = new javax.swing.JLabel();
        cmbSaveAsAudio = new javax.swing.JComboBox();

        modelJpegQuality.setValue(75);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Save");

        lblFolder.setText("Folder:");

        btnBrowse.setText("Browse...");
        btnBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseActionPerformed(evt);
            }
        });

        panelVideo.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        lblFrameSize.setText("Frame size:");

        lblFrameSizeWidth.setText("320");

        lblFrameSizeX.setText("x");

        lblFrameSizeHeight.setText("240");

        chkDontCrop.setText("Don't crop");
        chkDontCrop.setEnabled(false);
        chkDontCrop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkDontCropActionPerformed(evt);
            }
        });

        lblFrameRate.setText("Frame rate:");

        cmbFrameRate.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "15 fps" }));

        lblDecodeQuality.setText("Decode quality:");

        cmbDecodeQuality.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "High quality/slow decode" }));

        lblFormat.setText("Format:");

        cmbFormat.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "demux", "mdec", "jpeg", "bmp", "png", "gif", "yuv" }));
        cmbFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbFormatActionPerformed(evt);
            }
        });

        lblJpegQuality.setText("JPEG quality:");

        slideJpegQuality.setMinorTickSpacing(25);
        slideJpegQuality.setPaintTicks(true);
        slideJpegQuality.setValue(75);
        slideJpegQuality.setModel(modelJpegQuality);

        txtJpegQuality.setDocument(modelJpegQuality);

        lblFileNameVideo.setText("File name:");

        txtFileNameVideo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFileNameVideoActionPerformed(evt);
            }
        });
        txtFileNameVideo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtFileNameVideoFocusLost(evt);
            }
        });

        lblFrames.setText("Frames:");

        lblFramesStart.setText("1");

        lblFramesto.setText("to");

        lblFramesEnd.setText("453");

        org.jdesktop.layout.GroupLayout panelVideoLayout = new org.jdesktop.layout.GroupLayout(panelVideo);
        panelVideo.setLayout(panelVideoLayout);
        panelVideoLayout.setHorizontalGroup(
            panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelVideoLayout.createSequentialGroup()
                .addContainerGap()
                .add(panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(lblFrames)
                    .add(lblDecodeQuality)
                    .add(lblFrameRate)
                    .add(lblFrameSize)
                    .add(lblFormat)
                    .add(lblJpegQuality)
                    .add(lblFileNameVideo))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(panelVideoLayout.createSequentialGroup()
                        .add(lblFrameSizeWidth)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(lblFrameSizeX)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(lblFrameSizeHeight)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(chkDontCrop))
                    .add(cmbDecodeQuality, 0, 275, Short.MAX_VALUE)
                    .add(txtFileNameVideo, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, panelVideoLayout.createSequentialGroup()
                        .add(slideJpegQuality, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(txtJpegQuality, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 49, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(cmbFormat, 0, 275, Short.MAX_VALUE)
                    .add(cmbFrameRate, 0, 275, Short.MAX_VALUE)
                    .add(panelVideoLayout.createSequentialGroup()
                        .add(lblFramesStart)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(lblFramesto)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(lblFramesEnd)))
                .addContainerGap())
        );
        panelVideoLayout.setVerticalGroup(
            panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelVideoLayout.createSequentialGroup()
                .addContainerGap()
                .add(panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lblFrameSize)
                    .add(lblFrameSizeWidth)
                    .add(lblFrameSizeX)
                    .add(lblFrameSizeHeight)
                    .add(chkDontCrop))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lblFrames)
                    .add(lblFramesStart)
                    .add(lblFramesto)
                    .add(lblFramesEnd))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lblFrameRate)
                    .add(cmbFrameRate, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lblDecodeQuality)
                    .add(cmbDecodeQuality, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lblFormat)
                    .add(cmbFormat, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(lblJpegQuality)
                    .add(slideJpegQuality, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(txtJpegQuality, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelVideoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(txtFileNameVideo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(lblFileNameVideo))
                .addContainerGap())
        );

        chkVideo.setSelected(true);
        chkVideo.setText("Video");
        chkVideo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkVideoActionPerformed(evt);
            }
        });

        lblSaveAsVideo.setText("Save as:");

        cmbSaveAsVideo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "AVI", "Separate images" }));
        cmbSaveAsVideo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbSaveAsVideoActionPerformed(evt);
            }
        });

        btnCancel.setText("Close");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        panelAudio.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        lblChannels.setText("Channels:");

        lblChannelsValue.setText("Stereo");

        lblBitsPerSample.setText("Bits per sample:");

        lblBitsPerSampleValue.setText("16");

        lblSampleRate.setText("Sample rate:");

        lblSampleRateValue.setText("44100");

        lblFileNameAudio.setText("File name:");

        txtFileNameAudio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFileNameAudioActionPerformed(evt);
            }
        });
        txtFileNameAudio.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtFileNameAudioFocusLost(evt);
            }
        });

        org.jdesktop.layout.GroupLayout panelAudioLayout = new org.jdesktop.layout.GroupLayout(panelAudio);
        panelAudio.setLayout(panelAudioLayout);
        panelAudioLayout.setHorizontalGroup(
            panelAudioLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelAudioLayout.createSequentialGroup()
                .addContainerGap()
                .add(panelAudioLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, lblChannels)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, lblBitsPerSample)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, lblSampleRate)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, lblFileNameAudio))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelAudioLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(lblChannelsValue)
                    .add(lblBitsPerSampleValue)
                    .add(lblSampleRateValue)
                    .add(txtFileNameAudio, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelAudioLayout.setVerticalGroup(
            panelAudioLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelAudioLayout.createSequentialGroup()
                .addContainerGap()
                .add(panelAudioLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lblChannels)
                    .add(lblChannelsValue))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelAudioLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lblBitsPerSample)
                    .add(lblBitsPerSampleValue))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelAudioLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lblSampleRate)
                    .add(lblSampleRateValue))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelAudioLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(txtFileNameAudio, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(lblFileNameAudio))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        chkAudio.setSelected(true);
        chkAudio.setText("Audio");
        chkAudio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkAudioActionPerformed(evt);
            }
        });

        lblSaveAsAudio.setText("Save as:");

        cmbSaveAsAudio.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "part of AVI", "wav", "aiff" }));
        cmbSaveAsAudio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbSaveAsAudioActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(lblFolder)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(txtFolder, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(btnBrowse))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, panelVideo, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(layout.createSequentialGroup()
                                .add(chkVideo)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 178, Short.MAX_VALUE)
                                .add(lblSaveAsVideo)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(cmbSaveAsVideo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 224, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(btnCancel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(btnSave, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, panelAudio, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(layout.createSequentialGroup()
                                .add(chkAudio)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 173, Short.MAX_VALUE)
                                .add(lblSaveAsAudio)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(cmbSaveAsAudio, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 108, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lblFolder)
                    .add(btnBrowse)
                    .add(txtFolder, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(chkVideo)
                    .add(cmbSaveAsVideo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(lblSaveAsVideo))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelVideo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(chkAudio)
                    .add(lblSaveAsAudio)
                    .add(cmbSaveAsAudio, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelAudio, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(btnCancel)
                    .add(btnSave))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseActionPerformed
        JFileChooser fc = new BetterFileChooser(".");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int ret = fc.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            txtFolder.setText(fc.getSelectedFile().toString());
        }
    }//GEN-LAST:event_btnBrowseActionPerformed

    private void chkDontCropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkDontCropActionPerformed
        if (chkDontCrop.getModel().isSelected()) {
            
        } else {
            
        }
    }//GEN-LAST:event_chkDontCropActionPerformed

    private void cmbFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbFormatActionPerformed
        updateEnabling();
    }//GEN-LAST:event_cmbFormatActionPerformed

    private void txtFileNameVideoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFileNameVideoActionPerformed
        m_sVidBaseName = Misc.getBaseName(txtFileNameVideo);
    }//GEN-LAST:event_txtFileNameVideoActionPerformed

    private void txtFileNameVideoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtFileNameVideoFocusLost
        m_sVidBaseName = Misc.getBaseName(txtFileNameVideo);
    }//GEN-LAST:event_txtFileNameVideoFocusLost

    private void chkVideoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkVideoActionPerformed
        updateEnabling();
    }//GEN-LAST:event_chkVideoActionPerformed

    private void cmbSaveAsVideoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbSaveAsVideoActionPerformed
        updateEnabling();
    }//GEN-LAST:event_cmbSaveAsVideoActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        DecodeMediaItem();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void txtFileNameAudioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFileNameAudioActionPerformed
        m_sAudBaseName = Misc.getBaseName(txtFileNameAudio);
    }//GEN-LAST:event_txtFileNameAudioActionPerformed

    private void txtFileNameAudioFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtFileNameAudioFocusLost
        m_sAudBaseName = Misc.getBaseName(txtFileNameAudio);
    }//GEN-LAST:event_txtFileNameAudioFocusLost

    private void chkAudioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkAudioActionPerformed
        updateEnabling();
    }//GEN-LAST:event_chkAudioActionPerformed

    private void cmbSaveAsAudioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbSaveAsAudioActionPerformed
        updateEnabling();
    }//GEN-LAST:event_cmbSaveAsAudioActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowse;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnSave;
    private javax.swing.JCheckBox chkAudio;
    private javax.swing.JCheckBox chkDontCrop;
    private javax.swing.JCheckBox chkVideo;
    private javax.swing.JComboBox cmbDecodeQuality;
    private javax.swing.JComboBox cmbFormat;
    private javax.swing.JComboBox cmbFrameRate;
    private javax.swing.JComboBox cmbSaveAsAudio;
    private javax.swing.JComboBox cmbSaveAsVideo;
    private javax.swing.JLabel lblBitsPerSample;
    private javax.swing.JLabel lblBitsPerSampleValue;
    private javax.swing.JLabel lblChannels;
    private javax.swing.JLabel lblChannelsValue;
    private javax.swing.JLabel lblDecodeQuality;
    private javax.swing.JLabel lblFileNameAudio;
    private javax.swing.JLabel lblFileNameVideo;
    private javax.swing.JLabel lblFolder;
    private javax.swing.JLabel lblFormat;
    private javax.swing.JLabel lblFrameRate;
    private javax.swing.JLabel lblFrameSize;
    private javax.swing.JLabel lblFrameSizeHeight;
    private javax.swing.JLabel lblFrameSizeWidth;
    private javax.swing.JLabel lblFrameSizeX;
    private javax.swing.JLabel lblFrames;
    private javax.swing.JLabel lblFramesEnd;
    private javax.swing.JLabel lblFramesStart;
    private javax.swing.JLabel lblFramesto;
    private javax.swing.JLabel lblJpegQuality;
    private javax.swing.JLabel lblSampleRate;
    private javax.swing.JLabel lblSampleRateValue;
    private javax.swing.JLabel lblSaveAsAudio;
    private javax.swing.JLabel lblSaveAsVideo;
    private jpsxdec.BoundedRangeDocumentModel modelJpegQuality;
    private javax.swing.JPanel panelAudio;
    private javax.swing.JPanel panelVideo;
    private javax.swing.JSlider slideJpegQuality;
    private javax.swing.JTextField txtFileNameAudio;
    private javax.swing.JTextField txtFileNameVideo;
    private javax.swing.JTextField txtFolder;
    private javax.swing.JTextField txtJpegQuality;
    // End of variables declaration//GEN-END:variables
    
}
