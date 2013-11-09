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
import java.util.Arrays;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;

/** Holds the index unique number, and unique string id based on a file. */
public class IndexId {
    private final File _sourceFile;
    private final int[] _aiTreeIndexes;

    public IndexId(int iIndex) {
        _sourceFile = null;
        _aiTreeIndexes = new int[] { iIndex };
    }

    public IndexId(File baseFile) {
        _sourceFile = baseFile;
        _aiTreeIndexes = null;
    }

    private IndexId(File baseFile, int[] aiIndex) {
        _sourceFile = baseFile;
        _aiTreeIndexes = aiIndex;
    }

    public IndexId(String sSerialized) throws NotThisTypeException {

        String[] asParts = Misc.regex("([^\\[]+)(\\[[^\\]]+\\])?", sSerialized);
        if (asParts == null || asParts.length != 3)
            throw new NotThisTypeException("Invalid id format: " + sSerialized);

        if (UNNAMED.equals(asParts[1]))
            _sourceFile = null;
        else
            _sourceFile = new File(asParts[1]);

        if (asParts[2] == null)
            _aiTreeIndexes = null;
        else {
            _aiTreeIndexes = Misc.splitInt(asParts[2].substring(1, asParts[2].length()-1), "\\.");
            if (_aiTreeIndexes == null)
                throw new NotThisTypeException("Invalid id format: " + sSerialized);
        }

    }

    private static final String UNNAMED = "Unnamed";
    private static final File UNNAMED_FILE = new File(UNNAMED);
    private File safePath() {
        return _sourceFile == null ? UNNAMED_FILE : _sourceFile;
    }


    public String serialize() {
        return getId();
    }

    public String getId() {
        return Misc.forwardSlashPath(safePath()) + getTreeIndex();
    }

    public File getFile() {
        return _sourceFile;
    }

    private String getTreeIndex() {
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

    public File getSuggestedBaseName(String sFallback) {
        String sFile;
        if (_sourceFile == null)
            sFile = sFallback;
        else
            sFile = _sourceFile.getPath();
        return new File(sFile + getTreeIndex());
    }

    /** Like {@link File#getName()}. */
    public String getTopLevel() {
        return safePath().getName() + getTreeIndex();
    }

    /** Same as {@link #serialize()}. */
    @Override
    public String toString() {
        return serialize();
    }

    /** Returns if the supplied id is a direct parent of this id. */
    public boolean isParent(IndexId parentId) {
        if (isRoot())
            return false;
        
        // check if the other item is part of the same file
        File parentFile = parentId._sourceFile;
        if (_sourceFile != parentFile && (_sourceFile == null || !_sourceFile.equals(parentFile)))
            return false; // nope

        // make sure the other item is a direct parent
        int[] aiParentTreeIndexes = parentId._aiTreeIndexes;
        if (aiParentTreeIndexes == null) {
            if (_aiTreeIndexes.length == 1) {
                return true;
            }
        } else if (_aiTreeIndexes.length - 1 == aiParentTreeIndexes.length) {
            for (int i = 0; i < aiParentTreeIndexes.length; i++) {
                if (_aiTreeIndexes[i] != aiParentTreeIndexes[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    public boolean isRoot() {
        if (_aiTreeIndexes == null)
            return true;
        return (_sourceFile == null && _aiTreeIndexes.length == 1);
    }

    public IndexId createNext() {
        if (_aiTreeIndexes == null)
            throw new IllegalStateException("Unable to create the next id from " + this);
        int[] aiNext = _aiTreeIndexes.clone();
        aiNext[aiNext.length-1] += 1;
        return new IndexId(_sourceFile, aiNext);
    }

    IndexId createChild() {
        int[] aiChild;
        if (_aiTreeIndexes == null)
            aiChild = new int[] {0};
        else {
            aiChild = new int[_aiTreeIndexes.length+1];
            System.arraycopy(_aiTreeIndexes, 0, aiChild, 0, _aiTreeIndexes.length);
            aiChild[aiChild.length-1] = 0;
        }
        return new IndexId(_sourceFile, aiChild);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final IndexId other = (IndexId) obj;
        if (_sourceFile != other._sourceFile && (_sourceFile == null || !_sourceFile.equals(other._sourceFile)))
            return false;
        if (!Arrays.equals(this._aiTreeIndexes, other._aiTreeIndexes))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (this._sourceFile != null ? this._sourceFile.hashCode() : 0);
        return hash;
    }

}
