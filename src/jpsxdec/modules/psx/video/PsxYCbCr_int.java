/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.modules.psx.video;

import jpsxdec.formats.RGB;

public class PsxYCbCr_int {

    public int y1, y2, y3, y4, cb, cr;

    public PsxYCbCr_int() {
    }

    public void toRgb(RGB rgb1, RGB rgb2, RGB rgb3, RGB rgb4) {
        int iChromRed   =                        (1435 * cr) >> 10 ;
        int iChromGreen = ((-351 * cb) >> 10) + ((-731 * cr) >> 10);
        int iChromBlue  =  (1814 * cb) >> 10                       ;
        int iYshift;
        
        iYshift = y1 + 128;
        rgb1.setR(iYshift + iChromRed);
        rgb1.setG(iYshift + iChromGreen);
        rgb1.setB(iYshift + iChromBlue);

        iYshift = y2 + 128;
        rgb2.setR(iYshift + iChromRed);
        rgb2.setG(iYshift + iChromGreen);
        rgb2.setB(iYshift + iChromBlue);

        iYshift = y3 + 128;
        rgb3.setR(iYshift + iChromRed);
        rgb3.setG(iYshift + iChromGreen);
        rgb3.setB(iYshift + iChromBlue);

        iYshift = y4 + 128;
        rgb4.setR(iYshift + iChromRed);
        rgb4.setG(iYshift + iChromGreen);
        rgb4.setB(iYshift + iChromBlue);
    }

    public String toString() {
        return String.format( "([%d, %d, %d, %d] %d, %d)" , y1, y2, y3, y4, cb, cr);
    }

}
