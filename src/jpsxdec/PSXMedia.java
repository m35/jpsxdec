/* 
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


package jpsxdec;

import java.util.*;
import java.io.*;
import jpsxdec.CDSectorReader.CDSectorIterator;
import jpsxdec.CDSectorReader.CDXASector;
import jpsxdec.PSXSector.NotThisTypeException;


/** Abstract CD media type. Subclasses currently are only STR and XA */
abstract class PSXMediaAbstract {
    
    public static int DebugVerbose = 2;
    
    /** Finds all the media on the CD */
    public static ArrayList<PSXMediaAbstract> IndexCD(CDSectorReader oCD) 
            throws IOException 
    {
        
        CDSectorIterator oSectIterator = (CDSectorIterator)oCD.listIterator();
        
        ArrayList<PSXMediaAbstract> oMediaList = new ArrayList<PSXMediaAbstract>();
        
        if (!oSectIterator.hasNext()) return oMediaList;
        
        oSectIterator.next();
        PSXMediaAbstract oPsxMedia = null;
        while (oSectIterator.hasNext()) {
            
            try {
                oPsxMedia = new CDMediaSTR(oSectIterator);
                oMediaList.add(oPsxMedia);
                if (!oSectIterator.hasNext()) break;
            } catch (NotThisTypeException e1) {
                try {
                    oPsxMedia = new CDMediaXA(oSectIterator);
                    oMediaList.add(oPsxMedia);
                    if (!oSectIterator.hasNext()) break;
                } catch (NotThisTypeException e2) {
                    if (!oSectIterator.hasNext()) break;
                }
            }
            
        }
        
        return oMediaList;
    }
    
    /** Deserializes the CD index file, and creates a 
     *  list of media items on the CD */
    public static ArrayList<PSXMediaAbstract> IndexCD(CDSectorReader oCD, 
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
        
        ArrayList<PSXMediaAbstract> oMediaList = new ArrayList<PSXMediaAbstract>();
        
        PSXMediaAbstract oPsxMedia = null;
        BufferedReader oReader = 
                new BufferedReader(new FileReader(sSerialFile));
        String sLine;
        while ((sLine = oReader.readLine()) != null) {
            
            try {
                if (sLine.substring(3, 8).equals(":STR:")) {
                    oPsxMedia = new CDMediaSTR(oCD, sLine);
                    oMediaList.add(oPsxMedia);
                } else if (sLine.substring(3, 7).equals(":XA:")) {
                    oPsxMedia = new CDMediaXA(oCD, sLine);
                    oMediaList.add(oPsxMedia);
                }
            } catch (NotThisTypeException e1) {}            
        }
        oReader.close();
        
        return oMediaList;
    }
    
    /** Serializes a list of media items to a file */
    public static void SerializeMediaList(
            AbstractList<PSXMediaAbstract> oMediaList, 
            PrintStream oPrinter) 
        throws IOException 
    {
        oPrinter.println("# Any line that does not start " +
                         "with ### STR or ### XA is ignored.");
        int i = 0;
        for (PSXMediaAbstract oMedia : oMediaList) {
            oPrinter.println(String.format("%03d:%s", i, oMedia.toString()));
            i++;
        }
        oPrinter.close();
    }
    
    /** This class is used only during indexing. Once a file has been
     *  indexed, this information is no longer used. At that point this
     *  object just becomes a placeholder indicating that the channel
     *  has audio data. 
     *  FIXME: That's ugly. */
    protected static class AudioChannelInfo {
        public long Channel          = -1;
        public long BitsPerSampele   = -1;
        public long SamplesPerSecond = -1;
        public long MonoStereo       = -1;
        public long LastAudioSect    = -1;
    }
    
    protected int m_iStartSector;
    protected int m_iEndSector;
    protected CDSectorReader m_oCD;
    
    public PSXMediaAbstract(CDSectorIterator oSectIterator) {
        m_oCD = oSectIterator.getSourceCD();
    }
    
    /** Read in the start and end sectors */
    public PSXMediaAbstract(CDSectorReader oCD, String sSerial) 
            throws NotThisTypeException 
    {
        m_oCD = oCD;
        String asParts[] = sSerial.split(":");
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
}


class CDMediaSTR extends PSXMediaAbstract {
    
    long m_iAudioPeriod = -1;
    boolean m_blnHasAudio = false;
    
    long m_iCurFrame = -1;
    long m_iWidth = -1;
    long m_iHeight = -1;
    
    long m_lngStartFrame = -1;
    long m_lngEndFrame = -1;
    
    public CDMediaSTR(CDSectorIterator oSectIterator)  
            throws NotThisTypeException, IOException 
    {
        super(oSectIterator);
        
        AudioChannelInfo oAudInf = null;
        
        CDXASector oCDSect = oSectIterator.get();
        
        PSXSector oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        
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
        
        m_iStartSector = oCDSect.getSector();
        m_iEndSector = m_iStartSector;
        
        m_lngStartFrame = oFrame.getFrameNumber();
        m_lngEndFrame = oFrame.getFrameNumber();
        
        while (oSectIterator.hasNext()) {
            oCDSect = oSectIterator.next();
            oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
            
            if (oPsxSect instanceof PSXSectorNull) {
                // just skip it
            } else if (oPsxSect instanceof PSXSectorAudio2048) {
                // just skip it
            } else if (oPsxSect instanceof IVideoChunkSector) {
                
                oFrame = (IVideoChunkSector)oPsxSect;
                if (oFrame.getWidth() == m_iWidth &&
                        oFrame.getHeight() == m_iHeight &&
                        (oFrame.getFrameNumber() == m_iCurFrame ||
                        oFrame.getFrameNumber() == m_iCurFrame+1))
                {

                    m_iCurFrame = oFrame.getFrameNumber();
                    m_lngEndFrame = m_iCurFrame;
                } else {
                    break;
                }
                
                m_iEndSector = oCDSect.getSector();
            } else if (oPsxSect instanceof IAudioSector) {
                oAudio = (PSXSectorAudioChunk)oPsxSect;
                
                if (oAudInf != null) {
                    
                    if (oAudio.getMonoStereo() != oAudInf.MonoStereo ||
                        oAudio.getSamplesPerSecond() != oAudInf.SamplesPerSecond ||
                        oAudio.getBitsPerSample() != oAudInf.BitsPerSampele ||
                        oAudio.getChannel() != oAudInf.Channel)
                    {
                        break;
                    }
                    
                    if ((oCDSect.getSector() - oAudInf.LastAudioSect) != m_iAudioPeriod) 
                    {
                        //break;
                    }
                    
                    oAudInf.LastAudioSect = oCDSect.getSector();
                    
                } else {
                    m_iAudioPeriod = oCDSect.getSector() - m_iStartSector + 1;
                    
                    oAudInf = new AudioChannelInfo();
                    oAudInf.LastAudioSect = oCDSect.getSector();
                    oAudInf.MonoStereo = oAudio.getMonoStereo();
                    oAudInf.SamplesPerSecond = oAudio.getSamplesPerSecond();
                    oAudInf.BitsPerSampele = oAudio.getBitsPerSample();
                    oAudInf.Channel = oAudio.getChannel();
                }
                
                m_iEndSector = oCDSect.getSector();
            } else {
                break; // some other sector type? we're done.
            }
            
            if (oPsxSect != null && DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
        } // while
        
        if (oAudInf != null) m_blnHasAudio = true;
    }
    
    public CDMediaSTR(CDSectorReader oCD, String sSerial) 
            throws NotThisTypeException 
    {
        super(oCD, sSerial);
        String asParts[] = sSerial.split(":");
        if (!asParts[1].equals("STR") || asParts.length != 4) 
            throw new NotThisTypeException();
        
        String asStartEndFrame[] = asParts[3].split("-");
        try {
            m_lngStartFrame = Integer.parseInt(asStartEndFrame[0]);
            m_lngEndFrame = Integer.parseInt(asStartEndFrame[1]);
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
    /** Returns null if movie has no audio */
    public ArrayList<PSXSector> GetAudioSectors() throws IOException {
        if (!m_blnHasAudio) return null;
        
        ArrayList<PSXSector> oChanSects = 
                new ArrayList<PSXSector>();
        
        for (ListIterator<CDXASector> oIter = m_oCD.listIterator(m_iStartSector); 
             oIter.hasNext() && oIter.nextIndex() < m_iEndSector;) 
        {
            
            CDXASector oCDSect = oIter.next();
            
            PSXSector oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
            
            if (oPsxSect instanceof PSXSectorAudioChunk) {
                oChanSects.add((PSXSectorAudioChunk)oPsxSect);
            }
        }
        
        return oChanSects;
    }
    
    public ArrayList<PSXSector> GetFrameSectors(long iFrame) 
            throws IOException 
    {
        ArrayList<PSXSector> oFrameChunks = 
                new ArrayList<PSXSector>();
        
        for (ListIterator<CDXASector> oIter = m_oCD.listIterator(m_iStartSector); 
             oIter.hasNext() && oIter.nextIndex() < m_iEndSector;) 
        {
            CDXASector oCDSect = oIter.next();
            
            PSXSector oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
            
            if (oPsxSect instanceof IVideoChunkSector) {
                IVideoChunkSector oFrameChunk = (IVideoChunkSector)oPsxSect;
                if (oFrameChunk.getFrameNumber() == iFrame)
                    oFrameChunks.add(oPsxSect);
            }
        }
        
        return oFrameChunks;
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



class CDMediaXA extends PSXMediaAbstract {
    
    long m_iAudioPeriod = -1;
    
    boolean[] m_ablnChannelHasAudio = new boolean[/*32*/] {
        false, false, false, false, false, false, false, false, 
        false, false, false, false, false, false, false, false, 
        false, false, false, false, false, false, false, false, 
        false, false, false, false, false, false, false, false
    };
    
    
    public CDMediaXA(CDSectorIterator oSectIterator) 
            throws NotThisTypeException, IOException 
    {
        super(oSectIterator);
        AudioChannelInfo aoChannelInfos[] = new AudioChannelInfo[32];
        CDXASector oCDSect = oSectIterator.get();
        PSXSector oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        
        if (!(oPsxSect instanceof PSXSectorAudioChunk))
            throw new NotThisTypeException();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        m_iStartSector = oCDSect.getSector();
        m_iEndSector = m_iStartSector;
        
        while (oSectIterator.hasNext()) {
            oCDSect = oSectIterator.next();
            oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
            
            if (!(oPsxSect instanceof PSXSectorAudioChunk)) {
                if (oCDSect.getSubMode() != 0) break;
            } else {
                
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
                        if ((oCDSect.getSector() - m_oAuInfo.LastAudioSect) != m_iAudioPeriod)
                            break;
                    } else {
                        m_iAudioPeriod = oCDSect.getSector() - m_oAuInfo.LastAudioSect;
                    }
                    
                    m_oAuInfo.LastAudioSect = oCDSect.getSector();
                    
                } else {
                    AudioChannelInfo m_oAuInfo = new AudioChannelInfo();
                    
                    m_oAuInfo.LastAudioSect = oCDSect.getSector();
                    m_oAuInfo.BitsPerSampele = oAudio.getBitsPerSample();
                    m_oAuInfo.SamplesPerSecond = oAudio.getSamplesPerSecond();
                    m_oAuInfo.MonoStereo = oAudio.getMonoStereo();
                    m_oAuInfo.Channel = oAudio.getChannel();
                    
                    aoChannelInfos[(int)oAudio.getChannel()] = m_oAuInfo;
                }
                
                m_iEndSector = oCDSect.getSector();
            } // if (oPsxSect instanceof CDSectorAudio)
            
            if (oPsxSect != null && DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
        } // while
        
        for (int i = 0; i < aoChannelInfos.length; i++) {
            if (aoChannelInfos[i] != null) 
                m_ablnChannelHasAudio[i] = true;
        }
    }
    
    public CDMediaXA(CDSectorReader oCD, String sSerial) 
            throws NotThisTypeException, IOException 
    {
        super(oCD, sSerial);
        String asParts[] = sSerial.split(":");
        if (!asParts[1].equals("XA"))
            throw new NotThisTypeException();
        
        String asChannels[] = asParts[3].split(",");
        try {
            for (String sChan : asChannels) {
                int iChan = Integer.parseInt(sChan);
                m_ablnChannelHasAudio[iChan] = true;
            }
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
    /** Returns null if no sectors are available */
    public ArrayList<PSXSector> GetChannelSectors(int iChan) 
            throws IOException
    {
        assert(iChan > 0 && iChan < 32);
        
        if (!m_ablnChannelHasAudio[iChan]) return null;
        
        ArrayList<PSXSector> oChanSects = 
                new ArrayList<PSXSector>();
        
        for (ListIterator<CDXASector> oIter = m_oCD.listIterator(m_iStartSector); 
        oIter.hasNext() && oIter.nextIndex() < m_iEndSector;) {
            CDXASector elem = oIter.next();
            
            PSXSector oSect = 
                    PSXSector.SectorIdentifyFactory(elem);
            
            // FIXME: Change this to IAudioSector when you get oIter working
            if (oSect instanceof PSXSectorAudioChunk) {
                PSXSectorAudioChunk oAudio = (PSXSectorAudioChunk)oSect;
                if (oAudio.getChannel() == iChan) oChanSects.add(oAudio);
            }
        }
        
        return oChanSects;
    }
    
    public String toString() {
        StringBuilder oSB = new StringBuilder();
        for (int i = 0; i < m_ablnChannelHasAudio.length; i++) {
            if (m_ablnChannelHasAudio[i]) {
                if (oSB.length() > 0)
                    oSB.append(",");
                oSB.append(i);
            }
        }
        return "XA:" + super.toString() + ":" + oSB.toString();
    }
}