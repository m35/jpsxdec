/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2026  Michael Sabin
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
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
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
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.tojpeg.Mdec2Jpeg;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import jpsxdec.util.aviwriter.AviWriter;
import jpsxdec.util.aviwriter.AviWriterDIB;
import jpsxdec.util.aviwriter.AviWriterMJPG;
import jpsxdec.util.aviwriter.AviWriterYV12;
import jpsxdec.util.mkvwriter.IMkvWriter;
import jpsxdec.util.mkvwriter.MkvJYuvWriter;
import jpsxdec.util.mkvwriter.MkvMjpegWriter;
import jpsxdec.util.mkvwriter.MkvPngWriter;

/**
 * Handles the video branch of the {@link VDP Video Decoding Pipeline}.
 * @see VDP for how this integrates into the larger pipeline.
 */
public class VDPtoVideo implements Closeable, DecodedAudioPacket.Listener,
                                   VDP.IFileGenerator, VDP.IMdecListener, VDP.IDecodedListener
{

    // This top part of the file handles the actual writing to the output file,
    // depending on the video format chosen. These could be made as separate
    // subclasses, but would be a lot of overhead with little benefit.

    private AviWriter _aviWriter;
    private AviWriterDIB _aviDib;
    private AviWriterYV12 _aviYuv;
    private AviWriterMJPG _aviMjpeg;

    private RgbIntImage _rgbImgBuf;
    private Pc601YCbCrImage _pcYuvImgBuff;
    private Rec601YCbCrImage _recYuvImgBuff;

    private IMkvWriter _mkvWriter;
    private MkvPngWriter _mkvPng;
    private MkvJYuvWriter _mkvYuv;
    private MkvMjpegWriter _mkvMjpeg;

    @Override
    public void assertAcceptsDecoded(@Nonnull MdecDecoder decoder) throws IllegalArgumentException {
        if (_videoFormat == VideoFormat.AVI_YUV || _videoFormat == VideoFormat.AVI_JYUV || _videoFormat == VideoFormat.MKV_JYUV) {
            if (!(decoder instanceof MdecDecoder_double))
                throw new IllegalArgumentException(_videoFormat + " requires a " + MdecDecoder_double.class.getName());
        }
    }

    /** Returns the writer that should be closed in {@link #close()}. */
    private Closeable doOpen() throws FileNotFoundException, IOException {
        boolean blnStereo = _af != null && _af.getChannels() != 1;
        int iSampleRate = _af != null ? Math.round(_af.getSampleRate()) : -1;

        if (_videoFormat == VideoFormat.AVI_RGB) {
            if (_af == null)
                _aviWriter = _aviDib = new AviWriterDIB(_outputFile, _iWidth, _iHeight, _vidSync.getFpsNum(), _vidSync.getFpsDenom());
            else
                _aviWriter = _aviDib = new AviWriterDIB(_outputFile, _iWidth, _iHeight, _vidSync.getFpsNum(), _vidSync.getFpsDenom(), _af);
            _rgbImgBuf = new RgbIntImage(_iWidth, _iHeight);
        } else if (_videoFormat == VideoFormat.AVI_YUV || _videoFormat == VideoFormat.AVI_JYUV) {
            if (_af == null)
                _aviWriter = _aviYuv = new AviWriterYV12(_outputFile, _iWidth, _iHeight, _vidSync.getFpsNum(), _vidSync.getFpsDenom());
            else
                _aviWriter = _aviYuv = new AviWriterYV12(_outputFile, _iWidth, _iHeight, _vidSync.getFpsNum(), _vidSync.getFpsDenom(), _af);
            if (_videoFormat == VideoFormat.AVI_YUV)
                _recYuvImgBuff = new Rec601YCbCrImage(_iWidth, _iHeight);
            else
                _pcYuvImgBuff = new Pc601YCbCrImage(_iWidth, _iHeight);
        } else if (_videoFormat == VideoFormat.AVI_MJPG) {
            if (_af == null)
                _aviWriter = _aviMjpeg = new AviWriterMJPG(_outputFile, _iWidth, _iHeight, _vidSync.getFpsNum(), _vidSync.getFpsDenom());
            else
                _aviWriter = _aviMjpeg = new AviWriterMJPG(_outputFile, _iWidth, _iHeight, _vidSync.getFpsNum(), _vidSync.getFpsDenom(), _af);
        } else if (_videoFormat == VideoFormat.MKV_PNG) {
            if (_af == null)
                _mkvWriter = _mkvPng = new MkvPngWriter(_outputFile, _iWidth, _iHeight, _vidSync.getFps());
            else
                _mkvWriter = _mkvPng = new MkvPngWriter(_outputFile, _iWidth, _iHeight, _vidSync.getFps(), blnStereo, iSampleRate);
            _rgbImgBuf = new RgbIntImage(_iWidth, _iHeight);
        } else if (_videoFormat == VideoFormat.MKV_JYUV) {
            if (_af == null)
                _mkvWriter = _mkvYuv = new MkvJYuvWriter(_outputFile, _iWidth, _iHeight, _vidSync.getFps());
            else
                _mkvWriter = _mkvYuv = new MkvJYuvWriter(_outputFile, _iWidth, _iHeight, _vidSync.getFps(), blnStereo, iSampleRate);
            _pcYuvImgBuff = new Pc601YCbCrImage(_iWidth, _iHeight);
        } else if (_videoFormat == VideoFormat.MKV_MJPG) {
            if (_af == null)
                _mkvWriter = _mkvMjpeg = new MkvMjpegWriter(_outputFile, _iWidth, _iHeight, _vidSync.getFps());
            else
                _mkvWriter = _mkvMjpeg = new MkvMjpegWriter(_outputFile, _iWidth, _iHeight, _vidSync.getFps(), blnStereo, iSampleRate);
        }
        return _aviWriter != null ? _aviWriter : _mkvWriter;
    }

    private void writeErrorFrame(@Nonnull BufferedImage bi, @Nonnull Fraction presentationSecond) throws IOException {
        if (_videoFormat == VideoFormat.AVI_RGB) {
            RgbIntImage rgb = new RgbIntImage(bi);
            _aviDib.writeFrameRGB(rgb.getData(), 0, rgb.getWidth());
        } else if (_videoFormat == VideoFormat.AVI_YUV) {
            Rec601YCbCrImage yuv = new Rec601YCbCrImage(bi);
            _aviYuv.write(yuv.getYBuff(), yuv.getCbBuff(), yuv.getCrBuff());
        } else if (_videoFormat == VideoFormat.AVI_JYUV) {
            Pc601YCbCrImage yuv = new Pc601YCbCrImage(bi);
            _aviYuv.write(yuv.getYBuff(), yuv.getCbBuff(), yuv.getCrBuff());
        } else if (_videoFormat == VideoFormat.AVI_MJPG) {
            _aviMjpeg.writeFrame(bi);

        } else if (_mkvWriter != null) {
            _mkvWriter.writeImage(bi, presentationSecond);
        }
    }

    private void writeBlankFrame() throws IOException {
        if (_aviWriter != null)
            _aviWriter.writeBlankFrame();
        // mkv doesn't need to write blank frames
    }

    private void repeatPreviousFrame() throws IOException {
        if (_aviWriter != null)
            _aviWriter.repeatPreviousFrame();
        // mkv doesn't need to write duplicate frames
    }

    private void writeDecodedFrame(@Nonnull MdecDecoder decoder, @Nonnull Fraction presentationSecond) throws IOException {
        if (_videoFormat == VideoFormat.AVI_RGB) {
            decoder.readDecodedRgb(_iWidth, _iHeight, _rgbImgBuf.getData());
            _aviDib.writeFrameRGB(_rgbImgBuf.getData(), 0, _iWidth);
        } else if (_videoFormat == VideoFormat.AVI_YUV) {
            ((MdecDecoder_double)decoder).readDecoded_Rec601_YCbCr420(_recYuvImgBuff);
            _aviYuv.write(_recYuvImgBuff.getYBuff(), _recYuvImgBuff.getCbBuff(), _recYuvImgBuff.getCrBuff());
        } else if (_videoFormat == VideoFormat.AVI_JYUV) {
            ((MdecDecoder_double)decoder).readDecoded_JFIF_YCbCr420(_pcYuvImgBuff);
            _aviYuv.write(_pcYuvImgBuff.getYBuff(), _pcYuvImgBuff.getCbBuff(), _pcYuvImgBuff.getCrBuff());

        } else if (_videoFormat == VideoFormat.MKV_PNG) {
            decoder.readDecodedRgb(_iWidth, _iHeight, _rgbImgBuf.getData());
            _mkvPng.writeImage(_rgbImgBuf.toBufferedImage(), presentationSecond);
        } else if (_videoFormat == VideoFormat.MKV_JYUV) {
            ((MdecDecoder_double)decoder).readDecoded_JFIF_YCbCr420(_pcYuvImgBuff);
            _mkvYuv.writeYCbCrFrame(_pcYuvImgBuff.getYBuff(), _pcYuvImgBuff.getCbBuff(), _pcYuvImgBuff.getCrBuff(), presentationSecond);
        }
    }

    private void writeJpegFrame(@Nonnull ExposedBAOS jpeg, @Nonnull Fraction presSec) throws IOException {
        if (_aviMjpeg != null)
            _aviMjpeg.writeFrame(jpeg.getBuffer(), 0, jpeg.size());
        else if (_mkvMjpeg != null)
            _mkvMjpeg.writeJpegBytes(jpeg.toByteArray(), presSec);
    }

    private void writeAudio(@Nonnull byte[] abData, @Nonnull Fraction presTime) throws IOException {
        if (_aviWriter != null)
            _aviWriter.writeAudio(abData, 0, abData.length);
        else if (_mkvWriter != null)
            _mkvWriter.writeAudio(abData, presTime);
    }

    private void writeSilentSamples(int iSampleFrameCount, @Nonnull Fraction presTime) throws IOException {
        if (_aviWriter != null)
            _aviWriter.writeSilentSamples(iSampleFrameCount);
        else if (_mkvWriter != null)
            _mkvWriter.writeAudioSilence(iSampleFrameCount, presTime);
    }

    // ===================================================================================
    // ===================================================================================
    // ===================================================================================


    private static final Logger LOG = Logger.getLogger(VDPtoVideo.class.getName());

    @Nonnull
    private final VideoFormat _videoFormat;
    @Nonnull
    private final File _outputFile;
    @CheckForNull
    private VDP.GeneratedFileListener _fileGenListener;
    private final int _iWidth, _iHeight;
    @Nonnull
    private final VideoSync _vidSync;
    private final AudioVideoSync _avSync;
    private final AudioFormat _af;
    @Nonnull
    private final ILocalizedLogger _log;

    @CheckForNull
    private Closeable _writer;

    private int _iVideoFramesWritten = 0;
    private long _lngAudioSampleFramesWritten = 0;

    @CheckForNull
    private Mdec2Jpeg _jpegTranslator;
    @CheckForNull
    private ExposedBAOS _jpegBuffer;

    /** Video without audio. */
    public VDPtoVideo(@Nonnull VideoFormat videoFormat,
                      @Nonnull File outputFile, int iWidth, int iHeight,
                      @Nonnull VideoSync vidSync,
                      @Nonnull ILocalizedLogger log)
    {
        assertValidFormat(videoFormat);
        _videoFormat = videoFormat;
        _outputFile = outputFile;
        _iWidth = iWidth; _iHeight = iHeight;
        _vidSync = vidSync; _avSync =  null;
        _af = null;
        _log = log;
    }

    /** Video with audio. */
    public VDPtoVideo(@Nonnull VideoFormat videoFormat,
                      @Nonnull File outputFile, int iWidth, int iHeight,
                      @Nonnull AudioVideoSync avSync, @Nonnull AudioFormat af,
                      @Nonnull ILocalizedLogger log)
    {
        assertValidFormat(videoFormat);
        _videoFormat = videoFormat;
        _outputFile = outputFile;
        _iWidth = iWidth; _iHeight = iHeight;
        _vidSync = _avSync = avSync;
        _af = af;
        _log = log;
    }

    private static void assertValidFormat(VideoFormat videoFormat) {
        switch (videoFormat) {
            case AVI_RGB:
            case AVI_YUV:
            case AVI_JYUV:
            case AVI_MJPG:
            case MKV_PNG:
            case MKV_JYUV:
            case MKV_MJPG:
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    public int getVideoFramesWritten() {
        return _iVideoFramesWritten;
    }

    public long getAudioSampleFramesWritten() {
        return _lngAudioSampleFramesWritten;
    }

    @Override
    public @Nonnull ILocalizedLogger getLog() {
        return _log;
    }

    public @Nonnull File getOutputFile() {
        return _outputFile;
    }

    @Override
    public void setGenFileListener(@CheckForNull VDP.GeneratedFileListener listener) {
        _fileGenListener = listener;
    }

    public void open() throws LocalizedFileNotFoundException, FileNotFoundException, IOException {
        if (_writer != null)
            return;
        IO.makeDirsForFile(_outputFile);
        _writer = doOpen();
        if (_fileGenListener != null)
            _fileGenListener.fileGenerated(_outputFile);
    }

    private void assertVideoWriterIsOpen() throws IllegalStateException {
        if (_writer == null)
            throw new IllegalStateException("Video writer is not open");
    }

    @Override
    public void error(@Nonnull ILocalizedMessage errMsg, @CheckForNull FormattedFrameNumber frameNumber,
                      @Nonnull Fraction presentationSector)
            throws IOWritingException
    {
        assertVideoWriterIsOpen();

        try {
            Fraction presentationSecond = prepForFrame(frameNumber, presentationSector);
            writeErrorFrame(makeErrorImage(errMsg, _iWidth, _iHeight), presentationSecond);
            _iVideoFramesWritten++;
        } catch (IOException ex) {
            throw new IOWritingException(ex, _outputFile);
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

    private @Nonnull Fraction prepForFrame(@CheckForNull FormattedFrameNumber frameNumber,
                                           @Nonnull Fraction presentationSector)
            throws IOException
    {
        assertVideoWriterIsOpen();

        // if first frame
        if (_iVideoFramesWritten < 1 && _vidSync.getInitialVideo() > 0) {

            _log.log(Level.INFO, I.WRITING_BLANK_FRAMES_TO_ALIGN_AV(_vidSync.getInitialVideo()));
            writeBlankFrame();
            _iVideoFramesWritten++;
            for (int i = _vidSync.getInitialVideo()-1; i > 0; i--) {
                repeatPreviousFrame();
                _iVideoFramesWritten++;
            }

        }

        int iDupCount = _vidSync.calculateFramesToCatchUp(presentationSector, _iVideoFramesWritten);

        if (iDupCount < 0) {
            // this does happen on occasion:
            // * A few frames get off pretty bad from fps
            // * Frames end early (like iki) so presentation sector is
            //   pretty off (but frame is ok)
            // TODO: fix when frame ends early
            // i.e. have a range of presentation sectors (ug)
            _log.log(Level.WARNING, VDPerrors.FRAME_NUM_AHEAD_OF_READING(frameNumber, -iDupCount));
        } else {
            while (iDupCount > 0) { // could happen with first frame
                if (_iVideoFramesWritten < 1) { // TODO: fix design so this isn't needed
                    _log.log(Level.INFO, I.WRITING_BLANK_FRAMES_TO_ALIGN_AV(1));
                    LOG.log(Level.INFO, "Writing blank frame for frame {0}", frameNumber);
                    writeBlankFrame();
                    _iVideoFramesWritten++;
                } else {
                    _log.log(Level.INFO, I.WRITING_DUP_FRAMES_TO_ALIGN_AV(1));
                    LOG.log(Level.INFO, "Writing dup frame for frame {0}", frameNumber);
                    repeatPreviousFrame();
                    _iVideoFramesWritten++;
                }
                iDupCount--;
            }
        }

        return _vidSync.getSecondsPerFrame().multiply(_iVideoFramesWritten);
    }


    @Override
    public void decoded(@Nonnull MdecDecoder decoder, @CheckForNull FormattedFrameNumber frameNumber,
                        @Nonnull Fraction presentationSector)
            throws IOWritingException
    {
        assertVideoWriterIsOpen();
        try {
            Fraction presentationSecond = prepForFrame(frameNumber, presentationSector);
            writeDecodedFrame(decoder, presentationSecond);
            _iVideoFramesWritten++;
        } catch (IOException ex) {
            throw new IOWritingException(ex, _outputFile);
        }
    }

    @Override
    public void mdec(@Nonnull MdecInputStream mdecIn, @CheckForNull FormattedFrameNumber frameNumber,
                     @Nonnull Fraction presentationSector)
            throws IOWritingException
    {
        assertVideoWriterIsOpen();
        ILocalizedMessage err;
        Exception fail;
        try {
            if (_jpegTranslator == null)
                _jpegTranslator = new Mdec2Jpeg(_iWidth, _iHeight);
            _jpegTranslator.readMdec(mdecIn);

            if (_jpegBuffer == null)
                _jpegBuffer = new ExposedBAOS();
            else
                _jpegBuffer.reset();

            try {
                _jpegTranslator.writeJpeg(_jpegBuffer);
            } catch (IOException ex) {
                throw new RuntimeException("Should not happen", ex);
            }

            try {
                Fraction presentationSecond = prepForFrame(frameNumber, presentationSector);
                writeJpegFrame(_jpegBuffer, presentationSecond);
                _iVideoFramesWritten++;
            } catch (IOException ex) {
                throw new IOWritingException(ex, _outputFile);
            }
            return;
            // kinda icky way to do this
        } catch (MdecException.ReadCorruption ex) {
            err = VDPerrors.FRAME_NUM_CORRUPTED(frameNumber);
            fail = ex;
        } catch (MdecException.EndOfStream ex) {
            err = VDPerrors.FRAME_NUM_INCOMPLETE(frameNumber);
            fail = ex;
        } catch (MdecException.TooMuchEnergy ex) {
            err = VDPerrors.JPEG_ENCODER_FRAME_FAIL(frameNumber);
            fail = ex;
        }
        _log.log(Level.WARNING, err, fail);
        error(err, frameNumber, presentationSector);
    }


    @Override
    public void audioPacketComplete(@Nonnull DecodedAudioPacket packet,
                                    @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        assertVideoWriterIsOpen();

        assert _avSync != null;

        try {
            if (_lngAudioSampleFramesWritten < 1 && _avSync.getInitialAudio() > 0) {
                _log.log(Level.INFO, I.WRITING_SILECE_TO_SYNC_AV(_avSync.getInitialAudio()));
                Fraction presentationSecond = new Fraction(_lngAudioSampleFramesWritten, _avSync.getSamplesFramesPerSecond());
                writeSilentSamples(_avSync.getInitialAudio(), presentationSecond);
                _lngAudioSampleFramesWritten += _avSync.getInitialAudio();
            }
            int lngNeededSilence = _avSync.calculateAudioToCatchUp(packet.getPresentationSector(), _lngAudioSampleFramesWritten);
            if (lngNeededSilence > 0) {
                _log.log(Level.INFO, I.WRITING_SILENCE_TO_KEEP_AV_SYNCED(lngNeededSilence));
                Fraction presentationSecond = new Fraction(_lngAudioSampleFramesWritten, _avSync.getSamplesFramesPerSecond());
                writeSilentSamples(lngNeededSilence, presentationSecond);
                _lngAudioSampleFramesWritten += lngNeededSilence;
            }

            Fraction presentationSecond = new Fraction(_lngAudioSampleFramesWritten, _avSync.getSamplesFramesPerSecond());
            byte[] abData = packet.getData();
            writeAudio(abData, presentationSecond);
            _lngAudioSampleFramesWritten += packet.getSampleFrameCount();
        } catch (IOException ex) {
            throw new LoggedFailure(_log, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(_outputFile.toString()), ex);
        }
    }

    @Override
    public void close() throws IOException {
        if (_writer != null) {
            _writer.close();
        }
    }

}
