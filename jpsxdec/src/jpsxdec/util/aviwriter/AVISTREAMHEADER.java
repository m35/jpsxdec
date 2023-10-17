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

package jpsxdec.util.aviwriter;

import java.io.IOException;
import java.io.RandomAccessFile;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;

/** Represents the
 * <a href="http://msdn2.microsoft.com/en-us/library/ms779638(VS.85).aspx">AVISTREAMHEADER</a>
 * C structure.
 * <p>
 * See also
 * <a href="http://blog.jtimothyking.com/2006/12/15/does-bad-writing-reflect-poor-programming-skills">additional details</a>
 * on the {@link #dwInitialFrames} field.
 */
class AVISTREAMHEADER extends AVIstruct {
     public final /*FOURCC*/ int   fcc                    = string2int("strh");
     public final /*DWORD */ int   cb                     = sizeof() - 8;
     public       /*FOURCC*/ int   fccType                = 0;
     public       /*FOURCC*/ int   fccHandler             = 0;
     public       /*DWORD */ int   dwFlags                = 0;
     public       /*WORD  */ short wPriority              = 0;
     public       /*WORD  */ short wLanguage              = 0;
     public       /*DWORD */ long  dwInitialFrames        = 0;
     public       /*DWORD */ long  dwScale                = 0;
     public       /*DWORD */ long  dwRate                 = 0;
     public       /*DWORD */ long  dwStart                = 0;
     public       /*DWORD */ long  dwLength               = 0;
     public       /*DWORD */ long  dwSuggestedBufferSize  = 0;
     public       /*DWORD */ long  dwQuality              = 0;
     public       /*DWORD */ long  dwSampleSize           = 0;
     //struct {
         public /*short int*/ short left            = 0;
         public /*short int*/ short top             = 0;
         public /*short int*/ short right           = 0;
         public /*short int*/ short bottom          = 0;
     //}  rcFrame

    @Override
    public void write(@Nonnull RandomAccessFile raf) throws IOException {
        IO.writeInt32LE(raf, fcc);
        IO.writeInt32LE(raf, cb);
        IO.writeInt32LE(raf, fccType);
        IO.writeInt32LE(raf, fccHandler);
        IO.writeInt32LE(raf, dwFlags);
        IO.writeInt16LE(raf, wPriority);
        IO.writeInt16LE(raf, wLanguage);
        IO.writeInt32LE(raf, (int)dwInitialFrames);
        IO.writeInt32LE(raf, (int)dwScale);
        IO.writeInt32LE(raf, (int)dwRate);
        IO.writeInt32LE(raf, (int)dwStart);
        IO.writeInt32LE(raf, (int)dwLength);
        IO.writeInt32LE(raf, (int)dwSuggestedBufferSize);
        IO.writeInt32LE(raf, (int)dwQuality);
        IO.writeInt32LE(raf, (int)dwSampleSize);

        IO.writeInt16LE(raf, left);
        IO.writeInt16LE(raf, top);
        IO.writeInt16LE(raf, right);
        IO.writeInt16LE(raf, bottom);

    }

    @Override
    public int sizeof() {
        return 64;
    }

}
