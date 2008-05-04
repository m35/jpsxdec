/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * Matrix8x8.java
 */

package jpsxdec.mdec;

/** A very simple Matrix class. */
public class Matrix8x8 {

    final protected int Width = 8;
    final protected int Height = 8;
    private double Points[];

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public Matrix8x8() {
        Points = new double[Width * Height];
    }
	
    public Matrix8x8(double vals[]) {
        assert(vals.length == 64);
        Points = vals.clone();
    }

    public Matrix8x8(int vals[]) {
        assert(vals.length == 64);
       
        Points = new double[Width * Height];

        for (int i=0; i < 64; i++) {
            Points[i] = vals[i];
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public int getWidth() {
        return Width;
    }

    public int getHeight() {
        return Height;
    }

    /** Returns the underlying array of doubles */
    public double[] getPoints() {
        return Points;
    }
    
    public double getPoint(int x, int y) {
        return Points[x + (y << 3) /* Width*/];
    }

    public void setPoint(int x, int y, double val) {
        Points[x + (y << 3) /* Width*/] = val;
    }
    
    /** Performs += operation */
    public void setAddPoint(int x, int y, double val) {
        Points[x + (y << 3) /* Width*/] += val;
    }
    
    public void setAddPoint(int i, double val) {
        Points[i] += val;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public String toString() { 
        StringBuilder oSB = new StringBuilder();
        oSB.append("{");
        for (int y=0; y < getHeight(); y++) {
            if (y > 0) oSB.append(String.format("%n"));
            oSB.append("{");
            for (int x = 0; x < getWidth(); x++) {
                if (x > 0) oSB.append(", ");
                oSB.append(String.format("%1.3f", getPoint(x, y)));
            }
            oSB.append("}");
        }
        oSB.append("}");
        return oSB.toString();
    }

}

