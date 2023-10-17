/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

import java.io.PrintStream;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.psxvideo.bitstreams.IBitStreamUncompressor;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.BinaryDataNotRecognized;

/** Analyzes a sector-based frame and calculates frequently used values. */
public class SectorBasedFrameAnalysis extends BitStreamAnalysis {

    public static @Nonnull SectorBasedFrameAnalysis create(@Nonnull ISectorBasedDemuxedFrame frame)
            throws BinaryDataNotRecognized, MdecException.ReadCorruption, MdecException.EndOfStream
    {
        byte[] abBitStream = frame.copyDemuxData();
        assert abBitStream.length == frame.getDemuxSize();

        IBitStreamUncompressor mis = frame.getCustomFrameMdecStream();
        if (mis == null)
            return new SectorBasedFrameAnalysis(abBitStream, frame);
        else
            return new SectorBasedFrameAnalysis(abBitStream, mis, frame);
    }

    @Nonnull
    private final ISectorBasedDemuxedFrame _frame;

    protected SectorBasedFrameAnalysis(@Nonnull byte[] abBitstream, @Nonnull ISectorBasedDemuxedFrame frame)
            throws BinaryDataNotRecognized, MdecException.ReadCorruption, MdecException.EndOfStream
    {
        super(abBitstream, frame.getWidth(), frame.getHeight());
        _frame = frame;
    }

    protected SectorBasedFrameAnalysis(@Nonnull byte[] abBitstream, @Nonnull IBitStreamUncompressor uncompressor, @Nonnull ISectorBasedDemuxedFrame frame)
            throws BinaryDataNotRecognized, MdecException.ReadCorruption, MdecException.EndOfStream
    {
        super(abBitstream, uncompressor, frame.getWidth(), frame.getHeight());
        _frame = frame;
    }

    @Override
    public void printInfo(@Nonnull PrintStream ps) {
        ps.println(_frame);
        ps.println("Available demux size: " + _frame.getDemuxSize());
        _frame.printSectors(ps);
        super.printInfo(ps);
    }
}
