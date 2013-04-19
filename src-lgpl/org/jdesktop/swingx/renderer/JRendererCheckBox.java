/*
 * $Id: JRendererCheckBox.java 3512 2009-09-22 07:54:02Z kleopatra $
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
package org.jdesktop.swingx.renderer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JCheckBox;
import javax.swing.UIManager;

import org.jdesktop.swingx.painter.Painter;

/**
 * A <code>JCheckBox</code> optimized for usage in renderers and
 * with a minimal background painter support. <p>
 * 
 * <i>Note</i>: the painter support will be switched to painter_work as 
 * soon it enters main. 
 * 
 * @author Jeanette Winzenburg
 */
public class JRendererCheckBox extends JCheckBox implements PainterAware {
    protected Painter painter;

    /**
     * {@inheritDoc}
     */
    public Painter getPainter() {
        return painter;
    }


    /**
     * {@inheritDoc}
     */
    public void setPainter(Painter painter) {
        Painter old = getPainter();
        this.painter = painter;
        if (painter != null) {
            // ui maps to !opaque
            // Note: this is incomplete - need to keep track of the 
            // "real" contentfilled property
            // JW: revisit - really needed after fix for #897?
            setContentAreaFilled(false);
        } // PENDING JW: asymetric! no else?
//        else {
//            setContentAreaFilled(true);
//        }
        firePropertyChange("painter", old, getPainter());
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to return true if there is no painter.<p>
     * 
     */
    @Override
    public boolean isOpaque() {
        // JW: fix for #897, not sure of any side-effects
        // contentAreaFilled and opaque might be inconsistent
        return painter == null;
    }

    /**
     * Overridden for performance reasons.<p>
     * PENDING: Think about Painters and opaqueness?
     * 
     */
//    @Override
//    public boolean isOpaque() { 
//        Color back = getBackground();
//        Component p = getParent(); 
//        if (p != null) { 
//            p = p.getParent(); 
//        }
//        // p should now be the JTable. 
//        boolean colorMatch = (back != null) && (p != null) && 
//            back.equals(p.getBackground()) && 
//                        p.isOpaque();
//        return !colorMatch && super.isOpaque(); 
//    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to not automatically de/register itself from/to the ToolTipManager.
     * As rendering component it is not considered to be active in any way, so the
     * manager must not listen. 
     */
    @Override
    public void setToolTipText(String text) {
        putClientProperty(TOOL_TIP_TEXT_KEY, text);
    }

    
    @Override
    protected void paintComponent(Graphics g) {
        // JW: hack around for #1178-swingx (core issue) 
        // grab painting if Nimbus detected
        if ((painter != null) || isNimbus()) {
            // we have a custom (background) painter
            // try to inject if possible
            // there's no guarantee - some LFs have their own background 
            // handling  elsewhere
            paintComponentWithPainter((Graphics2D) g);
        } else {
            // no painter - delegate to super
            super.paintComponent(g);
        }
    }

    /**
     * Hack around Nimbus not respecting background colors if UIResource.
     * So by-pass ... 
     * 
     * @return
     */
    private boolean isNimbus() {
        return UIManager.getLookAndFeel().getName().contains("Nimbus");
    }


    /**
     * 
     * Hack around AbstractPainter.paint bug which disposes the Graphics.
     * So here we give it a scratch to paint on. <p>
     * TODO - remove again, the issue is fixed?
     * 
     * @param g the graphics to paint on
     */
    private void paintPainter(Graphics g) {
        if (painter == null) return;
        // fail fast: we assume that g must not be null
        // which throws an NPE here instead deeper down the bowels
        // this differs from corresponding core implementation!
        Graphics2D scratch = (Graphics2D) g.create();
        try {
            painter.paint(scratch, this, getWidth(), getHeight());
        }
        finally {
            scratch.dispose();
        }
    }

    /**
     * 
     * @param g
     */
    protected void paintComponentWithPainter(Graphics2D g) {
        // 1. be sure to fill the background
        // 2. paint the painter
        // by-pass ui.update and hook into ui.paint directly
        if (ui != null) {
            // fail fast: we assume that g must not be null
            // which throws an NPE here instead deeper down the bowels
            // this differs from corresponding core implementation!
            Graphics scratchGraphics = g.create();
            try {
                scratchGraphics.setColor(getBackground());
                scratchGraphics.fillRect(0, 0, getWidth(), getHeight());
                paintPainter(g);
                ui.paint(scratchGraphics, this);
            } finally {
                scratchGraphics.dispose();
            }
        }

    }
    
    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a> 
     * for more information.
     *
     * @since 1.5
     */
    @Override
    public void invalidate() {}

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a> 
     * for more information.
     */
    @Override
    public void validate() {}

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a> 
     * for more information.
     */
    @Override
    public void revalidate() {}

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a> 
     * for more information.
     */
    @Override
    public void repaint(long tm, int x, int y, int width, int height) {}

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a> 
     * for more information.
     */
    @Override
    public void repaint(Rectangle r) { }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a> 
     * for more information.
     *
     * @since 1.5
     */
    @Override
    public void repaint() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a> 
     * for more information.
     */
    @Override
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {  
        // Strings get interned...
        if ("text".equals(propertyName)) {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a> 
     * for more information.
     */
    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) { }



}