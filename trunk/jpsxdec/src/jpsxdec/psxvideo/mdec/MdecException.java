/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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

package jpsxdec.psxvideo.mdec;

import javax.annotation.Nonnull;
import jpsxdec.i18n.LocalizedException;
import jpsxdec.i18n.LocalizedMessage;

/** Superclass of the different MDEC conversion and bitstream exceptions. */
public abstract class MdecException extends LocalizedException {

    public MdecException(@Nonnull LocalizedMessage msg) {
        super(msg);
    }

    public MdecException(@Nonnull Throwable cause) {
        super(cause);
    }

    public MdecException(@Nonnull LocalizedMessage msg, @Nonnull Throwable cause) {
        super(msg, cause);
    }

    // =========================================================
    

    /** Error related to reading from an MDEC stream. */
    public static class Read extends MdecException {

        public Read(@Nonnull LocalizedMessage message) {
            super(message);
        }

        public Read(@Nonnull Throwable cause) {
            super(cause);
        }

        public Read(@Nonnull LocalizedMessage msg, @Nonnull Throwable cause) {
            super(msg, cause);
        }

    }

    /** Error related to decoding an MDEC stream. */
    public static class Decode extends Read {

        public Decode(@Nonnull LocalizedMessage message) {
            super(message);
        }

        public Decode(@Nonnull Throwable cause) {
            super(cause);
        }

        public Decode(@Nonnull LocalizedMessage message, @Nonnull Throwable cause) {
            super(message, cause);
        }

    }

    /** Error related to uncompressing an MDEC bitstream. */
    public static class Uncompress extends Read {

        public Uncompress(@Nonnull LocalizedMessage message) {
            super(message);
        }

        public Uncompress(@Nonnull Throwable cause) {
            super(cause);
        }

    }

    // ======================================================================

    /** Error related to writing to an MDEC stream. */
    public static class Write extends MdecException {

        public Write(@Nonnull LocalizedMessage message) {
            super(message);
        }

        public Write(@Nonnull Exception cause) {
            super(cause);
        }

    }

    /** Error related to encoding to an MDEC stream. */
    public static class Encode extends Write {

        public Encode(@Nonnull LocalizedMessage message) {
            super(message);
        }

    }

    /** Error related to compressing an MDEC bitstream. */
    public static class Compress extends Write {

        public Compress(@Nonnull LocalizedMessage message) {
            super(message);
        }

    }

    /** A specific type of bitstream compression failure
     * which indicates to the caller that the stream could be
     * compressed if the quantization scale was higher.
     * This is basically only used for Lain escape code compression,
     * but is a general concept among video bitstream encoders (e.g. MPEG1). */
    public static class TooMuchEnergyToCompress extends Compress {

        public TooMuchEnergyToCompress(@Nonnull LocalizedMessage message) {
            super(message);
        }

    }

}
