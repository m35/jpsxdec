/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2020  Michael Sabin
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

import java.io.PrintStream;
import java.util.List;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.DebugLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.ParsedMdecImage;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.Fraction;
import jpsxdec.util.player.PlayController;

/** Represents all variations of PlayStation video streams. */
public abstract class DiscItemVideoStream extends DiscItem {

    @Nonnull
    private final Dimensions _dims;

    @Nonnull
    protected final IndexSectorFrameNumber.Format _indexSectorFrameNumberFormat;

    public DiscItemVideoStream(@Nonnull CdFileSectorReader cd,
                               int iStartSector, int iEndSector,
                               @Nonnull Dimensions dim,
                               @Nonnull IndexSectorFrameNumber.Format frameNumberFormat)
    {
        super(cd, iStartSector, iEndSector);
        _dims = dim;
        _indexSectorFrameNumberFormat = frameNumberFormat;
    }

    public DiscItemVideoStream(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
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
        return _dims.shouldBeCropped();
    }

    abstract public @Nonnull FrameNumber getStartFrame();
    abstract public @Nonnull FrameNumber getEndFrame();
    abstract public @Nonnull List<FrameNumber.Type> getFrameNumberTypes();

    final public int getFrameCount() {
        return _indexSectorFrameNumberFormat.getFrameCount();
    }


    /** 1 for 1x (75 sectors/second)
     *  2 for 2x (150 sectors/second)
     *  {@code <= 0} if unknown. */
    abstract public int getDiscSpeed();

    abstract public @Nonnull Fraction getSectorsPerFrame();

    /** Returns the sector on the disc where the video should start playing. */
    abstract public int getAbsolutePresentationStartSector();

    /** Returns if the raw video frame data (the bitstream) can be identified
     * and decoded independent of any extra information.
     * Nearly all videos do have bitstreams that can be identified and decoded
     * all on their own (except for the frame dimensions usually).
     * A few games however use very different bitstream formats that, on their
     * own, would be impossible to decode. They need some additional
     * contextual information to do so.
     * The video saver uses this information to determine if the bitstream
     * format (.bs) can be used as an output format. */
    abstract public boolean hasIndependentBitstream();

    /** Returns the approximate duration of the video in seconds.
     *  Intended for use with video playback progress bar. */
    abstract public double getApproxDuration();

    abstract public @Nonnull PlayController makePlayController();

    /** Creates a demuxer that can handle frames in this video. */
    abstract public @Nonnull ISectorClaimToDemuxedFrame makeDemuxer();


    final public void frameInfoDump(@Nonnull final PrintStream ps, final boolean blnMore) {
        ISectorClaimToDemuxedFrame demuxer = makeDemuxer();
        demuxer.setFrameListener(new IDemuxedFrame.Listener() {
            @Override
            public void frameComplete(IDemuxedFrame frame) {
                MdecInputStream mis = frame.getCustomFrameMdecStream();
                try {

                    if (mis != null) {
                        ps.println(frame);

                        ps.println("Frame data info: " + mis);
                        if (blnMore) {
                            ParsedMdecImage parsed = new ParsedMdecImage(mis, getWidth(), getHeight());
                            parsed.drawMacroBlocks(ps);
                        }
                    } else {
                        try {
                            SectorBasedFrameAnalysis frameAnalysis = SectorBasedFrameAnalysis.create(frame);
                            frameAnalysis.printInfo(ps);
                            if (blnMore)
                                frameAnalysis.drawMacroBlocks(ps);
                        } catch (BinaryDataNotRecognized ex) {
                            ps.println("Frame not recognized");
                            ex.printStackTrace(ps);
                        }
                    }
                } catch (MdecException.EndOfStream ex) {
                    ex.printStackTrace(ps);
                } catch (MdecException.ReadCorruption ex) {
                    ex.printStackTrace(ps);
                }

                System.out.println("_____________________________________________________________________");
            }
        });

        System.out.println(this);
        SectorClaimSystem it = createClaimSystem();
        demuxer.attachToSectorClaimer(it);
        while (it.hasNext()) {
            try {
                IIdentifiedSector sector = it.next(DebugLogger.Log);
            } catch (CdFileSectorReader.CdReadException ex) {
                throw new RuntimeException("IO error with dev tool", ex);
            }
        }
        it.close(DebugLogger.Log);
    }

}
