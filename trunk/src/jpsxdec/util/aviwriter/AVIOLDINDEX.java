/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

/** Represents the 
 * <a href="http://msdn2.microsoft.com/en-us/library/ms779634(VS.85).aspx">AVIOLDINDEX</a>
 * C structure. */
class AVIOLDINDEX extends AVIstruct {

    public static int AVIIF_KEYFRAME = 0x10;
    
    /** Struture used for the {@link AVIOLDINDEX#aIndex} array. */
    public static class AVIOLDINDEXENTRY extends AVIstruct {
        public /*DWORD*/ int dwChunkId = 0;
        public /*DWORD*/ int dwFlags   = 0;
        public /*DWORD*/ int dwOffset  = 0;
        public /*DWORD*/ int dwSize    = 0;
        
        @Override
        public void write(RandomAccessFile raf) throws IOException {
            write32LE(raf, dwChunkId);
            write32LE(raf, dwFlags  );
            write32LE(raf, dwOffset );
            write32LE(raf, dwSize   );
        }

        @Override
        public int sizeof() {
            return 16;
        }
        
        public static AVIOLDINDEXENTRY[] newarray(int i) {
            AVIOLDINDEXENTRY[] a = new AVIOLDINDEXENTRY[i];
            for (int j = 0; j < a.length; j++) {
                a[j] = new AVIOLDINDEXENTRY();
            }
            return a;
        }
        
    }
    
    
    public /*FOURCC*/ final int fcc    = string2int("idx1");
    public /*DWORD */ final int cb     ;
    public AVIOLDINDEXENTRY aIndex[] = null;    
    
    public AVIOLDINDEX(AVIOLDINDEXENTRY[] a) {
        aIndex = a;
        cb = sizeof() - 8;
    }
    
    @Override
    public void write(RandomAccessFile raf) throws IOException {
        write32LE(raf, fcc);
        write32LE(raf, cb );
        for (AVIOLDINDEXENTRY e : aIndex) {
            e.write(raf);
        }
    }

    @Override
    public int sizeof() {
        if (aIndex != null && aIndex.length > 0)
            return 8 + aIndex.length * aIndex[0].sizeof();
        else
            return 8;
    }
    
}
