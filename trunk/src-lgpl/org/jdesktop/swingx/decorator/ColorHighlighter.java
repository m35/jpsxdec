/*
 * $Id: ColorHighlighter.java,v 1.11 2008/10/14 22:31:37 rah003 Exp $
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

import java.awt.Color;
import java.awt.Component;

/**
 * A Highlighter to modify component colors. <p>
 * 
 * @author Jeanette Winzenburg
 */
public class ColorHighlighter extends AbstractHighlighter {
    
    private Color background;
    private Color foreground;
    private Color selectedBackground;
    private Color selectedForeground;

    /**
     * Instantiates a ColorHighlighter with null colors and default
     * HighlightPredicate.
     */
    public ColorHighlighter() {
        this(null);
    }

    /**
     * Instantiates a ColorHighlighter with null colors and uses the
     * specified HighlightPredicate.
     * 
     * @param predicate the HighlightPredicate to use.
     */
    public ColorHighlighter(HighlightPredicate predicate) {
        this(predicate, null, null);
    }

    /**
     * Constructs a <code>ColorHighlighter</code> with the specified
     * background and foreground colors and null section colors. Uses
     * the default predicate.
     *
     * @param cellBackground background color for unselected cell state
     * @param cellForeground foreground color for unselected cell state
     */
    public ColorHighlighter(Color cellBackground, Color cellForeground) {
        this(null, cellBackground, cellForeground);
    }

    /**
     * Constructs a <code>ColorHighlighter</code> with the specified
     * unselected colors and HighlightPredicate. 
     * Initializes selected colors to null.
     * 
     * @param predicate the HighlightPredicate to use.
     * @param cellBackground background color for unselected cell state
     * @param cellForeground foreground color for unselected cell state
     */
    public ColorHighlighter(HighlightPredicate predicate, Color cellBackground, 
            Color cellForeground) {
        this(predicate, cellBackground, cellForeground, null, null);
    }
    
    /**
     * Constructs a <code>ColorHighlighter</code> with the specified
     * background and foreground colors for unselected and selected cells.
     * Uses the default HighlightPredicate.
     *
     * @param cellBackground background color for unselected cell state
     * @param cellForeground foreground color for unselected cell state
     * @param selectedBackground background color for selected cell state
     * @param selectedForeground foreground color for selected cell state
    */
    public ColorHighlighter(Color cellBackground, Color cellForeground, 
            Color selectedBackground, Color selectedForeground) {
        this(null, cellBackground, cellForeground, selectedBackground, selectedForeground);
    }


    /**
     * Constructs a <code>ColorHighlighter</code> with the specified colors
     * and HighlightPredicate.
     * 
     * @param predicate the HighlightPredicate to use.
     * @param cellBackground background color for unselected cell state
     * @param cellForeground foreground color for unselected cell state
     * @param selectedBackground background color for selected cell state
     * @param selectedForeground foreground color for selected cell state
     */
    public ColorHighlighter(HighlightPredicate predicate, Color cellBackground,
            Color cellForeground, Color selectedBackground,
            Color selectedForeground) {
        super(predicate);
        this.background = cellBackground;
        this.foreground = cellForeground;
        this.selectedBackground = selectedBackground;
        this.selectedForeground = selectedForeground;
    }

    

    /**
     * {@inheritDoc}
     */
    @Override
    protected Component doHighlight(Component renderer, ComponentAdapter adapter) {
        applyBackground(renderer, adapter);
        applyForeground(renderer, adapter);
        return renderer;
    }
   
    
    /**
    * Applies a suitable background for the renderer component within the
    * specified adapter. <p>
    * 
    * This implementation applies its background or selectedBackground color
    * (depending on the adapter's selected state) if != null. 
    * Otherwise it does nothing.
    *
    * @param renderer the cell renderer component that is to be decorated
    * @param adapter the ComponentAdapter for this decorate operation
    */
    protected void applyBackground(Component renderer, ComponentAdapter adapter) {
        Color color = adapter.isSelected() ? getSelectedBackground() : getBackground();
        if (color != null) {
            renderer.setBackground(color);
        }

    }
    /**
    * Applies a suitable foreground for the renderer component within the
    * specified adapter. <p>
    * 
    * This implementation applies its foreground or selectedfForeground color
    * (depending on the adapter's selected state) if != null. 
    * Otherwise it does nothing.
    *
    * @param renderer the cell renderer component that is to be decorated
    * @param adapter the ComponentAdapter for this decorate operation
     */
    protected void applyForeground(Component renderer, ComponentAdapter adapter) {
        Color color = adapter.isSelected() ? getSelectedForeground() : getForeground();
        if (color != null) {
            renderer.setForeground(color);
        }
    }


//---------------------- state
    
    /**
     * Returns the background color of this <code>ColorHighlighter</code>.
     *
     * @return the background color of this <code>ColorHighlighter</code>,
     *          or null, if no background color has been set
     */
    public Color getBackground() {
        return background;
    }

    /**
     * Sets the background color of this <code>ColorHighlighter</code> and 
     * notifies registered ChangeListeners.
     *  
     * @param color the background color of this <code>Highlighter</code>,
     *          or null, to clear any existing background color
     */
    public void setBackground(Color color) {
        if (areEqual(color, getBackground())) return;
        background = color;
        fireStateChanged();
    }

    /**
     * Returns the foreground color of this <code>ColorHighlighter</code>.
     *
     * @return the foreground color of this <code>ColorHighlighter</code>,
     *          or null, if no foreground color has been set
     */
    public Color getForeground() {
        return foreground;
    }

    /**
     * Sets the foreground color of this <code>ColorHighlighter</code> and notifies
     * registered ChangeListeners.
     *
     * @param color the foreground color of this <code>ColorHighlighter</code>,
     *          or null, to clear any existing foreground color
     */
    public void setForeground(Color color) {
        if (areEqual(color, getForeground())) return;
        foreground = color;
        fireStateChanged();
    }

    /**
     * Returns the selected background color of this <code>ColorHighlighter</code>.
     *
     * @return the selected background color of this <code>ColorHighlighter</code>,
     *          or null, if no selected background color has been set
     */
    public Color getSelectedBackground() {
        return selectedBackground;
    }

    /**
     * Sets the selected background color of this <code>ColorHighlighter</code>
     * and notifies registered ChangeListeners.
     *
     * @param color the selected background color of this <code>ColorHighlighter</code>,
     *          or null, to clear any existing selected background color
     */
    public void setSelectedBackground(Color color) {
        if (areEqual(color, getSelectedBackground()))return;
        selectedBackground = color;
        fireStateChanged();
    }

    /**
     * Returns the selected foreground color of this <code>ColorHighlighter</code>.
     *
     * @return the selected foreground color of this <code>ColorHighlighter</code>,
     *          or null, if no selected foreground color has been set
     */
    public Color getSelectedForeground() {
        return selectedForeground;
    }

    /**
     * Sets the selected foreground color of this <code>ColorHighlighter</code> and
     * notifies registered ChangeListeners.
     *
     * @param color the selected foreground color of this <code>ColorHighlighter</code>,
     *          or null, to clear any existing selected foreground color
     */
    public void setSelectedForeground(Color color) {
        if (areEqual(color, getSelectedForeground())) return;
        selectedForeground = color;
        fireStateChanged();
    }



}
