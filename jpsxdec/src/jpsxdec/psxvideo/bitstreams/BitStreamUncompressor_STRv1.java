/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

package jpsxdec.psxvideo.bitstreams;

import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.util.IO;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.LocalizedIncompatibleException;

/** Rather uncommon STR "version 1" video frame format.
 * Identical to STRv2, but has a version of 1 in the header.
 * Used in FF7, FF Tactics, and Tekken 2 (and probably others).
 *<p>
 * FF7 video also contains run-length codes where AC is 0.
 * This case obviously works with PlayStation hardware, so is already
 * accepted by the STRv2 uncompressor to make things simpler, faster, and
 * more robust (because AC=0 may occur in games besides FF7). See
 * {@link BitStreamUncompressor_STRv2#readEscapeAcCode(jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode)}.
 *<p>
 * I suspect the reason for this FF7 AC=0 waste is because they compressed the
 * frames further to fit camera data. This led to AC values being reduced,
 * some falling to 0, but they didn't merge those codes to save space.
 */
public class BitStreamUncompressor_STRv1 extends BitStreamUncompressor_STRv2 {

    @Override
    protected boolean readHeader(@Nonnull byte[] abFrameData, int iDataSize,
                                 @Nonnull ArrayBitReader bitReader)
    {
        if (!_header.readHeader(abFrameData, iDataSize, 1))
            return false;

        bitReader.reset(abFrameData, iDataSize, true, 8);
        return true;
    }

    public static boolean checkHeader(@Nonnull byte[] abFrameData) {
        StrHeader header = new StrHeader();
        return header.readHeader(abFrameData, abFrameData.length, 1);
    }

    public static int getQscale(@Nonnull byte[] abFrameData) throws BinaryDataNotRecognized {
        if (!checkHeader(abFrameData))
            throw new BinaryDataNotRecognized();

        return IO.readSInt16LE(abFrameData, 4);
    }
    
    @Override
    public @Nonnull String getName() {
        return "STRv1";
    }

    @Override
    public @Nonnull BitStreamCompressor_STRv1 makeCompressor() {
        return new BitStreamCompressor_STRv1();
    }

    public static class BitStreamCompressor_STRv1 extends BitStreamCompressor_STRv2 {

        @Override
        protected int getHeaderVersion() { return 1; }

        @Override
        protected int getFrameQscale(@Nonnull byte[] abFrameData) throws LocalizedIncompatibleException {
            try {
                return BitStreamUncompressor_STRv1.getQscale(abFrameData);
            } catch (BinaryDataNotRecognized ex) {
                throw new LocalizedIncompatibleException(I.FRAME_NOT_STRV1(), ex);
            }
        }

    }

}
