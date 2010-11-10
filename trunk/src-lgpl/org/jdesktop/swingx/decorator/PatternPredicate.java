/*
 * $Id: PatternPredicate.java,v 1.8 2008/10/14 22:31:38 rah003 Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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

import java.awt.Component;
import java.util.regex.Pattern;

/**
 * Pattern based HighlightPredicate.
 * 
 * @author Jeanette Winzenburg
 */
public class PatternPredicate implements HighlightPredicate {
    public static final int ALL = -1;
    
    private int highlightColumn;
    private int testColumn;
    private Pattern pattern;
    
    /**
     * Instantiates a Predicate with the given Pattern and testColumn index
     * (in model coordinates) highlighting all columns.
     *  A column index of -1 is interpreted
     * as "all". (PENDING: search forum for the exact definition, legacy 
     * base pattern and search behave differently?) 
     * 
     * 
     * @param pattern the Pattern to test the cell value against
     * @param testColumn the column index of the cell which contains the value
     *   to test against the pattern 
     */
    public PatternPredicate(Pattern pattern, int testColumn) {
        this(pattern, testColumn, ALL);
    }

    /**
     * Instantiates a Predicate with the given Pattern and test-/decorate
     * column index in model coordinates. A column index of -1 is interpreted
     * as "all". (PENDING: search forum for the exact definition, legacy 
     * base pattern and search behave differently?) 
     * 
     * 
     * @param pattern the Pattern to test the cell value against
     * @param testColumn the column index of the cell which contains the value
     *   to test against the pattern 
     * @param decorateColumn the column index of the cell which should be 
     *   decorated if the test against the value succeeds.
     */
    public PatternPredicate(Pattern pattern, int testColumn, int decorateColumn) {
        this.pattern = pattern;
        this.testColumn = testColumn;
        this.highlightColumn = decorateColumn;
    }

    /**
     * Instantiates a Predicate with the given regex and test-/decorate
     * column index in model coordinates. The pattern string is compiled to a 
     * Pattern with flags 0. A column index of -1 is interpreted
     * as "all". (PENDING: search forum for the exact definition, legacy 
     * base pattern and search behave differently?) 
     * 
     * @param regex the regex string to test the cell value against
     * @param testColumn the column index of the cell which contains the value
     *   to test against the pattern 
     * @param decorateColumn the column index of the cell which should be 
     *   decorated if the test against the value succeeds.
     */
    public PatternPredicate(String regex, int testColumn, int decorateColumn) {
        this(Pattern.compile(regex), testColumn, decorateColumn);
    }

    /**
     * Instantiates a Predicate with the given regex and test
     * column index in model coordinates. The pattern string is compiled to a 
     * Pattern with flags 0. A column index of -1 is interpreted
     * as "all". (PENDING: search forum for the exact definition, legacy 
     * base pattern and search behave differently?) 
     * 
     * @param regex the regex string to test the cell value against
     * @param testColumn the column index of the cell which contains the value
     *   to test against the pattern 
     */
    public PatternPredicate(String regex, int testColumn) {
        this(Pattern.compile(regex), testColumn);
    }

    public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
        if (isHighlightCandidate(renderer, adapter)) {
            return test(renderer, adapter);
        }
        return false;
    }

    /**
     * Test the value. This is called only if the 
     * pre-check returned true, because accessing the 
     * value might be potentially costly
     * @param renderer
     * @param adapter
     * @return
     */
    private boolean test(Component renderer, ComponentAdapter adapter) {
        if (!adapter.isTestable(testColumn))
            return false;
        String value = adapter.getString(testColumn);
        
        if ((value == null) || (value.length() == 0)) {
            return false;
        }
        return pattern.matcher(value).find();
     // this is pre-767-swingx: consistent string api
//        Object value = adapter.getValue(testColumn);
//
//        if (value == null) {
//            return false;
//        } else {
//            return pattern.matcher(value.toString()).find();
//        }
    }

    /**
     * A quick pre-check.
     * 
     * @param renderer
     * @param adapter
     * @return
     */
    private boolean isHighlightCandidate(Component renderer, ComponentAdapter adapter) {
        return (pattern != null) && 
            ((highlightColumn < 0) ||
               (highlightColumn == adapter.viewToModel(adapter.column)));
    }

    /**
     * 
     * @return returns the column index to decorate (in model coordinates)
     */
    public int getHighlightColumn() {
        return highlightColumn;
    }

    /**
     * 
     * @return returns the Pattern to test the cell value against
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * 
     * @return the column to use for testing (in model coordinates)
     */
    public int getTestColumn() {
        return testColumn;
    }

}
