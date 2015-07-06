/**
 *  Cell.java
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
import java.util.*;


//>>>>pdfjet {
public class Cell {

    protected double width = 70.0;
    protected double height = 0.0;

    protected Font font = null;
    protected String text = " ";
    protected int align = Align.LEFT;
    protected Point point = null;

    public Border border = null;

    protected int colspan = 1;
    protected double[] bgColor = {1.0, 1.0, 1.0};
    protected double[] penColor = {0.0, 0.0, 0.0};
    protected double[] brushColor = {0.0, 0.0, 0.0};

    protected double lineWidth = 0.0;

    
    public Cell(Font font) {
        this.font = font;
        this.border = new Border();
    }


    public Cell(Font font, String text) {
        this.font = font;
        this.text = text;
        this.border = new Border();
    }


    public void setFont(Font font) {
        this.font = font;
    }


    public Font getFont() {
        return font;
    }


    public void setText(String text) {
        this.text = text;
    }


    public String getText() {
        return text;
    }


    public void setPoint(Point point) {
        this.point = point;
    }


    public Point getPoint() {
        return point;
    }


    public Border getBorder() {
        return border;
    }


    public double getWidth() {
        return width;
    }


    public double getHeight() {
        return height;
    }


    public double getColspan() {
        return colspan;
    }


    public void setBorder(Border border) {
        this.border = border;
    }


    public void setNoBorders() {
        this.border.top = false;
        this.border.bottom = false;
        this.border.left = false;
        this.border.right = false;
    }


    public void setBgColor(double[] bgColor) {
        this.bgColor = bgColor;
    }


    public void setBgColor(int[] rgb) {
        this.bgColor =
                new double[] { rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0 };
    }


    public void setFgColor(double[] fgColor) {
        this.penColor = fgColor;
        this.brushColor = fgColor;
    }


    public void setFgColor(int[] rgb) {
        this.penColor =
                new double[] { rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0 };
        this.brushColor =
                new double[] { rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0 };
    }


    public void setPenColor(double[] fgColor) {
        this.penColor = fgColor;
    }


    public void setPenColor(int[] rgb) {
        this.penColor =
                new double[] { rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0 };
    }


    public void setBrushColor(double[] fgColor) {
        this.brushColor = fgColor;
    }


    public void setBrushColor(int[] rgb) {
        this.brushColor =
                new double[] { rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0 };
    }


    public void setTextAlignment(int alignment) {
        this.align = alignment;
    }


    public void setColSpan( int colspan ) {
        this.colspan = colspan;
    }


    public void setWidth( double width ) {
        this.width = width;
    }


    public void setHeight( double height ) {
        this.height = height;
    }
    
    
    public void setColspan(int colspan) {
    	this.colspan = colspan;
    }


    public void paint(
            Page page,
            double x,
            double y,
            double w,
            double h,
            double margin) throws Exception {

        drawBackground(page, x, y, w, h);
        drawBorders(page, x, y, w, h);
        drawText(page, x, y, w, margin);

    }


    private void drawBackground(
            Page page,
            double x,
            double y,
            double cell_w,
            double cell_h) throws Exception {

        page.setBrushColor(brushColor[0], brushColor[1], brushColor[2]);
        Box box = new Box(x, y, cell_w, cell_h);
        box.setColor(bgColor);
        box.setFillShape(true);
        box.drawOn(page);

    }


    private void drawBorders(
            Page page,
            double x,
            double y,
            double cell_w,
            double cell_h) throws Exception {

        page.setPenWidth(lineWidth);
        page.setPenColor(penColor[0], penColor[1], penColor[2]);

        if (border.left) {
            page.moveTo(x, y);
            page.lineTo(x, y + cell_h);
            page.strokePath();
        }

        if (border.right) {
            page.moveTo(x + cell_w, y);
            page.lineTo(x + cell_w, y + cell_h);
            page.strokePath();
        }

        if (border.top) {
            page.moveTo(x, y);
            page.lineTo(x + cell_w, y);
            page.strokePath();
        }

        if (border.bottom) {
            page.moveTo(x, y + cell_h);
            page.lineTo(x + cell_w, y + cell_h);
            page.strokePath();
        }

    }


    private void drawText(
            Page page,
            double x,
            double y,
            double cell_w,
            double margin) throws Exception {

        double y_text = y + font.ascent + margin;
        page.setPenColor(penColor[0], penColor[1], penColor[2]);
        page.setBrushColor(brushColor[0], brushColor[1], brushColor[2]);

        if (align == Align.RIGHT) {
            page.drawString(
                    font,
                    text,
                    (x + cell_w) - (font.stringWidth(text) + margin), y_text);
        } else if (align == Align.CENTER) {
            page.drawString(
                    font,
                    text,
                    x + (cell_w - font.stringWidth(text)) / 2, y_text);
        } else {
            // Use the default - Align.LEFT
            page.drawString(
                    font,
                    text,
                    x + margin,
                    y_text);
        }

        if (point != null) {
            point.x = (x + cell_w) - (font.ascent / 2 + margin);
            point.y = y + (font.ascent / 2 + margin);
            point.r = font.ascent / 3;
            page.drawPoint(point);
        }

    }

}   // End of Cell.java
//<<<<}
