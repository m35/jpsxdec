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
 * Lain_SITE.java
 */

package jpsxdec.plugins;

import java.io.*;
import javax.imageio.ImageIO;
import jpsxdec.*;
import jpsxdec.media.Tim;
import jpsxdec.util.*;

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
            final RandomAccessFile oRAF = new RandomAccessFile(sSiteFile, "r");
            
            long lngFileOffset = 0;
            int iIndex = 0;
            
            while (lngFileOffset < oRAF.length()) {
                /*  _Site Header_
                 *  32 bits; 'napk'
                 *  -comprssed TIM file
                 */

                char[] napk = new char[4];
                oRAF.seek(lngFileOffset);
                napk[0] = (char)oRAF.read();
                napk[1] = (char)oRAF.read();
                napk[2] = (char)oRAF.read();
                napk[3] = (char)oRAF.read();

                if (new String(napk).equals("napk")) {

                    // compressed TIM file

                    byte[] img = null;
                    try {
                        img = Lain_Pk.Decompress(oRAF);

                        ByteArrayInputStream oByteStream = new ByteArrayInputStream(img);
                        
                        Tim oTim = new Tim(oByteStream);
                        for (int i = 0; i < oTim.getPaletteCount(); i++) {
                            ImageIO.write(oTim.toBufferedImage(i), "png", 
                                    new File(String.format(
                                    "%s%03d-%d.png",
                                    sOutBaseName,
                                    iIndex, i
                                    )));
                        }
                        
                    } catch (NotThisTypeException ex) {
                        //ex.printStackTrace();
                        // if not a compressed TIM, write the uncompressed data
                        FileOutputStream oFOS = new FileOutputStream(String.format(
                                "%s%03d.dat",
                                sOutBaseName,
                                iIndex
                                ));
                        oFOS.write(img);
                        oFOS.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    iIndex++;
                } else if (napk[0] == 0x10 && napk[1] == 0x00 && napk[2] == 0x00 && napk[3] == 0x00) {
                    // uncompressed TIM file
                    oRAF.seek(lngFileOffset); // back up to the start of it
                    try {

                        Tim oTim = new Tim(new InputStream() {
                            @Override
                            public int read() throws IOException {
                                return oRAF.read();
                            }
                        });
                        for (int i = 0; i < oTim.getPaletteCount(); i++) {
                            ImageIO.write(oTim.toBufferedImage(i), "png", 
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
