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
        return (OS.indexOf("windows nt")   > -1 )
            || (OS.indexOf("windows 2000") > -1 )
            || (OS.indexOf("windows xp")   > -1 );
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
    
    /** Calls win32 FindWindow() function.
     * I find it interesting that Java sets the native win32 'Window Class' of
     * a JFrame to be the actual Java class name.
     * 
     * e.g. FindWindow(myjframe.getClass().getName(), myjframe.getTitle());
     * 
     * @param sClass  Name of the JFrame class.
     * @param sTitle  Caption of the window. 
     * @return The win32 HWND of the JFrame    */
    public native int FindWindow(String sClass, String sTitle);

    public native int InitAVIWrite();

    public int WriteAudio(byte[] ab, int i) {
        if (i == ab.length)
            return WriteAudio(ab);
        else
        {
            byte[] ab2 = jpsxdec.util.Misc.copyOfRange(ab, 0, i-1);
            return WriteAudio(ab2);
        }
    }

    public native int WriteFrame(byte[] abRGB);
    
    public native int WriteAudio(byte[] abSamples);

    @Override
    protected void finalize() throws Throwable {
        Close();
        super.finalize();
    }
    
}
