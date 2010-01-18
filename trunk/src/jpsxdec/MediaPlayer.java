/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.player.AudioProcessor;
import jpsxdec.player.IAudioDecoder;
import jpsxdec.player.IAudioVideoReader;
import jpsxdec.player.IDecodableAudioChunk;
import jpsxdec.player.IDecodableFrame;
import jpsxdec.player.IVideoDecoder;
import jpsxdec.player.VideoProcessor;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.psx.str.DiscItemSTRVideo;
import jpsxdec.plugins.psx.str.IVideoSector;
import jpsxdec.plugins.psx.str.StrFramePushDemuxer;
import jpsxdec.plugins.psx.video.DemuxImage;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;
import jpsxdec.plugins.psx.video.decode.UncompressionException;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder_int;
import jpsxdec.plugins.psx.video.mdec.idct.simple_idct;
import jpsxdec.plugins.xa.IDiscItemAudioSectorDecoder;
import jpsxdec.plugins.xa.IDiscItemAudioStream;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.SourceDataLineAudioOutputStream;

/** Holds all the class implementations that the jpsxdec.player framework
 *  needs to playback PlayStation audio and/or video. */
public class MediaPlayer {
    
    public static class StrReader implements IAudioVideoReader {

        private final DiscItem _mediaItem;

        private final int _iStartSector;
        private final int _iEndSector;
        private int _iFrame;
        private StrFramePushDemuxer _demuxer;
        private int _iSector;

        public StrReader(IDiscItemAudioStream aud)
                throws FileNotFoundException, UnsupportedAudioFileException,
                       IOException
        {
            _mediaItem = (DiscItem) aud;
            _iSector = _iStartSector = aud.getStartSector();
            _iEndSector = aud.getEndSector();

            _iFrame = -1;

            _demuxer = new StrFramePushDemuxer(_iFrame);
        }

        public StrReader(DiscItemSTRVideo vid) 
                throws UnsupportedAudioFileException, IOException
        {
            this(vid, vid.getStartSector(), vid.getEndSector());
        }
        
        public StrReader(DiscItemSTRVideo vid, int iSectorStart, int iSectorEnd) 
                throws UnsupportedAudioFileException, IOException
        {
            _mediaItem = vid;
            _iSector = _iStartSector = iSectorStart;
            _iEndSector = iSectorEnd;

            _iFrame = vid.getStartFrame();

            _demuxer = new StrFramePushDemuxer(_iFrame);
        }

        /** Adds a video sector to a frame demuxer. It turns out to be more
         * complicated than you'd think. */
        private static StrFramePushDemuxer addToDemux(VideoProcessor vidProc,
                                                      StrFramePushDemuxer demuxer,
                                                      IVideoSector vidSector,
                                                      int iSectorsFromStart)
                throws IOException
        {
            if (demuxer == null) {
                // create the demuxer for the sector's frame
                demuxer = new StrFramePushDemuxer(vidSector.getFrameNumber());
            }
            if (demuxer.getFrameNumber() == vidSector.getFrameNumber()) {
                // add the sector if it is the same frame number
                demuxer.addChunk(vidSector);
            } else {
                // if sector has a different frame number, close off the demuxer
                DemuxImage demuxFrame = demuxer.getDemuxFrame();
                // create a new one with this new sector
                demuxer = new StrFramePushDemuxer();
                demuxer.addChunk(vidSector);
                // and send the finished frame thru the pipe
                // (wanted to wait in case of an error)
                vidProc.addFrame(new StrFrame(demuxFrame, iSectorsFromStart));
            }
            if (demuxer.isFull()) {
                // send the image thru the pipe if it is complete
                DemuxImage demuxFrame = demuxer.getDemuxFrame();
                demuxer = null;
                vidProc.addFrame(new StrFrame(demuxFrame, iSectorsFromStart));
            }
            return demuxer;
        }

        public boolean readNext(VideoProcessor vidProc, AudioProcessor audProc) {

            try {

                if (!(_iSector <= _iEndSector))
                    return false;

                CDSector cdSector = _mediaItem.getSourceCD().getSector(_iSector);
                IdentifiedSector identifiedSector = JPSXPlugin.identifyPluginSector(cdSector);
                if (vidProc != null && identifiedSector instanceof IVideoSector) {
                    if (_mediaItem.getStartSector() <= _iSector &&
                        _iSector <= _mediaItem.getEndSector())
                    {
                        IVideoSector vidSector = (IVideoSector) identifiedSector;
                        _demuxer = addToDemux(vidProc, _demuxer, vidSector, _iSector - _iStartSector);
                        _iFrame = vidSector.getFrameNumber();
                    }
                } else if (audProc != null && identifiedSector != null) {
                    audProc.addDecodableAudioChunk(new XAAudioChunk(identifiedSector));
                }
                _iSector++;
                return true;

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public void seekToTime(long lngTime) {
            throw new RuntimeException();
        }

        public void reset() {
            throw new RuntimeException();
        }

        public void seekToFrame(int iFrame) {
            throw new RuntimeException();
        }

    }

    public static class XAAudioChunk implements IDecodableAudioChunk {

        private IdentifiedSector _sector;

        public XAAudioChunk(IdentifiedSector sector) {
            _sector = sector;
        }

    }

    public static class StrFrame implements IDecodableFrame {

        private final DemuxImage _demux;
        private int _iSectorsFromStart;

        public StrFrame(DemuxImage imgFile, int iSectorsFromStart) throws IOException {
            _demux = imgFile;
            _iSectorsFromStart = iSectorsFromStart;
        }
        public int getWidth() {
            return _demux.getWidth();
        }

        public int getHeight() {
            return _demux.getHeight();
        }

        public int getFrameNumber() {
            return _demux.getFrameNumber();
        }

        public long getPresentationTime() {
            return (long)(_iSectorsFromStart * 1000 / 150);
        }

    }

    public static class XAAudioDecoder implements IAudioDecoder {

        private IDiscItemAudioStream _audioDiscItem;
        private IDiscItemAudioSectorDecoder _audioDecoder;
        private final static boolean BIGENDIAN = true;

        public XAAudioDecoder(IDiscItemAudioStream audStream) {
            _audioDiscItem = audStream;
        }

        public void initialize(SourceDataLine dataLine) {
            _audioDecoder = _audioDiscItem.makeDecoder(
                    new SourceDataLineAudioOutputStream(dataLine), BIGENDIAN, 1.0);
        }

        public void decodeAudio(IDecodableAudioChunk audioChunk, SourceDataLine dataLine) 
                throws IOException
        {
            if (audioChunk instanceof XAAudioChunk) {
                _audioDecoder.feedSector(((XAAudioChunk)audioChunk)._sector);
            }
        }

        public void reset() {
            _audioDecoder.reset();
        }

        public AudioFormat getAudioFormat() {
            return _audioDiscItem.getAudioFormat(BIGENDIAN);
        }


    }

    public static class StrVideoDecoder implements IVideoDecoder {

        private final DiscItemSTRVideo _sourceVidItem;
        private final MdecDecoder_int _decoder;
        private final RgbIntImage _rgb;
        private DemuxFrameUncompressor _uncompressor;

        public StrVideoDecoder(DiscItemSTRVideo sourceVidItem) {
            _sourceVidItem = sourceVidItem;
            _decoder = new MdecDecoder_int(new simple_idct(),
                                           _sourceVidItem.getWidth(),
                                           _sourceVidItem.getHeight());
            _rgb = new RgbIntImage(_sourceVidItem.getWidth(),
                                   _sourceVidItem.getHeight());
        }

        protected DemuxFrameUncompressor getUncompressor(DemuxImage demux) {
            if (_uncompressor == null) {
                _uncompressor = JPSXPlugin.identifyUncompressor(demux);
                if (_uncompressor == null) {
                    return null;
                }
            }
            try {
                _uncompressor.reset(demux.getData());
            } catch (NotThisTypeException ex) {
                _uncompressor = JPSXPlugin.identifyUncompressor(demux);
                if (_uncompressor == null) {
                    return null;
                }
            }
            return _uncompressor;
        }


        public void reset() {
            // nothing to do
        }

        public BufferedImage decodeVideo(IDecodableFrame frame, BufferedImage usable) {
            if (frame instanceof StrFrame) {
                StrFrame pframe = (StrFrame)frame;
                DemuxFrameUncompressor uncompressor = getUncompressor(pframe._demux);
                try {
                    _decoder.decode(uncompressor);
                } catch (UncompressionException ex) {
                    ex.printStackTrace();
                }
                _decoder.readDecodedRGB(_rgb);
                return _rgb.toBufferedImage();
            }
            return null;
        }

        public int getWidth() {
            return _sourceVidItem.getWidth();
        }

        public int getHeight() {
            return _sourceVidItem.getHeight();
        }
    }

}
