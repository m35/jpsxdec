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
 * 
 * GAME SPECIFIC:
 * - Soul Reaver has some 'almost' TIM files. Test these with PsxMC
 * - Soul Reaver 009:TIM is extra wide and saved as jpg looks weird
 * - FF Chronicles has a tim file being saved as blank
 * - That FF7 gold movie has audio and video displacement like no other
 * - Very large audio pop at beginning of jumpingflash2_movie-01.str. Test with other decoders
 * - FF Chron:Chrono Trigger movies are encoded choppy?? Test with other decoders.
 * - Pop at end of 429:XA disc 1. Test with PsxMC
 * - Legend of Mana has 000:TIM with 64 palettes! One being saved all transparent?
 * 
 * - Linux: 
        VLC claims avi files are broken. Always shows even after repair
        mplayer plays MJPG stretched virt and crashes with DIB. 
                Plays vid upside-down + horz stretched after VLC repair
        totem shows some messy green stuff in MJPG
 * - Linux (Compiz): Progress gui often appears blank, so there's no way to close it
 * ? Make avi saving more robust by writing as much of the header as possible
 *   and updating the frame count, file size, etc in the headers as frames as written,
 *   and maybe even writing the index after each frame, then backing up and
 *   overwriting it with the next frame and then the index again (could really
 *   slow down the writing, maybe provide this as an option?)
 * /- Pull the uncompressing and MDEC decoding out of the PSXMedia and put it in
 *   the savers/players.
 * - Organize 2.5 uncompressors: 1) Current straight-forward one
 *                               2) The optimized if-bit-tree one
 *                              .5) The optimized if-bit-tree that immediately 
 *                                  pumps it into MDEC decoding.
 *   and 2.5 MDEC: 1) Current high-quality MDEC that can use high-quality slow as hell IDCT
 *                 2) Current high-quality MDEC that can use high-quality Stephen's IDCT
 *                .5) Combined ultra-fast IDCT with (.5) Uncompressor
 *   And finally create a native lib that will also perform the (.5) case like a bat outta hell
 * 
 * - Create a "player" (like a saver) that watches how much time has elapsed
 *   and skips decoding frames as necessary.
 * - Create a separate class for demux and mdec 'image' saving 
 * 
 * - Add abstract protected function "IdentifySector()" in MediaStreaming
 *   to more quickly figure out what type of sectors are read (since it won't
 *   have to go through the whole list of like 10 types).
 * 
 * - inform user if corrupted data is found while reading sectors
 * - unify the audio/video format handling so i'm not just passing strings around
 * - Re-add storing the saving folder to ini file
 * - Figure out why jlist has empty entries at the end
 * - Add some checking during index file load to stop a 600mb scan of death
 * - change TIM serialization to include how many palettes it has
 * - change gui to use JFileChooser
 * - clean up main
 * // add tim decoding gui
 * ! unify Settings and SaveOpions classes
 * ! add the more thorough searching for TIM files to PSXMedia. do it like
 *   XA searching with a static function in PSXMediaTIM
 * ! Progress gui- when fatal exception occurs, throw the stack trace into the text box
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
 * - visually populate the list as the disc/file is being indexed
 * - use ISO9660 to help name media items
 * - add MediaVideo.DecodeFrame for quick thumbnailing
 * - add best guess for Audio2048 properties so fps calc has a chance to figure it out
 * 
 * - pull yuv writing to its own class, a YuvWriter, and let it accept
 *   PsxYuv images for each frame's input.
 * - Change PsxYuv.write(OutputStream) to only write the header if requested,
 *   so multiple images can be written to the same stream
 * - fix output of YUV images to adjust for yuv4mpeg2 color space
 * 
 * /- change Settings class to use a tiered architecture, remvoing
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
 * // All InputStreams: implement read(byte[]) methods for hopefully faster reading.
 * - audio decoding: Str audio write directly to byte array and use ByteArrayInputStream
 *                   Square audio write directy to byte array and use a Byte2DArrayInputStream
 *                   or write to an InterleavedShort2DArrayOutputStream
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
 * /- add raw CD reading for windows
 * /- add encoding to a video format
 * 
 */

package jpsxdec;


import jpsxdec.cdreaders.CDXASector;
import jpsxdec.media.IProgressListener;
import java.io.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import jpsxdec.cdreaders.CDXAIterator;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.iso9660.ISOFile;
import jpsxdec.cdreaders.iso9660.PathTableBE;
import jpsxdec.cdreaders.iso9660.TestISO;
import jpsxdec.cdreaders.iso9660.VolumePrimaryDescriptor;
import jpsxdec.mdec.MDEC;
import jpsxdec.demuxers.*;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.media.*;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.media.savers.*;
import jpsxdec.media.savers.Formats.ImgSeqVidFormat;
import jpsxdec.plugins.*;
import jpsxdec.sectortypes.*;
import jpsxdec.uncompressors.StrFrameUncompressor;
import jpsxdec.util.*;

public class Main {
    
    public static int DebugVerbose = 2;
    public final static String Version = "0.34(beta)";
    public final static String VerString = "jPSXdec: PSX media decoder, v" + Version;
    private static Settings m_oMainSettings;

    private static void timsearch() {
        try {
            CDSectorReader oCD = CDSectorReader.Open("..\\..\\disc1.iso");
            
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
        
        //timsearch(); // test
        
        m_oMainSettings = new Settings(args);

        if (m_oMainSettings.gotDebug()) {
            // set verbosity
            int[] aiVerbosityLevels = m_oMainSettings.getDebug();
            Main.DebugVerbose = aiVerbosityLevels[0];
            MediaHandler.DebugVerbose = aiVerbosityLevels[1];
            PSXMedia.DebugVerbose = aiVerbosityLevels[2];
            StrFrameUncompressor.DebugVerbose = aiVerbosityLevels[3];
            MDEC.DebugVerbose = aiVerbosityLevels[4];
        }
        
        if (m_oMainSettings.getMainCommand() == Settings.MAIN_CMD_STARTGUI) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    new Gui().setVisible(true);
                }
            });

            return;
        }
        
        
                
        if (DebugVerbose > 0)
            System.err.println(VerString);
        
        switch (m_oMainSettings.getMainCommand()) {
            case Settings.MAIN_CMD_INDEX:
                System.exit(IndexOnly());
                break;
            case Settings.MAIN_CMD_DECODE:
                System.exit(NormalDecode());
                break;
            case Settings.MAIN_CMD_SPECIALFILE:
                System.exit(DecodeSpecialFrameFile());
                break;
            case Settings.MAIN_CMD_SECTORDUMP:
                System.exit(SectorList());
                break;
            case Settings.MAIN_CMD_PLUGIN:
                System.exit(Plugin());
                break;
            case Settings.MAIN_CMD_DIR:
            case Settings.MAIN_CMD_COPY:
                System.exit(DirCopy());
                break;
        }
        
        return;
    }
    
    //--------------------------------------------------------------------------
    
    private static int DirCopy() {
        //open input file
        CDSectorReader oCD;
        try {
            oCD = CDSectorReader.Open(m_oMainSettings.getInputFile());
        
            if (!oCD.HasSectorHeader()) {
                if (DebugVerbose > 0) {
                    System.err.println("Copying a CD type file without a header seems kinda useless?");
                }
                return -1;
            }

            VolumePrimaryDescriptor vpd = 
                    new VolumePrimaryDescriptor(oCD.getSector(16).getSectorDataStream());
            
            ArrayList<ISOFile> oFileList = new ArrayList<ISOFile>();

            try {
                TestISO.getFileList(vpd.root_directory_record, oCD, oFileList, new File(""));
            } catch (IndexOutOfBoundsException ex) {
                // try to read as much of the dir as is available
            }
            
            ISOFile fileToCopy = null;
            
            for (ISOFile ofile : oFileList) {
                if (m_oMainSettings.getMainCommand() == Settings.MAIN_CMD_DIR) {
                    System.out.println(ofile.getPath() 
                            + " " + ofile.getStartSector() + "-" + 
                            ofile.getEndSector());
                } else if (m_oMainSettings.getMainCommand() == Settings.MAIN_CMD_COPY) {
                    if (ofile.getPath().equals(m_oMainSettings.getFileToCopy())) {
                        fileToCopy = ofile;
                    }
                }
            }
            
            if (m_oMainSettings.getMainCommand() == Settings.MAIN_CMD_DIR)
                return 0;
            
            if (fileToCopy != null)
                System.out.println("Going to copy " + fileToCopy.toString());
            else {
                System.out.println("File not found.");
                return -1;
            }
            
            CDXAIterator fileRange = new CDXAIterator(oCD, (int)fileToCopy.getStartSector(), (int)fileToCopy.getEndSector());
            
            FileOutputStream fos = new FileOutputStream(
                    m_oMainSettings.constructOutPath(fileToCopy.getName()));
            
            while (fileRange.hasNext()) {
                CDXASector oSect = fileRange.next();
                if (oSect == null) {
                    //todo err
                } else {
                    fos.write(oSect.getRawSectorData());
                }
            }
            
            fos.close();
            
            return 0;
            
        } catch (NotThisTypeException ex) {
            if (DebugVerbose > 0)
                System.err.println("CD/file does not have directory information.");
            return -1;
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else if (DebugVerbose > 0)
                System.err.println(ex.getMessage());
            return -1;
        }
    }
    
    private static int Plugin() {
        
        String sOutFileBase = "";
        String sOutFolder = "";
        if (m_oMainSettings.gotOutputFile()) {
            sOutFileBase = m_oMainSettings.getOutputFile();
        }
        if (m_oMainSettings.gotOutputFolder()) {
            sOutFolder = m_oMainSettings.getOutputFolder();
        }
        
        String sPath = new File(sOutFolder, sOutFileBase).getAbsolutePath();
        
        if (m_oMainSettings.getPluginCommand().equals("lapks"))
            System.exit(Lain_LAPKS.DecodeLAPKS(m_oMainSettings.getInputFile(), sPath));
        else if (m_oMainSettings.getPluginCommand().equals("site"))
            System.exit(Lain_SITE.DecodeSITE(m_oMainSettings.getInputFile(), sPath));
        
        return 0;
    }
    
    private static int IndexOnly() {
        //open input file
        CDSectorReader oCD;
        try {
            oCD = CDSectorReader.Open(m_oMainSettings.getInputFile());
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
            oMedias = new MediaHandler(oCD, new IProgressListener.IProgressEventListener() {
                public boolean ProgressUpdate(String sWhatDoing, double dblPercentComplete) {
                    return true;
                }

                public boolean ProgressUpdate(String sEvent) {
                    if (DebugVerbose > 0)
                        System.err.println(sEvent);
                    return false;
                }
            });
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
            oCD = CDSectorReader.Open(m_oMainSettings.getInputFile());
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
        MediaHandler oHandler;
        boolean blnSaveIndexFile = false;
        try {
            String sIndexFile = m_oMainSettings.getIndexFile();
            if (sIndexFile  != null) {
                if (new File(m_oMainSettings.getIndexFile()).exists()) {
                    if (DebugVerbose > 1)
                        System.err.println("Reading file index");
                    oHandler = new MediaHandler(oCD, sIndexFile);
                } else {
                    if (DebugVerbose > 1)
                        System.err.println("Indexing file");
        
                    oHandler = new MediaHandler(oCD);
                    blnSaveIndexFile = true;
                }
            } else {
                if (DebugVerbose > 1)
                    System.err.println("Indexing file");
        
                oHandler = new MediaHandler(oCD);
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
                oHandler.SerializeMediaList(oPrinter);
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
            for (PSXMedia oMedia : oHandler) {
                System.err.println(oMedia.toString());
            }
        }

        // decode the desired media item(s)
        try {
            
            if (m_oMainSettings.getDecodeIndex() == Settings.DECODE_ALL) {
                
                for (PSXMedia oMedia : oHandler) {
                    System.err.println(oMedia.toString());
                    DecodeMediaItem(oMedia);
                } // for
            
            } else {

                int iIndex = m_oMainSettings.getDecodeIndex();
                if (oHandler.hasIndex(iIndex)) {
                    PSXMedia oMedia = oHandler.getByIndex(iIndex);
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

        SavingOptions oOptions = new SavingOptions(oMedia);
        oOptions.setDecodeVideo(m_oMainSettings.decodeVideo());
        oOptions.setDecodeAudio(m_oMainSettings.decodeAudio());
        
        if (m_oMainSettings.gotOutputFile()) {
        
            oOptions.setVideoFilenameBase(m_oMainSettings.getOutputFile());
            oOptions.setAudioFilenameBase(m_oMainSettings.getOutputFile());
        }
        if (m_oMainSettings.gotOutputFolder()) 
            oOptions.setFolder(new File(m_oMainSettings.getOutputFolder()));
        
        if (m_oMainSettings.gotVidFormat())
            oOptions.setVideoFormat(m_oMainSettings.getVidFormat());
        
        if (m_oMainSettings.gotAudFormat())
            oOptions.setAudioFormat(m_oMainSettings.getAudFormat());
        
        if (m_oMainSettings.gotFrames()) {
            oOptions.setStartFrame(m_oMainSettings.getStartFrame());
            oOptions.setEndFrame(m_oMainSettings.getEndFrame());
        }
        
        if (m_oMainSettings.gotFps())
            oOptions.setFps(m_oMainSettings.getFps());
        
        if (m_oMainSettings.gotJpgQuality())
            oOptions.setJpegQuality(m_oMainSettings.getJpgQuality() / 100.0f);

        oOptions.setDoNotCrop(m_oMainSettings.noCrop());
        
        SaverFactory.DecodeStreaming(oOptions, oListener);
        
    }
    
    private static void DecodeTIMMedia(PSXMediaTIM oMedia) throws IOException {
        String sBaseName;
        
        if (m_oMainSettings.gotOutputFile())
            sBaseName = String.format("%s[%d]",
                    m_oMainSettings.getOutputFile(), oMedia.getIndex());
        else
            sBaseName = oMedia.getSuggestedName();
        
        if (m_oMainSettings.gotOutputFolder())
            sBaseName = new File(m_oMainSettings.getOutputFolder(), sBaseName).toString();
        
        Tim oTim = oMedia.getTIM();
        // make sure output images format is ok
        Formats.ImgSeqVidFormat oFmt;
        if (m_oMainSettings.gotTimFormat())
            oFmt = m_oMainSettings.getTimFormat();
        else if (m_oMainSettings.getVidFormat() instanceof Formats.ImgSeqVidFormat) {
            oFmt = (ImgSeqVidFormat) m_oMainSettings.getVidFormat();
        } else {
            oFmt = Formats.PNG_IMG_SEQ;
        }
        
        for (int i = 0; i < oTim.getPaletteCount(); i++) {
            BufferedImage bi = oTim.toBufferedImage(i);
            ImageIO.write(bi, oFmt.getId(), 
                    new File(String.format(
                    "%s_p%02d.%s",
                    sBaseName, i, oFmt.getExt())));
        }
    }
    
    //--------------------------------------------------------------------------

    private static int DecodeSpecialFrameFile() {
        
        Formats.Format oOutFormat;
        
        /*
         * Valid output formats:
         *  <java image formats>
         *  if demux, then mdec is ok
         *  yuv
         * 
         */
        
        if (m_oMainSettings.gotVidFormat()) {
            oOutFormat = m_oMainSettings.getVidFormat();
            
            if (oOutFormat == Formats.DEMUX) { // demux not allowed
                oOutFormat = Formats.PNG_IMG_SEQ;
            } else if (oOutFormat.equals("mdec")) { // mdec only if input is demux
                if (m_oMainSettings.getSpecialInType().equals("mdec")) 
                    oOutFormat = Formats.PNG_IMG_SEQ;
            } else if (!Formats.getAllJavaImgFormats().contains(oOutFormat)) {
                // otherwise it needs to be one of the valid output formats
                oOutFormat = Formats.PNG_IMG_SEQ;
            }
            
        } else {
            oOutFormat = Formats.PNG_IMG_SEQ;
        }
            
        String sOutFile = m_oMainSettings.constructOutPath(m_oMainSettings.getInputFileBase());
            
        try {
            
            if (DebugVerbose > 0)
                System.err.println("Reading frame file");
            
            final FileInputStream is;
            is = new FileInputStream(m_oMainSettings.getInputFile());
            
            InputStream iis = new IO.InputStreamWithFP(is);
            
            DecodeSpecialFile(
                    sOutFile + "." + oOutFormat.getExt(),
                    m_oMainSettings.getSpecialInType(),
                    oOutFormat.getId(),
                    iis, 
                    m_oMainSettings.getWidth(), 
                    m_oMainSettings.getHeight());
            
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
            oCD = CDSectorReader.Open(m_oMainSettings.getInputFile());
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
            if (m_oMainSettings.getSectorDumpFile().equals("-"))
                ps = System.out;
            else
                ps = new PrintStream(m_oMainSettings.getSectorDumpFile());
            
            PSXSectorRangeIterator oIter = new PSXSectorRangeIterator(oCD);
            while (oIter.hasNext()) {
                PSXSector oSect = oIter.next();
                if (oSect != null) {
                    String s = oSect.toString();
                    if (s != null) ps.println(s);
                } else {
                    ps.println("Error with sector " + (oIter.getIndex() - 1));
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

    private static void DecodeSpecialFile(
            String sFrameFile,
            String sInputFormat, 
            String sOutputFormat, 
            InputStream str, 
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

            
            str = new StrFrameUncompressor(str, lngWidth, lngHeight).getStream();
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
        oYuv = MDEC.getQualityMdec().DecodeFrame(str, lngWidth, lngHeight);
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
