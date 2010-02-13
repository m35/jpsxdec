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

package jpsxdec.plugins.psx.str;

import jpsxdec.plugins.IdentifiedSector;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.formats.Yuv4mpeg2;
import jpsxdec.formats.Yuv4mpeg2Writer;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.ProgressListener;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.aviwriter.AviWriter;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;
import jpsxdec.plugins.psx.video.decode.UncompressionException;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder_double;
import jpsxdec.plugins.psx.video.mdec.MdecInputStream;
import jpsxdec.plugins.psx.video.mdec.MdecInputStream.MdecCode;
import jpsxdec.plugins.psx.video.mdec.idct.StephensIDCT;
import jpsxdec.plugins.xa.IAudioSectorDecoder;
import jpsxdec.plugins.xa.IAudioReceiver;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import jpsxdec.util.aviwriter.AviWriterDIB;
import jpsxdec.util.aviwriter.AviWriterMJPG;
import jpsxdec.util.aviwriter.AviWriterYV12;


class SectorMovieWriters  {

    private static final Logger log = Logger.getLogger(SectorMovieWriters.class.getName());

    //##########################################################################
    //## The Writers ###########################################################
    //##########################################################################

    public static class DemuxSequenceWriter implements SectorMovieWriter, IDemuxReceiver {

        protected final String _sBaseName;
        protected final DiscItemSTRVideo _vidItem;
        private final int _iStartFrame;
        private final int _iEndFrame;
        private ProgressListener _progress;
        private final FrameDemuxer _demuxer;

        protected final int _iDigitCount;

        public DemuxSequenceWriter(DiscItemSTRVideo vidItem, String sBaseName,
                           int iStartFrame, int iEndFrame)
        {
            _sBaseName = sBaseName;
            _iStartFrame = iStartFrame;
            _iEndFrame = iEndFrame;
            _vidItem = vidItem;

            _iDigitCount = String.valueOf(_iEndFrame).length();

            _demuxer = new FrameDemuxer(this, vidItem.getStartSector(), vidItem.getEndSector());
        }

        public void close() throws IOException { /* nothing to do */ }
        public int getMovieEndSector() { return _vidItem.getEndSector(); }
        public int getMovieStartSector() { return _vidItem.getStartSector(); }
        public int getStartFrame() { return _iStartFrame; }
        public int getEndFrame() { return _iEndFrame; }
        protected ProgressListener getListener() { return _progress; }
        public void setListener(ProgressListener pl) { _progress = pl; }
        public int getWidth() { return _vidItem.getWidth(); }
        public int getHeight() { return _vidItem.getHeight(); }


        protected String makeFileName(int iFrame) {
            return String.format("%s_%dx%d[%0"+_iDigitCount+"d].demux",
                                  _sBaseName,
                                  _vidItem.getWidth(), _vidItem.getHeight(),
                                  iFrame);
        }

        public String getOutputFile() {
            return makeFileName(_iStartFrame) + " to " + makeFileName(_iEndFrame);
        }

        @Override
        public void feedSectorForVideo(IVideoSector sector) throws IOException {
            _demuxer.feedSector(sector);
        }

        public void feedSectorForAudio(IdentifiedSector sector) throws IOException {
            throw new UnsupportedOperationException("Cannot write audio with image sequence.");
        }

        @Override
        public void receive(byte[] abDemux, int iSize, int iFrameNumber, int iFrameEndSector) throws IOException {
            if (iFrameNumber < _iStartFrame || iFrameNumber > _iEndFrame)
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

        private DemuxFrameUncompressor _uncompressor;

        public MdecSequenceWriter(DiscItemSTRVideo vidItem, String sBaseName,
                          int iStartFrame, int iEndFrame)
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame);

        }
        
        private DemuxFrameUncompressor identify(byte[] abDemuxBuf, int iStart, int iFrame)
                throws NotThisTypeException
        {
            DemuxFrameUncompressor uncompressor = JPSXPlugin.identifyUncompressor(abDemuxBuf, iStart, iFrame);
            if (uncompressor == null) {
                throw new NotThisTypeException("Error with frame " + iFrame + ": Unable to determine frame type.");
            } else {
                String s = "Using " + uncompressor.toString() + " uncompressor";
                log.info(s);
                getListener().info(s);
            }
            return uncompressor;
        }
        protected DemuxFrameUncompressor resetUncompressor(byte[] abDemuxBuf, int iStart, int iFrame) throws NotThisTypeException {
            if (_uncompressor == null) {
                _uncompressor = identify(abDemuxBuf, iStart, iFrame);
                _uncompressor.reset(abDemuxBuf, iStart);
            } else {
                try {
                    _uncompressor.reset(abDemuxBuf, iStart);
                } catch (NotThisTypeException ex) {
                    _uncompressor = identify(abDemuxBuf, iStart, iFrame);
                    _uncompressor.reset(abDemuxBuf, iStart);
                }
            }
            return _uncompressor;
        }


        @Override
        public void receive(byte[] abDemux, int iSize, int iFrameNumber, int iFrameEndSector) throws IOException {
            try {
                DemuxFrameUncompressor uncompressor = resetUncompressor(abDemux, 0, iFrameNumber);
                receiveUncompressor(uncompressor, iFrameNumber, iFrameEndSector);
            } catch (NotThisTypeException ex) {
                log.log(Level.WARNING, null, ex);
                getListener().warning(ex);
            }
        }

        protected void receiveUncompressor(DemuxFrameUncompressor uncompressor, int iFrameNumber, int iFrameEndSector) throws IOException {
            File f = new File(makeFileName(iFrameNumber));
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                try {
                    writeMdec(uncompressor, fos);
                } catch (UncompressionException ex) {
                    log.log(Level.WARNING, "Error uncompressing frame " + iFrameNumber, ex);
                    getListener().warning("Error uncompressing frame " + iFrameNumber, ex);
                }
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Error writing frame " + iFrameNumber, ex);
                getListener().error("Error writing frame " + iFrameNumber, ex);
            } finally {
                try {
                    fos.close();
                } catch (IOException ex) {
                    log.log(Level.SEVERE, "Error closing file for frame " + iFrameNumber, ex);
                    getListener().error("Error closing file for frame " + iFrameNumber, ex);
                }
            }
        }

        protected String makeFileName(int iFrame) {
            return String.format("%s_%dx%d[%0"+_iDigitCount+"d].mdec",
                                 _sBaseName,
                                 _vidItem.getWidth(), _vidItem.getHeight(),
                                 iFrame);
        }

        private void writeMdec(MdecInputStream mdecIn, OutputStream streamOut)
                throws UncompressionException, IOException
        {
            MdecCode code = new MdecCode();
            final int TOTAL_BLOCKS = ((getHeight() + 15)) / 16 * ((getWidth() + 15) / 16) * 6;
            int iBlock = 0;
            while (iBlock < TOTAL_BLOCKS) {
                if (mdecIn.readMdecCode(code)) {
                    iBlock++;
                }
                IO.writeInt16LE(streamOut, code.toMdecWord());
            }
        }

    }

    //..........................................................................


    public abstract static class AbstractDecodedWriter extends MdecSequenceWriter {

        protected MdecDecoder _decoder;
        private final int _iCroppedWidth, _iCroppedHeight;

        public AbstractDecodedWriter(DiscItemSTRVideo vidItem,
                String sBaseName,
                int iStartFrame, int iEndFrame,
                MdecDecoder decoder, boolean blnCrop)
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame);
            if (blnCrop) {
                _iCroppedWidth = _vidItem.getWidth();
                _iCroppedHeight = _vidItem.getHeight();
            } else {
                _iCroppedWidth = (_vidItem.getWidth() + 15) & ~15;
                _iCroppedHeight = (_vidItem.getHeight() + 15) & ~15;
            }
            _decoder = decoder; // may be null for now, but set in subclass constructors
        }

        @Override
        final protected void receiveUncompressor(DemuxFrameUncompressor uncompressor, int iFrameNumber, int iFrameEndSector) throws IOException {
            try {
                _decoder.decode(uncompressor);
                receiveDecoded(_decoder, iFrameNumber, iFrameEndSector);
            } catch (UncompressionException ex) {
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

        public DecodedJavaImageSequenceWriter(DiscItemSTRVideo vidItem,
                String sBaseName,
                int iStartFrame, int iEndFrame,
                MdecDecoder decoder, boolean blnCrop,
                JavaImageFormat eFormat)
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame, decoder, blnCrop);

            _rgbBuff = new RgbIntImage(getCroppedWidth(), getCroppedHeight());
            _eFmt = eFormat;
        }

        @Override
        protected void receiveDecoded(MdecDecoder decoder, int iFrame, int iFrameEndSector) {
            decoder.readDecodedRGB(_rgbBuff);
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

        protected String makeFileName(int iFrame) {
            return String.format("%s[%0"+_iDigitCount+"d].%s",
                              _sBaseName, iFrame,
                              _eFmt.getExtension());
        }

    }



    public static class DecodedYuv4mpeg2Writer extends AbstractDecodedWriter {

        private final Yuv4mpeg2 _yuvImgBuff;
        private Yuv4mpeg2Writer _writer;
        private final MdecDecoder_double _decoderDbl;

        public DecodedYuv4mpeg2Writer(DiscItemSTRVideo vidItem,
                                    String sBaseName,
                                    int iStartFrame, int iEndFrame,
                                    boolean blnSingleSpeed,
                                    boolean blnCrop)
               throws IOException
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame, null, blnCrop);

            final Fraction frameRate;
            if (blnSingleSpeed) {
                frameRate = new Fraction(75).divide(_vidItem.getSectorsPerFrame());
            } else {
                frameRate = new Fraction(150).divide(_vidItem.getSectorsPerFrame());
            }

            _decoder = _decoderDbl = new MdecDecoder_double(new StephensIDCT(), getCroppedWidth(), getCroppedHeight());
            _yuvImgBuff = new Yuv4mpeg2(getCroppedWidth(), getCroppedHeight());

            File f = new File(_sBaseName + ".y4m");

            _writer = new Yuv4mpeg2Writer(f, getCroppedWidth(), getCroppedHeight(),
                                          (int)frameRate.getNumerator(), (int)frameRate.getDenominator(),
                                          Yuv4mpeg2.SUB_SAMPLING);
        }

        @Override
        protected void receiveDecoded(MdecDecoder decoder, int iFrame, int iFrameEndSector) throws IOException {
            _decoderDbl.readDecodedYuv4mpeg2(_yuvImgBuff);
            _writer.writeFrame(_yuvImgBuff);
        }

        @Override
        protected void receiveError(Throwable ex, int iFrame) throws IOException {
            BufferedImage bi = makeErrorImage(ex, _writer.getWidth(), _writer.getHeight());
            Yuv4mpeg2 yuv = new Yuv4mpeg2(bi);
            _writer.writeFrame(yuv);
        }


        @Override
        public String getOutputFile() {
            return _sBaseName + ".y4m";
        }

        @Override
        public void close() throws IOException {
            _writer.close();
        }

    }


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

        private IAudioSectorDecoder _audioSectorDecoder;

        private final int _iStartSector, _iEndSector;

        protected final VideoSync _vidSync;

        private double _volume = 1.0;

        protected AviWriter _aviWriter;

        public AbstractDecodedAviWriter(DiscItemSTRVideo vidItem,
                                        String sBaseName,
                                        int iStartFrame, int iEndFrame,
                                        boolean blnSingleSpeed,
                                        MdecDecoder decoder,
                                        boolean blnCrop,
                                        boolean blnPrecisesAV,
                                        IAudioSectorDecoder audioDecoder)
                throws IOException
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame, decoder, blnCrop);

            final int iSectorsPerSecond = blnSingleSpeed ? 75 : 150;

            _audioSectorDecoder = audioDecoder;

            if (_audioSectorDecoder != null) {

                AudioFormat fmt = _audioSectorDecoder.getOutputFormat();
                if (fmt.isBigEndian())
                    throw new IllegalArgumentException("Audio format must be little endian for avi writing.");

                AudioVideoSync avSync = new AudioVideoSync(
                        _vidItem.getPresentationStartSector(),
                        iSectorsPerSecond,
                        _vidItem.getSectorsPerFrame(),
                        _audioSectorDecoder.getPresentationStartSector(),
                        fmt.getSampleRate(),
                        blnPrecisesAV);

                _audioSectorDecoder.open(new AviAudioWriter(avSync));

                _vidSync = avSync;

                _iStartSector = Math.min(_vidItem.getStartSector(),
                                         _audioSectorDecoder.getStartSector());
                _iEndSector = Math.max(_vidItem.getEndSector(),
                                       _audioSectorDecoder.getEndSector());

            } else {
                _vidSync = new VideoSync(_vidItem.getPresentationStartSector(), 
                                         iSectorsPerSecond,
                                         _vidItem.getSectorsPerFrame());

                _iStartSector = _vidItem.getStartSector();
                _iEndSector = _vidItem.getEndSector();


            }

        }

        private class AviAudioWriter implements IAudioReceiver {
            private final AudioVideoSync _avSync;

            public AviAudioWriter(AudioVideoSync avSync) {
                _avSync = avSync;
            }

            public void close() { /* do nothing */ }

            public void write(AudioFormat inFormat, byte[] abData, int iStart, int iLen, int iPresentationSector) throws IOException
            {
                if (_aviWriter.getAudioSamplesWritten() < 1 &&
                    _avSync.getInitialAudio() > 0)
                {
                    getListener().warning("Writing " + _avSync.getInitialAudio() + " samples of silence to align audio/video playback.");
                    _aviWriter.writeSilentSamples(_avSync.getInitialAudio());
                }
                long lngNeededSilence = _avSync.calculateNeededSilence(iPresentationSector, iLen / inFormat.getFrameSize());
                if (lngNeededSilence > 0) {
                    getListener().warning("Adding " + lngNeededSilence + " samples to keep audio in sync.");
                    _aviWriter.writeSilentSamples(lngNeededSilence);
                }
                _aviWriter.writeAudio(abData, iStart, iLen);
            }
        }
        @Override
        public void feedSectorForAudio(IdentifiedSector sector) throws IOException {
            if (_audioSectorDecoder == null)
                return;
            
            _audioSectorDecoder.feedSector(sector);
        }

        @Override
        public int getMovieEndSector() {
            return _iEndSector;
        }

        @Override
        public int getMovieStartSector() {
            return _iStartSector;
        }


        @Override
        public String getOutputFile() {
            return _sBaseName + ".avi";
        }

        @Override
        public void close() throws IOException {
            if (_aviWriter != null) {
                _aviWriter.close();
                _aviWriter = null;
                _audioSectorDecoder = null;
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
            
            int iDupCount = _vidSync.calculateFramesToCatchup(
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

        private final float _fltJpgQuality;
        private final AviWriterMJPG _writerMjpg;
        private final RgbIntImage _rgbBuff;

        public DecodedAviWriter_MJPG(DiscItemSTRVideo vidItem, String sBaseName,
                    int iStartFrame, int iEndFrame,
                    boolean blnSingleSpeed, MdecDecoder decoder,
                    boolean blnCrop, float fltJpgQuality,
                    boolean blnPreciseAV,
                    IAudioSectorDecoder audioDecoder)
                throws IOException
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame,
                    blnSingleSpeed, decoder, blnCrop, blnPreciseAV, audioDecoder);

            _fltJpgQuality = fltJpgQuality;

            _writerMjpg = new AviWriterMJPG(new File(getOutputFile()),
                                         getCroppedWidth(), getCroppedHeight(),
                                         _vidSync.getFpsNum(),
                                         _vidSync.getFpsDenom(),
                                         _fltJpgQuality,
                                         audioDecoder == null ? null : audioDecoder.getOutputFormat());

            super._aviWriter = _writerMjpg;
            _rgbBuff = new RgbIntImage(getCroppedWidth(), getCroppedHeight());

        }

        @Override
        protected void actuallyWrite(MdecDecoder decoder, int iFrame) throws IOException {
            decoder.readDecodedRGB(_rgbBuff);
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

        public DecodedAviWriter_DIB(DiscItemSTRVideo vidItem,
                                  String sBaseName,
                                  int iStartFrame, int iEndFrame,
                                  boolean blnSingleSpeed,
                                  MdecDecoder decoder,
                                  boolean blnCrop,
                                  boolean blnPreciseAV,
                                  IAudioSectorDecoder audioDecoder)
                throws IOException
        {
            super(vidItem,
                  sBaseName,
                  iStartFrame, iEndFrame,
                  blnSingleSpeed,
                  decoder,
                  blnCrop,
                  blnPreciseAV,
                  audioDecoder);

            _writerDib = new AviWriterDIB(new File(getOutputFile()),
                                          getCroppedWidth(), getCroppedHeight(),
                                          _vidSync.getFpsNum(),
                                          _vidSync.getFpsDenom(),
                                          audioDecoder == null ? null : audioDecoder.getOutputFormat());

            super._aviWriter = _writerDib;
            _rgbBuff = new RgbIntImage(getCroppedWidth(), getCroppedHeight());
        }

        @Override
        protected void actuallyWrite(MdecDecoder decoder, int iFrame) throws IOException {
            decoder.readDecodedRGB(_rgbBuff);
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

        private final Yuv4mpeg2 _yuvImgBuff;
        private final AviWriterYV12 _writerYuv;
        private final MdecDecoder_double _decoderDbl;

        public DecodedAviWriter_YV12(DiscItemSTRVideo vidItem,
                                   String sBaseName,
                                   int iStartFrame, int iEndFrame,
                                   boolean blnSingleSpeed,
                                   boolean blnCrop,
                                   boolean blnPreciseAV,
                                   IAudioSectorDecoder audioDecoder)
                   throws IOException
        {
            super(vidItem, sBaseName,
                  iStartFrame, iEndFrame,
                  blnSingleSpeed, null, blnCrop,
                  blnPreciseAV,
                  audioDecoder);

            _decoder = _decoderDbl = new MdecDecoder_double(new StephensIDCT(), getCroppedWidth(), getCroppedHeight());

            _yuvImgBuff = new Yuv4mpeg2(getWidth(), getHeight());

            _writerYuv = new AviWriterYV12(new File(getOutputFile()),
                                          _vidItem.getWidth(), _vidItem.getHeight(),
                                          _vidSync.getFpsNum(),
                                          _vidSync.getFpsDenom(),
                                          audioDecoder == null ? null : audioDecoder.getOutputFormat());

            super._aviWriter = _writerYuv;
        }

        @Override
        protected void actuallyWrite(MdecDecoder decoder, int iFrame) throws IOException {
            _decoderDbl.readDecodedYuv4mpeg2(_yuvImgBuff);
            _writerYuv.write(_yuvImgBuff.getY(), _yuvImgBuff.getCr(), _yuvImgBuff.getCb());
        }

        @Override
        protected void writeError(Throwable ex) throws IOException {
            BufferedImage bi = makeErrorImage(ex, _writerYuv.getWidth(), _writerYuv.getHeight());
            Yuv4mpeg2 yuv = new Yuv4mpeg2(bi);
            _writerYuv.write(yuv.getY(), yuv.getCr(), yuv.getCb());
        }

    }

}