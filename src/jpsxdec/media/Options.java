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
 * Options.java
 */

package jpsxdec.media;

/** Used to pass decoding options for the PSXMedia.Decode() function. */
public interface Options {
    
    public static interface IOutputFileName extends Options {
        /** File name should not have an extension. */
        String getOutputFileName();
    }
    
    public static interface IOutputImageFormat extends Options {
        String getOutputImageFormat();
    }
    
    public static interface IInputImageFormat extends Options {
        String getInputImageFormat();
    }
    
    ////////////////////////////////////////////////////////////////////////
    
    public static interface IVideoOptions extends Options, IOutputImageFormat, IOutputFileName {
        /** {@inheritDoc}
         * Should have a %d in the file name for the frame number. */
        String getOutputFileName(); // dup for added doc
        
        long getStartFrame();
        long getEndFrame();
    }
    
    public static interface IAudioOptions extends Options, IOutputFileName {
        double getScale();
        String getOutputFormat();
        
    }
    
    public static interface IXAOptions extends Options, IAudioOptions {
        /** Return -1 to decode all channels */
        int getChannel();
    }
    
    public static interface IImageOptions extends Options, IOutputFileName, IOutputImageFormat {
        /** {@inheritDoc}
         * Should have a %d in the file name for the palette number. */
        String getOutputFileName(); // dup for added doc
        
    }
}
