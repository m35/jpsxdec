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

package jpsxdec;

import jpsxdec.plugins.xa.AudioSync;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import jpsxdec.plugins.xa.IAudioReceiver;

/** Wraps SourceDataLine with my AudioOutputStream interface. */
public class SourceDataLineAudioReceiver implements IAudioReceiver {

    private final SourceDataLine _dataLine;
    private AudioSync _audioSync;

    private final int _iFrameSize;
    private byte[] _abZeroBuff;

    public SourceDataLineAudioReceiver(SourceDataLine dataLine, int iSectorsPerSecond, int iMovieStartSector) {
        _dataLine = dataLine;
        _iFrameSize = _dataLine.getFormat().getFrameSize();

        _audioSync = new AudioSync(iMovieStartSector, iSectorsPerSecond, _dataLine.getFormat().getSampleRate());
    }

    public void close() throws IOException {
        _dataLine.close();
    }

    public AudioFormat getFormat() {
        return _dataLine.getFormat();
    }

    public void write(AudioFormat inFormat, byte[] abData, int iOffset, int iLength, int iEndingSector) throws IOException {
        if (!inFormat.matches(_dataLine.getFormat()))
            throw new IllegalArgumentException("Incompatable audio format.");

        long lngSampleDiff = _audioSync.calculateNeededSilence(iEndingSector, iLength / _iFrameSize);

        if (lngSampleDiff > 0) {
            System.out.println("Audio out of sync " + lngSampleDiff + " samples, adding silence.");
            if (_abZeroBuff == null)
                _abZeroBuff = new byte[_iFrameSize * 2048];
            long lngBytesToWrite = lngSampleDiff * _iFrameSize;
            while (lngBytesToWrite > 0) {
                lngBytesToWrite -= _dataLine.write(_abZeroBuff, 0, (int)Math.min(lngBytesToWrite, _abZeroBuff.length));
            }
        }

        _dataLine.write(abData, iOffset, iLength);
    }
}
