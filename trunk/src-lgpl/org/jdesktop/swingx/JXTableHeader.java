/*
 * $Id: JXTableHeader.java,v 1.34 2009/05/06 10:42:59 kleopatra Exp $
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.io.Serializable;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.event.TableColumnModelExtListener;
import org.jdesktop.swingx.table.ColumnHeaderRenderer;
import org.jdesktop.swingx.table.TableColumnExt;

/**
 * TableHeader with extended functionality if associated Table is of
 * type JXTable.<p>
 * 
 * <h2> Extended user interaction </h2>
 * 
 * <ul>
 * <li> Supports column sorting by mouse clicks into a header cell 
 *  (outside the resize region). The concrete gestures are configurable 
 *  by providing a custom SortGestureRecognizer.  The default recognizer
 *  toggles sort order on mouseClicked. On shift-mouseClicked, it resets any column sorting. 
 * Both are done by invoking the corresponding methods of JXTable, 
 * <code> toggleSortOrder(int) </code> and <code> resetSortOrder() </code>
 * <li> Supports column pack (== auto-resize to exactly fit the contents)
 *  on double-click in resize region.
 *  <li> Supports horizontal auto-scroll if a column is dragged outside visible rectangle. 
 *  This feature is enabled if the autoscrolls property is true. The default is false 
 *  (because of Issue #788-swingx which still isn't fixed for jdk1.6).
 * </ul>
 * 
 * <h2> Extended functionality </h2>
 * 
 * <ul>
 * <li> Installs a default header renderer which is able to show sort icons. 
 *   LAF provided special effects are uneffected.
 * <li> Listens to TableColumn propertyChanges to update itself accordingly.
 * <li> Supports per-column header ToolTips. 
 * <li> Guarantees reasonable minimal height > 0 for header preferred height.
 * </ul>
 * 
 * 
 * @author Jeanette Winzenburg
 * 
 * @see JXTable#toggleSortOrder(int)
 * @see JXTable#resetSortOrder()
 * @see SortGestureRecognizer
 * @see ColumnHeaderRenderer
 */
public class JXTableHeader extends JTableHeader 
    implements TableColumnModelExtListener {

    /**
     * The recognizer used for interpreting mouse events as sorting user gestures.
     */
    private SortGestureRecognizer sortGestureRecognizer;

    /**
     *  Constructs a <code>JTableHeader</code> with a default 
     *  <code>TableColumnModel</code>.
     *
     * @see #createDefaultColumnModel
     */
    public JXTableHeader() {
        super();
    }

    /**
     * Constructs a <code>JTableHeader</code> which is initialized with
     * <code>cm</code> as the column model. If <code>cm</code> is
     * <code>null</code> this method will initialize the table header with a
     * default <code>TableColumnModel</code>.
     * 
     * @param columnModel the column model for the table
     * @see #createDefaultColumnModel
     */
    public JXTableHeader(TableColumnModel columnModel) {
        super(columnModel);
    }


    /**
     * {@inheritDoc} <p>
     * Sets the associated JTable. Enables enhanced header
     * features if table is of type JXTable.<p>
     * 
     * PENDING: who is responsible for synching the columnModel?
     */
    @Override
    public void setTable(JTable table) {
        super.setTable(table);
//        setColumnModel(table.getColumnModel());
        // the additional listening option makes sense only if the table
        // actually is a JXTable
        if (getXTable() != null) {
            installHeaderListener();
        } else {
            uninstallHeaderListener();
        }
    }

    /**
     * Implements TableColumnModelExt to allow internal update after
     * column property changes.<p>
     * 
     * This implementation triggers a resizeAndRepaint on every propertyChange which
     * doesn't already fire a "normal" columnModelEvent.
     * 
     * @param event change notification from a contained TableColumn.
     * @see #isColumnEvent(PropertyChangeEvent)
     * @see TableColumnModelExtListener
     * 
     * 
     */
    public void columnPropertyChange(PropertyChangeEvent event) {
       if (isColumnEvent(event)) return;
       resizeAndRepaint(); 
    }
    
    
    /**
     * Returns a boolean indicating if a property change event received
     * from column changes is expected to be already broadcasted by the
     * core TableColumnModel. <p>
     * 
     * This implementation returns true for notification of width, preferredWidth
     * and visible properties, false otherwise.
     * 
     * @param event the PropertyChangeEvent received as TableColumnModelExtListener.
     * @return a boolean to decide whether the same event triggers a
     *   base columnModelEvent.
     */
    protected boolean isColumnEvent(PropertyChangeEvent event) {
        return "width".equals(event.getPropertyName()) || 
            "preferredWidth".equals(event.getPropertyName())
            || "visible".equals(event.getPropertyName());
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to respect the column tooltip, if available. 
     * 
     * @return the column tooltip of the column at the mouse position 
     *   if not null or super if not available.
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        String columnToolTipText = getColumnToolTipText(event);
        return columnToolTipText != null ? columnToolTipText : super.getToolTipText(event);
    }

    /**
     * Returns the column tooltip of the column at the position
     * of the MouseEvent, if a tooltip is available.
     * 
     * @param event the mouseEvent representing the mouse location.
     * @return the column tooltip of the column below the mouse location,
     *   or null if not available.
     */
    protected String getColumnToolTipText(MouseEvent event) {
        if (getXTable() == null) return null;
        int column = columnAtPoint(event.getPoint());
        if (column < 0) return null;
        TableColumnExt columnExt = getXTable().getColumnExt(column);
        return columnExt != null ? columnExt.getToolTipText() : null;
    }
    
    /**
     * Returns the associated table if it is of type JXTable, or null if not.
     * 
     * @return the associated table if of type JXTable or null if not.
     */
    public JXTable getXTable() {
        if (!(getTable() instanceof JXTable))
            return null;
        return (JXTable) getTable();
    }

    /**
     * Returns the TableCellRenderer to use for the column with the given index. This
     * implementation returns the column's header renderer if available or this header's
     * default renderer if not.
     * 
     * @param columnIndex the index in view coordinates of the column
     * @return the renderer to use for the column, guaranteed to be not null.
     */
    public TableCellRenderer getCellRenderer(int columnIndex) {
        TableCellRenderer renderer = getColumnModel().getColumn(columnIndex).getHeaderRenderer();
        return renderer != null ? renderer : getDefaultRenderer();
    }
    
    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to adjust for a reasonable minimum height. Done to fix Issue 334-swingx,
     * which actually is a core issue misbehaving in returning a zero height
     * if the first column has no text. 
     * 
     * @see #getPreferredSize(Dimension)
     * @see #getMinimumHeight(int).
     * 
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        pref = getPreferredSize(pref);
        pref.height = getMinimumHeight(pref.height);
        return pref;
    }
    
    /**
     * Returns a preferred size which is adjusted to the maximum of all
     * header renderers' height requirement.
     * 
     * @param pref an initial preferred size
     * @return the initial preferred size with its height property adjusted 
     *      to the maximum of all renderers preferred height requirement. 
     *  
     *  @see #getPreferredSize()
     *  @see #getMinimumHeight(int)
     */
    protected Dimension getPreferredSize(Dimension pref) {
        int height = pref.height;
        for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
            TableCellRenderer renderer = getCellRenderer(i);
            Component comp = renderer.getTableCellRendererComponent(table, 
                    getColumnModel().getColumn(i).getHeaderValue(), false, false, -1, i);
            height = Math.max(height, comp.getPreferredSize().height);
        }
        pref.height = height;
        return pref;
        
    }

    /**
     * Returns a reasonable minimal preferred height for the header. This is
     * meant as a last straw if all header values are null, renderers report 0 as
     * their preferred height.<p>
     * 
     * This implementation returns the default header renderer's preferred height as measured
     * with a dummy value if the input height is 0, otherwise returns the height
     * unchanged.
     * 
     * @param height the initial height.
     * @return a reasonable minimal preferred height.
     * 
     * @see #getPreferredSize()
     * @see #getPreferredSize(Dimension)
     */
    protected int getMinimumHeight(int height) {
        if ((height == 0)) {
//                && (getXTable() != null) 
//                && getXTable().isColumnControlVisible()){
            TableCellRenderer renderer = getDefaultRenderer();
            Component comp = renderer.getTableCellRendererComponent(getTable(), 
                        "dummy", false, false, -1, -1);
            height = comp.getPreferredSize().height;
        }
        return height;
    }
    
    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to update the default renderer. 
     * 
     * @see #preUpdateRendererUI()
     * @see #postUpdateRendererUI(TableCellRenderer)
     * @see ColumnHeaderRenderer
     */
    @Override
    public void updateUI() {
        TableCellRenderer oldRenderer = preUpdateRendererUI();
        super.updateUI();
        postUpdateRendererUI(oldRenderer);
    }

    /**
     * Prepares the default renderer and internal state for updateUI. 
     * Returns the default renderer set when entering this method.
     * Called from updateUI before calling super.updateUI to 
     * allow UIDelegate to cleanup, if necessary. This implementation
     * does so by restoring the header's default renderer to the 
     * <code>ColumnHeaderRenderer</code>'s delegate.
     *  
     * @return the current default renderer 
     * @see #updateUI()
     */
    protected TableCellRenderer preUpdateRendererUI() {
        TableCellRenderer oldRenderer = getDefaultRenderer();
        // reset the default to the original to give S
        if (oldRenderer instanceof ColumnHeaderRenderer) {
            setDefaultRenderer(((ColumnHeaderRenderer)oldRenderer).getDelegateRenderer());
        }
        return oldRenderer;
    }

    /**
     * Cleans up after the UIDelegate has updated the default renderer.
     * Called from <code>updateUI</code> after calling <code>super.updateUI</code>.
     * This implementation wraps a <code>UIResource</code> default renderer into a 
     * <code>ColumnHeaderRenderer</code>.
     * 
     * @param oldRenderer the default renderer before updateUI
     * 
     * @see #updateUI()
     * 
     * 
     */
    protected void postUpdateRendererUI(TableCellRenderer oldRenderer) {
        TableCellRenderer current = getDefaultRenderer();
        if (!(current instanceof ColumnHeaderRenderer) && (current instanceof UIResource)) {
            ColumnHeaderRenderer renderer;
            if (oldRenderer instanceof ColumnHeaderRenderer) {
                renderer = (ColumnHeaderRenderer) oldRenderer;
                renderer.updateUI(this);
            } else {
                renderer = new ColumnHeaderRenderer(this);
            }
            setDefaultRenderer(renderer);
        }
    }
    
    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to scroll the table to keep the dragged column visible.
     * This side-effect is enabled only if the header's autoscroll property is
     * <code>true</code> and the associated table is of type JXTable.<p> 
     * 
     * The autoscrolls is disabled by default. With or without - core 
     * issue #6503981 has weird effects (for jdk 1.6 - 1.6u3) on a plain 
     * JTable as well as a JXTable, fixed in 1.6u4.
     * 
     */
    @Override
    public void setDraggedDistance(int distance) {
        int old = getDraggedDistance();
        super.setDraggedDistance(distance);
        // fire because super doesn't
        firePropertyChange("draggedDistance", old, getDraggedDistance());
        if (!getAutoscrolls() || (getXTable() == null)) return;
        TableColumn column = getDraggedColumn();
        // fix for #788-swingx: don't try to scroll if we have no dragged column
        // as doing will confuse the horizontalScrollEnabled on the JXTable.
        if (column != null) {
            getXTable().scrollColumnToVisible(getViewIndexForColumn(column));
        }
    }
    
    /**
     * Returns the the dragged column if and only if, a drag is in process and
     * the column is visible, otherwise returns <code>null</code>.
     * 
     * @return the dragged column, if a drag is in process and the column is
     *         visible, otherwise returns <code>null</code>
     * @see #getDraggedDistance
     */
    @Override
    public TableColumn getDraggedColumn() {
        return isVisible(draggedColumn) ? draggedColumn : null; 
    }

    /**
     * Checks and returns the column's visibility. 
     * 
     * @param column the <code>TableColumn</code> to check
     * @return a boolean indicating if the column is visible
     */
    private boolean isVisible(TableColumn column) {
        return getViewIndexForColumn(column) >= 0;
    }

    /**
     * Returns the (visible) view index for the table column
     * or -1 if not visible or not contained in this header's
     * columnModel.
     * 
     * 
     * @param aColumn the TableColumn to find the view index for
     * @return the view index of the given table column or -1 if not visible
     * or not contained in the column model.
     */
    private int getViewIndexForColumn(TableColumn aColumn) {
        if (aColumn == null)
            return -1;
        TableColumnModel cm = getColumnModel();
        for (int column = 0; column < cm.getColumnCount(); column++) {
            if (cm.getColumn(column) == aColumn) {
                return column;
            }
        }
        return -1;
    }


    /**
     * Returns the SortGestureRecognizer to use. If none available, lazily 
     * creates a default.
     * 
     * @return the SortGestureRecognizer to use for interpreting mouse events
     *    as sort gestures.
     *    
     * @see #setSortGestureRecognizer(SortGestureRecognizer)
     * @see #createSortGestureRecognizer()   
     */
    public SortGestureRecognizer getSortGestureRecognizer() {
        if (sortGestureRecognizer == null) {
            sortGestureRecognizer = createSortGestureRecognizer();
        }
        return sortGestureRecognizer;
        
    }
    
    /**
     * Sets the SortGestureRecognizer to use for interpreting mouse events
     *    as sort gestures. If null, a default as returned by createSortGestureRecognizer
     *    is used.<p>
     *    
     * This is a bound property.   
     * 
     * @param recognizer the SortGestureRecognizer to use for interpreting mouse events
     *    as sort gestures
     *    
     * @see #getSortGestureRecognizer()
     * @see #createSortGestureRecognizer()    
     */
    public void setSortGestureRecognizer(SortGestureRecognizer recognizer) {
        SortGestureRecognizer old = getSortGestureRecognizer();
        this.sortGestureRecognizer = recognizer;
        firePropertyChange("sortGestureRecognizer", old, getSortGestureRecognizer());
    }
    
    /**
     * Creates and returns the default SortGestureRecognizer.
     * @return the default SortGestureRecognizer to use for interpreting mouse events
     *    as sort gestures.
     * 
     * @see #getSortGestureRecognizer()
     * @see #setSortGestureRecognizer(SortGestureRecognizer)
     */
    protected SortGestureRecognizer createSortGestureRecognizer() {
        return new SortGestureRecognizer();
    }

    /**
     * Controller for mapping left mouse clicks to sort/-unsort gestures for use
     * in interested mouse listeners. This base class interprets a single click
     * for toggling sort order, and a single SHIFT-left click for unsort.
     * <p>
     * 
     * A custom implementation which doesn't allow unsort.
     * 
     * <pre>
     * &lt;code&gt;
     * public class CustomRecognizer extends SortGestureRecognizer {
     *        // Disable reset gesture.
     *          &#064;Override 
     *           public boolean isResetSortOrderGesture(MouseEvent e) { 
     *                  return false; 
     *          }
     * }
     * tableHeader.setSortGestureRecognizer(new CustomRecognizer());
     * &lt;/code&gt;
     * </pre>
     * 
     * <b>Note</b>: Unsort as of SwingX means to reset the sort of all columns.
     * Which currently doesn't make a difference because it supports single
     * column sorts only. Might become significant after switching to JDK 1.6
     * which supports multiple column sorting (if we can keep up the pluggable
     * control).
     * 
     * 
     */
    public static class SortGestureRecognizer {

        /**
         * Returns a boolean indicating whether the mouse event should be interpreted
         * as an unsort trigger or not.
         * @param e a mouseEvent representing a left mouse click.
         * @return true if the mouse click should be used as a unsort gesture
         */
        public boolean isResetSortOrderGesture(MouseEvent e) {
            return isSortOrderGesture(e) && isResetModifier(e);
        }

        /**
         * Returns a boolean indicating whether the mouse event should be interpreted
         * as a toggle sort trigger or not.
         * @param e a mouseEvent representing a left mouse click.
         * @return true if the mouse click should be used as a toggle sort gesture
         */
        public boolean isToggleSortOrderGesture(MouseEvent e) {
            return isSortOrderGesture(e) && !isResetModifier(e);
        }
        
        /**
         * Returns a boolean indicating whether the mouse event should be interpreted
         * as any type of sort change trigger.
         * @param e a mouseEvent representing a left mouse click.
         * @return true if the mouse click should be used as a sort/unsort gesture
         */
        public boolean isSortOrderGesture(MouseEvent e) {
            return e.getClickCount() == 1;
        }
        
        /**
         * Returns a boolean indicating whether the mouse event's modifier should be interpreted
         * as a unsort or not.
         * 
         * @param e a mouseEvent representing a left mouse click.
         * @return true if the mouse click's modifier should be interpreted as a reset.
         * 
         */
        protected boolean isResetModifier(MouseEvent e) {
            return ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK);
        }

    }

    /**
     * Creates and installs header listeners to service the extended functionality.
     * This implementation creates and installs a custom mouse input listener.
     */
    protected void installHeaderListener() {
        if (headerListener == null) {
            headerListener = new HeaderListener();
            addMouseListener(headerListener);
            addMouseMotionListener(headerListener);

        }
    }

    /**
     * Uninstalls header listeners to service the extended functionality.
     * This implementation uninstalls a custom mouse input listener.
     */
    protected void uninstallHeaderListener() {
        if (headerListener != null) {
            removeMouseListener(headerListener);
            removeMouseMotionListener(headerListener);
            headerListener = null;
        }
    }

    private MouseInputListener headerListener;

    private class HeaderListener implements MouseInputListener, Serializable {
        private TableColumn cachedResizingColumn;

        public void mouseClicked(MouseEvent e) {
            if (shouldIgnore(e)) {
                return;
            }
            if (isInResizeRegion(e)) {
                doResize(e);
            }
        }

        private boolean shouldIgnore(MouseEvent e) {
            return !SwingUtilities.isLeftMouseButton(e)
              || !table.isEnabled();
        }

        private void doResize(MouseEvent e) {
            if (e.getClickCount() != 2)
                return;
            int column = getViewIndexForColumn(cachedResizingColumn);
            if (column >= 0) {
                (getXTable()).packColumn(column, 5);
            }
            uncacheResizingColumn();

        }


        public void mouseReleased(MouseEvent e) {
            cacheResizingColumn(e);
        }

        public void mousePressed(MouseEvent e) {
            cacheResizingColumn(e);
        }

        private void cacheResizingColumn(MouseEvent e) {
            if (!getSortGestureRecognizer().isSortOrderGesture(e))
                return;
            TableColumn column = getResizingColumn();
            if (column != null) {
                cachedResizingColumn = column;
            }
        }

        private void uncacheResizingColumn() {
            cachedResizingColumn = null;
        }

        private boolean isInResizeRegion(MouseEvent e) {
            return cachedResizingColumn != null; // inResize;
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
            uncacheResizingColumn();
        }

        public void mouseDragged(MouseEvent e) {
            uncacheResizingColumn();
        }

        public void mouseMoved(MouseEvent e) {
        }
    }



}
