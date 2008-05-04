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
 * PSXMediaChronoX.java
 */

package jpsxdec.media;

import java.io.IOException;
import java.util.NoSuchElementException;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.media.StrFpsCalc.*;
import jpsxdec.sectortypes.PSXSectorChronoXAudio;
import jpsxdec.sectortypes.PSXSectorFrameChunk;

/** TODO: Docs */
public class PSXMediaChronoX extends PSXMediaSquare {
    
    private long m_lngStartFrame = -1;
    private long m_lngEndFrame = -1;
    
    private long m_lngWidth = -1;
    private long m_lngHeight = -1;
    
    public PSXMediaChronoX(PSXSectorRangeIterator oSectIterator) 
                throws NotThisTypeException, IOException 
    {
        super(oSectIterator.getSourceCD());
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorChronoXAudio))
            throw new NotThisTypeException();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        PSXSectorChronoXAudio oCXAudSect = (PSXSectorChronoXAudio)oPsxSect;

        super.m_iStartSector = oPsxSect.getSector();
        super.m_iEndSector = m_iStartSector;

        long iCurFrame = oCXAudSect.getFrameNumber();
        m_lngStartFrame = iCurFrame;
        m_lngEndFrame = iCurFrame;
        
        oSectIterator.skipNext();
        while (oSectIterator.hasNext()) {
            oPsxSect = oSectIterator.peekNext();
            
            long lngThisFrame;
            if (oPsxSect instanceof PSXSectorChronoXAudio) {
                lngThisFrame = ((PSXSectorChronoXAudio)oPsxSect).getFrameNumber();
            } else if (oPsxSect instanceof PSXSectorFrameChunk) {
                PSXSectorFrameChunk oFrmChk = (PSXSectorFrameChunk)oPsxSect;
                lngThisFrame = oFrmChk.getFrameNumber();
                if (m_lngWidth < 0)
                    m_lngWidth = oFrmChk.getWidth();
                else
                    if (m_lngWidth != oFrmChk.getWidth())
                        break;
                
                if (m_lngHeight < 0)
                    m_lngHeight = oFrmChk.getHeight();
                else
                    if (m_lngHeight != oFrmChk.getHeight())
                        break;
            }  else {
                break; // some other sector type? we're done.
            }
            
            if (lngThisFrame == iCurFrame ||
                lngThisFrame == iCurFrame+1) 
            {
                m_lngEndFrame = iCurFrame = lngThisFrame;
            } else {
                break;
            }

            m_iEndSector = oPsxSect.getSector();
            
            if (oPsxSect != null && DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
            oSectIterator.skipNext();
        } // while
        
    }

    public PSXMediaChronoX(CDSectorReader oCD, String sSerial) throws NotThisTypeException
    {
        super(oCD, sSerial, "ChronoX");
        try {
            IndexLineParser parse = new IndexLineParser(
                    "$| Frames #-# #x#", sSerial);

            parse.skip();
            m_lngStartFrame = parse.get(m_lngStartFrame);
            m_lngEndFrame   = parse.get(m_lngEndFrame);
            m_lngWidth      = parse.get(m_lngWidth);
            m_lngHeight     = parse.get(m_lngHeight);
        
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        } catch (IllegalArgumentException ex) {
            throw new NotThisTypeException();
        } catch (NoSuchElementException ex) {
            throw new NotThisTypeException();
        }
        
    }
    public String toString() {
        return super.toString("ChronoX") + String.format(
                "| Frames %d-%d %dx%d",
                m_lngStartFrame, m_lngEndFrame,
                m_lngWidth, m_lngHeight);
    }

    public int getMediaType() {
        return PSXMedia.MEDIA_TYPE_VIDEO_AUDIO | PSXMedia.MEDIA_TYPE_VIDEO;
    }
    
    @Override
    public boolean hasVideo() {
        return true;
    }

    @Override
    public int getAudioChannels() {
        return 2;
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
    public long getWidth() {
        return m_lngWidth;
    }
    
    @Override
    public long getHeight() {
        return m_lngHeight;
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


