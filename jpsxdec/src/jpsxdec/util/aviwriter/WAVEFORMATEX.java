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

/** Represents the C WAVEFORMATEX structure.
 *  http://msdn2.microsoft.com/en-us/library/ms713497(VS.85).aspx */
class WAVEFORMATEX extends AVIstruct {

    static short WAVE_FORMAT_PCM = 1;

    // ** VirtualDub writes 16 bytes struct (leaves off cbSize)

    public /*WORD */ short wFormatTag      = 0;
    public /*WORD */ short nChannels       = 0;
    public /*DWORD*/ int   nSamplesPerSec  = 0;
    public /*DWORD*/ int   nAvgBytesPerSec = 0;
    public /*WORD */ short nBlockAlign     = 0;
    public /*WORD */ short wBitsPerSample  = 0;
    //public /*WORD */ short cbSize          = 0; **


    @Override
    public void write(@Nonnull RandomAccessFile raf) throws IOException {
        /*WORD */ IO.writeInt16LE(raf, wFormatTag     );
        /*WORD */ IO.writeInt16LE(raf, nChannels      );
        /*DWORD*/ IO.writeInt32LE(raf, nSamplesPerSec );
        /*DWORD*/ IO.writeInt32LE(raf, nAvgBytesPerSec);
        /*WORD */ IO.writeInt16LE(raf, nBlockAlign    );
        /*WORD */ IO.writeInt16LE(raf, wBitsPerSample );
        ///*WORD */ IO.writeInt16LE(raf, cbSize         ); **
    }

    @Override
    public int sizeof() {
        return 18-2; // **
    }

}
