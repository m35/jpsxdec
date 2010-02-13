/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.plugins.psx.lain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.psx.str.SectorSTR;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


public class SectorLainVideo extends SectorSTR {

    private byte _bQuantizationScaleLumin;
    private byte _bQuantizationScaleChrom;

    public SectorLainVideo(CDSector cdSector) throws NotThisTypeException {
        super(cdSector); // will call overridden readHeader() method
    }

    @Override
    protected void readHeader(ByteArrayInputStream inStream) throws NotThisTypeException, IOException {
        _lngMagic = IO.readUInt32LE(inStream);
        if (_lngMagic != VIDEO_CHUNK_MAGIC)
            throw new NotThisTypeException();

        _iChunkNumber = IO.readSInt16LE(inStream);
        if (_iChunkNumber < 0)
            throw new NotThisTypeException();
        _iChunksInThisFrame = IO.readSInt16LE(inStream);
        if (_iChunksInThisFrame < 1)
            throw new NotThisTypeException();
        _iFrameNumber = IO.readSInt32LE(inStream);
        if (_iFrameNumber < 1)
            throw new NotThisTypeException();

        _lngUsedDemuxedSize = IO.readSInt32LE(inStream);
        if (_lngUsedDemuxedSize < 0)
            throw new NotThisTypeException();

        _iWidth = IO.readSInt16LE(inStream);
        if (_iWidth != 320)
            throw new NotThisTypeException();
        _iHeight = IO.readSInt16LE(inStream);
        if (_iHeight != 240)
            throw new NotThisTypeException();

        _bQuantizationScaleLumin = IO.readSInt8(inStream);
        if (_bQuantizationScaleLumin < 0)
            throw new NotThisTypeException();
        _bQuantizationScaleChrom = IO.readSInt8(inStream);
        if (_bQuantizationScaleChrom < 0)
            throw new NotThisTypeException();

        _lngMagic3800 = IO.readUInt16LE(inStream);
        if (_lngMagic3800 != 0x3800 && _lngMagic3800 != 0x0000 && _lngMagic3800 != _iFrameNumber)
            throw new NotThisTypeException();

        _lngRunLengthCodeCount = IO.readUInt16LE(inStream);

        _lngVersion = IO.readUInt16LE(inStream);
        if (_lngVersion != 0)
            throw new NotThisTypeException();

        _lngFourZeros = IO.readUInt32LE(inStream);
        if (_lngFourZeros != 0)
            throw new NotThisTypeException();
    }

    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d %dx%d ver:%d " +
            "{demux frame size=%d rlc=%d 3800=%04x qscaleL=%d qscaleC=%d 4*00=%08x}",
            getTypeName(),
            super.cdToString(),
            _iFrameNumber,
            _iChunkNumber,
            _iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _lngVersion,
            _lngUsedDemuxedSize,
            _lngRunLengthCodeCount,
            _lngMagic3800,
            _bQuantizationScaleLumin,
            _bQuantizationScaleChrom,
            _lngFourZeros
            );
    }

    @Override
    public JPSXPlugin getSourcePlugin() {
        return JPSXPluginLain.getPlugin();
    }

    @Override
    public String getTypeName() {
        return "Lain Video";
    }

}
