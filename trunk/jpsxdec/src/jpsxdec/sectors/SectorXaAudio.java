/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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
import jpsxdec.audio.XaAdpcmDecoder;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.XaAnalysis;
import jpsxdec.util.ByteArrayFPIS;


/** Standard audio sector for XA. */
public class SectorXaAudio extends IdentifiedSector {

    /** Maximum channel number (inclusive) that will be considered for
     * XA sector.
     * My understanding is channel is technically supposed to be
     * between 0 and 31. Some games seem to use values outside of that range 
     * for both valid and null XA audio sectors. */
    public static final int MAX_VALID_CHANNEL = 254;
    

    private int _iSamplesPerSecond;
    private int _iBitsPerSample;
    private boolean _blnStereo;
    private int _iErrors;

    public SectorXaAudio(CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        XaAnalysis analysis = XaAnalysis.analyze(cdSector, MAX_VALID_CHANNEL);
        if (analysis == null) return;

        _blnStereo = analysis.blnStereo;
        _iSamplesPerSecond = analysis.iSamplesPerSecond;
        _iBitsPerSample = analysis.iBitsPerSample;

        _iErrors = analysis.iErrors;

        setProbability(analysis.iProbability);        
    }

    public int getChannel() {
        return getCdSector().getSubHeaderChannel();
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
            return XaAdpcmDecoder.pcmSamplesGeneratedFromXaAdpcmSector(_iBitsPerSample) / 2;
        else
            return XaAdpcmDecoder.pcmSamplesGeneratedFromXaAdpcmSector(_iBitsPerSample);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("XA Audio ");
        sb.append(super.toString()).append(' ');
        sb.append(_blnStereo ? "Stereo" : "Mono").append(' ');
        sb.append(_iBitsPerSample).append(" bits/sample ");
        sb.append(_iSamplesPerSecond).append(" samples/sec");
        if (_iErrors > 0)
            sb.append(" {").append(_iErrors).append(" errors}");
        if (isSilent())
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
    public boolean isSilent() {
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
