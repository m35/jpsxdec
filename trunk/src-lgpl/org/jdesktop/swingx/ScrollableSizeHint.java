/*
 * $Id$
 *
 * Copyright 2009 Sun Microsystems, Inc., 4150 Network Circle,
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
 *
 */
package org.jdesktop.swingx;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.util.Contract;

/**
 * Sizing hints for layout, useful f.i. in a Scrollable implementation.<p>
 * 
 * Inspired by <a href=
 * http://tips4java.wordpress.com/2009/12/20/scrollable-panel/> Rob Camick</a>.
 * <p>
 * PENDING JW: naming... suggestions?<br>
 * KS: I'd go with TrackingHint or ScrollableTrackingHint, since it is used in getScrollableTracksViewportXXX.
 * 
 * 
 * @author Jeanette Winzenburg
 * @author Karl Schaefer
 */
public enum ScrollableSizeHint {
    /**
     * Size should be unchanged.
     */
    NONE {
        /**
         * {@inheritDoc}
         */
        @Override
        boolean getTracksParentSizeImpl(JComponent component, int orientation) {
            return false;
        }
    }, 
    
    /**
     * Size should be adjusted to parent size. 
     */
    FIT {
        /**
         * {@inheritDoc}
         */
        @Override
        boolean getTracksParentSizeImpl(JComponent component, int orientation) {
            return true;
        }
    }, 
    
    /**
     * Stretches the component when its parent is larger than its minimum size.
     */
    MINIMUM_STRETCH {
        /**
         * {@inheritDoc}
         */
        @Override
        boolean getTracksParentSizeImpl(JComponent component, int orientation) {
            switch (orientation) {
            case SwingConstants.HORIZONTAL:
                return component.getParent() instanceof JViewport
                        && component.getParent().getWidth() > component.getMinimumSize().width
                        && component.getParent().getWidth() < component.getMaximumSize().width;
            case SwingConstants.VERTICAL:
                return component.getParent() instanceof JViewport
                        && component.getParent().getHeight() > component.getMinimumSize().height
                        && component.getParent().getHeight() < component.getMaximumSize().height;
            default:
                throw new IllegalArgumentException("invalid orientation");
            }
        }
    },
    
    /**
     * Stretches the component when its parent is larger than its preferred size.
     */
    PREFERRED_STRETCH {
        /**
         * {@inheritDoc}
         */
        @Override
        boolean getTracksParentSizeImpl(JComponent component, int orientation) {
            switch (orientation) {
            case SwingConstants.HORIZONTAL:
                return component.getParent() instanceof JViewport
                        && component.getParent().getWidth() > component.getPreferredSize().width
                        && component.getParent().getWidth() < component.getMaximumSize().width;
            case SwingConstants.VERTICAL:
                return component.getParent() instanceof JViewport
                        && component.getParent().getHeight() > component.getPreferredSize().height
                        && component.getParent().getHeight() < component.getMaximumSize().height;
            default:
                throw new IllegalArgumentException("invalid orientation");
            }
        }
    },
    
    /**
     * Width should be stretched to parent width if smaller, unchanged otherwise.
     */
    @Deprecated
    HORIZONTAL_STRETCH {
        /**
         * {@inheritDoc}
         * <p>
         * Returns {@code false}.
         */
        @Override
        @Deprecated
        public boolean isVerticalCompatible() {
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        boolean getTracksParentSizeImpl(JComponent component, int orientation) {
            if (orientation != SwingConstants.HORIZONTAL) {
                throw new IllegalArgumentException();
            }
            
            if (component.getParent() != null) {
                return component.getParent().getWidth() > component.getPreferredSize().width
                    && component.getParent().getWidth() < component.getMaximumSize().width;
            }

            return false;
        }
    },
    
    /**
     * Width should be stretched to parent height if smaller, unchanged otherwise.
     */
    @Deprecated
    VERTICAL_STRETCH {
        /**
         * {@inheritDoc}
         * <p>
         * Returns {@code false}.
         */
        @Override
        @Deprecated
        public boolean isHorizontalCompatible() {
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        boolean getTracksParentSizeImpl(JComponent component, int orientation) {
            if (orientation != SwingConstants.VERTICAL) {
                throw new IllegalArgumentException();
            }
            
            if (component.getParent() != null) {
                return component.getParent().getHeight() > component.getPreferredSize().height
                        && component.getParent().getWidth() < component.getMaximumSize().height;
            }

            return false;
        }
    };
    
    /**
     * Returns a boolean indicating whether the component's size should be
     * adjusted to parent.
     *  
     * @param component the component resize, must not be null
     * @return a boolean indicating whether the component's size should be
     *    adjusted to parent
     *    
     * @throws NullPointerException if component is null   
     */
    @Deprecated
    public boolean getTracksParentSize(JComponent component) {
        return getTracksParentSize(component, 0);
    }
    
    /**
     * Returns a boolean indicating whether the component's size should be
     * adjusted to parent.
     *  
     * @param component the component resize, must not be null
     * @return a boolean indicating whether the component's size should be
     *    adjusted to parent
     *    
     * @throws NullPointerException if component is null
     * @throws IllegalArgumentException if orientation is invalid
     */
    public boolean getTracksParentSize(JComponent component, int orientation) {
        Contract.asNotNull(component, "component must be not-null");
        
        return getTracksParentSizeImpl(component, orientation);
    }

    /**
     * Returns a boolean indicating whether the hint can be used in 
     * horizontal orientation.
     * 
     * @return a boolean indicating whether the hint can be used in horizontal
     *   orientation. 
     */
    @Deprecated
    public boolean isHorizontalCompatible() {
        return true;
    }
    
    /**
     * Returns a boolean indicating whether the hint can be used in 
     * vertical orientation.
     * 
     * @return a boolean indicating whether the hint can be used in vertical
     *   orientation. 
     */
    @Deprecated
    public boolean isVerticalCompatible() {
        return true;
    }

    /**
     * Determines whether the supplied component is smaller than its parent; used to determine
     * whether to track with the parents size.
     * 
     * @param component
     *            the component to test
     * @param orientation
     *            the orientation to test
     * @return {@code true} to track; {@code false} otherwise
     */
    abstract boolean getTracksParentSizeImpl(JComponent component, int orientation);
}
