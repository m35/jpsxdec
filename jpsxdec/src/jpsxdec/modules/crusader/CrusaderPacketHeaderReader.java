/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2019  Michael Sabin
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;


public class CrusaderPacketHeaderReader {

    private static final Logger LOG = Logger.getLogger(CrusaderPacketHeaderReader.class.getName());

    public interface Header {
        int getByteSize();
    }

    public static class VideoHeader implements Header {
        private final short _iWidth;
        private final short _iHeight;
        private final int _iFrameNumber;
        private final int _iByteSize;

        public VideoHeader(@Nonnull byte[] abHeader, int iRemainingPayloadSize)
                throws BinaryDataNotRecognized
        {
            _iWidth = IO.readSInt16BE(abHeader, 8);
            if (_iWidth < 1)
                throw new BinaryDataNotRecognized();
            _iHeight = IO.readSInt16BE(abHeader, 10);
            if (_iHeight < 1)
                throw new BinaryDataNotRecognized();
            _iFrameNumber = IO.readSInt32BE(abHeader, 12);
            if (_iFrameNumber < 0)
                throw new BinaryDataNotRecognized();
            _iByteSize = iRemainingPayloadSize;
        }
        public short getWidth() { return _iWidth; }
        public short getHeight() { return _iHeight; }
        public int getFrameNumber() { return _iFrameNumber; }
        public int getByteSize() { return _iByteSize; }
    }

    // in big-endian
    private static final long AUDIO_ID = 0x08000200L;
    
    public static class AudioHeader implements Header {
        private final int _iPresentationSample;
        private final int _iByteSize;

        public AudioHeader(@Nonnull byte[] abHeader, int iRemainingPayloadSize) 
                throws BinaryDataNotRecognized
        {
            // 2 for left and right audio channels
            // 16 for SPU ADPCM sound unit count
            // always be sure the audio data is a multiple of 16*2
            if (iRemainingPayloadSize % (SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT * 2) != 0)
                throw new BinaryDataNotRecognized();
            _iPresentationSample = IO.readSInt32BE(abHeader, 8);
            if (_iPresentationSample < 0)
                throw new BinaryDataNotRecognized();
            final long lngAudioId = IO.readUInt32BE(abHeader, 12);
            if (lngAudioId != AUDIO_ID)
                throw new BinaryDataNotRecognized();
            _iByteSize = iRemainingPayloadSize;
        }

        public int getPresentationSample() { return _iPresentationSample; }
        /** Guaranteed to be a multiple of 16*2 (i.e. stereo SPU sound units). */
        public int getByteSize() { return _iByteSize; }
    }

    
    // in big-endian
    private static final int MDEC = 0x4d444543;
    private static final int ad20 = 0x61643230;
    private static final int ad21 = 0x61643231;


    /** Tries to read a Crusader audio or video packet header from the input stream.
     * @throws EOFException if not enough data in stream to read a header.
     * @throws IOException if error reading from input stream.
     * @throws BinaryDataNotRecognized if the next 16 bytes are not recognized as a header.
     */
    public static @Nonnull Header read(@Nonnull InputStream stream)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        byte[] abHeader = IO.readByteArray(stream, 16);
        try {
            int iPayloadSize = IO.readSInt32BE(abHeader, 4);
            int iRemainingPayloadSize = iPayloadSize - 16;
            if (iRemainingPayloadSize < 1)
                throw new BinaryDataNotRecognized("Payload size = " + iPayloadSize);

            int iMagic = IO.readSInt32BE(abHeader, 0);
            switch (iMagic) {
                case MDEC:
                    return new VideoHeader(abHeader, iRemainingPayloadSize);
                case ad20:
                case ad21:
                    return new AudioHeader(abHeader, iRemainingPayloadSize);
                default:
                    throw new BinaryDataNotRecognized("Unknown magic " + iMagic);
            }
        } catch (BinaryDataNotRecognized ex) {
            if (LOG.isLoggable(Level.INFO)) {
                StringBuilder sb = new StringBuilder("Invalid Crusader header ");
                for (byte b : abHeader) {
                    sb.append(String.format("%02x", b));
                }
                LOG.log(Level.INFO, sb.toString(), ex);
            }
            throw ex;
        }
    }

}
