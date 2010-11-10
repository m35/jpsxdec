/*
 * $Id: ColumnHeaderRenderer.java,v 1.24 2009/02/01 15:01:06 rah003 Exp $
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

package org.jdesktop.swingx.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.border.IconBorder;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.icon.SortArrowIcon;
import org.jdesktop.swingx.plaf.ColumnHeaderRendererAddon;
import org.jdesktop.swingx.plaf.LookAndFeelAddons;

/**
 * Header renderer class which renders column sort feedback (arrows).
 * <p>
 * Additionally, it allows to customize renderer properties like <code>Font</code>, 
 * <code>Alignment</code> and similar. This part needs to go somewhere else
 * when we switch to Mustang.
 * <p>
 * 
 * Note: #169-jdnc, #193-swingx - Header doesn't look right in winXP/mac seem - to be
 * fixed, but could be brittle. Won't do more about it, Mustang will take care once
 * SwingLabs is switched over to 1.6.
 * 
 * @status.target jdk15-glue
 * 
 * @author Amy Fowler
 * @author Ramesh Gupta
 * @author Jeanette Winzenburg
 */
public class ColumnHeaderRenderer extends JComponent 
    implements TableCellRenderer
    , UIResource {
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger
            .getLogger(ColumnHeaderRenderer.class.getName());
    // the inheritance is only to make sure we are updated on LF change
    public static final String UP_ICON_KEY = "ColumnHeaderRenderer.upIcon";
    public static final String DOWN_ICON_KEY = "ColumnHeaderRenderer.downIcon";
    public static final String VISTA_BORDER_HACK = "ColumnHeaderRenderer.vistaBorderHack";
    public static final String METAL_BORDER_HACK = "ColumnHeaderRenderer.metalBorderHack";
    static {
        LookAndFeelAddons.contribute(new ColumnHeaderRendererAddon());
    }
    private static TableCellRenderer sharedInstance = null;

    private static Icon defaultDownIcon = new SortArrowIcon(false);

    private static Icon defaultUpIcon = new SortArrowIcon(true);

    private Icon downIcon = defaultDownIcon;

    private Icon upIcon = defaultUpIcon;

    private IconBorder iconBorder = new IconBorder();
    private boolean antiAliasedText = false;

    private TableCellRenderer delegateRenderer;

    private LabelProperties label;

    /**
     * Returns the shared ColumnHeaderRenderer. <p> 
     * @return the shared header renderer.
     * 
     */
    public static TableCellRenderer getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new ColumnHeaderRenderer();
        }
        return sharedInstance;
    }

    /**
     * 
     * @return a <code>ColumnHeaderRenderer</code>
     */
    public static ColumnHeaderRenderer createColumnHeaderRenderer() {
        return new ColumnHeaderRenderer();
    }

    /*
     * JW: a story ... ordered from latest to older.
     * 
     * Yet another chapter: The problem with some LFs is that they
     * install a LF specific renderer in uidelegate.installUI, 
     * unconditionally overwriting the renderer created and installed
     * by the tableHeader.createDefaultRenderer. Looks like the
     * ui install is needed to support rollover (maybe other?) "live"
     * effects. If so, there are some implications:
     * 
     *  - the default renderer must not be shared across tableHeaders.
     *  - the ColumnHeaderRenderer's delegateRenderer must be updated
     *    to the last ui-installed default
     *  - the tableHeader must reset its default to the delegateRenderer
     *    before calling super.updateUI (to give the uidelegate the
     *    possibility to cleanup)  
     *
     * ------------------------------------------- 
     * latest: don't use a custom component and don't add the original
     * and the arrow - use the original only and compound a border with 
     * arrow icon. How does it look in XP/Mac?
     * 
     * 
     * ----------------- below is the comment as of ColumnHeaderRenderer
     * Original used a Label to show the typical text/icon part and another
     * Label to show the up/down arrows, added both to this and configured both
     * directly in getTableCellRendererComponent.
     * 
     * My first shot to solve the issues was to delegate the text/icon part to
     * the defaultRenderer as returned by the JTableHeader: replace the first
     * label with the rendererComponent of the renderer. In
     * getTableCellRendererComponent let the renderer configure the comp and
     * "move" the border from the delegateComp to this - so it's bordering both
     * the comp and the arrow.
     * 
     * Besides not working (WinXP style headers are still not shown :-( it has
     * issues with opaqueness: different combinations of this.opaque and
     * delegate.opaque all have issues 
     *  1. if the delegate is not explicitly set to false the border looks wrong 
     *  2. if this is set to true we can have custom background 
     *     per cell but no setting the header background has no
     *     effect - and changing LF doesn't take up the LF default background ...
     *  3. if this is set to false we can't have custom cell background
     * 
     * 
     * 
     */

    public ColumnHeaderRenderer() {
        label = new LabelProperties();
        initDelegate();
        updateIconUI();
    }

    public ColumnHeaderRenderer(JTableHeader header) {
        label = new LabelProperties();
        updateUI(header);
    }

    private void initDelegate() {
        // JW: hacking around core problems with standalone table header
        // so we create a core table and use its default header
        // PENDING: sure about not introducing memory issues? 
//        JTable table = new JTable();
        JTableHeader header = null; //table.getTableHeader();
        if (header == null) {
            header = new JTableHeader();
        }
        delegateRenderer = header.getDefaultRenderer();
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        Component comp = configureDelegate(table, value, isSelected, hasFocus, rowIndex,
                columnIndex);
        if ((table instanceof JXTable) && (comp instanceof JComponent)) {
            // work-around core issues
            hackBorder((JComponent) comp);
        }
        adjustComponentOrientation(comp);
        return comp;
    }


    /**
     * 
     * @param component
     */
    private void hackBorder(JComponent component) {
        if (hackBorder(component, VISTA_BORDER_HACK)) return;
        hackBorder(component, METAL_BORDER_HACK);
    }

    /**
     * 
     * @param component
     */
    private boolean hackBorder(JComponent component, Object key) {
        Border hackBorder = UIManager.getBorder(key);
        if (hackBorder == null) return false;
            component.setBorder(hackBorder);
            return true;
    }

    /**
     * Adjusts the Component's orientation to JXTable's CO if appropriate.
     * Here: always.
     * 
     * @param stamp the component to adjust.
     */
    protected void adjustComponentOrientation(Component stamp) {
        if (stamp.getComponentOrientation().equals(getComponentOrientation())) return;
        stamp.applyComponentOrientation(getComponentOrientation());
    }

    private Component configureDelegate(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        Component comp = delegateRenderer.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, rowIndex, columnIndex);

        applyLabelProperties(comp);
        return comp;
    }

    private void applyLabelProperties(Component delegateRendererComponent) {
        if (delegateRendererComponent instanceof JLabel) {
            label.applyPropertiesTo((JLabel) delegateRendererComponent);
        } else {
            label.applyPropertiesTo(delegateRenderer);
        }
    }

    public void setAntiAliasedText(boolean antiAlias) {
        this.antiAliasedText = antiAlias;
    }

    public boolean getAntiAliasedText() {
        return antiAliasedText;
    }

    @Override
    public void setBackground(Color background) {
        // this is called somewhere along initialization of super?
        if (label != null) {
            Color old = getBackground();
            label.setBackground(background);
            firePropertyChange("background", old, getBackground());
        }
    }
    
    @Override
    public Color getBackground() {
        return label.getBackground();
    }

    @Override
    public void setForeground(Color foreground) {
         if (label != null) {
             Color old = getForeground();
             label.setForeground(foreground);
             firePropertyChange("foreground", old, getForeground());
        }
    }
    
    @Override
    public Color getForeground() {
        return label.getForeground();
    }

    @Override
    public void setFont(Font font) {
        if (label != null) {
            Font old = getFont();
            label.setFont(font);
            firePropertyChange("font", old, getFont());
        }
    }
    
    @Override
    public Font getFont() {
        return label.getFont();
    }

    public void setDownIcon(Icon icon) {
        Icon old = getDownIcon();
        this.downIcon = icon;
        firePropertyChange("downIcon", old, getDownIcon());
    }

    public Icon getDownIcon() {
        return downIcon;
    }

    public void setUpIcon(Icon icon) {
        Icon old = getUpIcon();
        this.upIcon = icon;
        firePropertyChange("upIcon", old, getUpIcon());
    }

    public Icon getUpIcon() {
        return upIcon;
    }

    public void setHorizontalAlignment(int alignment) {
        int old = getHorizontalAlignment();
        label.setHorizontalAlignment(alignment);
        firePropertyChange("horizontalAlignment", old, getHorizontalAlignment());
    }

    public int getHorizontalAlignment() {
        return label.getHorizontalAlignment();
    }

    public void setHorizontalTextPosition(int textPosition) {
        int old = getHorizontalTextPosition();
        label.setHorizontalTextPosition(textPosition);
        firePropertyChange("horizontalTextPosition", old, getHorizontalTextPosition());
    }

    public int getHorizontalTextPosition() {
        return label.getHorizontalTextPosition();
    }

    public void setIcon(Icon icon) {
        Icon old = getIcon();
        label.setIcon(icon);
        firePropertyChange("icon", old, getIcon());
    }

    public Icon getIcon() {
        return label.getIcon();
    }

    public void setIconTextGap(int iconTextGap) {
        int old = getIconTextGap();
        label.setIconTextGap(iconTextGap);
        firePropertyChange("iconTextGap", old, getIconTextGap());
    }

    public int getIconTextGap() {
        return label.getIconTextGap();
    }

    public void setVerticalAlignment(int alignment) {
        int old = getVerticalAlignment();
        label.setVerticalAlignment(alignment);
        firePropertyChange("verticalAlignment", old, getVerticalAlignment());
    }

    public int getVerticalAlignment() {
        return label.getVerticalAlignment();
    }

    public void setVerticalTextPosition(int textPosition) {
        int old = getVerticalTextPosition();
        label.setVerticalTextPosition(textPosition);
        firePropertyChange("verticalTextPosition", old, getVerticalTextPosition());
    }

    public int getVerticalTextPosition() {
        return label.getVerticalTextPosition();
    }

    /**
     * @return the delegate renderer.
     */
    public TableCellRenderer getDelegateRenderer() {
        return delegateRenderer;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        initDelegate();
        updateIconUI();
    }
    
    public void updateUI(JTableHeader header) {
        updateIconUI();
        if (header.getDefaultRenderer() != this) {
            delegateRenderer = header.getDefaultRenderer();
        }
    }
    
    private void updateIconUI() {
        if (getUpIcon() instanceof UIResource) {
            Icon icon = UIManager.getIcon(UP_ICON_KEY);
            setUpIcon(icon != null ? icon : defaultUpIcon);
            
        }
        if (getDownIcon() instanceof UIResource) {
            Icon icon = UIManager.getIcon(DOWN_ICON_KEY);
            setDownIcon(icon != null ? icon : defaultDownIcon);
            
        }
    }

}
