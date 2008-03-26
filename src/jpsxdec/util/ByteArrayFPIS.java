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
 * ByteArrayFPIS.java
 */

package jpsxdec.util;

import java.io.ByteArrayInputStream;


public class ByteArrayFPIS extends ByteArrayInputStream implements IGetFilePointer {

    private long m_lngFP = 0;
    
    public ByteArrayFPIS(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }

    public ByteArrayFPIS(byte[] buf) {
        super(buf);
    }
    
    public ByteArrayFPIS(byte[] buf, int offset, int length, long filePos) {
        super(buf, offset, length);
        m_lngFP = filePos;
    }

    public ByteArrayFPIS(byte[] buf, long filePos) {
        super(buf);
        m_lngFP = filePos;
    }

    public ByteArrayFPIS(ByteArrayFPIS bafp, int offset, int length, long filePos) {
        super(bafp.buf, bafp.pos + offset, length);
        m_lngFP = filePos;
    }
    
    public long getFilePointer() {
        return m_lngFP + super.pos;
    }
    
    public ByteArrayFPIS copy() {
        return new ByteArrayFPIS(super.buf, super.pos, super.count, m_lngFP);
    }
    
    public int size() {
        return super.count - super.pos;
    }
}
