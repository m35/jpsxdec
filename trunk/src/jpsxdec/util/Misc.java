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

package jpsxdec.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextField;

/** Miscellaneous helper functions. */
public final class Misc {

    public static String[] regex(String regex, String s) {
        return regex(Pattern.compile(regex), s);
    }

    public static String[] regex(Pattern regex, String s) {
        Matcher m = regex.matcher(s);
        if (!m.find()) return null;
        String as[] = new String[m.groupCount()+1];
        for (int i = 0; i < as.length; i++) {
            as[i] = m.group(i);
        }
        return as;
    }

    /** Returns an array of all matches of all groups. */
    public static String[] regexAll(Pattern regex, String s) {
        Matcher m = regex.matcher(s);
        ArrayList<String> matches = new ArrayList<String>();
        while (m.find()) {
            for (int i = 0; i <= m.groupCount(); i++) {
                matches.add(m.group(i));
            }
        }
        return matches.toArray(new String[matches.size()]);
    }
    
    /** http://www.rgagnon.com/javadetails/java-0029.html */
    public static String stack2string(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    
    public static Vector<String> append(Vector<String> v, String[] as) {
        for (String s : as) {
            v.add(s);
        }
        return v;
    }
    
    public static Vector<String> append(String[] as, Vector<String> v) {
        for (int i = 0; i < as.length; i++) {
            v.insertElementAt(as[i], i);
        }
        return v;
    }
    
    public static Vector join(Vector v1, Vector v2) {
        Vector v = new Vector(v1.size() + v2.size());
        for (Object i : v1) {
            v.add(i);
        }
        for (Object i : v2) {
            v.add(i);
        }
        return v;
    }
    
    public static String dup(String s, int count) {
        if (count == 0)
            return "";
        StringBuilder oSB = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            oSB.append(s);
        }
        return oSB.toString();
    }

    public static String dup(char c, int count) {
        if (count == 0)
            return "";
        char[] ac = new char[count];
        Arrays.fill(ac, c);
        return new String(ac);
    }
    
    /** Manual implementation of the Java 6 Array.copyOfRange function. 
     *  Borrowed from some older Apache code. */
    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        
        byte[] arr = new byte[newLength];
        int ceil = original.length-from;
        int len = (ceil < newLength) ? ceil : newLength;
        System.arraycopy(original, from, arr, 0, len);
        
        return arr;
    }

    
    public static String getBaseName(JTextField tf) {
        return getBaseName(tf.getText());
    }
    
    public static String getBaseName(String txt) {
        int i = txt.lastIndexOf('.');
        if (i >= 0)
            return txt.substring(0, i);
        else
            return txt;
    }
    
    public static String getExt(JTextField tf) {
        return getExt(tf.getText());
    }
    
    public static String getExt(String txt) {
        int i = txt.lastIndexOf('.');
        if (i >= 0)
            return txt.substring(i+1);
        else
            return "";
    }
    
    
    public static String join(Iterable ao, String sBetween) {
        StringBuilder oSB = new StringBuilder();
        boolean blnFirst = true;
        for (Object o : ao) {
            if (!blnFirst) oSB.append(sBetween); else blnFirst = false;
            oSB.append(o.toString());
        }
        return oSB.toString();
    }

    
    public static String join(Object[] ao, String sBetween) {
        StringBuilder oSB = new StringBuilder();
        boolean blnFirst = true;
        for (Object o : ao) {
            if (!blnFirst) oSB.append(sBetween); else blnFirst = false;
            oSB.append(o.toString());
        }
        return oSB.toString();
    }

    /** Splits a string into an array of ints. 
     *  Returns null if non-int values encoutnered. */
    public static int[] splitInt(String s, String regex) {
        String[] split = s.split(regex);
        int[] ai = new int[split.length];
        
        try {
            for (int i = 0; i < split.length; i++) {
                ai[i] = Integer.parseInt(split[i]);
            }
            return ai;

        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    public static long[] splitLong(String s, String regex) {
        String[] split = s.split(regex);
        long[] ai = new long[split.length];
        
        try {
            for (int i = 0; i < split.length; i++) {
                ai[i] = Long.parseLong(split[i]);
            }
            return ai;

        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    
    /** Splits string s via sDelimiter and parses the resulting array
     *  into an array of ints. If there is any error, then null is returned. */
    public static int[] parseDelimitedInts(String s, String sDelimiter) {
        String[] asParse = s.split(sDelimiter);
        return stringArrayToIntArray(asParse);
    }

    /** Parses an array of strings into an array of ints. If there is any
     *  error, or if any of the values are negitive, null is returned. */
    public static int[] stringArrayToIntArray(String[] as) {
        try {
            int[] aiVals = new int[as.length];
            for (int i = 0; i < as.length; i++) {
                aiVals[i] = Integer.parseInt(as[i]);
                if (aiVals[i] < 0) return null;
            }
            
            return aiVals;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    public static String[] parseFilename(String s) {
        int i = s.lastIndexOf('.');
        if (i >= 0) {
            return new String[] {s.substring(0, i), s.substring(i)};
        } else {
            return new String[] {s, ""};
        }
    }

    private final static String[] ZERO_PAD = new String[] {
        "", "0", "00", "000", "0000", "00000", "000000", "0000000", "00000000",
        "000000000", "0000000000", "00000000000", "000000000000",
        "0000000000000", "00000000000000", "000000000000000",
        "0000000000000000", "00000000000000000", "000000000000000000",
        "0000000000000000000", "00000000000000000000", "000000000000000000000",
        "0000000000000000000000", "00000000000000000000000",
        "000000000000000000000000", "0000000000000000000000000",
        "00000000000000000000000000", "000000000000000000000000000",
        "0000000000000000000000000000", "00000000000000000000000000000",
        "000000000000000000000000000000", "0000000000000000000000000000000",
        "00000000000000000000000000000000"
    };
    public static String bitsToString(long val, int iCount) {
        String sBin = Long.toBinaryString(val);
        int len = sBin.length();

        if (len < iCount)
            return ZERO_PAD[iCount - len] + sBin;
        else if (len > iCount)
            return sBin.substring(len - iCount);
        else
            return sBin;
    }

    public static boolean listsEqual(List a, List b) {
        if (a == b) return true;

        if (a == null || b == null)
            return false;

        if (a.size() != b.size()) return false;

        Iterator ai = a.iterator();
        Iterator bi = b.iterator();

        while (ai.hasNext() && bi.hasNext()) {
            if (!ai.next().equals(bi.next()))
                return false;
        }

        return true;
    }

}
