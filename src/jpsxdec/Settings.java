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
 * Settings.java
 *
 */

package jpsxdec;

import java.io.*;
import jpsxdec.util.CmdLineParser;
import jpsxdec.util.CmdLineParser.Option;
import java.util.*;
import javax.imageio.ImageIO;

public class Settings {
    
    private Settings() {};
    
    
    public final static String[] VALID_FORMATS = GetValidFormats();
    
    // the main commands
    public final static int DECODE_ONE = 1;
    public final static int DECODE_ALL = 2;
    public final static int DECODE_SECTORS_FRAME = 3;
    public final static int DECODE_SECTORS_AUDIO = 4;
    public final static int SECTOR_LIST = 5;
    public final static int SPECIAL_IN_FILE = 6;
    public final static int INDEX_ONLY = 7;
    
    /*------------------------------------------------------------------------*/
    /*-- Static Fields -------------------------------------------------------*/
    /*------------------------------------------------------------------------*/
    
    private static int m_iMainCommandType = -1;
    
    
    private static int m_iMediaItemToDecode = -1;
    
    private static boolean m_blnDecodeAudio = true;
    private static boolean m_blnDecodeVideo = true;
    
    private static String m_sIndexFile = null;
    
    private static int m_iStartFrame = -1;
    private static int m_iEndFrame = 99999999;
    
    private static int[] m_aiSectorList = null;
    
    private static double m_dblAudioScale = 1.0;
    private static int m_iChannel = -1;
    
    private static String m_sFormat = "png";
    
    private static int[] m_aiVerbosityLevels = new int[] {2, 2, 2, 2, 2};
    
    private static String m_sInputFileName = null;
    private static String m_sOutputFileName = null;
    
    private static String m_sSpecialInFileType = null;
    private static int m_iWidth = -1;
    private static int m_iHeight = -1;
    
    /*------------------------------------------------------------------------*/
    /*-- Static Properties ---------------------------------------------------*/
    /*------------------------------------------------------------------------*/
    
    public static int getItemToDecode() {
        return m_iMediaItemToDecode;
    }
    
    public static boolean DecodeAudio() {
        return m_blnDecodeAudio;
    }
    
    public static boolean DecodeVideo() {
        return m_blnDecodeVideo;
    }
    
    public static String getIndexFile() {
        return m_sIndexFile;
    }
    
    public static int getStartFrame() {
        return m_iStartFrame;
    }
    
    public static int getEndFrame() {
        return m_iEndFrame;
    }
    
    public static double getAudioScale() {
        return m_dblAudioScale;
    }
    
    public static int getChannel() {
        return m_iChannel;
    }
    
    public static String getOutputFormat() {
        return m_sFormat;
    }
    
    public static int[] getVerbosityLevels() {
        return m_aiVerbosityLevels;
    }
    
    public static String getInputFile() {
        return m_sInputFileName;
    }
    
    /** demux, or 0rlc */
    public static String getInputFileFormat() {
        return m_sSpecialInFileType;
    }
    
    public static String getOutputFile() {
        return m_sOutputFileName;
    }
    
    public static int[] getSectorList() {
        return m_aiSectorList;
    }
    
    static long getWidth() {
        return m_iWidth;
    }
    
    static long getHeight() {
        return m_iHeight;
    }
    
    static int getMainCommandType() {
        return m_iMainCommandType;
    }
    
    /*------------------------------------------------------------------------*/
    /*-- Output --------------------------------------------------------------*/
    /*------------------------------------------------------------------------*/
    
    private static void ExitWithError(String sError) {
        System.err.println("Error: " + sError);
        System.err.println("Try -? for help.");
        System.err.println();
        System.exit(2);
    }
    
    private static void PrintUsage() {
        String[] sUseMsg = {
        Main.VerString,
        "",
        "Usage:",
        "  java -jar jpsxdec [options] <input-file> <output-file>",
        "",
        "Where:",
        "  <input-file>           Name of the input file",
        "  <output-file>          Name of the output file",
        "",
        "  -d/--decode #          Decode the media item #",
        "  -a/--decode-all        Decode all media items",
        "  --decode-frame <list>  Decode list of sectors into a frame",
        "  --decode-audio <list>  Decode list of sectors into audio",
        "  --sector-list          Dumps list of sector types",
        "  -?/-h/--help           Display this message",
        "  --in-file <type>       Decode special input file type: demux, 0rlc",
        "  -i/--index <file>      Name of the index file to use/create",
        "",
        "  --noaudio              Don't decode audio",
        "  --onlyaudio            Don't decode video",
        "  -f/--frame #           Only decode frame #",
        "  -s/--frames #-#        Decode frames from # to #",
        "  -c/--channel #         Only decode audio channel #",
        "  -o/--format <format>   Output format",
        "                         " + Join(VALID_FORMATS, ", "),
        "  --width #              Used with --in-file",
        "  --height #             Used with --in-file",
        "  --verbose #,#,#,#,#    Verbosity output levels (debug)",
        ""
        };
        
        for (String s : sUseMsg) {
            System.out.println(s);
        }
    }
    
    /*------------------------------------------------------------------------*/
    /*-- Main function -------------------------------------------------------*/
    /*------------------------------------------------------------------------*/
    
    /** Processes arguments. If there is an error, exits the program. */
    public static void ProcessArguments(String[] args) {
        
        CmdLineParser oParser = new CmdLineParser();
        
        // Main command types
        Option oblnMainHelp1 = oParser.addBooleanOption('?', "help");
        Option oblnMainHelp2 = oParser.addBooleanOption('h', "help");
        Option oiMainDecode1 = oParser.addIntegerOption('d', "decode");
        Option oblnMainDecodeAll = oParser.addBooleanOption('a', "decode-all");
        Option oblnMainSectorList = oParser.addBooleanOption("sector-list");
        Option osMainDecodeFrameSects = oParser.addStringOption("decode-frame");
        Option osMainDecodeAudioSects = oParser.addStringOption("decode-audio");
        Option osMainSpecialInFileType = oParser.addStringOption("in-file");
        
        // both main and optional command depending on main command type
        Option osMainOptIndexFile = oParser.addStringOption('i', "index");
        
        // optional commands depending on main command type
        Option oblnOptNoAudio = oParser.addBooleanOption("noaudio");
        Option oblnOptOnlyAudio = oParser.addBooleanOption("onlyaudio");
        Option oiOptFrameNum = oParser.addIntegerOption('f', "frame");
        Option osOptFrameRange = oParser.addStringOption('s', "frames");
        Option oiOptChannel = oParser.addIntegerOption('c', "channel");
        Option odblOptVolScale = oParser.addDoubleOption("vol-scale");
        Option osOptFrameFormat = oParser.addStringOption('o', "format");
        
        // special required options for in-file option
        Option oiSpecialWidth = oParser.addIntegerOption("width");
        Option oiSpecialHeight = oParser.addIntegerOption("height");
        
        // Always an option
        Option oiOptVerbosity = oParser.addStringOption("verbose");
        
        try {
            oParser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            ExitWithError(e.getMessage());
        }
        
        //----------------------------------------------------------------------
        
        Object oOpt;
        
        //..............................................................
        // Check main command types
        
        // help
        if (oParser.getOptionValue(oblnMainHelp1) != null || 
            oParser.getOptionValue(oblnMainHelp2) != null) 
        {
            // just wanting help
            PrintUsage();
            System.exit(0);
        }
        
        // Decode one media item
        oOpt = oParser.getOptionValue(oiMainDecode1);
        if (oOpt != null) {
            m_iMediaItemToDecode = (Integer)oOpt;
            if (m_iMediaItemToDecode < 0) ExitWithError("Negitive numbers are silly");
            m_iMainCommandType = DECODE_ONE;
        }
        
        // Decode all media items
        oOpt = oParser.getOptionValue(oblnMainDecodeAll);
        if (oOpt != null) {
            if (m_iMainCommandType > 0) ExitWithError("Too many main commands");
            m_iMainCommandType = DECODE_ALL;
        }
        
        // Sector list dump
        oOpt = oParser.getOptionValue(oblnMainSectorList);
        if (oOpt != null) {
            if (m_iMainCommandType > 0) ExitWithError("Too many main commands");
            m_iMainCommandType = SECTOR_LIST;
        }
        
        // Special input file type
        oOpt = oParser.getOptionValue(osMainSpecialInFileType);
        if (oOpt != null) {
            if (m_iMainCommandType > 0) ExitWithError("Too many main commands");
            m_iMainCommandType = SPECIAL_IN_FILE;
            m_sSpecialInFileType = (String)oOpt;
            
            oOpt = oParser.getOptionValue(oiSpecialWidth);
            if (oOpt == null) ExitWithError("Missing --width option");
            m_iWidth = (Integer)oOpt;
            if (m_iWidth < 0) ExitWithError("Negitive numbers are silly");
            
            oOpt = oParser.getOptionValue(oiSpecialHeight);
            if (oOpt == null) ExitWithError("Missing --height option");
            m_iHeight = (Integer)oOpt;
            if (m_iHeight < 0) ExitWithError("Negitive numbers are silly");
        }
        
        // Decode a list of sectors to a frame
        oOpt = oParser.getOptionValue(osMainDecodeFrameSects);
        if (oOpt != null) {
            if (m_iMainCommandType > 0) 
                ExitWithError("Too many main commands");
            m_aiSectorList = HandleSectorListOption((String)oOpt);
            if (m_aiSectorList == null) 
                ExitWithError("Invalid list of sectors or file not found");
            m_iMainCommandType = DECODE_SECTORS_FRAME;
        }
        
        // Decode a list of sectors to audio
        oOpt = oParser.getOptionValue(osMainDecodeAudioSects);
        if (oOpt != null) {
            if (m_iMainCommandType > 0) 
                ExitWithError("Too many main commands");
            m_aiSectorList = HandleSectorListOption((String)oOpt);
            if (m_aiSectorList == null) 
                ExitWithError("Invalid list of sectors or file not found");
            m_iMainCommandType = DECODE_SECTORS_AUDIO;
        }
        
        // Get index file if there is one
        m_sIndexFile = (String)oParser.getOptionValue(osMainOptIndexFile);
        
        // If no other main command options were provided, but
        // index file was provided, then index file becomes the main command
        int iAdditionalArgs = 2;
        if (m_iMainCommandType < 0 && m_sIndexFile != null) {
            m_iMainCommandType = INDEX_ONLY;
            iAdditionalArgs = 1;
        }
        
        // Make sure we got a main command
        if (m_iMainCommandType < 0)
            ExitWithError("Missing a main command");
        
        //..............................................................
        // Check remaining args
        
        String[] asRemainingArgs = oParser.getRemainingArgs();
        if (asRemainingArgs.length < iAdditionalArgs)
            ExitWithError("Too few additional arguments (should be "+iAdditionalArgs+").");
        else if (asRemainingArgs.length > iAdditionalArgs)
            ExitWithError("Too many additional arguments (should only be "+iAdditionalArgs+").");
        
        if (iAdditionalArgs > 0)
            m_sInputFileName = asRemainingArgs[0];
        // TODO:
        // output-file could be stdout, but only in the following cases:
        //   --decode # , --frame # and --noaudio are used together
        //   --decode # and --onlyaudio
        //   --sector-list
        //   --decode-frame
        //   --decode-audio
        // This could be slightly improved if we output multiple yuv frames
        // in a single stream. Then we could use --decode # and --noaudio
        if (iAdditionalArgs > 1)
            m_sOutputFileName = asRemainingArgs[1];
        
        
        //..............................................................
        // Check some of the optional options
        
        m_blnDecodeAudio = 
            !(Boolean)oParser.getOptionValue(oblnOptNoAudio, new Boolean(false));
        m_blnDecodeVideo = 
         !(Boolean)oParser.getOptionValue(oblnOptOnlyAudio, new Boolean(false));
        m_dblAudioScale = 
            (Double)oParser.getOptionValue(odblOptVolScale, new Double(1.0));
        m_iChannel = 
            (Integer)oParser.getOptionValue(oiOptChannel, new Integer(-1));
        
        //..............................................................
        // Check frame number options
        
        oOpt = oParser.getOptionValue(oiOptFrameNum);
        if (oOpt != null) {
            m_iStartFrame = (Integer)oOpt;
            if (m_iStartFrame < 0) ExitWithError("Negitive numbers are silly");
            m_iEndFrame = m_iStartFrame;
            m_blnDecodeAudio = false;
            m_blnDecodeVideo = true;
        }
        
        oOpt = oParser.getOptionValue(osOptFrameRange);
        if (oOpt != null) {
            int[] aiFrames = ParseDelimitedInts((String)oOpt, "-");
            if (aiFrames == null || aiFrames.length != 2)
                ExitWithError("Invalid range of frames");
            m_iStartFrame = aiFrames[0];
            m_iEndFrame = aiFrames[1];
            m_blnDecodeAudio = false;
            m_blnDecodeVideo = true;
        }
        
        //..............................................................
        // Check frame output format option
        
        
        m_sFormat = (String)oParser.getOptionValue(osOptFrameFormat, "png");
        m_sFormat = m_sFormat.toLowerCase();
        if (Arrays.binarySearch(VALID_FORMATS, m_sFormat) < 0)
            ExitWithError("Invalid output format (valid formats are " 
                          + Join(VALID_FORMATS, ", ") + ").");
        
        //..............................................................
        // Check verbosity option
        
        oOpt = oParser.getOptionValue(oiOptVerbosity);
        if (oOpt != null)
        {
            int[] ai = ParseDelimitedInts((String)oOpt, ",");
            if (ai != null) m_aiVerbosityLevels = ai;
        }
        
        
    }
    
    /*------------------------------------------------------------------------*/
    /*-- Private helper functions --------------------------------------------*/
    /*------------------------------------------------------------------------*/
    
    /** Splits string s via sDelimiter and parses the resulting array
     *  into an array of ints. If there is any error, then null is returned. */
    private static int[] ParseDelimitedInts(String s, String sDelimiter) {
        String[] asParse = s.split(sDelimiter);
        return StringArrayToIntArray(asParse);
    }

    /** Parses an array of strings into an array of ints. If there is any
     *  error, or if any of the values are negitive, null is returned. */
    private static int[] StringArrayToIntArray(String[] as) {
        try {
            int[] aiVals = new int[as.length];
            for (int i = 0; i < as.length; i++) {
                aiVals[i] = Integer.parseInt(as[i]);
                if (aiVals[i] < 0) return null;
            }
            
            return aiVals;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    /** Returns a sorted array of available ImageIO formats, plus our own 
     * special formats. */
    private static String[] GetValidFormats() {
        ArrayList<String> oValidFormats = new ArrayList<String>();
        oValidFormats.add("yuv");
        oValidFormats.add("y4m");
        oValidFormats.add("demux");
        oValidFormats.add("0rlc");
        String[] asReaderFormats = ImageIO.getReaderFormatNames();
        for (String s : asReaderFormats) {
            s = s.toLowerCase();
            if (oValidFormats.indexOf(s) < 0)
                oValidFormats.add(s);
        }
        
        Collections.sort(oValidFormats);
        
        return oValidFormats.toArray(new String[] {});
    }
    
    /** Joins an array of strings into a single string, inserting f between
     *  each array element. */
    private static String Join(String[] as, String f) {
        StringBuilder oSB = new StringBuilder();
        for (String s : as) {
            if (oSB.length() > 0)
                oSB.append(f);
            oSB.append(s);
        }
        return oSB.toString();
    }
    
    /** Either parses the supplied comma-separated list of sectors, or
     *  tries to open the supplied file to read a list of sectors. 
     *  Returns the list of sectors as an array of ints, or null if there
     *  is any error. */
    private static int[] HandleSectorListOption(String sListOrFile) {
        int[] ai = ParseDelimitedInts(sListOrFile, ",");
        if (ai == null) {
            if (new File(sListOrFile).exists()) {
                StringBuilder oSB = null;
                try {
                    FileReader fr = new FileReader(sListOrFile);
                    BufferedReader br = new BufferedReader(fr);
                    oSB = new StringBuilder();
                    String sLine;
                    while ((sLine = br.readLine()) != null) {
                        oSB.append(sLine);
                        oSB.append(" ");
                    }
                    fr.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                if (oSB != null && oSB.length() > 0) {
                    ai = StringArrayToIntArray(oSB.toString().split("[^\\d]"));
                }
            }
        }
        return ai;
    }
    
}
