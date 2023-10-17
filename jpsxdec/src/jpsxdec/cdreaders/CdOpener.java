/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.util.IO;

public class CdOpener {

    private static final Logger LOG = Logger.getLogger(CdOpener.class.getName());


    private static final int DEFAULT_SECTOR_BUFFER_COUNT = 64;

    // -----------------------------------------------------------------------------------

    /** Tries to guess the sector size. */
    public static @Nonnull CdFileSectorReader open(@Nonnull String sFilePath)
            throws CdException.FileNotFound, CdException.FileTooSmallToIdentify, CdException.FileTooLarge, CdException.Read
    {
        return open(new File(sFilePath));
    }

    /** Tries to guess the sector size. */
    public static @Nonnull CdFileSectorReader open(@Nonnull File filePath)
            throws CdException.FileNotFound, CdException.FileTooSmallToIdentify, CdException.FileTooLarge, CdException.Read
    {
        return open(filePath, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    /** Tries to guess the sector size. */
    public static @Nonnull CdFileSectorReader open(@Nonnull File filePath, int iSectorsToBuffer)
            throws CdException.FileNotFound, CdException.FileTooSmallToIdentify, CdException.FileTooLarge, CdException.Read
    {
        LOG.info(filePath.getPath());

        BufferedBytesReader cdReader = openReader(filePath);
        boolean blnExceptionThrown = true;
        try {
            CdFileSectorReader cd = open(cdReader);
            cd.setSectorsToBuffer(iSectorsToBuffer);
            blnExceptionThrown = false;
            return cd;
        } finally {
            if (blnExceptionThrown)
                IO.closeSilently(cdReader, LOG);
        }
    }

    /** Tries to guess the sector size. */
    public static @Nonnull CdFileSectorReader open(@Nonnull BufferedBytesReader cdReader)
            throws CdException.FileTooSmallToIdentify, CdException.Read
    {
        SectorFactory sectorFactory = SectorFactory.identify(cdReader);
        return new CdFileSectorReader(cdReader, sectorFactory);
    }

    /** Tries to guess the sector size. */
    public static  @Nonnull CdFileSectorReader openRamDisc(@Nonnull File sourceFile, @Nonnull byte[] abDisc)
            throws CdException.FileTooSmallToIdentify, CdException.Read
    {
        BufferedBytesReader reader = new BufferedBytesReader(sourceFile, abDisc);
        return open(reader);
    }

    // ...................................................................................

    /** Uses the given sector size. */
    public static @Nonnull CdFileSectorReader openWithSectorSize(@Nonnull String sFilePath, int iSectorSize)
            throws CdException.FileNotFound, CdException.FileTooSmallToIdentify, CdException.FileTooLarge, CdException.Read
    {
        return openWithSectorSize(new File(sFilePath), iSectorSize);
    }

    /** Uses the given sector size. */
    public static @Nonnull CdFileSectorReader openWithSectorSize(@Nonnull File filePath, int iSectorSize)
            throws CdException.FileNotFound, CdException.FileTooSmallToIdentify, CdException.FileTooLarge, CdException.Read
    {
        return openWithSectorSize(filePath, iSectorSize, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    /** Uses the given sector size. */
    public static @Nonnull CdFileSectorReader openWithSectorSize(@Nonnull File filePath, int iSectorSize, int iSectorsToBuffer)
            throws CdException.FileNotFound, CdException.FileTooSmallToIdentify, CdException.FileTooLarge, CdException.Read
    {
        LOG.info(filePath.getPath());

        switch (iSectorSize) {
            case CdSector.SECTOR_SIZE_2048_ISO:
            case CdSector.SECTOR_SIZE_2336_BIN_NOSYNC:
            case CdSector.SECTOR_SIZE_2352_BIN:
            case CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL:
                break;
            default:
                throw new IllegalArgumentException("Invalid sector size to open disc image as " + iSectorSize);
        }

        BufferedBytesReader cdReader = openReader(filePath);
        boolean blnExceptionThrown = true;
        try {
            CdFileSectorReader cd = openWithSectorSize(cdReader, iSectorSize);
            cd.setSectorsToBuffer(iSectorsToBuffer);
            blnExceptionThrown = false;
            return cd;
        } finally {
            if (blnExceptionThrown)
                IO.closeSilently(cdReader, LOG);
        }
    }

    /** Uses the given sector size. */
    public static @Nonnull CdFileSectorReader openWithSectorSize(@Nonnull BufferedBytesReader cdReader, int iSectorSize)
            throws CdException.FileTooSmallToIdentify
    {
        SectorFactory sectorFactory = SectorFactory.forSize(iSectorSize, 0);

        int iMinSize = sectorFactory.getMinimumDiscSize();
        if (cdReader.length() < iMinSize)
            throw new CdException.FileTooSmallToIdentify(cdReader.getSourceFile(), cdReader.length(), iMinSize);

        return new CdFileSectorReader(cdReader, sectorFactory);
    }

    // ...................................................................................

    /** Deserializes the CD. */
    public static @Nonnull CdFileSectorReader deserialize(@Nonnull String sSerialization)
            throws LocalizedDeserializationFail, CdException.FileNotFound, CdException.Read
    {
        return deserialize(sSerialization, DEFAULT_SECTOR_BUFFER_COUNT);
    }

    /** Deserializes the CD. */
    public static @Nonnull CdFileSectorReader deserialize(@Nonnull String sSerialization, int iSectorsToBuffer)
            throws LocalizedDeserializationFail, CdException.FileNotFound, CdException.Read
    {
        // 1. check for bad serialization
        SerializedDisc sd = new SerializedDisc(sSerialization);
        LOG.info(sd.getFilename());
        File filePath = new File(sd.getFilename());

        // 2. get sector factory ready
        SectorFactory sectorFactory = SectorFactory.forSize(sd.getSectorSize(), sd.getStartOffset());

        // 3. open file
        BufferedBytesReader cdReader;
        try {
            cdReader = openReader(filePath);
        } catch (CdException.FileTooLarge ex) {
            // treat a too big file as a serialization issue
            // since the serialized sector count should keep it in a valid range
            long lngActualSectorCount = sectorFactory.calculateSectorCount(ex.getFileSize());
            if (sd.getSectorCount() == lngActualSectorCount)
                throw new RuntimeException("Serializer should have blocked too big disc");
            throw new LocalizedDeserializationFail(I.CD_SECTOR_COUNT_MISMATCH(sd.getSectorCount(), lngActualSectorCount));
        }

        boolean blnExceptionThrown = true;
        try {
            // 4. check sector count matches
            // this is equivalent to checking if the file is too small
            // since the serialized sector count should keep it in a valid range
            int iFileSectorCount = sectorFactory.calculateSectorCount(cdReader.length());
            if (sd.getSectorCount() != iFileSectorCount)
                throw new LocalizedDeserializationFail(I.CD_SECTOR_COUNT_MISMATCH(sd.getSectorCount(), iFileSectorCount));

            // 5. done
            CdFileSectorReader cd = new CdFileSectorReader(cdReader, sectorFactory);
            cd.setSectorsToBuffer(iSectorsToBuffer);
            blnExceptionThrown = false;
            return cd;
        } finally {
            if (blnExceptionThrown)
                IO.closeSilently(cdReader, LOG);
        }
    }

    // -----------------------------------------------------------------------------------

    private static @Nonnull BufferedBytesReader openReader(@Nonnull File filePath)
            throws CdException.FileNotFound, CdException.FileTooLarge, CdException.Read
    {
        BufferedBytesReader reader = ZippedCdReader.tryReadZippedDisc(filePath);
        if (reader != null)
            return reader;

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(filePath, "r");
        } catch (FileNotFoundException ex) {
            throw new CdException.FileNotFound(filePath, ex);
        }

        boolean blnExceptionThrown = true;
        try {
            long lngFileLength;
            try {
                lngFileLength = raf.length();
            } catch (IOException ex) {
                throw new CdException.Read(filePath, ex);
            }

            if (lngFileLength > Integer.MAX_VALUE)
                throw new CdException.FileTooLarge(filePath, lngFileLength, Integer.MAX_VALUE);

            reader = new BufferedBytesReader(filePath, raf, (int) lngFileLength);
            blnExceptionThrown = false;
            return reader;
        } finally {
            if (blnExceptionThrown)
                IO.closeSilently(raf, LOG);
        }
    }

}
