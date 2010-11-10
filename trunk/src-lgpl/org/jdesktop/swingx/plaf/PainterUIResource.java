/*
 * $Id: PainterUIResource.java,v 1.3 2009/02/04 20:00:22 kschaefe Exp $
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

package org.jdesktop.swingx.plaf;

import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.plaf.UIResource;
import org.jdesktop.swingx.painter.Painter;

/**
 * An implementation of Painter as a UIResource. UI classes that create Painters
 * should use this class.
 * 
 * @author rbair
 * @author Karl George Schaefer
 * @param <T> a subclass of JComponent
 */
public class PainterUIResource<T extends JComponent> implements Painter<T>, UIResource {
    private Painter<? super T> p;

    /**
     * Creates a new instance of PainterUIResource with the specified delegate
     * painter.
     * 
     * @param p
     *            the delegate painter
     */
    public PainterUIResource(Painter<? super T> p) {
        this.p = p;
    }
    
    /**
     * {@inheritDoc}
     */
    public void paint(Graphics2D g, T component, int width, int height) {
        if (p != null) {
            p.paint(g, component, width, height);
        }
    }
}
