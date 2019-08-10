/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.TaskCanceledException;

/** Encapsulates the reading of a CD image (BIN/CUE, ISO), 
 * or a file containing some (possibly raw) sectors of a CD.
 * The resulting data is mostly the same.
 * This class tries to guess what type of file it is.
 * <ul>
 * <li>{@link CdSector#SECTOR_SIZE_2048_ISO}
 * <li>{@link CdSector#SECTOR_SIZE_2336_BIN_NOSYNC}
 * <li>{@link CdSector#SECTOR_SIZE_2352_BIN}
 * <li>{@link CdSector#SECTOR_SIZE_2448_BIN_SUBCHANNEL}
 * </ul>
 */
public class CdFileSectorReader implements Closeable {

    private static final Logger LOG = Logger.getLogger(CdFileSectorReader.class.getName());

    private static final int DEFAULT_SECTOR_BUFFER_COUNT   = 16;

    /** Exception if a CD file is not found or cannot be opened. */
    public static class CdFileNotFoundException extends FileNotFoundException {

        @Nonnull
        private final File _file;

        public CdFileNotFoundException(@Nonnull File file, FileNotFoundException ex) {
            super(file.getPath());
            initCause(ex);
            _file = file;
        }

        public @Nonnull File getFile() {
            return _file;
        }
    }

    /** Exception if there is an error reading from the CD file. */
    public static class CdReadException extends IOException {

        @Nonnull
        private final File _file;

        public CdReadException(@Nonnull File file, IOException ex) {
            super(ex);
            _file = file;
        }

        public @Nonnull File getFile() {
            return _file;
        }
    }
    /** Exception if there is an error writing to the CD file. */
    public static class CdWriteException extends IOException {

        @Nonnull
        private final File _file;

        public CdWriteException(@Nonnull File file, IOException ex) {
            super(ex);
            _file = file;
        }

        public @Nonnull File getFile() {
            return _file;
        }
    }

    /** Exception if the source CD file is too small to be identified
     * (like {@code < 2048 bytes}) */
    public static class FileTooSmallToIdentifyException extends Exception {

        private final long _lngFileSize;

        public FileTooSmallToIdentifyException(long lngFileSize) {
            _lngFileSize = lngFileSize;
        }

        public long getFileSize() {
            return _lngFileSize;
        }
    }

    /** Exception if there is an error re-opening the CD file
     * (like for write-access). */
    public static class CdReopenException extends IOException {

        @Nonnull
        private final File _file;

        public CdReopenException(File file, Throwable cause) {
            super(cause);
            _file = file;
        }

        public File getFile() {
            return _file;
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Nonnull
    private RandomAccessFile _inputFile;
    @Nonnull
    private final File _sourceFile;
    /** Creates sectors from the data based on the type of disc image it is. */
    @Nonnull
    private final SectorFactory _sectorFactory;
    /** Number of full sectors in the disc image. */
    private final int _iSectorCount;

    private int _iCachedSectorStart;
    private int _iSectorsToCache;
    @CheckForNull
    private byte[] _abBulkReadCache;
    private long _lngCacheFileOffset;

    @CheckForNull
    private DiscPatcher _patcher;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public CdFileSectorReader(@Nonnull File inputFile)
            throws CdFileNotFoundException, FileTooSmallToIdentifyException, CdReadException
    {
        this(inputFile, false, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    public CdFileSectorReader(@Nonnull File inputFile, boolean blnAllowWrites)
            throws CdFileNotFoundException, FileTooSmallToIdentifyException, CdReadException
    {
        this(inputFile, blnAllowWrites, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    /** Opens a CD file for reading. Tries to guess the CD size. */
    public CdFileSectorReader(@Nonnull File sourceFile,
                              boolean blnAllowWrites, int iSectorsToBuffer)
            throws CdFileNotFoundException, FileTooSmallToIdentifyException, CdReadException
    {
        LOG.info(sourceFile.getPath());

        _sourceFile = sourceFile;
        _iSectorsToCache = iSectorsToBuffer;

        try {
            _inputFile = new RandomAccessFile(sourceFile, blnAllowWrites ? "rw" : "r");
        } catch (FileNotFoundException ex) {
            throw new CdFileNotFoundException(sourceFile, ex);
        }

        boolean blnExceptionThrown = true;
        try {

            SectorFactory factory;
            try {

                try {
                    LOG.info("Attempting to identify as 2352/2448");
                    factory = new Cd2352or2448Factory(_inputFile, true /*2352*/, true /*2448*/);
                    LOG.log(Level.INFO, "Disc type identified as {0,number,#}", factory.getRawSectorSize());
                } catch (FileTooSmallToIdentifyException ex) {
                    try {
                        LOG.info("Attempting to identify as 2336");
                        factory = new Cd2336Factory(_inputFile);
                        LOG.info("Disc type identified as 2336");
                    } catch (FileTooSmallToIdentifyException ex1) {
                        LOG.info("Unknown disc type, assuming 2048");
                        // we couldn't figure out what it is
                        // assume ISO style if it's big enough
                        long lngFileSize = _inputFile.length();
                        if (lngFileSize < CdSector.SECTOR_SIZE_2048_ISO) {
                            _inputFile.close();
                            throw new FileTooSmallToIdentifyException(lngFileSize);
                        }
                        factory = new Cd2048Factory();
                    }
                }

            } catch (IOException ex) {
                throw new CdReadException(sourceFile, ex);
            }
            _sectorFactory = factory;

            _iSectorCount = calculateSectorCount();
            blnExceptionThrown = false;
        } finally {
            if (blnExceptionThrown)
                IO.closeSilently(_inputFile, LOG);
        }

        if (_sectorFactory.get1stSectorOffset() != 0)
            LOG.log(Level.WARNING, "First CD sector starts at offset {0}",
                                   _sectorFactory.get1stSectorOffset());
    }

    public CdFileSectorReader(@Nonnull File inputFile, int iSectorSize)
            throws CdFileNotFoundException, FileTooSmallToIdentifyException, CdReadException
    {
        this(inputFile, iSectorSize, false, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    /** Opens a CD file for reading using the provided sector size.
     * @throws FileTooSmallToIdentifyException If the disc image doesn't match the sector size.
     */
    public CdFileSectorReader(@Nonnull File sourceFile,
            int iSectorSize, boolean blnAllowWrites, int iSectorsToBuffer)
            throws CdFileNotFoundException, FileTooSmallToIdentifyException, CdReadException
    {
        LOG.info(sourceFile.getPath());

        _sourceFile = sourceFile;
        _iSectorsToCache = iSectorsToBuffer;

        try {
            _inputFile = new RandomAccessFile(sourceFile, blnAllowWrites ? "rw" : "r");
        } catch (FileNotFoundException ex) {
            throw new CdFileNotFoundException(sourceFile, ex);
        }

        boolean blnExceptionThrown = true;
        try {
            switch (iSectorSize) {
                case CdSector.SECTOR_SIZE_2048_ISO:
                    _sectorFactory = new Cd2048Factory();
                    break;
                case CdSector.SECTOR_SIZE_2336_BIN_NOSYNC:
                    _sectorFactory = new Cd2336Factory(_inputFile);
                    break;
                case CdSector.SECTOR_SIZE_2352_BIN:
                    _sectorFactory = new Cd2352or2448Factory(_inputFile, true /*2352*/, false /*2448*/);
                    break;
                case CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL:
                    _sectorFactory = new Cd2352or2448Factory(_inputFile, false /*2352*/, true /*2448*/);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid sector size to open disc image as " + iSectorSize);
            }
            blnExceptionThrown = false;
        } catch (IOException ex) {
            throw new CdReadException(sourceFile, ex);
        } finally {
            if (blnExceptionThrown)
                IO.closeSilently(_inputFile, LOG);
        }

        _iSectorCount = calculateSectorCount();

        if (_sectorFactory.get1stSectorOffset() != 0)
            LOG.log(Level.WARNING, "First CD sector starts at offset {0}",
                                   _sectorFactory.get1stSectorOffset());
    }

    public CdFileSectorReader(@Nonnull String sSerialization, boolean blnAllowWrites)
            throws LocalizedDeserializationFail, CdFileNotFoundException, CdReadException
    {
        this(sSerialization, blnAllowWrites, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    public CdFileSectorReader(@Nonnull String sSerialization, boolean blnAllowWrites, int iSectorsToBuffer)
            throws LocalizedDeserializationFail, CdFileNotFoundException, CdReadException
    {
        String[] asValues = Misc.regex(DESERIALIZATION, sSerialization);
        if (asValues == null || asValues.length != 5)
            throw new LocalizedDeserializationFail(I.CD_DESERIALIZE_FAIL(sSerialization));

        try {
            _iSectorCount = Integer.parseInt(asValues[3]);
            long lngStartOffset = Long.parseLong(asValues[4]);
            int iSectorSize = Integer.parseInt(asValues[2]);

            switch (iSectorSize) {
                case CdSector.SECTOR_SIZE_2048_ISO:
                    _sectorFactory = new Cd2048Factory(lngStartOffset);
                    break;
                case CdSector.SECTOR_SIZE_2336_BIN_NOSYNC:
                    _sectorFactory = new Cd2336Factory(lngStartOffset);
                    break;
                case CdSector.SECTOR_SIZE_2352_BIN:
                    _sectorFactory = new Cd2352or2448Factory(true, lngStartOffset);
                    break;
                case CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL:
                    _sectorFactory = new Cd2352or2448Factory(false, lngStartOffset);
                    break;
                default:
                    throw new LocalizedDeserializationFail(I.CD_DESERIALIZE_FAIL(sSerialization));
            }
        } catch (NumberFormatException ex) {
            throw new LocalizedDeserializationFail(I.CD_DESERIALIZE_FAIL(sSerialization), ex);
        }

        _sourceFile = new File(asValues[1]);

        try {
            _inputFile = new RandomAccessFile(_sourceFile, blnAllowWrites ? "rw" : "r");
        } catch (FileNotFoundException ex) {
            throw new CdFileNotFoundException(_sourceFile, ex);
        }

        _iSectorsToCache = iSectorsToBuffer;

        int iActualSectorCount = calculateSectorCount();

        if (_iSectorCount != iActualSectorCount) {
            IO.closeSilently(_inputFile, LOG);
            throw new LocalizedDeserializationFail(I.SECTOR_COUNT_MISMATCH(_iSectorCount, iActualSectorCount));
        }

    }

    private int calculateSectorCount() throws CdReadException {
        try {
            return (int)((_inputFile.length() - _sectorFactory.get1stSectorOffset())
                    / _sectorFactory.getRawSectorSize());
        } catch (IOException ex) {
            throw new CdReadException(_sourceFile, ex);
        }
    }

    public final static String SERIALIZATION_START = "Filename:";

    private static final String DESERIALIZATION =
            SERIALIZATION_START + "([^|]+)\\|Sector size:(\\d+)\\|Sector count:(\\d+)\\|First sector offset:(\\d+)";

    private static final String SERIALIZATION =
            SERIALIZATION_START + "%s|Sector size:%d|Sector count:%d|First sector offset:%d";

    public @Nonnull String serialize() {
        return String.format(SERIALIZATION,
                _sourceFile.getPath(),
                _sectorFactory.getRawSectorSize(),
                _iSectorCount,
                _sectorFactory.get1stSectorOffset());
    }

    public boolean matchesSerialization(@Nonnull String sSerialization) {
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

    /** Size of the raw sectors of the source disc image. */
    public int getRawSectorSize() {
        return _sectorFactory.getRawSectorSize();
    }

    /** If sectors of this disc image could have raw sector headers
     * (i.e. not ISO 2048 images). */
    public boolean hasSectorHeader() {
        return _sectorFactory.hasSectorHeader();
    }

    public @Nonnull File getSourceFile() {
        return _sourceFile;
    }

    /** Returns the actual offset in bytes from the start of the source file
     *  to the raw start of {@code iSector}. */
    public long getFilePointer(int iSector) {
        return (long)iSector * _sectorFactory.getRawSectorSize() + _sectorFactory.get1stSectorOffset();
    }

    /** Returns the number of sectors in the disc image. */
    public int getSectorCount() {
        return _iSectorCount;
    }

    public @Nonnull ILocalizedMessage getTypeDescription() {
        return _sectorFactory.getTypeDescription();
    }

    //..........................................................................

    public @Nonnull CdSector getSector(int iSector) throws CdReadException {
        if (iSector < 0 || iSector >= _iSectorCount)
            throw new IndexOutOfBoundsException("Sector "+iSector+" not in bounds of CD");

        if (iSector >= _iCachedSectorStart + _iSectorsToCache || iSector < _iCachedSectorStart || _abBulkReadCache == null) {
            _abBulkReadCache = null; // in case of failure, make sure we aren't left with some invalid cache

            _iCachedSectorStart = iSector;
            _lngCacheFileOffset = getFilePointer(iSector);

            byte[] abBulkReadCache = new byte[_sectorFactory.getRawSectorSize() * _iSectorsToCache];
            try {
                _inputFile.seek(_lngCacheFileOffset);
                int iBytesRead = IO.readByteArrayMax(_inputFile, abBulkReadCache, 0, abBulkReadCache.length);
                if (iBytesRead < _sectorFactory.getRawSectorSize())
                    throw new RuntimeException("Should have already verified this should not happen");
            } catch (IOException ex) {
                throw new CdReadException(_sourceFile, ex);
            }

            // made sure everything is good before we save the cache
            _abBulkReadCache = abBulkReadCache;
        }

        int iOffset = _sectorFactory.getRawSectorSize() * (iSector - _iCachedSectorStart);

        return _sectorFactory.createSector(iSector, _abBulkReadCache, iOffset, _lngCacheFileOffset + iOffset);
    }

    //..........................................................................

    /** Will fail if CD was not opened with write access. */
    void writeSector(int iSector, @Nonnull byte[] abSrcUserData)
            throws CdReadException, CdWriteException
    {
        CdSector cdSector = getSector(iSector);

        if (cdSector.getCdUserDataSize() != abSrcUserData.length)
            throw new IllegalArgumentException("Data to write is not the right size.");
        
        byte[] abRawData = cdSector.rebuildRawSector(abSrcUserData);

        long lngOffset = (long)_sectorFactory.get1stSectorOffset() + 
                         (long)_sectorFactory.getRawSectorSize() * iSector;

        try {
            _inputFile.seek(lngOffset);
            _inputFile.write(abRawData);
            // clearing the cache could be done here, but it wouldn't
            // affect anything that has already been read (which is most things)
        } catch (IOException ex) {
            throw new CdWriteException(_sourceFile, ex);
        }
    }

    public void beginPatching() throws DiscPatcher.CreatePatchFileException {
        if (_patcher != null)
            _patcher.cancel();
        _patcher = new DiscPatcher(this);
    }

    /** Returns the current patch file.
     * @throws IllegalStateException is not currently patching. */
    public @Nonnull File getTemporaryPatchFile() {
        if (_patcher == null)
            throw new IllegalStateException();
        return _patcher.getTempFile();
    }

    public void addPatch(int iSector, int iOffsetInSector, 
                         @Nonnull byte[] abBytesToReplace)
            throws DiscPatcher.WritePatchException
    {
        addPatch(iSector, iOffsetInSector, abBytesToReplace,
                 0, abBytesToReplace.length);
    }

    public void addPatch(int iSector, int iOffsetInSector, 
                         @Nonnull byte[] abBytesToReplace,
                         int iStartByteToUse, int iNumberOfBytesToReplace)
            throws DiscPatcher.WritePatchException
    {
        if (_patcher == null)
            throw new IllegalStateException();
        _patcher.addPatch(iSector, iOffsetInSector,
                          abBytesToReplace, iStartByteToUse,
                          iNumberOfBytesToReplace);
    }

    public void applyPatches(@Nonnull ProgressLogger pl)
            throws CdReopenException,
                   CdReadException,
                   CdWriteException,
                   DiscPatcher.PatchReadException,
                   TaskCanceledException
    {
        if (_patcher == null)
            throw new IllegalStateException();
        _patcher.applyPatches(this, pl);
        _patcher = null;
    }

    void reopenForWriting() throws CdReopenException {
        try {
            _inputFile.close(); // expose close exception
            _inputFile = new RandomAccessFile(_sourceFile, "rw");
        } catch (IOException ex) {
            throw new CdReopenException(_sourceFile, ex);
        }
    }

    //..........................................................................

    @Override
    public String toString() {
        return serialize();
    }

    /* ---------------------------------------------------------------------- */
    /* Sector Creator types ------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    private interface SectorFactory {
        @Nonnull CdSector createSector(int iSector, @Nonnull byte[] abSectorBuff, int iOffset, long lngFilePointer);
        @Nonnull ILocalizedMessage getTypeDescription();
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

        public @Nonnull CdSector createSector(int iSector, @Nonnull byte[] abSectorBuff, int iOffset, long lngFilePointer) {
            return new CdSector2048(iSector, abSectorBuff, iOffset, lngFilePointer);
        }


        public @Nonnull ILocalizedMessage getTypeDescription() {
            return I.CD_FORMAT_2048();
        }

        public boolean hasSectorHeader() {
            return false;
        }

        public long get1stSectorOffset() {
            return _lng1stSectorOffset;
        }

        public int getRawSectorSize() {
            return CdSector.SECTOR_SIZE_2048_ISO;
        }
    }
    
    private static class Cd2336Factory implements SectorFactory {

        private long _lng1stSectorOffset;

        /** Searches through the first 33 sectors for a full XA audio sector.
         *<p>
         *  Note: This assumes the input file has the data aligned at every 4 bytes!
         */
        public Cd2336Factory(@Nonnull RandomAccessFile cdFile) 
                throws FileTooSmallToIdentifyException, IOException
        {
            long lngFileLength = cdFile.length();
            if (lngFileLength < CdSector.SECTOR_SIZE_2336_BIN_NOSYNC)
                throw new FileTooSmallToIdentifyException(lngFileLength);

            // Optimization TODO: With the new api I can read the whole test block at once
            byte[] abTestSectorData = new byte[CdSector.SECTOR_SIZE_2336_BIN_NOSYNC];

            // only search up to 33 sectors into the file
            // because that's the maximum XA audio span
            // (this misses audio that starts later in the file however)
            int iMaxSearch = CdSector.SECTOR_SIZE_2336_BIN_NOSYNC * 33;
            if (iMaxSearch > lngFileLength)
                iMaxSearch = (int) lngFileLength;

            // Only detect XA ADPCM audio sectors to determine if it's SECTOR_MODE2
            for (long lngSectStart = 0;
                 lngSectStart < iMaxSearch - abTestSectorData.length;
                 lngSectStart+=4)
            {
                if (isXaSector(cdFile, lngSectStart, abTestSectorData)) {
                    // we've found an XA audio sector
                    // maybe try to find another just to be sure?

                    // only check up to 146 sectors because, if the sector size is actually 2352,
                    // then around 147, the offset difference adds up to another whole 2352 sector
                    // this also avoids loop-around collision with 2448 sector size
                    int iTimes = 0;
                    for (long lngAdditionalOffset = CdSector.SECTOR_SIZE_2336_BIN_NOSYNC;
                         lngSectStart + lngAdditionalOffset < lngFileLength - abTestSectorData.length &&
                         iTimes < 146;
                         lngAdditionalOffset+=CdSector.SECTOR_SIZE_2336_BIN_NOSYNC,
                         iTimes++)
                    {
                        if (isXaSector(cdFile, lngSectStart + lngAdditionalOffset, abTestSectorData)) {
                            // sweet, we found another one. we're done.
                            // backup to the first sector
                            _lng1stSectorOffset = lngSectStart % CdSector.SECTOR_SIZE_2336_BIN_NOSYNC;
                            return;
                        }
                    }
                }
            }
            throw new FileTooSmallToIdentifyException(lngFileLength);
        }

        private static boolean isXaSector(@Nonnull RandomAccessFile cdFile,
                                          long lngSectorStart,
                                          @Nonnull byte[] abReusableBuffer)
                throws IOException
        {
            cdFile.seek(lngSectorStart);
            IO.readByteArray(cdFile, abReusableBuffer);
            CdSector cdSector = new CdSector2336(0, abReusableBuffer, 0, lngSectorStart);
            XaAnalysis xa = XaAnalysis.analyze(cdSector, 254);
            return (xa != null && xa.iProbability == 100);
        }

        private Cd2336Factory(long lngStartOffset) {
            _lng1stSectorOffset = lngStartOffset;
        }

        public @Nonnull CdSector createSector(int iSector, @Nonnull byte[] abSectorBuff, int iOffset, long lngFilePointer) {
            return new CdSector2336(iSector, abSectorBuff, iOffset, lngFilePointer);
        }

        public @Nonnull ILocalizedMessage getTypeDescription() {
            return I.CD_FORMAT_2336();
        }
        public boolean hasSectorHeader() {
            return true;
        }

        public long get1stSectorOffset() {
            return _lng1stSectorOffset;
        }

        public int getRawSectorSize() {
            return CdSector.SECTOR_SIZE_2336_BIN_NOSYNC;
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
        public Cd2352or2448Factory(@Nonnull RandomAccessFile cdFile, boolean blnCheck2352, boolean blnCheck2448)
                throws FileTooSmallToIdentifyException, IOException
        {
            long lngFileLength = cdFile.length();
            if (lngFileLength < CdSectorHeader.SECTOR_SYNC_HEADER.length)
                throw new FileTooSmallToIdentifyException(lngFileLength);

            byte[] abSyncHeader = new byte[CdSectorHeader.SECTOR_SYNC_HEADER.length];

            for (long lngSectStart = 0;
                 lngSectStart < Math.min(lngFileLength - abSyncHeader.length, CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL * 2);
                 lngSectStart++)
            {
                cdFile.seek(lngSectStart);
                IO.readByteArray(cdFile, abSyncHeader);
                if (Arrays.equals(abSyncHeader, CdSectorHeader.SECTOR_SYNC_HEADER)) {
                    LOG.log(Level.FINE, "Possible sync header at {0,number,#}", lngSectStart);
                    // we think we found a sync header
                    if (blnCheck2352 && checkMore(CdSector.SECTOR_SIZE_2352_BIN, cdFile, lngSectStart, abSyncHeader)) {
                        _bln2352 = true;
                        _lng1stSectorOffset = lngSectStart % CdSector.SECTOR_SIZE_2352_BIN;
                        return;
                    } else if (blnCheck2448 && checkMore(CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL, cdFile, lngSectStart, abSyncHeader)) {
                        _bln2352 = false;
                        _lng1stSectorOffset = lngSectStart % CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL;
                        return;
                    }
                }
            }
            throw new FileTooSmallToIdentifyException(lngFileLength);
        }

        /** Check for 10 more seek headers after the initial one just to be sure. */
        private boolean checkMore(int iSectorSize, @Nonnull RandomAccessFile cdFile, long lngSectStart, @Nonnull byte[] abSyncHeader)
                throws IOException
        {
            long lngSectorsToTry = (cdFile.length()-lngSectStart-CdSectorHeader.SECTOR_SYNC_HEADER.length) /
                                   CdSector.SECTOR_SIZE_2352_BIN;
            // but make sure we don't check past the end of the file
            if (lngSectorsToTry > 10)
                    lngSectorsToTry = 10;

            for (int iOfs = iSectorSize;
                 lngSectorsToTry > 0;
                 lngSectorsToTry--, iOfs+=iSectorSize)
            {
                cdFile.seek(lngSectStart + iOfs);
                IO.readByteArray(cdFile, abSyncHeader);
                if (!Arrays.equals(abSyncHeader, CdSectorHeader.SECTOR_SYNC_HEADER))
                    return false; // aw, too bad, back to the drawing board
            }
            return true;
        }

        public Cd2352or2448Factory(boolean blnIs2352, long lngStartOffset) {
            _bln2352 = blnIs2352;
            _lng1stSectorOffset = lngStartOffset;
        }

        public @Nonnull CdSector createSector(int iSector, @Nonnull byte[] abSectorBuff, int iOffset, long lngFilePointer) {
            return new CdSector2352(iSector, abSectorBuff, iOffset, lngFilePointer);
        }

        public @Nonnull ILocalizedMessage getTypeDescription() {
            return _bln2352 ?
                    I.CD_FORMAT_2352() :
                    I.CD_FORMAT_2448();
        }
        public boolean hasSectorHeader() {
            return true;
        }

        public long get1stSectorOffset() {
            return _lng1stSectorOffset;
        }

        public int getRawSectorSize() {
            return _bln2352 ?
                    CdSector.SECTOR_SIZE_2352_BIN :
                    CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL;
        }
    }


}
