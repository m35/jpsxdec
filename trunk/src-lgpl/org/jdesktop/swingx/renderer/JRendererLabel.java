/*
 * $Id: JRendererLabel.java,v 1.15 2009/02/01 15:01:03 rah003 Exp $
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JLabel;

import org.jdesktop.swingx.painter.Painter;

/**
 * A <code>JLabel</code> optimized for usage in renderers and
 * with a minimal background painter support. <p>
 * 
 * <i>Note</i>: the painter support will be switched to painter_work as 
 * soon it enters main. 
 * 
 * The reasoning for the performance-overrides is copied from core: <p>
 * 
 * The standard <code>JLabel</code> component was not
 * designed to be used this way and we want to avoid 
 * triggering a <code>revalidate</code> each time the
 * cell is drawn. This would greatly decrease performance because the
 * <code>revalidate</code> message would be
 * passed up the hierarchy of the container to determine whether any other
 * components would be affected.  
 * As the renderer is only parented for the lifetime of a painting operation
 * we similarly want to avoid the overhead associated with walking the
 * hierarchy for painting operations.
 * So this class
 * overrides the <code>validate</code>, <code>invalidate</code>,
 * <code>revalidate</code>, <code>repaint</code>, and
 * <code>firePropertyChange</code> methods to be 
 * no-ops and override the <code>isOpaque</code> method solely to improve
 * performance.  If you write your own renderer component,
 * please keep this performance consideration in mind.
 * <p>
 * 
 * @author Jeanette Winzenburg
 */
public class JRendererLabel extends JLabel implements PainterAware {

    protected Painter painter;
    private boolean strict;

    /**
     * 
     */
    public JRendererLabel() {
        super();
      setOpaque(true);
    }

    /**
     * Overridden for performance reasons.<p>
     * PENDING: Think about Painters and opaqueness?
     * 
     */
    @Override
    public boolean isOpaque() { 
        Color back = getBackground();
        Component p = getParent(); 
        if (p != null) { 
            p = p.getParent(); 
        }
        // p should now be the JTable. 
        boolean colorMatch = (back != null) && (p != null) && 
            back.equals(p.getBackground()) && 
                        p.isOpaque();
        return !colorMatch && super.isOpaque(); 
    }

    /**
     * {@inheritDoc}
     */
    public void setPainter(Painter painter) {
        Painter old = getPainter();
        this.painter = painter;
        firePropertyChange("painter", old, getPainter());
    }

    /**
     * {@inheritDoc}
     */
    public Painter getPainter() {
        return painter;
    }
    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to inject Painter's painting. <p>
     * TODO: cleanup logic - see JRendererCheckBox.
     * 
     */
    @Override
    protected void paintComponent(Graphics g) {
        if (painter != null) {
            // we have a custom (background) painter
            // try to inject if possible
            // there's no guarantee - some LFs have their own background 
            // handling  elsewhere
            if (isOpaque()) {
                // replace the paintComponent completely 
                paintComponentWithPainter((Graphics2D) g);
            } else {
                // transparent apply the background painter before calling super
                paintPainter(g);
                super.paintComponent(g);
            }
        } else {
            // nothing to worry about - delegate to super
            super.paintComponent(g);
        }
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
    
//    public void setStrictWidth(boolean strict) {
//        this.strict = strict;
//    }
//
//    @Override
//    public Dimension getMaximumSize() {
//        if (strict) {
//            return super.getMaximumSize();
//        }
//        Dimension max = super.getMaximumSize();
//        max.width = Integer.MAX_VALUE - 1;
//        return max;
//    }

    /**
     * PRE: painter != null, isOpaque()
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
            Graphics2D scratchGraphics = (Graphics2D) g.create();
                try {
                    scratchGraphics.setColor(getBackground());
                    scratchGraphics.fillRect(0, 0, getWidth(), getHeight());
                    paintPainter(g);
                    ui.paint(scratchGraphics, this);
                }
                finally {
                    scratchGraphics.dispose();
                }
        }        
    }

    
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