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
 */

package jpsxdec;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import java.io.File;
import java.util.Vector;
import jpsxdec.media.savers.Formats;
import jpsxdec.util.Fraction;
import jpsxdec.util.Misc;


public class Settings {
    
    public static void main(String[] args) {
        
        Settings m = new Settings(args);
        
        if (m.getMainCommand() == Settings.MAIN_CMD_STARTGUI) {
            System.out.println("Starting gui");
        }

    }
    
    
    private static class MyArgParser extends ArgParser {

        public MyArgParser(String arg0, boolean arg1) {
            super(arg0, arg1);
        }

        public MyArgParser(String arg0) {
            super(arg0);
        }

        @Override
        public void printErrorAndExit(String msg) {
            ExitWithError(msg);
        }

        @Override
        public String getHelpMessage() {
            return HELP_MESSAGE;
        }
        
        
        
    }
    
    
    //=================================================================
    
    private static void ExitWithError(String sError) {
        System.err.println("Error: " + sError);
        System.err.println("Try -? for help.");
        System.err.println();
        System.exit(1);
    }
    
    private final static String AUD_FMT_LIST;
    private final static String TIM_IMG_FMT_LIST;
    private final static String IMAGE_FMT_DESCRIPTION;
    static {
        
        AUD_FMT_LIST = joinExts(Formats.getJavaAudFormats());
        TIM_IMG_FMT_LIST = joinExts(Formats.getAllJavaImgFormats());
        
        String sVidImgFormats = joinExts(Formats.getVidCompatableImgFmts());
        
        // TODO: Clean this mess up
        
        IMAGE_FMT_DESCRIPTION = 
"          Image sequence: " + sVidImgFormats + "\n" +
joinExts(Formats.getExtendedSeqFormats(), 26, true) +
"                     Avi: " + joinId(Formats.getAviVidFormats(), true);
        
    }
    
    private static String joinExts(Vector<? extends Formats.Format> oFmts) {
        return joinExts(oFmts, 0, false);
    }
    private static String joinExts(Vector<? extends Formats.Format> oFmts, boolean withDesc) {
        return joinExts(oFmts, 0, withDesc);
    }
    private static String joinExts(Vector<? extends Formats.Format> oFmts, int indent, boolean withDesc) {
        StringBuilder oSB = new StringBuilder();
        boolean blnFirst = true;
        for (Formats.Format fmt : oFmts) {
            if (indent > 0)
                oSB.append(Misc.dup(" ", indent));
            else
                if (!blnFirst) oSB.append(", "); else blnFirst = false;
            oSB.append(fmt.getExt());
            if (withDesc) {
                oSB.append(" (");
                oSB.append(fmt.getDesciption());
                oSB.append(")");
            }
            if (indent > 0)
                oSB.append('\n');
        }
        return oSB.toString();
    }
    private static String joinId(Vector<? extends Formats.Format> oFmts, boolean withDesc) {
        StringBuilder oSB = new StringBuilder();
        boolean blnFirst = true;
        for (Formats.Format fmt : oFmts) {
            if (!blnFirst) oSB.append(", "); else blnFirst = false;
            oSB.append(fmt.getId());
            if (withDesc) {
                oSB.append(" (");
                oSB.append(fmt.getDesciption());
                oSB.append(")");
            }
        }
        return oSB.toString();
    }
    
    private static String HELP_MESSAGE = 
Main.VerString + "\n\n" +
"Usage:                                                                    \n" +
"    java -jar jpsxdec.jar INFILE [out] {main option} [additional options] \n" +
"                                                                          \n" +
"Where:                                                                    \n" +
"    INFILE                    Input file name                             \n" +
"    out                       Output file base name (optional)            \n" +
"                                                                          \n" +
"Main options (require one):                                               \n" +
"    -?/-help                  Show this message                           \n" +
"    -decode/-d { # | all }    Decode one or all media items               \n" +
"    -index/-idx/-i <file>     Save index file                             \n" +
"    -sectordump <file>        Write list of sector types (for debugging)  \n" +
"    -intype { demux | mdec }  Decode special type with -width & -height   \n" +
"    -plugin <name>            Use top-secret plugins                      \n" +
"    -dir                      If INFILE is CD, lists files on it          \n" +
"    -copy <name>              Copy raw file out of CD                     \n" +
"                                                                          \n" +
"Additional options (optional):                                            \n" +
"    -index/-idx/-i <file>     Load and/or save index file during task     \n" +
"    -vidfmt/-vf <format>      Save video as:                              \n" +
                         IMAGE_FMT_DESCRIPTION                          + "\n" +
"    -audfmt/-af <format>      Save audio as:                              \n" +
"                                 " +    AUD_FMT_LIST                   + "\n" +
"    -tim <format>             Save TIM files as:                          \n" +            
"                                 " +    TIM_IMG_FMT_LIST               + "\n" +
"    -jpg #                    JPEG or MJPG quality: 0 to 100              \n" +
"    -vol #                    Audio volume: 0 to 100                      \n" +
"    -novid                    Don't decode video                          \n" +
"    -noaud                    Don't decode audio                          \n" +
"    -f/-frame[s] { # | #-# }  Decode one frame, or range of frames        \n" +
"    -fps { # | #/# }          Write avi/yuv with specified frame rate     \n" +
"    -nocrop                   Don't crop frames with dims not div by 16   \n" +
"\n"+            
"    -width #                  Width of special -intype file               \n" +
"    -height #                 Height of special -intype file              \n" +
"\n"+            
"    -debug #,#,#,#,#          Prints verbose debugging messages           \n" +
"                                                                          \n" +
"Default is to save video as compressed AVI,                               \n" +
"audio as part of AVI, and TIM as PNG.                                     \n" +
"If no options provided, GUI is started.                                   \n";
    
    private static void HelpAndExit() {
        System.out.println(HELP_MESSAGE);
        System.exit(0);
    }
    
    
    /*------------------------------------------------------------------------*/
    /*- Properties -----------------------------------------------------------*/
    /*------------------------------------------------------------------------*/

    private int m_iMainCmd;
    public int getMainCommand() { return m_iMainCmd; }
    
    private String[] m_asRemaining;
    public String[] getRemainingArgs() {
        return m_asRemaining;
    }
    
    private String m_sInputFile;
    private String m_sInputFileBase;
    public String getInputFile() { return m_sInputFile; }
    public String getInputFileBase() { return m_sInputFileBase; }

    Fraction m_oFps;
    public boolean gotFps() { return m_oFps != null; }
    public Fraction getFps() { return m_oFps; }
    
    //...............................................................

    private String m_sOutputFolder;
    private String m_sOutputFile;
    
    public boolean gotOutputFolder() { return m_sOutputFolder != null; }
    public String getOutputFolder() { return m_sOutputFolder; }
    
    public boolean gotOutputFile() { return m_sOutputFile != null; }
    public String getOutputFile() { return m_sOutputFile; }
    
    public String constructOutPath(String sDefFile) {
        if (gotOutputFile())
            sDefFile = getOutputFile();
        
        if (gotOutputFolder())
            return new File(getOutputFolder(), sDefFile).toString();
        else
            return sDefFile;
    }
    
    //...............................................................
    
    private String m_sFileToCopy;
    public String getFileToCopy() { return m_sFileToCopy; }
    
    private String m_sPluginCommand;
    public String getPluginCommand() { return m_sPluginCommand; }
    private String m_sSectorDumpFile;
    public String getSectorDumpFile() { return m_sSectorDumpFile; }

    private String m_sIndexFile;
    public boolean gotIndexFile() { return m_sIndexFile != null; }
    public String getIndexFile() { return m_sIndexFile; }


    private int m_iDecodeIndex;
    public boolean gotDecodeIndex() { return m_iDecodeIndex >= 0; }
    public int getDecodeIndex() { return m_iDecodeIndex; }


    private boolean m_blnDecodeVid = true;
    private boolean m_blnDecodeAud = true;
    public boolean decodeAudio() { return m_blnDecodeAud; }
    public boolean decodeVideo() { return m_blnDecodeVid; }

    private boolean m_blnNoCrop = false;
    public boolean noCrop() { return m_blnNoCrop; }

    private int m_iStartFrame = -1;
    private int m_iEndFrame = -1;
    public boolean gotFrames() { return m_iEndFrame >= 0 && m_iStartFrame >= 0; };
    public int getStartFrame() { return m_iStartFrame; }
    public int getEndFrame() { return m_iEndFrame; }

    private String m_sSpecialInType;
    private int m_iWidth;
    private int m_iHeight;
    public String getSpecialInType() { return m_sSpecialInType; }
    public int getHeight() { return m_iHeight; }
    public int getWidth() { return m_iWidth; }

    private int m_iJpgQuality = -1;
    public boolean gotJpgQuality() { return m_iJpgQuality >= 0; };
    public int getJpgQuality() { return m_iJpgQuality; }

    private int m_iVolume = -1;
    public boolean gotVolume() { return m_iVolume >= 0; }
    public int getVolume() { return m_iVolume; }

    private Formats.Format m_oAudFormat;
    public boolean gotAudFormat() { return m_oAudFormat != null; }
    public Formats.Format getAudFormat() { return m_oAudFormat; }

    private Formats.Format m_oVidFormat;
    public boolean gotVidFormat() { return m_oVidFormat != null; }
    public Formats.Format getVidFormat() { return m_oVidFormat; }

    private int[] m_aiDebug;
    public boolean gotDebug() { return m_aiDebug != null; }
    public int[] getDebug() { return m_aiDebug; }
    
    private Formats.ImgSeqVidFormat m_oTimFormat;
    public boolean gotTimFormat() { return m_oTimFormat != null; }
    public Formats.ImgSeqVidFormat  getTimFormat() { return m_oTimFormat; }
    
    /*------------------------------------------------------------------------*/
    /*------------------------------------------------------------------------*/

    public final static int MAIN_CMD_HELP        = 1 << 0;
    public final static int MAIN_CMD_STARTGUI    = 1 << 1;
    public final static int MAIN_CMD_DECODE      = 1 << 2;
    public final static int MAIN_CMD_SECTORDUMP  = 1 << 3;
    public final static int MAIN_CMD_PLUGIN      = 1 << 4;
    public final static int MAIN_CMD_INDEX       = 1 << 5;
    public final static int MAIN_CMD_SPECIALFILE = 1 << 6;
    public final static int MAIN_CMD_DIR         = 1 << 7;
    public final static int MAIN_CMD_COPY        = 1 << 8;
    
    public Settings(String[] args) {
        
        // look for help option if it exists
        ArgParser parser = new MyArgParser("", true);
        parser.matchAllArgs(args, 0, ArgParser.EXIT_ON_ERROR);

        
        // if 0 or 1 args, start gui with supplied in-file (if any)
        if (args.length == 0) {
            m_iMainCmd = MAIN_CMD_STARTGUI;
        } else if (args.length == 1) {
            parseInputFile(args);
            m_iMainCmd = MAIN_CMD_STARTGUI;
        } else {
            // if 2 or more, let's see what we got
            
            // first remove the first arg
            parseInputFile(args);
            String[] remain = new String[args.length-1];
            System.arraycopy(args, 1, remain, 0, remain.length);

            // parse the commands
            remain = parseMainCommand(remain);
            
            // if there is at least one leftover arg,
            if (remain != null && remain.length > 0) {
                // assume it is the output file name
                
                parseOutputFile(remain[0]);

                // remove it from the remaining args
                if (remain.length > 1) {
                    m_asRemaining = new String[remain.length-1];
                    System.arraycopy(remain, 1, m_asRemaining, 0, m_asRemaining.length);
                } else {
                    m_asRemaining = null;
                }
            } else {
                m_asRemaining = null;
            }
        }
        
    }
    
    private void parseInputFile(String[] args) {
        if (args.length < 1) return;
        
        String s = args[0];
        
        File oFile = new File(s);

        if (!oFile.exists())  {
            String abs = oFile.getAbsolutePath().toUpperCase();
            boolean isRoot = false;
            for (File root : File.listRoots()) {
                if (abs.equals(root.getAbsolutePath().toUpperCase())) {
                    isRoot = true;
                    break;
                }
            }
            
            if (!isRoot)
                ExitWithError(s + " does not exist.");
        }
        
        m_sInputFile = s;
        m_sInputFileBase = Misc.getBaseName(oFile.getName());
    }
    
    private void parseOutputFile(String s) {
        /* path\
         * path\path\
         * path\file
         * \file
         */
        
        File oPath = new File(s);
        
        File oParent = oPath.getParentFile();
        if (oParent != null && !oParent.isDirectory()) {
            ExitWithError("Invalid output path " + oParent.toString());
        }
        
        if (oPath.isDirectory()) {
            m_sOutputFolder = oPath.getAbsolutePath();
        } else {
            if (oParent != null)
                m_sOutputFolder = oParent.getAbsolutePath();
            m_sOutputFile = oPath.getName();
        }
        
    }
    
    
    
    private final static String PLUGINS = "lapks, site";
    
    private String[] parseMainCommand(String[] args) {
        
        
        ArgParser parser = new MyArgParser("", false);
        
        //----------------------------------------
        
        StringHolder decode = new StringHolder();
        parser.addOption("-decode,-d %s", decode);
        
        StringHolder sectordump = new StringHolder();
        parser.addOption("-sectordump %s", sectordump);
        
        StringHolder plugin = new StringHolder();
        parser.addOption("-plugin %s {"+ PLUGINS +"}", plugin);
        
        StringHolder index = new StringHolder();
        parser.addOption("-index,-i,-idx %s", index);
        
        StringHolder intype = new StringHolder();
        parser.addOption("-intype %s {demux, mdec}", intype);
        
        StringHolder debug = new StringHolder();
        parser.addOption("-debug %s", debug);
        
        StringHolder copy = new StringHolder();
        parser.addOption("-copy %s", copy);

        BooleanHolder dir = new BooleanHolder(false);
        parser.addOption("-dir %v", dir);
        
        //.........................................
        
        IntHolder width = new IntHolder(-1);
        parser.addOption("-width %i {[0, 99999]}", width);

        IntHolder height = new IntHolder(-1);
        parser.addOption("-height %i {[0, 99999]}", height);

        //-----------------------------------------
        
        String[] remain = parser.matchAllArgs(args, 0, ArgParser.EXIT_ON_ERROR);
        
        //-----------------------------------------
        
        if (debug.value != null)
            m_aiDebug = parseDebug(debug.value);
        

        if (decode.value != null) m_iMainCmd |= MAIN_CMD_DECODE;
        if (sectordump.value != null) m_iMainCmd |= MAIN_CMD_SECTORDUMP;
        if (plugin.value != null) m_iMainCmd |= MAIN_CMD_PLUGIN;
        if (intype.value != null) m_iMainCmd |= MAIN_CMD_SPECIALFILE;
        if (copy.value != null) m_iMainCmd |= MAIN_CMD_COPY;
        if (dir.value) m_iMainCmd |= MAIN_CMD_DIR;
        
        if (m_iMainCmd == 0 && index.value != null) m_iMainCmd = MAIN_CMD_INDEX;
        
        //-----------------------------------------
        

        m_sIndexFile = index.value;
        
        switch (m_iMainCmd) {
            case MAIN_CMD_SPECIALFILE:
                if (width.value < 0 || height.value < 0)
                    ExitWithError("-intype command must have -with and -height options.");
        
                m_sSpecialInType = intype.value;
                m_iWidth = width.value;
                m_iHeight = height.value;
                
                remain = parseDecodeOptions(remain);
                break;
                
            case MAIN_CMD_DECODE: 

                m_iDecodeIndex = parseDecodeItem(decode.value);
                
                remain = parseDecodeOptions(remain);
                break;
                
            case MAIN_CMD_SECTORDUMP:
                m_sSectorDumpFile = sectordump.value;
                break;
                
            case MAIN_CMD_INDEX:
                break;
                
            case MAIN_CMD_PLUGIN:
                m_sPluginCommand = plugin.value;
                break;
                
            case MAIN_CMD_COPY:
                m_sFileToCopy = copy.value;
                break;
                
            case MAIN_CMD_DIR:
                break;
                
            case 0:
                ExitWithError("Need a main command.");
                break;
            default:
                ExitWithError("Too many main commands.");
                break;
        }
        
        
        return remain;
        
    }
    
    
    public final static int DECODE_ALL = -1;
    
    private static int parseDecodeItem(String s) {
        if (s.equals("all"))
            return DECODE_ALL;
        else {
            try {
                int i = Integer.parseInt(s);
                if (i < 0) throw new NumberFormatException();
                return i;
            } catch (NumberFormatException ex) {
                ExitWithError("Invalid item to decode: " + s);
                throw new RuntimeException("Invalid item to decode: " + s);
            }
        }
    }
    
    
    private static int[] parseDebug(String s) {
        try {
            String[] split = s.split(",");
            
            if (split.length != 5) {
                ExitWithError("Invalid debug option: " + s);
            }
            
            int[] aidbg = new int[split.length];
            for (int i = 0; i < aidbg.length; i++) {
                aidbg[i] = Integer.parseInt(split[i]);
            }
            
            return aidbg;

        } catch (NumberFormatException ex) {
            ExitWithError("Invalid debug option: " + s);
            throw new RuntimeException("Invalid debug option: " + s);
        }
    }
    
    
    
    private String[] parseDecodeOptions(String[] args) {
        
        if (args == null) return null;
        
        ArgParser parser = new MyArgParser("", false);
        
        //----------------------------------------
        
        BooleanHolder noaud = new BooleanHolder(false);
        parser.addOption("-noaud %v", noaud);
        
        BooleanHolder novid = new BooleanHolder(false);
        parser.addOption("-novid %v", novid);
        
        IntHolder jpg = new IntHolder(-1);
        parser.addOption("-jpg %i {[0, 100]}", jpg);
        
        IntHolder vol = new IntHolder(-1);
        parser.addOption("-vol %i {[0, 100]}", vol);
        
        StringHolder frames = new StringHolder();
        parser.addOption("-frame,-frames,-f %s", frames);
        
        StringHolder fps = new StringHolder();
        parser.addOption("-fps %s", fps);
        
        BooleanHolder nocrop = new BooleanHolder(false);
        parser.addOption("-nocrop %v", nocrop);
        
        StringHolder vidfmt = new StringHolder();
        parser.addOption("-vidfmt,-vf %s", vidfmt);
        
        StringHolder audfmt = new StringHolder();
        parser.addOption("-audfmt,-af %s", audfmt);
        
        StringHolder tim = new StringHolder();
        parser.addOption("-tim %s", tim);
        
        
        //-----------------------------------------
        
        String[] remain = null;
        remain = parser.matchAllArgs(args, 0, ArgParser.EXIT_ON_ERROR);
        
        //-----------------------------------------
        
        m_blnDecodeAud = !noaud.value;
        m_blnDecodeVid = !novid.value;
        
        m_iJpgQuality = jpg.value;
        m_iVolume = vol.value;
        
        if (frames.value != null) {
            int[] aiframes = parseFrames(frames.value);
            m_iStartFrame = aiframes[0];
            m_iEndFrame = aiframes[1];
        }
        
        if (fps.value != null) {
            m_oFps = parseFps(fps.value);
        }
        
        m_blnNoCrop = nocrop.value;
        
        if (vidfmt.value != null) {
            for (Formats.ImgSeqVidFormat fmt : Formats.getVidCompatableImgFmts()) {
                if (fmt.getExt().equalsIgnoreCase(vidfmt.value)) {
                    m_oVidFormat = fmt;
                    break;
                }
            }
            if (m_oVidFormat == null) {
                for (Formats.ImgSeqVidFormat fmt : Formats.getExtendedSeqFormats()) {
                    if (fmt.getExt().equalsIgnoreCase(vidfmt.value)) {
                        m_oVidFormat = fmt;
                        break;
                    }
                }
                if (m_oVidFormat == null) {
                    for (Formats.AviVidFormat fmt : Formats.getAviVidFormats()) {
                        if (fmt.getId().equalsIgnoreCase(vidfmt.value)) {
                            m_oVidFormat = fmt;
                            break;
                        }
                    }
                }
            }
            if (m_oVidFormat == null)
                ExitWithError("Unhandled video format " + vidfmt.value + ".");
        }
        
        if (audfmt.value != null) {
            for (Formats.AudFormat fmt : Formats.getJavaAudFormats()) {
                if (fmt.getExt().equalsIgnoreCase(audfmt.value)) {
                    m_oAudFormat = fmt;
                    break;
                }
            }
            if (m_oAudFormat == null)
                ExitWithError("Unhandled audio format " + audfmt.value + ".");
        }
        
        if (tim.value != null) {
            for (Formats.ImgSeqVidFormat fmt : Formats.getAllJavaImgFormats()) {
                if (fmt.getExt().equalsIgnoreCase(tim.value)) {
                    m_oTimFormat = fmt;
                    break;
                }
            }
            if (m_oTimFormat == null)
                ExitWithError("Unhandled TIM format " + tim.value + ".");
        }
        
        return remain;
    }
    

    private static Fraction parseFps(String s) {
        String[] split = s.split("/");
        try {
            
            int num;
            int denom;
            
            if (split.length == 1) {
                num = Integer.parseInt(split[0]);
                denom = 1;
            } else if (split.length == 2) {
                num = Integer.parseInt(split[0]);
                denom = Integer.parseInt(split[1]);
            } else {
                ExitWithError("Invalid frames per second: " + s);
                throw new RuntimeException("Invalid frames per second: " + s);
            }
            return new Fraction(num, denom);
            
        } catch (NumberFormatException ex) {
            ExitWithError("Invalid frames per second: " + s);
            throw new RuntimeException("Invalid frames per second: " + s);
        }
    }
    
    private static int[] parseFrames(String s) {
        int iStart, iEnd;
        String[] split = s.split("-");
        try {
            
            if (split.length == 2) {
                iStart = Integer.parseInt(split[0]);
                iEnd = Integer.parseInt(split[1]);
            } else {
                iStart = iEnd = Integer.parseInt(s);
            }
            
            return new int[] {iStart, iEnd};
            
        } catch (NumberFormatException ex) {
            ExitWithError("Invalid frame: " + s);
            throw new RuntimeException("Invalid frame: " + s);
        }
    }
    
    
    
    
}

