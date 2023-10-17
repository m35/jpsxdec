/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

package jpsxdec.modules.crusader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;


public abstract class CrusaderPacket {
    // in big-endian
    public static final int MDEC_MAGIC = 0x4d444543;
    public static final int ad20_MAGIC = 0x61643230;
    public static final int ad21_MAGIC = 0x61643231;

    public static final int FRAMES_PER_SECOND = 15;
    public static final int SECTORS_PER_FRAME = 150 / FRAMES_PER_SECOND;

    public static final int CRUSADER_SAMPLE_FRAMES_PER_SECOND = 22050;
    public static final int SAMPLE_FRAMES_PER_SECTOR = CRUSADER_SAMPLE_FRAMES_PER_SECOND / 150;

    static {
        if (CRUSADER_SAMPLE_FRAMES_PER_SECOND % 150 != 0 || 150 % FRAMES_PER_SECOND != 0)
            throw new RuntimeException("Crusader sample rate doesn't cleanly divide by sector rate");
    }
    public static final AudioFormat CRUSADER_AUDIO_FORMAT = new AudioFormat(CRUSADER_SAMPLE_FRAMES_PER_SECOND, 16, 2, true, false);

    private enum Type {
        MDEC, ad20, ad21
    }

    /** First half of the packet header. */
    public static class HeaderType {
        public static final int SIZEOF = 8;

        /** Returns null if 0's found instead of proper packet header. */
        public static @CheckForNull HeaderType read(@Nonnull InputStream is)
                throws EOFException, IOException, BinaryDataNotRecognized
        {
            int iMagic = IO.readSInt32BE(is);

            Type magic;
            switch (iMagic) {
                case 0:
                    return null;
                case MDEC_MAGIC:
                    magic = Type.MDEC; break;
                case ad20_MAGIC:
                    magic = Type.ad20; break;
                case ad21_MAGIC:
                    magic = Type.ad21; break;
                default:
                    throw new BinaryDataNotRecognized();
            }

            int iPacketSize = IO.readSInt32BE(is);
            return new HeaderType(magic, iPacketSize);
        }

        private final Type _magic;
        private final int _iPacketSize;

        private HeaderType(@Nonnull Type magic, int iPacketSize) {
            _magic = magic;
            _iPacketSize = iPacketSize;
        }

        public int getRemainingPacketSize() {
            return _iPacketSize - SIZEOF;
        }

        public @Nonnull CrusaderPacket readPacket(@Nonnull InputStream is)
                throws EOFException, IOException, BinaryDataNotRecognized
        {
            byte[] abPacket = IO.readByteArray(is, getRemainingPacketSize());
            if (_magic == Type.MDEC)
                return new Video(_magic, abPacket);
            else
                return new Audio(_magic, abPacket);
        }

        @Override
        public String toString() {
            return String.format("size %d", _iPacketSize);
        }


    }


    public static class Audio extends CrusaderPacket {
        // "ad20" or "ad21"                               // 4 bytes (already read)
        // packet size (including this header)            // 4 bytes (already read)
        private final int _iPresentationSampleFrame;      // 4 bytes
        private static final long AUDIO_ID = 0x08000200L; // 4 bytes (in big-endian)

        private Audio(@Nonnull Type magic, @Nonnull byte[] abPacket)
                throws BinaryDataNotRecognized
        {
            super(magic, abPacket);
            // 2 for left and right audio channels
            // 16 for SPU ADPCM sound unit count
            // always be sure the audio data is a multiple of 16*2
            if (getPayloadSize() % (SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT * 2) != 0)
                throw new BinaryDataNotRecognized();
            _iPresentationSampleFrame = IO.readSInt32BE(abPacket, 0);
            if (_iPresentationSampleFrame < 0)
                throw new BinaryDataNotRecognized();
            final long lngAudioId = IO.readUInt32BE(abPacket, 4);
            if (lngAudioId != AUDIO_ID)
                throw new BinaryDataNotRecognized();
        }

        public int getPresentationSampleFrame() {
            return _iPresentationSampleFrame;
        }

        public int getSoundUnitPairCount() {
            return getPayloadSize() / (SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT * 2);
        }

        @Override
        public String toString() {
            return super.toString() + " presentation sample " + _iPresentationSampleFrame;
        }
    }

    public static class Video extends CrusaderPacket {
        // "MDEC" // 4 bytes (already read)
        // packet size (including this header) // 4 bytes (already read)
        private final short _iWidth;     // 2 bytes
        private final short _iHeight;    // 2 bytes
        private final int _iFrameNumber; // 4 bytes

        private Video(@Nonnull Type magic, @Nonnull byte[] abPacket) throws BinaryDataNotRecognized {
            super(magic, abPacket);
            _iWidth = IO.readSInt16BE(abPacket, 0);
            if (_iWidth < 1)
                throw new BinaryDataNotRecognized();
            _iHeight = IO.readSInt16BE(abPacket, 2);
            if (_iHeight < 1)
                throw new BinaryDataNotRecognized();
            _iFrameNumber = IO.readSInt32BE(abPacket, 4);
            if (_iFrameNumber < 0)
                throw new BinaryDataNotRecognized();
        }

        public short getWidth() {
            return _iWidth;
        }

        public short getHeight() {
            return _iHeight;
        }

        public int getFrameNumber() {
            return _iFrameNumber;
        }

        @Override
        public String toString() {
            return String.format("%s %dx%d frame %d", super.toString(),
                                 _iWidth, _iHeight, _iFrameNumber);
        }


    }


    private static final int REMAINING_PACKET_HEADER_SIZE = 8;

    @Nonnull
    private final Type _magic; // 4 bytes (already read)
    @Nonnull
    private final byte[] _abPacket;

    private int _iStartSector = -1, _iEndSector = -1;

    public CrusaderPacket(@Nonnull Type magic, @Nonnull byte[] abPacket) {
        _magic = magic;
        _abPacket = abPacket;
    }

    void setSectors(int iStartSector, int iEndSector) {
        _iStartSector = iStartSector;
        _iEndSector = iEndSector;
    }

    public int getStartSector() {
        return _iStartSector;
    }

    public int getEndSector() {
        return _iEndSector;
    }

    public @Nonnull byte[] copyPayload() {
        return Arrays.copyOfRange(_abPacket, REMAINING_PACKET_HEADER_SIZE, _abPacket.length);
    }

    final public int getPayloadSize() {
        return _abPacket.length - REMAINING_PACKET_HEADER_SIZE;
    }

    @Override
    public String toString() {
        return String.format("%s sectors %d-%d payload size %d",
                             _magic, _iStartSector, _iEndSector, getPayloadSize());
    }

}
