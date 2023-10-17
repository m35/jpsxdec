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

package jpsxdec.modules.square;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.strvideo.GenericStrVideoSector;


/** This is the header for Chrono Cross 'null' video sectors. */
public class SectorChronoXVideoNull extends IdentifiedSector {

    private long _lngMagic;
    private int _iChunkNumber;
    private int _iChunksInThisFrame;
    private int _iFrameNumber;

    public SectorChronoXVideoNull(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        // only if it has a sector header should we check if it reports DATA or VIDEO
        if (subModeExistsAndMaskEquals(SubMode.MASK_DATA | SubMode.MASK_VIDEO, 0))
            return;

        _lngMagic = cdSector.readUInt32LE(0);
        if (_lngMagic != GenericStrVideoSector.ChronoXHeader.CHRONO_CROSS_VIDEO_CHUNK_MAGIC1 &&
            _lngMagic != GenericStrVideoSector.ChronoXHeader.CHRONO_CROSS_VIDEO_CHUNK_MAGIC2)
            return;

        _iChunkNumber = cdSector.readSInt16LE(4);
        if (_iChunkNumber < 0) return;
        _iChunksInThisFrame = cdSector.readSInt16LE(6);
        if (_iChunksInThisFrame < 1) return;
        _iFrameNumber = cdSector.readSInt16LE(8);
        if (_iFrameNumber < 0) return;

        for (int i = 10; i < 32; i++) {
            if (cdSector.readUserDataByte(i) != -1) return;
        }

        setProbability(100);
    }

    // .. Public functions .................................................

    @Override
    public @Nonnull String getTypeName() {
        return "CX Video Null";
    }

    @Override
    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d",
            getTypeName(),
            super.cdToString(),
            _iFrameNumber,
            _iChunkNumber,
            _iChunksInThisFrame
            );
    }

}

