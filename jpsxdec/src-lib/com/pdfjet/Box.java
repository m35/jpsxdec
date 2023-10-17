/**
 *  Box.java
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
public class Box {

    protected double x = 0.0;
    protected double y = 0.0;

    private double w = 0.0;
    private double h = 0.0;

    private double[] color = {0.0, 0.0, 0.0};
    private double width = 0.3;
    private String pattern = "[] 0";
    private boolean fill_shape = false;


    public Box() {
    }


    public Box(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }


    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }


    public void setSize(double w, double h) {
        this.w = w;
        this.h = h;
    }


    public void setColor(double[] color) {
        this.color = color;
    }


    public void setColor(int[] rgb) {
        this.color = new double[] {rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0};
    }


    public void setLineWidth(double width) {
        this.width = width;
    }


    public void setPattern(String pattern) {
        this.pattern = pattern;
    }


    public void setFillShape(boolean fill_shape) {
        this.fill_shape = fill_shape;
    }


    public void placeIn(
            Box box,
            double x_offset,
            double y_offset) throws Exception {
        this.x = box.x + x_offset;
        this.y = box.y + y_offset;
    }


    public void scaleBy(double factor) throws Exception {
        this.x *= factor;
        this.y *= factor;
    }


    public void drawOn(Page page) throws Exception {
        page.setPenWidth(width);
        page.setLinePattern(pattern);
        page.moveTo(x, y);
        page.lineTo(x + w, y);
        page.lineTo(x + w, y + h);
        page.lineTo(x, y + h);
        page.closePath();
        if (fill_shape) {
            page.setBrushColor(
                    color[0], color[1], color[2]);
            page.fillPath();
        } else {
            page.setPenColor(
                    color[0], color[1], color[2]);
            page.strokePath();
        }
    }

}   // End of Box.java
//<<<<}
