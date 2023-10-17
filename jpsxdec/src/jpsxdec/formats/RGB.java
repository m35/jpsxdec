/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.formats;

/** Basic mutable class to manage RGB values. */
public class RGB {
    private int r, g, b;

    public RGB() {
    }

    public RGB(int iRgb) {
        this((iRgb >> 16) & 0xFF,
             (iRgb >>  8) & 0xFF,
             (iRgb      ) & 0xFF);
    }

    public RGB(int iR, int iG, int iB) {
        r = iR;
        g = iG;
        b = iB;
    }

    public void setR(double dblRed) {
        r = (int)Math.round(dblRed);
    }
    public void setG(double dblGreen) {
        g = (int)Math.round(dblGreen);
    }
    public void setB(double dblBlue) {
        b = (int)Math.round(dblBlue);
    }
    public void setR(int iRed)   { r = iRed; }
    public void setG(int iGreen) { g = iGreen; }
    public void setB(int iBlue)  { b = iBlue; }
    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }

    public int toInt() {
        int clampr = r < 0 ? 0x000000 : r > 255 ? 0xff0000 : r << 16;
        int clampg = g < 0 ? 0x000000 : g > 255 ? 0x00ff00 : g << 8;
        int clampb = b < 0 ? 0x000000 : b > 255 ? 0x0000ff : b;
        return 0xFF000000 | clampr | clampg | clampb;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d)", r, g, b);
    }

    /** 0x__RRGGBB */
    public void set(int iRgb) {
        r = (iRgb >> 16) & 0xFF;
        g = (iRgb >>  8) & 0xFF;
        b = (iRgb      ) & 0xFF;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RGB other = (RGB) obj;
        return this.r == other.r && this.g == other.g && this.b == other.b;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + this.r;
        hash = 53 * hash + this.g;
        hash = 53 * hash + this.b;
        return hash;
    }


}
