/*
 * $Id: TreeCellContext.java,v 1.11 2009/03/10 12:13:42 kleopatra Exp $
 *
 * Copyright 2008 Sun Microsystems, Inc., 4150 Network Circle,
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
package org.jdesktop.swingx.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTree;

/**
 * Tree specific <code>CellContext</code>. 
 * 
 * <ul>
 * <li>PENDING: use focus border as returned from list or table instead of
 * rolling its own? The missing ui-border probably is a consequence of the
 * border hacking as implemented in core default renderer. SwingX has a
 * composite default which should use the "normal" border.
 * <li> PENDING: selection colors couple explicitly to SwingX - should we go JXTree as
 *   generic type?
 * <li> PENDING: for a JXTree use the icons as returned by the xtree api?
 * </ul>
 */
public class TreeCellContext extends CellContext {
    /** the icon to use for a leaf node. */
    protected Icon leafIcon;

    /** the default icon to use for a closed folder. */
    protected Icon closedIcon;

    /** the default icon to use for a open folder. */
    protected Icon openIcon;

    /** the border around a focused node. */
    private Border treeFocusBorder;

    /**
     * Sets state of the cell's context. Note that the component might be null
     * to indicate a cell without a concrete context. All accessors must cope
     * with.
     * 
     * @param component the component the cell resides on, might be null
     * @param value the content value of the cell
     * @param row the cell's row index in view coordinates
     * @param column the cell's column index in view coordinates
     * @param selected the cell's selected state
     * @param focused the cell's focused state
     * @param expanded the cell's expanded state
     * @param leaf the cell's leaf state
     */
    public void installContext(JTree component, Object value, int row, int column,
            boolean selected, boolean focused, boolean expanded, boolean leaf) {
        this.component = component;
        installState(value, row, column, selected, focused, expanded, leaf);
    }

    
    @Override
    public JTree getComponent() {
        return (JTree) super.getComponent();
    }
    
//------------------- accessors for derived state
    
    /**
     * Returns the treePath for the row or null if invalid.
     * 
     */
    public TreePath getTreePath() {
        if (getComponent() == null) return null;
        if ((row < 0) || (row >= getComponent().getRowCount())) return null;
        return getComponent().getPathForRow(row);
    }
    /**
     * {@inheritDoc}
     * <p>
     * PENDING: implement to return the tree cell editability!
     */
    @Override
    public boolean isEditable() {
        return false;
        // return getComponent() != null ? getComponent().isCellEditable(
        // getRow(), getColumn()) : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Color getSelectionBackground() {
        if (getComponent() instanceof JXTree) {
            return ((JXTree) getComponent()).getSelectionBackground();
        }
        return UIManager.getColor("Tree.selectionBackground");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Color getSelectionForeground() {
        if (getComponent() instanceof JXTree) {
            return ((JXTree) getComponent()).getSelectionForeground();
        }
        return UIManager.getColor("Tree.selectionForeground");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUIPrefix() {
        return "Tree.";
    }

    /**
     * Returns the default icon to use for leaf cell.
     * 
     * @return the icon to use for leaf cell.
     */
    protected Icon getLeafIcon() {
        return leafIcon != null ? leafIcon : UIManager
                .getIcon(getUIKey("leafIcon"));
    }

    /**
     * Returns the default icon to use for open cell.
     * 
     * @return the icon to use for open cell.
     */
    protected Icon getOpenIcon() {
        return openIcon != null ? openIcon : UIManager
                .getIcon(getUIKey("openIcon"));
    }

    /**
     * Returns the default icon to use for closed cell.
     * 
     * @return the icon to use for closed cell.
     */
    protected Icon getClosedIcon() {
        return closedIcon != null ? closedIcon : UIManager
                .getIcon(getUIKey("closedIcon"));
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to return a default depending for the leaf/open cell state.
     */
    @Override
    public Icon getIcon() {
        if (isLeaf()) {
            return getLeafIcon();
        }
        if (isExpanded()) {
            return getOpenIcon();
        }
        return getClosedIcon();
    }

    @Override
    protected Border getFocusBorder() {
        if (treeFocusBorder == null) {
            treeFocusBorder = new TreeFocusBorder();
        }
        return treeFocusBorder;
    }

    /**
     * Border used to draw around the content of the node. <p>
     * PENDING: isn't that the same as around a list or table cell, but
     * without a tree-specific key/value pair in UIManager?
     */
    public class TreeFocusBorder extends LineBorder {

        private Color treeBackground;

        private Color focusColor;

        public TreeFocusBorder() {
            super(Color.BLACK);
            treeBackground = getBackground();
            if (treeBackground != null) {
                focusColor = new Color(~treeBackground.getRGB());
            }
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y,
                int width, int height) {
            Color color = UIManager.getColor("Tree.selectionBorderColor");
            if (color != null) {
                lineColor = color;
            }
            if (isDashed()) {
                if (treeBackground != c.getBackground()) {
                    treeBackground = c.getBackground();
                    focusColor = new Color(~treeBackground.getRGB());
                }

                Color old = g.getColor();
                g.setColor(focusColor);
                BasicGraphicsUtils.drawDashedRect(g, x, y, width, height);
                g.setColor(old);

            } else {
                super.paintBorder(c, g, x, y, width, height);
            }

        }

        /**
         * @return a boolean indicating whether the focus border
         *   should be painted dashed style.
         */
        private boolean isDashed() {
            return Boolean.TRUE.equals(UIManager
                    .get("Tree.drawDashedFocusIndicator"));

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isBorderOpaque() {
            return false;
        }

    }

}