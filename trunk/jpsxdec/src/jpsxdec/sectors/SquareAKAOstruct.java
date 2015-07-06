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

package jpsxdec.sectors;

import jpsxdec.cdreaders.CdSector;

/** The AKAO structure used in some Square games. FF8, FF9, and Chrono Cross
 *  movies all contain this structure. It seems to be different than the AKAO
 *  structure used for sound-effects (from what I read in the Qhimm forums).
 *  As you can see, there are not many fields identified for this structure. */
public class SquareAKAOstruct {
    
    public static final long AKAO_ID = 0x4F414B41L; // "AKAO" in little-endian
    public static final int SIZE = 80;
    
    public final long AKAO;                 // 0 [4 bytes]
    public final long FrameNumSub1;         // 4 [4 bytes] often the frame number - 1
    // [20 bytes] unknown
    public final long Unknown;              // 28 [4 bytes]
    public final long BytesOfData;          // 32 [4 bytes] number of bytes of audio data
    // [44 bytes] unknown
    
    public SquareAKAOstruct(CdSector cdSector, int i) {
        AKAO = cdSector.readUInt32LE(i);
        FrameNumSub1 = cdSector.readUInt32LE(i+4);

        Unknown = cdSector.readUInt32LE(i+28);
        BytesOfData = cdSector.readUInt32LE(i+32);
    }
    
    public String toString() {
        return String.format(
            "AKAO:%s frame-1:%d ?:%04x Size:%d",
            AKAO == AKAO_ID ? "'AKAO'" : String.format("%08x", AKAO),
            FrameNumSub1,
            Unknown,
            BytesOfData
            );
    }

}
