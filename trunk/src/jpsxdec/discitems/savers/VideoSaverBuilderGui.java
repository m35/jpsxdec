/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

package jpsxdec.discitems.savers;

import com.jhlabs.awt.ParagraphLayout;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import jpsxdec.SavingGuiTable;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.savers.VideoSaverBuilder.DecodeQualities;
import jpsxdec.util.Fraction;

/** Gui for {@link VideoSaverBuilder} options. */
public class VideoSaverBuilderGui extends DiscItemSaverBuilderGui<VideoSaverBuilder> {

    private JPanel _topPanel = new JPanel(new ParagraphLayout());

    public VideoSaverBuilderGui(VideoSaverBuilder writerBuilder) {
        super(writerBuilder, new BorderLayout());
        setParagraphLayoutPanel(_topPanel);

        addControls(
            new FileName(),
            new VideoFormat(),
            new Crop(),
            new DiscSpeed(),
            new DecodeQuality(),
            new JpgCompression(),
            new PreciseFps(),
            //new Volume(),
            new PreciseAv(),
            new ParallelAudio()
        );

        add(_topPanel, BorderLayout.NORTH);
        
    }

    private class VideoFormat extends AbstractCombo {
        public VideoFormat() { super("Video format"); }
        public int getSize() {
            return _writerBuilder.getVideoFormat_listSize();
        }
        public Object getElementAt(int index) {
            return _writerBuilder.getVideoFormat_listItem(index);
        }
        public void setSelectedItem(Object anItem) {
            _writerBuilder.setVideoFormat((VideoSaverBuilder.VideoFormat) anItem);
        }
        public Object getSelectedItem() {
            return _writerBuilder.getVideoFormat();
        }
        protected boolean getEnabled() { return true; }
    }

    private class DecodeQuality extends AbstractCombo {
        public DecodeQuality() { super("Decode quality:"); }
        public int getSize() {
            return _writerBuilder.getDecodeQuality_listSize();
        }
        public Object getElementAt(int index) {
            return _writerBuilder.getDecodeQuality_listItem(index);
        }
        public void setSelectedItem(Object anItem) {
            _writerBuilder.setDecodeQuality((DecodeQualities) anItem);
        }
        public Object getSelectedItem() {
            return _writerBuilder.getDecodeQuality();
        }
        protected boolean getEnabled() {
            return _writerBuilder.getDecodeQuality_enabled();
        }
    }


    private class JpgCompression extends AbstractSlider {
        public JpgCompression() { super("JPG compression quality:"); }
        public int getValue() {
            return (int) (_writerBuilder.getJpgCompression() * 100);
        }
        public void setValue(int n) {
            _writerBuilder.setJpgCompression(n / 100.f);
        }
        protected boolean isEnabled() {
            return _writerBuilder.getJpgCompression_enabled();
        }
    }

    private class Volume extends AbstractSlider {
        public Volume() { super("Audio volume:"); }
        public int getValue() {
            return (int) (_writerBuilder.getAudioVolume() * 100);
        }
        public void setValue(int n) {
            _writerBuilder.setAudioVolume(n / 100.f);
        }
        protected boolean isEnabled() {
            return _writerBuilder.getAudioVolume_enabled();
        }
    }


    private class Crop extends ToggleButtonModel implements ChangeListener {
        JCheckBox __chk = new JCheckBox("Crop");
        JLabel __label = new JLabel("Dimensions:");
        JLabel __dims;
        boolean __cur = isSelected();
        public Crop() {
            __chk.setModel(this);
            __dims = new JLabel(_writerBuilder.getWidth() + " x " + _writerBuilder.getHeight());
            _topPanel.add(__label, ParagraphLayout.NEW_PARAGRAPH);
            _topPanel.add(__dims);
            _topPanel.add(__chk);
        }
        public void stateChanged(ChangeEvent e) {
            if (isSelected() != __cur) {
                __cur = isSelected();
                __dims.setText(_writerBuilder.getWidth() + " x " + _writerBuilder.getHeight());
                fireStateChanged();
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                        isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
            }
        }
        public boolean isSelected() {
            return _writerBuilder.getCrop();
        }
        public void setSelected(boolean b) {
            _writerBuilder.setCrop(b);
        }
        public boolean isEnabled() {
            return _writerBuilder.getCrop_enabled();
        }
    }

    private class PreciseFps extends AbstractCheck {
        public PreciseFps() { super("Precise fps:"); }
        public boolean isSelected() {
            return _writerBuilder.getPreciseFrameTiming();
        }
        public void setSelected(boolean b) {
            _writerBuilder.setPreciseFrameTiming(b);
        }
        public boolean isEnabled() {
            return _writerBuilder.getPreciseFrameTiming_enabled();
        }
    }

    private class PreciseAv extends AbstractCheck {
        public PreciseAv() { super("Precise audio/video sync:"); }
        public boolean isSelected() {
            return _writerBuilder.getPreciseAVSync();
        }
        public void setSelected(boolean b) {
            _writerBuilder.setPreciseAVSync(b);
        }
        public boolean isEnabled() {
            return _writerBuilder.getPreciseAVSync_enabled();
        }
    }


    private abstract class AbstractDiscSpeed extends ToggleButtonModel implements ChangeListener {
        JRadioButton __btn = new JRadioButton();
        boolean __prev = isSelected();
        public AbstractDiscSpeed(String sLabel) {
            __btn.setText(sLabel);
            __btn.setModel(this);
            _topPanel.add(__btn);
        }
        public void stateChanged(ChangeEvent e) {
            if (isSelected() != __prev) {
                __prev = isSelected();
                fireStateChanged();
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                        __prev ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
            }
            if (__btn.isEnabled() != isEnabled())
                __btn.setEnabled(isEnabled());
        }
        public boolean isEnabled() {
            return _writerBuilder.getSingleSpeed_enabled();
        }
        abstract public boolean isSelected();
        abstract public void setSelected(boolean b);
    }
    private class DiscSpeed1x extends AbstractDiscSpeed {
        public DiscSpeed1x() { super("1x"); }
        public boolean isSelected() {
            return _writerBuilder.getSingleSpeed();
        }
        public void setSelected(boolean b) {
            if (b) _writerBuilder.setSingleSpeed(b);
        }
    }
    private class DiscSpeed2x extends AbstractDiscSpeed {
        public DiscSpeed2x() { super("2x"); }
        public boolean isSelected() {
            return !_writerBuilder.getSingleSpeed();
        }
        public void setSelected(boolean b) {
            if (b) _writerBuilder.setSingleSpeed(!b);
        }
    }

    private class DiscSpeed implements ChangeListener {
        JLabel __label = new JLabel("Disc speed:");
        JLabel __fps = new JLabel();
        boolean __cur;
        AbstractDiscSpeed __1x, __2x;
        public DiscSpeed() {
            __label.setEnabled(_writerBuilder.getSingleSpeed_enabled());
            __cur = _writerBuilder.getSingleSpeed();
            _topPanel.add(__label, ParagraphLayout.NEW_PARAGRAPH);
            __1x = new DiscSpeed1x();
            __2x = new DiscSpeed2x();
            updateFps();
            _topPanel.add(__fps);
        }
        public void stateChanged(ChangeEvent e) {
            updateFps();
            if (_writerBuilder.getSingleSpeed_enabled() != __label.isEnabled())
                __label.setEnabled(_writerBuilder.getSingleSpeed_enabled());
            __1x.stateChanged(e);
            __2x.stateChanged(e);
        }
        private void updateFps() {
            Fraction fps = _writerBuilder.getFps();
            if ((fps.getNumerator() % fps.getDenominator()) == 0)
                __fps.setText(String.format("%d fps", fps.getNumerator() / fps.getDenominator()));
            else
                __fps.setText(String.format("%s (%d/%d) fps",
                        DiscItemVideoStream.formatFps(fps),
                        fps.getNumerator(), fps.getDenominator()));
        }
    }

    private class FileName implements ChangeListener {
        JLabel __label = new JLabel("Saved as:");
        JLabel __postfix1 = new JLabel(" ");
        JLabel __postfix2 = new JLabel(" ");
        public FileName() {
            updateEndings();
            _topPanel.add(__label, ParagraphLayout.NEW_PARAGRAPH);
            _topPanel.add(__postfix1);
            _topPanel.add(__postfix2, ParagraphLayout.NEW_LINE);
        }
        public void stateChanged(ChangeEvent e) {
            updateEndings();
        }
        private void updateEndings() {
            String sStart = _writerBuilder.getOutputPostfixStart();
            String sEnd = _writerBuilder.getOutputPostfixEnd();
            if (sStart.equals(sEnd)) {
                if (!sStart.equals(__postfix1.getText()))
                    __postfix1.setText(_writerBuilder.getOutputBaseName() + sStart);
                if (!" ".equals(__postfix2.getText()))
                    __postfix2.setText(" ");
            } else {
                if (!sStart.equals(__postfix1.getText()))
                    __postfix1.setText(_writerBuilder.getOutputBaseName() + sStart);
                sEnd = "to: " + _writerBuilder.getOutputBaseName() + sEnd;
                if (!sEnd.equals(__postfix2.getText()))
                    __postfix2.setText(sEnd);
            }
        }
    }

    private enum COLUMNS {
        Save() {
            public Class type() { return Boolean.class; }
            public boolean editable() { return true; }
            public Object get(VideoSaverBuilder bldr, int i) {
                return bldr.getParallelAudio_selected(i);
            }
            public void set(VideoSaverBuilder bldr, int i, Object val) {
                bldr.setParallelAudio(i, (Boolean)val);
            }
        },
        Num() {
            public Class type() { return Integer.class; }
            public Object get(VideoSaverBuilder bldr, int i) {
                return bldr.getParallelAudio(i).getIndexId().getListIndex();
            }
            public String toString() { return "#"; }
        },
        Id() {
            public Class type() { return String.class; }
            public Object get(VideoSaverBuilder bldr, int i) {
                return bldr.getParallelAudio(i).getIndexId().getTopLevel();
            }
            public String toString() { return ""; }
        },
        Details() {
            public Class type() { return String.class; }
            public Object get(VideoSaverBuilder bldr, int i) {
                return bldr.getParallelAudio(i).getInterestingDescription();
            }
        };
        abstract public Class type();
        public boolean editable() { return false; }
        abstract public Object get(VideoSaverBuilder bldr, int i);
        public void set(VideoSaverBuilder bldr, int i, Object val) {}
    }
    private class ParallelAudio extends AbstractTableModel implements ChangeListener {

        JTable __tbl;

        public ParallelAudio() {
            __tbl = new JTable(this);
            __tbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            add(new JScrollPane(__tbl), BorderLayout.CENTER);
            SavingGuiTable.autoResizeColWidth(__tbl);
        }

        public int getRowCount() {
            return _writerBuilder.getParallelAudioCount();
        }

        public int getColumnCount() {
            return COLUMNS.values().length;
        }

        public String getColumnName(int columnIndex) {
            return COLUMNS.values()[columnIndex].toString();
        }

        public Class<?> getColumnClass(int columnIndex) {
            return COLUMNS.values()[columnIndex].type();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return COLUMNS.values()[columnIndex].editable();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return COLUMNS.values()[columnIndex].get(_writerBuilder, rowIndex);
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            COLUMNS.values()[columnIndex].set(_writerBuilder, rowIndex, aValue);
        }

        public void stateChanged(ChangeEvent e) {
            __tbl.setEnabled(_writerBuilder.getParallelAudio_enabled());
            this.fireTableDataChanged();
        }

    }

}
