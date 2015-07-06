/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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
import java.io.File;
import javax.annotation.Nonnull;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedMessage;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate.Upsampler;
import jpsxdec.util.Fraction;

/** Gui for {@link VideoSaverBuilder} options. */
public abstract class VideoSaverBuilderGui<T extends VideoSaverBuilder> extends DiscItemSaverBuilderGui<T> {

    private final JPanel _topPanel = new JPanel(new ParagraphLayout());

    public VideoSaverBuilderGui(@Nonnull T writerBuilder) {
        super(writerBuilder, new BorderLayout());
        setParagraphLayoutPanel(_topPanel);

        addListeners(
            new FileName(),
            new VideoFormat(),
            new Crop(),
            new DiscSpeed(),
            new DecodeQuality(),
            new ChromaUpsampling()
            //new Volume(),
        );

        add(_topPanel, BorderLayout.NORTH);
        
    }

    private class VideoFormat extends AbstractCombo {
        public VideoFormat() { super(I.GUI_VIDEO_FORMAT_LABEL()); }
        public int getSize() {
            return _writerBuilder.getVideoFormat_listSize();
        }
        public Object getElementAt(int index) {
            return _writerBuilder.getVideoFormat_listItem(index);
        }
        public void setSelectedItem(Object anItem) {
            _writerBuilder.setVideoFormat((jpsxdec.discitems.savers.VideoFormat) anItem);
        }
        public Object getSelectedItem() {
            return _writerBuilder.getVideoFormat();
        }
        protected boolean getEnabled() { return true; }
    }

    private class DecodeQuality extends AbstractCombo {
        public DecodeQuality() { super(I.GUI_DECODE_QUALITY_LABEL()); }
        public int getSize() {
            return _writerBuilder.getDecodeQuality_listSize();
        }
        public Object getElementAt(int index) {
            return _writerBuilder.getDecodeQuality_listItem(index);
        }
        public void setSelectedItem(Object anItem) {
            _writerBuilder.setDecodeQuality((MdecDecodeQuality) anItem);
        }
        public Object getSelectedItem() {
            return _writerBuilder.getDecodeQuality();
        }
        protected boolean getEnabled() {
            return _writerBuilder.getDecodeQuality_enabled();
        }
    }


    private class ChromaUpsampling extends AbstractCombo {
        public ChromaUpsampling() { super(I.GUI_CHROMA_UPSAMPLING_LABEL()); }
        public int getSize() {
            return _writerBuilder.getChromaInterpolation_listSize();
        }
        public Object getElementAt(int index) {
            return _writerBuilder.getChromaInterpolation_listItem(index);
        }
        public void setSelectedItem(Object anItem) {
            _writerBuilder.setChromaInterpolation((Upsampler) anItem);
        }
        public Object getSelectedItem() {
            return _writerBuilder.getChromaInterpolation();
        }
        protected boolean getEnabled() {
            return _writerBuilder.getChromaInterpolation_enabled();
        }
    }

    private class Volume extends AbstractSlider {
        public Volume() { super(I.GUI_AUDIO_VOLUME_LABEL()); }
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
        final JCheckBox __chk = new JCheckBox(I.GUI_CROP_CHECKBOX().getLocalizedMessage());
        final JLabel __label = new JLabel(I.GUI_DIMENSIONS_LABEL().getLocalizedMessage());
        @Nonnull final JLabel __dims;
        boolean __cur = isSelected();
        public Crop() {
            __chk.setModel(this);
            __dims = new JLabel(I.GUI_DIMENSIONS_WIDTH_X_HEIGHT_LABEL(_writerBuilder.getWidth(), _writerBuilder.getHeight()).getLocalizedMessage());
            _topPanel.add(__label, ParagraphLayout.NEW_PARAGRAPH);
            _topPanel.add(__dims);
            _topPanel.add(__chk);
        }
        public void stateChanged(ChangeEvent e) {
            if (isSelected() != __cur) {
                __cur = isSelected();
                fireStateChanged();
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                        isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
            }
            __dims.setText(I.GUI_DIMENSIONS_WIDTH_X_HEIGHT_LABEL(_writerBuilder.getWidth(), _writerBuilder.getHeight()).getLocalizedMessage());
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


    private abstract class AbstractDiscSpeed extends ToggleButtonModel implements ChangeListener {
        final JRadioButton __btn = new JRadioButton();
        boolean __prev = isSelected();
        public AbstractDiscSpeed(@Nonnull LocalizedMessage label) {
            __btn.setText(label.getLocalizedMessage());
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
        public DiscSpeed1x() { super(I.DISC_SPEED_1X()); }
        public boolean isSelected() {
            return _writerBuilder.getSingleSpeed();
        }
        public void setSelected(boolean b) {
            if (b) _writerBuilder.setSingleSpeed(b);
        }
    }
    private class DiscSpeed2x extends AbstractDiscSpeed {
        public DiscSpeed2x() { super(I.DISC_SPEED_2X()); }
        public boolean isSelected() {
            return !_writerBuilder.getSingleSpeed();
        }
        public void setSelected(boolean b) {
            if (b) _writerBuilder.setSingleSpeed(!b);
        }
    }

    private class DiscSpeed implements ChangeListener {
        final JLabel __label = new JLabel(I.GUI_DISC_SPEED_LABEL().getLocalizedMessage());
        final JLabel __fps = new JLabel();
        boolean __cur;
        @Nonnull final AbstractDiscSpeed __1x, __2x;
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
                __fps.setText(I.GUI_FPS_LABLE_WHOLE_NUMBER(fps.getNumerator() / fps.getDenominator()).getLocalizedMessage());
            else
                __fps.setText(I.GUI_FPS_LABEL_FRACTION(fps.asDouble(),
                              fps.getNumerator(), fps.getDenominator()).getLocalizedMessage());
        }
    }

    private class FileName implements ChangeListener {
        final JLabel __label = new JLabel(I.GUI_SAVE_AS_LABEL().getLocalizedMessage());
        final JTextArea __files = makeMultiLineJLabel(2);
        public FileName() {
            updateEndings();
            _topPanel.add(__label, ParagraphLayout.NEW_PARAGRAPH);
            _topPanel.add(__files, ParagraphLayout.STRETCH_H);
        }
        public void stateChanged(ChangeEvent e) {
            updateEndings();
        }
        private void updateEndings() {
            File[] aoFiles = _writerBuilder.getOutputFileRange();
            if (aoFiles.length == 1) {
                String sPath = aoFiles[0].toString();
                if (!sPath.equals(__files.getText()))
                    __files.setText(sPath);
            } else {
                String s = I.GUI_OUTPUT_VIDEO_FILE_RANGE(aoFiles[0], aoFiles[1]).getLocalizedMessage();
                if (!s.equals(__files.getText()))
                    __files.setText(s);
            }
        }
    }


}
