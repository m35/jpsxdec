/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
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
 * PSXMediaFF9.java
 */

package jpsxdec.media;

import java.io.IOException;
import java.util.NoSuchElementException;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorFF9;
import jpsxdec.sectortypes.PSXSectorFF9.*;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.media.StrFpsCalc.*;

/** TODO: Docs */
public class PSXMediaFF9 extends PSXMediaSquare {
    
    private long m_lngStartFrame = -1;
    private long m_lngEndFrame = -1;
    private int m_iSamplesPerSecond = -1;
    
    public PSXMediaFF9(PSXSectorRangeIterator oSectIterator) 
                throws NotThisTypeException, IOException 
    {
        super(oSectIterator.getSourceCD());
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorFF9))
            throw new NotThisTypeException();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        PSXSectorFF9 oFF9Sect;
        
        oFF9Sect = (PSXSectorFF9)oPsxSect;

        super.m_iStartSector = oPsxSect.getSector();
        super.m_iEndSector = m_iStartSector;

        long iCurFrame = oFF9Sect.getFrameNumber();
        m_lngStartFrame = oFF9Sect.getFrameNumber();
        m_lngEndFrame = oFF9Sect.getFrameNumber();
        
        oSectIterator.skipNext();
        while (oSectIterator.hasNext()) {
            oPsxSect = oSectIterator.peekNext();
            
            if (oPsxSect instanceof PSXSectorFF9) {
                
                oFF9Sect = (PSXSectorFF9)oPsxSect;
                if (oFF9Sect.getFrameNumber() == iCurFrame ||
                    oFF9Sect.getFrameNumber() == iCurFrame+1) 
                {
                    iCurFrame = oFF9Sect.getFrameNumber();
                    m_lngEndFrame = iCurFrame;
                } else {
                    break;
                }
                
                if (oFF9Sect instanceof PSXSectorFF9Audio) {
                    
                    PSXSectorFF9Audio oAudSect = (PSXSectorFF9Audio)oFF9Sect;
                    
                    if (m_iSamplesPerSecond <= 0) {
                        m_iSamplesPerSecond = oAudSect.getSamplesPerSecond();
                    } else if (m_iSamplesPerSecond != oAudSect.getSamplesPerSecond()) {
                        throw new RuntimeException("This is a very strange FF9 audio sector, and should be examined: " + oAudSect.toString());
                    }
                    
                }
                
                m_iEndSector = oPsxSect.getSector();
            }  else {
                break; // some other sector type? we're done.
            }
            
            if (oPsxSect != null && DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
            oSectIterator.skipNext();
        } // while
        
    }

    public PSXMediaFF9(CDSectorReader oCD, String sSerial) throws NotThisTypeException
    {
        super(oCD, sSerial, "FF9");
        try {
            IndexLineParser parse = new IndexLineParser(
                    "$| Frames #-# Samples/second #", sSerial);

            parse.skip();
            m_lngStartFrame = parse.get(m_lngStartFrame);
            m_lngEndFrame   = parse.get(m_lngEndFrame);
            m_iSamplesPerSecond = parse.get(m_iSamplesPerSecond);
        
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        } catch (IllegalArgumentException ex) {
            throw new NotThisTypeException();
        } catch (NoSuchElementException ex) {
            throw new NotThisTypeException();
        }
        
    }
    public String toString() {
        return super.toString("FF9") + String.format(
                "| Frames %d-%d Samples/second %d",
                m_lngStartFrame, m_lngEndFrame,
                m_iSamplesPerSecond);
    }

    public int getMediaType() {
        return PSXMedia.MEDIA_TYPE_VIDEO_AUDIO | 
                (m_iSamplesPerSecond > 0 ? PSXMedia.MEDIA_TYPE_VIDEO : 0);
    }
    
    @Override
    public boolean hasVideo() {
        return true;
    }

    @Override
    public int getAudioChannels() {
        if (m_iSamplesPerSecond > 0)
            return 2;
        else
            return 0;
    }
    
    @Override
    public long getStartFrame() {
        return m_lngStartFrame;
    }

    @Override
    public long getEndFrame() {
        return m_lngEndFrame;
    }

    @Override
    public FramesPerSecond[] getPossibleFPS() {
        return new FramesPerSecond[] {new FramesPerSecond(15, 1, 150)};
    }
    
    @Override
    public int getSamplesPerSecond() {
        return m_iSamplesPerSecond;
    }


}


