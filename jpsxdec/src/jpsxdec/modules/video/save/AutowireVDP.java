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

import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.sharedaudio.DecodedAudioPacket;
import jpsxdec.modules.sharedaudio.ISectorAudioDecoder;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToDemuxedFrame;

/** Automatically connects all video pipeline and other components. */
public class AutowireVDP {

    // Expose these publicly for the saver to send data into
    @CheckForNull
    private VDP.IBitstreamListener _bitstreamListener;
    public @CheckForNull VDP.IBitstreamListener getBitstreamListener() {
        return _bitstreamListener;
    }
    @CheckForNull
    private VDP.IMdecListener _mdecListener;
    public @CheckForNull VDP.IMdecListener getMdecListener() {
        return _mdecListener;
    }

    // Expose this publicly to open/close the AVI
    @CheckForNull
    private VDP.ToAvi _toAvi;
    public @CheckForNull VDP.ToAvi getAvi() {
        return _toAvi;
    }


    // =========================================================================

    public void autowire() throws IllegalStateException {
        _bitstreamListener = chooseOnlyOne(_bitstream2File, _bitstream2Mdec);
        _mdecListener = chooseOnlyOne(_mdec2Decoded, _mdec2File, _mdec2Jpeg, _mdec2MjpegAvi);

        if (_frame2Bitstream != null) {
            _frame2Bitstream.setListener(_bitstreamListener);
            _frame2Bitstream.setListener(_mdecListener);
        }

        wireDecodedIntoMdec();
        wireMdecIntoBitstream();
        _toAvi = chooseOnlyOne(_decoded2JYuvAvi, _decoded2RgbAvi, _decoded2YuvAvi, _mdec2MjpegAvi);
        wireGenFileListener();
        wireSectorClaim();
        wireAudioAndFrame();
    }

    private void wireGenFileListener() {
        if (_generatedFileListener == null)
            return;
        if (_bitstream2File != null)
            _bitstream2File.setGenFileListener(_generatedFileListener);
        if (_decoded2JavaImage != null)
            _decoded2JavaImage.setGenFileListener(_generatedFileListener);
        if (_decoded2JYuvAvi != null)
            _decoded2JYuvAvi.setGenFileListener(_generatedFileListener);
        if (_decoded2RgbAvi != null)
            _decoded2RgbAvi.setGenFileListener(_generatedFileListener);
        if (_decoded2YuvAvi != null)
            _decoded2YuvAvi.setGenFileListener(_generatedFileListener);
        if (_mdec2File != null)
            _mdec2File.setGenFileListener(_generatedFileListener);
        if (_mdec2Jpeg != null)
            _mdec2Jpeg.setGenFileListener(_generatedFileListener);
        if (_mdec2MjpegAvi != null)
            _mdec2MjpegAvi.setGenFileListener(_generatedFileListener);
    }

    private void wireMdecIntoBitstream() {
        VDP.IMdecListener mdecListener = chooseOnlyOne(_mdec2Decoded, _mdec2File, _mdec2Jpeg, _mdec2MjpegAvi);
        if (_bitstream2Mdec == null)
            return;
        if (mdecListener != null)
            _bitstream2Mdec.setMdecListener(mdecListener);
    }

    private void wireDecodedIntoMdec() {
        if (_decodedListener == null)
            _decodedListener = chooseOnlyOne(_decoded2JYuvAvi, _decoded2JavaImage, _decoded2RgbAvi, _decoded2YuvAvi);
        if (_decodedListener == null)
            return;
        if (_mdec2Decoded != null)
            _mdec2Decoded.setDecoded(_decodedListener);
    }

    private static @CheckForNull <T> T chooseOnlyOne(T... elements) {
        T nonNull = null;
        for (T element : elements) {
            if (element != null) {
                if (nonNull != null)
                    throw new IllegalStateException("More than one set: " + Arrays.toString(elements));
                nonNull = element;
            }
        }
        return nonNull;
    }

    // =========================================================================
    // Sector claim, audio, video

    @CheckForNull
    private SectorClaimSystem _sectorClaimSystem;
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem sectorClaimSystem) {
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
    public void setMap(@Nonnull ISectorClaimToDemuxedFrame sectorClaimToDemuxedFrame) {
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
    private Frame2Bitstream _frame2Bitstream;
    public void setMap(@Nonnull Frame2Bitstream frame2Bitstream) {
        setFrameListener(frame2Bitstream);
        _frame2Bitstream = frame2Bitstream;
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

    @CheckForNull
    private VDP.GeneratedFileListener _generatedFileListener;
    public void setFileListener(@Nonnull VDP.GeneratedFileListener generatedFileListener) {
        assertNull(_generatedFileListener);
        _generatedFileListener = generatedFileListener;
    }

    // =========================================================================
    // Bitstream


    @CheckForNull
    private VDP.Bitstream2File _bitstream2File;
    public void setMap(@Nonnull VDP.Bitstream2File bitstream2File) {
        assertNull(_bitstream2File);
        _bitstream2File = bitstream2File;
    }

    @CheckForNull
    private VDP.Bitstream2Mdec _bitstream2Mdec;
    public void setMap(@Nonnull VDP.Bitstream2Mdec bitstream2Mdec) {
        assertNull(_bitstream2Mdec);
        _bitstream2Mdec = bitstream2Mdec;
    }

    // =========================================================================
    // decoded

    @CheckForNull
    private VDP.Decoded2JavaImage _decoded2JavaImage;
    public void setMap(@Nonnull VDP.Decoded2JavaImage decoded2JavaImage) {
        assertNull(_decoded2JavaImage);
        _decoded2JavaImage = decoded2JavaImage;
    }

    // =========================================================================
    // MDEC

    @CheckForNull
    private VDP.Mdec2Decoded _mdec2Decoded;
    public void setMap(@Nonnull VDP.Mdec2Decoded mdec2Decoded) {
        assertNull(_mdec2Decoded);
        _mdec2Decoded = mdec2Decoded;
    }

    @CheckForNull
    private VDP.Mdec2File _mdec2File;
    public void setMap(@Nonnull VDP.Mdec2File mdec2File) {
        assertNull(_mdec2File);
        _mdec2File = mdec2File;
    }

    @CheckForNull
    private VDP.Mdec2Jpeg _mdec2Jpeg;
    public void setMap(@Nonnull VDP.Mdec2Jpeg mdec2Jpeg) {
        assertNull(_mdec2Jpeg);
        _mdec2Jpeg = mdec2Jpeg;
    }

    // =========================================================================
    // AVI

    @CheckForNull
    private VDP.IDecodedListener _decodedListener;
    public void setDecodedListener(@Nonnull VDP.IDecodedListener decodedListener) {
        assertNull(_decodedListener);
        _decodedListener = decodedListener;
    }
    @CheckForNull
    private VDP.Decoded2JYuvAvi _decoded2JYuvAvi;
    public void setToAvi(@Nonnull VDP.Decoded2JYuvAvi decoded2JYuvAvi) {
        assertNull(_decoded2JYuvAvi);
        _decoded2JYuvAvi = decoded2JYuvAvi;
    }
    @CheckForNull
    private VDP.Decoded2RgbAvi _decoded2RgbAvi;
    public void setToAvi(@Nonnull VDP.Decoded2RgbAvi decoded2RgbAvi) {
        assertNull(_decoded2RgbAvi);
        _decoded2RgbAvi = decoded2RgbAvi;
    }
    @CheckForNull
    private VDP.Decoded2YuvAvi _decoded2YuvAvi;
    public void setToAvi(@Nonnull VDP.Decoded2YuvAvi decoded2YuvAvi) {
        assertNull(_decoded2YuvAvi);
        _decoded2YuvAvi = decoded2YuvAvi;
    }
    @CheckForNull
    private VDP.Mdec2MjpegAvi _mdec2MjpegAvi;
    public void setToAvi(@Nonnull VDP.Mdec2MjpegAvi mdec2MjpegAvi) {
        assertNull(_mdec2MjpegAvi);
        _mdec2MjpegAvi = mdec2MjpegAvi;
    }

    // =========================================================================

    private static void assertNull(Object o) {
        if (o != null)
            throw new IllegalStateException(o.getClass() + " was already set");
    }

}
