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

package jpsxdec.psxvideo.bitstreams;

import javax.annotation.Nonnull;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;

public abstract class StrHeader {

    public static final int SIZEOF = 8;

    /** Frame's quantization scale. */
    private int _iQuantizationScale = -1;
    private int _iHalfMdecCodeCountCeil32 = -1;
    private int _iVersion = -1;

    private final boolean _blnIsValid;
    protected StrHeader(@Nonnull byte[] abFrameData, int iDataSize,
                        int iExpectedVersion)
    {
        if (iDataSize < SIZEOF) {
            _blnIsValid = false;
        } else {
            int iHalfMdecCodeCountCeil32 = IO.readSInt16LE(abFrameData, 0);
            int iMagic3800               = IO.readUInt16LE(abFrameData, 2);
            int iQscale                  = IO.readSInt16LE(abFrameData, 4);
            int iVersion                 = IO.readSInt16LE(abFrameData, 6);

            _blnIsValid = iMagic3800 == 0x3800 &&
                          iQscale >= 1 && iQscale <= 63 &&
                          iVersion == iExpectedVersion &&
                          iHalfMdecCodeCountCeil32 >= 0;
            if (_blnIsValid) {
                _iQuantizationScale = iQscale;
                _iHalfMdecCodeCountCeil32 = iHalfMdecCodeCountCeil32;
                _iVersion = iVersion;
            }
        }
    }

    public int getQuantizationScale() {
        if (!_blnIsValid) throw new IllegalStateException();
        return _iQuantizationScale;
    }

    public int getHalfMdecCodeCountCeil32() {
        if (!_blnIsValid) throw new IllegalStateException();
        return _iHalfMdecCodeCountCeil32;
    }

    public boolean isValid() {
        return _blnIsValid;
    }

    public @Nonnull BitStreamUncompressor makeNew(@Nonnull byte[] abBitstream) throws BinaryDataNotRecognized {
        return makeNew(abBitstream, abBitstream.length);
    }

    abstract public @Nonnull BitStreamUncompressor makeNew(@Nonnull byte[] abBitstream, int iBitstreamSize)
            throws BinaryDataNotRecognized;

    @Override
    public String toString() {
        if (_blnIsValid)
            return "Qscale " + _iQuantizationScale + " count " + _iHalfMdecCodeCountCeil32;
        else
            return "Invalid STR header";
    }

}
