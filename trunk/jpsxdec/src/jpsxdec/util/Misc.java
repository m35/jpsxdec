/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** Miscellaneous helper functions. */
public final class Misc {

    /** Returns an array of just the matching groups.
     * @return null if failed to match */
    public static @CheckForNull String[] regex(@Nonnull String regex, @Nonnull String s) {
        return regex(Pattern.compile(regex), s);
    }

    /** Returns an array of just the matching groups.
     * @return null if failed to match */
    public static @CheckForNull String[] regex(@Nonnull Pattern regex, @Nonnull String s) {
        Matcher m = regex.matcher(s);
        if (!m.find()) return null;
        String as[] = new String[m.groupCount()+1];
        for (int i = 0; i < as.length; i++) {
            as[i] = m.group(i);
        }
        return as;
    }

    /** Returns an array of all matches of all groups.
     * @return null if failed to match */
    public static @Nonnull String[] regexAll(@Nonnull Pattern regex, @Nonnull String s) {
        Matcher m = regex.matcher(s);
        ArrayList<String> matches = new ArrayList<String>();
        while (m.find()) {
            for (int i = 0; i <= m.groupCount(); i++) {
                matches.add(m.group(i));
            }
        }
        return matches.toArray(new String[matches.size()]);
    }

    /** Converts the String to int via {@link Integer#parseInt(java.lang.String)},
     * unless it is null, then returns the default. */
    public static int parseIntOrDefault(@CheckForNull String sInt, int iDefault) throws NumberFormatException {
        if (sInt == null)
            return iDefault;
        else
            return Integer.parseInt(sInt);
    }

    /** http://www.rgagnon.com/javadetails/java-0029.html */
    public static @Nonnull String stack2string(@Nonnull Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static int intCompare(int i1, int i2) {
        if (i1 < i2)
            return -1;
        else if (i1 > i2)
            return 1;
        else
            return 0;
    }
    
    /** Duplicates a string {@code count} times. */
    public static @Nonnull String dup(@Nonnull String s, int count) {
        if (count == 0)
            return "";
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /** Duplicates a character {@code count} times. */
    public static @Nonnull String dup(char c, int count) {
        if (count == 0)
            return "";
        char[] ac = new char[count];
        Arrays.fill(ac, c);
        return new String(ac);
    }
    
    /** Manual implementation of the Java 6 Array.copyOfRange function. 
     *  Borrowed from some older Apache code. */
    public static @Nonnull byte[] copyOfRange(@Nonnull byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        
        byte[] arr = new byte[newLength];
        int ceil = original.length-from;
        int len = (ceil < newLength) ? ceil : newLength;
        System.arraycopy(original, from, arr, 0, len);
        
        return arr;
    }

    /** Removes the extension from the given file name/path. */
    public static @Nonnull String removeExt(@Nonnull String sFileName) {
        int i = sFileName.lastIndexOf('.');
        if (i >= 0)
            return sFileName.substring(0, i);
        else
            return sFileName;
    }
    
    /** Gets the extension from the given file name/path.
     * @return empty string if no extension.  */
    public static @Nonnull String getExt(@Nonnull String sFileName) {
        int i = sFileName.lastIndexOf('.');
        if (i >= 0)
            return sFileName.substring(i+1);
        else
            return "";
    }

    private static final URI CURRENT_URI = new File(".").toURI();
    public static @Nonnull String forwardSlashPath(@Nonnull File f) {
        return CURRENT_URI.relativize(f.toURI()).toString();
    }

    public static @Nonnull String join(@Nonnull Iterable ao, @Nonnull String sBetween) {
        StringBuilder sb = new StringBuilder();
        boolean blnFirst = true;
        for (Object o : ao) {
            if (!blnFirst) sb.append(sBetween); else blnFirst = false;
            sb.append(o.toString());
        }
        return sb.toString();
    }

    
    public static @Nonnull String join(@Nonnull Object[] ao, @Nonnull String sBetween) {
        StringBuilder sb = new StringBuilder();
        boolean blnFirst = true;
        for (Object o : ao) {
            if (!blnFirst) sb.append(sBetween); else blnFirst = false;
            sb.append(o.toString());
        }
        return sb.toString();
    }

    /** Splits a string into an array of ints. 
     *  Returns null if non-int values encountered. */
    public static @CheckForNull int[] splitInt(@Nonnull String s, @Nonnull String regex) {
        String[] asSplit = s.split(regex);
        return stringArrayToIntArray(asSplit);
    }
    
    public static @CheckForNull long[] splitLong(@Nonnull String s, @Nonnull String regex) {
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
    
    /** Parses an array of strings into an array of ints. If there is any
     *  error, or if any of the values are negative, null is returned. */
    public static @CheckForNull int[] stringArrayToIntArray(@Nonnull String[] as) {
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
    public static @Nonnull String bitsToString(long val, int iCount) {
        String sBin = Long.toBinaryString(val);
        int len = sBin.length();

        if (len < iCount)
            return ZERO_PAD[iCount - len] + sBin;
        else if (len > iCount)
            return sBin.substring(len - iCount);
        else
            return sBin;
    }

    public static void log(@Nonnull Logger log, @Nonnull Level level,
                           @CheckForNull Throwable cause,
                           @Nonnull String sMessage, Object ... aoArguments)
    {
        LogRecord lr = new LogRecord(level, sMessage);
        lr.setLoggerName(log.getName());
        lr.setParameters(aoArguments);
        lr.setThrown(cause);
        log.log(lr);
    }

}
