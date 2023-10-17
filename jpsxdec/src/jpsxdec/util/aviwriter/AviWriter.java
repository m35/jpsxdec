/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.util.aviwriter;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.Version;
import jpsxdec.i18n.I;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.aviwriter.AVIOLDINDEX.AVIOLDINDEXENTRY;

/**
 * Creates AVI files with audio and video without the need for JMF.
 * Subclasses should take care of codec handling. Note that this cannot
 * create AVI files larger than 4GB.
 * <p>
 * This code is originally based on (but now hardly resembles) the
 * <a href="http://rsb.info.nih.gov/ij">ImageJ</a> program.
 * <blockquote>
 *      ImageJ is being developed at the National Institutes of Health by an
 *      employee of the Federal Government in the course of his official duties.
 *      Pursuant to Title 17, Section 105 of the United States Code, this software
 *      is not subject to copyright protection and is in the public domain.
 *      ImageJ is an experimental system. NIH assumes no responsibility whatsoever
 *      for its use by other parties, and makes no guarantees, expressed or implied,
 *      about its quality, reliability, or any other characteristic.
 * </blockquote>
 * The ImageJ AviWriter class was based on the FileAvi class written by
 * William Gandler. That FileAvi class is part of Matthew J. McAuliffe's
 * <a href="http://mipav.cit.nih.gov/">MIPAV</a> program, which also
 * appears to be in the public domain.
 * <p>
 * An <a href="http://www.alexander-noe.com/video/documentation/avi.pdf">
 * AVI pdf document</a>
 * <p>
 * According to the Multimedia Guru,
 * <a href="http://guru.multimedia.cx/the-avi-container-file-format/">
 * AVI can support variable frame rates</a>.
 * <p>
 * Works with Java 1.5 or higher.
 */
public abstract class AviWriter implements Closeable {

    public static boolean MOCK_WRITING = false;

    private static RandomAccessFile openFileForWriting(@Nonnull File outputFile)
            throws FileNotFoundException, IOException
    {
        RandomAccessFile raf;
        if (MOCK_WRITING)
            raf = Md5RandomAccessFile.newThreadMd5Raf();
        else
            raf = new RandomAccessFile(outputFile, "rw");
        return raf;
    }

    /** Enable logging of every chunk written to the AVI. */
    private static final boolean DEBUG = false;

    // -------------------------------------------------------------------------
    // -- Fields ---------------------------------------------------------------
    // -------------------------------------------------------------------------

    @Nonnull
    private final File _outputFile;

    /** Width of the frame in pixels. */
    private final int _iWidth;
    /** Height of the frame in pixels. */
    private final int _iHeight;

    /** Numerator of the frames/second fraction. */
    private final long _lngFpsNumerator;
    /** Denominator of the frames/second fraction. */
    private final long _lngFpsDenominator;

    /** Static buffer available for this class, or any subclass to store
     * data before writing. Allocate more space if not big enough. */
    protected byte[] _abWriteBuffer;

    /** Number of frames written. */
    private long _lngFrameCount = 0;

    /** Audio format of the audio stream, or null if there is no audio. */
    @CheckForNull
    private final AudioFormat _audioFormat;

    /** Number of audio samples written. */
    private long _lngSampleCount = 0;

    private final boolean _blnCompressedVideo;
    @Nonnull
    private final String _sFourCCcodec;
    private final int _iCompression;

    // -------------------------------------------------------------------------
    // -- Properties -----------------------------------------------------------
    // -------------------------------------------------------------------------

    public @Nonnull File getFile() {
        return _outputFile;
    }

    public @CheckForNull AudioFormat getAudioFormat() {
        return _audioFormat;
    }

    /** Numerator of the frames/second fraction. */
    public long getFramesPerSecNum() {
        return _lngFpsNumerator;
    }
    /** Denominator of the frames/second fraction. */
    public long getFramesPerSecDenom() {
        return _lngFpsDenominator;
    }
    public int getWidth() {
        return _iWidth;
    }
    public int getHeight() {
        return _iHeight;
    }

    /** Number of frames written. */
    public long getVideoFramesWritten() {
        return _lngFrameCount;
    }

    /** Number of audio samples written. */
    public long getAudioSampleFramesWritten() {
        return _lngSampleCount;
    }

    /** Returns approximately how many audio samples play during each frame
     * (may be a fraction off). */
    public int getAudioSamplesPerFrame() {
        if (_audioFormat == null)
            return 0;

        //  samples/second / frames/second = samples/frame
        return Math.round(_audioFormat.getSampleRate() * _lngFpsDenominator / _lngFpsNumerator);
    }

    // -------------------------------------------------------------------------
    // -- AVI Structure Fields -------------------------------------------------
    // -------------------------------------------------------------------------

    @CheckForNull
    private RandomAccessFile _aviFile;

    private Chunk _RIFF_chunk;
    private     Chunk _LIST_hdr1;
    private         AVIMAINHEADER _avih;
    private         Chunk _LIST_strl_vid;
    private             Chunk _strf_vid;
    private                 AVISTREAMHEADER _strh_vid;
    private                 BITMAPINFOHEADER _bif;
                        //strf_vid
    private             Chunk _strn_vid;
                    //LIST_strl_vid
    private         Chunk _LIST_strl_aud;
    private             Chunk _strf_aud;
    private                 AVISTREAMHEADER _strh_aud;
    private                 WAVEFORMATEX _wavfmt;
                        //strf_aud
                    //LIST_strl_aud
                //LIST_hdr1
                    //JUNK_writerId;
    private     Chunk LIST_movi;
                    /* image and audio chunk data go here */
                //LIST_movi
    private     AVIOLDINDEX avioldidx;
            //RIFF_chunk

    /** Holds the 'idx' section index data. */
    @Nonnull
    private ArrayList<AVIOLDINDEXENTRY> _indexList;


    // -------------------------------------------------------------------------
    // -- Constructors ---------------------------------------------------------
    // -------------------------------------------------------------------------

    private static @Nonnull byte[] getMeta() {
        String sMeta = System.getProperty("meta");
        if (sMeta == null)
            sMeta = I.JPSXDEC_VERSION_NON_COMMERCIAL(Version.Version).getEnglishMessage();
        return Misc.stringToAscii(sMeta);
    }

    /** Audio data must be signed 16-bit PCM in little-endian order. */
    protected AviWriter(final @Nonnull File outputFile,
                        final int iWidth, final int iHeight,
                        final long lngFrames, final long lngPerSecond,
                        final @CheckForNull AudioFormat audioFormat,
                        final boolean blnCompressedVideo,
                        final @Nonnull String sFourCCcodec,
                        final int iBytes)
            throws FileNotFoundException, IOException
    {
        _outputFile = outputFile;

        _blnCompressedVideo = blnCompressedVideo;
        _sFourCCcodec = sFourCCcodec;
        _iCompression = iBytes;

        _iWidth = iWidth;
        _iHeight = iHeight;
        if (_iWidth < 1 || _iHeight < 1)
            throw new IllegalArgumentException("Video dimensions must be greater than 0.");

        _lngFpsNumerator = lngFrames;
        _lngFpsDenominator = lngPerSecond;
        if (_lngFpsNumerator < 1 || _lngFpsDenominator < 1)
            throw new IllegalArgumentException("Frames/Second must be greater than 0 " +
                                               "(and less than infinity).");

        if (audioFormat != null) {
            if (audioFormat.getChannels() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio channels cannot be NOT_SPECIFIED.");
            if (audioFormat.getFrameRate() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio frame rate cannot be NOT_SPECIFIED.");
            if (audioFormat.getFrameSize() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio frame size cannot be NOT_SPECIFIED.");
            if (audioFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio sample rate cannot be NOT_SPECIFIED.");
            if (audioFormat.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio sample size cannot be NOT_SPECIFIED.");
            if (audioFormat.isBigEndian())
                throw new IllegalArgumentException("Audio must be little-endian.");
            if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
                throw new IllegalArgumentException("Audio encoding needs to be PCM_SIGNED.");
            // TODO: Are there any more checks to perform?
        }
        _audioFormat = audioFormat;

        _aviFile = openFileForWriting(outputFile);
        try {

        _aviFile.setLength(0); // trim the file to 0 in case the file already exists

        //----------------------------------------------------------------------
        // Setup the header structure.
        // Actual values will be filled in when avi is closed.

        _RIFF_chunk = new Chunk(_aviFile, "RIFF", "AVI ");

            _LIST_hdr1 = new Chunk(_aviFile, "LIST", "hdrl");

                _avih = new AVIMAINHEADER();
                _avih.makePlaceholder(_aviFile);

                _LIST_strl_vid = new Chunk(_aviFile, "LIST", "strl");

                    _strh_vid = new AVISTREAMHEADER();
                    _strh_vid.makePlaceholder(_aviFile);

                    _strf_vid = new Chunk(_aviFile, "strf");

                        _bif = new BITMAPINFOHEADER();
                        _bif.makePlaceholder(_aviFile);

                    _strf_vid.endChunk(_aviFile);

                    _strn_vid = new Chunk(_aviFile, "strn");
                    _aviFile.write(Misc.stringToAscii("jPSXdec AVI    \0"));
                    _strn_vid.endChunk(_aviFile);

                _LIST_strl_vid.endChunk(_aviFile);

                if (_audioFormat != null) { // if there is audio
                _LIST_strl_aud = new Chunk(_aviFile, "LIST", "strl");

                    _strh_aud = new AVISTREAMHEADER();
                    _strh_aud.makePlaceholder(_aviFile);

                    _strf_aud = new Chunk(_aviFile, "strf");

                        _wavfmt = new WAVEFORMATEX();
                        _wavfmt.makePlaceholder(_aviFile);

                    _strf_aud.endChunk(_aviFile);

                _LIST_strl_aud.endChunk(_aviFile);
                }

            _LIST_hdr1.endChunk(_aviFile);

            // some programs will use this to identify the program that wrote the avi
            Chunk JUNK_writerId = new Chunk(_aviFile, "JUNK");
                _aviFile.write(getMeta());
                _aviFile.write(0);
            JUNK_writerId.endChunk(_aviFile);

            LIST_movi = new Chunk(_aviFile, "LIST", "movi");

        } catch (IOException ex) {
            closeSilentlyDueToError();
            throw ex;
        }
            // now we're ready to start accepting video/audio data

            // generate an index as we write 'movi' section
            _indexList = new ArrayList<AVIOLDINDEXENTRY>();
    }

    final protected void closeSilentlyDueToError() {
        IO.closeSilently(_aviFile, Logger.getLogger(AviWriter.class.getName()));
    }

    // -------------------------------------------------------------------------
    // -- Writing functions ----------------------------------------------------
    // -------------------------------------------------------------------------

    /** Uses a special feature of AVI to duplicate a frame by referencing
     * it twice. This adds almost no extra size to the file.
     * @throws IllegalStateException If no frames have been written yet. */
    public void repeatPreviousFrame() {
        if (_lngFrameCount < 1)
            throw new IllegalStateException("Unable to repeat a previous frame that doesn't exist.");

        if (DEBUG) {
            try {
                System.out.println("Frame DUPLICATE @" + _aviFile.getFilePointer());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        int iIndex = _indexList.size() - 1;

        // find the previous chunk that is a frame
        final int VID_CHUNK_ID = AVIstruct.string2int("00d_") & 0x00FFFFFF;
        while (true) {
            int iChunkId = _indexList.get(iIndex).dwChunkId;
            // does it start with '00d'?
            if ((iChunkId & 0x00FFFFFF) == VID_CHUNK_ID)
                break;
            else
                iIndex--;
        }
        // add the same reference in the list
        _indexList.add(_indexList.get(iIndex));
        _lngFrameCount++;
    }

    /** Audio format must be signed 16-bit PCM in little-endian order. */
    public void writeAudio(@Nonnull AudioInputStream audStream) throws AviIsClosedException, IOException {
        if (_aviFile == null) throw new AviIsClosedException();
        if (_audioFormat == null)
            throw new IllegalStateException("Unable to write audio to video-only avi.");

        AudioFormat fmt = audStream.getFormat();
        if (!fmt.matches(_audioFormat))
            throw new IllegalArgumentException("Audio stream format does not match.");

        Chunk data_size;

        AVIOLDINDEXENTRY idxentry = new AVIOLDINDEXENTRY();
        idxentry.dwOffset = (int)(_aviFile.getFilePointer() - (LIST_movi.getStart() + 4));

        idxentry.dwChunkId = AVIstruct.string2int("01wb");
        idxentry.dwFlags = 0;

        long lngSampleCount;
        long lngFilePointer;
        if (DEBUG) {
            lngSampleCount = _lngSampleCount;
            lngFilePointer = _aviFile.getFilePointer();
        }

        data_size = new Chunk(_aviFile, "01wb");

            if (_abWriteBuffer == null || _abWriteBuffer.length < _audioFormat.getFrameSize() * 1024)
                _abWriteBuffer = new byte[_audioFormat.getFrameSize() * 1024];

            // write the data then pad the data to 4 byte boundary
            int i, iTotal = 0;
            while ((i = audStream.read(_abWriteBuffer)) > 0) {
                iTotal += i;
                _lngSampleCount += i / _audioFormat.getFrameSize();
                _aviFile.write(_abWriteBuffer, 0, i);
            }
            if (iTotal % _audioFormat.getFrameSize() != 0)
                throw new RuntimeException("Read and wrote partial sample.");

        // end the chunk
        data_size.endChunk(_aviFile);

        if (DEBUG) {
            System.out.println("Audio " + lngSampleCount + " @" + lngFilePointer + " length " + data_size.getSize() + " silence");
        }

        // add this item to the index
        idxentry.dwSize = data_size.getSize();
        _indexList.add(idxentry);
    }

    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public void writeAudio(@Nonnull byte[] abData) throws IOException {
        writeAudio(abData, 0, abData.length);
    }

    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public void writeAudio(@Nonnull byte[] abData, int iOfs, int iLen) throws AviIsClosedException, IOException {
        if (_aviFile == null) throw new AviIsClosedException();
        if (_audioFormat == null)
            throw new IllegalStateException("Unable to write audio to video-only avi.");

        if (DEBUG) {
            System.out.println("Audio " + _lngSampleCount + " @" + _aviFile.getFilePointer() + " length " + iLen + " " + md5(abData, iOfs, iLen));
        }

        // TODO: Maybe have better handling if half a sample is provided
        if (iLen % _audioFormat.getFrameSize() != 0)
            throw new IllegalArgumentException("Half an audio sample can't be processed.");

        AVIOLDINDEXENTRY idxentry = new AVIOLDINDEXENTRY();
        idxentry.dwOffset = (int)(_aviFile.getFilePointer() - (LIST_movi.getStart() + 4));
        idxentry.dwChunkId = AVIstruct.string2int("01wb");
        idxentry.dwFlags = 0;

        Chunk data_size = new Chunk(_aviFile, "01wb");

            // write the data
            _aviFile.write(abData, iOfs, iLen);

        // end the chunk
        data_size.endChunk(_aviFile);

        _lngSampleCount += iLen / _audioFormat.getFrameSize();

        // add the index to the list
        idxentry.dwSize = data_size.getSize();
        _indexList.add(idxentry);
    }

    public void writeSilentSamples(long lngSampleCount) throws AviIsClosedException, IOException {
        if (_audioFormat == null)
            throw new IllegalStateException("Unable to write audio to video-only avi.");

        writeAudio(new AudioInputStream(new IO.ZeroInputStream(), _audioFormat, lngSampleCount));
    }

    /** Subclasses will use this method to write each frame's data. */
    protected void writeFrameChunk(@Nonnull byte[] abData, int iOfs, int iLen) throws AviIsClosedException, IOException {
        if (_aviFile == null) throw new AviIsClosedException();

        if (DEBUG) {
            System.out.println("Frame " + _lngFrameCount + " @" + _aviFile.getFilePointer() + " " + md5(abData, iOfs, iLen));
        }

        AVIOLDINDEXENTRY idxentry = new AVIOLDINDEXENTRY();
        idxentry.dwOffset = (int)(_aviFile.getFilePointer() - (LIST_movi.getStart() + 4));
        String sChunkId;
        if (_blnCompressedVideo)
            sChunkId = "00dc";  // dc for compressed frame
        else
            sChunkId = "00db";  // db for uncompressed frame
        idxentry.dwChunkId = AVIstruct.string2int(sChunkId);
        idxentry.dwFlags = AVIOLDINDEX.AVIIF_KEYFRAME; // Write the flags - select AVIIF_KEYFRAME
                                                       // AVIIF_KEYFRAME 0x00000010L
                                                       // The flag indicates key frames in the video sequence.
        Chunk data_size = new Chunk(_aviFile, sChunkId);

            // write the data
            _aviFile.write(abData, iOfs, iLen);

        // end the chunk
        data_size.endChunk(_aviFile);

        _lngFrameCount++;

        // add the index to the list
        idxentry.dwSize = data_size.getSize();
        _indexList.add(idxentry);
    }

    /** Subclasses should implement writing of a simple blank frame. */
    abstract public void writeBlankFrame() throws IOException;

    // -------------------------------------------------------------------------
    // -- Close ----------------------------------------------------------------
    // -------------------------------------------------------------------------

    /** Finishes writing the AVI header and index and closes the file.
     * This must be called and complete successfully for the AVI to be playable.
     */
    @Override
    public void close() throws AviIsClosedException, IOException {
        if (_aviFile == null) throw new AviIsClosedException();

            LIST_movi.endChunk(_aviFile);

            // write idx
            avioldidx = new AVIOLDINDEX(_indexList.toArray(new AVIOLDINDEXENTRY[_indexList.size()]));
            avioldidx.write(_aviFile);
            // /write idx

        _RIFF_chunk.endChunk(_aviFile);

        //######################################################################
        //## Fill the headers fields ###########################################
        //######################################################################

        //_avih.fcc                 = 'avih';  // the avih sub-CHUNK
        //_avih.cb                  = 0x38;    // the length of the avih sub-CHUNK (38H) not including the
                                               // the first 8 bytes for avihSignature and the length
        _avih.dwMicroSecPerFrame    = (int)((_lngFpsDenominator/(double)_lngFpsNumerator)*1.0e6);
        _avih.dwMaxBytesPerSec      = 0;       // (maximum data rate of the file in bytes per second)
        _avih.dwPaddingGranularity  = 0;
        _avih.dwFlags               = AVIMAINHEADER.AVIF_HASINDEX |
                                     AVIMAINHEADER.AVIF_ISINTERLEAVED;
                                              // 10H AVIF_HASINDEX: The AVI file has an idx1 chunk containing
                                              // an index at the end of the file.  For good performance, all
                                              // AVI files should contain an index.
        _avih.dwTotalFrames         = _lngFrameCount;  // total frame number
        _avih.dwInitialFrames       = 0;      // Initial frame for interleaved files.
                                              // Noninterleaved files should specify 0.
        if (_audioFormat == null)
            _avih.dwStreams         = 1;       // number of streams in the file - here 1 video and zero audio.
        else
            _avih.dwStreams         = 2;       // number of streams in the file - here 1 video and zero audio.
        _avih.dwSuggestedBufferSize = 0;       // Suggested buffer size for reading the file.
                                               // Generally, this size should be large enough to contain the largest
                                               // chunk in the file.
                                               // dwSuggestedBufferSize - Suggested buffer size for reading the file.
        _avih.dwWidth               = _iWidth;  // image width in pixels
        _avih.dwHeight              = _iHeight; // image height in pixels
        //_avih.dwReserved1         = 0;        //  Microsoft says to set the following 4 values to 0.
        //_avih.dwReserved2         = 0;        //
        //_avih.dwReserved3         = 0;        //
        //_avih.dwReserved4         = 0;        //


        //######################################################################
        // AVISTREAMHEADER for video

        //_strh_vid.fcc                  = 'strh';              // strh sub-CHUNK
        //_strh_vid.cb                   = 56;                  // the length of the strh sub-CHUNK
        _strh_vid.fccType                = AVIstruct.string2int("vids"); // the type of data stream - here vids for video stream
        _strh_vid.fccHandler             = AVIstruct.string2int(_sFourCCcodec);
        _strh_vid.dwFlags                = 0;
        _strh_vid.wPriority              = 0;
        _strh_vid.wLanguage              = 0;
        _strh_vid.dwInitialFrames        = 0;
        _strh_vid.dwScale                = _lngFpsDenominator;
        _strh_vid.dwRate                 = _lngFpsNumerator; // frame rate for video streams
        _strh_vid.dwStart                = 0;                // this field is usually set to zero
        _strh_vid.dwLength               = _lngFrameCount;   // playing time of AVI file as defined by scale and rate
                                                             // Set equal to the number of frames
        // TODO: Add a sugested buffer size
        _strh_vid.dwSuggestedBufferSize  = 0;  // Suggested buffer size for reading the stream.
                                               // Typically, this contains a value corresponding to the largest chunk
                                               // in a stream.
        _strh_vid.dwQuality              = -1; // encoding quality given by an integer between
                                               // 0 and 10,000.  If set to -1, drivers use the default
                                               // quality value.
        _strh_vid.dwSampleSize           = 0;
        _strh_vid.left                   = 0;
        _strh_vid.top                    = 0;
        _strh_vid.right                  = (short)_iWidth; // virtualdub uses width
        _strh_vid.bottom                 = (short)_iHeight; // virtualdub uses height

        //######################################################################
        // BITMAPINFOHEADER

        //_bif.biSize        = 40;       // Write header size of BITMAPINFO header structure
                                         // Applications should use this size to determine which BITMAPINFO header structure is
                                         // being used.  This size includes this biSize field.
        _bif.biWidth         = _iWidth;  // BITMAP width in pixels
        _bif.biHeight        = _iHeight; // image height in pixels.  If height is positive,
                                         // the bitmap is a bottom up DIB and its origin is in the lower left corner.  If
                                         // height is negative, the bitmap is a top-down DIB and its origin is the upper
                                         // left corner.  This negative sign feature is supported by the Windows Media Player, but it is not
                                         // supported by PowerPoint.
        //_bif.biPlanes      = 1;        // biPlanes - number of color planes in which the data is stored
                                         // This must be set to 1.
        _bif.biBitCount      = 24;       // biBitCount - number of bits per pixel #
                                         // 0L for BI_RGB, uncompressed data as bitmap
                                         // or type of compression used
        _bif.biCompression   = _iCompression;

        _bif.biSizeImage     = 0;
        _bif.biXPelsPerMeter = 0;        // horizontal resolution in pixels
        _bif.biYPelsPerMeter = 0;        // vertical resolution in pixels
                                         // per meter
        _bif.biClrUsed       = 0;        //
        _bif.biClrImportant  = 0;        // biClrImportant - specifies that the first x colors of the color table
                                         // are important to the DIB.  If the rest of the colors are not available,
                                         // the image still retains its meaning in an acceptable manner.  When this
                                         // field is set to zero, all the colors are important, or, rather, their
                                         // relative importance has not been computed.

        //######################################################################
        // AVISTREAMHEADER for audio

        if (_audioFormat != null) {
            //_strh_aud.fcc                  = 'strh';              // strh sub-CHUNK
            //_strh_aud.cb                   = 56;                  // length of the strh sub-CHUNK
            _strh_aud.fccType                = AVIstruct.string2int("auds"); // Write the type of data stream - here auds for audio stream
            _strh_aud.fccHandler             = 0; // no fccHandler for wav
            _strh_aud.dwFlags                = 0;
            _strh_aud.wPriority              = 0;
            _strh_aud.wLanguage              = 0;
            _strh_aud.dwInitialFrames        = 1; // virtualdub uses 1
            _strh_aud.dwScale                = 1;
            _strh_aud.dwRate                 = (int)_audioFormat.getSampleRate(); // sample rate for audio streams
            _strh_aud.dwStart                = 0;   // this field is usually set to zero
            // FIXME: for some reason virtualdub has a different dwLength value
            _strh_aud.dwLength               = (int)_lngSampleCount;   // playing time of AVI file as defined by scale and rate
                                                   // Set equal to the number of audio samples in file?
            // TODO: Add suggested audio buffer size
            _strh_aud.dwSuggestedBufferSize  = 0;   // Suggested buffer size for reading the stream.
                                                   // Typically, this contains a value corresponding to the largest chunk
                                                   // in a stream.
            _strh_aud.dwQuality              = -1;  // encoding quality given by an integer between
                                                   // 0 and 10,000.  If set to -1, drivers use the default
                                                   // quality value.
            _strh_aud.dwSampleSize           = _audioFormat.getFrameSize();
            _strh_aud.left                   = 0;
            _strh_aud.top                    = 0;
            _strh_aud.right                  = 0;
            _strh_aud.bottom                 = 0;

            //######################################################################
            // WAVEFORMATEX

            _wavfmt.wFormatTag       = WAVEFORMATEX.WAVE_FORMAT_PCM;
            _wavfmt.nChannels        = (short)_audioFormat.getChannels();
            _wavfmt.nSamplesPerSec   = (int)_audioFormat.getFrameRate();
            _wavfmt.nAvgBytesPerSec  = _audioFormat.getFrameSize() * _wavfmt.nSamplesPerSec;
            _wavfmt.nBlockAlign      = (short)_audioFormat.getFrameSize();
            _wavfmt.wBitsPerSample   = (short) _audioFormat.getSampleSizeInBits();
            //wavfmt.cbSize           = 0; // not written

        }

        //######################################################################
        //######################################################################
        //######################################################################

        // go back and write the headers
        _avih.goBackAndWrite(_aviFile);
        _strh_vid.goBackAndWrite(_aviFile);
        _bif.goBackAndWrite(_aviFile);

        if (_audioFormat != null) {
            _strh_aud.goBackAndWrite(_aviFile);
            _wavfmt.goBackAndWrite(_aviFile);
        }

        // and we're done
        _aviFile.close();
        _aviFile = null;

        _RIFF_chunk = null;
            _LIST_hdr1 = null;
                _avih = null;
                _LIST_strl_vid = null;
                    _strf_vid = null;
                        _strh_vid = null;
                        _bif = null;
                    _strn_vid = null;
                _LIST_strl_aud = null;
                    _strf_aud = null;
                        _strh_aud = null;
                        _wavfmt = null;
            LIST_movi = null;
            avioldidx = null;
    }

    @Override
    public String toString() {
        return String.format("%s %s %dx%d %d/%d fps",
                             getClass().getSimpleName(), getFile(),
                             getWidth(), getHeight(),
                             getFramesPerSecNum(), getFramesPerSecDenom());
    }

    // -------------------------------------------------------------------------
    // -- Private functions ----------------------------------------------------
    // -------------------------------------------------------------------------

    /** Represents an AVI RIFF 'chunk'. When created, it saves the current
     *  position in the AVI RandomAccessFile. When endChunk() is called,
     *  it temporarily jumps back to the start of the chunk and records how
     *  many bytes have been written. */
    private static class Chunk {

        private static final byte[] ZEROES3 = new byte[3];

        private final long _lngPos;
        private int _iSize = -1;

        Chunk(@Nonnull RandomAccessFile raf, @Nonnull String sChunkName) throws IOException {
            IO.writeInt32LE(raf, AVIstruct.string2int(sChunkName));
            _lngPos = raf.getFilePointer();
            raf.writeInt(0);
        }

         Chunk(@Nonnull RandomAccessFile raf, @Nonnull String sChunkName, @Nonnull String sSubChunkName) throws IOException {
            this(raf, sChunkName);
            IO.writeInt32LE(raf, AVIstruct.string2int(sSubChunkName));
        }

        /** Jumps back to saved position in the RandomAccessFile and writes
         *  how many bytes have passed since the position was saved, then
         *  returns to the current position again. Pads to a 4 byte boundary. */
        public void endChunk(@Nonnull RandomAccessFile raf) throws IOException {
            long lngCurPos = raf.getFilePointer(); // save this pos
            _iSize = (int)(lngCurPos - (_lngPos + 4)); // calculate number of bytes since start of chunk

            // pad to 4 byte boundary
            int iNon4bytes = _iSize % 4;
            if (iNon4bytes > 0) {
                int iBytesToPad = 4 - iNon4bytes;
                raf.write(ZEROES3, 0, iBytesToPad);
                _iSize += iBytesToPad;
                lngCurPos += iBytesToPad;
            }

            raf.seek(_lngPos); // go back to where the header is
            IO.writeInt32LE(raf, _iSize); // write the header size
            raf.seek(lngCurPos); // return to current position
        }

        /** After endChunk() has been called, returns the size that was
         *  written. */
        private int getSize() {
            return _iSize;
        }

        /** Returns the position where the size will be written when
         *  endChunk() is called. */
        private long getStart() {
            return _lngPos;
        }
    }

    /**
     * For debugging.
     * @see #DEBUG
     */
    private static @Nonnull String md5(@Nonnull byte[] abData, int iOfs, int iLen) {
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            md.update(abData, iOfs, iLen);
            BigInteger number = new BigInteger(1, md.digest());
            String sHashText = number.toString(16);
            sHashText = Misc.zeroPadString(sHashText, 32, false);
            return sHashText;
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}
