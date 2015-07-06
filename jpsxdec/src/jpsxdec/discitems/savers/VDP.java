/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2015  Michael Sabin
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedMessage;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.ISectorAudioDecoder;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.formats.YCbCrImage;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.aviwriter.AviWriter;
import jpsxdec.util.aviwriter.AviWriterDIB;
import jpsxdec.util.aviwriter.AviWriterMJPG;
import jpsxdec.util.aviwriter.AviWriterYV12;

/** Video Decoding Pipeline. 
 * The pipeline is a little complicated since each path is specific about
 * its inputs and outputs. Here are all the possible branches:
 *<pre>
 *  Bitstream -+-> File (Bitstream2File)
 *             |
 *             +-> Mdec (Bitstream2Mdec) -+-> File (Mdec2File)
 *                                        |
 *                                        +-> Jpeg (Mdec2Jpeg)
 *                                        |
 *                                        +-> MjpegAvi (Mdec2MjpegAvi)
 *                                        |
 *                                        +-> Decoded -+-> JavaImage (Decoded2JavaImage)
 *                                                     |
 *                                                     +-> RgbAvi, YuvAvi, JYuvAvi (Decoded2...)
 *</pre>
 */
public class VDP {
    
    private static final Logger LOG = Logger.getLogger(VDP.class.getName());

    public interface GeneratedFileListener {
        void fileGenerated(@Nonnull File f);
    }

    public interface IBitstreamListener {
        void bitstream(@Nonnull byte[] abBitstream, int iSize,
                       @Nonnull FrameNumber frameNumber, int iFrameEndSector) throws IOException;
        void setLog(@CheckForNull Logger log);
    }

    public static class Bitstream2File implements IBitstreamListener {

        @Nonnull
        private final FrameFileFormatter _formatter;
        @Nonnull
        private Logger _log = LOG;
        @CheckForNull
        private GeneratedFileListener _fileGenListener;

        public Bitstream2File(@Nonnull FrameFileFormatter formatter) {
            _formatter = formatter;
        }

        public void bitstream(@Nonnull byte[] abBitstream, int iSize,
                              @Nonnull FrameNumber frameNumber, int iFrameEndSector)
                throws IOException
        {
            File f = _formatter.format(frameNumber, _log);
            IO.makeDirsForFile(f);
            FileOutputStream fos = new FileOutputStream(f);
            if (_fileGenListener != null)
                _fileGenListener.fileGenerated(f);
            try {
                fos.write(abBitstream, 0, iSize);
            } finally {
                try {
                    fos.close();
                } catch (IOException ex) {
                    _log.log(Level.SEVERE, null, ex);
                }
            }
        }

        public void setLog(@CheckForNull Logger log) {
            _log = log == null ? LOG : log;
        }

        public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }
    }

    public static class Bitstream2Mdec implements IBitstreamListener {

        @Nonnull
        private Logger _log = LOG;
        @CheckForNull
        private BitStreamUncompressor _uncompressor;
        @Nonnull
        private final IMdecListener _listener;

        public Bitstream2Mdec(@Nonnull IMdecListener mdecListener) {
            _listener = mdecListener;
        }

        private BitStreamUncompressor identify(@Nonnull byte[] abBitstream, int iBitstreamSize) {
            BitStreamUncompressor uncompressor = BitStreamUncompressor.identifyUncompressor(abBitstream);
            if (uncompressor != null) {
                try {
                    uncompressor.reset(abBitstream, iBitstreamSize);
                    I.VIDEO_FMT_IDENTIFIED(uncompressor.getName()).log(_log, Level.INFO);
                } catch (NotThisTypeException ex) {
                    LOG.log(Level.SEVERE, "Uncompressor rejected frame it just acceped", ex);
                    uncompressor = null;
                }
            }
            return uncompressor;
        }
        private BitStreamUncompressor resetUncompressor(@Nonnull byte[] abBitstream, int iBitstreamSize) {
            if (_uncompressor == null) {
                _uncompressor = identify(abBitstream, iBitstreamSize);
            } else {
                try {
                    _uncompressor.reset(abBitstream, iBitstreamSize);
                } catch (NotThisTypeException ex) {
                    _uncompressor = identify(abBitstream, iBitstreamSize);
                }
            }
            return _uncompressor;
        }

        public void bitstream(@Nonnull byte[] abBitstream, int iBitstreamSize, 
                              @Nonnull FrameNumber frameNumber, int iFrameEndSector)
                throws IOException
        {
            resetUncompressor(abBitstream, iBitstreamSize);
            if (_uncompressor == null) {
                LocalizedMessage msg = I.UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(frameNumber);
                msg.log(_log, Level.SEVERE);
                _listener.error(msg, frameNumber, iFrameEndSector);
            } else
                _listener.mdec(_uncompressor, frameNumber, iFrameEndSector);
        }

        public void setLog(@CheckForNull Logger log) {
            _log = log == null ? LOG : log;
            _listener.setLog(log);
        }
    }

    public interface IMdecListener {
        void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException;
        void error(@Nonnull LocalizedMessage errMsg, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException;
        void setLog(@CheckForNull Logger log);
    }
    
    public static class Mdec2File implements IMdecListener {

        @Nonnull
        private final FrameFileFormatter _formatter;
        private final int _iTotalBlocks;
        @Nonnull
        private Logger _log = LOG;
        @CheckForNull
        private GeneratedFileListener _fileGenListener;

        public Mdec2File(@Nonnull FrameFileFormatter formatter, int iWidth, int iHeight) {
            _formatter = formatter;
            _iTotalBlocks = Calc.blocks(iHeight, iWidth);
        }
        
        public void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FrameNumber frameNumber, int iFrameEndSector_ignored)
                throws IOException
        {
            File f = _formatter.format(frameNumber, _log);
            IO.makeDirsForFile(f);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(f));
                if (_fileGenListener != null)
                    _fileGenListener.fileGenerated(f);
                try {
                    MdecInputStreamReader.writeMdecBlocks(mdecIn, bos, _iTotalBlocks);
                } catch (MdecException ex) {
                    I.FRAME_UNCOMPRESS_ERR(frameNumber).log(_log, Level.WARNING, ex);
                }
            } catch (IOException ex) {
                I.FRAME_WRITE_ERR(f, frameNumber).log(_log, Level.WARNING, ex);
            } finally {
                if (bos != null) try {
                    bos.close();
                } catch (IOException ex) {
                    I.FRAME_FILE_CLOSE_ERR(f, frameNumber).log(_log, Level.SEVERE, ex);
                }
            }
        }

        public void error(@Nonnull LocalizedMessage errMsg, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) {
            // sender already logged it
        }

        public void setLog(@CheckForNull Logger log) {
            _log = log == null ? LOG : log;
        }

        public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }
    }

    
    public static class Mdec2Jpeg implements IMdecListener {

        @Nonnull
        private final FrameFileFormatter _formatter;
        @Nonnull
        private final jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg _jpegTranslator;
        @Nonnull
        private final ExposedBAOS _buffer = new ExposedBAOS();
        @Nonnull
        private Logger _log = LOG;
        @CheckForNull
        private GeneratedFileListener _fileGenListener;

        public Mdec2Jpeg(@Nonnull FrameFileFormatter formatter, int iWidth, int iHeight) {
            _formatter = formatter;
            _jpegTranslator = new jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg(iWidth, iHeight);
        }

        public void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            File f = _formatter.format(frameNumber, _log);
            IO.makeDirsForFile(f);
            try {
                _jpegTranslator.readMdec(mdecIn);
            } catch (MdecException.Decode ex) {
                I.FRAME_UNCOMPRESS_ERR(frameNumber).log(_log, Level.WARNING, ex);
            }
            _buffer.reset();
            _jpegTranslator.writeJpeg(_buffer);
            FileOutputStream fos = new FileOutputStream(f);
            if (_fileGenListener != null)
                _fileGenListener.fileGenerated(f);
            try {
                fos.write(_buffer.getBuffer(), 0, _buffer.size());
            } finally {
                try {
                    fos.close();
                } catch (IOException ex) {
                    _log.log(Level.SEVERE, null, ex);
                }
            }
        }

        public void error(@Nonnull LocalizedMessage errMsg, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) {
            // sender already logged it
        }

        public void setLog(@CheckForNull Logger log) {
            _log = log == null ? LOG : log;
        }

        public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }
    }


    public static class Mdec2Decoded implements IMdecListener {

        @Nonnull
        private final MdecDecoder _decoder;
        @CheckForNull
        private IDecodedListener _listener;
        @Nonnull
        private Logger _log = LOG;

        public Mdec2Decoded(@Nonnull MdecDecoder decoder) {
            _decoder = decoder;
        }

        public void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_listener == null)
                throw new IllegalStateException("IDecodedListener must be set");
            try {
                _decoder.decode(mdecIn);
            } catch (MdecException.Decode ex) {
                I.FRAME_UNCOMPRESS_ERR(frameNumber).log(_log, Level.WARNING, ex);
            }
            _listener.decoded(_decoder, frameNumber, iFrameEndSector);
        }

        public void error(@Nonnull LocalizedMessage errMsg, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_listener == null)
                throw new IllegalStateException("IDecodedListener must be set");
            _listener.error(errMsg, frameNumber, iFrameEndSector);
        }

        public void setDecoded(@CheckForNull IDecodedListener decoded) {
            if (decoded == null)
                return;
            decoded.assertAcceptsDecoded(_decoder);
            _listener = decoded;
        }

        public void setLog(@CheckForNull Logger log) {
            _log = log == null ? LOG : log;
            if (_listener == null)
                throw new IllegalStateException("IDecodedListener must be set");
            _listener.setLog(log);
        }
        
    }

    public interface IDecodedListener {
        void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException;
        void error(@Nonnull LocalizedMessage errMsg, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException;
        void setLog(@CheckForNull Logger log);
        void assertAcceptsDecoded(@Nonnull MdecDecoder decoder);
    }

    public static class Decoded2JavaImage implements IDecodedListener {

        @Nonnull
        private final FrameFileFormatter _formatter;
        @Nonnull
        private final String _sFmt;
        @Nonnull
        private final BufferedImage _rgbImg;
        @Nonnull
        private Logger _log = LOG;
        @CheckForNull
        private GeneratedFileListener _fileGenListener;

        public Decoded2JavaImage(@Nonnull FrameFileFormatter formatter, @Nonnull JavaImageFormat eFmt, int iWidth, int iHeight) {
            _formatter = formatter;
            _sFmt = eFmt.getId();
            _rgbImg = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
        }
        
        public void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            decoder.readDecodedRgb(_rgbImg.getWidth(), _rgbImg.getHeight(),
                    ((DataBufferInt)_rgbImg.getRaster().getDataBuffer()).getData());
            File f = _formatter.format(frameNumber, _log);
            IO.makeDirsForFile(f);
            try {
                if (ImageIO.write(_rgbImg, _sFmt, f)) {
                    if (_fileGenListener != null)
                        _fileGenListener.fileGenerated(f);
                } else {
                    I.FRAME_FILE_WRITE_UNABLE(f, frameNumber).log(_log, Level.WARNING);
                }
            } catch (IOException ex) {
                I.FRAME_WRITE_ERR(f, frameNumber).log(_log, Level.WARNING, ex);
            }
        }

        public void error(@Nonnull LocalizedMessage errMsg, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) {
            // sender already logged it
        }

        public void assertAcceptsDecoded(@Nonnull MdecDecoder decoder) {
        }

        public void setLog(@CheckForNull Logger log) {
            _log = log == null ? LOG : log;
        }

        public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }
    }

    // ########################################################################
    // ########################################################################
    // ########################################################################
    // ########################################################################

    /** Most Avi will take Decoded as input, but MJPG will need Mdec as input,
     *  so save the interface implementation for subclasses. */
    public static abstract class ToAvi implements ISectorAudioDecoder.ISectorTimedAudioWriter {
        @Nonnull
        protected final File _outputFile;
        protected final int _iWidth, _iHeight;
        @Nonnull
        protected final VideoSync _vidSync;
        @CheckForNull
        private final AudioVideoSync _avSync;
        @CheckForNull
        protected final AudioFormat _af;
        @Nonnull
        protected Logger _log = LOG;
        @CheckForNull
        protected AviWriter _writer;
        @CheckForNull
        protected GeneratedFileListener _fileGenListener;

        /** Video without audio. */
        public ToAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync) {
            _outputFile = outputFile;
            _iWidth = iWidth; _iHeight = iHeight;
            _vidSync = vidSync; _avSync =  null;
            _af = null;
        }

        /** Video with audio. */
        public ToAvi(@Nonnull File outputFile, int iWidth, int iHeight,
                     @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af)
        {
            _outputFile = outputFile;
            _iWidth = iWidth; _iHeight = iHeight;
            _vidSync = _avSync = avSync;
            _af = af;
        }

        final public @Nonnull File getOutputFile() {
            return _outputFile;
        }

        abstract public void open() throws IOException;

        // subclasses will implement IDecodedListener or IMdecListener to match this
        abstract public void error(@Nonnull LocalizedMessage sErr, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException;

        final protected void prepForFrame(@CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_writer == null)
                throw new IllegalStateException("Avi writer is not open");

            // if first frame
            if (_writer.getVideoFramesWritten() < 1 && _vidSync.getInitialVideo() > 0) {

                I.WRITING_BLANK_FRAMES_TO_ALIGN_AV(_vidSync.getInitialVideo()).log(_log, Level.INFO);
                _writer.writeBlankFrame();
                for (int i = _vidSync.getInitialVideo()-1; i > 0; i--) {
                    _writer.repeatPreviousFrame();
                }

            }

            int iDupCount = _vidSync.calculateFramesToCatchUp(
                                        iFrameEndSector,
                                        _writer.getVideoFramesWritten());

            if (iDupCount < 0) {
                // hopefully this will never happen because the frame rate
                // calculated during indexing should prevent it
                if (frameNumber == null)
                    I.FRAME_AHEAD_OF_READING(-iDupCount).log(_log, Level.WARNING);
                else
                    I.FRAME_NUM_AHEAD_OF_READING(frameNumber, -iDupCount).log(_log, Level.WARNING);
            } else {
                while (iDupCount > 0) { // could happen with first frame
                    if (_writer.getVideoFramesWritten() < 1) // TODO: fix design so this isn't needed
                        _writer.writeBlankFrame();
                    else
                        _writer.repeatPreviousFrame();
                    iDupCount--;
                }
            }
        }

        /** Writes audio. */
        final public void write(@Nonnull AudioFormat format, @Nonnull byte[] abData, int iStart, int iLen, int iPresentationSector) throws IOException {
            if (_writer == null)
                throw new IllegalStateException("Avi writer is not open");

            // _avSync should not be null if this method is called

            if (_writer.getAudioSamplesWritten() < 1 &&
                _avSync.getInitialAudio() > 0)
            {
                I.WRITING_SILECE_TO_SYNC_AV(_avSync.getInitialAudio()).log(_log, Level.INFO);
                _writer.writeSilentSamples(_avSync.getInitialAudio());
            }
            long lngNeededSilence = _avSync.calculateAudioToCatchUp(iPresentationSector, _writer.getAudioSamplesWritten());
            if (lngNeededSilence > 0) {
                I.WRITING_SILENCE_TO_KEEP_AV_SYNCED(lngNeededSilence).log(_log, Level.INFO);
                _writer.writeSilentSamples(lngNeededSilence);
            }

            _writer.writeAudio(abData, iStart, iLen);
        }

        public void close() throws IOException {
            if (_writer != null) {
                _writer.close();
            }
        }

        public void setLog(@CheckForNull Logger log) {
            _log = log == null ? LOG : log;
        }

        public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }
    }

    static class Decoded2RgbAvi extends ToAvi implements IDecodedListener {
        @CheckForNull
        private AviWriterDIB _writerDib;
        @CheckForNull
        private int[] _aiImageBuf;

        public Decoded2RgbAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync) {
            super(outputFile, iWidth, iHeight, vidSync);
        }

        public Decoded2RgbAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af) {
            super(outputFile, iWidth, iHeight, avSync, af);
        }

        public void assertAcceptsDecoded(@Nonnull MdecDecoder decoder) {
        }

        public void open() throws IOException {
            if (_writer == null) {
                IO.makeDirsForFile(_outputFile);
                _writer = _writerDib = new AviWriterDIB(_outputFile,
                                                        _iWidth, _iHeight,
                                                        _vidSync.getFpsNum(),
                                                        _vidSync.getFpsDenom(),
                                                        _af);
                if (_fileGenListener != null)
                    _fileGenListener.fileGenerated(_outputFile);
                _aiImageBuf = new int[_iWidth*_iHeight];
            }
        }

        public void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_writerDib == null)
                throw new IllegalStateException("AVI not open.");
            prepForFrame(frameNumber, iFrameEndSector);
            decoder.readDecodedRgb(_writerDib.getWidth(), _writerDib.getHeight(), _aiImageBuf);
            _writerDib.writeFrameRGB(_aiImageBuf, 0, _writerDib.getWidth());
        }

        public void error(@Nonnull LocalizedMessage errMsg, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_writerDib == null)
                throw new IllegalStateException("AVI not open.");
            prepForFrame(frameNumber, iFrameEndSector);
            BufferedImage bi = makeErrorImage(errMsg, _writerDib.getWidth(), _writerDib.getHeight());
            RgbIntImage rgb = new RgbIntImage(bi);
            _writerDib.writeFrameRGB(rgb.getData(), 0, _writerDib.getWidth());
        }

    }

    static class Decoded2YuvAvi extends ToAvi implements IDecodedListener {
        @CheckForNull
        protected YCbCrImage _yuvImgBuff;
        @CheckForNull
        protected AviWriterYV12 _writerYuv;

        public Decoded2YuvAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync) {
            super(outputFile, iWidth, iHeight, vidSync);
        }

        public Decoded2YuvAvi(@Nonnull File outputFile, int iWidth, int iHeight, 
                              @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af)
        {
            super(outputFile, iWidth, iHeight, avSync, af);
        }

        public void assertAcceptsDecoded(@Nonnull MdecDecoder decoder) {
            if (!(decoder instanceof MdecDecoder_double))
                throw new IllegalArgumentException(getClass().getName() + " can't handle " + decoder.getClass().getName());
        }
        
        public void open() throws IOException {
            if (_writer == null) {
                IO.makeDirsForFile(_outputFile);
                _writer = _writerYuv = new AviWriterYV12(_outputFile,
                                                         _iWidth, _iHeight,
                                                         _vidSync.getFpsNum(),
                                                         _vidSync.getFpsDenom(),
                                                         _af);
                if (_fileGenListener != null)
                    _fileGenListener.fileGenerated(_outputFile);
                _yuvImgBuff = new YCbCrImage(_iWidth, _iHeight);
            }
        }

        public void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_writerYuv == null)
                throw new IllegalStateException("AVI not open.");
            prepForFrame(frameNumber, iFrameEndSector);
            // only accepts MdecDecoder_double, verified in assertAcceptsDecoded()
            ((MdecDecoder_double)decoder).readDecoded_Rec601_YCbCr420(_yuvImgBuff);
            _writerYuv.write(_yuvImgBuff.getY(), _yuvImgBuff.getCb(), _yuvImgBuff.getCr());
        }

        public void error(@Nonnull LocalizedMessage errMsg, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_writerYuv == null)
                throw new IllegalStateException("AVI not open.");
            prepForFrame(frameNumber, iFrameEndSector);
            // TODO: write error with proper sample range
            BufferedImage bi = makeErrorImage(errMsg, _writerYuv.getWidth(), _writerYuv.getHeight());
            YCbCrImage yuv = new YCbCrImage(bi);
            _writerYuv.write(yuv.getY(), yuv.getCb(), yuv.getCr());
        }

    }


    static class Decoded2JYuvAvi extends Decoded2YuvAvi {

        public Decoded2JYuvAvi(@Nonnull File outputFile, int iWidth, int iHeight, 
                               @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af)
        {
            super(outputFile, iWidth, iHeight, avSync, af);
        }

        public Decoded2JYuvAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync) {
            super(outputFile, iWidth, iHeight, vidSync);
        }

        @Override
        public void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_writerYuv == null)
                throw new IllegalStateException("AVI not open.");
            prepForFrame(frameNumber, iFrameEndSector);
            // only accepts MdecDecoder_double, verified in assertAcceptsDecoded()
            ((MdecDecoder_double)decoder).readDecoded_JFIF_YCbCr420(_yuvImgBuff);
            _writerYuv.write(_yuvImgBuff.getY(), _yuvImgBuff.getCb(), _yuvImgBuff.getCr());
        }
        
    }

    /** This Avi output is unique in that it takes Mdec as input instead of Decoded. */
    public static class Mdec2MjpegAvi extends ToAvi implements IMdecListener {
        @Nonnull
        private final jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg _jpegTranslator;
        @Nonnull
        private final ExposedBAOS _buffer = new ExposedBAOS();
        @CheckForNull
        private AviWriterMJPG _mjpegWriter;

        public Mdec2MjpegAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync) {
            super(outputFile, iWidth, iHeight, vidSync);
            _jpegTranslator = new jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg(iWidth, iHeight);
        }

        public Mdec2MjpegAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af) {
            super(outputFile, iWidth, iHeight, avSync, af);
            _jpegTranslator = new jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg(iWidth, iHeight);
        }

        public void open() throws IOException {
            if (_writer == null) {
                IO.makeDirsForFile(_outputFile);
                _writer = _mjpegWriter = new AviWriterMJPG
                        (_outputFile, _iWidth, _iHeight, _vidSync.getFpsNum(), _vidSync.getFpsDenom(), _af);
                if (_fileGenListener != null)
                    _fileGenListener.fileGenerated(_outputFile);
            }
        }

        public void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_mjpegWriter == null)
                throw new IllegalStateException("AVI not open.");
            try {
                _jpegTranslator.readMdec(mdecIn);
            } catch (MdecException.Decode ex) {
                I.FRAME_UNCOMPRESS_ERR(frameNumber).log(_log, Level.WARNING, ex);
            }
            _buffer.reset();
            _jpegTranslator.writeJpeg(_buffer);
            prepForFrame(frameNumber, iFrameEndSector);
            _mjpegWriter.writeFrame(_buffer.getBuffer(), 0, _buffer.size());
        }

        public void error(@Nonnull LocalizedMessage errMsg, @CheckForNull FrameNumber frameNumber, int iFrameEndSector) throws IOException {
            if (_mjpegWriter == null)
                throw new IllegalStateException("AVI not open.");
            prepForFrame(frameNumber, iFrameEndSector);
            _mjpegWriter.writeFrame(makeErrorImage(errMsg, _iWidth, _iHeight));
        }

    }

    
    /** Draw the error onto a blank image. */
    private static @Nonnull BufferedImage makeErrorImage(@Nonnull LocalizedMessage sErr, int iWidth, int iHeight) {
        BufferedImage bi = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.white);
        g.drawString(sErr.getLocalizedMessage(), 5, 20);
        g.dispose();
        return bi;
    }
}
