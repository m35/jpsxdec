/**
 *  Page.java
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
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;


//>>>>pdfjet {
public class Page {

    protected ByteArrayOutputStream buf = null;
    protected String writingMode = "1 0 0 1 ";
    protected int renderingMode = 0;
    protected double width = 0.0;
    protected double height = 0.0;
    protected List<Annotation> annots = null;
    protected PDF pdf = null;

    private double[] pen_color = {0.0, 0.0, 0.0};
    private double[] brush_color = {0.0, 0.0, 0.0};
    private double pen_width = 0.0;
    private String line_pattern = "[] 0";


    public Page(PDF pdf, double[] pageSize) throws Exception {
        this.pdf = pdf;
        annots = new ArrayList<Annotation>();
        width = pageSize[0];
        height = pageSize[1];
        buf = new ByteArrayOutputStream(8192);
        pdf.pages.add(this);
    }


    protected void drawLine(
            double x1,
            double y1,
            double x2,
            double y2) throws IOException {
        moveTo(x1, y1);
        lineTo(x2, y2);
        strokePath();
    }


    protected void drawString(
            Font font,
            String str,
            double x,
            double y) throws IOException {
        append("BT\n");
        append("/F");
        append(font.objNumber);
        append(' ');
        append(font.size);
        append(" Tf\n");
        if (renderingMode != 0) {
            append(renderingMode);
            append(" Tr\n");
        }
        append(writingMode);
        append(x);
        append(' ');
        append(height - y);
        append(" Tm\n");

        append("[ (");
        for (int i = 0; i < str.length(); i++) {
            int c1 = str.charAt(i);
            if (font.isComposite) {
                if (c1 < font.firstChar || c1 > font.lastChar) {
                    append((byte) 0x0000);
                    append((byte) 0x0020);
                    continue;
                }

                byte hi = (byte) (c1 >> 8);
                byte lo = (byte) (c1);
                if (hi == '(' || hi == ')' || hi == '\\') {
                    append((byte) '\\');
                }
                append((byte) (c1 >> 8));
                if (lo == '(' || lo == ')' || lo == '\\') {
                    append((byte) '\\');
                }
                append((byte) (c1));
                continue;
            }

            if (c1 < font.firstChar || c1 > font.lastChar) {
                c1 = 0x0020;
            }
            if (c1 == '(' || c1 == ')' || c1 == '\\') {
                append((byte) '\\');
            }
            append((byte) c1);

            if (font.isStandard == false) continue;
            if (font.kernPairs == false) continue;
            if (font.name.startsWith("C") ||    // Courier
                font.name.startsWith("S") ||    // Symbol
                font.name.startsWith("Z")) {    // ZapfDingbats
                continue;
            }

            if (i == str.length() - 1) break;
            c1 -= 32;
            int c2 = str.charAt(i + 1);
            if (c2 < font.firstChar || c2 > font.lastChar) {
                c2 = 32;
            }
            for (int j = 2; j < font.metrics[c1].length; j += 2) {
                if (font.metrics[c1][j] == c2) {
                    append(") ");
                    append(-font.metrics[c1][j + 1]);
                    append(" (");
                    break;
                }
            }
        }
        append(") ] TJ\n");

        append("ET\n");
    }


    /**
     * Set color for stroking operations
     */
    protected void setPenColor(
            double r, double g, double b) throws IOException {
        if (pen_color[0] == r &&
                pen_color[1] == g &&
                pen_color[2] == b) {
            return;
        } else {
            pen_color[0] = r;
            pen_color[1] = g;
            pen_color[2] = b;
        }
        append(r);
        append(' ');
        append(g);
        append(' ');
        append(b);
        append(" RG\n");
    }


    /**   
     * Set color for nonstroking operations
     */
    protected void setBrushColor(
            double r, double g, double b) throws IOException {
        if (brush_color[0] == r &&
                brush_color[1] == g &&
                brush_color[2] == b) {
            return;
        } else {
            brush_color[0] = r;
            brush_color[1] = g;
            brush_color[2] = b;
        }
        append(r);
        append(' ');
        append(g);
        append(' ');
        append(b);
        append(" rg\n");
    }


    protected void setDefaultLineWidth() throws IOException {
        append(0.0);
        append(" w\n");
    }


    protected void setLinePattern(String pattern) throws IOException {
        if (pattern.equals(line_pattern)) {
            return;
        } else {
            line_pattern = pattern;
        }
        if (pattern.startsWith("[")) {
            append(pattern);
        } else {
            int dash = 0;
            int space = 0;
            for (int i = 0; i < pattern.length(); i++) {
                if (pattern.charAt(i) == '-') {
                    dash++;
                } else {
                    space++;
                }
            }
            if (dash == 0 || space == 0) {
                append("[] 0");
            } else {
                append("[" + dash/2 + " " + space/2 + "] 0");
            }
        }
        append(" d\n");
    }


    protected void setDefaultLinePattern() throws IOException {
        append("[] 0");
        append(" d\n");
    }


    protected void setPenWidth(double width) throws IOException {
        if (pen_width == width) {
            return;
        }
        pen_width = width;
        append(pen_width);
        append(" w\n");
    }


    protected void moveTo(double x, double y) throws IOException {
        append(x);
        append(' ');
        append(height - y);
        append(" m\n");
    }


    protected void lineTo(double x, double y) throws IOException {
        append(x);
        append(' ');
        append(height - y);
        append(" l\n");
    }


    protected void closePath() throws IOException {
        append("h\n");
    }


    protected void strokePath() throws IOException {
        append("S\n");
    }


    protected void fillPath() throws IOException {
        append("f\n");
    }


    protected void drawPath(
            List<Point> list, char operand) throws Exception {
        if (list.size() < 2) {
            throw new Exception(
                    "The Path object must contain at least 2 points");
        }
        Point point = list.get(0);
        moveTo(point.x, point.y);
        int numOfCurvePoints = 0;
        for (int i = 1; i < list.size(); i++) {
            point = list.get(i);
            if (point.isCurvePoint) {
                append(point.x);
                append(' ');
                append(height - point.y);
                if (numOfCurvePoints < 2) {
                    append(' ');
                    numOfCurvePoints++;
                } else {
                    append(" c\n");
                    numOfCurvePoints = 0;
                }
            } else {
                lineTo(point.x, point.y);
            }
        }
        if (numOfCurvePoints != 0) {
            throw new Exception(
                    "Invalid number of curve points in the Path object");
        }
        append(operand);
        append('\n');
    }


    protected void drawBezierCurve(
            List<Point> list, char operand) throws IOException {
        Point point = list.get(0);
        moveTo(point.x, point.y);
        for (int i = 1; i < list.size(); i++) {
            point = list.get(i);
            append(point.x);
            append(' ');
            append(height - point.y);
            if (i % 3 == 0) {
                append(" c\n");
            } else {
                append(' ');
            }
        }

        append(operand);
        append('\n');
    }


    protected void drawCircle(
            double x,
            double y,
            double r,
            char operand) throws Exception {
        List<Point> list = new ArrayList<Point>();

        Point point = new Point();
        point.x = x;
        point.y = y - r;
        list.add(point);    // Starting point

        point = new Point();
        point.x = x + 0.55*r;
        point.y = y - r;
        list.add(point);
        point = new Point();
        point.x = x + r;
        point.y = y - 0.55*r;
        list.add(point);
        point = new Point();
        point.x = x + r;
        point.y = y;
        list.add(point);

        point = new Point();
        point.x = x + r;
        point.y = y + 0.55*r;
        list.add(point);
        point = new Point();
        point.x = x + 0.55*r;
        point.y = y + r;
        list.add(point);
        point = new Point();
        point.x = x;
        point.y = y + r;
        list.add(point);

        point = new Point();
        point.x = x - 0.55*r;
        point.y = y + r;
        list.add(point);
        point = new Point();
        point.x = x - r;
        point.y = y + 0.55*r;
        list.add(point);
        point = new Point();
        point.x = x - r;
        point.y = y;
        list.add(point);

        point = new Point();
        point.x = x - r;
        point.y = y - 0.55*r;
        list.add(point);
        point = new Point();
        point.x = x - 0.55*r;
        point.y = y - r;
        list.add(point);
        point = new Point();
        point.x = x;
        point.y = y - r;
        list.add(point);

        drawBezierCurve(list, operand);
    }


    protected void drawPoint(Point p) throws Exception {
        if (p.shape == Point.INVISIBLE) return;

        List<Point> list = null;
        Point point = null;

        if (p.shape == Point.CIRCLE) {
            if (p.fill_shape) {
                drawCircle(p.x, p.y, p.r, 'f');
            } else {
                drawCircle(p.x, p.y, p.r, 'S');
            }
        } else if (p.shape == Point.DIAMOND) {
            list = new ArrayList<Point>();
            point = new Point();
            point.x = p.x;
            point.y = p.y - p.r;
            list.add(point);
            point = new Point();
            point.x = p.x + p.r;
            point.y = p.y;
            list.add(point);
            point = new Point();
            point.x = p.x;
            point.y = p.y + p.r;
            list.add(point);
            point = new Point();
            point.x = p.x - p.r;
            point.y = p.y;
            list.add(point);
            if (p.fill_shape) {
                drawPath(list, 'f');
            } else {
                drawPath(list, 's');
            }
        } else if (p.shape == Point.BOX) {
            list = new ArrayList<Point>();
            point = new Point();
            point.x = p.x - p.r;
            point.y = p.y - p.r;
            list.add(point);
            point = new Point();
            point.x = p.x + p.r;
            point.y = p.y - p.r;
            list.add(point);
            point = new Point();
            point.x = p.x + p.r;
            point.y = p.y + p.r;
            list.add(point);
            point = new Point();
            point.x = p.x - p.r;
            point.y = p.y + p.r;
            list.add(point);
            if (p.fill_shape) {
                drawPath(list, 'f');
            } else {
                drawPath(list, 's');
            }
        } else if (p.shape == Point.PLUS) {
            drawLine(p.x - p.r, p.y, p.x + p.r, p.y);
            drawLine(p.x, p.y - p.r, p.x, p.y + p.r);
        } else if (p.shape == Point.UP_ARROW) {
            list = new ArrayList<Point>();
            point = new Point();
            point.x = p.x;
            point.y = p.y - p.r;
            list.add(point);
            point = new Point();
            point.x = p.x + p.r;
            point.y = p.y + p.r;
            list.add(point);
            point = new Point();
            point.x = p.x - p.r;
            point.y = p.y + p.r;
            list.add(point);
            if (p.fill_shape) {
                drawPath(list, 'f');
            } else {
                drawPath(list, 's');
            }
        } else if (p.shape == Point.DOWN_ARROW) {
            list = new ArrayList<Point>();
            point = new Point();
            point.x = p.x - p.r;
            point.y = p.y - p.r;
            list.add(point);
            point = new Point();
            point.x = p.x + p.r;
            point.y = p.y - p.r;
            list.add(point);
            point = new Point();
            point.x = p.x;
            point.y = p.y + p.r;
            list.add(point);
            if (p.fill_shape) {
                drawPath(list, 'f');
            } else {
                drawPath(list, 's');
            }
        } else if (p.shape == Point.LEFT_ARROW) {
            list = new ArrayList<Point>();
            point = new Point();
            point.x = p.x + p.r;
            point.y = p.y + p.r;
            list.add(point);
            point = new Point();
            point.x = p.x - p.r;
            point.y = p.y;
            list.add(point);
            point = new Point();
            point.x = p.x + p.r;
            point.y = p.y - p.r;
            list.add(point);
            if (p.fill_shape) {
                drawPath(list, 'f');
            } else {
                drawPath(list, 's');
            }
        } else if (p.shape == Point.RIGHT_ARROW) {
            list = new ArrayList<Point>();
            point = new Point();
            point.x = p.x - p.r;
            point.y = p.y - p.r;
            list.add(point);
            point = new Point();
            point.x = p.x + p.r;
            point.y = p.y;
            list.add(point);
            point = new Point();
            point.x = p.x - p.r;
            point.y = p.y + p.r;
            list.add(point);
            if (p.fill_shape) {
                drawPath(list, 'f');
            } else {
                drawPath(list, 's');
            }
        } else if (p.shape == Point.H_DASH) {
            drawLine(p.x - p.r, p.y, p.x + p.r, p.y);
        } else if (p.shape == Point.V_DASH) {
            drawLine(p.x, p.y - p.r, p.x, p.y + p.r);
        } else if (p.shape == Point.X_MARK) {
            drawLine(p.x - p.r, p.y - p.r, p.x + p.r, p.y + p.r);
            drawLine(p.x - p.r, p.y + p.r, p.x + p.r, p.y - p.r);
        } else if (p.shape == Point.MULTIPLY) {
            drawLine(p.x - p.r, p.y - p.r, p.x + p.r, p.y + p.r);
            drawLine(p.x - p.r, p.y + p.r, p.x + p.r, p.y - p.r);
            drawLine(p.x - p.r, p.y, p.x + p.r, p.y);
            drawLine(p.x, p.y - p.r, p.x, p.y + p.r);
        } else if (p.shape == Point.STAR) {
            double angle = Math.PI / 10;
            double sin18 = Math.sin(angle);
            double cos18 = Math.cos(angle);
            double a = p.r * cos18;
            double b = p.r * sin18;
            double c = 2 * a * sin18;
            double d = 2 * a * cos18 - p.r;
            list = new ArrayList<Point>();
            point = new Point();
            point.x = p.x;
            point.y = p.y - p.r;
            list.add(point);
            point = new Point();
            point.x = p.x + c;
            point.y = p.y + d;
            list.add(point);
            point = new Point();
            point.x = p.x - a;
            point.y = p.y - b;
            list.add(point);
            point = new Point();
            point.x = p.x + a;
            point.y = p.y - b;
            list.add(point);
            point = new Point();
            point.x = p.x - c;
            point.y = p.y + d;
            list.add(point);
            if (p.fill_shape) {
                drawPath(list, 'f');
            } else {
                drawPath(list, 's');
            }
        }
    }


    protected void setTextRenderingMode(int mode) throws Exception {
        if (mode == 0 || mode == 1
                || mode == 2 || mode == 3
                || mode == 4 || mode == 5
                || mode == 6 || mode == 7) {
            this.renderingMode = mode;
        } else {
            throw new Exception("Invalid text rendering mode: " + mode);
        }
    }


    protected void setTextDirection(int degrees) throws Exception {
        if (degrees > 360) degrees %= 360;
        if (degrees == 0) {
            writingMode = "1 0 0 1 ";
        } else if (degrees == 90) {
            writingMode = "0 1 -1 0 ";
        } else if (degrees == 180) {
            writingMode = "-1 0 0 -1 ";
        } else if (degrees == 270) {
            writingMode = "0 -1 1 0 ";
        } else if (degrees == 360) {
            writingMode = "1 0 0 1 ";
        } else {
            double sinOfAngle = Math.sin(degrees * (Math.PI / 180));
            double cosOfAngle = Math.cos(degrees * (Math.PI / 180));
            StringBuilder sb = new StringBuilder();
            sb.append(cosOfAngle);
            sb.append(' ');
            sb.append(sinOfAngle);
            sb.append(' ');
            sb.append(-sinOfAngle);
            sb.append(' ');
            sb.append(cosOfAngle);
            sb.append(' ');
            writingMode = sb.toString().replace(',', '.');
        }
    }


    protected void append(String str) throws IOException {
        for (int i = 0; i < str.length(); i++) {
            buf.write((byte) str.charAt(i));
        }
    }


    protected void append(int num) throws IOException {
        append(String.valueOf(num));
    }


    protected void append(double val) throws IOException {
        append(String.valueOf(val).replace(',', '.'));
    }


    protected void append(char ch) throws IOException {
        buf.write((byte) ch);
    }


    protected void append(byte b) throws IOException {
        buf.write(b);
    }

}   // End of Page.java
//<<<<}
