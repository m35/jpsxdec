/*
 * $Id: TableCellContext.java 3783 2010-09-15 13:13:23Z kleopatra $
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

import javax.swing.JTable;
import javax.swing.UIManager;

import org.jdesktop.swingx.plaf.UIManagerExt;

/**
 * Table specific <code>CellContext</code>.
 * 
 * This implementation optionally can handle LAF provide alternateRowColor. The default 
 * is not doing it. To enable, client code must set a UI-Property with key 
 * HANDLE_ALTERNATE_ROW_BACKGROUND to Boolean.TRUE.
 */
public class TableCellContext extends CellContext {

    public static final String HANDLE_ALTERNATE_ROW_BACKGROUND = "TableCellContext.handleAlternateRowBackground";
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
    public void installContext(JTable component, Object value, int row, int column,
            boolean selected, boolean focused, boolean expanded, boolean leaf) {
        this.component = component;
        installState(value, row, column, selected, focused, expanded, leaf);
    }
    
    
    @Override
    public JTable getComponent() {
        return (JTable) super.getComponent();
    }

    /**
     * Returns the cell's editable property as returned by table.isCellEditable
     * or false if the table is null.
     * 
     * @return the cell's editable property. 
     */
    @Override
    public boolean isEditable() {
        if ((getComponent() == null) || !isValidRow() || !isValidColumn()) {
            return false;
        }
        return getComponent().isCellEditable(getRow(), getColumn());
    }

    /** 
     * @inherited <p>
     * Overridden to respect UI alternating row colors.
     * 
     */
    @Override
    protected Color getBackground() {
        if (isDropOn()) {
            return getSelectionBackground();
        }
        if (getComponent() == null) return null;
        Color color = getAlternateRowColor();
        // JW: this is fixing a core bug - alternate color (aka: different 
        // from default table background) should be the odd row
        if ((color != null) && getRow() >= 0 && getRow() % 2 == 1) {
            return color;
        }
        return getComponent().getBackground();
    }
    
    /**
     * Returns a Color to for odd row background if this context should handle the
     * alternating row color AND the UIManager has the alternateRowColor property set.
     * Returns null otherwise.
     * 
     * @return the color to use for odd row background, or null if either this context
     *    does not handle or no alternate row color is set.
     */
    protected Color getAlternateRowColor() {
        if (!Boolean.TRUE.equals(UIManager.get(HANDLE_ALTERNATE_ROW_BACKGROUND))) return null;
        return UIManagerExt.getColor(getUIPrefix() + "alternateRowColor");
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Color getSelectionBackground() {
        Color selection = null;
        if (isDropOn()) {
            selection = getDropCellBackground();
            if (selection != null) return selection;
        }
        return getComponent() != null ? getComponent()
                .getSelectionBackground() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Color getSelectionForeground() {
        Color selection = null;
        if (isDropOn()) {
            selection = getDropCellForeground();
            if (selection != null) return selection;
        }
        return getComponent() != null ? getComponent()
                .getSelectionForeground() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUIPrefix() {
        return "Table.";
    }

    /**
     * PRE getComponent != null
     * 
     * @return whether the column coordinate is valid in this context
     */
    protected boolean isValidColumn() {
        return getColumn() >= 0 && getColumn() < getComponent().getColumnCount() ;
    }

    /**
     * PRE getComponent != null
     * 
     * @return whether the row coordinate is valid in this context
     */
    protected boolean isValidRow() {
        return getRow() >= 0 && getRow() < getComponent().getRowCount() ;
    }


}