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

import jpsxdec.plugins.JPSXPlugin;
import java.io.IOException;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.IndexingDemuxerIS;
import jpsxdec.plugins.DiscItemSerialization;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor_FF7;

/**
 * Watches for Square's unique audio format sectors, and Final Fantasy 7
 * and Chrono Cross video sectors.
 */
public class JPSXPluginSquare extends JPSXPlugin {

    private static JPSXPluginSquare SINGLETON;

    public static JPSXPluginSquare getPlugin() {
        if (SINGLETON == null)
            SINGLETON = new JPSXPluginSquare();
        return SINGLETON;
    }

    private int _iAudioStartSector;
    private long _lngAudioLeftSampleCount;
    private long _lngAudioRightSampleCount;

    private ISquareAudioSector _prevAudioSect;

    private int _iPrevLeftAudioSectorNum = -1;
    /** -1 indicates no period has been calculated yet. */
    private int _iLeftAudioPeriod = -1;

    private JPSXPluginSquare() {
    }

    @Override
    public IdentifiedSector identifySector(CDSector sector) {
        try { return new SectorFF7Video(sector); }
        catch (NotThisTypeException ex) {}
        try { return new SectorChronoXAudio(sector); }
        catch (NotThisTypeException ex) {}
        try { return new SectorFF8.PSXSectorFF8Audio(sector); }
        catch (NotThisTypeException ex) {}
        try { return new SectorFF8.PSXSectorFF8Video(sector); }
        catch (NotThisTypeException ex) {}
        try { return new SectorFF9.PSXSectorFF9Audio(sector); }
        catch (NotThisTypeException ex) {}
        try { return new SectorFF9.PSXSectorFF9Video(sector); }
        catch (NotThisTypeException ex) {}
        try { return new SectorChronoXVideo(sector); }
        catch (NotThisTypeException ex) {}
        try { return new SectorChronoXVideoNull(sector); }
        catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void deserialize_lineRead(DiscItemSerialization fields) {
        try {
            if (DiscItemSquareAudioStream.TYPE_ID.equals(fields.getType()))
                super.addDiscItem(new DiscItemSquareAudioStream(fields));
        } catch (NotThisTypeException ex) {}
    }

    /**
     * All known games that use Square audio run their streaming media at 2x
     * speed.
     */
    @Override
    public void indexing_sectorRead(IdentifiedSector identifiedSector) {
        if (!(identifiedSector instanceof ISquareAudioSector))
            return;

        ISquareAudioSector audioSect = (ISquareAudioSector)identifiedSector;

        // FF9 has some movies without audio
        if (audioSect.getLeftSampleCount() == 0 && audioSect.getRightSampleCount() == 0)
            return;

        boolean blnNewVid = false;
        if (audioSect.getAudioChannel() == 0) {
            if (_iPrevLeftAudioSectorNum >= 0) {
                int iPeriod = audioSect.getSectorNumber() - _iPrevLeftAudioSectorNum;
                if (_iLeftAudioPeriod < 0)
                    _iLeftAudioPeriod = iPeriod;
                else if (_iLeftAudioPeriod != iPeriod)
                    blnNewVid = true;
            }
            _iPrevLeftAudioSectorNum = audioSect.getSectorNumber();
        }

        if (blnNewVid || _prevAudioSect == null || !audioSect.matchesPrevious(_prevAudioSect))
        {
            if (_prevAudioSect != null) {
                // submit the audio stream that just ended
                super.addDiscItem(_prevAudioSect.createMedia(_iAudioStartSector, _lngAudioLeftSampleCount, _lngAudioRightSampleCount, _iLeftAudioPeriod - 1));
            }
            // reset the current audio stream being tracked
            _iAudioStartSector = audioSect.getSectorNumber();
            _lngAudioLeftSampleCount = 0;
            _lngAudioRightSampleCount = 0;
            _iLeftAudioPeriod = -1;
        }

        _lngAudioLeftSampleCount += audioSect.getLeftSampleCount();
        _lngAudioRightSampleCount += audioSect.getRightSampleCount();

        _prevAudioSect = audioSect;
    }

    @Override
    public void indexing_endOfDisc() {
        if (_prevAudioSect != null) {
            // submit the audio stream
            super.addDiscItem(_prevAudioSect.createMedia(_iAudioStartSector, _lngAudioLeftSampleCount, _lngAudioRightSampleCount, _iLeftAudioPeriod - 1));
        }
    }

    @Override
    public void indexing_static(IndexingDemuxerIS oIS) throws IOException {
        // add any Square specific static media here
    }

    @Override
    public String getPluginDescription() {
        return "SquareSoft game handling for jPSXdec by Michael Sabin";
    }

    @Override
    public DemuxFrameUncompressor identifyVideoFrame(byte[] abHeaderBytes, long lngFrameNum) {
        if (DemuxFrameUncompressor_FF7.checkHeader(abHeaderBytes))
            return new DemuxFrameUncompressor_FF7();
        else
            return null;
    }
}
