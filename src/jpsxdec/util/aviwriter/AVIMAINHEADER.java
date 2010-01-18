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

package jpsxdec.util.aviwriter;

import java.io.IOException;
import java.io.RandomAccessFile;

/** Represents the C AVIMAINHEADER structure. 
 * http://msdn2.microsoft.com/en-us/library/ms779632(VS.85).aspx */
class AVIMAINHEADER extends AVIstruct {
    public final static int AVIF_HASINDEX      = 0x00000010;
    public final static int AVIF_ISINTERLEAVED = 0x00000100;

    public final /*FOURCC*/ int  fcc                    = string2int("avih");
    public final /*DWORD */ int  cb                     = sizeof() - 8;
    public       /*DWORD */ long dwMicroSecPerFrame     = 0;
    public       /*DWORD */ long dwMaxBytesPerSec       = 0;
    public       /*DWORD */ long dwPaddingGranularity   = 0;
    public       /*DWORD */ int  dwFlags                = 0;
    public       /*DWORD */ long dwTotalFrames          = 0;
    public       /*DWORD */ long dwInitialFrames        = 0;
    public       /*DWORD */ long dwStreams              = 0;
    public       /*DWORD */ long dwSuggestedBufferSize  = 0;
    public       /*DWORD */ long dwWidth                = 0;
    public       /*DWORD */ long dwHeight               = 0;
    public final /*DWORD */ int  dwReserved1            = 0;
    public final /*DWORD */ int  dwReserved2            = 0;
    public final /*DWORD */ int  dwReserved3            = 0;
    public final /*DWORD */ int  dwReserved4            = 0;
    
    @Override
    public void write(RandomAccessFile raf) throws IOException {
        write32LE(raf, fcc                       );
        write32LE(raf, cb                        );
        write32LE(raf, (int)dwMicroSecPerFrame   );
        write32LE(raf, (int)dwMaxBytesPerSec     );
        write32LE(raf, (int)dwPaddingGranularity );
        write32LE(raf, dwFlags                   );
        write32LE(raf, (int)dwTotalFrames        );
        write32LE(raf, (int)dwInitialFrames      );
        write32LE(raf, (int)dwStreams            );
        write32LE(raf, (int)dwSuggestedBufferSize);
        write32LE(raf, (int)dwWidth              );
        write32LE(raf, (int)dwHeight             );
        write32LE(raf, dwReserved1               );
        write32LE(raf, dwReserved2               );
        write32LE(raf, dwReserved3               );
        write32LE(raf, dwReserved4               );
    }
    
    @Override
    public int sizeof() {
        return 64;
    }
    
}
