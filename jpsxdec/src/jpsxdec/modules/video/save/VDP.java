/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2026  Michael Sabin
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

package jpsxdec.modules.video.save;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedFileNotFoundException;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.IBitStreamUncompressor;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;

/**
 * "Video Decoding Pipeline".
 *
 * The pipeline is a little complicated since each path is specific about
 * its inputs and outputs. Here are all the possible branches:
 *<pre>
 *  Bitstream
 *    |
 *    +----> File ({@link Bitstream2File})
 *    |
 *    +----> Mdec ({@link Bitstream2Mdec})
 *           |
 *           +----> File ({@link Mdec2File})
 *           |
 *           +----> Jpeg ({@link Mdec2Jpeg})
 *           |
 *           +----> AVI mjpeg ({@link VDPtoVideo} with {@link VideoFormat#AVI_MJPG})
 *           |
 *           +----> Decoded ({@link Mdec2Decoded})
 *                  |
 *                  +----> JavaImage ({@link Decoded2JavaImage})
 *                  |
 *                  +----> Other video formats ({@link VDPtoVideo} with {@link VideoFormat})
 *</pre>
 */
public class VDP {

    public static boolean LOG_STACK_TRACE = true;
    public static boolean MOCK_CREATE_DIRECTORY = false;

    private static void makeDirsForFile(@Nonnull File f) throws LocalizedFileNotFoundException {
        if (!MOCK_CREATE_DIRECTORY)
            IO.makeDirsForFile(f);
    }


    private static final Logger LOG = Logger.getLogger(VDP.class.getName());

    public interface GeneratedFileListener {
        void fileGenerated(@Nonnull File f);
    }
    public interface IFileGenerator {
        void setGenFileListener(@CheckForNull GeneratedFileListener listener);
    }

    public interface IBitstreamListener {
        void bitstream(@Nonnull byte[] abBitstream, int iSize,
                       @CheckForNull FormattedFrameNumber frameNumber,
                       @Nonnull Fraction presentationSector)
                throws IOWritingException;
    }
    public interface IBitstreamProducer {
        void setListener(@CheckForNull VDP.IBitstreamListener bitstreamListener);
    }

    public static class Bitstream2File implements IBitstreamListener, IFileGenerator {

        @Nonnull
        private final VideoFileNameFormatter _formatter;
        @Nonnull
        private final ILocalizedLogger _log;
        @CheckForNull
        private GeneratedFileListener _fileGenListener;

        public Bitstream2File(@Nonnull VideoFileNameFormatter formatter, @Nonnull ILocalizedLogger log) {
            _formatter = formatter;
            _log = log;
        }

        @Override
        public void bitstream(@Nonnull byte[] abBitstream, int iSize,
                              @CheckForNull FormattedFrameNumber frameNumber,
                              @Nonnull Fraction presentationSector)
        {
            File f = _formatter.format(frameNumber, _log);
            try {
                makeDirsForFile(f);
            } catch (LocalizedFileNotFoundException ex) {
                _log.log(Level.SEVERE, ex.getSourceMessage(), ex);
                return;
            }

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                if (_fileGenListener != null)
                    _fileGenListener.fileGenerated(f);
                fos.write(abBitstream, 0, iSize);
            } catch (FileNotFoundException ex) {
                _log.log(Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(f.toString()), ex);
            } catch (IOException ex) {
                _log.log(Level.SEVERE, VDPerrors.FRAME_WRITE_ERR(f, frameNumber), ex);
            } finally {
                IO.closeSilently(fos, LOG);
            }
        }

        @Override
        public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }
    }

    public static class Bitstream2Mdec implements IBitstreamListener, IMdecProducer {

        @CheckForNull
        private IMdecListener _listener;
        @CheckForNull
        private Class<? extends IBitStreamUncompressor> _uncompressorType;

        public Bitstream2Mdec() {
        }
        public Bitstream2Mdec(@Nonnull IMdecListener mdecListener) {
            _listener = mdecListener;
        }

        @Override
        public void setListener(@CheckForNull IMdecListener listener) {
            _listener = listener;
        }

        @Override
        public void bitstream(@Nonnull byte[] abBitstream, int iBitstreamSize,
                              @CheckForNull FormattedFrameNumber frameNumber,
                              @Nonnull Fraction presentationSector)
                throws IOWritingException
        {
            try {
                IBitStreamUncompressor uncompressor = BitStreamUncompressor.identifyUncompressor(
                                                        abBitstream, iBitstreamSize);
                if (_uncompressorType != null) {
                    Class<? extends IBitStreamUncompressor> newType = uncompressor.getClass();
                    if (!_uncompressorType.equals(newType)) {
                        LOG.log(Level.WARNING, "Bitstream format changed from {0} to {1}",
                                new Object[]{_uncompressorType.getSimpleName(), newType.getSimpleName()});
                        _uncompressorType = newType;
                    }
                } else {
                    _uncompressorType = uncompressor.getClass();
                    LOG.log(Level.INFO, "Bitstream format: {0}", _uncompressorType.getSimpleName());
                }
                if (_listener != null)
                    _listener.mdec(uncompressor, frameNumber, presentationSector);
            } catch (BinaryDataNotRecognized ex) {
                ILocalizedMessage msg = VDPerrors.UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(frameNumber);
                if (_listener != null) {
                    _listener.getLog().log(Level.SEVERE, msg, LOG_STACK_TRACE ? ex : null);
                    _listener.error(msg, frameNumber, presentationSector);
                }
            }
        }

    }

    /** Either
     * {@link #mdec(MdecInputStream, FormattedFrameNumber, Fraction)}
     * or
     * {@link #error(ILocalizedMessage, FormattedFrameNumber, Fraction)}
     * will be called for each frame. */
    public interface IMdecListener {
        void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FormattedFrameNumber frameNumber, @Nonnull Fraction presentationSector) throws IOWritingException;
        void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber, @Nonnull Fraction presentationSector) throws IOWritingException;
        @Nonnull ILocalizedLogger getLog();
    }
    public interface IMdecProducer {
        void setListener(@CheckForNull VDP.IMdecListener mdecListener);
    }

    public static class Mdec2File implements IMdecListener, IFileGenerator {

        @Nonnull
        private final VideoFileNameFormatter _formatter;
        private final int _iTotalBlocks;
        @Nonnull
        private final ILocalizedLogger _log;
        @CheckForNull
        private GeneratedFileListener _fileGenListener;

        public Mdec2File(@Nonnull VideoFileNameFormatter formatter, int iWidth, int iHeight, @Nonnull ILocalizedLogger log) {
            _formatter = formatter;
            _iTotalBlocks = Calc.blocks(iWidth, iHeight);
            _log = log;
        }

        @Override
        public void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FormattedFrameNumber frameNumber,
                         @Nonnull Fraction presentationSector_ignored)
        {
            File f = _formatter.format(frameNumber, _log);
            try {
                makeDirsForFile(f);
            } catch (LocalizedFileNotFoundException ex) {
                _log.log(Level.SEVERE, ex.getSourceMessage(), ex);
                return; // just skip the file without failing
            }

            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(f));
                if (_fileGenListener != null)
                    _fileGenListener.fileGenerated(f);
                try {
                    MdecInputStreamReader.writeMdecBlocks(mdecIn, bos, _iTotalBlocks);
                } catch (MdecException.ReadCorruption ex) {
                    _log.log(Level.SEVERE, VDPerrors.FRAME_NUM_CORRUPTED(frameNumber), ex);
                } catch (MdecException.EndOfStream ex) {
                    _log.log(Level.SEVERE, VDPerrors.FRAME_NUM_INCOMPLETE(frameNumber), ex);
                }
            } catch (FileNotFoundException ex) {
                _log.log(Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(f.toString()), ex);
            } catch (IOException ex) {
                _log.log(Level.SEVERE, VDPerrors.FRAME_WRITE_ERR(f, frameNumber), ex);
            } finally {
                IO.closeSilently(bos, LOG);
            }
        }

        @Override
        public void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber,
                          @Nonnull Fraction presentationSector)
        {
            // error frames are simply not written
        }

        @Override
        public @Nonnull ILocalizedLogger getLog() {
            return _log;
        }

        @Override
        public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }
    }


    public static class Mdec2Jpeg implements IMdecListener, IFileGenerator {

        @Nonnull
        private final VideoFileNameFormatter _formatter;
        @Nonnull
        private final jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg _jpegTranslator;
        @Nonnull
        private final ExposedBAOS _buffer = new ExposedBAOS();
        @Nonnull
        private final ILocalizedLogger _log;
        @CheckForNull
        private GeneratedFileListener _fileGenListener;

        public Mdec2Jpeg(@Nonnull VideoFileNameFormatter formatter, int iWidth, int iHeight, @Nonnull ILocalizedLogger log) {
            _formatter = formatter;
            _jpegTranslator = new jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg(iWidth, iHeight);
            _log = log;
        }

        @Override
        public void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FormattedFrameNumber frameNumber,
                         @Nonnull Fraction presentationSector)
        {
            File f = _formatter.format(frameNumber, _log);
            try {
                makeDirsForFile(f);
            } catch (LocalizedFileNotFoundException ex) {
                _log.log(Level.SEVERE, ex.getSourceMessage(), ex);
                return; // just skip the file without failing
            }

            try {
                _jpegTranslator.readMdec(mdecIn);
            } catch (MdecException.TooMuchEnergy ex) {
                _log.log(Level.WARNING, VDPerrors.JPEG_ENCODER_FRAME_FAIL(frameNumber), ex);
                return; // just skip the file without failing
            } catch (MdecException.ReadCorruption ex) {
                _log.log(Level.WARNING, VDPerrors.FRAME_NUM_CORRUPTED(frameNumber), ex);
                return; // just skip the file without failing
            } catch (MdecException.EndOfStream ex) {
                _log.log(Level.WARNING, VDPerrors.FRAME_NUM_INCOMPLETE(frameNumber), ex);
                return; // just skip the file without failing
            }

            _buffer.reset();
            try {
                _jpegTranslator.writeJpeg(_buffer);
            } catch (IOException ex) {
                throw new RuntimeException("Should not happen", ex);
            }

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                if (_fileGenListener != null)
                    _fileGenListener.fileGenerated(f);
                fos.write(_buffer.getBuffer(), 0, _buffer.size());
            } catch (FileNotFoundException ex) {
                _log.log(Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(f.toString()), ex);
            } catch (IOException ex) {
                _log.log(Level.WARNING, VDPerrors.FRAME_WRITE_ERR(f, frameNumber), ex);
            } finally {
                IO.closeSilently(fos, LOG);
            }
        }

        @Override
        public void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber,
                          @Nonnull Fraction presentationSector)
        {
            // error frames are simply not written
        }

        @Override
        public @Nonnull ILocalizedLogger getLog() {
            return _log;
        }

        @Override
        public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }
    }


    public static class Mdec2Decoded implements IMdecListener, IDecodedProducer {

        @Nonnull
        private final MdecDecoder _decoder;
        @Nonnull
        private final ILocalizedLogger _log;
        @CheckForNull
        private IDecodedListener _listener;

        public Mdec2Decoded(@Nonnull MdecDecoder decoder, @Nonnull ILocalizedLogger log) {
            _decoder = decoder;
            _log = log;
        }

        @Override
        public void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FormattedFrameNumber frameNumber,
                         @Nonnull Fraction presentationSector)
                throws IOWritingException
        {
            try {
                _decoder.decode(mdecIn);
            } catch (MdecException.ReadCorruption ex) {
                _log.log(Level.SEVERE, VDPerrors.FRAME_NUM_CORRUPTED(frameNumber), ex);
            } catch (MdecException.EndOfStream ex) {
                _log.log(Level.SEVERE, VDPerrors.FRAME_NUM_INCOMPLETE(frameNumber), ex);
            }
            if (_listener != null)
                _listener.decoded(_decoder, frameNumber, presentationSector);
        }

        @Override
        public void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber,
                          @Nonnull Fraction presentationSector)
                throws IOWritingException
        {
            if (_listener != null)
                _listener.error(errMsg, frameNumber, presentationSector);
        }

        @Override
        public void setDecodedListener(@CheckForNull IDecodedListener decoded) {
            if (decoded == null)
                return;
            decoded.assertAcceptsDecoded(_decoder);
            _listener = decoded;
        }

        @Override
        public @Nonnull ILocalizedLogger getLog() {
            return _log;
        }

    }

    public interface IDecodedListener {
        void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FormattedFrameNumber frameNumber,
                     @Nonnull Fraction presentationSector) throws IOWritingException;
        void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber,
                   @Nonnull Fraction presentationSector) throws IOWritingException;
        void assertAcceptsDecoded(@Nonnull MdecDecoder decoder) throws IllegalArgumentException;
    }
    public interface IDecodedProducer {
        void setDecodedListener(@CheckForNull IDecodedListener decoded);
    }

    public static class Decoded2JavaImage implements IDecodedListener, IFileGenerator {

        @Nonnull
        private final VideoFileNameFormatter _formatter;
        @Nonnull
        private final String _sImageIOid;
        @Nonnull
        private final BufferedImage _rgbImg;
        @Nonnull
        private final ILocalizedLogger _log;
        @CheckForNull
        private GeneratedFileListener _fileGenListener;

        public Decoded2JavaImage(@Nonnull VideoFileNameFormatter formatter, @Nonnull JavaImageFormat eFmt, int iWidth, int iHeight, @Nonnull ILocalizedLogger log) {
            _formatter = formatter;
            _sImageIOid = eFmt.getImageIOid();
            _rgbImg = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
            _log = log;
        }

        @Override
        public void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FormattedFrameNumber frameNumber,
                            @Nonnull Fraction presentationSector)
        {
            decoder.readDecodedRgb(_rgbImg.getWidth(), _rgbImg.getHeight(),
                    ((DataBufferInt)_rgbImg.getRaster().getDataBuffer()).getData());

            File f = _formatter.format(frameNumber, _log);
            try {
                makeDirsForFile(f);
            } catch (LocalizedFileNotFoundException ex) {
                _log.log(Level.SEVERE, ex.getSourceMessage(), ex);
                return;
            }

            try {
                if (ImageIO.write(_rgbImg, _sImageIOid, f)) {
                    if (_fileGenListener != null)
                        _fileGenListener.fileGenerated(f);
                } else {
                    _log.log(Level.WARNING, VDPerrors.FRAME_FILE_WRITE_UNABLE(f, frameNumber));
                }
            } catch (IOException ex) {
                _log.log(Level.WARNING, VDPerrors.FRAME_WRITE_ERR(f, frameNumber), ex);
            }
        }

        @Override
        public void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber,
                          @Nonnull Fraction presentationSector)
        {
            // error frames are simply not written
        }

        @Override
        public void assertAcceptsDecoded(@Nonnull MdecDecoder decoder) {}

        @Override
        public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }
    }

}
