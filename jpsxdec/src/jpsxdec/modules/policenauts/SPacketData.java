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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import jpsxdec.adpcm.SpuAdpcmDecoder;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;

/** Wraps a {@link SPacketPos} with the actual packet data. */
public class SPacketData {

    @Nonnull
    private final SPacketPos _sPacketPos;
    @Nonnull
    private final byte[] _abData;

    public SPacketData(@Nonnull SPacketPos sPacketPos, @Nonnull byte[] abData) {
        _sPacketPos = sPacketPos;
        _abData = abData;
    }

    public boolean isAudio() {
        return _sPacketPos.isAudio();
    }

    public boolean isVideo() {
        return _sPacketPos.isVideo();
    }

    public @Nonnull byte[] getData() {
        return _abData;
    }

    public int getSoundUnitCount() {
        return _abData.length / SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT;
    }

    public void decodeAudio(@Nonnull SpuAdpcmDecoder.Mono decoder, @Nonnull OutputStream pcmOut) {
        ByteArrayInputStream spuIn = new ByteArrayInputStream(_abData);
        try {
            decoder.decode(spuIn, getSoundUnitCount(), pcmOut);
        } catch (IOException ex) {
            throw new RuntimeException("Should not happen", ex);
        }
    }

    public int getKlbsStartSectorNum() {
        return _sPacketPos.getKlbsStartSectorNum();
    }

    public int getKlbsEndSectorNum() {
        return _sPacketPos.getKlbsEndSectorNum();
    }

    public int getStartSector() {
        return _sPacketPos.getStartSector();
    }

    public int getEndSectorInclusive() {
        return _sPacketPos.getEndSectorInclusive();
    }

    public int getTimestamp() {
        return _sPacketPos.getTimestamp();
    }

    public int getDuration() {
        return _sPacketPos.getDuration();
    }

    @Override
    public String toString() {
        return _sPacketPos.toString();
    }
}
