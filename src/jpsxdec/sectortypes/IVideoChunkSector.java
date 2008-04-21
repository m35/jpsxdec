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
 * IVideoChunkSector.java
 */

package jpsxdec.sectortypes;

import jpsxdec.util.ByteArrayFPIS;

/** Interface that should be implemented by all video sector classes. 
 *  Used primarily in the demuxers. */
public interface IVideoChunkSector {
    
    /** Where this sector falls in the order of 
     *  video sectors for this frame. Note that this will be different
     *  from the chunk number used in FF8 and FF9 frames. */
    long getChunkNumber();

    /** Number of video sectors used to hold the frame that this sector
     *  is a part of. */
    long getChunksInFrame();

    /** Frame number that this sector is a part of. */
    long getFrameNumber();

    /** Height of the frame in pixels. */
    long getHeight();

    /** Width of the frame in pixels. */
    long getWidth();

    int getPsxUserDataSize();
            
    ByteArrayFPIS getUserDataStream();
}
