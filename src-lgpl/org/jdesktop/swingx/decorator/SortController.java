/*
 * $Id: SortController.java,v 1.5 2006/06/08 13:00:43 kleopatra Exp $
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

import java.util.Comparator;
import java.util.List;

import org.jdesktop.swingx.JXTable;

/**
 * Defines the interactive sort control for a table. All sort gesture requests
 * from a {@link JXTable} will be routed through the SortController returned
 * from {@link FilterPipeline}.
 * <p>
 * 
 * 
 * To customize sorting behaviour, f.i. to define a toggle sort sequence
 * different from the default, subclass {@link FilterPipeline} and implement a
 * custom SortController.
 * 
 * <pre>
 * <code>
 * public class CustomToggleSortOrderFP extends FilterPipeline {
 * 
 *     public CustomToggleSortOrderFP() {
 *         super();
 *     }
 * 
 *     public CustomToggleSortOrderFP(Filter[] inList) {
 *         super(inList);
 *     }
 * 
 *     //@Override
 *     protected SortController createDefaultSortController() {
 *         return new CustomSortController();
 *     }
 * 
 *     protected class CustomSortController extends SorterBasedSortController {
 * 
 *         //@Override
 *         public void toggleSortOrder(int column, Comparator comparator) {
 *             Sorter currentSorter = getSorter();
 *             if ((currentSorter != null)
 *                     && (currentSorter.getColumnIndex() == column)
 *                     && !currentSorter.isAscending()) {
 *                 setSorter(null);
 *             } else {
 *                 super.toggleSortOrder(column, comparator);
 *             }
 *         }
 * 
 *     }
 * }
 * 
 * // use it
 * xTable.setFilters(new CustomToggleSortOrderFP());
 * 
 * </code>
 * </pre> 
 *  
 * <p>
 * The GlazedLists project (http://publicobject.com/glazedlists/) 
 * has an example about how to replace the SwingX'
 * internal (view based) by an external 
 * (model-decoration based) sort/filter mechanism. 
 *  
 * <p>
 * This interface is inspired by a Java 1.6 class RowSorter, extracting
 *  the sort control part - change notification and index mapping is left to the 
 *  enclosing FilterPipeline. 
 * 
 *  @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 * 
 */
public interface SortController {

    /**
     * Reverses the sort order of the specified column. 
     * It is up to implementating classes to provide the exact behavior when invoked.
     *
     * @param column the model index of the column to toggle
     */
    void toggleSortOrder(int column);

    /**
     * Reverses the sort order of the specified column. 
     * It is up to implementating classes to provide the exact behavior when invoked.
     *
     * @param column the model index of the column to toggle
     * @param comparator the comparator to use
     */
    void toggleSortOrder(int column, Comparator comparator);
    
    /**
     * Set the sort order by column.
     */
    void setSortKeys(List<? extends SortKey> keys);

    /**
     * List the sort order by column.
     */
    List<? extends SortKey> getSortKeys();

    /**
     * Get the sort order of the specified column.
     * 
     * PENDING (JW) - remove? Looks like an "intermediate" convenience method.
     * Not used in SwingX, only in test methods.
     * 
     * @return one of {@link SortOrder#ASCENDING},
     *     {@link SortOrder#DESCENDING} or {@link SortOrder#UNSORTED}.
     */
    SortOrder getSortOrder(int column);


}
