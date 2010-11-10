/*
 * $Id: ColumnControlButtonAddon.java,v 1.4 2008/10/14 22:31:43 rah003 Exp $
 *
 * Copyright 2007 Sun Microsystems, Inc., 4150 Network Circle,
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

import javax.swing.plaf.InsetsUIResource;

import org.jdesktop.swingx.icon.ColumnControlIcon;

/**
 * Addon to load LF specific properties for the ColumnControlButton.
 * 
 * @author Jeanette Winzenburg
 */
public class ColumnControlButtonAddon extends AbstractComponentAddon {

    /**
     * Instantiates the addon for ColumnControlButton.
     */
    public ColumnControlButtonAddon() {
        super("ColumnControlButton");
    }

    @Override
    protected void addBasicDefaults(LookAndFeelAddons addon,
            DefaultsList defaults) {
        super.addBasicDefaults(addon, defaults);
        defaults.add("ColumnControlButton.actionIcon", new ColumnControlIcon());
        defaults.add("ColumnControlButton.margin", new InsetsUIResource(1, 2, 2, 1)); 
    }

    
}
