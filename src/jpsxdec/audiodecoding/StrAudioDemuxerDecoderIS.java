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
 * StrAudioDemuxerDecoderIS.java
 */

package jpsxdec.audiodecoding;

import javax.sound.sampled.AudioInputStream;
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
import jpsxdec.audiodecoding.StrADPCMDecoder.ADPCMDecodingContext;

public class StrAudioDemuxerDecoderIS extends InputStream {

    public static int DebugVerbose = 2;

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private AdvancedIOIterator<PSXSector> m_oPsxSectorIterator;
    private Short2DArrayInputStream m_oCurrentDecodedBuffer;
    private double m_dblScale;

    /** Counts the number of samples decoded so far */
    long m_lngCurrentLengthInSamples = 0;

    long m_lngChannel;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public StrAudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter) 
            throws IOException
    {
        this(oPsxIter, 1.0f);
    }
    public StrAudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     int iChannel) 
            throws IOException
    {
        this(oPsxIter, iChannel, 1.0f);
    }
    public StrAudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     double dblScale) 
            throws IOException
    {
        this(oPsxIter, -1, dblScale);
    }

    public StrAudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     int iChannel,
                                     double dblScale)
            throws IOException
    {
        m_dblScale = dblScale;
        m_oPsxSectorIterator = oPsxIter;

        m_lngChannel = iChannel;
        
        short[][] asiDecoded = FindAndDecodeNextSector(m_oPsxSectorIterator);

        if (asiDecoded == null)
            m_oCurrentDecodedBuffer = null;
        else
            m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public long getLength() {
        // since we're decoding one sector at a time, there's
        // no way to determine the length ahead of time
        //System.out.println(AudioSystem.NOT_SPECIFIED);
        return AudioSystem.NOT_SPECIFIED;
    }

    public boolean HasAudio() {
        return m_oCurrentDecodedBuffer != null;
    }

    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    long m_lngFile = -1;
    int m_iBitsPerSample = -1;
    int m_iSamplesPerSec = -1;
    int m_iMonoStereo = -1;

    private ADPCMDecodingContext[] m_oAudioDecodingContexts = new ADPCMDecodingContext[2];
    
    public AudioFormat getFormat() {
        return new AudioFormat(
                m_iSamplesPerSec,
                16,        // we're turning it into a 16 bit signed pcm
                m_iMonoStereo, // stereo?
                true,      // signed
                false);    // little-endian (not big-endian)            
    }

    
    public short[][] FindAndDecodeNextSector(AdvancedIOIterator<PSXSector> oPsxSectorIter) throws IOException {
        PSXSectorAudioChunk oAudSect;
        oAudSect = FindNextMatchingSector(oPsxSectorIter);
        if (oAudSect == null)
            return null;

        DataInputStream oDatStream = new DataInputStream(oAudSect);
        short[][] asiDecoded = 
                StrADPCMDecoder.DecodeMore(oDatStream, 
                                           m_iBitsPerSample, 
                                           m_iMonoStereo, 
                                           m_oAudioDecodingContexts[0], 
                                           m_oAudioDecodingContexts[1]);
        return asiDecoded;
    }

    private PSXSectorAudioChunk FindNextMatchingSector(AdvancedIOIterator<PSXSector> oPsxSectorIter) throws IOException {

        while (oPsxSectorIter.hasNext()) {

            PSXSector oSector = oPsxSectorIter.next();

            if (!(oSector instanceof PSXSectorAudioChunk)) continue;

            PSXSectorAudioChunk oAudioSect = (PSXSectorAudioChunk)oSector;

            // If this is the first sector we're looking at
            if (m_oAudioDecodingContexts[0] == null) {
                if (m_lngChannel < 0 || m_lngChannel == oAudioSect.getChannel()) 
                {
                    m_lngFile = oAudioSect.getFile();
                    m_lngChannel = oAudioSect.getChannel();

                    m_iBitsPerSample = oAudioSect.getBitsPerSample();
                    m_iSamplesPerSec = oAudioSect.getSamplesPerSecond();
                    m_iMonoStereo = oAudioSect.getMonoStereo();

                    m_oAudioDecodingContexts[0] = new ADPCMDecodingContext(m_dblScale);
                    if (m_iMonoStereo == 2)
                        m_oAudioDecodingContexts[1] = new ADPCMDecodingContext(m_dblScale);
                    else
                        m_oAudioDecodingContexts[1] = null;

                    return oAudioSect;
                }

            } else {
                // Makre sure the next audio sector is compatable with existing audio
                // (Spyro is known to suddenly change sample rate)
                if (m_lngFile == oAudioSect.getFile() &&
                    m_lngChannel == oAudioSect.getChannel() &&
                    m_iBitsPerSample == oAudioSect.getBitsPerSample() &&
                    m_iSamplesPerSec == oAudioSect.getSamplesPerSecond() &&
                    m_iMonoStereo == oAudioSect.getMonoStereo())
                {
                    return oAudioSect;
                }
            }
        }
        return null;

    }

    // ..................................................................
    
    int m_iBytesRead = 0;
    public int read() throws IOException {
        
        if (m_oCurrentDecodedBuffer == null) return -1;
        
        int iByte = m_oCurrentDecodedBuffer.read();
        while (iByte < 0)
        {
            if (DebugVerbose > 5)
                System.out.println("Bytes read from sector: " + m_iBytesRead);
            
            short[][] asiDecoded = FindAndDecodeNextSector(m_oPsxSectorIterator);

            if (asiDecoded == null) {
                m_oCurrentDecodedBuffer = null;
                return -1;
            } else {
                m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
            }
            
            m_iBytesRead = 0;
            iByte = m_oCurrentDecodedBuffer.read(); // try again
        }
        m_iBytesRead++;
        return iByte;
    }

}
