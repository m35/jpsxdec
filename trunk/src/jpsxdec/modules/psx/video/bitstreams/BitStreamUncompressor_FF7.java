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

package jpsxdec.modules.psx.video.bitstreams;

import java.util.logging.Logger;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** Final Fantasy 7 video uncompressor.
 * Makes use of most of STR v2 code.
 * Just adds handling for camera data at the start of the frame.
 */
public class BitStreamUncompressor_FF7 extends BitStreamUncompressor_STRv2 {

    private static final Logger log = Logger.getLogger(BitStreamUncompressor_FF7.class.getName());
    protected Logger getLog() { return log; }

    public BitStreamUncompressor_FF7() {
        super();
    }

    @Override
    protected void readHeader(byte[] abFrameData, int iStart, ArrayBitReader bitReader) throws NotThisTypeException {

        _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, iStart+0);
        _iMagic3800          = IO.readUInt16LE(abFrameData, iStart+2);
        _iQscale             = IO.readSInt16LE(abFrameData, iStart+4);
        int iVersion         = IO.readSInt16LE(abFrameData, iStart+6);

        if (_iMagic3800 != 0x3800 || _iQscale < 1 ||
            iVersion != 1 || _iHalfVlcCountCeil32 < 0)
            throw new NotThisTypeException();

        bitReader.reset(abFrameData, true, iStart+8);
    }

    public static boolean checkHeader(byte[] abFrameData) {

        int iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        int iMagic3800          = IO.readUInt16LE(abFrameData, 2);
        int iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion            = IO.readSInt16LE(abFrameData, 6);

        return !(iMagic3800 != 0x3800 || iQscale < 1 ||
                 iVersion != 1 || iHalfVlcCountCeil32 < 0);
    }

    @Override
    public String toString() {
        return "FF7";
    }

    @Override
    public BitStreamCompressor makeCompressor() {
        return new Compressor_FF7();
    }

    public static class Compressor_FF7 extends BitStreamUncompressor_STRv2.BitstreamCompressor_STRv2 {

        @Override
        protected int getHeaderVersion() { return 1; }

    }

}
