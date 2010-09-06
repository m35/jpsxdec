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

package jpsxdec;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Misc;

/** Parses the command-line for the primary jPSXdec commands and holds the
 *  results. */
public class MainCommandLineParser {

    private static String[] MAIN_HELP = loadMainHelp();

    private static String[] loadMainHelp() {
        InputStream is = MainCommandLineParser.class.getResourceAsStream("main_cmdline_help.dat");
        if (is == null)
            throw new RuntimeException("Unable to find help resource " +
                    MainCommandLineParser.class.getResource("main_cmdline_help.dat"));
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        ArrayList<String> lines = new ArrayList<String>();
        try {
            String sLine;
            while ((sLine = br.readLine()) != null) {
                lines.add(sLine);
            }
            br.close();
            return lines.toArray(new String[lines.size()]);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void printMainHelp(PrintStream ps) {
        for (String sLine : MAIN_HELP) {
            ps.println(sLine);
        }
    }
    
    //=================================================================
    
    /** Display an error message, then exit the program. */
    private static void exitWithError(String sError) {
        System.err.println("Error: " + sError);
        System.err.println("Try -? for help.");
        System.err.println();
        System.exit(1);
    }
    
    /*------------------------------------------------------------------------*/
    /*- Properties -----------------------------------------------------------*/
    /*------------------------------------------------------------------------*/

    private int _iMainCmd;
    public int getMainCommand() { return _iMainCmd; }
    
    private String[] _asRemaining;
    public String[] getRemainingArgs() {
        return _asRemaining;
    }
    
    private String _sInputFile;
    /** Holds the input file without the extention. */
    private String _sInputFileBase;
    public String getInputFile() { return _sInputFile; }
    /** Returns the input file without the extention. */
    public String getInputFileBase() { return _sInputFileBase; }

    //...............................................................

    private String _sOutputFolder;
    private String _sOutputFile;
    
    public boolean gotOutputFolder() { return _sOutputFolder != null; }
    public String getOutputFolder() { return _sOutputFolder; }
    
    public boolean gotOutputFile() { return _sOutputFile != null; }
    public String getOutputFile() { return _sOutputFile; }
    
    /** Constructs a path using any folder supplied for output, and using
     *  the file name from the command-line, or the file name
     *  sDefFile argument. */
    public String constructOutPath(String sDefFile) {
        if (gotOutputFile())
            sDefFile = getOutputFile();
        
        if (gotOutputFolder())
            return new File(getOutputFolder(), sDefFile).toString();
        else
            return sDefFile;
    }
    
    //...............................................................
    
    /** For the -copysect %s command. */
    private int[] _aiSectorsToCopy;
    /** For the -copysect %s command. */
    public int[] getSectorsToCopy() { return _aiSectorsToCopy; }
    
    /** For the -sectordump %s command. */
    private String _sOutFile;
    /** For the -sectordump %s command. */
    public String getOutFile() { return _sOutFile; }

    /** For the -fps command. */
    private int _iFpsDumpItem;
    /** For the -fps command. */
    public int getFpsDumpItem() {
        return _iFpsDumpItem;
    }

    /** For the -index %s command. */
    private String _sIndexFile;
    /** For the -index %s command. */
    public boolean gotIndexFile() { return _sIndexFile != null; }
    /** For the -index %s command. */
    public String getIndexFile() { return _sIndexFile; }


    /** For the -decode, -play, and -encode. */
    private int _iDiscItemIndex;
    /** For the -decode, -play, and -encode. */
    public int getDiscItemIndex() { return _iDiscItemIndex; }

    /** For the -a command. */
    private String _sDecodeAllType;
    /** For the -a command. */
    public String getDecodeAllType() { return _sDecodeAllType; }

    /** For the -intype and -width # -height # commands. */
    private String _sStaticInType;
    /** For the -intype and -width # -height # commands. */
    public String getSpecialInType() { return _sStaticInType; }

    /** For the -v # command. */
    private int _iVerbose;
    /** For the -v # command. */
    public int getVerbose() { return _iVerbose; }

    /*------------------------------------------------------------------------*/
    /*------------------------------------------------------------------------*/

    public final static int MAIN_CMD_STARTGUI         = 1 << 1;
    public final static int MAIN_CMD_DECODE           = 1 << 2;
    public final static int MAIN_CMD_DECODE_ALL_TYPE  = 1 << 3;
    public final static int MAIN_CMD_SECTORLIST       = 1 << 4;
    public final static int MAIN_CMD_INDEX            = 1 << 5;
    public final static int MAIN_CMD_STATICFILE       = 1 << 6;
    public final static int MAIN_CMD_COPYSECT         = 1 << 7;
    public final static int MAIN_CMD_FPS_DUMP         = 1 << 8;
    public final static int MAIN_CMD_ITEM_HELP        = 1 << 9;
    public final static int MAIN_CMD_PLAY             = 1 << 10;
    public final static int MAIN_CMD_ENCODE           = 1 << 11;
    public final static int MAIN_CMD_VISUALIZE        = 1 << 12;
    
    /** Parses command-line arguments. */
    public MainCommandLineParser(String[] asArgs) {

        if (asArgs.length < 1) {
            exitWithError("Need at least one argument.");
        }

        ArgParser parser = new ArgParser("", false);

        //======================================================

        // first check for main help,

        // but need to ignore item help
        IntHolder ignoreItemHelp = new IntHolder(-999);
        parser.addOption("-h,-help,-? %i", ignoreItemHelp);
        parser.matchAllArgs(asArgs, 0, 0);
        // if it didn't find item help
        if (ignoreItemHelp.value == -999) {
            // then check for main help
            parser = new ArgParser("", false);
            BooleanHolder mainHelp = new BooleanHolder(false);
            parser.addOption("-h,-help,-? %v", mainHelp);
            parser.matchAllArgs(asArgs, 0, 0);
            
            if (mainHelp.value) {
                System.out.println(Main.VerStringNonCommercial);
                System.out.println();
                printMainHelp(System.out);
                System.exit(0);
            }
        }

        parser = new ArgParser("", false);

        //======================================================
        
        parseInputFile(asArgs);
        // remove the first elements from the list
        String[] asRemain = new String[asArgs.length-1];
        System.arraycopy(asArgs, 1, asRemain, 0, asRemain.length);
        asArgs = asRemain;

        //======================================================
        
        IntHolder decode = new IntHolder(-10);
        parser.addOption("-decode,-d %i {[0,"+Integer.MAX_VALUE+"]}", decode);

        StringHolder decodeAllType = new StringHolder();
        parser.addOption("-a %s", decodeAllType);

        StringHolder index = new StringHolder();
        parser.addOption("-index,-i,-idx %s", index);

        StringHolder staticType = new StringHolder();
        parser.addOption("-static %s", staticType);

        //........................................

        StringHolder copysect = new StringHolder();
        parser.addOption("-copysect %s", copysect);

        StringHolder sectordump = new StringHolder();
        parser.addOption("-sectordump %s", sectordump);

        StringHolder visualize = new StringHolder();
        parser.addOption("-visualize %s", visualize);

        IntHolder fpsdump = new IntHolder(-10);
        parser.addOption("-fpsdump %i {[0,"+Integer.MAX_VALUE+"]}", fpsdump);

        IntHolder play = new IntHolder(-10);
        parser.addOption("-play %i {[0,"+Integer.MAX_VALUE+"]}", play);

        IntHolder encode = new IntHolder(-10);
        parser.addOption("-strreplace %i {[0,"+Integer.MAX_VALUE+"]}", encode);

        IntHolder itemHelp = new IntHolder(-10);
        parser.addOption("-h,-help,-? %i", itemHelp);
        
        //........................................
        
        IntHolder verbose = new IntHolder(FeedbackStream.NORM);
        parser.addOption("-v,-verbose %i", verbose);

        //-----------------------------------------

        _asRemaining = parser.matchAllArgs(asArgs, 0, ArgParser.EXIT_ON_ERROR);

        //-----------------------------------------
        
        _iVerbose = verbose.value;

        //-----------------------------------------

        // Find how many main commands were provided
        if (decode.value >= 0)           _iMainCmd |= MAIN_CMD_DECODE;
        if (decodeAllType.value != null) _iMainCmd |= MAIN_CMD_DECODE_ALL_TYPE;
        if (sectordump.value != null)    _iMainCmd |= MAIN_CMD_SECTORLIST;
        if (visualize.value != null)     _iMainCmd |= MAIN_CMD_VISUALIZE;
        if (staticType.value != null)    _iMainCmd |= MAIN_CMD_STATICFILE;
        if (copysect.value != null)      _iMainCmd |= MAIN_CMD_COPYSECT;
        if (fpsdump.value >= 0)          _iMainCmd |= MAIN_CMD_FPS_DUMP;
        if (itemHelp.value >= 0)         _iMainCmd |= MAIN_CMD_ITEM_HELP;
        if (play.value >= 0)             _iMainCmd |= MAIN_CMD_PLAY;
        if (encode.value >= 0)           _iMainCmd |= MAIN_CMD_ENCODE;

        // if no other main commands were provided
        if (index.value != null) {
            // then index can be the main command
            if (_iMainCmd == 0)
                _iMainCmd = MAIN_CMD_INDEX;
            _sIndexFile = index.value;
        }

        //-----------------------------------------

        // do extra stuff depending on the main command
        switch (_iMainCmd) {
            case MAIN_CMD_STATICFILE:
                _sStaticInType = staticType.value;
                break;

            case MAIN_CMD_DECODE:
                _iDiscItemIndex = decode.value;
                break;

            case MAIN_CMD_DECODE_ALL_TYPE:
                _sDecodeAllType = decodeAllType.value;
                break;

            case MAIN_CMD_SECTORLIST:
                _sOutFile = sectordump.value;
                break;

            case MAIN_CMD_INDEX:
                break;

            case MAIN_CMD_COPYSECT:
                _aiSectorsToCopy = parseNumberRange(copysect.value);
                if (_aiSectorsToCopy == null)
                    exitWithError("Invalid sector range " + copysect.value);
                break;

            case MAIN_CMD_FPS_DUMP:
                _iFpsDumpItem = fpsdump.value;
                break;

            case MAIN_CMD_ITEM_HELP:
                _iDiscItemIndex = itemHelp.value;
                break;

            case MAIN_CMD_PLAY:
                _iDiscItemIndex = play.value;
                break;

            case MAIN_CMD_ENCODE:
                _iDiscItemIndex = encode.value;
                break;

            case MAIN_CMD_VISUALIZE:
                _sOutFile = visualize.value;
                break;

            case 0:
                exitWithError("Need a main command.");
                break;
            default:
                exitWithError("Too many main commands.");
                break;
        }


    }
    
    /** Takes the first item and then checks if it's a valid file. */
    private void parseInputFile(String[] args) {
        if (args == null || args.length < 1) return;
        
        String s = args[0];
        File oFile = new File(s);

        // if the file doesn't exist
        if (!oFile.exists())  {
            // check if it's a root drive
            String abs = oFile.getAbsolutePath().toUpperCase();
            boolean isRoot = false;
            for (File root : File.listRoots()) {
                if (abs.equals(root.getAbsolutePath().toUpperCase())) {
                    isRoot = true;
                    break;
                }
            }
            
            if (!isRoot)
                exitWithError("\"" + s + "\" file not found.");
        }
        
        _sInputFile = s;
        _sInputFileBase = Misc.getBaseName(oFile.getName());
    }
    
    /** Checks if the output file is valid. */
    private void parseOutputFile(String s) {
        /* path\
         * path\path\
         * path\file
         * \file
         */
        
        File oPath = new File(s);
        
        // if it uses an invalid folder, throw an error
        File oParent = oPath.getParentFile();
        if (oParent != null && !oParent.isDirectory()) {
            exitWithError("Invalid output path " + oParent.toString());
        }
        
        // check if the whole output file is an existing folder
        if (oPath.isDirectory()) {
            _sOutputFolder = oPath.getAbsolutePath();
        } else {
            // otherwise it's a file name, and with maybe a folder as well
            if (oParent != null)
                _sOutputFolder = oParent.getAbsolutePath();
            _sOutputFile = oPath.getName();
        }
        
    }
    
    
    /** Parse a number range. e.g. 5-10
     * @return Array of 2 elements, or null on error. */
    public static int[] parseNumberRange(String s) {
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
            return null;
        }
    }


    /** Parse a string of comma-delimited numbers and ranges, then creates an
     *  array with indexes toggled based on the numbers.
     *  e.g. 3,6-9,15 */
    public static boolean[] parseNumberListRange(String s, int iMax) {
        boolean[] abln = new boolean[iMax];
        Arrays.fill(abln, false);
        try {
            for (String num : s.split(",")) {
                if (s.indexOf('-') > 0) {
                    // TODO: Finish
                    throw new UnsupportedOperationException("Not finished yet.");
                } else {
                    abln[Integer.parseInt(num)] = true;
                }
            }
        } catch (NumberFormatException ex) {
            return null;
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
        return abln;
    }

    
}

