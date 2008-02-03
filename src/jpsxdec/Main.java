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
 * * Rearrage CDXASector so submode is its own class, and referring it can be
 *   via sector.getSubmode.getByte(), sector.getSubmode.getEofMarker(), etc
 * * Test out the new games
 * // Finish testing FF8 and FF9
 * // Change MediaHandler to not be an AbstractList, but implements the foreach
 *   interface, and have two getters: getByIndex() and getByString(). Get rid
 *   of HashtableInt and just have an internal Hashtable. Each media item added
 *   will be put in the Hashtable with two keys: the index, and the string.
 *   Maybe also have an Arraylist just to keep an ordered list of items for
 *   iterations/foreach.
 * // Do something better with that darn Options class...
 * // make a CDMediaHandler extends Abstractlist<PSXMedia> class
 *   it could handle serialze/deserialize, and storing the source file
 * // better handle the indexing of media items (associate the index # with the item)
 * // accept a callback class that can get the status of the decoding,
 *   e.g. the current decoded macro-block (to display on screen)
 *        debug/error messages (puts the debug in the main class, 
 *        and sets up easier debugging feedback once a gui is added)
 * - Probably get rid of VideoFrameConverter instance stuff, and move the
 *   static stuff into PSXMedia.java
 * - Perhaps put the ADPCMdecoder code into StrAudioDemuxerDecoder and FF8AudioDemuxerDecoder
 * // Rename Yuv4mpeg2 to something else since it doesn't produce
 *   compliant YUV spec frames anymore (since PSX uses different YUV->RGB equation).
 * * change output of YUV images to adjust for yuv4mpeg2 format 
 * * Change PsxYuv.write(OutputStream) to only write the header if requested,
 *   so multiple images can be written to the same file
 * // change movie decoder to not reset the iterator each frame, &&
 * // + make sure Demuxer knows how to only search for sectors for the next frame
 * - add the more through searching for TIM files to PSXMedia
 * - change Settings class to use a tiered architecture, remvoing
 *   elements from the array as we find matching commands and passing the
 *   rest furthur down the stream
 *
 *
 * /- finish STR format documentation
 * - make code documentation
 * - make manual
 * - get some pre-MDEC data out of an emulator to compare with my pre-MDEC data
 * // Add track handling to CDSectorReader -- it's too much of a pain and not really worth it. just gonna return null if the sector type is unrecognized
 * /- make CREDITS file
 * /- CLEANUP!!
 * /- better organize the error reporting/checking
 * - develop a test set
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
 * - Consider making PSXSector just a subclass of CDXASector
 *
 * FUTURE VERSIONS:
 * - Add frame rate calculation
 * - add better stdin/stdout file handling
 * // add FF8 audio decoding
 * - add GUI
 * - add raw CD reading
 * - add encoding to a video format
 * 
 */


package jpsxdec;


import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import jpsxdec.audiodecoding.StrAudioDemuxerDecoderIS;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.mdec.MDEC;
import jpsxdec.demuxers.*;
import jpsxdec.media.*;
import jpsxdec.media.Tim;
import jpsxdec.plugins.*;
import jpsxdec.sectortypes.*;
import jpsxdec.uncompressors.StrFrameUncompressorIS;
import jpsxdec.util.*;

public class Main {
    
    public static int DebugVerbose = 2;
    public final static String Version = "0.31(beta)";
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
        Main.DebugVerbose = 10;
        PSXMedia.DebugVerbose = 10;
        StrFrameUncompressorIS.DebugVerbose = 10;
        MDEC.DebugVerbose = 5;
        */
        if (args.length == 0) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    new Gui().setVisible(true);
                }
            });

            return;
        }
        
        
                
        m_oMainSettings = new Settings(args);
        
        /*  // Uncommenting this will enable the super fast, but low quality IDCT
        jpsxdec.mdec.IDCT idct = new jpsxdec.mdec.IDCT();
        jpsxdec.mdec.MDEC.IDCT = idct;
        idct.norm(jpsxdec.mdec.MDEC.PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX.getPoints());
         //*/
        
        // Uncommenting this will enable the super slow IDCT
        //jpsxdec.mdec.MDEC.IDCT = new jpsxdec.mdec.SimpleIDCT();
        
        // set verbosity
        int[] aiVerbosityLevels = m_oMainSettings.getVerbosityLevels();
        Main.DebugVerbose = aiVerbosityLevels[0];
        PSXMedia.DebugVerbose = aiVerbosityLevels[1];
        //StrFrameDemuxerIS.DebugVerbose = aiVerbosityLevels[2];
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
        MediaHandler oMedias;
        try {
            PrintStream oPrinter;
            if (m_oMainSettings.getIndexFile().equals("-"))
                oPrinter = System.out;
            else
                oPrinter = new PrintStream(m_oMainSettings.getIndexFile());
            oMedias = new MediaHandler(oCD);
            oMedias.SerializeMediaList(oPrinter);
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
        MediaHandler oMedias;
        boolean blnSaveIndexFile = false;
        try {
            String sIndexFile = m_oMainSettings.getIndexFile();
            if (sIndexFile  != null) {
                if (new File(m_oMainSettings.getIndexFile()).exists()) {
                    if (DebugVerbose > 1)
                        System.err.println("Reading file index");
                    oMedias = new MediaHandler(oCD, sIndexFile);
                } else {
                    if (DebugVerbose > 1)
                        System.err.println("Indexing file");
        
                    oMedias = new MediaHandler(oCD);
                    blnSaveIndexFile = true;
                }
            } else {
                if (DebugVerbose > 1)
                    System.err.println("Indexing file");
        
                oMedias = new MediaHandler(oCD);
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
                oMedias.SerializeMediaList(oPrinter);
            } catch (IOException ex) {
                if (DebugVerbose > 2)
                    ex.printStackTrace();
                else
                    System.err.println(ex.getMessage());
                return -1;
            }
        }

        // decode the desired media item(s)
        try {
            
            if (m_oMainSettings.getMainCommandType() == Settings.DECODE_ALL) {
                
                for (PSXMedia oMedia : oMedias) {
                    System.err.println(oMedia.toString());
                    DecodeMediaItem(oMedia);
                } // for
            
            } else {

                int iIndex = m_oMainSettings.getItemToDecode();
                if (oMedias.hasIndex(iIndex)) {
                    DecodeMediaItem(oMedias.getByIndex(iIndex));
                } else {
                    System.err.println("Sorry, couldn't find media item " 
                            + iIndex);
                }

            }
        
            if (DebugVerbose > 1)
                System.err.println("Media decoding complete.");
        
        } catch (IOException ex) {
            ex.printStackTrace();
        }
            
        return 0;
    }
    
    private static void DecodeMediaItem(PSXMedia oMedia) throws IOException {
        /*
        String sNameEnd = "";
        if ((oMedia.getMediaType() & PSXMedia.MEDIA_TYPE_AUDIO) > 0)
            sNameEnd = "";
        else if ((oMedia.getMediaType() & PSXMedia.MEDIA_TYPE_IMAGE) > 0)
            sNameEnd = "_p%d";
        else if ((oMedia.getMediaType() & PSXMedia.MEDIA_TYPE_VIDEO) > 0)
            sNameEnd = "_f%04d";
        else if ((oMedia.getMediaType() & PSXMedia.MEDIA_TYPE_XA) > 0)
            sNameEnd = "_c%02d";
        */
        
        final String sFinalName = 
                String.format("%s%03d", 
                    m_oMainSettings.getOutputFile(),
                    oMedia.getIndex());
        
        oMedia.setCallback(new IProgressCallback.IProgressCallbackEventError() {

            public boolean ProgressCallback(String sEvent) {
                System.err.println(sEvent);
                return true;
            }

            public boolean ProgressCallback(String sWhatDoing, double dblPercentComplete) {
                System.err.println(sWhatDoing);
                return true;
            }

            public void ProgressCallback(Exception e) {
                e.printStackTrace(System.err);
            }
        });
        
        if (oMedia.hasAudio() && m_oMainSettings.DecodeAudio()) {
            oMedia.DecodeAudio(sFinalName, "wav", m_oMainSettings.getAudioScale());
        }
        
        if (oMedia.hasVideo() && m_oMainSettings.DecodeVideo()) {
            Integer oiStart = null, oiEnd = null;
            if (m_oMainSettings.getStartFrame() != -1)
                oiStart = new Integer(m_oMainSettings.getStartFrame());
            if (m_oMainSettings.getEndFrame() != -1)
                oiEnd = new Integer(m_oMainSettings.getEndFrame());
            
            oMedia.DecodeVideo(sFinalName, m_oMainSettings.getOutputFormat(),
                                oiStart, oiEnd);
        }
        
        if (oMedia.hasXAChannels() && m_oMainSettings.DecodeAudio()) {
            Integer oiChannel = null;
            if (m_oMainSettings.getChannel() != -1)
                oiChannel = new Integer(m_oMainSettings.getChannel());
                    
            oMedia.DecodeXA(sFinalName, "wav", m_oMainSettings.getAudioScale(), oiChannel);
        }
        
        if (oMedia.hasImage()) {
            oMedia.DecodeImage(sFinalName, m_oMainSettings.getOutputFormat());
        }
        
        oMedia.setCallback(null);

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
            if (dec.hasAudio()) {
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
