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

import jpsxdec.util.Fraction;

public class AudioSync {

    private final int _iFirstPresentationSector;

    private final int _iSectorsPerSecond;
    private final Fraction _samplesPerSecond;
    
    private final Fraction _samplesPerSector;

    public AudioSync(int iFirstPresentationSector,
                     int iSectorsPerSecond,
                     float fltSamplesPerSecond)
    {
        _iSectorsPerSecond = iSectorsPerSecond;
        _samplesPerSecond = new Fraction(Math.round(fltSamplesPerSecond * 1000), 1000);
        // samples/sector = samples/second / sectors/second
        _samplesPerSector = _samplesPerSecond.divide(_iSectorsPerSecond);

        _iFirstPresentationSector = iFirstPresentationSector;
    }

    public int getSectorsPerSecond() {
        return _iSectorsPerSecond;
    }

    public Fraction getSamplesPerSecond() {
        return _samplesPerSecond;
    }

    public Fraction getSamplesPerSector() {
        return _samplesPerSector;
    }

    public long calculateAudioToCatchUp(int iAudioPresentationSector,
                                       long lngSamplesWritten)
    {
        Fraction presentationTime = new Fraction(iAudioPresentationSector - _iFirstPresentationSector, _iSectorsPerSecond);
        Fraction movieTime = Fraction.divide(lngSamplesWritten, _samplesPerSecond);
        Fraction timeDiff = presentationTime.subtract(movieTime);
        Fraction sampleDiff = timeDiff.multiply(_samplesPerSecond);

        long lngSampleDifference = Math.round(sampleDiff.asDouble());

        return lngSampleDifference;
    }

}
