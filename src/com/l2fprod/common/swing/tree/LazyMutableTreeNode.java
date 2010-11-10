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
package com.l2fprod.common.swing.tree;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * LazyMutableTreeNode. <br>
 *  
 */
public abstract class LazyMutableTreeNode extends DefaultMutableTreeNode {

  private boolean loaded = false;

  public LazyMutableTreeNode() {
    super();
  }

  public LazyMutableTreeNode(Object userObject) {
    super(userObject);
  }

  public LazyMutableTreeNode(Object userObject, boolean allowsChildren) {
    super(userObject, allowsChildren);
  }

  public int getChildCount() {
    synchronized (this) {
      if (!loaded) {
        loaded = true;
        loadChildren();
      }
    }
    return super.getChildCount();
  }

  public void clear() {
    removeAllChildren();
    loaded = false;
  }

  public boolean isLoaded() {
    return loaded;
  }

  protected abstract void loadChildren();

}
