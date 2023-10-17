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

package jpsxdec.modules.square;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.adpcm.SpuAdpcmDecoder;
import jpsxdec.adpcm.SpuAdpcmEncoder;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.i18n.I;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.IO;

/** Square audio sectors come in pairs. This class is awesome because it
 * takes care of reliably identifying if the pairs match and is the source
 * of truth on sample rate, sample count, and start/end sectors. */
public class SquareAudioSectorPair {

    private static final Logger LOG = Logger.getLogger(SquareAudioSectorPair.class.getName());

    @CheckForNull
    private final ISquareAudioSector _leftSector;
    @CheckForNull
    private final ISquareAudioSector _rightSector;
    private final int _iHeaderFrameNumber;
    private final int _iSampleFramesPerSecond;
    private final int _iSoundUnitCount;
    private final int _iStartSector;
    private final int _iEndSector;

    public SquareAudioSectorPair(@CheckForNull ISquareAudioSector leftSector,
                                 @CheckForNull ISquareAudioSector rightSector,
                                 int iHeaderFrameNumber,
                                 int iSampleFramesPerSecond,
                                 int iSoundUnitCount,
                                 int iStartSector, int iEndSector)
    {
        if (leftSector == null && rightSector == null)
            throw new IllegalArgumentException();
        _leftSector = leftSector;
        _rightSector = rightSector;
        _iHeaderFrameNumber = iHeaderFrameNumber;
        _iSampleFramesPerSecond = iSampleFramesPerSecond;
        _iSoundUnitCount = iSoundUnitCount;
        _iStartSector = iStartSector;
        _iEndSector = iEndSector;
    }

    public boolean matchesPrevious(@Nonnull SquareAudioSectorPair prev) {
        if (_iSampleFramesPerSecond != prev._iSampleFramesPerSecond)
            return false;
        if (_iHeaderFrameNumber <= prev._iHeaderFrameNumber)
            return false;
        return true;
    }

    public int getHeaderFrameNumber() {
        return _iHeaderFrameNumber;
    }

    public int getSampleFramesPerSecond() {
        return _iSampleFramesPerSecond;
    }

    public int getSoundUnitCount() {
        return _iSoundUnitCount;
    }

    public int getStartSector() {
        return _iStartSector;
    }

    public int getEndSector() {
        return _iEndSector;
    }

    public int getPresentationSector() {
        if (_rightSector != null)
            return _rightSector.getSectorNumber();
        else
            return _leftSector.getSectorNumber() + 1;
    }

    /** @return Number of PCM sample frames decoded. */
    public int decode(@Nonnull SpuAdpcmDecoder.Stereo decoder, @Nonnull OutputStream out)
            throws EOFException, IOException
    {
        final InputStream left;
        final InputStream right;
        if (_leftSector == null) {
            left = new IO.ZeroInputStream();
            right = _rightSector.getIdentifiedUserDataStream();
        } else {
            left = _leftSector.getIdentifiedUserDataStream();
            if (_rightSector == null) {
                right = new IO.ZeroInputStream();
            } else { // both != null
                right = _rightSector.getIdentifiedUserDataStream();
            }
        }
        return decoder.decode(left, right, getSoundUnitCount(), out);
    }

    public void replace(@Nonnull SpuAdpcmEncoder.Stereo encoder,
                        @Nonnull DiscPatcher patcher,
                        @Nonnull ILocalizedLogger log)
            throws DiscPatcher.WritePatchException, EOFException, IOException
    {
        ByteArrayFPIS leftSoundUnitReader = null;
        if (_leftSector != null)
            leftSoundUnitReader = _leftSector.getIdentifiedUserDataStream();

        ByteArrayFPIS rightSoundUnitReader = null;
        if (_rightSector != null)
            rightSoundUnitReader = _rightSector.getIdentifiedUserDataStream();

        long lngStartOfSamples = encoder.getSampleFramesReadAndEncoded();

        ExposedBAOS encodedLeft = new ExposedBAOS();
        ExposedBAOS encodedRight = new ExposedBAOS();

        for (int iSoundUnitsReplaced = 0; iSoundUnitsReplaced < getSoundUnitCount(); iSoundUnitsReplaced++) {
            byte bLeftFlagBits = 0;
            if (leftSoundUnitReader != null) {
                SpuAdpcmSoundUnit su = new SpuAdpcmSoundUnit(leftSoundUnitReader);
                bLeftFlagBits = su.getFlagBits();
            }
            byte bRightFlagBits = 0;
            if (rightSoundUnitReader != null) {
                SpuAdpcmSoundUnit su = new SpuAdpcmSoundUnit(rightSoundUnitReader);
                bRightFlagBits = su.getFlagBits();
            }

            byte[][] aabEncoded = encoder.encode1SoundUnit(bLeftFlagBits, bRightFlagBits);
            encodedLeft.write(aabEncoded[0]);
            encodedRight.write(aabEncoded[1]);
        }

        String sDbgFormat = "Replacing {0,number,#} out of {1,number,#} bytes in sector {2} with samples starting at {3,number,#}";

        if (_leftSector != null) {
            LOG.log(Level.INFO, sDbgFormat, new Object[]{encodedLeft.size(), _leftSector.getAudioDataSize(), _leftSector, lngStartOfSamples});
            log.log(Level.INFO, I.WRITING_SAMPLES_TO_SECTOR(lngStartOfSamples, _leftSector.toString()));
            patcher.addPatch(_leftSector.getSectorNumber(), _leftSector.getAudioDataStartOffset(), encodedLeft.toByteArray());
        }
        if (_rightSector != null) {
            LOG.log(Level.INFO, sDbgFormat, new Object[]{encodedRight.size(), _rightSector.getAudioDataSize(), _rightSector, lngStartOfSamples});
            log.log(Level.INFO, I.WRITING_SAMPLES_TO_SECTOR(lngStartOfSamples, _rightSector.toString()));
            patcher.addPatch(_rightSector.getSectorNumber(), _rightSector.getAudioDataStartOffset(), encodedRight.toByteArray());
        }
    }

}
