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
 * StrDiffCombine.java
 */

package jpsxdec.plugins;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.CDXAIterator;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.demuxers.StrFramePullDemuxerIS;
import jpsxdec.mdec.MDEC.Mdec16Bits;
import jpsxdec.media.MediaHandler;
import jpsxdec.media.PSXMediaSTR;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSector.PSXSectorFrameChunk;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.uncompressors.StrFrameRecompressorIS;
import jpsxdec.uncompressors.StrFrameUncompressorIS;
import jpsxdec.uncompressors.StrFrameUncompressorIS.Block;
import jpsxdec.uncompressors.StrFrameUncompressorIS.MacroBlock;

/** Simple command-line to combine two STR files. Used for Lain sub-titling. */
public class StrDiffCombine {

    public static void main(String[] args) throws IOException {

        if (args.length != 4) {
            String[] sHelp = new String[] {
                "Playstation STR difference combine: for adding subtitles to the Lain PSX game movies",
                "Parameters:",
                "  1) folder with bitmaps of original frames",
                "  2) folder with bitmaps of sub-titled frames",
                "  3) original STR file",
                "  4) STR file with sub-titles",
            };
            for (String s : sHelp) {
                System.out.println(s);
            }

            return;
        }
        
        String[] asOriginalFiles = GetSortedImageFileList(args[0]);
        String[] asSubFiles = GetSortedImageFileList(args[1]);
        
        int iFrameCount = asOriginalFiles.length;
        if (iFrameCount != asSubFiles.length)
            throw new IllegalArgumentException("There needs to be the same number of files.");

        // Open the original movie /////////////////////////////////////////////
        CDSectorReader oCD;
        oCD = new CDSectorReader(args[2], true);
        
        MediaHandler oMedias;
        oMedias = new MediaHandler(oCD);
        
        PSXMediaSTR oOriginalMedia = (PSXMediaSTR)oMedias.getByIndex(0);
        
        // Make sure frame count matches ///////////////////////////////////////
        if (oOriginalMedia.getEndFrame() - oOriginalMedia.getStartFrame() + 1 !=
            asOriginalFiles.length)
            throw new IllegalArgumentException("The movie frame count does not match the files in the folders.");
        ////////////////////////////////////////////////////////////////////////
        
        PSXSectorRangeIterator oOriginalIter = oOriginalMedia.getSectorIterator();
        
        // TODO: Add more subtitle quality variations
        PSXSectorRangeIterator[] aoSubIters = new PSXSectorRangeIterator[1];
        aoSubIters[0] = GetMediaItem(args[3]).getSectorIterator();
        
        for (int iFrame = (int)oOriginalMedia.getStartFrame(); 
             iFrame <= (int)oOriginalMedia.getEndFrame(); 
             iFrame++) 
        {
            // decompress the frame from the original image //////////////////
            StrFramePullDemuxerIS oOriginalDemux = new StrFramePullDemuxerIS(oOriginalIter, iFrame);
            StrFrameRecompressorIS oOriginalFrame = 
                    new StrFrameRecompressorIS(oOriginalDemux, 
                    oOriginalDemux.getWidth(), oOriginalDemux.getHeight());
                    
            int iFile = iFrame - (int)oOriginalMedia.getStartFrame();
            BufferedImage oOriginalBmp = ImageIO.read(new File(args[0], asOriginalFiles[iFile]));
            BufferedImage oSubBmp = ImageIO.read(new File(args[1], asSubFiles[iFile]));

            // Find how much space the entire demuxed frame can use
            // (need to exhaust the demux to make sure we get the full data size)
            while (oOriginalDemux.read() >= 0) {}
            long lngMaximumFrameSize = oOriginalDemux.getFrameUserDataSize();
            
            // compare the frames
            ByteArrayOutputStream baos = CompareFrames(
                    oOriginalBmp, oSubBmp, 
                    oOriginalFrame, aoSubIters, 
                    lngMaximumFrameSize,
                    iFrame);
            
            if (baos == null)
                System.out.println("Frame " + iFrame + " is the same");
            else {
                System.out.println("Frame " + iFrame + " is the different");
                System.out.println("  new version uses " + baos.size() + " of " + lngMaximumFrameSize + " bytes (" + (lngMaximumFrameSize - baos.size()) + " free)");
                // write the new frame back into the original file
                System.out.println("  writing new frame back into the original file");
                RemuxFrame(oCD, oOriginalMedia, iFrame, baos.toByteArray());
                System.out.println();
            }
        }
        
    }
    
    /** Gets the first media item in a media file. */
    private static PSXMediaSTR GetMediaItem(String sFile) throws IOException {
        //open input file
        CDSectorReader oCD;
        oCD = new CDSectorReader(sFile);
        
        MediaHandler oMedias;
        oMedias = new MediaHandler(oCD);
        
        return (PSXMediaSTR) oMedias.getByIndex(0);
    }
    
    /** Returns a sorted list of image files found in the folder. */
    private static String[] GetSortedImageFileList(String sFile) 
            throws FileNotFoundException 
    {
        File oFile = new File(sFile);
        if (!oFile.exists() || !oFile.isDirectory())
            throw new FileNotFoundException("Folder " + oFile.getPath() + " does not exist.");
        FilenameFilter oFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".png") || name.endsWith(".bmp"));
            }
        };
        String[] asNames = oFile.list(oFilter);
        if (asNames == null) 
            throw new FileNotFoundException("No bmp or png images found in folder " + oFile.getPath());
        java.util.Arrays.sort(asNames);
        return asNames;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    public static ByteArrayOutputStream CompareFrames(
            BufferedImage bi1, BufferedImage bi2, 
            StrFrameRecompressorIS oOriginalFrame, 
            PSXSectorRangeIterator[] oQualityIters, 
            long lngMaximumFrameSize,
            int iFrame) 
            throws IOException 
    {
        
        int iWidth = bi1.getWidth();
        int iHeight = bi1.getHeight();

        if ( (iWidth % 16 != 0) || (iHeight % 16 != 0) )
            throw new IllegalArgumentException("Image dimensions need to be divisible by 16.");
        if ( iWidth != bi2.getWidth() || iHeight != bi2.getHeight() )
            throw new IllegalArgumentException("Image dimensions need to be the same.");
        
        // find the differences between the images using 16x16 blocks
        ArrayList<Point> oBlkDiff = Compare16x16(bi1, bi2, iWidth, iHeight);
        
        // no differences between the images
        if (oBlkDiff.size() == 0) return null;
        
        return FindBestFit(oBlkDiff, oQualityIters, oOriginalFrame, 
                lngMaximumFrameSize,
                iFrame);
    }

    /** Compres two BufferedImages for differences in 16 x16 pixel blocks.
     * @return List of block coordiantes that are different. */
    public static ArrayList<Point> Compare16x16(BufferedImage bi1, BufferedImage bi2, int iWidth, int iHeight) {
        if ( iWidth != bi1.getWidth() || iHeight != bi1.getHeight() ||
             iWidth != bi2.getWidth() || iHeight != bi2.getHeight() )
            throw new IllegalArgumentException("Image dimensions don't match given dimensions.");
        
        int[] ai1 = new int[16 * 16];
        int[] ai2 = new int[16 * 16];
        ArrayList<Point> oDiffList = new ArrayList<Point>();
        
        for (int x=0; x < iWidth; x+=16) {
            for (int y=0; y < iHeight; y+=16) {
                ai1 = bi1.getRGB(x, y, 16, 16, ai1, 0, 16);
                ai2 = bi2.getRGB(x, y, 16, 16, ai2, 0, 16);
                for (int i=0; i < 16*16; i++)
                {
                    if (ai1[i] != ai2[i]) {
                        oDiffList.add(new Point(x / 16, y / 16));
                        break;
                    }
                }
                    
            }
        }
        
        oDiffList.trimToSize();
        return oDiffList;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    private static ByteArrayOutputStream FindBestFit(
            ArrayList<Point> oDiffBlks, 
            PSXSectorRangeIterator[] oSubTitleQualityIters,
            StrFrameRecompressorIS oOriginalFrame, 
            long lngMaximumFrameSize,
            int iFrame) 
            throws IOException 
    {
        
        int iCurrentQuality = oSubTitleQualityIters.length / 2;
        
        CopyMacroBlocks(
                oDiffBlks,
                oSubTitleQualityIters[iCurrentQuality], 
                oOriginalFrame, iFrame);
        
        MacroBlock[] oUncompressedMacBlocks = oOriginalFrame.getDecodedMacroBlocks();

        // count how many macro blocks the frame has now
        long lngMacroBlocks = 0;
        for (int i=0; i < oUncompressedMacBlocks.length; i++) {
            lngMacroBlocks += oUncompressedMacBlocks[i].getMdecCodeCount();
        }   
        oOriginalFrame.setNumberOfRunLenthCodes(lngMacroBlocks);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        oOriginalFrame.write(baos);

        if (baos.size() > lngMaximumFrameSize)
            throw new RuntimeException("Poo, frame " + iFrame + " won't fit :(");
            
        return baos;
    }
    
    /** Copies the specified macro blocks from the different frame to the
     * original frame. The macro-blocks scale will be adjusted
     * if the two frames quantiation scale are different. */
    private static void CopyMacroBlocks(
            ArrayList<Point> oDiffMacBlks, 
            PSXSectorRangeIterator oSubtitleIter, 
            StrFrameRecompressorIS oOriginalFrame, int iFrame) 
            throws IOException 
    {
        // uncompress the sub-titled frame
        StrFramePullDemuxerIS oSubtitleDemux = new StrFramePullDemuxerIS(oSubtitleIter, iFrame);
        
        StrFrameUncompressorIS oSubtitledFrame = 
                new StrFrameUncompressorIS(oSubtitleDemux, 
                oSubtitleDemux.getWidth(), oSubtitleDemux.getHeight());
        
        // get the macro-blocks of both
        MacroBlock[] oSubtitleMacroBlocks = oSubtitledFrame.getDecodedMacroBlocks();
        MacroBlock[] oOriginalMacroBlocks = oOriginalFrame.getDecodedMacroBlocks();
        
        // save the height in macro-blocks for quick calc
        int iMacHeight = (int)oSubtitledFrame.getHeight() / 16;
        
        for (Point oDiffBlk : oDiffMacBlks) {

            // get the macro block to copy
            // (the calc is backwards silly!)
            int idx = oDiffBlk.y + oDiffBlk.x * iMacHeight;
            MacroBlock oSubtitleMacBlk = oSubtitleMacroBlocks[idx];
            
            // make sure the Chrominance quantization scale is the same
            long lngOriginalScale = oOriginalFrame.getQuantizationScaleChrominance();
            long lngSubScale = oSubtitledFrame.getQuantizationScaleChrominance();
            if (lngOriginalScale != lngSubScale) 
            {
                System.out.println("  frame " + iFrame + " has different chrominance");
                // need to scale blocks Cb and Cr
                ChangeMacroBlockQuantizationScale( oSubtitleMacBlk.Cb,
                        lngSubScale,
                        lngOriginalScale );
                
                ChangeMacroBlockQuantizationScale( oSubtitleMacBlk.Cr,
                        lngSubScale,
                        lngOriginalScale );
            }
            // make sure the Luminance quantization scale is the same
            lngOriginalScale = oOriginalFrame.getQuantizationScaleLuminance();
            lngSubScale = oSubtitledFrame.getQuantizationScaleLuminance();
            if (lngOriginalScale != lngSubScale) 
            {
                System.out.println("  frame " + iFrame + " has different luminance");
                
                // need to scale blocks Y1-4
                ChangeMacroBlockQuantizationScale( oSubtitleMacBlk.Y1,
                        lngSubScale,
                        lngOriginalScale );
                
                ChangeMacroBlockQuantizationScale( oSubtitleMacBlk.Y2,
                        lngSubScale,
                        lngOriginalScale );
                
                ChangeMacroBlockQuantizationScale( oSubtitleMacBlk.Y3,
                        lngSubScale,
                        lngOriginalScale );
                
                ChangeMacroBlockQuantizationScale( oSubtitleMacBlk.Y4,
                        lngSubScale,
                        lngOriginalScale );
            }

            // and finally just replace the old macro-block with the new one
            oOriginalMacroBlocks[idx] = oSubtitleMacBlk;

        }
    }
    
    /** An exciting function that changes the quantization scale of a block
     *  in a macro block. */
    private static void ChangeMacroBlockQuantizationScale(
            Block oBlock, long lngBlksCurScale, long lngNewScale) 
    {
        // update the DC coefficient
        oBlock.DCCoefficient.Top6Bits = (int)lngNewScale;
        
        // copy array into arraylist
        ArrayList<Mdec16Bits> oACCodes = 
                new ArrayList<Mdec16Bits>(oBlock.ACCoefficients.length);
        for (Mdec16Bits oMdec : oBlock.ACCoefficients) {
            oACCodes.add(oMdec);
        }

        int i = 0;
        while (i < oACCodes.size()) {
            Mdec16Bits oMdecCode = oACCodes.get(i);
            
            // scale the AC coefficient
            oMdecCode.Bottom10Bits = (int) (
                jpsxdec.util.Math.round(
                oMdecCode.Bottom10Bits * lngBlksCurScale / (double)lngNewScale )
                )
                ;
            
            // if the AC coefficient becomes zero
            // (i.e. if code becomes (#, 0) ), we should remove this code
            if (oMdecCode.Bottom10Bits == 0) {
                oACCodes.remove(i);
                // update the next code (if any) with the removed code's run + 1
                if (i < oACCodes.size())
                    oACCodes.get(i).Top6Bits += oMdecCode.Top6Bits + 1;
            } else {
                // next code
                i++;
            }
        }

        // update the AC coefficients in the block
        oBlock.ACCoefficients = oACCodes.toArray(new Mdec16Bits[0]);
    }
    
    ////////////////////////////////////////////////////////////////////////////    
    
    /** Overwrites the demuxed frame data with new data. 
     * @return how many bytes were written. */
    private static int RemuxFrame(CDSectorReader oCD, PSXMediaSTR oMovie, long lngFrame, byte[] abDemux) throws IOException {
        // get the iterator
        CDXAIterator oCDIter = new CDXAIterator(oCD, 
                (int)oMovie.getStartSector(), (int)oMovie.getEndSector());
        
        int iDemuxPos = 0;
        // while there's still data to write
        while (oCDIter.hasNext() && iDemuxPos < abDemux.length) {
        
            // if the next sector is a frame chunk
            CDXASector oCDSect = oCDIter.peekNext();
            PSXSector oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
            if (oPsxSect instanceof PSXSectorFrameChunk) {
                // and it is of the frame we want
                PSXSectorFrameChunk oFrmChk = (PSXSectorFrameChunk)oPsxSect;
                if (oFrmChk.getFrameNumber() == lngFrame) {
                    
                    // get the original sector data
                    byte[] abSect = oCDSect.getSectorData();
                    // get how much space is free after the header
                    int iNumBytesToCopy = oFrmChk.getPsxUserDataSize();

                    // only copy what's left if we're almost done
                    if (iNumBytesToCopy > abDemux.length - iDemuxPos)
                        iNumBytesToCopy = abDemux.length - iDemuxPos;
                    
                    // overwrite the bytes after the frame chunk header
                    System.arraycopy(abDemux, iDemuxPos, 
                            abSect, PSXSectorFrameChunk.FRAME_CHUNK_HEADER_SIZE, 
                            iNumBytesToCopy);
                    
                    // write the sector to the file
                    oCD.writeSector(oCDIter.getIndex(), abSect);
                    
                    iDemuxPos += iNumBytesToCopy;
                    
                } else if (oFrmChk.getFrameNumber() > lngFrame) {
                    // we passed the frame, which means we have too much data to fit
                    throw new RuntimeException("Too much data to fit in " + lngFrame);
                }
            }
            
            oCDIter.skipNext();

        }        
        
        // return how many bytes were written
        return iDemuxPos;
    }
}
