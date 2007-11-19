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
 *
 */

package jpsxdec;

import java.util.AbstractList;
import java.util.ArrayList;
//import java.util.Iterator;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import jpsxdec.PSXSector.*;

public class StrAudioDemuxerDecoderIS extends InputStream {

    public static int DebugVerbose = 2;

    /** Same idea as ByteArrayInputStream, only with a 2D array of shorts. */
    private static class Short2DArrayInputStream extends InputStream {

        private short[][] m_ShortArray;
        private int m_iSampleIndex = 0;
        private int m_iChannelIndex = 0;
        private int m_iByteIndex = 0;

        public Short2DArrayInputStream(short[][] ShortArray) {
            m_ShortArray = ShortArray;
        }

        public int read() throws IOException {
            if (m_iSampleIndex >= m_ShortArray[m_iChannelIndex].length) 
                return -1;

            int iRet = m_ShortArray[m_iChannelIndex][m_iSampleIndex];

            if (m_iByteIndex == 0)
                iRet &= 0xFF;
            else // m_iByteIndex == 1
                iRet = (iRet >>> 8) & 0xFF;

            Increment();

            return iRet;

        }

        private void Increment() {
            m_iByteIndex = (m_iByteIndex + 1) % 2;
            if (m_iByteIndex == 0) { // if m_iByteIndex overflowed
                m_iChannelIndex = (m_iChannelIndex + 1) % m_ShortArray.length;
                if (m_iChannelIndex == 0) { // if m_iChannelIndex overflowed
                    m_iSampleIndex++;
                }
            }
        }
    }
    /* ...................................................................... */
    /* ...................................................................... */
    

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    AdvancedIOIterator<PSXSector> m_oPsxSectorIterator;
    AudioFormat m_oAudFormat;
    IAudioSector m_oCurrentAudioSector;
    
    /** Counts the number of samples decoded so far */
    long m_lngCurrentLengthInSamples = 0;
    float m_fltScale = 1.0f;

    long m_lngFile = -1;
    long m_lngChannel = -1;
    
    //int m_iCurrentSector = -1;
    private Short2DArrayInputStream m_oCurrentDecodedBuffer;
    private StrADPCMDecoder m_oAudioDecodingInstance;

    private long m_lngLastFilePointer = -1;

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
                                    float fltScale) 
            throws IOException
    {
        this(oPsxIter, -1, fltScale);
    }

    public StrAudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                    int iChannel,
                                    float fltScale)
            throws IOException
    {
        m_oPsxSectorIterator = oPsxIter;
        m_fltScale = fltScale;
        m_lngChannel = iChannel;
        
        m_oCurrentAudioSector = FindNextMatchingSector();
        
        // no audio found?
        if (m_oCurrentAudioSector == null) return;
        
        // make a stream out of it
        PSXSector oAudSect = (PSXSector)m_oCurrentAudioSector;
        DataInputStream oSectorStream = new DataInputStream(oAudSect);
        
        // add the clip length to the full length
        m_lngCurrentLengthInSamples += m_oCurrentAudioSector.getSampleLength();
        
        if (DebugVerbose > 5)
            System.out.println("Reading another audio sector: " 
                                + m_oCurrentAudioSector.getFilePointer());
        
        short[][] asiDecoded = 
           m_oAudioDecodingInstance.DecodeMore(oSectorStream, m_oCurrentAudioSector.getSampleLength());

        m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
        
    }

    private IAudioSector FindNextMatchingSector() throws IOException {

        while (m_oPsxSectorIterator.hasNext()) {
        
            PSXSector oSector = m_oPsxSectorIterator.next();
            
            if (!(oSector instanceof IAudioSector)) continue;

            IAudioSector oAudioSect = (IAudioSector)oSector;

            // If this is the first sector we're looking at
            if (m_lngFile < 0) {
                if (m_lngChannel < 0 || m_lngChannel == oSector.getChannel()) 
                {
                    // save the data
                    m_lngFile = oSector.getFile();
                    m_lngChannel = oSector.getChannel();

                    m_oAudioDecodingInstance = 
                            new StrADPCMDecoder(
                            oAudioSect.getBitsPerSample(), 
                            oAudioSect.getMonoStereo(),
                            m_fltScale);

                    m_oAudFormat = new AudioFormat(
                            oAudioSect.getSamplesPerSecond(),
                            16,        // we're turning it into a 16 bit signed pcm
                            oAudioSect.getMonoStereo(), // stereo?
                            true,      // signed
                            false);    // little-endian (not big-endian)

                    return oAudioSect;
                }

            } else {
                // Makre sure the submitted audio is compatable with existing audio
                // (Spyro is known to suddenly change sample rate)
                if (m_lngFile == oSector.getFile() &&
                    m_lngChannel == oSector.getChannel() &&
                    m_oCurrentAudioSector.matches(oAudioSect))
                {
                    //TODO: Also make sure if it's FF8 audio that we
                    // alternate between left/right
                    // if we're decoding FF8 audio sectors, we need them in twos
                    //if (oAudioSect instanceof PSXSectorFF8AudioChunk)
                    return oAudioSect;
                }
            }
        }
        return null;

    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Get the current position in the current sector being read.
     *  [implements IGetFilePointer] */
    public long getFilePointer() {
        if (m_oCurrentAudioSector == null)
            return m_lngLastFilePointer;
        else
            return m_oCurrentAudioSector.getFilePointer();
    }
    
    AudioFormat getFormat() {
        return m_oAudFormat;
    }

    long getLength() {
        // since we're decoding one sector at a time, there's
        // no way to determine the length ahead of time
        //System.out.println(AudioSystem.NOT_SPECIFIED);
        return AudioSystem.NOT_SPECIFIED;
    }

    boolean HasAudio() {
        return m_oCurrentAudioSector != null;
    }

    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    int m_iBytesRead = 0;
    public int read() throws IOException {
        if (m_oCurrentDecodedBuffer == null) return -1;
        int iByte = m_oCurrentDecodedBuffer.read();
        while (iByte < 0)
        {
            if (DebugVerbose > 5)
                System.out.println("Bytes read from sector: " + m_iBytesRead);
            
            m_lngLastFilePointer = m_oCurrentAudioSector.getFilePointer();
            
            m_oCurrentAudioSector = FindNextMatchingSector();
            if (m_oCurrentAudioSector == null) {
                m_oCurrentDecodedBuffer = null;
                return -1;
            }
            if (DebugVerbose > 5)
                System.out.println("Reading another audio sector: " 
                                    + m_oCurrentAudioSector.getFilePointer());
            
            PSXSector oAudSect = (PSXSector)m_oCurrentAudioSector;
            
            DataInputStream oSectorStream = new DataInputStream(oAudSect);
            
            short[][] asiDecoded = 
               m_oAudioDecodingInstance.DecodeMore(oSectorStream, m_oCurrentAudioSector.getSampleLength());
            
            m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
            m_iBytesRead = 0;
            iByte = m_oCurrentDecodedBuffer.read(); // try again
        }
        m_iBytesRead++;
        return iByte;
    }

}
