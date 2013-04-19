/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;


/** Abstract superclass of all disc items. A "disc item" represents some media
 * or information that can be extracted from the disc and saved separately,
 * usually in better format. The information contained by DiscItems can be
 * serialized and deserialized for easy storage. DiscItems should be able to
 * generate a {@link DiscItemSaverBuilder} which provides options on how the
 * disc item can be converted and saved.
 * <p>
 * DiscItems also can (and usually should) hold an {@link IndexId} indicating
 * where this DiscItem falls within a list of DiscItems. This id can also
 * contain a suggested name that this DiscItem might use when extracting and
 * saving (e.g. if it's part of a file on a disc, use that file name).
 */
public abstract class DiscItem implements Comparable<DiscItem> {

    /** Basic types of {@link DiscItem}s. */
    public static enum GeneralType {
        Audio, Video, Image, File
    }


    private final int _iStartSector;
    private final int _iEndSector;
    private CdFileSectorReader _cdReader;

    /** Often sequential and hopefully unique number identifying this {@link DiscItem}. */
    private int _iIndex;
    private IndexId _indexId;

    protected DiscItem(int iStartSector, int iEndSector) {
        _iStartSector = iStartSector;
        _iEndSector = iEndSector;
    }

    /** Deserializes the basic information about this {@link DiscItem}. */
    protected DiscItem(SerializedDiscItem fields) throws NotThisTypeException
    {
        int[] aiRng = fields.getSectorRange();
        _iStartSector = aiRng[0];
        _iEndSector   = aiRng[1];
        _iIndex = fields.getIndex();
        _indexId = new IndexId(fields.getId());
    }

    /** Child classes should override and add their own fields. */
    public SerializedDiscItem serialize() {
        return new SerializedDiscItem(getSerializationTypeId(),
                   _iIndex, _indexId == null ? null : _indexId.serialize(),
                   _iStartSector, _iEndSector);
    }

    /** String of the 'Type:' value in the serialization string. */
    abstract public String getSerializationTypeId();

    // TODO: see if this can be removed
    public void setSourceCd(CdFileSectorReader cd) {
        _cdReader = cd;
    }

    public CdFileSectorReader getSourceCd() {
        return _cdReader;
    }

    public void setIndex(int iIndex) {
        _iIndex = iIndex;
    }

    public int getIndex() {
        return _iIndex;
    }

    /** @return if {@link IndexId} was accepted. */
    public boolean setIndexId(IndexId id) {
        _indexId = id;
        return true;
    }

    public IndexId getIndexId() {
        return _indexId;
    }
    
    /** Returns how likely the supplied {@link DiscItem} 
     * is a child of this item. */
    public int getParentRating(DiscItem child) {
        return 0;
    }
    /** Attempts to add the child item to this item.
     * @return if the item was accepted as a child.  */
    public boolean addChild(DiscItem child) {
        return false;
    }

    /** Number of children. */
    public int getChildCount() {
        return 0;
    }

    /** Children of this item.
     * @return null if no children. */
    public Iterable<DiscItem> getChildren() {
        return null;
    }

    /** Returns how many sectors this item and the supplied disc item overlap. */
    public int getOverlap(DiscItem other) {
        // does not overlap this file at all
        if (other.getEndSector() < getStartSector() || other.getStartSector() > getEndSector())
            return 0;

        // there is definitely some overlap

        int iOverlap;
        if (other.getStartSector() < getStartSector()) {
            if (other.getEndSector() > getEndSector()) {
                // this item is totally inside other item
                iOverlap = getSectorLength();
            } else {
                // other item is totall inside this item
                iOverlap = other.getSectorLength();
            }
        } else {
            if (other.getEndSector() > getEndSector()) {
                // first part of other item is overlaps this item
                iOverlap = getEndSector() - other.getStartSector() + 1;
            } else {
                // last part of other item is overlaps this item
                iOverlap = other.getEndSector() - other.getStartSector() + 1;
            }
        }
        assert(iOverlap >= 0);
        return iOverlap;
    }

    /** Returns the iIndex'th sector of this media item (beginning from
     *  the first sector of the media item). */
    public CdSector getRelativeSector(int iIndex) throws IOException {
        if (iIndex > getSectorLength())
            throw new IllegalArgumentException("Sector index out of bounds of this disc item");
        return _cdReader.getSector(getStartSector() + iIndex);
    }

    public IdentifiedSector getRelativeIdentifiedSector(int iIndex) throws IOException {
        return identifySector(getRelativeSector(iIndex));
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

    /** Because the media items knows what sectors are used,
     *  it can reduce the number of tests to identify a CD sector type.
     *  @return Identified sector, or null if no match. */
    public IdentifiedSector identifySector(CdSector sector) {
        return IdentifiedSector.identifySector(sector);
        /* // this is nice in theory, but I don't think I'll mess with it
        for (Class<IdentifiedSector> sectorType : _aoSectorsUsed) {
            try {
                Constructor<IdentifiedSector> construct = sectorType.getConstructor(CdSector.class);
                return construct.newInstance(sector);
            }
            catch (InstantiationException ex) {throw new RuntimeException(ex);}
            catch (IllegalAccessException ex) {throw new RuntimeException(ex);}
            catch (InvocationTargetException ex) {throw new RuntimeException(ex);}
            catch (NoSuchMethodException ex) {throw new RuntimeException(ex);}
        }
        */
    }

    /** Returns the serialization. This is what is written to the index file. */
    public String toString() {
        return serialize().serialize();
    }

    /** General type of the disc item (audio, video, file, image, etc.) */
    abstract public GeneralType getType();

    /** Description of various details about the disc item. */
    abstract public String getInterestingDescription();

    abstract public DiscItemSaverBuilder makeSaverBuilder();

    public File getSuggestedBaseName() {
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
    public int compareTo(DiscItem other) {
        if (getStartSector() < other.getStartSector())
            return -1;
        else if (getStartSector() > other.getStartSector())
            return 1;
        else if (getEndSector() > other.getEndSector())
            return -1;
        else if (getEndSector() < other.getEndSector())
            return 1;
        else
            return 0;
    }
}
