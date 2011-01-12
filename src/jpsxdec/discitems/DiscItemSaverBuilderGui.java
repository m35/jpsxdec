/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.discitems;

import com.jhlabs.awt.ParagraphLayout;
import java.awt.LayoutManager;
import java.awt.event.ItemEvent;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public abstract class DiscItemSaverBuilderGui extends JPanel {

    public DiscItemSaverBuilderGui() {
    }

    public DiscItemSaverBuilderGui(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public DiscItemSaverBuilderGui(LayoutManager layout) {
        super(layout);
    }

    public DiscItemSaverBuilderGui(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    /** If the saver builder is compatible with this gui, returns true
     * and replaces the underlying builder with the supplied one,
     * otherwise returns false. */
    abstract public boolean useSaverBuilder(DiscItemSaverBuilder saverBuilder);


    protected abstract class AbstractSlider extends DefaultBoundedRangeModel implements ChangeListener {
        JLabel __label;
        JSlider __slider;
        JLabel __value;
        int __cur;
        public AbstractSlider(String sLabel) {
            __cur = getValue();
            __slider = new JSlider(this);
            __label = new JLabel(sLabel);
            __value = new JLabel(getValue() + "%");
            __label.setEnabled(isEnabled());
            __slider.setEnabled(isEnabled());
            __value.setEnabled(isEnabled());
            add(__label, ParagraphLayout.NEW_PARAGRAPH);
            add(__slider);
            add(__value);
        }
        public int getMaximum() { return 100; }
        public int getMinimum() { return 0; }
        public void stateChanged(ChangeEvent e) {
            if (getValue() != __cur) {
                __cur = getValue();
                __value.setText(__cur + "%");
                fireStateChanged();
            }
            if (__label.isEnabled() != isEnabled())
                __label.setEnabled(isEnabled());
            if (__slider.isEnabled() != isEnabled())
                __slider.setEnabled(isEnabled());
            if (__value.isEnabled() != isEnabled())
                __value.setEnabled(isEnabled());
        }
        abstract public int getValue();
        abstract public void setValue(int n);
        abstract protected boolean isEnabled();
    }

    protected abstract class AbstractCheck extends ToggleButtonModel implements ChangeListener {
        JCheckBox __chk = new JCheckBox();
        JLabel __label;
        boolean __cur = isSelected();
        public AbstractCheck(String sLabel) {
            __label = new JLabel(sLabel);
            __label.setEnabled(isEnabled());
            __chk.setModel(this);
            add(__label, ParagraphLayout.NEW_PARAGRAPH);
            add(__chk);
        }
        public void stateChanged(ChangeEvent e) {
            if (isSelected() != __cur) {
                __cur = isSelected();
                fireStateChanged();
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                        isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
            }
            if (__chk.isEnabled() != isEnabled())
                fireStateChanged();
            if (__label.isEnabled() != isEnabled())
                __label.setEnabled(isEnabled());
        }
        abstract public boolean isSelected();
        abstract public void setSelected(boolean b);
        abstract public boolean isEnabled();
    }


    protected abstract class AbstractCombo extends AbstractListModel implements ComboBoxModel, ChangeListener {
        Object __cur = getSelectedItem();
        JLabel __label;
        JComboBox __combo;

        public AbstractCombo(String sLabel) {
            __label = new JLabel(sLabel);
            __combo = new JComboBox(this);
            add(__label, ParagraphLayout.NEW_PARAGRAPH);
            add(__combo, ParagraphLayout.STRETCH_H);
        }
        public void stateChanged(ChangeEvent e) {
            if (__cur != getSelectedItem()) {
                __cur = getSelectedItem();
                fireContentsChanged(this, 0, getSize());
            }
            if (getEnabled() != __label.isEnabled())
                __label.setEnabled(getEnabled());
            if (getEnabled() != __combo.isEnabled())
                __combo.setEnabled(getEnabled());
        }
        abstract public int getSize();
        abstract public Object getElementAt(int index);
        abstract public void setSelectedItem(Object anItem);
        abstract public Object getSelectedItem();
        abstract protected boolean getEnabled();
    }
    
}
