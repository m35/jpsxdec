/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2020  Michael Sabin
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

package jpsxdec.modules.policenauts;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.adpcm.SoundUnitDecoder;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/** Policenauts FMVs consist of several packets of data.
 * All the packets have a type identifier, and all the identifiers start with
 * the letter 'S'. */
public class SPacket {

    public static final int AUDIO_SAMPLE_FRAMES_PER_SECOND = 44100; // confirmed
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(AUDIO_SAMPLE_FRAMES_PER_SECOND, 16, 1, true, false);
    public static final Fraction AUDIO_SAMPLE_FRAMES_PER_TIMESTAMP =
            new Fraction(16384 / SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT * SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT, 156);
    public static final Fraction TIMESTAMP_UNITS_PER_SECOND = Fraction.divide(AUDIO_SAMPLE_FRAMES_PER_SECOND, AUDIO_SAMPLE_FRAMES_PER_TIMESTAMP);
    public static final Fraction SECONDS_PER_TIMESTAMP = Fraction.divide(1, TIMESTAMP_UNITS_PER_SECOND);

    public static final int TIMESTAMP_UNITS_PER_FRAME = 20;
    public static final Fraction FRAMES_PER_SECOND = TIMESTAMP_UNITS_PER_SECOND.divide(TIMESTAMP_UNITS_PER_FRAME);

    public static final Fraction SECTORS150_PER_FRAME = Fraction.divide(150, FRAMES_PER_SECOND);
    public static final Fraction SECTORS150_PER_TIMESTAMP = SECONDS_PER_TIMESTAMP.multiply(150);

    public enum Type {
        /** SPU ADPCM. */
        SDNSSDTS("SDNSSDTS (audio)"),
        /** Frame bitstream. */
        SCIPPDTS("SCIPPDTS (video)"),
        SDNSHDTS,
        SCTELLEC,
        SCTEGOLD,
        SCTEGLEC,
        SCTEMLEC;

        @CheckForNull
        private final String _sToString;

        private Type() { _sToString = null; }
        private Type(String sToString) { _sToString = sToString; }

        @Override
        public String toString() {
            return _sToString == null ? super.toString() : _sToString;
        }
    }

    public static final int SIZEOF = 48;

    // Zeroes                       // 8 bytes  @ 0
    @Nonnull
    private final Type _type;       // 8 bytes  @ 8
    private final int _iTimestamp;  // 4 bytes  @ 16
    private final int _iDuration;   // 4 bytes  @ 20
    private final int _iOffset;     // 4 bytes  @ 24
    private final int _iSize;       // 4 bytes  @ 28
    // Zeroes                       // 16 bytes @ 32

    public SPacket(@Nonnull InputStream is) throws EOFException, IOException, BinaryDataNotRecognized {

        long lng8Zeroes = IO.readSInt64BE(is);
        if (lng8Zeroes != 0)
            throw new BinaryDataNotRecognized();

        byte[] abTag = new byte[8];
        IO.readByteArray(is, abTag);
        try {
            _type = Type.valueOf(Misc.asciiToString(abTag));
        } catch (IllegalArgumentException ex) {
            throw new BinaryDataNotRecognized(ex);
        }
        _iTimestamp = IO.readSInt32LE(is);
        if (_iTimestamp < 0 || _iTimestamp > 63780)
            throw new BinaryDataNotRecognized();
        _iDuration = IO.readSInt32LE(is);
        if (_iDuration < 0 || _iDuration > 156)
            throw new BinaryDataNotRecognized();
        _iOffset = IO.readSInt32LE(is);
        if (_iOffset < 1)
            throw new BinaryDataNotRecognized();
        _iSize = IO.readSInt32LE(is);
        if (_iSize < 5 || _iSize > 17000)
            throw new BinaryDataNotRecognized();
        lng8Zeroes = IO.readSInt64BE(is);
        if (lng8Zeroes != 0)
            throw new BinaryDataNotRecognized();
        lng8Zeroes = IO.readSInt64BE(is);
        if (lng8Zeroes != 0)
            throw new BinaryDataNotRecognized();

        if (_type == Type.SDNSSDTS && !(_iSize % SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT == 0))
            throw new BinaryDataNotRecognized();
    }

    public @Nonnull Type getType() {
        return _type;
    }

    public int getTimestamp() {
        return _iTimestamp;
    }

    public int getDuration() {
        return _iDuration;
    }

    public int getOffset() {
        return _iOffset;
    }

    public int getSize() {
        return _iSize;
    }

    @Override
    public String toString() {
        return String.format("%s Time:%d Duration:%d Offset:%d Size:%d",
            _type, _iTimestamp, _iDuration, _iOffset, _iSize);
    }
}
