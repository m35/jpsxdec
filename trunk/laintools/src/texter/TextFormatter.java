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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

public class TextFormatter {

    private final HashMap<String, Color> nameColorMap = new HashMap<String, Color>();

    public TextFormatter(String sFile) throws IOException {
        load(sFile);
    }

    public TextFormatter() {
    }

    public void load(String sFile) throws IOException {
        FileInputStream fis = new FileInputStream(sFile);
        Ini ini = new Ini(fis);
        fis.close();
        
        Section sect = ini.get("Colors");
        if (sect == null) throw new IllegalArgumentException("Colors section not found.");

        nameColorMap.clear();

        for (Entry<String, String> entry : sect.entrySet()) {
            nameColorMap.put(entry.getKey(), new Color(Integer.parseInt(entry.getValue(), 16)));
        }
    }

    private AttributedString formatString(String sTxt, Font fnt) {
        
        if (sTxt.length() == 0) 
            throw new IllegalArgumentException("Can't format empty text.");
        
        Font fntBold = fnt.deriveFont(Font.BOLD);
        AttributedString formattedTxt = new AttributedString(sTxt);
        // set font of entire string
        formattedTxt.addAttribute(TextAttribute.FONT, fnt);
        
        int iCurIdx = 0;
        int iLastNamePos = -1;
        Color curColor = null;
        while (true) {

            // TODO: change this to
            // (1) find all the names
            // (2) sort them by occurance
            // (3) format those pieces

            String sNextName = findNextName(sTxt, iCurIdx);
            if (sNextName != null) {
                int iNextNamePos = sTxt.indexOf(sNextName + ":", iCurIdx);
                
                // bold name
                formattedTxt.addAttribute(TextAttribute.FONT, fntBold, iNextNamePos, iNextNamePos + sNextName.length());
                
                // color paragraph
                if (curColor != null) {
                    formattedTxt.addAttribute(TextAttribute.FOREGROUND, curColor, iLastNamePos, iNextNamePos);
                }
                iLastNamePos = iNextNamePos;
                iCurIdx = iNextNamePos + sNextName.length();
                curColor = nameColorMap.get(sNextName);
            } else {
                // color rest of the text
                if (curColor != null) {
                    formattedTxt.addAttribute(TextAttribute.FOREGROUND, curColor, iLastNamePos, sTxt.length());
                }
                // and exit because we're done
                break;
            }

        }
        
        return formattedTxt;
    }
    
    
    private String findNextName(String sTxt, int iPos) {
        String sClosestName = null;
        int iClosestNamePos = -1;

        for (String sName : nameColorMap.keySet()) {
            int i = sTxt.indexOf(sName + ":", iPos);
            if (i >= 0 && (iClosestNamePos < 0 || i < iClosestNamePos) ) {
                sClosestName = sName;
                iClosestNamePos = i;                            
            }
        }

        return sClosestName;
    }

    public void add(String name, Color color) {
        nameColorMap.put(name, color);
    }

    public AttributedStringIterator[] getIterator(String sTxt, Font font, Graphics2D g2d) {
        // hide comments
        String sSave = sTxt;
        sTxt = sTxt.replaceAll("\\[[^\\]]*\\]", "");
        // break up the text by the text breakers
        String[] asTxt = sTxt.split("[\\r\\n\\s]*(-----*|====)[\\r\\n\\s]*");
        int iSize = asTxt.length;

        StringBuilder sb = new StringBuilder(asTxt[0]);
        for (int i = 1; i < iSize; i++)
            sb.append(' ').append(asTxt[i]);

        AttributedString x = formatString(sb.toString(), font);

        AttributedStringIterator[] aoIter = new AttributedStringIterator[iSize];

        for (int i = 0, iOfs = 0; i < iSize; i++) {
            aoIter[i] = new AttributedStringIterator(asTxt[i], g2d,
                    x.getIterator(null, iOfs, iOfs+asTxt[i].length()),
                    iOfs);
            iOfs += asTxt[i].length() + 1;
        }

        return aoIter;
    }

    public static class AttributedStringIterator {
        final LineBreakMeasurer lbm;
        final String sTxt;
        final int iStart;

        public AttributedStringIterator(String _sTxt, Graphics2D g2d, AttributedCharacterIterator charIter, int start) {
            // need the FontRenderContext
            FontRenderContext frc = g2d.getFontRenderContext();

            // create the LineBreakMeasurer
            lbm = new LineBreakMeasurer(charIter, BreakIterator.getLineInstance(), frc);

            sTxt = _sTxt;
            iStart = start;
        }

        private final static Pattern NEWLINE = Pattern.compile("(\r\n|\n)");
        public TextLayout getNextLineToDraw(int iWidth) {
            
            Matcher matcher = NEWLINE.matcher(sTxt);
            if (matcher.find(lbm.getPosition()-iStart)) {
                return lbm.nextLayout(iWidth, matcher.end()+iStart, true);
            } else {
                return lbm.nextLayout(iWidth);
            }
            
           /* 
            int iNextLine = sTxt.indexOf('\n', lbm.getPosition()-iStart);
            if (iNextLine < 0)
                return lbm.nextLayout(iWidth);
            else
                    return lbm.nextLayout(iWidth, iNextLine+1+iStart, true);
                    * 
                    */
        }

        public boolean hasMore() {
            return lbm.getPosition() - iStart < sTxt.length();
        }

    }


}
