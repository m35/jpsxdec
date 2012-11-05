/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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

package jpsxdec.sectors;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.audio.XaAdpcmDecoder;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode;
import jpsxdec.util.ByteArrayFPIS;


/** Standard audio sector for XA. */
public class SectorXaAudio extends IdentifiedSector {

    private static final Logger log = Logger.getLogger(SectorXaAudio.class.getName());

    private int _iSamplesPerSecond;
    private int _iBitsPerSample;
    private boolean _blnStereo;
    private int _iErrors;

    public SectorXaAudio(CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        if (!cdSector.hasSubHeader()) return;
        if (cdSector.subModeMask(SubMode.MASK_FORM | SubMode.MASK_AUDIO) !=
                                (SubMode.MASK_FORM | SubMode.MASK_AUDIO)) return;
        // Ace Combat 3 has several sectors with channel 255
        // They seem to be "null" sectors
        if (cdSector.getSubHeaderChannel() < 0 || cdSector.getSubHeaderChannel() >= 32) return;

        _blnStereo = cdSector.getCodingInfo().isStereo();
        _iSamplesPerSecond = cdSector.getCodingInfo().getSampleRate();
        _iBitsPerSample = cdSector.getCodingInfo().getBitsPerSample();

        // TODO: check Sound Parameters values that the index is valid

        int iErrors = 0;
        int iMaxErrors;
        if (_iBitsPerSample == 4) {
            iMaxErrors = XaAdpcmDecoder.ADPCM_SOUND_GROUPS_PER_SECTOR * 4 * 2;
            for (int iOfs = 0; 
                 iOfs < cdSector.getCdUserDataSize() - XaAdpcmDecoder.SIZE_OF_SOUND_GROUP;
                 iOfs+=XaAdpcmDecoder.SIZE_OF_SOUND_GROUP)
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
                 iOfs < cdSector.getCdUserDataSize() - XaAdpcmDecoder.SIZE_OF_SOUND_GROUP;
                 iOfs+=XaAdpcmDecoder.SIZE_OF_SOUND_GROUP)
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

        _iErrors = iErrors;

        setProbability(100 - iErrors * 100 / iMaxErrors);
        
        if (iErrors > 0) {
            log.log(Level.WARNING, "{0} errors out of {1} in XA sound parameters for {2}", 
                    new Object[]{iErrors, iMaxErrors, this.toString()});
        }
    }

    public boolean isStereo() {
        return _blnStereo;
    }

    public int getBitsPerSample() {
        return _iBitsPerSample;
    }

    public int getSamplesPerSecond() {
        return _iSamplesPerSecond;
    }

    /** The last 20 bytes of the sector are unused. */
    // [extends IdentifiedSector]
    public int getIdentifiedUserDataSize() {
            return super.getCdSector().getCdUserDataSize() - 20;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                0, getIdentifiedUserDataSize());
    }

    
    public String getTypeName() {
        return "XA";
    }

    public long getSampleCount() {
        if (_blnStereo)
            return XaAdpcmDecoder.pcmSamplesGeneratedFromXAADPCMSector(_iBitsPerSample) / 2;
        else
            return XaAdpcmDecoder.pcmSamplesGeneratedFromXAADPCMSector(_iBitsPerSample);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("XA Audio ");
        sb.append(super.toString()).append(' ');
        sb.append(_blnStereo ? "Stereo" : "Mono").append(' ');
        sb.append(_iBitsPerSample).append(" bits/sample ");
        sb.append(_iSamplesPerSecond).append(" samples/sec");
        if (_iErrors > 0)
            sb.append(" {").append(_iErrors).append(" errors}");
        if (isAllQuiet())
            sb.append(" SILENT");
        return sb.toString();
    }

    public boolean matchesPrevious(IdentifiedSector prevSect) {
        if (!(prevSect instanceof SectorXaAudio))
            return false;

        SectorXaAudio prevXA = (SectorXaAudio)prevSect;

        if (getChannel() != prevXA.getChannel() ||
            getBitsPerSample() != prevXA.getBitsPerSample() ||
            getSamplesPerSecond() != prevXA.getSamplesPerSecond() ||
            isStereo() != prevXA.isStereo())
            return false;
        int iStride = getSectorNumber() - prevSect.getSectorNumber();
        if (iStride == 1)
            return true;
        if (calculateDiscSpeed(_iSamplesPerSecond, _blnStereo, _iBitsPerSample, iStride) > 0)
            return true;
        else
            return false;
    }

    /** Checks if the sector doesn't generate any sound. Note that this
     *  does not mean, if the sector was decoded as part of an ADPCM stream,
     *  that it would actually produce only silence. Because ADPCM uses prior
     *  samples to determine the current sample, the first 2 or so samples
     *  might have sound. But after that would be silence. This method is
     *  most reliable if this sector is not associated with any other XA
     *  sectors. In that case it really doesn't generate anything but silence.
     *  */
    public boolean isAllQuiet() {
        for(int iSndGrp = 0, i = 0;
            iSndGrp < XaAdpcmDecoder.ADPCM_SOUND_GROUPS_PER_SECTOR;
            iSndGrp++, i += XaAdpcmDecoder.SIZE_OF_SOUND_GROUP)
        {
            // just check if all ADPCM values are 0
            for (int j = 16; j < XaAdpcmDecoder.SIZE_OF_SOUND_GROUP; j++) {
                if (getCdSector().readUserDataByte(i+j) != 0)
                    return false;
            }
        }
        return true;
    }

    /** Return 1 for 1x, 2 for 2x, or -1 for impossible.
     * <pre>
     * Disc Speed = ( Samples/sec * Mono/Stereo * Stride * Bits/sample ) / 16128
     *
     * Samples/sec  Mono/Stereo  Bits/sample  Stride  Disc Speed
     *   18900           1           4          2      invalid
     *   18900           1           4          4      invalid
     *   18900           1           4          8      invalid
     *   18900           1           4          16       75    "Level C"
     *   18900           1           4          32       150   "Level C"
     *
     *   18900           1           8          2      invalid
     *   18900           1           8          4        150   "Level A"
     *   18900           1           8          8        75    "Level A"
     *   18900           1           8          16     invalid
     *   18900           1           8          32     invalid
     *
     *   18900           2           4          2      invalid
     *   18900           2           4          4      invalid
     *   18900           2           4          8        75    "Level C"
     *   18900           2           4          16       150   "Level C"
     *   18900           2           4          32     invalid
     *
     *   18900           2           8          2      invalid
     *   18900           2           8          4        75    "Level A"
     *   18900           2           8          8        150   "Level A"
     *   18900           2           8          16     invalid
     *   18900           2           8          32     invalid
     *
     *   37800           1           4          2      invalid
     *   37800           1           4          4      invalid
     *   37800           1           4          8        75    "Level B"
     *   37800           1           4          16       150   "Level B"
     *   37800           1           4          32     invalid
     *
     *   37800           1           8          2      invalid
     *   37800           1           8          4        75    "Level A"
     *   37800           1           8          8        150   "Level A"
     *   37800           1           8          16     invalid
     *   37800           1           8          32     invalid
     *
     *   37800           2           4          2      invalid
     *   37800           2           4          4        75    "Level B"
     *   37800           2           4          8        150   "Level B"
     *   37800           2           4          16     invalid
     *   37800           2           4          32     invalid
     *
     *   37800           2           8          2        75    "Level A"
     *   37800           2           8          4        150   "Level A"
     *   37800           2           8          8      invalid
     *   37800           2           8          16     invalid
     *   37800           2           8          32     invalid
     *</pre>*/
    public static int calculateDiscSpeed(int iSamplesPerSecond,
                                         boolean blnStereo, 
                                         int iBitsPerSample,
                                         int iSectorStride)
    {
        if (iSectorStride < 1)
            return -1;

        int iDiscSpeed_x_16128 = iSamplesPerSecond *
                               (blnStereo ? 2 : 1) *
                                iSectorStride *
                                iBitsPerSample;

        if (iDiscSpeed_x_16128 == 75 * 16128)
            return 1; // 1x = 75 sectors/sec
        else if (iDiscSpeed_x_16128 == 150 * 16128)
            return 2; // 2x = 150 sectors/sec
        else
            return -1;
    }

    @Override
    public int getErrorCount() {
        return _iErrors;
    }

    @Override
    public void printErrors(PrintStream ps) {
        ps.println("Sector " + getSectorNumber() + ": " +_iErrors+ " errors in XA sound parameters");
    }


}
