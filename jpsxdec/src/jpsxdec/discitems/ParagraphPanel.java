/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ItemEvent;
import javax.annotation.Nonnull;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.i18n.ILocalizedMessage;

/** {@link JPanel} using {@link ParagraphLayout}. Contains a lot of useful
 * controls what will automatically add themselves to the panel in
 * proper paragraph format. */
public abstract class ParagraphPanel extends JPanel {

    public ParagraphPanel() {
        super(new ParagraphLayout());
    }

    protected abstract class AbstractCheck extends ToggleButtonModel implements ChangeListener {
        protected final JCheckBox __chk = new JCheckBox();
        @Nonnull protected final JLabel __label;
        boolean __cur = isSelected();
        public AbstractCheck(@Nonnull ILocalizedMessage label) {
            __label = new JLabel(label.getLocalizedMessage());
            add(__label, ParagraphLayout.NEW_PARAGRAPH);
            add(__chk);
            __chk.setModel(null);
        }
        @Override
        public void stateChanged(ChangeEvent e) {
            if (__chk.getModel() == null)
                __chk.setModel(this);
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
            boolean v = isVisible();
            __label.setVisible(v);
            __chk.setVisible(v);
        }
        @Override abstract public boolean isSelected();
        @Override abstract public void setSelected(boolean b);
        @Override abstract public boolean isEnabled();
        public boolean isVisible() { return true; }
    }


    protected abstract class AbstractCombo<T> extends AbstractListModel<T> implements ComboBoxModel<T>, ChangeListener {
        @Nonnull protected Object __cur = new Object();
        @Nonnull protected final JLabel __label;
        @Nonnull protected final JComboBox<T> __combo;

        public AbstractCombo(@Nonnull ILocalizedMessage label, boolean blnStretch) {
            __label = new JLabel(label.getLocalizedMessage());
            __combo = new JComboBox<T>(this);
            add(__label, ParagraphLayout.NEW_PARAGRAPH);
            if (blnStretch)
                add(__combo, ParagraphLayout.STRETCH_H);
            else
                add(__combo);
        }
        @Override
        public void stateChanged(@Nonnull ChangeEvent e) {
            Object o;
            if (!__cur.equals(o = getSelectedItem())) {
                __cur = o;
            }
            fireContentsChanged(this, 0, getSize());
            updateEnabled();
        }
        private void updateEnabled() {
            boolean bln = getEnabled();
            if (bln != __label.isEnabled())
                __label.setEnabled(bln);
            if (bln != __combo.isEnabled())
                __combo.setEnabled(bln);
        }
        @Override
        abstract public int getSize();
        @Override
        abstract public T getElementAt(int index);
        @Override
        abstract public void setSelectedItem(Object anItem);
        @Override
        abstract public Object getSelectedItem();
        abstract protected boolean getEnabled();
    }

    protected abstract class AbstractComboExtension<T> extends AbstractListModel<T> implements ComboBoxModel<T>, ChangeListener {
        @Nonnull protected Object __cur;
        @Nonnull protected final JLabel __label;
        @Nonnull protected final JLabel __name;
        @Nonnull protected final JComboBox<T> __combo;

        public AbstractComboExtension(@Nonnull ILocalizedMessage label, @Nonnull String baseName) {
            __label = new JLabel(label.getLocalizedMessage());
            __name = new JLabel(baseName);
            __combo = new JComboBox<T>(this);
            add(__label, ParagraphLayout.NEW_PARAGRAPH);
            add(__name);
            add(__combo);
        }
        @Override
        public void stateChanged(ChangeEvent e) {
            if (__cur != getSelectedItem()) {
                __cur = getSelectedItem();
                fireContentsChanged(this, 0, getSize());
            }
            __name.setText(getBaseName());
        }
        @Override
        abstract public int getSize();
        @Override
        abstract public T getElementAt(int index);
        @Override
        abstract public void setSelectedItem(Object anItem);
        @Override
        abstract public Object getSelectedItem();
        abstract public String getBaseName();
    }

    protected static @Nonnull JTextArea makeMultiLineJLabel(int iVisibleLineHeight) {
        JTextArea txtArea = new JTextArea(iVisibleLineHeight, 0);
        Font f = UIManager.getFont("TextField.font");
        txtArea.setFont(f);
        txtArea.setEditable(false);
        txtArea.setWrapStyleWord(true);
        txtArea.setLineWrap(true);
        txtArea.setOpaque(false);
        Color c = txtArea.getBackground();
        txtArea.setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
        return txtArea;
    }
}
