/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.modules.tim;


import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DemuxedSectorInputStream;
import jpsxdec.discitems.Dimensions;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.tim.Tim;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;


/** Represents a TIM image found on a PlayStation disc. TIM images could be found
 * anywhere in a sector. */
public class DiscItemTim extends DiscItem implements DiscItem.IHasStartOffset {

    private static final Logger LOG = Logger.getLogger(DiscItemTim.class.getName());

    public static final String TYPE_ID = "Tim";

    private static final String START_OFFSET_KEY = "Start Offset";
    private final int _iStartOffset;
    private static final String PALETTE_COUNT_KEY = "Palettes";
    private final int _iPaletteCount;
    private static final String BITSPERPIXEL_KEY = "Bpp";
    private final int _iBitsPerPixel;
    @Nonnull
    private final Dimensions _dimensions;

    public DiscItemTim(@Nonnull ICdSectorReader cd,
                       int iStartSector, int iEndSector,
                       int iStartOffset, int iPaletteCount, int iBitsPerPixel,
                       int iWidth, int iHeight)
    {
        super(cd, iStartSector, iEndSector);
        _iStartOffset = iStartOffset;
        _iPaletteCount = iPaletteCount;
        _iBitsPerPixel = iBitsPerPixel;
        _dimensions = new Dimensions(iWidth, iHeight);
    }

    public DiscItemTim(@Nonnull ICdSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
        _iStartOffset = fields.getInt(START_OFFSET_KEY);
        _iPaletteCount = fields.getInt(PALETTE_COUNT_KEY);
        _iBitsPerPixel = fields.getInt(BITSPERPIXEL_KEY);
        _dimensions = new Dimensions(fields);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem fields = super.serialize();
        fields.addNumber(START_OFFSET_KEY, _iStartOffset);
        _dimensions.serialize(fields);
        fields.addNumber(PALETTE_COUNT_KEY, _iPaletteCount);
        fields.addNumber(BITSPERPIXEL_KEY, _iBitsPerPixel);
        return fields;
    }

    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        return I.GUI_TIM_IMAGE_DETAILS(_dimensions.getWidth(), _dimensions.getHeight(), _iPaletteCount);
    }

    @Override
    public @Nonnull TimSaverBuilder makeSaverBuilder() {
        return new TimSaverBuilder(this);
    }

    @Override
    public @Nonnull GeneralType getType() {
        return GeneralType.Image;
    }

    @Override
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

    public @Nonnull Tim readTim() throws CdException.Read, BinaryDataNotRecognized {
        DemuxedSectorInputStream stream = new DemuxedSectorInputStream(
                getSourceCd(), getStartSector(), getStartOffset());
        try {
            return Tim.read(stream);
        } catch (IOException ex) {
            if (ex instanceof CdException.Read)
                throw (CdException.Read)ex;
            throw new CdException.Read(getSourceCd().getSourceFile(), ex);
        }
    }

    public int getWidth() {
        return _dimensions.getWidth();
    }

    public int getHeight() {
        return _dimensions.getHeight();
    }

    public void replace(@Nonnull DiscPatcher patcher, @Nonnull File timFile, @Nonnull FeedbackStream fbs)
            throws FileNotFoundException, EOFException, IOException,
                   BinaryDataNotRecognized,
                   LocalizedIncompatibleException,
                   CdException.Read,
                   DiscPatcher.WritePatchException
    {
        FileInputStream fis = new FileInputStream(timFile);
        Tim newTim;
        try {
            newTim = Tim.read(fis);
        } finally {
            IO.closeSilently(fis, LOG);
        }

        replace(patcher, newTim, fbs);
    }

    public void replace(@Nonnull DiscPatcher patcher, @Nonnull Tim newTim, @Nonnull FeedbackStream fbs)
            throws LocalizedIncompatibleException,
                   CdException.Read,
                   DiscPatcher.WritePatchException
    {
        // read both Tims

        Tim currentTim = null;
        try {
            currentTim = readTim();
        } catch (CdException.Read | BinaryDataNotRecognized ex) {
            // this is bad
            throw new RuntimeException("Existing tim unreadable", ex);
        }

        Tim.Mismatch result = currentTim.matches(newTim);
        if (result != null) {
            throw new LocalizedIncompatibleException(I.TIM_INCOMPATIBLE(
                        newTim.toString(), currentTim.toString()));
        }


        byte[] abNewTim;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            currentTim.write(baos);
            byte[] abCurrentTim = baos.toByteArray();
            baos.reset();
            newTim.write(baos);
            abNewTim = baos.toByteArray();
            if (abCurrentTim.length != abNewTim.length)
                throw new RuntimeException(String.format("Matching tim binary length differ?? cur %s=%d new %s=%d",
                                                          currentTim, abCurrentTim.length, newTim, abNewTim.length));
        } catch (IOException ex) {
            throw new RuntimeException("BAOS", ex);
        }
        writeNewTimData(patcher, abNewTim, fbs);
    }

    private void writeNewTimData(@Nonnull DiscPatcher patcher, @Nonnull byte[] abNewTim, @Nonnull FeedbackStream fbs)
            throws CdException.Read, DiscPatcher.WritePatchException
    {
        // write to the first sector
        ICdSectorReader cd = getSourceCd();
        int iSector = getStartSector();
        CdSector cdSector = cd.getSector(iSector);
        int iBytesToWrite = abNewTim.length;
        if (_iStartOffset + iBytesToWrite > cdSector.getCdUserDataSize())
            iBytesToWrite = cdSector.getCdUserDataSize() - _iStartOffset;
        fbs.println(I.CMD_TIM_REPLACE_SECTOR_BYTES(iBytesToWrite, iSector));
        patcher.addPatch(iSector, _iStartOffset, abNewTim, 0, iBytesToWrite);

        //write to the remaining sectors
        int iBytesWritten = iBytesToWrite;
        while (iBytesWritten < abNewTim.length) {
            iSector++;
            if (iSector > getEndSector())
                throw new RuntimeException("Replacing TIM is somehow writing to too many sectors.");

            cdSector = cd.getSector(iSector);
            iBytesToWrite = abNewTim.length - iBytesWritten;
            if (iBytesToWrite > cdSector.getCdUserDataSize())
                iBytesToWrite = cdSector.getCdUserDataSize();
            fbs.println(I.CMD_TIM_REPLACE_SECTOR_BYTES(iBytesToWrite, iSector));
            patcher.addPatch(iSector, 0, abNewTim, iBytesWritten, iBytesToWrite);
            iBytesWritten += iBytesToWrite;
        }
    }

}
