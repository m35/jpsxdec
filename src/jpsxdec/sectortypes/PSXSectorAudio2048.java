/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * PSXSectorAudio2048.java
 */

package jpsxdec.sectortypes;

import java.io.DataInputStream;
import java.io.IOException;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.NotThisTypeException;


/** By far the slowest at identifying the sector type. For ISO files only. */
public class PSXSectorAudio2048 extends PSXSector {

    int m_iBitsPerSample = -1;

    public PSXSectorAudio2048(CDXASector oCDSect)
            throws NotThisTypeException 
    {
        super(oCDSect);
        if (oCDSect.hasSectorHeader()) throw new NotThisTypeException();

        DataInputStream oDIS = new DataInputStream(oCDSect.getSectorDataStream());
        
        int aiSndParams[] = new int[4];

        for (int i = 0; i < 16; i++) {
            try {
                aiSndParams[0] = oDIS.readInt();
                aiSndParams[1] = oDIS.readInt();
                aiSndParams[2] = oDIS.readInt();
                aiSndParams[3] = oDIS.readInt();
            } catch (IOException ex) {
                // i guess only if the sector data isn't long enough
                // not that it really matters for this type of sector
                throw new NotThisTypeException();
            }

            // if we don't know the bits/sample yet
            if (m_iBitsPerSample < 0) {
                // parameter bytes for 4 bits/sample
                if (aiSndParams[0] == aiSndParams[1] &&
                        aiSndParams[2] == aiSndParams[3]) {
                    // however, only if all 4 ints are not equal can we be sure
                    // (if they're equal then it could be either 4 or 8 bps)
                    if (aiSndParams[0] != aiSndParams[2])
                        m_iBitsPerSample = 4;

                }
                // parameter bytes for 8 bits/sample
                else if (aiSndParams[0] == aiSndParams[2] &&
                        aiSndParams[1] == aiSndParams[3]) {
                    m_iBitsPerSample = 8;
                } else
                    throw new NotThisTypeException();

            }
            // if it's 4 bits/sample and the parameters don't fit the pattern
            else if (m_iBitsPerSample == 4 &&
                    (aiSndParams[0] != aiSndParams[1] ||
                    aiSndParams[2] != aiSndParams[3])) {
                throw new NotThisTypeException();
            }
            // if it's 8 bits/sample and the parameters don't fit the pattern
            else if (m_iBitsPerSample == 8 &&
                    (aiSndParams[0] != aiSndParams[2] ||
                    aiSndParams[1] != aiSndParams[3])) {
                throw new NotThisTypeException();
            }
            try {
                oDIS.skip(128 - 16);
            } catch (IOException ex) {
                // i guess only if the sector data isn't long enough
                throw new NotThisTypeException();
            }
        } // for

        // if it made it this far, then there is a very good chance
        // this is an audio sector

        // At this point we will probably know the bits/sample.
        // If this is a movie, we can also narrow down the samples/sec 
        // & mono/stereo to at most 2 choices by seeing how often the STR
        // has audio data. 
        // But for XA, there's absolutely no way we can figure out if there 
        // are 32 audio channels or fewer.

        // At 4 bits/sample
        // If it is every 32 sectors, then it must be mono @ 18900 samples/sec
        // If it is every 16 sectors, then it must be either
        //                               stereo @ 18900 samples/sec
        //                              or mono @ 37800 samples/sec
        // If it is every 8 sectors, then it must be stereo @ 37800 samples/sec

        // At 8 bits/sample
        // It could never be 32 sectors
        // If it is every 16 sectors, then it must be mono @ 18900 samples/sec
        // If it is every 8 sectors, then it must be either
        //                                  stereo @ 18900 samples/sec
        //                                 or mono @ 37800 samples/sec
        // If it is every 4 sectors, then it must be stereo @ 37800 samples/sec

        // I'm believe the above is correct, but I haven't really checked.
        // Again, not that any of this really matters because we can't
        // decode the audio anyway.
    }

    public String toString() {
        return "Audio2048 " + super.toString() +
                String.format(
                " bits/sample:%d channels:? samples/sec:?",
                m_iBitsPerSample);
    }

    protected int getDemuxedDataStart(int iDataSize) {
        return 0;
    }

    protected int getDemuxedDataLength(int iDataSize) {
        return iDataSize; // should be 2048
    }
}
