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
 * StrAudioPullDemuxerDecoderIS.java
 */

package jpsxdec.audiodecoding;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import jpsxdec.*;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSector.*;
import jpsxdec.util.AdvancedIOIterator;
import jpsxdec.util.IO.Short2DArrayInputStream;

/** Demuxes audio sectors together, and decodes them. */
public class StrAudioPullDemuxerDecoderIS extends InputStream {

    public static int DebugVerbose = 2;

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Iterate through the sectors of the disc. */
    private AdvancedIOIterator<PSXSector> m_oPsxSectorIterator;
    
    /** Buffer to hold the current decoded PCM data that will be sent out
     *  through the read() functon. This will be null when there is no
     *  more data to be read. */
    private Short2DArrayInputStream m_oCurrentDecodedBuffer;
    
    /** How much to scale the decoded audio by before clamping. */
    private double m_dblScale;

    /** Channel to decode. */
    private long m_lngChannel;

    /** Sector file number (pretty much always 1). -1 for uninitialized. */
    private long m_lngFile = -1;
    /** bits per ADPCM sample. -1 for uninitialized. */
    private int m_iBitsPerSample = -1;
    /** Samples/second. -1 for uninitialized. */
    private int m_iSamplesPerSec = -1;
    /** 1 for mono, 2 for stereo, -1 for uninitialized. */
    private int m_iMonoStereo = -1;

    /** Hold's the decoding contexts for the left/right audio channels. 
     *  If the audio is mono, then only 1 decoding context will be put in 
     *  this array. If stereo, then 2 decoding context will be put 
     *  in this array. */
    private ADPCMDecodingContext[] m_oAudioDecodingContexts = new ADPCMDecodingContext[2];
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Decodes the first audio channel found. 
     * @param oPsxIter  Sector iterator */
    public StrAudioPullDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter) 
            throws IOException
    {
        this(oPsxIter, 1.0f);
    }
    /**  @param oPsxIter  Sector iterator 
     *   @param iChannel  Channel to decode. If -1, decodes the first channel found. */
    public StrAudioPullDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     int iChannel) 
            throws IOException
    {
        this(oPsxIter, iChannel, 1.0f);
    }
    /** Decodes the first audio channel found. 
     * @param oPsxIter  Sector iterator 
     * @param dblScale  How much to scale the decoded audio before clamping */
    public StrAudioPullDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     double dblScale) 
            throws IOException
    {
        this(oPsxIter, -1, dblScale);
    }

    /** 
     * @param oPsxIter  Sector iterator 
     * @param dblScale  How much to scale the decoded audio before clamping
     * @param iChannel  Channel to decode. If -1, decodes the first channel found. */
    public StrAudioPullDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     int iChannel,
                                     double dblScale)
            throws IOException
    {
        // save the properties
        m_dblScale = dblScale;
        m_oPsxSectorIterator = oPsxIter;
        m_lngChannel = iChannel;
        
        // try to decode the first sector
        short[][] asiDecoded = FindAndDecodeNextSector(m_oPsxSectorIterator);

        if (asiDecoded == null)
            m_oCurrentDecodedBuffer = null;
        else
            m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** May someday return the length of the audio in samples, but currently
     *  just returns AudioSystem.NOT_SPECIFIED. */
    public long getLength() {
        // I'd have to pre-calculate the length during indexing,
        // but I don't really want to.
        return AudioSystem.NOT_SPECIFIED;
    }

    /** Returns true if there is audio remaining to be read. */
    public boolean hasAudio() {
        return m_oCurrentDecodedBuffer != null;
    }

    public AudioFormat getFormat() {
        return new AudioFormat(
                m_iSamplesPerSec,
                16,        // we're turning it into a 16 bit signed pcm
                m_iMonoStereo, // stereo?
                true,      // signed
                false);    // little-endian (not big-endian)            
    }
    
    /** [InputStream] {@inheritDoc} */ @Override 
    public int read() throws IOException {
        
        if (m_oCurrentDecodedBuffer == null) return -1;
        
        int iByte = m_oCurrentDecodedBuffer.read();
        while (iByte < 0)
        {
            short[][] asiDecoded = FindAndDecodeNextSector(m_oPsxSectorIterator);

            if (asiDecoded == null) {
                m_oCurrentDecodedBuffer = null;
                return -1;
            } else {
                m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
            }
            
            iByte = m_oCurrentDecodedBuffer.read(); // try again
        }
        
        return iByte;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private short[][] FindAndDecodeNextSector(AdvancedIOIterator<PSXSector> oPsxSectorIter) throws IOException {
        PSXSectorAudioChunk oAudSect;
        // search for the next sector to decode
        oAudSect = FindNextMatchingSector(oPsxSectorIter);
        // return null if nothing found
        if (oAudSect == null)
            return null;

        // now decode the data
        DataInputStream oDatStream = new DataInputStream(oAudSect);
        short[][] asiDecoded = 
                StrADPCMDecoder.DecodeMore(oDatStream, 
                                           m_iBitsPerSample, 
                                           m_iMonoStereo, 
                                           m_oAudioDecodingContexts[0], 
                                           m_oAudioDecodingContexts[1]);
        // and return it
        return asiDecoded;
    }

    private PSXSectorAudioChunk FindNextMatchingSector(AdvancedIOIterator<PSXSector> oPsxSectorIter) throws IOException {

        while (oPsxSectorIter.hasNext()) {
            // get the next sector of the disc
            PSXSector oSector = oPsxSectorIter.next();
            // skip it if it's not audio
            if (!(oSector instanceof PSXSectorAudioChunk)) continue;

            PSXSectorAudioChunk oAudioSect = (PSXSectorAudioChunk)oSector;

            // If this is the first sector we're looking at
            if (m_oAudioDecodingContexts[0] == null) {
                // and if it's the channel we want (or just accept it
                // if we don't care about the channel)
                if (m_lngChannel < 0 || m_lngChannel == oAudioSect.getChannel()) 
                {
                    // save properties of the audio sector
                    m_lngFile = oAudioSect.getFile();
                    m_lngChannel = oAudioSect.getChannel();
                    m_iBitsPerSample = oAudioSect.getBitsPerSample();
                    m_iSamplesPerSec = oAudioSect.getSamplesPerSecond();
                    m_iMonoStereo = oAudioSect.getMonoStereo();

                    // create the first decoding context
                    m_oAudioDecodingContexts[0] = new ADPCMDecodingContext(m_dblScale);
                    // and if it's stereo, create a second
                    if (m_iMonoStereo == 2)
                        m_oAudioDecodingContexts[1] = new ADPCMDecodingContext(m_dblScale);
                    else
                        m_oAudioDecodingContexts[1] = null;

                    // return the matching sector
                    return oAudioSect;
                }

            } else {
                // Make sure the next audio sector is compatable with existing audio
                // (e.g. Spyro is known to suddenly change sample rate)
                // Note that this should have already been handled during
                // indexing of the CD. But we'll do it here again just in case.
                if (m_lngFile == oAudioSect.getFile() &&
                    m_lngChannel == oAudioSect.getChannel() &&
                    m_iBitsPerSample == oAudioSect.getBitsPerSample() &&
                    m_iSamplesPerSec == oAudioSect.getSamplesPerSecond() &&
                    m_iMonoStereo == oAudioSect.getMonoStereo())
                {
                    // return the matching sector
                    return oAudioSect;
                }
            }
        }
        
        // end of disc sectors. no more matches left
        return null;
    }

}
