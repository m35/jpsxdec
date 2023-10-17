/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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
import java.util.AbstractList;
import javax.annotation.Nonnull;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/** Parses PSound .ppl files.
 * <p>
 * After PSound scans a file for audio, you can save the search results to a
 * .ppl file. The developer of PSound (sailrush) appears to have deliberately
 * obfuscated or encrypted the .ppl file information, specifically the offset
 * in the source file where the audio data starts. Unlike jPSXdec, his source
 * code is not available to understand how or why.
 */
public class PSoundPpl extends AbstractList<PSoundPpl.Entry> {

    private static final String PSND = "PSnd";

    private final int _iVersion;
    @Nonnull
    private final Entry[] _aoEntries;

    public PSoundPpl(@Nonnull InputStream is)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        byte[] abPsnd = IO.readByteArray(is, 4);
        String sPsnd = Misc.asciiToString(abPsnd);
        if (!PSND.equals(sPsnd))
            throw new BinaryDataNotRecognized();
        _iVersion = IO.readSInt32LE(is);
        if (_iVersion != 1)
            throw new BinaryDataNotRecognized();
        int iEntryCount = IO.readSInt32LE(is);
        if (iEntryCount < 1)
            throw new BinaryDataNotRecognized();
        _aoEntries = new Entry[iEntryCount];
        for (int i = 0; i < _aoEntries.length; i++) {
            _aoEntries[i] = new Entry(is, i);
        }
    }

    @Override
    public @Nonnull Entry get(int index) {
        return _aoEntries[index];
    }

    @Override
    public int size() {
        return _aoEntries.length;
    }

    @Override
    public String toString() {
        return "Version " + _iVersion + ", " + _aoEntries.length + " entries";
    }



    public static class Entry {
        @Nonnull
        private final String _sItemName;
        @Nonnull
        private final String _sSourceFilePath;
        /** The offset in the source file where the audio start, but
         * encrypted using pure poop. */
        private final long _lngEncryptedOffset;
        /** Not always zero apparently. */
        private final byte[] _abUnknown5Bytes = new byte[5];

        /** The offset in the source file where the audio start, unencrypted
         * using pure awesome. */
        private final long _lngDecryptedOffset;

        /** 255 characters with room for a null terminator. */
        private static final int ITEM_NAME_BUFFER_SIZE = 256;
        /** Standard Windows MAX_PATH length. */
        private static final int FILE_NAME_BUFFER_SIZE = 260;

        private Entry(@Nonnull InputStream is, int iIndex)
                throws EOFException, IOException, BinaryDataNotRecognized
        {
            // buffer to store the item name and file name
            // the reason for this will become clear later
            byte[] abItemNameAndFileName = new byte[ITEM_NAME_BUFFER_SIZE + FILE_NAME_BUFFER_SIZE];

            int iItemNameLength = IO.readUInt8(is);
            if (iItemNameLength == 0)
                throw new BinaryDataNotRecognized();
            IO.readByteArray(is, abItemNameAndFileName, 0, iItemNameLength);
            // add null terminator (not really necessary since Java arrays are 0)
            abItemNameAndFileName[iItemNameLength] = 0;

            int iFileNameLength = IO.readUInt8(is);
            if (iFileNameLength == 0)
                throw new BinaryDataNotRecognized();
            IO.readByteArray(is, abItemNameAndFileName, ITEM_NAME_BUFFER_SIZE, iFileNameLength);
            // add null terminator (not really necessary since Java arrays are 0)
            abItemNameAndFileName[ITEM_NAME_BUFFER_SIZE+iFileNameLength] = 0;

            // extract the strings
            _sItemName = Misc.asciiToString(abItemNameAndFileName, 0, iItemNameLength);
            _sSourceFilePath = Misc.asciiToString(abItemNameAndFileName, ITEM_NAME_BUFFER_SIZE, iFileNameLength);

            _lngEncryptedOffset = IO.readUInt32LE(is);

            // the 5 unknown zero bytes appear to be used for something
            // but need to see an example file with non-zero values
            // to get a lead as to what
            IO.readByteArray(is, _abUnknown5Bytes);

            /*.................................................................
            Now to decrypt the source file offset (i.e. what kind of crap is this?)

            ; The state of the machine:
            ;   eax        = the encrypted offset as read from the ppl file
            ;   [ebp-228h] = name of the item
            ;   [ebp-12Dh] = 5 bytes before the name of the file
            ;   [ebp-128h] = name of the file
            ;   [ebp-18h]  = empty variable (for our purposes)
            ;   [ebp-10h]  = empty variable (for our purposes)
            ;   [ebp-0Ch]  = entry index
            ;   [ebp+08h]  = length of the file name as read from the ppl file
            ;   [ebp+0Ch]  = item name length + file name length (how many bits to rotate)

            mov     [ebp-10h], eax          ; save the encrypted offset

            movzx   eax, byte ptr [ebp+08h] ; file name length
            mov     eax, [ebp+eax-12Dh]     ; essentially *((int*)&filename[eax-5])
            mov     [ebp-18h], eax          ; save that value

            mov     eax, [ebp-10h]          ; restore the encrypted offset
            not     eax                     ; ~not the value
            mov     cl, byte ptr [ebp+0Ch]  ; how many bits to rotate
            ror     eax, cl                 ; rotate the bits
            mov     ecx, [ebp-18h]          ; get the value read from above
            xor     eax, ecx                ; xor the two

            sub     eax, [ebp-0Ch]          ; substract the index of the entry
            mov     [ebp-10h], eax          ; store the unencrypted value
            */

            // stack
            int bitsToRotate = iItemNameLength + iFileNameLength;
            int temp;

            // registers
            int eax;
            int cl;
            int ecx;

            eax = iFileNameLength;
            eax = IO.readSInt32LE(abItemNameAndFileName, ITEM_NAME_BUFFER_SIZE + eax - 5);
            temp = eax;
            eax = (int)_lngEncryptedOffset;
            eax = ~eax;
            cl = bitsToRotate & 0x1F;
            eax = (eax >>> cl) | (eax << (32-cl)); // ror
            ecx = temp;
            eax = eax ^ ecx;
            eax = eax - iIndex;

            _lngDecryptedOffset = (long)eax & 0xffffffffL;
            //.................................................................

            // it is interesting to note that PSound gets weird when the
            // source file name is only 4 characters (e.g. C:\x)
        }

        public @Nonnull String getItemName() {
            return _sItemName;
        }

        public @Nonnull String getSourceFilePath() {
            return _sSourceFilePath;
        }

        public long getOffset() {
            return _lngDecryptedOffset;
        }

        @Override
        public String toString() {
            return String.format("\"%s\" \"%s\" %02x%02x%02x%02x%02x? %08x -> %d",
                                 _sSourceFilePath, _sItemName,
                                 _abUnknown5Bytes[0],
                                 _abUnknown5Bytes[1],
                                 _abUnknown5Bytes[2],
                                 _abUnknown5Bytes[3],
                                 _abUnknown5Bytes[4],
                                 _lngEncryptedOffset, _lngDecryptedOffset
                                 );
        }

    }

}
