/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2014  Michael Sabin
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

import java.io.IOException;
import jpsxdec.discitems.psxvideoencode.ReplaceFrames;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.encode.ParsedMdecImage;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.ConsoleProgressListenerLogger;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.player.PlayController;

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
    private final FrameNumberFormat _frameNumberFormat;

    public DiscItemVideoStream(int iStartSector, int iEndSector, 
                               int iWidth, int iHeight,
                               int iFrameCount,
                               FrameNumberFormat frameNumberFormat)
    {
        super(iStartSector, iEndSector);
        _iWidth = iWidth;
        _iHeight = iHeight;
        _iFrameCount = iFrameCount;
        _frameNumberFormat = frameNumberFormat;
    }
    
    public DiscItemVideoStream(SerializedDiscItem fields) throws NotThisTypeException {
        super(fields);
        
        int[] ai = fields.getDimensions(DIMENSIONS_KEY);
        _iWidth = ai[0];
        _iHeight = ai[1];
        
        _iFrameCount = fields.getInt(FRAMECOUNT_KEY);

        _frameNumberFormat = new FrameNumberFormat(fields.getString(FORMAT_KEY));
    }
    
    @Override
    public SerializedDiscItem serialize() {
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
    
    abstract public FrameNumber getStartFrame();

    abstract public FrameNumber getEndFrame();

    public int getFrameCount() {
        return _iFrameCount;
    }

    final public FrameNumberFormat getFrameNumberFormat() {
        return _frameNumberFormat;
    }
    
    public boolean shouldBeCropped() {
        return (_iHeight % 16) != 0 ||
               (_iWidth  % 16) != 0;
    }

    @Override
    public GeneralType getType() {
        return GeneralType.Video;
    }

    /** 1 for 1x (75 sectors/second), 2 for 2x (150 sectors/second), or -1 if unknown. */
    abstract public int getDiscSpeed();
    
    abstract public PlayController makePlayController();

    /** Creates a demuxer that can handle frames in this video. */
    abstract public ISectorFrameDemuxer makeDemuxer();
    
    abstract public Fraction getSectorsPerFrame();
    
    abstract public int getPresentationStartSector();

    /** Returns the approximate duration of the video in seconds.
     *  Intended for use with video playback progress bar. */
    abstract public double getApproxDuration();


    public void frameInfoDump(final FeedbackStream fbs) throws IOException {
        DiscItemVideoStream vidItem = this;

        ISectorFrameDemuxer demuxer = makeDemuxer();
        demuxer.setFrameListener(new ISectorFrameDemuxer.ICompletedFrameListener() {
            public void frameComplete(IDemuxedFrame frame) throws IOException {
                try {
                    byte[] abBitStream = frame.copyDemuxData(null);
                    BitStreamUncompressor uncompressor = BitStreamUncompressor.identifyUncompressor(abBitStream);
                    uncompressor.reset(abBitStream, frame.getDemuxSize());
                    ParsedMdecImage parsed = new ParsedMdecImage(getWidth(), getHeight());
                    parsed.readFrom(uncompressor);
                    uncompressor.skipPaddingBits();
                    fbs.println(frame);
                    fbs.indent();
                    try {
                        fbs.println("Bitstream info: " + uncompressor);
                        fbs.println("Available demux size: " + frame.getDemuxSize());
                        fbs.indent();
                        try {
                            frame.printSectors(fbs);
                            if (fbs.printMore()) {
                                int iMbWidth  = Calc.macroblockDim(_iWidth),
                                    iMbHeight = Calc.macroblockDim(_iHeight);
                                for (int iMbY = 0; iMbY < iMbHeight; iMbY++) {
                                    for (int iMbX = 0; iMbX < iMbWidth; iMbX++) {
                                        fbs.println(iMbX + ", " + iMbY);
                                        fbs.indent();
                                        try {
                                            for (int iBlk = 0; iBlk < 6; iBlk++) {
                                                fbs.println(parsed.getBlockInfo(iMbX, iMbY, iBlk));
                                            }
                                        } finally {
                                            fbs.outdent();
                                        }
                                    }
                                }
                            }
                        } finally {
                            fbs.outdent();
                        }
                    } finally {
                        fbs.outdent();
                    }
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        ConsoleProgressListenerLogger log = new ConsoleProgressListenerLogger("frameInfoDump", fbs);
        try {
            for (int iSector = 0;
                iSector < vidItem.getSectorLength();
                iSector++)
            {
                try {
                    IdentifiedSector sector = vidItem.getRelativeIdentifiedSector(iSector);
                    if (sector != null) {
                        demuxer.feedSector(sector, log);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            demuxer.flush(log);
        } finally {
            log.close();
        }
    }

    public void replaceFrames(FeedbackStream Feedback, String sXmlFile) 
            throws IOException, NotThisTypeException, MdecException
    {
        ReplaceFrames replacers = new ReplaceFrames(sXmlFile);
        replacers.replaceFrames(this, getSourceCd(), Feedback);
    }
}
