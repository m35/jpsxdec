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
 * SequenceFPIS.java
 */

package jpsxdec.util;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import jpsxdec.sectortypes.IVideoChunkSector;


public class SequenceFPIS extends SequenceInputStream implements IGetFilePointer {

    public static class ArrayEnum implements Enumeration {

        private IVideoChunkSector[] m_ais;
        private int m_i = 0;
        private ByteArrayFPIS m_oCurStream;
        
        public ArrayEnum(IVideoChunkSector[] ais) {
            m_ais = ais;
        }
        
        public boolean hasMoreElements() {
            return (m_i < m_ais.length) && (m_ais[m_i] != null);
        }

        public InputStream nextElement() {
            m_oCurStream = m_ais[m_i].getUserDataStream();
            m_i++;
            return m_oCurStream;
        }
        
        public InputStream curElement() {
            return m_oCurStream;
        }
        
    }
    
    private ArrayEnum m_oEnum;
    
    public SequenceFPIS(ArrayEnum ae) {
        super(ae);
        m_oEnum = ae;
    }

    public long getFilePointer() {
        Object is = m_oEnum.curElement();
        if (is instanceof IGetFilePointer)
            return ((IGetFilePointer)is).getFilePointer();
        else
            return -1;
    }

}
