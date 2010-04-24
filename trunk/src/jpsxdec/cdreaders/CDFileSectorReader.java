/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.cdreaders;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;
import jpsxdec.modules.xa.JPSXModuleXAAudio;
import jpsxdec.modules.xa.SectorXA;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** Reads a CD image (BIN/CUE, ISO), or a file containing some sectors of a CD 
 *  as a CD. This class does its best to guess what type of file it is. */
public class CDFileSectorReader extends CDSectorReader {

    private static final Logger log = Logger.getLogger(CDFileSectorReader.class.getName());
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    private RandomAccessFile _inputFile;
    private String _sSourceFilePath;
    private long _lngFirstSectorOffset = -1;
    private int _iRawSectorTypeSize = -1;
    private int _iSectorCount = -1;
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Opens a CD file for reading only. */
    public CDFileSectorReader(String sFile) throws IOException {
        this(sFile, -1, false);
    }

    public CDFileSectorReader(String sFile, int iSectorSize) throws IOException {
        this(sFile, iSectorSize, false);
    }

    public CDFileSectorReader(String sFile, boolean blnAllowWrites) throws IOException {
        this(sFile, -1, blnAllowWrites);
    }

    /** Opens a CD file for reading, with the option of allowing writing. */
    public CDFileSectorReader(String sFile, int iSectorSize, boolean blnAllowWrites)
            throws IOException 
    {
        _sSourceFilePath = new File(sFile).getPath();
        log.info(_sSourceFilePath);
        if (blnAllowWrites)
            _inputFile = new RandomAccessFile(sFile, "rw");
        else
            _inputFile = new RandomAccessFile(sFile, "r");

        switch (iSectorSize) {
            case CDSector.SECTOR_MODE1_OR_MODE2_FORM1:
                _iRawSectorTypeSize = iSectorSize;
                break;
            case CDSector.SECTOR_MODE2:
                test2336();
                _iRawSectorTypeSize = iSectorSize;
                break;
            case CDSector.SECTOR_RAW_AUDIO:
                test2352();
                _iRawSectorTypeSize = iSectorSize;
                break;
            default:
                // Step 1, search for FFFFFF sync header within the first SECTOR_RAW_AUDIO + a few bytes
                // If found, jump though X sectors to make sure there are regular sync headers
                // if not found, we can assume it's not SECTOR_RAW_AUDIO
                // that will remove the chance of many false positives
                if (test2352()) break;
                if (test2336()) break;
        }

        if (_lngFirstSectorOffset < 0) {
            // we couldn't figure out what it is, assuming ISO style
            log.info("Unknown disc type, assuming ISO sector size: " + CDSector.SECTOR_MODE1_OR_MODE2_FORM1);
            _lngFirstSectorOffset = 0;
            _iRawSectorTypeSize = CDSector.SECTOR_MODE1_OR_MODE2_FORM1;
        } else if (_lngFirstSectorOffset > 0) {
            log.info("Disc type identified, sector size: " + _iRawSectorTypeSize);
            // Back up to the first sector in case we matched at the second sector
            _lngFirstSectorOffset =
                    _lngFirstSectorOffset % _iRawSectorTypeSize;
        }

        _iSectorCount = (int)((_inputFile.length() - _lngFirstSectorOffset)
                                / _iRawSectorTypeSize);
    }

    public void close() throws IOException {
        _inputFile.close();
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public String getSourceFile() {
        return _sSourceFilePath;
    }

    @Override
    public String getSourceFileBaseName() {
        return new File(_sSourceFilePath).getName();
    }

    //..........................................................................

    /** Returns the actual offset in bytes from the start of the file/CD 
     *  to the start of iSector. */
    public long getFilePointer(int iSector) {
        return iSector * _iRawSectorTypeSize + _lngFirstSectorOffset ;
    }

    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public boolean hasSectorHeader() {
        switch (_iRawSectorTypeSize) {
            case CDSector.SECTOR_MODE1_OR_MODE2_FORM1: // 2048
                return false;
            case CDSector.SECTOR_MODE2:         // 2336
                return true;
            case CDSector.SECTOR_RAW_AUDIO:     // 2352
                return true;
            default: 
                throw new RuntimeException("Should never happen: what kind of sector size is this?");
        }
    }
    
    //..........................................................................

    /** Returns the number of sectors in the file/CD */
    public int size() {
        return _iSectorCount;
    }
    
    //..........................................................................
    
    /** Returns the requested sector. */
    public CDSector getSector(int iSector) 
            throws IOException, IndexOutOfBoundsException 
    {
        if (iSector < 0 || iSector >= _iSectorCount)
            throw new IndexOutOfBoundsException("Sector not in bounds of CD");
        
        byte abSectorBuff[] = new byte[_iRawSectorTypeSize];
        int iBytesRead = 0;
        
        long lngFileOffset = _lngFirstSectorOffset
                          + _iRawSectorTypeSize * iSector;
        
        // in the very unlikely case this class is ever used in a
        // multi-threaded environment, this is the only part
        // that needs to be syncronized.
        synchronized(this) {
            _inputFile.seek(lngFileOffset);
            iBytesRead = _inputFile.read(abSectorBuff);
        }

        if (iBytesRead != abSectorBuff.length) {
            // if we only got part of a sector
        }
        
        try {
            return new CDSector(_iRawSectorTypeSize, abSectorBuff, iSector, lngFileOffset);
        } catch (NotThisTypeException ex) {
            // unable to create a CDXA sector from the data.
            // Some possible causes:
            //  - It's a raw CD audio sector
            //  - At the end of the CD and the last sector is incomplete?
            //  - XA audio data is incorrect (corrupted)
            throw new SectorReadErrorException("Sector " + iSector + " appears to be corrupted: " + ex.getMessage());
        }
    }
    
    public void writeSector(int iSector, byte[] abSrcUserData) 
            throws IOException 
    {
        
        CDSector oSect = getSector(iSector);
        
        if (oSect.getCdUserDataSize() != abSrcUserData.length)
            throw new IOException("Data to write is not the right size");
        
        long lngUserDataOfs = oSect.getFilePointer();
        
        synchronized(this) {
            _inputFile.seek(lngUserDataOfs);
            _inputFile.write(abSrcUserData);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Searches through the first 33 sectors for a full XA audio sector.
     *<p>
     *  Note: This assumes the input file has the data aligned at every 4 bytes!
     */
    private boolean test2336() throws IOException {

        byte abTestSectorData[] = new byte[CDSector.SECTOR_MODE2];

        // only search up to 33 sectors into the file
        // because that's the maximum XA audio span
        // (this misses audio that starts later in the file however)
        int iMaxSearch = CDSector.SECTOR_MODE2 * 33;

        // Only detect XA ADPCM audio sectors to determine if it's SECTOR_MODE2
        for (long lngSectStart = 0;
             lngSectStart < iMaxSearch - abTestSectorData.length;
             lngSectStart+=4)
        {
            try {
                _inputFile.seek(lngSectStart);
                IO.readByteArray(_inputFile, abTestSectorData);
                CDSector oCDSect = new CDSector(CDSector.SECTOR_MODE2, abTestSectorData, 0, -1);
                IdentifiedSector oPSXSect = JPSXModuleXAAudio.getModule().identifySector(oCDSect);
                if (oPSXSect instanceof SectorXA) {
                    // we've found an XA audio sector
                    // maybe try to find another just to be sure?

                    // only check 146 sectors because, if the sector size was 2352,
                    // then around 147, the offset difference adds up to another
                    // whole 2352 sector
                    for (long lngAdditionalOffset = 0, iTimes = 0;
                         lngSectStart + lngAdditionalOffset < _inputFile.length() - abTestSectorData.length &&
                         iTimes < 146;
                         lngAdditionalOffset+=CDSector.SECTOR_MODE2,
                         iTimes++)
                    {
                        _inputFile.seek(lngSectStart + lngAdditionalOffset);
                        IO.readByteArray(_inputFile, abTestSectorData, 0, CDSector.SECTOR_MODE2);
                        oPSXSect = JPSXModuleXAAudio.getModule().identifySector(oCDSect);
                        if (oPSXSect instanceof SectorXA) {
                            _lngFirstSectorOffset = lngSectStart;
                            _iRawSectorTypeSize = CDSector.SECTOR_MODE2;
                            return true;
                        }
                    }
                }
            } catch (NotThisTypeException ex) {

            }
        }
        return false;
    }

    /** Searches through the first SECTOR_RAW_AUDIO*2 bytes
     *  for a CD sync mark.
     *<p>
     *  Note: This assumes the input file has the data aligned at every 4 bytes!
     */
    private boolean test2352() throws IOException {
        byte[] abSyncHeader = new byte[CDSector.SECTOR_SYNC_HEADER.length];

        OuterSearch:
        for (long lngSectStart = 0;
             lngSectStart < CDSector.SECTOR_RAW_AUDIO * 2;
             lngSectStart++)
        {
            _inputFile.seek(lngSectStart);
            IO.readByteArray(_inputFile, abSyncHeader);
            if (Arrays.equals(abSyncHeader, CDSector.SECTOR_SYNC_HEADER)) {
                // we think we found a sync header
                // check for 10 more seek headers after this one to be sure
                long lngSectorsToTry = Math.min(10, (_inputFile.length()-lngSectStart-abSyncHeader.length) / CDSector.SECTOR_RAW_AUDIO);
                for (int i = 0, iOfs = CDSector.SECTOR_RAW_AUDIO;
                     i < lngSectorsToTry;
                     i++, iOfs+=CDSector.SECTOR_RAW_AUDIO)
                {
                    _inputFile.seek(lngSectStart + iOfs);
                    IO.readByteArray(_inputFile, abSyncHeader);
                    if (!Arrays.equals(abSyncHeader, CDSector.SECTOR_SYNC_HEADER))
                        continue OuterSearch; // aw, too bad, back to the drawing board
                }
                _lngFirstSectorOffset = lngSectStart;
                _iRawSectorTypeSize = CDSector.SECTOR_RAW_AUDIO;
                log.info("Found sync header at " + lngSectStart);
                return true;
            }
        }
        return false;
    }

    public String serialize() {
        return String.format("Filename:%s|Sector size:%d|Sector count:%d|First sector offset:%d",
                _sSourceFilePath,
                _iRawSectorTypeSize,
                _iSectorCount,
                _lngFirstSectorOffset);
    }

    @Override
    public String toString() {
        return serialize();
    }

    @Override
    public String getTypeDescription() {
        switch (_iRawSectorTypeSize) {
            case CDSector.SECTOR_MODE1_OR_MODE2_FORM1: // 2048
                return ".iso (2048 bytes/sector) format";
            case CDSector.SECTOR_MODE2:         // 2336
                return "partial header (2336 bytes/sector) format";
            case CDSector.SECTOR_RAW_AUDIO:     // 2352
                return "BIN/CUE (2352 bytes/sector) format";
            default:
                throw new RuntimeException("Should never happen: what kind of sector size is this?");
        }
    }
    
    

}
