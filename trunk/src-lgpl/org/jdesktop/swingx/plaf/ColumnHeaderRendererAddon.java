/*
 * $Id: ColumnHeaderRendererAddon.java,v 1.13 2008/12/16 09:58:59 kleopatra Exp $
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

import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.metal.MetalBorders;

import org.jdesktop.swingx.plaf.windows.WindowsClassicLookAndFeelAddons;
import org.jdesktop.swingx.table.ColumnHeaderRenderer;
import org.jdesktop.swingx.util.JVM;
import org.jdesktop.swingx.util.OS;

/**
 * Addon for ColumnHeaderRenderer.<p>
 * Loads LF specific sort icons.
 * 
 * @author Jeanette Winzenburg
 * @author Karl Schaefer
 */
public class ColumnHeaderRendererAddon extends AbstractComponentAddon {
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger
            .getLogger(ColumnHeaderRendererAddon.class.getName());
    
    public ColumnHeaderRendererAddon() {
        super("ColumnHeaderRenderer");
    }
  
    /**
     * {@inheritDoc}
     */
    @Override
    protected void addBasicDefaults(LookAndFeelAddons addon,
            DefaultsList defaults) {
        super.addBasicDefaults(addon, defaults);
        
        defaults.add(ColumnHeaderRenderer.UP_ICON_KEY,
                LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "basic/resources/sort-jlf-up.png"));
        defaults.add(ColumnHeaderRenderer.DOWN_ICON_KEY,
                LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "basic/resources/sort-jlf-dn.png"));
        hackMetalBorder(addon, defaults);

    }

    private void hackMetalBorder(LookAndFeelAddons addon, DefaultsList defaults) {
        Border border = UIManager.getBorder("TableHeader.cellBorder");
        if (border instanceof MetalBorders.TableHeaderBorder) {
            border = new BorderUIResource.CompoundBorderUIResource(border, 
                    BorderFactory.createEmptyBorder());
            // too heavyweight ...
//            UIManager.put("TableHeader.cellBorder", border);
//            LOG.info("updated border " + border);
            defaults.add(ColumnHeaderRenderer.METAL_BORDER_HACK, border);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addLinuxDefaults(LookAndFeelAddons addon,
            DefaultsList defaults) {
        super.addLinuxDefaults(addon, defaults);
        
        if (isSynth()) {
            defaults.add(ColumnHeaderRenderer.UP_ICON_KEY,
                    LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "linux/resources/sort-gtk-up.png"));
            defaults.add(ColumnHeaderRenderer.DOWN_ICON_KEY,
                    LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "linux/resources/sort-gtk-dn.png"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addMacDefaults(LookAndFeelAddons addon, DefaultsList defaults) {
        super.addMacDefaults(addon, defaults);
        
        defaults.add(ColumnHeaderRenderer.UP_ICON_KEY,
                LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "macosx/resources/sort-osx-up.png"));
        defaults.add(ColumnHeaderRenderer.DOWN_ICON_KEY,
                LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "macosx/resources/sort-osx-dn.png"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addWindowsDefaults(LookAndFeelAddons addon, DefaultsList defaults) {
        super.addWindowsDefaults(addon, defaults);
        
        if (OS.isWindowsXP()) {
            defaults.add(ColumnHeaderRenderer.UP_ICON_KEY,
                    LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "windows/resources/sort-xp-up.png"));
            defaults.add(ColumnHeaderRenderer.DOWN_ICON_KEY,
                    LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "windows/resources/sort-xp-dn.png"));
        } else {
            defaults.add(ColumnHeaderRenderer.UP_ICON_KEY,
                    LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "windows/resources/sort-w2k-up.png"));
            defaults.add(ColumnHeaderRenderer.DOWN_ICON_KEY,
                    LookAndFeel.makeIcon(ColumnHeaderRendererAddon.class, "windows/resources/sort-w2k-dn.png"));
        }
        
        hackVistaHeaderBorder(addon, defaults);
    }
    
    /**
     * Hack around the oversized vista header border installed by core.
     * Registers a (5,5,5,5) empty border for vista themes. Does nothing if the
     * OS is not Vista or the addon is classic windows or the JVM isn't 1.5. 
     * 
     * PENDING: can we have XP themes under vista? If so, this needs to be changed - 
     * most probably the xp border is okay.
     * 
     * @param addon
     * @param defaults
     */
    private void hackVistaHeaderBorder(LookAndFeelAddons addon, DefaultsList defaults) {
        // do nothing if not vista or for classic design under vista
        if (!OS.isWindowsVista() || (addon instanceof WindowsClassicLookAndFeelAddons)
                || !JVM.current().isOneDotFive())
            return;
        defaults.add(ColumnHeaderRenderer.VISTA_BORDER_HACK,
                new BorderUIResource.EmptyBorderUIResource(5, 5, 5, 5));
    }

}
