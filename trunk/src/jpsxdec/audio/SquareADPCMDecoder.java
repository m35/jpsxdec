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

package jpsxdec.audio;

import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import jpsxdec.util.ByteArrayFPIS;

/** Decodes Square's unique ADPCM audio data. Confirmed used in FF8, FF9,
 *  and Chrono Cross. This is the simplest and most straight-forward
 *  implementation I could think of. A fixed point implementation
 *  would of course be faster. */
public final class SquareADPCMDecoder {

    private static final Logger log = Logger.getLogger(SquareADPCMDecoder.class.getName());

    private final SoundUnit _leftSoundUnit;
    private final SoundUnit _rightSoundUnit;

    private final ShortEndianWriter _decodeBuffer;
    
    public SquareADPCMDecoder(boolean blnBigEndian, double dblVolume) {
        _leftSoundUnit = new SoundUnit(dblVolume);
        _rightSoundUnit = new SoundUnit(dblVolume);

        // create the output buffer depending on the how much
        // data will be decoded and the desired endian-ness
        if (blnBigEndian)
            _decodeBuffer = new ShortEndianWriter.BE();
        else
            _decodeBuffer = new ShortEndianWriter.LE();
    }

    public static int calculateOutputBufferSize(int iInputBytes) {
        if ((iInputBytes % SoundUnit.SIZE_IN_BYTES) > 0)
            throw new IllegalArgumentException("iInputBytes must be divisible by " + SoundUnit.SIZE_IN_BYTES);

        // each sound unit takes 16 bytes
        int iSoundUnitCount = iInputBytes / SoundUnit.SIZE_IN_BYTES;

        return iSoundUnitCount * SoundUnit.SAMPLES_PER_SOUND_UNIT * 2 * 2;
    }
    
    /** Decodes a requested amount of audio data from the input stream. 
     *  Output buffer must be big enough to fit the decoded data.
     *  The necessary size can be determined using
     *  {@link #calculateOutputBufferSize(int)}.
     */
    public int decode(InputStream leftStream,
                       InputStream rightStream,
                       int iNumBytes,
                       byte[] abOutBuffer)
            throws IOException
    {
        if ((iNumBytes % SoundUnit.SIZE_IN_BYTES) > 0)
            throw new IllegalArgumentException("iNumBytes must be divisible by " + SoundUnit.SIZE_IN_BYTES);

        // each sound unit takes 16 bytes
        int iSoundUnitCount = iNumBytes / SoundUnit.SIZE_IN_BYTES;

        int iOutputSize = iSoundUnitCount * SoundUnit.SAMPLES_PER_SOUND_UNIT * 2 * 2;

        if (abOutBuffer.length < iOutputSize)
            throw new IllegalArgumentException("Out buffer size not big enough.");

        _decodeBuffer.resetPos();

        for (int iSoundUnitIdx = 0; iSoundUnitIdx < iSoundUnitCount; iSoundUnitIdx++)
        {
            short[] asiLeftSamples = _leftSoundUnit.readSoundUnit(leftStream);
            short[] asiRightSamples = _rightSoundUnit.readSoundUnit(rightStream);

            for (int iSampleIdx = 0; iSampleIdx < SoundUnit.SAMPLES_PER_SOUND_UNIT; iSampleIdx++) {
                _decodeBuffer.write(asiLeftSamples[iSampleIdx], abOutBuffer);
                _decodeBuffer.write(asiRightSamples[iSampleIdx], abOutBuffer);
            }
        }
        return iOutputSize;
    }
    
    public void resetContext() {
        _leftSoundUnit.resetContext();
        _rightSoundUnit.resetContext();
    }

    public void setVolume(double dblVolume) {
        _leftSoundUnit.setVolume(dblVolume);
        _rightSoundUnit.setVolume(dblVolume);
    }

    public double getVolume() {
        // assume both are the same
        return _leftSoundUnit.getVolume();
    }
    
    
    private static class SoundUnit {
        public static final int SAMPLES_PER_SOUND_UNIT = 28;
        // sound units take up 16 bytes
        // 2 bytes for the sound parameter (only 1 actually has value)
        // 14 bytes of sound samples
        public static final int SIZE_IN_BYTES = 16;
        
        private byte _bParameter_Range;
        private byte _bParameter_FilterIndex;
        private final ADPCMContext _adpcmContext;
        private final short[] _asiPcmSampleBuffer = new short[SAMPLES_PER_SOUND_UNIT];

        public SoundUnit(double dblVolume) {
            _adpcmContext = new ADPCMContext(dblVolume);
        }

        private void readSoundParamter(InputStream inStream) throws IOException
        {
            int iSoundParameter = inStream.read();
            if (iSoundParameter < 0)
                throw new EOFException("Unexpected end of audio data");
            
            _bParameter_Range       = (byte)(iSoundParameter & 0xF);
            _bParameter_FilterIndex = (byte)((iSoundParameter >>> 4) & 0xF);

            if (_bParameter_FilterIndex > 4) {
                // TODO: show some kind of user visible message that audio has been compromised
                StringBuilder sb = new StringBuilder("Square ADPCM Sound Parameter Filter Index > 4 (");
                sb.append(_bParameter_FilterIndex);
                sb.append(") [sound parameter 0x");
                sb.append(Integer.toHexString(iSoundParameter));
                if (inStream instanceof ByteArrayFPIS) {
                    sb.append(" at ");
                    sb.append(((ByteArrayFPIS)inStream).getFilePointer()-1);
                }
                sb.append("]");
                log.warning(sb.toString());
                _bParameter_FilterIndex = (byte)(_bParameter_FilterIndex & 3);
            }
            
            // for some reason this byte isn't used?
            if (inStream.skip(1) != 1)
                throw new EOFException("Unexpected end of audio data");
        }
        
        /** Square's K0 multiplier (don't ask me, it's just how it is). */
        private final static double K0[] = new double[] {
            0.0,
            0.9375,
            1.796875,
            1.53125,
            1.90625  // one more possible value than standard XA audio
        };
        /** Square's K1 multiplier (don't ask me, it's just how it is). */
        private final static double K1[] = new double[] {
            0.0,
            0.0,
            -0.8125,
            -0.859375,
            -0.9375  // one more possible value than standard XA audio
        };

        public short[] readSoundUnit(InputStream inStream) throws IOException
        {
            readSoundParamter(inStream);

            for (int i = 0; i < SAMPLES_PER_SOUND_UNIT;) {
                // read a byte
                int iByte = inStream.read();
                if (iByte < 0)
                    throw new EOFException("Unexpected end of audio data");
                
                // get the two ADPCM samples from it
                // shift the nibble into the top of a short
                _asiPcmSampleBuffer[i  ] = (short)((iByte & 0x0F) << 12);
                _asiPcmSampleBuffer[i+1] = (short)((iByte & 0xF0) <<  8);
                
                for (int j=0; j<2; j++, i++) {
                    // shift sound data according to the range, keeping the sign
                    long lngResult = (_asiPcmSampleBuffer[i] >> _bParameter_Range);

                    // adjust according to the filter
                    double dblResult =
                        (lngResult + K0[_bParameter_FilterIndex] * _adpcmContext.getPreviousPCMSample1()
                                   + K1[_bParameter_FilterIndex] * _adpcmContext.getPreviousPCMSample2());

                    _asiPcmSampleBuffer[i] = _adpcmContext.saveScaleRoundClampPCMSample(dblResult);
                }
            }
            
            return _asiPcmSampleBuffer;
        }

        public void resetContext() {
            _adpcmContext.reset();
        }

        public double getVolume() {
            return _adpcmContext.getVolumeScale();
        }

        public void setVolume(double dblVolume) {
            _adpcmContext.setVolumeScale(dblVolume);
        }
    }

    public AudioFormat getOutputFormat(int iSampleRate) {
        return new AudioFormat(iSampleRate, 16, 2, true, _decodeBuffer.isBigEndian());
    }

}
