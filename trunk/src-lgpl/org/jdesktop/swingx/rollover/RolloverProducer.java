/*
 * $Id: RolloverProducer.java,v 1.2 2009/03/11 12:06:01 kleopatra Exp $
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
package org.jdesktop.swingx.rollover;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

/**
 * Mouse/Motion/Listener which maps mouse coordinates to client coordinates
 * and stores these as client properties in the target JComponent. The exact
 * mapping process is left to subclasses. Typically, they will map to "cell"
 * coordinates. <p>
 * 
 * Note: this class assumes that the target component is of type JComponent.<p>
 * Note: this implementation is stateful, it can't be shared across different 
 * instances of a target component.<p>
 * 
 * 
 * @author Jeanette Winzenburg
 */
public abstract class RolloverProducer implements MouseListener, MouseMotionListener {

    /** 
     * Key for client property mapped from mouse-triggered action.
     * Note that the actual mouse-event which results in setting the property
     * depends on the implementation of the concrete RolloverProducer. 
     */
    public static final String CLICKED_KEY = "swingx.clicked";

    /** Key for client property mapped from rollover events */
    public static final String ROLLOVER_KEY = "swingx.rollover";

    //        public static final String PRESSED_KEY = "swingx.pressed";

    //----------------- mouseListener

    /**
     * Implemented to map to client property clicked and fire always.
     */
    public void mouseReleased(MouseEvent e) {
        updateRollover(e, CLICKED_KEY, true);
    }

    /**
     * Implemented to map to client property rollover and fire only if client
     * coordinate changed.
     */
    public void mouseEntered(MouseEvent e) {
        updateRollover(e, ROLLOVER_KEY, false);
    }

    /**
     * Implemented to remove client properties rollover and clicked. if the
     * source is a JComponent. Does nothing otherwise.
     */
    public void mouseExited(MouseEvent e) {
        ((JComponent) e.getSource()).putClientProperty(ROLLOVER_KEY, null);
        ((JComponent) e.getSource()).putClientProperty(CLICKED_KEY, null);
    }

    /**
     * Implemented to do nothing.
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * Implemented to do nothing.
     */
    public void mousePressed(MouseEvent e) {
    }

    // ---------------- MouseMotionListener
    /**
     * Implemented to do nothing.
     * PENDING JW: probably should do something? Mapped coordinates will be out of synch
     * after a drag.
     */
    public void mouseDragged(MouseEvent e) {
    }

    /**
     * Implemented to map to client property rollover and fire only if client
     * coordinate changed.
     */
    public void mouseMoved(MouseEvent e) {
        updateRollover(e, ROLLOVER_KEY, false);
    }

    //---------------- mapping methods
    
    /**
     * Controls the mapping of the given mouse event to a client property. This
     * implementation first calls updateRolloverPoint to convert the mouse coordinates.
     * Then calls updateClientProperty to actually set the client property in the
     * 
     * @param e the MouseEvent to map to client coordinates
     * @param property the client property to map to
     * @param fireAlways a flag indicating whether a client event should be fired if unchanged.
     * 
     * @see #updateRolloverPoint(JComponent, Point)
     * @see #updateClientProperty(JComponent, String, boolean)
     */
    protected void updateRollover(MouseEvent e, String property,
            boolean fireAlways) {
        updateRolloverPoint((JComponent) e.getComponent(), e.getPoint());
        updateClientProperty((JComponent) e.getSource(), property, fireAlways);
    }

    /** Current mouse location in client coordinates. */
    protected Point rollover = new Point(-1, -1);

    /**
     * Sets the given client property to the value of current mouse location in 
     * client coordinates. If fireAlways, the property is force to fire a change.
     *  
     * @param component the target component
     * @param property the client property to set
     * @param fireAlways a flag indicating whether a client property 
     *  should be forced to fire an event.
     */
    protected void updateClientProperty(JComponent component, String property,
            boolean fireAlways) {
        if (fireAlways) {
            // fix Issue #864-swingx: force propertyChangeEvent
            component.putClientProperty(property, null);
            component.putClientProperty(property, new Point(rollover));
        } else {
            Point p = (Point) component.getClientProperty(property);
            if (p == null || (rollover.x != p.x) || (rollover.y != p.y)) {
                component.putClientProperty(property, new Point(rollover));
            }
        }
    }

    /**
     * Subclasses must implement to map the given mouse coordinates into
     * appropriate client coordinates. The result must be stored in the 
     * rollover field. 
     * 
     * @param component the target component which received a mouse event
     * @param mousePoint the mouse position of the event, coordinates are 
     *    component pixels
     */
    protected abstract void updateRolloverPoint(JComponent component, Point mousePoint);

}