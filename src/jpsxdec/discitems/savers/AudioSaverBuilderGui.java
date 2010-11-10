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

package jpsxdec.discitems.savers;

import com.jhlabs.awt.ParagraphLayout;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.formats.JavaAudioFormat;

public class AudioSaverBuilderGui extends DiscItemSaverBuilderGui {

    private AudioSaverBuilder _sourceBuilder;
    private final ChangeListener[] _aoControls;

    /** Use just 1 change listener to notify all the controls so it's
     * easier to swap out when the builder is changed. */
    private ChangeListener _listenerWrapper = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            for (ChangeListener control : _aoControls) {
                control.stateChanged(e);
            }
        }
    };

    public AudioSaverBuilderGui(AudioSaverBuilder builder) {
        super(new ParagraphLayout());
        _sourceBuilder = builder;

        _aoControls = new ChangeListener[] {
            new AudioFormat(),
            new Volume()
        };

        _sourceBuilder.addChangeListener(_listenerWrapper);
    }

    @Override
    public boolean useSaverBuilder(DiscItemSaverBuilder saverBuilder) {
        if (saverBuilder instanceof AudioSaverBuilder) {
            DiscItemSaverBuilder oldBuilder = _sourceBuilder;
            _sourceBuilder = (AudioSaverBuilder) saverBuilder;
            oldBuilder.removeChangeListener(_listenerWrapper);
            _sourceBuilder.addChangeListener(_listenerWrapper);
            _listenerWrapper.stateChanged(null);
            return true;
        } else {
            return false;
        }

        
    }

    protected class AudioFormat extends AbstractListModel implements ComboBoxModel, ChangeListener {
        Object __cur = getSelectedItem();
        JLabel __label;
        JLabel __name;
        JComboBox __combo;

        public AudioFormat() {
            __label = new JLabel("Saved as:");
            __name = new JLabel(_sourceBuilder.getFileBaseName());
            __combo = new JComboBox(this);
            add(__label, ParagraphLayout.NEW_PARAGRAPH);
            add(__name);
            add(__combo);
        }
        public void stateChanged(ChangeEvent e) {
            if (__cur != getSelectedItem()) {
                __cur = getSelectedItem();
                fireContentsChanged(this, 0, getSize());
            }
        }
        public int getSize() {
            return _sourceBuilder.getContainerFormat_listSize();
        }
        public Object getElementAt(int index) {
            return _sourceBuilder.getContainerFormat_listItem(index);
        }
        public void setSelectedItem(Object anItem) {
            _sourceBuilder.setContainerForamt((JavaAudioFormat) anItem);
        }
        public Object getSelectedItem() {
            return _sourceBuilder.getContainerFormat();
        }
    }

    private class Volume extends AbstractSlider {
        public Volume() { super("Volume:"); }
        public int getValue() {
            return (int) (_sourceBuilder.getVolume() * 100);
        }
        public void setValue(int n) {
            _sourceBuilder.setVolume(n / 100.);
        }
        protected boolean isEnabled() {
            return true;
        }
    }

    

}
