/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** Miscellaneous helper functions. */
public final class Misc {

    public static @Nonnull byte[] stringToAscii(@Nonnull String string) {
        return string.getBytes(StandardCharsets.US_ASCII);
    }

    public static @Nonnull String asciiToString(@Nonnull byte[] ascii) {
        return asciiToString(ascii, 0, ascii.length);
    }
    public static @Nonnull String asciiToString(@Nonnull byte[] ascii, int iOffset, int iLength) {
        return new String(ascii, iOffset, iLength, StandardCharsets.US_ASCII);
    }

    /** Makes a date that is iSeconds past the year 0. */
    public static @Nonnull Date dateFromSeconds(int iSeconds) {
        Calendar c = Calendar.getInstance();
        c.set(0, 0, 0, 0, 0, iSeconds);
        return c.getTime();
    }

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
        String[] as = new String[m.groupCount()+1];
        for (int i = 0; i < as.length; i++) {
            as[i] = m.group(i);
        }
        return as;
    }

    /** Returns an array of all matches of all groups.
     * @return empty array if failed to match */
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

    /** http://www.rgagnon.com/javadetails/java-0029.html */
    public static @Nonnull String stack2string(@Nonnull Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /** Null (and somewhat type) safe {@link Object#equals(Object). */
    public static <T, U extends T> boolean objectEquals(@CheckForNull T o1, @CheckForNull U o2) {
        return o1 == o2 || (o1 != null && o1.equals(o2));
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

    /** Removes the extension from the given file name/path. */
    public static @Nonnull String removeExt(@Nonnull String sFileName) {
        int i = sFileName.lastIndexOf('.');
        if (i >= 0)
            return sFileName.substring(0, i);
        else
            return sFileName;
    }

    /** Gets the extension from the given file name/path, without the '.'.
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

    public static @Nonnull String join(@Nonnull Iterable<?> ao, @Nonnull String sBetween) {
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

    public static @Nonnull String bitsToString(long lng, int iLength) {
        String sBin = Long.toBinaryString(lng);
        return zeroPadString(sBin, iLength, true);
    }

    public static @Nonnull String intToPadded0String(int i, int iLength) {
        String sInt = Integer.toString(i);
        return zeroPadString(sInt, iLength, false);
    }

    /** Left-pad the string with zeros to the given length, trimming if shorter
     * when desired. */
    public static @Nonnull String zeroPadString(@Nonnull String s, int iLength, boolean blnTrim) {
        int iSLen = s.length();
        if (iSLen < iLength) {
            char[] acRet = new char[iLength];
            int i = iLength - iSLen;
            Arrays.fill(acRet, 0, i, '0');
            s.getChars(0, iSLen, acRet, i);
            return new String(acRet);
        } else if (iSLen > iLength && blnTrim) {
            return s.substring(iSLen - iLength);
        } else {
            return s;
        }
    }

    /*** Log a message that has parameters and exception. */
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
