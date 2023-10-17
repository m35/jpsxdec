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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.ByteArrayFPIS;

/** Audio sectors used in Chrono Cross movies. Nearly identical to FF9
 *  audio sectors. */
public class SectorChronoXAudio extends IdentifiedSector implements ISquareAudioSector {

    private static final Logger LOG = Logger.getLogger(SectorChronoXAudio.class.getName());

    // .. Static stuff .....................................................

    /** Used on disc 1. */
    public static final long AUDIO_CHUNK_MAGIC1 = 0x00000160L;
    /** Used on disc 1. */
    public static final long AUDIO_CHUNK_MAGIC2 = 0x00010160L;
    /** Used on disc 2. */
    public static final long AUDIO_CHUNK_MAGIC3 = 0x01000160L;
    /** Used on disc 2. */
    public static final long AUDIO_CHUNK_MAGIC4 = 0x01010160L;

    public static final int FRAME_AUDIO_CHUNK_HEADER_SIZE = 208;

    // .. Instance .........................................................

    // Magic                                 //  0    [4 bytes]
    private int _iAudioChunkNumber;          //  4    [2 bytes]
    private int _iAudioChunksInFrame;        //  6    [2 bytes]
    private int _iHeaderFrameNumber;         //  8    [2 bytes]
    // 118 bytes unknown                     //  10   [118 bytes]
    @CheckForNull
    private SquareAKAOstruct _akaoStruct;    //  128  [80 bytes]
    //   208 TOTAL

    private int _iSoundUnitCount;

    public SectorChronoXAudio(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        // since all Chrono Cross movie sectors are in Mode 2 Form 1, we can
        // still decode the movie even if there is no raw sector header.
        // DATA must be set, and FORM must not be set
        if (subModeExistsAndMaskDoesNotEqual(SubMode.MASK_DATA | SubMode.MASK_FORM, SubMode.MASK_DATA))
            return;

        // make sure the magic number is correct
        long lngMagic = cdSector.readUInt32LE(0);
        if (lngMagic != AUDIO_CHUNK_MAGIC1 &&
            lngMagic != AUDIO_CHUNK_MAGIC2 &&
            lngMagic != AUDIO_CHUNK_MAGIC3 &&
            lngMagic != AUDIO_CHUNK_MAGIC4)
            return;

        _iAudioChunkNumber = cdSector.readSInt16LE(4);
        if (_iAudioChunkNumber != 0 && _iAudioChunkNumber != 1) return;
        _iAudioChunksInFrame = cdSector.readSInt16LE(6);
        if (_iAudioChunksInFrame != 2) return;
        _iHeaderFrameNumber = cdSector.readSInt16LE(8);
        if (_iHeaderFrameNumber < 1) return;

        _akaoStruct = new SquareAKAOstruct(cdSector, 128);
        // All Chrono Chross movies have an AKAO tag
        if (_akaoStruct.AKAO != SquareAKAOstruct.AKAO_ID) return;
        if (_akaoStruct.BytesOfData > CdSector.SECTOR_USER_DATA_SIZE_MODE2FORM2) return;

        if (_akaoStruct.BytesOfData % SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT != 0) {
            // this shouldn't happen, if it does we'll only pay attention to the full sound units
            LOG.log(Level.WARNING, "Bytes of audio data {0} is not a multiple of sound unit size in sector {1}",
                                   new Object[]{_akaoStruct.BytesOfData, cdSector});
        }
        _iSoundUnitCount = (int) (_akaoStruct.BytesOfData / SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT);

        setProbability(100);
    }

    @Override
    public @Nonnull String getTypeName() {
        return "CX Audio";
    }

    @Override
    public int getSoundUnitCount() {
        return _iSoundUnitCount;
    }

    @Override
    public int getHeaderFrameNumber() {
        return _iHeaderFrameNumber;
    }

    @Override
    public int getSampleFramesPerSecond() {
        return 44100;
    }

    @Override
    public boolean isLeftChannel() {
        switch (_iAudioChunkNumber) {
            case 0: return true;
            case 1: return false;
            default: throw new IllegalStateException();
        }
    }

    @Override
    public int getAudioDataStartOffset() {
        return FRAME_AUDIO_CHUNK_HEADER_SIZE;
    }

    @Override
    public int getAudioDataSize() {
        return (int)_akaoStruct.BytesOfData;
    }

    @Override
    public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(getCdSector().getCdUserDataStream(),
                FRAME_AUDIO_CHUNK_HEADER_SIZE, getAudioDataSize());
    }

    @Override
    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d %s",
            getTypeName(),
            super.toString(),
            _iHeaderFrameNumber,
            _iAudioChunkNumber,
            _iAudioChunksInFrame,
            _akaoStruct == null ? "<indexing>" : _akaoStruct.toString()
            );
    }

}
