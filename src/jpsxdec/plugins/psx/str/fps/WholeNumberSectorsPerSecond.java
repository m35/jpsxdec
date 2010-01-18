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

package jpsxdec.plugins.psx.str.fps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Pattern;
import jpsxdec.util.Misc;

/** A clever method to detect whole-number (integer) sectors/second rate of
 * STR movies. This can also handle variable sector rates, and returns the
 * lowest common denominator of the varying rates (unless it's 1, then
 * it fails). */
public class WholeNumberSectorsPerSecond {
    
    private int _iCurFrame;
    private int _iLastSect = -1;
    // TODO: how can we detect if the video has a variable frame rate?
    // maybe detect when rates change by some factor?

    private int[] _aiPossibleGcds;
    private BetweenFrames _firstBetween;

    public WholeNumberSectorsPerSecond(int iFrame) {
        _iCurFrame = iFrame;
    }

    /**
     * @param iSector  Number of sectors since the start of the video (starts at 0).
     * @param iFrame  Frame number at the sector.
     */
    public boolean matchesNextVideo(int iSector, int iFrame) {

        boolean blnRet = true;

        // if it's a new frame
        if (_iCurFrame != iFrame) {

            BetweenFrames bf = new BetweenFrames(_iLastSect+1, iSector+1, _iCurFrame, iFrame);

            if (_aiPossibleGcds == null) {
                if (_firstBetween == null) {
                    _firstBetween = bf;
                } else {
                    _aiPossibleGcds = bf.generateSectorsPerFrame(_firstBetween);
                }
            } else {
                _aiPossibleGcds = bf.updateSectorsPerFrame(_aiPossibleGcds);
                blnRet = _aiPossibleGcds != null;
            }
        }
        
        _iCurFrame = iFrame;
        _iLastSect = iSector;

        return blnRet;
    }

    public int[] getSectorsPerFrame() {
        return _aiPossibleGcds;
    }

    private static class BetweenFrames {
        private final int _iLastSectorOfPreviousFrame;
        private final int _iFirstSectorOfNewFrame;
        private final int _iPreviousFrame;
        private final int _iNewFrame;

    /**
     * @param iLastSectorOfPreviousFrame  Assumes sector indexing starts at 1!
     * @param iFirstSectorOfNewFrame  Assumes sector indexing starts at 1!
     */
        public BetweenFrames(int iLastSectorOfPreviousFrame,
                             int iFirstSectorOfNewFrame,
                             int iPreviousFrame,
                             int iNewFrame)
        {
            _iLastSectorOfPreviousFrame = iLastSectorOfPreviousFrame;
            _iFirstSectorOfNewFrame = iFirstSectorOfNewFrame;
            _iPreviousFrame = iPreviousFrame;
            _iNewFrame = iNewFrame;
        }

        public int getDifference() {
            return _iFirstSectorOfNewFrame - _iLastSectorOfPreviousFrame;
        }

        public int[] generateSectorsPerFrame(BetweenFrames previous) {
            int[] aiPossibleSectorsPerFrame = new int[
                Math.max(getDifference(), previous.getDifference())
            ];

            int iMin = _iLastSectorOfPreviousFrame - previous._iFirstSectorOfNewFrame + 1;
            int iMax = _iFirstSectorOfNewFrame - previous._iLastSectorOfPreviousFrame - 1;

            int iSectorsPerFrame = iMin;

            for (int i = 0; i < aiPossibleSectorsPerFrame.length; i++) {
                aiPossibleSectorsPerFrame[i] = iSectorsPerFrame;
                iSectorsPerFrame++;
            }
            
            return aiPossibleSectorsPerFrame;
        }
        
        public int[] updateSectorsPerFrame(int[] aiPossibleSectorsPerFrame) {
            int[] aiNewPossibleSectPerFrame = new int[
                getDifference() * aiPossibleSectorsPerFrame.length
            ];

            // run gcd on the old gcds with the new end sectors
            // count the non-one values
            // cross-gcd
            int iNewSectPerFrameIdx = 0;
            for (int iOldSectPerFrameIdx = 0;
                 iOldSectPerFrameIdx < aiPossibleSectorsPerFrame.length;
                 iOldSectPerFrameIdx++)
            {
                for (int iSector = _iLastSectorOfPreviousFrame+1;
                         iSector <= _iFirstSectorOfNewFrame;
                         iSector++)
                {
                    int iGcd = jpsxdec.util.Maths.gcd(
                            aiPossibleSectorsPerFrame[iOldSectPerFrameIdx],
                            iSector);
                    if (iGcd > 1 && 
                        !contains(aiNewPossibleSectPerFrame, iNewSectPerFrameIdx, iGcd))
                    {
                        aiNewPossibleSectPerFrame[iNewSectPerFrameIdx] = iGcd;
                        iNewSectPerFrameIdx++;
                    }
                }
            }

            if (iNewSectPerFrameIdx == 0) {
                // if all gcds are 1, then no more information can be gained from this approach
                return null;
            } else {
                int[] aiTrimmedList = new int[iNewSectPerFrameIdx];
                System.arraycopy(aiNewPossibleSectPerFrame, 0, aiTrimmedList, 0, iNewSectPerFrameIdx);
                return aiTrimmedList;

            }
        }

        private static boolean contains(int[] ai, int iLen, int iVal) {
            for (int i = 0; i < iLen; i++) {
                if (ai[i] == iVal)
                    return true;
            }
            return false;
        }
    }


    //########################################################################//

    public static void main(String[] args) throws IOException {
        String sFile = "fps.txt";
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(sFile)));

        Pattern p = Pattern.compile("\\d+");

        String sLine = r.readLine();
        String[] asParts = Misc.regexAll(p, sLine);
        int iSector = Integer.parseInt(asParts[0]);
        int iFrame = Integer.parseInt(asParts[1]);
        WholeNumberSectorsPerSecond vfr = new WholeNumberSectorsPerSecond(iFrame);

        while ((sLine = r.readLine()) != null) {
            asParts = Misc.regexAll(p, sLine);
            iSector = Integer.parseInt(asParts[0]);
            if (asParts.length > 1) {
                iFrame = Integer.parseInt(asParts[1]);
                boolean ok = vfr.matchesNextVideo(iSector, iFrame);
                System.out.format("Sector %d, Frame %d, ok %b", iSector, iFrame, ok);
                System.out.println();
                if (!ok)
                    break;
            }
        }
        System.out.println(Arrays.toString(vfr.getSectorsPerFrame()) );
        
    }
}
