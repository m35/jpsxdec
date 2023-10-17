/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;


public class XaAudioFormat {

    /** Serialization key for File. */
    private final static String FILE_NUMBER_KEY = "File";

    public final int iFileNumber;

    /** Serialization key for Channel. */
    private final static String CHANNEL_KEY = "Channel";

    /** CD stream channel number.  */
    public final int iChannel;

    /** Serialization key for sample rate. */
    private static final String SAMPLES_PER_SEC_KEY = "Samples/Sec";
    /** Sample rate of the audio stream. Should be either 37800 or 18900. */
    public final int iSampleFramesPerSecond;

    /** Serialization key for bits/sample. */
    private final static String BITSPERSAMPLE_KEY = "Bits/Sample";
    /** ADPCM bits per sample that the audio is encoded as. Should be 4 or 8. */
    public final int iBitsPerSample;

    /** Serialization key for stereo. */
    private static final String STEREO_KEY = "Stereo?";
    /** If the audio is in stereo. */
    public final boolean blnIsStereo;

    public XaAudioFormat(int iFileNumber, int iChannel, int iSampleFramesPerSecond, int iBitsPerSample, boolean blnIsStereo) {
        if (!validChannel(iChannel))
            throw new IllegalArgumentException("Channel " + iChannel + " is not between 0 and 255");
        if (!validFileNumber(iFileNumber))
            throw new IllegalArgumentException("Invalid file number " + iFileNumber);
        if (!validBitsPerSample(iBitsPerSample))
            throw new IllegalArgumentException("Bits/sample " + iBitsPerSample + " is not 4 or 8");
        if (!validSampleFramesPerSecond(iSampleFramesPerSecond))
            throw new IllegalArgumentException();

        this.iFileNumber = iFileNumber;
        this.iChannel = iChannel;
        this.iSampleFramesPerSecond = iSampleFramesPerSecond;
        this.iBitsPerSample = iBitsPerSample;
        this.blnIsStereo = blnIsStereo;
    }

    public XaAudioFormat(@Nonnull SectorXaAudio xa) {
        this(xa.getFileNumber(), xa.getChannel(), xa.getSamplesPerSecond(), xa.getAdpcmBitsPerSample(), xa.isStereo());
    }

    public XaAudioFormat(@Nonnull SerializedDiscItem fields) throws LocalizedDeserializationFail {
        blnIsStereo = fields.getYesNo(STEREO_KEY);
        iSampleFramesPerSecond = fields.getInt(SAMPLES_PER_SEC_KEY);
        iChannel = fields.getInt(CHANNEL_KEY);
        iFileNumber = fields.getInt(FILE_NUMBER_KEY);
        iBitsPerSample = fields.getInt(BITSPERSAMPLE_KEY);

        if (!validChannel(iChannel))
            throw new LocalizedDeserializationFail(I.FIELD_HAS_INVALID_VALUE_NUM(CHANNEL_KEY, iChannel));
        if (!validFileNumber(iFileNumber))
            throw new LocalizedDeserializationFail(I.FIELD_HAS_INVALID_VALUE_NUM(FILE_NUMBER_KEY, iFileNumber));
        if (!validBitsPerSample(iBitsPerSample))
            throw new LocalizedDeserializationFail(I.FIELD_HAS_INVALID_VALUE_NUM(BITSPERSAMPLE_KEY, iBitsPerSample));
        if (!validSampleFramesPerSecond(iSampleFramesPerSecond))
            throw new LocalizedDeserializationFail(I.FIELD_HAS_INVALID_VALUE_NUM(SAMPLES_PER_SEC_KEY, iSampleFramesPerSecond));
    }

    public static boolean validChannel(int iChannel) {
        return iChannel >= 0 && iChannel <= 255; // if the channel i
    }

    public static boolean validFileNumber(int iFileNumber) {
        return iFileNumber >= 0 && iFileNumber <= 255;
    }

    public static boolean validBitsPerSample(int iBitsPerSample) {
        return iBitsPerSample == 4 || iBitsPerSample == 8;
    }

    public static boolean validSampleFramesPerSecond(int iSampleFramesPerSecond) {
        return iSampleFramesPerSecond == 18900 || iSampleFramesPerSecond == 37800;
    }

    public void serialize(@Nonnull SerializedDiscItem fields) {
        fields.addNumber(FILE_NUMBER_KEY, iFileNumber);
        fields.addNumber(CHANNEL_KEY, iChannel);
        fields.addYesNo(STEREO_KEY, blnIsStereo);
        fields.addNumber(SAMPLES_PER_SEC_KEY, iSampleFramesPerSecond);
        fields.addNumber(BITSPERSAMPLE_KEY, iBitsPerSample);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + this.iFileNumber;
        hash = 11 * hash + this.iChannel;
        hash = 11 * hash + this.iSampleFramesPerSecond;
        hash = 11 * hash + this.iBitsPerSample;
        hash = 11 * hash + (this.blnIsStereo ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        XaAudioFormat other = (XaAudioFormat) obj;
        return this.iFileNumber == other.iFileNumber &&
               this.iChannel == other.iChannel &&
               this.iSampleFramesPerSecond == other.iSampleFramesPerSecond &&
               this.iBitsPerSample == other.iBitsPerSample &&
               this.blnIsStereo == other.blnIsStereo;
    }

    public @CheckForNull DiscSpeed calculateDiscSpeed(int iSectorStride) {
        return calculateDiscSpeed(iSampleFramesPerSecond, blnIsStereo, iBitsPerSample, iSectorStride);
    }


    /** Returns null for impossible.
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
    private static @CheckForNull DiscSpeed calculateDiscSpeed(int iSamplesPerSecond,
                                                              boolean blnStereo,
                                                              int iBitsPerSample,
                                                              int iSectorStride)
    {
        if (iSectorStride < 0)
            throw new IllegalArgumentException();
        if (iSectorStride == 0)
            return null;

        int iDiscSpeed_x_16128 = iSamplesPerSecond *
                               (blnStereo ? 2 : 1) *
                                iSectorStride *
                                iBitsPerSample;

        if (iDiscSpeed_x_16128 == 75 * 16128)
            return DiscSpeed.SINGLE; // 1x = 75 sectors/sec
        else if (iDiscSpeed_x_16128 == 150 * 16128)
            return DiscSpeed.DOUBLE; // 2x = 150 sectors/sec
        else
            return null;
    }


}
