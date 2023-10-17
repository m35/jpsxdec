/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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
import jpsxdec.modules.audio.DecodedAudioPacket;
import jpsxdec.modules.audio.ISectorClaimToDecodedAudio;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToFrameAndAudio;

/** Automatically connects all video pipeline and other components. */
public class AutowireVDP {

    private boolean _blnWired = false;

    /** This is also a {@link VDP.IMdecProducer}, see its javadoc for why. */
    @CheckForNull
    private Frame2BitstreamOrMdec _frame2BitstreamOrMdec;
    public void setFrame2BitstreamOrMdec(@Nonnull Frame2BitstreamOrMdec frame2BitstreamOrMdec) {
        assertNull(_frame2BitstreamOrMdec);
        assertNull(_frameListener);
        assertNull(_bsProducer);
        _frame2BitstreamOrMdec = frame2BitstreamOrMdec;
    }

    @CheckForNull
    private VDP.IBitstreamProducer _bsProducer;
    public void setBitstreamProducer(@Nonnull VDP.IBitstreamProducer publishesBs) {
        assertNull(_bsProducer);
        assertNull(_frame2BitstreamOrMdec);
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
        if (_blnWired)
            throw new IllegalStateException("Already wired");
        _blnWired = true;

        if (_sectorClaimSystem != null) {
            if (_sectorClaimToDecodedAudio != null)
                _sectorClaimToDecodedAudio.attachToSectorClaimer(_sectorClaimSystem);
            else if (_sectorClaimToFrameAndAudio != null)
                _sectorClaimToFrameAndAudio.attachToSectorClaimer(_sectorClaimSystem);
        }

        if (_sectorClaimToDecodedAudio != null && _audioListener != null)
            _sectorClaimToDecodedAudio.setAudioListener(_audioListener);

        if (_sectorClaimToFrameAndAudio != null) {
            if (_audioListener != null)
                _sectorClaimToFrameAndAudio.setAudioListener(_audioListener);

            if (_frameListener != null)
                _sectorClaimToFrameAndAudio.setFrameListener(_frameListener);
            else if (_frame2BitstreamOrMdec != null)
                _sectorClaimToFrameAndAudio.setFrameListener(_frame2BitstreamOrMdec);
        }

        if (_bitstreamListener != null) {
            if (_bsProducer != null)
                _bsProducer.setListener(_bitstreamListener);
            else if (_frame2BitstreamOrMdec != null)
                _frame2BitstreamOrMdec.setListener(_bitstreamListener);
        }

        if (_mdecListener != null) {
            if (_mdecProducer != null)
                _mdecProducer.setListener(_mdecListener);
            if (_frame2BitstreamOrMdec != null)
                _frame2BitstreamOrMdec.setListener(_mdecListener);
        }

        if (_decodedListener != null && _decodedProducer != null)
            _decodedProducer.setDecodedListener(_decodedListener);

        if (_generatedFileListener != null && _fileGenerator != null)
            _fileGenerator.setGenFileListener(_generatedFileListener);
    }

    // =========================================================================
    // Sector claim, audio, video

    @CheckForNull
    private SectorClaimSystem _sectorClaimSystem;
    public void setSectorClaimSystem(@Nonnull SectorClaimSystem sectorClaimSystem) {
        assertNull(_sectorClaimSystem);
        _sectorClaimSystem = sectorClaimSystem;
    }
    public @CheckForNull SectorClaimSystem getSectorClaimSystem() {
        return _sectorClaimSystem;
    }

    @CheckForNull
    private ISectorClaimToDecodedAudio _sectorClaimToDecodedAudio;
    public void setSectorClaim2DecodedAudio(@Nonnull ISectorClaimToDecodedAudio sectorClaimToDecodedAudio) {
        assertNull(_sectorClaimToDecodedAudio);
        assertNull(_sectorClaimToFrameAndAudio);
        _sectorClaimToDecodedAudio = sectorClaimToDecodedAudio;
    }

    @CheckForNull
    private ISectorClaimToFrameAndAudio _sectorClaimToFrameAndAudio;
    public void setSectorClaim2FrameAndAudio(@Nonnull ISectorClaimToFrameAndAudio sectorClaimToFrameAndAudio) {
        assertNull(_sectorClaimToFrameAndAudio);
        assertNull(_sectorClaimToDecodedAudio);
        _sectorClaimToFrameAndAudio = sectorClaimToFrameAndAudio;
    }

    @CheckForNull
    private IDemuxedFrame.Listener _frameListener;
    public void setFrameListener(@Nonnull IDemuxedFrame.Listener frameListener) {
        assertNull(_frameListener);
        assertNull(_frame2BitstreamOrMdec);
        _frameListener = frameListener;
    }

    @CheckForNull
    private DecodedAudioPacket.Listener _audioListener;
    public void setAudioPacketListener(@Nonnull DecodedAudioPacket.Listener audioListener) {
        assertNull(_audioListener);
        _audioListener = audioListener;
    }

    // =========================================================================

    private static void assertNull(Object o) {
        if (o != null)
            throw new IllegalStateException(o.getClass() + " was already set");
    }

}
