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
package com.l2fprod.common.swing.plaf;

import com.l2fprod.common.swing.JDirectoryChooser;

import java.util.Arrays;
import java.util.List;

/**
 * Addon for <code>JDirectoryChooser</code>.<br>
 *
 * @author <a href="mailto:fred@L2FProd.com">Frederic Lavigne</a>
 */
public class JDirectoryChooserAddon extends AbstractComponentAddon {

  public JDirectoryChooserAddon() {
    super("JDirectoryChooser");
  }
  
  protected void addBasicDefaults(LookAndFeelAddons addon, List defaults) {
    defaults.addAll(Arrays.asList(new Object[]{
      JDirectoryChooser.UI_CLASS_ID,
      "com.l2fprod.common.swing.plaf.windows.WindowsDirectoryChooserUI",
    }));
    
    addResource(defaults, "com.l2fprod.common.swing.plaf.DirectoryChooserUIRB");
  }
 
}