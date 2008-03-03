/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
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
 * SimpleIDCT.java
 */

package jpsxdec.mdec;

/** This is the simplest implementation of the Inverse Discrete Cosine 
 *  Transform. If I understand correctly, it's inverse 2D DCT-II, specifically.
 *
 * It's as simple as I could make it, and as such, it's about as
 * slow as can be.
 *
 * Note: Using the Stephen's version is about 13 times faster!!!!
 * The results are almost indistinguishable. I don't think there
 * is any intentional loss of precision with the optimized version.
 * It's just how the floating point numbers happen to round.
 */
public class SimpleIDCT implements IDCTinterface {

    public Matrix8x8 IDCT(Matrix8x8 oDCTMat) {

        int iWidth = oDCTMat.getWidth();
        int iHeight = oDCTMat.getHeight();

        Matrix8x8 oPixelMat = new Matrix8x8();

        int Pixelx, Pixely, DCTx, DCTy;

        for (Pixelx = 0; Pixelx < iWidth; Pixelx++) {
            for (Pixely = 0; Pixely < iHeight; Pixely++) {

                double dblTotal = 0;

                for (DCTx = 0; DCTx < iWidth; DCTx++) {
                    for (DCTy = 0; DCTy < iHeight; DCTy++) {

                        double dblSubTotal = oDCTMat.getPoint(DCTx, DCTy);

                        if (DCTx == 0)
                            dblSubTotal *= Math.sqrt(1.0f / iWidth);
                        else
                            dblSubTotal *= Math.sqrt(2.0f / iWidth);

                        if (DCTy == 0)
                            dblSubTotal *= Math.sqrt(1.0f / iHeight);
                        else
                            dblSubTotal *= Math.sqrt(2.0f / iHeight);

                        dblSubTotal
                                *= Math.cos( DCTx * Math.PI * (2 * Pixelx + 1)
                                / (2.0f * iWidth)            )
                                *  Math.cos( DCTy * Math.PI * (2 * Pixely + 1)
                                / (2.0f * iHeight)           );

                        dblTotal += dblSubTotal;
                    }
                }

                oPixelMat.setPoint(Pixelx, Pixely, dblTotal);
            }
        }

        return oPixelMat;
    }

}
