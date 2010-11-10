/*
 * $Id: DefaultTableColumnModelExt.java,v 1.29 2009/01/02 13:28:03 rah003 Exp $
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

package org.jdesktop.swingx.table;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.event.TableColumnModelExtListener;


/**
 * A default implementation of <code>TableColumnModelExt</code>.
 * <p>
 * 
 * TODO: explain sub-optimal notification on showing/hiding columns.
 * (hot fixed issues #156, #157. To really do it
 * need enhanced TableColumnModelEvent and -Listeners that are
 * aware of the event.)
 * 
 *  
 * @author Richard Bair
 * @author Jeanette Winzenburg
 */
public class DefaultTableColumnModelExt extends DefaultTableColumnModel 
    implements TableColumnModelExt {
    /** flag to distinguish a shown/hidden column from really added/removed
     *  columns during notification. This is brittle! 
     */ 
    private static final String IGNORE_EVENT = "TableColumnModelExt.ignoreEvent";
    /**
     * contains a list of all columns, in the order in which were
     * added to the model.
     */
    private List<TableColumn> initialColumns = new ArrayList<TableColumn>();
    
    /**
     * contains a list of all column, in the order they would appear if
     * all were visible.
     */
    private List<TableColumn> currentColumns = new ArrayList<TableColumn>();

    /**
     * Listener attached to TableColumnExt instances to listen for changes
     * to their visibility status, and to hide/show the column as oppropriate
     */
    private VisibilityListener visibilityListener = new VisibilityListener();
    
    /** 
     * Creates a an empty DefaultTableColumnModelExt. 
     */
    public DefaultTableColumnModelExt() {
        super();
    }

//----------------------- implement TableColumnModelExt
    
    /**
     * {@inheritDoc}
     */
    public List<TableColumn> getColumns(boolean includeHidden) {
        if (includeHidden) {
            return new ArrayList<TableColumn>(initialColumns);
        } 
        return Collections.list(getColumns());
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount(boolean includeHidden) {
        if (includeHidden) {
            return initialColumns.size();
        }
        return getColumnCount();
    }
    
    /**
     * {@inheritDoc}
     */
    public TableColumnExt getColumnExt(Object identifier) {
        for (Iterator<TableColumn> iter = initialColumns.iterator(); iter.hasNext();) {
            TableColumn column = iter.next();
            if ((column instanceof TableColumnExt) && (identifier.equals(column.getIdentifier()))) {
                return (TableColumnExt) column;
            }
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public TableColumnExt getColumnExt(int columnIndex) {
        TableColumn column = getColumn(columnIndex);
        if (column instanceof TableColumnExt) {
            return (TableColumnExt) column;
        }
        return null;
    }
    
    /**
     * hot fix for #157: listeners that are aware of
     * the possible existence of invisible columns
     * should check if the received columnRemoved originated
     * from moving a column from visible to invisible.
     * 
     * @param oldIndex the fromIndex of the columnEvent
     * @return true if the column was moved to invisible
     */
    public boolean isRemovedToInvisibleEvent(int oldIndex) {
        if (oldIndex >= currentColumns.size()) return false;
        if (!(currentColumns.get(oldIndex) instanceof TableColumnExt)) return false;
        return Boolean.TRUE.equals(((TableColumnExt) currentColumns.get(oldIndex)).getClientProperty(IGNORE_EVENT));
    }

    /**
     * hot fix for #157: listeners that are aware of
     * the possible existence of invisible columns
     * should check if the received columnAdded originated
     * from moving a column from invisible to visible.
     * 
     * @param newIndex the toIndex of the columnEvent
     * @return true if the column was moved to visible
     */
    public boolean isAddedFromInvisibleEvent(int newIndex) {
        if (!(getColumn(newIndex) instanceof TableColumnExt)) return false;
        return Boolean.TRUE.equals(((TableColumnExt) getColumn(newIndex)).getClientProperty(IGNORE_EVENT));
    }

//------------------------ TableColumnModel
    
    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to update internals related to column visibility.
     */
    @Override
    public void removeColumn(TableColumn column) {
        //remove the visibility listener if appropriate
        if (column instanceof TableColumnExt) {
            ((TableColumnExt)column).removePropertyChangeListener(visibilityListener);
        }
        currentColumns.remove(column);
        initialColumns.remove(column);
        //let the superclass handle notification etc
        super.removeColumn(column);
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to update internals related to column visibility.
     */
    @Override
    public void addColumn(TableColumn aColumn) {
        // hacking to guarantee correct events
        // two step: add as visible, setVisible
        boolean oldVisible = true;
        //add the visibility listener if appropriate
        if (aColumn instanceof TableColumnExt) {
            TableColumnExt xColumn = (TableColumnExt) aColumn;
            oldVisible = xColumn.isVisible();
            xColumn.setVisible(true);
            xColumn.addPropertyChangeListener(visibilityListener);
        }
        // append the column to the end of both initial- and currentColumns. 
        currentColumns.add(aColumn);
        initialColumns.add(aColumn);
        // let super handle the event notification, super.book-keeping
        super.addColumn(aColumn);
        if (aColumn instanceof TableColumnExt) {
            // reset original visibility
            ((TableColumnExt) aColumn).setVisible(oldVisible);
        }
        
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to update internals related to column visibility.
     */
    @Override
    public void moveColumn(int columnIndex, int newIndex) {
        if (columnIndex != newIndex) {
            updateCurrentColumns(columnIndex, newIndex);
        }
        super.moveColumn(columnIndex, newIndex);
    }

    /**
     * Adjusts the current column sequence when a visible column is moved.
     *  
     * @param oldIndex the old visible position.
     * @param newIndex the new visible position.
     */
    private void updateCurrentColumns(int oldIndex, int newIndex) {
        TableColumn movedColumn = tableColumns.elementAt(oldIndex);
        int oldPosition = currentColumns.indexOf(movedColumn);
        TableColumn targetColumn = tableColumns.elementAt(newIndex);
        int newPosition = currentColumns.indexOf(targetColumn);
        currentColumns.remove(oldPosition);
        currentColumns.add(newPosition, movedColumn);
        
    }

    /**
     * Update internal state after the visibility of the column
     * was changed to invisible. The given column is assumed to
     * be contained in this model.
     * 
     * @param col the column which was hidden.
     */    
    protected void moveToInvisible(TableColumnExt col) {
        col.putClientProperty(IGNORE_EVENT, Boolean.TRUE);
        super.removeColumn(col);
        col.putClientProperty(IGNORE_EVENT, null);
    }


    /**
     * Update internal state after the visibility of the column
     * was changed to visible. The given column is assumed to
     * be contained in this model.
     *  
     * @param col the column which was made visible.
     */    
    protected void moveToVisible(TableColumnExt col) {
        col.putClientProperty(IGNORE_EVENT, Boolean.TRUE);
        // two step process: first add at end of columns
        // then move to "best" position relative to where it
        // was before hiding.
        super.addColumn(col);
        // this is analogous to the proposed fix in #253-swingx
        // but uses the currentColumns as reference.
        Integer addIndex = currentColumns.indexOf(col);
        for (int i = 0; i < (getColumnCount() - 1); i++) {
            TableColumn tableCol = getColumn(i);
            int actualPosition = currentColumns.indexOf(tableCol);
            if (actualPosition > addIndex) {
                super.moveColumn(getColumnCount() - 1, i);
                break;
            }
        }

        col.putClientProperty(IGNORE_EVENT, null);
    }


    /**
     * TODO JW: move into propertyChanged! No need for a dedicated listener.
     * Changed evaluation JW: may still be required as super removes itself as
     * propertyChangeListener if column is hidden 
     */
    private class VisibilityListener implements PropertyChangeListener, Serializable {        
        public void propertyChange(PropertyChangeEvent evt) {
            if ("visible".equals(evt.getPropertyName())) {
                TableColumnExt columnExt = (TableColumnExt)evt.getSource();

                if (columnExt.isVisible()) {
                    moveToVisible(columnExt);
                    fireColumnPropertyChange(evt);
                } else  {
                    moveToInvisible(columnExt);
                }
            }  else if (!((TableColumnExt) evt.getSource()).isVisible()) {
                fireColumnPropertyChange(evt);
            }
        }
    }
 
    // enhanced listener notification
    
    
    /**
     * Exposed for testing only - don't use! Will be removed again!
     * @return super's listener list
     */
    protected EventListenerList getEventListenerList() {
        return listenerList;
    }

    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        fireColumnPropertyChange(evt);
    }

    /**
     * Notifies <code>TableColumnModelExtListener</code>s about property
     * changes of contained columns. The event instance
     * is the original as fired by the <code>TableColumn</code>.
     * @param  evt the event received
     * @see EventListenerList
     */
    protected void fireColumnPropertyChange(PropertyChangeEvent evt) {
        if (IGNORE_EVENT.equals(evt.getPropertyName())) return;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TableColumnModelExtListener.class) {
                ((TableColumnModelExtListener)listeners[i+1]).
                    columnPropertyChange(evt);
            }
        }
    }

    
    /**
     * {@inheritDoc} <p>
     * 
     * 
     * Overridden to install enhanced notification of listeners of type.
     * TableColumnModelListenerExt about property changes of contained columns.
     *  
     */
    @Override
    public void addColumnModelListener(TableColumnModelListener x) {
        super.addColumnModelListener(x);
        if (x instanceof TableColumnModelExtListener) {
            listenerList.add(TableColumnModelExtListener.class, (TableColumnModelExtListener) x);
        }
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to uninstall enhanced notification of listeners of type.
     * TableColumnModelListenerExt about property changes of contained columns.
     */
    @Override
    public void removeColumnModelListener(TableColumnModelListener x) {
        super.removeColumnModelListener(x);
        if (x instanceof TableColumnModelExtListener) {
            listenerList.remove(TableColumnModelExtListener.class, (TableColumnModelExtListener) x);
        }
    }

    /**
     * @return array of all registered listeners 
     */
    public TableColumnModelExtListener[] getTableColumnModelExtListeners() {
        return listenerList.getListeners(TableColumnModelExtListener.class);
    }
}
