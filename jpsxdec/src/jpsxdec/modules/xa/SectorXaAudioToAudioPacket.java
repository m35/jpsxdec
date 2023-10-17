/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

package jpsxdec.modules.xa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.adpcm.XaAdpcmDecoder;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.audio.sectorbased.SectorBasedDecodedAudioPacket;
import jpsxdec.util.ByteArrayFPIS;

/** Converts a XA audio sector to a single decoded audio packet.
 * Maintains the decoding context. */
public class SectorXaAudioToAudioPacket implements IdentifiedSectorListener<SectorXaAudio> {

    @Nonnull
    private final XaAdpcmDecoder _decoder;
    private final ByteArrayOutputStream _tempBuffer = new ByteArrayOutputStream();
    private final int _iFileNumber;
    private final int _iChannel;
    private final int _iSampleFramesPerSecond;
    @Nonnull
    private final AudioFormat _audioFormat;
    @CheckForNull
    private SectorBasedDecodedAudioPacket.Listener _listener;
    private final int _iStartSector;
    private final int _iEndSectorInclusive;

    public SectorXaAudioToAudioPacket(@Nonnull XaAdpcmDecoder decoder, int iSampleFramesPerSecond,
                                      int iFileNumber, int iChannel,
                                      int iStartSector, int iEndSectorInclusive)
    {
        _decoder = decoder;
        _audioFormat = decoder.getOutputFormat(iSampleFramesPerSecond);
        _iSampleFramesPerSecond = iSampleFramesPerSecond;
        _iFileNumber = iFileNumber;
        _iChannel = iChannel;
        _iStartSector = iStartSector;
        _iEndSectorInclusive = iEndSectorInclusive;
    }
    public void setListener(@CheckForNull SectorBasedDecodedAudioPacket.Listener listener) {
        _listener = listener;
    }

    @Override
    public @Nonnull Class<SectorXaAudio> getListeningFor() {
        return SectorXaAudio.class;
    }

    @Override
    public void feedSector(@Nonnull SectorXaAudio xaSector, @Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (xaSector.getSectorNumber() < _iStartSector ||
            xaSector.getSectorNumber() > _iEndSectorInclusive)
            return;

        if (xaSector.getFileNumber() != _iFileNumber ||
            xaSector.getChannel() != _iChannel ||
            xaSector.getAdpcmBitsPerSample() != _decoder.getAdpcmBitsPerSample() ||
            xaSector.isStereo() != _decoder.isStereo() ||
            xaSector.getSamplesPerSecond() != _iSampleFramesPerSecond)
            return;

        _tempBuffer.reset();
        long lngSamplesWritten = _decoder.getSampleFramesWritten();

        try {
            ByteArrayFPIS inStream = xaSector.getIdentifiedUserDataStream();
            _decoder.decode(inStream, _tempBuffer, xaSector.getSectorNumber());
        } catch (IOException ex) {
            throw new RuntimeException("Should not happen when reading/writing to/from byte stream", ex);
        }

        if (_decoder.hadCorruption())
            log.log(Level.WARNING, I.XA_AUDIO_CORRUPTED(xaSector.getSectorNumber(), lngSamplesWritten));

        if (_listener != null) {
            SectorBasedDecodedAudioPacket packet = new SectorBasedDecodedAudioPacket(_iChannel, _audioFormat,
                                                                                     _tempBuffer.toByteArray(),
                                                                                     xaSector.getSectorNumber());

            _listener.audioPacketComplete(packet, log);
        }
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
    }


    public double getVolume() {
        return _decoder.getVolume();
    }

    public @Nonnull AudioFormat getOutputFormat() {
        return _audioFormat;
    }

}
