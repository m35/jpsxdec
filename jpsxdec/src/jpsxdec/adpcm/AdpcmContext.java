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

package jpsxdec.adpcm;

/** Maintains the previous two decoded PCM samples, necessary for ADPCM
 *  decoding. Also scales the volume prior to clamping.
 *  One instance of a context is needed for each audio channel
 *  (one for mono, two for stereo).  */
public class AdpcmContext {

    /* -- Fields ------------------------------------------------------------ */

    /** How much to scale the PCM samples before clamping. */
    private final double _dblVolumeScale;

    /** The previous PCM sample decoded. */
    private double _dblPreviousPCMSample1 = 0;
    /** The second-to-previous PCM sample decoded. */
    private double _dblPreviousPCMSample2 = 0;

    /* -- Constructors ------------------------------------------------------ */

    /** Create new ADPCMDecodingContext.
     * @param dblVolumeScale  Scale the decoded audio before clamping. */
    public AdpcmContext(double dblVolumeScale) {
        _dblVolumeScale = dblVolumeScale;
    }

    /** Create new ADPCMDecodingContext with a 1.0 audio scale. */
    public AdpcmContext() {
        this(1.0);
    }

    /* -- Functions --------------------------------------------------------- */

    public double getVolumeScale() {
        return _dblVolumeScale;
    }

    /** Performs 4 steps:
     * <ol>
     * <li>Saves the unmodified sample as one of the previous 2 samples read.
     * <li>Scales the sample according to the volume scale.
     * <li>Rounds the sample to the nearest integer.
     * <li>Clamps the sample within a signed 16-bit range.
     * </ol>
     * @param dblPCMSample  raw PCM sample, before rounding or clamping.
     * @return The polished PCM sample for saving. */
    public short saveScaleRoundClampPCMSample(double dblPCMSample) {
        // Save the previous sample
        _dblPreviousPCMSample2 = _dblPreviousPCMSample1;
        _dblPreviousPCMSample1 = dblPCMSample;
        // scale, round, and clamp
        long lngSample = jpsxdec.util.Maths.round(dblPCMSample * _dblVolumeScale);
        return clampPCM(lngSample);
    }

    /** Clamps the PCM audio sample within a signed 16-bit value. */
    private short clampPCM(long lngPCMSample) {
        if (lngPCMSample > Short.MAX_VALUE)
            return Short.MAX_VALUE;
        else if (lngPCMSample < Short.MIN_VALUE)
            return Short.MIN_VALUE;
        else
            return (short) lngPCMSample;
    }

    /** The previous PCM sample decoded. */
    public double getPreviousPCMSample1() {
        return _dblPreviousPCMSample1;
    }

    /** The second-to-previous PCM sample decoded. */
    public double getPreviousPCMSample2() {
        return _dblPreviousPCMSample2;
    }

}
