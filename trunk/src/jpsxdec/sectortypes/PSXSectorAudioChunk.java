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
 * PSXSectorAudioChunk.java
 */

package jpsxdec.sectortypes;

import javax.sound.sampled.AudioFormat;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.NotThisTypeException;


/** Standard audio sector for STR for XA. */
public class PSXSectorAudioChunk extends PSXSector {

    private int m_iSamplesPerSec;
    private int m_iBitsPerSample;
    /** 1 for mono, 2 for stereo. */
    private int m_iMonoStereo;

    public PSXSectorAudioChunk(CDXASector oCDSect) throws NotThisTypeException {
        super(oCDSect);

        if (!oCDSect.hasSectorHeader())
            throw new NotThisTypeException();
        if (oCDSect.getSubMode().form != 2)
            throw new NotThisTypeException();
        if (oCDSect.getSubMode().audio != 1)
            throw new NotThisTypeException();

        switch(oCDSect.getCodingInfo_SampleRate()) {
            case 0: m_iSamplesPerSec = 37800; break;
            case 1: m_iSamplesPerSec = 18900; break;
            default: throw new NotThisTypeException();
        }
        switch(oCDSect.getCodingInfo_BitsPerSample()) {
            case 0: m_iBitsPerSample = 4; break;
            case 1: m_iBitsPerSample = 8; break;
            default: throw new NotThisTypeException();
        }
        switch(oCDSect.getCodingInfo_MonoStereo()) {
            case 0: m_iMonoStereo = 1; break; // mono (1 channel)
            case 1: m_iMonoStereo = 2; break; // stereo (2 channels)
            default: throw new NotThisTypeException();
        }
    }

    public String toString() {
        return "Audio " + super.toString() +
                String.format(
                " bits/sample:%d channels:%d samples/sec:%d",
                m_iBitsPerSample,
                m_iMonoStereo,
                m_iSamplesPerSec);
    }

    public int getSamplesPerSecond() {
        return m_iSamplesPerSec;
    }

    public int getBitsPerSample() {
        return m_iBitsPerSample;
    }

    /** 1 for mono, 2 for stereo. */
    public int getMonoStereo() {
        return m_iMonoStereo;
    }

    /** Returns the length of the sector in samples (i.e. 4032 / channels). */
    public long getSampleLength() {
        return 4032 / m_iMonoStereo;
    }
    
    /** Assumes it will be decoded to signed, little-endian, 16 bits-per-sample stream. */
    public AudioFormat getAudioFormat() {
        return new AudioFormat(
                m_iSamplesPerSec,
                16,        // we're turning it into a 16 bit signed pcm
                m_iMonoStereo, // stereo?
                true,      // signed
                false);    // little-endian (not big-endian)            
    }

    /** [extends PSXSector] */
    protected int getDemuxedDataStart(int iDataSize) {
        return 0;
    }
    /** The last 20 bytes of the sector are unused. [extends PSXSector] */
    protected int getDemuxedDataLength(int iDataSize) {
        return iDataSize - 20;
    }

}
