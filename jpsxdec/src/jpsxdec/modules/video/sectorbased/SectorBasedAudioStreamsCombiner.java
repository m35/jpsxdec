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

package jpsxdec.modules.video.sectorbased;

import java.util.List;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.audio.sectorbased.DiscItemSectorBasedAudioStream;
import jpsxdec.modules.audio.sectorbased.ISectorClaimToSectorBasedDecodedAudio;
import jpsxdec.modules.audio.sectorbased.SectorBasedDecodedAudioPacket;

/** Combines multiple the {@link ISectorClaimToSectorBasedDecodedAudio}s from multiple
 * {@link DiscItemSectorBasedAudioStream} into a single continuous stream.
 * This is necessary when a video stream has multiple audio contiguous
 * audio clips, usually due to corrupted audio sectors due to ripping error. */
public class SectorBasedAudioStreamsCombiner implements ISectorClaimToSectorBasedDecodedAudio {

    @Nonnull
    private final AudioFormat _outFormat;
    private final int _iSampleRate;
    private int _iStartSector;
    private int _iEndSector;
    private int _iPresStartSector;

    @Nonnull
    private final ISectorClaimToSectorBasedDecodedAudio[] _aoDecoders;
    @Nonnull
    private final DiscItemSectorBasedAudioStream[] _aoSrcItems;

    public SectorBasedAudioStreamsCombiner(@Nonnull List<DiscItemSectorBasedAudioStream> audStreams, double dblVolume)
    {
        if (thereIsOverlap(audStreams))
            throw new IllegalArgumentException("Streams are not mutually exclusive.");

        _aoSrcItems = audStreams.toArray(new DiscItemSectorBasedAudioStream[audStreams.size()]);

        _aoDecoders = new ISectorClaimToSectorBasedDecodedAudio[_aoSrcItems.length];

        final boolean blnIsStereo;

        blnIsStereo = audStreams.get(0).isStereo();
        _iSampleRate = audStreams.get(0).getSampleFramesPerSecond();
        _iStartSector = audStreams.get(0).getStartSector();
        _iEndSector = audStreams.get(0).getEndSector();
        _iPresStartSector = audStreams.get(0).getPresentationStartSector();
        DiscSpeed discSpeed = audStreams.get(0).getDiscSpeed(); // first stream may not know the speed
        _aoDecoders[0] = audStreams.get(0).makeDecoder(dblVolume);

        for (int i = 1; i < _aoDecoders.length; i++) {
            DiscItemSectorBasedAudioStream aud = audStreams.get(i);

            if (!aud.hasSameFormat(audStreams.get(0)))
                throw new IllegalArgumentException("Different format audio.");

            if (discSpeed != null) {
                // make sure to accept unknown disc speeds
                if (aud.getDiscSpeed() != null && discSpeed != aud.getDiscSpeed())
                    throw new IllegalArgumentException("Different disc speeds.");
            } else {
                discSpeed = aud.getDiscSpeed();
            }

            _iStartSector = Math.min(_iStartSector, aud.getStartSector());
            _iEndSector = Math.max(_iEndSector, aud.getEndSector());
            _iPresStartSector = Math.min(_iPresStartSector, aud.getPresentationStartSector());

            _aoDecoders[i] = aud.makeDecoder(dblVolume);
        }

        _outFormat = new AudioFormat(_iSampleRate, 16, blnIsStereo ? 2 : 1,
                                     true, false);
    }

    private static boolean thereIsOverlap(@Nonnull List<DiscItemSectorBasedAudioStream> audStreams) {
        for (int i = 0; i < audStreams.size(); i++) {
            for (int j = i+1; j < audStreams.size(); j++) {
                if (audStreams.get(i).overlaps(audStreams.get(j)))
                    return true;
            }
        }
        return false;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        for (ISectorClaimToSectorBasedDecodedAudio decoder : _aoDecoders) {
            decoder.attachToSectorClaimer(scs);
        }
    }

    @Override
    public void setSectorBasedAudioListener(@Nonnull SectorBasedDecodedAudioPacket.Listener listener) {
         for (ISectorClaimToSectorBasedDecodedAudio decoder : _aoDecoders) {
            decoder.setSectorBasedAudioListener(listener);
         }
    }

    @Override
    public double getVolume() {
        // assume the volume is the same for all decoders
        return _aoDecoders[0].getVolume();
    }

    @Override
    public @Nonnull AudioFormat getOutputFormat() {
        return _outFormat;
    }

    @Override
    public int getSampleFramesPerSecond() {
        return _iSampleRate;
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
    public int getAbsolutePresentationStartSector() {
        return _iPresStartSector;
    }

}
