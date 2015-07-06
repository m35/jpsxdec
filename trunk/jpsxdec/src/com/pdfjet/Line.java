/**
 *  Line.java
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
public class Line {

    private double x1 = 0.0;
    private double y1 = 0.0;
    private double x2 = 0.0;
    private double y2 = 0.0;

    private double box_x = 0.0;
    private double box_y = 0.0;

    private double[] color = {0.0, 0.0, 0.0};
    private double width = 0.3;
    private String pattern = "[] 0";


    public Line() {
    }


    public Line(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }


    public void setPattern(String pattern) {
        this.pattern = pattern;
    }


    public void setStartPoint(double x, double y) {
        this.x1 = x;
        this.y1 = y;
    }


    public void setEndPoint(double x, double y) {
        this.x2 = x;
        this.y2 = y;
    }


    public Point getStartPoint() {
        return new Point(x1, y1);
    }


    public Point getEndPoint() {
        return new Point(x2, y2);
    }


    public void setWidth(double width) {
        this.width = width;
    }


    public void setColor(double[] color) {
        this.color = color;
    }


    public void setColor(int[] rgb) {
        this.color = new double[] {rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0};
    }


    public void placeIn(Box box) throws Exception {
        placeIn(box, 0.0, 0.0);
    }


    public void placeIn(
            Box box,
            double x_offset,
            double y_offset) throws Exception {
        box_x = box.x + x_offset;
        box_y = box.y + y_offset;
    }


    public void scaleBy(double factor) throws Exception {
        this.x1 *= factor;
        this.x2 *= factor;
        this.y1 *= factor;
        this.y2 *= factor;
    }


    public void drawOn(Page page) throws Exception {
        page.setPenColor(color[0], color[1], color[2]);
        page.setPenWidth(width);
        page.setLinePattern(pattern);
        page.drawLine(
                x1 + box_x,
                y1 + box_y,
                x2 + box_x,
                y2 + box_y);
    }

}   // End of Line.java
//<<<<}
