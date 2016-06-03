/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

import java.io.File;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


/** Abstract superclass of all disc items. A "disc item" represents some media
 * or information that can be extracted from the disc and saved separately,
 * usually in better format. The information contained by DiscItems can be
 * serialized and deserialized for easy storage. DiscItems should be able to
 * generate a {@link DiscItemSaverBuilder} which provides options on how the
 * disc item can be converted and saved.
 * <p>
 * DiscItems holds an {@link IndexId} indicating where this DiscItem falls
 * within a list of DiscItems. This id can also contain a suggested name
 * that this DiscItem might use when extracting and saving
 * (e.g. if it's part of a file on a disc, use that file name).
 * <p>
 * IndexId ({@link #setIndexId(jpsxdec.discitems.IndexId)}) and
 * Index# ({@link #setIndex(int)}) must be set before using this object. */
public abstract class DiscItem implements Comparable<DiscItem> {

    /** Basic types of {@link DiscItem}s. */
    public static enum GeneralType {
        Audio(I.ITEM_TYPE_AUDIO(), I.ITEM_TYPE_AUDIO_APPLY()),
        Video(I.ITEM_TYPE_VIDEO(), I.ITEM_TYPE_VIDEO_APPLY()),
        Image(I.ITEM_TYPE_IMAGE(), I.ITEM_TYPE_IMAGE_APPLY()),
        File (I.ITEM_TYPE_FILE() , I.ITEM_TYPE_FILE_APPLY() ),
        ;

        @Nonnull
        private final ILocalizedMessage _localizedName;
        @Nonnull
        private final ILocalizedMessage _localizedApplyToName;

        private GeneralType(@Nonnull ILocalizedMessage name, 
                            @Nonnull ILocalizedMessage applyToName)
        {
            _localizedName = name;
            _localizedApplyToName = applyToName;
        }

        public @Nonnull ILocalizedMessage getName() {
            return _localizedName;
        }

        /** Could be different when used in the phrase "Apply to all {0}". */
        public @Nonnull ILocalizedMessage getApplyToName() {
            return _localizedApplyToName;
        }
    }


    private final int _iStartSector;
    private final int _iEndSector;
    @Nonnull
    private final CdFileSectorReader _cdReader;

    /** Often sequential and hopefully unique number identifying this {@link DiscItem}. */
    private int _iIndex = -1;
    @CheckForNull
    private IndexId _indexId;

    protected DiscItem(@Nonnull CdFileSectorReader cd, int iStartSector, int iEndSector) {
        _cdReader = cd;
        _iStartSector = iStartSector;
        _iEndSector = iEndSector;
    }

    /** Deserializes the basic information about this {@link DiscItem}. */
    protected DiscItem(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields) 
            throws NotThisTypeException
    {
        _cdReader = cd;
        int[] aiRng = fields.getSectorRange();
        _iStartSector = aiRng[0];
        _iEndSector   = aiRng[1];
        _iIndex = fields.getIndex();
        _indexId = new IndexId(fields.getId());
    }

    /** This is what is written to the index file.
     * <p>
     * Child classes should override, call {@code super}, and add their own fields.
     * <p>
     * If object is still under construction (i.e. id# and index id are not set)
     * a usable serialization will be returned, but cannot be deserialized.
     */
    public @Nonnull SerializedDiscItem serialize() {
        return new SerializedDiscItem(getSerializationTypeId(),
                   _iIndex, _indexId == null ? null : _indexId.serialize(),
                   _iStartSector, _iEndSector);
    }

    /** String of the 'Type:' value in the serialization string. */
    abstract public @Nonnull String getSerializationTypeId();

    public @Nonnull CdFileSectorReader getSourceCd() {
        return _cdReader;
    }

    public void setIndex(int iIndex) {
        _iIndex = iIndex;
    }

    public int getIndex() {
        if (_iIndex == -1)
            throw new IllegalStateException("Index# should have been set before use.");
        return _iIndex;
    }

    /** @return if {@link IndexId} was accepted. */
    public boolean setIndexId(@Nonnull IndexId id) {
        _indexId = id;
        return true;
    }

    final public @Nonnull IndexId getIndexId() {
        if (_indexId == null)
            throw new IllegalStateException("IndexId should have been set before use.");
        return _indexId;
    }
    
    /** Returns how likely the supplied {@link DiscItem} 
     * is a child of this item. */
    public int getParentRating(@Nonnull DiscItem child) {
        return 0;
    }
    /** Attempts to add the child item to this item.
     * @return if the item was accepted as a child.  */
    public boolean addChild(@Nonnull DiscItem child) {
        return false;
    }

    /** Number of children. */
    public int getChildCount() {
        return 0;
    }

    /** Children of this item.
     * @return null if no children. */
    public @CheckForNull Iterable<DiscItem> getChildren() {
        return null;
    }

    /** Returns how many sectors this item and the supplied disc item overlap. */
    public int getOverlap(@Nonnull DiscItem other) {
        // does not overlap this item at all
        if (other.getEndSector() < getStartSector() || other.getStartSector() > getEndSector())
            return 0;

        // there is definitely some overlap

        int iOverlap;
        if (other.getStartSector() < getStartSector()) {
            if (other.getEndSector() > getEndSector()) {
                // this item is totally inside other item
                iOverlap = getSectorLength();
            } else {
                // other item is totally inside this item
                iOverlap = other.getSectorLength();
            }
        } else {
            if (other.getEndSector() > getEndSector()) {
                // first part of other item overlaps this item
                iOverlap = getEndSector() - other.getStartSector() + 1;
            } else {
                // last part of other item overlaps this item
                iOverlap = other.getEndSector() - other.getStartSector() + 1;
            }
        }
        assert iOverlap >= 0;
        return iOverlap;
    }

    /** Returns the iIndex'th sector of this media item (beginning from
     *  the first sector of the media item). */
    public @Nonnull CdSector getRelativeSector(int iIndex) throws IOException {
        if (iIndex > getSectorLength())
            throw new IllegalArgumentException("Sector index out of bounds of this disc item");
        return _cdReader.getSector(getStartSector() + iIndex);
    }

    public @Nonnull IdentifiedSectorIterator identifiedSectorIterator() {
        return IdentifiedSectorIterator.create(_cdReader, _iStartSector, _iEndSector);
    }

    /** First sector of the source disc that holds data related to this disc item.
     *  Always less-than or equal to getEndSector(). */
    public int getStartSector() {
        return _iStartSector;
    }

    /** Last sector of the source disc that holds data related to this disc item.
     *  Always greater-than or equal to getStartSector(). */
    public int getEndSector() {
        return _iEndSector;
    }

    /** Returns the number of sectors that may hold data related to this disc item. */
    public int getSectorLength() {
        return _iEndSector - _iStartSector + 1;
    }

    /** Returns the serialization. */
    public String toString() {
        return serialize().serialize();
    }

    /** General type of the disc item (audio, video, file, image, etc.) */
    abstract public @Nonnull GeneralType getType();

    /** Description of various details about the disc item. */
    abstract public @Nonnull ILocalizedMessage getInterestingDescription();

    abstract public @Nonnull DiscItemSaverBuilder makeSaverBuilder();

    public @Nonnull File getSuggestedBaseName() {
        File suggestedBaseName;
        if (_indexId == null) {
            // use the source CD filename as the base name
            suggestedBaseName = new File(_cdReader.getSourceFile().getName());
        } else {
            // use the index's base name if we can
            suggestedBaseName = _indexId.getSuggestedBaseName(_cdReader.getSourceFile().getName());
        }
        return suggestedBaseName;
    }

    // [implements Comparable]
    public int compareTo(@Nonnull DiscItem other) {
        if (getStartSector() < other.getStartSector())
            return -1;
        else if (getStartSector() > other.getStartSector())
            return 1;
        else
            // have more encompassing disc items come first (result is much cleaner)
            return Misc.intCompare(other.getEndSector(), getEndSector());
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DiscItem other = (DiscItem) obj;
        return compareTo(other)== 0;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

}
