/*
 * $Id: AbstractTreeTableModel.java,v 1.8 2008/10/14 22:31:37 rah003 Exp $
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

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.tree.TreeModelSupport;

// There is no javax.swing.tree.AbstractTreeModel; There ought to be one.

/**
 * AbstractTreeTableModel provides an implementation of
 * {@link org.jdesktop.swingx.treetable.TreeTableModel} as a convenient starting
 * point in defining custom data models for
 * {@link org.jdesktop.swingx.JXTreeTable}. It takes care of listener
 * management and contains convenience methods for creating and dispatching
 * {@code TreeModelEvent}s. To create a concreate instance of
 * {@code TreeTableModel} you need only to provide implementations for the
 * following methods:
 * 
 * <pre>
 * public int getColumnCount();
 * public Object getValueAt(Object node, int column);
 * public Object getChild(Object parent, int index);
 * public int getChildCount(Object parent);
 * public int getIndexOfChild(Object parent, Object child);
 * public boolean isLeaf(Object node);
 * </pre>
 * 
 * @author Ramesh Gupta
 * @author Karl Schaefer
 */
public abstract class AbstractTreeTableModel implements TreeTableModel {

    /**
     * Root node of the model
     */
    protected Object root;

    /**
     * Provides support for event dispatching.
     */
    protected TreeModelSupport modelSupport;
    
    /**
     * Constructs an {@code AbstractTreeTableModel} with a {@code null} root
     * node.
     */
    public AbstractTreeTableModel() {
        this(null);
    }

    /**
     * Constructs an {@code AbstractTreeTableModel} with the specified root
     * node.
     * 
     * @param root
     *            root node
     */
    public AbstractTreeTableModel(Object root) {
        this.root = root;
        this.modelSupport = new TreeModelSupport(this);
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getColumnClass(int column) {
        return Object.class;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(int column) {
        //Copied from AbstractTableModel.
        //Should use same defaults when possible.
        String result = "";
        
        for (; column >= 0; column = column / 26 - 1) {
            result = (char) ((char) (column % 26) + 'A') + result;
        }
        
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int getHierarchicalColumn() {
        if (getColumnCount() == 0) {
            return -1;
        }
        
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Object getRoot() {
        return root;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCellEditable(Object node, int column) {
        // RG: Fix Issue 49 -- Cell not editable, by default.
        // Subclasses might override this to return true.
        return false;
    }

    /**
     * Returns <code>true</code> if <code>node</code> is a leaf.
     *
     * @impl {@code true} if {@code getChildCount(node) == 0}
     * @param   node  a node in the tree, obtained from this data source
     * @return  true if <code>node</code> is a leaf
     */
    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    /**
     * Sets the value for the {@code node} at {@code columnIndex} to
     * {@code value}.
     * 
     * @impl is no-op; provided for convenience for uneditable models
     * @param value
     *            the new value
     * @param node
     *            the node whose value is to be changed
     * @param column
     *            the column whose value is to be changed
     * @see #getValueAt
     * @see #isCellEditable
     * @see javax.swing.table.TableModel#setValueAt(Object, int, int)
     */
    public void setValueAt(Object value, Object node, int column) {
        //does nothing
    }
    
    /**
     * Called when value for the item identified by path has been changed. If
     * newValue signifies a truly new value the model should post a
     * {@code treeNodesChanged} event.
     * <p>
     * 
     * @impl is no-op. A {@code JXTreeTable} does not usually edit the node directly.
     * @param path
     *            path to the node that has changed
     * @param newValue
     *            the new value from the <code>TreeCellEditor</code>
     */
    public void valueForPathChanged(TreePath path, Object newValue) {
        //does nothing
    }

    /**
     * {@inheritDoc}
     */
    public void addTreeModelListener(TreeModelListener l) {
        modelSupport.addTreeModelListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeTreeModelListener(TreeModelListener l) {
        modelSupport.removeTreeModelListener(l);
    }

    /**
     * Returns an array of all the <code>TreeModelListener</code>s added
     * to this JXTreeTable with addTreeModelListener().
     *
     * @return all of the <code>TreeModelListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public TreeModelListener[] getTreeModelListeners() {
        return modelSupport.getTreeModelListeners();
    }
}
