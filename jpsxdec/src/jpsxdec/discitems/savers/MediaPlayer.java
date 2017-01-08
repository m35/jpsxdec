/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

package jpsxdec.discitems.savers;

import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.ISectorAudioDecoder;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_int;
import jpsxdec.psxvideo.mdec.idct.SimpleIDCT;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.util.DebugLogger;
import jpsxdec.util.LoggedFailure;
import jpsxdec.util.player.AudioVideoReader;
import jpsxdec.util.player.IDecodableFrame;
import jpsxdec.util.player.ObjectPool;

/** Holds all the class implementations that the {@link jpsxdec.util.player} 
 * framework needs to playback PlayStation audio and/or video. */
public class MediaPlayer extends AudioVideoReader implements ISectorFrameDemuxer.ICompletedFrameListener {

    private static final Logger LOG = Logger.getLogger(MediaPlayer.class.getName());

    private static final boolean DEBUG = false;

    private final int _iMovieStartSector;
    private final int _iMovieEndSector;
    @Nonnull
    private final CdFileSectorReader _cdReader;
    private final double _dblDuration;

    //----------------------------------------------------------

    @CheckForNull
    private final DiscItemVideoStream _vid;
    private int _iSectorsPerSecond;
    @CheckForNull
    private final VDP.Bitstream2Mdec _b2m;
    @CheckForNull
    private final VDP.Mdec2Decoded _m2d;
    @CheckForNull
    private final ISectorFrameDemuxer _demuxer;

    public MediaPlayer(@Nonnull DiscItemVideoStream vid, @Nonnull ISectorFrameDemuxer demuxer) {
        this(vid, demuxer, vid.getStartSector(), vid.getEndSector());
    }


    public MediaPlayer(@Nonnull DiscItemVideoStream vid, @Nonnull ISectorFrameDemuxer demuxer,
                       int iSectorStart, int iSectorEnd)
    {
        _cdReader = vid.getSourceCd();
        _iMovieStartSector = iSectorStart;
        _iMovieEndSector = iSectorEnd;
        if (vid.getDiscSpeed() == 1) {
            _iSectorsPerSecond = 75;
        } else {
            // if disc speed is unknown, assume 2x
            _iSectorsPerSecond = 150;
        }
        _dblDuration = vid.getApproxDuration();

        _vid = vid;
        _m2d = new VDP.Mdec2Decoded(new MdecDecoder_int(new SimpleIDCT(),
                                                        vid.getWidth(),
                                                        vid.getHeight()), 
                                    DebugLogger.Log);
        _b2m = new VDP.Bitstream2Mdec(_m2d);
        _demuxer = demuxer;
        _demuxer.setFrameListener(this);
    }

    //-----------------------------------------------------------------------

    @CheckForNull
    private ISectorAudioDecoder _audioDecoder;
    @CheckForNull
    private AudioPlayerSectorTimedWriter _audioOut;

    public MediaPlayer(@Nonnull DiscItemAudioStream aud) {
        _cdReader = aud.getSourceCd();
        _iMovieStartSector = aud.getStartSector();
        _iMovieEndSector = aud.getEndSector();
        if (aud.getDiscSpeed() == 1) {
            _iSectorsPerSecond = 75;
        } else {
            // if disc speed is unknown, assume 2x
            _iSectorsPerSecond = 150;
        }
        _dblDuration = aud.getApproxDuration();

        _audioDecoder = aud.makeDecoder(1.0);
        _audioOut = new AudioPlayerSectorTimedWriter(this, _iMovieStartSector, _iSectorsPerSecond, _audioDecoder.getSamplesPerSecond());
        _audioDecoder.setAudioListener(_audioOut);


        // ignore video
        _vid = null;
        _b2m = null;
        _m2d = null;
        _demuxer = null;
    }
    
    //----------------------------------------------------------

    public MediaPlayer(@Nonnull DiscItemVideoStream vid, 
                       @Nonnull ISectorFrameDemuxer demuxer,
                       @Nonnull ISectorAudioDecoder audio,
                       int iSectorStart, int iSectorEnd)
    {
        // do the video init
        this(vid, demuxer, iSectorStart, iSectorEnd);

        if (audio.getDiscSpeed() == 1) {
            _iSectorsPerSecond = 75;
        } else {
            // if disc speed is unknown, assume 2x
            _iSectorsPerSecond = 150;
        }

        // manually init the audio
        _audioDecoder = audio;
        _audioOut = new AudioPlayerSectorTimedWriter(this, _iMovieStartSector, _iSectorsPerSecond, _audioDecoder.getSamplesPerSecond());
        _audioDecoder.setAudioListener(_audioOut);
    }

    public void demuxThread() {

        try {

            final int iSectorLength = _iMovieEndSector - _iMovieStartSector + 1;

            IdentifiedSectorIterator it = IdentifiedSectorIterator.create(_cdReader, _iMovieStartSector, _iMovieEndSector);
            for (int iSector = 0; it.hasNext() && stillPlaying(); iSector++)
            {
                IdentifiedSector identifiedSector = it.next();
                if (identifiedSector != null) {
                    if (_demuxer != null) {
                        _demuxer.feedSector(identifiedSector, DebugLogger.Log);
                    }
                    // if frame demuxer and audio decoder are the same object
                    // don't feed the sector twice (this is currently only for
                    // Crusader movies)
                    if (_audioDecoder != null && _audioDecoder != _demuxer) {
                        _audioDecoder.feedSector(identifiedSector, DebugLogger.Log);
                    }
                }
                
                setReadProgress(iSector*100 / iSectorLength);

            }

            if (_demuxer != null)
                _demuxer.flush(DebugLogger.Log);

        } catch (LoggedFailure ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void frameComplete(@Nonnull IDemuxedFrame frame) {
        StrFrame strFrame = _framePool.borrow();
        strFrame.init(frame.getDemuxSize(), frame.getFrame(), frame.getPresentationSector() - _iMovieStartSector);
        strFrame.__abDemuxBuf = frame.copyDemuxData(strFrame.__abDemuxBuf);
        writeFrame(strFrame);
    }

    // #########################################################################
    // #########################################################################

    public @CheckForNull AudioFormat getAudioFormat() {
        if (_audioDecoder == null)
            return null;
        return _audioDecoder.getOutputFormat();
    }


    // #########################################################################
    // #########################################################################

    private class DecodableFramePool extends ObjectPool<StrFrame> {

        @Override
        protected StrFrame createNewObject() {
            if (DEBUG) System.err.println("Creating new pool object.");
            return new StrFrame();
        }

    }
    private final DecodableFramePool _framePool = new DecodableFramePool();


    public boolean hasVideo() {
        return _vid != null;
    }

    public int getVideoWidth() {
        if (_vid == null)
            throw new UnsupportedOperationException("Accessing video dimension for audio only player");
        return _vid.getWidth();
    }

    public int getVideoHeight() {
        if (_vid == null)
            throw new UnsupportedOperationException("Accessing video dimension for audio only player");
        return _vid.getHeight();
    }

    public double getDuration() {
        return _dblDuration;
    }

    private class StrFrame implements IDecodableFrame, VDP.IDecodedListener {

        @CheckForNull
        public byte[] __abDemuxBuf;
        @CheckForNull
        private FrameNumber __frameNum;
        private int __iSectorFromStart;
        @CheckForNull
        private int[] __aiDrawHere;
        
        public void init(int iSize, @Nonnull FrameNumber frameNum, int iSectorFromStart) {
            if (__abDemuxBuf == null || __abDemuxBuf.length < iSize)
                __abDemuxBuf = new byte[iSize];
            __iSectorFromStart = iSectorFromStart;
            __frameNum = frameNum;
        }

        public long getPresentationTime() {
            return (__iSectorFromStart * 1000000000L / _iSectorsPerSecond);
        }

        public void decodeVideo(@Nonnull int[] drawHere) {
            // _md2 and _b2m should != null when processing frames
            // if not, bad stuff should happen
            _m2d.setDecoded(this);
            __aiDrawHere = drawHere;
            try {
                // this will call _m2d which in turn will call decoded()
                // __abDemuxBuf and __frameNum should have been initialied in init()
                _b2m.bitstream(__abDemuxBuf, __abDemuxBuf.length, __frameNum, _iMovieEndSector);
            } catch (LoggedFailure ex) {
                System.err.print("Frame "+__frameNum+' '+ex.getMessage());
                if (ex.getCause() != null && ex.getCause().getMessage() != null)
                    System.err.println(": " + ex.getCause().getMessage());
                else
                    System.err.println();
            } finally {
                _m2d.setDecoded(null);
                __aiDrawHere = null;
            }
        }

        public void assertAcceptsDecoded(@Nonnull MdecDecoder decoder) {}

        public void decoded(@Nonnull MdecDecoder decoder,
                            @CheckForNull FrameNumber frameNumber,
                            int iFrameEndSector)
        {
            decoder.readDecodedRgb(getVideoWidth(), getVideoHeight(), __aiDrawHere);
        }

        public void error(@Nonnull ILocalizedMessage errMsg,
                          @CheckForNull FrameNumber frameNumber,
                          int iFrameEndSector)
        {
            System.err.println(errMsg.getEnglishMessage());
        }

        public void returnToPool() {
            if (DEBUG) System.err.println("Returning object to pool.");
            _framePool.giveBack(this);
        }

    }

}
