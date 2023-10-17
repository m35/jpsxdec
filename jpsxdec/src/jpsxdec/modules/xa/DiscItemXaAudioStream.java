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

package jpsxdec.modules.xa;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.adpcm.XaAdpcmDecoder;
import jpsxdec.adpcm.XaAdpcmEncoder;
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.formats.Signed16bitLittleEndianLinearPcmAudioInputStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.audio.sectorbased.DiscItemSectorBasedAudioStream;
import jpsxdec.modules.audio.sectorbased.ISectorClaimToSectorBasedDecodedAudio;
import jpsxdec.modules.audio.sectorbased.SectorBasedDecodedAudioPacket;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.Misc;
import jpsxdec.util.TaskCanceledException;

/** Represents a series of XA ADPCM sectors that combine to make an audio stream. */
public class DiscItemXaAudioStream extends DiscItemSectorBasedAudioStream {

    private static final Logger LOG = Logger.getLogger(DiscItemXaAudioStream.class.getName());

    /** Type identifier for this disc item. */
    public static final String TYPE_ID = "XA";

    @Nonnull
    private final XaAudioFormat _format;

    /** Serialization key for audio sector stride. */
    private final static String STRIDE_KEY = "Sector stride";
    /** Number of non-audio sectors between each audio sector.
     *  Should be 1, 4, 8, 16, or 32, or -1 if unknown (only one sector of audio).*/
    private final int _iSectorStride;

    /** Serialization key for disc speed. */
    private final static String DISC_SPEED_KEY = "Disc speed";
    /** Speed that the disc must spin to properly play the audio stream.
     * null if unknown (unknown should only occur if this audio stream is only one sector long)
     * or invalid (sector stride is 1) */
    @CheckForNull
    private final DiscSpeed _discSpeed;

    @CheckForNull
    private final BitSet _sectorsWithAudio;

    public DiscItemXaAudioStream(@Nonnull ICdSectorReader cd,
                                 int iStartSector, int iEndSector,
                                 @Nonnull XaAudioFormat format,
                                 int iStride,
                                 @CheckForNull BitSet sectorsWithAudio)
    {
        super(cd, iStartSector, iEndSector);

        if (iStride != -1 && iStride != 1 && iStride != 2 &&
            iStride != 4 && iStride != 8 && iStride != 16 && iStride != 32)
            throw new IllegalArgumentException("Illegal audio sector stride " + iStride);

        _format = format;
        _iSectorStride = iStride;

        // if there is no sector stride (iStride == -1, 1 sector long)
        // or the stride is only 1, then the disc speed is unknown/invalid
        if (_iSectorStride == -1 || _iSectorStride == 1) {
            _discSpeed = null;
        } else {
            _discSpeed = _format.calculateDiscSpeed(_iSectorStride);
            if (_discSpeed == null)
                throw new RuntimeException(String.format(
                        "Disc speed calc doesn't add up: Samples/sec %d Stereo %s Bits/sample %s Stride %d",
                        _format.iSampleFramesPerSecond, String.valueOf(_format.blnIsStereo),
                        _format.iBitsPerSample, _iSectorStride));
        }

        _sectorsWithAudio = sectorsWithAudio;
    }

    public DiscItemXaAudioStream(@Nonnull ICdSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);

        _format = new XaAudioFormat(fields);

        _iSectorStride = fields.getInt(STRIDE_KEY);
        if (_iSectorStride != -1 && _iSectorStride != 1 && _iSectorStride != 2 &&
            _iSectorStride != 4 && _iSectorStride != 8 && _iSectorStride != 16 && _iSectorStride != 32)
            throw new LocalizedDeserializationFail(I.FIELD_HAS_INVALID_VALUE_NUM(STRIDE_KEY, _iSectorStride));

        String sDiscSpeed = fields.getString(DISC_SPEED_KEY);
        if ("1x".equals(sDiscSpeed))
            _discSpeed = DiscSpeed.SINGLE;
        else if ("2x".equals(sDiscSpeed))
            _discSpeed = DiscSpeed.DOUBLE;
        else if ("?".equals(sDiscSpeed))
            _discSpeed = null;
        else throw new LocalizedDeserializationFail(I.FIELD_HAS_INVALID_VALUE_STR(DISC_SPEED_KEY, sDiscSpeed));

        _sectorsWithAudio = null;
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem fields = super.serialize();

        _format.serialize(fields);

        fields.addNumber(STRIDE_KEY, _iSectorStride);
        if (_discSpeed == DiscSpeed.SINGLE)
            fields.addString(DISC_SPEED_KEY, "1x");
        else if (_discSpeed == DiscSpeed.DOUBLE)
            fields.addString(DISC_SPEED_KEY, "2x");
        else
            fields.addString(DISC_SPEED_KEY, "?");

        return fields;
    }

    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public @Nonnull GeneralType getType() {
        return GeneralType.Audio;
    }

    public int getChannel() {
        return _format.iChannel;
    }

    @Override
    public boolean isStereo() {
        return _format.blnIsStereo;
    }

    public int getSectorStride() {
        return _iSectorStride;
    }

    @Override
    public int getSectorsPastEnd() {
        return _iSectorStride - 1;
    }

    @Override
    public @CheckForNull DiscSpeed getDiscSpeed() {
        return _discSpeed;
    }

    @Override
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        Date secs = Misc.dateFromSeconds(Math.max((int)getApproxDuration(), 1));
        return I.GUI_AUDIO_DESCRIPTION(secs, _format.iSampleFramesPerSecond, _format.blnIsStereo ? 2 : 1);
    }

    @Override
    public int getSampleFramesPerSecond() {
        return _format.iSampleFramesPerSecond;
    }

    @Override
    public int getPresentationStartSector() {
        return getStartSector();
    }

    @Override
    public double getApproxDuration() {
        return getSampleFrameCount() / (double)_format.iSampleFramesPerSecond;
    }

    @Override
    public long getSampleFrameCount() {
        int iAudioSectorCount = getAudioSectorCount();
        return (long)iAudioSectorCount *
                XaAdpcmDecoder.pcmSampleFramesGeneratedFromXaAdpcmSector(_format.iBitsPerSample, _format.blnIsStereo);
    }

    private int getAudioSectorCount() {
        return calculateAudioSectorCount(getSectorLength(), _iSectorStride);
    }

    private static int calculateAudioSectorCount(int iSectorLength, int iSectorStride) {
        int iAudioSectorCount;
        if (iSectorLength == 1)
            iAudioSectorCount = 1;
        else {
            assert iSectorStride > 0;
            iAudioSectorCount = iSectorLength / iSectorStride + 1;
        }
        return iAudioSectorCount;
    }

    public boolean isConfirmedToBeSilent() {
        if (_sectorsWithAudio == null)
            return false; // without the bitset, we can't confirm if it is silent
        return _sectorsWithAudio.isEmpty();
    }

    @Override
    public String toString() {
        if (isConfirmedToBeSilent())
            return super.toString() + " // (SILENT)";
        else
            return super.toString();
    }

    @Override
    public @Nonnull ISectorClaimToSectorBasedDecodedAudio makeDecoder(double dblVolume) {
        return new XAConverter(dblVolume);
    }

    public @Nonnull XaAdpcmDecoder makeXaDecoder(double dblVolume) {
        return new XaAdpcmDecoder(_format.iBitsPerSample,
                                  isStereo(), dblVolume);
    }

    /** Returns an array with 2 elements. */
    public @Nonnull DiscItemXaAudioStream[] split(int iBeforeSector) {
        if (iBeforeSector <= getStartSector() || iBeforeSector > getEndSector())
            throw new IllegalArgumentException("Split sector outside the bounds of XA audio stream");

        int iFirstEnd = iBeforeSector - ((iBeforeSector - getStartSector()-1) % _iSectorStride) - 1;
        int iSecondStart = iBeforeSector + (_iSectorStride - (iBeforeSector - getStartSector()-1) % _iSectorStride) - 1;

        BitSet firstSectorsWithAudio = null;
        BitSet secondSectorsWithAudio = null;
        if (_sectorsWithAudio != null) {
            int iSectorsInFirst = calculateAudioSectorCount(iFirstEnd - getStartSector() + 1, _iSectorStride);
            if (iSectorsInFirst >= _sectorsWithAudio.length()) {
                firstSectorsWithAudio = _sectorsWithAudio;
                secondSectorsWithAudio = new BitSet();
            } else {
                firstSectorsWithAudio = _sectorsWithAudio.get(0, iSectorsInFirst);
                secondSectorsWithAudio = _sectorsWithAudio.get(iSectorsInFirst, _sectorsWithAudio.length());
            }
        }


        DiscItemXaAudioStream first = new DiscItemXaAudioStream(getSourceCd(), getStartSector(), iFirstEnd,
                                                                _format, _iSectorStride, firstSectorsWithAudio);
        DiscItemXaAudioStream second = new DiscItemXaAudioStream(getSourceCd(), iSecondStart, getEndSector(),
                                                                 _format, _iSectorStride, secondSectorsWithAudio);
        return new DiscItemXaAudioStream[] { first, second };
    }


    @Override
    public void replace(@Nonnull DiscPatcher patcher, @Nonnull File audioFile, @Nonnull ProgressLogger pl)
            throws IOException,
                   UnsupportedAudioFileException,
                   LocalizedIncompatibleException,
                   CdException.Read,
                   DiscPatcher.WritePatchException,
                   TaskCanceledException, LoggedFailure
    {
        Signed16bitLittleEndianLinearPcmAudioInputStream ais = openForReplace(audioFile, getSampleFramesPerSecond(), isStereo(), pl);
        Closeable streamToClose = ais;
        try {
            XaAdpcmEncoder encoder;
            try {
                streamToClose = encoder = new XaAdpcmEncoder(ais, _format.iBitsPerSample);
            } catch (IncompatibleException ex) {
                throw new RuntimeException("This should have been checked already", ex);
            }

            SectorClaimSystem it = createClaimSystem();
            pl.progressStart(getStartSector(), getEndSector());
            while (it.hasNext()) {
                IIdentifiedSector origIdSect = it.next(pl);
                if (origIdSect instanceof SectorXaAudio && isPartOfStream((SectorXaAudio)origIdSect)) {
                    long lngSampleFramesReadBefore = encoder.getSampleFramesRead();
                    pl.log(Level.INFO, I.WRITING_SAMPLES_TO_SECTOR(lngSampleFramesReadBefore, origIdSect.toString()));

                    byte[] abEncoded;
                    try {
                        abEncoded = encoder.encode1Sector();
                    } catch (EOFException ex) {
                        throw new LocalizedIncompatibleException(I.CMD_REPLACE_AUDIO_TOO_SHORT(ais.getSampleFramesRead(), getSampleFrameCount()), ex);
                    }

                    pl.log(Level.INFO, I.CMD_PATCHING_SECTOR_DESCRIPTION(origIdSect.toString())); // TODO kinda redundant logging here
                    if (pl.isSeekingEvent())
                        pl.event(I.CMD_PATCHING_SECTOR_NUMBER(origIdSect.getSectorNumber()));

                    patcher.addPatch(origIdSect.getSectorNumber(), 0, abEncoded);
                }
                pl.progressUpdate(origIdSect.getSectorNumber());
            }
            it.flush(pl);

            if (!ais.nextReadReturnsEndOfStream()) {
                throw new LocalizedIncompatibleException(I.CMD_REPLACE_AUDIO_TOO_LONG(getSampleFrameCount()));
            }
            pl.progressEnd();
        } finally {
            IO.closeSilently(streamToClose, LOG);
        }

    }

    public void replaceXa(@Nonnull DiscPatcher patcher, @Nonnull DiscItemXaAudioStream other, @Nonnull ProgressLogger pl)
            throws LocalizedIncompatibleException,
                   CdException.Read,
                   DiscPatcher.WritePatchException,
                   TaskCanceledException,
                   LoggedFailure
    {
        pl.log(Level.INFO, I.CMD_PATCHING_DISC_ITEM(this.toString()));
        pl.log(Level.INFO, I.CMD_PATCHING_WITH_DISC_ITEM(other.toString()));

        // if missing sector? no, that's not right unless the index is busted
        if (getSampleFramesPerSecond() != other.getSampleFramesPerSecond() ||
            _format.blnIsStereo    != other._format.blnIsStereo    ||
            _format.iBitsPerSample != other._format.iBitsPerSample ||
            getAudioSectorCount() != other.getAudioSectorCount())
        {
            // Don't want to use sector length in case one of them consists of
            // all adjacent sectors and the other one doesn't
            throw new LocalizedIncompatibleException(I.XA_REPLACE_FORMAT_MISMATCH(
                other._format.iBitsPerSample, other.getSampleFrameCount(),other._format.blnIsStereo ? 2 : 1,other.getSampleFramesPerSecond(),
                      _format.iBitsPerSample,       getSampleFrameCount(),      _format.blnIsStereo ? 2 : 1,      getSampleFramesPerSecond()));
        }
        // there should be no missing XA sectors in either of these items
        // if there are, the index is out of sync with the actual disc
        // and we won't guarantee what happens next (i.e. undefined behavior)

        SectorClaimSystem origIt = createClaimSystem();
        SectorClaimSystem patchIt = other.createClaimSystem();
        pl.progressStart(getSectorLength());
        for (int iSector = 0; origIt.hasNext(); iSector++) {
            IIdentifiedSector origIdSect = origIt.next(pl);
            if (origIdSect instanceof SectorXaAudio && isPartOfStream((SectorXaAudio)origIdSect)) {
                SectorXaAudio origXaSect = (SectorXaAudio) origIdSect;
                // seek to the next other XA sector
                IIdentifiedSector patchIdSect = null;
                do {
                    if (!patchIt.hasNext()) {
                        // if this happens, the index and disc of one,
                        // or both of the items is out of sync. just fail
                        throw new LocalizedIncompatibleException(I.XA_COPY_REPLACE_SRC_XA_EXHAUSTED());
                    }
                    patchIdSect = patchIt.next(pl);
                } while (!(patchIdSect instanceof SectorXaAudio && other.isPartOfStream((SectorXaAudio)patchIdSect)));
                SectorXaAudio patchXaSect = (SectorXaAudio) patchIdSect;
                pl.log(Level.INFO, I.CMD_PATCHING_SECTOR_DESCRIPTION(origXaSect.toString()));
                pl.log(Level.INFO, I.CMD_PATCHING_WITH_SECTOR_DESCRIPTION(patchXaSect.toString()));
                if (pl.isSeekingEvent())
                    pl.event(I.CMD_PATCHING_SECTOR_NUMBER(origIdSect.getSectorNumber()));
                byte[] abPatchData = patchXaSect.getCdSector().getCdUserDataCopy();
                patcher.addPatch(origXaSect.getSectorNumber(), 0, abPatchData);

                pl.progressUpdate(iSector);
            }
        }
        origIt.flush(pl);
        origIt.flush(pl);
        // hopefully all the sectors were coped at this point and no remain in this XA stream
        pl.progressEnd();
    }

    private class XAConverter implements ISectorClaimToSectorBasedDecodedAudio {

        @Nonnull
        private final SectorXaAudioToAudioPacket __xa2ap;

        public XAConverter(double dblVolume) {
            __xa2ap = new SectorXaAudioToAudioPacket(
                    DiscItemXaAudioStream.this.makeXaDecoder(dblVolume),
                    _format.iSampleFramesPerSecond, _format.iFileNumber, _format.iChannel,
                    DiscItemXaAudioStream.this.getStartSector(),
                    DiscItemXaAudioStream.this.getEndSector());
        }

        @Override
        public void setSectorBasedAudioListener(@Nonnull SectorBasedDecodedAudioPacket.Listener listener) {
            __xa2ap.setListener(listener);
        }

        @Override
        public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
            scs.addIdListener(__xa2ap);
        }

        @Override
        public double getVolume() {
            return __xa2ap.getVolume();
        }

        @Override
        public int getSampleFramesPerSecond() {
            return _format.iSampleFramesPerSecond;
        }

        @Override
        public @Nonnull AudioFormat getOutputFormat() {
            return __xa2ap.getOutputFormat();
        }

        @Override
        public int getEndSector() {
            return DiscItemXaAudioStream.this.getEndSector();
        }

        @Override
        public int getStartSector() {
            return DiscItemXaAudioStream.this.getStartSector();
        }

        @Override
        public int getAbsolutePresentationStartSector() {
            return DiscItemXaAudioStream.this.getPresentationStartSector();
        }

    }

    public boolean isPartOfStream(@Nonnull SectorXaAudio xaSector) {
        return xaSector.getSectorNumber() >= getStartSector() &&
               xaSector.getSectorNumber() <= getEndSector() &&
               _format.equals(new XaAudioFormat(xaSector));
    }

}
