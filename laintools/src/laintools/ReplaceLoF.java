/*
 * LainTools: PSX Serial Experiments Lain Hacking and Translation Tools
 * Copyright (C) 2011  Michael Sabin
 *
 * Redistribution and use of the LainTools code or any derivative works are
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

package laintools;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import jpsxdec.tim.Tim;
import jpsxdec.util.IO;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IncompatibleException;

public class ReplaceLoF {

    private static final int LOF_OFFSET = 65/*2048*/;
    private static final int LOF_MAX_SIZE = 2*2048;
    
    private static final int VER_OFFSET = 13/*2048*/;
    private static final int VER_MAX_SIZE = 2*2048;
    
    private static final int CREDIT1_OFFSET = 46/*2048*/;
    private static final int CREDIT1_MAX_SIZE = 4*2048;
    
    private static final int CREDIT2_OFFSET = 50/*2048*/;
    private static final int CREDIT2_MAX_SIZE = 5*2048;
    
    private static final int CREDIT3_OFFSET = 55/*2048*/;
    private static final int CREDIT3_MAX_SIZE = 3*2048;
    
    private static boolean SAVE_DEBUG_FILES = true;

    private static Tim extractTim(RandomAccessFile bin, int iSectorOffset, String sBaseName) throws IOException, BinaryDataNotRecognized {
        
        bin.seek(iSectorOffset*2048);
        
        byte[] napk = IO.readByteArray(bin, 4);
        
        if (!BINextrator.NAPK.equals(new String(napk)))
            throw new RuntimeException("Did not find compressed data");

        byte[] abOriginalTimData = Lain_Pk.decompress(bin);

        if (SAVE_DEBUG_FILES) {
            IO.writeFile(sBaseName + ".tim", abOriginalTimData);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(abOriginalTimData);

        Tim originalTim = Tim.read(bais);

        // make sure we can recreate the Tim exactly if we wanted
        ByteArrayOutputStream outTest = new ByteArrayOutputStream();
        originalTim.write(outTest);
        
        if (SAVE_DEBUG_FILES) {
            IO.writeFile(sBaseName+"-recreated.tim", outTest.toByteArray());
        }
        
        if (!Arrays.equals(abOriginalTimData, outTest.toByteArray())) {
            throw new RuntimeException("Failed trying to recreate the Tim exactly");
        }
        
        return originalTim;
    }

    
    private static void patchLoFTim(Tim lofTim) throws IOException, BinaryDataNotRecognized, IncompatibleException {
        // palette 3 is the one that looks best for LoF
        // but any palette is fine
        BufferedImage image = lofTim.toBufferedImage(3);
        BufferedImage clut = lofTim.getClutImage();
        
        if (SAVE_DEBUG_FILES) {
            ImageIO.write(image, "png", new File("LoF.png"));
            ImageIO.write(clut, "png", new File("LoF-clut.png"));
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        lofTim.write(baos);
        byte[] abOriginal = baos.toByteArray();
        Tim dupTest = Tim.read(new ByteArrayInputStream(abOriginal));
        dupTest.replaceImageData(image, clut);
        baos.reset();
        dupTest.write(baos);

        // Sanity check that Tim logic is working
        if (!Arrays.equals(abOriginal, baos.toByteArray())) {
            throw new RuntimeException("Something went wrong trying to recreate the Tim exactly");
        }

        blankRectangle(image, 0, 48, 192, 24);
        blankRectangle(clut, 2, 1, 3, 1);
        
        if (SAVE_DEBUG_FILES) {
            ImageIO.write(image, "png", new File("LoF-patched.png"));
            ImageIO.write(clut, "png", new File("LoF-clut-patched.png"));
        }
        
        lofTim.replaceImageData(image, clut);
        
        if (SAVE_DEBUG_FILES) {
            FileOutputStream fos = new FileOutputStream("LoF-patched.tim");
            lofTim.write(fos);
            fos.close();
        }
    }
    
    private static void blankRectangle(BufferedImage bi, int iX, int iY, int iW, int iH) {
        for (int y = 0; y < iH; y++) {
            for (int x = 0; x < iW; x++) {
                bi.setRGB(iX + x, iY + y, 0);
            }
        }
    }
    
    private static void patchVersionTim(Tim version, String sVersion) throws IOException, IncompatibleException {
        final Color TEXT_COLOR = new Color(0xff, 0xff, 0xff);
        
        BufferedImage img = version.toBufferedImage(1);
        Graphics g = img.getGraphics();
        g.setFont(new Font("Arial Narrow", 0, 9));
        g.setColor(TEXT_COLOR);
        g.drawString("ENGLISH", 114, 10);
        g.drawString("PATCH", 114, 17);
        g.drawString(sVersion, 114, 24);
        g.dispose();
        
        if (SAVE_DEBUG_FILES) {
            ImageIO.write(img, "png", new File("EnGVer-patched.png"));
        }
        
        version.replaceImageData(img, version.getClutImage());
    }

    private static void patchBinBin(String sNewBinFile, String sSlps, 
            Tim lof, Tim version, 
            Tim credits1, Tim credits2, Tim credits3) 
            throws IOException 
    {
        RandomAccessFile slps = new RandomAccessFile(sSlps, "rw");
        
        RandomAccessFile bin = new RandomAccessFile(sNewBinFile, "rw");
        
        write(bin, slps, compressTim(lof, LOF_MAX_SIZE), LOF_OFFSET, 562920+21*8);
        write(bin, slps, compressTim(version, VER_MAX_SIZE), VER_OFFSET, 562920+3*8);
        
/*        
        bin.seek(CREDIT1_OFFSET);
        bin.write(compressTim(credits1, CREDIT1_MAX_SIZE));
        
        bin.seek(CREDIT2_OFFSET);
        bin.write(compressTim(credits2, CREDIT2_MAX_SIZE));
        
        bin.seek(CREDIT3_OFFSET);
        bin.write(compressTim(credits3, CREDIT3_MAX_SIZE));
  
  * 
  */
        bin.close();
    }
    
    private static void write(RandomAccessFile bin, RandomAccessFile slps, byte[] ab, int iSectorOffset, int iTableOffset) throws IOException {
        System.out.println("Writing " + ab.length + " bytes to @" + iSectorOffset + " (" + (iSectorOffset*2048) + ")");
        bin.seek(iSectorOffset*2048);
        bin.write(ab);
        
        System.out.println("Updating table at " + iTableOffset);
        slps.seek(iTableOffset);
        IO.writeInt32LE(slps, iSectorOffset);
        IO.writeInt32LE(slps, ab.length);
    }
    
    private static byte[] compressTim(Tim tim, int iMaxSize) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tim.write(baos);
        byte[] ab = baos.toByteArray();
        baos.reset();
        baos.write(BINextrator.NAPK.getBytes());
        Lain_Pk.compress(ab, baos);
        if (baos.size() > iMaxSize)
            throw new RuntimeException("New compressed image is too big");
        return baos.toByteArray();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        if (args.length >= 4) {
            String sSlps = args[0],
                   sOriginalBin = args[1],
                   sNewBin = args[2],
                   sVersion = args[3];
            
            RandomAccessFile bin = new RandomAccessFile(sOriginalBin, "r");

            System.out.println("Extracting LoF image from " + sOriginalBin);
            Tim lof = extractTim(bin, LOF_OFFSET, "LoF");
            System.out.println("Extracting version image from " + sOriginalBin);
            Tim version = extractTim(bin, VER_OFFSET, "ver");
            Tim[] credits = new Tim[3];
            credits[0] = extractTim(bin, CREDIT1_OFFSET, "credit1");
            credits[1] = extractTim(bin, CREDIT2_OFFSET, "credit2");
            credits[2] = extractTim(bin, CREDIT3_OFFSET, "credit3");
            bin.close();
            System.out.println("Patching the images");
            patchLoFTim(lof);
            patchVersionTim(version, sVersion);
            //patchCredits(credits, args[3], args[4], args[5]);
            System.out.println("Creating new copy of "+sOriginalBin+" to " + sNewBin);
            copyFile(new File(sOriginalBin), new File(sNewBin));
            System.out.println("Writing patched image to " + sNewBin);
            patchBinBin(sNewBin, sSlps, lof, version, null, null, null);
        } else {
            String[] asMsg = {
                "Arguments:",
                "  <original SLPS> <original BIN.BIN> <new BIN.BIN to create> <version>"
            };
            for (String s : asMsg) {
                System.out.println(s);
            }
        }
    }

    public static void copyFile(File src, File dest) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            IO.writeIStoFile(in, dest);
        } finally {
            in.close();
        }
    }


    private static void patchCredits(Tim[] credits, String c1, String c2, String c3) {
        
    }

}
