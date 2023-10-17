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

package jpsxdec.modules.player;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.audio.DecodedAudioPacket;
import jpsxdec.modules.video.save.AudioSync;
import jpsxdec.util.IO;

/** Receives audio packets and sends them to the inner {@link OutputStream}
 * making sure to add silence if there is any gap in the packets.
 * This ensures the audio stays in sync with the playback. */
public class AudioPlayerSectorTimedWriter implements DecodedAudioPacket.Listener {

    @Nonnull
    private final OutputStream _audioOut;
    @Nonnull
    private final AudioSync _audioSync;

    private long _lngSampleFramesWritten = 0;

    public AudioPlayerSectorTimedWriter(@Nonnull OutputStream audioOutputStream,
                                        int iMovieStartSector,
                                        @Nonnull DiscSpeed discSpeed,
                                        int iSamplesPerSecond)
    {
        _audioOut = audioOutputStream;
        _audioSync = new AudioSync(iMovieStartSector, discSpeed, iSamplesPerSecond);
    }

    @Override
    public void audioPacketComplete(@Nonnull DecodedAudioPacket packet, @Nonnull ILocalizedLogger log) {
        try {
            long lngSampleFrameDiff = _audioSync.calculateAudioToCatchUp(packet.getPresentationSector(), _lngSampleFramesWritten);
            if (lngSampleFrameDiff > 0) {
                System.out.println("Audio out of sync " + lngSampleFrameDiff + " samples, adding silence.");
                long lngSilentBytes = lngSampleFrameDiff * packet.getAudioFormat().getFrameSize();
                while (lngSilentBytes > 0) {
                    int iToWrite = (int) Math.min((long)Integer.MAX_VALUE, lngSilentBytes);
                    IO.writeZeros(_audioOut, iToWrite);
                    lngSilentBytes -= iToWrite;
                }
                // TODO move this inside the loop in case there is an error it will keep the inner state correct
                _lngSampleFramesWritten += lngSampleFrameDiff;
            }

            _lngSampleFramesWritten += packet.getSampleFrameCount();

            byte[] abData = packet.getData();
            //System.out.println("Sending " + abData.length + " bytes of audio");
            _audioOut.write(abData);
        } catch (IOException ex) {
            // wrap the exception and have the MediaPlayer catch it outside the pipeline
            throw new WrapException(ex);
        }
    }
}
