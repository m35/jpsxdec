/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2023  Michael Sabin
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

package jpsxdec.cdreaders;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.adpcm.XaAdpcmDecoder;

/** Analyzes CD sectors to determine if they are XA audio sectors.
 * XA audio is part of the "Green Book" standard, so should be part of the
 * CD package.  */
public class XaAnalysis {

    private static final Logger LOG = Logger.getLogger(XaAnalysis.class.getName());

    public final int iSamplesPerSecond;
    public final int iBitsPerSample;
    public final boolean blnStereo;
    public final int iErrors;
    public final int iProbability;

    private XaAnalysis(int iSamplesPerSecond, int iBitsPerSample,
                       boolean blnStereo, int iErrors, int iProbability)
    {
        this.iSamplesPerSecond = iSamplesPerSecond;
        this.iBitsPerSample = iBitsPerSample;
        this.blnStereo = blnStereo;
        this.iErrors = iErrors;
        this.iProbability = iProbability;
    }


    /** Analyzes a CD sector to determine if it is a XA audio sector.
     * @return null if definitely not a XA audio sector. */
    public static @CheckForNull XaAnalysis analyze(@Nonnull CdSector cdSector) {
        if (cdSector.getType() != CdSector.Type.MODE2FORM2)
            return null;

        CdSectorXaSubHeader sh = cdSector.getSubHeader();
        if (sh == null)
            return null;

        if (sh.getSubMode().mask(CdSectorXaSubHeader.SubMode.MASK_FORM  |
                                 CdSectorXaSubHeader.SubMode.MASK_AUDIO |
                                 CdSectorXaSubHeader.SubMode.MASK_DATA  |
                                 CdSectorXaSubHeader.SubMode.MASK_VIDEO |
                                 CdSectorXaSubHeader.SubMode.MASK_REAL_TIME)
                                 !=
                                (CdSectorXaSubHeader.SubMode.MASK_FORM  |
                                 CdSectorXaSubHeader.SubMode.MASK_AUDIO |
                                 CdSectorXaSubHeader.SubMode.MASK_REAL_TIME))
        {
            return null;
        }

        if (sh.getChannel() < 0 || sh.getChannel() > 255)
            throw new RuntimeException("This should never happen");

        boolean blnStereo = sh.getCodingInfo().isStereo();
        int iSamplesPerSecond = sh.getCodingInfo().getSamplesPerSecond();
        int iBitsPerSample = sh.getCodingInfo().getBitsPerSample();

        // TODO: check Sound Parameters values that the index is valid

        int iErrors = 0;
        int iMaxErrors;
        if (iBitsPerSample == 4) {
            iMaxErrors = XaAdpcmDecoder.ADPCM_SOUND_GROUPS_PER_SECTOR * 4 * 2;
            for (int iOfs = 0;
                 iOfs < cdSector.getCdUserDataSize() - XaAdpcmDecoder.SIZEOF_SOUND_GROUP;
                 iOfs+=XaAdpcmDecoder.SIZEOF_SOUND_GROUP)
            {
                // the 8 sound parameters (one for each sound unit)
                // are repeated twice, and are ordered like this:
                // 0,1,2,3, 0,1,2,3, 4,5,6,7, 4,5,6,7
                for (int i = 0; i < 4; i++) {
                    if (cdSector.readUserDataByte(iOfs + i) != cdSector.readUserDataByte(iOfs + 4 + i))
                        iErrors++;
                    if (cdSector.readUserDataByte(iOfs + 8 + i) != cdSector.readUserDataByte(iOfs + 12 + i))
                        iErrors++;
                }
            }
        } else {
            iMaxErrors = XaAdpcmDecoder.ADPCM_SOUND_GROUPS_PER_SECTOR * 4 * 3;
            for (int iOfs = 0;
                 iOfs < cdSector.getCdUserDataSize() - XaAdpcmDecoder.SIZEOF_SOUND_GROUP;
                 iOfs+=XaAdpcmDecoder.SIZEOF_SOUND_GROUP)
            {
                // the 4 sound parameters (one for each sound unit)
                // are repeated four times and are ordered like this:
                // 0,1,2,3, 0,1,2,3, 0,1,2,3, 0,1,2,3
                for (int i = 0; i < 4; i++) {
                    byte b = cdSector.readUserDataByte(iOfs + i);
                    if (b != cdSector.readUserDataByte(iOfs + 4 + i))
                        iErrors++;
                    if (b != cdSector.readUserDataByte(iOfs + 8 + i))
                        iErrors++;
                    if (b != cdSector.readUserDataByte(iOfs + 12 + i))
                        iErrors++;
                }
            }
        }

        int iProbability = 100 - iErrors * 100 / iMaxErrors;

        if (iErrors > 0) {
            LOG.log(Level.WARNING, "{0,number,#} errors out of {1,number,#} in XA sound parameters for {2}",
                    new Object[]{iErrors, iMaxErrors, cdSector});
        }

        return new XaAnalysis(iSamplesPerSecond, iBitsPerSample, blnStereo, iErrors, iProbability);
    }

}
