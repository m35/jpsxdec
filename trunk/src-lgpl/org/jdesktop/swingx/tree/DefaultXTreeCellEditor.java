/*
 * $Id: DefaultXTreeCellEditor.java,v 1.7 2009/03/26 10:25:23 kleopatra Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.tree;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;

import org.jdesktop.swingx.decorator.UIDependent;

/**
 * Subclassed to hack around core bug with RtoL editing (#4980473).
 * 
 * The price to pay is currently is to guarantee a minimum size of the
 * editing field (is only one char wide if the node value is null).
 * 
 * PENDING: any possibility to position the editorContainer? 
 * BasicTreeUI adds it to the tree and positions at the node location. 
 * That's not a problem in LToR, only
 * in RToL 
 *
 * 
 * @author Jeanette Winzenburg
 */
public class DefaultXTreeCellEditor extends DefaultTreeCellEditor implements UIDependent {

    public DefaultXTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
        super(tree, renderer);
    }

    public DefaultXTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer,
            TreeCellEditor editor) {
        super(tree, renderer, editor);
    }

    public void setRenderer(DefaultTreeCellRenderer renderer) {
        this.renderer = renderer;
    }
    
    public DefaultTreeCellRenderer getRenderer() {
        return renderer;
    }
    
    public class XEditorContainer extends EditorContainer {

        @Override
        public Dimension getPreferredSize() {
            if (isRightToLeft()) {
                if(editingComponent != null) {
                    Dimension         pSize = editingComponent.getPreferredSize();
    
                    pSize.width += offset + 5;
    
                    Dimension         rSize = (renderer != null) ?
                                              renderer.getPreferredSize() : null;
    
                    if(rSize != null)
                        pSize.height = Math.max(pSize.height, rSize.height);
                    if(editingIcon != null)
                        pSize.height = Math.max(pSize.height,
                                                editingIcon.getIconHeight());
    
                    // trying to enforce a minimum size leads to field being painted over the icon
                    // Make sure width is at least 100.
    //                pSize.width = Math.max(pSize.width, 100);
                    return pSize;
                }
                return new Dimension(0, 0);
            }
            return super.getPreferredSize();
            
        }

        @Override
        public void doLayout() {
            if (isRightToLeft()) {
                Dimension             cSize = getSize();

                editingComponent.getPreferredSize();
                editingComponent.setLocation(0, 0);
                editingComponent.setBounds(0, 0,
                                           cSize.width - offset,
                                           cSize.height);
            } else {

                super.doLayout();
            }
        }


        @Override
        public void paint(Graphics g) {
            if (isRightToLeft()) {
                Dimension size = getSize();

                // Then the icon.
                if (editingIcon != null) {
                    int yLoc = Math.max(0, (size.height - editingIcon
                            .getIconHeight()) / 2);
                    int xLoc = Math.max(0, size.width - offset);
                    editingIcon.paintIcon(this, g, xLoc, yLoc);
                }
                // need to prevent super from painting the icon
                Icon rememberIcon = editingIcon;
                editingIcon = null;
                super.paint(g);
                editingIcon = rememberIcon;
                
            } else {
                super.paint(g);
            }
        }
        
    }


    @Override
    protected Container createContainer() {
        return new XEditorContainer();
    }

    @Override
    protected void prepareForEditing() {
        super.prepareForEditing();
        applyComponentOrientation();
    }

    protected void applyComponentOrientation() {
        if (tree != null) {
            editingContainer.applyComponentOrientation(tree.getComponentOrientation());
        }
        
    }

    /**
     * @return
     */
    private boolean isRightToLeft() {
        return (tree != null) && (!tree.getComponentOrientation().isLeftToRight());
    }

    /**
     * Implement UIDependent. Quick hack for #1060-swingx: icons lost on laf toggle.
     */
    public void updateUI() {
        if (getRenderer() != null) {
            SwingUtilities.updateComponentTreeUI(getRenderer());
        }
        if (realEditor instanceof JComponent) {
            SwingUtilities.updateComponentTreeUI((JComponent) realEditor);
        } else if (realEditor instanceof UIDependent) {
            ((UIDependent) realEditor).updateUI();
        }
        
    }

}
