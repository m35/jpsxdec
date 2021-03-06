/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019  Michael Sabin
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

package jpsxdec.modules.roadrash;

import java.io.PrintStream;
import java.util.Arrays;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.Fraction;


public class DemuxedRoadRashFrame implements IDemuxedFrame {

    @Nonnull
    private final RoadRashPacketSectors _sectors;
    @Nonnull
    private final RoadRashPacket.MDEC _mdecPacket;
    @Nonnull
    private final RoadRashPacket.VLC0 _vlc;
    @Nonnull
    private final FrameNumber _frameNumber;
    @Nonnull
    private final Fraction _presentationSector;

    public DemuxedRoadRashFrame(@Nonnull RoadRashPacketSectors sectors,
                                @Nonnull RoadRashPacket.MDEC mdecPacket,
                                @Nonnull RoadRashPacket.VLC0 vlc,
                                @Nonnull FrameNumber frameNumber,
                                @Nonnull Fraction presentationSector)
    {
        _sectors = sectors;
        _mdecPacket = mdecPacket;
        _vlc = vlc;
        _frameNumber = frameNumber;
        _presentationSector = presentationSector;
    }


    public int getWidth() {
        return _mdecPacket.getWidth();
    }

    public int getHeight() {
        return _mdecPacket.getHeight();
    }

    public @Nonnull MdecInputStream getCustomFrameMdecStream() {
        return _vlc.makeFrameBitStreamUncompressor(_mdecPacket);
    }

    public @Nonnull FrameNumber getFrame() {
        return _frameNumber;
    }

    public int getStartSector() {
        return _sectors.iStartSector;
    }

    public int getEndSector() {
        return _sectors.iEndSector;
    }

    public @Nonnull Fraction getPresentationSector() {
        return _presentationSector;
    }

    public int getDemuxSize() {
        return _mdecPacket.getBitstream().length;
    }

    /** Not really useful, but why not. */
    public @Nonnull byte[] copyDemuxData() {
        return Arrays.copyOfRange(_mdecPacket.getBitstream(), 0, getDemuxSize());
    }

    public void printSectors(PrintStream ps) {
        // TODO?
    }

    public void writeToSectors(byte[] abNewDemux, int iNewUsedSize, int iNewMdecCodeCount, CdFileSectorReader cd, ILocalizedLogger log) throws LoggedFailure {
        throw new UnsupportedOperationException("No support for replacing Road Rash frames");
    }

}
