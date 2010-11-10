/*
 * $Id: ShuttleSorter.java,v 1.10 2008/10/14 22:31:37 rah003 Exp $
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

/**
 * Pluggable sorting filter.
 *
 * @author Ramesh Gupta
 */
public class ShuttleSorter extends Sorter {
    private int[]    toPrevious;

    public ShuttleSorter() {
        this(0, true);
    }

    public ShuttleSorter(int col, boolean ascending) {
        // pending .. chain to this
        super(col, ascending);
    }

    public ShuttleSorter(int col, boolean ascending, Comparator comparator) {
        super(col, ascending, comparator);
    }
    
    @Override
    protected void init() {
        // JW: ?? called from super, so toPrevious is still null after running
        // this method??
        toPrevious = new int[0];
    }

    
    /**
     * Resets the internal row mappings from this filter to the previous filter.
     */
    @Override
    protected void reset() {
        int inputSize = getInputSize();
        toPrevious = new int[inputSize];
        fromPrevious = new int[inputSize];
        for (int i = 0; i < inputSize; i++) {
            toPrevious[i] = i;    // reset before sorting
        }
    }

    /**
     *  Performs the sort. Calls sort only if canFilter(), 
     *  regards all values as equal (== doesn't sort) otherwise.
     *  
     *  @see #canFilter()
     */
    @Override
    protected void filter() {
        if (canFilter() ) {
            sort(toPrevious.clone(), toPrevious, 0, toPrevious.length);
        }
        // Generate inverse map for implementing convertRowIndexToView();
        for (int i = 0; i < toPrevious.length; i++) {
            fromPrevious[toPrevious[i]] = i;
        }
    }

    /**
     * This is a quickfix for #55-swingx: NPE if sorter is in pipeline.
     * No way to automatically cleanup "from the outside" if the sorter 
     * is hooked to a columnIndex which is no longer valid. So we check here
     * for assigned and valid index.<p>
     *  
     *  PENDING: should be done higher up?
     *  
     * @return boolean to indicate whether accessing the values is valid.
     */
    protected boolean canFilter() {
        return adapter != null && (getColumnIndex() < adapter.getColumnCount());
    }

    @Override
    public int getSize() {
        return toPrevious.length;
    }

    @Override
    protected int mapTowardModel(int row) {
        return toPrevious[row];
    }

// Adapted from Phil Milne's TableSorter implementation.
// This implementation, however, is not coupled to TableModel in any way,
// and may be used with list models and other types of models easily.

// This is a home-grown implementation which we have not had time
// to research - it may perform poorly in some circumstances. It
// requires twice the space of an in-place algorithm and makes
// NlogN assigments shuttling the values between the two
// arrays. The number of compares appears to vary between N-1 and
// NlogN depending on the initial order but the main reason for
// using it here is that, unlike qsort, it is stable.
    protected void sort(int from[], int to[], int low, int high) {
        if (high - low < 2) {
            return;
        }
        int middle = (low + high) >> 1;

        sort(to, from, low, middle);
        sort(to, from, middle, high);

        int p = low;
        int q = middle;

        /* This is an optional short-cut; at each recursive call,
         check to see if the elements in this subset are already
         ordered.  If so, no further comparisons are needed; the
         sub-array can just be copied.  The array must be copied rather
         than assigned otherwise sister calls in the recursion might
         get out of sinc.  When the number of elements is three they
         are partitioned so that the first set, [low, mid), has one
         element and and the second, [mid, high), has two. We skip the
         optimisation when the number of elements is three or less as
         the first compare in the normal merge will produce the same
         sequence of steps. This optimisation seems to be worthwhile
         for partially ordered lists but some analysis is needed to
         find out how the performance drops to Nlog(N) as the initial
         order diminishes - it may drop very quickly.  */

        if (high - low >= 4 && compare(from[middle - 1], from[middle]) <= 0) {
            for (int i = low; i < high; i++) {
                to[i] = from[i];
            }
            return;
        }

        // A normal merge.

        for (int i = low; i < high; i++) {
            if (q >= high || (p < middle && compare(from[p], from[q]) <= 0)) {
                to[i] = from[p++];
            }
            else {
                to[i] = from[q++];
            }
        }
    }
}
