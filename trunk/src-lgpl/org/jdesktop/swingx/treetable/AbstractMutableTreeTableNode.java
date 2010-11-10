/*
 * $Id: AbstractMutableTreeTableNode.java,v 1.7 2008/10/14 22:31:37 rah003 Exp $
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;

/**
 * {@code AbstractMutableTreeTableNode} provides an implementation of most of
 * the {@code MutableTreeTableNode} features.
 * 
 * @author Karl Schaefer
 */
public abstract class AbstractMutableTreeTableNode implements
        MutableTreeTableNode {
    /** this node's parent, or null if this node has no parent */
    protected MutableTreeTableNode parent;

    /**
     * List of children, if this node has no children the list will be empty.
     * This list will never be null.
     */
    protected final List<MutableTreeTableNode> children;

    /** optional user object */
    protected transient Object userObject;

    protected boolean allowsChildren;

    public AbstractMutableTreeTableNode() {
        this(null);
    }

    public AbstractMutableTreeTableNode(Object userObject) {
        this(userObject, true);
    }

    public AbstractMutableTreeTableNode(Object userObject,
            boolean allowsChildren) {
        this.userObject = userObject;
        this.allowsChildren = allowsChildren;
        children = createChildrenList();
    }

    /**
     * Creates the list used to manage the children of this node.
     * <p>
     * This method is called by the constructor.
     * 
     * @return a list; this list is guaranteed to be non-{@code null}
     */
    protected List<MutableTreeTableNode> createChildrenList() {
        return new ArrayList<MutableTreeTableNode>();
    }
    
    public void add(MutableTreeTableNode child) {
        insert(child, getChildCount());
    }

    /**
     * {@inheritDoc}
     */
    public void insert(MutableTreeTableNode child, int index) {
        if (!allowsChildren) {
            throw new IllegalStateException("this node cannot accept children");
        }

        if (children.contains(child)) {
            children.remove(child);
            index--;
        }
        
        children.add(index, child);

        if (child.getParent() != this) {
            child.setParent(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(int index) {
        children.remove(index).setParent(null);
    }

    /**
     * {@inheritDoc}
     */
    public void remove(MutableTreeTableNode node) {
        children.remove(node);
        node.setParent(null);
    }

    /**
     * {@inheritDoc}
     */
    public void removeFromParent() {
        parent.remove(this);
    }

    /**
     * {@inheritDoc}
     */
    public void setParent(MutableTreeTableNode newParent) {
        if (newParent == null || newParent.getAllowsChildren()) {
            if (parent != null && parent.getIndex(this) != -1) {
                parent.remove(this);
            }
        } else {
            throw new IllegalArgumentException(
                    "newParent does not allow children");
        }

        parent = newParent;

        if (parent != null && parent.getIndex(this) == -1) {
            parent.insert(this, parent.getChildCount());
        }
    }

    /**
     * Returns this node's user object.
     * 
     * @return the Object stored at this node by the user
     * @see #setUserObject
     * @see #toString
     */
    public Object getUserObject() {
        return userObject;
    }

    /**
     * {@inheritDoc}
     */
    public void setUserObject(Object object) {
        userObject = object;
    }

    /**
     * {@inheritDoc}
     */
    public TreeTableNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    /**
     * {@inheritDoc}
     */
    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    /**
     * {@inheritDoc}
     */
    public TreeTableNode getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration<? extends MutableTreeTableNode> children() {
        return Collections.enumeration(children);
    }

    /**
     * {@inheritDoc}
     */
    public boolean getAllowsChildren() {
        return allowsChildren;
    }

    /**
     * Determines whether or not this node is allowed to have children. If
     * {@code allowsChildren} is {@code false}, all of this node's children are
     * removed.
     * <p>
     * Note: By default, a node allows children.
     * 
     * @param allowsChildren
     *            {@code true} if this node is allowed to have children
     */
    public void setAllowsChildren(boolean allowsChildren) {
        this.allowsChildren = allowsChildren;

        if (!this.allowsChildren) {
            children.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    /**
     * Determines whether the specified column is editable.
     * 
     * @param column
     *            the column to query
     * @return always returns {@code false}
     */
    public boolean isEditable(int column) {
        return false;
    }

    /**
     * Sets the value for the given {@code column}.
     * 
     * @impl does nothing. It is provided for convenience.
     * @param aValue
     *            the value to set
     * @param column
     *            the column to set the value on
     */
    public void setValueAt(Object aValue, int column) {
        // does nothing
    }

    /**
     * Returns the result of sending <code>toString()</code> to this node's
     * user object, or null if this node has no user object.
     * 
     * @see #getUserObject
     */
    @Override
    public String toString() {
        if (userObject == null) {
            return "";
        } else {
            return userObject.toString();
        }
    }
}
