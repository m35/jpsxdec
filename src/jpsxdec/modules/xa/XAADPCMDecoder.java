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

import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import jpsxdec.util.IO;

/** The ultimate PlayStation (and CD-i) XA ADPCM decoder. Based on the code
 * and documentation by Jonathan Atkins (http://freshmeat.net/projects/cdxa/).
 *<p>
 * You cannot decode while reading the data because the data is interleaved.
 * You have to read and decode all of SoundUnit #1 before you can start
 * decoding SoundUnit #2. Once you've read all of sound unit #1, you've already
 * read all the other sound units anyway. So you HAVE to have a buffer for each.
 *<p>
 * However, if there is a read error, this should try to decode everything it
 * has before returning, possibly including any amount of the other Sound Units
 * (even if the ADPCMContext might be messed up a bit). The resulting data will
 * be inaccurate, but it may be better than nothing.
 *
 *<hr>
 * This should be designed in one of 4 ways:
 *<ol>
 *<li>THE DECODER SHOULD BE MINIMAL AND JUST RETURN AN ARRAY OF
 * SHORTS (still interleaved stereo because that shouldn't change, but
 * the return of shorts eliminates the need to know about endian-ness)
 * That array of shorts could then be converted to an AudioInputStream
 * either by first copying to a byte[] array and wrapping with
 * AudioInputStream.
 *<li>MINIMUL DECODER, BUT RETURNS AN ARRAY OF BYTES IN THE CHOSEN ENDIAN ORDER
 *<li>DECODER DOES EVERYTHING AND RETURNS AN AUDIOINPUTSTREAM
 *<li>MAKE IT WORK LIKE THE REST OF THE JAVA AUDIO SYSTEM,
 * AND HAVE THIS CLASS implement TargetDataLine
 *</ol>
 * This has been implemented using the 2nd method.
 */
public abstract class XAADPCMDecoder {

    /**
     * Creates a XA ADPCM decoder for the supplied input format
     * (bits/sample and stereo) and writes the decoded PCM audio in the
     * supplied output format (endian-ness and volume).
     *
     * @param iBitsPerSample  ADPCM bits per sample: either 4 or 8.
     * @param blnIsStereo     true for stereo, false for mono.
     * @param blnBigEndian    true for big-endian, false for little-endian.
     * @param dblVolume       Audio scaled by this amount.
     */
    public static XAADPCMDecoder create(int iBitsPerSample, boolean blnIsStereo,
                                        boolean blnBigEndian, double dblVolume)
    {
        // create one sound group to handle the decoding process
        ADPCMSoundGroupReader soundGroupReader;
        if (iBitsPerSample == 8)
            soundGroupReader = new ADPCMSoundGroupReader.BitsPerSample8();
        else if (iBitsPerSample == 4)
            soundGroupReader = new ADPCMSoundGroupReader.BitsPerSample4();
        else
            throw new IllegalArgumentException("Invalid bits per sample.");

        ShortEndianWriter shortWriter = null;
        if (blnBigEndian)
            shortWriter = new ShortEndianWriter.BE();
        else
            shortWriter = new ShortEndianWriter.LE();

        if (blnIsStereo)
            return new XAADPCMDecoder_Stereo(soundGroupReader, shortWriter, dblVolume);
        else
            return new XAADPCMDecoder_Mono(soundGroupReader, shortWriter, dblVolume);
    }

    /** The number of PCM samples generated by a ADPCM audio sector = 4032. */
    public static final int PCM_SAMPLES_FROM_XA_ADPCM_SECTOR = 4032;

    /** Number of samples generated from an XA ADPCM sector. */
    public static final int BYTES_GENERATED_FROM_XAADPCM_SECTOR =
            PCM_SAMPLES_FROM_XA_ADPCM_SECTOR * 2;

    /** The number of Sound Groups in a ADPCM audio sector = 18. */
    static final int ADPCM_SOUND_GROUPS_PER_SECTOR = 18;

    static final int SIZE_OF_SOUND_GROUP = 128;

    ////////////////////////////////////////////////////////////////////////////

    /** Sound group used to decode all the sound groups per sector. */
    protected final ADPCMSoundGroupReader _soundGroupReader;

    protected final ShortEndianWriter _shortWriter;

    private XAADPCMDecoder(ADPCMSoundGroupReader soundGroupReader, 
                           ShortEndianWriter shortWriter)
    {
        _soundGroupReader = soundGroupReader;
        _shortWriter = shortWriter;
    }

    /** Decodes a sector's worth of ADPCM data.
     *
     * Audio data on the PSX is encoded using
     * Adaptive Differential Pulse Code Modulation (ADPCM).
     *
     * A full sector of 2304 bytes will convert to 4032/channels samples (or 8064 bytes).
     * Returns 4032 samples/channels shorts (or 8064 bytes).
     */
    public void decode(InputStream inStream, byte[] abOutBuffer) throws IOException {
        if (abOutBuffer.length < BYTES_GENERATED_FROM_XAADPCM_SECTOR)
            throw new IllegalArgumentException(
                    "Output buffer size must be at least " + BYTES_GENERATED_FROM_XAADPCM_SECTOR);

        _shortWriter.resetPos();
        /* There are 18 sound groups,
         * each having  16 bytes of interleaved sound parameters,
         *         and 112 bytes of interleaved ADPCM data
         * ( 18*(16+112) = 2304 bytes will be read ) */
        for (int iSoundGroup = 0; iSoundGroup < ADPCM_SOUND_GROUPS_PER_SECTOR; iSoundGroup++) {
            _soundGroupReader.readSoundGroup(inStream);
            ADPCMSoundGroupToPCM(_soundGroupReader, abOutBuffer);
        }
    }

    public boolean isBigEndian() {
        return _shortWriter.isBigEndian();
    }

    /** Finishes converting the ADPCM samples to PCM and writes them to the buffer. */
    abstract protected void ADPCMSoundGroupToPCM(ADPCMSoundGroupReader soundGroup, byte[] abOut);

    /** Returns the volume scale that PCM samples are multiplied by before being clamped. */
    abstract public double getVolume();

    /** Sets the volume scale that PCM samples are multiplied by before being clamped. */
    abstract public void setVolume(double dblVolume);

    /** Returns if the decoder is outputting stereo audio. */
    abstract public boolean isStereo();

    /** Resets the decoding context(s). */
    abstract public void resetContext();

    abstract public AudioFormat getOutputFormat(int iSampleRate);
    
    //########################################################################//
    //## Concrete implementations ############################################//
    //########################################################################//

    /** Mono implementation. */
    private static class XAADPCMDecoder_Mono extends XAADPCMDecoder {

        private final ADPCMContext _monoContext;

        public XAADPCMDecoder_Mono(ADPCMSoundGroupReader soundGroupReader, 
                                   ShortEndianWriter shortWriter,
                                   double dblVolume)
        {
            super(soundGroupReader, shortWriter);
            _monoContext = new ADPCMContext(dblVolume);
        }

        public void resetContext() {
            _monoContext.reset();
        }

        public boolean isStereo() {
            return false;
        }

        public double getVolume() {
            return _monoContext.getVolumeScale();
        }

        public void setVolume(double dblVolume) {
            _monoContext.setVolumeScale(dblVolume);
        }


        public void ADPCMSoundGroupToPCM(ADPCMSoundGroupReader soundGroup, byte[] abOut) {
            for (int iSoundUnitIdx = 0; iSoundUnitIdx < soundGroup.getSoundUnitCount(); iSoundUnitIdx++) {
                ADPCMSoundUnit soundUnit = soundGroup.getSoundUnit(iSoundUnitIdx);
                soundUnit.resetIndex();
                for (int iSample = 0; iSample < ADPCMSoundUnit.SAMPLES_PER_SOUND_UNIT; iSample++) {
                    _shortWriter.write(soundUnit.readPCMSample(_monoContext), abOut);
                }
            }
        }

        public AudioFormat getOutputFormat(int iSampleRate) {
            return new AudioFormat(iSampleRate, 16, 1, true, _shortWriter.isBigEndian());
        }
    }

    //########################################################################//

    /** Stereo implementation. */
    private static class XAADPCMDecoder_Stereo extends XAADPCMDecoder {

        private final ADPCMContext _leftContext;
        private final ADPCMContext _rightContext;

        public XAADPCMDecoder_Stereo(ADPCMSoundGroupReader soundGroupReader,
                                     ShortEndianWriter shortWriter,
                                     double dblVolume)
        {
            super(soundGroupReader, shortWriter);
            _leftContext = new ADPCMContext(dblVolume);
            _rightContext = new ADPCMContext(dblVolume);
        }

        public void resetContext() {
            _leftContext.reset();
            _rightContext.reset();
        }

        public boolean isStereo() {
            return true;
        }

        public double getVolume() {
            // assume same volume for right channel as well (if it has one)
            return _leftContext.getVolumeScale();
        }

        public void setVolume(double dblVolume) {
            _leftContext.setVolumeScale(dblVolume);
            _rightContext.setVolumeScale(dblVolume);
        }

        public void ADPCMSoundGroupToPCM(ADPCMSoundGroupReader soundGroup, byte[] abOut) {
            for (int iSoundUnitIdx = 0; iSoundUnitIdx < soundGroup.getSoundUnitCount(); iSoundUnitIdx+=2) {
                ADPCMSoundUnit leftSoundUnit  = soundGroup.getSoundUnit(iSoundUnitIdx  );
                ADPCMSoundUnit rightSoundUnit = soundGroup.getSoundUnit(iSoundUnitIdx+1);
                leftSoundUnit.resetIndex();
                rightSoundUnit.resetIndex();
                for (int iSample = 0; iSample < ADPCMSoundUnit.SAMPLES_PER_SOUND_UNIT; iSample++) {
                    _shortWriter.write( leftSoundUnit.readPCMSample(_leftContext ), abOut);
                    _shortWriter.write(rightSoundUnit.readPCMSample(_rightContext), abOut);
                }
            }
        }

        public AudioFormat getOutputFormat(int iSampleRate) {
            return new AudioFormat(iSampleRate, 16, 2, true, _shortWriter.isBigEndian());
        }
    }


    /** A sound group has 16 bytes of sound parameters, followed by 112 bytes
     *  of interleaved ADPCM audio data. */
    protected abstract static class ADPCMSoundGroupReader {

        /** Length will be the number of sound units per sound group. */
        protected final ADPCMSoundUnit _aoSoundUnits[];

        /** Initialize with the requested number of sound units (should be 4 or 8). */
        protected ADPCMSoundGroupReader(int iSoundUnitsPerSoundGroup) {
            _aoSoundUnits = new ADPCMSoundUnit[iSoundUnitsPerSoundGroup];
            for (int iSoundUnit = 0; iSoundUnit < _aoSoundUnits.length; iSoundUnit++) {
                _aoSoundUnits[iSoundUnit] = new ADPCMSoundUnit();
            }
        }

        public ADPCMSoundUnit getSoundUnit(int i) {
            return _aoSoundUnits[i];
        }

        public int getSoundUnitCount() {
            return _aoSoundUnits.length;
        }

        /** Reads 16 bytes of sound parameters, followed by
         *  112 bytes of interleaved sound units.
         *  Each sound unit will produce 28 PCM samples. */
        abstract protected void readSoundGroup(InputStream inStream)
                throws IOException;

        ////////////////////////////////////////////////////////////////////////
        // Concrete implementations ////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////
        
        /** {@inheritDoc}
         *
         * When the audio is stored in 8 bits/sample, the audio is interleaved
         * between 4 sound units.
         */
        public static class BitsPerSample8 extends ADPCMSoundGroupReader {

            public BitsPerSample8() {
                // 4 sound units when 8 bits/sample
                super(4);
            }

            /** {@inheritDoc}
             *
             * For 8 bits/sample, there are only 4 sound units that each
             * produce 28 PCM samples.
             */
            protected void readSoundGroup(InputStream inStream)
                    throws IOException
            {
                int iByte;

                // Process the 16 byte sound parameters at the
                // start of each sound group
                // the 4 sound parameters (one for each sound unit)
                // are repeated four times and are ordered like this:
                // 0,1,2,3, 0,1,2,3, 0,1,2,3, 0,1,2,3

                // so just read the first 4 parameters
                for (int i = 0; i < 4; i++) {
                    iByte = inStream.read();
                    if (iByte < 0)
                        throw new EOFException("Unexpected end of audio data");
                    _aoSoundUnits[i].setSoundParamter(iByte);
                    _aoSoundUnits[i].resetIndex();
                }
                // and then skip the rest
                IO.skip(inStream, 12);

                // de-interleave the sound units
                for (int iSampleIdx = 0; 
                     iSampleIdx < ADPCMSoundUnit.SAMPLES_PER_SOUND_UNIT;
                     iSampleIdx++)
                {
                    // read a sample for each of the 4 sound units
                    for (int iSoundUnit = 0; iSoundUnit < 4;  iSoundUnit++)
                    {
                        iByte = inStream.read();
                        if (iByte < 0)
                            throw new EOFException("Unexpected end of audio data");
                        // sound unit bytes are interleaved like this
                        // 0,1,2,3, 0,1,2,3, 0,1,2,3 ...
                        // 1 sample for one sound unit per byte

                        _aoSoundUnits[iSoundUnit].writeADPCMSample(
                            (short)(iByte << 8)); // shift the byte into the top of a short
                    }
                }
            }

        }
        /** {@inheritDoc}
         *
         * When the audio is stored in 4 bits/sample, the audio is interleaved
         * between 8 sound units.
         */
        public static class BitsPerSample4 extends ADPCMSoundGroupReader {

            public BitsPerSample4() {
                // 8 sound units when 4 bits/sample
                super(8);
            }

            /** {@inheritDoc}
             *
             * For 4 bits/sample, there are 8 sound units that each
             * produce 28 PCM samples. */
            protected void readSoundGroup(InputStream inStream)
                    throws IOException
            {
                int iByte;

                // Process the 16 byte sound parameters at the
                // start of each sound group
                // the 8 sound parameters (one for each sound unit)
                // are repeated twice, and are ordered like this:
                // 0,1,2,3, 0,1,2,3, 4,5,6,7, 4,5,6,7

                // so skip the first 4 bytes
                IO.skip(inStream, 4);
                // read the 8 sound parameters
                for (int i = 0; i < 8; i++) {
                    iByte = inStream.read();
                    if (iByte < 0)
                        throw new EOFException("Unexpected end of audio data");
                    _aoSoundUnits[i].setSoundParamter(iByte);
                    // reset the sound group index in the process
                    _aoSoundUnits[i].resetIndex();
                }
                // and skip the last 4 bytes
                IO.skip(inStream, 4);

                // de-interleave the sound units
                for (int iSampleIdx = 0; 
                     iSampleIdx < ADPCMSoundUnit.SAMPLES_PER_SOUND_UNIT;
                     iSampleIdx++)
                {
                    // read a sample for each of the 8 sound units
                    for (int iSoundUnit = 0; iSoundUnit < 8;)
                    {
                        iByte = inStream.read();
                        if (iByte < 0)
                            throw new EOFException("Unexpected end of audio data");
                        
                        // sound unit bytes are interleaved like this
                        // 1/0,3/2,5/4,7/6, 1/0,3/2,5/4,7/6, ...
                        // 1 byte produces two samples, but for different sound units

                        short siADPCMSample;

                        // shift the nibble into the top of a short
                        siADPCMSample = (short)((iByte & 0x0F) << 12);
                        _aoSoundUnits[iSoundUnit].writeADPCMSample(siADPCMSample);
                        iSoundUnit++;
                        // shift the nibble into the top of a short
                        siADPCMSample = (short)((iByte & 0xF0) << 8);
                        _aoSoundUnits[iSoundUnit].writeADPCMSample(siADPCMSample);
                        iSoundUnit++;
                    }
                }
            }
        }

    }

    /** Sound unit found in a sound group. Maintains its ADPCM parameter
     * from the Sound Group parameters, and has a buffer to hold the
     * deinterleaved ADPCM samples for this sound unit. Maintains an index as
     * samples are written to the buffer. Uses the same index as samples are
     * then decoded and read from the buffer.
     *<p>
     * Each Sound Unit generates 28 PCM samples. */
    protected static class ADPCMSoundUnit {

        /** Standard K0 multiplier (don't ask me, it's just how it is). */
        private static final double K0[] = new double[] {
            0.0,
            0.9375,
            1.796875,
            1.53125
        };

        /** Standard K1 multiplier (don't ask me, it's just how it is). */
        private static final double K1[] = new double[] {
            0.0,
            0.0,
            -0.8125,
            -0.859375
        };

        /** The number of ADPCM (as well as PCM) samples per sound unit. */
        public static final int SAMPLES_PER_SOUND_UNIT = 28;

        /** Holds the non-interleaved ADPCM samples for this sound-unit.  */
        private final short[] _asiAdpcmSamples = new short[SAMPLES_PER_SOUND_UNIT];

        /** The 'range' parameter. How many bits to
         * shift the ADPCM sample to the left. */
        private byte _bParameter_Range;
        /** The 'filter' index parameter. Which K0 and K1 table index to
         *  multiply the previous two ADPCM samples by. */
        private byte _bParameter_FilterIndex;
        /** Buffer index. */
        private int _iPos = 0;

        /** @param iSoundParameter  An unsigned byte value holding the range and
         *                          filter parameters for this sound unit. */
        public void setSoundParamter(int iSoundParameter) {
            _bParameter_Range       = (byte)(iSoundParameter & 0xF);
            _bParameter_FilterIndex = (byte)((iSoundParameter >>> 4) & 0x3);
        }

        public void resetIndex() {
            _iPos = 0;
        }

        public void writeADPCMSample(short siSample) {
            _asiAdpcmSamples[_iPos] = siSample;
            _iPos++;
        }

        /** Taking de-interleaved sound unit data, the sound parameter for that
         *  unit, and the decoding context (for the previous 2 samples read),
         *  we can convert an entire sound unit.
         * Converts sound samples as they are read. */
        public short readPCMSample(ADPCMContext context) {
            short siADPCM_sample = _asiAdpcmSamples[_iPos];
            _iPos++;

            // shift sound data according to the range, keeping the sign
            long lngResult = (siADPCM_sample >> _bParameter_Range);

            // adjust according to the filter
            double dblResult = lngResult +
                K0[_bParameter_FilterIndex] * context.getPreviousPCMSample1() +
                K1[_bParameter_FilterIndex] * context.getPreviousPCMSample2();

            // let the context scale, round, and clamp
            // fianlly return the polished sample
            return context.saveScaleRoundClampPCMSample(dblResult);
        }
    }

}
