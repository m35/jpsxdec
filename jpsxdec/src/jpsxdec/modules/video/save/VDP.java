/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.formats.Pc601YCbCrImage;
import jpsxdec.formats.Rec601YCbCrImage;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedFileNotFoundException;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.audio.DecodedAudioPacket;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.IBitStreamUncompressor;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
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
 *                                        +-> Decoded (Mdec2Decoded) -+-> JavaImage (Decoded2JavaImage)
 *                                                                    |
 *                                                                    +-> RgbAvi, YuvAvi, JYuvAvi (Decoded2...)
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
                IO.makeDirsForFile(f);
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
                _log.log(Level.SEVERE, FrameMessage.FRAME_WRITE_ERR(f, frameNumber), ex);
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
                ILocalizedMessage msg = FrameMessage.UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(frameNumber);
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
                IO.makeDirsForFile(f);
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
                    _log.log(Level.SEVERE, FrameMessage.FRAME_NUM_CORRUPTED(frameNumber), ex);
                } catch (MdecException.EndOfStream ex) {
                    _log.log(Level.SEVERE, FrameMessage.FRAME_NUM_INCOMPLETE(frameNumber), ex);
                }
            } catch (FileNotFoundException ex) {
                _log.log(Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(f.toString()), ex);
            } catch (IOException ex) {
                _log.log(Level.SEVERE, FrameMessage.FRAME_WRITE_ERR(f, frameNumber), ex);
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
                IO.makeDirsForFile(f);
            } catch (LocalizedFileNotFoundException ex) {
                _log.log(Level.SEVERE, ex.getSourceMessage(), ex);
                return; // just skip the file without failing
            }

            try {
                _jpegTranslator.readMdec(mdecIn);
            } catch (MdecException.TooMuchEnergy ex) {
                _log.log(Level.WARNING, FrameMessage.JPEG_ENCODER_FRAME_FAIL(frameNumber), ex);
                return; // just skip the file without failing
            } catch (MdecException.ReadCorruption ex) {
                _log.log(Level.WARNING, FrameMessage.FRAME_NUM_CORRUPTED(frameNumber), ex);
                return; // just skip the file without failing
            } catch (MdecException.EndOfStream ex) {
                _log.log(Level.WARNING, FrameMessage.FRAME_NUM_INCOMPLETE(frameNumber), ex);
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
                _log.log(Level.WARNING, FrameMessage.FRAME_WRITE_ERR(f, frameNumber), ex);
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
                _log.log(Level.SEVERE, FrameMessage.FRAME_NUM_CORRUPTED(frameNumber), ex);
            } catch (MdecException.EndOfStream ex) {
                _log.log(Level.SEVERE, FrameMessage.FRAME_NUM_INCOMPLETE(frameNumber), ex);
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
                IO.makeDirsForFile(f);
            } catch (LocalizedFileNotFoundException ex) {
                _log.log(Level.SEVERE, ex.getSourceMessage(), ex);
                return;
            }

            try {
                if (ImageIO.write(_rgbImg, _sImageIOid, f)) {
                    if (_fileGenListener != null)
                        _fileGenListener.fileGenerated(f);
                } else {
                    _log.log(Level.WARNING, FrameMessage.FRAME_FILE_WRITE_UNABLE(f, frameNumber));
                }
            } catch (IOException ex) {
                _log.log(Level.WARNING, FrameMessage.FRAME_WRITE_ERR(f, frameNumber), ex);
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

    // ########################################################################
    // ########################################################################
    // ########################################################################
    // ########################################################################

    public static abstract class ToVideo implements Closeable, DecodedAudioPacket.Listener, IFileGenerator  {
        @Nonnull
        protected final File _outputFile;
        @CheckForNull
        private GeneratedFileListener _fileGenListener;

        public ToVideo(@Nonnull File outputFile) {
            _outputFile = outputFile;
        }

        final public @Nonnull File getOutputFile() {
            return _outputFile;
        }

        @Override
        final public void setGenFileListener(@CheckForNull GeneratedFileListener listener) {
            _fileGenListener = listener;
        }

        protected void fileGenerated(@Nonnull File file) {
            if (_fileGenListener != null)
                _fileGenListener.fileGenerated(file);
        }

        abstract public void open() throws LocalizedFileNotFoundException, FileNotFoundException, IOException;
    }

    /** Most Avi will take Decoded as input, but MJPG will need Mdec as input,
     *  so save the interface implementation for subclasses. */
    public static abstract class ToAvi extends ToVideo {
        protected final int _iWidth, _iHeight;
        @Nonnull
        protected final VideoSync _vidSync;
        @CheckForNull
        private final AudioVideoSync _avSync;
        @CheckForNull
        protected final AudioFormat _af;
        @Nonnull
        protected final ILocalizedLogger _log;
        @CheckForNull
        protected AviWriter _writer;

        /** Video without audio. */
        public ToAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync, @Nonnull ILocalizedLogger log) {
            super(outputFile);
            _iWidth = iWidth; _iHeight = iHeight;
            _vidSync = vidSync; _avSync =  null;
            _af = null;
            _log = log;
        }

        /** Video with audio. */
        public ToAvi(@Nonnull File outputFile, int iWidth, int iHeight,
                     @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af, @Nonnull ILocalizedLogger log)
        {
            super(outputFile);
            _iWidth = iWidth; _iHeight = iHeight;
            _vidSync = _avSync = avSync;
            _af = af;
            _log = log;
        }

        final public @CheckForNull AviWriter getAviWriter() {
            return _writer;
        }

        // subclasses will implement IDecodedListener or IMdecListener to match this
        abstract public void error(@Nonnull ILocalizedMessage sErr, @CheckForNull FormattedFrameNumber frameNumber,
                                   @Nonnull Fraction presentationSector) throws IOWritingException;

        final protected void prepForFrame(@CheckForNull FormattedFrameNumber frameNumber,
                                          @Nonnull Fraction presentationSector)
                throws IOException
        {
            if (_writer == null)
                throw new IllegalStateException("Avi writer is not open");

            // if first frame
            if (_writer.getVideoFramesWritten() < 1 && _vidSync.getInitialVideo() > 0) {

                _log.log(Level.INFO, I.WRITING_BLANK_FRAMES_TO_ALIGN_AV(_vidSync.getInitialVideo()));
                _writer.writeBlankFrame();
                for (int i = _vidSync.getInitialVideo()-1; i > 0; i--) {
                    _writer.repeatPreviousFrame();
                }

            }

            int iDupCount = _vidSync.calculateFramesToCatchUp(
                                        presentationSector,
                                        _writer.getVideoFramesWritten());

            if (iDupCount < 0) {
                // this does happen on occasion:
                // * A few frames get off pretty bad from fps
                // * Frames end early (like iki) so presentation sector is
                //   pretty off (but frame is ok)
                // TODO: fix when frame ends early
                // i.e. have a range of presentation sectors (ug)
                _log.log(Level.WARNING, FrameMessage.FRAME_NUM_AHEAD_OF_READING(frameNumber, -iDupCount));
            } else {
                while (iDupCount > 0) { // could happen with first frame
                    if (_writer.getVideoFramesWritten() < 1) { // TODO: fix design so this isn't needed
                        _log.log(Level.INFO, I.WRITING_BLANK_FRAMES_TO_ALIGN_AV(1));
                        LOG.log(Level.INFO, "Writing blank frame for frame {0}", frameNumber);
                        _writer.writeBlankFrame();
                    } else {
                        _log.log(Level.INFO, I.WRITING_DUP_FRAMES_TO_ALIGN_AV(1));
                        LOG.log(Level.INFO, "Writing dup frame for frame {0}", frameNumber);
                        _writer.repeatPreviousFrame();
                    }
                    iDupCount--;
                }
            }
        }

        @Override
        final public void audioPacketComplete(@Nonnull DecodedAudioPacket packet,
                                              @Nonnull ILocalizedLogger log)
                throws LoggedFailure
        {
            if (_writer == null)
                throw new IllegalStateException("Avi writer is not open");

            // _avSync should not be null if this method is called
            try {
                if (_writer.getAudioSampleFramesWritten() < 1 &&
                    _avSync.getInitialAudio() > 0)
                {
                    _log.log(Level.INFO, I.WRITING_SILECE_TO_SYNC_AV(_avSync.getInitialAudio()));
                    _writer.writeSilentSamples(_avSync.getInitialAudio());
                }
                long lngNeededSilence = _avSync.calculateAudioToCatchUp(packet.getPresentationSector(), _writer.getAudioSampleFramesWritten());
                if (lngNeededSilence > 0) {
                    _log.log(Level.INFO, I.WRITING_SILENCE_TO_KEEP_AV_SYNCED(lngNeededSilence));
                    _writer.writeSilentSamples(lngNeededSilence);
                }

                byte[] abData = packet.getData();
                _writer.writeAudio(abData, 0, abData.length);
            } catch (IOException ex) {
                throw new LoggedFailure(_log, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(_writer.getFile().toString()), ex);
            }
        }

        @Override
        public void close() throws IOException {
            if (_writer != null) {
                _writer.close();
            }
        }
    }

    public static class Decoded2RgbAvi extends ToAvi implements IDecodedListener {
        @CheckForNull
        private AviWriterDIB _writerDib;
        @CheckForNull
        private int[] _aiImageBuf;

        public Decoded2RgbAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync, @Nonnull ILocalizedLogger log) {
            super(outputFile, iWidth, iHeight, vidSync, log);
        }

        public Decoded2RgbAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af, @Nonnull ILocalizedLogger log) {
            super(outputFile, iWidth, iHeight, avSync, af, log);
        }

        @Override
        public void assertAcceptsDecoded(@Nonnull MdecDecoder decoder) {}

        @Override
        public void open()
                throws LocalizedFileNotFoundException, FileNotFoundException, IOException
        {
            if (_writer == null) {
                makeDirsForFile(_outputFile);
                _writer = _writerDib = new AviWriterDIB(_outputFile,
                                                        _iWidth, _iHeight,
                                                        _vidSync.getFpsNum(),
                                                        _vidSync.getFpsDenom(),
                                                        _af);
                fileGenerated(_outputFile);
                _aiImageBuf = new int[_iWidth*_iHeight];
            }
        }

        @Override
        public void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FormattedFrameNumber frameNumber,
                            @Nonnull Fraction presentationSector)
                throws IOWritingException
        {
            if (_writerDib == null)
                throw new IllegalStateException("AVI not open.");
            decoder.readDecodedRgb(_writerDib.getWidth(), _writerDib.getHeight(), _aiImageBuf);
            try {
                prepForFrame(frameNumber, presentationSector);
                _writerDib.writeFrameRGB(_aiImageBuf, 0, _writerDib.getWidth());
            } catch (IOException ex) {
                throw new IOWritingException(ex, _writer.getFile());
            }
        }

        @Override
        public void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber,
                          @Nonnull Fraction presentationSector)
                throws IOWritingException
        {
            if (_writerDib == null)
                throw new IllegalStateException("AVI not open.");
            BufferedImage bi = makeErrorImage(errMsg, _writerDib.getWidth(), _writerDib.getHeight());
            RgbIntImage rgb = new RgbIntImage(bi);
            try {
                prepForFrame(frameNumber, presentationSector);
                _writerDib.writeFrameRGB(rgb.getData(), 0, _writerDib.getWidth());
            } catch (IOException ex) {
                throw new IOWritingException(ex, _writer.getFile());
            }
        }

    }

    /** Only supports videos with even dimensions. */
    public static class Decoded2YuvAvi extends ToAvi implements IDecodedListener {
        @CheckForNull
        private Rec601YCbCrImage _yuvImgBuff;
        @CheckForNull
        protected AviWriterYV12 _writerYuv;

        /** @throws IllegalArgumentException if dimensions are not even */
        public Decoded2YuvAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync, @Nonnull ILocalizedLogger log) {
            super(outputFile, iWidth, iHeight, vidSync, log);
            if (((iWidth | iHeight) & 1) != 0)
                throw new IllegalArgumentException("YUV AVI only supports even dimensions");
        }

        /** @throws IllegalArgumentException if dimensions are not even */
        public Decoded2YuvAvi(@Nonnull File outputFile, int iWidth, int iHeight,
                              @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af, @Nonnull ILocalizedLogger log)
        {
            super(outputFile, iWidth, iHeight, avSync, af, log);
            if (((iWidth | iHeight) & 1) != 0)
                throw new IllegalArgumentException("YUV AVI only supports even dimensions");
        }

        @Override
        public void assertAcceptsDecoded(@Nonnull MdecDecoder decoder) throws IllegalArgumentException {
            if (!(decoder instanceof MdecDecoder_double))
                throw new IllegalArgumentException(getClass().getName() + " can't handle " + decoder.getClass().getName());
        }

        @Override
        public void open()
                throws LocalizedFileNotFoundException, FileNotFoundException, IOException
        {
            if (_writer == null) {
                IO.makeDirsForFile(_outputFile);
                _writer = _writerYuv = new AviWriterYV12(_outputFile,
                                                         _iWidth, _iHeight,
                                                         _vidSync.getFpsNum(),
                                                         _vidSync.getFpsDenom(),
                                                         _af);
                fileGenerated(_outputFile);
                _yuvImgBuff = new Rec601YCbCrImage(_iWidth, _iHeight);
            }
        }

        @Override
        public void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FormattedFrameNumber frameNumber,
                            @Nonnull Fraction presentationSector)
                throws IOWritingException
        {
            if (_writerYuv == null)
                throw new IllegalStateException("AVI not open.");
            // only accepts MdecDecoder_double, verified in assertAcceptsDecoded()
            ((MdecDecoder_double)decoder).readDecoded_Rec601_YCbCr420(_yuvImgBuff);
            try {
                prepForFrame(frameNumber, presentationSector);
                // if this happens for the JYUV subclass, the brightness will be off
                // but it's an error image so doesn't really matter
                _writerYuv.write(_yuvImgBuff.getYBuff(), _yuvImgBuff.getCbBuff(), _yuvImgBuff.getCrBuff());
            } catch (IOException ex) {
                throw new IOWritingException(ex, _writerYuv.getFile());
            }
        }

        @Override
        public void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber,
                          @Nonnull Fraction presentationSector)
                throws IOWritingException
        {
            if (_writerYuv == null)
                throw new IllegalStateException("AVI not open.");
            // TODO: write error with proper sample range
            BufferedImage bi = makeErrorImage(errMsg, _writerYuv.getWidth(), _writerYuv.getHeight());
            try {
                prepForFrame(frameNumber, presentationSector);
                Rec601YCbCrImage yuv = new Rec601YCbCrImage(bi);
                _writerYuv.write(yuv.getYBuff(), yuv.getCbBuff(), yuv.getCrBuff());
            } catch (IOException ex) {
                throw new IOWritingException(ex, _writer.getFile());
            }
        }

    }


    public static class Decoded2JYuvAvi extends Decoded2YuvAvi {

        @CheckForNull
        protected Pc601YCbCrImage _yuvImgBuff;

        public Decoded2JYuvAvi(@Nonnull File outputFile, int iWidth, int iHeight,
                               @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af, @Nonnull ILocalizedLogger log)
        {
            super(outputFile, iWidth, iHeight, avSync, af, log);
        }

        public Decoded2JYuvAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync, @Nonnull ILocalizedLogger log) {
            super(outputFile, iWidth, iHeight, vidSync, log);
        }

        @Override
        public void open() throws LocalizedFileNotFoundException, FileNotFoundException, IOException {
            super.open();
            _yuvImgBuff = new Pc601YCbCrImage(_iWidth, _iHeight);
        }

        @Override
        public void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FormattedFrameNumber frameNumber,
                            @Nonnull Fraction presentationSector)
                throws IOWritingException
        {
            if (_writerYuv == null)
                throw new IllegalStateException("AVI not open.");
            // only accepts MdecDecoder_double, verified in assertAcceptsDecoded()
            ((MdecDecoder_double)decoder).readDecoded_JFIF_YCbCr420(_yuvImgBuff);
            try {
                prepForFrame(frameNumber, presentationSector);
                _writerYuv.write(_yuvImgBuff.getYBuff(), _yuvImgBuff.getCbBuff(), _yuvImgBuff.getCrBuff());
            } catch (IOException ex) {
                throw new IOWritingException(ex, _writer.getFile());
            }
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

        public Mdec2MjpegAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull VideoSync vidSync, @Nonnull ILocalizedLogger log) {
            super(outputFile, iWidth, iHeight, vidSync, log);
            _jpegTranslator = new jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg(iWidth, iHeight);
        }

        public Mdec2MjpegAvi(@Nonnull File outputFile, int iWidth, int iHeight, @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af, @Nonnull ILocalizedLogger log) {
            super(outputFile, iWidth, iHeight, avSync, af, log);
            _jpegTranslator = new jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg(iWidth, iHeight);
        }

        @Override
        public void open()
                throws LocalizedFileNotFoundException, FileNotFoundException, IOException
        {
            if (_writer == null) {
                IO.makeDirsForFile(_outputFile);
                _writer = _mjpegWriter = new AviWriterMJPG(_outputFile, _iWidth, _iHeight, _vidSync.getFpsNum(), _vidSync.getFpsDenom(), _af);
                fileGenerated(_outputFile);
            }
        }

        @Override
        public void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FormattedFrameNumber frameNumber,
                         @Nonnull Fraction presentationSector)
                throws IOWritingException
        {
            if (_mjpegWriter == null)
                throw new IllegalStateException("AVI not open.");
            ILocalizedMessage err;
            Exception fail;
            try {
                _jpegTranslator.readMdec(mdecIn);
                _buffer.reset();
                try {
                    _jpegTranslator.writeJpeg(_buffer);
                } catch (IOException ex) {
                    throw new RuntimeException("Should not happen", ex);
                }

                try {
                    prepForFrame(frameNumber, presentationSector);
                    _mjpegWriter.writeFrame(_buffer.getBuffer(), 0, _buffer.size());
                } catch (IOException ex) {
                    throw new IOWritingException(ex, _mjpegWriter.getFile());
                }
                return;
                // kinda icky way to do this
            } catch (MdecException.ReadCorruption ex) {
                err = FrameMessage.FRAME_NUM_CORRUPTED(frameNumber);
                fail = ex;
            } catch (MdecException.EndOfStream ex) {
                err = FrameMessage.FRAME_NUM_INCOMPLETE(frameNumber);
                fail = ex;
            } catch (MdecException.TooMuchEnergy ex) {
                err = FrameMessage.JPEG_ENCODER_FRAME_FAIL(frameNumber);
                fail = ex;
            }
            _log.log(Level.WARNING, err, fail);
            error(err, frameNumber, presentationSector);
        }

        @Override
        public void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber,
                          @Nonnull Fraction presentationSector) throws IOWritingException {
            if (_mjpegWriter == null)
                throw new IllegalStateException("AVI not open.");
            try {
                prepForFrame(frameNumber, presentationSector);
                _mjpegWriter.writeFrame(makeErrorImage(errMsg, _iWidth, _iHeight));
            } catch (IOException ex) {
                throw new IOWritingException(ex, _writer.getFile());
            }
        }

        @Override
        public @Nonnull ILocalizedLogger getLog() {
            return _log;
        }

    }


    /** Draw the error onto a blank image. */
    private static @Nonnull BufferedImage makeErrorImage(@Nonnull ILocalizedMessage sErr, int iWidth, int iHeight) {
        BufferedImage bi = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.white);
        g.drawString(sErr.getLocalizedMessage(), 5, 20);
        g.dispose();
        return bi;
    }


    /** Different messages depending on whether the frame number is null or not. */
    private static class FrameMessage {

        private static @Nonnull ILocalizedMessage JPEG_ENCODER_FRAME_FAIL(
                @CheckForNull FormattedFrameNumber frameNumber)
        {
            if (frameNumber == null)
                return I.JPEG_ENCODER_FRAME_FAIL_NO_FRAME();
            else
                return I.JPEG_ENCODER_FRAME_FAIL(frameNumber.toString());
        }

        private static @Nonnull ILocalizedMessage FRAME_NUM_INCOMPLETE(
                @CheckForNull FormattedFrameNumber frameNumber)
        {
            if (frameNumber == null)
                return I.FRAME_INCOMPLETE();
            else
                return I.FRAME_NUM_INCOMPLETE(frameNumber.getUnpaddedValue());
        }

        private static @Nonnull ILocalizedMessage FRAME_NUM_CORRUPTED(
                @CheckForNull FormattedFrameNumber frameNumber)
        {
            if (frameNumber == null)
                return I.FRAME_CORRUPTED();
            else
                return I.FRAME_NUM_CORRUPTED(frameNumber.getUnpaddedValue());
        }

        private static @Nonnull ILocalizedMessage FRAME_NUM_AHEAD_OF_READING(
                @CheckForNull FormattedFrameNumber frameNumber, int iFrameCount)
        {
            if (frameNumber == null)
                return I.FRAME_AHEAD_OF_READING(iFrameCount);
            else
                return I.FRAME_NUM_AHEAD_OF_READING(frameNumber.getUnpaddedValue(), iFrameCount);
        }

        private static @Nonnull ILocalizedMessage FRAME_WRITE_ERR(
                File f, @CheckForNull FormattedFrameNumber frameNumber)
        {
            if (frameNumber == null)
                return I.IO_WRITING_FILE_ERROR_NAME(f.toString());
            else
                return I.FRAME_WRITE_ERR(f, frameNumber.getUnpaddedValue());
        }

        private static @Nonnull ILocalizedMessage FRAME_FILE_WRITE_UNABLE(
                File f, @CheckForNull FormattedFrameNumber frameNumber)
        {
            if (frameNumber == null)
                return I.IO_WRITING_FILE_ERROR_NAME(f.toString());
            else
                return I.FRAME_FILE_WRITE_UNABLE(f.toString(), frameNumber.getUnpaddedValue());
        }

        private static @Nonnull ILocalizedMessage UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(
                @CheckForNull FormattedFrameNumber frameNumber)
        {
            if (frameNumber == null)
                return I.UNABLE_TO_DETERMINE_FRAME_TYPE();
            else
                return I.UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(frameNumber.getUnpaddedValue());
        }

    }
}
