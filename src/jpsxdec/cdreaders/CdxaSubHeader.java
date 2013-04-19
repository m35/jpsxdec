/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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

import java.util.logging.Logger;

    
/** Represents a raw CD header without a sync header and sector header. */
public class CdxaSubHeader {

    private static enum IssueType {
        EQUAL_BOTH_GOOD(0) {public String log(String s1, String s2, int c) {return null;}},
        EQUAL_BOTHBAD(0) {public String log(String s1, String s2, int c) {
            return s1 + " (bad) == " + s2 + " (bad)";
        }},
        DIFF_BOTHGOOD(0) {public String log(String s1, String s2, int c) {
            if (c < 0)
                return (s1 + " != " + s2 + " (chose " + s1 + " by confidence)");
            else if (c > 0)
                return (s1 + " != " + s2 + " (chose " + s2 + " by confidence)");
            else
                return (s1 + " != " + s2 + " (chose "  + s1 + " by default)");
        }},
        DIFF_1GOOD2BAD(-1) {public String log(String s1, String s2, int c) {
            return (s1 + " != " + s2 + " (bad) (chose "  + s1 + ")");
        }},
        DIFF_1BAD2GOOD(+1) {public String log(String s1, String s2, int c) {
            return (s1 + " (bad) != " + s2 + " (chose "  + s2 + ")");
        }},
        DIFF_BOTHBAD(0) {public String log(String s1, String s2, int c) {
            if (c < 0)
                return (s1 + " (bad) != " + s2 + " (bad) (chose " + s1 + " by confidence)");
            else if (c > 0)
                return (s1 + " (bad) != " + s2 + " (bad) (chose " + s2 + " by confidence)");
            else
                return (s1 + " (bad) != " + s2 + " (bad) (chose "  + s1 + " by default)");
        }};

        public final int Balance;
        IssueType(int i) { Balance = i; }
        abstract public String log(String s1, String s2, int c);

        // .........................................

        public static IssueType diffIssue(boolean blnValid1, boolean blnValid2) {
            if (blnValid1) {
                if (blnValid2) {
                    return IssueType.DIFF_BOTHGOOD;
                } else
                    return IssueType.DIFF_1GOOD2BAD;
            } else {
                if (blnValid2) {
                    return IssueType.DIFF_1BAD2GOOD;
                } else
                    return IssueType.DIFF_BOTHBAD;
            }
        }
    }

    /** Side of the header in bytes. */
    public static final int SIZE = 8;

    /** Returns the size of the subheader data structure in bytes. */
    public int getSize() {
        return SIZE;
    }

    private int _iFileNum1 = -1;            // [1 byte] used to identify sectors
                                        //          belonging to the same file
    private int _iFileNum2;
    private final IssueType _eFileIssue;
    public int getFileNumber() {
        switch (_eFileIssue) {
            case DIFF_1BAD2GOOD:
                return _iFileNum2;
            case DIFF_BOTHGOOD:
            case DIFF_BOTHBAD:
                return _iConfidenceBalance <= 0 ? _iFileNum1 : _iFileNum2;
            default:
                return _iFileNum1;
        }
    }

    private int _iChannel1 = -1;         // [1 byte] 0-31 for ADPCM audio
    private int _iChannel2;
    private final IssueType _eChannelIssue;
    public int getChannel() { 
        switch (_eChannelIssue) {
            case DIFF_1BAD2GOOD:
                return _iChannel2;
            case DIFF_BOTHGOOD:
            case DIFF_BOTHBAD:
                return _iConfidenceBalance <= 0 ? _iChannel1 : _iChannel2;
            default:
                return _iChannel1;
        }
    }

    private SubMode _submode1;            // [1 byte]
    private SubMode _submode2;
    private final IssueType _eSubModeIssue;
    public SubMode getSubMode() { 
        switch (_eChannelIssue) {
            case DIFF_1BAD2GOOD:
                return _submode2;
            case DIFF_BOTHGOOD:
            case DIFF_BOTHBAD:
                return _iConfidenceBalance <= 0 ? _submode1 : _submode2;
            default:
                return _submode1;
        }
    }

    private CodingInfo _codingInfo1;     // [1 byte]
    private CodingInfo _codingInfo2;
    private final IssueType _eCodingInfoIssue;
    public CodingInfo getCodingInfo() { 
        switch (_eChannelIssue) {
            case DIFF_1BAD2GOOD:
                return _codingInfo2;
            case DIFF_BOTHGOOD:
            case DIFF_BOTHBAD:
                return _iConfidenceBalance <= 0 ? _codingInfo1 : _codingInfo2;
            default:
                return _codingInfo1;
        }
    }

    private final int _iConfidenceBalance;


    private static boolean isFileValid(int iFileNumber) {
        return iFileNumber == 0 || iFileNumber == 1;
    }
    private static boolean isChannelValid(int iChannelNumber) {
        // my understanding is channel is technically supposed to be
        // between 0 and 31, but PSX seems to allow for any byte value.
        // still warn anyway
        return iChannelNumber >= 0 && iChannelNumber < 32;
    }


    public CdxaSubHeader(byte[] abSectorData, int iStartOffset) {

        _iFileNum1 = abSectorData[iStartOffset+0] & 0xff;
        _iFileNum2 = abSectorData[iStartOffset+0+4] & 0xff;
        _iChannel1 = abSectorData[iStartOffset+1] & 0xff;
        _iChannel2 = abSectorData[iStartOffset+1+4] & 0xff;
        _submode1 = new SubMode(abSectorData[iStartOffset+2] & 0xff);
        _submode2 = new SubMode(abSectorData[iStartOffset+2+4] & 0xff);
        _codingInfo1 = new CodingInfo(abSectorData[iStartOffset+3] & 0xff);
        _codingInfo2 = new CodingInfo(abSectorData[iStartOffset+3+4] & 0xff);

        int iConfidenceBalance = 0;

        boolean blnValid1 = isFileValid(_iFileNum1);
        if (_iFileNum1 == _iFileNum2) {
            _eFileIssue = blnValid1 ? IssueType.EQUAL_BOTH_GOOD :
                                      IssueType.EQUAL_BOTHBAD;
        } else {
            _eFileIssue = IssueType.diffIssue(blnValid1, isFileValid(_iFileNum2));
            iConfidenceBalance += _eFileIssue.Balance;
        }

        blnValid1 = isChannelValid(_iChannel1);
        if (_iChannel1 == _iChannel2) {
            _eChannelIssue = blnValid1 ? IssueType.EQUAL_BOTH_GOOD :
                                         IssueType.EQUAL_BOTHBAD;
        } else {
            _eChannelIssue = IssueType.diffIssue(blnValid1, isChannelValid(_iChannel2));
            iConfidenceBalance += _eChannelIssue.Balance;
        }

        blnValid1 = _submode1.isValid();
        if (_submode1.toByte() == _submode2.toByte()) {
            _eSubModeIssue = blnValid1 ? IssueType.EQUAL_BOTH_GOOD :
                                         IssueType.EQUAL_BOTHBAD;
        } else {
            _eSubModeIssue = IssueType.diffIssue(blnValid1, _submode2.isValid());
            iConfidenceBalance += _eSubModeIssue.Balance;
        }

        blnValid1 = _codingInfo1.isValid();
        if (_codingInfo1.toByte() == _codingInfo2.toByte()) {
            _eCodingInfoIssue = blnValid1 ? IssueType.EQUAL_BOTH_GOOD :
                                            IssueType.EQUAL_BOTHBAD;
        } else {
            _eCodingInfoIssue = IssueType.diffIssue(blnValid1, _codingInfo2.isValid());
            iConfidenceBalance += _eChannelIssue.Balance;
        }

        _iConfidenceBalance = iConfidenceBalance;
    }

    int getErrorCount() {
        int i = _eFileIssue != IssueType.EQUAL_BOTH_GOOD ? 1 : 0;
        if (_eChannelIssue != IssueType.EQUAL_BOTH_GOOD)
            i++;
        if (_eSubModeIssue != IssueType.EQUAL_BOTH_GOOD)
            i++;
        if (_eCodingInfoIssue != IssueType.EQUAL_BOTH_GOOD)
            i++;
        return i;
    }

    void printErrors(int iSector, Logger logger) {
        if (_eFileIssue != IssueType.EQUAL_BOTH_GOOD) {
            logger.warning("Sector " + iSector + " File Number corrupted: " + 
                _eFileIssue.log(String.valueOf(_iFileNum1), String.valueOf(_iFileNum2), _iConfidenceBalance));
        }
        if (_eChannelIssue != IssueType.EQUAL_BOTH_GOOD) {
            logger.warning("Sector " + iSector + " Channel Number corrupted: " +
                _eChannelIssue.log(String.valueOf(_iChannel1), String.valueOf(_iChannel2), _iConfidenceBalance));
        }
        if (_eSubModeIssue != IssueType.EQUAL_BOTH_GOOD) {
            logger.warning("Sector " + iSector + " Submode corrupted: " +
                _eSubModeIssue.log(_submode1.toString(), _submode2.toString(), _iConfidenceBalance));
        }
        if (_eCodingInfoIssue != IssueType.EQUAL_BOTH_GOOD) {
            logger.warning("Sector " + iSector + " Coding Info corrupted: " +
                _eCodingInfoIssue.log(_codingInfo1.toString(), _codingInfo2.toString(), _iConfidenceBalance));
        }
    }

    public String toString() {
        return String.format("File.Channel:%d.%d Submode:%s",
                    getFileNumber(), getChannel(), getSubMode().toString());
    }
    
    //**************************************************************************

    public static class SubMode {

        /** Sub-mode in its original bits. */
        private final int _iSubmode;

        SubMode(int i) {
            _iSubmode = i;
        }

        /**'M'*/public static final int MASK_EOF_MARKER = 0x80;
        /**'R'*/public static final int MASK_REAL_TIME = 0x40;
        /**'2'*/public static final int MASK_FORM = 0x20;
        /**'T'*/public static final int MASK_TRIGGER = 0x10;
        /**'D'*/public static final int MASK_DATA = 0x08;
        /**'A'*/public static final int MASK_AUDIO = 0x04;
        /**'V'*/public static final int MASK_VIDEO = 0x02;
        /**'E'*/public static final int MASK_END_AUDIO = 1;

        /** bit 7:  0 for all sectors except last sector of a file. */
        public boolean getEofMarker() { return (_iSubmode & 0x80) != 0; }
        /** bit 6:  1 for real time mode. */
        public boolean getRealTime() { return (_iSubmode & 0x40) != 0; }
        /** bit 5: 1 for Form 1, 2 for Form 2. */
        public int getForm() { return ((_iSubmode >> 5) & 1) + 1; }
        /** bit 4:  used for application. */
        public boolean getTrigger() { return (_iSubmode & 0x10) != 0; }
        /** bit 3:  1 could mean data or video data.
         *          (should be) Mutually exclusive with bits 2 and 1. */
        public boolean getData() { return (_iSubmode & 0x08) != 0; }
        /** bit 2:  1 for ADPCM sector.
         *          (should be) Mutually exclusive with bits 3 and 1. */
        public boolean getAudio() { return (_iSubmode & 0x04) != 0; }
        /** bit 1:  1 for video sector.
         *          (should be) Mutually exclusive with bits 3 and 2. */
        public boolean getVideo() { return (_iSubmode & 0x02) != 0; }
        /** bit 0:  identifies end of audio frame */
        public boolean getEndAudio() { return (_iSubmode & 1) != 0; }


        public int toByte() { return _iSubmode; }

        public boolean isValid() {
            int iByte = (_iSubmode >> 1) & 7;
            int iCount = 0;
            for (int i=8; i > 0; i >>= 1) {
                if ((iByte & i) != 0)
                    iCount++;
            }
            return iCount <= 1;
        }


        /**
         * Return 8 character string representing the 8 bit flags:
         *<pre>
         * M - EOF marker
         * R - Real-time
         * 2 - Form 1/2
         * T - Trigger
         * D - Data
         * A - Audio
         * V - Video
         * E - End audio
         *</pre>
         */
        public String toString() {
            return CHAR_FLAGS_2[_iSubmode >> 4] + CHAR_FLAGS_1[_iSubmode & 0xf];
        }

        private static final String[] CHAR_FLAGS_1 = {
            "----",
            "---E",
            "--V-",
            "--VE",
            "-A--",
            "-A-E",
            "-AV-",
            "-AVE",
            "D---",
            "D--E",
            "D-V-",
            "D-VE",
            "DA--",
            "DA-E",
            "DAV-",
            "DAVE",
        };
        private static final String[] CHAR_FLAGS_2 = {
            "----",
            "---T",
            "--2-",
            "--2T",
            "-R--",
            "-R-T",
            "-R2-",
            "-R2T",
            "M---",
            "M--T",
            "M-2-",
            "M-2T",
            "MR--",
            "MR-T",
            "MR2-",
            "MR2T",
        };

    }

    //**************************************************************************

    public static class CodingInfo {
        /** Coding info in its original bits. */
        private final int _iCodinginfo;

        /** bit 7:    =0 ?  */
        public boolean getReserved() { return (_iCodinginfo & 0x80) != 0; }

        /** bit 6:          */
        public boolean getEmphasis() { return (_iCodinginfo & 0x40) != 0; }

        /** bits 5,4: 00=4bits (B,C format)
         *            01=8bits */
        public int getBitsPerSample() { return (_iCodinginfo & 0x10) == 0 ? 4 : 8; }

        /** bits 3,2: 00=37.8kHz (A,B format)
         *            01=18.9kHz */
        public int getSampleRate() { return (_iCodinginfo & 0x04) == 0 ? 37800 : 18900; }

        /** bits 1,0: 00=mono 01=stereo,
         *            other values reserved */
        public boolean isStereo() { return (_iCodinginfo & 0x01) != 0; }

        CodingInfo(int i) {
            _iCodinginfo = i;
        }

        public int toByte() {
           return _iCodinginfo;
        }

        public boolean isValid() {
            return (_iCodinginfo & 0x2a) == 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(isStereo() ? "Stereo" : "Mono");
            sb.append((_iCodinginfo & 2) == 0 ? " " : "! ");
            sb.append(getBitsPerSample()).append(" bits/sample");
            sb.append((_iCodinginfo & 8) == 0 ? " " : "! ");
            sb.append(getSampleRate()).append(" samples/sec");
            if ((_iCodinginfo & 32) != 0) sb.append('!');
            return sb.toString();
        }
    }

}
    
