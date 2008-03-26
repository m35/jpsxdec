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
 * Misc.java
 */

package jpsxdec.util;

import java.util.Collections;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioSystem;

public final class Misc {
    
    /** Returns a sorted list of available ImageIO formats. */
    public static Vector<String> GetJavaImageFormats() {
        Vector<String> oValidFormats = new Vector<String>();
        String[] asReaderFormats = ImageIO.getReaderFormatNames();
        for (String s : asReaderFormats) {
            s = s.toLowerCase();
            if (oValidFormats.indexOf(s) < 0) {
                oValidFormats.add(s);
            }
        }
        
        Collections.sort(oValidFormats);
        
        return oValidFormats;
    }
    
    /** Gets the AudioFileFormat.Type from its string representation */
    public static Type AudioFileFormatStringToType(String sFormat) {
        if (sFormat.equals(Type.AIFC.toString()))
            return AudioFileFormat.Type.AIFC;
        else if (sFormat.equals(Type.AIFF.toString()))
            return AudioFileFormat.Type.AIFF;
        else if (sFormat.equals(Type.AU.toString()))
            return AudioFileFormat.Type.AU;
        else if (sFormat.equals(Type.SND.toString()))
            return AudioFileFormat.Type.SND;
        else if (sFormat.equals(Type.WAVE.toString()))
            return AudioFileFormat.Type.WAVE;
        else
            return null;
    }
    
    /** Gets the string representation of an AudioFileFormat.Type *
    public static String AudioFileFormatTypeToString(Type oFormat) {
        if (oFormat.equals(AudioFileFormat.Type.AIFC))
            return "aifc";
        else if (oFormat.equals(AudioFileFormat.Type.AIFF))
            return "aiff";
        else if (oFormat.equals(AudioFileFormat.Type.AU))
            return "au";
        else if (oFormat.equals(AudioFileFormat.Type.SND))
            return "snd";
        else if (oFormat.equals(AudioFileFormat.Type.WAVE))
            return "wav";
        else
            return null;
    }*/
    
    public static Vector<String> GetJavaAudioFormats() {
        Vector<String> v = new Vector<String>();
        for (Type t : AudioSystem.getAudioFileTypes()) {
            v.add(t.toString());
        }

        return v;                
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
    
}
