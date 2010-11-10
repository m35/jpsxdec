/*
 * $Id: PatternFilter.java,v 1.12 2008/10/14 22:31:38 rah003 Exp $
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

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Pluggable pattern filter.
 *
 * @author Ramesh Gupta
 */
public class PatternFilter extends Filter implements PatternMatcher {
    private ArrayList<Integer> toPrevious;
    protected Pattern pattern = null;

    /**
     * Instantiates a includeAll PatternFilter with matchFlag 0 on
     * column 0.
     *
     */
    public PatternFilter() {
        this(null, 0, 0);
    }

    /**
     * Instantiates a PatternFilter with a Pattern compiled from the
     * given regex and matchFlags on the column in model coordinates.
     * 
     * @param regularExpr the regex to compile, a null or empty String
     *   is interpreted as ".*"
     * @param matchFlags the matchflags to use in the compile
     * @param col the column to filter in model coordinates.
     */
    public PatternFilter(String regularExpr, int matchFlags, int col) {
        super(col);
        setPattern(regularExpr, matchFlags);
    }

    /**
     * Convenience to set the pattern in terms of a regex and
     * matchFlags, which are used to compile the pattern to apply.
     * 
     * @param regularExpr the regex to compile, a null or empty String
     *   is interpreted as ".*"
     * @param matchFlags the matchflags to use in the compile
     * @see java.util.regex.Pattern#compile for details
     */
    public void setPattern(String regularExpr, int matchFlags) {
        if ((regularExpr == null) || (regularExpr.length() == 0)) {
            regularExpr = ".*";
        }
        setPattern(Pattern.compile(regularExpr, matchFlags));
    }

    /**
     * Sets the pattern used by this filter for matching.
     *
     * @param pattern the pattern used by this filter for matching
     * @see java.util.regex.Pattern
     */
    public void setPattern(Pattern pattern) {
        if (pattern == null) {
            setPattern(null, 0);
        } else {
            this.pattern = pattern;
            refresh();
        }
    }

    /**
     * Returns the pattern used by this filter for matching.
     *
     * @return the pattern used by this filter for matching
     * @see java.util.regex.Pattern
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Resets the internal row mappings from this filter to the previous filter.
     */
    @Override
    protected void reset() {
        toPrevious.clear();
        int inputSize = getInputSize();
        fromPrevious = new int[inputSize];  // fromPrevious is inherited protected
        for (int i = 0; i < inputSize; i++) {
            fromPrevious[i] = -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void filter() {
        if (pattern != null) {
            int inputSize = getInputSize();
            int current = 0;
            for (int i = 0; i < inputSize; i++) {
                if (test(i)) {
                    toPrevious.add(new Integer(i));
                    // generate inverse map entry while we are here
                    fromPrevious[i] = current++;
                }
            }
        }
    }

    /**
     * Tests whether the given row (in this filter's coordinates) should
     * be added. <p>
     * 
     * PENDING JW: why is this public? called from a protected method? 
     * @param row the row to test
     * @return true if the row should be added, false if not.
     */
    public boolean test(int row) {
        // TODO: PENDING: wrong false?
        // null pattern should be treated the same as null searchString
        // which is open
        // !testable should be clarified to mean "ignore" when filtering
        if (pattern == null) {
            return false;
        }

        // ask the adapter if the column should be includes
        if (!adapter.isTestable(getColumnIndex())) {
            return false; 
        }
        String text = getInputString(row, getColumnIndex());
        return isEmpty(text) ? false : pattern.matcher(text).find();
// pre-767-swingx: use componentAdapter's string rep
//        Object value = getInputValue(row, getColumnIndex());
//        return value == null ? false : pattern.matcher(value.toString()).find();
    }

    /**
     * @param text
     * @return
     */
    private boolean isEmpty(String text) {
        return (text == null) || (text.length() == 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() {
        return toPrevious.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int mapTowardModel(int row) {
        return toPrevious.get(row);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() {
        toPrevious = new ArrayList<Integer>();
    }

}
