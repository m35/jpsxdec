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

package jpsxdec.plugins.psx.square;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSector.CDXAHeader.SubMode.DATA_AUDIO_VIDEO;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** This is the header for Chrono Cross video sectors. */
public class SectorChronoXVideoNull extends IdentifiedSector
{
    private static final Logger log = Logger.getLogger(SectorChronoXVideoNull.class.getName());
    protected Logger log() { return log; }

    // .. Static stuff .....................................................

    public static final long CHRONO_CROSS_VIDEO_CHUNK_MAGIC1 = 0x81010160;
    public static final long CHRONO_CROSS_VIDEO_CHUNK_MAGIC2 = 0x01030160;

    public SectorChronoXVideoNull(CDSector cdSector) throws NotThisTypeException {
        super(cdSector);
        // only if it has a sector header should we check if it reports DATA or VIDEO
        if (cdSector.hasSectorHeader()) {
            DATA_AUDIO_VIDEO eType = cdSector.getSubMode().getDataAudioVideo();
            if (eType != DATA_AUDIO_VIDEO.DATA &&
                eType != DATA_AUDIO_VIDEO.VIDEO)
            {
                throw new NotThisTypeException();
            }
        }
        try {
            readHeader(cdSector.getCDUserDataStream());
        } catch (IOException ex) {
            throw new NotThisTypeException();
        }
    }

    private long _lngMagic;
    private int _iChunkNumber;
    private int _iChunksInThisFrame;
    private int _iFrameNumber;

    protected void readHeader(ByteArrayInputStream inStream)
            throws NotThisTypeException, IOException 
    {
        _lngMagic = IO.readUInt32LE(inStream);

        // TODO: This extra Chrono Cross hack is ugly, fix it
        if (_lngMagic != CHRONO_CROSS_VIDEO_CHUNK_MAGIC1 &&
            _lngMagic != CHRONO_CROSS_VIDEO_CHUNK_MAGIC2)
            throw new NotThisTypeException();

        _iChunkNumber = IO.readSInt16LE(inStream);
        if (_iChunkNumber < 0)
            throw new NotThisTypeException();
        _iChunksInThisFrame = IO.readSInt16LE(inStream);
        if (_iChunksInThisFrame < 1)
            throw new NotThisTypeException();
        _iFrameNumber = IO.readSInt16LE(inStream);
        if (_iFrameNumber < 0)
            throw new NotThisTypeException();

        for (int i = 0; i < 22; i++) {
            if (inStream.read() != 0xff)
                throw new NotThisTypeException();
        }

    }

    // .. Public functions .................................................

    public String getTypeName() {
        return "CX Video";
    }


    public JPSXPlugin getSourcePlugin() {
        return JPSXPluginSquare.getPlugin();
    }

    public int getSectorType() {
        return SECTOR_UNKNOWN;
    }

    public int getPSXUserDataSize() {
        return 0;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return null;
    }

}

