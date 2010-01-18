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

package jpsxdec.util.aviwriter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.aviwriter.AVIOLDINDEX.AVIOLDINDEXENTRY;

/**
 * AVI encoder to write uncompressed, RGB DIB video, or compressed MJPG video, 
 * along with uncompressed PCM audio. The resulting MJPG AVI seems playable on 
 * vanilla Windows XP systems, and of course VLC.
 * <p> 
 * This code is originally based on (but now hardly resembles) the ImageJ 
 * package at http://rsb.info.nih.gov/ij
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
 * MIPAV program, available from http://mipav.cit.nih.gov/, which also
 * appears to be in the public domain.
 * <p>
 * I owe my MJPG understanding to the jpegtoavi program.
 * http://sourceforge.net/projects/jpegtoavi/
 * <p>
 * Random list of codecs
 * http://www.oltenia.ro/download/pub/windows/media/video/tools/GSpot/gspot22/GSpot22.dat
 * <p>
 * http://www.alexander-noe.com/video/documentation/avi.pdf
 * <p>
 * According to the Multimedia Guru, AVI can support variable frame rates.
 * http://guru.multimedia.cx/the-avi-container-file-format/
 * <p>
 * Works with Java 1.5 or higher.
 */
public class AviWriter {

    /** Hold's true if system can write "jpeg" images. */
    private final static boolean CAN_ENCODE_JPEG;
    static {
        // check if the system can write "jpeg" images
        boolean bln = false;
        for (String s : ImageIO.getReaderFormatNames()) {
            if (s.equals("jpeg")) {
                bln = true;
                break;
            }
        }
        CAN_ENCODE_JPEG = bln;
    }
    
    // -------------------------------------------------------------------------
    // -- Fields ---------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    /** Width of the frame in pixels. */
    private final int _iWidth;
    /** Height of the frame in pixels. */
    private final int _iHeight;
    
    /** Numerator of the frames/second fraction. */
    private final long _lngFrames;
    /** Denominator of the frames/second fraction. */
    private final long _lngPerSecond;
    
    /** Size of the frame data in bytes. Only applicable to DIB AVI.
     *  Each DIB frame submitted is compared to this value to ensure
     *  proper data. */
    private int _iFrameByteSize = -1;

    /** Temporary buffer available to store data before writing. */
    private byte[] _abWriteBuffer;

    /** Number of frames written. */
    private long _lngFrameCount = 0;

    private final AudioFormat _audioFormat;

    /** Number of audio samples written. */
    private double _dblSampleCount = 0;
    
    /** The image writer used to convert the BufferedImages to BMP or JPEG. */
    private final ImageWriter _imgWriter;
    /** Only used for MJPG when not using default quality level. */
    private final ImageWriteParam _writeParams;
    /** True if writing avi with MJPG codec, false if writing DIB codec. */
    private final boolean _blnMJPG;

    // -------------------------------------------------------------------------
    // -- Properties -----------------------------------------------------------
    // -------------------------------------------------------------------------
    
    public AudioFormat getAudioFormat() {
        return _audioFormat;
    }
    public long getFramesPerSecNum() {
        return _lngFrames;
    }
    public long getFramesperSecDenom() {
        return _lngPerSecond;
    }
    public int getWidth() {
        return _iWidth;
    }
    public int getHeight() {
        return _iHeight;
    }

    public long getVideoFramesWritten() {
        return _lngFrameCount;
    }

    public long getAudioSamplesWritten() {
        return (long)_dblSampleCount;
    }

    // -------------------------------------------------------------------------
    // -- AVI Structure Fields -------------------------------------------------
    // -------------------------------------------------------------------------
    
    private RandomAccessFile raFile;
    
    private Chunk RIFF_chunk;
    private     Chunk LIST_hdr1;
    private         AVIMAINHEADER avih;
    private         Chunk LIST_strl_vid;
    private             Chunk strf_vid;
    private                 AVISTREAMHEADER strh_vid;
    private                 BITMAPINFOHEADER bif;
                        //strf_vid
                    //LIST_strl_vid
    private         Chunk LIST_strl_aud;
    private             Chunk strf_aud;
    private                 AVISTREAMHEADER strh_aud;
    private                 WAVEFORMATEX wavfmt;
                        //strf_aud
                    //LIST_strl_aud
                //LIST_hdr1
    private     Chunk LIST_movi;
                  /* image and audio chunk data */
                //LIST_movi
    private     AVIOLDINDEX avioldidx;
            //RIFF_chunk
    
    /** Holds the 'idx' section index data. */
    private ArrayList<AVIOLDINDEXENTRY> _indexList;
    
    
    // -------------------------------------------------------------------------
    // -- Constructors ---------------------------------------------------------
    // -------------------------------------------------------------------------
    
    public AviWriter(final File oOutputfile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond)
            throws IOException
    {
        this(oOutputfile,
             iWidth, iHeight,
             lngFrames, lngPerSecond,
             false, true, -0.1f, null);
    }
    public AviWriter(final File outFile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond,
                     float fltQuality)
             throws IOException
    {
        this(outFile,
             iWidth, iHeight,
             lngFrames, lngPerSecond,
             true, false, fltQuality,
             null);
    }
    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public AviWriter(final File outFile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond,
                     boolean blnDefaultLossyQuality,
                     final AudioFormat audioFormat)
            throws IOException
    {
        this(outFile,
             iWidth, iHeight,
             lngFrames, lngPerSecond,
             true, true, -0.1f, audioFormat);
    }
    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public AviWriter(final File outFile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond,
                     final AudioFormat audioFormat)
            throws IOException
    {
        this(outFile,
             iWidth, iHeight,
             lngFrames, lngPerSecond,
             false, true, -0.1f, audioFormat);
    }

    /** Write MJPG encoded frames with specified quality.
     * @param iAudChannels 0, 1 or 2 audio channels.
     * @param fltMjpgQuality  quality from 0 (lowest) to 1 (highest). */
    public AviWriter(final File oOutputfile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond,
                     final float fltLossyQuality,
                     final AudioFormat oAudioFormat)
            throws IOException
    {
        this(oOutputfile,
             iWidth, iHeight,
             lngFrames, lngPerSecond,
             true, false, fltLossyQuality,
             oAudioFormat);
    }
    
    /** Audio data must be signed 16-bit PCM in little-endian order. */
    private AviWriter(final File oOutputfile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond,
                     final boolean blnIsLossy,
                     final boolean blnDefaultLossyQuality,
                     final float fltLossyQuality,
                     final AudioFormat oAudioFormat)
            throws IOException
    {
        Iterator<ImageWriter> oIter;

        if (blnIsLossy) {
            if (!CAN_ENCODE_JPEG)
                throw new UnsupportedOperationException("Unable to create 'jpeg' images on this platform.");

            oIter = ImageIO.getImageWritersByFormatName("jpeg");
            _imgWriter = oIter.next();
            
            if (blnDefaultLossyQuality) {
                _writeParams = null;
            } else {
                // TODO: Make sure thumbnails are not being created in the jpegs
                _writeParams = _imgWriter.getDefaultWriteParam();

                _writeParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // 0 for lowest qulaity, 1 for highest
                _writeParams.setCompressionQuality(fltLossyQuality);
            }
            _blnMJPG = true;
        } else {
            oIter = ImageIO.getImageWritersByFormatName("bmp");
            _imgWriter = (ImageWriter)oIter.next();
            _writeParams = null;
            _blnMJPG = false;
        }

        _iWidth = iWidth;
        _iHeight = iHeight;
        if (_iWidth < 1 || _iHeight < 1)
            throw new IllegalArgumentException("Video dimensions must be greater than 0.");

        _lngFrames = lngFrames;
        _lngPerSecond = lngPerSecond;
        if (_lngFrames < 1 || _lngPerSecond < 1)
            throw new IllegalArgumentException("Frames/Second must be greater than 0 " +
                                               "(and less than infinity).");

        if (oAudioFormat != null) {
            if (oAudioFormat.getChannels() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio channels cannot be NOT_SPECIFIED.");
            if (oAudioFormat.getFrameRate() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio frame rate cannot be NOT_SPECIFIED.");
            if (oAudioFormat.getFrameSize() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio frame size cannot be NOT_SPECIFIED.");
            if (oAudioFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio sample rate cannot be NOT_SPECIFIED.");
            if (oAudioFormat.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED)
                throw new IllegalArgumentException("Audio sample size cannot be NOT_SPECIFIED.");
            if (oAudioFormat.isBigEndian())
                throw new IllegalArgumentException("Audio must be little-endian.");
            // TODO: Are there any more checks to perform?
        }
        _audioFormat = oAudioFormat;

        raFile = new RandomAccessFile(oOutputfile, "rw");
        raFile.setLength(0); // trim the file to 0

        //----------------------------------------------------------------------
        // Setup the header structure. 
        // Actual values will be filled in when avi is closed.
        
        RIFF_chunk = new Chunk(raFile, "RIFF", "AVI ");
        
            LIST_hdr1 = new Chunk(raFile, "LIST", "hdrl"); 
        
                avih = new AVIMAINHEADER();
                avih.makePlaceholder(raFile);
            
                LIST_strl_vid = new Chunk(raFile, "LIST", "strl");

                    strh_vid = new AVISTREAMHEADER();
                    strh_vid.makePlaceholder(raFile);

                    strf_vid = new Chunk(raFile, "strf");

                        bif = new BITMAPINFOHEADER();
                        bif.makePlaceholder(raFile);                               

                    strf_vid.endChunk(raFile);
                    
                LIST_strl_vid.endChunk(raFile);
                
                if (_audioFormat != null) { // if there is audio
                LIST_strl_aud = new Chunk(raFile, "LIST", "strl");

                    strh_aud = new AVISTREAMHEADER();
                    strh_aud.makePlaceholder(raFile);

                    strf_aud = new Chunk(raFile, "strf");

                        wavfmt = new WAVEFORMATEX();
                        wavfmt.makePlaceholder(raFile);

                    strf_aud.endChunk(raFile);

                LIST_strl_aud.endChunk(raFile);
                }

            LIST_hdr1.endChunk(raFile);
            
            LIST_movi = new Chunk(raFile, "LIST", "movi");

            // now we're ready to start accepting video/audio data
            
            // generate an index as we write 'movi' section
            _indexList = new ArrayList<AVIOLDINDEXENTRY>();
    }

    // -------------------------------------------------------------------------
    // -- Public functions -----------------------------------------------------
    // -------------------------------------------------------------------------
    
    /** Assumes width/height is correct. 
     * abData should either be DIB data 
     * (BGR rows inverted and widths padded to 4 byte boundaries),
     * or JPEG data. */
    private void writeFrameFormatted(byte[] abData) throws IOException {
        if (raFile == null) throw new IOException("Avi file is closed");
        // only keep track of frame data size if it's DIB. They should all be the same
        if (!_blnMJPG) {
            if (_iFrameByteSize < 0)
                _iFrameByteSize = abData.length;
            else if (_iFrameByteSize != abData.length)
                throw new IllegalArgumentException("Frame data size is not consistent");
        }
        
        writeStreamDataChunk(abData, 0, abData.length, true);
    }

    /** @param abData  RGB image data stored at 24 bits/pixel (3 bytes/pixel) */
    public void writeFrameRGB(byte[] abData, int iStart, int iLineStride) {
        int iLinePadding = (_iWidth * 3) & 3;
        if (iLinePadding != 0)
            iLinePadding = 4 - iLinePadding;
        int iNeededBuffSize = (_iWidth * 3 + iLinePadding) * _iHeight ;
        if (_abWriteBuffer == null || _abWriteBuffer.length < iNeededBuffSize)
            _abWriteBuffer = new byte[iNeededBuffSize];

        int iSrcLine = iStart;
        int iDestPos = 0;
        for (int y = _iHeight-1; y >= 0; y--) {
            int iSrcPos = iSrcLine;
            for (int x = 0; x < _iWidth; x++) {
                _abWriteBuffer[iDestPos] = abData[iSrcPos+2];
                iDestPos++;
                _abWriteBuffer[iDestPos] = abData[iSrcPos+1];
                iDestPos++;
                _abWriteBuffer[iDestPos] = abData[iSrcPos+0];
                iDestPos++;
                iSrcPos+=3;
            }
            iSrcLine += iLineStride;
            for (int i = 0; i < iLinePadding; i++) {
                _abWriteBuffer[iDestPos] = 0;
                iDestPos++;
            }
        }
    }

    /** @param abData  RGB image data stored at RGB in the lower bytes of an int. */
    public void writeFrameRGB(int[] aiData, int iStart, int iLineStride) {
        int iLinePadding = (_iWidth * 3) & 3;
        if (iLinePadding != 0)
            iLinePadding = 4 - iLinePadding;
        int iNeededBuffSize = (_iWidth * 3 + iLinePadding) * _iHeight ;
        if (_abWriteBuffer == null || _abWriteBuffer.length < iNeededBuffSize)
            _abWriteBuffer = new byte[iNeededBuffSize];

        int iSrcLine = iStart;
        int iDestPos = 0;
        for (int y = _iHeight-1; y >= 0; y--) {
            int iSrcPos = iSrcLine;
            for (int x = 0; x < _iWidth; x++) {
                int c = aiData[iSrcPos];
                _abWriteBuffer[iDestPos] = (byte)(c >> 16);
                iDestPos++;
                _abWriteBuffer[iDestPos] = (byte)(c >>  8);
                iDestPos++;
                _abWriteBuffer[iDestPos] = (byte)(c      );
                iDestPos++;
                iSrcPos++;
            }
            iSrcLine += iLineStride;
            for (int i = 0; i < iLinePadding; i++) {
                _abWriteBuffer[iDestPos] = 0;
                iDestPos++;
            }
        }
    }

    /** Converts a BufferedImage to proper avi format and writes it. */
    public void writeFrame(BufferedImage bi) throws IOException {
        if (raFile == null) throw new IOException("Avi file is closed");
        if (_iWidth != bi.getWidth())
            throw new IllegalArgumentException("AviWriter: Frame width doesn't match" +
                    " (was " + _iWidth + ", now " + bi.getWidth() + ").");
        
        if (_iHeight != bi.getHeight())
            throw new IllegalArgumentException("AviWriter: Frame height doesn't match" +
                    " (was " + _iHeight + ", now " + bi.getHeight() + ").");
        
        if (_blnMJPG) {
            writeFrameFormatted(image2MJPEG(bi).getBuffer());
        } else {
            writeFrameFormatted(image2DIB(bi, _iFrameByteSize));
        }
    }

    public void repeatPreviousFrame() {
        if (_lngFrameCount < 1)
            throw new IllegalStateException("Unable to repeat a previous frame that doesn't exist.");

        int iIndex = _indexList.size() - 1;
        
        final int VID_CHUNK_ID = AVIstruct.string2int("00d_") & 0x00FFFFFF;
        while (true) {
            int iChunkId = _indexList.get(iIndex).dwChunkId;
            if ((iChunkId & 0x00FFFFFF) == VID_CHUNK_ID)
                break;
            else
                iIndex--;
        }
        _indexList.add(_indexList.get(iIndex));
        _lngFrameCount++;
    }

    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public void writeAudio(byte[] abData) throws IOException {
        writeAudio(abData, 0, abData.length);
    }

    public void writeAudio(byte[] abData, int iOffset, int iLength) throws IOException {
        if (raFile == null) throw new IOException("Avi file is closed");
        if (_audioFormat == null)
            throw new IllegalStateException("Unable to write audio to video-only avi.");
        writeStreamDataChunk(abData, iOffset, iLength, false);
    }

    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public void writeAudio(AudioInputStream oData) throws IOException {
        if (raFile == null) throw new IOException("Avi file is closed");
        if (_audioFormat == null)
            throw new IllegalStateException("Unable to write audio to video-only avi.");
        
        AudioFormat fmt = oData.getFormat();
        if (!fmt.matches(_audioFormat))
            throw new IllegalArgumentException("Audio stream format does not match.");
        
        Chunk data_size;

        AVIOLDINDEXENTRY idxentry = new AVIOLDINDEXENTRY();
        idxentry.dwOffset = (int)(raFile.getFilePointer() - (LIST_movi.getStart() + 4));
        
        idxentry.dwChunkId = AVIstruct.string2int("01wb");
        idxentry.dwFlags = 0;
        
        data_size = new Chunk(raFile, "01wb");

            if (_abWriteBuffer == null || _abWriteBuffer.length < 1024)
                _abWriteBuffer = new byte[1024];

            // write the data, padded to 4 byte boundary
            int i;
            while ((i = oData.read(_abWriteBuffer, 0, 1024)) > 0) {
                _dblSampleCount += i / (double)_audioFormat.getFrameSize();
                raFile.write(_abWriteBuffer, 0, i);
            }
            
            int remaint = (int)(4 - (  (raFile.getFilePointer() - (data_size.getStart()+4) )  % 4)) % 4;
            while (remaint > 0) { raFile.write(0); remaint--; }
            
        // end the chunk
        data_size.endChunk(raFile);
        
        // add this item to the index
        idxentry.dwSize = (int)data_size.getSize();
        _indexList.add(idxentry);
    }
    
    private void writeStreamDataChunk(byte[] abData, int iOfs, int iLen, boolean blnIsVideo) throws IOException {

        Chunk data_size;

        AVIOLDINDEXENTRY idxentry = new AVIOLDINDEXENTRY();
        idxentry.dwOffset = (int)(raFile.getFilePointer() - (LIST_movi.getStart() + 4));

        if (blnIsVideo) { // if video

            String sChunkId;
            if (_blnMJPG)
                sChunkId = "00dc";  // dc for compressed frame
            else
                sChunkId = "00db";  // db for uncompressed frame
            idxentry.dwChunkId = AVIstruct.string2int(sChunkId);
            idxentry.dwFlags = AVIOLDINDEX.AVIIF_KEYFRAME; // Write the flags - select AVIIF_KEYFRAME
                                                           // AVIIF_KEYFRAME 0x00000010L
                                                           // The flag indicates key frames in the video sequence.
            _lngFrameCount++;
            
            data_size = new Chunk(raFile, sChunkId);
        } else { // if audio
            // TODO: Maybe have better handling if half a sample is provided
            if (iLen % _audioFormat.getFrameSize() != 0)
                throw new IllegalArgumentException("Half an audio sample can't be processed.");
                
            idxentry.dwChunkId = AVIstruct.string2int("01wb");
            idxentry.dwFlags = 0;


            _dblSampleCount += iLen / (double)_audioFormat.getFrameSize();
            
            data_size = new Chunk(raFile, "01wb");
        }

        // write the data, padded to 4 byte boundary
        int remaint = (4 - (iLen % 4)) % 4;
        raFile.write(abData, iOfs, iLen);
        while (remaint > 0) { raFile.write(0); remaint--; }
        // end the chunk
        data_size.endChunk(raFile);
        
        // add the index to the list
        idxentry.dwSize = (int)data_size.getSize();
        _indexList.add(idxentry);
    }
    
    /** I'm tempted to remove the IOException throw, but Java's
     *  RandomAccessFile can also throw an IOException on close(). */
    public void close() throws IOException {
        if (raFile == null) throw new IOException("Avi file is closed");
        
            LIST_movi.endChunk(raFile);
            
            // write idx
            avioldidx = new AVIOLDINDEX(_indexList.toArray(new AVIOLDINDEXENTRY[_indexList.size()]));
            avioldidx.write(raFile);
            // /write idx
            
        RIFF_chunk.endChunk(raFile);
        
        //######################################################################
        //## Fill the headers fields ###########################################
        //######################################################################
        
        //avih.fcc                 = 'avih';  // the avih sub-CHUNK
        //avih.cb                  = 0x38;    // the length of the avih sub-CHUNK (38H) not including the
                                              // the first 8 bytes for avihSignature and the length            
        avih.dwMicroSecPerFrame    = (int)((_lngPerSecond/(double)_lngFrames)*1.0e6);
        avih.dwMaxBytesPerSec      = 0;       // (maximum data rate of the file in bytes per second)
        avih.dwPaddingGranularity  = 0;
        avih.dwFlags               = AVIMAINHEADER.AVIF_HASINDEX | 
                                     AVIMAINHEADER.AVIF_ISINTERLEAVED;    
                                              // just set the bit for AVIF_HASINDEX
                                              // 10H AVIF_HASINDEX: The AVI file has an idx1 chunk containing
                                              // an index at the end of the file.  For good performance, all
                                              // AVI files should contain an index.                         
        avih.dwTotalFrames         = _lngFrameCount;  // total frame number
        avih.dwInitialFrames       = 0;       // Initial frame for interleaved files.
                                              // Noninterleaved files should specify 0.
        if (_audioFormat == null)
            avih.dwStreams         = 1;       // number of streams in the file - here 1 video and zero audio.
        else
            avih.dwStreams         = 2;       // number of streams in the file - here 1 video and zero audio.
        avih.dwSuggestedBufferSize = 0;       // Suggested buffer size for reading the file.
                                              // Generally, this size should be large enough to contain the largest
                                              // chunk in the file.
                                              // dwSuggestedBufferSize - Suggested buffer size for reading the file.
        avih.dwWidth               = _iWidth;  // image width in pixels
        avih.dwHeight              = _iHeight; // image height in pixels
        //avih.dwReserved1         = 0;       //  Microsoft says to set the following 4 values to 0.
        //avih.dwReserved2         = 0;       //  
        //avih.dwReserved3         = 0;       //  
        //avih.dwReserved4         = 0;       //  
        
        
        //######################################################################
        // AVISTREAMHEADER for video
        
        //strh_vid.fcc                  = 'strh';              // strh sub-CHUNK
        //strh_vid.cb                   = 56;                  // the length of the strh sub-CHUNK
        strh_vid.fccType                = AVIstruct.string2int("vids"); // the type of data stream - here vids for video stream
       // Write DIB for Microsoft Device Independent Bitmap.  Note: Unfortunately,
       // at least 3 other four character codes are sometimes used for uncompressed
       // AVI videos: 'RGB ', 'RAW ', 0x00000000
        if (_blnMJPG)
            strh_vid.fccHandler         = AVIstruct.string2int("MJPG");
        else
            strh_vid.fccHandler         = AVIstruct.string2int("DIB ");
        strh_vid.dwFlags                = 0;
        strh_vid.wPriority              = 0;
        strh_vid.wLanguage              = 0;
        strh_vid.dwInitialFrames        = 0;
        strh_vid.dwScale                = _lngPerSecond;
        strh_vid.dwRate                 = _lngFrames; // frame rate for video streams
        strh_vid.dwStart                = 0;         // this field is usually set to zero
        strh_vid.dwLength               = _lngFrameCount; // playing time of AVI file as defined by scale and rate
                                               // Set equal to the number of frames
        // TODO: Add a sugested buffer size
        strh_vid.dwSuggestedBufferSize  = 0;   // Suggested buffer size for reading the stream.
                                               // Typically, this contains a value corresponding to the largest chunk
                                               // in a stream.
        strh_vid.dwQuality              = -1;  // encoding quality given by an integer between
                                               // 0 and 10,000.  If set to -1, drivers use the default 
                                               // quality value.
        strh_vid.dwSampleSize           = 0; 
        strh_vid.left                   = 0;
        strh_vid.top                    = 0;
        strh_vid.right                  = (short)_iWidth; // virtualdub uses width
        strh_vid.bottom                 = (short)_iHeight; // virtualdub uses height

        //######################################################################
        // BITMAPINFOHEADER
        
        //bif.biSize        = 40;      // Write header size of BITMAPINFO header structure
                                       // Applications should use this size to determine which BITMAPINFO header structure is 
                                       // being used.  This size includes this biSize field.                                 
        bif.biWidth         = _iWidth;  // BITMAP width in pixels
        bif.biHeight        = _iHeight; // image height in pixels.  If height is positive,
                                       // the bitmap is a bottom up DIB and its origin is in the lower left corner.  If 
                                       // height is negative, the bitmap is a top-down DIB and its origin is the upper
                                       // left corner.  This negative sign feature is supported by the Windows Media Player, but it is not
                                       // supported by PowerPoint.                                                                        
        //bif.biPlanes      = 1;       // biPlanes - number of color planes in which the data is stored
                                       // This must be set to 1.
        bif.biBitCount      = 24;      // biBitCount - number of bits per pixel #
        if (_blnMJPG)                 // 0L for BI_RGB, uncompressed data as bitmap
            bif.biCompression   = AVIstruct.string2int("MJPG"); 
        else // type of compression used
            bif.biCompression   = BITMAPINFOHEADER.BI_RGB; 
        bif.biSizeImage     = 0;
        bif.biXPelsPerMeter = 0;       // horizontal resolution in pixels
        bif.biYPelsPerMeter = 0;       // vertical resolution in pixels
                                       // per meter
        bif.biClrUsed       = 0;       //
        bif.biClrImportant  = 0;       // biClrImportant - specifies that the first x colors of the color table 
                                       // are important to the DIB.  If the rest of the colors are not available,
                                       // the image still retains its meaning in an acceptable manner.  When this
                                       // field is set to zero, all the colors are important, or, rather, their
                                       // relative importance has not been computed.
        
        //######################################################################
        // AVISTREAMHEADER for audio

        if (_audioFormat != null) {
            //strh.fcc                  = 'strh';              // strh sub-CHUNK
            //strh.cb                   = 56;                  // length of the strh sub-CHUNK
            strh_aud.fccType                = AVIstruct.string2int("auds"); // Write the type of data stream - here auds for audio stream
            strh_aud.fccHandler             = 0; // no fccHandler for wav
            strh_aud.dwFlags                = 0;
            strh_aud.wPriority              = 0;
            strh_aud.wLanguage              = 0;
            strh_aud.dwInitialFrames        = 1; // virtualdub uses 1
            strh_aud.dwScale                = 1;
            strh_aud.dwRate                 = (int)_audioFormat.getSampleRate(); // sample rate for audio streams
            strh_aud.dwStart                = 0;   // this field is usually set to zero
            // FIXME: for some reason virtualdub has a different dwLength value
            strh_aud.dwLength               = (int)_dblSampleCount;   // playing time of AVI file as defined by scale and rate
                                                   // Set equal to the number of audio samples in file?
            // TODO: Add suggested audio buffer size
            strh_aud.dwSuggestedBufferSize  = 0;   // Suggested buffer size for reading the stream.
                                                   // Typically, this contains a value corresponding to the largest chunk
                                                   // in a stream.
            strh_aud.dwQuality              = -1;  // encoding quality given by an integer between
                                                   // 0 and 10,000.  If set to -1, drivers use the default 
                                                   // quality value.
            strh_aud.dwSampleSize           = _audioFormat.getFrameSize();
            strh_aud.left                   = 0;
            strh_aud.top                    = 0;
            strh_aud.right                  = 0;
            strh_aud.bottom                 = 0;   

            //######################################################################
            // WAVEFORMATEX

            wavfmt.wFormatTag       = WAVEFORMATEX.WAVE_FORMAT_PCM;
            wavfmt.nChannels        = (short)_audioFormat.getChannels();
            wavfmt.nSamplesPerSec   = (int)_audioFormat.getFrameRate();
            wavfmt.nAvgBytesPerSec  = _audioFormat.getFrameSize() * wavfmt.nSamplesPerSec;
            wavfmt.nBlockAlign      = (short)_audioFormat.getFrameSize();
            wavfmt.wBitsPerSample   = (short) _audioFormat.getSampleSizeInBits();
            //wavfmt.cbSize           = 0; // not written                

        }
        
        //######################################################################
        //######################################################################
        //######################################################################
        
        // go back and write the headers
        avih.goBackAndWrite(raFile);
        strh_vid.goBackAndWrite(raFile);
        bif.goBackAndWrite(raFile);
        
        if (_audioFormat != null) {
            strh_aud.goBackAndWrite(raFile);
            wavfmt.goBackAndWrite(raFile);
        }
        
        // and we're done
        raFile.close();
        raFile = null;
        
        RIFF_chunk = null;
            LIST_hdr1 = null;
                avih = null;
                LIST_strl_vid = null;
                    strf_vid = null;
                        strh_vid = null;
                        bif = null;
                LIST_strl_aud = null;
                    strf_aud = null;
                        strh_aud = null;
                        wavfmt = null;
            LIST_movi = null;
            avioldidx = null;
    }
    
    // -------------------------------------------------------------------------
    // -- Private functions ----------------------------------------------------
    // -------------------------------------------------------------------------
    
    private byte[] image2DIB(BufferedImage bmp, int iSize) throws IOException {
        // first make sure this is a 24 bit RGB image
        ColorModel cm = bmp.getColorModel();
        if (bmp.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            // if not, convert it
            BufferedImage buffer = new BufferedImage( bmp.getWidth(), bmp.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = buffer.createGraphics();
            g.drawImage(bmp, 0, 0, null);
            g.dispose();
            bmp = buffer;
        }
        
        ExposedBAOS oDIBstream;
        if (iSize <= 32) 
            oDIBstream = writeImageToBytes(bmp, new ExposedBAOS());
        else
            // use a ByteArrayOutputStream with the same
            // initial size as the last frame (saves time and memory re-allocation)
            oDIBstream = writeImageToBytes(bmp, new ExposedBAOS(iSize + 54));
        // get the 'bfOffBits' value, which says where the 
        // image data actually starts (should be 54)
        int iDataStart = read32LE(oDIBstream.getBuffer(), 10);
        // return the data from that byte onward
        int iBufferSize = oDIBstream.size() - iDataStart;
        if (_abWriteBuffer == null || _abWriteBuffer.length < iBufferSize)
            _abWriteBuffer = new byte[iBufferSize];
        System.arraycopy(oDIBstream.getBuffer(), iDataStart, _abWriteBuffer, 0, iBufferSize);
        return _abWriteBuffer;
    }
    
    /** Read a 32 little-endian value from a position in an array. */
    private static int read32LE(byte[] ab, int iPos) {
        return ( (ab[iPos+0] & 0xFF) ) |
               ( (ab[iPos+1] & 0xFF) << 8 ) |
               ( (ab[iPos+2] & 0xFF) << 16) |
               ( (ab[iPos+3] & 0xFF) << 24);
    } 
    
    /** Converts a BufferedImage into a frame to be written into a MJPG avi. */
    private ExposedBAOS image2MJPEG(BufferedImage img) throws IOException {
        ExposedBAOS oJpgStream = writeImageToBytes(img, new ExposedBAOS());
        //IO.writeFile("test.bin", abJpg); // debug
        JPEG2MJPEG(oJpgStream.getBuffer());
        return oJpgStream;
    }
    
    private ExposedBAOS writeImageToBytes(BufferedImage img, ExposedBAOS oOut) throws IOException {
        // wrap the ByteArrayOutputStream with a MemoryCacheImageOutputStream
        MemoryCacheImageOutputStream oMemOut = new MemoryCacheImageOutputStream(oOut);
        // set our image writer's output stream
        _imgWriter.setOutput(oMemOut);
        
        // wrap the BufferedImage with a IIOImage
        IIOImage oImgIO = new IIOImage(img, null, null);
        // finally write the buffered image to the output stream 
        // using our parameters (if any)
        _imgWriter.write(null, oImgIO, _writeParams);
        // don't forget to flush
        oMemOut.flush();
        oMemOut.close();
        
        // clear image writer's output stream
        _imgWriter.setOutput(null);
        
        // return the result
        return oOut;
    }
    
    
    /** Converts JPEG file data to be used in an MJPG AVI. */
    private static void JPEG2MJPEG(byte [] ab) throws IOException {
        if (ab[6] != 'J' || ab[7] != 'F' || ab[8] != 'I' || ab[9] != 'F')
            throw new IOException("JFIF header not found in jpeg data, unable to write frame to AVI.");
        // http://cekirdek.pardus.org.tr/~ismail/ffmpeg-docs/mjpegdec_8c-source.html#l00869
        // ffmpeg treats the JFIF and AVI1 header differently. It's probably
        // safer to stick with standard JFIF header since that's what JPEG uses.
        /*
        ab[6] = 'A';
        ab[7] = 'V';
        ab[8] = 'I';
        ab[9] = '1';
        */
    }


    /** Represents an AVI 'chunk'. When created, it saves the current
     *  position in the AVI RandomAccessFile. When endChunk() is called,
     *  it temporarily jumps back to the start of the chunk and records how 
     *  many bytes have been written. */
    private static class Chunk {
        final private long m_lngPos;
        private long m_lngSize = -1;
        
        Chunk(RandomAccessFile raf, String sChunkName) throws IOException {
            AVIstruct.write32LE(raf, AVIstruct.string2int(sChunkName));
            m_lngPos = raf.getFilePointer();
            raf.writeInt(0);
        }
        
         Chunk(RandomAccessFile raf, String sChunkName, String sSubChunkName) throws IOException {
            this(raf, sChunkName);
            AVIstruct.write32LE(raf, AVIstruct.string2int(sSubChunkName));
        }
        
        /** Jumps back to saved position in the RandomAccessFile and writes
         *  how many bytes have passed since the position was saved, then
         *  returns to the current position again. */
        public void endChunk(RandomAccessFile raf) throws IOException {
            long lngCurPos = raf.getFilePointer(); // save this pos
            raf.seek(m_lngPos); // go back to where the header is
            m_lngSize = (lngCurPos - (m_lngPos + 4)); // save number of bytes since start of chunk
            AVIstruct.write32LE(raf, (int)m_lngSize); // write the header size
            raf.seek(lngCurPos); // return to current position
        }

        /** After endChunk() has been called, returns the size that was
         *  written. */
        private long getSize() {
            return m_lngSize;
        }
        
        /** Returns the position where the size will be written when
         *  endChunk() is called. */
        private long getStart() {
            return m_lngPos;
        }
    }
    
}
