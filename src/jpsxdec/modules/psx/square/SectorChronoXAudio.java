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

package jpsxdec.modules.psx.square;

import jpsxdec.modules.IdentifiedSector;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode.DATA_AUDIO_VIDEO;
import jpsxdec.modules.DiscItem;
import jpsxdec.modules.JPSXModule;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** Audio sectors used in Chrono Cross movies. Nearly identical to FF9
 *  audio sectors. */
public class SectorChronoXAudio extends IdentifiedSector
        implements ISquareAudioSector
{
    // .. Static stuff .....................................................

    /** Used on disc 1. */
    public static final int AUDIO_CHUNK_MAGIC1 = 0x00000160;
    /** Used on disc 1. */
    public static final int AUDIO_CHUNK_MAGIC2 = 0x00010160;
    /** Used on disc 2. */
    public static final int AUDIO_CHUNK_MAGIC3 = 0x01000160;
    /** Used on disc 2. */
    public static final int AUDIO_CHUNK_MAGIC4 = 0x01010160;
    
    public static final int FRAME_AUDIO_CHUNK_HEADER_SIZE = 208;

    // .. Instance .........................................................

    // Magic                                  //  0    [4 bytes]
    private int _iAudioChunkNumber;          //  4    [2 bytes]
    private int _iAudioChunksInFrame;        //  6    [2 bytes]
    private int _iFrameNumber;               //  8    [2 bytes]
    // 118 bytes unknown                     //  12   [118 bytes]
    private SquareAKAOstruct _akaoStruct;    //  130  [80 bytes]
    //   208 TOTAL

    public SectorChronoXAudio(CdSector oCDSect) throws NotThisTypeException {
        super(oCDSect);

        // since all Chrono Cross movie sectors are in Mode 2 Form 1, we can 
        // still decode the movie even if there is no raw sector header.
        if (oCDSect.hasSectorHeader() &&
            ( oCDSect.getSubMode().getDataAudioVideo() != DATA_AUDIO_VIDEO.DATA ||
              oCDSect.getSubMode().getForm() != 1 ))
            throw new NotThisTypeException();

        try {
            InputStream is = oCDSect.getCdUserDataStream();

            long lngMagic = IO.readUInt32LE(is);
            
            // make sure the magic nubmer is correct
            if (lngMagic != AUDIO_CHUNK_MAGIC1 &&
                lngMagic != AUDIO_CHUNK_MAGIC2 &&
                lngMagic != AUDIO_CHUNK_MAGIC3 &&
                lngMagic != AUDIO_CHUNK_MAGIC4)
                throw new NotThisTypeException();

            _iAudioChunkNumber = IO.readSInt16LE(is);
            if (_iAudioChunkNumber < 0 || _iAudioChunkNumber > 1)
                throw new NotThisTypeException();
            _iAudioChunksInFrame = IO.readSInt16LE(is);
            if (_iAudioChunksInFrame != 2)
                throw new NotThisTypeException();
            _iFrameNumber = IO.readSInt16LE(is);
            if (_iFrameNumber < 1)
                throw new NotThisTypeException();
            
            IO.skip(is, 118);

            _akaoStruct = new SquareAKAOstruct(is);
            
            // All Chrono Chross movies have an AKAO tag
            if (_akaoStruct.AKAO != SquareAKAOstruct.AKAO_ID)
                throw new NotThisTypeException();
            
        } catch (IOException ex) {
            throw new NotThisTypeException();
        }
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
        return super.getCDSector().getCdUserDataSize() -
                FRAME_AUDIO_CHUNK_HEADER_SIZE;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCDSector().getCdUserDataStream(),
                FRAME_AUDIO_CHUNK_HEADER_SIZE, getIdentifiedUserDataSize());
    }

    /** [implements IPSXSector] */
    public int getSectorType() {
        return SECTOR_AUDIO;
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
            return getAudioDataSize() / 2; // TODO: I know this is wrong
        else
            return 0;
    }

    public long getRightSampleCount() {
        // if it's the 2nd (1) chunk, then it holds the right audio
        if (getAudioChunkNumber() == 1) 
            return getAudioDataSize() / 2; // TODO: I know this is wrong
        else
            return 0;
    }

    public boolean matchesPrevious(ISquareAudioSector oPrevSect) {
        if (!(oPrevSect instanceof SectorChronoXAudio))
            return false;

        if (oPrevSect.getAudioChunkNumber() == 0) {
            if (getAudioChunkNumber() != 1 ||
                oPrevSect.getFrameNumber() != getFrameNumber() ||
                oPrevSect.getSectorNumber() + 1 != getSectorNumber())
                return false;
        } else if (oPrevSect.getAudioChunkNumber() == 1) {
            if (getAudioChunkNumber() != 0)
                return false;
            if (oPrevSect.getFrameNumber() > getFrameNumber())
                return false;
        }

        return true;
    }

    public DiscItem createMedia(int iStartSector, long lngLeftSampleCount, long lngRightSampleCount, int iPeriod) {
            return new DiscItemSquareAudioStream(
                iStartSector, getSectorNumber(),
                lngLeftSampleCount, lngRightSampleCount,
                getSamplesPerSecond(), iPeriod - 1);
    }

    public JPSXModule getSourceModule() {
        return JPSXModuleSquare.getModule();
    }

}
