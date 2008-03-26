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
 * - That FF7 gold movie has audio and video displacement like no other
 * - Very large audio pop at beginning of jumpingflash2_movie-01.str
 * - change TIM serialization to include how many palettes it has
 * - change gui to use JFileChooser
 * - clean up main
 * - add tim decoding gui
 * ! unify Settings and SaveOpions classes
 * ? make playback listeners more robust by allowing any number of listeners
 * - add volume option to gui
 * - make IDCT easier to interchange
 * - test test test
 * - in progress gui, setup a timer to only update the display at most every second 
 * - combine IProgressListener and Progress gui interfaces so there is only one
 * - Change IndexLineParser to just use regex for parsing
 * 
 * - change media list to be a tree list showing all the files on the disc
 *   and what media belongs to what file. if not a disc image, just show
 *   the file name and the media it contains
 * - use ISO9660 to help name media items
 * - add MediaVideo.DecodeFrame for quick thumbnailing
 * - add best guess for Audio2048 properties so fps calc has a chance to figure it out
 * ? Create an AVIStreamWriter that generates AVI linerally. Need to pass all
 *   necessary properties on creation so the headers can be written. Size of
 *   file must be pre-calculated, along with size of all chunks of
 *   media, so DIB output is the only option.
 * 
 * - pull yuv writing to its own class, a YuvWriter, and let it accept
 *   PsxYuv images for each frame's input.
 * - Change PsxYuv.write(OutputStream) to only write the header if requested,
 *   so multiple images can be written to the same stream
 * - change output of YUV images to adjust for yuv4mpeg2 format 
 * 
 * - add the more thorough searching for TIM files to PSXMedia
 * - change Settings class to use a tiered architecture, remvoing
 *   elements from the array as we find matching commands and passing the
 *   rest furthur down the stream
 * 
 * // Finish encoder gui.
 * // Find if XA is interleaved in Saiyuki
 * // Test the new games
 * // Figure out chrono cross audio format
 * // Add jpeg quality settings to AviWriter MJPG output
 * // Demuxer: Use ArrayList to hold sectors
 * // rename .0rlc to .mdec
 * // add TIM writing
 * // Change architechture to use Push. Unify the interface for
 *   movie, xa, and image. Create listener interface for each
 * // Rearrage CDXASector so submode is its own class, and referring it can be
 *   via sector.getSubmode.getByte(), sector.getSubmode.getEofMarker(), etc
 * // Probably get rid of VideoFrameConverter instance stuff, and move the
 *   static stuff into PSXMedia.java
 * // Perhaps put the ADPCMdecoder code into StrAudioDemuxerDecoder and FF8AudioDemuxerDecoder
 * // Create entire separate classes for 'play' decoding to the different major types of media
 *
 * OPTIMIZE:
 * * All InputStreams: implement read(byte[]) methods for hopefully faster reading.
 * - audio decoding: Str audio write directly to byte array and use ByteArrayInputStream
 *                   Square audio write directy to byte array and use a Byte2DArrayInputStream
 *                   or write to a InterleavedShort2DArrayOutputStream
 * ? don't use streams to read headers, just pass the original byte array and read values out of it
 * 
 * /- finish STR format documentation
 * - make code documentation
 * - make manual
 * /- make CREDITS file
 * /- CLEANUP!!
 * // better organize the error reporting/checking
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
 * ? get rid of mdec & yuv listener. the demux istener must handle the rest of the decoding.
 * ? Consider making PSXSector just a subclass of CDXASector
 *
 * FUTURE VERSIONS:
 * // Add frame rate calculation
 * - add better stdin/stdout file handling
 * //- add FF9 audio decoding
 * /- add GUI
 * - add raw CD reading
 * /- add encoding to a video format
 * 
 */

package jpsxdec;


import java.io.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import jpsxdec.audiodecoding.StrAudioPullDemuxerDecoderIS;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.mdec.MDEC;
import jpsxdec.demuxers.*;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.media.*;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.media.savers.*;
import jpsxdec.plugins.*;
import jpsxdec.sectortypes.*;
import jpsxdec.uncompressors.StrFrameUncompressorIS;
import jpsxdec.util.*;

public class Main {
    
    public static int DebugVerbose = 2;
    public final static String Version = "0.33(beta)";
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
                    if (oIter.peekNext() instanceof PSXSectorUnknownData)
                        break;
                    else
                        oIter.skipNext();
                }
                if (!oIter.hasNext()) break;

                try {
                    UnknownDataPullDemuxerIS oStrm = new UnknownDataPullDemuxerIS(oIter);
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
        
        /* <-- Commenting this will enable the super fast, but low quality IDCT
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
        //StrFramePullDemuxerIS.DebugVerbose = aiVerbosityLevels[2];
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
        
        // print the index for confirmation
        if (DebugVerbose > 0) {
            for (PSXMedia oMedia : oMedias) {
                System.err.println(oMedia.toString());
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
                    PSXMedia oMedia = oMedias.getByIndex(iIndex);
                    System.err.println(oMedia.toString());
                    DecodeMediaItem(oMedia);
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
    
    
/**<pre>
 *                 video             xa        tim
 *              +---------------|----------|--------|
 *              | +a+v: avi     |          |        |
 *          avi | -a+v: avi     |          |        |
 *              | +a-v: wav     | +a: wav  |        |
 *              |---------------| -a: n/a  |        |
 * img sequence | +a+v: img+wav |          |        |
 *         mdec | -a+v: img     |          | image  |
 *        demux | +a-v: wav     |          |        |
 *              |--------------------------|        |
 *              | +a+v: raw                |        |
 *         raw  | -a+v: raw                |        |
 *    (str, xa) | +a-v: raw                |        |
 *              +-----------------------------------|
 * 
 * if input is tim (not streaming)
 *  ... output image
 * else if (streaming & ) output is raw
 *  ... output raw
 * else if input is xa
 *  ... if decode aud, output wav else do nothing
 * else (if input is video)
 *  ... if decode vid && has vid
 *  ...  ... if avi
 *  ...  ... else (image sequence)
 *  ... else if decode audio
 *  ...  ... 
 *  ... else do nothing
 *</pre>*/
    private static void DecodeMediaItem(PSXMedia oMedia) throws IOException {

        if (oMedia instanceof PSXMediaStreaming) {
            DecodeStreaming((PSXMediaStreaming)oMedia);
        } else if (oMedia instanceof PSXMediaTIM) {
            DecodeTIMMedia((PSXMediaTIM)oMedia);
        }
        
    }
    
    private static void DecodeStreaming(PSXMediaStreaming oMedia) throws IOException {
        final String sFinalName = 
                String.format("%s%03d", 
                    m_oMainSettings.getOutputFile(),
                    oMedia.getIndex());
        
        oMedia.Reset();
        
        IProgressListener oListener = new IProgressListener.IProgressEventErrorListener() {
            public void ProgressUpdate(Exception e) {
                e.printStackTrace(System.err);
            }
            public boolean ProgressUpdate(String sWhatDoing, double dblPercentComplete) {
                System.err.println(sWhatDoing);
                return false;
            }
            public boolean ProgressUpdate(String sEvent) {
                System.err.println(sEvent);
                return false;
            }
        };

        if (m_oMainSettings.getOutputFormat().equals("xa") || 
            m_oMainSettings.getOutputFormat().equals("str"))
        {
            RawSaver oRawSaver = new RawSaver(oMedia, 
                new File(sFinalName + "." + m_oMainSettings.getOutputFormat()));
            oRawSaver.addProgressListener(oListener);
            try {
                oMedia.Play();
            } finally {
                oRawSaver.done();
            }
            
        } else if (oMedia instanceof PSXMediaStreaming) {
            long lngStartTime = System.currentTimeMillis();
            
            DecodeStreamingVideo((PSXMediaStreaming)oMedia, sFinalName, oListener);
            
            long lngEndTime = System.currentTimeMillis();
            
            long lngFrames = ((PSXMediaStreaming)oMedia).getEndFrame() - ((PSXMediaStreaming)oMedia).getStartFrame() + 1;
            
            System.err.println(String.format("%1.3f frames per second.",
                    (double)lngFrames / (lngEndTime - lngStartTime) * 1000
                    ));
            
        }        
    }
    
    public static void DecodeStreamingVideo(
                        PSXMediaStreaming oMedia, 
                        String sFinalName, 
                        IProgressListener oListener) 
            throws IOException 
    {
/*
        oMedia.Reset();
        
        if (m_oMainSettings.DecodeVideo() && oMedia.hasVideo()) {
        
            if (m_oMainSettings.getOutputFormat().equals("avi") || 
                m_oMainSettings.getOutputFormat().equals("avi-mjpg") )
            {
                if (m_oMainSettings.getStartFrame() != -1)
                    oMedia.seek(m_oMainSettings.getStartFrame());

                AviSaver oAviSaver;
                oAviSaver = new AviSaver(oMedia, 
                        sFinalName+ ".avi", 
                        m_oMainSettings.getOutputFormat().equals("avi-mjpg"), 
                        m_oMainSettings.getEndFrame(),
                        m_oMainSettings.DecodeAudio());
                
                oAviSaver.addProgressListener(oListener);
                try {
                    oMedia.Play();
                    oAviSaver.done();
                } finally {
                }

                if (oAviSaver.getException() != null) throw oAviSaver.getException();

            } 
            else 
            { // image sequence

                if (m_oMainSettings.getStartFrame() != -1)
                    oMedia.seek(m_oMainSettings.getStartFrame());

                ImageSequenceSaver oImageSaver = 
                        new ImageSequenceSaver(oMedia, 
                        oMedia.getPossibleFPS()[0],
                        sFinalName, 
                        m_oMainSettings.getOutputFormat(), 
                        m_oMainSettings.getEndFrame(),
                        -1);

                oImageSaver.addProgressListener(oListener);
                try {
                    oMedia.Play();
                } finally {
                    oImageSaver.done();
                }            
                if (oImageSaver.getException() != null) throw oImageSaver.getException();

            } 
        }
        else if (m_oMainSettings.DecodeAudio()) 
        {
            
            AudioSaver oAudioSaver = 
                    new AudioSaver(oMedia, 
                    sFinalName);
            
            oAudioSaver.addProgressListener(oListener);
            try {
                oMedia.Play();
            } finally {
                oAudioSaver.done();
            }            
            if (oAudioSaver.getException() != null) throw oAudioSaver.getException();
        } else {
            System.err.println("There's nothing to decode with the parameters provided.");
        }*/
    }
    
    
    private static void DecodeTIMMedia(PSXMediaTIM oMedia) throws IOException {
        final String sFinalName = 
            String.format("%s%03d", 
                m_oMainSettings.getOutputFile(),
                oMedia.getIndex());
            
        Tim oTim = oMedia.getTIM();
        // make sure output images format is ok
        Vector<String> oFrmts = Misc.GetJavaImageFormats();
        String sFormat = m_oMainSettings.getOutputFormat();
        if (!oFrmts.contains(sFormat)) {
            sFormat = "png";
        }
        
        for (int i = 0; i < oTim.getPaletteCount(); i++) {
            BufferedImage bi = oTim.toBufferedImage(i);
            ImageIO.write(bi, sFormat, 
                    new File(String.format(
                    "%s_p%02d.%s",
                    sFinalName, i, sFormat)));
        }
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
            StrFramePullDemuxerIS str = new StrFramePullDemuxerIS(oIter);
        
            String sFrameFile = String.format(
                        "%s.%s",
                        m_oMainSettings.getOutputFile(), 
                        m_oMainSettings.getOutputFormat());

            System.err.println("Reading frame sectors");
            DecodeAndSaveFrame(
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
            StrAudioPullDemuxerDecoderIS dec = new StrAudioPullDemuxerDecoderIS(oIter);
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
            
            final FileInputStream is;
            is = new FileInputStream(m_oMainSettings.getInputFile());
            
            InputStream iis = new IO.InputStreamWithFP(is);
            
            DecodeAndSaveFrame(
                    m_oMainSettings.getInputFileFormat(), 
                    m_oMainSettings.getOutputFormat(),
                    iis, sFrameFile, 
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

    private static void DecodeAndSaveFrame(String sInputFormat, 
                                          String sOutputFormat,
                                          InputStream str, 
                                          String sFrameFile,
                                          long lngWidth,
                                          long lngHeight) 
            throws IOException 
    {
        
        if (str instanceof IWidthHeight) {
            lngWidth = ((IWidthHeight)str).getWidth();
            lngHeight = ((IWidthHeight)str).getHeight();
        }
        
        if (sInputFormat.equals("demux")) {

            if (sOutputFormat.equals("demux")) {
                FileOutputStream fos = new FileOutputStream(sFrameFile);
                int ib;
                while ((ib = str.read()) >= 0)
                    fos.write(ib);
                fos.close();
                return;
            }

            
            str = new StrFrameUncompressorIS(str, lngWidth, lngHeight).getStream();
            if (sOutputFormat.equals("mdec")) {
                FileOutputStream fos = new FileOutputStream(sFrameFile);
                int ib;
                while ((ib = str.read()) >= 0)
                    fos.write(ib);
                fos.close();
                return;
            }

        }
        
        PsxYuv oYuv;
        oYuv = MDEC.DecodeFrame(str, lngWidth, lngHeight);
        if (sOutputFormat.equals("yuv") || sOutputFormat.equals("y4m")) {
            FileOutputStream fos = new FileOutputStream(sFrameFile);
            oYuv.Write(fos, new Fraction(15, 1));
            fos.close();
            return;
        }

        BufferedImage bi = oYuv.toBufferedImage();
        ImageIO.write(bi, sOutputFormat, new File(sFrameFile));
    }
        
}
