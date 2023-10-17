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

package jpsxdec.discitems;

import java.io.File;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.util.Misc;

/** Holds the unique index number, and unique string id for a disc item.
 * These can be compared and nested. */
public class IndexId {
    // this implementation could probably be improved
    // it starts with a source file (or '?' if none)
    // followed by a sequence of nested indexed levels
    private final @CheckForNull File _sourceFile;
    private final @CheckForNull int[] _aiTreeIndexes;

    public IndexId(int iIndex) {
        _sourceFile = null;
        _aiTreeIndexes = new int[] { iIndex };
    }

    public IndexId(@Nonnull File baseFile) {
        _sourceFile = baseFile;
        _aiTreeIndexes = null;
    }

    private IndexId(@CheckForNull File baseFile, @Nonnull int[] aiIndex) {
        _sourceFile = baseFile;
        _aiTreeIndexes = aiIndex;
    }

    public IndexId(@Nonnull String sSerialized) throws LocalizedDeserializationFail {

        String[] asParts = Misc.regex("([^\\[]+)(\\[[^\\]]+\\])?", sSerialized);
        if (asParts == null || asParts.length != 3)
            throw new LocalizedDeserializationFail(I.ID_FORMAT_INVALID(sSerialized));

        if (UNNAMED_INDEX.equals(asParts[1]))
            _sourceFile = null;
        else
            _sourceFile = new File(asParts[1]);

        if (asParts[2] == null)
            _aiTreeIndexes = null;
        else {
            _aiTreeIndexes = Misc.splitInt(asParts[2].substring(1, asParts[2].length()-1), "\\.");
            if (_aiTreeIndexes == null)
                throw new LocalizedDeserializationFail(I.ID_FORMAT_INVALID(sSerialized));
        }

    }

    /** How unnamed files will be saved in the index (never localized). */
    private static final String UNNAMED_INDEX = "?";
    /** How unnamed files will be displayed (localized). */
    private static final ILocalizedMessage UNNAMED_FILE_NAME = I.UNNAMED_DISC_ITEM();
    /** Pre-create file with the localized name. */
    private static final File UNNAMED_FILE = new File(UNNAMED_FILE_NAME.getLocalizedMessage());
    /** Always returns a non-null path. */
    private @Nonnull File safePath() {
        return _sourceFile == null ? UNNAMED_FILE : _sourceFile;
    }


    public @Nonnull String serialize() {
        return (_sourceFile == null ? UNNAMED_INDEX : Misc.forwardSlashPath(_sourceFile)) +
                getTreeIndex();
    }

    public @Nonnull String getId() {
        return Misc.forwardSlashPath(safePath()) + getTreeIndex();
    }

    public @CheckForNull File getFile() {
        return _sourceFile;
    }

    private @Nonnull String getTreeIndex() {
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

    public @Nonnull File getSuggestedBaseName(@Nonnull String sFallback) {
        String sFile;
        if (_sourceFile == null)
            sFile = sFallback;
        else
            sFile = _sourceFile.getPath();
        return new File(sFile + getTreeIndex());
    }

    /** Like {@link File#getName()}. */
    public @Nonnull String getTopLevel() {
        return safePath().getName() + getTreeIndex();
    }

    @Override
    public String toString() {
        return serialize();
    }

    /** Returns if the supplied id is a direct parent of this id. */
    public boolean isParent(@Nonnull IndexId parentId) {
        if (isRoot())
            return false;

        // check if the other item is part of the same file
        File parentFile = parentId._sourceFile;
        if (!Misc.objectEquals(_sourceFile, parentFile))
            return false; // nope

        // make sure the other item is a direct parent
        int[] aiParentTreeIndexes = parentId._aiTreeIndexes;
        assert _aiTreeIndexes != null; // _aiTreeIndexes already confirmed to != null in isRoot()
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

    public @Nonnull IndexId createNext() {
        if (_aiTreeIndexes == null)
            throw new IllegalStateException("Unable to create the next id from " + this);
        int[] aiNext = _aiTreeIndexes.clone();
        aiNext[aiNext.length-1] += 1;
        return new IndexId(_sourceFile, aiNext);
    }

    public @Nonnull IndexId createChild() {
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
        if (!Misc.objectEquals(_sourceFile, other._sourceFile))
            return false;
        if (!Arrays.equals(this._aiTreeIndexes, other._aiTreeIndexes))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (_sourceFile != null ? _sourceFile.hashCode() : 0);
        return hash;
    }

}
