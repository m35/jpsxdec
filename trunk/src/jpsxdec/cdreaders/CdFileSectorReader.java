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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jpsxdec.sectors.SectorXA;
import jpsxdec.util.IO;
import jpsxdec.util.IOException6;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;

/** Encapsulates the reading of a CD image (BIN/CUE, ISO), 
 * or a file containing some (possibly raw) sectors of a CD.
 * The resulting data is mostly the same.
 * This class tries to guess what type of file it is. */
public class CdFileSectorReader {

    private static final Logger log = Logger.getLogger(CdFileSectorReader.class.getName());

    /** Normal sector data size: 2048. */
    public final static int SECTOR_SIZE_2048_ISO         = 2048;
    /** Raw sector without sync header: 2336. */
    public final static int SECTOR_SIZE_2336_BIN_NOSYNC  = 2336;
    /** Full raw sector: 2352. */
    public final static int SECTOR_SIZE_2352_BIN         = 2352;
    /** Normal sector data size: 2442. */
    public final static int SECTOR_SIZE_2448_BIN_SUBCHANNEL  = 2448;


    public final static int SECTOR_USER_DATA_SIZE_MODE1   = 2048;
    /** XA Audio sector: 2324. */
    public final static int SECTOR_USER_DATA_SIZE_MODE2   = 2324;

    public final static int SECTOR_USER_DATA_SIZE_CD_AUDIO   = 2352;

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private final RandomAccessFile _inputFile;
    private final File _sourceFile;
    /** Creates sectors from the data based on the type of disc image it is. */
    private final SectorCreator _sectorCreator;
    /** Number of full sectors in the disc image. */
    private final int _iSectorCount;

    private int _iTolerance;
    
    private int _iSectorsToBuffer;
    private byte[] _abBulkReadCache;
    private long _lngCacheFileOffset;
    private int _iCacheSector;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public CdFileSectorReader(File inputFile) throws IOException {
        this(inputFile, false, 1, 16);
    }

    public CdFileSectorReader(File inputFile, boolean b) throws IOException {
        this(inputFile, b, 1, 16);
    }

    /** Opens a CD file for reading. Tries to guess the CD size. */
    public CdFileSectorReader(File sourceFile,
            boolean blnAllowWrites,
            int iTolerance, int iSectorsToBuffer)
            throws IOException
    {
        log.info(sourceFile.getPath());

        _iTolerance = iTolerance;
        _sourceFile = sourceFile;
        _iSectorsToBuffer = iSectorsToBuffer;

        _inputFile = new RandomAccessFile(sourceFile, blnAllowWrites ? "rw" : "r");

        SectorCreator creator;

        try {
            creator = new Cd2352or2448(_inputFile, true /*2352*/, true /*2448*/);
            log.info("Disc type identified as " + creator.getTypeDescription());
        } catch (NotThisTypeException ex) {
            try {
                creator = new Cd2336(_inputFile);
                log.info("Disc type identified as " + creator.getTypeDescription());
            } catch (NotThisTypeException ex1) {
                // we couldn't figure out what it is, assuming ISO style
                creator = new Cd2048();
                log.info("Unknown disc type, assuming " + creator.getTypeDescription());
            }
        }
        
        _sectorCreator = creator;

        _iSectorCount = calculateSectorCount();
    }

    /** Opens a CD file for reading using the provided sector size. 
     * If the disc image doesn't match the sector size, IOException is thrown.
     */
    public CdFileSectorReader(File sourceFile,
            int iSectorSize, boolean blnAllowWrites,
            int iTolerance, int iSectorsToBuffer)
            throws IOException
    {
        log.info(sourceFile.getPath());

        _iTolerance = iTolerance;
        _sourceFile = sourceFile;
        _iSectorsToBuffer = iSectorsToBuffer;

        _inputFile = new RandomAccessFile(sourceFile, blnAllowWrites ? "rw" : "r");

        try {
            switch (iSectorSize) {
                case SECTOR_SIZE_2048_ISO:
                    _sectorCreator = new Cd2048();
                    break;
                case SECTOR_SIZE_2336_BIN_NOSYNC:
                    _sectorCreator = new Cd2336(_inputFile);
                    break;
                case SECTOR_SIZE_2352_BIN:
                    _sectorCreator = new Cd2352or2448(_inputFile, true /*2352*/, false /*2448*/);
                    break;
                case SECTOR_SIZE_2448_BIN_SUBCHANNEL:
                    _sectorCreator = new Cd2352or2448(_inputFile, false /*2352*/, true /*2448*/);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid sector size to open disc image as " + iSectorSize);
            }
        } catch (NotThisTypeException ex) {
            throw new IOException6(ex);
        }

        _iSectorCount = calculateSectorCount();
    }

    public CdFileSectorReader(String sSerialization, boolean blnAllowWrites)
            throws IOException, NotThisTypeException
    {
        this(sSerialization, blnAllowWrites, 1, 16);
    }

    public CdFileSectorReader(String sSerialization, boolean blnAllowWrites,
                              int iTolerance, int iSectorsToBuffer)
                              throws IOException, NotThisTypeException
    {
        String[] asValues = Misc.regex(Pattern.compile("Filename:([^|]+)\\|Sector size:(\\d+)\\|Sector count:(\\d+)\\|First sector offset:(\\d+)"), sSerialization);
        if (asValues == null || asValues.length != 5)
            throw new NotThisTypeException("Failed to deserialize CD string: " + sSerialization);

        try {
            _iSectorCount = Integer.parseInt(asValues[3]);
            long lngStartOffset = Long.parseLong(asValues[4]);
            int iSectorSize = Integer.parseInt(asValues[2]);

            switch (iSectorSize) {
                case SECTOR_SIZE_2048_ISO:
                    _sectorCreator = new Cd2048(lngStartOffset);
                    break;
                case SECTOR_SIZE_2336_BIN_NOSYNC:
                    _sectorCreator = new Cd2336(lngStartOffset);
                    break;
                case SECTOR_SIZE_2352_BIN:
                    _sectorCreator = new Cd2352or2448(true, lngStartOffset);
                    break;
                case SECTOR_SIZE_2448_BIN_SUBCHANNEL:
                    _sectorCreator = new Cd2352or2448(false, lngStartOffset);
                    break;
                default:
                    throw new NotThisTypeException();
            }
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }

        _sourceFile = new File(asValues[1]);

        _inputFile = new RandomAccessFile(_sourceFile, blnAllowWrites ? "rw" : "r");

        _iSectorsToBuffer = iSectorsToBuffer;
        _iTolerance = iTolerance;

        int iActualSectorCount = calculateSectorCount();
        if (_iSectorCount != iActualSectorCount) {
            _inputFile.close();
            throw new NotThisTypeException(String.format(
                    "Serialized sector count (%d) does not match actual (%d)",
                    _iSectorCount, iActualSectorCount));
        }

    }

    private int calculateSectorCount() throws IOException {
        return (int)((_inputFile.length() - _sectorCreator.get1stSectorOffset())
                      / _sectorCreator.getRawSectorSize());
    }

    public final static String SERIALIZATION_START = "Filename:";

    public String serialize() {
        return String.format(SERIALIZATION_START + "%s|Sector size:%d|Sector count:%d|First sector offset:%d",
                _sourceFile.getPath(),
                _sectorCreator.getRawSectorSize(),
                _iSectorCount,
                _sectorCreator.get1stSectorOffset());
    }



    public void close() throws IOException {
        _inputFile.close();
    }

    //..........................................................................

    public int getSectorSize() {
        return _sectorCreator.getRawSectorSize();
    }

    public boolean hasSectorHeader() {
        return _sectorCreator.hasSectorHeader();
    }

    public File getSourceFile() {
        return _sourceFile;
    }

    /** Returns the actual offset in bytes from the start of the file/CD 
     *  to the start of iSector. */
    public long getFilePointer(int iSector) {
        return iSector * _sectorCreator.getRawSectorSize() + _sectorCreator.get1stSectorOffset();
    }

    /** Returns the number of sectors in the file/CD */
    public int getLength() {
        return _iSectorCount;
    }

    public String getTypeDescription() {
        return _sectorCreator.getTypeDescription();
    }

    //..........................................................................

    public CdSector getSector(int iSector) throws IOException {
        if (iSector < 0 || iSector >= _iSectorCount)
            throw new IndexOutOfBoundsException("Sector not in bounds of CD");


        if (iSector >= _iCacheSector + _iSectorsToBuffer || iSector < _iCacheSector || _abBulkReadCache == null) {
            _abBulkReadCache = new byte[_sectorCreator.getRawSectorSize() * _iSectorsToBuffer];
            _iCacheSector = iSector;
            _lngCacheFileOffset = getFilePointer(iSector);

            _inputFile.seek(_lngCacheFileOffset);
            int iBytesRead = _inputFile.read(_abBulkReadCache);
            if (iBytesRead < _sectorCreator.getRawSectorSize()) {
                throw new IllegalStateException("Failed to read at least 1 entire sector.");
            }
        }

        int iOffset = _sectorCreator.getRawSectorSize() * (iSector - _iCacheSector);

        try {
            return _sectorCreator.createSector(iSector, _abBulkReadCache, iOffset, _lngCacheFileOffset + iOffset, _iTolerance);
        } catch (NotThisTypeException ex) {
            throw new SectorReadErrorException("Error reading sector " + iSector + ": " + ex.getMessage());
        }
    }

    //..........................................................................

    /** Will fail if CD was not opened with write access. */
    public void writeSector(int iSector, byte[] abSrcUserData)
            throws IOException
    {

        CdSector oSect = getSector(iSector);

        if (oSect.getCdUserDataSize() != abSrcUserData.length)
            throw new IllegalArgumentException("Data to write is not the right size.");

        long lngUserDataOfs = oSect.getFilePointer();

        _inputFile.seek(lngUserDataOfs);
        _inputFile.write(abSrcUserData);
    }

    //..........................................................................

    @Override
    public String toString() {
        return serialize();
    }

    /* ---------------------------------------------------------------------- */
    /* Sector Creator types ------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    private interface SectorCreator {
        CdSector createSector(int iSector, byte[] abSectorBuff, int iOffset, long lngFilePointer, int iTolerance) throws NotThisTypeException;
        String getTypeDescription();
        boolean hasSectorHeader();
        long get1stSectorOffset();
        int getRawSectorSize();
    }

    private static class Cd2048 implements SectorCreator {

        final private long _lng1stSectorOffset;

        public Cd2048() {
            _lng1stSectorOffset = 0;
        }

        public Cd2048(long lngStartOffset) {
            _lng1stSectorOffset = lngStartOffset;
        }

        public CdSector createSector(int iSector, byte[] abSectorBuff, int iOffset, long lngFilePointer, int iTolerance) {
            return new CdSector2048(abSectorBuff, iOffset, iSector, iTolerance);
        }


        public String getTypeDescription() {
            return ".iso (2048 bytes/sector) format";
        }

        public boolean hasSectorHeader() {
            return false;
        }

        public long get1stSectorOffset() {
            return _lng1stSectorOffset;
        }

        public int getRawSectorSize() {
            return SECTOR_SIZE_2048_ISO;
        }
    }
    
    private static class Cd2336 implements SectorCreator {

        private long _lng1stSectorOffset;

        /** Searches through the first 33 sectors for a full XA audio sector.
         *<p>
         *  Note: This assumes the input file has the data aligned at every 4 bytes!
         */
        public Cd2336(RandomAccessFile cdFile) throws IOException, NotThisTypeException {
            if (cdFile.length() < SECTOR_SIZE_2336_BIN_NOSYNC)
                throw new NotThisTypeException();

            // TODO: With the new api I can read the whole test block at once
            byte[] abTestSectorData = new byte[SECTOR_SIZE_2336_BIN_NOSYNC];

            // only search up to 33 sectors into the file
            // because that's the maximum XA audio span
            // (this misses audio that starts later in the file however)
            int iMaxSearch = SECTOR_SIZE_2336_BIN_NOSYNC * 33;
            if (iMaxSearch > cdFile.length())
                iMaxSearch = (int) cdFile.length();

            // Only detect XA ADPCM audio sectors to determine if it's SECTOR_MODE2
            for (long lngSectStart = 0;
                 lngSectStart < iMaxSearch - abTestSectorData.length;
                 lngSectStart+=4)
            {
                try {
                    cdFile.seek(lngSectStart);
                    IO.readByteArray(cdFile, abTestSectorData);
                    CdSector cdSector = new CdSector2336(abTestSectorData, 0, -1, -1, 0);
                    SectorXA xaSector = new SectorXA(cdSector);
                    // we've found an XA audio sector
                    // maybe try to find another just to be sure?

                    // only check up to 146 sectors because, if the sector size is actually 2352,
                    // then around 147, the offset difference adds up to another
                    // whole 2352 sector
                    for (long lngAdditionalOffset = SECTOR_SIZE_2336_BIN_NOSYNC, iTimes = 1;
                         lngSectStart + lngAdditionalOffset < cdFile.length() - abTestSectorData.length &&
                         iTimes < 146;
                         lngAdditionalOffset+=SECTOR_SIZE_2336_BIN_NOSYNC,
                         iTimes++)
                    {
                        cdFile.seek(lngSectStart + lngAdditionalOffset);
                        IO.readByteArray(cdFile, abTestSectorData, 0, SECTOR_SIZE_2336_BIN_NOSYNC);
                        try {
                            xaSector = new SectorXA(cdSector);

                            // sweet, we found another one. we're done.
                            // backup to the first sector
                            _lng1stSectorOffset = lngSectStart % SECTOR_SIZE_2336_BIN_NOSYNC;
                            return;
                        } catch (NotThisTypeException ex) {
                        }
                    }
                } catch (NotThisTypeException ex) {

                }
            }
            throw new NotThisTypeException();
        }

        private Cd2336(long lngStartOffset) {
            _lng1stSectorOffset = lngStartOffset;
        }

        public CdSector createSector(int iSector, byte[] abSectorBuff, int iOffset, long lngFilePointer, int iTolerance) throws NotThisTypeException {
            return new CdSector2336(abSectorBuff, iOffset, iSector, lngFilePointer, iTolerance);
        }

        public String getTypeDescription() {
            return "partial header (2336 bytes/sector) format";
        }
        public boolean hasSectorHeader() {
            return true;
        }

        public long get1stSectorOffset() {
            return _lng1stSectorOffset;
        }

        public int getRawSectorSize() {
            return SECTOR_SIZE_2336_BIN_NOSYNC;
        }
    }

    private static class Cd2352or2448 implements SectorCreator {

        private final long _lng1stSectorOffset;
        private final boolean _bln2352;

        /** Searches through the first SECTOR_RAW_AUDIO*2 bytes
         *  for a CD sync mark.
         */
        public Cd2352or2448(RandomAccessFile cdFile, boolean blnCheck2352, boolean blnCheck2448) throws IOException, NotThisTypeException {
            byte[] abSyncHeader = new byte[CdxaHeader.SECTOR_SYNC_HEADER.length];
            
            for (long lngSectStart = 0;
                 lngSectStart < SECTOR_SIZE_2352_BIN * 2;
                 lngSectStart++)
            {
                cdFile.seek(lngSectStart);
                IO.readByteArray(cdFile, abSyncHeader);
                if (Arrays.equals(abSyncHeader, CdxaHeader.SECTOR_SYNC_HEADER)) {
                    log.info("Possible sync header at " + lngSectStart);
                    // we think we found a sync header
                    if (blnCheck2352 && checkMore(SECTOR_SIZE_2352_BIN, cdFile, lngSectStart, abSyncHeader)) {
                        _bln2352 = true;
                        _lng1stSectorOffset = lngSectStart % SECTOR_SIZE_2352_BIN;
                        return;
                    } else if (blnCheck2448 && checkMore(SECTOR_SIZE_2448_BIN_SUBCHANNEL, cdFile, lngSectStart, abSyncHeader)) {
                        _bln2352 = false;
                        _lng1stSectorOffset = lngSectStart % SECTOR_SIZE_2448_BIN_SUBCHANNEL;
                        return;
                    }
                }
            }
            throw new NotThisTypeException();
        }

        private static boolean checkMore(int iSectorSize, RandomAccessFile cdFile, long lngSectStart, byte[] abSyncHeader) throws IOException {
            // check for 10 more seek headers after this one to be sure
            long lngSectorsToTry = Math.min(10, (cdFile.length()-lngSectStart-CdxaHeader.SECTOR_SYNC_HEADER.length) / SECTOR_SIZE_2352_BIN);
            for (int i = 0, iOfs = iSectorSize;
                 i < lngSectorsToTry;
                 i++, iOfs+=iSectorSize)
            {
                cdFile.seek(lngSectStart + iOfs);
                IO.readByteArray(cdFile, abSyncHeader);
                if (!Arrays.equals(abSyncHeader, CdxaHeader.SECTOR_SYNC_HEADER))
                    return false; // aw, too bad, back to the drawing board
            }
            return true;
        }

        private Cd2352or2448(boolean blnIs2352, long lngStartOffset) {
            _bln2352 = blnIs2352;
            _lng1stSectorOffset = lngStartOffset;
        }

        public CdSector createSector(int iSector, byte[] abSectorBuff, int iOffset, long lngFilePointer, int iTolerance) throws NotThisTypeException {
            return new CdSector2352(abSectorBuff, iOffset, iSector, lngFilePointer, iTolerance);
        }

        public String getTypeDescription() {
            return _bln2352 ? 
                "BIN/CUE (2352 bytes/sector) format" :
                "BIN/CUE + Sub Channel (2448 bytes/sector) format";
        }
        public boolean hasSectorHeader() {
            return true;
        }

        public long get1stSectorOffset() {
            return _lng1stSectorOffset;
        }

        public int getRawSectorSize() {
            return _bln2352 ?
                    SECTOR_SIZE_2352_BIN :
                    SECTOR_SIZE_2448_BIN_SUBCHANNEL;
        }
    }


}
