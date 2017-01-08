/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2017  Michael Sabin
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

package jpsxdec.discitems;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.psxvideoencode.ReplaceFrames;
import jpsxdec.i18n.I;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.encode.ParsedMdecImage;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.DebugLogger;
import jpsxdec.util.DeserializationFail;
import jpsxdec.util.Fraction;
import jpsxdec.util.LoggedFailure;
import jpsxdec.util.ProgressLogger;
import jpsxdec.util.TaskCanceledException;
import jpsxdec.util.player.PlayController;

/** Represents all variations of PlayStation video streams. */
public abstract class DiscItemVideoStream extends DiscItem {
    
    private static final String DIMENSIONS_KEY = "Dimensions";
    /** Width of video in pixels. */
    private final int _iWidth;
    /** Height of video in pixels. */
    private final int _iHeight;

    private static final String FRAMECOUNT_KEY = "Frame Count";
    /** Number of frames. */
    private final int _iFrameCount;

    private static final String FORMAT_KEY = "Digits";
    @Nonnull
    private final FrameNumberFormat _frameNumberFormat;

    public DiscItemVideoStream(@Nonnull CdFileSectorReader cd,
                               int iStartSector, int iEndSector,
                               int iWidth, int iHeight,
                               int iFrameCount,
                               @Nonnull FrameNumberFormat frameNumberFormat)
    {
        super(cd, iStartSector, iEndSector);
        _iWidth = iWidth;
        _iHeight = iHeight;
        _iFrameCount = iFrameCount;
        _frameNumberFormat = frameNumberFormat;
    }
    
    public DiscItemVideoStream(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws DeserializationFail
    {
        super(cd, fields);
        
        int[] ai = fields.getDimensions(DIMENSIONS_KEY);
        _iWidth = ai[0];
        _iHeight = ai[1];
        
        _iFrameCount = fields.getInt(FRAMECOUNT_KEY);

        _frameNumberFormat = new FrameNumberFormat(fields.getString(FORMAT_KEY));
    }
    
    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        serial.addDimensions(DIMENSIONS_KEY, _iWidth, _iHeight);
        serial.addNumber(FRAMECOUNT_KEY, _iFrameCount);
        serial.addString(FORMAT_KEY, _frameNumberFormat.serialize());
        return serial;
    }
    
    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }
    
    abstract public @Nonnull FrameNumber getStartFrame();

    abstract public @Nonnull FrameNumber getEndFrame();

    public int getFrameCount() {
        return _iFrameCount;
    }

    final public @Nonnull FrameNumberFormat getFrameNumberFormat() {
        return _frameNumberFormat;
    }
    
    public boolean shouldBeCropped() {
        return (_iHeight % 16) != 0 ||
               (_iWidth  % 16) != 0;
    }

    @Override
    public @Nonnull GeneralType getType() {
        return GeneralType.Video;
    }

    /** 1 for 1x (75 sectors/second), 2 for 2x (150 sectors/second), or -1 if unknown. */
    abstract public int getDiscSpeed();
    
    abstract public @Nonnull PlayController makePlayController();

    /** Creates a demuxer that can handle frames in this video. */
    abstract public @Nonnull ISectorFrameDemuxer makeDemuxer();
    
    abstract public @Nonnull Fraction getSectorsPerFrame();
    
    abstract public int getPresentationStartSector();

    /** Returns the approximate duration of the video in seconds.
     *  Intended for use with video playback progress bar. */
    abstract public double getApproxDuration();


    public void frameInfoDump(@Nonnull final PrintStream ps, final boolean blnMore) {
        ISectorFrameDemuxer demuxer = makeDemuxer();
        demuxer.setFrameListener(new ISectorFrameDemuxer.ICompletedFrameListener() {
            public void frameComplete(IDemuxedFrame frame) {
                ps.println(frame);
                ps.println("  Available demux size: " + frame.getDemuxSize());
                frame.printSectors(ps); // ideally would be indented by 4
                
                byte[] abBitStream = frame.copyDemuxData(null);
                try {
                    BitStreamUncompressor uncompressor = BitStreamUncompressor.identifyUncompressor(abBitStream, frame.getDemuxSize());
                    ParsedMdecImage parsed = new ParsedMdecImage(uncompressor, getWidth(), getHeight());
                    uncompressor.skipPaddingBits();
                    ps.println("  Bitstream info: " + uncompressor);
                    if (blnMore) {
                        int iMbWidth  = Calc.macroblockDim(_iWidth),
                            iMbHeight = Calc.macroblockDim(_iHeight);
                        for (int iMbY = 0; iMbY < iMbHeight; iMbY++) {
                            for (int iMbX = 0; iMbX < iMbWidth; iMbX++) {
                                ps.println("    " +iMbX + ", " + iMbY);
                                for (int iBlk = 0; iBlk < 6; iBlk++) {
                                    ps.println("      "+parsed.getBlockInfo(iMbX, iMbY, iBlk));
                                }
                            }
                        }
                    }
                } catch (BinaryDataNotRecognized ex) {
                    ps.println("  Frame not recognized");
                } catch (Exception ex) {
                    ex.printStackTrace(ps);
                }
            }
        });

        try {
            IdentifiedSectorIterator it = identifiedSectorIterator();
            while (it.hasNext()) {
                try {
                    IdentifiedSector sector = it.next();
                    if (sector != null) {
                        demuxer.feedSector(sector, DebugLogger.Log);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException("IO error with dev tool", ex);
                }
            }
            demuxer.flush(DebugLogger.Log);
        } catch (LoggedFailure ex) {
            throw new RuntimeException("Should not happen", ex);
        }
    }

    public void replaceFrames(@Nonnull ProgressLogger pl, @Nonnull String sXmlFile)
            throws LoggedFailure, TaskCanceledException
    {
        ReplaceFrames replacers;
        try {
            replacers = new ReplaceFrames(sXmlFile);
        } catch (FileNotFoundException ex) {
            throw new LoggedFailure(pl, Level.SEVERE,
                                    I.IO_OPENING_FILE_NOT_FOUND_NAME(sXmlFile), ex);
        } catch (IOException ex) {
            throw new LoggedFailure(pl, Level.SEVERE,
                                    I.IO_READING_FILE_ERROR_NAME(sXmlFile), ex);
        } catch (DeserializationFail ex) {
            throw new LoggedFailure(pl, Level.SEVERE,
                                    ex.getSourceMessage(), ex);
        }
        replacers.replaceFrames(this, getSourceCd(), pl);
    }
}
