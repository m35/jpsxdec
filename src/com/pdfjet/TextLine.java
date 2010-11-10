/**
 *  TextLine.java
 *
Copyright (c) 2007, 2008, 2009, 2010 Innovatics Inc.

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
 
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and / or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.pdfjet;

import java.lang.*;
import java.text.*;
import java.util.*;


//>>>>pdfjet {
public class TextLine {

    protected double x = 0.0;
    protected double y = 0.0;

    protected Font font = null;
    protected String str = "";
    protected String uri = null;
    protected boolean underline = false;
    protected boolean strike = false;
    protected int degrees = 0;

    protected double[] color = {0.0, 0.0, 0.0};

    private double box_x = 0.0;
    private double box_y = 0.0;


    public TextLine(Font font) {
        this.font = font;
    }


    public TextLine(Font font, String str) {
        this.font = font;
        this.str = str;
    }


    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }


    public void setText(String str) {
        this.str = str;
    }


    public void setFont(Font font) {
        this.font = font;
    }


    public void setColor(double[] color) {
        this.color = color;
    }


    public void setColor(int[] rgb) {
        this.color = new double[] {rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0};
    }


    public String getText() {
        return str;
    }


    public double[] getColor() {
        return color;
    }


    public void setURIAction(String uri) {
        this.uri = uri;
    }


    public void setUnderline(boolean underline) {
        this.underline = underline;
    }


    public void setStrikeLine(boolean strike) {
        this.strike = strike;
    }


    public void setTextDirection(int degrees) {
        this.degrees = degrees;
    }


    public void placeIn(Box box) {
        box_x = box.x;
        box_y = box.y;
    }


    public void drawOn(Page page) throws Exception {
        drawOn(page, true);
    }


    public void drawOn(Page page, boolean draw) throws Exception {
        if (!draw) return;

        page.setTextDirection(degrees);
        x += box_x;
        y += box_y;
        if (uri != null) {
            page.annots.add(new Annotation(uri,
                    x,
                    page.height - (y - font.ascent),
                    x + font.stringWidth(str),
                    page.height - (y - font.descent)));
        }

        if (str != null) {
            page.setBrushColor(
                    color[0], color[1], color[2]);
            page.drawString(font, str, x, y);
        }

        if (underline) {
            page.setPenWidth(font.underlineThickness);
            page.setPenColor(color[0], color[1], color[2]);
            double lineLength = font.stringWidth(str);
            double radians = Math.PI * degrees / 180.0;
            double x_adjust = font.underlinePosition * Math.sin(radians);
            double y_adjust = font.underlinePosition * Math.cos(radians);
            double x2 = x + lineLength * Math.cos(radians);
            double y2 = y - lineLength * Math.sin(radians);
            page.moveTo(x + x_adjust, y + y_adjust);
            page.lineTo(x2 + x_adjust, y2 + y_adjust);
            page.strokePath();
        }

        if (strike) {
            page.setPenWidth(font.underlineThickness);
            page.setPenColor(color[0], color[1], color[2]);
            double lineLength = font.stringWidth(str);
            double radians = Math.PI * degrees / 180.0;
            double x_adjust = ( font.body_height / 4.0 ) * Math.sin(radians);
            double y_adjust = ( font.body_height / 4.0 ) * Math.cos(radians);
            double x2 = x + lineLength * Math.cos(radians);
            double y2 = y - lineLength * Math.sin(radians);
            page.moveTo(x - x_adjust, y - y_adjust);
            page.lineTo(x2 - x_adjust, y2 - y_adjust);
            page.strokePath();
        }

        page.setTextDirection(0);
    }

}   // End of TextLine.java
//<<<<}
