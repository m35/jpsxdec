/*
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

package com.sun.java.swing.plaf.windows;

import java.awt.Color;
import java.awt.Graphics;


/**
 * Performance improvement for {@link org.jdesktop.swingx.JXTreeTable}.
 * <p>
 * The infamous JTreeTable. Nearly all Java tree-tables are based on an
 * example published by Sun many years ago. The trick is to render just a
 * small rectangle of the {@link javax.swing.JTree} component in each cell. 
 * With the cells stacked up, it looks like one continuous component.
 * <p>
 * To render each rectangle, {@link Graphics#translate(int, int)} is called
 * when the JTree returned by {@link javax.swing.table.TableCellRenderer} is
 * painted. This is functional in theory, but Windows has some serious
 * performance side-effects when painting with a translation. Now combine
 * that with painting the tree dotted lines one dot at a time as the
 * {@link javax.swing.plaf.basic.BasicTreeUI} does, and the tree-table becomes
 * painfully slow.
 * <p>
 * This little hack overrides the dotted line paint
 * call with a single solid line paint call. Performance is improved
 * significantly. 
 * <p>
 * This class is instantiated dynamically on Windows and passed to
 * {@link org.jdesktop.swingx.JXTreeTable#setUI(javax.swing.plaf.TableUI)}.
 * <p>
 * I also tried {@link java.awt.Graphics2D#setStroke(java.awt.Stroke)} to a dotted line, but the
 * performance was still abysmal.
 *
 * @author Michael
 */
public class WindowsTreeTableUI extends WindowsTreeUI {

    @Override
    protected void drawDashedHorizontalLine(Graphics g, int y, int x1, int x2) {
        Color c = g.getColor();
        g.setColor(c.brighter());
        g.drawLine(x1, y, x2, y);
        g.setColor(c);
    }

    @Override
    protected void drawDashedVerticalLine(Graphics g, int x, int y1, int y2) {
        Color c = g.getColor();
        g.setColor(c.brighter());
        g.drawLine(x, y1, x, y2);
        g.setColor(c);
    }

}