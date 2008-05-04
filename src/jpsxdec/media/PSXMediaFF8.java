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
 * PSXMediaFF8.java
 */

package jpsxdec.media;

import java.io.IOException;
import java.util.NoSuchElementException;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorFF8;
import jpsxdec.sectortypes.PSXSectorFF8.*;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.media.StrFpsCalc.*;

/** TODO: Add docs */
public class PSXMediaFF8 extends PSXMediaSquare
{
    
    long m_lngStartFrame = -1;
    long m_lngEndFrame = -1;
    boolean m_blnHasVideo = false;
    
    public PSXMediaFF8(PSXSectorRangeIterator oSectIterator) 
                throws NotThisTypeException, IOException
    {
        super(oSectIterator.getSourceCD());
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorFF8))
            throw new NotThisTypeException();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        PSXSectorFF8 oFF8Sect;
        
        oFF8Sect = (PSXSectorFF8)oPsxSect;

        super.m_iStartSector = oPsxSect.getSector();
        super.m_iEndSector = m_iStartSector;

        long iCurFrame = oFF8Sect.getFrameNumber();
        m_lngStartFrame = oFF8Sect.getFrameNumber();
        m_lngEndFrame = oFF8Sect.getFrameNumber();
        
        oSectIterator.skipNext();
        while (oSectIterator.hasNext()) {
            oPsxSect = oSectIterator.peekNext();
            
            if (oPsxSect instanceof PSXSectorFF8) {
                
                oFF8Sect = (PSXSectorFF8)oPsxSect;
                if (oFF8Sect.getFrameNumber() == iCurFrame ||
                    oFF8Sect.getFrameNumber() == iCurFrame+1) 
                {
                    iCurFrame = oFF8Sect.getFrameNumber();
                    m_lngEndFrame = iCurFrame;
                } else {
                    break;
                }
                
                if (oPsxSect instanceof PSXSectorFF8Video)
                    m_blnHasVideo = true;
                m_iEndSector = oPsxSect.getSector();
            }  else {
                break; // some other sector type? we're done.
            }
            
            if (oPsxSect != null && DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
            oSectIterator.skipNext();
        } // while
        
    }
    
    public PSXMediaFF8(CDSectorReader oCD, String sSerial) throws NotThisTypeException
    {
        super(oCD, sSerial, "FF8");
        
        try {
            IndexLineParser parse = new IndexLineParser(
                    "$| Frames #-# Has video #", sSerial);

            parse.skip();
            m_lngStartFrame = parse.get(m_lngStartFrame);
            m_lngEndFrame   = parse.get(m_lngEndFrame);
            int iHasVid = -1;
            iHasVid = parse.get(iHasVid);
            if (iHasVid == 1)
                m_blnHasVideo = true;
            else 
                m_blnHasVideo = false;
        
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        } catch (IllegalArgumentException ex) {
            throw new NotThisTypeException();
        } catch (NoSuchElementException ex) {
            throw new NotThisTypeException();
        }
        
    }
    
    public String toString() {
        return super.toString("FF8") + String.format(
                "| Frames %d-%d Has video %d",
                m_lngStartFrame, m_lngEndFrame,
                m_blnHasVideo ? 1 : 0);
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
    public int getAudioChannels() {
        return 2;
    }

    @Override
    public boolean hasVideo() {
        return m_blnHasVideo;
    }

    @Override
    public FramesPerSecond[] getPossibleFPS() {
        return new FramesPerSecond[] {new FramesPerSecond(15, 1, 150)};
    }
    
    @Override
    public int getSamplesPerSecond() {
        return 44100;
    }

}