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
 * ISquareAudioSector.java
 */

package jpsxdec.sectortypes;

import jpsxdec.util.ByteArrayFPIS;

/** For audio sectors in Square games: FF8, FF9, and Chrono Cross. 
 *  Used by SquareAudioPullDemuxerDecoderIS, and PSXMedia playing. */
public interface ISquareAudioSector {

    /** Audio samples/second. */
    int getSamplesPerSecond();
    /** Number of bytes of audio data in the Square ADPCM format. */
    int getAudioDataSize();
    /** Frame number that this sector belongs to. */
    long getFrameNumber();
    /** Number of chunks of audio data in this frame (should always be 2). */
    long getAudioChunksInFrame();
    /** The chunk number of this audio sector (either 0 or 1). */
    long getAudioChunkNumber();
    
    ByteArrayFPIS getUserDataStream();
}
