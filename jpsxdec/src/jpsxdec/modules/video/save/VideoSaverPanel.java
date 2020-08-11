/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2020  Michael Sabin
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

package jpsxdec.modules.video.save;

import com.jhlabs.awt.ParagraphLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.File;
import javax.annotation.Nonnull;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.discitems.CombinedBuilderListener;
import jpsxdec.discitems.ParagraphPanel;
import jpsxdec.i18n.I;
import jpsxdec.psxvideo.mdec.ChromaUpsample;
import jpsxdec.util.Fraction;

/** Abstract {@link ParagraphPanel} shared among video
 * {@link jpsxdec.discitems.DiscItemSaverBuilder}s. */
public abstract class VideoSaverPanel<T extends VideoSaverBuilder> extends ParagraphPanel {

    /**{@link CombinedBuilderListener} accessible to child classes. */
    @Nonnull
    protected final CombinedBuilderListener<T> _bl;

    protected VideoSaverPanel(@Nonnull CombinedBuilderListener<T> bl) {
        _bl = bl;
        _bl.addListeners(
            new FileName(),
            new VideoFormat(),
            new Crop(),
            new DiscSpeed(),
            new DecodeQuality(),
            new ChromaUpsampling()
        );
    }

    private class VideoFormat extends AbstractCombo {
        public VideoFormat() { super(I.GUI_VIDEO_FORMAT_LABEL(), true); }
        public int getSize() {
            return _bl.getBuilder().getVideoFormat_listSize();
        }
        public Object getElementAt(int index) {
            return _bl.getBuilder().getVideoFormat_listItem(index);
        }
        public void setSelectedItem(Object anItem) {
            _bl.getBuilder().setVideoFormat((jpsxdec.modules.video.save.VideoFormat) anItem);
        }
        public Object getSelectedItem() {
            return _bl.getBuilder().getVideoFormat();
        }
        protected boolean getEnabled() { return true; }
    }

    private class DecodeQuality extends AbstractCombo {
        public DecodeQuality() { super(I.GUI_DECODE_QUALITY_LABEL(), true); }
        public int getSize() {
            return _bl.getBuilder().getDecodeQuality_listSize();
        }
        public Object getElementAt(int index) {
            return _bl.getBuilder().getDecodeQuality_listItem(index);
        }
        public void setSelectedItem(Object anItem) {
            _bl.getBuilder().setDecodeQuality((MdecDecodeQuality) anItem);
        }
        public Object getSelectedItem() {
            return _bl.getBuilder().getDecodeQuality();
        }
        protected boolean getEnabled() {
            return _bl.getBuilder().getDecodeQuality_enabled();
        }
    }


    private class ChromaUpsampling extends AbstractCombo {
        public ChromaUpsampling() { super(I.GUI_CHROMA_UPSAMPLING_LABEL(), true); }
        public int getSize() {
            return _bl.getBuilder().getChromaInterpolation_listSize();
        }
        public Object getElementAt(int index) {
            return _bl.getBuilder().getChromaInterpolation_listItem(index);
        }
        public void setSelectedItem(Object anItem) {
            _bl.getBuilder().setChromaInterpolation((ChromaUpsample) anItem);
        }
        public Object getSelectedItem() {
            return _bl.getBuilder().getChromaInterpolation();
        }
        protected boolean getEnabled() {
            return _bl.getBuilder().getChromaInterpolation_enabled();
        }
    }

    private class Crop extends ToggleButtonModel implements ChangeListener {
        final JCheckBox __chk = new JCheckBox(I.GUI_CROP_CHECKBOX().getLocalizedMessage());
        final JLabel __label = new JLabel(I.GUI_DIMENSIONS_LABEL().getLocalizedMessage());
        @Nonnull final JLabel __dims;
        boolean __cur = isSelected();
        public Crop() {
            __chk.setModel(null);
            __dims = new JLabel(I.GUI_DIMENSIONS_WIDTH_X_HEIGHT_LABEL(_bl.getBuilder().getWidth(), _bl.getBuilder().getHeight()).getLocalizedMessage());
            add(__label, ParagraphLayout.NEW_PARAGRAPH);
            add(__dims);
            add(__chk);
        }
        public void stateChanged(ChangeEvent e) {
            if (__chk.getModel() == null)
                __chk.setModel(this);
            if (isSelected() != __cur) {
                __cur = isSelected();
                fireStateChanged();
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                        isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
            }
            __dims.setText(I.GUI_DIMENSIONS_WIDTH_X_HEIGHT_LABEL(_bl.getBuilder().getWidth(), _bl.getBuilder().getHeight()).getLocalizedMessage());
        }
        public boolean isSelected() {
            return _bl.getBuilder().getCrop();
        }
        public void setSelected(boolean b) {
            _bl.getBuilder().setCrop(b);
        }
        public boolean isEnabled() {
            return _bl.getBuilder().getCrop_enabled();
        }
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

    private class FileName implements ChangeListener {
        final JLabel __label = new JLabel(I.GUI_SAVE_AS_LABEL().getLocalizedMessage());
        final JTextArea __files = makeMultiLineJLabel(2);
        public FileName() {
            add(__label, ParagraphLayout.NEW_PARAGRAPH);
            add(__files, ParagraphLayout.STRETCH_H);
        }
        public void stateChanged(ChangeEvent e) {
            updateEndings();
        }
        private void updateEndings() {
            File[] aoFiles = _bl.getBuilder().getOutputFileRange();
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


