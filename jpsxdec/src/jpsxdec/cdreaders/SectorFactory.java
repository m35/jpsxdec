/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;

public abstract class SectorFactory {

    private static final Logger LOG = Logger.getLogger(SectorFactory.class.getName());


    public static @Nonnull SectorFactory forSize(int iSectorSize, int iStartOffset) {
        switch (iSectorSize) {
            case CdSector.SECTOR_SIZE_2048_ISO:
                return new Cd2048Factory(iStartOffset);
            case CdSector.SECTOR_SIZE_2336_BIN_NOSYNC:
                return new Cd2336Factory(iStartOffset);
            case CdSector.SECTOR_SIZE_2352_BIN:
                return new Cd2352or2448Factory(iStartOffset, true /*2352*/);
            case CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL:
                return new Cd2352or2448Factory(iStartOffset, false /*2448*/);
            default:
                throw new IllegalArgumentException("Invalid sector size to open disc image as " + iSectorSize);
        }
    }

    public static @Nonnull SectorFactory identify(@Nonnull BufferedBytesReader cdReader)
            throws CdException.Read, CdException.FileTooSmallToIdentify
    {
        if (cdReader.length() < CdSector.SECTOR_SIZE_2048_ISO)
            throw new CdException.FileTooSmallToIdentify(cdReader.getSourceFile(), cdReader.length(), CdSector.SECTOR_SIZE_2048_ISO);

        LOG.info("Attempting to identify as 2448");
        SectorFactory factory = Cd2352or2448Factory.identify2352else2448(cdReader, false /*2448*/);

        if (factory == null) {
            LOG.info("Attempting to identify as 2352");
            factory = Cd2352or2448Factory.identify2352else2448(cdReader, true /*2352*/);
        }

        if (factory == null) {
            LOG.info("Attempting to identify as 2336");
            factory = Cd2336Factory.identify2336(cdReader);
        }

        if (factory == null) {
            LOG.info("Unknown disc type, assuming 2048");
            factory = new Cd2048Factory(0);
        } else {
            LOG.log(Level.INFO, "Disc type identified as {0,number,#}", factory.getRawSectorSize());
        }

        return factory;
    }

    private final int _i1stSectorOffset;

    private SectorFactory(int i1stSectorOffset) {
        _i1stSectorOffset = i1stSectorOffset;
    }

    final public int get1stSectorOffset() {
        return _i1stSectorOffset;
    }

    /** Returns the actual offset in bytes from the start of the source file
     *  to the raw start of {@code iSector}. */
    final public int getFilePointer(int iSector) {
        return iSector * getRawSectorSize() + get1stSectorOffset();
    }

    final public long calculateSectorCount(long lngFileLength) {
        return (lngFileLength - get1stSectorOffset()) / getRawSectorSize();
    }
    final public int calculateSectorCount(int iFileLength) {
        return (iFileLength - get1stSectorOffset()) / getRawSectorSize();
    }

    final public int getMinimumDiscSize() {
        return getRawSectorSize() + get1stSectorOffset();
    }

    abstract public @Nonnull ILocalizedMessage getTypeDescription();
    abstract public boolean hasSectorHeader();
    abstract public int getRawSectorSize();
    abstract public @Nonnull CdSector createSector(int iSector, @Nonnull byte[] abSectorBuff, int iOffset, int iFilePointer);

    // ===================================================================================

    private static class Cd2048Factory extends SectorFactory {

        public Cd2048Factory(int i1stSectorOffset) {
            super(i1stSectorOffset);
        }

        @Override
        public @Nonnull CdSector createSector(int iSector, @Nonnull byte[] abSectorBuff, int iOffset, int iFilePointer) {
            return new CdSector2048(iSector, abSectorBuff, iOffset, iFilePointer);
        }

        @Override
        public @Nonnull ILocalizedMessage getTypeDescription() {
            return I.CD_FORMAT_2048();
        }

        @Override
        public boolean hasSectorHeader() {
            return false;
        }

        @Override
        public int getRawSectorSize() {
            return CdSector.SECTOR_SIZE_2048_ISO;
        }
    }

    // ===================================================================================

    private static class Cd2336Factory extends SectorFactory {

        public static @CheckForNull Cd2336Factory identify2336(@Nonnull BufferedBytesReader cdReader)
                throws CdException.Read
        {
            /*
            Maximum size would look for 2 XA sectors with the maximum span,
            which is 32 sectors. Assuming there is no extra padding at the
            start of the file, the first XA sector could maybe be up to 32
            2336 sectors into the file.
            */
            final int MAX_SEARCH_SIZE = CdSector.SECTOR_SIZE_2336_BIN_NOSYNC * 32;

            // But don't read beyond the end of the file.
            int iMaxStartByteToSearch = Math.min(cdReader.length() - CdSector.SECTOR_SIZE_2336_BIN_NOSYNC,
                                                 MAX_SEARCH_SIZE);

            // If the file length is smaller than a sector, the logic will just skip to the end

            // Only detect XA ADPCM audio sectors to determine if it's SECTOR_MODE2
            for (int i1stXaSectorOffset = 0; i1stXaSectorOffset <= iMaxStartByteToSearch; i1stXaSectorOffset += 4) {

                if (isXaSector(cdReader, i1stXaSectorOffset)) {
                    // we've found an XA audio sector

                    // now find another at valid intervals
                    for (int iNextSectorToCheck : new int[] {2, 4, 8, 16, 32}) {
                        int i2ndXaSectorOffset = i1stXaSectorOffset + iNextSectorToCheck * CdSector.SECTOR_SIZE_2336_BIN_NOSYNC;
                        // watch out for the end of the file
                        if (i2ndXaSectorOffset > cdReader.length() - CdSector.SECTOR_SIZE_2336_BIN_NOSYNC)
                            break;
                        if (isXaSector(cdReader, i2ndXaSectorOffset)) {
                            // sweet, we found another one. we're done.
                            // backup to the first sector
                            int i1stSectorOffset = i1stXaSectorOffset % CdSector.SECTOR_SIZE_2336_BIN_NOSYNC;
                            return new Cd2336Factory(i1stSectorOffset);
                        }
                    }
                }
            }

            // either we have read enough to be pretty sure it's not 2336
            // or there's not enough data to really verify it is 2336
            return null;
        }

        private static boolean isXaSector(@Nonnull BufferedBytesReader cdReader, int iOffset)
                throws CdException.Read
        {
            cdReader.readAndBuffer(iOffset, CdSector.SECTOR_SIZE_2336_BIN_NOSYNC);
            CdSector cdSector = new CdSector2336(0, cdReader.getBuffer(), cdReader.getBufferReadOffset(), iOffset);
            XaAnalysis xa = XaAnalysis.analyze(cdSector);
            if (xa != null) {
                if (xa.iProbability == 100)
                    return true;
            }
            return false;
        }

        // ......................

        private Cd2336Factory(int i1stSectorOffset) {
            super(i1stSectorOffset);
        }

        @Override
        public @Nonnull CdSector createSector(int iSector, @Nonnull byte[] abSectorBuff, int iOffset, int iFilePointer) {
            return new CdSector2336(iSector, abSectorBuff, iOffset, iFilePointer);
        }

        @Override
        public @Nonnull ILocalizedMessage getTypeDescription() {
            return I.CD_FORMAT_2336();
        }
        @Override
        public boolean hasSectorHeader() {
            return true;
        }

        @Override
        public int getRawSectorSize() {
            return CdSector.SECTOR_SIZE_2336_BIN_NOSYNC;
        }
    }

    // ===================================================================================

    private static class Cd2352or2448Factory extends SectorFactory {

        public static @CheckForNull Cd2352or2448Factory identify2352else2448(@Nonnull BufferedBytesReader cdReader, boolean blnCheck2352else2448)
                throws CdException.Read
        {
            int iSectorSize = blnCheck2352else2448 ? CdSector.SECTOR_SIZE_2352_BIN : CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL;

            /*
            Search for a sync header in the first 2352|2448 * 2 bytes.
            If found, look for a few more after that to confirm
            */
            final int MAX_STARING_SEARCH_OFFSET = iSectorSize * 2;

            // But don't read beyond the end of the file.
            int iMaxStartByteToSearch = Math.min(cdReader.length() - iSectorSize,
                                                 MAX_STARING_SEARCH_OFFSET);

            for (int iSectorStart = 0; iSectorStart < iMaxStartByteToSearch; iSectorStart++) {
                if (isSyncHeader(cdReader, iSectorStart)) {
                    LOG.log(Level.FINE, "Possible sync header at {0,number,#}", iSectorStart);
                    // we think we found a sync header
                    if (checkMore(iSectorSize, cdReader, iSectorStart, 3)) {
                        int i1stSectorOffset = iSectorStart % iSectorSize;
                        return new Cd2352or2448Factory(i1stSectorOffset, blnCheck2352else2448);
                    }
                }
            }
            // either we have read enough to be pretty sure it's not 2352|2448
            // or there's not enough data to really verify it is 2352|2448
            return null;
        }

        private static boolean isSyncHeader(@Nonnull BufferedBytesReader cdReader, int iPosition) throws CdException.Read {
            cdReader.readAndBuffer(iPosition, CdSectorHeader.SECTOR_SYNC_HEADER.length);
            int i = cdReader.getBufferReadOffset();
            byte[] abSyncHeader = Arrays.copyOfRange(cdReader.getBuffer(), i, i + CdSectorHeader.SECTOR_SYNC_HEADER.length);
            return Arrays.equals(abSyncHeader, CdSectorHeader.SECTOR_SYNC_HEADER);
        }

        /** Check for more seek headers after the initial one just to be sure. */
        private static boolean checkMore(int iSectorSize, @Nonnull BufferedBytesReader cdReader,
                                         int iSectorStart, int iSectorsToTry)
                throws CdException.Read
        {
            // see how many sectors are available to check
            int iSectorsRemaining = (cdReader.length() - iSectorStart - CdSectorHeader.SECTOR_SYNC_HEADER.length) /
                                     iSectorSize;

            if (iSectorsRemaining < iSectorsToTry)
                return false; // not enough sectors to be confident

            for (int iOfs = iSectorSize; iSectorsToTry > 0; iSectorsToTry--, iOfs+=iSectorSize) {
                if (!isSyncHeader(cdReader, iSectorStart + iOfs))
                    return false; // aw, too bad, back to the drawing board
            }
            return true;
        }

        // ......................

        private final boolean _bln2352else2448;

        public Cd2352or2448Factory(int i1stSectorOffset, boolean blnIs2352else2448) {
            super(i1stSectorOffset);
            _bln2352else2448 = blnIs2352else2448;
        }

        @Override
        public @Nonnull CdSector createSector(int iSector, @Nonnull byte[] abSectorBuff, int iOffset, int iFilePointer) {
            return new CdSector2352(iSector, abSectorBuff, iOffset, iFilePointer);
        }

        @Override
        public @Nonnull ILocalizedMessage getTypeDescription() {
            return _bln2352else2448 ?
                    I.CD_FORMAT_2352() :
                    I.CD_FORMAT_2448();
        }
        @Override
        public boolean hasSectorHeader() {
            return true;
        }

        @Override
        public int getRawSectorSize() {
            return _bln2352else2448 ?
                    CdSector.SECTOR_SIZE_2352_BIN :
                    CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL;
        }
    }

}
