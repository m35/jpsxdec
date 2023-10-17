/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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

package jpsxdec.adpcm;

/**
 * Allows us to reuse code for both XA and SPU ADPCM encoding and decoding.
 */
public abstract class K0K1Filter {

    /** The number of filter values. */
    abstract public int getCount();
    /** K0 filter at the supplied index. */
    abstract public double getK0(int iIndex);
    /** K1 filter at the supplied index. */
    abstract public double getK1(int iIndex);

    /** K0 and K1 filters for XA ADPCM audio. */
    public static final K0K1Filter XA = new Xa();
    /** K0 and K1 filters for SPU ADPCM audio. */
    public static final K0K1Filter SPU = new Spu();

    /** K0 and K1 filters for XA ADPCM audio. */
    private static class Xa extends K0K1Filter {
        /** XA ADPCM K0 filter multiplier. */
        static final double[] SoundUnit_K0 = {
            0.0     ,
            0.9375  ,  //  60.0 / 64.0
            1.796875,  // 115.0 / 64.0
            1.53125 ,  //  98.0 / 64.0
        };

        /** XA ADPCM K1 filter multiplier. */
        static final double[] SoundUnit_K1 = {
             0.0     ,
             0.0     ,
            -0.8125  , // -52.0 / 64.0
            -0.859375, // -55.0 / 64.0
        };

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public double getK0(int iIndex) {
            return SoundUnit_K0[iIndex];
        }

        @Override
        public double getK1(int iIndex) {
            return SoundUnit_K1[iIndex];
        }
    }


    /** K0 and K1 filters for SPU ADPCM audio. */
    private static class Spu extends K0K1Filter {
        /**
         * SPU-ADPCM K0 multiplier (filter).
         * SPU-ADPCM supports 5 filters, unlike XA-ADPCM which only support 4.
         */
        private final static double K0[] = new double[] {
            0.0     ,
            0.9375  ,  //  60.0 / 64.0
            1.796875,  // 115.0 / 64.0
            1.53125 ,  //  98.0 / 64.0
            1.90625 ,  // 122.0 / 64.0 <- one more possible value than XA-ADPCM
        };
        /**
         * SPU-ADPCM K1 multiplier (filter).
         * SPU-ADPCM supports 5 filters, unlike XA-ADPCM which only support 4.
         */
        private final static double K1[] = new double[] {
             0.0     ,
             0.0     ,
            -0.8125  , // -52.0 / 64.0
            -0.859375, // -55.0 / 64.0
            -0.9375  , // -60.0 / 64.0 <- one more possible value than XA-ADPCM
        };


        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public double getK0(int iIndex) {
            return K0[iIndex];
        }

        @Override
        public double getK1(int iIndex) {
            return K1[iIndex];
        }
    }

}
