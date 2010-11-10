/*
 * $Id: Sorter.java,v 1.20 2006/07/05 05:07:09 dmouse Exp $
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

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Pluggable sorting filter.
 *
 * @author Ramesh Gupta
 */
public abstract class Sorter extends Filter {
    private boolean ascending = true;
    // JW: need to be updated if default locale changed
    private Collator  collator;   // RG: compute this once
    private Locale currentLocale;
    private Comparator comparator;

    public Sorter() {
        this(0, true);
    }

    public Sorter(int col, boolean ascending) {
        this(col, ascending, null);
    }

    public Sorter(int col, boolean ascending, Comparator comparator) {
        super(col);
        this.comparator = comparator;
        setAscending(ascending);
    }

    
    @Override
    protected void refresh(boolean reset) {
        refreshCollator();
        super.refresh(reset);
    }
    /** 
     * Subclasses must call this before filtering to guarantee the
     * correct collator!
     */
    protected void refreshCollator() {
        if (!Locale.getDefault().equals(currentLocale)) {
            currentLocale = Locale.getDefault();
            collator = Collator.getInstance();
        }
    }

    /**
     * exposed for testing only!
     * @return <code>Collator</code>
     */
    protected Collator getCollator() {
        return collator;
    }
    /**
     * set the Comparator to use when comparing values.
     * If not null every compare will be delegated to it.
     * If null the compare will follow the internal compare
     * (no contract, but implemented here as:
     * first check if the values are Comparable, if so
     * delegate, then compare the String representation)
     *
     * @param comparator
     */
    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
        refresh();
    }

    public Comparator getComparator() {
        return this.comparator;
    }

    /**
     * Compares and returns the entries in row1 vs row2 and 
     * returns -1, 0, -1 depending on their being &lt;, ==, > in the
     * current sort direction.
     * 
     * PRE: getColumnIndex() valid.
     * 
     *  NOTE: this formerly was public ... and without precondition.
     *  
     *  
     * @param row1 
     * @param row2
     * @return returns -1, 0, -1 depending on row1/row2 being &lt;, ==, > in the current sort direction
     */
    protected int compare(int row1, int row2) {
        int result = compare(row1, row2, getColumnIndex());
        return ascending ? result : -result;
    }

    /* Adapted from Phil Milne's TableSorter implementation.
        This implementation, however, is not coupled to TableModel in any way,
        and may be used with list models and other types of models easily. */

    private int compare(int row1, int row2, int col) {
        Object o1 = getInputValue(row1, col);
        Object o2 = getInputValue(row2, col);
        // If both values are null return 0
        if (o1 == null && o2 == null) {
            return 0;
        }
        else if (o1 == null) { // Define null less than everything.
            return -1;
        }
        else if (o2 == null) {
            return 1;
        }
        // JW: have to handle null first of all
        // Seemingly, Comparators are not required to handle null. Hmm...
        if (comparator != null) {
            return comparator.compare(o1, o2);
        }
        // make sure we use the collator for string compares
        if ((o1.getClass() == String.class) && (o2.getClass() == String.class)) {
            return collator.compare((String)o1, (String) o2);
        }
        // patch from Jens Elkner (#189)
        if ((o1.getClass().isInstance(o2)) && (o1 instanceof Comparable)) {
            Comparable c1 = (Comparable) o1;
            return c1.compareTo(o2);
        } else if (o2.getClass().isInstance(o1) && (o2 instanceof Comparable)) {
            Comparable c2 = (Comparable) o2;
            return -c2.compareTo(o1);
        }
        return collator.compare(o1.toString(), o2.toString());
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
        refresh();
    }

    public SortOrder getSortOrder() {
        return isAscending() ? SortOrder.ASCENDING : SortOrder.DESCENDING; 
    }
    
    /**
     * Updates itself according to the SortKey's properties.
     * 
     * @param sortKey 
     * @throws IllegalArgumentException if sortKey = null
     * @throws IllegalArgumentException if !sortKey.sortOrder().isSorted
     */
    public void setSortKey(SortKey sortKey) {
        if ((sortKey == null) || (!sortKey.getSortOrder().isSorted())) {
            throw new IllegalArgumentException("SortKey must not be null with sorted SortOrder");
        }
        boolean forceRefresh = false;
        if (ascending != sortKey.getSortOrder().isAscending()) {
            forceRefresh = true;
            ascending = sortKey.getSortOrder().isAscending();
        }
        if (((comparator != null) && !comparator.equals(sortKey.getComparator()))
              || ((comparator == null) && (sortKey.getComparator() != null))) {
            forceRefresh = true;
            comparator = sortKey.getComparator();
        }
        // JW: take care of notification - refresh only if needed but then guarantee!
        // problem - if columns are the same, super does nothing
        if (getColumnIndex() != sortKey.getColumn()) {
            // super handles event notification
            forceRefresh = false;
            setColumnIndex(sortKey.getColumn());
        }
        if (forceRefresh) {
            refresh();
        }
    }
    
    public SortKey getSortKey() {
        return new SortKey(getSortOrder(), getColumnIndex(), getComparator());
    }
    public void toggle() {
        ascending = !ascending;
        refresh();
    }

}
