/*
 * $Id: EmptyIcon.java,v 1.3 2007/05/20 21:17:52 kschaefe Exp $
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

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;

/**
 * An empty icon with arbitrary width and height.
 */
public final class EmptyIcon implements Icon, Serializable {

  private int width;
  private int height;
  
  public EmptyIcon() {
    this(0, 0);
  }
  
  public EmptyIcon(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public int getIconHeight() {
    return height;
  }

  public int getIconWidth() {
    return width;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
  }

}
