/*
 * $Id: DefaultTableRenderer.java,v 1.25 2009/04/28 10:55:29 kleopatra Exp $
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


import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;


/**
 * Adapter to glue SwingX renderer support to core api. It has convenience
 * constructors to create a LabelProvider, optionally configured with a
 * StringValue and horizontal alignment. Typically, client code does not
 * interact with this class except at instantiation time.
 * <p>
 * 
 * <code>JXTable</code> uses instances of this as per-class default renderers.
 * 
 * <pre><code>
 * setDefaultRenderer(Object.class, new DefaultTableRenderer());
 * setDefaultRenderer(Number.class, new DefaultTableRenderer(
 *         FormatStringValues.NUMBER_TO_STRING, JLabel.RIGHT));
 * setDefaultRenderer(Date.class, new DefaultTableRenderer(
 *         FormatStringValues.DATE_TO_STRING));
 * // use the same center aligned default for Image/Icon
 * TableCellRenderer renderer = new DefaultTableRenderer(new MappedValue(
 *         StringValues.EMPTY, IconValues.ICON), JLabel.CENTER);
 * setDefaultRenderer(Icon.class, renderer);
 * setDefaultRenderer(ImageIcon.class, renderer);
 * // use a CheckBoxProvider for booleans
 * setDefaultRenderer(Boolean.class,
 *         new DefaultTableRenderer(new CheckBoxProvider()));
 * </code></pre>
 * 
 * 
 * 
 * @author Jeanette Winzenburg
 * 
 * @see ComponentProvider
 * @see LabelProvider
 * @see StringValue
 * @see IconValue
 * @see MappedValue
 * @see CellContext
 * 
 */
public class DefaultTableRenderer extends AbstractRenderer
        implements TableCellRenderer {

    private TableCellContext cellContext;
    
    
    /**
     * Instantiates a default table renderer with the default component
     * provider. 
     * 
     * @see #DefaultTableRenderer(ComponentProvider)
     */
    public DefaultTableRenderer() {
        this((ComponentProvider) null);
    }

    /**
     * Instantiates a default table renderer with the given component provider.
     * If the controller is null, creates and uses a default. The default
     * provider is of type <code>LabelProvider</code>.
     * 
     * @param componentProvider the provider of the configured component to
     *        use for cell rendering
     */
    public DefaultTableRenderer(ComponentProvider componentProvider) {
        super(componentProvider);
        this.cellContext = new TableCellContext();
    }

    /**
     * Instantiates a default table renderer with a default component
     * provider using the given converter. 
     * 
     * @param converter the converter to use for mapping the
     *   content value to a String representation.
     *   
     * @see #DefaultTableRenderer(ComponentProvider)  
     */
    public DefaultTableRenderer(StringValue converter) {
        this(new LabelProvider(converter));
    }

    /**
     * Instantiates a default table renderer with a default component
     * provider using the given converter and horizontal 
     * alignment. 
     * 
     * @param converter the converter to use for mapping the
     *   content value to a String representation.
     *   
     * @see #DefaultTableRenderer(ComponentProvider)  
     */
    public DefaultTableRenderer(StringValue converter, int alignment) {
        this(new LabelProvider(converter, alignment));
    }

    /**
     * Intantiates a default table renderer with default component provider
     * using both converters.
     * 
     * @param stringValue the converter to use for the string representation
     * @param iconValue the converter to use for the icon representation
     */
    public DefaultTableRenderer(StringValue stringValue, IconValue iconValue) {
        this(new MappedValue(stringValue, iconValue));
    }

    /**
     * Intantiates a default table renderer with default component provider
     * using both converters and the given alignment.
     * 
     * @param stringValue the converter to use for the string representation
     * @param iconValue the converter to use for the icon representation
     * @param alignment the rendering component's horizontal alignment
     */
    public DefaultTableRenderer(StringValue stringValue, IconValue iconValue,
            int alignment) {
        this(new MappedValue(stringValue, iconValue), alignment);
    }

    // -------------- implements javax.swing.table.TableCellRenderer
    /**
     * 
     * Returns a configured component, appropriate to render the given
     * list cell.  
     * 
     * @param table the <code>JTable</code>
     * @param value the value to assign to the cell at
     *        <code>[row, column]</code>
     * @param isSelected true if cell is selected
     * @param hasFocus true if cell has focus
     * @param row the row of the cell to render
     * @param column the column of the cell to render
     * @return the default table cell renderer
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        cellContext.installContext(table, value, row, column, isSelected, hasFocus,
                true, true);
        Component comp = componentController.getRendererComponent(cellContext);
        // fix issue #1040-swingx: memory leak if value not released
        cellContext.replaceValue(null);
        return comp;
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    protected ComponentProvider createDefaultComponentProvider() {
        return new LabelProvider();
    }


}


