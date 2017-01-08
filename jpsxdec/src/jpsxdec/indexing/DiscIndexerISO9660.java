/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemISO9660File;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.iso9660.DirectoryRecord;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorISO9660DirectoryRecords;
import jpsxdec.sectors.SectorISO9660VolumePrimaryDescriptor;
import jpsxdec.util.DeserializationFail;
import jpsxdec.util.ILocalizedLogger;

/**
 * Constructs the ISO9660 file system.
 */
public class DiscIndexerISO9660 extends DiscIndexer implements DiscIndexer.Identified {

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

    public void indexingSectorRead(@Nonnull CdSector cdSector,
                                   @CheckForNull IdentifiedSector idSector)
    {
        if (cdSector.isCdAudioSector())
            setSectorType(cdSector.getSectorNumberFromStart(), CD_AUDIO);
        else if (cdSector.hasSubHeader() && cdSector.getSubMode().getForm() == 2) // TODO: ugly
            setSectorType(cdSector.getSectorNumberFromStart(), MODE2FORM2);
        // Mode 1 is interesting, but not important to our needs
        // searching for bits later would be annoying trying to ignore mode 1
        //else if (cdSector.isMode1())
        //    setSectorType(cdSector.getSectorNumberFromStart(), MODE1);

        if (idSector instanceof SectorISO9660DirectoryRecords) {
            SectorISO9660DirectoryRecords oDirRectSect =
                    (SectorISO9660DirectoryRecords) idSector;
            _dirRecords.add(oDirRectSect);
        } else if (idSector instanceof SectorISO9660VolumePrimaryDescriptor) {
            SectorISO9660VolumePrimaryDescriptor oVolDescriptSect =
                    (SectorISO9660VolumePrimaryDescriptor) idSector;
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
        
        
        _iSectorNumberDiff = 0;
        if (_primaryDescriptors.size() > 1) {
            LOG.warning("Disc has more than 1 primary descriptors??");
            for (SectorISO9660VolumePrimaryDescriptor pd : _primaryDescriptors) {
                LOG.log(Level.WARNING, "{0}", pd);
            }
        } else if (_primaryDescriptors.size() == 1) {
            SectorISO9660VolumePrimaryDescriptor priDesc = _primaryDescriptors.get(0);
            CdSector cdSector = priDesc.getCdSector();
            if (cdSector.hasHeaderSectorNumber()) {
                int iHeaderSector = cdSector.getHeaderSectorNumber();
                if (iHeaderSector != -1)
                    _iSectorNumberDiff = iHeaderSector - cdSector.getSectorNumberFromStart();
            }
            getFileList(priDesc.getVPD().root_directory_record, null);
        }
    }

    /** The difference between the sector number in raw sector headers
     * and the sector number from the start of the file.
     * This is always 0 if there is no raw sector header, or if an entire
     * disc image is used. */
    private int _iSectorNumberDiff = 0;
    
    private void getFileList(@Nonnull DirectoryRecord rec, @CheckForNull File parentDir) {

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
                
                if ((childDr.flags & DirectoryRecord.FLAG_IS_DIRECTORY) == 0) {
                    long lngFileSize = childDr.size;
                    int iSectLength = (int)((lngFileSize+2047) / 2048); // round up to nearest sector
                    int iStartSector = (int)childDr.extent;
                    int iEndSector = iStartSector + iSectLength - 1;

                    // search for any non 2352 sized sectors in the file
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
                    
                    super.addDiscItem(new DiscItemISO9660File(
                            getCd(), iStartSector, iEndSector,
                            drDir, lngFileSize, blnHasMode2Form2, blnHasCdAudio));
                }
                getFileList(childDr, drDir);
            }
        }
    }
    
    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) 
            throws DeserializationFail
    {
        if (DiscItemISO9660File.TYPE_ID.equals(fields.getType()))
            return new DiscItemISO9660File(getCd(), fields);
        return null;
    }

    public @Nonnull ArrayList<SectorISO9660DirectoryRecords> getDirectoryRecords() {
        return _dirRecords;
    }

    public @Nonnull ArrayList<SectorISO9660VolumePrimaryDescriptor> getPrimaryDescriptors() {
        return _primaryDescriptors;
    }

    @Override
    public void listPostProcessing(Collection<DiscItem> allItems) {
    }

    @Override
    public void indexGenerated(@Nonnull DiscIndex discIndex) {
        if (_primaryDescriptors.size() > 0)
            discIndex.setDiscName(_primaryDescriptors.get(0).getVPD().volume_id.trim());
    }
    
    private @CheckForNull SectorISO9660DirectoryRecords getDirRecSect(int iSector) {
        for (SectorISO9660DirectoryRecords oDirRec : _dirRecords) {
            if (oDirRec.getSectorNumber() == iSector)
                return oDirRec;
        }
        return null;
    }

}
