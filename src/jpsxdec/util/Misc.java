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
 * Misc.java
 */

package jpsxdec.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import javax.swing.JTextField;

public final class Misc {
    
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
        StringBuilder oSB = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            oSB.append(s);
        }
        return oSB.toString();
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
        int i = txt.indexOf('.');
        if (i >= 0)
            return txt.substring(0, i);
        else
            return txt;
    }
    
    public static String getExt(JTextField tf) {
        return getExt(tf.getText());
    }
    
    public static String getExt(String txt) {
        int i = txt.indexOf('.');
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
    
    /** Splits string s via sDelimiter and parses the resulting array
     *  into an array of ints. If there is any error, then null is returned. */
    public static int[] ParseDelimitedInts(String s, String sDelimiter) {
        String[] asParse = s.split(sDelimiter);
        return StringArrayToIntArray(asParse);
    }

    /** Parses an array of strings into an array of ints. If there is any
     *  error, or if any of the values are negitive, null is returned. */
    public static int[] StringArrayToIntArray(String[] as) {
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
    
}
