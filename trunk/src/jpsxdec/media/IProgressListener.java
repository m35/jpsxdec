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
 * IProgressListener.java
 */

package jpsxdec.media;

/** A general purpose listener interface for reporting progress. */
public interface IProgressListener {
    /** Return true to stop, false to continue */
    boolean ProgressUpdate(String sWhatDoing, double dblPercentComplete);
    
    public static interface IProgressEventListener extends IProgressListener {
        /** Return true to stop, false to continue */
        boolean ProgressUpdate(String sEvent);
    }
    
    public static interface IProgressErrorListener extends IProgressListener {
        /** Return true to stop, false to continue */
        void ProgressUpdate(Exception e);
    }
    
    public static interface IProgressEventErrorListener extends IProgressEventListener, IProgressErrorListener {
    }
    
}
