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

package jpsxdec.modules.tim;


import com.jhlabs.awt.ParagraphLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.discitems.CombinedBuilderListener;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.ParagraphPanel;
import jpsxdec.i18n.I;
import jpsxdec.tim.Tim;


/** GUI to select {@link TimSaverBuilder} options. */
class TimSaverBuilderGui extends DiscItemSaverBuilderGui implements ChangeListener {

    private static final Logger LOG = Logger.getLogger(TimSaverBuilderGui.class.getName());

    @Nonnull
    private final CombinedBuilderListener<TimSaverBuilder> _bl;

    private final JPanel _panelImages = new JPanel(new GridLayout());

    public TimSaverBuilderGui(@Nonnull TimSaverBuilder builder) {
        super(new BorderLayout());

        _bl = new CombinedBuilderListener<TimSaverBuilder>(builder);

        updatePreviews();

        add(new PPanel(_bl), BorderLayout.NORTH);
        add(_panelImages, BorderLayout.CENTER);
        _bl.addListeners(this);
    }

    @Override
    public boolean useSaverBuilder(@Nonnull DiscItemSaverBuilder saverBuilder) {
        boolean blnOk = _bl.changeSourceBuilder(saverBuilder);
        if (blnOk)
            updatePreviews();
        return blnOk;
    }

    @Override
    public void stateChanged(@CheckForNull ChangeEvent e) {
        for (Component c : _panelImages.getComponents()) {
            if (c instanceof TimPaletteSelector)
                ((TimPaletteSelector)c).stateChanged();
        }
    }

    private static class PPanel extends ParagraphPanel {

        @Nonnull
        private final CombinedBuilderListener<TimSaverBuilder> _bl;

        private PPanel(@Nonnull CombinedBuilderListener<TimSaverBuilder> bl) {
            _bl = bl;
            _bl.addListeners(
                new FileNames(),
                new Format()
            );
        }

        private class FileNames implements ChangeListener {
            final JTextArea __files = makeMultiLineJLabel(3);
            public FileNames() {
                updateText();

                JScrollPane p = new JScrollPane(__files);
                p.setBorder(null);
                add(new JLabel(I.GUI_SAVE_AS_LABEL().getLocalizedMessage()), ParagraphLayout.NEW_PARAGRAPH);
                add(p, ParagraphLayout.STRETCH_H);
            }
            @Override
            public void stateChanged(ChangeEvent e) {
                updateText();
            }
            private void updateText() {
                __files.setText(_bl.getBuilder().getOutputFilesSummary().getLocalizedMessage());
            }
        }

        private class Format extends AbstractCombo<TimSaverBuilder.TimSaveFormat> {
            public Format() {
                super(I.GUI_TIM_SAVE_FORMAT_LABEL(), true);
            }
            @Override
            public int getSize() {
                return _bl.getBuilder().getImageFormat_listSize();
            }
            @Override
            public TimSaverBuilder.TimSaveFormat getElementAt(int index) {
                return _bl.getBuilder().getImageFormat_listItem(index);
            }
            @Override
            public void setSelectedItem(Object anItem) {
                _bl.getBuilder().setImageFormat((TimSaverBuilder.TimSaveFormat) anItem);
            }
            @Override
            public Object getSelectedItem() {
                return _bl.getBuilder().getImageFormat();
            }
            @Override
            protected boolean getEnabled() {return true; }
        }
    }

    private void updatePreviews() {
        _panelImages.removeAll();
        GridLayout gl = (GridLayout) _panelImages.getLayout();

        try {
            // XXX: I don't like having to read from the disc until saving actually begins
            // or the user explicitly chooses to preview the item
            Tim tim = _bl.getBuilder().readTim();
            int iPals = tim.getPaletteCount();

            double dblPalSqrt = Math.sqrt(iPals);
            gl.setRows((int)Math.floor(dblPalSqrt));
            gl.setColumns((int)Math.ceil(dblPalSqrt));

            for (int i = 0; i < iPals; i++) {
                _panelImages.add(new TimPaletteSelector(tim, i, _bl.getBuilder()));
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error reading TIM preview", ex);
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            gl.setRows(1);
            gl.setColumns(1);
            JTextArea t = new JTextArea(I.GUI_TIM_ERR_READING_PREVIEW(sw.toString()).getLocalizedMessage());
            t.setLineWrap(true);
            _panelImages.add(new JScrollPane(t));
        }

    }

}
