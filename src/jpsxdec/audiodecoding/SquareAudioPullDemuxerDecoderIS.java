/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * SquareAudioPullDemuxerDecoderIS.java
 */

package jpsxdec.audiodecoding;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.sectortypes.ISquareAudioSector;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.util.AdvancedIOIterator;

/** Square games Final Fantasy 8, Final Fantasy 9, and Chrono Cross store the 
 * audio in similar format. This decodes the audio one sector at a time
 * as it is read. */
public class SquareAudioPullDemuxerDecoderIS extends InputStream {

    public static int DebugVerbose = 2;

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Iterate through the sectors of the disc. */
    private AdvancedIOIterator<PSXSector> m_oPsxSectorIterator;

    /** Buffer to hold the current decoded PCM data that will be sent out
     *  through the read() functon. This will be null when there is no
     *  more data to be read. */
    private Short2dArrayInputStream m_oCurrentDecodedBuffer;
    
    /** Decoding context for the left channel. */
    private ADPCMDecodingContext m_oLeftContext;
    /** Decoding context for the right channel. */
    private ADPCMDecodingContext m_oRightContext;
    
    private long m_lngFrame = 1;
    private int m_iSamplesPerSecond = -1;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public SquareAudioPullDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter) 
            throws IOException
    {
        this(oPsxIter, 1.0);
    }
    public SquareAudioPullDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                           double dblScale)
            throws IOException
    {
        m_oPsxSectorIterator = oPsxIter;
        
        // FF8/FF9/Chrono Cross audio is always in stereo, so create the
        // ADPCM contexts for left and right channels.
        m_oLeftContext = new ADPCMDecodingContext(dblScale);
        m_oRightContext = new ADPCMDecodingContext(dblScale);
        
        short[][] asiDecoded = FindAndDecodeNextFrame(m_oPsxSectorIterator, m_lngFrame);

        if (asiDecoded == null)
            m_oCurrentDecodedBuffer = null;
        else
            m_oCurrentDecodedBuffer = new Short2dArrayInputStream(asiDecoded);
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public AudioFormat getFormat() {
        return new AudioFormat(
                m_iSamplesPerSecond,
                16,        // we're turning it into a 16 bit signed pcm
                2,         // stereo
                true,      // signed
                false);    // little-endian (not big-endian)            
    }

    public long getLength() {
        // I'd have to pre-calculate the length during indexing,
        // but I don't really want to.
        return AudioSystem.NOT_SPECIFIED;
    }

    /** Returns true if there is audio remaining to be read. */
    public boolean hasAudio() {
        return m_oCurrentDecodedBuffer != null;
    }
    
    public int getMonoStereo() {
        return 2;
    }
    
    public int getSamplesPerSec() {
        return m_iSamplesPerSecond;
    }

    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public int read() throws IOException {
        
        if (m_oCurrentDecodedBuffer == null) return -1;
        
        int iByte = m_oCurrentDecodedBuffer.read();
        while (iByte < 0)
        {
            m_lngFrame++;
            short[][] asiDecoded = FindAndDecodeNextFrame(m_oPsxSectorIterator, m_lngFrame);

            if (asiDecoded == null) {
                m_oCurrentDecodedBuffer = null;
                return -1;
            } else {
                m_oCurrentDecodedBuffer = new Short2dArrayInputStream(asiDecoded);
            }
            
            iByte = m_oCurrentDecodedBuffer.read(); // try again
        }
        return iByte;
    }

   
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    
    private short[][] FindAndDecodeNextFrame(AdvancedIOIterator<PSXSector> oIter, long frame) 
            throws IOException 
    {
        PSXSector oSector;
        ISquareAudioSector oAudioSect;
        
        ISquareAudioSector oLeftChunk = null;
        ISquareAudioSector oRightChunk = null;
        
        // seek ahead until we find the first sector of the desired frame
        while (oIter.hasNext()) {

            oSector = oIter.next();
            if (!(oSector instanceof ISquareAudioSector)) continue;
            oAudioSect = (ISquareAudioSector)oSector;

            if (oAudioSect.getFrameNumber() < frame) 
                continue;
            else if (oAudioSect.getFrameNumber() > frame) 
                throw new IOException("Error demuxing audio: Already past target frame");
                
            if (oAudioSect.getAudioChunkNumber() != 0) 
                throw new IOException("Error demuxing audio: Already past target sector");

            oLeftChunk = oAudioSect;
            break;
        }
        
        // if we're at the end of the iterator, we're done
        if (oLeftChunk == null) return null;
        
        // then the next sector should be the right audio channel
        if (!oIter.hasNext()) {
            oSector = oIter.next();
            if (!(oSector instanceof ISquareAudioSector)) {
                oAudioSect = (ISquareAudioSector)oSector;
                if (oAudioSect.getFrameNumber() == frame &&
                    oAudioSect.getAudioChunkNumber() == 1)
                {
                    oRightChunk = oAudioSect;
                }
            }
        }
        
        if (oRightChunk == null)
            throw new IOException("Error demuxing audio: Found only half the channels");

        if (oLeftChunk.getSamplesPerSecond() != oRightChunk.getSamplesPerSecond())
            // should never happen
            throw new IOException("Error demuxing audio: Left/Right channels have different samples/second");
        
        if (m_iSamplesPerSecond < 0) // first sector gives us samples/sector
            m_iSamplesPerSecond = oLeftChunk.getSamplesPerSecond();
        else if (m_iSamplesPerSecond != oLeftChunk.getSamplesPerSecond())
            // should never happen
            throw new IOException("Error demuxing audio: Samples/second changed");
        
        short[][] asi = new short[2][];
        
        asi[0] = SquareADPCMDecoder.DecodeMore(oLeftChunk.getUserDataStream(),
                                                m_oLeftContext,
                                                oLeftChunk.getAudioDataSize());
            
        asi[1] = SquareADPCMDecoder.DecodeMore(oRightChunk.getUserDataStream(),
                                                m_oRightContext,
                                                oRightChunk.getAudioDataSize());
        return asi;
    }

}
