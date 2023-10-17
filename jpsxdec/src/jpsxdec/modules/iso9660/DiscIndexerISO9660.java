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

package jpsxdec.modules.iso9660;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorHeader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.iso9660.DirectoryRecord;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.Misc;

/** Constructs the ISO9660 file system of a disc. */
public class DiscIndexerISO9660 extends DiscIndexer implements IdentifiedSectorListener<IIdentifiedSector> {

    private static final Logger LOG = Logger.getLogger(DiscIndexerISO9660.class.getName());

    private final ArrayList<SectorISO9660DirectoryRecords> _dirRecords =
            new ArrayList<SectorISO9660DirectoryRecords>();
    private final ArrayList<SectorISO9660VolumePrimaryDescriptor> _primaryDescriptors =
            new ArrayList<SectorISO9660VolumePrimaryDescriptor>();
    private final BitSet _sectorTypes = new BitSet();

    private static final int MODE2FORM1 = 0; // assume all sectors are this
    private static final int MODE2FORM2 = 1;
    private static final int CD_AUDIO = 2;
    private static final int MODE1 = 3; // currently ignored
    private void setSectorType(int iSector, int iType) {
        int iBit = iSector*2;
        if ((iType & 1) != 0)
            _sectorTypes.set(iBit);
        if ((iType & 2) != 0)
            _sectorTypes.set(iBit+1);
    }

    @Nonnull
    private final ILocalizedLogger _errLog;

    public DiscIndexerISO9660(@Nonnull ILocalizedLogger errLog) {
        _errLog = errLog;
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        if (DiscItemISO9660File.TYPE_ID.equals(fields.getType()))
            return new DiscItemISO9660File(getCd(), fields);
        return null;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        scs.addIdListener(this);
    }

    @Override
    public @Nonnull Class<IIdentifiedSector> getListeningFor() {
        return IIdentifiedSector.class;
    }

    @Override
    public void feedSector(@Nonnull IIdentifiedSector idSector, @Nonnull ILocalizedLogger log) throws LoggedFailure {
        IdentifiedSector isoSector;
        if (idSector instanceof SectorISO9660VolumePrimaryDescriptor ||
            idSector instanceof SectorISO9660DirectoryRecords)
        {
            isoSector = (IdentifiedSector) idSector;
        } else {
            isoSector = null;
        }
        isoSectorRead(idSector.getCdSector(), isoSector);

    }


    public void isoSectorRead(@Nonnull CdSector cdSector, @Nonnull IdentifiedSector idSector) {
        int iSectorType;
        switch (cdSector.getType()) {
            case CD_AUDIO:
                iSectorType = CD_AUDIO;
                break;
            case UNKNOWN2048:
            case MODE2FORM1:
                iSectorType = MODE2FORM1;
                break;
            case MODE2FORM2:
                iSectorType = MODE2FORM2;
                break;

            case MODE1:
                // Mode 1 is interesting, but not important to our needs
                // searching for bits later would be annoying while trying to ignore mode 1
                //iSectorType = MODE1;
                iSectorType = MODE2FORM1;
                break;
            default:
                throw new RuntimeException();
        }
        setSectorType(cdSector.getSectorIndexFromStart(), iSectorType);

        if (idSector instanceof SectorISO9660DirectoryRecords) {
            SectorISO9660DirectoryRecords dirRectSect =
                    (SectorISO9660DirectoryRecords) idSector;
            _dirRecords.add(dirRectSect);
        } else if (idSector instanceof SectorISO9660VolumePrimaryDescriptor) {
            SectorISO9660VolumePrimaryDescriptor volDescriptSect =
                    (SectorISO9660VolumePrimaryDescriptor) idSector;
            _primaryDescriptors.add(volDescriptSect);
        }
    }

    /** The difference between the sector number in raw sector headers
     * and the sector number from the start of the file.
     *
     * This is to handle the case where some starting portion of the disc
     * image is missing. In that case, the sector index will be smaller
     * than the sector header number.
     *
     * {@link DirectoryRecord}s refer to the location of child
     * {@link DirectoryRecord}s by their sector index, which should be
     * the same as the sector header number. This variable tries to
     * fix that issue so the sector index can align with the sector header
     * number.
     *
     * This will naturally be zero if the CD does not have sector header,
     * or no sectors are missing from the start of the file.
     *
     * The case where a sector is somehow skipped in the middle of the disc
     * image is not handled. */
    private int _iSectorNumberDiff = 0;

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {

        if (_primaryDescriptors.isEmpty()) {
            LOG.warning("Disc has no primary descriptor");
            // TODO try to build filesystem using only dirRecords
            return;
        } else if (_primaryDescriptors.size() > 1) {
            LOG.warning("Disc has more than 1 primary descriptors??");
            for (SectorISO9660VolumePrimaryDescriptor pd : _primaryDescriptors) {
                LOG.log(Level.WARNING, "{0}", pd);
            }
            return;
        }

        // calculate the possible diff
        _iSectorNumberDiff = 0;
        SectorISO9660VolumePrimaryDescriptor priDesc = _primaryDescriptors.get(0);
        CdSector cdSector = priDesc.getCdSector();
        CdSectorHeader header = cdSector.getHeader();
        if (header != null) {
            int iHeaderSector = header.calculateSectorNumber();
            if (iHeaderSector != -1) // if not CD audio
                _iSectorNumberDiff = iHeaderSector - cdSector.getSectorIndexFromStart();
        }

        // recursively build the file system
        DirectoryRecord rootDirRec = priDesc.getVPD().root_directory_record;
        if ((rootDirRec.flags & DirectoryRecord.FileFlags.Directory) == 0) {
            // odd, unlikely, impossible? case where there is only 1 file
            LOG.log(Level.WARNING, "Root directory record with only 1 file?? {0}", rootDirRec.toString());
            processDirectoryRecord_File(rootDirRec, new File(rootDirRec.name));
        } else {
            processDirectoryRecord_Directory(rootDirRec, null);
        }
    }

    @Override
    public void indexGenerated(@Nonnull DiscIndex discIndex) {
        // we have the name of the disc
        if (_primaryDescriptors.size() > 0)
            discIndex.setDiscName(_primaryDescriptors.get(0).getVPD().volume_id.trim());
    }

    // -------------------------------------------------------------------------
    // Recursively build the file system

    private void processDirectoryRecord(DirectoryRecord dirRec, @CheckForNull File parentPath) {
        // ignore '.' and '..' directory entries, otherwise infinite loop
        if (dirRec.name.equals(DirectoryRecord.CURRENT_DIRECTORY) ||
            dirRec.name.equals(DirectoryRecord.PARENT_DIRECTORY))
            return;

        File dirRecPath = new File(parentPath, dirRec.name);

        if ((dirRec.flags & DirectoryRecord.FileFlags.Directory) == 0) {
            processDirectoryRecord_File(dirRec, dirRecPath);
        } else {
            processDirectoryRecord_Directory(dirRec, dirRecPath);
        }
    }

    private void processDirectoryRecord_Directory(@Nonnull DirectoryRecord dirRec, @CheckForNull File dirRecPath) {
        assert (dirRec.flags & DirectoryRecord.FileFlags.Directory) != 0;

        // return if this is an empty directory
        if (dirRec.extent == 0 || dirRec.size == 0)
            return;

        // directory records can span multiple sectors
        for (int iSect = 0; iSect < dirRec.size / 2048; iSect++) {

            SectorISO9660DirectoryRecords dirRecSect = getDirRecSector((int)(dirRec.extent + _iSectorNumberDiff + iSect));
            // we may not have got a directory record at that sector :(
            if (dirRecSect != null) {

                // walk through the entries in this sector's directory record
                for (DirectoryRecord childDirRec : dirRecSect.getRecords()) {
                    processDirectoryRecord(childDirRec, dirRecPath);
                }
            }
        }
    }

    /** Try to find a {@link SectorISO9660DirectoryRecords} with the given
     * sector index among all the directory records collected. */
    private @CheckForNull SectorISO9660DirectoryRecords getDirRecSector(int iSector) {
        for (SectorISO9660DirectoryRecords dirRec : _dirRecords) {
            if (dirRec.getSectorNumber() == iSector)
                return dirRec;
        }
        return null;
    }

    private void processDirectoryRecord_File(@Nonnull DirectoryRecord fileDirRec, @Nonnull File filePath) {
        assert (fileDirRec.flags & DirectoryRecord.FileFlags.Directory) == 0;

        long lngFileSize = fileDirRec.size;
        long lngStartSector = fileDirRec.extent;

        long lngSectLength = (lngFileSize+2047) / 2048; // round up to nearest sector

        if (lngStartSector > Integer.MAX_VALUE ||
            lngSectLength > Integer.MAX_VALUE ||
            lngStartSector + lngSectLength > Integer.MAX_VALUE)
        {
            // the file is out of this world :O
            LOG.log(Level.SEVERE, "File with impossible start sector {0} or length {1}", new Object[]{lngStartSector, lngSectLength});
            _errLog.log(Level.SEVERE, I.ISO_FILE_CORRUPTED_IGNORING( fileDirRec.name));
            return;
        }

        int iStartSector = (int)lngStartSector;
        int iEndSector;
        if (lngFileSize == 0) {
            // 0 sized files still seem to be given a single
            // blank sector
            iEndSector = iStartSector;
        } else {
            int iSectLength = (int)lngSectLength;
            iEndSector = iStartSector + iSectLength - 1;
        }

        addFileDiscItem(iStartSector, iEndSector, filePath, lngFileSize);
    }

    private void addFileDiscItem(int iStartSector, int iEndSector, @Nonnull File filePath, long lngFileSize) {
        // TODO add warning if any files overlap (Felony 11-79)

        if (iEndSector > getCd().getSectorCount()) {
            // the file sectors protrude off the end of the disc
            // could happen if the image has been trimmed
            // but some games just have it intentionally
            _errLog.log(Level.WARNING,
                        I.NOT_CONTAINED_IN_DISC(Misc.forwardSlashPath(filePath)));
        }

        // having collected the sector types of the entire image
        // we can now search for any non 2352 sized sectors
        // within the file boundaries
        boolean blnHasCdAudio = false;
        boolean blnHasMode2Form2 = false;
        int iBit = iStartSector * 2;
        int iEndBit = (iEndSector+1) * 2;
        while (!blnHasMode2Form2 && !blnHasCdAudio) {
            int iSetBitPos = _sectorTypes.nextSetBit(iBit);
            if (iSetBitPos < 0 || iSetBitPos >= iEndBit)
                break;
            if (iSetBitPos % 2 == 0) {
                blnHasMode2Form2 = true;
            } else {
                blnHasCdAudio = true;
            }
            iBit = iSetBitPos;
        }

        addDiscItem(new DiscItemISO9660File(
                    getCd(), iStartSector, iEndSector,
                    filePath, lngFileSize,
                    blnHasMode2Form2, blnHasCdAudio));

    }

    // -------------------------------------------------------------------------

    @Override
    public void listPostProcessing(Collection<DiscItem> allItems) {
    }
    @Override
    public boolean filterChild(DiscItem parent, DiscItem child) {
        return false;
    }

}
