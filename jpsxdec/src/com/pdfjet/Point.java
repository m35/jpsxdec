/**
 *  Point.java
 *
Copyright (c) 2007, 2008, 2009 Innovatics Inc.

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
public class Point {

    public static final int INVISIBLE = -1;
    public static final int CIRCLE = 0;
    public static final int DIAMOND = 1;
    public static final int BOX = 2;
    public static final int PLUS = 3;
    public static final int H_DASH = 4;
    public static final int V_DASH = 5;
    public static final int MULTIPLY = 6;
    public static final int STAR = 7;
    public static final int X_MARK = 8;
    public static final int UP_ARROW = 9;
    public static final int DOWN_ARROW = 10;
    public static final int LEFT_ARROW = 11;
    public static final int RIGHT_ARROW = 12;

    public static final boolean IS_CURVE_POINT = true;

    protected double x = 0.0;
    protected double y = 0.0;
    protected double r = 2.0;

    protected int shape = 0;
    protected double[] color = {0.0, 0.0, 0.0};
    protected double line_width = 0.3;
    protected String line_pattern = "[] 0";
    protected boolean fill_shape = false;

    protected boolean isCurvePoint = false;

    protected String text = null;
    protected String uri = null;
    protected List<String> info = null;

    // drawLineTo == false means:
    //      Don't draw a line to this point from the previous
    protected boolean drawLineTo = false;

    private double box_x = 0.0;
    private double box_y = 0.0;


    public Point() {
    }


    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }


    public Point(double x, double y, boolean isCurvePoint) {
        this.x = x;
        this.y = y;
        this.isCurvePoint = isCurvePoint;
    }


    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }


    public void setX(double x) {
        this.x = x;
    }


    public double getX() {
        return x;
    }


    public void setY(double y) {
        this.y = y;
    }


    public double getY() {
        return y;
    }


    public void setRadius(double r) {
        this.r = r;
    }


    public double getRadius() {
        return r;
    }


    public void setShape(int shape) {
        this.shape = shape;
    }


    public int getShape() {
        return shape;
    }


    public void setFillShape(boolean fill) {
        this.fill_shape = fill;
    }


    public boolean getFillShape() {
        return fill_shape;
    }


    public void setColor(double[] color) {
        this.color = color;
    }


    public void setColor(int[] rgb) {
        this.color = new double[] {rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0};
    }


    public double[] getColor() {
        return color;
    }


    public void setLineWidth(double line_width) {
        this.line_width = line_width;
    }


    public double getLineWidth() {
        return line_width;
    }


    public void setLinePattern(String line_pattern) {
        this.line_pattern = line_pattern;
    }


    public String getLinePattern() {
        return line_pattern;
    }


    public void setDrawLineTo(boolean drawLineTo) {
        this.drawLineTo = drawLineTo;
    }


    public boolean getDrawLineTo() {
        return drawLineTo;
    }


    public void setURIAction(String uri) {
        this.uri = uri;
    }


    public String getURIAction() {
        return uri;
    }


    public void setText(String text) {
        this.text = text;
    }


    public String getText() {
        return text;
    }


    public void setInfo(List<String> info) {
        this.info = info;
    }


    public List<String> getInfo() {
        return info;
    }


    public void placeIn(
            Box box,
            double x_offset,
            double y_offset) throws Exception {
        box_x = box.x + x_offset;
        box_y = box.y + y_offset;
    }


    public void drawOn(Page page) throws Exception {
        page.setPenWidth(line_width);
        page.setLinePattern(line_pattern);

        if (fill_shape) {
            page.setBrushColor(
                    color[0], color[1], color[2]);
        } else {
            page.setPenColor(
                    color[0], color[1], color[2]);
        }

        x += box_x;
        y += box_y;
        page.drawPoint(this);
        x -= box_x;
        y -= box_y;
    }

}   // End of Point.java
//<<<<}
