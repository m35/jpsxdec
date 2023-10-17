/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.util.IO;
import jpsxdec.util.TaskCanceledException;

/** Collects a set of changes to be made to a {@link ICdSectorReader},
 * and then apples all the changes at one time.
 * This is better than applying each individual change immediately
 * because it leaves the CD unchanged if an error occurs, and it can be used
 * to do a 'dry run' of the changes.
 * Note that due to its intentionally simplistic design, this patch feature
 * could never be used as a general method to build and distribute patches. */
public class DiscPatcher implements Closeable {

    private static final Logger LOG = Logger.getLogger(DiscPatcher.class.getName());

    public static class CreatePatchFileException extends IOException {
        @Nonnull
        private final File _file;

        public CreatePatchFileException(File file, IOException cause) {
            super(cause);
            _file = file;
        }

        public File getFile() {
            return _file;
        }

    }

    public static class WritePatchException extends IOException {
        @Nonnull
        private final File _file;

        public WritePatchException(File file, IOException cause) {
            super(cause);
            _file = file;
        }

        public File getFile() {
            return _file;
        }
    }

    public static class PatchReadException extends IOException {
        @Nonnull
        private final File _file;

        public PatchReadException(File file, IOException cause) {
            super(cause);
            _file = file;
        }

        public File getFile() {
            return _file;
        }
    }


    private static class PatchEntry implements Comparable<PatchEntry> {
        public final int iSector;
        public final int iOffsetInSector;
        public final int iNumberOfBytesToReplace;
        public final long lngOffsetInPatchFile;

        public PatchEntry(int iSector, int iOffsetInSector,
                          int iNumberOfBytesToReplace, long lngOffsetInPatchFile)
        {
            if (iSector < 0 ||
                iOffsetInSector < 0 ||
                iOffsetInSector + iNumberOfBytesToReplace > CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL)
                throw new IllegalArgumentException();
            this.iSector = iSector;
            this.iOffsetInSector = iOffsetInSector;
            this.iNumberOfBytesToReplace = iNumberOfBytesToReplace;
            this.lngOffsetInPatchFile = lngOffsetInPatchFile;
        }

        @Override
        public int compareTo(PatchEntry o) {
            int i = Integer.compare(iSector, o.iSector);
            if (i != 0)
                return i;
            boolean blnNoOverlap =
                    ((iOffsetInSector + iNumberOfBytesToReplace) < o.iOffsetInSector) ||
                    ((o.iOffsetInSector + o.iNumberOfBytesToReplace) < iOffsetInSector);
            if (blnNoOverlap) {
                return Integer.compare(iOffsetInSector, o.iOffsetInSector);
            } else {
                // here we abuse the comparison logic:
                // by being equal, the collection will report that it could not
                // add a duplicate object,
                // but we know that means there was some overlap,
                // so an exception should be thrown
                return 0;
            }
        }

        @Override
        public boolean equals(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }
    }

    private static final String TEMP_FILE_PREFIX = "jpsxdec-patch";
    private static final String TEMP_FILE_SUFFIX = ".temp";

    @Nonnull
    private final File _patchFileName;
    @CheckForNull
    private RandomAccessFile _patchFile;
    private final TreeSet<PatchEntry> _patches = new TreeSet<PatchEntry>();

    public DiscPatcher() throws CreatePatchFileException {
        try {
            _patchFileName = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            _patchFileName.deleteOnExit();
        } catch (IOException ex) {
            File nearlyTempFileName = new File(TEMP_FILE_PREFIX + "???" + TEMP_FILE_SUFFIX);
            throw new CreatePatchFileException(nearlyTempFileName, ex);
        }
        try {
            _patchFile = new RandomAccessFile(_patchFileName, "rw");
        } catch (FileNotFoundException ex) {
            throw new CreatePatchFileException(_patchFileName, ex);
        }
    }

    public @Nonnull File getTempFile() {
        return _patchFileName;
    }

    public void addPatch(int iSector, int iOffsetInSector,
                         @Nonnull byte[] abBytesToReplace)
            throws DiscPatcher.WritePatchException
    {
        addPatch(iSector, iOffsetInSector, abBytesToReplace,
                 0, abBytesToReplace.length);
    }

    public void addPatch(int iSector,
                         int iOffsetInSector, @Nonnull byte[] abBytesToReplace,
                         int iStartByteToUse, int iNumberOfBytesToReplace)
            throws WritePatchException
    {
        if (_patchFile == null)
            throw new IllegalStateException();

        LOG.log(Level.INFO, "Patch sector {0} @{1} {2} bytes",
                new Object[]{iSector, iOffsetInSector, iNumberOfBytesToReplace});
        try {
            PatchEntry patch = new PatchEntry(iSector, iOffsetInSector,
                                              iNumberOfBytesToReplace,
                                              _patchFile.getFilePointer());
            if (!_patches.add(patch)) {
                // if it could not be added, it means there is overlap
                // which is not allowed
                throw new IllegalArgumentException("Replacement overlap");
            }
            _patchFile.write(abBytesToReplace, iStartByteToUse, iNumberOfBytesToReplace);
        } catch (IOException ex) {
            // if getFilePointer() or write() fail
            throw new WritePatchException(_patchFileName, ex);
        }
    }

    /** Applies the patches then silently discards the temp patch file.
     *
     * @throws jpsxdec.cdreaders.CdFileSectorReader.CdReopenException
     *         error reopening cd for writing
     * @throws CdException.Read
     *         error reading from cd while patching
     * @throws jpsxdec.cdreaders.CdFileSectorReader.CdWriteException
     *         error writing to cd while patching
     * @throws jpsxdec.cdreaders.DiscPatcher.PatchReadException
     *         error reading temp patch file while patching
     * @throws TaskCanceledException hopefully this doesn't happen
     */
    public void applyPatches(@Nonnull CdFileSectorReader cd, @Nonnull ProgressLogger pl)
            throws CdException.Read,
                   CdFileSectorReader.CdReopenException,
                   CdFileSectorReader.CdWriteException,
                   PatchReadException,
                   TaskCanceledException
    {
        if (_patches.isEmpty()) {
            LOG.warning("Nothing to patch");
            discard();
            pl.progressStart(0);
            pl.progressEnd();
            return;
        }

        PatchEntry last = _patches.last();
        if (last.iSector >= cd.getSectorCount())
            throw new IllegalArgumentException("Patches for sectors outside of disc");
        // TODO to be extra safe, could track the max index of bytes changed
        // and check if it fits in the sector size of the disc before starting to patch

        Iterator<PatchEntry> it = _patches.iterator();

        pl.progressStart(_patches.size());

        cd.reopenForWriting();

        CdSector sector = null;
        byte[] abUserData = null;

        try {
            for (int i = 0; it.hasNext(); i++) {
                PatchEntry patch = it.next();

                if (sector != null) {
                    // don't apply the changes until a sector is fully patched
                    if (patch.iSector != sector.getSectorIndexFromStart()) {
                        // write
                        pl.log(Level.INFO, new UnlocalizedMessage("Writing patched sector " + sector.getSectorIndexFromStart()));
                        assert abUserData != null; // is not null if sector is not null
                        cd.writeSector(sector.getSectorIndexFromStart(), abUserData);
                        sector = null;
                        abUserData = null;
                    }
                }

                if (sector == null) {
                    sector = cd.getSector(patch.iSector);
                    abUserData = sector.getCdUserDataCopy();
                }

                try {
                    _patchFile.seek(patch.lngOffsetInPatchFile);
                    assert abUserData != null; // is not null if sector is not null
                    IO.readByteArray(_patchFile, abUserData, patch.iOffsetInSector, patch.iNumberOfBytesToReplace);
                } catch (IOException ex) {
                    throw new PatchReadException(_patchFileName, ex);
                }

                if (pl.isSeekingEvent())
                    pl.event(new UnlocalizedMessage("Applying patch " + i + " of " + _patches.size()));
                pl.progressUpdate(i);
            }
            if (sector != null) {
                // final write
                assert abUserData != null; // is not null if sector is not null
                cd.writeSector(sector.getSectorIndexFromStart(), abUserData);
            }

            pl.progressEnd();
        } finally {
            // if the patches were applied, discard the temp file.
            // if there were errors, discard it as well because the disc
            // image is now in a broken state and this shouldn't be used again
            discard();
        }
    }

    /** Silently throws away the temp patch file. */
    public void discard() {
        if (_patchFile == null)
            return;
        IO.closeSilently(_patchFile, LOG);
        _patchFile = null;
    }

    /** Same as {@link #discard()}. */
    @Override
    public void close() {
        discard();
    }
}
