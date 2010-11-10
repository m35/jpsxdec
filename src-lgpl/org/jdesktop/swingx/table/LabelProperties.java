/*
 * $Id: LabelProperties.java,v 1.1 2008/06/17 10:07:47 kleopatra Exp $
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
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.table.TableCellRenderer;

/**
 * Class used to store label properties in a single object so that they
 * may be applied as a set on renderers. <p>
 * 
 * NOTE JW: no longer used except in ColumnHeaderRenderer which is EOL (will be
 * removed once we switch over to jdk16). So probably will be removed as well.
 * 
 * @author Amy Fowler
 * @version 1.0
 */

public class LabelProperties extends JLabel {
    private static final int BACKGROUND_SET = 1;
    private static final int FOREGROUND_SET = 2;
    private static final int FONT_SET = 4;
    private static final int HORIZONTAL_ALIGNMENT_SET = 8;
    private static final int HORIZONTAL_TEXT_POSITION_SET = 16;
    private static final int ICON_SET = 32;
    private static final int ICON_TEXT_GAP_SET = 64;
    private static final int TEXT_SET = 128;
    private static final int VERTICAL_ALIGNMENT_SET = 256;
    private static final int VERTICAL_TEXT_POSITION_SET = 512;

    private int setFlags = 0;

    public LabelProperties() {
        super();
        addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                Object value = e.getNewValue();
                if (propertyName.equals("background")) {
                    if (value != null) {
                        setFlags |= BACKGROUND_SET;
                    } else {
                        setFlags &= (~BACKGROUND_SET);
                    }
                }
                else if (propertyName.equals("font")) {
                    if (value != null) {
                        setFlags |= FONT_SET;
                    } else {
                        setFlags &= (~FONT_SET);
                    }
                }
                else if (propertyName.equals("foreground")) {
                    if (value != null) {
                        setFlags |= FOREGROUND_SET;
                    } else {
                        setFlags &= (~FOREGROUND_SET);
                    }
                }
                else if (propertyName.equals("horizontalAlignment")) {
                    if (value != null && ((Integer)value).intValue() != -1) {
                        setFlags |= HORIZONTAL_ALIGNMENT_SET;
                    } else {
                        setFlags &= (~HORIZONTAL_ALIGNMENT_SET);
                    }
                }
                else if (propertyName.equals("horizontalTextPosition")) {
                    if (value != null && ((Integer)value).intValue() != -1) {
                        setFlags |= HORIZONTAL_TEXT_POSITION_SET;
                    } else {
                        setFlags &= (~HORIZONTAL_TEXT_POSITION_SET);
                    }
                }
                else if (propertyName.equals("icon")) {
                    if (value != null) {
                        setFlags |= ICON_SET;
                    } else {
                        setFlags &= (~ICON_SET);
                    }
                }
                else if (propertyName.equals("iconTextGap")) {
                    if (value != null && ((Integer)value).intValue() != -1) {
                        setFlags |= ICON_TEXT_GAP_SET;
                    } else {
                        setFlags &= (~ICON_TEXT_GAP_SET);
                    }
                }
                else if (propertyName.equals("text")) {
                    if (value != null) {
                        setFlags |= TEXT_SET;
                    } else {
                        setFlags &= (~TEXT_SET);
                    }
                }
                else if (propertyName.equals("verticalAlignment")) {
                    if (value != null && ((Integer)value).intValue() != -1) {
                        setFlags |= VERTICAL_ALIGNMENT_SET;
                    } else {
                        setFlags &= (~VERTICAL_ALIGNMENT_SET);
                    }
                }
                else if (propertyName.equals("verticalTextPosition")) {
                    if (value != null && ((Integer)value).intValue() != -1) {
                        setFlags |= VERTICAL_TEXT_POSITION_SET;
                    } else {
                        setFlags &= (~VERTICAL_TEXT_POSITION_SET);
                    }
                }
            }
        });
    }

    public LabelProperties(Color background, Color foreground, Font font,
                           int horizontalAlignment, int horizontalTextPosition,
                           int verticalAlignment, int verticalTextPosition,
                           Icon icon, int iconTextGap, String text) {
        this();
        setBackground(background);
        setForeground(foreground);
        setFont(font);
        setHorizontalAlignment(horizontalAlignment);
        setHorizontalTextPosition(horizontalTextPosition);
        setVerticalAlignment(verticalAlignment);
        setVerticalTextPosition(verticalTextPosition);
        setIcon(icon);
        setIconTextGap(iconTextGap);
        setText(text);
    }

    @Override
    public boolean isBackgroundSet() {
        return (setFlags & BACKGROUND_SET) > 0;
    }

    @Override
    public boolean isForegroundSet() {
        return (setFlags & FOREGROUND_SET) > 0;
    }

    @Override
    public boolean isFontSet() {
        return (setFlags & FONT_SET) > 0;
    }

    public boolean isHorizontalAlignmentSet() {
        return (setFlags & HORIZONTAL_ALIGNMENT_SET) > 0;
    }

    public boolean isHorizontalTextPositionSet() {
        return (setFlags & HORIZONTAL_TEXT_POSITION_SET) > 0;
    }

    public boolean isIconSet() {
        return (setFlags & ICON_SET) > 0;
    }

    public boolean isIconTextGapSet() {
        return (setFlags & ICON_TEXT_GAP_SET) > 0;
    }

    public boolean isTextSet() {
        return (setFlags & TEXT_SET) > 0;
    }

    public boolean isVerticalAlignmentSet() {
        return (setFlags & VERTICAL_ALIGNMENT_SET) > 0;
    }

    public boolean isVerticalTextPositionSet() {
        return (setFlags & VERTICAL_TEXT_POSITION_SET) > 0;
    }

    public boolean noPropertiesSet() {
        return setFlags == 0;
    }

    public void applyPropertiesTo(JLabel label) {
        if (noPropertiesSet()) {
            return;
        }
        if (isBackgroundSet()) {
            label.setBackground(getBackground());
        }
        if (isForegroundSet()) {
            label.setForeground(getForeground());
        }
        if (isFontSet()) {
            label.setFont(getFont());
        }
        if (isHorizontalAlignmentSet()) {
            label.setHorizontalAlignment(getHorizontalAlignment());
        }
        if (isHorizontalTextPositionSet()) {
            label.setHorizontalTextPosition(getHorizontalTextPosition());
        }
        if (isIconSet()) {
            label.setIcon(getIcon());
        }
        if (isIconTextGapSet()) {
            label.setIconTextGap(getIconTextGap());
        }
        if (isTextSet()) {
            label.setText(getText());
        }
        if (isVerticalAlignmentSet()) {
            label.setVerticalAlignment(getVerticalAlignment());
        }
        if (isVerticalTextPositionSet()) {
            label.setVerticalTextPosition(getVerticalTextPosition());
        }
    }

    public void applyPropertiesTo(AbstractButton button) {
         if (noPropertiesSet()) {
             return;
         }
         if (isBackgroundSet()) {
             button.setBackground(getBackground());
         }
         if (isForegroundSet()) {
             button.setForeground(getForeground());
         }
         if (isFontSet()) {
             button.setFont(getFont());
         }
         if (isHorizontalAlignmentSet()) {
             button.setHorizontalAlignment(getHorizontalAlignment());
         }
         if (isHorizontalTextPositionSet()) {
             button.setHorizontalTextPosition(getHorizontalTextPosition());
         }
         if (isIconSet()) {
             button.setIcon(getIcon());
         }
         if (isIconTextGapSet()) {
             button.setIconTextGap(getIconTextGap());
         }
         if (isTextSet()) {
             button.setText(getText());
         }
         if (isVerticalAlignmentSet()) {
             button.setVerticalAlignment(getVerticalAlignment());
         }
         if (isVerticalTextPositionSet()) {
             button.setVerticalTextPosition(getVerticalTextPosition());
         }
     }

     public void applyPropertiesTo(LabelProperties props) {
         if (noPropertiesSet()) {
             return;
         }
         if (isBackgroundSet()) {
             props.setBackground(getBackground());
         }
         if (isForegroundSet()) {
             props.setForeground(getForeground());
         }
         if (isFontSet()) {
             props.setFont(getFont());
         }
         if (isHorizontalAlignmentSet()) {
             props.setHorizontalAlignment(getHorizontalAlignment());
         }
         if (isHorizontalTextPositionSet()) {
             props.setHorizontalTextPosition(getHorizontalTextPosition());
         }
         if (isIconSet()) {
             props.setIcon(getIcon());
         }
         if (isIconTextGapSet()) {
             props.setIconTextGap(getIconTextGap());
         }
         if (isTextSet()) {
             props.setText(getText());
         }
         if (isVerticalAlignmentSet()) {
             props.setVerticalAlignment(getVerticalAlignment());
         }
         if (isVerticalTextPositionSet()) {
             props.setVerticalTextPosition(getVerticalTextPosition());
         }
     }

     public void applyPropertiesTo(TableCellRenderer renderer) {
         if (renderer instanceof JLabel) {
             applyPropertiesTo( (JLabel) renderer);
         }
         else if (renderer instanceof AbstractButton) {
             applyPropertiesTo( (AbstractButton) renderer);
         }
     }
}
