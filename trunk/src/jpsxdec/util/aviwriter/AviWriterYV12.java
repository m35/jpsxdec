/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.util.aviwriter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;


public class AviWriterYV12 extends AviWriter {

    private final int _iFrameSize, _iFrameYSize, _iFrameCSize;

    public AviWriterYV12(final File outFile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond)
            throws IOException
    {
        this(outFile,
             iWidth, iHeight,
             lngFrames, lngPerSecond,
             null);
    }

    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public AviWriterYV12(final File outFile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond,
                     final AudioFormat oAudioFormat)
            throws IOException
    {
        super(outFile, iWidth, iHeight, lngFrames, lngPerSecond, oAudioFormat, 
                false, "YV12", AVIstruct.string2int("YV12"));

        if (((iWidth | iHeight) & 1) != 0)
            throw new IllegalArgumentException("Dimensions must be divisible by 2");

        _iFrameYSize = iWidth * iHeight;
        _iFrameCSize = iWidth * iHeight / 4;

        _iFrameSize = _iFrameYSize + _iFrameCSize * 2;
    }

    public void write(byte[] abY, byte[] abCr, byte[] abCb) throws IOException {
        if (_abWriteBuffer == null || _abWriteBuffer.length < _iFrameSize)
            _abWriteBuffer = new byte[_iFrameSize];

        if (abY.length != _iFrameYSize)
            throw new IllegalArgumentException("Y data wrong size.");
        if (abCb.length != _iFrameCSize)
            throw new IllegalArgumentException("Cb data wrong size.");
        if (abCr.length != _iFrameCSize)
            throw new IllegalArgumentException("Cr data wrong size.");

        System.arraycopy(abY, 0, _abWriteBuffer, 0, _iFrameYSize);
        System.arraycopy(abCr, 0, _abWriteBuffer, _iFrameYSize, _iFrameCSize);
        System.arraycopy(abCb, 0, _abWriteBuffer, _iFrameYSize+_iFrameCSize, _iFrameCSize);

        writeFrameChunk(_abWriteBuffer, 0, _iFrameSize);
    }
    
    @Override
    public void writeBlankFrame() throws IOException {
        if (_abWriteBuffer == null || _abWriteBuffer.length < _iFrameSize)
            _abWriteBuffer = new byte[_iFrameSize];

        Arrays.fill(_abWriteBuffer, 0, _iFrameYSize, (byte)0);
        Arrays.fill(_abWriteBuffer, _iFrameYSize, _iFrameYSize + _iFrameCSize*2, (byte)128);

        writeFrameChunk(_abWriteBuffer, 0, _iFrameSize);
    }

}
