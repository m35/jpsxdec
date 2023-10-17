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

import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.mdec.MdecInputStream;

/**
 * Because some video formats have context dependent bitstreams, or in other words,
 * the bitstream frames cannot be decoded independently, we need 2 possible publishers (producers).
 * @see DiscItemVideoStream#hasIndependentBitstream()
 * @see IDemuxedFrame#getCustomFrameMdecStream()
 */
public class Frame2BitstreamOrMdec implements IDemuxedFrame.Listener, VDP.IMdecProducer, VDP.IBitstreamProducer {

    @Nonnull
    private final FrameNumber.Type _frameNumberType;
    @Nonnull
    private final ILocalizedLogger _log;

    @CheckForNull
    private VDP.IBitstreamListener _bitstreamListener;

    @CheckForNull
    private VDP.IMdecListener _mdecListener;

    public Frame2BitstreamOrMdec(@Nonnull FrameNumber.Type frameNumberType, @Nonnull ILocalizedLogger log) {
        _frameNumberType = frameNumberType;
        _log = log;
    }

    @Override
    final public void setListener(@CheckForNull VDP.IBitstreamListener bitstreamListener) {
        _bitstreamListener = bitstreamListener;
    }

    @Override
    final public void setListener(@CheckForNull VDP.IMdecListener mdecListener) {
        _mdecListener = mdecListener;
    }

    final public @Nonnull FrameNumber.Type getFrameNumberType() {
        return _frameNumberType;
    }

    @Override
    public void frameComplete(@Nonnull IDemuxedFrame frame) throws LoggedFailure {
        FormattedFrameNumber ffn = frame.getFrame().getNumber(_frameNumberType);
        MdecInputStream customStream = frame.getCustomFrameMdecStream();

        try {
            if (customStream != null) {
                if (_mdecListener == null)
                    throw new IllegalStateException("No mdec to write to");
                _mdecListener.mdec(customStream, ffn, frame.getPresentationSector());
            } else {
                if (_bitstreamListener == null)
                    throw new IllegalStateException("No bitstream to write to");
                byte[] abBitstream = frame.copyDemuxData();
                _bitstreamListener.bitstream(abBitstream, frame.getDemuxSize(), ffn, frame.getPresentationSector());
            }
        } catch (IOWritingException ex) {
            throw new LoggedFailure(_log, Level.SEVERE,
                        I.IO_WRITING_TO_FILE_ERROR_NAME(ex.getOutFile().toString()), ex);
        }
    }
}
