/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2023  Michael Sabin
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

package jpsxdec.modules.video.sectorbased;

import com.jhlabs.awt.ParagraphLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.annotation.Nonnull;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import jpsxdec.discitems.CombinedBuilderListener;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.gui.SavingGuiTable;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.modules.video.save.VideoSaverPanel;
import jpsxdec.util.Fraction;


public class SectorBasedVideoSaverBuilderGui extends DiscItemSaverBuilderGui {

    @Nonnull
    private final CombinedBuilderListener<SectorBasedVideoSaverBuilder> _bh;

    public SectorBasedVideoSaverBuilderGui(@Nonnull SectorBasedVideoSaverBuilder saverBuilder) {
        super(new BorderLayout());
        _bh = new CombinedBuilderListener<SectorBasedVideoSaverBuilder>(saverBuilder);
        add(new PPanel(_bh), BorderLayout.NORTH);
        _bh.addListeners(new ParallelAudio());
    }

    @Override
    public boolean useSaverBuilder(@Nonnull DiscItemSaverBuilder saverBuilder) {
        return _bh.changeSourceBuilder(saverBuilder);
    }

    private static class PPanel extends VideoSaverPanel<SectorBasedVideoSaverBuilder> {

        public PPanel(@Nonnull CombinedBuilderListener<SectorBasedVideoSaverBuilder> bh) {
            super(bh);
            _bl.addListeners(new DiscSpeed(), new EmulateAv());
        }

        private class DiscSpeed implements ChangeListener, ActionListener {
            final ButtonGroup __grp = new ButtonGroup();
            final JLabel __label = new JLabel(I.GUI_DISC_SPEED_LABEL().getLocalizedMessage());
            final JLabel __fps = new JLabel();
            boolean __cur;
            final JRadioButton __1x = new JRadioButton(I.DISC_SPEED_1X().getLocalizedMessage()),
                               __2x = new JRadioButton(I.DISC_SPEED_2X().getLocalizedMessage());
            public DiscSpeed() {
                add(__label, ParagraphLayout.NEW_PARAGRAPH);
                add(__1x);
                add(__2x);
                __grp.add(__1x);
                __grp.add(__2x);
                __1x.addActionListener(this);
                __2x.addActionListener(this);
                add(__fps);
            }
            public void stateChanged(ChangeEvent e) {
                updateFps();
                boolean blnEnabled = _bl.getBuilder().getSingleSpeed_enabled();
                __label.setEnabled(blnEnabled);
                __1x.setEnabled(blnEnabled);
                __2x.setEnabled(blnEnabled);
                if (_bl.getBuilder().getSingleSpeed())
                    __grp.setSelected(__1x.getModel(), true);
                else
                    __grp.setSelected(__2x.getModel(), true);
            }
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == __1x)
                    _bl.getBuilder().setSingleSpeed(true);
                else
                    _bl.getBuilder().setSingleSpeed(false);
            }
            private void updateFps() {
                Fraction fps = _bl.getBuilder().getFps();
                if ((fps.getNumerator() % fps.getDenominator()) == 0)
                    __fps.setText(I.GUI_FPS_LABLE_WHOLE_NUMBER(fps.getNumerator() / fps.getDenominator()).getLocalizedMessage());
                else
                    __fps.setText(I.GUI_FPS_LABEL_FRACTION(fps.asDouble(),
                                  fps.getNumerator(), fps.getDenominator()).getLocalizedMessage());
            }
        }

        private class EmulateAv extends AbstractCheck {
            public EmulateAv() { super(I.GUI_EMULATE_PSX_AV_SYNC_LABEL()); }
            public boolean isSelected() {
                return _bl.getBuilder().getEmulatePsxAvSync();
            }
            public void setSelected(boolean b) {
                _bl.getBuilder().setEmulatePsxAVSync(b);
            }
            public boolean isEnabled() {
                return _bl.getBuilder().getEmulatePsxAVSync_enabled();
            }
        }
    }

    private enum COLUMNS {
        Save(Boolean.class, I.GUI_TREE_SAVE_COLUMN()) {
            public boolean editable() { return true; }
            public Object get(SectorBasedVideoSaverBuilder bldr, int i) {
                return bldr.getParallelAudio_selected(i);
            }
            public void set(SectorBasedVideoSaverBuilder bldr, int i, Object val) {
                bldr.setParallelAudio(i, (Boolean)val);
            }
        },
        Num(Integer.class, I.GUI_TREE_INDEX_NUMBER_COLUMN()) {
            public Object get(SectorBasedVideoSaverBuilder bldr, int i) {
                return bldr.getParallelAudio(i).getIndex();
            }
        },
        Id(String.class, I.GUI_VID_AUDIO_SAVE_ID_COLUMN()) {
            public Object get(SectorBasedVideoSaverBuilder bldr, int i) {
                return bldr.getParallelAudio(i).getIndexId().getTopLevel();
            }
        },
        Details(String.class, I.GUI_TREE_DETAILS_COLUMN()) {
            public Object get(SectorBasedVideoSaverBuilder bldr, int i) {
                return bldr.getParallelAudio(i).getInterestingDescription();
            }
        };

        public boolean editable() { return false; }
        abstract public Object get(SectorBasedVideoSaverBuilder bldr, int i);
        public void set(SectorBasedVideoSaverBuilder bldr, int i, Object val) {}

        @Nonnull
        private final Class<?> _type;
        @Nonnull
        private final ILocalizedMessage _name;

        private COLUMNS(@Nonnull Class<?> type, @Nonnull ILocalizedMessage name) {
            _type = type;
            _name = name;
        }

        final public Class<?> type() { return _type; }
        @Override
        final public String toString() { return _name.getLocalizedMessage(); }
    }

    private class ParallelAudio extends AbstractTableModel implements ChangeListener {

        @Nonnull final JTable __tbl;

        public ParallelAudio() {
            __tbl = new JTable(this);
            __tbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            add(new JScrollPane(__tbl), BorderLayout.CENTER);
            SavingGuiTable.autoResizeColWidth(__tbl); // TODO: bad dependency
        }

        @Override
        public int getRowCount() {
            return _bh.getBuilder().getParallelAudioCount();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.values().length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMNS.values()[columnIndex].toString();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return COLUMNS.values()[columnIndex].type();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return COLUMNS.values()[columnIndex].editable();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return COLUMNS.values()[columnIndex].get(_bh.getBuilder(), rowIndex);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            COLUMNS.values()[columnIndex].set(_bh.getBuilder(), rowIndex, aValue);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            __tbl.setEnabled(_bh.getBuilder().getParallelAudio_enabled());
            this.fireTableDataChanged();
        }

    }
}
