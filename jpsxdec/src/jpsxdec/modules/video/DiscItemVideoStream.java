/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2023  Michael Sabin
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

package jpsxdec.modules.video;

import java.util.List;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.Dimensions;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.save.VideoSaverBuilder;
import jpsxdec.util.player.PlayController;

/** Represents all variations of PlayStation video streams. */
public abstract class DiscItemVideoStream extends DiscItem {

    @Nonnull
    private final Dimensions _dims;

    @Nonnull
    protected final IndexSectorFrameNumber.Format _indexSectorFrameNumberFormat;

    public DiscItemVideoStream(@Nonnull ICdSectorReader cd,
                               int iStartSector, int iEndSector,
                               @Nonnull Dimensions dim,
                               @Nonnull IndexSectorFrameNumber.Format frameNumberFormat)
    {
        super(cd, iStartSector, iEndSector);
        _dims = dim;
        _indexSectorFrameNumberFormat = frameNumberFormat;
    }

    public DiscItemVideoStream(@Nonnull ICdSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
        _dims = new Dimensions(fields);
        _indexSectorFrameNumberFormat = new IndexSectorFrameNumber.Format(fields);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        _dims.serialize(serial);
        _indexSectorFrameNumberFormat.serialize(serial);
        return serial;
    }

    @Override
    final public @Nonnull GeneralType getType() {
        return GeneralType.Video;
    }

    final public int getWidth() {
        return _dims.getWidth();
    }

    final public int getHeight() {
        return _dims.getHeight();
    }

    final public boolean shouldBeCropped() {
        return (getWidth()  % 16) != 0 ||
               (getHeight() % 16) != 0;
    }

    abstract public @Nonnull FrameNumber getStartFrame();
    abstract public @Nonnull FrameNumber getEndFrame();
    abstract public @Nonnull List<FrameNumber.Type> getFrameNumberTypes();

    final public int getFrameCount() {
        return _indexSectorFrameNumberFormat.getFrameCount();
    }


    /** Returns if the raw video frame data (the bitstream) can be identified
     * and decoded independent of any extra information.
     * Nearly all videos do have bitstreams that can be identified and decoded
     * all on their own (except for the frame dimensions usually).
     * A few games however use very different bitstream formats that, on their
     * own, would be impossible to decode. They need some additional
     * contextual information to do so.
     * The video saver uses this information to determine if the bitstream
     * format (.bs) can be used as an output format.
     * @see IDemuxedFrame#getCustomFrameMdecStream()
     */
    abstract public boolean hasIndependentBitstream();

    abstract public @Nonnull PlayController makePlayController();

    @Override
    abstract public @Nonnull VideoSaverBuilder makeSaverBuilder();
}
