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
 */

/* TODO:
 * - Probably get rid of VideoFrameConverter instance stuff, and move the
 *   static stuff into PSXMedia.java
 * - Perhaps put the ADPCMdecoder code into StrAudioDemuxerDecoder and FF8AudioDemuxerDecoder
 * - Rename Yuv4mpeg2 to something else since it doesn't produce
 *   compliant YUV spec frames anymore (since PSX uses different YUV->RGB equation).
 * - figure out how to output YUV images now that we know the YUV->RGB
 *   equation is different from the YUV image format...
 * - better handle the indexing of media items (associate the index # with the item)
 * // Rename LittleEndianIO class to io class
 * // Move audio demuxerdecoders and ADPCMdecoder into its own package.
 * // Do something with AudioConverter class. -- deleted
 * // Move the Tim class somewhere...probably in the media package
 * // Rename IIDCT to IDCTinterface
 * // Rename StrFrameMDEC to...I dunno, PsxMDEC? MDEC?
 * // change movie decoder to not reset the iterator each frame, &&
 * // + make sure Demuxer knows how to only search for sectors for the next frame
 * - add the more through searching for TIM files to PSXMedia
 * - change Settings class to use a tiered architecture, remvoing
 *   elements from the array as we find matching commands and passing the
 *   rest furthur down the stream
 * - accept a callback class that can get the status of the decoding,
 *   e.g. the current decoded macro-block (to display on screen)
 *        debug/error messages (puts the debug in the main class, 
 *        and sets up easier debugging feedback once a gui is added)
 * // move all CD indexing related stuff into its own sub package, and all
 *   decoding related stuff into its own package, and just keep Main and 
 *   Settings in the root package.
 *
 *
 * - finish STR format documentation
 * - make code documentation
 * - make manual
 * - get some pre-MDEC data out of an emulator to compare with my pre-MDEC data
 * // check if clamping yuv4mpeg2 values at 1-254 help
 * // Add track handling to CDSectorReader -- it's too much of a pain and not really worth it. just gonna return null if the sector type is unrecognized
 * /- make CREDITS file
 * /- CLEANUP!!
 * /- better organize the error reporting/checking
 * //- better organize PSXSectorIterator file/classes
 * //- develop a test set
 *
 * OPTIONS:
 * - need to clean up plugin command-line
 * - add option to set 24/16 bit color, or yuv420/yuv422
 * - add option to select the IDCT
 * - add option to ignore checks and just decode whatever it can (probably part of --decode-frame/--decode-audio)
 * - add option to copy str/xa sectors from image
 * - add --format option to LAPKS
 * - add --nocrop option
 * - add option to save audio to the other formats
 *
 * CONSIDER:
 * - consider making a CDMediaHandler extends Abstractlist<PSXMediaAbstract> class
 *   it could handle serialze/deserialize, and storing the source file
 * - Consider making PSXSector just a subclass of CDXASector
 *
 * FUTURE VERSIONS:
 * - Add frame rate calculation
 * - add better stdin/stdout file handling
 * - add FF8 audio decoding
 * - add GUI
 * - add raw CD reading
 * - add encoding to a video format
 * 
 */


package jpsxdec;


import java.io.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import jpsxdec.audiodecoding.StrAudioDemuxerDecoderIS;
import jpsxdec.cdreaders.CDSectorReader.CDXASector;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.mdec.MDEC;
import jpsxdec.mdec.Yuv4mpeg2;
import jpsxdec.demuxers.*;
import jpsxdec.media.*;
import jpsxdec.media.Tim;
import jpsxdec.plugins.*;
import jpsxdec.sectortypes.*;
import jpsxdec.uncompressors.StrFrameUncompressorIS;
import jpsxdec.util.*;

public class Main {
    
    public static int DebugVerbose = 2;
    public final static String Version = "0.30(beta)";
    public final static String VerString = "jPSXdec: PSX media decoder, v" + Version;
    private static Settings m_oMainSettings;
    
    private static void timsearch() {
        try {
            CDSectorReader oCD = new CDSectorReader("..\\..\\disc1.iso");
            
            PSXSectorRangeIterator oIter = new PSXSectorRangeIterator(oCD);
            int iCount = 0;
            while (oIter.hasNext()) {
                
                while (oIter.hasNext())
                {
                    if (oIter.peekNext() instanceof PSXSector.PSXSectorUnknownData)
                        break;
                    else
                        oIter.skipNext();
                }
                if (!oIter.hasNext()) break;

                try {
                    UnknownDataDemuxerIS oStrm = new UnknownDataDemuxerIS(oIter);
                    while (true) {
                        oStrm.mark(-1);
                        long lng = oStrm.getFilePointer();
                        try {
                            Tim oTim = new Tim(oStrm);
                            System.out.println(iCount + "# "+ lng + " to " + oStrm.getFilePointer());
                            for (int i = 0; i < oTim.getPaletteCount(); i++) {
                                ImageIO.write(oTim.toBufferedImage(i),"png", new File(iCount + "-" + lng + "-" + i + ".png"));
                            }
                            iCount++;
                        } catch (NotThisTypeException ex) {}
                        oStrm.reset();
                        oStrm.skip(4);
                    }
                } catch (EOFException ex) {
                    //ex.printStackTrace();
                }
            }
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.exit(0);
    }
    
    
    public static void main(String[] args) {
        
        //timsearch();
        
        /*
        if (args.length == 0) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new Gui().setVisible(true);
                }
            });
            
            System.exit(0);
        }
        */
        
                
        m_oMainSettings = new Settings(args);
        
        /*  // Uncommenting this will enable the super fast, but low quality IDCT
        jpsxdec.InverseDiscreteCosineTransform.IDCT idct = 
                new jpsxdec.InverseDiscreteCosineTransform.IDCT();
        StrFrameMDEC.IDCT = idct;
        idct.norm(StrFrameMDEC.MPEG1_DEFAULT_INTRA_QUANTIZATION_MATRIX.getPoints());
         //*/
        
        // Uncommenting this will enable the super slow IDCT
        //StrFrameMDEC.IDCT = new jpsxdec.InverseDiscreteCosineTransform.SimpleIDCT();
        
        // set verbosity
        int[] aiVerbosityLevels = m_oMainSettings.getVerbosityLevels();
        Main.DebugVerbose = aiVerbosityLevels[0];
        PSXMedia.DebugVerbose = aiVerbosityLevels[1];
        StrFrameDemuxerIS.DebugVerbose = aiVerbosityLevels[2];
        StrFrameUncompressorIS.DebugVerbose = aiVerbosityLevels[3];
        MDEC.DebugVerbose = aiVerbosityLevels[4];
        
        if (DebugVerbose > 0)
            System.err.println(VerString);
        
        switch (m_oMainSettings.getMainCommandType()) {
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
                System.exit(Lain_LAPKS.DecodeLAPKS(m_oMainSettings.getInputFile(), m_oMainSettings.getOutputFile()));
                break;
            case Settings.PLUGIN_SITE:
                System.exit(Lain_SITE.DecodeSITE(m_oMainSettings.getInputFile(), m_oMainSettings.getOutputFile()));
                break;
        }
        
        return;
    }
    
    //--------------------------------------------------------------------------
    
    private static int IndexOnly() {
        //open input file
        CDSectorReader oCD;
        try {
            oCD = new CDSectorReader(m_oMainSettings.getInputFile());
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
            if (m_oMainSettings.getIndexFile().equals("-"))
                oPrinter = System.out;
            else
                oPrinter = new PrintStream(m_oMainSettings.getIndexFile());
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
            oCD = new CDSectorReader(m_oMainSettings.getInputFile());
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
            String sIndexFile = m_oMainSettings.getIndexFile();
            if (sIndexFile  != null) {
                if (new File(m_oMainSettings.getIndexFile()).exists()) {
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
                if (m_oMainSettings.getIndexFile().equals("-"))
                    oPrinter = System.out;
                else
                    oPrinter = new PrintStream(m_oMainSettings.getIndexFile());
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

        // Clamp media indexes
        if (m_oMainSettings.getMainCommandType() == m_oMainSettings.DECODE_ALL) {
            iMediaStart = 0;
            iMediaEnd = oMedias.size()-1;
        } else {
            iMediaStart = m_oMainSettings.getItemToDecode();
            if (iMediaStart < 0) iMediaStart = 0;
            if (iMediaStart > oMedias.size()-1) iMediaStart = oMedias.size()-1;
            iMediaEnd = iMediaStart;
        }
        
        
        try {
            
            
            for (iMediaIndex = iMediaStart; iMediaIndex <= iMediaEnd; iMediaIndex++) 
            {
                PSXMedia oMedia = oMedias.get(iMediaIndex);
                
                final String sFinalName = 
                        String.format("%s%03d", 
                            m_oMainSettings.getOutputFile(),
                            iMediaIndex);
                
                switch (oMedia.getMediaType()) {
                    case PSXMedia.MEDIA_TYPE_IMAGE:
                        if (m_oMainSettings.DecodeVideo()) {
                            System.err.println("Decoding image: " + oMedia.toString());
                            oMedia.Decode(new Options.IImageOptions() {
                                public String getOutputFileName() {
                                    return m_oMainSettings.getOutputFile() + "_p%d";
                                }

                                public String getOutputImageFormat() {
                                    return m_oMainSettings.getOutputFormat();
                                }
                            });
                        }
                        break;
                    case PSXMedia.MEDIA_TYPE_VIDEO:
                        if (m_oMainSettings.DecodeVideo()) {
                            System.err.println("Decoding video: " + oMedia.toString());
                            oMedia.Decode(new Options.IVideoOptions() {
                                public String getOutputFileName() {
                                    return sFinalName + "_f%04d";
                                }

                                public long getStartFrame() {
                                    return m_oMainSettings.getStartFrame();
                                }

                                public long getEndFrame() {
                                    return m_oMainSettings.getEndFrame();
                                }

                                public String getOutputImageFormat() {
                                    return m_oMainSettings.getOutputFormat();
                                }
                            });
                        }
                        break;
                    case PSXMedia.MEDIA_TYPE_VIDEO_AUDIO:
                        if (m_oMainSettings.DecodeVideo()) {
                            System.err.println("Decoding video: " + oMedia.toString());
                            oMedia.Decode(new Options.IVideoOptions() {
                                public String getOutputFileName() {
                                    return sFinalName + "_f%04d";
                                }

                                public long getStartFrame() {
                                    return m_oMainSettings.getStartFrame();
                                }

                                public long getEndFrame() {
                                    return m_oMainSettings.getEndFrame();
                                }

                                public String getOutputImageFormat() {
                                    return m_oMainSettings.getOutputFormat();
                                }
                            });
                        }
                        if (m_oMainSettings.DecodeAudio()) {
                            System.err.println("Decoding audio: " + oMedia.toString());
                            oMedia.Decode(new Options.IAudioOptions() {
                                public String getOutputFileName() {
                                    return sFinalName;
                                }
                                
                                public double getScale() {
                                    return m_oMainSettings.getAudioScale();
                                }

                                public String getOutputFormat() {
                                    return "wav";
                                }
                            });
                        }
                        break;
                    case PSXMedia.MEDIA_TYPE_XA:
                        if (m_oMainSettings.DecodeAudio()) {
                            System.err.println("Decoding XA audio: " + oMedia.toString());
                            oMedia.Decode(new Options.IXAOptions() {
                                public String getOutputFileName() {
                                    return sFinalName + "_c%02d";
                                }
                                
                                public int getChannel() {
                                    return m_oMainSettings.getChannel();
                                }

                                public double getScale() {
                                    return m_oMainSettings.getAudioScale();
                                }

                                public String getOutputFormat() {
                                    return "wav";
                                }
                            });
                        }
                        break;
                } // switch

            } // for
            
            if (DebugVerbose > 1)
                System.err.println("Media decoding complete.");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
            
        return 0;
    }
    
    //--------------------------------------------------------------------------
    
    private static int DecodeFrameSectors() {
        CDSectorReader oCD;
        try {
            oCD = new CDSectorReader(m_oMainSettings.getInputFile());
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
        
        PSXSectorListIterator oIter = 
                new PSXSectorListIterator(oCD, m_oMainSettings.getSectorList());
        
        try {
            StrFrameDemuxerIS str = new StrFrameDemuxerIS(oIter);
        
            String sFrameFile = String.format(
                        "%s.%s",
                        m_oMainSettings.getOutputFile(), 
                        m_oMainSettings.getOutputFormat());

            System.err.println("Reading frame sectors");
            jpsxdec.media.VideoFrameConverter.DecodeAndSaveFrame(
                    "demux", 
                    m_oMainSettings.getOutputFormat(),
                    str, sFrameFile, 
                    -1, -1);
            
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
            oCD = new CDSectorReader(m_oMainSettings.getInputFile());
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
        
        PSXSectorListIterator oIter = 
                new PSXSectorListIterator(oCD, m_oMainSettings.getSectorList());
        
        try {
            StrAudioDemuxerDecoderIS dec = new StrAudioDemuxerDecoderIS(oIter);
            if (dec.HasAudio()) {
                AudioInputStream str = 
                        new AudioInputStream(dec, dec.getFormat(), dec.getLength());

                if (DebugVerbose > 0)
                    System.err.println("Reading audio sectors");

                AudioSystem.write(str, AudioFileFormat.Type.WAVE, 
                        new File(String.format(
                        "%s.wav",
                        m_oMainSettings.getOutputFile()
                        )));
            } else {
                if (DebugVerbose > 0)
                    System.err.println("No audio found");
            }
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
    
    private static int DecodeSpecialFrameFile() {
        
        String sFrameFile = String.format(
                    "%s.%s",
                    m_oMainSettings.getOutputFile(), 
                    m_oMainSettings.getOutputFormat());
        try {
            
            if (DebugVerbose > 0)
                System.err.println("Reading frame file");
            
            FileInputStream is;
            is = new FileInputStream(m_oMainSettings.getInputFile());
            jpsxdec.media.VideoFrameConverter.DecodeAndSaveFrame(
                    m_oMainSettings.getInputFileFormat(), 
                    m_oMainSettings.getOutputFormat(),
                    is, sFrameFile, 
                    m_oMainSettings.getWidth(), m_oMainSettings.getHeight());
            
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
   
    private static int SectorList() {
        CDSectorReader oCD;
        try {
            oCD = new CDSectorReader(m_oMainSettings.getInputFile());
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
            if (m_oMainSettings.getOutputFile().equals("-"))
                ps = System.out;
            else
                ps = new PrintStream(m_oMainSettings.getOutputFile());
            
            PSXSectorRangeIterator oIter = new PSXSectorRangeIterator(oCD);
            while (oIter.hasNext()) {
                PSXSector oSect = oIter.next();
                if (oSect != null) {
                    String s = oSect.toString();
                    if (s != null) ps.println(s);
                }
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
