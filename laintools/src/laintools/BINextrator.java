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

import java.io.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import jpsxdec.tim.Tim;
import jpsxdec.util.IO;
import jpsxdec.util.BinaryDataNotRecognized;

/** Class to extract the images/files from SITEA.BIN, SITEB.BIN, and BIN.BIN files. */
public class BINextrator {

    public static int DebugVerbose = 2;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Expecting 2 parameters: <bin-file> <out-base-name>");
            return;
        }
        extract(args[0], args[1]);
    }

    static final String NAPK = "napk";
    static final byte[] TIM_MAGIC = {0x10, 0x00, 0x00, 0x00};

    /** Decodes all the images from the SITEA.BIN, SITEB.BIN, and BIN.BIN files.
     *  This function needs a standard 2048-per-sector (i.e. ISO) copy of the
     *  files. This can easily be accomplished by simply copying the file off
     *  the disc using normal operating system commands, or even providing the
     *  path directly to the file on the disc.
     *<p>
     *  Output file names will look like this:
     *  <sOutBaseName>###.png
     *<p>
     *  Note that the SLPS_016.03/SLPS_016.04 file has tables that index
     *  these files with starting sector and data size.
     *  These tables are not needed to simply find and extract images.
     *
     * @param sSiteFile    the path to the SITEA.BIN, SITEB.BIN, or BIN.BIN file
     * @param sOutBaseName output base name of the files
     * @return 0 on ok, -1 on error
     */
    public static int extract(String sSiteFile, String sOutBaseName) {

        try {
            final RandomAccessFile raf = new RandomAccessFile(sSiteFile, "r");

            long lngFileOffset = 0;
            int iIndex = 0;

            while (lngFileOffset < raf.length()) {
                /*  _Site Header_
                 *  32 bits; 'napk'
                 *  -comprssed TIM file
                 */

                raf.seek(lngFileOffset);
                byte[] napk = IO.readByteArray(raf, 4);

                if (NAPK.equals(new String(napk))) {
                    System.out.println("Found compressed data at sector " + (lngFileOffset / 2048) + " ("+ lngFileOffset + ")");

                    // compressed TIM file

                    byte[] abImg = null;
                    try {
                        abImg = Lain_Pk.decompress(raf);

                        ByteArrayInputStream bais = new ByteArrayInputStream(abImg);

                        Tim tim = Tim.read(bais);
                        System.out.println("It is a TIM image: " + tim);
                        for (int i = 0; i < tim.getPaletteCount(); i++) {
                            String sFile = String.format(
                                        "%s%03d-%d.png",
                                        sOutBaseName,
                                        iIndex, i
                                        );
                            System.out.println("Saving " + sFile);
                            ImageIO.write(tim.toBufferedImage(i), "png",
                                    new File(String.format(sFile)));
                        }

                    } catch (BinaryDataNotRecognized ex) {
                        String sFile = String.format(
                                "%s%03d.dat",
                                sOutBaseName,
                                iIndex
                                );
                        System.out.println("It is unknown data, saving " + sFile);
                        // if not a compressed TIM, write the uncompressed data
                        FileOutputStream fos = new FileOutputStream(sFile);
                        fos.write(abImg);
                        fos.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    long lngSize = raf.getFilePointer() - lngFileOffset;
                    System.out.println("Size is " + lngSize + " (" + ((lngSize + 2047) / 2048) + " sectors)");
                    iIndex++;
                } else if (Arrays.equals(napk, TIM_MAGIC)) {
                    System.out.println("Found uncompressed TIM image at sector " + (lngFileOffset / 2048) + " ("+ lngFileOffset + ")");
                    // uncompressed TIM file
                    raf.seek(lngFileOffset); // back up to the start of it
                    try {

                        Tim tim = Tim.read(new InputStream() {
                            @Override
                            public int read() throws IOException {
                                return raf.read();
                            }
                        });
                        System.out.println(tim);
                        for (int i = 0; i < tim.getPaletteCount(); i++) {
                            String sFile = String.format(
                                    "%s%03d-%d.png",
                                    sOutBaseName,
                                    iIndex, i
                                    );
                            System.out.println("Saving " + sFile);

                            ImageIO.write(tim.toBufferedImage(i), "png",
                                    new File(sFile));
                        }
                    } catch (BinaryDataNotRecognized ex) {
                        ex.printStackTrace();
                    }

                    long lngSize = raf.getFilePointer() - lngFileOffset;
                    System.out.println("Size is " + lngSize + " (" + ((lngSize + 2047) / 2048) + " sectors)");
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
