/*
 * LainTools: PSX Serial Experiments Lain Hacking and Translation Tools
 * Copyright (C) 2011  Michael Sabin
 *
 * Redistribution and use of the LainTools code or any derivative works are
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

package texter;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class RectFinder {
    public static ArrayList<Rectangle> findAvailableBoxes(
            int iLineHeight,
            int iInitialX, 
            int iInitialY, 
            int iShiftX,
            int iShiftY,
            BufferedImage bitMask)
    {
        
        final int iBmpW = bitMask.getWidth();
        final int iBmpH = bitMask.getHeight();
        final int[] aiPixels = bitMask.getRGB(0, 0, iBmpW, iBmpH, null, 0, iBmpW);
        
        Rectangle rect = new Rectangle(0, iInitialY, 1, iLineHeight);
        ArrayList<Rectangle> al = new ArrayList<Rectangle>();

        // walk down the lines of the image and try to create the widest
        // rectangle possible before hitting the mask

        for (; rect.y+rect.height < iBmpH; rect.y++) {
            //System.out.println("Line " + oRect.y);
            for (rect.x = iInitialX; rect.x+rect.width < iBmpW; rect.x++) {
                
                if (!rectHitsBitMask(rect, iBmpW, iBmpH, aiPixels)) {
                    
                    for (int i = 2; i < iBmpW - rect.x; i++) {
                        rect.width = i;
                        if (rectHitsBitMask(rect, iBmpW, iBmpH, aiPixels))
                            break;
                    }
                    rect.width--;
                    Rectangle saveRect = rect;
                    rect = new Rectangle(iInitialX, rect.y+rect.height, 1, iLineHeight);
                    saveRect.x += iShiftX;
                    saveRect.y += iShiftY;
                    al.add(saveRect);
                    
                    if (rect.x > iBmpW || rect.y > iBmpH) return al;
                
                    //System.out.println("Got a rect: " + oSavRect.toString());
                }
                        
            }
        }

        return al;
    }
    
    
    private static boolean rectHitsBitMask(Rectangle rect, int iBmpW, int iBmpH, int[] aiPixels ) {
        for (int y = 0; y < rect.height; y++) {
            for (int x = 0; x < rect.width; x++) {
                int ox = rect.x+x, oy = rect.y+y;
                if (ox >= 0 && oy >= 0 && ox < iBmpW && oy < iBmpH) 
                {
                    int clr = aiPixels[ox + oy * iBmpW];
                    //System.out.println("(" + ox + ", " + oy + ") = " + String.format("%06x", clr));
                    if (clr != 0xffffffff)
                        return true;
                }
            }
        }
        return false;
    }

    
    

}
