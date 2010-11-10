/*
 * $Id: SelectionMapper.java,v 1.5 2006/09/21 12:08:18 kleopatra Exp $
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
package org.jdesktop.swingx.decorator;

import javax.swing.ListSelectionModel;

/**
 * Responsible for keeping track of selection in model coordinates.<p>
 * 
 * updates view selection on pipeline change.
 * updates model selection on view selection change.
 * 
 * @author Jeanette Winzenburg
 */
public interface SelectionMapper {

    /**
     * sets the view selection model. Must not be null.
     * 
     * @param viewSelectionMode holding selected indices in view coordinates
     */
    void setViewSelectionModel(ListSelectionModel viewSelectionMode);

    /**
     * @return view selection model
     */
    ListSelectionModel getViewSelectionModel();

    /**
     * Install the new filter pipeline and map the newly filtered data
     * towards the view.
     */
    void setFilters(FilterPipeline pipeline);

    /**
     * Toggle whether selection mapping is active. If mapping is active, user
     * actions to select rows in the view will be recorded in model coordinates.
     * When the model changes due to filtering, the view selection will be
     * updated to maintain the logical selection.
     */
    void setEnabled(boolean enabled);

    /**
     * @return true if selection mapping is active.
     */
    boolean isEnabled();

    void clearModelSelection();

    /**
     * Adjust the model by adding the specified rows.
     */
    void insertIndexInterval(int start, int length, boolean before);

    /**
     * Adjust the model by removing the specified rows.
     */
    void removeIndexInterval(int start, int end);
}
