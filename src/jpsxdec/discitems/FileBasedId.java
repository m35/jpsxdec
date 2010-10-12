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

import java.io.File;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;

public class FileBasedId {
    private final File _baseFile;
    private final int[] _aiSubIndexes;

    public FileBasedId() {
        _baseFile = null;
        _aiSubIndexes = new int[] { 0 };
    }

    public FileBasedId(File baseFile) {
        _baseFile = baseFile;
        _aiSubIndexes = null;
    }

    private FileBasedId(File baseName, int[] aiSubIndexes) {
        _baseFile = baseName;
        _aiSubIndexes = aiSubIndexes;
    }

    public FileBasedId(String sSerialized) throws NotThisTypeException {
        if (sSerialized.endsWith("]")) {
            int iIdxStart = sSerialized.lastIndexOf("[");
            _baseFile = new File(sSerialized.substring(0, iIdxStart));
            try {
                _aiSubIndexes = Misc.parseDelimitedInts(sSerialized.substring(iIdxStart+1, sSerialized.length()-1), "\\.");
            } catch (NumberFormatException ex) {
                throw new NotThisTypeException("Invalid id format: " + sSerialized);
            }
        } else {
            _baseFile = new File(sSerialized);
            _aiSubIndexes = null;
        }
    }

    public String serialize() {
        String sFilePart;
        if (_baseFile == null)
            sFilePart = "Unnamed";
        else
            sFilePart = _baseFile.getPath();

        String sNumberPart = getBaseIndex();

        return sFilePart + sNumberPart;
    }


    public File getBaseFile() {
        return _baseFile;
    }

    public int getDepth() {
        return _aiSubIndexes == null ? 0 : _aiSubIndexes.length;
    }

    public int getLevelIndex(int iLevel) {
        return _aiSubIndexes[iLevel];
    }

    public String getBaseIndex() {
        if (_aiSubIndexes == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder("[");
            for (int i : _aiSubIndexes) {
                if (sb.length() > 1)
                    sb.append('.');
                sb.append(i);
            }
            sb.append(']');
            return sb.toString();
        }
    }

    public FileBasedId newChild() {
        int[] aiChildInexes;
        if (_aiSubIndexes == null) {
            aiChildInexes = new int[] { 0 };
        } else {
            aiChildInexes = new int[_aiSubIndexes.length + 1];
            System.arraycopy(_aiSubIndexes, 0, aiChildInexes, 0, _aiSubIndexes.length);
            aiChildInexes[aiChildInexes.length-1] = 0;
        }
        return new FileBasedId(_baseFile, aiChildInexes);
    }

    public FileBasedId newIncrement() {
        if (_aiSubIndexes == null)
            throw new IllegalStateException("Cannot create a sibling without indexes");
        int[] aiSiblingIndexes = _aiSubIndexes.clone();
        aiSiblingIndexes[aiSiblingIndexes.length - 1] += 1;
        return new FileBasedId(_baseFile, aiSiblingIndexes);
    }
}
