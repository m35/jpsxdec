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
 * VideoForWindows.java
 */

package jpsxdec.nativeclass;

public class VideoForWindows {
    
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isWin() {
        return OS.indexOf("windows") > -1;
    }
    private static boolean isRecentWin() {
        return ( (OS.indexOf("windows nt") > -1)
         || (OS.indexOf("windows 2000") > -1 )
         || (OS.indexOf("windows xp") > -1) );
    }
    private static boolean isOldWin() {
        return (OS.indexOf("windows 9") > -1);
    }
    static {
        if (isWin()) {
            System.loadLibrary("vidforwin");
        }
    }
    
    public VideoForWindows() {
        if (!isWin()) throw new RuntimeException("VideoForWindows only available on Windows platform.");
    }
    
    
    public native int Init(String sFileName, 
            int iWidth, int iHeight, 
            int iFrames, int iPerSecond,  // frames/second
            int iAudioChannels, int iSamplesPerSecond);
    
    public native int Close();

    public native int PromptForCompression(int hWnd);
    public native int FindWindow(String sClass, String sTitle);

    public native int InitAVIWrite();

    public native int WriteFrame(byte[] bRGB);
    
    public native int WriteAudio(byte[] bSamples);

    @Override
    protected void finalize() throws Throwable {
        Close();
        super.finalize();
    }
    
}
