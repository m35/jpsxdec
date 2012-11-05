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

package jpsxdec.discitems;


import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.savers.TimSaverBuilder;
import jpsxdec.tim.Tim;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.NotThisTypeException;


/** Represents a TIM file found on a PlayStation disc. TIM images could be found
 * anywhere in a sector. */
public class DiscItemTim extends DiscItem {

    private static final Logger log = Logger.getLogger(DiscItemTim.class.getName());

    public static final String TYPE_ID = "Tim";

    private static final String START_OFFSET_KEY = "Start Offset";
    private final int _iStartOffset;
    private static final String PALETTE_COUNT_KEY = "Palettes";
    private final int _iPaletteCount;
    private static final String BITSPERPIXEL_KEY = "Bpp";
    private final int _iBitsPerPixel;
    private static final String DIMENSIONS_KEY = "Dimensions";
    private final int _iWidth, _iHeight;
    
    public DiscItemTim(int iStartSector, int iEndSector, 
                       int iStartOffset, int iPaletteCount, int iBitsPerPixel,
                       int iWidth, int iHeight)
    {
        super(iStartSector, iEndSector);
        _iStartOffset = iStartOffset;
        _iPaletteCount = iPaletteCount;
        _iBitsPerPixel = iBitsPerPixel;
        _iWidth = iWidth;
        _iHeight = iHeight;
    }
    
    public DiscItemTim(DiscItemSerialization fields) throws NotThisTypeException {
        super(fields);
        _iStartOffset = fields.getInt(START_OFFSET_KEY);
        _iPaletteCount = fields.getInt(PALETTE_COUNT_KEY);
        _iBitsPerPixel = fields.getInt(BITSPERPIXEL_KEY);
        int[] aiDims = fields.getDimensions(DIMENSIONS_KEY);
        _iWidth = aiDims[0];
        _iHeight = aiDims[1];
    }
    
    public DiscItemSerialization serialize() {
        DiscItemSerialization fields = super.superSerial(TYPE_ID);
        fields.addNumber(START_OFFSET_KEY, _iStartOffset);
        fields.addDimensions(DIMENSIONS_KEY, _iWidth, _iHeight);
        fields.addNumber(PALETTE_COUNT_KEY, _iPaletteCount);
        fields.addNumber(BITSPERPIXEL_KEY, _iBitsPerPixel);
        return fields;
    }
    
    public String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getInterestingDescription() {
        return _iWidth + "x" + _iHeight + ", Palettes: " + _iPaletteCount;
    }

    public TimSaverBuilder makeSaverBuilder() {
        return new TimSaverBuilder(this);
    }

    @Override
    public GeneralType getType() {
        return GeneralType.Image;
    }

    public int getStartOffset() {
        return _iStartOffset;
    }

    /** Returns the number of palettes if the TIM file is paletted 
     * (with a CLUT). 1 could mean a paletted image with 1 palette, or a
     * true color image. It is possible for paletted images to not have a CLUT,
     * in that case a paletted image with a gray scale palette is created. */
    public int getPaletteCount() {
        return _iPaletteCount;
    }

    public int getBitsPerPixel() {
        return _iBitsPerPixel;
    }

    public Tim readTim() throws IOException, NotThisTypeException {
        DemuxedSectorInputStream stream = new DemuxedSectorInputStream(
                getSourceCd(), getStartSector(), getStartOffset());
        return Tim.read(stream);
    }

    public int getWidth() {
        return _iWidth;
    }
    
    public int getHeight() {
        return _iHeight;
    }

    /** Attempts to replace the TIM image on the disc with the a new TIM created
     * from a BufferedImage. */
    public void replace(FeedbackStream Feedback, BufferedImage bi) 
            throws IOException, NotThisTypeException 
    {
        if (bi.getWidth() != _iWidth || bi.getHeight() != _iHeight)
            throw new IllegalArgumentException(String.format(
                    "New TIM dimensions (%dx%d) do not match existing TIM dimensions (%dx%d).",
                    bi.getWidth(), bi.getHeight(), _iWidth, _iHeight));
        if (_iPaletteCount != 1)
            throw new IllegalArgumentException("Unable to replace a multi-paletted TIM with a simple image.");
        
        DemuxedSectorInputStream stream = new DemuxedSectorInputStream(
                getSourceCd(), getStartSector(), getStartOffset());
        Tim tim = Tim.read(stream);
        tim.replaceImageData(bi);
        
        // get the byte size of the current tim
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tim.write(baos);
        byte[] abNewTim = baos.toByteArray();
        
        writeNewTimData(abNewTim, Feedback);
    }
    
    private void writeNewTimData(byte[] abNewTim, FeedbackStream Feedback) throws IOException {
        // write to the first sector
        CdFileSectorReader cd = getSourceCd();
        int iSector = getStartSector();
        byte[] abUserData = cd.getSector(iSector).getCdUserDataCopy();
        int iBytesToWrite = abNewTim.length;
        if (_iStartOffset + iBytesToWrite > abUserData.length)
            iBytesToWrite = abUserData.length - _iStartOffset;
        Feedback.println("Writing " + iBytesToWrite + " bytes to sector " + iSector);
        System.arraycopy(abNewTim, 0, abUserData, _iStartOffset, iBytesToWrite);
        cd.writeSector(iSector, abUserData);
        
        //write to the remaining sectors
        int iBytesWritten = iBytesToWrite;
        while (iBytesWritten < abNewTim.length) {
            iSector++; 
            if (iSector > getEndSector())
                throw new RuntimeException("Replacing TIM is somehow writing to too many sectors.");
            
            abUserData = cd.getSector(iSector).getCdUserDataCopy();
            iBytesToWrite = abNewTim.length - iBytesWritten;
            if (iBytesToWrite > abUserData.length)
                iBytesToWrite = abUserData.length;
            Feedback.println("Writing " + iBytesToWrite + " bytes to sector " + iSector);
            System.arraycopy(abNewTim, iBytesWritten, abUserData, 0, iBytesToWrite);
            cd.writeSector(iSector, abUserData);
            iBytesWritten += iBytesToWrite;
        }
    }
    
}