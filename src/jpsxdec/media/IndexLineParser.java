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
 * IndexLineParser.java
 */

package jpsxdec.media;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Parses strings for data using a simple syntax.
 * <pre>
 *  '$' means 1 or more characters (regex ".+")
 *  '#' means a positive integer (regex "\d+")
 * </pre>
 */
public class IndexLineParser {

    final Iterator<String> m_oTokenIter;
    final int m_iSize;
    final String m_sRemaining;

    public IndexLineParser(String sPattern, String sData)
            throws IllegalArgumentException 
    {
        ArrayList<String> oTokenList = new ArrayList<String>();
        int iDatPos = 0;
        StringBuilder strbld;

        for (int iPatPos = 0; iPatPos < sPattern.length(); iPatPos++) {
            char pc = sPattern.charAt(iPatPos);
            if (iDatPos >= sData.length()) {
                throw new IllegalArgumentException("Data \"" + sData + "\" does not match settings \"" + sPattern + "\": Out of bounds");
            }
            switch (pc) {
                case '#':
                    strbld = new StringBuilder();
                    while (iDatPos < sData.length()) {
                        char ds = sData.charAt(iDatPos);
                        if (Character.isDigit(ds)) {
                            strbld.append(ds);
                            iDatPos++;
                        } else {
                            break;
                        }
                    }
                    if (strbld.length() == 0) {
                        throw new IllegalArgumentException("Data \"" + sData + "\" (at char " + iDatPos + ") does not match settings \"" + sPattern + "\" (at char " + iPatPos + ")");
                    }
                    oTokenList.add(strbld.toString());
                    break;
                case '$':
                    strbld = new StringBuilder();
                    if (iPatPos + 1 == sPattern.length()) {
                        oTokenList.add(sData.substring(iDatPos));
                    } else {
                        char sbnext = sPattern.charAt(iPatPos + 1);
                        while (iDatPos < sData.length()) {
                            char ds = sData.charAt(iDatPos);
                            if (ds == sbnext) {
                                break;
                            }
                            strbld.append(ds);
                            iDatPos++;
                        }
                        oTokenList.add(strbld.toString());
                    }
                    break;
                default:
                    if (sData.charAt(iDatPos) != pc) {
                        throw new IllegalArgumentException("Data \"" + sData + "\" (at char " + iDatPos + ") does not match settings \"" + sPattern + "\" (at char " + iPatPos + ")");
                    }
                    iDatPos++;
                    break;
            }
        }
        m_oTokenIter = oTokenList.iterator();
        m_iSize = oTokenList.size();
        if (iDatPos < sData.length()) {
            m_sRemaining = sData.substring(iDatPos);
        } else {
            m_sRemaining = "";
        }
    }

    long get(long l) throws NoSuchElementException, NumberFormatException {
        return Long.parseLong(getNextToken());
    }
    
    int get(int i) throws NoSuchElementException, NumberFormatException {
        return Integer.parseInt(getNextToken());
    }

    String get(String s) throws NoSuchElementException {
        return getNextToken();
    }

    String getNextToken() throws NoSuchElementException {
        return m_oTokenIter.next();
    }
    
    void skip() throws NoSuchElementException {
        m_oTokenIter.next();
    }

    int size() {
        return m_iSize;
    }

    /** Returns the remaning string that was left after parsing was complete. */
    String getRemaining() {
        return m_sRemaining;
    }
}
