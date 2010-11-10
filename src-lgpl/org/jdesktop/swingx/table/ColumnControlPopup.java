/*
 * $Id: ColumnControlPopup.java,v 1.4 2008/10/14 22:31:40 rah003 Exp $
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
package org.jdesktop.swingx.table;

import java.awt.ComponentOrientation;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;

import org.jdesktop.swingx.action.AbstractActionExt;

/**
 * Encapsulates the popup component which is the delegate for
 * all popup visuals, used by a ColumnControlButton.
 * <p>
 * For now, this class a simple extraction of what a ColumnControl needs. 
 * Usage will drive further evolution.
 * 
 */
public interface ColumnControlPopup {

    /**
     * Updates all internal visuals after changing a UI-delegate. <p>
     * 
     * The method called by ColumnControlButton in it's updateUI.
     * As there is a good probability that at the time of a
     * ColumnControlButton is updated after a ui-delegate change the
     * popup is not visible/part of the container hierarchy, this
     * method must be messaged manually. 
     * 
     * @see javax.swing.JComponent#updateUI()
     *
     */
    void updateUI();

    /**
     * Toggles the popup's visibility. This method is responsible for
     * placing itself relative to the given owner if toggled to visible.
     * 
     * @param owner the JComponent which triggered the visibility change, typically
     *   a ColumnControlButton.
     */
    void toggleVisibility(JComponent owner);

    /**
     * Applies the specified component orientation to all internal widgets.
     * This method must be called by the owner if its component orientation 
     * changes. 
     * 
     * @param o the componentOrientation to apply to all internal widgets.
     * @see javax.swing.JComponent#applyComponentOrientation(ComponentOrientation).
     */
    void applyComponentOrientation(ComponentOrientation o);

    /**
     * Removes all items from the popup. 
     */
    void removeAll();

    /**
     * Adds items corresponding to the column's visibility actions.
     * <p>
     * Each <code>Action</code> in the list is a <code>stateAction</code>,
     * its <code>selected</code> property bound to a column's
     * <code>visible</code> property, that is toggling the selected will
     * toggle the column's visibility (if the action is enabled).
     * 
     * The  <code>Action</code>s <code>name</code> property is bound to 
     * the column's <code>title</code>.
     * 
     * @param actions List of AbstractActionExt to add.
     */
    void addVisibilityActionItems(List<? extends AbstractActionExt> actions);
    // JW: dooohhh ... what a winding description ...
    // sure need to have a better abstraction! 
    // 
    
    /**
     * Adds additional actions to the popup. 
     * 
     * @param actions List of <code>Action</code>s to add to the popup.
     */
    void addAdditionalActionItems(List<? extends Action> actions);

}