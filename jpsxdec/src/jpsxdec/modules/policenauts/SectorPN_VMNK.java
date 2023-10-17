/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.modules.policenauts;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;

/** @see SPacket */
public class SectorPN_VMNK extends SectorPolicenauts {

    public static final int WIDTH = 288;
    public static final int HEIGHT = 144;

    // I don't know what these mean
    private static final byte[] VMNK_HEADER = {
        'V', 'M', 'N', 'K',
	(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0xF0, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x04, (byte)0x00,
	(byte)0x10, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x10, (byte)0x00, (byte)0x00, (byte)0x00
    };

    private int _iWidth;
    private int _iHeight;

    public SectorPN_VMNK(@Nonnull CdSector cdSector) {
        super(cdSector, false);
        if (isSuperInvalidElseReset()) return;

        for (int i = 0; i < VMNK_HEADER.length; i++) {
            if (cdSector.readUserDataByte(i) != VMNK_HEADER[i])
                return;
        }

        _iWidth = cdSector.readSInt32LE(28);
        if (_iWidth != WIDTH)
            return;
        _iHeight = cdSector.readSInt32LE(32);
        if (_iHeight != HEIGHT)
            return;

        for (int i = VMNK_HEADER.length + 8; i < cdSector.getCdUserDataSize(); i++) {
            if (cdSector.readUserDataByte(i) != 0)
                return;
        }

        setProbability(100);
    }

    @Override
    public @Nonnull String getTypeName() {
        return "Policenauts VMNK";
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    @Override
    public String toString() {
        return String.format("%s %s %dx%d", getTypeName(), super.toString(), _iWidth, _iHeight);
    }

}
