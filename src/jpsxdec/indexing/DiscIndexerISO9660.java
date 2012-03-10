/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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

package jpsxdec.indexing;

import jpsxdec.sectors.SectorISO9660DirectoryRecords;
import jpsxdec.sectors.SectorISO9660VolumePrimaryDescriptor;
import jpsxdec.discitems.DiscItemISO9660File;
import jpsxdec.iso9660.DirectoryRecord;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemSerialization;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;

/**
 * Constructs the ISO9660 file system.
 */
public class DiscIndexerISO9660 extends DiscIndexer {

    private static final Logger log = Logger.getLogger(DiscIndexerISO9660.class.getName());

    private final ArrayList<SectorISO9660DirectoryRecords> _dirRecords =
            new ArrayList<SectorISO9660DirectoryRecords>();
    private final ArrayList<SectorISO9660VolumePrimaryDescriptor> _primaryDescriptors =
            new ArrayList<SectorISO9660VolumePrimaryDescriptor>();

    private final Logger _errLog;

    public DiscIndexerISO9660(Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public void indexingSectorRead(IdentifiedSector identifiedSect) {
        if (identifiedSect instanceof SectorISO9660DirectoryRecords) {
            SectorISO9660DirectoryRecords oDirRectSect =
                    (SectorISO9660DirectoryRecords) identifiedSect;
            _dirRecords.add(oDirRectSect);
        } else if (identifiedSect instanceof SectorISO9660VolumePrimaryDescriptor) {
            SectorISO9660VolumePrimaryDescriptor oVolDescriptSect =
                    (SectorISO9660VolumePrimaryDescriptor) identifiedSect;
            _primaryDescriptors.add(oVolDescriptSect);
        }
    }

    @Override
    public void indexingEndOfDisc() {
        _dirRecords.trimToSize();
        _primaryDescriptors.trimToSize();
        
        if (_dirRecords.isEmpty() && _primaryDescriptors.isEmpty())
        {
            return;
        }
        
        
        if (_primaryDescriptors.size() > 1) {
            log.warning("Disc has more than 1 primary descriptors??");
            for (SectorISO9660VolumePrimaryDescriptor pd : _primaryDescriptors) {
                log.warning(pd.toString());
            }
        } else if (_primaryDescriptors.size() == 1) {
            SectorISO9660VolumePrimaryDescriptor priDesc = _primaryDescriptors.get(0);
            CdSector cdSector = priDesc.getCDSector();
            if (cdSector.hasHeaderSectorNumber())
                _iSectorNumberDiff = cdSector.getHeaderSectorNumber() - cdSector.getSectorNumberFromStart();
            else
                _iSectorNumberDiff = 0;
            getFileList(priDesc.getVPD().root_directory_record, null);
        }
    }

    /** The difference between the sector number in raw sector headers
     * and the sector number from the start of the file.
     * This is always 0 if there is no raw sector header, and if an entire
     * disc image is used. */
    private int _iSectorNumberDiff = 0;
    
    private void getFileList(DirectoryRecord rec, File parentDir) {

        // return if this isn't a directory, or if an empty directory
        if ((rec.flags & DirectoryRecord.FLAG_IS_DIRECTORY) == 0 || 
             rec.extent == 0 || rec.size == 0) 
            return;

        for (int iSect = 0; iSect < rec.size / 2048; iSect++) {

            SectorISO9660DirectoryRecords dirRecSect = getDirRecSect((int)(rec.extent + _iSectorNumberDiff + iSect));
            if (dirRecSect == null) return;

            for (DirectoryRecord childDr : dirRecSect.getRecords()) {

                if (childDr.name.equals(".") || childDr.name.equals(".."))
                    continue;

                // TODO: cleanup this mess
                File drDir;
                if (parentDir == null)
                    drDir = new File(childDr.name);
                else
                    drDir = new File(parentDir, childDr.name);
                if ((childDr.flags & DirectoryRecord.FLAG_IS_DIRECTORY) == 0)
                {
                    int iSectLength = (int)((childDr.size+2047) / 2048); // round up to nearest sector
                    super.addDiscItem(new DiscItemISO9660File(
                            (int)childDr.extent, (int)(childDr.extent + iSectLength - 1),
                            drDir, childDr.size));
                }
                getFileList(childDr, drDir);
            }
        }
    }
    
    @Override
    public DiscItem deserializeLineRead(DiscItemSerialization fields) {
        try {
            if (DiscItemISO9660File.TYPE_ID.equals(fields.getType())) {
                return new DiscItemISO9660File(fields);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void staticRead(DemuxedUnidentifiedDataStream indexingIS) throws IOException {
        // this doesn't create static items
    }

    public ArrayList<SectorISO9660DirectoryRecords> getDirectoryRecords() {
        return _dirRecords;
    }

    public ArrayList<SectorISO9660VolumePrimaryDescriptor> getPrimaryDescriptors() {
        return _primaryDescriptors;
    }

    @Override
    public void mediaListGenerated(DiscIndex discIndex) {

        if (_primaryDescriptors.size() > 0)
            discIndex.setDiscName(_primaryDescriptors.get(0).getVPD().volume_id.trim());
    }
    
    private SectorISO9660DirectoryRecords getDirRecSect(int iSector) {
        for (SectorISO9660DirectoryRecords oDirRec : _dirRecords) {
            if (oDirRec.getSectorNumber() == iSector)
                return oDirRec;
        }
        return null;
    }

}
