/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * PSXSector.java
 */

package jpsxdec.sectortypes;

import java.io.*;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.sectortypes.PSXSectorFF8.*;
import jpsxdec.sectortypes.PSXSectorFF9.*;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.NotThisTypeException;

/** Base class for all PSX sector types. Encapsulates raw sectors with
 *  special meaning. Note that this doesn't store the whole underlying raw 
 *  sector data in order to save memory. */
public abstract class PSXSector {
    

    /** Identify the type of the supplied sector. */
    public static PSXSector SectorIdentifyFactory(CDXASector oCDSect) {
        if (oCDSect == null) return null;
        
        PSXSector oPsxSector;
        
        try {
            oPsxSector = new PSXSectorNull(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFrameChunk(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorAudioChunk(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFF8Video(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFF8Audio(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFF9Video(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFF9Audio(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorChronoXAudio(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorAudio2048(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorAliceFrameChunk(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        // we dunno what this sector is, default to PSXSectorUnknownData
        oPsxSector = new PSXSectorUnknownData(oCDSect);
        return oPsxSector;
        
    }
    
    /**************************************************************************/
    /**************************************************************************/
    
    /** Save the basic sector info. */
    protected final long m_iFile;
    /** Save the basic sector info. */
    protected int m_iChannel;
    /** Save the basic sector info. */
    protected final int m_iSector;
    /** The original file offset where the sector was found. */
    protected final long m_lngSectorFilePointer;
    
    protected final String m_sSubMode;
    
    /** The 'user data' portion of the CDXA sector (without the raw headers). */
    protected ByteArrayFPIS m_abUserData;
    
    public PSXSector(CDXASector oCDSect) {
        m_iSector = oCDSect.getSector();
        m_iFile = oCDSect.getFile();
        m_iChannel = oCDSect.getChannel();
        m_lngSectorFilePointer = oCDSect.getFilePointer();
        m_abUserData = oCDSect.getSectorDataStream();
        if (oCDSect.hasSectorHeader())
            m_sSubMode = " Submode:" + oCDSect.getSubMode().toString();
        else
            m_sSubMode = "";
    }
    
    /** Extend this function in sub-classes to define what bytes
     * are part of the demuxed stream. */
    protected abstract int getDemuxedDataStart(int iDataSize);
    /** Extend this function in sub-classes to define what bytes
     * are part of the demuxed stream. */
    protected abstract int getDemuxedDataLength(int iDataSize);
    
    public int getPsxUserDataSize() {
        return getDemuxedDataLength(m_abUserData.size()); 
    }
    
    public ByteArrayFPIS getUserDataStream() {
        return new ByteArrayFPIS(m_abUserData, 
                getDemuxedDataStart(m_abUserData.size()), 
                getDemuxedDataLength(m_abUserData.size()), 
                m_lngSectorFilePointer);
    }
   
    /** Returns a string description of the sector type, or null if 
     *  the sector contains unknown data */
    public String toString() {
        return String.format("[Sector:%d File:%d Channel:%d%s]",
                m_iSector, m_iFile, m_iChannel, m_sSubMode);
    }
    
    /** The 'file' value in the raw CDXA header, or -1 if there was no header. */
    public long getFile() {
        return m_iFile;
    }
    
    /** The 'channel' value in the raw CDXA header, or -1 if there was no header. */
    public long getChannel() {
        return m_iChannel;
    }

    /** The sector number in the original file. */
    public int getSector() {
        return m_iSector;
    }
    
}


