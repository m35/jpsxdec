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

package jpsxdec.plugins.psx.video.encode;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import jpsxdec.plugins.psx.video.PsxYuvImage;
import jpsxdec.plugins.psx.video.DemuxImage;
import jpsxdec.plugins.psx.video.encode.ParsedMdecImage.Block;
import jpsxdec.plugins.psx.video.encode.ParsedMdecImage.MacroBlock;
import jpsxdec.util.IO;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor_FF7;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor_STRv2;


public class STRRecompressor {

    private static final int JENOVA_E = 1;
    private static final int ENDING2E = 2;
    private static final int PROCESS_FILE = ENDING2E;

    final int WIDTH;
    final int HEIGHT;
    final String SOURCE_PICS_DIR;
    final String SOURCE_DEMUX_DIR;
    final String MODDED_PICS_DIR;
    final String OUTPUT_DEMUX_DIR;

    final DemuxFrameUncompressor_FF7 UNCOMPRESSOR_FF7 = new DemuxFrameUncompressor_FF7();
    final DemuxFrameUncompressor_STRv2 UNCOMPRESSOR_V2 = new DemuxFrameUncompressor_STRv2();

    public STRRecompressor(String sSrcPicsDir, String sModPicsDir, 
                           String sSrcDemuxDir, String sOutDemuxDir) throws Throwable
    {
        SOURCE_PICS_DIR = sSrcPicsDir;
        MODDED_PICS_DIR = sModPicsDir;
        SOURCE_DEMUX_DIR = sSrcDemuxDir;
        OUTPUT_DEMUX_DIR = sOutDemuxDir;

        File[] aoSourcePics = IO.getSortedFileList(SOURCE_PICS_DIR, ".png", ".bmp");
        File[] aoModdedPics = IO.getSortedFileList(MODDED_PICS_DIR, ".png", ".bmp");
        File[] aoSourceDemux = IO.getSortedFileList(SOURCE_DEMUX_DIR, ".demux");
        final int FRAME_COUNT = aoSourcePics.length;
        if (FRAME_COUNT != aoSourceDemux.length ||
            FRAME_COUNT != aoModdedPics.length)
            throw new IllegalArgumentException("Number of files aren't the same in the directories");

        // read the first image just to get the movie dimensions
        BufferedImage bi = ImageIO.read(aoSourcePics[0]);
        WIDTH = bi.getWidth();
        HEIGHT = bi.getHeight();

        for (int iFrame = 0; iFrame < FRAME_COUNT; iFrame++) {
            doFrame(ImageIO.read(aoSourcePics[iFrame]),
                    ImageIO.read(aoModdedPics[iFrame]),
                    new DemuxImage(WIDTH, HEIGHT, aoSourceDemux[iFrame]),
                    iFrame, aoSourcePics[iFrame].getName(), aoModdedPics[iFrame].getName());
        }

    }



    private void doFrame(
            BufferedImage oSrcImg,
            BufferedImage oModImg,
            DemuxImage oSrcDemux,
            int iFile, String sOrigFile, String sModFile) throws Throwable
    {
        ArrayList<Point> oDiffPoints = compare16x16(oSrcImg, oModImg);
        if (oDiffPoints == null) {
            System.out.println("Files " + sOrigFile + " & " + sModFile + " are identical.");
            return;
        }

        System.out.println("Files " + sOrigFile + " & " + sModFile + " have " + oDiffPoints.size() + " different Macblks.");

        ParsedMdecImage oSrcUncompress = new ParsedMdecImage(WIDTH, HEIGHT);
        ParsedMdecImage oModUncompress = new ParsedMdecImage(WIDTH, HEIGHT);

        UNCOMPRESSOR_FF7.reset(oSrcDemux.getData(), 0);
        oSrcUncompress.readFrom(UNCOMPRESSOR_FF7);
        int iSrcDemuxSize = UNCOMPRESSOR_FF7.getPosition();

        System.out.println("  Original file MAX demux size: " + oSrcDemux.getBufferSize());
        System.out.println("  Original file demux size: " + iSrcDemuxSize);
        System.out.println("  Original file MDEC count: " + oSrcUncompress.getRunLengthCodeCount());

        int iQscale = oSrcUncompress.getFirstChromQscale();
        while (true) {
            System.out.println("  Trying qscale " + iQscale);
            MdecEncoder oEncoder = new MdecEncoder(new PsxYuvImage(oModImg), iQscale);
            //UNCOMPRESSOR_V2.reset(oModDemux.getData());
            //int iModDemuxSize = UNCOMPRESSOR_V2.getPosition();
            oModUncompress.readFrom(oEncoder.getStream());

            for (Point oDiffPt : oDiffPoints) {
                System.out.format("    Copying macroblock (%d, %d)", oDiffPt.x, oDiffPt.y);
                System.out.println();
                MacroBlock oSrcMacBlk = oSrcUncompress.getMacroBlock(oDiffPt.x, oDiffPt.y);
                MacroBlock oModMacBlk = oModUncompress.getMacroBlock(oDiffPt.x, oDiffPt.y);
                copyMacroBlock(oSrcMacBlk, oModMacBlk);
            }

            int iMergedDemuxSize = writeMerged(oSrcUncompress, iFile);
            if (iMergedDemuxSize <= oSrcDemux.getBufferSize()) {
                System.out.format("    Merged file %d demux size %d <= max source %d ", iFile, iMergedDemuxSize, oSrcDemux.getBufferSize());
                System.out.println();
                break;
            } else {
                System.out.format("    >>> Merged file %d demux size %d > max source %d <<<", iFile, iMergedDemuxSize, oSrcDemux.getBufferSize());
                System.out.println();
            }
            iQscale++;
        }
    }

    private void copyMacroBlock(MacroBlock oSrcMacBlk, MacroBlock oModMacBlk) {
        for (int iBlock = 0; iBlock < 6; iBlock++) {
            Block oSrcBlk = oSrcMacBlk.getBlock(iBlock);
            Block oModBlk = oModMacBlk.getBlock(iBlock);
            if (oSrcBlk.getQscale() != oModBlk.getQscale()) {
                System.out.format("      %s qscale is different (%d -> %d) ",
                                  oSrcBlk.getName(), oModBlk.getQscale(), oSrcBlk.getQscale());
                System.out.println();

                oModBlk.changeQuantizationScale(oSrcBlk.getQscale());
            }
            oSrcMacBlk.setBlock(oModBlk);
        }
    }

    private int writeMerged(ParsedMdecImage oMerged, int iFile) throws Throwable {
        System.out.println("  Merged frame MDEC count: " + oMerged.getRunLengthCodeCount());
        String sFile = String.format("merged%04d.demux", iFile);
        System.out.println("  Writing merged file " + sFile);
        File oOutputFile = new File(OUTPUT_DEMUX_DIR, sFile);
        FileOutputStream fso = new FileOutputStream(oOutputFile);

        BitStreamWriter bsw = new BitStreamWriter(fso);

        int iQscale = oMerged.getMacroBlock(0, 0).getBlock(0).getQscale();

        DemuxFrameUncompressor_FF7.Recompressor_FF7  oRecompressor = new DemuxFrameUncompressor_FF7.Recompressor_FF7();
        oRecompressor.compressToDemuxFF7(bsw, iQscale, oMerged.getRunLengthCodeCount());
        oRecompressor.write(oMerged.getStream());
        bsw.close();
        return (int)oOutputFile.length();
    }

    private static ArrayList<Point> compare16x16(BufferedImage bi1, BufferedImage bi2) {

        ArrayList<Point> oDiffPoints = new ArrayList<Point>();

        int[] ai1 = new int[16 * 16];
        int[] ai2 = new int[16 * 16];

        for (int x=0; x < bi1.getWidth(); x+=16) {
            int y=0;
            if (PROCESS_FILE == ENDING2E) { // if ENDING2_E
                y = bi1.getHeight() - 16*2;
            }
            for (; y < bi1.getHeight(); y+=16) {
                ai1 = bi1.getRGB(x, y, 16, 16, ai1, 0, 16);
                ai2 = bi2.getRGB(x, y, 16, 16, ai2, 0, 16);
                for (int i=0; i < 16*16; i++) {
                    if (ai1[i] != ai2[i]) {
                        oDiffPoints.add(new Point(x / 16, y / 16));
                        break;
                    }
                }

            }
        }

        if (oDiffPoints.size() > 0)
            return oDiffPoints;
        else
            return null;
    }

}
