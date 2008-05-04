/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
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
 * Short2dArrayInputStream.java
 */

package jpsxdec.audiodecoding;

import java.io.IOException;
import java.io.InputStream;


/** Same idea as ByteArrayInputStream, only with a 2D array of shorts. */
public class Short2dArrayInputStream extends InputStream {

    private short[][] m_ShortArray;
    private int m_iSampleIndex = 0;
    private int m_iChannelIndex = 0;
    private int m_iByteIndex = 0;

    public Short2dArrayInputStream(short[][] ShortArray) {
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

