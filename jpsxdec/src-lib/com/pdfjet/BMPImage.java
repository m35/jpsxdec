/**
 * 
 *  Copyright (c) 2010 Jonas Krogsbll

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
 
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and / or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.pdfjet;


import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


public class BMPImage {

    int w = 0;              // Image width in pixels
    int h = 0;              // Image height in pixels

    byte[] image;           // The reconstructed image data
    byte[] deflated;        // The deflated reconstructed image data

    private int bpp;
    private byte palette[][];
    private boolean r5g6b5; // If 16 bit image two encodings can occur
    
    private static final int m10000000 = 0x80;
    private static final int m01000000 = 0x40;
    private static final int m00100000 = 0x20;
    private static final int m00010000 = 0x10;
    private static final int m00001000 = 0x08;
    private static final int m00000100 = 0x04;
    private static final int m00000010 = 0x02;
    private static final int m00000001 = 0x01;
    private static final int m11110000 = 0xF0;
    private static final int m00001111 = 0x0F;

    /* Tested with images created from GIMP */
    public BMPImage( java.io.InputStream is ) throws Exception {
        palette = null;
        byte bm[] = getBytes(is, 2);
        // From Wikipedia
        if((bm[0] == 'B' && bm[1] == 'M')||
           (bm[0] == 'B' && bm[1] == 'A')||
           (bm[0] == 'C' && bm[1] == 'I')||
           (bm[0] == 'C' && bm[1] == 'P')||
           (bm[0] == 'I' && bm[1] == 'C')||
           (bm[0] == 'P' && bm[1] == 'T')) {
            skipNBytes(is, 8);
            int offset = readSignedInt(is);
            int sizeOfHeader = readSignedInt(is);
            w = readSignedInt(is);
            h = readSignedInt(is);
            skipNBytes(is, 2);
            bpp = read2BytesLE(is);
            int compression = readSignedInt(is);
            if (bpp > 8) {
                r5g6b5 = (compression == 3);
                skipNBytes(is, 20);
                // if (sizeOfHeader > 40) {
                //     skipNBytes(is, sizeOfHeader-40);
                // }
                if (offset > 54) {
                    skipNBytes(is, offset-54);
                }
            } else {
                skipNBytes(is, 12);
                int numpalcol = readSignedInt(is);
                skipNBytes(is, 4);
                parsePalette(is, numpalcol);
            }
            parseData(is);
        } else {
            throw new Exception("BMP data could not be parsed!");
        }

    }

    private void parseData(java.io.InputStream is) throws Exception {
        // rowsize is 4 * ceil (bpp*width/32.0)
        image = new byte[w * h * 3];

        int rowsize = 4 * (int)Math.ceil(bpp*w/32.0);   // 4 byte alignment
        // hiv hver rkke ud:
        byte row[];
        int index;
        try {
            for (int i = 0; i < h; i++) {
                row = getBytes(is, rowsize);
                switch (bpp) {
                case  1: row = bit1to8(row, w); break;  // opslag i palette
                case  4: row = bit4to8(row, w); break;  // opslag i palette
                case  8: break;                         // opslag i palette
                case 16: 
                    if (r5g6b5)
                        row = bit16to24(row, w);        // 5,6,5 bit
                    else 
                        row = bit16to24b(row, w);
                    break;
                case 24: break;                         // bytes are correct
                case 32: row = bit32to24(row, w); break;
                default:
                    throw new Exception(
                            "Can only parse 1 bit, 4bit, 8bit, 16bit, 24bit and 32bit images");
                }

                index = w*(h-i-1)*3;
                if (palette != null) {  // indexed
                    for (int j = 0; j < w; j++) {
                        image[index++] = palette[(row[j]<0)?row[j]+256:row[j]][2];
                        image[index++] = palette[(row[j]<0)?row[j]+256:row[j]][1];
                        image[index++] = palette[(row[j]<0)?row[j]+256:row[j]][0];
                    }
                } else {                // not indexed
                    for (int j = 0; j < w*3; j+=3) {
                        image[index++] = row[j+2];
                        image[index++] = row[j+1];
                        image[index++] = row[j+0];
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            throw new Exception ("BMP parse error: imagedata not correct");
        }

        ByteArrayOutputStream data2 = new ByteArrayOutputStream(32768);
        DeflaterOutputStream dos = new DeflaterOutputStream(data2, new Deflater());
        dos.write(image, 0, image.length);
        dos.finish();
        deflated = data2.toByteArray();
    }

    // 5 + 6 + 5 in B G R format 2 bytes to 3 bytes
    private static byte[] bit16to24(byte[] row, int width) {
        byte[] ret = new byte[width * 3];
        int j = 0;
        for (int i = 0; i < width*2; i+=2) {
            // System.out.println("B1: " + row[i] + ", B2: " + row[i+1]);
            ret[j++] = (byte)((row[i] & 0x1F)<<3);
            ret[j++] = (byte)(((row[i+1] & 0x07)<<5)+((row[i] & 0xE0)>>3));
            ret[j++] = (byte)((row[i+1] & 0xF8));
            // System.out.println("green: " + ret[j-1]);
        }
        return ret;
    }

    // 5 + 5 + 5 in B G R format 2 bytes to 3 bytes
    private static byte[] bit16to24b(byte[] row, int width) {
        byte[] ret = new byte[width * 3];
        int j = 0;
        for (int i = 0; i < width*2; i+=2) {
            // System.out.println("B1: " + row[i] + ", B2: " + row[i+1]);
            ret[j++] = (byte)((row[i] & 0x1F)<<3);
            ret[j++] = (byte)(((row[i+1] & 0x03)<<6)+((row[i] & 0xE0)>>2));
            ret[j++] = (byte)((row[i+1] & 0x7C)<<1);
            // System.out.println("green: " + ret[j-1]);
        }
        return ret;
    }

    /* alpha first? */
    private static byte[] bit32to24(byte[] row, int width) {
        byte[] ret = new byte[width * 3];
        int j = 0;
        for (int i = 0; i < width*4; i+=4) {
            ret[j++] = row[i+1];
            ret[j++] = row[i+2];
            ret[j++] = row[i+3]; 
        }
        return ret;
    }

    private static byte[] bit4to8(byte[] row, int width) {
        byte[] ret = new byte[width];
        for(int i = 0; i < width; i++) {
            if (i % 2 == 0) {
                ret[i] =(byte) ((row[i/2] & m11110000)>>4);
            } else {
                ret[i] =(byte) ((row[i/2] & m00001111));
            }
        }
        return ret;
    }

    private static byte[] bit1to8(byte[] row, int width) {
        byte[] ret = new byte[width];
        for(int i = 0; i < width; i++) {
            switch (i % 8) {
            case 0: ret[i] =(byte) ((row[i/8] & m10000000)>>7); break;
            case 1: ret[i] =(byte) ((row[i/8] & m01000000)>>6); break;
            case 2: ret[i] =(byte) ((row[i/8] & m00100000)>>5); break;
            case 3: ret[i] =(byte) ((row[i/8] & m00010000)>>4); break;
            case 4: ret[i] =(byte) ((row[i/8] & m00001000)>>3); break;
            case 5: ret[i] =(byte) ((row[i/8] & m00000100)>>2); break;
            case 6: ret[i] =(byte) ((row[i/8] & m00000010)>>1); break;
            case 7: ret[i] =(byte) ((row[i/8] & m00000001)); break;
            }
        }
        return ret;
    }

    private void parsePalette(java.io.InputStream is, int size)
            throws Exception {
        palette = new byte[size][];
        for (int i = 0; i < size; i++) palette[i] = getBytes(is, 4);

        //  byte skipzero;
        //  for (int i = 0; i < size; i++) {
        //      palette[i] = getBytes(is, 3);
        //      skipzero = getBytes(is, 1)[0];
        //      if (skipzero != (byte)0) {
        //          System.out.println("Error "+skipzero);
        //      }
        //  }
    }

    private void skipNBytes( java.io.InputStream inputStream, int n ) {
        try {
            getBytes(inputStream, n);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] getBytes( java.io.InputStream inputStream, int length )
            throws Exception {
        byte[] buf = new byte[ length ];
        inputStream.read( buf, 0, buf.length );
        return buf;
    }

    private int read2BytesLE(java.io.InputStream inputStream)
            throws Exception {
        byte[] buf = getBytes(inputStream, 2);
        int val = 0;
        val |= buf[ 1 ] & 0xff;
        val <<= 8;
        val |= buf[ 0 ] & 0xff;
        return val;
    }

    private int readSignedInt(java.io.InputStream inputStream)
            throws Exception {
        byte[] buf = getBytes(inputStream, 4);
        long val = 0L;
        val |= buf[ 3 ] & 0xff;
        val <<= 8;
        val |= buf[ 2 ] & 0xff;
        val <<= 8;
        val |= buf[ 1 ] & 0xff;
        val <<= 8;
        val |= buf[ 0 ] & 0xff;
        return (int)val;
    }

    public int getWidth() {
        return this.w;
    }

    public int getHeight() {
        return this.h;
    }

    public byte[] getData() {
        return this.deflated;
    }

}
