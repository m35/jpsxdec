/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.modules.audio.sectorbased;

import java.awt.BorderLayout;
import javax.annotation.Nonnull;
import jpsxdec.discitems.CombinedBuilderListener;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.ParagraphPanel;
import jpsxdec.formats.JavaAudioFormat;
import jpsxdec.i18n.I;

public class SectorBasedAudioSaverBuilderGui extends DiscItemSaverBuilderGui {

    @Nonnull
    private final CombinedBuilderListener<SectorBasedAudioSaverBuilder> _bl;

    public SectorBasedAudioSaverBuilderGui(@Nonnull SectorBasedAudioSaverBuilder saverBuilder) {
        super(new BorderLayout());
        _bl = new CombinedBuilderListener<SectorBasedAudioSaverBuilder>(saverBuilder);
        add(new PPanel(_bl), BorderLayout.NORTH);
    }

    @Override
    public boolean useSaverBuilder(@Nonnull DiscItemSaverBuilder saverBuilder) {
        return _bl.changeSourceBuilder(saverBuilder);
    }

    private static class PPanel extends ParagraphPanel {
        @Nonnull
        private final CombinedBuilderListener<SectorBasedAudioSaverBuilder> _bl;

        public PPanel(@Nonnull CombinedBuilderListener<SectorBasedAudioSaverBuilder> bl) {
            _bl = bl;
            _bl.addListeners(
                new AudioFormat()
            );
        }

        protected class AudioFormat extends AbstractComboExtension<JavaAudioFormat> {

            public AudioFormat() {
                super(I.GUI_SAVE_AS_LABEL(), _bl.getBuilder().getFileBaseName());
            }
            @Override
            public int getSize() {
                return _bl.getBuilder().getContainerFormat_listSize();
            }
            @Override
            public JavaAudioFormat getElementAt(int index) {
                return _bl.getBuilder().getContainerFormat_listItem(index);
            }
            @Override
            public void setSelectedItem(Object anItem) {
                _bl.getBuilder().setContainerFormat((JavaAudioFormat) anItem);
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

    }

}
