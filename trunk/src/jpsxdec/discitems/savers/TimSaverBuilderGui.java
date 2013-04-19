/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.tim.Tim;


/** GUI to select {@link TimSaverBuilder} options. */
class TimSaverBuilderGui extends DiscItemSaverBuilderGui<TimSaverBuilder> implements ChangeListener {

    private static final Logger LOG = Logger.getLogger(TimSaverBuilderGui.class.getName());

    private final JPanel _panelImages = new JPanel(new GridLayout());

    private final JPanel _topParagraphPanel = new JPanel(new ParagraphLayout());

    public TimSaverBuilderGui(TimSaverBuilder builder) {
        super(builder, new BorderLayout());
        setParagraphLayoutPanel(_topParagraphPanel);

        addListeners(this, 
                new FileNames(),
                new Format());

        updatePreviews();

        add(_topParagraphPanel, BorderLayout.NORTH);
        add(_panelImages, BorderLayout.CENTER);
    }

    private class FileNames implements ChangeListener {
        JTextArea __files = new JTextArea(3, 0);
        public FileNames() {
            Font f = UIManager.getFont("TextField.font");
            __files.setFont(f);
            __files.setEditable(false);
            __files.setWrapStyleWord(true);
            __files.setLineWrap(true);
            __files.setOpaque(false);
            Color c = __files.getBackground();
            __files.setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
            updateText();

            JScrollPane p = new JScrollPane(__files);
            p.setBorder(null);
            _topParagraphPanel.add(new JLabel("Output:"), ParagraphLayout.NEW_PARAGRAPH);
            _topParagraphPanel.add(p, ParagraphLayout.STRETCH_H);
        }
        public void stateChanged(ChangeEvent e) {
            updateText();
        }
        private void updateText() {
            __files.setText(_writerBuilder.getOutputFilesSummary());
        }
    }


    private void updatePreviews() {
        _panelImages.removeAll();
        GridLayout gl = (GridLayout) _panelImages.getLayout();

        try {
            // XXX: I don't like having to read from the disc until saving actually begins
            // or the user explicitly choses to preview the item
            Tim tim = _writerBuilder.readTim();
            int iPals = tim.getPaletteCount();

            double dblPalSqrt = Math.sqrt(iPals);
            gl.setRows((int)Math.floor(dblPalSqrt));
            gl.setColumns((int)Math.ceil(dblPalSqrt));

            for (int i = 0; i < iPals; i++) {
                _panelImages.add(new TimPaletteSelector(tim, i, _writerBuilder));
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error reading TIM preview", ex);
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            gl.setRows(1);
            gl.setColumns(1);
            JTextArea t = new JTextArea("Error reading TIM preview\n"+sw.toString());
            t.setLineWrap(true);
            _panelImages.add(new JScrollPane(t));
        }
    }

    public void stateChanged(ChangeEvent e) {
        for (Component c : _panelImages.getComponents()) {
            if (c instanceof TimPaletteSelector)
                ((TimPaletteSelector)c).stateChanged();
        }
    }

    @Override
    public boolean useSaverBuilder(DiscItemSaverBuilder saverBuilder) {
        boolean blnChanged = super.useSaverBuilder(saverBuilder);
        if (blnChanged) {
            updatePreviews();
        }
        return blnChanged;
    }

    private class Format extends AbstractCombo {

        public Format() {
            super("Format:");
        }

        @Override
        public int getSize() {
            return _writerBuilder.getImageFormat_listSize();
        }

        @Override
        public Object getElementAt(int index) {
            return _writerBuilder.getImageFormat_listItem(index);
        }

        @Override
        public void setSelectedItem(Object anItem) {
            _writerBuilder.setImageFormat((TimSaverBuilder.TimSaveFormat) anItem);
        }

        @Override
        public Object getSelectedItem() {
            return _writerBuilder.getImageFormat();
        }

        @Override
        protected boolean getEnabled() {return true; }
    }

}
