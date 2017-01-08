/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2017  Michael Sabin
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.DiscItemSpu;
import jpsxdec.i18n.I;
import jpsxdec.i18n.UnlocalizedMessage;

public class SpuSaverBuilderGui extends DiscItemSaverBuilderGui
        implements ActionListener, LineListener, Runnable
{

    public static SpuSaverBuilderGui make(@Nonnull SpuSaverBuilder sourceBldr) {
        SpuSaverBuilderGui g = new SpuSaverBuilderGui(sourceBldr);
        g.initComponents();
        return g;
    }

    private final CombinedBuilderListener<SpuSaverBuilder> _bl;
    private final PPanel _ppanel;
    private final JButton _btnPreviewStop;
    private Clip _clip;
    private boolean _blnPlaying = false;

    private SpuSaverBuilderGui(@Nonnull SpuSaverBuilder sourceBldr) {
        super(new GridBagLayout());
        _bl = new CombinedBuilderListener<SpuSaverBuilder>(sourceBldr);
        _ppanel = new PPanel(_bl);
        _btnPreviewStop = new JButton("Preview"); // I18N
    }
    private void initComponents() {
        _btnPreviewStop.addActionListener(this);
        
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
        gbc.weighty = 1.0;
        ParagraphLayout pLayout = (ParagraphLayout) _ppanel.getLayout();
        int iMargin = pLayout.getHMargin();
        gbc.insets.left = iMargin;
        gbc.insets.right = iMargin;
        add(_btnPreviewStop, gbc);
    }

    @Override
    public boolean useSaverBuilder(@Nonnull DiscItemSaverBuilder saverBuilder) {
        // TODO: figure out why this hangs sometimes when stopping early
        stop();
        return _bl.changeSourceBuilder(saverBuilder);
    }

    // .........................................................................
    // Playback

    /** Play/stop button pressed. */
    public void actionPerformed(ActionEvent e) {
        if (_blnPlaying) {
            stop();
        } else {
            play();
        }
    }

    public void play() {
        try {
            _blnPlaying = true;
            // TODO? disable other components, or maybe show popup?
            _clip = AudioSystem.getClip();
            _clip.open(_bl.getBuilder().getStream());
            _clip.addLineListener(this);
            _clip.start();
            _btnPreviewStop.setText("Stop"); // I18N
        } catch (Exception ex) {
            Logger.getLogger(DiscItemSpu.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Listen for end of audio playback. */
    public void update(LineEvent event) {
        if (LineEvent.Type.STOP.equals(event.getType())) {
            stop();
        }
    }

    public void stop() {
        Clip c = _clip;
        if (c != null)
            c.close();
        _blnPlaying = false;
        if (SwingUtilities.isEventDispatchThread()) {
            run();
        } else {
            try {
                SwingUtilities.invokeAndWait(this);
            } catch (Exception ex) {
                Logger.getLogger(DiscItemSpu.class.getName()).log(Level.SEVERE,
                                                                  null,
                                                                  ex);
            }
        }
    }

    /** Updates the play button at the end of playback.
     * Always run in the GUI thread. */
    public void run() {
        _btnPreviewStop.setText("Preview"); // I18N
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

        protected class AudioFormat extends AbstractComboExtension {

            public AudioFormat() {
                super(I.GUI_SAVE_AS_LABEL(), _bl.getBuilder().getFileBaseName());
            }
            public int getSize() {
                return _bl.getBuilder().getContainerFormat_listSize();
            }
            public Object getElementAt(int index) {
                return _bl.getBuilder().getContainerFormat_listItem(index);
            }
            public void setSelectedItem(Object anItem) {
                _bl.getBuilder().setContainerForamt((SpuSaverBuilder.SpuSaverFormat) anItem);
            }
            public Object getSelectedItem() {
                return _bl.getBuilder().getContainerFormat();
            }
        }

        private class SampleRate extends AbstractCombo {

            public SampleRate() {
                super(new UnlocalizedMessage("Sample rate:"), false); // I18N
                __combo.setEditable(true);
            }

            @Override
            public int getSize() {
                return _bl.getBuilder().getSampleRate_listSize();
            }

            @Override
            public Object getElementAt(int index) {
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

            @Override
            protected boolean getEnabled() {
                return true;
            }
        }

    }

}
