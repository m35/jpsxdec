/*
 * $Id: SortArrowIcon.java,v 1.3 2005/10/10 18:01:39 rbair Exp $
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.plaf.UIResource;

public class SortArrowIcon implements Icon, UIResource {
    private boolean ascending = true;
    //REMIND(aim): support more configurable sizes
    private int width = 8;
    private int height = 8;

    public SortArrowIcon(boolean ascending) {
        this.ascending = ascending;
    }

    public int getIconWidth() {
        return width;
    }

    public int getIconHeight() {
        return height;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color base = c.getBackground();
        Color shadow = base.darker().darker();
        Color highlight = Color.white;

        if (ascending) {
            g.setColor(shadow);
            int y1 = height-1;
            for(int x1=0; x1 < width/2 ; x1++) {
                g.drawLine(x + x1, y + y1, x + x1, y + y1 - 1);
                y1 -= ((x1+1 == (width/2)-1)? 1 : 2);
            }
            g.setColor(highlight);
            y1 = height-1;
            for (int x1 = width-1; x1 >= width / 2; x1--) {
                g.drawLine(x + x1, y + y1, x + x1, y + y1 - 1);
                y1 -= ( (x1 - 1 == (width / 2)) ? 1 : 2);
            }
            g.drawLine(x + 1, y + height-1, x + width - 1, y + height-1);
        } else {
            // descending
            g.setColor(shadow);
            int y1 = 1;
            for (int x1 = 0; x1 < width/2 ; x1++) {
                g.drawLine(x + x1, y + y1, x + x1, y + y1 + 1);
                y1 += (x1+1 == (width/2-1))? 1 : 2;
            }
            g.setColor(highlight);
            y1 = 1;
            for (int x1 = width - 1; x1 >= width/2; x1--) {
                g.drawLine(x + x1, y + y1, x + x1, y + y1 + 1);
                y1 += (x1-1 == width/2)? 1 : 2;
            }
            g.setColor(shadow);
            g.drawLine(x + 1, y + 1, x + width - 1, y + 1);
        }
    }
}
