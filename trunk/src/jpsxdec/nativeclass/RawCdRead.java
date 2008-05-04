/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
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
 * RawCdRead.java
 */

package jpsxdec.nativeclass;

import java.io.IOException;
import jpsxdec.cdreaders.SectorReadErrorException;


public class RawCdRead {

    public final static int RAW_SECTOR_SIZE = 2352;
    
    public final static int TRUE = 1;
    public final static int FALSE = 0;
    
    // -- JNI interface errors -------------------------------------------------
    public final static int CD_IS_NULL = -99;
    public final static int BYTE_BUFFER_IS_NULL = -98;
    public final static int BYTE_BUFFER_IS_TOO_SMALL = -97;
    public final static int UNABLE_TO_GET_PRIMITIVEARRAYCRITICAL = -96;
    
    
    // -- CD reader class errors -----------------------------------------------
    // General
    public final static int NO_ERR                          = 0   ;
    public final static int ERR_CD_HANDLE_IS_NULL           = -999;
    
    // cd->Open()
    public final static int OK_NO_TRACKS_FOUND              = 100 ;
    public final static int ERR_DRIVE_IS_NOT_CD             = -100;
    public final static int ERR_GETTING_DRIVE_HANDLE        = -101;
    public final static int ERR_UNABLE_TO_LOCK_CD           = -102;
    public final static int ERR_UNABLE_TO_READ_TRACK_TABLE  = -103;
    
    // cd->GetTrackStart() and cd->GetTrackSize()
    public final static int ERR_TRACK_OUT_OF_BOUNDS         = -300;
    
    // cd->ReadSector()
    public final static int ERR_SECTOR_OUT_OF_BOUNDS        = -400;
    public final static int ERR_COUNT_OUT_OF_BOUNDS         = -401;
    public final static int ERR_UNABLE_TO_READ_SECTOR       = -500;
    // -------------------------------------------------------------------------


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
            System.loadLibrary("rawcdread");
        } else {
            throw new RuntimeException("RawCdRead only available on Windows platform.");
        }
    }
    
    private RawCdRead() {}
    
    private static native int _Open(char cDrive);
    public static void Open(char cDrive) throws IOException {
        int err = _Open(cDrive);
        switch (err) {
            case NO_ERR:             return;
            case OK_NO_TRACKS_FOUND: return;
            case ERR_DRIVE_IS_NOT_CD:             throw new IOException("Drive is not a CD.");
            case ERR_GETTING_DRIVE_HANDLE:        throw new IOException("Unable to acquire CD drive handle.");
            case ERR_UNABLE_TO_LOCK_CD:           throw new IOException("Unable to lock CD drive.");
            case ERR_UNABLE_TO_READ_TRACK_TABLE:  throw new IOException("Unable to read CD track table.");
            default:
                throw new IOException("Unknown CD Open() error " + err + ".");
        }
    }
    
    private static native int _IsOpened();
    public static boolean IsOpened() throws IOException {
        int err = _IsOpened();
        switch (err) {
            case TRUE: return true;
            case FALSE: return false;
            case CD_IS_NULL:
            case ERR_CD_HANDLE_IS_NULL: throw new IOException("Native CD pointer is null (probably because it is not open.");
            default:
                throw new IOException("Unknown CD IsOpened() error " + err + ".");
        }
    }
    
    private static native int _Close();
    public static void Close() throws IOException {
        int err = _Close();
        switch (err) {
            case NO_ERR: return;
            case CD_IS_NULL:
            case ERR_CD_HANDLE_IS_NULL: throw new IOException("Native CD pointer is null (probably because it is not open.");
            default:
                throw new IOException("Unknown CD Close() error " + err + ".");
        }
    }

    private static native int _IsCDReady( char cDrive );
    public static boolean IsCDReady(char cDrive) throws IOException {
        int err = _IsCDReady(cDrive);
        switch (err) {
            case TRUE: return true;
            case FALSE: return false;
            case CD_IS_NULL:
            case ERR_CD_HANDLE_IS_NULL: throw new IOException("Native CD pointer is null (probably because it is not open.");
            default:
                throw new IOException("Unknown CD IsCDReady() error " + err + ".");
        }
    }
    public static boolean IsCDReady() throws IOException {
        return IsCDReady('\0');
    }

    private static native int _GetTrackCount();
    public static int GetTrackCount() throws IOException {
        int err = _GetTrackCount();
        switch (err) {
            case CD_IS_NULL:
            case ERR_CD_HANDLE_IS_NULL: throw new IOException("Native CD pointer is null (probably because it is not open.");
            default:
                if (err < 0)
                    throw new IOException("Unknown CD GetTrackCount() error " + err + ".");
                else
                    return err;
        }
    }
    
    private static native int _GetTrackStart( int iTrack );
    public static int GetTrackStart(int iTrack) throws IOException {
        int err = _GetTrackStart(iTrack);
        switch (err) {
            case ERR_TRACK_OUT_OF_BOUNDS: throw new IOException("Invalid track number " + iTrack + ".");
            case CD_IS_NULL:
            case ERR_CD_HANDLE_IS_NULL: throw new IOException("Native CD pointer is null (probably because it is not open.");
            default:
                if (err < 0)
                    throw new IOException("Unknown CD GetTrackCount() error " + err + ".");
                else
                    return err;
        }
    }
    private static native int _GetTrackSize( int iTrack );
    public static int GetTrackSize(int iTrack) throws IOException {
        int err = _GetTrackSize(iTrack);
        switch (err) {
            case ERR_TRACK_OUT_OF_BOUNDS: throw new IOException("Invalid track number " + iTrack + ".");
            case CD_IS_NULL:
            case ERR_CD_HANDLE_IS_NULL: throw new IOException("Native CD pointer is null (probably because it is not open.");
            default:
                if (err < 0)
                    throw new IOException("Unknown CD GetTrackCount() error " + err + ".");
                else
                    return err;
        }
    }

    private static native int _ReadSector( int iSector, byte[] pcBuf, int iCount );
    public static void ReadSector(int iSector, byte[] pcBuf, int iCount) throws IOException {
        if (pcBuf == null) throw new IllegalArgumentException("Byte buffer cannot be null.");
        int err = _ReadSector(iSector, pcBuf, iCount);
        switch (err) {
            case BYTE_BUFFER_IS_NULL:      throw new IllegalArgumentException("Byte buffer cannot be null.");
            case BYTE_BUFFER_IS_TOO_SMALL: throw new IllegalArgumentException("Byte buffer is too small.");
            
            case CD_IS_NULL:
            case ERR_CD_HANDLE_IS_NULL: throw new IOException("Native CD pointer is null (probably because it is not open.");
            
            case NO_ERR:             return;
            case OK_NO_TRACKS_FOUND: return;
            case ERR_DRIVE_IS_NOT_CD:             throw new IOException("Drive is not a CD.");
            case ERR_GETTING_DRIVE_HANDLE:        throw new IOException("Unable to acquire CD drive handle.");
            case ERR_UNABLE_TO_LOCK_CD:           throw new IOException("Unable to lock CD drive.");
            case ERR_UNABLE_TO_READ_TRACK_TABLE:  throw new IOException("Unable to read CD track table.");
            case ERR_UNABLE_TO_READ_SECTOR:       throw new SectorReadErrorException("Unable to read sector " + iSector + ".");
            default:
                throw new IOException("Unknown CD ReadSector() error " + err + ".");
        }
    }
    public static void ReadSector(int iSector, byte[] pcBuf) throws IOException {
        ReadSector(iSector, pcBuf, 1);
    }
    
    private static native int _GetSectorCount();
    public static int GetSectorCount() throws IOException {
        int err = _GetSectorCount();
        switch (err) {
            case ERR_CD_HANDLE_IS_NULL: throw new IOException("Native CD pointer is null (probably because it is not open.");
            default:
                if (err < 0)
                    throw new IOException("Unknown CD GetSectorCount() error " + err + ".");
                else
                    return err;
        }
    }

    private static native void _UseASPI(boolean b);
    
}
