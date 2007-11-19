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
 * // change TIM class to save color indexed bufferedimages
 * // Change CDSectorReader to NOT be Abstractlist, and change Iterators into my "AdvancedIterator" class
 * // make Settings NOT a static class, and just keep it as a
 *   field in the Main class (since it shouldn't be used anywhere else).
 *   and then i can pass the settings object into plugins to do with it
 *   what they will.
 * // finish change uncompresser to handle Lain final movie, and also ff7 movies
 * // change movie decoder to not reset the iterator each frame, &&
 * // + make sure Demuxer knows how to only search for sectors for the next frame
 * - add searching for TIM files in PSXMedia
 * - try changing decoder to only divide the luminance DC by 4, but leave chrom DC alone
 * - change Settings class to use a tiered architecture, remvoing
 *   elements from the array as we find matching commands and passing the
 *   rest furthur down the stream
 * - accept a callback class that can get the status of the decoding,
 *   e.g. the current decoded macro-block (to display on screen)
 *        debug/error messages (puts the debug in the main class, 
 *        and sets up easier debugging feedback once a gui is added)
 * - move all CD indexing related stuff into its own sub package, and all
 *   decoding related stuff into its own package, and just keep Main and 
 *   Settings in the root package.
 *
 *
 * - finish STR format documentation
 * - make code documentation
 * - make manual
 * - get some pre-MDEC data out of an emulator to compare with my pre-MDEC data
 * - improve command-line documentation
 * x Add track handling to CDSectorReader -- it's too much of a pain and not really worth it. just gonna return null if the sector type is unrecognized
 * - check if clamping yuv4mpeg2 values at 1-254 help
 * - need to clean up  plugin command-line
 * - add option to set 24/16 bit color, or yuv420/yuv422
 * /- make CREDITS file
 * /- CLEANUP!!
 * /- better organize the error reporting/checking
 * //- better organize PSXSectorIterator file/classes
 * //- develop a test set
 *
 * OPTIONS:
 * - add option to select the IDCT
 * - add option to ignore checks and just decode whatever it can (probably part of --decode-frame/--decode-audio)
 * - add option to copy str/xa sectors from image
 * - add --format option to LAPKS
 * - add --nocrop option
 *
 * CONSIDER:
 * - consider making a CDMediaHandler extends Abstractlist<PSXMediaAbstract> class
 *   it could handle serialze/deserialize, and storing the source file
 * - Consider making PSXSector just a subclass of CDSector
 *
 * FUTURE VERSIONS:
 * - Add frame rate calculation
 * - add better stdin/stdout file handling
 * - add FF8 audio decoding
 * - add GUI
 * - add raw CD reading
 * - add encoding to a video format
 * - add option to save audio to the other formats
 * 
 */


package jpsxdec;


import java.io.*;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import javax.sound.sampled.*;
import jpsxdec.CDSectorReader.CDXASector;
import jpsxdec.plugins.*;
import jpsxdec.util.Matrix8x8;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.Yuv4mpeg2;
import jpsxdec.PSXMedia.*;

public class Main {
    
    public static int DebugVerbose = 2;
    public final static String Version = "0.27(beta)";
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
        StrFrameMDEC.DebugVerbose = aiVerbosityLevels[4];
        
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

        if (m_oMainSettings.getMainCommandType() == m_oMainSettings.DECODE_ALL) {
            iMediaStart = 0;
            iMediaEnd = oMedias.size()-1;
        } else {
            iMediaStart = m_oMainSettings.getItemToDecode();
            if (iMediaStart < 0) iMediaStart = 0;
            if (iMediaStart > oMedias.size()-1) iMediaStart = oMedias.size()-1;
            iMediaEnd = iMediaStart;
        }
        
        
        for (iMediaIndex = iMediaStart; iMediaIndex <= iMediaEnd; iMediaIndex++) 
        {
            PSXMedia oMedia = oMedias.get(iMediaIndex);

            // movie frames
            if (m_oMainSettings.DecodeVideo() && (oMedia instanceof PSXMediaSTR)) {
                PSXMediaSTR oMovie = (PSXMediaSTR)oMedia;
                DecodeVideo(oMovie, iMediaIndex);
                
            }

            // movie audio
            if (m_oMainSettings.DecodeAudio() && (oMedia instanceof PSXMediaSTR)) {
                PSXMediaSTR oMovie = (PSXMediaSTR)oMedia;
                if (oMovie.HasAudio()) {
                    try {
                        if (DebugVerbose > 1)
                            System.err.println("Reading movie audio");

                        PSXSectorRangeIterator oIter = oMovie.GetSectorIterator();
                        StrAudioDemuxerDecoderIS dec = 
                                new StrAudioDemuxerDecoderIS(oIter);
                        AudioInputStream str = 
                                new AudioInputStream(dec, dec.getFormat(), dec.getLength());

                        AudioSystem.write(str, AudioFileFormat.Type.WAVE, 
                                new File(String.format(
                                "%s%03d.wav",
                                m_oMainSettings.getOutputFile(), iMediaIndex
                                )));
                    } catch (IOException ex) {
                        if (DebugVerbose > 2)
                            ex.printStackTrace();
                        else if (DebugVerbose > 0)
                            System.err.println(ex.getMessage());
                    }
                }
            }
                
            // XA audio
            if (m_oMainSettings.DecodeAudio() && (oMedia instanceof PSXMediaXA)) {
                PSXMediaXA oAudio = (PSXMediaXA)oMedia;

                int iChannelStart, iChannelEnd, iChannelIndex;
                if (m_oMainSettings.getChannel() < 0) {
                    iChannelStart = 0;
                    iChannelEnd = 31;
                } else {
                    if (m_oMainSettings.getChannel() > 31)
                        iChannelStart = 31;
                    else
                        iChannelStart = m_oMainSettings.getChannel();
                    iChannelEnd = iChannelStart;
                }

                for (iChannelIndex = iChannelStart; iChannelIndex <= iChannelEnd; iChannelIndex++) {
                    
                    try {
                        PSXSectorRangeIterator oIter =
                                oAudio.GetChannelSectorIterator(iChannelIndex);

                        if (oIter != null)
                        {
                            if (DebugVerbose > 1)
                                System.err.println("Reading channel " 
                                                   + iChannelIndex);
                    
                            StrAudioDemuxerDecoderIS dec = 
                                    new StrAudioDemuxerDecoderIS(oIter, iChannelIndex);
                            AudioInputStream str = 
                                    new AudioInputStream(dec, dec.getFormat(), dec.getLength());
                            AudioSystem.write(str, AudioFileFormat.Type.WAVE, 
                                    new File(String.format(
                                    "%s%03d_ch%02d.wav",
                                    m_oMainSettings.getOutputFile(), 
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
            
            if (oMedia instanceof PSXMediaTIM) {
                if (DebugVerbose > 1) System.err.println("Reading TIM image");
                PSXMediaTIM oTimMedia = (PSXMediaTIM)oMedia;
                PSXSectorRangeIterator oSectIter = oTimMedia.GetSectorIterator();
                try {
                    InputStream oIS = new UnknownDataDemuxerIS(oSectIter);
                    Tim oTim = new Tim(oIS);
                    for (int i = 0; i < oTim.getPaletteCount(); i++) {
                        ImageIO.write(
                              oTim.toBufferedImage(i),
                                // TODO: Add setting type here
                                "png",
                                new File(String.format(
                                    "%s%03d_pal%02d.png",
                                    m_oMainSettings.getOutputFile(), 
                                    iMediaIndex,
                                    i))
                                    );
                    }
                    
                } catch (IOException ex) {
                    if (DebugVerbose > 2)
                        ex.printStackTrace();
                    else if (DebugVerbose > 0)
                        System.err.println(ex.getMessage());
                } catch (NotThisTypeException ex) {
                    if (DebugVerbose > 2)
                        ex.printStackTrace();
                    else if (DebugVerbose > 0)
                        System.err.println(ex.getMessage());
                }
                
            }

        }
            
        return 0;
    }
    
    //..........................................................................
    
    private static void DecodeVideo(PSXMediaSTR oMovie, int iMediaIndex) {
        long iFrameStart = m_oMainSettings.getStartFrame();
        long iFrameEnd = m_oMainSettings.getEndFrame();
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

        PSXSectorRangeIterator oIter = oMovie.GetSectorIterator();
        int iSaveIndex1 = oIter.getIndex();
        int iSaveIndex2 = iSaveIndex1;
        for (iFrameIndex = iFrameStart; iFrameIndex <= iFrameEnd; iFrameIndex++) 
        {
            String sFrameFile = String.format(
                        "%s%03d_f%04d.%s",
                        m_oMainSettings.getOutputFile(), 
                        iMediaIndex, 
                        iFrameIndex,
                        m_oMainSettings.getOutputFormat());
            try {
                
                oIter.gotoIndex(iSaveIndex1);
                StrFrameDemuxerIS str = 
                        new StrFrameDemuxerIS(oIter, iFrameIndex);
                
                if (DebugVerbose > 0)
                    System.err.println("Reading frame " + iFrameIndex);

                DecodeAndSaveFrame("demux", str, sFrameFile);
                
                // if the iterator was searched to the end, we don't
                // want to save the end sector (or we'll miss sectors
                // before it). This can happen when saving as demux because 
                // it has to search to the end of the stream
                if (oIter.hasNext()) {
                    iSaveIndex1 = iSaveIndex2;
                    iSaveIndex2 = oIter.getIndex();
                }

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
            is.mark(0);
            DecodeAndSaveFrame(m_oMainSettings.getInputFileFormat(), is, sFrameFile);
            
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
            lngWidth = m_oMainSettings.getWidth();
            lngHeight = m_oMainSettings.getHeight();
        }
            
        
        if (sInputType.equals("demux")) {

            if (m_oMainSettings.getOutputFormat().equals("demux")) {
                FileOutputStream fos = new FileOutputStream(sFrameFile);
                int ib;
                while ((ib = str.read()) >= 0)
                    fos.write(ib);
                fos.close();
                return;
            }

            str = new StrFrameUncompressorIS(str, lngWidth, lngHeight);
            if (m_oMainSettings.getOutputFormat().equals("0rlc")) {
                FileOutputStream fos = new FileOutputStream(sFrameFile);
                int ib;
                while ((ib = str.read()) >= 0)
                    fos.write(ib);
                fos.close();
                return;
            }

        }
        
        Yuv4mpeg2 oYuv = StrFrameMDEC.DecodeFrame(str, lngWidth, lngHeight);
        if (m_oMainSettings.getOutputFormat().equals("yuv") ||
            m_oMainSettings.getOutputFormat().equals("y4m")) 
        {
            FileOutputStream fos = new FileOutputStream(sFrameFile);
            oYuv.Write(fos);
            fos.close();
            return;
        }

        BufferedImage bi = oYuv.toBufferedImage();
        ImageIO.write(bi, m_oMainSettings.getOutputFormat(), new File(sFrameFile));
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
