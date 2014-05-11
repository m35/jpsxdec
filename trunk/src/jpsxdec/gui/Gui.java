/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

import com.l2fprod.common.swing.JDirectoryChooser;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import jpsxdec.I18N;
import jpsxdec.gui.GuiTree.TreeItem;
import jpsxdec.Main;
import jpsxdec.Version;
import jpsxdec.cdreaders.CdFileNotFoundException;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.UserFriendlyLogger;
import jpsxdec.util.player.PlayController;
import jpsxdec.util.player.PlayController.Event;
import org.openide.awt.DropDownButtonFactory;

public class Gui extends javax.swing.JFrame {

    private static final Logger LOG = Logger.getLogger(Gui.class.getName());

    // -------------------------------------------------------------------------
    
    private DiscIndex _index;
    private PlayController _currentPlayer;
    private final ArrayList<DiscItemSaverBuilderGui> _saverGuis =
            new ArrayList<DiscItemSaverBuilderGui>();
    private final GuiSettings _settings;
    private String _sCommandLineFile;

    // -------------------------------------------------------------------------

    public Gui() {

        // use the system's L&F if available (for great justice!)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error setting system L&F", ex);
        }

        setTitle(Version.VerStringNonCommercial);

        initComponents();
        
        _guiTab.setEnabledAt(1, false);

        // center the gui
        this.setLocationRelativeTo(null);

        _settings = new GuiSettings();
        _settings.load();

        _guiDirectory.setText(_settings.getSavingDir());

        convertToolbar();

        Image icon16 = Toolkit.getDefaultToolkit().createImage(getClass().getResource("icon16.png"));
        try {
            // setIconImages() is only available in Java 6+, but jPSXdec is targetted for Java 5
            // we optionally take advantage of it using reflection
            Method setIconImages = this.getClass().getMethod("setIconImages", List.class);
            ArrayList<Image> icons = new ArrayList<Image>(3);
            Image icon32 = Toolkit.getDefaultToolkit().createImage(getClass().getResource("icon32.png"));
            Image icon48 = Toolkit.getDefaultToolkit().createImage(getClass().getResource("icon48.png"));
            icons.add(icon16);
            icons.add(icon32);
            icons.add(icon48);
            setIconImages.invoke(this, icons);
        } catch (Exception ex) {
            LOG.log(Level.INFO, "Unable to set multiple icons", ex);
            setIconImage(icon16);
        }

    }


    public void setCommandLineFile(String sCmdLineFile) {
        _sCommandLineFile = sCmdLineFile;
    }

    // -------------------------------------------------------------------------
    // -- Toolbar dropdown tweak -----------------------------------------------
    // -------------------------------------------------------------------------

    private void convertToolbar() {
        _guiToolbar.removeAll();

        _guiOpenDisc = convertButton(_guiOpenDisc, new DiscMenu());
        _guiOpenIndex = convertButton(_guiOpenIndex, new IndexMenu());

        _guiToolbar.add(_guiOpenDisc);
        _guiToolbar.add(_guiToolbarSeparator1);
        _guiToolbar.add(_guiOpenIndex);
        _guiToolbar.add(_guiToolbarSeparator2);
        _guiToolbar.add(_guiSaveIndex);
        _guiToolbar.validate();
    }

    private static JButton convertButton(JButton oldButton, JPopupMenu menu) {
        JButton newButton = DropDownButtonFactory.createDropDownButton(oldButton.getIcon(), menu);
        newButton.setText(oldButton.getText());
        newButton.setHorizontalTextPosition(SwingConstants.LEFT);
        newButton.setMargin(oldButton.getMargin());
        for (ActionListener listener : oldButton.getActionListeners()) {
            newButton.addActionListener(listener);
        }
        return newButton;
    }

    private class DiscMenu extends JPopupMenu implements PopupMenuListener, ActionListener {

        private final JMenuItem EMPTY_MENU_ITEM = new JMenuItem("Empty");

        public DiscMenu() {
            add(EMPTY_MENU_ITEM);
            addPopupMenuListener(this);
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            removeAll();
            for (String sName : _settings.getPreviousImages()) {
                JMenuItem item = new JMenuItem(sName);
                item.addActionListener(this);
                add(item);
            }
        }

        public void actionPerformed(ActionEvent e) {
            File f = new File(e.getActionCommand());
            if (f.exists() && !promptSaveIndex())
                return;
            openDisc(f);
        }
        
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        public void popupMenuCanceled(PopupMenuEvent e) {}
    }

    private class IndexMenu extends JPopupMenu implements PopupMenuListener, ActionListener {

        private final JMenuItem EMPTY_MENU_ITEM = new JMenuItem("Empty");
        
        public IndexMenu() {
            add(EMPTY_MENU_ITEM);
            addPopupMenuListener(this);
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            removeAll();
            for (String sName : _settings.getPreviousIndexes()) {
                JMenuItem item = new JMenuItem(sName);
                item.addActionListener(this);
                add(item);
            }
        }

        public void actionPerformed(ActionEvent e) {
            File f = new File(e.getActionCommand());
            if (f.exists() && !promptSaveIndex())
                return;
            openIndex(f);
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        public void popupMenuCanceled(PopupMenuEvent e) {}
    }

    // -------------------------------------------------------------------------
    // -- Basic index load/save operations -------------------------------------
    // -------------------------------------------------------------------------


    private void openIndex(File indexFile) {
        File dir = indexFile.getParentFile();
        if (dir != null)
            _settings.setIndexDir(dir.getAbsolutePath());

        final int[] aiWarnErr = new int[2];
        UserFriendlyLogger log = new UserFriendlyLogger("index");
        try {
            log.setListener(new UserFriendlyLogger.OnWarnErr() {
                public void onWarn(LogRecord record) {
                    aiWarnErr[0]++;
                }
                public void onErr(LogRecord record) {
                    aiWarnErr[1]++;
                }
            });
            log.log(Level.INFO, "Opening index {0}", indexFile); // I18N
            
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            setIndex(new DiscIndex(indexFile.getPath(), log), indexFile.getName());
            _settings.addPreviousIndex(indexFile.getAbsolutePath());
            _guiSaveIndex.setEnabled(false);
            if (aiWarnErr[0] > 0 || aiWarnErr[1] > 0) {
                StringBuilder sb = new StringBuilder(I18N.S("Loaded {0,number,#} items, but with issues.", _index.size())); // I18N
                sb.append('\n');
                if (aiWarnErr[0] > 0)
                    sb.append(I18N.S("  Warnings: {0,number,#}", aiWarnErr[0])) // I18N
                      .append('\n');
                if (aiWarnErr[1] > 0)
                    sb.append(I18N.S("  Errors: {0,number,#}", aiWarnErr[1])) // I18N
                      .append('\n');
                sb.append(I18N.S("See {0} for details.", log.getFileName())); // I18N
                
                JOptionPane.showMessageDialog(this, sb.toString(), I18N.S("Issues loading index"), JOptionPane.WARNING_MESSAGE); // I18N
            }
        } catch (NotThisTypeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18N.S("Error loading index file"), JOptionPane.WARNING_MESSAGE); // I18N
        } catch (CdFileNotFoundException ex) {
            log.log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this, I18N.S("Unable to open {0}", ex.getFile()), // I18N
                                                I18N.S("File not found"), JOptionPane.WARNING_MESSAGE); // I18N
        } catch (FileNotFoundException ex) {
            log.log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this, I18N.S("Unable to open {0}", ex.getMessage()), // I18N
                                                I18N.S("File not found"), JOptionPane.WARNING_MESSAGE); // I18N
            _settings.removePreviousIndex(indexFile.getAbsolutePath());
        } catch (Throwable ex) {
            ex.printStackTrace();
            log.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex, I18N.S("Bad error"), JOptionPane.ERROR_MESSAGE); // I18N
        } finally {
            setCursor(Cursor.getDefaultCursor());
            log.close();
        }

    }

    private void setIndex(DiscIndex index, String sIndexFile) {
        final CdFileSectorReader oldCd;
        if (_index == null)
            oldCd = null;
        else
            oldCd = _index.getSourceCd();
        
        _index = index;
        setDisc(_index.getSourceCd());
        _saverGuis.clear();

        _guiSaveAll.setEnabled(true);
        _guiSelectAll.setEnabled(true);

        _guiDiscTree.formatTreeTable(_index);

        if (sIndexFile == null) {
            sIndexFile = I18N.S("*Unsaved*"); // I18N
        }
        setIndexTitle(sIndexFile);

        _currentPlayer = null;

        // don't want to close the old CD immediately
        // because some (player) threads may not be finished using it
        // wait 5 seconds before closing
        if (oldCd != null){
            Timer t = new Timer(5000, new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        System.out.println("Closing closed disc");
                        oldCd.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        LOG.log(Level.SEVERE, "Error closing CD", ex);
                    }
                }
            });
            t.setRepeats(false);
            t.start();
        }
    }

    private void setIndexTitle(String sFile) {
        setTitle(sFile + " - " + Version.VerStringNonCommercial);
    }

    private boolean saveIndex() {
        if (_index == null)
            return true;

        BetterFileChooser fc = new BetterFileChooser(_settings.getIndexDir());
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setDialogTitle(I18N.S("Save index")); // I18N
        fc.addChoosableFileFilter(GuiFileFilters.INDEX_FILE_FILTER);
        fc.setFileFilter(GuiFileFilters.INDEX_FILE_FILTER);
        int iResult = fc.showSaveDialog(this);
        if (iResult != BetterFileChooser.APPROVE_OPTION)
            return false;
        _settings.setIndexDir(fc.getCurrentDirectory().getPath());
        try {

            File selection = fc.getSelectedFile();
            PrintStream ps = new PrintStream(selection);
            try {
                _index.serializeIndex(ps);
            } finally {
                ps.close();
            }

            _settings.addPreviousIndex(selection.getAbsolutePath());

            _guiSaveIndex.setEnabled(false);
            setIndexTitle(selection.getName());
            return true;
        } catch (Throwable ex) {
            ex.printStackTrace();
            LOG.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex, I18N.S("Error saving index"), JOptionPane.ERROR_MESSAGE); // I18N
            return false;
        }

    }

    // -------------------------------------------------------------------------
    // -- Basic disc load/save operations --------------------------------------
    // -------------------------------------------------------------------------

    private void setDisc(CdFileSectorReader cd) {
        _guiDiscInfoLine1.setText(cd.getSourceFile().getAbsoluteFile().getPath());
        _guiDiscInfoLine2.setText(cd.getTypeDescription());

        _settings.addPreviousImage(cd.getSourceFile().getAbsolutePath());
    }

    private void openDisc(File file) {
        File dir = file.getParentFile();
        if (dir != null)
            _settings.setImageDir(dir.getAbsolutePath());
        try {

            CdFileSectorReader cd = new CdFileSectorReader(file);

            if (!cd.hasSectorHeader())
                JOptionPane.showMessageDialog(this, I18N.S("Disc image does not have raw headers -- audio may not be detected.")); // I18N

            IndexingGui gui = new IndexingGui(this, cd);
            gui.setVisible(true);
            if (gui.getIndex() == null) {
                // indexing was canceled
                cd.close();
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                setIndex(gui.getIndex(), null);
                _guiSaveIndex.setEnabled(true);
            }

        } catch (FileNotFoundException ex) {
            LOG.log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this, I18N.S("Unable to open {0}", file), // I18N
                                                I18N.S("File not found"), // I18N
                                          JOptionPane.WARNING_MESSAGE);
            _settings.removePreviousImage(file.getAbsolutePath());
        } catch (Throwable ex) {
            ex.printStackTrace();
            LOG.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex, I18N.S("Bad error"), JOptionPane.ERROR_MESSAGE); // I18N
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        _guiTopPanel = new javax.swing.JPanel();
        _guiToolbar = new javax.swing.JToolBar();
        _guiOpenDisc = new javax.swing.JButton();
        _guiToolbarSeparator1 = new javax.swing.JToolBar.Separator();
        _guiOpenIndex = new javax.swing.JButton();
        _guiToolbarSeparator2 = new javax.swing.JToolBar.Separator();
        _guiSaveIndex = new javax.swing.JButton();
        _guiDiscInfoContainer = new javax.swing.JPanel();
        _guiDiscInfoLine1 = new javax.swing.JLabel();
        _guiDiscInfoLine2 = new javax.swing.JLabel();
        _guiMainSplit = new javax.swing.JSplitPane();
        _guiTreeContainer = new javax.swing.JPanel();
        _guiDiscTreeScroll = new javax.swing.JScrollPane();
        _guiDiscTree = new jpsxdec.gui.GuiTree();
        _guiTreeButtonContainer = new javax.swing.JPanel();
        _guiSelectAll = new javax.swing.JButton();
        _guiSelectAllType = new javax.swing.JComboBox();
        _guiExpandAll = new javax.swing.JButton();
        _guiCollapseAll = new javax.swing.JButton();
        _guiTabContainer = new javax.swing.JPanel();
        _guiTab = new javax.swing.JTabbedPane();
        _guiSaveContainer = new javax.swing.JPanel();
        _guiDirChooserContainer = new javax.swing.JPanel();
        _guiDirectoryLbl = new javax.swing.JLabel();
        _guiDirectory = new javax.swing.JTextField();
        _guiChooseDir = new javax.swing.JButton();
        _guiSavePanel = new javax.swing.JPanel();
        _guiSaveBtnContainer = new javax.swing.JPanel();
        _guiApplyAll = new javax.swing.JButton();
        _guiSaveAll = new javax.swing.JButton();
        _guiPreviewContainer = new javax.swing.JPanel();
        _guiPlayPauseBtn = new javax.swing.JButton();
        _guiPreviewPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });
        getContentPane().setLayout(new java.awt.BorderLayout(5, 5));

        _guiTopPanel.setLayout(new java.awt.BorderLayout());

        _guiToolbar.setFloatable(false);
        _guiToolbar.setRollover(true);

        _guiOpenDisc.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpsxdec/gui/media-optical-5.png"))); // NOI18N
        _guiOpenDisc.setText(I18N.S("Open and Analyze Disc Image")); // I18N
        _guiOpenDisc.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        _guiOpenDisc.setMargin(new java.awt.Insets(5, 10, 5, 10));
        _guiOpenDisc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiOpenDiscActionPerformed(evt);
            }
        });
        _guiToolbar.add(_guiOpenDisc);
        _guiToolbar.add(_guiToolbarSeparator1);

        _guiOpenIndex.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpsxdec/gui/insert-numbers.png"))); // NOI18N
        _guiOpenIndex.setText(I18N.S("Open Index")); // I18N
        _guiOpenIndex.setFocusable(false);
        _guiOpenIndex.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        _guiOpenIndex.setMargin(new java.awt.Insets(5, 10, 5, 10));
        _guiOpenIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiOpenIndexActionPerformed(evt);
            }
        });
        _guiToolbar.add(_guiOpenIndex);
        _guiToolbar.add(_guiToolbarSeparator2);

        _guiSaveIndex.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpsxdec/gui/3floppy_unmount.png"))); // NOI18N
        _guiSaveIndex.setText(I18N.S("Save Index")); // I18N
        _guiSaveIndex.setEnabled(false);
        _guiSaveIndex.setFocusable(false);
        _guiSaveIndex.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        _guiSaveIndex.setMargin(new java.awt.Insets(5, 10, 5, 10));
        _guiSaveIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiSaveIndexActionPerformed(evt);
            }
        });
        _guiToolbar.add(_guiSaveIndex);

        _guiTopPanel.add(_guiToolbar, java.awt.BorderLayout.NORTH);

        _guiDiscInfoContainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _guiDiscInfoContainer.setLayout(new javax.swing.BoxLayout(_guiDiscInfoContainer, javax.swing.BoxLayout.PAGE_AXIS));

        _guiDiscInfoLine1.setText("  ");
        _guiDiscInfoContainer.add(_guiDiscInfoLine1);

        _guiDiscInfoLine2.setText("  ");
        _guiDiscInfoContainer.add(_guiDiscInfoLine2);

        _guiTopPanel.add(_guiDiscInfoContainer, java.awt.BorderLayout.SOUTH);

        getContentPane().add(_guiTopPanel, java.awt.BorderLayout.NORTH);

        _guiMainSplit.setDividerLocation(500);
        _guiMainSplit.setResizeWeight(1.0);

        _guiTreeContainer.setLayout(new java.awt.BorderLayout(5, 5));

        _guiDiscTreeScroll.setPreferredSize(new java.awt.Dimension(500, 300));

        _guiDiscTree.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        _guiDiscTree.setMinimumSize(new java.awt.Dimension(200, 0));
        _guiDiscTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                _guiDiscTreeValueChanged(evt);
            }
        });
        _guiDiscTreeScroll.setViewportView(_guiDiscTree);

        _guiTreeContainer.add(_guiDiscTreeScroll, java.awt.BorderLayout.CENTER);

        _guiTreeButtonContainer.setLayout(new java.awt.GridBagLayout());

        _guiSelectAll.setText(I18N.S("Select ...")); // I18N
        _guiSelectAll.setEnabled(false);
        _guiSelectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiSelectAllActionPerformed(evt);
            }
        });
        _guiTreeButtonContainer.add(_guiSelectAll, new java.awt.GridBagConstraints());

        _guiSelectAllType.setModel(new DefaultComboBoxModel(GuiTree.Select.values()));
        _guiTreeButtonContainer.add(_guiSelectAllType, new java.awt.GridBagConstraints());

        _guiExpandAll.setText(I18N.S("Expand All")); // I18N
        _guiExpandAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiExpandAllActionPerformed(evt);
            }
        });
        _guiTreeButtonContainer.add(_guiExpandAll, new java.awt.GridBagConstraints());

        _guiCollapseAll.setText(I18N.S("Collapse All")); // I18N
        _guiCollapseAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiCollapseAllActionPerformed(evt);
            }
        });
        _guiTreeButtonContainer.add(_guiCollapseAll, new java.awt.GridBagConstraints());

        _guiTreeContainer.add(_guiTreeButtonContainer, java.awt.BorderLayout.SOUTH);

        _guiMainSplit.setLeftComponent(_guiTreeContainer);

        _guiTabContainer.setLayout(new java.awt.BorderLayout(5, 5));

        _guiTab.setMinimumSize(new java.awt.Dimension(400, 5));
        _guiTab.setPreferredSize(new java.awt.Dimension(400, 500));
        _guiTab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                _guiTabStateChanged(evt);
            }
        });

        _guiSaveContainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        _guiSaveContainer.setMinimumSize(new java.awt.Dimension(0, 500));
        _guiSaveContainer.setPreferredSize(new java.awt.Dimension(0, 500));
        _guiSaveContainer.setLayout(new java.awt.BorderLayout());

        _guiDirChooserContainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));
        _guiDirChooserContainer.setLayout(new java.awt.BorderLayout(2, 0));

        _guiDirectoryLbl.setText(I18N.S("Directory:")); // I18N
        _guiDirChooserContainer.add(_guiDirectoryLbl, java.awt.BorderLayout.WEST);
        _guiDirChooserContainer.add(_guiDirectory, java.awt.BorderLayout.CENTER);

        _guiChooseDir.setText(I18N.S("...")); // I18N
        _guiChooseDir.setMargin(new java.awt.Insets(2, 3, 2, 2));
        _guiChooseDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiChooseDirActionPerformed(evt);
            }
        });
        _guiDirChooserContainer.add(_guiChooseDir, java.awt.BorderLayout.LINE_END);

        _guiSaveContainer.add(_guiDirChooserContainer, java.awt.BorderLayout.NORTH);

        _guiSavePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        _guiSavePanel.setLayout(new java.awt.BorderLayout());
        _guiSaveContainer.add(_guiSavePanel, java.awt.BorderLayout.CENTER);

        _guiSaveBtnContainer.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        _guiApplyAll.setText(I18N.S("Apply to all {0}")); // I18N
        _guiApplyAll.setEnabled(false);
        _guiApplyAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiApplyAllActionPerformed(evt);
            }
        });
        _guiSaveBtnContainer.add(_guiApplyAll);

        _guiSaveAll.setText(I18N.S("Save All Selected")); // I18N
        _guiSaveAll.setEnabled(false);
        _guiSaveAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiSaveAllActionPerformed(evt);
            }
        });
        _guiSaveBtnContainer.add(_guiSaveAll);

        _guiSaveContainer.add(_guiSaveBtnContainer, java.awt.BorderLayout.SOUTH);

        _guiTab.addTab("    Save    ", _guiSaveContainer);

        _guiPreviewContainer.setLayout(new java.awt.BorderLayout());

        _guiPlayPauseBtn.setText(I18N.S("Play")); // I18N
        _guiPlayPauseBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiPlayPauseBtnActionPerformed(evt);
            }
        });
        _guiPreviewContainer.add(_guiPlayPauseBtn, java.awt.BorderLayout.SOUTH);

        _guiPreviewPanel.setLayout(new java.awt.BorderLayout());
        _guiPreviewContainer.add(_guiPreviewPanel, java.awt.BorderLayout.CENTER);

        _guiTab.addTab("    Play    ", _guiPreviewContainer);

        _guiTabContainer.add(_guiTab, java.awt.BorderLayout.CENTER);
        _guiTab.getAccessibleContext().setAccessibleName("DoStuffTabs");

        _guiMainSplit.setRightComponent(_guiTabContainer);

        getContentPane().add(_guiMainSplit, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void _guiOpenIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiOpenIndexActionPerformed
        if (!promptSaveIndex())
            return;
        BetterFileChooser fc = new BetterFileChooser(_settings.getIndexDir());
        fc.setAcceptAllFileFilterUsed(true);
        fc.setDialogTitle(I18N.S("Load index")); // I18N
        fc.addChoosableFileFilter(GuiFileFilters.INDEX_FILE_FILTER);
        fc.setFileFilter(GuiFileFilters.INDEX_FILE_FILTER);
        int iResult = fc.showOpenDialog(this);
        if (iResult != BetterFileChooser.APPROVE_OPTION)
            return;
        openIndex(fc.getSelectedFile());
        
    }//GEN-LAST:event__guiOpenIndexActionPerformed


    private void _guiDiscTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event__guiDiscTreeValueChanged
        TreeItem selection = _guiDiscTree.getTreeTblSelection();

        _guiTab.setSelectedIndex(0);
        JPanel gui = null;
        if (selection != null) {
            gui = getGuiForDiscItem(selection.getBuilder());
            _guiTab.setEnabledAt(1, selection.canPlay());
        } else {
            _guiTab.setSelectedIndex(0);
            _guiTab.setEnabledAt(1, false);
        }
        if (gui != null && !gui.isAncestorOf(_guiSavePanel)) {
            _guiSavePanel.removeAll();
            _guiSavePanel.add(gui);
            _guiApplyAll.setEnabled(true);
            _guiApplyAll.setText(I18N.S("Apply to all {0}", selection.getType())); // I18N
        } else if (gui == null) {
            _guiSavePanel.removeAll();
            _guiApplyAll.setEnabled(false);
            _guiApplyAll.setText(I18N.S("Apply to all {0}", // I18N
                                        ""));
        }
        _guiSavePanel.revalidate();
        _guiSavePanel.repaint();

    }//GEN-LAST:event__guiDiscTreeValueChanged

    private DiscItemSaverBuilderGui getGuiForDiscItem(DiscItemSaverBuilder builder) {
        if (builder == null)
            return null;
        for (DiscItemSaverBuilderGui gui : _saverGuis) {
            if (gui.useSaverBuilder(builder)) {
                return gui;
            }
        }
        DiscItemSaverBuilderGui gui = builder.getOptionPane();
        if (gui != null)
            _saverGuis.add(gui);
        return gui;
    }

    private void _guiPlayPauseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiPlayPauseBtnActionPerformed
        try {
            if (_currentPlayer != null) {
                if (_guiPlayPauseBtn.getText().equals(I18N.S("Play"))) { // I18N
                    _currentPlayer.unpause();
                    _guiPlayPauseBtn.setText(I18N.S("Pause")); // I18N 
                } else {
                    _currentPlayer.pause();
                    _guiPlayPauseBtn.setText(I18N.S("Play")); // I18N
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            LOG.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event__guiPlayPauseBtnActionPerformed

    private void _guiOpenDiscActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiOpenDiscActionPerformed
        if (!promptSaveIndex())
            return;
        BetterFileChooser fc = new BetterFileChooser(_settings.getImageDir());
        fc.setAcceptAllFileFilterUsed(true);
        fc.setDialogTitle(I18N.S("Select disc image or media file")); // I18N
        for (FileFilter filter : GuiFileFilters.DISC_OPEN_FILTERS) {
            fc.addChoosableFileFilter(filter);
        }
        fc.setFileFilter(GuiFileFilters.DISC_OPEN_FILTERS[0]);
        int iResult = fc.showOpenDialog(this);
        if (iResult != BetterFileChooser.APPROVE_OPTION)
            return;

        openDisc(fc.getSelectedFile());

    }//GEN-LAST:event__guiOpenDiscActionPerformed



    private void _guiSaveIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiSaveIndexActionPerformed
        saveIndex();
    }//GEN-LAST:event__guiSaveIndexActionPerformed



    private void _guiSelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiSelectAllActionPerformed
        _guiDiscTree.selectAllType((GuiTree.Select)_guiSelectAllType.getSelectedItem());
    }//GEN-LAST:event__guiSelectAllActionPerformed


    private void _guiExpandAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiExpandAllActionPerformed
        _guiDiscTree.expandAll();
    }//GEN-LAST:event__guiExpandAllActionPerformed

    private void _guiCollapseAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiCollapseAllActionPerformed
        _guiDiscTree.collapseAll();
    }//GEN-LAST:event__guiCollapseAllActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (!promptSaveIndex())
            return;
        if (_settings != null) {
            _settings.setSavingDir(_guiDirectory.getText());
            try {
                _settings.save();
            } catch (Throwable ex) {
                LOG.log(Level.WARNING, "Error saving ini file", ex);
            }
        }
        System.exit(0);
        
    }//GEN-LAST:event_formWindowClosing

    private boolean promptSaveIndex() {
        if (_guiSaveIndex.isEnabled()) {
            int iRet = JOptionPane.showConfirmDialog(this, I18N.S("The index has not been saved. Save index?"), // I18N
                                                     I18N.S("Save index?"), JOptionPane.YES_NO_CANCEL_OPTION); // I18N
            if (iRet == JOptionPane.CANCEL_OPTION)
                return false;
            else if (iRet == JOptionPane.YES_OPTION) {
                return saveIndex();
            }
        }
        return true;
    }

    private void _guiApplyAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiApplyAllActionPerformed

        TreeItem node = _guiDiscTree.getTreeTblSelection();
        if (node == null)
            return;
        DiscItemSaverBuilder builder = node.getBuilder();
        if (builder != null) {
            int iCount = _guiDiscTree.applySettings(builder);
            JOptionPane.showMessageDialog(this, I18N.S("Applied settings to {0,number,#} items.", iCount)); // I18N
        }
    }//GEN-LAST:event__guiApplyAllActionPerformed


    private void _guiSaveAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiSaveAllActionPerformed

        try {

            String sDir = _guiDirectory.getText();
            File dir = sDir.length() == 0 ? null : new File(sDir);
            ArrayList<IDiscItemSaver> savers = _guiDiscTree.collectSelected(dir);
            if (savers.isEmpty()) {
                JOptionPane.showMessageDialog(this, I18N.S("Nothing is marked for saving.")); // I18N
                return;
            }
            
            SavingGui gui = new SavingGui(this, savers, 
                    _index.getSourceCd().toString());
            gui.setVisible(true);

        } catch (Throwable ex) {
            ex.printStackTrace();
            LOG.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex, I18N.S("Bad error"), JOptionPane.ERROR_MESSAGE); // I18N
        }

    }//GEN-LAST:event__guiSaveAllActionPerformed



    private void _guiChooseDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiChooseDirActionPerformed

        JDirectoryChooser chooser = new JDirectoryChooser(_guiDirectory.getText());
        int choice = chooser.showOpenDialog(this);
        if(choice != JDirectoryChooser.APPROVE_OPTION)
            return;
        _guiDirectory.setText(chooser.getSelectedFile().getAbsolutePath());

    }//GEN-LAST:event__guiChooseDirActionPerformed

    
    private transient final PlayController.PlayerListener _playerListener = new PlayController.PlayerListener() {
        public void update(Event eEvent) {
            switch (eEvent) {
                case Stop:
                    _guiPlayPauseBtn.setEnabled(false);
                    break;
            }
        }
    };
    
    private void _guiTabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event__guiTabStateChanged
        
        if (_guiTab.getSelectedIndex() == 1) {
            TreeItem selection = _guiDiscTree.getTreeTblSelection();
            if (selection == null)
                return;
            _currentPlayer = selection.getPlayer();
            if (_currentPlayer != null) {
                try {
                    _currentPlayer.start();
                } catch (LineUnavailableException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    _currentPlayer = null;
                }
            }
            if (_currentPlayer != null) {
                if (_currentPlayer.hasVideo()) {
                    _guiPreviewPanel.add(_currentPlayer.getVideoScreen());
                    _guiPreviewPanel.revalidate();
                }
                _currentPlayer.addLineListener(_playerListener);
                _guiPlayPauseBtn.setText(I18N.S("Play")); // I18N
                _guiPlayPauseBtn.setEnabled(true);
            }
        } else {
            if (_currentPlayer != null) {
                _currentPlayer.stop();
                _currentPlayer = null;
                _guiPreviewPanel.removeAll();
                _guiPreviewPanel.validate();
                _guiPreviewPanel.repaint();
            }        
        }
        
        
    }//GEN-LAST:event__guiTabStateChanged

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        if (_sCommandLineFile == null)
            return;
        FileInputStream fis = null;
        try {
            File f = new File(_sCommandLineFile);
            fis = new FileInputStream(f);
            byte[] abBuffer = new byte[Version.VerString.length()*2];
            IO.readByteArray(fis, abBuffer);
            fis.close();
            fis = null;
            String s = new String(abBuffer, "ascii");
            if (s.contains("jPSXdec")) {
            //if (_sCommandLineFile.substring(_sCommandLineFile.length()-4).equalsIgnoreCase(".idx")) {
                openIndex(f);
            } else {
                openDisc(f);
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, null, ex);
            JOptionPane.showMessageDialog(this,
                    I18N.S("Error opening {0}: {1}", ex.getLocalizedMessage(), _sCommandLineFile), // I18N
                    I18N.S("Failed to read file"), // I18N
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_formWindowOpened



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton _guiApplyAll;
    private javax.swing.JButton _guiChooseDir;
    private javax.swing.JButton _guiCollapseAll;
    private javax.swing.JPanel _guiDirChooserContainer;
    private javax.swing.JTextField _guiDirectory;
    private javax.swing.JLabel _guiDirectoryLbl;
    private javax.swing.JPanel _guiDiscInfoContainer;
    private javax.swing.JLabel _guiDiscInfoLine1;
    private javax.swing.JLabel _guiDiscInfoLine2;
    private jpsxdec.gui.GuiTree _guiDiscTree;
    private javax.swing.JScrollPane _guiDiscTreeScroll;
    private javax.swing.JButton _guiExpandAll;
    private javax.swing.JSplitPane _guiMainSplit;
    private javax.swing.JButton _guiOpenDisc;
    private javax.swing.JButton _guiOpenIndex;
    private javax.swing.JButton _guiPlayPauseBtn;
    private javax.swing.JPanel _guiPreviewContainer;
    private javax.swing.JPanel _guiPreviewPanel;
    private javax.swing.JButton _guiSaveAll;
    private javax.swing.JPanel _guiSaveBtnContainer;
    private javax.swing.JPanel _guiSaveContainer;
    private javax.swing.JButton _guiSaveIndex;
    private javax.swing.JPanel _guiSavePanel;
    private javax.swing.JButton _guiSelectAll;
    private javax.swing.JComboBox _guiSelectAllType;
    private javax.swing.JTabbedPane _guiTab;
    private javax.swing.JPanel _guiTabContainer;
    private javax.swing.JToolBar _guiToolbar;
    private javax.swing.JToolBar.Separator _guiToolbarSeparator1;
    private javax.swing.JToolBar.Separator _guiToolbarSeparator2;
    private javax.swing.JPanel _guiTopPanel;
    private javax.swing.JPanel _guiTreeButtonContainer;
    private javax.swing.JPanel _guiTreeContainer;
    // End of variables declaration//GEN-END:variables


    public static void main(String args[]) {
        Main.loadDefaultLogger();
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Gui().setVisible(true);
            }
        });
    }

}
