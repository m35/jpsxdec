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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/** Writes a VAG ("very audio good") file.
 * <p>
 * VAG files seem to be a common format involved in PlayStation 1 development.
 * It contains a single sound clip of SPU audio.
 * <p>
 * The file contains the following data.
 * <ul>
 *   <li> Magic 'VAGp' (pGAV in little-endian)
 *   <li> Version (0x20)
 *   <li> Reserved zeros
 *   <li> Length of the audio
 *   <li> Sample rate
 *   <li> More reserved zeros
 *   <li> Textual identifier
 *   <li> More reserved zeros
 *   <li> SPU audio
 * </ul>
 * <p>
 * This simply wraps SPU audio into the VAG format. No effort is made
 * to verify that the SPU audio is valid. */
public class VagWriter implements Closeable {

    private static final Logger LOG = Logger.getLogger(VagWriter.class.getName());

    /** Returns if the given identifier is valid for the VAG file.
     * VAG files contain a 16 character identifier that can only be
     * letters and numbers. */
    public static boolean isValidId(@Nonnull String sId) {
        if (sId.length() > 16)
            return false;
        for (int i = 0; i < sId.length(); i++) {
            if (!Character.isLetterOrDigit(sId.charAt(i)))
                return false;
        }
        return true;
    }

    private static final int VERSION  = 0x00000020;
    private static final int RESERVED = 0x00000000;
    private static final int POSITION_OF_SOUND_LENGTH_IN_VAG_FILE = 12;

    /** Needs to be {@link RandomAccessFile} since we have to jump back
     * to the start to write the length of the audio (similar to .wav and .avi
     * files). */
    @Nonnull
    private final RandomAccessFile _vagFile;

    @Nonnull
    private final String _sId;
    private final int _iSampleRate;
    /** Tacks the number of sound units that have been written. */
    private int _iWrittenSoundUnitCount = 0;

    /** {@link #VagWriter(java.io.File, java.lang.String, int)} with the
     * string filename converted to {@link File}. */
    public VagWriter(@Nonnull String sOutputFile, @Nonnull String sId, int iSampleRate)
            throws FileNotFoundException, IOException
    {
        this(new File(sOutputFile), sId, iSampleRate);
    }
    /** Creates a VAG file with then given identifier and sample rate.
     * @throws FileNotFoundException If failed to open output file
     * @throws IOException If error writing header
     * @throws IllegalArgumentException If the identifier does not pass
     *                                  {@link #isValidId(java.lang.String)}
     */
    public VagWriter(@Nonnull File outputFile, @Nonnull String sId, int iSampleRate)
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        // verify valid ID
        if (!isValidId(sId))
            throw new IllegalArgumentException(sId);

        _sId = sId;
        _iSampleRate = iSampleRate;

        _vagFile = new RandomAccessFile(outputFile, "rw");
        try {
            _vagFile.write(Misc.stringToAscii("VAGp"));
            IO.writeInt32LE(_vagFile, VERSION);
            IO.writeInt32LE(_vagFile, RESERVED);
            IO.writeInt32LE(_vagFile, 0); // will write the audio length here on close
            IO.writeInt32LE(_vagFile, _iSampleRate);
            IO.writeZeros(_vagFile, 12); // reserved
            _vagFile.write(Misc.stringToAscii(_sId));
            IO.writeZeros(_vagFile, 16 - _sId.length());
            IO.writeZeros(_vagFile, 16); // ???
        } catch (IOException ex) {
            IO.closeSilently(_vagFile, LOG);
            throw ex;
        }
    }

    /** {@link #writeSoundUnit(byte[], int)} at offset 0. */
    public void writeSoundUnit(@Nonnull byte[] abSoundUnit) throws IOException {
        writeSoundUnit(abSoundUnit, 0);
    }
    /** Writes 16 bytes from the given buffer to the VAG file.
     * @throws IndexOutOfBoundsException If the offset is invalid or there are
     *                                   not 16 bytes available.
     */
    public void writeSoundUnit(@Nonnull byte[] abSoundUnit, int iOffset) throws IOException {
        _vagFile.write(abSoundUnit, iOffset, SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT);
        _iWrittenSoundUnitCount++;
    }

    /** {@inheritDoc }
     * Updates the VAG header with the audio length and closes the file. */
    @Override
    public void close() throws IOException {
        boolean blnExceptionThrown = true;
        try {
            _vagFile.seek(POSITION_OF_SOUND_LENGTH_IN_VAG_FILE);
            IO.writeInt32LE(_vagFile, _iWrittenSoundUnitCount * SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT);
            blnExceptionThrown = false;
        } finally {
            if (blnExceptionThrown)
                IO.closeSilently(_vagFile, LOG);
            else
                _vagFile.close();
        }
    }
}
