/*
 * $Id: UIColorHighlighterAddon.java,v 1.3 2008/10/14 22:31:43 rah003 Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import org.jdesktop.swingx.plaf.windows.WindowsLookAndFeelAddons;
import org.jdesktop.swingx.util.OS;

/**
 * Loads LF specific background striping colors. 
 * 
 * The colors are based on the LF selection colors for certain
 * LFs and themes, for unknown LFs/themes a generic grey is used.  
 * 
 * 
 * @author Jeanette Winzenburg
 * @author Karl Schaefer
 */
public class UIColorHighlighterAddon extends AbstractComponentAddon {

    public UIColorHighlighterAddon() {
        super("UIColorHighlighter");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addBasicDefaults(LookAndFeelAddons addon, DefaultsList defaults) {
        super.addBasicDefaults(addon, defaults);
        
        defaults.add("UIColorHighlighter.stripingBackground", new ColorUIResource(229, 229, 229));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addMacDefaults(LookAndFeelAddons addon, DefaultsList defaults) {
        super.addMacDefaults(addon, defaults);
        
        defaults.add("UIColorHighlighter.stripingBackground", new ColorUIResource(237, 243, 254));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addMetalDefaults(LookAndFeelAddons addon, DefaultsList defaults) {
        super.addMetalDefaults(addon, defaults);
        
        if (MetalLookAndFeel.getCurrentTheme() instanceof OceanTheme) {
            defaults.add("UIColorHighlighter.stripingBackground", new ColorUIResource(230, 238, 246));
        } else {
            defaults.add("UIColorHighlighter.stripingBackground", new ColorUIResource(235, 235, 255));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addWindowsDefaults(LookAndFeelAddons addon, DefaultsList defaults) {
        super.addWindowsDefaults(addon, defaults);
        
        if (OS.isUsingWindowsVisualStyles()) {
            String xpStyle = OS.getWindowsVisualStyle();
            
            if (WindowsLookAndFeelAddons.HOMESTEAD_VISUAL_STYLE
                    .equalsIgnoreCase(xpStyle)) {
                defaults.add("UIColorHighlighter.stripingBackground", new ColorUIResource(228, 231, 219));
            } else if (WindowsLookAndFeelAddons.SILVER_VISUAL_STYLE
                    .equalsIgnoreCase(xpStyle)) {
                defaults.add("UIColorHighlighter.stripingBackground", new ColorUIResource(235, 235, 236));
            } else {
                // default blue
                defaults.add("UIColorHighlighter.stripingBackground", new ColorUIResource(224, 233, 246));
            }
            
        } else {
            defaults.add("UIColorHighlighter.stripingBackground", new ColorUIResource(218, 222, 233));
        }
    }
}
