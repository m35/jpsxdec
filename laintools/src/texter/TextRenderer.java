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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.Iterator;
import texter.TextFormatter.AttributedStringIterator;

public class TextRenderer {

    /** Renders formatted text on the graphics area "within" the rectangles. 
     *  Within, meaning the left, top, and right boundaries (it will
     *  run over the bottom boundary if the font/characters are big
     *  enough to). */
    public static boolean renderText(
            Graphics2D g2d,
            ArrayList<Rectangle> rects,
            AttributedStringIterator txtIter,
            boolean blnDrawText,
            boolean blnDrawBounds) 
    {

        // need the FontRenderContext
        FontRenderContext frc = g2d.getFontRenderContext();

        Iterator<Rectangle> rectIter = rects.iterator();
        // keep going as long as there is text to draw, and rectangles left to draw in
        while (txtIter.hasMore() && rectIter.hasNext()) {
            Rectangle thisLineRect = rectIter.next();
            TextLayout layout = txtIter.getNextLineToDraw(thisLineRect.width);
            // returns null if word will not fit
            if (layout == null)
                continue; // try the next rectangle until we can fit the word

            // get the offset of the base position from the top-left pixel position
            //System.out.println(thisLineRect.toString());
            Rectangle textBounds = layout.getPixelBounds(frc, thisLineRect.x, thisLineRect.y);
            int iDiffX = thisLineRect.x - textBounds.x;
            int iDiffY = thisLineRect.y - textBounds.y;
            
            if (blnDrawBounds) {
                // draw text bounding box
                g2d.setColor(Color.GREEN);
                g2d.drawRect(textBounds.x + iDiffX, textBounds.y + iDiffY, textBounds.width, textBounds.height);
                // draw target rectangle
                g2d.setColor(Color.WHITE);
                g2d.drawRect(thisLineRect.x, thisLineRect.y, thisLineRect.width, thisLineRect.height);
            }

            // draw the text
            if (blnDrawText)
                layout.draw(g2d, thisLineRect.x + iDiffX, thisLineRect.y + iDiffY);
        }
        return txtIter.hasMore();
    }
    


}
