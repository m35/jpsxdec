/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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
import java.util.AbstractList;
import java.util.ArrayList;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;

/** Holds the index unique number, and unique string id based on a file. */
public class IndexId extends AbstractList<IndexId> {
    private File _sourceFile;
    private int[] _aiTreeIndexes;

    private final DiscItem _item;
    private final int _iListIndex;

    private ArrayList<IndexId> _kids = new ArrayList<IndexId>();

    public IndexId(DiscItem item, int iIndex) {
        _item = item;
        _iListIndex = iIndex;
    }

    public IndexId(DiscItem item, int iIndex, File baseFile) {
        this(item, iIndex);
        _sourceFile = baseFile;
    }

    public IndexId(String sSerialized, DiscItem item) throws NotThisTypeException {
        _item = item;

        String[] asParts = Misc.regex("#(\\d+) ([^\\[]+)(\\[[^\\]]+\\])?", sSerialized);
        if (asParts == null || asParts.length != 4)
            throw new NotThisTypeException("Invalid id format: " + sSerialized);

        try {
            _iListIndex = Integer.parseInt(asParts[1]);
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException("Invalid id format: " + sSerialized);
        }

        if (!"Unnamed".equals(asParts[2])) {
            _sourceFile = new File(asParts[2]);
        }

        if (asParts[3] != null) {
            _aiTreeIndexes = Misc.parseDelimitedInts(asParts[3].substring(1, asParts[3].length()-1), "\\.");
            if (_aiTreeIndexes == null)
                throw new NotThisTypeException("Invalid id format: " + sSerialized);
        }

        //System.out.println(sSerialized + " -> " + this);
    }

    public int getListIndex() {
        return _iListIndex;
    }

    public DiscItem getItem() {
        return _item;
    }

    public int recursiveSetTreeIndex(File file, int[] aiParentIndex, int iChildInc) {
        if (_sourceFile == null) {
            _sourceFile = file;

            if (aiParentIndex == null) {
                _aiTreeIndexes = new int[] { iChildInc };
            } else {
                _aiTreeIndexes = new int[aiParentIndex.length+1];
                System.arraycopy(aiParentIndex, 0, _aiTreeIndexes, 0, aiParentIndex.length);
                _aiTreeIndexes[_aiTreeIndexes.length-1] = iChildInc;
            }

            iChildInc++;
        }
        
        if (_kids.size() > 0) {
            int iChildIndex = 0;
            for (IndexId child : _kids) {
                iChildIndex = child.recursiveSetTreeIndex(_sourceFile, _aiTreeIndexes, iChildIndex);
            }
        }
        _item.setIndexId(this);
        return iChildInc;
    }

    private static final File UNNAMED = new File("Unnamed");
    private File safePath() {
        return _sourceFile == null ? UNNAMED : _sourceFile;
    }


    public String serialize() {
        return "#" + _iListIndex + " " + getId();
    }

    public String getId() {
        return Misc.forwardSlashPath(safePath()) + getTreeIndex();
    }

    public File getFile() {
        return _sourceFile;
    }

    public String getTreeIndex() {
        if (_aiTreeIndexes == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder("[");
            for (int i : _aiTreeIndexes) {
                if (sb.length() > 1)
                    sb.append('.');
                sb.append(i);
            }
            sb.append(']');
            return sb.toString();
        }
    }

    public File getSuggestedBaseName() {
        if (_sourceFile == null)
            return null;
        else
            return new File(Misc.getBaseName(_sourceFile.getPath()) + getTreeIndex());
    }

    public String getTopLevel() {
        return safePath().getName() + getTreeIndex();
    }

    /** Same as {@link #serialize()}. */
    @Override
    public String toString() {
        return serialize();
    }

    public void findAndAddChildren(ArrayList<DiscItem> LIST) {
        Outside:
        for (DiscItem item : LIST) {

            // make sure they are part of the same file
            File otherBaseFile = item.getIndexId()._sourceFile;
            if (_sourceFile == null) {
                if (otherBaseFile != null)
                    continue;
            } else {
                if (otherBaseFile == null || !_sourceFile.equals(otherBaseFile)) {
                    continue;
                }
            }

            // make sure the other item is a direct child
            int[] aiOtherTreeIndexes = item.getIndexId()._aiTreeIndexes;
            if (aiOtherTreeIndexes == null)
                continue;
            if (_aiTreeIndexes == null) {
                if (aiOtherTreeIndexes.length == 1) {
                    _kids.add(item.getIndexId());
                }
            } else if(aiOtherTreeIndexes.length == _aiTreeIndexes.length + 1) {
                for (int i = 0; i < _aiTreeIndexes.length; i++) {
                    if (_aiTreeIndexes[i] != aiOtherTreeIndexes[i])
                        continue Outside;
                }

                _kids.add(item.getIndexId());
            }

        }
    }

    public boolean isRoot() {
        if (_aiTreeIndexes == null)
            return true;
        return (_sourceFile == null && _aiTreeIndexes.length == 1);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && _item.equals(((IndexId)o)._item);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + _item.hashCode();
    }

    @Override
    public IndexId get(int index) {
        return _kids.get(index);
    }

    @Override
    public int size() {
        return _kids.size();
    }

    @Override
    public boolean add(IndexId o) {
        return _kids.add(o);
    }

    @Override
    public IndexId set(int index, IndexId element) {
        return _kids.set(index, element);
    }


}
