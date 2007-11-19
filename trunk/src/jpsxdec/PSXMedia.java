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
 * PSXMedia.java
 *
 */

package jpsxdec;

import java.util.*;
import java.io.*;
import jpsxdec.CDSectorReader.CDXASector;
import jpsxdec.util.LittleEndianIO;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.PSXSector.*;



/** Abstract CD media type. Subclasses currently are only STR and XA */
public abstract class PSXMedia {
    
    public static int DebugVerbose = 2;
    
    /** Finds all the media on the CD */
    public static ArrayList<PSXMedia> IndexCD(CDSectorReader oCD)
            throws IOException 
    {
        
        PSXSectorRangeIterator oSectIterator = new PSXSectorRangeIterator(oCD);
        
        ArrayList<PSXMedia> oMediaList = new ArrayList<PSXMedia>();
        
        // If it doesn't even have 1 sector
        if (!oSectIterator.hasNext()) return oMediaList;
        
        PSXMedia oPsxMedia = null;
        while (oSectIterator.hasNext()) {
            
            try {
                oPsxMedia = new PSXMediaSTR(oSectIterator);
                if (DebugVerbose > 3)
                    System.err.println(oPsxMedia.toString());
                oMediaList.add(oPsxMedia);
                if (!oSectIterator.hasNext()) break;
            } catch (NotThisTypeException e1) {
                try {
                    oPsxMedia = new PSXMediaXA(oSectIterator);
                    if (DebugVerbose > 3)
                        System.err.println(oPsxMedia.toString());
                    oMediaList.add(oPsxMedia);
                    if (!oSectIterator.hasNext()) break;
                } catch (NotThisTypeException e2) {
                    try {
                        oPsxMedia = new PSXMediaTIM(oSectIterator);
                        if (DebugVerbose > 3)
                            System.err.println(oPsxMedia.toString());
                        oMediaList.add(oPsxMedia);
                        if (!oSectIterator.hasNext()) break;
                    } catch (NotThisTypeException e3) {
                        if (!oSectIterator.hasNext()) break;
                        oSectIterator.skipNext();
                    }
                }
            }
            
        }
        
        return oMediaList;
    }
    
    /** Deserializes the CD index file, and creates a
     *  list of media items on the CD */
    public static ArrayList<PSXMedia> IndexCD(CDSectorReader oCD,
                                              String sSerialFile)
            throws IOException 
    {
        //TODO: Want to read as much as we can from the file before an exception
        //      and return what we can get, but we also want to return that
        //      there was an error.
        /*
        ###:STR:<start sector>-<end sector>:<start-frame>-<end-frame>
        ###:XA:<start sector>-<end sector>:#,#,#,...,#
         */
        
        ArrayList<PSXMedia> oMediaList = new ArrayList<PSXMedia>();
        
        PSXMedia oPsxMedia = null;
        BufferedReader oReader =
                new BufferedReader(new FileReader(sSerialFile));
        String sLine;
        while ((sLine = oReader.readLine()) != null) {
            
            try {
                if (sLine.substring(3, 8).equals(":STR:")) {
                    oPsxMedia = new PSXMediaSTR(oCD, sLine);
                    oMediaList.add(oPsxMedia);
                } else if (sLine.substring(3, 7).equals(":XA:")) {
                    oPsxMedia = new PSXMediaXA(oCD, sLine);
                    oMediaList.add(oPsxMedia);
                } else if (sLine.substring(3, 8).equals(":TIM:")) {
                    oPsxMedia = new PSXMediaTIM(oCD, sLine);
                    oMediaList.add(oPsxMedia);
                }
            } catch (NotThisTypeException e1) {}
        }
        oReader.close();
        
        return oMediaList;
    }
    
    /** Serializes a list of media items to a file */
    public static void SerializeMediaList(AbstractList<PSXMedia> oMediaList,
                                          PrintStream oPrinter)
            throws IOException 
    {
        oPrinter.println("# Lines that begin with # are comments");
        oPrinter.println("# Format:");
        oPrinter.println("#   media_num:STR:start_sector-end_sector:frame_start-frame_end");
        oPrinter.println("#   media_num:XA:start_sector-end_sector:list,of,channels");
        oPrinter.println("#   media_num:TIM:start_sector-end_sector:start_sector_offset");
        int i = 0;
        for (PSXMedia oMedia : oMediaList) {
            oPrinter.println(String.format("%03d:%s", i, oMedia.toString()));
            i++;
        }
        oPrinter.close();
    }
    
    /** This class is used only during indexing. */
    private static class AudioChannelInfo {
        public long Channel          = -1;
        public long BitsPerSampele   = -1;
        public long SamplesPerSecond = -1;
        public long MonoStereo       = -1;
        public long LastAudioSect    = -1;
    }
    
    protected int m_iStartSector;
    protected int m_iEndSector;
    protected CDSectorReader m_oCD;
    
    public PSXMedia(PSXSectorRangeIterator oSectIterator) {
        m_oCD = oSectIterator.getSourceCD();
    }
    
    /** Read in the start and end sectors */
    public PSXMedia(CDSectorReader oCD, String sSerial, String sType)
            throws NotThisTypeException 
    {
        m_oCD = oCD;
        String asParts[] = sSerial.split(":");
        if (asParts.length < 2)
            throw new NotThisTypeException();
        if (!asParts[1].equals(sType))
            throw new NotThisTypeException();
        String asSectors[] = asParts[2].split("-");
        try {
            m_iStartSector = Integer.parseInt(asSectors[0]);
            m_iEndSector = Integer.parseInt(asSectors[1]);
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
    public long getStartSector() {
        return m_iStartSector;
    }
    public long getEndSector() {
        return m_iEndSector;
    }
    
    public String toString() {
        return m_iStartSector + "-" + m_iEndSector;
    }
    
    /**************************************************************************/
    /** Sub-classes ***********************************************************/
    /**************************************************************************/
    
    public static class PSXMediaSTR extends PSXMedia {
        
        long m_lngAudioSampleLength = 0;
        
        long m_iCurFrame = -1;
        long m_iWidth = -1;
        long m_iHeight = -1;
        
        long m_lngStartFrame = -1;
        long m_lngEndFrame = -1;
        
        public PSXMediaSTR(PSXSectorRangeIterator oSectIterator)
                throws NotThisTypeException, IOException 
        {
            super(oSectIterator);
            
            long iAudioPeriod = -1;
            AudioChannelInfo oAudInf = null;
            
            PSXSector oPsxSect = oSectIterator.peekNext();
            
            if (!(oPsxSect instanceof IVideoChunkSector) &&
                    !(oPsxSect instanceof PSXSectorFF8AudioChunk))
                throw new NotThisTypeException();
            
            if (DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
            IVideoChunkSector oFrame = (IVideoChunkSector)oPsxSect;
            PSXSectorAudioChunk oAudio;
            
            m_iWidth = oFrame.getWidth();
            m_iHeight = oFrame.getHeight();
            m_iCurFrame = oFrame.getFrameNumber();
            
            m_iStartSector = oPsxSect.getSector();
            m_iEndSector = m_iStartSector;
            
            m_lngStartFrame = oFrame.getFrameNumber();
            m_lngEndFrame = oFrame.getFrameNumber();
            
            oSectIterator.skipNext();
            while (oSectIterator.hasNext()) {
                oPsxSect = oSectIterator.peekNext();
                
                if (oPsxSect instanceof PSXSectorNull) {
                    // just skip it
                } else if (oPsxSect instanceof PSXSectorAudio2048) {
                    // just skip it
                } else if (oPsxSect instanceof IVideoChunkSector) {
                    
                    oFrame = (IVideoChunkSector)oPsxSect;
                    if (oFrame.getWidth() == m_iWidth &&
                            oFrame.getHeight() == m_iHeight &&
                            (oFrame.getFrameNumber() == m_iCurFrame ||
                            oFrame.getFrameNumber() == m_iCurFrame+1)) {
                        
                        m_iCurFrame = oFrame.getFrameNumber();
                        m_lngEndFrame = m_iCurFrame;
                    } else {
                        break;
                    }
                    
                    m_iEndSector = oPsxSect.getSector();
                } else if (oPsxSect instanceof IAudioSector) {
                    oAudio = (PSXSectorAudioChunk)oPsxSect;
                    
                    if (oAudInf != null) {
                        
                        if (oAudio.getMonoStereo() != oAudInf.MonoStereo ||
                                oAudio.getSamplesPerSecond() != oAudInf.SamplesPerSecond ||
                                oAudio.getBitsPerSample() != oAudInf.BitsPerSampele ||
                                oAudio.getChannel() != oAudInf.Channel) {
                            break;
                        }
                        
                        if ((oPsxSect.getSector() - oAudInf.LastAudioSect) != iAudioPeriod) {
                            //break;
                        }
                        
                        oAudInf.LastAudioSect = oPsxSect.getSector();
                        
                        m_lngAudioSampleLength += oAudio.getSampleLength();
                        
                    } else {
                        iAudioPeriod = oPsxSect.getSector() - m_iStartSector + 1;
                        
                        oAudInf = new AudioChannelInfo();
                        oAudInf.LastAudioSect = oPsxSect.getSector();
                        oAudInf.MonoStereo = oAudio.getMonoStereo();
                        oAudInf.SamplesPerSecond = oAudio.getSamplesPerSecond();
                        oAudInf.BitsPerSampele = oAudio.getBitsPerSample();
                        oAudInf.Channel = oAudio.getChannel();
                    }
                    
                    m_iEndSector = oPsxSect.getSector();
                } else {
                    break; // some other sector type? we're done.
                }
                
                if (oPsxSect != null && DebugVerbose > 2)
                    System.err.println(oPsxSect.toString());
                
                oSectIterator.skipNext();
            } // while
            
        }
        
        public PSXMediaSTR(CDSectorReader oCD, String sSerial)
                throws NotThisTypeException 
        {
            super(oCD, sSerial, "STR");
            String asParts[] = sSerial.split(":");
            if (asParts.length != 4)
                throw new NotThisTypeException();
            
            String asStartEndFrame[] = asParts[3].split("-");
            try {
                m_lngStartFrame = Integer.parseInt(asStartEndFrame[0]);
                m_lngEndFrame = Integer.parseInt(asStartEndFrame[1]);
            } catch (NumberFormatException ex) {
                throw new NotThisTypeException();
            }
        }
        
        public boolean HasAudio() {
            return m_lngAudioSampleLength != 0;
        }
        
        public PSXSectorRangeIterator GetSectorIterator() {
            return new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
        }
        
        public long GetAudioSampleLength() {
            return m_lngAudioSampleLength;
        }
        
        public String toString() {
            return "STR:" + super.toString() + ":"
                    + m_lngStartFrame + "-" + m_lngEndFrame;
        }
        
        long GetStartFrame() {
            return m_lngStartFrame;
        }
        
        long GetEndFrame() {
            return m_lngEndFrame;
        }
    }
    
    
    /**************************************************************************/
    /**************************************************************************/
    
    public static class PSXMediaXA extends PSXMedia {
        
        long m_iAudioPeriod = -1;
        
        /** In the future this *may* instead hold how many samples,
         *  or perhaps sectors, found for each channel, but for now,
         *  it just holds 1 or 0 for if a channel has audio */
        long[] m_alngChannelHasAudio = new long[/*32*/] {
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
        };
        
        
        public PSXMediaXA(PSXSectorRangeIterator oSectIterator)
                throws NotThisTypeException, IOException 
        {
            super(oSectIterator);
            AudioChannelInfo aoChannelInfos[] = new AudioChannelInfo[32];
            PSXSector oPsxSect = oSectIterator.peekNext();
            
            if (!(oPsxSect instanceof PSXSectorAudioChunk))
                throw new NotThisTypeException();
            
            if (DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
            m_iStartSector = oPsxSect.getSector();
            m_iEndSector = m_iStartSector;
            
            oSectIterator.skipNext();
            while (oSectIterator.hasNext()) {
                oPsxSect = oSectIterator.peekNext();
                
                if (oPsxSect instanceof PSXSectorNull) {
                    // skip
                } else if (oPsxSect instanceof PSXSectorAudioChunk) {
                    
                    PSXSectorAudioChunk oAudio = (PSXSectorAudioChunk)oPsxSect;
                    
                    if (aoChannelInfos[(int)oAudio.getChannel()] != null) {
                        
                        AudioChannelInfo m_oAuInfo =
                                aoChannelInfos[(int)oAudio.getChannel()];
                        
                        if (m_oAuInfo.BitsPerSampele != oAudio.getBitsPerSample() ||
                                m_oAuInfo.SamplesPerSecond != oAudio.getSamplesPerSecond() ||
                                m_oAuInfo.MonoStereo != oAudio.getMonoStereo() ||
                                m_oAuInfo.Channel != oAudio.getChannel()) 
                        {
                            break;
                        }
                        
                        if (m_iAudioPeriod > 0) {
                            if ((oAudio.getSector() - m_oAuInfo.LastAudioSect) != m_iAudioPeriod)
                                break;
                        } else {
                            m_iAudioPeriod = oAudio.getSector() - m_oAuInfo.LastAudioSect;
                        }
                        
                        m_oAuInfo.LastAudioSect = oAudio.getSector();
                        
                    } else {
                        AudioChannelInfo m_oAuInfo = new AudioChannelInfo();
                        
                        m_oAuInfo.LastAudioSect = oAudio.getSector();
                        m_oAuInfo.BitsPerSampele = oAudio.getBitsPerSample();
                        m_oAuInfo.SamplesPerSecond = oAudio.getSamplesPerSecond();
                        m_oAuInfo.MonoStereo = oAudio.getMonoStereo();
                        m_oAuInfo.Channel = oAudio.getChannel();
                        
                        aoChannelInfos[(int)oAudio.getChannel()] = m_oAuInfo;
                    }
                    
                    m_alngChannelHasAudio[(int)oAudio.getChannel()] += oAudio.getSampleLength();
                    
                    m_iEndSector = oPsxSect.getSector();
                } else {
                    break; // some other sector type? we're done.
                }
                
                if (oPsxSect != null && DebugVerbose > 2)
                    System.err.println(oPsxSect.toString());
                
                oSectIterator.skipNext();
            } // while
            
        }
        
        public PSXMediaXA(CDSectorReader oCD, String sSerial)
                throws NotThisTypeException, IOException 
        {
            super(oCD, sSerial, "XA");
            String asParts[] = sSerial.split(":");
            
            String asChannels[] = asParts[3].split(",");
            try {
                for (int i = 0; i < asChannels.length; i++) {
                    int iChan = Integer.parseInt(asChannels[i]);
                    m_alngChannelHasAudio[iChan] = 1;
                }
            } catch (NumberFormatException ex) {
                throw new NotThisTypeException();
            }
        }
        
        /** Returns null if no sectors are available */
        public PSXSectorRangeIterator GetChannelSectorIterator(int iChan) {
            assert(iChan >= 0 && iChan < 31);
            
            if (m_alngChannelHasAudio[iChan] == 0) return null;
            return new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
            
        }
        
        public String toString() {
            StringBuilder oSB = new StringBuilder();
            for (int i = 0; i < m_alngChannelHasAudio.length; i++) {
                if (m_alngChannelHasAudio[i] != 0) {
                    if (oSB.length() > 0)
                        oSB.append(",");
                    oSB.append(i);
                }
            }
            return "XA:" + super.toString() + ":" + oSB.toString();
        }
    }
    
    
    public static class PSXMediaTIM extends PSXMedia {
        
        public PSXMediaTIM(PSXSectorRangeIterator oSectIterator)
                throws NotThisTypeException, IOException 
        {
            super(oSectIterator);
            // we're assuming all TIM files begin at the start of a sector
            PSXSector oPsxSect = oSectIterator.peekNext();
            
            if (!(oPsxSect instanceof PSXSectorUnknownData))
                throw new NotThisTypeException();

            m_iStartSector = oSectIterator.getIndex();
            
            DataInputStream oDIS = new DataInputStream(new UnknownDataDemuxerIS(oSectIterator));
            try {
                
                if (oDIS.readInt() != 0x10000000)
                    throw new NotThisTypeException();

                int i = oDIS.readInt();
                if ((i & 0xF4FFFFFF) != 0)
                    throw new NotThisTypeException();
                    
                // possible TIM file
                long lng = LittleEndianIO.ReadUInt32LE(oDIS);
                if ((i & 0x08000000) == 0x08000000) {
                    // has CLUT, skip over it
                    if (oDIS.skip((int)lng - 4) != (lng - 4))
                        throw new NotThisTypeException();
                    lng = LittleEndianIO.ReadUInt32LE(oDIS);
                }
                // now skip over the image data
                if (oDIS.skip((int)lng - 4) != (lng - 4))
                    throw new NotThisTypeException();

                // if we made it this far, then we have ourselves
                // a TIM file (probably). Save the end sector
                m_iEndSector = oSectIterator.getIndex();
            } catch (EOFException ex) {
                throw new NotThisTypeException();
            }
            oSectIterator.skipNext();
            
        }
        
        public PSXMediaTIM(CDSectorReader oCD, String sSerial)
            throws NotThisTypeException
        {
            super(oCD, sSerial, "TIM");
            String asParts[] = sSerial.split(":");
            
            try {
                int iStartOffset = Integer.parseInt(asParts[3]);
            } catch (NumberFormatException ex) {
                throw new NotThisTypeException();
            }
        }
        
        public PSXSectorRangeIterator GetSectorIterator() {
            return new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
        }
        
        public String toString() {
            return "TIM:" + super.toString() + ":0";
        }
    }
    
}
