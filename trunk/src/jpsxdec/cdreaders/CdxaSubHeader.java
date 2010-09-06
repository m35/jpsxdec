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

    /** Side of the header in bytes. */
    public static final int SIZE = 8;

    public int getSize() {
        return SIZE;
    }

    private int file_number;            // [1 byte] used to identify sectors
                                        //          belonging to the same file
    public int getFileNumber() { return file_number; }
    private int channel;                // [1 byte] 0-31 for ADPCM audio
    public int getChannel() { return channel; }
    private SubMode submode;            // [1 byte]
    public SubMode getSubMode() { return submode; }
    private CodingInfo coding_info;     // [1 byte]
    public CodingInfo getCodingInfo() { return coding_info; }

    // Following the header are either [2324 bytes]
    // or [2048 bytes] of user data (depending on the mode/form).
    // Following that are [4 bytes] Error Detection Code (EDC)
    // or just 0x0000.
    // If the user data was 2048, then final [276 bytes] are error correction

    private abstract static class PickBest {

        public int pick(int iSector, byte[] abSectorData, int iStartOffset) throws NotThisTypeException {

            int iValue1 = abSectorData[iStartOffset] & 0xff;
            int iValue2 = abSectorData[iStartOffset+4] & 0xff;

            int iValue;

            // 1 bad, 2 good
            // 1 good, 2 bad
            // 1 and 2 bad but equal
            // 1 and 2 bad and different

            if (iValue1 == iValue2) {
                if (!isOk(iValue1)) {
                    String sErr = String.format("Sector %d %s values are corrupted %s == %s",
                                  iSector, valueName(), toString(iValue1), toString(iValue2));
                    log.warning(sErr);
                    throw new NotThisTypeException(sErr);
                }
                iValue = iValue1;
            } else {
                
                boolean blnValue1Valid, blnValue2Valid;
                blnValue1Valid = isOk(iValue1);
                blnValue2Valid = isOk(iValue2);

                if (blnValue1Valid && !blnValue2Valid) {
                    iValue = iValue1;
                    log.log(Level.WARNING, "Sector {0} {1} value 2 is corrupted {2} != {3}",
                        new Object[] {iSector, valueName(), toString(iValue1), toString(iValue2)});
                } else if (!blnValue1Valid && blnValue2Valid) {
                    iValue = iValue2;
                    log.log(Level.WARNING, "Sector {0} {1} value 1 is corrupted {2} != {3}",
                        new Object[] {iSector, valueName(), toString(iValue1), toString(iValue2)});
                } else {
                    String sErr = String.format("Sector %d %s values are valid but corrupted %s != %s",
                                  iSector, valueName(), toString(iValue1), toString(iValue2));
                    log.warning(sErr);
                    throw new NotThisTypeException(sErr);
                }
            }
            return iValue;
        }

        abstract public boolean isOk(int iByte);
        abstract public String valueName();
        abstract public String toString(int iValue);
    }

    private static PickBest FILE_PICKER = new PickBest() {
        public boolean isOk(int iFileNumber) {
            return iFileNumber == 0 || iFileNumber == 1;
        }
        public String valueName() { return "file"; }
        public String toString(int iValue) { return Integer.toString(iValue); }
    };
    private static PickBest CHANNEL_PICKER = new PickBest() {
        public boolean isOk(int iChannelNumber) {
            return iChannelNumber >= 0 && iChannelNumber < 32;
        }
        public String valueName() { return "channel"; }
        public String toString(int iValue) { return Integer.toString(iValue); }
    };
    private static PickBest SUBMODE_PICKER = new PickBest() {
        public boolean isOk(int iByte) {
            iByte = (iByte >> 1) & 7;
            int iCount = 0;
            for (int i=8; i > 0; i >>= 1) {
                if ((iByte & i) != 0)
                    iCount++;
            }
            return iCount <= 1;
        }
        public String valueName() { return "sub mode"; }
        public String toString(int iValue) { return Misc.bitsToString(iValue, 8); }
    };
    private static PickBest CODINGINFO_PICKER = new PickBest() {
        public boolean isOk(int iByte) {
            return (iByte & 0x2a) == 0;
        }
        public String valueName() { return "coding info"; }
        public String toString(int iValue) { return Misc.bitsToString(iValue, 8); }
    };


    public CdxaSubHeader(int iSector, byte[] abSectorData, int iStartOffset, int iTolerance)
            throws NotThisTypeException
    {
        file_number = FILE_PICKER.pick(iSector, abSectorData, iStartOffset+0);
        channel = CHANNEL_PICKER.pick(iSector, abSectorData, iStartOffset+1);
        submode = new SubMode(SUBMODE_PICKER.pick(iSector, abSectorData, iStartOffset+2));
        coding_info = new CodingInfo(CODINGINFO_PICKER.pick(iSector, abSectorData, iStartOffset+3));
    }

    //**************************************************************************

    public static class SubMode {

        /** Sub-mode in its original bits. */
        private final int _iSubmode;

        /** bit 7:  0 for all sectors except last sector of a file. */
        private final boolean _blnEofMarker;
        public boolean getEofMarker() { return _blnEofMarker; }

        /** bit 6:  1 for real time mode. */
        private final boolean _blnRealTime;
        public boolean getRealTime() { return _blnRealTime; }

        /** bit 5:  0 = form 1, 1 = form 2. */
        private final int _iForm;
        /** Form 1 or Form 2. */
        public int getForm() { return _iForm; }

        /** bit 4:  used for application. */
        private final boolean _blnTrigger;
        public boolean getTrigger() { return _blnTrigger; }

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
        private final DATA_AUDIO_VIDEO _eDataAudioVideo;
        public DATA_AUDIO_VIDEO getDataAudioVideo() {
            return _eDataAudioVideo;
        }

        /** bit 0:  identifies end of audio frame */
        private final boolean _blnEndAudio;
        public boolean getEndAudio() { return _blnEndAudio; }

        SubMode(int i) throws NotThisTypeException {
            _iSubmode = i;

            _blnEofMarker   =  (i & 0x80) != 0;
            _blnRealTime    =  (i & 0x40) != 0;
            _iForm          = ((i >> 5) & 1) + 1;
            _blnTrigger     =  (i & 0x10) != 0;
            int iBits = (i >> 1) & 7;
            switch (iBits) {
                case 4: _eDataAudioVideo = DATA_AUDIO_VIDEO.DATA; break;
                case 2: _eDataAudioVideo = DATA_AUDIO_VIDEO.AUDIO; break;
                case 1: _eDataAudioVideo = DATA_AUDIO_VIDEO.VIDEO; break;
                case 0: _eDataAudioVideo = DATA_AUDIO_VIDEO.NULL; break;
                default: throw new NotThisTypeException(
                        "CD sector submode data|audio|video is corrupted: " + Misc.bitsToString(iBits, 3) + "b");
            }
            _blnEndAudio    = (i & 1) != 0;
        }

        public int toByte() { return _iSubmode; }

        public String toString() { return Misc.bitsToString(_iSubmode, 8); }
    }

    //**************************************************************************

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

        CodingInfo(int i) throws NotThisTypeException {
            codinginfo = i;

            reserved           = (i & 0x80) != 0;
            emphasis           = (i & 0x40) != 0;
            int iBits = (i >> 4) & 3;
            switch (iBits) {
                case 0: bits_per_sample = 4; break;
                case 1: bits_per_sample = 8; break;
                default: throw new NotThisTypeException(
                        "CD sector coding info bits/sample is corrupted: " + iBits);
            }
            iBits = (i >> 2) & 3;
            switch (iBits) {
                case 0: sample_rate = 37800; break;
                case 1: sample_rate = 18900; break;
                default: throw new NotThisTypeException(
                        "CD sector coding info sample rate is corrupted: " + iBits);
            }
            iBits = (i >> 0) & 3;
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
        return String.format("File.Channel:%d.%d Submode:%s",
                    file_number, channel, submode.toString());
    }
}
    
