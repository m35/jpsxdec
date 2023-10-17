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

package jpsxdec.modules.spu;

import com.jhlabs.awt.ParagraphLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import jpsxdec.discitems.CombinedBuilderListener;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.ParagraphPanel;
import jpsxdec.i18n.I;
import jpsxdec.i18n.UnlocalizedMessage;

/** A very awesome GUI that reliably plays back the clip at the chosen sample rate. */
public class SpuSaverBuilderGui extends DiscItemSaverBuilderGui {

    @Nonnull
    private final CombinedBuilderListener<SpuSaverBuilder> _bl;
    @Nonnull
    private final PPanel _ppanel;
    @Nonnull
    private final JButton _btnPreviewStop;
    @Nonnull
    private final JCheckBox _chkAutoPlay;
    @Nonnull
    private final ButtonSetter _btnSetter;
    @CheckForNull
    private ItemPlayer _itemPlayer = null;

    public SpuSaverBuilderGui(@Nonnull SpuSaverBuilder sourceBldr) {
        super(new GridBagLayout());
        _bl = new CombinedBuilderListener<SpuSaverBuilder>(sourceBldr);
        _btnPreviewStop = new JButton("Preview"); // I18N
        _chkAutoPlay = new JCheckBox("Auto play"); // I18N
        _btnSetter = new ButtonSetter(_btnPreviewStop);
        _ppanel = new PPanel(_bl);
        initComponents();
    }
    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        add(_ppanel, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        ParagraphLayout pLayout = (ParagraphLayout) _ppanel.getLayout();
        int iMargin = pLayout.getHMargin();
        gbc.insets.left = iMargin;
        gbc.insets.right = iMargin;
        add(_btnPreviewStop, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weighty = 1.0;
        gbc.insets.left = iMargin;
        gbc.insets.right = iMargin;
        add(_chkAutoPlay, gbc);

        _btnPreviewStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                previewStopButtonClick(e);
            }
        });
    }

    private enum Action { PLAY, STOP, TOGGLE }

    @Override
    public boolean useSaverBuilder(@Nonnull DiscItemSaverBuilder saverBuilder) {
        ItemPlayer ip = _itemPlayer;
        if (ip != null) {
            ip.action(Action.STOP);
            _itemPlayer = null;
        }
        boolean blnIsMatch = _bl.changeSourceBuilder(saverBuilder);
        if (blnIsMatch && _chkAutoPlay.isSelected()) {
            _itemPlayer = new ItemPlayer(_btnSetter, _bl.getBuilder());
            _itemPlayer.action(Action.PLAY);
        }
        return blnIsMatch;
    }

    // .........................................................................
    // Playback

    private void previewStopButtonClick(ActionEvent e) {
        if (_itemPlayer == null)
            _itemPlayer = new ItemPlayer(_btnSetter, _bl.getBuilder());
        _itemPlayer.action(Action.TOGGLE);
    }

    private static class ItemPlayer {

        @Nonnull
        private final ButtonSetter _btnSetter;
        @Nonnull
        private final SpuSaverBuilder _builder;
        @CheckForNull
        private PlayInstance _playInstance;

        public ItemPlayer(@Nonnull ButtonSetter bs, @Nonnull SpuSaverBuilder builder) {
            _btnSetter = bs;
            _builder = builder;
        }

        public void play() {
            action(Action.PLAY);
        }
        public void stop() {
            action(Action.STOP);
        }
        public void toggle() {
            action(Action.TOGGLE);
        }

        private synchronized void action(@Nonnull Action act) {
            boolean blnWasPlaying = false;
            if (_playInstance != null) {
                blnWasPlaying = !_playInstance.isDone();
                _playInstance.stop();
                _playInstance = null;
            }
            if (act == Action.PLAY || act == Action.TOGGLE && !blnWasPlaying) {
                _playInstance = PlayInstance.startPlaying(_btnSetter, _builder);
            }
        }

        private static class PlayInstance implements Runnable {
            @Nonnull
            private final ButtonSetter _bs;
            @Nonnull
            private final SpuSaverBuilder _bldr;
            @CheckForNull
            private Clip _clip;
            private boolean _blnIsDone = false;

            private PlayInstance(@Nonnull ButtonSetter bs, @Nonnull SpuSaverBuilder bldr) {
                _bs = bs;
                _bldr = bldr;
            }

            public static PlayInstance startPlaying(@Nonnull ButtonSetter bs, @Nonnull SpuSaverBuilder bldr) {
                PlayInstance pi = new PlayInstance(bs, bldr);
                pi.openAndStart();
                Thread t = new Thread(pi, "Play" + bldr.getFileBaseName());
                t.start();
                return pi;
            }

            private void openAndStart() {
                try {
                    if (!_blnIsDone) {
                        double t1 = System.currentTimeMillis() / 1000.0;
                        _clip = AudioSystem.getClip();
                        double t2 = System.currentTimeMillis() / 1000.0;
                        if (t2 - t1 > 0.5)
                            System.out.format("%s.getClip() hung for %.2f", Thread.currentThread().getName(), t2 - t1).println();
                        _clip.open(_bldr.getAudioStream());
                        double t3 = System.currentTimeMillis() / 1000.0;
                        if (t3 - t2 > 0.5)
                            System.out.format("%s.open() hung for %.2f", Thread.currentThread().getName(), t3 - t2).println();
                        _clip.start();
                        double t4 = System.currentTimeMillis() / 1000.0;
                        if (t4 - t3 > 0.5)
                            System.out.format("%s.start() hung for %.2f", Thread.currentThread().getName(), t3 - t2).println();
                        _bs.action(Action.PLAY);
                        double t5 = System.currentTimeMillis() / 1000.0;
                    }
                } catch (LineUnavailableException | IOException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void run() {
                try {
                    while (!_clip.isRunning() && !_blnIsDone) {
                        Thread.sleep(100);
                    }
                    while (_clip.isRunning() && !_blnIsDone) {
                        Thread.sleep(100);
                    }
                    if (!_blnIsDone)
                        _bs.action(Action.STOP);
                    _blnIsDone = true;
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } finally {
                    if (_clip != null) {
                        double t1 = System.currentTimeMillis() / 1000.0;
                        _clip.stop();
                        double t2 = System.currentTimeMillis() / 1000.0;
                        if (t2 - t1 > 0.5)
                            System.out.format("%s.stop() hung for %.2f", Thread.currentThread().getName(), t2 - t1).println();
                        _clip.flush();
                        double t3 = System.currentTimeMillis() / 1000.0;
                        if (t3 - t2 > 0.5)
                            System.out.format("%s.flush() hung for %.2f", Thread.currentThread().getName(), t3 - t2).println();
                        _clip.close();
                        double t4 = System.currentTimeMillis() / 1000.0;
                        if (t4 - t3 > 0.5)
                            System.out.format("%s.close() hung for %.2f", Thread.currentThread().getName(), t4 - t3).println();
                    }
                }
            }

            public boolean isDone() {
                return _blnIsDone;
            }

            public void stop() {
                _blnIsDone = true;
                _bs.action(Action.STOP);
            }
        }

    }

    private static class ButtonSetter implements Runnable {
        @Nonnull
        private final JButton _btn;
        private boolean _blnShowPreview = true;

        public ButtonSetter(@Nonnull JButton btn) {
            _btn = btn;
        }

        public synchronized void action(@Nonnull Action act) {
            switch (act) {
                case PLAY:
                    _blnShowPreview = false;
                    break;
                case STOP:
                    _blnShowPreview = true;
                    break;
            }
            if (SwingUtilities.isEventDispatchThread()) {
                run();
            } else {
                SwingUtilities.invokeLater(this);
            }
        }

        @Override
        public void run() {
            if (_blnShowPreview) {
                _btn.setText("Preview"); // I18N
            } else {
                _btn.setText("Stop"); // I18N
            }
        }
    }

    // .........................................................................

    private static class PPanel extends ParagraphPanel {
        private final CombinedBuilderListener<SpuSaverBuilder> _bl;

        public PPanel(CombinedBuilderListener<SpuSaverBuilder> bl) {
            _bl = bl;
            _bl.addListeners(
                new AudioFormat(),
                new SampleRate()
            );
        }

        protected class AudioFormat extends AbstractComboExtension<SpuSaverBuilder.SpuSaverFormat> {
            public AudioFormat() {
                super(I.GUI_SAVE_AS_LABEL(), _bl.getBuilder().getFileBaseName());
            }
            @Override
            public int getSize() {
                return _bl.getBuilder().getContainerFormat_listSize();
            }
            @Override
            public SpuSaverBuilder.SpuSaverFormat getElementAt(int index) {
                return _bl.getBuilder().getContainerFormat_listItem(index);
            }
            @Override
            public void setSelectedItem(Object anItem) {
                _bl.getBuilder().setContainerForamt((SpuSaverBuilder.SpuSaverFormat) anItem);
            }
            @Override
            public Object getSelectedItem() {
                return _bl.getBuilder().getContainerFormat();
            }
            @Override
            public String getBaseName() {
                return _bl.getBuilder().getFileBaseName();
            }
        }

        private class SampleRate extends AbstractCombo<Integer> {

            public SampleRate() {
                super(new UnlocalizedMessage("Sample rate:"), false); // I18N
                __combo.setEditable(true);
            }

            @Override
            public int getSize() {
                return _bl.getBuilder().getSampleRate_listSize();
            }

            @Override
            public Integer getElementAt(int index) {
                return _bl.getBuilder().getSampleRate_listItem(index);
            }

            @Override
            public void setSelectedItem(Object anItem) {
                int iSampleRate = ((Number)anItem).intValue();
                _bl.getBuilder().setSampleRate(iSampleRate);
            }

            @Override
            public Object getSelectedItem() {
                return _bl.getBuilder().getSampleRate();
            }

            @Override protected boolean getEnabled() { return true; }
        }

    }

}
