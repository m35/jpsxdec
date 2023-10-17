/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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
import javax.annotation.Nonnull;


/** A CD "XA" sector. Here we use XA for what it officially stands for:
 * eXtended Architecture. It is defined in the "Green Book" CD standard.
 * This header contains a file number, a channel, a sub-more, and coding info.
 *
 * This implementation gets fancy by trying to identify and correct for
 * corruption.
 */
public class CdSectorXaSubHeader {

    private static final Logger LOG = Logger.getLogger(CdSectorXaSubHeader.class.getName());

    private enum IssueType {
        EQUAL_BOTH_GOOD(0) {public void err(String sName, String sVal1, String sVal2, int iConfidenceBalance, StringBuilder sb) {
            throw new IllegalStateException("Should never happen");
        }},
        EQUAL_BOTHBAD(0) {public void err(String sName, String sVal1, String sVal2, int iConfidenceBalance, StringBuilder sb) {
            sb.append(sName).append('[').append(sVal1).append(" (bad) == ").append(sVal2).append(" (bad)]");
        }},
        DIFF_BOTHGOOD(0) {public void err(String sName, String sVal1, String sVal2, int iConfidenceBalance, StringBuilder sb) {
            sb.append(sName).append('[').append(sVal1).append(" != ").append(sVal2).append(" chose ");
            if (iConfidenceBalance < 0)
                sb.append(sVal1).append(" by confidence]");
            else if (iConfidenceBalance > 0)
                sb.append(sVal2).append(" by confidence]");
            else
                sb.append(sVal1).append(" by default]");
        }},
        DIFF_1GOOD2BAD(-1) {public void err(String sName, String sVal1, String sVal2, int iConfidenceBalance, StringBuilder sb) {
            sb.append(sName).append('[').append(sVal1).append(" != ").append(sVal2).append(" (bad)]");
        }},
        DIFF_1BAD2GOOD(+1) {public void err(String sName, String sVal1, String sVal2, int iConfidenceBalance, StringBuilder sb) {
            sb.append(sName).append('[').append(sVal1).append(" (bad) != ").append(sVal2).append("]");
        }},
        DIFF_BOTHBAD(0) {public void err(String sName, String sVal1, String sVal2, int iConfidenceBalance, StringBuilder sb) {
            sb.append(sName).append('[').append(sVal1).append(" (bad) != ").append(sVal2).append(" (bad) chose ");
            if (iConfidenceBalance < 0)
                sb.append(sVal1).append(" by confidence]");
            else if (iConfidenceBalance > 0)
                sb.append(sVal2).append(" by confidence]");
            else
                sb.append(sVal1).append(" by default]");
        }};

        public final int Balance;
        IssueType(int i) { Balance = i; }
        abstract public void err(String sName, String sVal1, String sVal2, int iConfidenceBalance, StringBuilder sb);

        // .........................................

        public static @Nonnull IssueType diffIssue(boolean blnValid1, boolean blnValid2) {
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
    public static final int SIZEOF = 8;

    private final int _iFileNum1;            // [1 byte] used to identify sectors
                                             //          belonging to the same file
    private final int _iFileNum2;
    @Nonnull private final IssueType _eFileIssue;
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

    private final int _iChannel1;                // [1 byte] 0-31 for ADPCM audio
    private final int _iChannel2;
    @Nonnull private final IssueType _eChannelIssue;
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

    @Nonnull private final SubMode _submode1;   // [1 byte]
    @Nonnull private final SubMode _submode2;
    @Nonnull private final IssueType _eSubModeIssue;
    public @Nonnull SubMode getSubMode() {
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

    private final CodingInfo _codingInfo1;     // [1 byte]
    private final CodingInfo _codingInfo2;
    @Nonnull private final IssueType _eCodingInfoIssue;
    public @Nonnull CodingInfo getCodingInfo() {
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


    public CdSectorXaSubHeader(int iSector, @Nonnull byte[] abSectorData, int iStartOffset) {

        _iFileNum1 = abSectorData[iStartOffset+0] & 0xff;
        _iFileNum2 = abSectorData[iStartOffset+0+4] & 0xff;
        _iChannel1 = abSectorData[iStartOffset+1] & 0xff;
        _iChannel2 = abSectorData[iStartOffset+1+4] & 0xff;
        _submode1 = new SubMode(abSectorData[iStartOffset+2] & 0xff);
        _submode2 = new SubMode(abSectorData[iStartOffset+2+4] & 0xff);
        _codingInfo1 = new CodingInfo(abSectorData[iStartOffset+3] & 0xff);
        _codingInfo2 = new CodingInfo(abSectorData[iStartOffset+3+4] & 0xff);

        int iConfidenceBalance = 0;

        // I've tried to put some constraint on what is considered a
        // 'good' file number, but PSX seems to allow for any byte value.
        if (_iFileNum1 == _iFileNum2)
            _eFileIssue = IssueType.EQUAL_BOTH_GOOD;
        else
            _eFileIssue = IssueType.DIFF_BOTHGOOD;

        // my understanding is channel is technically supposed to be
        // between 0 and 31, but PSX seems to allow for any byte value.
        if (_iChannel1 == _iChannel2)
            _eChannelIssue = IssueType.EQUAL_BOTH_GOOD;
        else
            _eChannelIssue = IssueType.DIFF_BOTHGOOD;

        boolean blnValid1 = _submode1.isValid();
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

        if (LOG.isLoggable(Level.WARNING)) {
            StringBuilder sb = new StringBuilder();
            if (_eFileIssue != IssueType.EQUAL_BOTH_GOOD)
                _eFileIssue.err("File", String.valueOf(_iFileNum1), String.valueOf(_iFileNum2), iConfidenceBalance, sb);
            if (_eChannelIssue != IssueType.EQUAL_BOTH_GOOD) {
                if (sb.length() > 0)
                    sb.append(' ');
                _eChannelIssue.err("Channel", String.valueOf(_iChannel1), String.valueOf(_iChannel2), iConfidenceBalance, sb);
            }
            if (_eSubModeIssue != IssueType.EQUAL_BOTH_GOOD) {
                if (sb.length() > 0)
                    sb.append(' ');
                _eSubModeIssue.err("Submode", _submode1.toString(), _submode2.toString(), iConfidenceBalance, sb);
            }
            if (_eCodingInfoIssue != IssueType.EQUAL_BOTH_GOOD) {
                if (sb.length() > 0)
                    sb.append(' ');
                _eCodingInfoIssue.err("Coding info", _codingInfo1.toString(), _codingInfo2.toString(), iConfidenceBalance, sb);
            }
            if (sb.length() > 0)
                LOG.warning("Sector " +iSector+ " corrupted "+sb);
        }
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

    public boolean hasErrors() {
        return _eFileIssue       != IssueType.EQUAL_BOTH_GOOD ||
               _eChannelIssue    != IssueType.EQUAL_BOTH_GOOD ||
               _eSubModeIssue    != IssueType.EQUAL_BOTH_GOOD ||
               _eCodingInfoIssue != IssueType.EQUAL_BOTH_GOOD;
    }

    @Override
    public String toString() {
        return String.format("File.Channel:%d.%d Submode:%s",
                    getFileNumber(), getChannel(), getSubMode());
    }

    //**************************************************************************

    public static class SubMode {

        /** Sub-mode in its original bits. */
        private final int _iSubmode;

        SubMode(int i) {
            _iSubmode = i;
        }

        /**'F'*/public static final int MASK_END_OF_FILE = 0x80;
        /**'R'*/public static final int MASK_REAL_TIME = 0x40;
        /**'2'*/public static final int MASK_FORM = 0x20;
        /**'T'*/public static final int MASK_TRIGGER = 0x10;
        /**'D'*/public static final int MASK_DATA = 0x08;
        /**'A'*/public static final int MASK_AUDIO = 0x04;
        /**'V'*/public static final int MASK_VIDEO = 0x02;
        /**'E'*/public static final int MASK_END_OF_RECORD = 1;

        /** bit 7:  0 for all sectors except last sector of a file. */
        public boolean getEndOfFile() { return (_iSubmode & 0x80) != 0; }
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
        /** bit 0:  identifies end of record. */
        public boolean getEndOfRecord() { return (_iSubmode & 1) != 0; }

        /** Returns the submode bits masked against the given value. */
        public int mask(int i) {
            return _iSubmode & i;
        }

        public int toByte() { return _iSubmode; }


        public boolean isValid() {
            // TODO also check Data/Audio/Video exclusivity cuz it's proven extremely reliable
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
         * F - End of file (EOF)
         * R - Real-time (RT)
         * 2 - Form 1/2 (F)
         * T - Trigger (T)
         * D - Data (D)
         * A - Audio (A)
         * V - Video (V)
         * E - End of record (EOR)
         *</pre>
         */
        @Override
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
            "F---",
            "F--T",
            "F-2-",
            "F-2T",
            "FR--",
            "FR-T",
            "FR2-",
            "FR2T",
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
        public int getSamplesPerSecond() { return (_iCodinginfo & 0x04) == 0 ? 37800 : 18900; }

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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(isStereo() ? "Stereo" : "Mono");
            sb.append((_iCodinginfo & 2) == 0 ? " " : "! ");
            sb.append(getBitsPerSample()).append(" bits/sample");
            sb.append((_iCodinginfo & 8) == 0 ? " " : "! ");
            sb.append(getSamplesPerSecond()).append("Hz");
            if ((_iCodinginfo & 32) != 0) sb.append('!');
            return sb.toString();
        }
    }

}

