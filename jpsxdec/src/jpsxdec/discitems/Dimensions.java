/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.util.Misc;


public class Dimensions {

    static final String DIMENSIONS_KEY = "Dimensions";

    private final int _iWidth;
    private final int _iHeight;

    public Dimensions(int iWidth, int iHeight) {
        if (iWidth < 0 || iHeight < 0)
            throw new IllegalArgumentException();
        _iWidth = iWidth;
        _iHeight = iHeight;
    }

    public Dimensions(@Nonnull SerializedDiscItem fields) throws LocalizedDeserializationFail {
        String sDims = fields.getString(DIMENSIONS_KEY);
        int[] ai = Misc.splitInt(sDims, "x");
        if (ai == null || ai.length != 2)
            throw new LocalizedDeserializationFail(I.CMD_INVALID_DIMENSIONS(sDims));
        _iWidth = ai[0];
        _iHeight = ai[1];
    }

    public void serialize(@Nonnull SerializedDiscItem serial) {
        serial.addString(DIMENSIONS_KEY, String.format("%dx%d", _iWidth, _iHeight));
    }

    public int getWidth() {
        return _iWidth;
    }
    public int getHeight() {
        return _iHeight;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + this._iWidth;
        hash = 67 * hash + this._iHeight;
        return hash;
    }

    public boolean equals(int iWidth, int iHeight) {
        return _iWidth == iWidth && _iHeight == iHeight;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Dimensions other = (Dimensions) obj;
        return _iWidth == other._iWidth && _iHeight == other._iHeight;
    }

    @Override
    public String toString() {
        return _iWidth + "x" + _iHeight;
    }
}
