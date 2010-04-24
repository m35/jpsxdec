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

package jpsxdec.modules.iso9660;

import jpsxdec.modules.JPSXModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.modules.IndexingDemuxerIS;
import jpsxdec.modules.DiscItemSerialization;
import jpsxdec.modules.DiscItem;
import jpsxdec.modules.DiscIndex;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.modules.psx.video.bitstreams.BitStreamUncompressor;

/**
 * Collects all ISO9660 type sectors it runs across.
 * This information can be useful for various purposes after indexing.
 *
 */
public class JPSXModuleISO9660 extends JPSXModule {

    private static final Logger log = Logger.getLogger(JPSXModuleISO9660.class.getName());

    private static JPSXModuleISO9660 SINGLETON;

    public static JPSXModuleISO9660 getModule() {
        if (SINGLETON == null)
            SINGLETON = new JPSXModuleISO9660();
        return SINGLETON;
    }

    private ArrayList<SectorISO9660DirectoryRecords> _dirRecords =
            new ArrayList<SectorISO9660DirectoryRecords>();
    private ArrayList<SectorISO9660PathTable> _pathTables =
            new ArrayList<SectorISO9660PathTable>();
    private ArrayList<SectorISO9660VolumePrimaryDescriptor> _primaryDescriptors =
            new ArrayList<SectorISO9660VolumePrimaryDescriptor>();
    
    private JPSXModuleISO9660() {
        
    }

    @Override
    public IdentifiedSector identifySector(CDSector cdSector) {
        try { return new SectorISO9660DirectoryRecords(cdSector); }
        catch (NotThisTypeException ex) {}
        try { return new SectorISO9660VolumePrimaryDescriptor(cdSector); }
        catch (NotThisTypeException ex) {}
//        try { return new SectorISO9660PathTable(cdSector); }
//        catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void indexing_sectorRead(IdentifiedSector identifiedSect) {
        if (identifiedSect instanceof SectorISO9660DirectoryRecords) {
            SectorISO9660DirectoryRecords oDirRectSect =
                    (SectorISO9660DirectoryRecords) identifiedSect;
            _dirRecords.add(oDirRectSect);
        } else if (identifiedSect instanceof SectorISO9660PathTable) {
            SectorISO9660PathTable oPathTableSect =
                    (SectorISO9660PathTable) identifiedSect;
            _pathTables.add(oPathTableSect);
        } else if (identifiedSect instanceof SectorISO9660VolumePrimaryDescriptor) {
            SectorISO9660VolumePrimaryDescriptor oVolDescriptSect =
                    (SectorISO9660VolumePrimaryDescriptor) identifiedSect;
            _primaryDescriptors.add(oVolDescriptSect);
        }
    }

    @Override
    public void indexing_endOfDisc() {
        _dirRecords.trimToSize();
        _pathTables.trimToSize();
        _primaryDescriptors.trimToSize();
        
        if (_dirRecords.size() == 0 &&
            _pathTables.size() == 0 &&
            _primaryDescriptors.size() == 0)
        {
            return;
        }
        
        
        if (_primaryDescriptors.size() > 1) {
            log.warning("Disc has more than 1 primary descriptors??");
            for (SectorISO9660VolumePrimaryDescriptor pd : _primaryDescriptors) {
                log.warning(pd.toString());
            }
        } else if (_primaryDescriptors.size() == 1) {
            SectorISO9660VolumePrimaryDescriptor oPriDesc = _primaryDescriptors.get(0);
            CDSector oCDSect = oPriDesc.getCDSector();
            _iSectorOffset = oCDSect.getHeaderSectorNumber() - oCDSect.getSectorNumberFromStart();
            
            getFileList(oPriDesc.getVPD().root_directory_record, new File(""));
        }
    }

    private int _iSectorOffset = 0;
    
    private void getFileList(DirectoryRecord rec, File oDir) {

        // return if this isn't a directory, or if an empty directory
        if ((rec.flags & DirectoryRecord.FLAG_IS_DIRECTORY) == 0 || 
             rec.extent == 0 || rec.size == 0) 
            return;

        for (int iSect = 0; iSect < rec.size / 2048; iSect++) {

            SectorISO9660DirectoryRecords oDirRecSect = getDirRecSect((int)(rec.extent + _iSectorOffset + iSect));
            if (oDirRecSect == null) return;

            for (DirectoryRecord dr : oDirRecSect.getRecords()) {

                if (!dr.name.equals(".") && !dr.name.equals("..")) {
                    File drdir = new File(oDir, dr.name);
                    int iSectLength = (int)((dr.size+2047) / 2048); // round up to nearest sector
                    super.addDiscItem(new DiscItemISO9660File((int)dr.extent, (int)(dr.extent + iSectLength - 1), drdir, dr.size));
                    getFileList(dr, drdir);
                }
            }
        }
    }
    
    @Override
    public void deserialize_lineRead(DiscItemSerialization fields) {
        try {
            if (DiscItemISO9660File.TYPE_ID.equals(fields.getType()))
                super.addDiscItem(new DiscItemISO9660File(fields));
        } catch (NotThisTypeException ex) {}
    }

    @Override
    public void indexing_static(IndexingDemuxerIS indexingIS) throws IOException {
        // this doesn't create static items
    }

    public ArrayList<SectorISO9660DirectoryRecords> getDirectoryRecords() {
        return _dirRecords;
    }

    public ArrayList<SectorISO9660PathTable> getPathTables() {
        return _pathTables;
    }

    public ArrayList<SectorISO9660VolumePrimaryDescriptor> getPrimaryDescriptors() {
        return _primaryDescriptors;
    }

    @Override
    public void mediaListGenerated(DiscIndex discIndex) {
        super.mediaListGenerated(discIndex);

        for (SectorISO9660PathTable pathTable : _pathTables) {
            log.info(pathTable.toString());
        }

        if (_primaryDescriptors.size() > 0)
            discIndex.setDiscName(_primaryDescriptors.get(0).getVPD().volume_id.trim());
        
        for (DiscItem nonFile : discIndex) {
            DiscItemISO9660File mostOverlap = null;
            int iMostOverlap = 0;
            if (!(nonFile instanceof DiscItemISO9660File)) {
                for (DiscItem isFile : discIndex) {
                    if (isFile instanceof DiscItemISO9660File) {
                        DiscItemISO9660File fileItem = (DiscItemISO9660File)isFile;
                        int iOverlap = fileItem.getOverlap(nonFile);
                        if (iOverlap > iMostOverlap) {
                            if (mostOverlap != null) {
                                log.warning(nonFile + " intersects multiple files!");
                            }
                            mostOverlap = fileItem;
                            iMostOverlap = iOverlap;
                        }
                    }
                }
            }

            if (mostOverlap != null) {
                mostOverlap.addChild(nonFile);
            }
        }
    }
    
    private SectorISO9660DirectoryRecords getDirRecSect(int iSector) {
        for (SectorISO9660DirectoryRecords oDirRec : _dirRecords) {
            if (oDirRec.getSectorNumber() == iSector)
                return oDirRec;
        }
        return null;
    }

    @Override
    public String getModuleDescription() {
        return "ISO9660 Module for jPSXdec by Michael Sabin";
    }

    @Override
    public BitStreamUncompressor identifyVideoFrame(byte[] abHeaderBytes,
                                                     long lngFrameNum)
    {
        return null;
    }
}
