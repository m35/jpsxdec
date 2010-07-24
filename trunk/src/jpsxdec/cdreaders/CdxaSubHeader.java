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

package jpsxdec.cdreaders;

import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


    
/** Represents a raw CD header without a sync header and sector header. */
public class CdxaSubHeader {

    private static final Logger log = Logger.getLogger(CdxaSubHeader.class.getName());

    public static final int SIZE = 8;

    public int getSize() {
        return SIZE;
    }

    private int file_number;            // [1 byte] used to identify sectors
                                        //          belonging to the same file
    public int getFileNumber() { return file_number; }
    private int channel;                // [1 byte] 0-15 for ADPCM audio
    public int getChannel() { return channel; }
    private SubMode submode;            // [1 byte]
    public SubMode getSubMode() { return submode; }
    private CodingInfo coding_info;     // [1 byte]
    public CodingInfo getCodingInfo() { return coding_info; }

    // Following the header are either [2324 bytes]
    // or [2048 bytes] of user data (depending on the mode/form).
    // Following that are [4 bytes] Error Detection Code (EDC)
    // or just 0x0000.
    // If the user data was 2048, then final [276 bytes] is error correction

    public CdxaSubHeader(byte[] abSectorData, int iStartOffset, int iTolerance)
            throws NotThisTypeException
    {
        file_number = abSectorData[iStartOffset + 0] & 0xff;
        // Ace Combat 3 has several sectors with channel 255
        // They seem to be "null" sectors
        channel     = abSectorData[iStartOffset + 1] & 0xff;
        submode     = new SubMode(abSectorData[iStartOffset + 2]);
        coding_info = new CodingInfo(abSectorData[iStartOffset + 3]);

        int b;
        b = abSectorData[iStartOffset + 4] & 0xff;
        if (file_number != b)
            throw new NotThisTypeException(String.format("XA audio sector file number copy is corrupted: %d != %d", file_number, b));
        b = abSectorData[iStartOffset + 5] & 0xff;
        if (iTolerance < 1 && channel != b) {
            // Alundra 2 has the first channel correct, but the copy sometimes == 1 (unless my image is just bad)
            throw new NotThisTypeException(String.format("XA audio sector channel copy is corrupted: %d != %d", channel, b));
        }
        b = abSectorData[iStartOffset + 6] & 0xff;
        if (submode.toByte() != b)
            throw new NotThisTypeException(String.format("XA audio sector submode copy is corrupted: 0x%02x != 0x%02x", submode.toByte(), b));
        b = abSectorData[iStartOffset + 7] & 0xff;
        if (coding_info.toByte() != b)
            throw new NotThisTypeException(String.format("XA audio sector coding_info copy is corrupted: 0x%02x != 0x%02x", coding_info.toByte(), b));
    }


    public static class SubMode {
        /** Sub-mode in its original bits. */
        private final int submode;

        /** bit 7:  0 for all sectors except last sector of a file. */
        private final boolean eof_marker;
        public boolean getEofMarker() { return eof_marker; }

        /** bit 6:  1 for real time mode. */
        private final boolean real_time;
        public boolean getRealTime() { return real_time; }

        /** bit 5:  0 = form 1, 1 = form 2. */
        private final int form;
        /** Form 1 or Form 2. */
        public int getForm() { return form; }

        /** bit 4:  used for application. */
        private final boolean trigger;
        public boolean getTrigger() { return trigger; }

        public static enum DATA_AUDIO_VIDEO {
            /** Sector contains data (or video). */ DATA,
            /** Sector contains ADPCM audio.     */ AUDIO,
            /** Sector contains video data.      */ VIDEO,
            /** What I call a "null" sector. Basically
              * the contents are ignored.        */ NULL
        }
        /** bit 3:  1 could mean data or video data.
         *          Mutually exclusive with bits 2 and 1.
         *  bit 2:  1 for ADPCM sector.
         *          Mutually exclusive with bits 3 and 1.
         *  bit 1:  1 for video sector.
         *          Mutually exclusive with bits 3 and 2. */
        private final DATA_AUDIO_VIDEO data_audio_video;
        public DATA_AUDIO_VIDEO getDataAudioVideo() {
            return data_audio_video;
        }

        /** bit 0:  identifies end of audio frame */
        private final boolean end_audio;
        public boolean getEndAudio() { return end_audio; }

        SubMode(byte b) throws NotThisTypeException {
            submode = b & 0xff;

            eof_marker    = (b & 0x80) > 0;
            real_time     = (b & 0x40) > 0;
            form          = ((b >> 5) & 1) + 1;
            trigger       = (b & 0x10) > 0;
            int iBits = (b >> 1) & 7;
            switch (iBits) {
                case 4: data_audio_video = DATA_AUDIO_VIDEO.DATA; break;
                case 2: data_audio_video = DATA_AUDIO_VIDEO.AUDIO; break;
                case 1: data_audio_video = DATA_AUDIO_VIDEO.VIDEO; break;
                case 0: data_audio_video = DATA_AUDIO_VIDEO.NULL; break;
                default: throw new NotThisTypeException(
                        "CD sector submode data|audio|video is corrupted: " + Misc.bitsToString(iBits, 3) + "b");
            }
            end_audio     = (b & 1) > 0;
        }

        public int toByte() { return submode; }

        public String toString() { return Misc.bitsToString(submode, 8); }
    }

    public static class CodingInfo {
        /** Coding info in its original bits. */
        private final int codinginfo;

        /** bit 7:    =0 ?  */
        private final boolean reserved;
        public boolean getReserved() { return reserved; }

        /** bit 6:          */
        private final boolean emphasis;
        public boolean getEmphasis() { return emphasis; }

        /** bits 5,4: 00=4bits (B,C format)
         *            01=8bits */
        private final int     bits_per_sample;
        public int getBitsPerSample() { return bits_per_sample; }

        /** bits 3,2: 00=37.8kHz (A,B format)
         *            01=18.9kHz */
        private final int     sample_rate;
        public int getSampleRate() { return sample_rate; }

        /** bits 1,0: 00=mono 01=stereo,
         *            other values reserved */
        private final boolean mono_stereo;
        public boolean isStereo() { return mono_stereo; }

        CodingInfo(byte b) throws NotThisTypeException {
            codinginfo = b & 0xff;

            reserved           = (b & 0x80) > 0;
            emphasis           = (b & 0x40) > 0;
            int iBits = (b >> 4) & 3;
            switch (iBits) {
                case 0: bits_per_sample = 4; break;
                case 1: bits_per_sample = 8; break;
                default: throw new NotThisTypeException(
                        "CD sector coding info bits/sample is corrupted: " + iBits);
            }
            iBits = (b >> 2) & 3;
            switch (iBits) {
                case 0: sample_rate = 37800; break;
                case 1: sample_rate = 18900; break;
                default: throw new NotThisTypeException(
                        "CD sector coding info sample rate is corrupted: " + iBits);
            }
            iBits = (b >> 0) & 3;
            switch (iBits) {
                case 0: mono_stereo = false; break;
                case 1: mono_stereo = true;  break;
                default: throw new NotThisTypeException(
                        "CD sector coding info mono/stereo is corrupted: " + iBits);
            }

        }

        public int toByte() {
           return codinginfo;
        }

        public String toString() {
            return String.format("%s %d bits/sample %d samples/sec",
                                 mono_stereo ? "Stereo" : "Mono",
                                 bits_per_sample, sample_rate);
        }
    }

    public String toString() {
        return String.format("File.Channel:%d.%d Subcode:%s",
                    file_number, channel, submode.toString());
    }
}
    
