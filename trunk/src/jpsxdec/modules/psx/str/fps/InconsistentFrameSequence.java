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

package jpsxdec.modules.psx.str.fps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.logging.Logger;
import jpsxdec.util.Fraction;
import jpsxdec.util.NotThisTypeException;

/** There are some unique frame rates that just don't make any sense.
 * These unique sequences of frame sectors just have to be manually
 * specified.
 */
public class InconsistentFrameSequence {

    private static final Logger log = Logger.getLogger(InconsistentFrameSequence.class.getName());

    /** List of sectors/frame data files. Update this list if the files change. */
    public static final String[] FPS_LISTS = new String[] {
        "20FPS_A8.dat",
        "20FPS_A16.dat",
        "NTSC20_A8.dat",
        "NTSC15_A8-100,999.dat",
        "NTSC15_A8-101,1000.dat"
    };

    /** Parses the header line in a sectors/frame data file.  */
    private static class HeaderParse {
            public final int iSectors   ,
                iPerFrame  ,
                iAudStart  ,
                iAudStride ;
            public final boolean blnLoop;

            public HeaderParse(String sLine) {
                String[] asValues = sLine.split("\\D+");
                iSectors    = Integer.parseInt(asValues[0]);
                iPerFrame   = Integer.parseInt(asValues[1]);
                iAudStart   = Integer.parseInt(asValues[2]);
                iAudStride  = Integer.parseInt(asValues[3]);
                blnLoop = sLine.contains("loop");
            }
    }

    private static class LineParse {
       public  final int iSectorNum,
                    iFrameNum,
                    iChunkNum,
                    iChunkCount;

       public LineParse(String sLine) throws NotThisTypeException {
           this(sLine, 0, 0);
       }
       public LineParse(String sLine, int iLoopFrame, int iLoopSector) 
               throws NotThisTypeException
       {
            try {
                // parse out the sector#,frame#,chunk#, and chunk count
                String[] asValues = sLine.split("\\D+");
                iSectorNum = Integer.parseInt(asValues[0]) + iLoopSector;
                iFrameNum = Integer.parseInt(asValues[1]) + iLoopFrame;
                iChunkNum = Integer.parseInt(asValues[2]);
                iChunkCount = Integer.parseInt(asValues[3]);
           } catch (NumberFormatException ex) {
               throw new NotThisTypeException();
           } catch (ArrayIndexOutOfBoundsException ex) {
               throw new NotThisTypeException();
           }
       }
    }

    public static LinkedList<InconsistentFrameSequence> generate(int iFirstFrame, int iFirstChunk, int iFirstChunkCount) {
        LinkedList<InconsistentFrameSequence> possibles = new LinkedList<InconsistentFrameSequence>();
        for (String sResource : FPS_LISTS) {
            InconsistentFrameSequence match = create(sResource, iFirstFrame, iFirstChunk, iFirstChunkCount);
            if (match != null)
                possibles.add(match);
        }
        return possibles;
    }

    /** Seeks through an inconsistent sectors/frame data file to see if it
     * contains a matching frame, chunk, and chunk-count combination of values.
     * If so, returns the InconsistentFrameSequence starting at that line,
     * which can continued to be read and compared against sequential sectors.
     */
    private static InconsistentFrameSequence create(String sResource,
            int iFirstFrame, int iFirstChunk, int iFirstChunkCount)
    {
        try {
            InputStream is = InconsistentFrameSequence.class.getResourceAsStream(sResource);
            if (is == null)
                throw new RuntimeException("Unable to find inconsistent frame resource " + sResource);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String sLine = reader.readLine(); // read header
            HeaderParse header = new HeaderParse(sLine);
            int iStartLine = 0;
            while ((sLine = reader.readLine()) != null) {
                iStartLine++;
                LineParse line = new LineParse(sLine);
                // past the frame number?
                if (line.iFrameNum > iFirstFrame)
                    return null; // no need to continue
                // is it a match?
                if (iFirstFrame == line.iFrameNum &&
                    iFirstChunk == line.iChunkNum &&
                    iFirstChunkCount == line.iChunkCount)
                {
                    return new InconsistentFrameSequence(reader, sResource, iStartLine, header);
                }
            }
            return null;
        } catch (NotThisTypeException ex) {
            // doesn't match, so we're done
            return null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    private BufferedReader _reader;
    private final String _sSourceResource;
    private final HeaderParse _header;
    private int _iCurrentLineSector;

    private InconsistentFrameSequence(BufferedReader reader, String sSourceResource,
            int iStartLine, HeaderParse oHead)
    {
        _reader = reader;
        _header = oHead;
        _iCurrentLineSector = iStartLine;
        _sSourceResource = sSourceResource;
    }

    public boolean matchesNextVideo(int iSector, int iFrame, int iChunk, int iFrameChunkCount) {
        try {
            String sLine = null;
            while (_iCurrentLineSector <= iSector) {
                sLine = _reader.readLine();
                if (sLine == null && _header.blnLoop) {
                    _reader.close();
                    InputStream is = InconsistentFrameSequence.class.getResourceAsStream(_sSourceResource);
                    _reader = new BufferedReader(new InputStreamReader(is));
                    _reader.readLine(); // skip header
                    sLine = _reader.readLine();
                }
                _iCurrentLineSector++;
            }
            if (sLine == null) {
                // darn, the movie is longer that this sequence.
                // we can probably assume it's a match, but this code
                // should be cleaned up and refactored to handle that.
                // as for now, we'll fail
                log.warning("Movie is longer than sequence " + _sSourceResource);
                return false;
            }
            LineParse line = new LineParse(sLine);

            boolean ret = //iFrame == line.iFrameNum && // don't check frame in case of looping
                   iChunk == line.iChunkNum &&
                   line.iChunkCount == iFrameChunkCount;
            if (!ret) {
                return false; //for breakpoint
            }
            return true;
        } catch (NotThisTypeException ex) {
            // doesn't match, so we're done
            return false;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Fraction getSectorsPerFrame() {
        return new Fraction(_header.iSectors, _header.iPerFrame);
    }
}
