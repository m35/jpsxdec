/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.modules.video.sectorbased.fps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.Fraction;

/** There are some unique frame rates that just don't make any sense.
 * These unique sequences of frame sectors just have to be manually
 * specified. */
public class InconsistentFrameSequence {

    private static final Logger LOG = Logger.getLogger(InconsistentFrameSequence.class.getName());

    // -------------------------------------------------------------------------
    // --  Static stuff  -------------------------------------------------------
    // -------------------------------------------------------------------------

    /** List of sectors/frame data files. Update this list if the files change. */
    private static final String[] FPS_LISTS = new String[] {
        "20FPS_A8.dat",
        "20FPS_A16.dat",
        "NTSC20_A8.dat",
        "NTSC20_A8-SB.dat",
        "NTSC15_A8-100,999.dat",
        "NTSC15_A8-101,1000.dat",
        "LUNAR2_24FPS_A16(S43).dat",
        "LUNAR2_24FPS_A16(S56).dat",
        "DREDD15FPS.dat",
    };

    public static @Nonnull LinkedList<InconsistentFrameSequence> generate(int iFirstFrameStartSector,
                                                                          int iFirstFrameEndSector)
    {
        LinkedList<InconsistentFrameSequence> possibles = new LinkedList<InconsistentFrameSequence>();
        for (String sResource : FPS_LISTS) {
            possibles.add(new InconsistentFrameSequence(sResource));
        }
        return possibles;
    }

    /** Parses the header line in a sectors/frame sequence text file.
     * <pre>[sectors]/[per-frame] [audio start sector] [audio sector stride] [optional loop sector]</pre>
     * The 4 values can be delimited by anything that isn't a number.
     * if the optional loop sector is found, then the sequence will loop
     * back to the start when the end is reached.
     */
    private static class HeaderParse {
        public final int iSectors,
                         iPerFrame,
                         iAudStart,
                         iAudStride,
                         iLoopSector;

        public HeaderParse(@Nonnull String sLine) {
            String[] asValues = sLine.split("\\D+");
            iSectors    = Integer.parseInt(asValues[0]);
            iPerFrame   = Integer.parseInt(asValues[1]);
            iAudStart   = Integer.parseInt(asValues[2]);
            iAudStride  = Integer.parseInt(asValues[3]);
            if (asValues.length > 4)
                iLoopSector = Integer.parseInt(asValues[4]);
            else
                iLoopSector = -1;
        }
    }

    /** Parses a line in a sectors/frame sequence text file. The format is
     * <pre>
     * [frame start sector] [frame end sector]
     * </pre>
     * The 2 values can be delimited by anything that isn't a number.
     * Anything after the 2 values is ignored.
     */
    static class LineParse {
       public final int iFrameStartSector,
                        iFrameEndSector;

       public LineParse(@Nonnull String sLine) {
            // parse out the sector#,frame#,chunk#, and chunk count
            String[] asValues = sLine.split("\\D+");
            iFrameStartSector = Integer.parseInt(asValues[0]);
            iFrameEndSector = Integer.parseInt(asValues[1]);
       }
    }

    // -------------------------------------------------------------------------
    // --  Instance stuff  -----------------------------------------------------
    // -------------------------------------------------------------------------

    @Nonnull
    private BufferedReader _reader;
    @Nonnull
    private final String _sSourceResource;
    @Nonnull
    private final HeaderParse _header;
    private int _iLoopStartSector;

    private InconsistentFrameSequence(@Nonnull String sSourceResource) {
        _sSourceResource = sSourceResource;
        try {
            InputStream is = InconsistentFrameSequence.class.getResourceAsStream(sSourceResource);
            if (is == null)
                throw new RuntimeException("Unable to find inconsistent frame resource " + sSourceResource);
            _reader = new BufferedReader(new InputStreamReader(is));
            String sLine = _reader.readLine(); // read header
            // resource file should have at least 1 line
            _header = new HeaderParse(sLine);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private @CheckForNull String readNextLine() throws IOException {
        String sLine = _reader.readLine();
        if (sLine == null) {
            // if at the end of the resource, but we're looping
            if (_header.iLoopSector >= 0) {
                // close and reopen the text resource
                _reader.close(); // expose close exception
                InputStream is = InconsistentFrameSequence.class.getResourceAsStream(_sSourceResource);
                _reader = new BufferedReader(new InputStreamReader(is));
                _reader.readLine(); // skip header
                sLine = _reader.readLine();
                _iLoopStartSector += _header.iLoopSector;
            } else {
                // darn, the movie is longer than we have defined in the sequence.
                // we can probably assume it's a match, but this code
                // TODO: should be changed to handle that and report.
                // as for now, we'll fail
                LOG.log(Level.WARNING, "Movie is longer than sequence {0}", _sSourceResource);
            }
        }
        return sLine;
    }

    public boolean matchesNextVideo(int iFrameStartSector, int iFrameEndSector) {
        try {
            LineParse currentLineFrame;
            do {
                String sLine = readNextLine();
                if (sLine == null)
                    return false;
                currentLineFrame = new LineParse(sLine);
            } while (currentLineFrame.iFrameStartSector + _iLoopStartSector < iFrameStartSector);

            // TODO: Check if frame lies between the previous and next frames (instead of just inside this frame)
            // prevLineFrame.iFrameEndSector + _iLoopStartSector <= iFrameStartSector
            // nextLineFrame.iFrameStartSector + _iLoopStartSector <= iFrameEndSector
            if (currentLineFrame.iFrameStartSector + _iLoopStartSector <= iFrameStartSector &&
                currentLineFrame.iFrameEndSector   + _iLoopStartSector >= iFrameEndSector)
            {
                return true;
            } else {
                return false;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public @Nonnull Fraction getSectorsPerFrame() {
        return new Fraction(_header.iSectors, _header.iPerFrame);
    }

    @Override
    public String toString() {
        return _sSourceResource;
    }


}
