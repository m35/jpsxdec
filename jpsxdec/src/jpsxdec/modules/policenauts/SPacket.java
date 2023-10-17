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

package jpsxdec.modules.policenauts;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.adpcm.SoundUnitDecoder;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/**
 * Policenauts is a game released only in Japan, not originally for PlayStation,
 * but was later ported to the PlayStation. I suspect this is why the FMVs used
 * in the game are in a unique packet-based format.
 *
 * There are several packet types, all with a type identifier. All the
 * identifiers start with the letter 'S'. The packet types consist of more than
 * just the audio and video frames. The subtitles are perhaps the most
 * interesting of the extra metadata packets. But this implementation only
 * supports the audio and video packets.
 *
 * Videos begin with a {@link SectorPN_VMNK} sector, followed by a
 * {@link SectorPN_KLBS} sector followed by 128 sectors of packets, then another
 * {@link SectorPN_KLBS} sector followed by 128 sectors of packets, repeat until
 * there isn't a {@link SectorPN_KLBS} sector.
 */
public class SPacket {

    private static final Logger LOG = Logger.getLogger(SPacket.class.getName());

    // Frome these values, all other values can be derived
    public static final int AUDIO_SAMPLE_FRAMES_PER_SECOND = 44100; // confirmed
    /** The duration value of (almost?) every video frame packet.
     * @see #_iDuration */
    private static final int COMMON_FRAME_PACKET_DURATION = 20;
    /** Size of (almost?) every audio packet.
     * @see #_iSize */
    private static final int COMMON_AUDIO_PACKET_BYTE_SIZE = 16384;
    /** The duration value of (almost?) every audio packet.
     * @see #_iDuration */
    private static final int COMMON_AUDIO_PACKET_DURATION = 156;

    // Derived values
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(AUDIO_SAMPLE_FRAMES_PER_SECOND, 16, 1, true, false);
    private static final Fraction AUDIO_SAMPLE_FRAMES_PER_TIMESTAMP =
            new Fraction((COMMON_AUDIO_PACKET_BYTE_SIZE / SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT) * SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT,
                         COMMON_AUDIO_PACKET_DURATION);
    private static final Fraction TIMESTAMP_UNITS_PER_SECOND = Fraction.divide(AUDIO_SAMPLE_FRAMES_PER_SECOND, AUDIO_SAMPLE_FRAMES_PER_TIMESTAMP);
    private static final Fraction SECONDS_PER_TIMESTAMP = Fraction.divide(1, TIMESTAMP_UNITS_PER_SECOND);

    /** This ultimately comes to 12285/1024 = 11.9970703125.
     * Note this is not 15fps as reported by director Hideo Kojima ;) */
    public static final Fraction FRAMES_PER_SECOND = TIMESTAMP_UNITS_PER_SECOND.divide(COMMON_FRAME_PACKET_DURATION);

    /** Sectors/frame if the disc is spinning at 2x (150 sectors/second). */
    public static final Fraction SECTORS150_PER_FRAME = Fraction.divide(150, FRAMES_PER_SECOND);
    /** Sectors/timestamp if the disc is spinning at 2x (150 sectors/second). */
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
        // Check all values to make sure they're all generally within expexted ranges
        _iTimestamp = IO.readSInt32LE(is);
        if (_iTimestamp < 0 || _iTimestamp > 63780)
            throw new BinaryDataNotRecognized();
        _iDuration = IO.readSInt32LE(is);
        if (_iDuration < 0 || _iDuration > 156)
            throw new BinaryDataNotRecognized("Duration " + _iDuration);
        // TODO track frame duration for a video and calculate the FPS based on that, instead of the hard-coded one above
        if (_type == Type.SCIPPDTS && _iDuration != COMMON_FRAME_PACKET_DURATION)
            throw new BinaryDataNotRecognized("Video frame packet does not have common duration: "+
                                              _iDuration + " != " + COMMON_FRAME_PACKET_DURATION);
        _iOffset = IO.readSInt32LE(is);
        if (_iOffset < 1)
            throw new BinaryDataNotRecognized();
        _iSize = IO.readSInt32LE(is);
        if (_iSize < 5 || _iSize > 17000)
            throw new BinaryDataNotRecognized("Size " + _iSize);
        lng8Zeroes = IO.readSInt64BE(is);
        if (lng8Zeroes != 0)
            throw new BinaryDataNotRecognized();
        lng8Zeroes = IO.readSInt64BE(is);
        if (lng8Zeroes != 0)
            throw new BinaryDataNotRecognized();

        if (_type == Type.SDNSSDTS && (_iSize % SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT) != 0)
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
