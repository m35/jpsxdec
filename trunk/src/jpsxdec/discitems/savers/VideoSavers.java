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

package jpsxdec.discitems.savers;

import jpsxdec.sectors.IdentifiedSector;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import jpsxdec.discitems.ISectorAudioDecoder;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.formats.Rec601YCbCrImage;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.aviwriter.AviWriter;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.mdec.DecodingException;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_double;
import jpsxdec.util.aviwriter.AviWriterDIB;
import jpsxdec.util.aviwriter.AviWriterMJPG;
import jpsxdec.util.aviwriter.AviWriterYV12;

/** All ways you could save a video stream. Classes are combined into one file
 * because they all depend so much on each other. */
class VideoSavers  {

    private static final Logger log = Logger.getLogger(VideoSavers.class.getName());

    //##########################################################################
    //## The Writers ###########################################################
    //##########################################################################

    public static class DemuxSequenceWriter extends VideoSaver {

        protected VideoSaverBuilderSnapshot _snap;
        private final FrameDemuxer _demuxer;

        protected final int _iDigitCount;

        public DemuxSequenceWriter(VideoSaverBuilderSnapshot snap)
        {
            super(snap);
            _snap = snap;

            _iDigitCount = String.valueOf(_snap.saveEndFrame).length();

            _demuxer = new FrameDemuxer(_snap.videoItem.getWidth(), _snap.videoItem.getHeight(),
                                        _snap.videoItem.getStartSector(), _snap.videoItem.getEndSector())
            {
                private byte[] __abBuf;
                protected void frameComplete() throws IOException {
                    if (__abBuf == null || __abBuf.length < getDemuxSize())
                        __abBuf = new byte[getDemuxSize()];
                    copyDemuxData(__abBuf);
                    receive(__abBuf, getDemuxSize(), getFrame(), getPresentationSector());
                }
            };
        }

        public void close() throws IOException {
            _demuxer.flush();
        }
        public int getMovieEndSector() { return _snap.videoItem.getEndSector(); }
        public int getMovieStartSector() { return _snap.videoItem.getStartSector(); }
        public int getStartFrame() { return _snap.saveStartFrame; }
        public int getEndFrame() { return _snap.saveEndFrame; }
        public int getWidth() { return _snap.videoItem.getWidth(); }
        public int getHeight() { return _snap.videoItem.getHeight(); }


        protected String makeFileName(int iFrame) {
            return String.format(_snap.videoFormat.ext(_snap.videoItem), iFrame);
        }

        public void feedSectorForVideo(IVideoSector sector) throws IOException {
            _demuxer.feedSector(sector);
        }

        public void feedSectorForAudio(IdentifiedSector sector) throws IOException {
            throw new UnsupportedOperationException("Cannot write audio with image sequence.");
        }

        protected void receive(byte[] abDemux, int iSize, int iFrameNumber, int iFrameEndSector) throws IOException {
            if (iFrameNumber < _snap.saveStartFrame || iFrameNumber > _snap.saveEndFrame)
                return;
            
            File f = new File(makeFileName(iFrameNumber));
            FileOutputStream fos = new FileOutputStream(f);
            try {
                fos.write(abDemux, 0, iSize);
            } finally {
                fos.close();
            }
        }

    }

    //..........................................................................

    public static class MdecSequenceWriter extends DemuxSequenceWriter {

        private BitStreamUncompressor _uncompressor;

        public MdecSequenceWriter(VideoSaverBuilderSnapshot snap) {
            super(snap);
        }
        
        private BitStreamUncompressor identify(byte[] abDemuxBuf, int iFrame)
                throws NotThisTypeException
        {
            BitStreamUncompressor uncompressor = BitStreamUncompressor.identifyUncompressor(abDemuxBuf, iFrame);
            if (uncompressor == null) {
                throw new NotThisTypeException("Error with frame " + iFrame + ": Unable to determine frame type.");
            } else {
                String s = "Video format identified as " + uncompressor.toString();
                log.info(s);
                getListener().info(s);
            }
            return uncompressor;
        }
        protected BitStreamUncompressor resetUncompressor(byte[] abDemuxBuf, int iFrame) throws NotThisTypeException {
            if (_uncompressor == null) {
                _uncompressor = identify(abDemuxBuf, iFrame);
                _uncompressor.reset(abDemuxBuf);
            } else {
                try {
                    _uncompressor.reset(abDemuxBuf);
                } catch (NotThisTypeException ex) {
                    _uncompressor = identify(abDemuxBuf, iFrame);
                    _uncompressor.reset(abDemuxBuf);
                }
            }
            return _uncompressor;
        }


        @Override
        public void receive(byte[] abDemux, int iSize, int iFrameNumber, int iFrameEndSector) throws IOException {
            try {
                BitStreamUncompressor uncompressor = resetUncompressor(abDemux, iFrameNumber);
                receiveUncompressor(uncompressor, iFrameNumber, iFrameEndSector);
            } catch (NotThisTypeException ex) {
                log.log(Level.WARNING, null, ex);
                getListener().warning(ex);
            }
        }

        protected void receiveUncompressor(BitStreamUncompressor uncompressor, int iFrameNumber, int iFrameEndSector) throws IOException {
            File f = new File(makeFileName(iFrameNumber));
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(f));
                try {
                    final int TOTAL_BLOCKS = ((getHeight() + 15)) / 16 * ((getWidth() + 15) / 16) * 6;
                    MdecInputStreamReader.writeMdecBlocks(uncompressor, bos, TOTAL_BLOCKS);
                } catch (DecodingException ex) {
                    log.log(Level.WARNING, "Error uncompressing frame " + iFrameNumber, ex);
                    getListener().warning("Error uncompressing frame " + iFrameNumber, ex);
                }
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Error writing frame " + iFrameNumber, ex);
                getListener().error("Error writing frame " + iFrameNumber, ex);
            } finally {
                try {
                    bos.close();
                } catch (IOException ex) {
                    log.log(Level.SEVERE, "Error closing file for frame " + iFrameNumber, ex);
                    getListener().error("Error closing file for frame " + iFrameNumber, ex);
                }
            }
        }

    }

    //..........................................................................


    public abstract static class AbstractDecodedWriter extends MdecSequenceWriter {

        protected MdecDecoder _decoder;
        private final int _iCroppedWidth, _iCroppedHeight;

        public AbstractDecodedWriter(VideoSaverBuilderSnapshot snap)
        {
            super(snap);
            if (_snap.crop) {
                _iCroppedWidth = _snap.videoItem.getWidth();
                _iCroppedHeight = _snap.videoItem.getHeight();
            } else {
                _iCroppedWidth = (_snap.videoItem.getWidth() + 15) & ~15;
                _iCroppedHeight = (_snap.videoItem.getHeight() + 15) & ~15;
            }
            _decoder = _snap.videoDecoder; // may be null for now, but set in subclass constructors
        }

        @Override
        final protected void receiveUncompressor(BitStreamUncompressor uncompressor, int iFrameNumber, int iFrameEndSector) throws IOException {
            try {
                _decoder.decode(uncompressor);
                receiveDecoded(_decoder, iFrameNumber, iFrameEndSector);
            } catch (DecodingException ex) {
                log.log(Level.SEVERE, "Error uncompressing frame " + iFrameNumber, ex);
                getListener().error("Error uncompressing frame " + iFrameNumber, ex);
            }
        }

        protected int getCroppedHeight() {
            return _iCroppedHeight;
        }

        protected int getCroppedWidth() {
            return _iCroppedWidth;
        }

        abstract protected void receiveDecoded(MdecDecoder decoder, int iFrame, int iFrameEndSector) throws IOException;
        abstract protected void receiveError(Throwable ex, int iFrame) throws IOException;
    }

    //..........................................................................

    public static class DecodedJavaImageSequenceWriter extends AbstractDecodedWriter {

        private final JavaImageFormat _eFmt;
        private final RgbIntImage _rgbBuff;

        public DecodedJavaImageSequenceWriter(VideoSaverBuilderSnapshot snap) {
            super(snap);

            _rgbBuff = new RgbIntImage(getCroppedWidth(), getCroppedHeight());
            _eFmt = _snap.videoFormat.getImgFmt();
        }

        @Override
        protected void receiveDecoded(MdecDecoder decoder, int iFrame, int iFrameEndSector) {
            decoder.readDecodedRgb(_rgbBuff.getWidth(), _rgbBuff.getHeight(), _rgbBuff.getData(), 0, _rgbBuff.getWidth());
            BufferedImage bi = _rgbBuff.toBufferedImage();
            File f = new File(makeFileName(iFrame));
            try {
                if (!ImageIO.write(bi, _eFmt.getId(), f)) {
                    log.log(Level.WARNING, "Unable to write frame file " + f);
                    getListener().warning("Unable to write frame file " + f);
                }
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error writing frame file " + f, ex);
                getListener().error("Error writing frame file " + f, ex);
            }
        }

        @Override
        protected void receiveError(Throwable thrown, int iFrame) {
            log.log(Level.WARNING, "Error with frame " + iFrame, thrown);
            BufferedImage bi = makeErrorImage(thrown, _rgbBuff.getWidth(), _rgbBuff.getHeight());
            File f = new File(makeFileName(iFrame));
            try {
                if (!ImageIO.write(bi, _eFmt.getId(), f)) {
                    log.log(Level.WARNING, "Unable to write error frame file " + f);
                    getListener().warning("Unable to write error frame file " + f);
                }
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error writing error frame file " + f, ex);
                getListener().error("Error writing error frame file " + f, ex);
            }
        }

    }

    //..........................................................................

    protected static BufferedImage makeErrorImage(Throwable ex, int iWidth, int iHeight) {
        // draw the error onto a blank image
        BufferedImage bi = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.drawString(ex.getMessage(), 5, 20);
        g.dispose();
        return bi;
    }
    
    //..........................................................................
    
    public abstract static class AbstractDecodedAviWriter extends AbstractDecodedWriter {

        private final int _iStartSector, _iEndSector;

        protected final VideoSync _vidSync;

        private double _volume = 1.0;

        protected AviWriter _aviWriter;

        public AbstractDecodedAviWriter(VideoSaverBuilderSnapshot snap)
                throws IOException
        {
            super(snap);

            final int iSectorsPerSecond = _snap.singleSpeed ? 75 : 150;

            if (_snap.audioDecoder != null) {

                AudioFormat fmt = _snap.audioDecoder.getOutputFormat();
                if (fmt.isBigEndian())
                    throw new IllegalArgumentException("Audio format must be little endian for avi writing.");

                AudioVideoSync avSync = new AudioVideoSync(
                        _snap.videoItem.getPresentationStartSector(),
                        iSectorsPerSecond,
                        _snap.videoItem.getSectorsPerFrame(),
                        _snap.audioDecoder.getPresentationStartSector(),
                        fmt.getSampleRate(),
                        _snap.preciseAvSync);

                _snap.audioDecoder.open(new AviAudioWriter(avSync));

                _vidSync = avSync;

                _iStartSector = Math.min(_snap.videoItem.getStartSector(),
                                         _snap.audioDecoder.getStartSector());
                _iEndSector = Math.max(_snap.videoItem.getEndSector(),
                                       _snap.audioDecoder.getEndSector());

            } else {
                _vidSync = new VideoSync(_snap.videoItem.getPresentationStartSector(),
                                         iSectorsPerSecond,
                                         _snap.videoItem.getSectorsPerFrame());

                _iStartSector = _snap.videoItem.getStartSector();
                _iEndSector = _snap.videoItem.getEndSector();


            }

        }

        private class AviAudioWriter implements ISectorAudioDecoder.ISectorTimedAudioWriter {
            private final AudioVideoSync _avSync;

            public AviAudioWriter(AudioVideoSync avSync) {
                _avSync = avSync;
            }

            public void write(AudioFormat inFormat, byte[] abData, int iStart, int iLen, int iPresentationSector) throws IOException
            {
                if (_aviWriter.getAudioSamplesWritten() < 1 &&
                    _avSync.getInitialAudio() > 0)
                {
                    getListener().warning("Writing " + _avSync.getInitialAudio() + " samples of silence to align audio/video playback.");
                    _aviWriter.writeSilentSamples(_avSync.getInitialAudio());
                }
                long lngNeededSilence = _avSync.calculateAudioToCatchUp(iPresentationSector, _aviWriter.getAudioSamplesWritten());
                if (lngNeededSilence > 0) {
                    getListener().warning("Adding " + lngNeededSilence + " samples to keep audio in sync.");
                    _aviWriter.writeSilentSamples(lngNeededSilence);
                }

                _aviWriter.writeAudio(abData, iStart, iLen);
            }
        }
        @Override
        public void feedSectorForAudio(IdentifiedSector sector) throws IOException {
            if (_snap.audioDecoder == null)
                return;
            
            _snap.audioDecoder.feedSector(sector);
        }

        @Override
        public int getMovieEndSector() {
            return _iEndSector;
        }

        @Override
        public int getMovieStartSector() {
            return _iStartSector;
        }


        public String getOutputFile() {
            return _snap.baseName + ".avi";
        }

        @Override
        public void close() throws IOException {
            if (_aviWriter != null) {
                _aviWriter.close();
                _aviWriter = null;
                _snap = null;
            }
        }

        @Override
        protected void receiveDecoded(MdecDecoder decoder, int iFrame, int iFrameEndSector)
                throws IOException
        {

            // if first frame
            if (_aviWriter.getVideoFramesWritten() < 1 && _vidSync.getInitialVideo() > 0) {

                getListener().warning("Writing " + _vidSync.getInitialVideo() + " blank frame(s) to align audio/video playback.");
                _aviWriter.writeBlankFrame();
                for (int i = _vidSync.getInitialVideo()-1; i > 0; i--) {
                    _aviWriter.repeatPreviousFrame();
                }

            }
            
            int iDupCount = _vidSync.calculateFramesToCatchUp(
                                        iFrameEndSector,
                                        _aviWriter.getVideoFramesWritten());

            if (iDupCount < 0)
                // hopefully this will never happen because the frame rate
                // calculated during indexing should prevent it
                getListener().warning("Frame "+iFrame+" is ahead of reading by " + (-iDupCount) + " frame(s).");
            else while (iDupCount > 0) { // will never happen with first frame
                _aviWriter.repeatPreviousFrame();
                iDupCount--;
            }

            actuallyWrite(decoder, iFrame);
        }


        @Override
        final protected void receiveError(Throwable ex, int iFrame) {
            try {
                writeError(ex);
            } catch (IOException ex1) {
                log.log(Level.WARNING, "Error writing error frame " + iFrame, ex);
                getListener().error(ex);
            }
        }

        abstract protected void actuallyWrite(MdecDecoder decoder, int iFrame) throws IOException;
        abstract protected void writeError(Throwable ex) throws IOException;

    }

    public static class DecodedAviWriter_MJPG extends AbstractDecodedAviWriter {

        private final AviWriterMJPG _writerMjpg;
        private final RgbIntImage _rgbBuff;

        public DecodedAviWriter_MJPG(VideoSaverBuilderSnapshot snap)
                throws IOException
        {
            super(snap);

            _writerMjpg = new AviWriterMJPG(new File(getOutputFile()),
                                         getCroppedWidth(), getCroppedHeight(),
                                         _vidSync.getFpsNum(),
                                         _vidSync.getFpsDenom(),
                                         _snap.jpgCompression,
                                         _snap.audioDecoder == null ? null : _snap.audioDecoder.getOutputFormat());

            super._aviWriter = _writerMjpg;
            _rgbBuff = new RgbIntImage(getCroppedWidth(), getCroppedHeight());

        }

        @Override
        protected void actuallyWrite(MdecDecoder decoder, int iFrame) throws IOException {
            decoder.readDecodedRgb(_rgbBuff.getWidth(), _rgbBuff.getHeight(), _rgbBuff.getData(), 0, _rgbBuff.getWidth());
            _writerMjpg.writeFrame(_rgbBuff.toBufferedImage());
        }

        @Override
        protected void writeError(Throwable ex) throws IOException {
            _writerMjpg.writeFrame(makeErrorImage(ex, _aviWriter.getWidth(), _aviWriter.getHeight()));
        }
    }


    public static class DecodedAviWriter_DIB extends AbstractDecodedAviWriter {

        private final AviWriterDIB _writerDib;
        private final RgbIntImage _rgbBuff;

        public DecodedAviWriter_DIB(VideoSaverBuilderSnapshot snap)
                throws IOException
        {
            super(snap);

            _writerDib = new AviWriterDIB(new File(getOutputFile()),
                                          getCroppedWidth(), getCroppedHeight(),
                                          _vidSync.getFpsNum(),
                                          _vidSync.getFpsDenom(),
                                          _snap.audioDecoder == null ? null : _snap.audioDecoder.getOutputFormat());

            super._aviWriter = _writerDib;
            _rgbBuff = new RgbIntImage(getCroppedWidth(), getCroppedHeight());
        }

        @Override
        protected void actuallyWrite(MdecDecoder decoder, int iFrame) throws IOException {
            decoder.readDecodedRgb(_rgbBuff.getWidth(), _rgbBuff.getHeight(), _rgbBuff.getData(), 0, _rgbBuff.getWidth());
            _writerDib.writeFrameRGB(_rgbBuff.getData(), 0, _rgbBuff.getWidth());
        }

        @Override
        protected void writeError(Throwable ex) throws IOException {
            BufferedImage bi = makeErrorImage(ex, _writerDib.getWidth(), _writerDib.getHeight());
            RgbIntImage rgb = new RgbIntImage(bi);
            _writerDib.writeFrameRGB(rgb.getData(), 0, rgb.getWidth());
        }
    }


    //..........................................................................

    public static class DecodedAviWriter_YV12 extends AbstractDecodedAviWriter {

        protected final Rec601YCbCrImage _yuvImgBuff;
        protected final AviWriterYV12 _writerYuv;
        protected final MdecDecoder_double _decoderDbl;

        public DecodedAviWriter_YV12(VideoSaverBuilderSnapshot snap)
                   throws IOException
        {
            super(snap);

            _decoder = _decoderDbl = new MdecDecoder_double(new PsxMdecIDCT_double(), getCroppedWidth(), getCroppedHeight());

            _yuvImgBuff = new Rec601YCbCrImage(getWidth(), getHeight());

            _writerYuv = new AviWriterYV12(new File(getOutputFile()),
                                          _snap.videoItem.getWidth(), _snap.videoItem.getHeight(),
                                          _vidSync.getFpsNum(),
                                          _vidSync.getFpsDenom(),
                                          _snap.audioDecoder == null ? null : _snap.audioDecoder.getOutputFormat());

            super._aviWriter = _writerYuv;
        }

        @Override
        protected void actuallyWrite(MdecDecoder decoder, int iFrame) throws IOException {
            _decoderDbl.readDecodedRec601YCbCr420(_yuvImgBuff);
            _writerYuv.write(_yuvImgBuff.getY(), _yuvImgBuff.getCb(), _yuvImgBuff.getCr());
        }

        @Override
        protected void writeError(Throwable ex) throws IOException {
            BufferedImage bi = makeErrorImage(ex, _writerYuv.getWidth(), _writerYuv.getHeight());
            Rec601YCbCrImage yuv = new Rec601YCbCrImage(bi);
            _writerYuv.write(yuv.getY(), yuv.getCb(), yuv.getCr());
        }

    }

    public static class DecodedAviWriter_JYV12 extends DecodedAviWriter_YV12 {

        public DecodedAviWriter_JYV12(VideoSaverBuilderSnapshot snap) throws IOException {
            super(snap);
        }

        @Override
        protected void actuallyWrite(MdecDecoder decoder, int iFrame) throws IOException {
            _decoderDbl.readDecodedJfifYCbCr420(_yuvImgBuff);
            _writerYuv.write(_yuvImgBuff.getY(), _yuvImgBuff.getCb(), _yuvImgBuff.getCr());
        }

    }
}