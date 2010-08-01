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
import java.util.List;
import javax.sound.sampled.AudioFormat;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.Maths;


public class AudioStreamsCombiner implements IAudioSectorDecoder {
    private boolean _blnIsStereo;
    private int _iSampleRate;

    private final AudioFormat _outFormat;
    private int _iStartSector;
    private int _iEndSector;
    private int _iPresStartSector;

    private byte[] _abNormalizedBuffer;

    private final IAudioSectorDecoder[] _aoDecoders;

    private NormalizingOutputStream _normal;
    private IAudioReceiver _feedOut;

    public AudioStreamsCombiner(List<DiscItemAudioStream> audStreams,
                                 boolean blnBigEndian, double dblVolume)
    {

        ensureNotOverlap(audStreams);

        _aoDecoders = new IAudioSectorDecoder[audStreams.size()];

        _blnIsStereo = audStreams.get(0).isStereo();
        _iSampleRate = audStreams.get(0).getSampleRate();
        _iStartSector = audStreams.get(0).getStartSector();
        _iEndSector = audStreams.get(0).getEndSector();
        _iPresStartSector = audStreams.get(0).getPresentationStartSector();
        for (int i = 0; i < _aoDecoders.length; i++) {
            DiscItemAudioStream aud = audStreams.get(i);
            _aoDecoders[i] = aud.makeDecoder(blnBigEndian, dblVolume);
            _blnIsStereo = _blnIsStereo || aud.isStereo();
            _iSampleRate = Maths.gcd(_iSampleRate, aud.getSampleRate());
            _iStartSector = Math.min(_iStartSector, aud.getStartSector());
            _iEndSector = Math.max(_iEndSector, aud.getEndSector());
            _iPresStartSector = Math.min(_iPresStartSector, aud.getPresentationStartSector());
        }

         _outFormat = new AudioFormat(_iSampleRate, 16, _blnIsStereo ? 2 : 1,
                                      true, blnBigEndian);
    }

    private static void ensureNotOverlap(List<DiscItemAudioStream> audStreams) {
        for (int i = 0; i < audStreams.size(); i++) {
            for (int j = i+1; j < audStreams.size(); j++) {
                if (audStreams.get(i).overlaps(audStreams.get(j)))
                    throw new IllegalArgumentException("Streams are not mutually exclusive.");
            }
        }
    }

    public void open(IAudioReceiver audioOut) {

        _feedOut = audioOut;
        _normal = new NormalizingOutputStream();

         for (IAudioSectorDecoder decoder : _aoDecoders) {
             if (decoder.getOutputFormat().matches(_outFormat))
                decoder.open(_feedOut);
             else
                decoder.open(_normal);
         }
    }

    public double getVolume() {
        // assume the volume is the same for all decoders
        return _aoDecoders[0].getVolume();
    }

    public void setVolume(double dblVolume) {
        for (IAudioSectorDecoder decoder : _aoDecoders) {
            decoder.setVolume(dblVolume);
        }
    }

    public AudioFormat getOutputFormat() {
        return _outFormat;
    }

    public void reset() {
        for (IAudioSectorDecoder decoder : _aoDecoders) {
            decoder.reset();
        }
    }

    public int getStartSector() {
        return _iStartSector;
    }

    public int getEndSector() {
        return _iEndSector;
    }

    public int getPresentationStartSector() {
        return _iPresStartSector;
    }

    private IAudioSectorDecoder pickDecoder(int iSector) {
        for (IAudioSectorDecoder decoder : _aoDecoders) {
            if (decoder.getStartSector() <= iSector &&
                iSector <= decoder.getEndSector())
                return decoder;
        }
        return null;
    }

    public void feedSector(IdentifiedSector sector) throws IOException {
        IAudioSectorDecoder decoder = pickDecoder(sector.getSectorNumber());
        if (decoder == null)
            return;

        if (decoder.getOutputFormat().matches(_outFormat))
            decoder.feedSector(sector);
        else {
            if (_normal == null)
                _normal = new NormalizingOutputStream();

            decoder.feedSector(sector);
        }
    }


    private class NormalizingOutputStream implements IAudioReceiver {

        public void close() throws IOException {
            throw new UnsupportedOperationException("I hope this never happens.");
        }

        public void write(AudioFormat inFormat, byte[] abIn, int iInOfs, int iInLen, int iPresentationSector) throws IOException {

            boolean blnInIsStereo = inFormat.getChannels() == 2 ? true : false;
            int iInSampleRate = (int)inFormat.getSampleRate();

            if (_iSampleRate < iInSampleRate)
                throw new IllegalArgumentException("Unable to downsample.");
            if (_iSampleRate % iInSampleRate != 0)
                throw new IllegalArgumentException("Unable to upsample non multiple rate.");
            if (_blnIsStereo != blnInIsStereo && blnInIsStereo)
                throw new IllegalArgumentException("Unable to downsample to mono.");

            boolean blnStereoize = _blnIsStereo != blnInIsStereo;

            int iSizeMultiple = _iSampleRate / iInSampleRate;
            if (blnStereoize)
                iSizeMultiple *= 2;

            int iOutLen = iInLen * iSizeMultiple;
            if (_abNormalizedBuffer == null || _abNormalizedBuffer.length < iOutLen)
                _abNormalizedBuffer = new byte[iOutLen];

            int iInSampleSize = blnInIsStereo ? 4 : 2;
            if (iInLen % iInSampleSize != 0)
                throw new IllegalArgumentException("Input size is not a multiple of sample size.");
            int iSampleCount = iInLen / iInSampleSize;

            int iOutOfs = 0;
            for (int iSampleIdx = 0; iSampleIdx < iSampleCount; iSampleIdx++) {
                for (int iScaleIdx = 0; iScaleIdx < iSizeMultiple; iScaleIdx++) {
                    for (int iCopyIdx = 0; iCopyIdx < iInSampleSize; iCopyIdx++) {
                        _abNormalizedBuffer[iOutOfs] = abIn[iInOfs+iCopyIdx];
                        iOutOfs++;
                    }
                }
                iInOfs += iInSampleSize;
            }

            _feedOut.write(_outFormat, _abNormalizedBuffer, 0, iOutLen, iPresentationSector);
        }

    }

}
