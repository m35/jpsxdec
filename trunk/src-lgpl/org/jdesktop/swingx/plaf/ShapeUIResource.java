/*
 * $Id: ShapeUIResource.java,v 1.1 2008/01/03 19:08:32 rah003 Exp $
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

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.plaf.UIResource;

/**
 * An implementation of Shape that implements UIResource.  UI
 * classes that create Shapes should use this class.
 *
 * @author rah003
 */
public class ShapeUIResource implements Shape, UIResource {
    private Shape s;
    
    /** Creates a new instance of PainterUIResource */
    public ShapeUIResource(Shape p) {
        this.s = p;
    }
    
    public boolean contains(Point2D p) {
        return s.contains(p);
    }

    public boolean contains(Rectangle2D r) {
        return s.contains(r);
    }

    public boolean contains(double x, double y) {
        return s.contains(x, y);
    }

    public boolean contains(double x, double y, double w, double h) {
        return s.contains(x, y, w, h);
    }

    public Rectangle getBounds() {
        return s.getBounds();
    }

    public Rectangle2D getBounds2D() {
        return s.getBounds2D();
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return s.getPathIterator(at);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return s.getPathIterator(at, flatness);
    }

    public boolean intersects(Rectangle2D r) {
        return s.intersects(r);
    }

    public boolean intersects(double x, double y, double w, double h) {
        return s.intersects(x, y, w, h);
    }
}
