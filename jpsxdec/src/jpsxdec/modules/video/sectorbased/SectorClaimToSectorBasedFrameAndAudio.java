/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

package jpsxdec.modules.video.sectorbased;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.audio.DecodedAudioPacket;
import jpsxdec.modules.audio.sectorbased.ISectorClaimToSectorBasedDecodedAudio;
import jpsxdec.modules.audio.sectorbased.SectorBasedAudioListenerTranslator;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToFrameAndAudio;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.bitstreams.IBitStreamUncompressor;
import jpsxdec.util.Fraction;

public class SectorClaimToSectorBasedFrameAndAudio implements ISectorClaimToFrameAndAudio {
    @Nonnull
    private final SectorClaimToSectorBasedDemuxedFrame _video;
    @CheckForNull
    private final ISectorClaimToSectorBasedDecodedAudio _audio;

    private final int _iStartSector;
    private final int _iEndSector;

    public SectorClaimToSectorBasedFrameAndAudio(@Nonnull SectorClaimToSectorBasedDemuxedFrame video,
                                                 int iVidStartSector, int iVidEndSector,
                                                 @CheckForNull ISectorClaimToSectorBasedDecodedAudio audio)
    {
        _video = video;
        _audio = audio;
        if (_audio == null) {
            _iStartSector = iVidStartSector;
            _iEndSector = iVidEndSector;
        } else {
            _iStartSector = Math.min(iVidStartSector, audio.getStartSector());
            _iEndSector = Math.max(iVidEndSector, audio.getEndSector());
        }
    }

    @Override
    public int getStartSector() {
        return _iStartSector;
    }

    @Override
    public int getEndSector() {
        return _iEndSector;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        _video.attachToSectorClaimer(scs);
        if (_audio != null) {
            _audio.attachToSectorClaimer(scs);
        }
    }

    // .............

    @Override
    public void setFrameListener(@Nonnull IDemuxedFrame.Listener listener) {
        _video.setFrameListener(new FrameListenerTranslator(listener));
    }

    // .............

    @Override
    public boolean hasAudio() {
        return _audio != null;
    }

    @Override
    public void setAudioListener(@Nonnull DecodedAudioPacket.Listener listener) {
        _audio.setSectorBasedAudioListener(new SectorBasedAudioListenerTranslator(listener));
    }

    @Override
    public @Nonnull AudioFormat getOutputFormat() {
        return _audio.getOutputFormat();
    }

    @Override
    public double getVolume() {
        return _audio.getVolume();
    }

    @Override
    public int getAbsolutePresentationStartSector() {
        return _audio.getAbsolutePresentationStartSector();
    }

    @Override
    public int getSampleFramesPerSecond() {
        return _audio.getSampleFramesPerSecond();
    }

    private static class FrameListenerTranslator implements ISectorBasedDemuxedFrame.Listener {
        @Nonnull
        private final IDemuxedFrame.Listener _listener;

        public FrameListenerTranslator(@Nonnull IDemuxedFrame.Listener listener) {
            _listener = listener;
        }

        @Override
        public void frameComplete(@Nonnull ISectorBasedDemuxedFrame frame) throws LoggedFailure {
            _listener.frameComplete(new SectorBasedFrameWrapper(frame));
        }
    }


    private static class SectorBasedFrameWrapper implements IDemuxedFrame {

        @Nonnull
        private final ISectorBasedDemuxedFrame _sectorBased;

        public SectorBasedFrameWrapper(@Nonnull ISectorBasedDemuxedFrame sectorBased) {
            _sectorBased = sectorBased;
        }

        @Override
        public @Nonnull Fraction getPresentationSector() {
            return new Fraction(_sectorBased.getPresentationSector());
        }

        @Override
        public int getWidth() {
            return _sectorBased.getWidth();
        }

        @Override
        public int getHeight() {
            return _sectorBased.getHeight();
        }

        @Override
        public @Nonnull FrameNumber getFrame() {
            return _sectorBased.getFrame();
        }

        @Override
        public int getStartSector() {
            return _sectorBased.getStartSector();
        }

        @Override
        public int getEndSector() {
            return _sectorBased.getEndSector();
        }

        @Override
        public int getDemuxSize() {
            return _sectorBased.getDemuxSize();
        }

        @Override
        public @Nonnull byte[] copyDemuxData() {
            return _sectorBased.copyDemuxData();
        }

        @Override
        public @CheckForNull IBitStreamUncompressor getCustomFrameMdecStream() {
            return _sectorBased.getCustomFrameMdecStream();
        }

    }


}
