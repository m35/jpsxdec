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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.i18n.ILocalizedMessage;

/** A flat file implementation of {@link ICdSectorReader} backed by a
 *  {@link BufferedBytesReader}. */
public class CdFileSectorReader implements ICdSectorReader {

    private static final Logger LOG = Logger.getLogger(CdFileSectorReader.class.getName());

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

    /** Exception if there is an error re-opening the CD file
     * (like for write-access). */
    public static class CdReopenException extends IOException {

        @Nonnull
        private final File _file;

        public CdReopenException(@Nonnull File file, Throwable cause) {
            super(cause);
            _file = file;
        }

        public @Nonnull File getFile() {
            return _file;
        }
    }

    // -----------------------------------------------------------------------------------

    @Nonnull
    private final BufferedBytesReader _cdReader;
    /** Creates sectors from the data based on the type of disc image it is. */
    @Nonnull
    private final SectorFactory _sectorFactory;
    /** Number of full sectors in the disc image. */
    private final int _iSectorCount;

    public CdFileSectorReader(@Nonnull BufferedBytesReader cdReader,
                              @Nonnull SectorFactory sectorFactory)
    {
        if (sectorFactory.get1stSectorOffset() != 0)
            LOG.log(Level.WARNING, "First CD sector starts at offset {0}", sectorFactory.get1stSectorOffset());

        _cdReader = cdReader;
        _sectorFactory = sectorFactory;
        _iSectorCount = sectorFactory.calculateSectorCount(cdReader.length());
    }

    @Override
    public @Nonnull String serialize() {
        return serializeDisc().serializeString();
    }

    public @Nonnull SerializedDisc serializeDisc() {
        return new SerializedDisc(_cdReader.getSourceFile().getPath(), _sectorFactory.getRawSectorSize(),
                                  _iSectorCount, _sectorFactory.get1stSectorOffset());
    }

    @Override
    public boolean matchesSerialization(@Nonnull String sSerialization) {
        return serializeDisc().matches(sSerialization);
    }

    @Override
    public void close() throws IOException {
        _cdReader.close();
    }

    //..........................................................................

    /** Size of the raw sectors of the source disc image. */
    @Override
    public int getRawSectorSize() {
        return _sectorFactory.getRawSectorSize();
    }

    /** If sectors of this disc image could have raw sector headers
     * (i.e. not ISO 2048 images). */
    @Override
    public boolean hasSectorHeader() {
        return _sectorFactory.hasSectorHeader();
    }

    @Override
    public @Nonnull File getSourceFile() {
        return _cdReader.getSourceFile();
    }

    /** Returns the actual offset in bytes from the start of the source file
     *  to the raw start of {@code iSector}. */
    public int getFilePointer(int iSector) {
        return iSector * _sectorFactory.getRawSectorSize() + _sectorFactory.get1stSectorOffset();
    }

    /** Returns the number of sectors in the disc image. */
    @Override
    public int getSectorCount() {
        return _iSectorCount;
    }

    @Override
    public @Nonnull ILocalizedMessage getTypeDescription() {
        return _sectorFactory.getTypeDescription();
    }

    //..........................................................................

    @Override
    public @Nonnull CdSector getSector(int iSector) throws CdException.Read {
        if (iSector < 0 || iSector >= _iSectorCount)
            throw new IndexOutOfBoundsException("Sector "+iSector+" not in bounds of CD");

        int iFilePointer = _sectorFactory.getFilePointer(iSector);
        _cdReader.readAndBuffer(iFilePointer, _sectorFactory.getRawSectorSize());

        return _sectorFactory.createSector(iSector, _cdReader.getBuffer(), _cdReader.getBufferReadOffset(), iFilePointer);
    }

    //..........................................................................

    public void setSectorsToBuffer(int iSectorCount) {
        _cdReader.setBufferSize(iSectorCount * _sectorFactory.getRawSectorSize());
    }

    void reopenForWriting() throws CdReopenException {
        _cdReader.reopenForWriting();
    }

    /** Will fail if CD was not opened with write access. */
    void writeSector(int iSector, @Nonnull byte[] abSrcUserData)
            throws CdException.Read, CdWriteException
    {
        CdSector cdSector = getSector(iSector);

        if (cdSector.getCdUserDataSize() != abSrcUserData.length)
            throw new IllegalArgumentException("Data to write is not the right size.");

        byte[] abRawData = cdSector.rebuildRawSector(abSrcUserData);

        int iFilePointer = _sectorFactory.getFilePointer(iSector);

        _cdReader.write(iFilePointer, abRawData);
    }

    //..........................................................................

    @Override
    public String toString() {
        return serialize();
    }

}
