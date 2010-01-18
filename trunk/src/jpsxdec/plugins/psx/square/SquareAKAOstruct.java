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

package jpsxdec.plugins.psx.square;

import java.io.IOException;
import java.io.InputStream;
import jpsxdec.util.IO;

/** The AKAO structure used in some Square games. FF8, FF9, and Chrono Cross
 *  movies all contain this struture. It seems to be different than the AKAO
 *  structure used for sound-effects (from what I read in the Qhimm forums).
 *  As you can see, there are not many fields identified for this structure. */
public class SquareAKAOstruct {
    
    public static final long AKAO_ID = 0x4F414B41; // "AKAO" in little-endian
    public static final long SIZE = 80;
    
    final public long AKAO;                 // [4 bytes]
    final public long FrameNumSub1;         // [4 bytes] often the frame number - 1
    // [20 bytes] unknown
    final public long Unknown;              // [4 bytes]
    final public long BytesOfData;          // [4 bytes] number of bytes of audio data
    // [44 bytes] unknown
    
    public SquareAKAOstruct(InputStream inStream) throws IOException {
        AKAO = IO.readUInt32LE(inStream);
        FrameNumSub1 = IO.readUInt32LE(inStream);

        IO.skip(inStream, 20);

        Unknown = IO.readUInt32LE(inStream);
        BytesOfData = IO.readUInt32LE(inStream);

        IO.skip(inStream, 44);
    }
    
    public String toString() {
        return String.format(
            "AKAO:%08x frame-1:%d ?:%04x Size:%d",
            AKAO,
            FrameNumSub1,
            Unknown,
            BytesOfData
            );
    }

}
