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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import texter.RectFinder;


public class ImageRenderOptions {

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
    
    //<editor-fold defaultstate="collapsed" desc="StepSpinnerIntegerModel">
    public static class StepSpinnerIntegerModel extends SpinnerNumberModel
    {
        private final int stepSize;

        public StepSpinnerIntegerModel(int value, int minimum, int maximum, int stepSize) {
            super(value, minimum, maximum, stepSize);
            this.stepSize = stepSize;
        }

        @Override
        public void setValue(Object value) {
            if (!(value instanceof Integer)) 
                throw new IllegalArgumentException("illegal value");
            
            int ival = (Integer)value;
            int imin = (Integer)this.getMinimum();
            int imax = (Integer)this.getMaximum();
            
            if (ival % stepSize != 0)
                ival = ival - stepSize;
            
            if (ival < imin)
                ival = imin;
            else if (ival > imax)
                ival = imax;
            
            super.setValue(ival);
        }

    }//</editor-fold>

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // these are the models that actually hold the data, 
    // and are linked to the GUI widgets.
    public final StepSpinnerIntegerModel imageWidth ;
    public final StepSpinnerIntegerModel imageHeight;
    public final StepSpinnerIntegerModel initialX;
    public final StepSpinnerIntegerModel initialY;
    public final StepSpinnerIntegerModel shiftX;
    public final StepSpinnerIntegerModel shiftY;
    public final StepSpinnerIntegerModel lineHeight;
    public final StepSpinnerIntegerModel padding;


    ImageRenderOptions(RenderOptions ro, ImageRenderOptions iro) {
        imageWidth =  makeSpin(ro, iro.getImageWidth(), 32, 252, 4);
        imageHeight = makeSpin(ro, iro.getImageHeight(), 32, 200, 1);
        initialX =    makeSpin(ro, iro.getInitialX(), 0, 320, 1);
        initialY =    makeSpin(ro, iro.getInitialY(), 0, 240, 1);
        shiftX =      makeSpin(ro, iro.getShiftX(), 0, 320, 1);
        shiftY =      makeSpin(ro, iro.getShiftY(), 0, 240, 1);
        lineHeight =  makeSpin(ro, iro.getLineHeight(), 1, 240, 1);
        padding =     makeSpin(ro, iro.getPadding(), 0, 240, 1);
    }

    public ImageRenderOptions(RenderOptions ro) {
        imageWidth =  makeSpin(ro, 160, 32, 252, 4);
        imageHeight = makeSpin(ro, 120, 32, 200, 1);
        initialX =    makeSpin(ro, 0, 0, 320, 1);
        initialY =    makeSpin(ro, 0, 0, 240, 1);
        shiftX =      makeSpin(ro, 0, 0, 320, 1);
        shiftY =      makeSpin(ro, 0, 0, 240, 1);
        lineHeight =  makeSpin(ro, 10, 1, 240, 1);
        padding =     makeSpin(ro, 2, 0, 240, 1);
    }
    
    // quickly make a spinner model and add a listener
    public static StepSpinnerIntegerModel makeSpin(ChangeListener thiz, int iVal, int iMin, int iMax, int iStep) {
        StepSpinnerIntegerModel spin = new StepSpinnerIntegerModel(iMin, iMin, iMax, iStep);
        spin.setValue(iVal);
        spin.addChangeListener(thiz);
        return spin;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public int getInitialX() { return (Integer)initialX.getValue(); }
    public void setInitialX(int x) { initialX.setValue(x); }

    public int getInitialY() { return (Integer)initialY.getValue(); }
    public void setInitialY(int y) { initialY.setValue(y); }

    public int getShiftX() { return (Integer)shiftX.getValue(); }
    public void setShiftX(int x) { shiftX.setValue(x); }
    
    public int getShiftY() { return (Integer)shiftY.getValue(); }
    public void setShiftY(int y) { shiftY.setValue(y); }
    
    public int getLineHeight() { return (Integer)lineHeight.getValue(); }
    public void setLineHeight(int h) { lineHeight.setValue(h); }

    public int getImageWidth() { return (Integer)imageWidth.getValue(); }
    public void setImageWidth(int w) { imageWidth.setValue(w); }

    public int getImageHeight() { return (Integer)imageHeight.getValue(); }
    public void setImageHeight(int h) { imageHeight.setValue(h); }

    public int getPadding() { return (Integer)padding.getValue(); }
    public void setPadding(int pad) { padding.setValue(pad); }

    

    public static class Analysis {
        public BufferedImage preview;
        public BufferedImage save;
        public ArrayList<Rectangle> rects;

        public Analysis() {
            preview = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
            save = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        }

    }


    public Analysis analyze(BufferedImage mask) {
        Analysis anal = new Analysis();

        {
            Graphics2D g = anal.preview.createGraphics();
            drawWhiteArea(this, g, true);
            if (mask != null)
                g.drawImage(mask, 0, 0, null);
            g.dispose();
        }

        {
            Graphics2D g = anal.save.createGraphics();
            drawWhiteArea(this, g, false);
            g.dispose();
        }

        Rectangle sub = new Rectangle(calcCentered(getImageWidth(), getImageHeight()));
        sub.setSize(getImageWidth(), getImageHeight());
        anal.rects = RectFinder.findAvailableBoxes(
                getLineHeight(),
                getInitialX(),
                getInitialY(),
                getShiftX(),
                getShiftY(),
                anal.preview.getSubimage(sub.x, sub.y, sub.width, sub.height));
        for (Rectangle rectangle : anal.rects) {
            rectangle.translate(sub.x, sub.y);
        }

        return anal;
    }

    private static void drawWhiteArea(ImageRenderOptions ro, Graphics graphics, boolean bln) {
        BufferedImage biTxt = new BufferedImage(
                    ro.getImageWidth(), ro.getImageHeight(),
                    BufferedImage.TYPE_INT_RGB);

        Graphics2D g = biTxt.createGraphics();

        // fill it with white
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, ro.getImageWidth(), ro.getImageHeight());

        if (bln)
            // draw the cropped boarders
            addCroppedBorderMask(g, ro.getImageWidth(), ro.getImageHeight());

        if (bln)
            // add padding
            addPadding(g, ro.getImageWidth(), ro.getImageHeight(), ro.getPadding());

        g.dispose();

        Point p = calcCentered(ro.getImageWidth(), ro.getImageHeight());
        graphics.drawImage(biTxt, p.x, p.y, null);

    }

    public static Point calcCentered(int iWidth, int iHeight) {
        return new Point(162 - iWidth / 2,
                         119 - iHeight / 2);
    }

    private static final int CROP_TOP_EVEN = 3;
    private static final int CROP_TOP_ODD = 3;
    private static final int CROP_LEFT = 6;
    private static final int CROP_RIGHT = 12;
    private static final int CROP_BOTTOM = 1;

    private static void addCroppedBorderMask(Graphics2D g, int w, int h) {
        // draw the cropped borders
        int iCropTop;
        if (h % 2 == 0)
            iCropTop = CROP_TOP_EVEN;
        else
            iCropTop = CROP_TOP_ODD;

        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, CROP_LEFT, h);
        g.fillRect(0, 0, w, iCropTop);
        g.fillRect(w - CROP_RIGHT, 0, CROP_RIGHT, h);
        g.fillRect(0, h - CROP_BOTTOM, w, CROP_BOTTOM);
    }

    private static void addPadding(Graphics2D g, int w, int h, int pad) {

        if (pad < 1) return;

        int iCropTop;
        if (h % 2 == 0)
            iCropTop = CROP_TOP_EVEN;
        else
            iCropTop = CROP_TOP_ODD;

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(CROP_LEFT, iCropTop, w - (CROP_LEFT + CROP_RIGHT), pad);
        g.fillRect(CROP_LEFT, iCropTop, pad, h - (iCropTop + CROP_BOTTOM) );
        g.fillRect(CROP_LEFT, h - CROP_BOTTOM - pad, w - (CROP_LEFT + CROP_RIGHT), pad);
        g.fillRect(w - CROP_RIGHT - pad, iCropTop, pad, h - (iCropTop + CROP_BOTTOM));

    }

}
