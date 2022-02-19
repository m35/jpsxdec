/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2020  Michael Sabin
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

package jpsxdec.modules.video.save;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.sharedaudio.DecodedAudioPacket;
import jpsxdec.modules.sharedaudio.ISectorAudioDecoder;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToDemuxedFrame;

/** Automatically connects all video pipeline and other components. */
public class AutowireVDP {

    /**
     * Because some video formats have context dependent bitstreams, or in other words,
     * the bitstream frames cannot be decoded independently, we need 2 possible publishers.
     * @see DiscItemVideoStream#hasIndependentBitstream()
     * @see IDemuxedFrame#getCustomFrameMdecStream()
     */
    @CheckForNull
    private Frame2Bitstream _frame2Bitstream;
    public void setFrame2Bitstream(@Nonnull Frame2Bitstream frame2Bitstream) {
        assertNull(_frame2Bitstream);
        assertNull(_bsProducer);
        _frame2Bitstream = frame2Bitstream;
    }

    @CheckForNull
    private VDP.IBitstreamProducer _bsProducer;
    public void setBitstreamProducer(@Nonnull VDP.IBitstreamProducer publishesBs) {
        assertNull(_frame2Bitstream);
        assertNull(_bsProducer);
        _bsProducer = publishesBs;
    }

    @CheckForNull
    private VDP.IBitstreamListener _bitstreamListener;
    // Expose this publicly for the saver to send data into
    public @CheckForNull VDP.IBitstreamListener getBitstreamListener() {
        return _bitstreamListener;
    }
    public void setBitstreamListener(@Nonnull VDP.IBitstreamListener bitstreamListener) {
        assertNull(_bitstreamListener);
        _bitstreamListener = bitstreamListener;
    }

    @CheckForNull
    private VDP.IMdecProducer _mdecProducer;
    public void setMdecProducer(@Nonnull VDP.IMdecProducer mdecProducer) {
        assertNull(_mdecProducer);
        _mdecProducer = mdecProducer;
    }

    @CheckForNull
    private VDP.IMdecListener _mdecListener;
    // Expose this publicly for the saver to send data into
    public @CheckForNull VDP.IMdecListener getMdecListener() {
        return _mdecListener;
    }
    public void setMdecListener(@Nonnull VDP.IMdecListener mdecListener) {
        assertNull(_mdecListener);
        _mdecListener = mdecListener;
    }

    @CheckForNull
    private VDP.IDecodedProducer _decodedProducer;
    public void setDecodedProducer(@Nonnull VDP.IDecodedProducer decodedProducer) {
        assertNull(_decodedProducer);
        _decodedProducer = decodedProducer;
    }

    @CheckForNull
    private VDP.IDecodedListener _decodedListener;
    public void setDecodedListener(@Nonnull VDP.IDecodedListener decodedListener) {
        assertNull(_decodedListener);
        _decodedListener = decodedListener;
    }

    @CheckForNull
    private VDP.IFileGenerator _fileGenerator;
    public void setFileGenerator(@Nonnull VDP.IFileGenerator publishesFiles) {
        assertNull(_fileGenerator);
        _fileGenerator = publishesFiles;
    }

    @CheckForNull
    private VDP.GeneratedFileListener _generatedFileListener;
    public void setFileListener(@Nonnull VDP.GeneratedFileListener generatedFileListener) {
        assertNull(_generatedFileListener);
        _generatedFileListener = generatedFileListener;
    }

    // Expose this publicly to open/close the video
    @CheckForNull
    private VDP.ToVideo _toVideo;
    public @CheckForNull VDP.ToVideo getVideo() {
        return _toVideo;
    }
    public void setVideo(@Nonnull VDP.ToVideo toVideo) {
        assertNull(_toVideo);
        _toVideo = toVideo;
    }

    // =========================================================================

    // compound setters
    public void setBitstream2Mdec(@Nonnull VDP.Bitstream2Mdec bs2mdec) {
        setMdecProducer(bs2mdec);
        setBitstreamListener(bs2mdec);
    }

    public <T extends VDP.IMdecListener & VDP.IDecodedProducer> void setMdec2Decoded(@Nonnull T m2d) {
        setMdecListener(m2d);
        setDecodedProducer(m2d);
    }
    public <T extends VDP.IDecodedListener & VDP.IFileGenerator> void setDecoded2File(@Nonnull T d2f) {
        setDecodedListener(d2f);
        setFileGenerator(d2f);
    }
    public <T extends VDP.IMdecListener & VDP.IFileGenerator> void setMdec2File(@Nonnull T m2f) {
        setMdecListener(m2f);
        setFileGenerator(m2f);
    }

    // =========================================================================

    public void autowire() throws IllegalStateException {
        if (_bitstreamListener != null) {
            if (_bsProducer != null)
                _bsProducer.setListener(_bitstreamListener);
            if (_frame2Bitstream != null)
                _frame2Bitstream.setListener(_bitstreamListener);
        }
        if (_mdecListener != null) {
            if (_mdecProducer != null)
                _mdecProducer.setListener(_mdecListener);
            if (_frame2Bitstream != null)
                _frame2Bitstream.setListener(_mdecListener);
        }
        if (_decodedListener != null && _decodedProducer != null)
            _decodedProducer.setDecodedListener(_decodedListener);
        if (_generatedFileListener != null && _fileGenerator != null)
            _fileGenerator.setGenFileListener(_generatedFileListener);

        wireSectorClaim();
        wireAudioAndFrame();
    }

    // =========================================================================
    // Sector claim, audio, video

    @CheckForNull
    private SectorClaimSystem _sectorClaimSystem;
    public void attachToSectorClaimSystem(@Nonnull SectorClaimSystem sectorClaimSystem) {
        assertNull(_sectorClaimSystem);
        _sectorClaimSystem = sectorClaimSystem;
    }

    private void wireSectorClaim() {
        if (_sectorClaimSystem == null)
            return;
        if (_sectorAudioDecoder != null)
            _sectorAudioDecoder.attachToSectorClaimer(_sectorClaimSystem);
        if (_sectorClaimToDemuxedFrame != null)
            _sectorClaimToDemuxedFrame.attachToSectorClaimer(_sectorClaimSystem);
    }

    @CheckForNull
    private ISectorAudioDecoder _sectorAudioDecoder;
    public void setAudioDecoder(@Nonnull ISectorAudioDecoder sectorAudioDecoder) {
        assertNull(_sectorAudioDecoder);
        _sectorAudioDecoder = sectorAudioDecoder;
    }
    @CheckForNull
    private ISectorClaimToDemuxedFrame _sectorClaimToDemuxedFrame;
    public void setSectorClaim2Frame(@Nonnull ISectorClaimToDemuxedFrame sectorClaimToDemuxedFrame) {
        assertNull(_sectorClaimToDemuxedFrame);
        _sectorClaimToDemuxedFrame = sectorClaimToDemuxedFrame;
    }

    @CheckForNull
    private IDemuxedFrame.Listener _frameListener;
    public void setFrameListener(@Nonnull IDemuxedFrame.Listener frameListener) {
        assertNull(_frameListener);
        _frameListener = frameListener;
    }

    @CheckForNull
    private DecodedAudioPacket.Listener _audioListener;
    public void setAudioPacketListener(@Nonnull DecodedAudioPacket.Listener audioListener) {
        assertNull(_audioListener);
        _audioListener = audioListener;
    }

    private void wireAudioAndFrame() {
        if (_sectorAudioDecoder != null && _audioListener != null)
            _sectorAudioDecoder.setAudioListener(_audioListener);
        if (_sectorClaimToDemuxedFrame != null && _frameListener != null)
            _sectorClaimToDemuxedFrame.setFrameListener(_frameListener);
    }

    // =========================================================================

    private static void assertNull(Object o) {
        if (o != null)
            throw new IllegalStateException(o.getClass() + " was already set");
    }

}
