/*
 * LainTools: PSX Serial Experiments Lain Hacking and Translation Tools
 * Copyright (C) 2011  Michael Sabin
 *
 * Redistribution and use of the LainTools code or any derivative works are
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

package texter.models;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import org.ini4j.Ini;
import org.ini4j.OptionMap;
import org.ini4j.Profile.Section;
import texter.TextFormatter;
import texter.TextFormatter.AttributedStringIterator;
import texter.TextRenderer;
import texter.models.ImageRenderOptions.Analysis;
import texter.models.ImageRenderOptions.StepSpinnerIntegerModel;


public class RenderOptions implements ChangeListener, DocumentListener {

    //<editor-fold defaultstate="collapsed" desc="EasyDocument">
    public static class EasyDocument extends PlainDocument { 

        public EasyDocument() { }
        public EasyDocument(String text) {
            setText(text);
        }
        
        public String getText() {
            try {
                return this.getText(0, this.getLength());
            } catch (BadLocationException ex) {
                return null;
            }
        }
        
        public void setText(String s) {
            if (s == null) s = "";
            try {
                this.replace(0, this.getLength(), s, null);
            } catch (BadLocationException ex) {
                
            }
        }
    }//</editor-fold>
    
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // hold the original ini information, and use it when saving the ini again
    // (in case the ini has additional sections/values that we don't want to destroy)
    private Ini _sourceIni;
    private OptionMap _section;

    private ImageRenderOptions[] _options = new ImageRenderOptions[3];
    
    // flag if the data has changed since loaded/created
    private boolean _blnDirty = false;

    private boolean _blnDoesFit = true;

    private TextFormatter _nameColorMap;

    public final StepSpinnerIntegerModel fontSize;
    
    public final ComboBoxModel overlayMask;
    public final EasyDocument text;
    public final EasyDocument fontName;
    public final EasyDocument colorMap;

    public final String mediaName;
    private final String _sSites;

    private final PropertyChangeSupport _prop = new PropertyChangeSupport(this);

    public String toString() {
        StringBuilder sb = new StringBuilder(mediaName);
        if (!_blnDoesFit)
            sb.append('!');
        if (_blnDirty)
            sb.append('*');
        return sb.toString();
    }

    public RenderOptions(File dir, String sId, String sSite) throws IOException {
        this(sId, sSite);
        try {
            load(dir);
        } catch (FileNotFoundException ex) {
            System.out.println(sId + " file not found. Using default values.");
        }
    }

    public static class MaskImage {
        public final BufferedImage image;
        public final String name;

        public MaskImage(String name) throws IOException {
            this.name = name;
            image = ImageIO.read(new File(name));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass())
                return false;
            final MaskImage other = (MaskImage) obj;
            if (!Misc.objectEquals(this.name, other.name))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
            return hash;
        }

        public String toString() { return name; }
    }
    public static final MaskImage[] MASKS;
    static {
        try {
            MASKS = new MaskImage[]{new MaskImage("Mask1.png"), new MaskImage("Mask2.png")};
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public class MaskCombo extends DefaultComboBoxModel {
        public MaskCombo() {
            super(MASKS);
        }
        @Override
        public void setSelectedItem(Object anObject) {
            super.setSelectedItem(anObject);
            dirty(true);
        }
    }

    public RenderOptions(String sId, String sSite) {
        fontSize =    ImageRenderOptions.makeSpin(this, 11, 6, 50, 1);
        
        overlayMask = new MaskCombo();
        text =        makeDoc("Text");
        fontName =    makeDoc("Arial Narrow");
        colorMap =    makeDoc("colors.ini");
        mediaName =   sId;
        _sSites = sSite;

        _options[0] = new ImageRenderOptions(this);
        try {
            _nameColorMap = new TextFormatter(getColorMap());
        } catch (IOException ex) {
            Logger.getLogger(RenderOptions.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // quickly make a document model and add the listener
    private EasyDocument makeDoc(String sTxt) {
        EasyDocument doc = new EasyDocument(sTxt);
        doc.addDocumentListener(this);
        return doc;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public void load(File dir) throws IOException {

        File f = new File(dir, mediaName + ".ini");

        String s = new String(IO.readFile(f));
        String[] as = s.split("\\s*\\[Text\\]\\s*");
        text(as[1].replaceAll("\r", ""));
        _sourceIni = new Ini(new StringReader(as[0]));

        _section = _sourceIni.get("General");
        if (_section == null) throw new IOException("Section General not found in " + f);
        _section.to(this);

        for (int i = 0; i < 3; i++) {
            Section section = _sourceIni.get("Image" + i);
            if (section == null) {
                if (i == 0)
                    throw new IOException("Section Image"+i+" not found in " + f);
                continue;
            }
            ImageRenderOptions opt = new ImageRenderOptions(this);
            section.to(opt);
            _options[i] = opt;
        }

        _nameColorMap = new TextFormatter(getColorMap());

        render(false, true);

        dirty(false);
    }
    
    public void addImageOption() {
        ImageRenderOptions iro = _options[0];
        for (int i = 1; i < _options.length; i++) {
            if (_options[i] == null) {
                _options[i] = new ImageRenderOptions(this, iro);
                dirty(true);
                break;
            } else {
                iro = _options[i];
            }
        }
    }

    public void removeImageOption() {
        if (_options[2] != null) {
            _options[2] = null;
            dirty(true);
        } else if (_options[1] != null) {
            _options[1] = null;
            dirty(true);
        }
    }

    public void save(File dir) throws IOException {
        if (!_blnDirty) return;
        
        if (_sourceIni == null) {
            _sourceIni = new Ini();
            _section = _sourceIni.add("General");
        }
        
        _section.from(this);
        for (int i = 0; i < _options.length; i++) {
            if (_options[i] != null) {
                Section section = _sourceIni.get("Image"+i);
                if (section == null)
                    section = _sourceIni.add("Image"+i);
                section.from(_options[i]);
            }
        }
        FileOutputStream fos = new FileOutputStream(new File(dir, mediaName + ".ini"));
        _sourceIni.store(fos);
        PrintStream ps = new PrintStream(fos);
        ps.println("[Text]");
        ps.print(text().replaceAll("\n", "\r\n"));
        ps.close();
        
        dirty(false);
        
    }
    
    public BufferedImage renderTest() {
        return render(true, false);
    }

    public ImageRenderOptions getImgOpt(int i) {
        return _options[i];
    }

    public BufferedImage render(boolean blnPreview, boolean blnFitCheck) {
        BufferedImage render = new BufferedImage(320, 240*3, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = render.createGraphics();

        AttributedStringIterator[] formattedTxt = _nameColorMap.getIterator(text(), makeFont(), g);

        Analysis anal = null;
        int iHeight = 0;
        int iCurFmt = 0;
        for (int i = 0; i < _options.length && iCurFmt < formattedTxt.length; i++, iHeight += 240) {
            if (_options[i] != null)
                anal = _options[i].analyze(overlayImage());

            Graphics2D subG = (Graphics2D) g.create(0, iHeight, 320, 240);
            if (!blnFitCheck)
                subG.drawImage(blnPreview ? anal.preview : anal.save, 0, 0, null);
            TextRenderer.renderText(subG, anal.rects, formattedTxt[iCurFmt], !blnFitCheck, false);
            if (!formattedTxt[iCurFmt].hasMore())
                iCurFmt++;
            subG.dispose();
        }
        
        fits(iCurFmt >= formattedTxt.length || !formattedTxt[iCurFmt].hasMore());

        if (iHeight == render.getHeight())
            return render;
        else
            return render.getSubimage(0, 0, render.getWidth(), iHeight);
    }

    
    public void renderSave(File dir) throws IOException {
        BufferedImage bi = render(false, false);
        ImageRenderOptions iro = null;
        for (int i=0, iTop = 0; iTop < bi.getHeight(); i++, iTop+=240) {
            if (_options[i] != null)
                iro = _options[i];
            int w = iro.getImageWidth();
            int h = iro.getImageHeight();
            Point p = ImageRenderOptions.calcCentered(w, h);
            File f = new File(dir, mediaName + "-" + i + ".png");
            boolean bln = ImageIO.write(bi.getSubimage(p.x, p.y+iTop, w, h), "png", f);
            if (!bln)
                System.err.println("Failed to save " + f);
            System.out.println("Saved " + f);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    
    // if any of the render options change, set the dirty flag
    public void stateChanged(ChangeEvent e) {
        dirty(true);
    }
    public void insertUpdate(DocumentEvent e) {
        dirty(true);
    }
    public void removeUpdate(DocumentEvent e) {
        dirty(true);
    }
    public void changedUpdate(DocumentEvent e) {
        dirty(true);
    }

    public String sites() {
        return _sSites;
    }
    
    public boolean dirty() {
        return _blnDirty;
    }

    public void dirty(boolean bln) {
        if (_blnDirty == bln)
            return;
        _blnDirty = bln;
        _prop.firePropertyChange("toString", !_blnDirty, _blnDirty);
    }

    private void fits(boolean bln) {
        if (_blnDoesFit == bln)
            return;
        _blnDoesFit = bln;
        _prop.firePropertyChange("toString", !_blnDoesFit, _blnDoesFit);
    }

    public boolean doesFit() {
        return _blnDoesFit;
    }

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        _prop.removePropertyChangeListener(propertyName, listener);
    }

    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        _prop.addPropertyChangeListener(propertyName, listener);
    }

    

    ////////////////////////////////////////////////////////////////////////////
    
    public String getFontName() { return fontName.getText(); }
    public void setFontName(String name) { fontName.setText(name); }

    public Font makeFont() {
        return new Font(getFontName(), 0, getFontSize());
    }
    public BufferedImage overlayImage() {
        return ((MaskImage)overlayMask.getSelectedItem()).image;
    }

    public int getFontSize() { return (Integer)fontSize.getValue(); }
    public void setFontSize(int size) { 
        fontSize.setValue(size);
    }

    public String text() { return text.getText(); }
    public void text(String s) { text.setText(s); }

    public String getOverlayMask() { return ((MaskImage)overlayMask.getSelectedItem()).name;
    }
    public void setOverlayMask(String s) {
        MaskImage mi = MASKS[0];
        for (MaskImage maskImage : MASKS) {
            if (maskImage.name.equals(s)) {
                mi = maskImage;
                break;
            }
        }
        overlayMask.setSelectedItem(mi);
    }

    public String getColorMap() { return colorMap.getText(); }
    public void setColorMap(String map) { colorMap.setText(map); }

}
