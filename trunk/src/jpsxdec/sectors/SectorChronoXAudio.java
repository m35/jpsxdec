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

package jpsxdec.sectors;

import jpsxdec.audio.SquareAdpcmDecoder;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode;
import jpsxdec.util.ByteArrayFPIS;

/** Audio sectors used in Chrono Cross movies. Nearly identical to FF9
 *  audio sectors. */
public class SectorChronoXAudio extends IdentifiedSector
        implements ISquareAudioSector
{
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
    private int _iFrameNumber;               //  8    [2 bytes]
    // 118 bytes unknown                     //  10   [118 bytes]
    private SquareAKAOstruct _akaoStruct;    //  128  [80 bytes]
    //   208 TOTAL

    public SectorChronoXAudio(CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        // since all Chrono Cross movie sectors are in Mode 2 Form 1, we can 
        // still decode the movie even if there is no raw sector header.
        if (cdSector.hasSubHeader() &&
            cdSector.subModeMask(SubMode.MASK_DATA | SubMode.MASK_FORM) != SubMode.MASK_DATA)
        {
            return;
        }

        // make sure the magic nubmer is correct
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
        _iFrameNumber = cdSector.readSInt16LE(8);
        if (_iFrameNumber < 1) return;

        _akaoStruct = new SquareAKAOstruct(cdSector, 128);
        // All Chrono Chross movies have an AKAO tag
        if (_akaoStruct.AKAO != SquareAKAOstruct.AKAO_ID) return;

        setProbability(100);
    }

    // .. Properties .......................................................
    
    public int getAudioChunkNumber() {
        return _iAudioChunkNumber;
    }

    public int getAudioChannel() {
        return (int)_iAudioChunkNumber;
    }

    public int getAudioChunksInFrame() {
        return _iAudioChunksInFrame;
    }

    public int getFrameNumber() {
        return _iFrameNumber;
    }
    
    // .. Public functions .................................................

    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d %s",
            getTypeName(),
            super.toString(),
            _iFrameNumber,
            _iAudioChunkNumber,
            _iAudioChunksInFrame,
            _akaoStruct == null ? "<indexing>" : _akaoStruct.toString()
            );
    }

    public int getAudioDataSize() {
        return (int)_akaoStruct.BytesOfData;
    }

    public int getSamplesPerSecond() {
        return 44100;
    }

    public int getIdentifiedUserDataSize() {
        return super.getCdSector().getCdUserDataSize() -
                FRAME_AUDIO_CHUNK_HEADER_SIZE;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                FRAME_AUDIO_CHUNK_HEADER_SIZE, getIdentifiedUserDataSize());
    }

    public String getTypeName() {
        return "CX Audio";
    }

    public boolean isStereo() {
        return true;
    }

    public long getLeftSampleCount() {
        // if it's the 1st (0) chunk, then it holds the left audio
        if (getAudioChunkNumber() == 0) 
            return SquareAdpcmDecoder.calculateSamplesGenerated(getAudioDataSize());
        else
            return 0;
    }

    public long getRightSampleCount() {
        // if it's the 2nd (1) chunk, then it holds the right audio
        if (getAudioChunkNumber() == 1) 
            return SquareAdpcmDecoder.calculateSamplesGenerated(getAudioDataSize());
        else
            return 0;
    }

    public boolean matchesPrevious(ISquareAudioSector prevSect) {
        if (!(prevSect instanceof SectorChronoXAudio))
            return false;

        if (prevSect.getAudioChunkNumber() == 0) {
            if (getAudioChunkNumber() != 1 ||
                prevSect.getFrameNumber() != getFrameNumber() ||
                prevSect.getSectorNumber() + 1 != getSectorNumber())
                return false;
        } else if (prevSect.getAudioChunkNumber() == 1) {
            if (getAudioChunkNumber() != 0)
                return false;
            if (prevSect.getFrameNumber() > getFrameNumber())
                return false;
        }

        return true;
    }

}
