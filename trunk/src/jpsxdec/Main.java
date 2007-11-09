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
 * Main.java
 *
 */

/* TODO:
 * - finish STR format documentation
 * - make code documentation
 * - make manual
 * - get some pre-MDEC data out of an emulator to compare with my pre-MDEC data
 * - improve command-line documentation
 * // change the demuxer or uncompresser to handle Lain final movie, and also ff7 movies
 * - check if clamping yuv4mpeg2 values at 1-254 help
 * - add option to select the IDCT
 * - add option to ignore checks and just decode whatever it can (probably part of --decode-frame/--decode-audio)
 * - add option to copy str/xa sectors from image
 * - add --format option to LAPKS
 * - add option to set 24/16 bit color, or yuv420/yuv422
 * - change movie decoder to not reset the iterator each frame, &&
 *   + make sure Demuxer knows how to only search for sectors for the next frame
 * /- make CREDITS file
 * /- CLEANUP!!
 * /- better organize the error reporting/checking
 * //- better organize PSXSectorIterator file/classes
 * //- develop a test set
 *
 * CONSIDER:
 * - consider making a CDMediaHandler extends Abstractlist<PSXMediaAbstract> class
 *   it could handle serialze/deserialize, and storing the source file
 * - moving all CD indexing related stuff into its own sub package, and all
 *   decoding related stuff into its own package, and just keep Main and 
 *   Settings in the root package.
 * - Consider making PSXSector just a subclass of CDSector
 * - accept a callback class that can get the status of the decoding,
 *   e.g. the current decoded macro-block (to display on screen)
 *        debug/error messages (puts the debug in the main class, 
 *        and sets up easier debugging feedback once a gui is added)
 * - consider making Settings NOT a static class, and just keep it as a
 *   field in the Main class (since it shouldn't be used anywhere else).
 *   and then i can pass the settings object into plugins to do with it
 *   what they will.
 *
 * FUTURE VERSIONS:
 * - Add frame rate calculation
 * - add better stdin/stdout file handling
 * - add FF8 audio decoding
 * - add GUI
 * - add raw CD reading
 * - add encoding to a video format
 * - add --nocrop option
 * - add option to save audio to the other formats
 * 
 */


package jpsxdec;


import java.awt.Graphics2D;
import java.awt.Image;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import javax.sound.sampled.*;
import javax.swing.ImageIcon;
import jpsxdec.CDSectorReader.CDXASector;
import jpsxdec.util.Matrix8x8;
import jpsxdec.util.Yuv4mpeg2;
import jpsxdec.PSXMedia.PSXMediaSTR;
import jpsxdec.PSXMedia.PSXMediaXA;

public class Main {
    
    public static int DebugVerbose = 2;
    public final static String Version = "0.26(beta)";
    public final static String VerString = "jPSXdec: PSX media decoder, v" + Version;
    
    public static void main(String[] args) {
                
        Settings.ProcessArguments(args);
        
        /*  // Uncommenting this will enable the super fast, but low quality IDCT
        jpsxdec.InverseDiscreteCosineTransform.IDCT idct = 
                new jpsxdec.InverseDiscreteCosineTransform.IDCT();
        StrFrameMDEC.IDCT = idct;
        idct.norm(StrFrameMDEC.MPEG1_DEFAULT_INTRA_QUANTIZATION_MATRIX.getPoints());
         //*/
        
        // Uncommenting this will enable the super slow IDCT
        //StrFrameMDEC.IDCT = new jpsxdec.InverseDiscreteCosineTransform.SimpleIDCT();
        
        // set verbosity
        int[] aiVerbosityLevels = Settings.getVerbosityLevels();
        Main.DebugVerbose = aiVerbosityLevels[0];
        PSXMedia.DebugVerbose = aiVerbosityLevels[1];
        StrFrameDemuxerIS.DebugVerbose = aiVerbosityLevels[2];
        StrFrameUncompressorIS.DebugVerbose = aiVerbosityLevels[3];
        StrFrameMDEC.DebugVerbose = aiVerbosityLevels[4];
        
        if (DebugVerbose > 0)
            System.err.println(VerString);
        
        switch (Settings.getMainCommandType()) {
            case Settings.INDEX_ONLY:
                System.exit(IndexOnly());
                break;
            case Settings.DECODE_ONE:
            case Settings.DECODE_ALL:
                System.exit(NormalDecode());
                break;
            case Settings.SPECIAL_IN_FILE:
                System.exit(DecodeSpecialFrameFile());
                break;
            case Settings.DECODE_SECTORS_FRAME:
                System.exit(DecodeFrameSectors());
                break;
            case Settings.DECODE_SECTORS_AUDIO:
                System.exit(DecodeAudioSectors());
                break;
            case Settings.SECTOR_LIST:
                System.exit(SectorList());
                break;
            case Settings.PLUGIN_LAPKS:
                System.exit(Lain_LAPKS.DecodeLAPKS(Settings.getInputFile(), Settings.getOutputFile()));
                break;
            case Settings.PLUGIN_SITE:
                System.exit(Lain_SITE.DecodeSITE(Settings.getInputFile(), Settings.getOutputFile()));
                break;
        }
        
        return;
    }
    
    //--------------------------------------------------------------------------
    
    private static int IndexOnly() {
        //open input file
        CDSectorReader oCD;
        try {
            oCD = new CDSectorReader(Settings.getInputFile());
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }
        
        if (!oCD.HasSectorHeader() && DebugVerbose > 0) {
            System.err.println("Warning: Input file does not contain entire raw CD sectors.");
            System.err.println("         Audio cannot be decoded.");
        }
        
        if (DebugVerbose > 0)
            System.err.println("Creating index");
        
        // index the STR file
        ArrayList<PSXMedia> oMedias;
        boolean blnSaveIndexFile = false;
        try {
            PrintStream oPrinter;
            if (Settings.getIndexFile().equals("-"))
                oPrinter = System.out;
            else
                oPrinter = new PrintStream(Settings.getIndexFile());
            oMedias = PSXMedia.IndexCD(oCD);
            PSXMedia.SerializeMediaList(oMedias, oPrinter);
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }

        return 0;
    }
    
    //--------------------------------------------------------------------------
    
    private static int NormalDecode() {
        //open input file
        CDSectorReader oCD;
        try {
            oCD = new CDSectorReader(Settings.getInputFile());
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }
        
        if (!oCD.HasSectorHeader() && DebugVerbose > 0) {
            System.err.println("Warning: Input file does not contain entire raw CD sectors.");
            System.err.println("         Audio cannot be decoded.");
        }
        
        // index the STR file
        ArrayList<PSXMedia> oMedias;
        boolean blnSaveIndexFile = false;
        try {
            String sIndexFile = Settings.getIndexFile();
            if (sIndexFile  != null) {
                if (new File(Settings.getIndexFile()).exists()) {
                    if (DebugVerbose > 1)
                        System.err.println("Reading file index");
                    oMedias = PSXMedia.IndexCD(oCD, sIndexFile);
                } else {
                    if (DebugVerbose > 1)
                        System.err.println("Indexing file");
        
                    oMedias = PSXMedia.IndexCD(oCD);
                    blnSaveIndexFile = true;
                }
            } else {
                if (DebugVerbose > 1)
                    System.err.println("Indexing file");
        
                oMedias = PSXMedia.IndexCD(oCD);
            }
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }

        // save index file if necessary
        if (blnSaveIndexFile) {
            try {
                PrintStream oPrinter;
                if (Settings.getIndexFile().equals("-"))
                    oPrinter = System.out;
                else
                    oPrinter = new PrintStream(Settings.getIndexFile());
                PSXMedia.SerializeMediaList(oMedias, oPrinter);
            } catch (IOException ex) {
                if (DebugVerbose > 2)
                    ex.printStackTrace();
                else
                    System.err.println(ex.getMessage());
                return -1;
            }
        }

        // decode the desired media items
        
        int iMediaStart, iMediaEnd, iMediaIndex;

        if (Settings.getMainCommandType() == Settings.DECODE_ALL) {
            iMediaStart = 0;
            iMediaEnd = oMedias.size()-1;
        } else {
            iMediaStart = Settings.getItemToDecode();
            if (iMediaStart < 0) iMediaStart = 0;
            if (iMediaStart > oMedias.size()-1) iMediaStart = oMedias.size()-1;
            iMediaEnd = iMediaStart;
        }
        
        
        for (iMediaIndex = iMediaStart; iMediaIndex <= iMediaEnd; iMediaIndex++) 
        {
            PSXMedia oMedia = oMedias.get(iMediaIndex);

            // movie frames
            if (Settings.DecodeVideo() && (oMedia instanceof PSXMediaSTR)) {
                PSXMediaSTR oMovie = (PSXMediaSTR)oMedia;
                DecodeVideo(oMovie, iMediaIndex);
                
            }

            // movie audio
            if (Settings.DecodeAudio() && (oMedia instanceof PSXMediaSTR)) {
                PSXMediaSTR oMovie = (PSXMediaSTR)oMedia;
                try {
                    if (DebugVerbose > 1)
                        System.err.println("Reading movie audio");
                    
                    PSXSectorRangeIterator oWalker = 
                            oMovie.GetAudioSectorWalker();
                    if (oWalker != null) {
                        StrAudioDemuxerDecoderIS dec = 
                                new StrAudioDemuxerDecoderIS(oWalker);
                        AudioInputStream str = 
                                new AudioInputStream(dec, dec.getFormat(), dec.getLength());
                        
                        AudioSystem.write(str, AudioFileFormat.Type.WAVE, 
                                new File(String.format(
                                "%s%03d.wav",
                                Settings.getOutputFile(), iMediaIndex
                                )));
                    }
                } catch (IOException ex) {
                    if (DebugVerbose > 2)
                        ex.printStackTrace();
                    else if (DebugVerbose > 0)
                        System.err.println(ex.getMessage());
                }
            }
                
            // XA audio
            if (Settings.DecodeAudio() && (oMedia instanceof PSXMediaXA)) {
                PSXMediaXA oAudio = (PSXMediaXA)oMedia;

                int iChannelStart, iChannelEnd, iChannelIndex;
                if (Settings.getChannel() < 0) {
                    iChannelStart = 0;
                    iChannelEnd = 31;
                } else {
                    if (Settings.getChannel() > 31)
                        iChannelStart = 31;
                    else
                        iChannelStart = Settings.getChannel();
                    iChannelEnd = iChannelStart;
                }

                for (iChannelIndex = iChannelStart; iChannelIndex <= iChannelEnd; iChannelIndex++) {
                    
                    try {
                        PSXSectorRangeIterator oWalker =
                                oAudio.GetChannelSectorWalker(iChannelIndex);

                        if (oWalker != null)
                        {
                            if (DebugVerbose > 1)
                                System.err.println("Reading channel " 
                                                   + iChannelIndex);
                    
                            StrAudioDemuxerDecoderIS dec = 
                                    new StrAudioDemuxerDecoderIS(oWalker, iChannelIndex);
                            AudioInputStream str = 
                                    new AudioInputStream(dec, dec.getFormat(), dec.getLength());
                            AudioSystem.write(str, AudioFileFormat.Type.WAVE, 
                                    new File(String.format(
                                    "%s%03d_ch%02d.wav",
                                    Settings.getOutputFile(), 
                                    iMediaIndex, 
                                    iChannelIndex
                                    )));
                        }
                    } catch (IOException ex) {
                        if (DebugVerbose > 2)
                            ex.printStackTrace();
                        else if (DebugVerbose > 0)
                            System.err.println(ex.getMessage());
                    }
                    
                }

            }

        }
            
        return 0;
    }
    
    //..........................................................................
    
    private static void DecodeVideo(PSXMediaSTR oMovie, int iMediaIndex) {
        long iFrameStart = Settings.getStartFrame();
        long iFrameEnd = Settings.getEndFrame();
        long iFrameIndex;

        if (iFrameEnd < 0) iFrameEnd = oMovie.GetEndFrame();

        if (iFrameStart < oMovie.GetStartFrame()) 
            iFrameStart = oMovie.GetStartFrame();
        if (iFrameStart > oMovie.GetEndFrame())
            iFrameStart = oMovie.GetEndFrame();

        if (iFrameEnd < oMovie.GetStartFrame())
            iFrameEnd = oMovie.GetStartFrame();
        if (iFrameEnd > oMovie.GetEndFrame()) 
            iFrameEnd = oMovie.GetEndFrame();

        if (iFrameStart > iFrameEnd) {
            long lng = iFrameStart;
            iFrameStart = iFrameEnd;
            iFrameEnd = lng;
        }

        for (iFrameIndex = iFrameStart; iFrameIndex <= iFrameEnd; iFrameIndex++) 
        {
            String sFrameFile = String.format(
                        "%s%03d_f%04d.%s",
                        Settings.getOutputFile(), 
                        iMediaIndex, 
                        iFrameIndex,
                        Settings.getOutputFormat());
            try {
                
                PSXSectorRangeIterator oSectorWalker = 
                        oMovie.GetFrameSectorWalker(iFrameIndex);
                StrFrameDemuxerIS str = 
                        new StrFrameDemuxerIS(oSectorWalker, iFrameIndex);
                
                if (DebugVerbose > 0)
                    System.err.println("Reading frame " + iFrameIndex);

                DecodeAndSaveFrame("demux", str, sFrameFile);
                

            } catch (IOException ex) {
                if (DebugVerbose > 2)
                    ex.printStackTrace();
                else if (DebugVerbose > 0)
                    System.err.println(ex.getMessage());
            }

        } // for
    }
    
    //--------------------------------------------------------------------------
    
    private static int DecodeFrameSectors() {
        CDSectorReader oCD;
        try {
            oCD = new CDSectorReader(Settings.getInputFile());
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }
        
        if (!oCD.HasSectorHeader() && DebugVerbose > 0) {
            System.err.println("Warning: Input file does not contain entire raw CD sectors.");
            System.err.println("         Audio cannot be decoded.");
        }
        
        // get the walker (texas ranger)
        PSXSectorListIterator oSectorWalker = 
                new PSXSectorListIterator(oCD, Settings.getSectorList());
        
        StrFrameDemuxerIS str = new StrFrameDemuxerIS(oSectorWalker);
        
        String sFrameFile = String.format(
                    "%s.%s",
                    Settings.getOutputFile(), 
                    Settings.getOutputFormat());
        
        System.err.println("Reading frame sectors");
        try {
            
            DecodeAndSaveFrame("demux", str, sFrameFile);
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }
        
        return 0;
    }
    
    //..........................................................................
    
    private static int DecodeAudioSectors() {
        CDSectorReader oCD;
        try {
            oCD = new CDSectorReader(Settings.getInputFile());
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }
        
        if (!oCD.HasSectorHeader() && DebugVerbose > 0) {
            System.err.println("Warning: Input file does not contain entire raw CD sectors.");
            System.err.println("         Audio cannot be decoded.");
        }
        
        PSXSectorListIterator oSectorWalker = 
                new PSXSectorListIterator(oCD, Settings.getSectorList());
        
        StrAudioDemuxerDecoderIS dec = new StrAudioDemuxerDecoderIS(oSectorWalker);
        if (dec.HasAudio()) {
            AudioInputStream str = 
                    new AudioInputStream(dec, dec.getFormat(), dec.getLength());

            if (DebugVerbose > 0)
                System.err.println("Reading audio sectors");

            try {
                AudioSystem.write(str, AudioFileFormat.Type.WAVE, 
                        new File(String.format(
                        "%s.wav",
                        Settings.getOutputFile()
                        )));
            } catch (IOException ex) {
                if (DebugVerbose > 2)
                    ex.printStackTrace();
                else if (DebugVerbose > 0)
                    System.err.println(ex.getMessage());
                return -1;
            }
        } else {
            if (DebugVerbose > 0)
                System.err.println("No audio found");
        }
        
        return 0;
    }

    //..........................................................................
    
    private static int DecodeSpecialFrameFile() {
        
        String sFrameFile = String.format(
                    "%s.%s",
                    Settings.getOutputFile(), 
                    Settings.getOutputFormat());
        try {
            
            if (DebugVerbose > 0)
                System.err.println("Reading frame file");
            
            FileInputStream is;
            is = new FileInputStream(Settings.getInputFile());
            is.mark(0);
            DecodeAndSaveFrame(Settings.getInputFileFormat(), is, sFrameFile);
            
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }
        
        return 0;
    }

    //--------------------------------------------------------------------------
   
    private static void DecodeAndSaveFrame(String sInputType, 
                                           InputStream str, 
                                           String sFrameFile) 
            throws IOException 
    {
        long lngWidth;
        long lngHeight;
        
        if (str instanceof StrFrameDemuxerIS) {
            lngWidth = ((StrFrameDemuxerIS)str).getWidth();
            lngHeight = ((StrFrameDemuxerIS)str).getHeight();
        } else if (str instanceof StrFrameUncompressorIS) {
            lngWidth = ((StrFrameUncompressorIS)str).getWidth();
            lngHeight = ((StrFrameUncompressorIS)str).getHeight();
        } else {
            lngWidth = Settings.getWidth();
            lngHeight = Settings.getHeight();
        }
            
        
        if (sInputType.equals("demux")) {

            if (Settings.getOutputFormat().equals("demux")) {
                FileOutputStream fos = new FileOutputStream(sFrameFile);
                int ib;
                while ((ib = str.read()) >= 0)
                    fos.write(ib);
                fos.close();
                return;
            }

            str = new StrFrameUncompressorIS(str, lngWidth, lngHeight);
            if (Settings.getOutputFormat().equals("0rlc")) {
                FileOutputStream fos = new FileOutputStream(sFrameFile);
                int ib;
                while ((ib = str.read()) >= 0)
                    fos.write(ib);
                fos.close();
                return;
            }

        }
        
        Yuv4mpeg2 oYuv = StrFrameMDEC.DecodeFrame(str, lngWidth, lngHeight);
        if (Settings.getOutputFormat().equals("yuv") ||
            Settings.getOutputFormat().equals("y4m")) 
        {
            FileOutputStream fos = new FileOutputStream(sFrameFile);
            oYuv.Write(fos);
            fos.close();
            return;
        }

        BufferedImage bi = oYuv.toBufferedImage();
        ImageIO.write(bi, Settings.getOutputFormat(), new File(sFrameFile));
    }

    //--------------------------------------------------------------------------

    private static int SectorList() {
        CDSectorReader oCD;
        try {
            oCD = new CDSectorReader(Settings.getInputFile());
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }
        
        if (!oCD.HasSectorHeader() && DebugVerbose > 0) {
            System.err.println("Warning: Input file does not contain entire raw CD sectors.");
            System.err.println("         Audio cannot be decoded.");
        }
        
        if (DebugVerbose > 1)
            System.err.println("Generating sector list");
        
        PrintStream ps;
        try {
            if (Settings.getOutputFile().equals("-"))
                ps = System.out;
            else
                ps = new PrintStream(Settings.getOutputFile());
            
            for (CDXASector oCDXASect : oCD) {
                PSXSector oSect = 
                        PSXSector.SectorIdentifyFactory(oCDXASect);
                if (oSect != null)
                    ps.println(oSect.toString());
            }
            
            ps.flush();
            if (!ps.equals(System.out)) ps.close();
            return 0;
            
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }
    }

    
}
