/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins.psx.lain;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import javax.imageio.ImageIO;
import jpsxdec.plugins.psx.tim.Tim;
import jpsxdec.util.NotThisTypeException;

/** Class to decode the background audio images from the SITEA.BIN 
 *  and SITEB.BIN files. */
public class Lain_SITE {
    
    public static int DebugVerbose = 2;
    
    /** Decodes all the audio images from the SITEA.BIN and SITEB.BIN files.
     *  This function needs a standard 2048-per-sector (i.e. ISO) copy of the 
     *  files. This can easily be accomplished by simply copying the file off 
     *  the disc using normal operating system commands, or even providing the 
     *  path directly to the file on the disc.     
     *
     *  Output file names will look like this:
     *  <sOutBaseName>###.png
     * @param sSiteFile - the path to the SITEA.BIN or SITEB.BIN file
     * @param sOutBaseName - output base name of the files
     * @return 0 on ok, -1 on error
     */
    public static int DecodeSITE(String sSiteFile, String sOutBaseName) {
        //String sSiteFile = "SITEB.BIN";
        //String sOutBaseName = "sites\\SITEB";
        
        try {
            final RandomAccessFile raf = new RandomAccessFile(sSiteFile, "r");
            
            long lngFileOffset = 0;
            int iIndex = 0;
            
            while (lngFileOffset < raf.length()) {
                /*  _Site Header_
                 *  32 bits; 'napk'
                 *  -comprssed TIM file
                 */

                char[] napk = new char[4];
                raf.seek(lngFileOffset);
                napk[0] = (char)raf.read();
                napk[1] = (char)raf.read();
                napk[2] = (char)raf.read();
                napk[3] = (char)raf.read();

                if (new String(napk).equals("napk")) {

                    // compressed TIM file

                    byte[] abImg = null;
                    try {
                        abImg = Lain_Pk.decompress(raf);

                        ByteArrayInputStream oByteStream = new ByteArrayInputStream(abImg);
                        
                        Tim tim = Tim.read(oByteStream);
                        for (int i = 0; i < tim.getPaletteCount(); i++) {
                            ImageIO.write(tim.toBufferedImage(i), "png",
                                    new File(String.format(
                                    "%s%03d-%d.png",
                                    sOutBaseName,
                                    iIndex, i
                                    )));
                        }
                        
                    } catch (NotThisTypeException ex) {
                        //ex.printStackTrace();
                        // if not a compressed TIM, write the uncompressed data
                        FileOutputStream fos = new FileOutputStream(String.format(
                                "%s%03d.dat",
                                sOutBaseName,
                                iIndex
                                ));
                        fos.write(abImg);
                        fos.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    iIndex++;
                } else if (napk[0] == 0x10 && napk[1] == 0x00 && napk[2] == 0x00 && napk[3] == 0x00) {
                    // uncompressed TIM file
                    raf.seek(lngFileOffset); // back up to the start of it
                    try {

                        Tim tim = Tim.read(new InputStream() {
                            @Override
                            public int read() throws IOException {
                                return raf.read();
                            }
                        });
                        for (int i = 0; i < tim.getPaletteCount(); i++) {
                            ImageIO.write(tim.toBufferedImage(i), "png",
                                    new File(String.format(
                                    "%s%03d-%d.png",
                                    sOutBaseName,
                                    iIndex, i
                                    )));
                        }
                    } catch (NotThisTypeException ex) {
                        ex.printStackTrace();
                    }
                    
                    iIndex++;
                }
                lngFileOffset += 2048;
            }
            
        } catch (IOException ex) {
            ex.printStackTrace();
            return -1;
        }
        
        return 0;
    }
    
}
