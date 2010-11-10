/*
 * $Id: TreeTableCellEditor.java,v 1.17 2009/01/19 12:01:06 kleopatra Exp $
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

package org.jdesktop.swingx.treetable;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

/**
 * An editor that can be used to edit the tree column. This extends
 * DefaultCellEditor and uses a JTextField (actually, TreeTableTextField)
 * to perform the actual editing.
 * <p>To support editing of the tree column we can not make the tree
 * editable. The reason this doesn't work is that you can not use
 * the same component for editing and rendering. The table may have
 * the need to paint cells, while a cell is being edited. If the same
 * component were used for the rendering and editing the component would
 * be moved around, and the contents would change. When editing, this
 * is undesirable, the contents of the text field must stay the same,
 * including the caret blinking, and selections persisting. For this
 * reason the editing is done via a TableCellEditor.
 * <p>Another interesting thing to be aware of is how tree positions
 * its render and editor. The render/editor is responsible for drawing the
 * icon indicating the type of node (leaf, branch...). The tree is
 * responsible for drawing any other indicators, perhaps an additional
 * +/- sign, or lines connecting the various nodes. So, the renderer
 * is positioned based on depth. On the other hand, table always makes
 * its editor fill the contents of the cell. To get the allusion
 * that the table cell editor is part of the tree, we don't want the
 * table cell editor to fill the cell bounds. We want it to be placed
 * in the same manner as tree places it editor, and have table message
 * the tree to paint any decorations the tree wants. Then, we would
 * only have to worry about the editing part. The approach taken
 * here is to determine where tree would place the editor, and to override
 * the <code>reshape</code> method in the JTextField component to
 * nudge the textfield to the location tree would place it. Since
 * JXTreeTable will paint the tree behind the editor everything should
 * just work. So, that is what we are doing here. Determining of
 * the icon position will only work if the TreeCellRenderer is
 * an instance of DefaultTreeCellRenderer. If you need custom
 * TreeCellRenderers, that don't descend from DefaultTreeCellRenderer,
 * and you want to support editing in JXTreeTable, you will have
 * to do something similar.
 *
 * @author Scott Violet
 * @author Ramesh Gupta
 */
public class TreeTableCellEditor extends DefaultCellEditor {
    public TreeTableCellEditor(JTree tree) {
        super(new TreeTableTextField());
        if (tree == null) {
            throw new IllegalArgumentException("null tree");
        }
        // JW: no need to...
        this.tree = tree; // immutable
    }

    /**
     * Overriden to determine an offset that tree would place the editor at. The
     * offset is determined from the <code>getRowBounds</code> JTree method,
     * and additionaly from the icon DefaultTreeCellRenderer will use.
     * <p>
     * The offset is then set on the TreeTableTextField component created in the
     * constructor, and returned.
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        Component component = super.getTableCellEditorComponent(table, value,
                isSelected, row, column);
        // JW: this implementation is not bidi-compliant, need to do better
        initEditorOffset(table, row, column, isSelected);
        return component;
    }

    /**
     * @param row
     * @param isSelected
     */
    protected void initEditorOffset(JTable table, int row, int column,
            boolean isSelected) {
        if (tree == null)
            return;
        Rectangle bounds = tree.getRowBounds(row);
        int offset = bounds.x;
        Object node = tree.getPathForRow(row).getLastPathComponent();
        boolean leaf = tree.getModel().isLeaf(node);
        boolean expanded = tree.isExpanded(row);
        TreeCellRenderer tcr = tree.getCellRenderer();
        Component treeComponent = tcr.getTreeCellRendererComponent(tree, node,
                isSelected, expanded, leaf, row, false);
        // start patch from armond
//        int boundsWidth = bounds.width;
//        if (treeComponent instanceof JLabel) {
//            JLabel label = (JLabel) treeComponent;
// 
//            Icon icon = label.getIcon();
//            if (icon != null) {
//                if( table.getComponentOrientation().isLeftToRight())
//                    offset += icon.getIconWidth() + label.getIconTextGap();
// 
//                boundsWidth -= icon.getIconWidth();
//            }
//        }
//        ((TreeTableTextField) getComponent()).init(offset, column, boundsWidth, table);

        // start old version
        if ((treeComponent instanceof JLabel)
        // adjust the offset to account for the icon - at least
                // in LToR orientation. RToL is hard to tackle anyway...
                && table.getComponentOrientation().isLeftToRight()) {
            JLabel label = (JLabel) treeComponent;

            Icon icon = label.getIcon();
            if (icon != null) {
                offset += icon.getIconWidth() + label.getIconTextGap();
            }

        }
        ((TreeTableTextField) getComponent()).init(offset, column,
                bounds.width, table);
    }

    /**
     * This is overriden to forward the event to the tree. This will
     * return true if the click count >= clickCountToStart, or the event is null.
     */
    @Override
    public boolean isCellEditable(EventObject e) {
        // JW: quick fix for #592-swingx - 
        // editing not started on keyEvent in hierarchical column (1.6)
        if (e instanceof MouseEvent) {
          return (((MouseEvent) e).getClickCount() >= clickCountToStart);
        }
        return true;
//        if (e == null) {
//            return true;
//        }
//        else if (e instanceof MouseEvent) {
//            return (((MouseEvent) e).getClickCount() >= clickCountToStart);
//        }
//
// // e is some other type of event...
//        return false;
    }

    /**
     * Component used by TreeTableCellEditor. The only thing this does
     * is to override the <code>reshape</code> method, and to ALWAYS
     * make the x location be <code>offset</code>.
     */
    static class TreeTableTextField extends JTextField {
        void init(int offset, int column, int width, JTable table) {
            this.offset = offset;
            this.column = column;
            this.width = width;
            this.table = table;
            setComponentOrientation(table.getComponentOrientation());
        }
        
        private int offset; // changed to package private instead of public
        private int column;
        private int width;
        private JTable table;
        @SuppressWarnings("deprecation")
        @Override
        public void reshape(int x, int y, int width, int height) {
            // Allows precise positioning of text field in the tree cell.
            // following three lines didn't work out
            //Border border = this.getBorder(); // get this text field's border
            //Insets insets = border == null ? null : border.getBorderInsets(this);
            //int newOffset = offset - (insets == null ? 0 : insets.left);
            
            // start of old version
            if(table.getComponentOrientation().isLeftToRight()) {
                int newOffset = offset - getInsets().left;
                // this is LtR version
                super.reshape(x + newOffset, y, width - newOffset, height);
            } else {
                // right to left version
                int newOffset = offset + getInsets().left;
                int pos = getColumnPositionBidi();
                width = table.getColumnModel().getColumn(getBidiTreeColumn()).getWidth();
                width = width - (width - newOffset - this.width);
                super.reshape(pos, y, width, height);
            }
            
            // start of patch from armond
//            if(table.getComponentOrientation().isLeftToRight()) {
//                int newOffset = offset - getInsets().left;
//                // this is LtR version
//                super.reshape(x + newOffset, y, width - newOffset, height);
//            } else {
//                // right to left version
//                int newWidth = width + offset + this.width;
// 
//                super.reshape(x, y, newWidth, height);
//            }

        }
        
        /**
         * Returns the column for the tree in a bidi situation
         */
        private int getBidiTreeColumn() {
            // invert the column offet since this method will always be invoked
            // in a bidi situation
            return table.getColumnCount() - this.column - 1;
        }
        
        private int getColumnPositionBidi() {
            int width = 0;
            
            int column = getBidiTreeColumn();
            for(int iter = 0 ; iter < column ; iter++) {
                width += table.getColumnModel().getColumn(iter).getWidth();
            }
            return width;
        }
    }

    private final JTree tree; // immutable
}