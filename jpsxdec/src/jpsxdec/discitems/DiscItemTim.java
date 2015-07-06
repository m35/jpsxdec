/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.util.IncompatibleException;
import jpsxdec.i18n.LocalizedMessage;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.savers.TimSaverBuilder;
import jpsxdec.tim.Tim;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


/** Represents a TIM file found on a PlayStation disc. TIM images could be found
 * anywhere in a sector. */
public class DiscItemTim extends DiscItem {

    public static final String TYPE_ID = "Tim";

    private static final String START_OFFSET_KEY = "Start Offset";
    private final int _iStartOffset;
    private static final String PALETTE_COUNT_KEY = "Palettes";
    private final int _iPaletteCount;
    private static final String BITSPERPIXEL_KEY = "Bpp";
    private final int _iBitsPerPixel;
    private static final String DIMENSIONS_KEY = "Dimensions";
    private final int _iWidth, _iHeight;
    
    public DiscItemTim(@Nonnull CdFileSectorReader cd,
                       int iStartSector, int iEndSector,
                       int iStartOffset, int iPaletteCount, int iBitsPerPixel,
                       int iWidth, int iHeight)
    {
        super(cd, iStartSector, iEndSector);
        _iStartOffset = iStartOffset;
        _iPaletteCount = iPaletteCount;
        _iBitsPerPixel = iBitsPerPixel;
        _iWidth = iWidth;
        _iHeight = iHeight;
    }
    
    public DiscItemTim(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws NotThisTypeException
    {
        super(cd, fields);
        _iStartOffset = fields.getInt(START_OFFSET_KEY);
        _iPaletteCount = fields.getInt(PALETTE_COUNT_KEY);
        _iBitsPerPixel = fields.getInt(BITSPERPIXEL_KEY);
        int[] aiDims = fields.getDimensions(DIMENSIONS_KEY);
        _iWidth = aiDims[0];
        _iHeight = aiDims[1];
    }
    
    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem fields = super.serialize();
        fields.addNumber(START_OFFSET_KEY, _iStartOffset);
        fields.addDimensions(DIMENSIONS_KEY, _iWidth, _iHeight);
        fields.addNumber(PALETTE_COUNT_KEY, _iPaletteCount);
        fields.addNumber(BITSPERPIXEL_KEY, _iBitsPerPixel);
        return fields;
    }
    
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public @Nonnull LocalizedMessage getInterestingDescription() {
        return I.GUI_TIM_IMAGE_DETAILS(_iWidth, _iHeight, _iPaletteCount);
    }

    public @Nonnull TimSaverBuilder makeSaverBuilder() {
        return new TimSaverBuilder(this);
    }

    @Override
    public @Nonnull GeneralType getType() {
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

    public @Nonnull Tim readTim() throws IOException, NotThisTypeException {
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

    @Override
    public int compareTo(@Nonnull DiscItem other) {
        if (other instanceof DiscItemTim) {
            DiscItemTim otherTim = (DiscItemTim) other;
            int i = Misc.intCompare(getStartSector(), otherTim.getStartSector());
            if (i == 0)
                return Misc.intCompare(_iStartOffset, otherTim._iStartOffset);
            else
                return i;
        } else {
            return super.compareTo(other);
        }
    }

    /** Attempts to replace the TIM image on the disc with the a new TIM created
     * from a BufferedImage. */
    public void replace(@Nonnull FeedbackStream Feedback, @Nonnull BufferedImage bi)
            throws IOException, NotThisTypeException, IncompatibleException
    {
        if (bi.getWidth() != _iWidth || bi.getHeight() != _iHeight)
            throw new IncompatibleException(I.TIM_REPLACE_DIMENSIONS_MISMATCH(
                    bi.getWidth(), bi.getHeight(), _iWidth, _iHeight));
        if (_iPaletteCount != 1)
            throw new IncompatibleException(I.TIM_REPLACE_MULTI_CLUT_UNABLE());
        
        DemuxedSectorInputStream stream = new DemuxedSectorInputStream(
                getSourceCd(), getStartSector(), getStartOffset());
        Tim tim;
        try {
            tim = Tim.read(stream);
        } finally {
            try {
                stream.close();
            } catch (IOException ex) {
                Logger.getLogger(DiscItemTim.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        tim.replaceImageData(bi);
        
        // get the byte size of the current tim
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tim.write(baos);
        byte[] abNewTim = baos.toByteArray();
        
        writeNewTimData(abNewTim, Feedback);
    }
    
    private void writeNewTimData(@Nonnull byte[] abNewTim, @Nonnull FeedbackStream Feedback) throws IOException {
        // write to the first sector
        CdFileSectorReader cd = getSourceCd();
        int iSector = getStartSector();
        byte[] abUserData = cd.getSector(iSector).getCdUserDataCopy();
        int iBytesToWrite = abNewTim.length;
        if (_iStartOffset + iBytesToWrite > abUserData.length)
            iBytesToWrite = abUserData.length - _iStartOffset;
        Feedback.println(I.CMD_TIM_REPLACE_SECTOR_BYTES(iBytesToWrite, iSector));
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
            Feedback.println(I.CMD_TIM_REPLACE_SECTOR_BYTES(iBytesToWrite, iSector));
            System.arraycopy(abNewTim, iBytesWritten, abUserData, 0, iBytesToWrite);
            cd.writeSector(iSector, abUserData);
            iBytesWritten += iBytesToWrite;
        }
    }
    
}