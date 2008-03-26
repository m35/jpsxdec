/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.sectortypes;

import java.io.IOException;
import java.io.InputStream;
import jpsxdec.util.IO;

/** The AKAO structure used in some Square games. FF8, FF9, and Chrono Cross
 *  movies all contain this struture. It seems to be different than the AKAO
 *  structure used for sound-effects (from what I read in the Qhimm forums).
 *  As you can see, there are not many fields identified for this structure.*/
public class SquareAKAOstruct {
    
    public static final long AKAO_ID = 0x4F414B41; // "AKAO" in little-endian
    public static final long SIZE = 36;
    
    final public long AKAO;                 // [4 bytes]
    final public long FrameNumSub1;         // [4 bytes]
    // 20 bytes; unknown
    final public long Unknown;              // [4 bytes]
    final public long BytesOfData;          // [4 bytes] number of bytes of audio data
    
    public SquareAKAOstruct(InputStream oIS) throws IOException {
        AKAO = IO.ReadUInt32LE(oIS);
        FrameNumSub1 = IO.ReadUInt32LE(oIS);

        oIS.skip(20);

        Unknown = IO.ReadUInt32LE(oIS);
        BytesOfData = IO.ReadUInt32LE(oIS);
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
