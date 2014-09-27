/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.I18N;
import jpsxdec.LocalizedIOException;
import jpsxdec.LocalizedMessage;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;

/** Encapsulates the reading of a CD image (BIN/CUE, ISO), 
 * or a file containing some (possibly raw) sectors of a CD.
 * The resulting data is mostly the same.
 * This class tries to guess what type of file it is.
 * <ul>
 * <li>{@link #SECTOR_SIZE_2048_ISO}
 * <li>{@link #SECTOR_SIZE_2336_BIN_NOSYNC}
 * <li>{@link #SECTOR_SIZE_2352_BIN}
 * <li>{@link #SECTOR_SIZE_2448_BIN_SUBCHANNEL}
 * </ul>
 */
public class CdFileSectorReader {

    private static final Logger LOG = Logger.getLogger(CdFileSectorReader.class.getName());

    /** Normal iso sector data size: 2048. */
    public final static int SECTOR_SIZE_2048_ISO            = 2048;
    /** Raw sector without sync header: 2336. */
    public final static int SECTOR_SIZE_2336_BIN_NOSYNC     = 2336;
    /** Full raw sector: 2352. */
    public final static int SECTOR_SIZE_2352_BIN            = 2352;
    /** Full raw sector with sub-channel data: 2442. */
    public final static int SECTOR_SIZE_2448_BIN_SUBCHANNEL = 2448;


    /** Data sector payload size for and Mode 2 Form 1: 2048. */
    public final static int SECTOR_USER_DATA_SIZE_FORM1    = 2048;
    /** Payload size for and Mode 2 Form 2 (usually XA audio): 2324. */
    public final static int SECTOR_USER_DATA_SIZE_FORM2    = 2324;
    /** CD audio sector payload size: 2352. */
    public final static int SECTOR_USER_DATA_SIZE_CD_AUDIO = 2352;
    
    private static final int DEFAULT_SECTOR_BUFFER_COUNT   = 16;

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private RandomAccessFile _inputFile;
    private final File _sourceFile;
    /** Creates sectors from the data based on the type of disc image it is. */
    private final SectorFactory _sectorFactory;
    /** Number of full sectors in the disc image. */
    private final int _iSectorCount;

    private int _iCachedSectorStart;
    private int _iSectorsToCache;
    private byte[] _abBulkReadCache;
    private long _lngCacheFileOffset;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public CdFileSectorReader(File inputFile)
        throws IOException, CdFileNotFoundException
    {
        this(inputFile, false, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    public CdFileSectorReader(File inputFile, boolean blnAllowWrites)
        throws IOException, CdFileNotFoundException
    {

        this(inputFile, blnAllowWrites, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    public CdFileSectorReader(File inputFile, int iSectorSize) 
            throws IOException, CdFileNotFoundException
    {
        this(inputFile, iSectorSize, false, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    /** Opens a CD file for reading. Tries to guess the CD size. */
    public CdFileSectorReader(File sourceFile,
            boolean blnAllowWrites, int iSectorsToBuffer)
            throws IOException, CdFileNotFoundException
    {
        LOG.info(sourceFile.getPath());

        _sourceFile = sourceFile;
        _iSectorsToCache = iSectorsToBuffer;

        try {
            _inputFile = new RandomAccessFile(sourceFile, blnAllowWrites ? "rw" : "r");
        } catch (FileNotFoundException ex) {
            throw new CdFileNotFoundException(sourceFile);
        }

        SectorFactory factory;

        try {
            factory = new Cd2352or2448Factory(_inputFile, true /*2352*/, true /*2448*/);
            LOG.log(Level.INFO, "Disc type identified as {0}", factory.getTypeDescription());
        } catch (NotThisTypeException ex) {
            try {
                factory = new Cd2336Factory(_inputFile);
                LOG.log(Level.INFO, "Disc type identified as {0}", factory.getTypeDescription());
            } catch (NotThisTypeException ex1) {
                // we couldn't figure out what it is, assuming ISO style
                factory = new Cd2048Factory();
                LOG.log(Level.INFO, "Unknown disc type, assuming {0}", factory.getTypeDescription());
            }
        }
        
        _sectorFactory = factory;

        _iSectorCount = calculateSectorCount();
    }

    /** Opens a CD file for reading using the provided sector size. 
     * If the disc image doesn't match the sector size, IOException is thrown.
     */
    public CdFileSectorReader(File sourceFile,
            int iSectorSize, boolean blnAllowWrites, int iSectorsToBuffer)
            throws IOException
    {
        LOG.info(sourceFile.getPath());

        _sourceFile = sourceFile;
        _iSectorsToCache = iSectorsToBuffer;

        _inputFile = new RandomAccessFile(sourceFile, blnAllowWrites ? "rw" : "r");

        try {
            switch (iSectorSize) {
                case SECTOR_SIZE_2048_ISO:
                    _sectorFactory = new Cd2048Factory();
                    break;
                case SECTOR_SIZE_2336_BIN_NOSYNC:
                    _sectorFactory = new Cd2336Factory(_inputFile);
                    break;
                case SECTOR_SIZE_2352_BIN:
                    _sectorFactory = new Cd2352or2448Factory(_inputFile, true /*2352*/, false /*2448*/);
                    break;
                case SECTOR_SIZE_2448_BIN_SUBCHANNEL:
                    _sectorFactory = new Cd2352or2448Factory(_inputFile, false /*2352*/, true /*2448*/);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid sector size to open disc image as " + iSectorSize);
            }
        } catch (NotThisTypeException ex) {
            throw new LocalizedIOException(ex);
        }

        _iSectorCount = calculateSectorCount();
    }

    public CdFileSectorReader(String sSerialization, boolean blnAllowWrites)
            throws IOException, NotThisTypeException
    {
        this(sSerialization, blnAllowWrites, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    public CdFileSectorReader(String sSerialization, boolean blnAllowWrites, int iSectorsToBuffer)
            throws IOException, NotThisTypeException
    {
        String[] asValues = Misc.regex(DESERIALIZATION, sSerialization);
        if (asValues == null || asValues.length != 5)
            throw new NotThisTypeException("Failed to deserialize CD string: {0}", sSerialization); // I18N

        try {
            _iSectorCount = Integer.parseInt(asValues[3]);
            long lngStartOffset = Long.parseLong(asValues[4]);
            int iSectorSize = Integer.parseInt(asValues[2]);

            switch (iSectorSize) {
                case SECTOR_SIZE_2048_ISO:
                    _sectorFactory = new Cd2048Factory(lngStartOffset);
                    break;
                case SECTOR_SIZE_2336_BIN_NOSYNC:
                    _sectorFactory = new Cd2336Factory(lngStartOffset);
                    break;
                case SECTOR_SIZE_2352_BIN:
                    _sectorFactory = new Cd2352or2448Factory(true, lngStartOffset);
                    break;
                case SECTOR_SIZE_2448_BIN_SUBCHANNEL:
                    _sectorFactory = new Cd2352or2448Factory(false, lngStartOffset);
                    break;
                default:
                    throw new NotThisTypeException();
            }
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }

        _sourceFile = new File(asValues[1]);

        _inputFile = new RandomAccessFile(_sourceFile, blnAllowWrites ? "rw" : "r");

        _iSectorsToCache = iSectorsToBuffer;

        int iActualSectorCount = calculateSectorCount();
        if (_iSectorCount != iActualSectorCount) {
            _inputFile.close();
            throw new NotThisTypeException("Serialized sector count {0,number,#} does not match actual {1,number,#}", // I18N
                    _iSectorCount, iActualSectorCount);
        }

    }

    private int calculateSectorCount() throws IOException {
        return (int)((_inputFile.length() - _sectorFactory.get1stSectorOffset())
                      / _sectorFactory.getRawSectorSize());
    }

    public final static String SERIALIZATION_START = "Filename:";

    private static final String DESERIALIZATION =
            SERIALIZATION_START + "([^|]+)\\|Sector size:(\\d+)\\|Sector count:(\\d+)\\|First sector offset:(\\d+)";

    private static final String SERIALIZATION =
            SERIALIZATION_START + "%s|Sector size:%d|Sector count:%d|First sector offset:%d";

    public String serialize() {
        return String.format(SERIALIZATION,
                _sourceFile.getPath(),
                _sectorFactory.getRawSectorSize(),
                _iSectorCount,
                _sectorFactory.get1stSectorOffset());
    }

    public boolean matchesSerialization(String sSerialization) {
        String[] asValues = Misc.regex(DESERIALIZATION, sSerialization);
        if (asValues == null)
            return false;

        try {
            int iSectorSize = Integer.parseInt(asValues[2]);
            int iSectorCount = Integer.parseInt(asValues[3]);
            long lngStartOffset = Long.parseLong(asValues[4]);

            return iSectorCount == _iSectorCount &&
                   lngStartOffset == _sectorFactory.get1stSectorOffset() &&
                   iSectorSize == _sectorFactory.getRawSectorSize();
        } catch (NumberFormatException ex) {
            return false;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return false;
        }

    }

    public void close() throws IOException {
        _inputFile.close();
    }

    //..........................................................................

    public int getSectorSize() {
        return _sectorFactory.getRawSectorSize();
    }

    public boolean hasSectorHeader() {
        return _sectorFactory.hasSectorHeader();
    }

    public File getSourceFile() {
        return _sourceFile;
    }

    /** Returns the actual offset in bytes from the start of the file/CD 
     *  to the start of iSector. */
    public long getFilePointer(int iSector) {
        return (long)iSector * _sectorFactory.getRawSectorSize() + _sectorFactory.get1stSectorOffset();
    }

    /** Returns the number of sectors in the file/CD */
    public int getLength() {
        return _iSectorCount;
    }

    public LocalizedMessage getTypeDescription() {
        return _sectorFactory.getTypeDescription();
    }

    //..........................................................................

    public CdSector getSector(int iSector) throws IOException {
        if (iSector < 0 || iSector >= _iSectorCount)
            throw new IndexOutOfBoundsException("Sector "+iSector+" not in bounds of CD");


        if (iSector >= _iCachedSectorStart + _iSectorsToCache || iSector < _iCachedSectorStart || _abBulkReadCache == null) {
            _abBulkReadCache = new byte[_sectorFactory.getRawSectorSize() * _iSectorsToCache];
            _iCachedSectorStart = iSector;
            _lngCacheFileOffset = getFilePointer(iSector);

            _inputFile.seek(_lngCacheFileOffset);
            int iBytesRead = _inputFile.read(_abBulkReadCache);
            if (iBytesRead < _sectorFactory.getRawSectorSize()) {
                _abBulkReadCache = null;
                throw new LocalizedIOException("Failed to read at least 1 entire sector."); // I18N
            }
        }

        int iOffset = _sectorFactory.getRawSectorSize() * (iSector - _iCachedSectorStart);

        return _sectorFactory.createSector(iSector, _abBulkReadCache, iOffset, _lngCacheFileOffset + iOffset);
    }

    //..........................................................................

    /** Will fail if CD was not opened with write access. */
    public void writeSector(int iSector, byte[] abSrcUserData)
            throws IOException
    {

        CdSector cdSector = getSector(iSector);

        if (cdSector.getCdUserDataSize() != abSrcUserData.length)
            throw new IllegalArgumentException("Data to write is not the right size.");
        
        byte[] abRawData = cdSector.rebuildRawSector(abSrcUserData);

        long lngOffset = (long)_sectorFactory.get1stSectorOffset() + 
                         (long)_sectorFactory.getRawSectorSize() * iSector;

        _inputFile.seek(lngOffset);
        _inputFile.write(abRawData);
    }

    //..........................................................................

    @Override
    public String toString() {
        return serialize();
    }

    public void reopenForWriting() throws IOException {
        _inputFile.close();
        _inputFile = new RandomAccessFile(_sourceFile, "rw");
    }

    /* ---------------------------------------------------------------------- */
    /* Sector Creator types ------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    private interface SectorFactory {
        CdSector createSector(int iSector, byte[] abSectorBuff, int iOffset, long lngFilePointer);
        LocalizedMessage getTypeDescription();
        boolean hasSectorHeader();
        long get1stSectorOffset();
        int getRawSectorSize();
    }

    private static class Cd2048Factory implements SectorFactory {

        final private long _lng1stSectorOffset;

        public Cd2048Factory() {
            _lng1stSectorOffset = 0;
        }

        public Cd2048Factory(long lngStartOffset) {
            _lng1stSectorOffset = lngStartOffset;
        }

        public CdSector createSector(int iSector, byte[] abSectorBuff, int iOffset, long lngFilePointer) {
            return new CdSector2048(abSectorBuff, iOffset, iSector, lngFilePointer);
        }


        public LocalizedMessage getTypeDescription() {
            return new LocalizedMessage(".iso (2048 bytes/sector) format"); // I18N
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
    
    private static class Cd2336Factory implements SectorFactory {

        private long _lng1stSectorOffset;

        /** Searches through the first 33 sectors for a full XA audio sector.
         *<p>
         *  Note: This assumes the input file has the data aligned at every 4 bytes!
         */
        public Cd2336Factory(RandomAccessFile cdFile) throws IOException, NotThisTypeException {
            if (cdFile.length() < SECTOR_SIZE_2336_BIN_NOSYNC)
                throw new NotThisTypeException();

            // Optimization TODO: With the new api I can read the whole test block at once
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
                cdFile.seek(lngSectStart);
                IO.readByteArray(cdFile, abTestSectorData);
                CdSector cdSector = new CdSector2336(abTestSectorData, 0, -1, -1);
                if (cdSector.getSubMode().getForm() != 2 || cdSector.isCdAudioSector())
                    continue;

                XaAnalysis xa = XaAnalysis.analyze(cdSector, 254);
                if (xa != null && xa.iProbability == 100) {
                    // we've found an XA audio sector
                    // maybe try to find another just to be sure?

                    // only check up to 146 sectors because, if the sector size is actually 2352,
                    // then around 147, the offset difference adds up to another whole 2352 sector
                    // this also avoids loop-around collision with 2448 sector size
                    int iTimes = 0;
                    for (long lngAdditionalOffset = SECTOR_SIZE_2336_BIN_NOSYNC;
                         lngSectStart + lngAdditionalOffset < cdFile.length() - abTestSectorData.length &&
                         iTimes < 146;
                         lngAdditionalOffset+=SECTOR_SIZE_2336_BIN_NOSYNC,
                         iTimes++)
                    {
                        cdFile.seek(lngSectStart + lngAdditionalOffset);
                        IO.readByteArray(cdFile, abTestSectorData, 0, SECTOR_SIZE_2336_BIN_NOSYNC);

                        xa = XaAnalysis.analyze(cdSector, 254);
                        if (xa != null && xa.iProbability == 100) {
                            // sweet, we found another one. we're done.
                            // backup to the first sector
                            _lng1stSectorOffset = lngSectStart % SECTOR_SIZE_2336_BIN_NOSYNC;
                            return;
                        }
                    }
                }
            }
            throw new NotThisTypeException();
        }

        private Cd2336Factory(long lngStartOffset) {
            _lng1stSectorOffset = lngStartOffset;
        }

        public CdSector createSector(int iSector, byte[] abSectorBuff, int iOffset, long lngFilePointer) {
            CdSector2336 sector = new CdSector2336(abSectorBuff, iOffset, iSector, lngFilePointer);
            return sector;
        }

        public LocalizedMessage getTypeDescription() {
            return new LocalizedMessage("partial header (2336 bytes/sector) format"); // I18N
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

    private static class Cd2352or2448Factory implements SectorFactory {

        private final long _lng1stSectorOffset;
        private final boolean _bln2352;

        /** Searches through the first {@link #SECTOR_SIZE_2448_BIN_SUBCHANNEL}*2 bytes
         *  for a {@link CdxaHeader.SECTOR_SYNC_HEADER}, then tries to identify
         *  the type depending on if {@code blnCheck2352} or {@code blnCheck2448}
         *  should be checked.
         */
        public Cd2352or2448Factory(RandomAccessFile cdFile, boolean blnCheck2352, boolean blnCheck2448) throws IOException, NotThisTypeException {

            long lngFileLength = cdFile.length();
            if (lngFileLength < CdxaHeader.SECTOR_SYNC_HEADER.length)
                throw new NotThisTypeException();

            byte[] abSyncHeader = new byte[CdxaHeader.SECTOR_SYNC_HEADER.length];

            for (long lngSectStart = 0;
                 lngSectStart < Math.min(lngFileLength - abSyncHeader.length, SECTOR_SIZE_2448_BIN_SUBCHANNEL * 2);
                 lngSectStart++)
            {
                cdFile.seek(lngSectStart);
                IO.readByteArray(cdFile, abSyncHeader);
                if (Arrays.equals(abSyncHeader, CdxaHeader.SECTOR_SYNC_HEADER)) {
                    LOG.log(Level.FINE, "Possible sync header at {0,number,#}", lngSectStart);
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

        /** Check for 10 more seek headers after the initial one just to be sure. */
        private boolean checkMore(int iSectorSize, RandomAccessFile cdFile, long lngSectStart, byte[] abSyncHeader) 
                throws IOException
        {
            // but make sure we don't check past the end of the file
            long lngSectorsToTry = Math.min(
                    10,
                    (cdFile.length()-lngSectStart-CdxaHeader.SECTOR_SYNC_HEADER.length) / SECTOR_SIZE_2352_BIN);

            for (int iOfs = iSectorSize;
                 lngSectorsToTry > 0;
                 lngSectorsToTry--, iOfs+=iSectorSize)
            {
                cdFile.seek(lngSectStart + iOfs);
                IO.readByteArray(cdFile, abSyncHeader);
                if (!Arrays.equals(abSyncHeader, CdxaHeader.SECTOR_SYNC_HEADER))
                    return false; // aw, too bad, back to the drawing board
            }
            return true;
        }

        public Cd2352or2448Factory(boolean blnIs2352, long lngStartOffset) {
            _bln2352 = blnIs2352;
            _lng1stSectorOffset = lngStartOffset;
        }

        public CdSector createSector(int iSector, byte[] abSectorBuff, int iOffset, long lngFilePointer) {
            CdSector2352 sector = new CdSector2352(abSectorBuff, iOffset, iSector, lngFilePointer);
            return sector;
        }

        public LocalizedMessage getTypeDescription() {
            return _bln2352 ?
                new LocalizedMessage("BIN/CUE (2352 bytes/sector) format") : // I18N
                new LocalizedMessage("BIN/CUE + Sub Channel (2448 bytes/sector) format"); // I18N
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
