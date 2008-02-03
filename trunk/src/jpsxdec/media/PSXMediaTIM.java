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
 * PSXMediaTIM.java
 */

package jpsxdec.media;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.demuxers.UnknownDataDemuxerIS;
import jpsxdec.sectortypes.PSXSector.PSXSectorUnknownData;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;



public class PSXMediaTIM extends PSXMedia {
    
    int m_iStartOffset;
    
    public PSXMediaTIM(PSXSectorRangeIterator oSectIterator) throws NotThisTypeException, IOException
    {
        super(oSectIterator);
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        // TODO: search for TIM files at places other than the start of a sector
        m_iStartOffset = 0;
        
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
            long lng = IO.ReadUInt32LE(oDIS);
            if ((i & 0x08000000) == 0x08000000) {
                // has CLUT, skip over it
                if (oDIS.skip((int) lng - 4)  != (lng - 4))
                    throw new NotThisTypeException();
                lng = IO.ReadUInt32LE(oDIS);
            }
            // now skip over the image data
            if (oDIS.skip((int) lng - 4)  != (lng - 4))
                throw new NotThisTypeException();

            // if we made it this far, then we have ourselves
            // a TIM file (probably). Save the end sector
            m_iEndSector = oSectIterator.getIndex(); // TODO: Get the sector from the item, not the iterator
        }  catch (EOFException ex) {
            throw new NotThisTypeException();
        }
        oSectIterator.skipNext();
        
    }
    
    public PSXMediaTIM(CDSectorReader oCD, String sSerial) throws NotThisTypeException
    {
        super(oCD, sSerial, "TIM");
        String asParts[] = sSerial.split(":");
        
        try {
            m_iStartOffset = Integer.parseInt(asParts[3]);
        }  catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
    public PSXSectorRangeIterator GetSectorIterator() {
        return new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
    }
    
    public String toString() {
        return super.toString("TIM") + ":" + m_iStartOffset;
    }

    public int getMediaType() {
        return PSXMedia.MEDIA_TYPE_IMAGE;
    }

    @Override
    public boolean hasImage() {
        return true;
    }

    @Override
    public void DecodeImage(String sFileBaseName, String sImgFormat) {
        
        PSXSectorRangeIterator oSectIter = GetSectorIterator();
        
        String sImgFile =
                sFileBaseName + "_p%02d." + sImgFormat;
        
        try {
            InputStream oIS = new UnknownDataDemuxerIS(oSectIter);
            Tim oTim = new Tim(oIS);
            for (int i = 0; i < oTim.getPaletteCount(); i++) {
                if (!super.Progress("Decoding TIM palette " + i, 
                        (double)i / oTim.getPaletteCount())) 
                    return;
        
                ImageIO.write(
                      oTim.toBufferedImage(i),
                      sImgFormat,
                      new File(String.format(sImgFile, i)));
            }

        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
        } catch (NotThisTypeException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
        }

    }

}