/*
 * $Id: SortOrder.java,v 1.5 2008/10/14 22:31:38 rah003 Exp $
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
package org.jdesktop.swingx.decorator;

/**
 * Encasulates sort state. 
 * There are several conenience methods to simplify usage of the three possible states 
 *  (unsorted, ascending sorted, descending sorted).
 *  PENDING: incomplete.
 * 
 * 
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public final class SortOrder {
    public static final SortOrder ASCENDING = new SortOrder("ascending");
    public static final SortOrder DESCENDING = new SortOrder("descending");
    public static final SortOrder UNSORTED = new SortOrder("unsorted");

    private final String name;
    private SortOrder(String name) {
        this.name = name;
    }
    
    /**
     * Convenience to check if the order is sorted.
     * @return false if unsorted, true for ascending/descending.
     */
    public boolean isSorted() {
        return this != UNSORTED;
    }
    
    public boolean isSorted(boolean ascending) {
        return isSorted() && (ascending == isAscending());
    }
    
    /**
     * Convenience to check for ascending sort order.
     * PENDING: is this helpful at all?
     * 
     * @return true if ascendingly sorted, false for unsorted/descending.
     */
    public boolean isAscending() {
        return this == ASCENDING;
    }
    
    @Override
    public String toString() {
        return name;
    }

}
