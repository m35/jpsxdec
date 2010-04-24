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

package jpsxdec.modules.xa;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.JPSXModule;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSector.CDXAHeader.SubMode.DATA_AUDIO_VIDEO;
import jpsxdec.modules.DiscItem;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** Standard audio sector for XA. */
public class SectorXA extends IdentifiedSector
{

    private final int _iSamplesPerSecond;
    private final int _iBitsPerSample;
    private final boolean _blnStereo;

    public SectorXA(CDSector cdSector) throws NotThisTypeException {
        super(cdSector);

        if (!cdSector.hasSectorHeader())
            throw new NotThisTypeException();
        if (cdSector.getSubMode().getForm() != 2)
            throw new NotThisTypeException();
        if (cdSector.getSubMode().getDataAudioVideo() != DATA_AUDIO_VIDEO.AUDIO)
            throw new NotThisTypeException();
        // Ace Combat 3 has several sectors with channel 255
        // They seem to be "null" sectors
        if (cdSector.getChannel() < 0 || cdSector.getChannel() >= 32)
            throw new NotThisTypeException();

        int iErrors = 0;
        _iBitsPerSample = cdSector.getCodingInfo().getBitsPerSample();
        if (_iBitsPerSample == 4) {
            for (int iOfs = 0; 
                 iOfs < cdSector.getCdUserDataSize() - XAADPCMDecoder.SIZE_OF_SOUND_GROUP;
                 iOfs+=XAADPCMDecoder.SIZE_OF_SOUND_GROUP)
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
            for (int iOfs = 0;
                 iOfs < cdSector.getCdUserDataSize() - XAADPCMDecoder.SIZE_OF_SOUND_GROUP;
                 iOfs+=XAADPCMDecoder.SIZE_OF_SOUND_GROUP)
            {
                // the 4 sound parameters (one for each sound unit)
                // are repeated four times and are ordered like this:
                // 0,1,2,3, 0,1,2,3, 0,1,2,3, 0,1,2,3
                for (int i = 0; i < 4; i++) {
                    if (cdSector.readUserDataByte(iOfs + i) != cdSector.readUserDataByte(iOfs + 4 + i))
                        iErrors++;
                    if (cdSector.readUserDataByte(iOfs + i) != cdSector.readUserDataByte(iOfs + 8 + i))
                        iErrors++;
                    if (cdSector.readUserDataByte(iOfs + i) != cdSector.readUserDataByte(iOfs + 12 + i))
                        iErrors++;
                }
            }
        }
        if (iErrors > 2)
            throw new NotThisTypeException();

        _blnStereo = cdSector.getCodingInfo().isStereo();
        _iSamplesPerSecond = cdSector.getCodingInfo().getSampleRate();
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

    /** The last 20 bytes of the sector are unused.
     *  [extends IdentifiedSector] */
    public int getIdentifiedUserDataSize() {
            return super.getCDSector().getCdUserDataSize() - 20;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCDSector().getCDUserDataStream(), 
                0, getIdentifiedUserDataSize());
    }

    
    public int getSectorType() {
        return SECTOR_AUDIO;
    }
    
    public String getTypeName() {
        return "XA";
    }

    public long getSampleCount() {
        if (_blnStereo)
            return XAADPCMDecoder.PCM_SAMPLES_FROM_XA_ADPCM_SECTOR / 2;
        else
            return XAADPCMDecoder.PCM_SAMPLES_FROM_XA_ADPCM_SECTOR;
    }

    public String toString() {
        return String.format("XA Audio %s %s %d bits/sample %d samples/sec",
               super.toString(),
               _blnStereo ? "Stereo" : "Mono",
               _iBitsPerSample,
               _iSamplesPerSecond);
    }

    public SectorXA matchesPrevious(IdentifiedSector prevSect) {
        if (!(prevSect instanceof SectorXA))
            return null;

        SectorXA prevXA = (SectorXA)prevSect;

        if (getChannel() != prevXA.getChannel() ||
            getBitsPerSample() != prevXA.getBitsPerSample() ||
            getSamplesPerSecond() != prevXA.getSamplesPerSecond() ||
            isStereo() != prevXA.isStereo())
            return null;
        int iStride = getSectorNumber() - prevSect.getSectorNumber();
        if (calculateDiscSpeed(_iSamplesPerSecond, _blnStereo, iStride) > 0)
            return prevXA;
        else
            return null;
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
        InputStream is = getIdentifiedUserDataStream();
        try {
            for(int iSndGrp = 0;
                iSndGrp < XAADPCMDecoder.ADPCM_SOUND_GROUPS_PER_SECTOR;
                iSndGrp++)
            {
                IO.skip(is, 16); // skip the sound parameters
                // just check if all ADPCM values are 0
                for (int j = 16; j < XAADPCMDecoder.SIZE_OF_SOUND_GROUP; j++) {
                    if (is.read() != 0)
                        return false;
                }
            }
        } catch (IOException ex) {}
        return true;
    }

    /**<pre>
     * Disc Speed = ( Samples/sec * Mono/Stereo * Period ) / 4032
     *
     * Samples/sec  Mono/Stereo  Bits/sample  Period  Disc Speed
     *   18900           1           4          4      invalid
     *   18900           1           4          8      invalid
     *   18900           1           4          16       75    "Level C"
     *   18900           1           4          32       150   "Level C"
     *   18900           1           8          4      invalid
     *   18900           1           8          8      invalid
     *   18900           1           8          16       75    "Level A"
     *   18900           1           8          32       150   "Level A"
     *   18900           2           4          4      invalid
     *   18900           2           4          8        75    "Level C"
     *   18900           2           4          16       150   "Level C"
     *   18900           2           4          32     invalid
     *   18900           2           8          4      invalid
     *   18900           2           8          8        75    "Level A"
     *   18900           2           8          16       150   "Level A"
     *   18900           2           8          32     invalid
     *   37800           1           4          4      invalid
     *   37800           1           4          8        75    "Level B"
     *   37800           1           4          16       150   "Level B"
     *   37800           1           4          32     invalid
     *   37800           1           8          4      invalid
     *   37800           1           8          8        75    "Level A"
     *   37800           1           8          16       150   "Level A"
     *   37800           1           8          32     invalid
     *   37800           2           4          4        75    "Level B"
     *   37800           2           4          8        150   "Level B"
     *   37800           2           4          16     invalid
     *   37800           2           4          32     invalid
     *   37800           2           8          4        75    "Level A"
     *   37800           2           8          8        150   "Level A"
     *   37800           2           8          16     invalid
     *   37800           2           8          32     invalid
     *</pre>*/
    public static int calculateDiscSpeed(int iSamplesPerSecond,
                                         boolean blnStereo, int iSectorStride)
    {
        if (iSectorStride < 1)
            return -1;

        int iDiscSpeed_x_4032 = iSamplesPerSecond *
                               (blnStereo ? 2 : 1) *
                                iSectorStride;

        if (iDiscSpeed_x_4032 == 75 * 4032)
            return 1; // 1x = 75 sectors/sec
        else if (iDiscSpeed_x_4032 == 150 * 4032)
            return 2; // 2x = 150 sectors/sec
        else
            return -1;
    }

    public DiscItem createMedia(int iStartSector, int iPeriod, long lngSampleCount) {
        return new DiscItemXAAudioStream(
                iStartSector, getSectorNumber(),
                getChannel(),
                lngSampleCount,
                getSamplesPerSecond(),
                isStereo(), getBitsPerSample(),
                iPeriod);
    }

    public JPSXModule getSourceModule() {
        return JPSXModuleXAAudio.getModule();
    }
}
