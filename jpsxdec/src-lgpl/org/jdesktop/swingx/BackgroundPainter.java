/*
 * $Id: BackgroundPainter.java 3954 2011-03-15 16:34:38Z kschaefe $
 *
 * Copyright 2010 Sun Microsystems, Inc., 4150 Network Circle,
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
package org.jdesktop.swingx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.UIManager;

import org.jdesktop.swingx.painter.Painter;

/**
 * A painter for handling backgrounds.
 * 
 * @author kschaefer
 */
class BackgroundPainter implements Painter<JComponent>, Serializable {
    private final Color color;
    
    public BackgroundPainter(Color color) {
        this.color = color;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(Graphics2D g, JComponent object, int width, int height) {
        if (color == null) {
            return;
        }
        
        if (object.isOpaque() || object instanceof AlphaPaintable || UIManager.getLookAndFeel().getID().equals("Nimbus")) {
            g.setColor(color);
            g.fillRect(0, 0, width, height);
        }
    }
}
