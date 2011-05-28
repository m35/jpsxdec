/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.lain;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import javax.imageio.ImageIO;
import jpsxdec.tim.Tim;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

public class LainAudioImageWriter {

    public static void main_(String[] args) throws IOException {
        RandomAccessFile raf = new RandomAccessFile("..\\..\\mystery.pk", "r");
        Lain_Pk.DEBUG = 10;
        
        IO.skip(raf, 4); // skip 'napk'
        
        byte[] ab = Lain_Pk.decompress(raf);
        
        raf.close();
        jpsxdec.util.IO.writeIStoFile(new ByteArrayInputStream(ab), "..\\..\\mystery.tim");
        
        //if (raf != null ) return;
        FileOutputStream fos = new FileOutputStream("..\\..\\mystery.pk2");
        
        fos.write(new byte[] {'n', 'a', 'p', 'k'});
        
        Lain_Pk.compress(ab, fos);
        
        fos.close();
    }

    public static int testZipTim(String sSiteFile, String sRecreated) {
        
        try {
            RandomAccessFile inFile = new RandomAccessFile(sSiteFile, "r");
            FileOutputStream outFile = new FileOutputStream(sRecreated);
            
            //inFile.seek(5275648);
            
            while (inFile.getFilePointer() < inFile.length()) {
                /*  _Site Header_
                 *  32 bits; 'napk'
                 *  -comprssed TIM file
                 */

                char[] napk = new char[4];
                napk[0] = (char)inFile.read();
                napk[1] = (char)inFile.read();
                napk[2] = (char)inFile.read();
                napk[3] = (char)inFile.read();

                if (new String(napk).equals("napk")) {

                    byte[] img = null;
                    img = Lain_Pk.decompress(inFile);
                    //outFile.write(img);
                    Tim tim = Tim.read(new ByteArrayInputStream(img));
                    ByteArrayOutputStream obuf = new ByteArrayOutputStream();
                    ImageIO.write(tim.toBufferedImage(0), "png", obuf);
                    BufferedImage bi = ImageIO.read(new ByteArrayInputStream(obuf.toByteArray()));
                    tim = Tim.create(bi, tim.getBitsPerPixel());
                    obuf = new ByteArrayOutputStream();
                    tim.write(obuf);

                    outFile.write(new byte[] {'n', 'a', 'p', 'k'});
                    Lain_Pk.compress(obuf.toByteArray(), outFile);
                    
                    long outPos = inFile.getFilePointer();
                    int iPadToNext2048in = (int)((outPos + 2047) / 2048 * 2048 - outPos);
                    IO.skip(inFile, iPadToNext2048in);
                    
                    System.out.println(outPos);
                    
                    long inPos = outFile.getChannel().position();
                    int iPadToNext2048out = (int)((inPos + 2047) / 2048 * 2048 - inPos);
                    outFile.write(new byte[iPadToNext2048out]);
                    
                    if (iPadToNext2048in != iPadToNext2048out || outPos != inPos)
                        throw new IOException("Decompresser and compresser ended up with different lengths.");
                        
                } else {
                    outFile.write(napk[0]);
                    outFile.write(napk[1]);
                    outFile.write(napk[2]);
                    outFile.write(napk[3]);
                    byte[] cpy = new byte[2048-4];
                    if (inFile.read(cpy) != cpy.length) throw new IOException();
                    outFile.write(cpy);
                }
            }
            
        } catch (NotThisTypeException ex) {
            ex.printStackTrace();
            return -1;
        } catch (IOException ex) {
            ex.printStackTrace();
            return -1;
        }
        
        return 0;
    }    
    
    
    public static void main(String[] args) throws IOException {
        final String ORIGINAL_SITEA = "SITEA.BIN";
        final String NEW_SITEA = "#SITEA.BIN-test";
        
        //testZipTim("SITEA.BIN", "..\\..\\sitea.bin1");
        labelSITE(ORIGINAL_SITEA, NEW_SITEA);
        //labelSITE("..\\..\\BIN.BIN", "BIN.BIN.dup");
        
        args = new String[] {NEW_SITEA, "..\\..\\LainImg\\disc1hack.bin", "7471"};
        //SectorCopy.main(args);
    }
    
    public static int labelSITE(String sSiteFile, String sRecreated) {
        
        try {
            final RandomAccessFile inFile = new RandomAccessFile(sSiteFile, "r");
            FileOutputStream outFile = new FileOutputStream(sRecreated);
            
            int iImageIndex = 0; // offsets start at ?
            
            while (inFile.getFilePointer() < inFile.length()) {
                
                char[] napk = new char[4];
                napk[0] = (char)inFile.read();
                napk[1] = (char)inFile.read();
                napk[2] = (char)inFile.read();
                napk[3] = (char)inFile.read();

                // save the start position of this TIM file
                long napkStart = inFile.getFilePointer();
                
                if (new String(napk).equals("napk")) {

                    
                    // decompress the TIM file
                    Tim originalTim = readUncompressConvertTim(inFile);

                    // calculate how big the TIM was in bytes
                    long napkEnd = inFile.getFilePointer();
                    long lngSizeOfTim = napkEnd - napkStart;
                    
                    System.out.println(String.format(
                            "Uncompressing image %d, offset %d (sector %d), size %d", 
                            iImageIndex, (napkStart-4), (napkStart-4)/ 2048, (lngSizeOfTim+4)));
                    
                    // pad to the next napk sector
                    long inPos = inFile.getFilePointer();
                    int iPadToNext2048in = (int)((inPos + 2047) / 2048 * 2048 - inPos);
                    IO.skip(inFile, iPadToNext2048in);
                    
                    /*
                    // add the number to the image
                    byte[] abBytesToWrite = writeNumberAndFillIfNeeded(originalTim, lngSizeOfTim, iImageIndex);
                    */
                    // replace with huge image
                    byte[] abBytesToWrite = makeHuge(originalTim, lngSizeOfTim, iImageIndex);
                    
                    iImageIndex++;
                    // and write it
                    outFile.write(new byte[] {'n', 'a', 'p', 'k'});
                    outFile.write(abBytesToWrite);

                    // pad to the current inFile position
                    long outPos = outFile.getChannel().position();
                    long iPadToNext2048out = inFile.getFilePointer() - outPos;
                    outFile.write(new byte[(int)iPadToNext2048out]);
                    
                        
                } else if (napk[0] == 0x10 && napk[1] == 0x00 && napk[2] == 0x00 && napk[3] == 0x00) {
                    
                    // backup to the start of the TIM file
                    napkStart -= 4;
                    inFile.seek(napkStart);
                    
                    Tim oTim = Tim.read(new InputStream() {
                        @Override
                        public int read() throws IOException {
                            return inFile.read();
                        }
                    });
                    
                    long napkEnd = inFile.getFilePointer();
                    long lngSizeOfTim = napkEnd - napkStart;
                    
                    System.out.println(String.format(
                            "Reading image %d, offset %d (sector %d), size %d", 
                            iImageIndex, (napkStart-4), (napkStart-4)/ 2048, lngSizeOfTim));
                    
                    // pad to the next napk sector
                    long inPos = inFile.getFilePointer();
                    int iPadToNext2048in = (int)((inPos + 2047) / 2048 * 2048 - inPos);
                    IO.skip(inFile, iPadToNext2048in);
                    
                    oTim.write(outFile);
                    iImageIndex++;
                    
                    // pad to the current inFile position
                    long outPos = outFile.getChannel().position();
                    long iPadToNext2048out = inFile.getFilePointer() - outPos;
                    outFile.write(new byte[(int)iPadToNext2048out]);
                    
                    
                } else {
                    throw new IOException("unknown sector type, offset " + napkStart);
                }
            }
            
        } catch (NotThisTypeException ex) {
            ex.printStackTrace();
            return -1;
        } catch (IOException ex) {
            ex.printStackTrace();
            return -1;
        }
        
        return 0;
    }    
    
    private static Tim readUncompressConvertTim(RandomAccessFile inFile) throws IOException, NotThisTypeException {
        byte[] img = null;
        img = Lain_Pk.decompress(inFile);
        return Tim.read(new ByteArrayInputStream(img));
    }

    private static byte[] makeHuge(
            Tim originalTim, long lngSizeOfTim, int iIndex) throws IOException 
    {
        BufferedImage bi;
        ByteArrayOutputStream newTim;
        
        if (iIndex == 31) {
            // 120x160 and 160x120 are the norm, with some 200x150
            // make a large binary image
            bi = new BufferedImage(32, 32,
                    BufferedImage.TYPE_3BYTE_BGR);

        } else if (iIndex == 584) {
            // 120x160 and 160x120 are the norm, with some 200x150
            // make a large binary image
            bi = new BufferedImage(64, 33,
                    BufferedImage.TYPE_3BYTE_BGR);

        } else if (iIndex == 607) {
            // 120x160 and 160x120 are the norm, with some 200x150
            // make a large binary image
            bi = new BufferedImage(64+32, 35,
                    BufferedImage.TYPE_3BYTE_BGR);

        } else {
            // 120x160 and 160x120 are the norm, with some 200x150
            // make a large binary image
            bi = new BufferedImage(250, 200,
                    BufferedImage.TYPE_3BYTE_BGR);

        }
        
        fillWhite(bi);
        putDots(bi);
        ImageIO.write(bi, "png", new File("sitereplace\\test" + iIndex + ".png")); // debug
        
        // again convert back to tim and compress
        newTim = makeCompressedTim(bi, originalTim.getBitsPerPixel());

        // if it fits the original size, then we're good
        if (newTim.size() <= lngSizeOfTim) {
            return newTim.toByteArray();
        }

        // guess we got carried away
        // just use the original tim
        newTim = makeCompressedTim(originalTim);
        
        // if it fits the original size, then we're good
        if (newTim.size() <= lngSizeOfTim) {
            return newTim.toByteArray();
        }
        
        throw new IOException("TIM will never fit!!");
    }

    
    private static void putDots(BufferedImage bi) {
        int w = bi.getWidth() / 2;
        int h = bi.getHeight() / 2;
        for (int i = 0; i < bi.getWidth(); i+=2) {
            bi.setRGB(i, h, 0x00000000);
        }
        for (int j = 0; j < bi.getHeight(); j+=2) {
            bi.setRGB(w, j, 0x00000000);
            bi.setRGB(w, j, 0x00000000);
            bi.setRGB(w, j, 0x00000000);
        }

    }
    
    private static byte[] writeNumberAndFillIfNeeded(
            Tim originalTim, long lngSizeOfTim, int iIndex) throws IOException 
    {
        BufferedImage bi;
        ByteArrayOutputStream newTim;
        
        bi = originalTim.toBufferedImage(0);
        ImageIO.write(bi, "png", new File("test-orig.png")); // debug
        
        // write the image number on it
        drawCentered(bi, iIndex+"", Color.red, 72);

        // convert back to tim and compress
        newTim = makeCompressedTim(bi, originalTim.getBitsPerPixel());
        
        // if it fits the original size, then we're good
        ImageIO.write(bi, "png", new File("test.png")); // debug
        if (newTim.size() <= lngSizeOfTim) {
            return newTim.toByteArray();
        }
        
        // new TIM is too big!!
        // make a binary image then
        bi = new BufferedImage(originalTim.getWidth(), originalTim.getHeight(), 
                BufferedImage.TYPE_BYTE_BINARY);
        
        // fill white, and put big black text on it
        fillWhite(bi);
        drawCentered(bi, iIndex+"", Color.black, 72);
        
        // again convert back to tim and compress
        newTim = makeCompressedTim(bi, originalTim.getBitsPerPixel());
        
        ImageIO.write(bi, "png", new File("test.png")); // debug
        // if it fits the original size, then we're good
        if (newTim.size() <= lngSizeOfTim) {
            return newTim.toByteArray();
        }
        
        // ack! ok, make the text smaller now :P
        fillWhite(bi);
        drawCentered(bi, iIndex+"", Color.black, 16);
        
        // again convert back to tim and compress
        newTim = makeCompressedTim(bi, originalTim.getBitsPerPixel());
        
        ImageIO.write(bi, "png", new File("test.png")); // debug
        // if it fits the original size, then we're good
        if (newTim.size() <= lngSizeOfTim) {
            return newTim.toByteArray();
        }
        
        throw new IOException("TIM will never fit!!");
    }
    
    
    private static ByteArrayOutputStream makeCompressedTim(Tim newTim) throws IOException {
        // write it out
        ByteArrayOutputStream buf1 = new ByteArrayOutputStream();
        newTim.write(buf1);
        // and compress it
        ByteArrayOutputStream buf2 = new ByteArrayOutputStream();
        Lain_Pk.compress(buf1.toByteArray(), buf2);
        return buf2;
    }
    
    private static ByteArrayOutputStream makeCompressedTim(BufferedImage bi, int iBitsPerPix) throws IOException {
        // convert to tim
        Tim newTim = Tim.create(bi, iBitsPerPix);
        return makeCompressedTim(newTim);
    }
    
    // copy an image
    // http://forum.java.sun.com/thread.jspa?threadID=711821&messageID=4118293
    
    private static void fillWhite(BufferedImage src) {
        Graphics2D g2d = src.createGraphics();
        g2d.setColor(Color.white);
        g2d.fill(new Rectangle2D.Float(0, 0, src.getWidth(), src.getHeight()));
        g2d.dispose();
    }
    
	// http://forum.java.sun.com/thread.jspa?threadID=5177110&messageID=9686241
    private static void drawCentered(BufferedImage src, String text, Paint color, int size) {
        int w = src.getWidth();
        int h = src.getHeight();
        Graphics2D g2 = src.createGraphics();
        // Draw text on top.
        Font font = g2.getFont().deriveFont((float)size);
        g2.setFont(font);
        FontRenderContext frc = g2.getFontRenderContext();

        float width = (float)font.getStringBounds(text, frc).getWidth();
        LineMetrics lm = font.getLineMetrics(text, frc);
        float height = lm.getAscent() + lm.getDescent();
        // Locate text, this will draw it centered
        float x = (w - width)/2;
        float y = (h + height)/2 - lm.getDescent();
        g2.setPaint(color);
        g2.drawString(text, x, y);
        g2.dispose();
    }

    
    
}
