/* 
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


/*
 * StrAudioDemuxerDecoderIS.java
 *
 */

package jpsxdec;

import java.util.AbstractList;
import java.util.ArrayList;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;

public class StrAudioDemuxerDecoderIS extends InputStream {


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

    ArrayList<IAudioSector> m_oAudioSectors = new ArrayList<IAudioSector>();
    AudioFormat m_oAudFormat;
    IAudioSector m_oReferenceSector;
    long m_lngLengthInSamples;

    long m_lngFile = -1;
    long m_lngChannel = -1;
    float m_fltScale = 1.0f;

    int m_iCurrentSector = -1;
    Short2DArrayInputStream m_oCurrentDecodedBuffer;
    StrADPCMDecoder m_oAudioDecodingInstance;


    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public StrAudioDemuxerDecoderIS(AbstractList<PSXSector> oSectorList) 
    {
        this(oSectorList, 1.0f);
    }

    public StrAudioDemuxerDecoderIS(AbstractList<PSXSector> oSectorList, 
                                    float fltScale) 
    {
        m_fltScale = fltScale;
        
        for (PSXSector oSector : oSectorList) {
            AddSector(oSector);
        }
        
        // if we're decoding FF8 audio sectors, we need them in twos
        if (m_oReferenceSector instanceof PSXSectorFF8AudioChunk)
            if (m_oAudioSectors.size() % 2 > 0)
                m_oAudioSectors.remove(m_oAudioSectors.size()-1);
        
        m_lngLengthInSamples = CalculateLength();
    }

    private boolean AddSector(PSXSector oSector) {

        if (!(oSector instanceof IAudioSector)) return false;
        
        IAudioSector oAudioSect = (IAudioSector)oSector;

        // If this is the first sector we're looking at
        if (m_oReferenceSector == null) {
            // save the data
            m_lngFile = oSector.getFile();
            m_lngChannel = oSector.getChannel();

            m_oReferenceSector = oAudioSect;

            m_oAudioDecodingInstance = 
                    new StrADPCMDecoder(
                    oAudioSect.getBitsPerSample(), 
                    oAudioSect.getMonoStereo(),
                    m_fltScale);

            m_oAudioSectors.add(oAudioSect);

            m_oAudFormat = new AudioFormat(
                    oAudioSect.getSamplesPerSecond(),
                    16,        // we're turning it into a 16 bit signed pcm
                    oAudioSect.getMonoStereo(), // stereo?
                    true,      // signed
                    false);    // little-endian (not big-endian)

            return true;
        } else {
            // Makre sure the submitted audio is compatable with existing audio
            // (Spyro is known to suddenly change sample rate)
            // Note: This is also checked in the CD indexer
            if (m_lngFile == oSector.getFile() &&
                m_lngChannel == oSector.getChannel() &&
                m_oReferenceSector.matches(oAudioSect))
            {
                m_oAudioSectors.add(oAudioSect);
                return true;
            } else {
                return false;
            }
        }
        

    }
    
    private long CalculateLength() {
        if (m_oReferenceSector instanceof PSXSectorFF8FrameChunk) { 
            return m_oAudioSectors.size() / 2 * 2940;
        } else if (m_oReferenceSector instanceof PSXSectorAudioChunk) { 
            return m_oAudioSectors.size() 
                   * 4032 / m_oReferenceSector.getMonoStereo();
        } else
            return AudioSystem.NOT_SPECIFIED;
    }


    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    AudioFormat getFormat() {
        return m_oAudFormat;
    }

    long getLength() {
        return m_lngLengthInSamples;
    }

    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public int read() throws IOException {
        int iByte = -1;
        while (( m_iCurrentSector < 0 || 
                (iByte = m_oCurrentDecodedBuffer.read()) < 0) 
               && m_iCurrentSector < m_oAudioSectors.size())
        {
            m_iCurrentSector++;
            if (m_iCurrentSector >= m_oAudioSectors.size()) return -1;
            IAudioSector oAudioChunk = m_oAudioSectors.get(m_iCurrentSector);
            PSXSector oAudSect = (PSXSector)oAudioChunk;
            
            int iSampleCount = 4032 / m_oAudioDecodingInstance.getMonoStereo();
            
            DataInputStream oSectorStream = new DataInputStream(oAudSect);
            
            short[][] asiDecoded = 
               m_oAudioDecodingInstance.DecodeMore(oSectorStream, iSampleCount);
            
            m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
        }
        return iByte;
    }

}
