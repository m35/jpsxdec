/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

import jpsxdec.sectors.IdentifiedSector;
import java.io.IOException;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.sectors.IdentifiedSectorRangeIterator;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


/** Abstract superclass of all disc items.. Constructors of sub-classes will
 * attempt to deserialize a PlayaStation disc item from CD sectors. */
public abstract class DiscItem {

    private final int _iStartSector;
    private final int _iEndSector;
    private CdFileSectorReader _cdReader;

    /** Index number of the media item in the file. */
    private int _iIndex = -1;
    private FileBasedId _uniqueId;

    public DiscItem(int iStartSector, int iEndSector) {
        _iStartSector = iStartSector;
        _iEndSector = iEndSector;
    }

    /** Deserializes the start and end sectors. */
    public DiscItem(DiscItemSerialization fields) throws NotThisTypeException
    {
        _iIndex = fields.getIndex();
        _uniqueId = new FileBasedId(fields.getUniqueId());
        int[] aiRng = fields.getSectorRange();
        _iStartSector = aiRng[0];
        _iEndSector   = aiRng[1];
    }

    final protected DiscItemSerialization superSerial(String sType) {
        return new DiscItemSerialization(_iIndex, 
                _uniqueId == null ? null : _uniqueId.serialize(),
                sType, _iStartSector, _iEndSector);
    }

    public abstract DiscItemSerialization serialize();

    /** For sorting. */
    public int compareTo(Object o) {
        if (o instanceof DiscItem) {
            DiscItem odi = (DiscItem)o;
            if (_iIndex < odi._iIndex)
                return -1;
            else if (_iIndex > odi._iIndex)
                return 1;
            else
                return 0;
        } else {
            throw new IllegalArgumentException(DiscItem.class.getName()+
                                               ".compareTo non-"+
                                               DiscItem.class.getName());
        }
    }

    /** Returns the iIndex'th sector of this media item (beginning from
     *  the first sector of the media item). */
    public CdSector getRelativeSector(int iIndex) throws IOException {
        if (iIndex > getSectorLength())
            throw new IllegalArgumentException("Sector index out of bounds of this media item");
        return _cdReader.getSector(getStartSector() + iIndex);
    }

    public IdentifiedSector getRelativeIdentifiedSector(int iIndex) throws IOException {
        return identifySector(getRelativeSector(iIndex));
    }

    abstract public DiscItemSaverBuilder makeSaverBuilder();

    /** Because the media items knows what sectors are used,
     *  it can reduce the number of tests to identify a CD sector type.
     *  @return Identified sector, or null if no match. */
    final public IdentifiedSector identifySector(CdSector sector) {
        return IdentifiedSector.identifySector(sector);
        /* // this is nice in theory, but I don't think I'll mess with it
        for (Class<IdentifiedSector> oSectorType : m_aoSectorsUsed) {
            try {
                Constructor<IdentifiedSector> oConstruct = oSectorType.getConstructor(CDSector.class);
                return oConstruct.newInstance(oSect);
            }
            catch (InstantiationException ex) {throw new RuntimeException(ex);}
            catch (IllegalAccessException ex) {throw new RuntimeException(ex);}
            catch (InvocationTargetException ex) {throw new RuntimeException(ex);}
            catch (NoSuchMethodException ex) {throw new RuntimeException(ex);}
        }
        throw new RuntimeException("How did the program reach this line??");
        */
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

    /** Returns the number of sectors that may hold data related to this disc itme. */
    public int getSectorLength() {
        return _iEndSector - _iStartSector + 1;
    }

    /** Get the index number (usually sequential) of this disc item. */
    public int getIndex() {
        return _iIndex;
    }

    public void setIndex(int iMediaIndex) {
        _iIndex = iMediaIndex;
    }

    public void setSourceCD(CdFileSectorReader cd) {
        _cdReader = cd;
    }

    public CdFileSectorReader getSourceCD() {
        return _cdReader;
    }

    public IdentifiedSectorRangeIterator getSectorIterator() {
        return new IdentifiedSectorRangeIterator(_cdReader, _iStartSector, _iEndSector);
    }

    public String getSuggestedBaseName() {
        String sBaseName;
        if (_uniqueId.getBaseFile() == null) {
            sBaseName = Misc.getBaseName(_cdReader.getSourceFile().getName());
        } else {
            sBaseName = Misc.getBaseName(_uniqueId.getBaseFile().getName());
        }
        String sIndex = _uniqueId.getBaseIndex();
        return sBaseName + sIndex;
    }

    public FileBasedId getUniqueId() {
        return _uniqueId;
    }
    public void setUniqueId(FileBasedId id) {
        _uniqueId = id;
    }

    public String toString() {
        return serialize().serialize();
    }

    /** Returns a string representing the type of this disc item. Used in case
     * the user wants to save all items of a certain type. */
    abstract public String getTypeId();

    /** Returns how many sectors this item and the supplied disc item overlap. */
    public int getOverlap(DiscItem other) {
        // does not overlap this file at all
        if (other.getEndSector() < getStartSector() || other.getStartSector() > getEndSector())
            return 0;

        // there is definitely some overlap

        int iOverlap;
        if (other.getStartSector() < getStartSector()) {
            if (other.getEndSector() > getEndSector()) {
                // this file is totally inside item
                iOverlap = getSectorLength();
            } else {
                // last part of item is inside this file
                iOverlap = other.getSectorLength();
            }
        } else {
            if (other.getEndSector() > getEndSector()) {
                // first part of item is inside this file
                iOverlap = getEndSector() - other.getStartSector() + 1;
            } else {
                // item is totally inside this file
                iOverlap = other.getEndSector() - other.getStartSector() + 1;
            }
        }
        assert(iOverlap >= 0);
        return iOverlap;
    }


}
