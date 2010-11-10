/*
 * $Id: ColumnControlIcon.java,v 1.6 2008/02/20 17:52:44 kleopatra Exp $
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

package org.jdesktop.swingx.icon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.plaf.UIResource;

/**
 * Icon class for rendering icon which indicates user control of
 * column visibility.
 * @author Amy Fowler
 * @version 1.0
 */
public class ColumnControlIcon implements Icon, UIResource {
    private int width = 10;
    private int height = 10;

    /** TODO: need to support small, medium, large */
    public ColumnControlIcon() {
    }

    public int getIconWidth() {
        return width;
    }

    public int getIconHeight() {
        return height;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color color = c.getForeground();
        g.setColor(color);

        // draw horizontal lines
        g.drawLine(x, y, x+8, y);
        g.drawLine(x, y+2, x+8, y+2);
        g.drawLine(x, y+8, x+2, y+8);

        // draw vertical lines
        g.drawLine(x, y+1, x, y+7);
        g.drawLine(x+4, y+1, x+4, y+4);
        g.drawLine(x+8, y+1, x+8, y+4);

        // draw arrow
        g.drawLine(x+3, y+6, x+9, y+6);
        g.drawLine(x+4, y+7, x+8, y+7);
        g.drawLine(x+5, y+8, x+7, y+8);
        g.drawLine(x+6, y+9, x+6, y+9);

    }

    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel(new ColumnControlIcon());
        frame.getContentPane().add(BorderLayout.CENTER, label);
        frame.pack();
        frame.setVisible(true);  
    }

}
