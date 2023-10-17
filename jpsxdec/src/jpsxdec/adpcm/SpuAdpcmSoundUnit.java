/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

package jpsxdec.adpcm;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;


/**
 * SPU ADPCM Sound Unit.
 * <p>
 * From <a href="http://problemkaputt.de/psx-spx.htm#soundprocessingunitspu">
 * Nocash PSXSPX Playstation Specifications</a>
 * <blockquote>
 * Samples consist of one or more 16-byte blocks:
 * <pre>
 * 00h       Shift/Filter (reportedly same as for CD-XA) (see there)
 * 01h       Flag Bits (see below)
 * 02h       Compressed Data (LSBs=1st Sample, MSBs=2nd Sample)
 * 03h       Compressed Data (LSBs=3rd Sample, MSBs=4th Sample)
 * 04h       Compressed Data (LSBs=5th Sample, MSBs=6th Sample)
 * ...       ...
 * 0Fh       Compressed Data (LSBs=27th Sample, MSBs=28th Sample)
 * </pre>
 * </blockquote>
 */
public class SpuAdpcmSoundUnit implements IAdpcmSoundUnit {

    private static final Logger LOG = Logger.getLogger(SpuAdpcmSoundUnit.class.getName());

    /** Sound units take up 16 bytes:
     * <ul>
     * <li>1 byte for the sound parameter
     * <li>1 byte for SPU flags
     * <li>14 bytes of ADPCM sound samples
    * </ul>
    */
    public static final int SIZEOF_SOUND_UNIT = 16;

    /** How to handle corrupted filter index.
     * Assumes only 1 bit of the relevant bits has been corrupted. */
    private final static byte[] FILTER_CORRUPTION_FIX = {
        // If the first 3 bits are withing range, just use them
        /* xxxxx000 -> 000 */ 0,
        /* xxxxx001 -> 001 */ 1,
        /* xxxxx010 -> 010 */ 2,
        /* xxxxx011 -> 011 */ 3,
        /* xxxxx100 -> 100 */ 4,

        // After that it gets interesting...

        // If the 100 bit is set and either 011 bits are set, what do we do?
        // Assuming corruption only toggled 1 bit it's hard to say which one
        // was corrupted.
        // In theory we may be able to check the samples along with the range
        // to see if we can determine which to use... which would be a ton of work
        // TODO: is there a way to choose corrupted filter in this case?
        /* xxxxx101 -> either 100 (4) or 001 (1) */ 1,
        /* xxxxx110 -> either 100 (4) or 010 (2) */ 2,

        // However, assuming corruption only toggled 1 bit
        // then the corrupted bit in 111 must be 100
        /* xxxxx111 -> 011 */ 3,
    };

    @Nonnull
    private final byte[] _abSoundUnit = new byte[SIZEOF_SOUND_UNIT];

    public SpuAdpcmSoundUnit(@Nonnull byte[] abSource) {
        this(abSource, 0);
    }
    public SpuAdpcmSoundUnit(@Nonnull byte[] abSource, int iSourceOffset) {
        if (iSourceOffset < 0 || iSourceOffset + SIZEOF_SOUND_UNIT > abSource.length)
            throw new IllegalArgumentException("iSourceOffset out of bounds " + iSourceOffset);

        System.arraycopy(abSource, iSourceOffset, _abSoundUnit, 0, SIZEOF_SOUND_UNIT);
        checkRange();
    }

    public SpuAdpcmSoundUnit(@Nonnull InputStream is)
                throws EOFException, IOException
    {
        IO.readByteArray(is, _abSoundUnit);
        checkRange();
    }

    private void checkRange() {
        // TODO: are there invalid range values?
        // technically any range value would work, however
        // a range of 15 would basically wipe out the sample to the value of 1 or -1
        if (getRange() > 12) {
            LOG.log(Level.INFO, "Range {0} > 12", getRange());
        }
    }

    @Override
    public short getShiftedAdpcmSample(int i) {
        int iIndex = i / 2;
        boolean blnBottomNibble = (i % 2 == 0);

        // get the two ADPCM samples at the byte
        int iBothNibbles = getAdpcmByte(iIndex) & 0xff;

        // make sure to shift the nibble into the top of a short to extend the sign
        if (blnBottomNibble) {
            return (short)((iBothNibbles & 0x0F) << 12);
        } else {
            return (short)((iBothNibbles & 0xF0) <<  8);
        }
    }


    public @CheckForNull String getCorruptionLog() {
        if (isFilterCorrupted() || isFlagBitsCorrupted()) {
            StringBuilder sbLog = new StringBuilder("Bad SPU ADPCM header:");

            if (isFilterCorrupted()) {
                sbLog.append(" Sound Parameter Filter Index[")
                     .append(getFilterIndex()).append(" > 4, using ")
                     .append(getUncorruptedFilterIndex()).append(']');
            }

            if (isFlagBitsCorrupted()) {
                sbLog.append(" BitFlags[").append(Misc.bitsToString(getFlagBits(), 8))
                     .append("]");
            }
            return sbLog.toString();
        } else {
            return null;
        }
    }


    public byte getSoundUnitParameters() {
        return  _abSoundUnit[0];
    }

    public int getFilterIndex() {
        return (getSoundUnitParameters() >> 4) & 0xf;
    }

    public boolean isFilterCorrupted() {
        return getFilterIndex() > 4;
    }

    @Override
    public int getUncorruptedFilterIndex() {
        if (isFilterCorrupted())
            return FILTER_CORRUPTION_FIX[getSoundUnitParameters() & 0x7];
        else
            return getFilterIndex();
    }

    @Override
    public int getRange() {
        return getSoundUnitParameters() & 0xf;
    }


    /**
     * Flag Bits (in 2nd byte of ADPCM Header).
     * <p>
     * From <a href="http://problemkaputt.de/psx-spx.htm#soundprocessingunitspu">
     * Nocash PSXSPX Playstation Specifications</a>
     * <blockquote>
     * <pre>
     * 0   Loop End    (0=No change, 1=Set ENDX flag and Jump to [1F801C0Eh+N*10h])
     * 1   Loop Repeat (0=Force Release and set ADSR Level to Zero; only if Bit0=1)
     * 2   Loop Start  (0=No change, 1=Copy current address to [1F801C0Eh+N*10h])
     * 3-7 Unknown    (usually 0)
     * </pre>
     * Possible combinations for Bit0-1 are:
     * <pre>
     * Code 0 = Normal     (continue at next 16-byte block)
     * Code 1 = End+Mute   (jump to Loop-address, set ENDX flag, Release, Env=0000h)
     * Code 2 = Ignored    (same as Code 0)
     * Code 3 = End+Repeat (jump to Loop-address, set ENDX flag)
     * </pre>
     * The Loop Start/End flags in the ADPCM Header allow to play one
     * or more sample block(s) in a loop, that can be either all block(s)
     * endless repeated, or only the last some block(s) of the sample.
     * <p>
     * There's no way to stop the output, so a one-shot sample must be
     * followed by dummy block (with Loop Start/End flags both set, and all
     * data nibbles set to zero; so that the block gets endless repeated,
     * but doesn't produce any sound).
     * </blockquote>
     * <p>
     * Not used in this decoder, but may be useful to be aware of.
     */
    public byte getFlagBits() {
        return  _abSoundUnit[1];
    }

    public boolean isFlagBitsCorrupted() {
        return (getFlagBits() & ~7) != 0;
    }

    private byte getAdpcmByte(int i) {
        if (i < 0 || i > SIZEOF_SOUND_UNIT - 2)
            throw new IndexOutOfBoundsException();
        return _abSoundUnit[2 + i];
    }

    @Override
    public String toString() {
        if (_abSoundUnit == null)
            return null;
        String sCorruptedFilter = "";
        if (isFilterCorrupted())
            sCorruptedFilter = String.format(" (corrupted -> %d)", getFilterIndex());
        String sFlagsCorrupted = "";
        if (isFlagBitsCorrupted())
            sFlagsCorrupted = " corrupted";
        return String.format("Params 0x%02x = filter %d%s + range %d, flags %s%s",
                             getSoundUnitParameters(), getFilterIndex(),
                             sCorruptedFilter, getRange(),
                             Misc.bitsToString(getFlagBits(), 8),
                             sFlagsCorrupted);
    }



}
