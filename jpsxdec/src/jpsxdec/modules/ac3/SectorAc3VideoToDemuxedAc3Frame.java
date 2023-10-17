/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2023  Michael Sabin
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

package jpsxdec.modules.ac3;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorRange;

/** Collects Ace Combat 3 sectors and generates frames.
 * Ace Combat 3 demuxers need to only demux for a given channel.
 * To demux for another channel, create a separate demuxer. */
public class SectorAc3VideoToDemuxedAc3Frame {

    public interface Listener {
        void frameComplete(@Nonnull DemuxedAc3Frame frame, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
    }

    private final int _iChannel;
    @Nonnull
    private final SectorRange _sectorRange;

    @CheckForNull
    private Ac3Demuxer _currentFrame;
    @CheckForNull
    private Listener _listener;

    public SectorAc3VideoToDemuxedAc3Frame(int iChannel, @Nonnull SectorRange sectorRange) {
        this(iChannel, sectorRange, null);
    }
    public SectorAc3VideoToDemuxedAc3Frame(int iChannel, @Nonnull SectorRange sectorRange, @Nonnull Listener listener) {
        _iChannel = iChannel;
        _sectorRange = sectorRange;
        _listener = listener;
    }
    public void setListener(@CheckForNull Listener listener) {
        _listener = listener;
    }

    public @Nonnull Ac3AddResult feedSector(@Nonnull SectorAceCombat3Video vidSector,
                                            @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        if (vidSector.getChannel() != _iChannel)
            return Ac3AddResult.WrongChannel;

        if (_currentFrame != null) {
            Ac3AddResult result = _currentFrame.addSector(vidSector);
            if (result == Ac3AddResult.WrongFormat)
                endOfSectors(log);
            // else ignore sectors with the same format
            //      and different channel (although the code above should prevent
            //      the different channel condition)
        }

        if (_sectorRange.sectorIsInRange(vidSector.getSectorNumber())) {
            if (_currentFrame == null)
                _currentFrame = new Ac3Demuxer(vidSector, log);
            if (_currentFrame.isFrameComplete())
                endOfSectors(log);
        } else {
            endOfSectors(log);
        }

        return Ac3AddResult.Same;
    }

    public void endOfSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (_currentFrame == null)
            return;
        if (_listener != null)
            _listener.frameComplete(_currentFrame.finishFrame(log), log);
        _currentFrame = null;
    }
}

