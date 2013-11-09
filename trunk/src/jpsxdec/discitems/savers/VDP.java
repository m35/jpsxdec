/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013  Michael Sabin
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
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
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

/** Video Decoding Pipeline. */
public class VDP {
    
    private static final Logger LOG = Logger.getLogger(VDP.class.getName());

    public interface IBitstreamListener {
        void bitstream(byte[] abBitstream, int iSize, int iFrameNumber, int iFrameEndSector) throws IOException;
        void setLog(Logger log);
    }

    public static class Bitstream2File implements IBitstreamListener {

        private final File _outFileStrFormat;
        private final String _sFormat;
        private Logger _log = LOG;

        /**
         * @param outFileStrFormat File path with the name optionally having one
         *        {@link String#format(java.lang.String, java.lang.Object[])}
         *        formatted {@code %d} for the frame number.
         */
        public Bitstream2File(File outFileStrFormat) {
            _outFileStrFormat = outFileStrFormat.getParentFile();
            _sFormat = outFileStrFormat.getName();
        }

        public void bitstream(byte[] abBitstream, int iSize, int iFrameNumber, int iFrameEndSector) throws IOException {
            File f = new File(_outFileStrFormat, String.format(_sFormat, iFrameNumber));
            IO.makeDirsForFile(f);
            FileOutputStream fos = new FileOutputStream(f);
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

        public void setLog(Logger log) {
            _log = log == null ? LOG : log;
        }
    }

    public static class Bitstream2Mdec implements IBitstreamListener {

        private Logger _log = LOG;
        private BitStreamUncompressor _uncompressor;
        private IMdecListener _listener;

        private BitStreamUncompressor identify(byte[] abBitstream) {
            BitStreamUncompressor uncompressor = BitStreamUncompressor.identifyUncompressor(abBitstream);
            if (uncompressor != null) {
                try {
                    uncompressor.reset(abBitstream);
                    _log.info("Video format identified as " + uncompressor.getName());
                } catch (NotThisTypeException ex) {
                    uncompressor = null;
                }
            }
            return uncompressor;
        }
        private BitStreamUncompressor resetUncompressor(byte[] abBitstream) {
            if (_uncompressor == null) {
                _uncompressor = identify(abBitstream);
            } else {
                try {
                    _uncompressor.reset(abBitstream);
                } catch (NotThisTypeException ex) {
                    _uncompressor = identify(abBitstream);
                }
            }
            return _uncompressor;
        }

        public void bitstream(byte[] abBitstream, int iSize, int iFrameNumber, int iFrameEndSector) throws IOException {
            resetUncompressor(abBitstream);
            if (_uncompressor == null) {
                _log.severe("Error with frame " + iFrameNumber + ": Unable to determine frame type.");
                _listener.error("Unable to determine frame type", iFrameNumber, iFrameEndSector);
            } else
                _listener.mdec(_uncompressor, iFrameNumber, iFrameEndSector);
        }

        public void setMdec(IMdecListener listener) {
            _listener = listener;
        }

        public void setLog(Logger log) {
            _log = log == null ? LOG : log;
            _listener.setLog(log);
        }
    }

    public interface IMdecListener {
        void mdec(MdecInputStream mdecIn, int iFrameNumber, int iFrameEndSector) throws IOException;
        void error(String sErr, int iFrameNumber, int iFrameEndSector) throws IOException;
        void setLog(Logger log);
    }
    
    public static class Mdec2File implements IMdecListener {

        private final File _outFileStrFormat;
        private final int _iTotalBlocks;
        private final String _sFormat;
        private Logger _log = LOG;

        /**
         * @param outFileStrFormat File path with the name optionally having one
         *        {@link String#format(java.lang.String, java.lang.Object[])}
         *        formatted {@code %d} for the frame number.
         */
        public Mdec2File(File outFileStrFormat, int iWidth, int iHeight) {
            _iTotalBlocks = Calc.blocks(iHeight, iWidth);
            _outFileStrFormat = outFileStrFormat.getParentFile();
            _sFormat = outFileStrFormat.getName();
        }
        
        public void mdec(MdecInputStream mdecIn, int iFrameNumber, int _) throws IOException {
            File f = new File(_outFileStrFormat, String.format(_sFormat, iFrameNumber));
            IO.makeDirsForFile(f);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(f));
                try {
                    MdecInputStreamReader.writeMdecBlocks(mdecIn, bos, _iTotalBlocks);
                } catch (MdecException ex) {
                    _log.log(Level.WARNING, "Error uncompressing frame " + iFrameNumber, ex);
                }
            } catch (IOException ex) {
                _log.log(Level.WARNING, "Error writing frame " + iFrameNumber, ex);
            } finally {
                if (bos != null) try {
                    bos.close();
                } catch (IOException ex) {
                    _log.log(Level.WARNING, "Error closing file for frame " + iFrameNumber, ex);
                }
            }
        }

        public void error(String sErr, int iFrameNumber, int iFrameEndSector) {
            // sender already logged it
        }

        public void setLog(Logger log) {
            _log = log == null ? LOG : log;
        }

    }

    
    public static class Mdec2Jpeg implements IMdecListener {

        private final File _outFileStrFormat;
        private final jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg _jpegTranslator;
        private final ExposedBAOS _buffer = new ExposedBAOS();
        private Logger _log = LOG;

        private final String _sFormat;

        /**
         * @param outFileStrFormat File path with the name optionally having one
         *        {@link String#format(java.lang.String, java.lang.Object[])}
         *        formatted {@code %d} for the frame number.
         */
        public Mdec2Jpeg(File outFileStrFormat, int iWidth, int iHeight) {
            _outFileStrFormat = outFileStrFormat.getParentFile();
            _sFormat = outFileStrFormat.getName();
            _jpegTranslator = new jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg(iWidth, iHeight);
        }

        public void mdec(MdecInputStream mdecIn, int iFrameNumber, int iFrameEndSector) throws IOException {
            File f = new File(_outFileStrFormat, String.format(_sFormat, iFrameNumber));
            IO.makeDirsForFile(f);
            try {
                _jpegTranslator.readMdec(mdecIn);
            } catch (MdecException.Decode ex) {
                _log.log(Level.WARNING, "Error uncompressing frame " + iFrameNumber, ex);
            }
            _buffer.reset();
            _jpegTranslator.writeJpeg(_buffer);
            IO.writeFile(f, _buffer.getBuffer(), 0, _buffer.size());
        }

        public void error(String sErr, int iFrameNumber, int iFrameEndSector) {
            // sender already logged it
        }

        public void setLog(Logger log) {
            _log = log == null ? LOG : log;
        }
        
    }


    public static class Mdec2Decoded implements IMdecListener {

        private final MdecDecoder _decoder;
        private IDecodedListener _listener;
        private Logger _log = LOG;

        public Mdec2Decoded(MdecDecoder decoder) {
            _decoder = decoder;
        }

        public void mdec(MdecInputStream mdecIn, int iFrameNumber, int iFrameEndSector) throws IOException {
            try {
                _decoder.decode(mdecIn);
            } catch (MdecException.Decode ex) {
                _log.log(Level.WARNING, "Error uncompressing frame " + iFrameNumber, ex);
            }
            _listener.decoded(_decoder, iFrameNumber, iFrameEndSector);
        }

        public void error(String sErr, int iFrameNumber, int iFrameEndSector) throws IOException {
            _listener.error(sErr, iFrameNumber, iFrameEndSector);
        }

        public void setDecoded(IDecodedListener decoded) {
            if (decoded == null)
                return;
            decoded.assertAcceptsDecoded(_decoder);
            _listener = decoded;
        }

        public void setLog(Logger log) {
            _log = log == null ? LOG : log;
            _listener.setLog(log);
        }
        
    }

    public interface IDecodedListener {
        void decoded(MdecDecoder decoder, int iFrameNumber, int iFrameEndSector) throws IOException;
        void error(String sErr, int iFrameNumber, int iFrameEndSector) throws IOException;
        void setLog(Logger log);
        void assertAcceptsDecoded(MdecDecoder decoder);
    }

    public static class Decoded2JavaImage implements IDecodedListener {
        
        private final File _outDileStrFormat;
        private final String _sFmt;
        private final BufferedImage _rgbImg;
        private Logger _log = LOG;

        private final String _sFormat;

        /**
         * @param outFileStrFormat File path with the name optionally having one
         *        {@link String#format(java.lang.String, java.lang.Object[])}
         *        formatted {@code %d} for the frame number.
         */
        public Decoded2JavaImage(File outFileStrFormat, JavaImageFormat eFmt, int iWidth, int iHeight) {
            _outDileStrFormat = outFileStrFormat.getParentFile();
            _sFmt = eFmt.getId();
            _rgbImg = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
            _sFormat = outFileStrFormat.getName();
        }
        
        public void decoded(MdecDecoder decoder, int iFrameNumber, int iFrameEndSector) throws IOException {
            decoder.readDecodedRgb(_rgbImg.getWidth(), _rgbImg.getHeight(),
                    ((DataBufferInt)_rgbImg.getRaster().getDataBuffer()).getData());
            File f = new File(_outDileStrFormat, String.format(_sFormat, iFrameNumber));
            IO.makeDirsForFile(f);
            try {
                if (!ImageIO.write(_rgbImg, _sFmt, f)) {
                    _log.log(Level.WARNING, "Unable to write frame file " + f);
                }
            } catch (IOException ex) {
                _log.log(Level.WARNING, "Error writing frame file " + f, ex);
            }
        }

        public void error(String sErr, int iFrameNumber, int iFrameEndSector) {
            // sender already logged it
        }

        public void assertAcceptsDecoded(MdecDecoder decoder) {
        }

        public void setLog(Logger log) {
            _log = log == null ? LOG : log;
        }
        
    }

    // ########################################################################
    // ########################################################################
    // ########################################################################
    // ########################################################################


    public static abstract class ToAvi implements ISectorAudioDecoder.ISectorTimedAudioWriter {
        protected final File _outputFile;
        protected final int _iWidth, _iHeight;
        protected final VideoSync _vidSync;
        private final AudioVideoSync _avSync;
        protected final AudioFormat _af;
        protected Logger _log = LOG;
        protected AviWriter _writer;

        public ToAvi(File outputFile, int iWidth, int iHeight, VideoSync vidSync) {
            _outputFile = outputFile;
            _iWidth = iWidth; _iHeight = iHeight;
            _vidSync = vidSync; _avSync =  null;
            _af = null;
        }

        public ToAvi(File outputFile, int iWidth, int iHeight, AudioVideoSync avSync, AudioFormat af) {
            _outputFile = outputFile;
            _iWidth = iWidth; _iHeight = iHeight;
            _vidSync = _avSync = avSync;
            _af = af;
        }

        public File getOutputFile() {
            return _outputFile;
        }

        abstract public void open() throws IOException;

        abstract public void error(String sErr, int iFrameNumber, int iFrameEndSector) throws IOException;

        final protected void prepForFrame(int iFrameNumber, int iFrameEndSector) throws IOException {
            if (_writer == null)
                throw new IllegalStateException("Avi writer is not open");

            // if first frame
            if (_writer.getVideoFramesWritten() < 1 && _vidSync.getInitialVideo() > 0) {

                _log.log(Level.INFO, "Writing " + _vidSync.getInitialVideo() + " blank frame(s) to align audio/video playback.");
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
                _log.log(Level.WARNING, "Frame "+iFrameNumber+" is ahead of reading by " + (-iDupCount) + " frame(s).");
            } else while (iDupCount > 0) { // could happen with first frame
                if (_writer.getVideoFramesWritten() < 1) // TODO: fix design so this isn't needed
                    _writer.writeBlankFrame();
                else
                    _writer.repeatPreviousFrame();
                iDupCount--;
            }
        }

        final public void write(AudioFormat format, byte[] abData, int iStart, int iLen, int iPresentationSector) throws IOException {
            if (_writer == null)
                throw new IllegalStateException("Avi writer is not open");

            if (_writer.getAudioSamplesWritten() < 1 &&
                _avSync.getInitialAudio() > 0)
            {
                _log.log(Level.INFO, "Writing " + _avSync.getInitialAudio() + " samples of silence to align audio/video playback.");
                _writer.writeSilentSamples(_avSync.getInitialAudio());
            }
            long lngNeededSilence = _avSync.calculateAudioToCatchUp(iPresentationSector, _writer.getAudioSamplesWritten());
            if (lngNeededSilence > 0) {
                _log.log(Level.INFO, "Adding " + lngNeededSilence + " samples to keep audio in sync.");
                _writer.writeSilentSamples(lngNeededSilence);
            }

            _writer.writeAudio(abData, iStart, iLen);
        }

        public void close() throws IOException {
            if (_writer != null) {
                _writer.close();
            }
        }

        public void setLog(Logger log) {
            _log = log == null ? LOG : log;
        }
        
    }

    static class Decoded2RgbAvi extends ToAvi implements IDecodedListener {
        
        private AviWriterDIB _writerDib;
        private int[] _aiImageBuf;

        public Decoded2RgbAvi(File outputFile, int iWidth, int iHeight, VideoSync vidSync) {
            super(outputFile, iWidth, iHeight, vidSync);
        }

        public Decoded2RgbAvi(File outputFile, int iWidth, int iHeight, AudioVideoSync avSync, AudioFormat af) {
            super(outputFile, iWidth, iHeight, avSync, af);
        }

        public void assertAcceptsDecoded(MdecDecoder decoder) {
        }

        public void open() throws IOException {
            if (_writer == null) {
                IO.makeDirsForFile(_outputFile);
                _writer = _writerDib = new AviWriterDIB(_outputFile,
                                                        _iWidth, _iHeight,
                                                        _vidSync.getFpsNum(),
                                                        _vidSync.getFpsDenom(),
                                                        _af);
                _aiImageBuf = new int[_iWidth*_iHeight];
            }
        }

        public void decoded(MdecDecoder decoder, int iFrameNumber, int iFrameEndSector) throws IOException {
            prepForFrame(iFrameNumber, iFrameEndSector);
            decoder.readDecodedRgb(_writerDib.getWidth(), _writerDib.getHeight(), _aiImageBuf);
            _writerDib.writeFrameRGB(_aiImageBuf, 0, _writerDib.getWidth());
        }

        public void error(String sErr, int iFrameNumber, int iFrameEndSector) throws IOException {
            prepForFrame(iFrameNumber, iFrameEndSector);
            BufferedImage bi = makeErrorImage(sErr, _writerDib.getWidth(), _writerDib.getHeight());
            RgbIntImage rgb = new RgbIntImage(bi);
            _writerDib.writeFrameRGB(rgb.getData(), 0, _writerDib.getWidth());
        }

    }

    static class Decoded2YuvAvi extends ToAvi implements IDecodedListener {

        protected YCbCrImage _yuvImgBuff;
        protected AviWriterYV12 _writerYuv;

        public Decoded2YuvAvi(File outputFile, int iWidth, int iHeight, VideoSync vidSync) {
            super(outputFile, iWidth, iHeight, vidSync);
        }

        public Decoded2YuvAvi(File outputFile, int iWidth, int iHeight, AudioVideoSync avSync, AudioFormat af) {
            super(outputFile, iWidth, iHeight, avSync, af);
        }

        public void assertAcceptsDecoded(MdecDecoder decoder) {
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
                _yuvImgBuff = new YCbCrImage(_iWidth, _iHeight);
            }
        }

        public void decoded(MdecDecoder decoder, int iFrameNumber, int iFrameEndSector) throws IOException {
            prepForFrame(iFrameNumber, iFrameEndSector);
            ((MdecDecoder_double)decoder).readDecoded_Rec601_YCbCr420(_yuvImgBuff);
            _writerYuv.write(_yuvImgBuff.getY(), _yuvImgBuff.getCb(), _yuvImgBuff.getCr());
        }

        public void error(String sErr, int iFrameNumber, int iFrameEndSector) throws IOException {
            prepForFrame(iFrameNumber, iFrameEndSector);
            // TODO: write error with proper sample range
            BufferedImage bi = makeErrorImage(sErr, _writerYuv.getWidth(), _writerYuv.getHeight());
            YCbCrImage yuv = new YCbCrImage(bi);
            _writerYuv.write(yuv.getY(), yuv.getCb(), yuv.getCr());
        }

    }


    static class Decoded2JYuvAvi extends Decoded2YuvAvi {

        public Decoded2JYuvAvi(File outputFile, int iWidth, int iHeight, AudioVideoSync avSync, AudioFormat af) {
            super(outputFile, iWidth, iHeight, avSync, af);
        }

        public Decoded2JYuvAvi(File outputFile, int iWidth, int iHeight, VideoSync vidSync) {
            super(outputFile, iWidth, iHeight, vidSync);
        }

        @Override
        public void decoded(MdecDecoder decoder, int iFrameNumber, int iFrameEndSector) throws IOException {
            prepForFrame(iFrameNumber, iFrameEndSector);
            ((MdecDecoder_double)decoder).readDecoded_JFIF_YCbCr420(_yuvImgBuff);
            _writerYuv.write(_yuvImgBuff.getY(), _yuvImgBuff.getCb(), _yuvImgBuff.getCr());
        }
        
    }


    public static class Mdec2MjpegAvi extends ToAvi implements IMdecListener {

        private final jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg _jpegTranslator;
        private final ExposedBAOS _buffer = new ExposedBAOS();
        private AviWriterMJPG _mjpegWriter;

        public Mdec2MjpegAvi(File outputFile, int iWidth, int iHeight, VideoSync vidSync) {
            super(outputFile, iWidth, iHeight, vidSync);
            _jpegTranslator = new jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg(iWidth, iHeight);
        }

        public Mdec2MjpegAvi(File outputFile, int iWidth, int iHeight, AudioVideoSync avSync, AudioFormat af) {
            super(outputFile, iWidth, iHeight, avSync, af);
            _jpegTranslator = new jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg(iWidth, iHeight);
        }

        public void mdec(MdecInputStream mdecIn, int iFrameNumber, int iFrameEndSector) throws IOException {
            try {
                _jpegTranslator.readMdec(mdecIn);
            } catch (MdecException.Decode ex) {
                _log.log(Level.WARNING, "Error uncompressing frame " + iFrameNumber, ex);
            }
            _buffer.reset();
            _jpegTranslator.writeJpeg(_buffer);
            prepForFrame(iFrameNumber, iFrameEndSector);
            _mjpegWriter.writeFrame(_buffer.getBuffer(), 0, _buffer.size());
        }


        public void open() throws IOException {
            if (_writer == null) {
                IO.makeDirsForFile(_outputFile);
                _writer = _mjpegWriter = new AviWriterMJPG
                        (_outputFile, _iWidth, _iHeight, _vidSync.getFpsNum(), _vidSync.getFpsDenom(), _af);
            }
        }

        public File getOutputFile() {
            return _outputFile;
        }

        public void error(String sErr, int iFrameNumber, int iFrameEndSector) throws IOException {
            prepForFrame(iFrameNumber, iFrameEndSector);
            _mjpegWriter.writeFrame(makeErrorImage(sErr, _iWidth, _iHeight));
        }

    }

    
    /** Draw the error onto a blank image */
    private static BufferedImage makeErrorImage(String sErr, int iWidth, int iHeight) {
        BufferedImage bi = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.white);
        g.drawString(sErr, 5, 20);
        g.dispose();
        return bi;
    }
}
