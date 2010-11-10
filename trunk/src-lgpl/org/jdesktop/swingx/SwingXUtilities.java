/*
 * $Id: SwingXUtilities.java,v 1.11 2009/04/02 20:35:54 kschaefe Exp $
 *
 * Copyright 2008 Sun Microsystems, Inc., 4150 Network Circle,
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Locale;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentInputMapUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.text.html.HTMLDocument;

/**
 * A collection of utility methods for Swing(X) classes.
 * 
 * <ul>
 * PENDING JW: think about location of this class and/or its methods, Options:
 * 
 *  <li> move this class to the swingx utils package which already has a bunch of xxUtils
 *  <li> move methods between xxUtils classes as appropriate (one window/comp related util)
 *  <li> keep here in swingx (consistent with swingutilities in core)
 * </ul>
 * @author Karl George Schaefer
 */
public final class SwingXUtilities {
    private SwingXUtilities() {
        //does nothing
    }


    /**
     * A helper for creating and updating key bindings for components with
     * mnemonics. The {@code pressed} action will be invoked when the mnemonic
     * is activated.
     * <p>
     * TODO establish an interface for the mnemonic properties, such as {@code
     * MnemonicEnabled} and change signature to {@code public static <T extends
     * JComponent & MnemonicEnabled> void updateMnemonicBinding(T c, String
     * pressed)}
     * 
     * @param c
     *            the component bindings to update
     * @param pressed
     *            the name of the action in the action map to invoke when the
     *            mnemonic is pressed
     * @throws NullPointerException
     *             if the component is {@code null}
     */
    public static void updateMnemonicBinding(JComponent c, String pressed) {
        updateMnemonicBinding(c, pressed, null);
    }
    
    /**
     * A helper for creating and updating key bindings for components with
     * mnemonics. The {@code pressed} action will be invoked when the mnemonic
     * is activated and the {@code released} action will be invoked when the
     * mnemonic is deactivated.
     * <p>
     * TODO establish an interface for the mnemonic properties, such as {@code
     * MnemonicEnabled} and change signature to {@code public static <T extends
     * JComponent & MnemonicEnabled> void updateMnemonicBinding(T c, String
     * pressed, String released)}
     * 
     * @param c
     *            the component bindings to update
     * @param pressed
     *            the name of the action in the action map to invoke when the
     *            mnemonic is pressed
     * @param released
     *            the name of the action in the action map to invoke when the
     *            mnemonic is released (if the action is a toggle style, then
     *            this parameter should be {@code null})
     * @throws NullPointerException
     *             if the component is {@code null}
     */
    public static void updateMnemonicBinding(JComponent c, String pressed, String released) {
        Class<?> clazz = c.getClass();
        int m = -1;
        
        try {
            Method mtd = clazz.getMethod("getMnemonic");
            m = (Integer) mtd.invoke(c);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("unable to access mnemonic", e);
        }
        
        InputMap map = SwingUtilities.getUIInputMap(c,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        if (m != 0) {
            if (map == null) {
                map = new ComponentInputMapUIResource(c);
                SwingUtilities.replaceUIInputMap(c,
                        JComponent.WHEN_IN_FOCUSED_WINDOW, map);
            }
            
            map.clear();
            
            //TODO is ALT_MASK right for all platforms?
            map.put(KeyStroke.getKeyStroke(m, InputEvent.ALT_MASK, false),
                    pressed);
            map.put(KeyStroke.getKeyStroke(m, InputEvent.ALT_MASK, true),
                    released);
            map.put(KeyStroke.getKeyStroke(m, 0, true), released);
        } else {
            if (map != null) {
                map.clear();
            }
        }
    }
    
    private static Component[] getChildren(Component c) {
        Component[] children = null;
        
        if (c instanceof MenuElement) {
            MenuElement[] elements = ((MenuElement) c).getSubElements();
            children = new Component[elements.length];
            
            for (int i = 0; i < elements.length; i++) {
                children[i] = elements[i].getComponent();
            }
        } else if (c instanceof Container) {
            children = ((Container) c).getComponents();
        }
        
        return children;
    }
    
    /**
     * Enables or disables of the components in the tree starting with {@code c}.
     * 
     * @param c
     *                the starting component
     * @param enabled
     *                {@code true} if the component is to enabled; {@code false} otherwise
     */
    public static void setComponentTreeEnabled(Component c, boolean enabled) {
        c.setEnabled(enabled);
        
        Component[] children = getChildren(c);
            
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                setComponentTreeEnabled(children[i], enabled);
            }
        }
    }
    
    /**
     * Sets the locale for an entire component hierarchy to the specified
     * locale.
     * 
     * @param c
     *                the starting component
     * @param locale
     *                the locale to set
     */
    public static void setComponentTreeLocale(Component c, Locale locale) {
        c.setLocale(locale);
        
        Component[] children = getChildren(c);
        
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                setComponentTreeLocale(children[i], locale);
            }
        }
    }

    /**
     * Sets the background for an entire component hierarchy to the specified
     * color.
     * 
     * @param c
     *                the starting component
     * @param color
     *                the color to set
     */
    public static void setComponentTreeBackground(Component c, Color color) {
        c.setBackground(color);
        
        Component[] children = getChildren(c);
        
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                setComponentTreeBackground(children[i], color);
            }
        }
    }

    /**
     * Sets the foreground for an entire component hierarchy to the specified
     * color.
     * 
     * @param c
     *                the starting component
     * @param color
     *                the color to set
     */
    public static void setComponentTreeForeground(Component c, Color color) {
        c.setForeground(color);
        
        Component[] children = getChildren(c);
        
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                setComponentTreeForeground(children[i], color);
            }
        }
    }

    /**
     * Sets the font for an entire component hierarchy to the specified font.
     * 
     * @param c
     *            the starting component
     * @param font
     *            the font to set
     */
    public static void setComponentTreeFont(Component c, Font font) {
        c.setFont(font);
        
        Component[] children = getChildren(c);
        
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                setComponentTreeFont(children[i], font);
            }
        }
    }

    private static String STYLESHEET = 
        "body { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0;"
        + " font-family: %s; font-size: %dpt;  }"
        + "a, p, li { margin-top: 0; margin-bottom: 0; margin-left: 0;"
        + " margin-right: 0; font-family: %s; font-size: %dpt;  }";
    
    /**
     * Sets the font used for HTML displays to the specified font. Components
     * that display HTML do not necessarily honor font properties, since the
     * HTML document can override these values. Calling {@code setHtmlFont}
     * after the data is set will force the HTML display to use the font
     * specified to this method.
     * 
     * @param doc
     *            the HTML document to update
     * @param font
     *            the font to use
     * @throws NullPointerException
     *             if any parameter is {@code null}
     */
    public static void setHtmlFont(HTMLDocument doc, Font font) {
        String stylesheet = String.format(STYLESHEET, font.getName(),
                font.getSize(), font.getName(), font.getSize());
        
        try {
            doc.getStyleSheet().loadRules(new StringReader(stylesheet), null);
        } catch (IOException e) {
            //this should never happen with our sheet
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * Updates the componentTreeUI of all top-level windows of the 
     * current application.
     * 
     */
    public static void updateAllComponentTreeUIs() {
        for (Frame frame : Frame.getFrames()) {
            updateAllComponentTreeUIs(frame);
        }
        
    }



    /**
     * Updates the componentTreeUI of the given window and all its
     * owned windows, recursively.
     * 
     * 
     * @param window the window to update
     */
    public static void updateAllComponentTreeUIs(Window window) {
        SwingUtilities.updateComponentTreeUI(window);
        for (Window owned : window.getOwnedWindows()) {
            updateAllComponentTreeUIs(owned);
        }
    }

    /**
     * An improved version of
     * {@link SwingUtilities#getAncestorOfClass(Class, Component)}. This method
     * traverses {@code JPopupMenu} invoker and uses generics to return an
     * appropriately typed object.
     * 
     * @param <T>
     *            the type of ancestor to find
     * @param clazz
     *            the class instance of the ancestor to find
     * @param c
     *            the component to start the search from
     * @return an ancestor of the correct type or {@code null} if no such
     *         ancestor exists. This method also returns {@code null} if any
     *         parameter is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAncestor(Class<T> clazz, Component c) {
        if (clazz == null || c == null) {
            return null;
        }
        
        Component parent = c.getParent();

        while (parent != null && !(clazz.isInstance(parent))) {
            parent = c instanceof JPopupMenu
                    ? ((JPopupMenu) c).getInvoker() : c.getParent();
        }
        
        return (T) parent;
    }

    /**
     * Returns whether the component is part of the parent's
     * container hierarchy. If a parent in the chain is of type 
     * JPopupMenu, the parent chain of its invoker is walked.
     * 
     * @param focusOwner
     * @param parent
     * @return true if the component is contained under the parent's 
     *    hierarchy, coping with JPopupMenus.
     */
    public static boolean isDescendingFrom(Component focusOwner, Component parent) {
        while (focusOwner !=  null) {
            if (focusOwner instanceof JPopupMenu) {
                focusOwner = ((JPopupMenu) focusOwner).getInvoker();
                if (focusOwner == null) {
                    return false;
                }
            }
            if (focusOwner == parent) {
                return true;
            }
            focusOwner = focusOwner.getParent();
        }
        return false;
    }

    /**
     * Checks and returns whether the given property should be replaced
     * by the UI's default value. 
     * 
     * @param property the property to check.
     * @return true if the given property should be replaced by the UI's
     *   default value, false otherwise. 
     */
    public static boolean isUIInstallable(Object property) {
       return (property == null) || (property instanceof UIResource);
    }

}
