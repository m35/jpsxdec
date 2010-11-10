/*
 * $Id: JXTreeTable.java,v 1.110 2009/03/20 15:41:31 kleopatra Exp $
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


package org.jdesktop.swingx;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ActionMap;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.SelectionMapper;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.rollover.RolloverProducer;
import org.jdesktop.swingx.rollover.RolloverRenderer;
import org.jdesktop.swingx.tree.DefaultXTreeCellRenderer;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableCellEditor;
import org.jdesktop.swingx.treetable.TreeTableModel;

/**
 * <p><code>JXTreeTable</code> is a specialized {@link javax.swing.JTable table}
 * consisting of a single column in which to display hierarchical data, and any
 * number of other columns in which to display regular data. The interface for
 * the data model used by a <code>JXTreeTable</code> is
 * {@link org.jdesktop.swingx.treetable.TreeTableModel}. It extends the
 * {@link javax.swing.tree.TreeModel} interface to allow access to cell data by
 * column indices within each node of the tree hierarchy.</p>
 *
 * <p>The most straightforward way create and use a <code>JXTreeTable</code>, is to
 * first create a suitable data model for it, and pass that to a
 * <code>JXTreeTable</code> constructor, as shown below:
 * <pre>
 *  TreeTableModel  treeTableModel = new FileSystemModel(); // any TreeTableModel
 *  JXTreeTable     treeTable = new JXTreeTable(treeTableModel);
 *  JScrollPane     scrollpane = new JScrollPane(treeTable);
 * </pre>
 * See {@link javax.swing.JTable} for an explanation of why putting the treetable
 * inside a scroll pane is necessary.</p>
 *
 * <p>A single treetable model instance may be shared among more than one
 * <code>JXTreeTable</code> instances. To access the treetable model, always call
 * {@link #getTreeTableModel() getTreeTableModel} and
 * {@link #setTreeTableModel(org.jdesktop.swingx.treetable.TreeTableModel) setTreeTableModel}.
 * <code>JXTreeTable</code> wraps the supplied treetable model inside a private
 * adapter class to adapt it to a {@link javax.swing.table.TableModel}. Although
 * the model adapter is accessible through the {@link #getModel() getModel} method, you
 * should avoid accessing and manipulating it in any way. In particular, each
 * model adapter instance is tightly bound to a single table instance, and any
 * attempt to share it with another table (for example, by calling
 * {@link #setModel(javax.swing.table.TableModel) setModel})
 * will throw an <code>IllegalArgumentException</code>!
 *
 * @author Philip Milne
 * @author Scott Violet
 * @author Ramesh Gupta
 */
public class JXTreeTable extends JXTable {
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(JXTreeTable.class
            .getName());
    /**
     * Key for clientProperty to decide whether to apply hack around #168-jdnc.
     */
    public static final String DRAG_HACK_FLAG_KEY = "treeTable.dragHackFlag";
    /**
     * Key for clientProperty to decide whether to apply hack around #766-swingx.
     */
    public static final String DROP_HACK_FLAG_KEY = "treeTable.dropHackFlag";
    /**
     * Renderer used to render cells within the
     *  {@link #isHierarchical(int) hierarchical} column.
     *  renderer extends JXTree and implements TableCellRenderer
     */
    private TreeTableCellRenderer renderer;

    /**
     * Editor used to edit cells within the
     *  {@link #isHierarchical(int) hierarchical} column.
     */
    private TreeTableCellEditor hierarchicalEditor;
    
    private TreeTableHacker treeTableHacker;
    private boolean consumedOnPress;

    /**
     * Constructs a JXTreeTable using a
     * {@link org.jdesktop.swingx.treetable.DefaultTreeTableModel}.
     */
    public JXTreeTable() {
        this(new DefaultTreeTableModel());
    }

    /**
     * Constructs a JXTreeTable using the specified
     * {@link org.jdesktop.swingx.treetable.TreeTableModel}.
     *
     * @param treeModel model for the JXTreeTable
     */
    public JXTreeTable(TreeTableModel treeModel) {
        this(new JXTreeTable.TreeTableCellRenderer(treeModel));
    }

    /**
     * Constructs a <code>JXTreeTable</code> using the specified
     * {@link org.jdesktop.swingx.JXTreeTable.TreeTableCellRenderer}.
     * 
     * @param renderer
     *                cell renderer for the tree portion of this JXTreeTable
     *                instance.
     */
    private JXTreeTable(TreeTableCellRenderer renderer) {
        // To avoid unnecessary object creation, such as the construction of a
        // DefaultTableModel, it is better to invoke
        // super(TreeTableModelAdapter) directly, instead of first invoking
        // super() followed by a call to setTreeTableModel(TreeTableModel).

        // Adapt tree model to table model before invoking super()
        super(new TreeTableModelAdapter(renderer));

        // renderer-related initialization
        init(renderer); // private method
        initActions();
        // disable sorting
        
        // no grid
        setShowGrid(false, false);

        hierarchicalEditor = new TreeTableCellEditor(renderer);
        
//        // No grid.
//        setShowGrid(false); // superclass default is "true"
//
//        // Default intercell spacing
//        setIntercellSpacing(spacing); // for both row margin and column margin

    }

    /**
     * Initializes this JXTreeTable and permanently binds the specified renderer
     * to it.
     *
     * @param renderer private tree/renderer permanently and exclusively bound
     * to this JXTreeTable.
     */
    private void init(TreeTableCellRenderer renderer) {
        this.renderer = renderer;
        assert ((TreeTableModelAdapter) getModel()).tree == this.renderer;
        
        // Force the JTable and JTree to share their row selection models.
        ListToTreeSelectionModelWrapper selectionWrapper =
            new ListToTreeSelectionModelWrapper();

        // JW: when would that happen?
        if (renderer != null) {
            renderer.bind(this); // IMPORTANT: link back!
            renderer.setSelectionModel(selectionWrapper);
        }
        // adjust the tree's rowHeight to this.rowHeight
        adjustTreeRowHeight(getRowHeight());

        setSelectionModel(selectionWrapper.getListSelectionModel());
        
        // propagate the lineStyle property to the renderer
        PropertyChangeListener l = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                JXTreeTable.this.renderer.putClientProperty(evt.getPropertyName(), evt.getNewValue());
                
            }
            
        };
        addPropertyChangeListener("JTree.lineStyle", l);
        
    }


    
    private void initActions() {
        // Register the actions that this class can handle.
        ActionMap map = getActionMap();
        map.put("expand-all", new Actions("expand-all"));
        map.put("collapse-all", new Actions("collapse-all"));
    }

    /**
     * A small class which dispatches actions.
     * TODO: Is there a way that we can make this static?
     */
    private class Actions extends UIAction {
        Actions(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent evt) {
            if ("expand-all".equals(getName())) {
        expandAll();
            }
            else if ("collapse-all".equals(getName())) {
                collapseAll();
            }
        }
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to keep the tree's enabled in synch.
     */
    @Override
    public void setEnabled(boolean enabled) {
        renderer.setEnabled(enabled);
        super.setEnabled(enabled);
    }
    

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to keep the tree's selectionBackground in synch.
     */
    @Override
    public void setSelectionBackground(Color selectionBackground) {
        // happens on instantiation, updateUI is called before the renderer is installed
        if (renderer != null)
            renderer.setSelectionBackground(selectionBackground);
        super.setSelectionBackground(selectionBackground);
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to keep the tree's selectionForeground in synch.
     */
    @Override
    public void setSelectionForeground(Color selectionForeground) {
        // happens on instantiation, updateUI is called before the renderer is installed
        if (renderer != null)
            renderer.setSelectionForeground(selectionForeground);
        super.setSelectionForeground(selectionForeground);
    }

    /**
     * Overriden to invoke repaint for the particular location if
     * the column contains the tree. This is done as the tree editor does
     * not fill the bounds of the cell, we need the renderer to paint
     * the tree in the background, and then draw the editor over it.
     * You should not need to call this method directly. <p>
     * 
     * Additionally, there is tricksery involved to expand/collapse
     * the nodes.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        getTreeTableHacker().hitHandleDetectionFromEditCell(column, e);    // RG: Fix Issue 49!
        boolean canEdit = super.editCellAt(row, column, e);
        if (canEdit && isHierarchical(column)) {
            repaint(getCellRect(row, column, false));
        }
        return canEdit;
    }

    /**
     * Overridden to enable hit handle detection a mouseEvent which triggered
     * a expand/collapse. 
     */
    @Override
    protected void processMouseEvent(MouseEvent e) {
        // BasicTableUI selects on released if the pressed had been 
        // consumed. So we try to fish for the accompanying released
        // here and consume it as wll. 
        if ((e.getID() == MouseEvent.MOUSE_RELEASED) && consumedOnPress) {
            consumedOnPress = false;
            e.consume();
            return;
        }
        if (getTreeTableHacker().hitHandleDetectionFromProcessMouse(e)) {
            // Issue #332-swing: hacking around selection loss.
            // prevent the
            // _table_ selection by consuming the mouseEvent
            // if it resulted in a expand/collapse
            consumedOnPress = true;
            e.consume();
            return;
        }
        consumedOnPress = false;
        super.processMouseEvent(e);
    }
    

    protected TreeTableHacker getTreeTableHacker() {
        if (treeTableHacker == null) {
            treeTableHacker = createTreeTableHacker();
        }
        return treeTableHacker;
    }
    
    protected TreeTableHacker createTreeTableHacker() {
//        return new TreeTableHacker();
        return new TreeTableHackerExt();
//        return new TreeTableHackerExt2();
    }

    /**
     * Temporary class to have all the hacking at one place. Naturally, it will
     * change a lot. The base class has the "stable" behaviour as of around
     * jun2006 (before starting the fix for 332-swingx). <p>
     * 
     * specifically:
     * 
     * <ol>
     * <li> hitHandleDetection triggeredn in editCellAt
     * </ol>
     * 
     */
    public class TreeTableHacker {

        protected boolean expansionChangedFlag;

        /**
         * Decision whether the handle hit detection
         *   should be done in processMouseEvent or editCellAt.
         * Here: returns false.
         * 
         * @return true for handle hit detection in processMouse, false
         *   for editCellAt.
         */
        protected boolean isHitDetectionFromProcessMouse() {
            return false;
        }

        /**
        * Entry point for hit handle detection called from editCellAt, 
        * does nothing if isHitDetectionFromProcessMouse is true;
        * 
        * @see #isHitDetectionFromProcessMouse()
        */
        public void hitHandleDetectionFromEditCell(int column, EventObject e) {
            if (!isHitDetectionFromProcessMouse()) {
                expandOrCollapseNode(column, e);
            }
        }

        /**
         * Entry point for hit handle detection called from processMouse.
         * Does nothing if isHitDetectionFromProcessMouse is false. 
         * 
         * @return true if the mouseEvent triggered an expand/collapse in
         *   the renderer, false otherwise. 
         *   
         * @see #isHitDetectionFromProcessMouse()
         */
        public boolean hitHandleDetectionFromProcessMouse(MouseEvent e) {
            if (!isHitDetectionFromProcessMouse())
                return false;
            int col = columnAtPoint(e.getPoint());
            return ((col >= 0) && expandOrCollapseNode(columnAtPoint(e
                    .getPoint()), e));
        }

        /**
         * Complete editing if collapsed/expanded.
         * <p>
         * 
         * Is: first try to stop editing before falling back to cancel.
         * <p>
         * This is part of fix for #730-swingx - editingStopped not always
         * called. The other part is to call this from the renderer before
         * expansion related state has changed.
         * <p>
         * 
         * Was: any editing is always cancelled.
         * <p>
         * This is a rude fix to #120-jdnc: data corruption on collapse/expand
         * if editing. This is called from the renderer after expansion related
         * state has changed.
         * 
         */
        protected void completeEditing() {
            if (isEditing()) {
                boolean success = getCellEditor().stopCellEditing();
                if (!success) {
                    getCellEditor().cancelCellEditing();
                }
            }
        }

        /**
         * Tricksery to make the tree expand/collapse.
         * <p>
         * 
         * This might be - indirectly - called from one of two places:
         * <ol>
         * <li> editCellAt: original, stable but buggy (#332, #222) the table's
         * own selection had been changed due to the click before even entering
         * into editCellAt so all tree selection state is lost.
         * 
         * <li> processMouseEvent: the idea is to catch the mouseEvent, check
         * if it triggered an expanded/collapsed, consume and return if so or 
         * pass to super if not.
         * </ol>
         * 
         * <p>
         * widened access for testing ...
         * 
         * 
         * @param column the column index under the event, if any.
         * @param e the event which might trigger a expand/collapse.
         * 
         * @return this methods evaluation as to whether the event triggered a
         *         expand/collaps
         */
        protected boolean expandOrCollapseNode(int column, EventObject e) {
            if (!isHierarchical(column))
                return false;
            if (!mightBeExpansionTrigger(e))
                return false;
            boolean changedExpansion = false;
            MouseEvent me = (MouseEvent) e;
            if (hackAroundDragEnabled(me)) {
                /*
                 * Hack around #168-jdnc: dirty little hack mentioned in the
                 * forum discussion about the issue: fake a mousePressed if drag
                 * enabled. The usability is slightly impaired because the
                 * expand/collapse is effectively triggered on released only
                 * (drag system intercepts and consumes all other).
                 */
                me = new MouseEvent((Component) me.getSource(),
                        MouseEvent.MOUSE_PRESSED, me.getWhen(), me
                                .getModifiers(), me.getX(), me.getY(), me
                                .getClickCount(), me.isPopupTrigger());

            }
            // If the modifiers are not 0 (or the left mouse button),
            // tree may try and toggle the selection, and table
            // will then try and toggle, resulting in the
            // selection remaining the same. To avoid this, we
            // only dispatch when the modifiers are 0 (or the left mouse
            // button).
            if (me.getModifiers() == 0
                    || me.getModifiers() == InputEvent.BUTTON1_MASK) {
                MouseEvent pressed = new MouseEvent(renderer, me.getID(), me
                        .getWhen(), me.getModifiers(), me.getX()
                        - getCellRect(0, column, false).x, me.getY(), me
                        .getClickCount(), me.isPopupTrigger());
                renderer.dispatchEvent(pressed);
                // For Mac OS X, we need to dispatch a MOUSE_RELEASED as well
                MouseEvent released = new MouseEvent(renderer,
                        java.awt.event.MouseEvent.MOUSE_RELEASED, pressed
                                .getWhen(), pressed.getModifiers(), pressed
                                .getX(), pressed.getY(), pressed
                                .getClickCount(), pressed.isPopupTrigger());
                renderer.dispatchEvent(released);
                if (expansionChangedFlag) {
                    changedExpansion = true;
                }
            }
            expansionChangedFlag = false;
            return changedExpansion;
        }

        protected boolean mightBeExpansionTrigger(EventObject e) {
            if (!(e instanceof MouseEvent)) return false;
            MouseEvent me = (MouseEvent) e;
            if (!SwingUtilities.isLeftMouseButton(me)) return false;
            return me.getID() == MouseEvent.MOUSE_PRESSED;
        }

        /**
         * called from the renderer's setExpandedPath after
         * all expansion-related updates happend.
         *
         */
        protected void expansionChanged() {
            expansionChangedFlag = true;
        }

    }

    /**
     * 
     * Note: currently this class looks a bit funny (only overriding
     * the hit decision method). That's because the "experimental" code
     * as of the last round moved to stable. But I expect that there's more
     * to come, so I leave it here.
     * 
     * <ol>
     * <li> hit handle detection in processMouse
     * </ol>
     */
    public class TreeTableHackerExt extends TreeTableHacker {


        /**
         * Here: returns true.
         * @inheritDoc
         */
        @Override
        protected boolean isHitDetectionFromProcessMouse() {
            return true;
        }

    }
    
    /**
     * Patch for #471-swingx: no selection on click in hierarchical column
     * outside of node-text. Mar 2007.
     * <p>
     * 
     * Note: this solves the selection issue but is not bidi-compliant - in RToL
     * contexts the expansion/collapse handles aren't detected and consequently
     * are disfunctional.
     * 
     * @author tiberiu@dev.java.net
     */
    public class TreeTableHackerExt2 extends TreeTableHackerExt {
        @Override
        protected boolean expandOrCollapseNode(int column, EventObject e) {
            if (!isHierarchical(column))
                return false;
            if (!mightBeExpansionTrigger(e))
                return false;
            boolean changedExpansion = false;
            MouseEvent me = (MouseEvent) e;
            if (hackAroundDragEnabled(me)) {
                /*
                 * Hack around #168-jdnc: dirty little hack mentioned in the
                 * forum discussion about the issue: fake a mousePressed if drag
                 * enabled. The usability is slightly impaired because the
                 * expand/collapse is effectively triggered on released only
                 * (drag system intercepts and consumes all other).
                 */
                me = new MouseEvent((Component) me.getSource(),
                        MouseEvent.MOUSE_PRESSED, me.getWhen(), me
                                .getModifiers(), me.getX(), me.getY(), me
                                .getClickCount(), me.isPopupTrigger());
            }
            // If the modifiers are not 0 (or the left mouse button),
            // tree may try and toggle the selection, and table
            // will then try and toggle, resulting in the
            // selection remaining the same. To avoid this, we
            // only dispatch when the modifiers are 0 (or the left mouse
            // button).
            if (me.getModifiers() == 0
                    || me.getModifiers() == InputEvent.BUTTON1_MASK) {
                // compute where the mouse point is relative to the tree
                // renderer
                Point treeMousePoint = getTreeMousePoint(column, me);
                int treeRow = renderer.getRowForLocation(treeMousePoint.x,
                        treeMousePoint.y);
                int row = 0;
                if (treeRow < 0) {
                    row = renderer.getClosestRowForLocation(treeMousePoint.x,
                            treeMousePoint.y);
                    Rectangle bounds = renderer.getRowBounds(row);
                    if (bounds == null) {
                        row = -1;
                    } else {
                        if ((bounds.y + bounds.height < treeMousePoint.y)
                                || bounds.x > treeMousePoint.x) {
                            row = -1;
                        }
                    }
                    // make sure the expansionChangedFlag is set to false for
                    // the case that up in the tree nothing happens
                    expansionChangedFlag = false;
                }

                if ((treeRow >= 0) || ((treeRow < 0) && (row < 0))) {
                    // default selection
                    MouseEvent pressed = new MouseEvent(renderer, me.getID(),
                            me.getWhen(), me.getModifiers(), treeMousePoint.x,
                            treeMousePoint.y, me.getClickCount(), me
                                    .isPopupTrigger());
                    renderer.dispatchEvent(pressed);
                    // For Mac OS X, we need to dispatch a MOUSE_RELEASED as
                    // well
                    MouseEvent released = new MouseEvent(renderer,
                            java.awt.event.MouseEvent.MOUSE_RELEASED, pressed
                                    .getWhen(), pressed.getModifiers(), pressed
                                    .getX(), pressed.getY(), pressed
                                    .getClickCount(), pressed.isPopupTrigger());
                    renderer.dispatchEvent(released);
                }
                if (expansionChangedFlag) {
                    changedExpansion = true;
                }
            }
            expansionChangedFlag = false;
            return changedExpansion;
        }

        /**
         * This is a patch provided for Issue #980-swingx which should
         * improve the bidi-compliance. Still doesn't work in our 
         * visual tests...
         * 
         * @param column the column index under the event, if any.
         * @param e the event which might trigger a expand/collapse.
         * @return the Point adjusted for bidi
         */
        protected Point getTreeMousePoint(int column, MouseEvent me) {
//            return new Point(me.getX()
//                    - getCellRect(0, column, false).x, me.getY());
            Rectangle tableCellRect = getCellRect(0, column, false);
           
            if( getComponentOrientation().isLeftToRight() ) {
                return new Point(me.getX() - tableCellRect.x, me.getY());
            }
 
            int x = (me.getX() - tableCellRect.x) - tableCellRect.width - 10;
            return new Point(x, me.getY());
        }
    }
    
    /**
     * decides whether we want to apply the hack for #168-jdnc. here: returns
     * true if dragEnabled() and the improved drag handling is not activated (or
     * the system property is not accessible). The given mouseEvent is not
     * analysed.
     * 
     * PENDING: Mustang?
     * 
     * @param me the mouseEvent that triggered a editCellAt
     * @return true if the hack should be applied.
     */
    protected boolean hackAroundDragEnabled(MouseEvent me) {
        Boolean dragHackFlag = (Boolean) getClientProperty(DRAG_HACK_FLAG_KEY);
        if (dragHackFlag == null) {
            // access and store the system property as a client property once
            String priority = null;
            try {
                priority = System.getProperty("sun.swing.enableImprovedDragGesture");

            } catch (Exception ex) {
                // found some foul expression or failed to read the property
            }
            dragHackFlag = (priority == null);
            putClientProperty(DRAG_HACK_FLAG_KEY, dragHackFlag);
        }
        return getDragEnabled() && dragHackFlag;
    }

    /**
     * Overridden to provide a workaround for BasicTableUI anomaly. Make sure
     * the UI never tries to resize the editor. The UI currently uses different
     * techniques to paint the renderers and editors. So, overriding setBounds()
     * is not the right thing to do for an editor. Returning -1 for the
     * editing row in this case, ensures the editor is never painted.
     *
     * {@inheritDoc}
     */
    @Override
    public int getEditingRow() {
        return isHierarchical(editingColumn) ? -1 : editingRow;
    }

    /**
     * Returns the actual row that is editing as <code>getEditingRow</code>
     * will always return -1.
     */
    private int realEditingRow() {
        return editingRow;
    }

    /**
     * Sets the data model for this JXTreeTable to the specified
     * {@link org.jdesktop.swingx.treetable.TreeTableModel}. The same data model
     * may be shared by any number of JXTreeTable instances.
     *
     * @param treeModel data model for this JXTreeTable
     */
    public void setTreeTableModel(TreeTableModel treeModel) {
        TreeTableModel old = getTreeTableModel();
//        boolean rootVisible = isRootVisible();
//        setRootVisible(false);
        renderer.setModel(treeModel);
//        setRootVisible(rootVisible);
        
        firePropertyChange("treeTableModel", old, getTreeTableModel());
    }

    /**
     * Returns the underlying TreeTableModel for this JXTreeTable.
     *
     * @return the underlying TreeTableModel for this JXTreeTable
     */
    public TreeTableModel getTreeTableModel() {
        return (TreeTableModel) renderer.getModel();
    }

    /**
     * <p>Overrides superclass version to make sure that the specified
     * {@link javax.swing.table.TableModel} is compatible with JXTreeTable before
     * invoking the inherited version.</p>
     *
     * <p>Because JXTreeTable internally adapts an
     * {@link org.jdesktop.swingx.treetable.TreeTableModel} to make it a compatible
     * TableModel, <b>this method should never be called directly</b>. Use
     * {@link #setTreeTableModel(org.jdesktop.swingx.treetable.TreeTableModel) setTreeTableModel} instead.</p>
     *
     * <p>While it is possible to obtain a reference to this adapted
     * version of the TableModel by calling {@link javax.swing.JTable#getModel()},
     * any attempt to call setModel() with that adapter will fail because
     * the adapter might have been bound to a different JXTreeTable instance. If
     * you want to extract the underlying TreeTableModel, which, by the way,
     * <em>can</em> be shared, use {@link #getTreeTableModel() getTreeTableModel}
     * instead</p>.
     *
     * @param tableModel must be a TreeTableModelAdapter
     * @throws IllegalArgumentException if the specified tableModel is not an
     * instance of TreeTableModelAdapter
     */
    @Override
    public final void setModel(TableModel tableModel) { // note final keyword
        if (tableModel instanceof TreeTableModelAdapter) {
            if (((TreeTableModelAdapter) tableModel).getTreeTable() == null) {
                // Passing the above test ensures that this method is being
                // invoked either from JXTreeTable/JTable constructor or from
                // setTreeTableModel(TreeTableModel)
                super.setModel(tableModel); // invoke superclass version

                ((TreeTableModelAdapter) tableModel).bind(this); // permanently bound
                // Once a TreeTableModelAdapter is bound to any JXTreeTable instance,
                // invoking JXTreeTable.setModel() with that adapter will throw an
                // IllegalArgumentException, because we really want to make sure
                // that a TreeTableModelAdapter is NOT shared by another JXTreeTable.
            }
            else {
                throw new IllegalArgumentException("model already bound");
            }
        }
        else {
            throw new IllegalArgumentException("unsupported model type");
        }
    }


    
    @Override
    public void tableChanged(TableModelEvent e) {
        if (isStructureChanged(e) || isUpdate(e)) {
            super.tableChanged(e);
        } else {
            resizeAndRepaint();
        }
    }

    /**
     *  Overridden to return a do-nothing mapper.
     *  
     */
    @Override
    public SelectionMapper getSelectionMapper() {
        if (selectionMapper == null) {
            selectionMapper = createSelectionMapper();
        }
        // JW: don't want to change super assumption 
        // (mapper != null) - the selection mapping will change 
        // anyway in Mustang (using core functionality)
        return selectionMapper;
    }

    /**
     * Fix for #745-swingx: remove static selectionMaper.
     * 
     * @return a SelectionMapper to use with this treeTable. Implemented
     *   with a no-op.
     */
    protected SelectionMapper createSelectionMapper() {

        SelectionMapper mapper = new SelectionMapper() {

            private ListSelectionModel viewSelectionModel = new DefaultListSelectionModel();

            public ListSelectionModel getViewSelectionModel() {
                return viewSelectionModel;
            }

            public void setViewSelectionModel(
                    ListSelectionModel viewSelectionModel) {
                this.viewSelectionModel = viewSelectionModel;
            }

            public void setFilters(FilterPipeline pipeline) {
                // do nothing
            }

            public void insertIndexInterval(int start, int length,
                    boolean before) {
                // do nothing
            }

            public void removeIndexInterval(int start, int end) {
                // do nothing
            }

            public void setEnabled(boolean enabled) {
                // do nothing
            }

            public boolean isEnabled() {
                return false;
            }

            public void clearModelSelection() {
                // do nothing
            }
        };
        return mapper;
    }
    
    /**
     * Throws UnsupportedOperationException because variable height rows are
     * not supported.
     *
     * @param row ignored
     * @param rowHeight ignored
     * @throws UnsupportedOperationException because variable height rows are
     * not supported
     */
    @Override
    public final void setRowHeight(int row, int rowHeight) {
        throw new UnsupportedOperationException("variable height rows not supported");
    }

    /**
     * Sets the row height for this JXTreeTable and forwards the 
     * row height to the renderering tree.
     * 
     * @param rowHeight height of a row.
     */
    @Override
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);
        adjustTreeRowHeight(getRowHeight()); 
    }

    /**
     * Forwards tableRowHeight to tree.
     * 
     * @param tableRowHeight height of a row.
     */
    protected void adjustTreeRowHeight(int tableRowHeight) {
        if (renderer != null && renderer.getRowHeight() != tableRowHeight) {
            renderer.setRowHeight(tableRowHeight);
        }
    }

    /**
     * Forwards treeRowHeight to table. This is for completeness only: the
     * rendering tree is under our total control, so we don't expect 
     * any external call to tree.setRowHeight.
     * 
     * @param treeRowHeight height of a row.
     */
    protected void adjustTableRowHeight(int treeRowHeight) {
        if (getRowHeight() != treeRowHeight) {
            adminSetRowHeight(treeRowHeight);
        }
    }


    /**
     * <p>Overridden to ensure that private renderer state is kept in sync with the
     * state of the component. Calls the inherited version after performing the
     * necessary synchronization. If you override this method, make sure you call
     * this version from your version of this method.</p>
     *
     * <p>This version maps the selection mode used by the renderer to match the
     * selection mode specified for the table. Specifically, the modes are mapped
     * as follows:
     * <pre>
     *  ListSelectionModel.SINGLE_INTERVAL_SELECTION: TreeSelectionModel.CONTIGUOUS_TREE_SELECTION;
     *  ListSelectionModel.MULTIPLE_INTERVAL_SELECTION: TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;
     *  any other (default): TreeSelectionModel.SINGLE_TREE_SELECTION;
     * </pre>
     *
     * {@inheritDoc}
     *
     * @param mode any of the table selection modes
     */
    @Override
    public void setSelectionMode(int mode) {
        if (renderer != null) {
            switch (mode) {
                case ListSelectionModel.SINGLE_INTERVAL_SELECTION: {
                    renderer.getSelectionModel().setSelectionMode(
                        TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
                    break;
                }
                case ListSelectionModel.MULTIPLE_INTERVAL_SELECTION: {
                    renderer.getSelectionModel().setSelectionMode(
                        TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
                    break;
                }
                default: {
                    renderer.getSelectionModel().setSelectionMode(
                        TreeSelectionModel.SINGLE_TREE_SELECTION);
                    break;
                }
            }
        }
        super.setSelectionMode(mode);
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to decorate the tree's renderer after calling super.
     * At that point, it is only the tree itself that has been decorated. 
     *
     * @param renderer the <code>TableCellRenderer</code> to prepare
     * @param row the row of the cell to render, where 0 is the first row
     * @param column the column of the cell to render, where 0 is the first column
     * @return the <code>Component</code> used as a stamp to render the specified cell
     * 
     * @see #applyRenderer(Component, ComponentAdapter)
     */
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row,
        int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        return applyRenderer(component, getComponentAdapter(row, column)); 
    }

    /**
     * Performs configuration of the tree's renderer if the adapter's column is
     * the hierarchical column, does nothing otherwise.
     * <p>
     * 
     * Note: this is legacy glue if the treeCellRenderer is of type
     * DefaultTreeCellRenderer. In that case the renderer's
     * background/foreground/Non/Selection colors are set to the tree's
     * background/foreground depending on the adapter's selection state. Does
     * nothing if the treeCellRenderer is backed by a ComponentProvider.
     * 
     * @param component the rendering component
     * @param adapter component data adapter
     * @throws NullPointerException if the specified component or adapter is
     *         null
     */
    protected Component applyRenderer(Component component,
            ComponentAdapter adapter) {
        if (component == null) {
            throw new IllegalArgumentException("null component");
        }
        if (adapter == null) {
            throw new IllegalArgumentException("null component data adapter");
        }

        if (isHierarchical(adapter.column)) {
            // After all decorators have been applied, make sure that relevant
            // attributes of the table cell renderer are applied to the
            // tree cell renderer before the hierarchical column is rendered!
            TreeCellRenderer tcr = renderer.getCellRenderer();
            if (tcr instanceof JXTree.DelegatingRenderer) {
                tcr = ((JXTree.DelegatingRenderer) tcr).getDelegateRenderer();

            }
            if (tcr instanceof DefaultTreeCellRenderer) {

                DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);
                // this effectively overwrites the dtcr settings
                if (adapter.isSelected()) {
                    dtcr.setTextSelectionColor(component.getForeground());
                    dtcr.setBackgroundSelectionColor(component.getBackground());
                } else {
                    dtcr.setTextNonSelectionColor(component.getForeground());
                    dtcr.setBackgroundNonSelectionColor(component
                            .getBackground());
                }
            }
        }
        return component;
    }

    /**
     * Sets the specified TreeCellRenderer as the Tree cell renderer.
     *
     * @param cellRenderer to use for rendering tree cells.
     */
    public void setTreeCellRenderer(TreeCellRenderer cellRenderer) {
        if (renderer != null) {
            renderer.setCellRenderer(cellRenderer);
        }
    }

    public TreeCellRenderer getTreeCellRenderer() {
        return renderer.getCellRenderer();
    }

    
    @Override
    public String getToolTipText(MouseEvent event) {
        int column = columnAtPoint(event.getPoint());
        if (isHierarchical(column)) {
            int row = rowAtPoint(event.getPoint());
            return renderer.getToolTipText(event, row, column);
        }
        return super.getToolTipText(event);
    }
    
    /**
     * Sets the specified icon as the icon to use for rendering collapsed nodes.
     *
     * @param icon to use for rendering collapsed nodes
     * 
     * @see JXTree#setCollapsedIcon(Icon)
     */
    public void setCollapsedIcon(Icon icon) {
        renderer.setCollapsedIcon(icon);
    }

    /**
     * Sets the specified icon as the icon to use for rendering expanded nodes.
     *
     * @param icon to use for rendering expanded nodes
     * 
     * @see JXTree#setExpandedIcon(Icon)
     */
    public void setExpandedIcon(Icon icon) {
        renderer.setExpandedIcon(icon);
    }

    /**
     * Sets the specified icon as the icon to use for rendering open container nodes.
     *
     * @param icon to use for rendering open nodes
     * 
     * @see JXTree#setOpenIcon(Icon)
     */
    public void setOpenIcon(Icon icon) {
        renderer.setOpenIcon(icon);
    }

    /**
     * Sets the specified icon as the icon to use for rendering closed container nodes.
     *
     * @param icon to use for rendering closed nodes
     * 
     * @see JXTree#setClosedIcon(Icon)
     */
    public void setClosedIcon(Icon icon) {
        renderer.setClosedIcon(icon);
    }

    /**
     * Sets the specified icon as the icon to use for rendering leaf nodes.
     *
     * @param icon to use for rendering leaf nodes
     * 
     * @see JXTree#setLeafIcon(Icon)
     */
    public void setLeafIcon(Icon icon) {
        renderer.setLeafIcon(icon);
    }

    /**
     * Property to control whether per-tree icons should be 
     * copied to the renderer on setTreeCellRenderer. <p>
     * 
     * The default value is false.
     * 
     * @param overwrite a boolean to indicate if the per-tree Icons should
     *   be copied to the new renderer on setTreeCellRenderer.
     * 
     * @see #isOverwriteRendererIcons()  
     * @see #setLeafIcon(Icon)
     * @see #setOpenIcon(Icon)
     * @see #setClosedIcon(Icon) 
     * @see JXTree#setOverwriteRendererIcons(boolean) 
     */
    public void setOverwriteRendererIcons(boolean overwrite) {
        renderer.setOverwriteRendererIcons(overwrite);
    }


    /**
     * Returns a boolean indicating whether the per-tree icons should be 
     * copied to the renderer on setTreeCellRenderer.
     * 
     * @return true if a TreeCellRenderer's icons will be overwritten with the
     *   tree's Icons, false if the renderer's icons will be unchanged.
     *   
     * @see #setOverwriteRendererIcons(boolean)
     * @see #setLeafIcon(Icon)
     * @see #setOpenIcon(Icon)
     * @see #setClosedIcon(Icon)  
     * @see JXTree#isOverwriteRendererIcons()
     *     
     */
    public boolean isOverwriteRendererIcons() {
        return renderer.isOverwriteRendererIcons();
    }
    
    /**
     * Overridden to ensure that private renderer state is kept in sync with the
     * state of the component. Calls the inherited version after performing the
     * necessary synchronization. If you override this method, make sure you call
     * this version from your version of this method.
     */
    @Override
    public void clearSelection() {
        if (renderer != null) {
            renderer.clearSelection();
        }
        super.clearSelection();
    }

    /**
     * Collapses all nodes in the treetable.
     */
    public void collapseAll() {
        renderer.collapseAll();
    }

    /**
     * Expands all nodes in the treetable.
     */
    public void expandAll() {
        renderer.expandAll();
    }

    /**
     * Collapses the node at the specified path in the treetable.
     *
     * @param path path of the node to collapse
     */
    public void collapsePath(TreePath path) {
        renderer.collapsePath(path);
    }

    /**
     * Expands the the node at the specified path in the treetable.
     *
     * @param path path of the node to expand
     */
    public void expandPath(TreePath path) {
        renderer.expandPath(path);
    }

    /**
     * Makes sure all the path components in path are expanded (except
     * for the last path component) and scrolls so that the 
     * node identified by the path is displayed. Only works when this
     * <code>JTree</code> is contained in a <code>JScrollPane</code>.
     * 
     * (doc copied from JTree)
     * 
     * PENDING: JW - where exactly do we want to scroll? Here: the scroll
     * is in vertical direction only. Might need to show the tree column?
     * 
     * @param path  the <code>TreePath</code> identifying the node to
     *          bring into view
     */
    public void scrollPathToVisible(TreePath path) {
        renderer.scrollPathToVisible(path);
//        if (path == null) return;
//        renderer.makeVisible(path);
//        int row = getRowForPath(path);
//        scrollRowToVisible(row);
    }

    
    /**
     * Collapses the row in the treetable. If the specified row index is
     * not valid, this method will have no effect.
     */
    public void collapseRow(int row) {
        renderer.collapseRow(row);
    }

    /**
     * Expands the specified row in the treetable. If the specified row index is
     * not valid, this method will have no effect.
     */
    public void expandRow(int row) {
        renderer.expandRow(row);
    }

    
    /**
     * Returns true if the value identified by path is currently viewable, which
     * means it is either the root or all of its parents are expanded. Otherwise,
     * this method returns false.
     *
     * @return true, if the value identified by path is currently viewable;
     * false, otherwise
     */
    public boolean isVisible(TreePath path) {
        return renderer.isVisible(path);
    }

    /**
     * Returns true if the node identified by path is currently expanded.
     * Otherwise, this method returns false.
     *
     * @param path path
     * @return true, if the value identified by path is currently expanded;
     * false, otherwise
     */
    public boolean isExpanded(TreePath path) {
        return renderer.isExpanded(path);
    }

    /**
     * Returns true if the node at the specified display row is currently expanded.
     * Otherwise, this method returns false.
     *
     * @param row row
     * @return true, if the node at the specified display row is currently expanded.
     * false, otherwise
     */
    public boolean isExpanded(int row) {
        return renderer.isExpanded(row);
    }

    /**
     * Returns true if the node identified by path is currently collapsed, 
     * this will return false if any of the values in path are currently not 
     * being displayed.   
     *
     * @param path path
     * @return true, if the value identified by path is currently collapsed;
     * false, otherwise
     */
    public boolean isCollapsed(TreePath path) {
        return renderer.isCollapsed(path);
    }

    /**
     * Returns true if the node at the specified display row is collapsed.
     *
     * @param row row
     * @return true, if the node at the specified display row is currently collapsed.
     * false, otherwise
     */
    public boolean isCollapsed(int row) {
        return renderer.isCollapsed(row);
    }

    
    /**
     * Returns an <code>Enumeration</code> of the descendants of the
     * path <code>parent</code> that
     * are currently expanded. If <code>parent</code> is not currently
     * expanded, this will return <code>null</code>.
     * If you expand/collapse nodes while
     * iterating over the returned <code>Enumeration</code>
     * this may not return all
     * the expanded paths, or may return paths that are no longer expanded.
     *
     * @param parent  the path which is to be examined
     * @return an <code>Enumeration</code> of the descendents of 
     *        <code>parent</code>, or <code>null</code> if
     *        <code>parent</code> is not currently expanded
     */
    
    public Enumeration<?> getExpandedDescendants(TreePath parent) {
        return renderer.getExpandedDescendants(parent);
    }

    
    /**
     * Returns the TreePath for a given x,y location.
     *
     * @param x x value
     * @param y y value
     *
     * @return the <code>TreePath</code> for the givern location.
     */
     public TreePath getPathForLocation(int x, int y) {
        int row = rowAtPoint(new Point(x,y));
        if (row == -1) {
          return null;  
        }
        return renderer.getPathForRow(row);
     }

    /**
     * Returns the TreePath for a given row.
     *
     * @param row
     *
     * @return the <code>TreePath</code> for the given row.
     */
     public TreePath getPathForRow(int row) {
        return renderer.getPathForRow(row);
     }

     /**
      * Returns the row for a given TreePath.
      *
      * @param path
      * @return the row for the given <code>TreePath</code>.
      */
     public int getRowForPath(TreePath path) {
       return renderer.getRowForPath(path);
     }

//------------------------------ exposed Tree properties

     /**
      * Determines whether or not the root node from the TreeModel is visible.
      *
      * @param visible true, if the root node is visible; false, otherwise
      */
     public void setRootVisible(boolean visible) {
         renderer.setRootVisible(visible);
         // JW: the revalidate forces the root to appear after a 
         // toggling a visible from an initially invisible root.
         // JTree fires a propertyChange on the ROOT_VISIBLE_PROPERTY
         // BasicTreeUI reacts by (ultimately) calling JTree.treeDidChange
         // which revalidate the tree part. 
         // Might consider to listen for the propertyChange (fired only if there
         // actually was a change) instead of revalidating unconditionally.
         revalidate();
         repaint();
     }

     /**
      * Returns true if the root node of the tree is displayed.
      *
      * @return true if the root node of the tree is displayed
      */
     public boolean isRootVisible() {
         return renderer.isRootVisible();
     }


    /**
     * Sets the value of the <code>scrollsOnExpand</code> property for the tree
     * part. This property specifies whether the expanded paths should be scrolled
     * into view. In a look and feel in which a tree might not need to scroll
     * when expanded, this property may be ignored.
     *
     * @param scroll true, if expanded paths should be scrolled into view;
     * false, otherwise
     */
    public void setScrollsOnExpand(boolean scroll) {
        renderer.setScrollsOnExpand(scroll);
    }

    /**
     * Returns the value of the <code>scrollsOnExpand</code> property.
     *
     * @return the value of the <code>scrollsOnExpand</code> property
     */
    public boolean getScrollsOnExpand() {
        return renderer.getScrollsOnExpand();
    }

    /**
     * Sets the value of the <code>showsRootHandles</code> property for the tree
     * part. This property specifies whether the node handles should be displayed.
     * If handles are not supported by a particular look and feel, this property
     * may be ignored.
     *
     * @param visible true, if root handles should be shown; false, otherwise
     */
    public void setShowsRootHandles(boolean visible) {
        renderer.setShowsRootHandles(visible);
        repaint();
    }

    /**
     * Returns the value of the <code>showsRootHandles</code> property.
     *
     * @return the value of the <code>showsRootHandles</code> property
     */
    public boolean getShowsRootHandles() {
        return renderer.getShowsRootHandles();
    }

    /**
     * Sets the value of the <code>expandsSelectedPaths</code> property for the tree
     * part. This property specifies whether the selected paths should be expanded.
     *
     * @param expand true, if selected paths should be expanded; false, otherwise
     */
    public void setExpandsSelectedPaths(boolean expand) {
        renderer.setExpandsSelectedPaths(expand);
    }

    /**
     * Returns the value of the <code>expandsSelectedPaths</code> property.
     *
     * @return the value of the <code>expandsSelectedPaths</code> property
     */
    public boolean getExpandsSelectedPaths() {
        return renderer.getExpandsSelectedPaths();
    }


    /**
     * Returns the number of mouse clicks needed to expand or close a node.
     *
     * @return number of mouse clicks before node is expanded
     */
    public int getToggleClickCount() {
        return renderer.getToggleClickCount();
    }

    /**
     * Sets the number of mouse clicks before a node will expand or close.
     * The default is two. 
     *
     * @param clickCount the number of clicks required to expand/collapse a node.
     */
    public void setToggleClickCount(int clickCount) {
        renderer.setToggleClickCount(clickCount);
    }


//------------------------------ exposed tree listeners
    
    /**
     * Adds a listener for <code>TreeExpansion</code> events.
     * 
     * TODO (JW): redirect event source to this. 
     * 
     * @param tel a TreeExpansionListener that will be notified 
     * when a tree node is expanded or collapsed
     */
    public void addTreeExpansionListener(TreeExpansionListener tel) {
        renderer.addTreeExpansionListener(tel);
    }

    /**
     * Removes a listener for <code>TreeExpansion</code> events.
     * @param tel the <code>TreeExpansionListener</code> to remove
     */
    public void removeTreeExpansionListener(TreeExpansionListener tel) {
        renderer.removeTreeExpansionListener(tel);
    }

    /**
     * Adds a listener for <code>TreeSelection</code> events.
     * TODO (JW): redirect event source to this. 
     * 
     * @param tsl a TreeSelectionListener that will be notified 
     * when a tree node is selected or deselected
     */
    public void addTreeSelectionListener(TreeSelectionListener tsl) {
        renderer.addTreeSelectionListener(tsl);
    }

    /**
     * Removes a listener for <code>TreeSelection</code> events.
     * @param tsl the <code>TreeSelectionListener</code> to remove
     */
    public void removeTreeSelectionListener(TreeSelectionListener tsl) {
        renderer.removeTreeSelectionListener(tsl);
    }

    /**
     * Adds a listener for <code>TreeWillExpand</code> events.
     * TODO (JW): redirect event source to this. 
     * 
     * @param tel a TreeWillExpandListener that will be notified 
     * when a tree node will be expanded or collapsed 
     */
    public void addTreeWillExpandListener(TreeWillExpandListener tel) {
        renderer.addTreeWillExpandListener(tel);
    }

    /**
     * Removes a listener for <code>TreeWillExpand</code> events.
     * @param tel the <code>TreeWillExpandListener</code> to remove
     */
    public void removeTreeWillExpandListener(TreeWillExpandListener tel) {
        renderer.removeTreeWillExpandListener(tel);
     }
 
    
    /**
     * Returns the selection model for the tree portion of the this treetable.
     *
     * @return selection model for the tree portion of the this treetable
     */
    public TreeSelectionModel getTreeSelectionModel() {
        return renderer.getSelectionModel();    // RG: Fix JDNC issue 41
    }

    /**
     * Overriden to invoke supers implementation, and then,
     * if the receiver is editing a Tree column, the editors bounds is
     * reset. The reason we have to do this is because JTable doesn't
     * think the table is being edited, as <code>getEditingRow</code> returns
     * -1, and therefore doesn't automaticly resize the editor for us.
     */
    @Override
    public void sizeColumnsToFit(int resizingColumn) {
        /** TODO: Review wrt doLayout() */
        super.sizeColumnsToFit(resizingColumn);
        // rg:changed
        if (getEditingColumn() != -1 && isHierarchical(editingColumn)) {
            Rectangle cellRect = getCellRect(realEditingRow(),
                getEditingColumn(), false);
            Component component = getEditorComponent();
            component.setBounds(cellRect);
            component.validate();
        }
    }


    /**
     * Determines if the specified column is defined as the hierarchical column.
     * 
     * @param column
     *            zero-based index of the column in view coordinates
     * @return true if the column is the hierarchical column; false otherwise.
     * @throws IllegalArgumentException
     *             if the column is less than 0 or greater than or equal to the
     *             column count
     */
    public boolean isHierarchical(int column) {
        if (column < 0 || column >= getColumnCount()) {
            throw new IllegalArgumentException("column must be valid, was" + column);
        }
        
        return (getHierarchicalColumn() == column);
    }

    /**
     * Returns the index of the hierarchical column. This is the column that is
     * displayed as the tree.
     * 
     * @return the index of the hierarchical column, -1 if there is
     *   no hierarchical column
     * 
     */
    public int getHierarchicalColumn() {
        return convertColumnIndexToView(((TreeTableModel) renderer.getModel()).getHierarchicalColumn());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (isHierarchical(column)) {
            return renderer;
        }
        
        return super.getCellRenderer(row, column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (isHierarchical(column)) {
            return hierarchicalEditor;
        }
        
        return super.getCellEditor(row, column);
    }
    
    
    @Override
    public void updateUI() {
        super.updateUI();
        updateHierarchicalRendererEditor();
    }

    /**
     * Updates Ui of renderer/editor for the hierarchical column. Need to do so
     * manually, as not accessible by the default lookup.
     */
    protected void updateHierarchicalRendererEditor() {
        if (renderer != null) {
           SwingUtilities.updateComponentTreeUI(renderer);
        }
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to message the tree directly if the column is the view index of
     * the hierarchical column. <p>
     * 
     * PENDING JW: revisit once we switch to really using a table renderer. As is, it's
     * a quick fix for #821-swingx: string rep for hierarchical column incorrect.
     */
    @Override
    public String getStringAt(int row, int column) {
        if (isHierarchical(column)) {
            return getHierarchicalStringAt(row);
        }
        return super.getStringAt(row, column);
    }

    /**
     * Returns the String representation of the hierarchical column at the given 
     * row. <p>
     * 
     * @param row the row index in view coordinates
     * @return the string representation of the hierarchical column at the given row.
     * 
     * @see #getStringAt(int, int)
     */
    private String getHierarchicalStringAt(int row) {
        return renderer.getStringAt(row);
    }

    /**
     * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel
     * to listen for changes in the ListSelectionModel it maintains. Once
     * a change in the ListSelectionModel happens, the paths are updated
     * in the DefaultTreeSelectionModel.
     */
    class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel {
        /** Set to true when we are updating the ListSelectionModel. */
        protected boolean updatingListSelectionModel;

        public ListToTreeSelectionModelWrapper() {
            super();
            getListSelectionModel().addListSelectionListener
                (createListSelectionListener());
        }

        /**
         * Returns the list selection model. ListToTreeSelectionModelWrapper
         * listens for changes to this model and updates the selected paths
         * accordingly.
         */
        ListSelectionModel getListSelectionModel() {
            return listSelectionModel;
        }

        /**
         * This is overridden to set <code>updatingListSelectionModel</code>
         * and message super. This is the only place DefaultTreeSelectionModel
         * alters the ListSelectionModel.
         */
        @Override
        public void resetRowSelection() {
            if (!updatingListSelectionModel) {
                updatingListSelectionModel = true;
                try {
                    super.resetRowSelection();
                }
                finally {
                    updatingListSelectionModel = false;
                }
            }
            // Notice how we don't message super if
            // updatingListSelectionModel is true. If
            // updatingListSelectionModel is true, it implies the
            // ListSelectionModel has already been updated and the
            // paths are the only thing that needs to be updated.
        }

        /**
         * Creates and returns an instance of ListSelectionHandler.
         */
        protected ListSelectionListener createListSelectionListener() {
            return new ListSelectionHandler();
        }

        /**
         * If <code>updatingListSelectionModel</code> is false, this will
         * reset the selected paths from the selected rows in the list
         * selection model.
         */
        protected void updateSelectedPathsFromSelectedRows() {
            if (!updatingListSelectionModel) {
                updatingListSelectionModel = true;
                try {
                    if (listSelectionModel.isSelectionEmpty()) {
                        clearSelection();
                    } else {
                        // This is way expensive, ListSelectionModel needs an
                        // enumerator for iterating.
                        int min = listSelectionModel.getMinSelectionIndex();
                        int max = listSelectionModel.getMaxSelectionIndex();

                        List<TreePath> paths = new ArrayList<TreePath>();
                        for (int counter = min; counter <= max; counter++) {
                            if (listSelectionModel.isSelectedIndex(counter)) {
                                TreePath selPath = renderer.getPathForRow(
                                    counter);

                                if (selPath != null) {
                                    paths.add(selPath);
                                }
                            }
                        }
                        setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
                        // need to force here: usually the leadRow is adjusted 
                        // in resetRowSelection which is disabled during this method
                        leadRow = leadIndex;
                    }
                }
                finally {
                    updatingListSelectionModel = false;
                }
            }
        }

        /**
         * Class responsible for calling updateSelectedPathsFromSelectedRows
         * when the selection of the list changse.
         */
        class ListSelectionHandler implements ListSelectionListener {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updateSelectedPathsFromSelectedRows();
                }
            }
        }
    }

    /**
     * 
     */
    protected static class TreeTableModelAdapter extends AbstractTableModel {
        private TreeModelListener treeModelListener;
        
        /**
         * Maintains a TreeTableModel and a JTree as purely implementation details.
         * Developers can plug in any type of custom TreeTableModel through a
         * JXTreeTable constructor or through setTreeTableModel().
         *
         * @param model Underlying data model for the JXTreeTable that will ultimately
         * be bound to this TreeTableModelAdapter
         * @param tree TreeTableCellRenderer instantiated with the same model as
         * specified by the model parameter of this constructor
         * @throws IllegalArgumentException if a null model argument is passed
         * @throws IllegalArgumentException if a null tree argument is passed
         */
        TreeTableModelAdapter(JTree tree) {
            assert tree != null;

            this.tree = tree; // need tree to implement getRowCount()
            tree.getModel().addTreeModelListener(getTreeModelListener());
            tree.addTreeExpansionListener(new TreeExpansionListener() {
                // Don't use fireTableRowsInserted() here; the selection model
                // would get updated twice.
                public void treeExpanded(TreeExpansionEvent event) {
                    updateAfterExpansionEvent(event);
                }

                public void treeCollapsed(TreeExpansionEvent event) {
                    updateAfterExpansionEvent(event);
                }
            });
            tree.addPropertyChangeListener("model", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    TreeTableModel model = (TreeTableModel) evt.getOldValue();
                    model.removeTreeModelListener(getTreeModelListener());
                    
                    model = (TreeTableModel) evt.getNewValue();
                    model.addTreeModelListener(getTreeModelListener());
                    
                    fireTableStructureChanged();
                }
            });
        }

        /**
         * updates the table after having received an TreeExpansionEvent.<p>
         * 
         * @param event the TreeExpansionEvent which triggered the method call.
         */
        protected void updateAfterExpansionEvent(TreeExpansionEvent event) {
            // moved to let the renderer handle directly
//            treeTable.getTreeTableHacker().setExpansionChangedFlag();
            // JW: delayed fire leads to a certain sluggishness occasionally? 
            fireTableDataChanged();
        }

        /**
         * Returns the JXTreeTable instance to which this TreeTableModelAdapter is
         * permanently and exclusively bound. For use by
         * {@link org.jdesktop.swingx.JXTreeTable#setModel(javax.swing.table.TableModel)}.
         *
         * @return JXTreeTable to which this TreeTableModelAdapter is permanently bound
         */
        protected JXTreeTable getTreeTable() {
            return treeTable;
        }

        /**
         * Immutably binds this TreeTableModelAdapter to the specified JXTreeTable.
         *
         * @param treeTable the JXTreeTable instance that this adapter is bound to.
         */
        protected final void bind(JXTreeTable treeTable) {
            // Suppress potentially subversive invocation!
            // Prevent clearing out the deck for possible hijack attempt later!
            if (treeTable == null) {
                throw new IllegalArgumentException("null treeTable");
            }

            if (this.treeTable == null) {
                this.treeTable = treeTable;
            }
            else {
                throw new IllegalArgumentException("adapter already bound");
            }
        }

        // Wrappers, implementing TableModel interface.
        // TableModelListener management provided by AbstractTableModel superclass.

        @Override
        public Class<?> getColumnClass(int column) {
            return ((TreeTableModel) tree.getModel()).getColumnClass(column);
        }

        public int getColumnCount() {
            return ((TreeTableModel) tree.getModel()).getColumnCount();
        }

        @Override
        public String getColumnName(int column) {
            return ((TreeTableModel) tree.getModel()).getColumnName(column);
        }

        public int getRowCount() {
            return tree.getRowCount();
        }

        public Object getValueAt(int row, int column) {
            // Issue #270-swingx: guard against invisible row
            Object node = nodeForRow(row);
            return node != null ? ((TreeTableModel) tree.getModel()).getValueAt(node, column) : null;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            // Issue #270-swingx: guard against invisible row
            Object node = nodeForRow(row);
            return node != null ? ((TreeTableModel) tree.getModel()).isCellEditable(node, column) : false;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            // Issue #270-swingx: guard against invisible row
            Object node = nodeForRow(row);
            if (node != null) {
                ((TreeTableModel) tree.getModel()).setValueAt(value, node, column);
            }
        }

        protected Object nodeForRow(int row) {
            // Issue #270-swingx: guard against invisible row
            TreePath path = tree.getPathForRow(row);
            return path != null ? path.getLastPathComponent() : null;
        }

        /**
         * @return <code>TreeModelListener</code>
         */
        private TreeModelListener getTreeModelListener() {
            if (treeModelListener == null) {
                treeModelListener = new TreeModelListener() {
                    
                    public void treeNodesChanged(TreeModelEvent e) {
//                        LOG.info("got tree event: changed " + e);
                        delayedFireTableDataUpdated(e);
                    }   

                    // We use delayedFireTableDataChanged as we can
                    // not be guaranteed the tree will have finished processing
                    // the event before us.
                    public void treeNodesInserted(TreeModelEvent e) {
                        delayedFireTableDataChanged(e, 1);
                    }

                    public void treeNodesRemoved(TreeModelEvent e) {
//                        LOG.info("got tree event: removed " + e);
                       delayedFireTableDataChanged(e, 2);
                    }

                    public void treeStructureChanged(TreeModelEvent e) {
                        // ?? should be mapped to structureChanged -- JW
                        if (isTableStructureChanged(e)) {
                            delayedFireTableStructureChanged();
                        } else {
                            delayedFireTableDataChanged();
                        }
                    }
                };
            }
            
            return treeModelListener;
        }

        /**
         * Decides if the given treeModel structureChanged should 
         * trigger a table structureChanged. Returns true if the 
         * source path is the root or null, false otherwise.<p>
         * 
         * PENDING: need to refine? "Marker" in Event-Object?
         * 
         * @param e the TreeModelEvent received in the treeModelListener's 
         *   treeStructureChanged
         * @return a boolean indicating whether the given TreeModelEvent
         *   should trigger a structureChanged.
         */
        private boolean isTableStructureChanged(TreeModelEvent e) {
            if ((e.getTreePath() == null) ||
                    (e.getTreePath().getParentPath() == null)) return true;
            return false;
        }

        /**
         * Invokes fireTableDataChanged after all the pending events have been
         * processed. SwingUtilities.invokeLater is used to handle this.
         */
        private void delayedFireTableStructureChanged() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fireTableStructureChanged();
                }
            });
        }

        /**
         * Invokes fireTableDataChanged after all the pending events have been
         * processed. SwingUtilities.invokeLater is used to handle this.
         */
        private void delayedFireTableDataChanged() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fireTableDataChanged();
                }
            });
        }

        /**
         * Invokes fireTableDataChanged after all the pending events have been
         * processed. SwingUtilities.invokeLater is used to handle this.
         * Allowed event types: 1 for insert, 2 for delete
         */
        private void delayedFireTableDataChanged(final TreeModelEvent tme, final int typeChange) {
            if ((typeChange < 1 ) || (typeChange > 2)) 
                throw new IllegalArgumentException("Event type must be 1 or 2, was " + typeChange);
            // expansion state before invoke may be different 
            // from expansion state in invoke 
            final boolean expanded = tree.isExpanded(tme.getTreePath());
            // quick test if tree throws for unrelated path. Seems like not.
//            tree.getRowForPath(new TreePath("dummy"));
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    int indices[] = tme.getChildIndices();
                    TreePath path = tme.getTreePath();
                    // quick test to see if bailing out is an option
//                    if (false) {
                    if (indices != null) {
                        if (expanded) { // Dont bother to update if the parent
                            // node is collapsed
                            // indices must in ascending order, as per TreeEvent/Listener doc
                            int min = indices[0];
                            int max = indices[indices.length - 1];
                            int startingRow = tree.getRowForPath(path) + 1;
                            min = startingRow + min;
                            max = startingRow + max;
                            switch (typeChange) {
                            case 1:
//                                LOG.info("rows inserted: path " + path + "/" + min + "/"
//                                        + max);
                                fireTableRowsInserted(min, max);
                                break;
                            case 2:
//                                LOG.info("rows deleted path " + path + "/" + min + "/"
//                                                + max);
                                fireTableRowsDeleted(min, max);
                                break;
                            }
                        } else {
                            // not expanded - but change might effect appearance
                            // of parent
                            // Issue #82-swingx
                            int row = tree.getRowForPath(path);
                            // fix Issue #247-swingx: prevent accidental
                            // structureChanged
                            // for collapsed path
                            // in this case row == -1, which ==
                            // TableEvent.HEADER_ROW
                            if (row >= 0)
                                fireTableRowsUpdated(row, row);
                        }
                    } else { // case where the event is fired to identify
                                // root.
                        fireTableDataChanged();
                    }
                }
            });
        }

        /**
         * This is used for updated only. PENDING: not necessary to delay?
         * Updates are never structural changes which are the critical.
         * 
         * @param tme
         */
        protected void delayedFireTableDataUpdated(final TreeModelEvent tme) {
            final boolean expanded = tree.isExpanded(tme.getTreePath());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    int indices[] = tme.getChildIndices();
                    TreePath path = tme.getTreePath();
                    if (indices != null) {
                        if (expanded) { // Dont bother to update if the parent
                            // node is collapsed
                            Object children[] = tme.getChildren();
                            // can we be sure that children.length > 0?
                            // int min = tree.getRowForPath(path.pathByAddingChild(children[0]));
                            // int max = tree.getRowForPath(path.pathByAddingChild(children[children.length -1]));
                            int min = Integer.MAX_VALUE;
                            int max = Integer.MIN_VALUE;
                            for (int i = 0; i < indices.length; i++) {
                                Object child = children[i];
                                TreePath childPath = path
                                        .pathByAddingChild(child);
                                int index = tree.getRowForPath(childPath);
                                if (index < min) {
                                    min = index;
                                }
                                if (index > max) {
                                    max = index;
                                }
                            }
//                            LOG.info("Updated: parentPath/min/max" + path + "/" + min + "/" + max);
                            // JW: the index is occasionally - 1 - need further digging 
                            fireTableRowsUpdated(Math.max(0, min), Math.max(0, max));
                        } else {
                            // not expanded - but change might effect appearance
                            // of parent Issue #82-swingx
                            int row = tree.getRowForPath(path);
                            // fix Issue #247-swingx: prevent accidental structureChanged
                            // for collapsed path in this case row == -1, 
                            // which == TableEvent.HEADER_ROW
                            if (row >= 0)
                                fireTableRowsUpdated(row, row);
                        }
                    } else { // case where the event is fired to identify
                                // root.
                        fireTableDataChanged();
                    }
                }
            });

        }

        private final JTree tree; // immutable
        private JXTreeTable treeTable = null; // logically immutable
    }

    static class TreeTableCellRenderer extends JXTree implements
        TableCellRenderer
        // need to implement RolloverRenderer
        // PENDING JW: method name clash rolloverRenderer.isEnabled and
        // component.isEnabled .. don't extend, use? And change
        // the method name in rolloverRenderer? 
        // commented - so doesn't show the rollover cursor.
        // 
//      ,  RolloverRenderer 
        {
        private PropertyChangeListener rolloverListener;

        // Force user to specify TreeTableModel instead of more general
        // TreeModel
        public TreeTableCellRenderer(TreeTableModel model) {
            super(model);
            putClientProperty("JTree.lineStyle", "None");
            setRootVisible(false); // superclass default is "true"
            setShowsRootHandles(true); // superclass default is "false"
                /**
                 * TODO: Support truncated text directly in
                 * DefaultTreeCellRenderer.
                 */
            // removed as fix for #769-swingx: defaults for treetable should be same as tree
//            setOverwriteRendererIcons(true);
// setCellRenderer(new DefaultTreeRenderer());
            setCellRenderer(new ClippedTreeCellRenderer());
        }

        
        /**
         * {@inheritDoc} <p>
         * 
         * Overridden to hack around #766-swingx: cursor flickering in DnD
         * when dragging over tree column. This is a core bug (#6700748) related
         * to painting the rendering component on a CellRendererPane. A trick
         * around is to let this return false. <p>
         * 
         * This implementation applies the trick, that is returns false always. 
         * The hack can be disabled by setting the treeTable's client property
         * DROP_HACK_FLAG_KEY to Boolean.FALSE. 
         * 
         */
        @Override
        public boolean isVisible() {
            return shouldApplyDropHack() ? false : super.isVisible();
        }


        /**
         * Returns a boolean indicating whether the drop hack should be applied.
         * 
         * @return a boolean indicating whether the drop hack should be applied.
         */
        protected boolean shouldApplyDropHack() {
            return !Boolean.FALSE.equals(treeTable.getClientProperty(DROP_HACK_FLAG_KEY));
        }


        /**
         * Hack around #297-swingx: tooltips shown at wrong row.
         * 
         * The problem is that - due to much tricksery when rendering the tree -
         * the given coordinates are rather useless. As a consequence, super
         * maps to wrong coordinates. This takes over completely.
         * 
         * PENDING: bidi?
         * 
         * @param event the mouseEvent in treetable coordinates
         * @param row the view row index
         * @param column the view column index
         * @return the tooltip as appropriate for the given row
         */
        private String getToolTipText(MouseEvent event, int row, int column) {
            if (row < 0) return null;
            String toolTip = null;
            TreeCellRenderer renderer = getCellRenderer();
            TreePath     path = getPathForRow(row);
            Object       lastPath = path.getLastPathComponent();
            Component    rComponent = renderer.getTreeCellRendererComponent
                (this, lastPath, isRowSelected(row),
                 isExpanded(row), getModel().isLeaf(lastPath), row,
                 true);

            if(rComponent instanceof JComponent) {
                Rectangle       pathBounds = getPathBounds(path);
                Rectangle cellRect = treeTable.getCellRect(row, column, false);
                // JW: what we are after
                // is the offset into the hierarchical column 
                // then intersect this with the pathbounds   
                Point mousePoint = event.getPoint();
                // translate to coordinates relative to cell
                mousePoint.translate(-cellRect.x, -cellRect.y);
                // translate horizontally to 
                mousePoint.translate(-pathBounds.x, 0);
                // show tooltip only if over renderer?
//                if (mousePoint.x < 0) return null;
//                p.translate(-pathBounds.x, -pathBounds.y);
                MouseEvent newEvent = new MouseEvent(rComponent, event.getID(),
                      event.getWhen(),
                      event.getModifiers(),
                      mousePoint.x, 
                      mousePoint.y,
//                    p.x, p.y, 
                      event.getClickCount(),
                      event.isPopupTrigger());
                
                toolTip = ((JComponent)rComponent).getToolTipText(newEvent);
            }
            if (toolTip != null) {
                return toolTip;
            }
            return getToolTipText();
        }

        /**
         * {@inheritDoc} <p>
         * 
         * Overridden to not automatically de/register itself from/to the ToolTipManager.
         * As rendering component it is not considered to be active in any way, so the
         * manager must not listen. 
         */
        @Override
        public void setToolTipText(String text) {
            putClientProperty(TOOL_TIP_TEXT_KEY, text);
        }

        /**
         * Immutably binds this TreeTableModelAdapter to the specified JXTreeTable.
         * For internal use by JXTreeTable only.
         *
         * @param treeTable the JXTreeTable instance that this renderer is bound to
         */
        public final void bind(JXTreeTable treeTable) {
            // Suppress potentially subversive invocation!
            // Prevent clearing out the deck for possible hijack attempt later!
            if (treeTable == null) {
                throw new IllegalArgumentException("null treeTable");
            }

            if (this.treeTable == null) {
                this.treeTable = treeTable;
                // commented because still has issus
//                bindRollover();
            }
            else {
                throw new IllegalArgumentException("renderer already bound");
            }
        }

        /**
         * Install rollover support.
         * Not used - still has issues.
         * - not bidi-compliant
         * - no coordinate transformation for hierarchical column != 0
         * - method name clash enabled
         * - keyboard triggered click unreliable (triggers the treetable)
         * ...
         */
        @SuppressWarnings("unused")
        private void bindRollover() {
            setRolloverEnabled(treeTable.isRolloverEnabled());
            treeTable.addPropertyChangeListener(getRolloverListener());
        }

        
        /**
         * @return
         */
        private PropertyChangeListener getRolloverListener() {
            if (rolloverListener == null) {
                rolloverListener = createRolloverListener();
            }
            return rolloverListener;
        }

        /**
         * Creates and returns a property change listener for 
         * table's rollover related properties. 
         * 
         * This implementation 
         * - Synchs the tree's rolloverEnabled 
         * - maps rollover cell from the table to the cell 
         *   (still incomplete: first column only)
         * 
         * @return
         */
        protected PropertyChangeListener createRolloverListener() {
            PropertyChangeListener l = new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    if ((treeTable == null) || (treeTable != evt.getSource()))
                        return;
                    if ("rolloverEnabled".equals(evt.getPropertyName())) {
                        setRolloverEnabled(((Boolean) evt.getNewValue()).booleanValue());
                    }
                    if (RolloverProducer.ROLLOVER_KEY.equals(evt.getPropertyName())){
                        rollover(evt);
                    } 
                }

                private void rollover(PropertyChangeEvent evt) {
                    boolean isHierarchical = isHierarchical((Point)evt.getNewValue());
                    putClientProperty(evt.getPropertyName(), isHierarchical ? 
                           new Point((Point) evt.getNewValue()) : null);
                }
                
                private boolean isHierarchical(Point point) {
                    if (point != null) {
                        int column = point.x;
                        if (column >= 0) {
                            return treeTable.isHierarchical(column);
                        }
                    }
                   return false;
                }
                @SuppressWarnings("unused")
                Point rollover = new Point(-1, -1);
            };
            return l;
        }

        /**
         * {@inheritDoc} <p>
         * 
         * Overridden to produce clicked client props only. The
         * rollover are produced by a propertyChangeListener to 
         * the table's corresponding prop.
         * 
         */
        @Override
        protected RolloverProducer createRolloverProducer() {
            return new RolloverProducer() {

                /**
                 * Overridden to do nothing.
                 * 
                 * @param e
                 * @param property
                 */
                @Override
                protected void updateRollover(MouseEvent e, String property, boolean fireAlways) {
                    if (CLICKED_KEY.equals(property)) {
                        super.updateRollover(e, property, fireAlways);
                    }
                }
                @Override
                protected void updateRolloverPoint(JComponent component,
                        Point mousePoint) {
                    JXTree tree = (JXTree) component;
                    int row = tree.getClosestRowForLocation(mousePoint.x, mousePoint.y);
                    Rectangle bounds = tree.getRowBounds(row);
                    if (bounds == null) {
                        row = -1;
                    } else {
                        if ((bounds.y + bounds.height < mousePoint.y) || 
                                bounds.x > mousePoint.x)   {
                               row = -1;
                           }
                    }
                    int col = row < 0 ? -1 : 0;
                    rollover.x = col;
                    rollover.y = row;
                }
                
            };
        }

        
        @Override
        public void scrollRectToVisible(Rectangle aRect) {
            treeTable.scrollRectToVisible(aRect);
        }

        @Override
        protected void setExpandedState(TreePath path, boolean state) {
            treeTable.getTreeTableHacker().completeEditing();
            super.setExpandedState(path, state);
            treeTable.getTreeTableHacker().expansionChanged();
            
        }

        /**
         * updateUI is overridden to set the colors of the Tree's renderer
         * to match that of the table.
         */
        @Override
        public void updateUI() {
            super.updateUI();
            // Make the tree's cell renderer use the table's cell selection
            // colors.
            // TODO JW: need to revisit...
            // a) the "real" of a JXTree is always wrapped into a DelegatingRenderer
            //  consequently the if-block never executes
            // b) even if it does it probably (?) should not 
            // unconditionally overwrite custom selection colors. 
            // Check for UIResources instead. 
            TreeCellRenderer tcr = getCellRenderer();
            if (tcr instanceof DefaultTreeCellRenderer) {
                DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);
                // For 1.1 uncomment this, 1.2 has a bug that will cause an
                // exception to be thrown if the border selection color is null.
                dtcr.setBorderSelectionColor(null);
                dtcr.setTextSelectionColor(
                    UIManager.getColor("Table.selectionForeground"));
                dtcr.setBackgroundSelectionColor(
                    UIManager.getColor("Table.selectionBackground"));
            }
        }

        /**
         * Sets the row height of the tree, and forwards the row height to
         * the table.
         * 
         *
         */
        @Override
        public void setRowHeight(int rowHeight) {
            // JW: can't ... updateUI invoked with rowHeight = 0
            // hmmm... looks fishy ...
//            if (rowHeight <= 0) throw 
//               new IllegalArgumentException("the rendering tree must have a fixed rowHeight > 0");
            super.setRowHeight(rowHeight);
            if (rowHeight > 0) {
                if (treeTable != null) {
                    treeTable.adjustTableRowHeight(rowHeight);
                }
            }
        }


        /**
         * This is overridden to set the height to match that of the JTable.
         */
        @Override
        public void setBounds(int x, int y, int w, int h) {
            if (treeTable != null) {
                y = 0;
                // It is not enough to set the height to treeTable.getHeight()
                h = treeTable.getRowCount() * this.getRowHeight();
//                int hierarchicalC = treeTable.getHierarchicalColumn();
//                if (hierarchicalC >= 0) {
//                    TableColumn column = treeTable.getColumn(hierarchicalC);
//                    w = Math.min(w, column.getWidth());
//                }
            }
            super.setBounds(x, y, w, h);
        }

        /**
         * Sublcassed to translate the graphics such that the last visible row
         * will be drawn at 0,0.
         */
        @Override
        public void paint(Graphics g) {
            Rectangle cellRect = treeTable.getCellRect(visibleRow, 0, false);
            g.translate(0, -cellRect.y);

            hierarchicalColumnWidth = getWidth();
            super.paint(g);

            // Draw the Table border if we have focus.
            if (highlightBorder != null) {
                // #170: border not drawn correctly
                // JW: position the border to be drawn in translated area
                // still not satifying in all cases...
                // RG: Now it satisfies (at least for the row margins)
                // Still need to make similar adjustments for column margins...
                highlightBorder.paintBorder(this, g, 0, cellRect.y,
                        getWidth(), cellRect.height);
            }
        }

        public void doClick() {
            if ((getCellRenderer() instanceof RolloverRenderer)
                    && ((RolloverRenderer) getCellRenderer()).isEnabled()) {
                ((RolloverRenderer) getCellRenderer()).doClick();
            }
            
        }

        public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
            assert table == treeTable;
            // JW: quick fix for the tooltip part of #794-swingx:
            // visual properties must be reset in each cycle.
            // reverted - otherwise tooltip per Highlighter doesn't work
            // 
//            setToolTipText(null);
            
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            }
            else {
                setBackground(table.getBackground());
               setForeground(table.getForeground());
            }

            highlightBorder = null;
            if (treeTable != null) {
                if (treeTable.realEditingRow() == row &&
                    treeTable.getEditingColumn() == column) {
                }
                else if (hasFocus) {
                    highlightBorder = UIManager.getBorder(
                        "Table.focusCellHighlightBorder");
                }
            }
            
            visibleRow = row;

            return this;
        }

        private class ClippedTreeCellRenderer extends DefaultXTreeCellRenderer 
            implements StringValue 
            {
            @SuppressWarnings("unused")
            private boolean inpainting;
            private String shortText;
            @Override
            public void paint(Graphics g) {
                String fullText = super.getText();
        
                 shortText = SwingUtilities.layoutCompoundLabel(
                    this, g.getFontMetrics(), fullText, getIcon(),
                    getVerticalAlignment(), getHorizontalAlignment(),
                    getVerticalTextPosition(), getHorizontalTextPosition(),
                    getItemRect(itemRect), iconRect, textRect,
                    getIconTextGap());

                /** TODO: setText is more heavyweight than we want in this
                 * situation. Make JLabel.text protected instead of private.
         */

                try {
                    inpainting = true;
                    // TODO JW: don't - override getText to return the short version
                    // during painting
                    setText(shortText); // temporarily truncate text
                    super.paint(g);
                } finally {
                    inpainting = false;
                    setText(fullText); // restore full text
                }
            }

            
            private Rectangle getItemRect(Rectangle itemRect) {
                getBounds(itemRect);
//                LOG.info("rect" + itemRect);
                itemRect.width = hierarchicalColumnWidth - itemRect.x;
                return itemRect;
            }

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                return super.getTreeCellRendererComponent(tree, getHierarchicalTableValue(value), sel, expanded, leaf,
                        row, hasFocus);
            }


            /**
             * 
             * @param node the node in the treeModel as passed into the TreeCellRenderer
             * @return the corresponding value of the hierarchical cell in the TreeTableModel
             */
            private Object getHierarchicalTableValue(Object node) {
                Object val = node;
                
                if (treeTable != null) {
                    int treeColumn = treeTable.getTreeTableModel().getHierarchicalColumn();
                    Object o = null; 
                    if (treeColumn >= 0) {
                        // following is unreliable during a paint cycle
                        // somehow interferes with BasicTreeUIs painting cache
//                        o = treeTable.getValueAt(row, treeColumn);
                        // ask the model - that's always okay
                        // might blow if the TreeTableModel is strict in
                        // checking the containment of the value and 
                        // this renderer is called for sizing with a prototype
                        o = treeTable.getTreeTableModel().getValueAt(node, treeColumn);
                    }
                    val = o;
                }
                return val;
            }

            /**
             * {@inheritDoc} <p>
             */
            public String getString(Object node) {
//                int treeColumn = treeTable.getTreeTableModel().getHierarchicalColumn();
//                if (treeColumn >= 0) {
//                    return StringValues.TO_STRING.getString(treeTable.getTreeTableModel().getValueAt(value, treeColumn));
//                }
                return StringValues.TO_STRING.getString(getHierarchicalTableValue(node));
            }

            // Rectangles filled in by SwingUtilities.layoutCompoundLabel();
            private final Rectangle iconRect = new Rectangle();
            private final Rectangle textRect = new Rectangle();
            // Rectangle filled in by this.getItemRect();
            private final Rectangle itemRect = new Rectangle();
        }

        /** Border to draw around the tree, if this is non-null, it will
         * be painted. */
        protected Border highlightBorder = null;
        protected JXTreeTable treeTable = null;
        protected int visibleRow = 0;

        // A JXTreeTable may not have more than one hierarchical column
        private int hierarchicalColumnWidth = 0;

    }

    /**
     * Returns the adapter that knows how to access the component data model.
     * The component data adapter is used by filters, sorters, and highlighters.
     *
     * @return the adapter that knows how to access the component data model
     */
    @Override
    protected ComponentAdapter getComponentAdapter() {
        if (dataAdapter == null) {
            dataAdapter = new TreeTableDataAdapter(this); 
        }
        return dataAdapter;
    }


    protected static class TreeTableDataAdapter extends JXTable.TableAdapter {
        private final JXTreeTable table;

        /**
         * Constructs a <code>TreeTableDataAdapter</code> for the specified
         * target component.
         *
         * @param component the target component
         */
        public TreeTableDataAdapter(JXTreeTable component) {
            super(component);
            table = component;
        }
        
        public JXTreeTable getTreeTable() {
            return table;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isExpanded() {
            return table.isExpanded(row); 
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getDepth() {
            return table.getPathForRow(row).getPathCount() - 1;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isLeaf() {
            // Issue #270-swingx: guard against invisible row
            TreePath path = table.getPathForRow(row);
            if (path != null) {
                return table.getTreeTableModel().isLeaf(path.getLastPathComponent());
            }
            // JW: this is the same as BasicTreeUI.isLeaf. 
            // Shouldn't happen anyway because must be called for visible rows only.
            return true; 
        }
        /**
         *
         * @return true if the cell identified by this adapter displays hierarchical
         *      nodes; false otherwise
         */
        @Override
        public boolean isHierarchical() {
            return table.isHierarchical(column);
        }

        /**
         * {@inheritDoc} <p>
         * 
         * Overridden to fix #821-swingx: string rep of hierarchical column incorrect.
         * In this case we must delegate to the tree directly if hidden, the visible
         * is handled by the TreeTable itself.<p>
         * 
         * PENDING JW: revisit once we switch to really using a table renderer. 
         */
        @Override
        public String getFilteredStringAt(int row, int column) {
            if (table.getTreeTableModel().getHierarchicalColumn() == column) {
                if (modelToView(column) < 0) {
                    // hidden hierarchical column, access directly
                    return table.getHierarchicalStringAt(row);
                }
            }
            return super.getFilteredStringAt(row, column);
        }
        
        /**
         * {@inheritDoc} <p>
         * 
         * Overridden to fix #821-swingx: string rep of hierarchical column incorrect.
         * In this case we must delegate to the tree directly if hidden, the visible
         * is handled by the TreeTable itself.<p>
         * 
         * PENDING JW: revisit once we switch to really using a table renderer. 
         */
        @Override
        public String getStringAt(int row, int column) {
            if (table.getTreeTableModel().getHierarchicalColumn() == column) {
                if (modelToView(column) < 0) {
                    // hidden hierarchical column, access directly
                    return table.getHierarchicalStringAt(row);
                }
            }
            return super.getStringAt(row, column);
        }
        
    }

}
