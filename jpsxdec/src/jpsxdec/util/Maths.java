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

/** Helpful math stuff. No I don't speak The Queen's English, I just wanted
 * to differentiate from the normal {@link java.lang.Math} class. */
public class Maths {

    /** A faster but LESS ACCURATE implementation of the Math.round() function.
     * This introduces a bit more floating-point error by the +/- 0.5. */
    public static long round(double dbl) {
        return dbl > 0 ? (long)(dbl + 0.5) : (long)(dbl - 0.5);
    }
    /** A faster but LESS ACCURATE implementation of the Math.round() function.
     * This introduces a bit more floating-point error by the +/- 0.5. */
    public static int iround(double dbl) {
        return dbl > 0 ? (int)(dbl + 0.5) : (int)(dbl - 0.5);
    }
    /** A faster but LESS ACCURATE implementation of the Math.round() function.
     * This introduces a bit more floating-point error by the +/- 0.5. */
    public static int round(float flt) {
        return flt > 0 ? (int)(flt + 0.5) : (int)(flt - 0.5);
    }


    // http://www.idevelopment.info/data/Programming/data_structures/java/gcd/GCD.java
    public static int gcd(int m, int n) {

        if (m < n) {
            int t = m;
            m = n;
            n = t;
        }

        int r = m % n;

        if (r == 0) {
            return n;
        } else {
            return gcd(n, r);
        }

    }

    public static long shrRound(long val, int shr) {
        if (shr == 0 || val == 0)
            return val;
        long i = (val >> (shr - 1)) & 1;
        return (val >> shr) + i;
    }

    public static boolean floatEquals(float f1, float f2, float epsilon) {
        return Math.abs(f1 - f2) <= epsilon;
    }
}
