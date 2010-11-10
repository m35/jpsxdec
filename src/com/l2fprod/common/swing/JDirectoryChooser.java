/**
 * L2FProd.com Common Components 7.3 License.
 *
 * Copyright 2005-2007 L2FProd.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.l2fprod.common.swing;

import com.l2fprod.common.swing.plaf.DirectoryChooserUI;
import com.l2fprod.common.swing.plaf.JDirectoryChooserAddon;
import com.l2fprod.common.swing.plaf.LookAndFeelAddons;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.ComponentUI;

/**
 * An extension of the JFileChooser but dedicated to directory selection. <br>
 *  
 * @javabean.class
 *          name="JDirectoryChooser"
 *          shortDescription="JDirectoryChooser allows to select one or more directories."
 *          stopClass="javax.swing.JFileChooser"
 * 
 * @javabean.icons
 *          mono16="JDirectoryChooser16-mono.gif"
 *          color16="JDirectoryChooser16.gif"
 *          mono32="JDirectoryChooser32-mono.gif"
 *          color32="JDirectoryChooser32.gif"
 */
public class JDirectoryChooser extends JFileChooser {

  public final static String UI_CLASS_ID = "l2fprod/DirectoryChooserUI";

  // ensure at least the default ui is registered
  static {
    LookAndFeelAddons.contribute(new JDirectoryChooserAddon());
  }

  private boolean showingCreateDirectory;

  /**
   * Used when generating PropertyChangeEvents for the "showingCreateDirectory" property
   */
  public static final String SHOWING_CREATE_DIRECTORY_CHANGED_KEY = "showingCreateDirectory";

  /**
   * Creates a JDirectoryChooser pointing to the user's home directory.
   */
  public JDirectoryChooser() {
    super();
    setShowingCreateDirectory(true);
  }

  /**
   * Creates a JDirectoryChooser using the given File as the path.
   * 
   * @param currentDirectory
   */
  public JDirectoryChooser(File currentDirectory) {
    super(currentDirectory);
    setShowingCreateDirectory(true);
  }

  /**
   * Creates a JDirectoryChooser using the given current directory and
   * FileSystemView
   * 
   * @param currentDirectory
   * @param fsv
   */
  public JDirectoryChooser(File currentDirectory, FileSystemView fsv) {
    super(currentDirectory, fsv);
    setShowingCreateDirectory(true);
  }

  /**
   * Creates a JDirectoryChooser using the given FileSystemView
   * 
   * @param fsv
   */
  public JDirectoryChooser(FileSystemView fsv) {
    super(fsv);
    setShowingCreateDirectory(true);
  }

  /**
   * Creates a JDirectoryChooser using the given path.
   * 
   * @param currentDirectoryPath
   */
  public JDirectoryChooser(String currentDirectoryPath) {
    super(currentDirectoryPath);
    setShowingCreateDirectory(true);
  }

  public JDirectoryChooser(String currentDirectoryPath, FileSystemView fsv) {
    super(currentDirectoryPath, fsv);
    setShowingCreateDirectory(true);
  }

  /**
   * Notification from the <code>UIManager</code> that the L&F has changed.
   * Replaces the current UI object with the latest version from the <code>UIManager</code>.
   * 
   * @see javax.swing.JComponent#updateUI
   */
  public void updateUI() {
    setUI((DirectoryChooserUI)LookAndFeelAddons.getUI(this,
      DirectoryChooserUI.class));
  }
  
  /**
   * Sets the L&F object that renders this component.
   * 
   * @param ui the <code>DirectoryChooserUI</code> L&F object
   * @see javax.swing.UIDefaults#getUI
   * 
   * @beaninfo bound: true hidden: true description: The UI object that
   * implements the taskpane group's LookAndFeel.
   */
  public void setUI(DirectoryChooserUI ui) {
    super.setUI((ComponentUI)ui);
  }
  
  /**
   * Returns the name of the L&F class that renders this component.
   * 
   * @return the string {@link #UI_CLASS_ID}
   * @see javax.swing.JComponent#getUIClassID
   * @see javax.swing.UIDefaults#getUI
   */
  public String getUIClassID() {
    return UI_CLASS_ID;
  }
  
  /**
   *
   * @return true if the "Make New Folder" button is shown, false otherwise
   */
  public boolean isShowingCreateDirectory() {
    return showingCreateDirectory;
  }

  /**
   * Sets whether or not the "Make New Folder" button is shown in the control
   * button area. Default is true.
   * 
   * @param showingCreateDirectory
   * @javabean.property
   *          bound="true"
   *          preferred="true"
   */
  public void setShowingCreateDirectory(boolean showingCreateDirectory) {    
    this.showingCreateDirectory = showingCreateDirectory;
    firePropertyChange(SHOWING_CREATE_DIRECTORY_CHANGED_KEY, !showingCreateDirectory,
      showingCreateDirectory);
  }
  
  protected JDialog createDialog(Component parent) throws HeadlessException {
    JDialog dialog = super.createDialog(parent);
    ((JComponent)dialog.getContentPane()).setBorder(
      LookAndFeelTweaks.WINDOW_BORDER);
    return dialog;
  }
  
}
