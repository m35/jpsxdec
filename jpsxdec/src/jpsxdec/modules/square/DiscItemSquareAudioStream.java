/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2020  Michael Sabin
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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.adpcm.SoundUnitDecoder;
import jpsxdec.adpcm.SpuAdpcmEncoder;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.sharedaudio.DecodedAudioPacket;
import jpsxdec.modules.sharedaudio.DiscItemAudioStream;
import jpsxdec.modules.sharedaudio.ISectorAudioDecoder;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.Misc;
import jpsxdec.util.TaskCanceledException;

/** Represents a series of Square SPU ADPCM sectors that combine to make an audio stream.
 * These kinds of streams are found in Final Fantasy 8, 9, and Chrono Cross.  */
public class DiscItemSquareAudioStream extends DiscItemAudioStream {

    private static final Logger LOG = Logger.getLogger(DiscItemSquareAudioStream.class.getName());

    public static final String TYPE_ID = "SquareAudio";

    private static final String SOUND_UNIT_COUNT_KEY = "Sound unit count";
    private final int _iSoundUnitCount;

    private static final String SAMPLES_PER_SEC_KEY = "Samples/Sec";
    private final int _iSampleFramesPerSecond;

    private static final String SECTORS_PAST_END_KEY = "Sectors past end";
    private final int _iSectorsPastEnd;

    public DiscItemSquareAudioStream(@Nonnull CdFileSectorReader cd,
                                     int iStartSector, int iEndSector,
                                     int iSoundUnitCount,
                                     int iSampleFramesPerSecond,
                                     int iSectorsPastEnd)
    {
        super(cd, iStartSector, iEndSector);

        _iSoundUnitCount = iSoundUnitCount;
        _iSampleFramesPerSecond = iSampleFramesPerSecond;
        _iSectorsPastEnd = iSectorsPastEnd;
    }

    public DiscItemSquareAudioStream(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);

        _iSampleFramesPerSecond = fields.getInt(SAMPLES_PER_SEC_KEY);
        _iSoundUnitCount = fields.getInt(SOUND_UNIT_COUNT_KEY);
        _iSectorsPastEnd = fields.getInt(SECTORS_PAST_END_KEY);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem fields = super.serialize();
        fields.addNumber(SAMPLES_PER_SEC_KEY, _iSampleFramesPerSecond);
        fields.addNumber(SOUND_UNIT_COUNT_KEY, _iSoundUnitCount);
        fields.addNumber(SECTORS_PAST_END_KEY, _iSectorsPastEnd);
        return fields;
    }

    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public boolean isStereo() {
        return true;
    }

    @Override
    public int getSectorsPastEnd() {
        return _iSectorsPastEnd;
    }

    @Override
    public int getDiscSpeed() {
        return 2;
    }

    @Override
    public int getPresentationStartSector() {
        return getStartSector() + 1; // TODO: this isn't quite right
    }

    @Override
    public long getSampleFrameCount() {
        return _iSoundUnitCount * (long)SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
    }

    @Override
    public double getApproxDuration() {
        return getSampleFrameCount() / (double)_iSampleFramesPerSecond;
    }

    @Override
    public int getSampleFramesPerSecond() {
        return _iSampleFramesPerSecond;
    }

    @Override
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        // unable to find ANY sources of info about how to localize durations
        Date secs = Misc.dateFromSeconds((int)Math.max(getSampleFrameCount() / _iSampleFramesPerSecond, 1));
        return I.GUI_AUDIO_DESCRIPTION(secs, _iSampleFramesPerSecond, 2);
    }

    @Override
    public @Nonnull ISectorAudioDecoder makeDecoder(double dblVolume) {
        return new SquareConverter(dblVolume);
    }

    // -------------------------------------------------------------------------

    private class SquareConverter implements ISectorAudioDecoder {

        @Nonnull
        private final SquareAudioSectorPairToAudioPacket _p2p;
        @Nonnull
        private final SquareAudioSectorToSquareAudioSectorPair _sa2p;

        public SquareConverter(double dblVolume) {
             _p2p = new SquareAudioSectorPairToAudioPacket(dblVolume);
             _sa2p = new SquareAudioSectorToSquareAudioSectorPair(makeSectorRange(), _p2p);
        }

        @Override
        public void setAudioListener(@Nonnull DecodedAudioPacket.Listener listener) {
            _p2p.setListener(listener);
        }

        @Override
        public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
            scs.addIdListener(_sa2p);
        }

        @Override
        public double getVolume() {
            return _p2p.getVolume();
        }

        @Override
        public @Nonnull AudioFormat getOutputFormat() {
            return _p2p.getFormat(_iSampleFramesPerSecond);
        }

        @Override
        public int getSampleFramesPerSecond() {
            return _iSampleFramesPerSecond;
        }

        @Override
        public int getDiscSpeed() {
            return 2;
        }

        @Override
        public int getEndSector() {
            return DiscItemSquareAudioStream.this.getEndSector();
        }

        @Override
        public int getStartSector() {
            return DiscItemSquareAudioStream.this.getStartSector();
        }

        @Override
        public int getAbsolutePresentationStartSector() {
            return DiscItemSquareAudioStream.this.getPresentationStartSector();
        }
    }

    // =========================================================================
    // replace

    @Override
    public void replace(@Nonnull ProgressLogger pl, @Nonnull File audioFile)
            throws IOException,
                   UnsupportedAudioFileException,
                   LocalizedIncompatibleException,
                   CdFileSectorReader.CdReadException,
                   DiscPatcher.WritePatchException,
                   TaskCanceledException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
        try {
            AudioFormat fmt = ais.getFormat();
            boolean blnFormatEquals = Math.abs(fmt.getSampleRate() - _iSampleFramesPerSecond) < 1f;
            blnFormatEquals &= ( isStereo()  && (fmt.getChannels() == 2) )
                                ||
                               ( !isStereo() && (fmt.getChannels() == 1) );
            blnFormatEquals &= ais.getFrameLength() == getSampleFrameCount();

            if (!blnFormatEquals) {
                throw new LocalizedIncompatibleException(
                I.AUDIO_REPLACE_FORMAT_MISMATCH(
                    ais.getFrameLength(), fmt.getChannels(), fmt.getSampleRate(),
                    getSampleFrameCount(), isStereo() ? 2 : 1, getSampleFramesPerSecond()
                ));
            }

            SpuAdpcmEncoder.Stereo encoder;
            try {
                encoder = new SpuAdpcmEncoder.Stereo(ais);
            } catch (IncompatibleException ex) {
                throw new RuntimeException("This should have already been checked above", ex);
            }

            // connect the pipeline
            PairCollector pairCollector = new PairCollector();
            SquareAudioSectorToSquareAudioSectorPair sas2sasp = new SquareAudioSectorToSquareAudioSectorPair(SectorRange.ALL, pairCollector);
            SectorClaimSystem it = createClaimSystem();
            it.addIdListener(sas2sasp);

            int iSoundUnitsReplaced = 0;
            int iSomewhatAccurateSectorProgress = getStartSector();

            pl.progressStart(_iSoundUnitCount);
            while (iSoundUnitsReplaced < _iSoundUnitCount) {
                it.next(pl);

                while (pairCollector.hasNext()) {
                    SquareAudioSectorPair pair = pairCollector.next();
                    iSoundUnitsReplaced += pair.replace(encoder, _iSoundUnitCount - iSoundUnitsReplaced, getSourceCd(), pl);
                    iSomewhatAccurateSectorProgress = pair.getEndSector();
                }
                if (pl.isSeekingEvent())
                    pl.event(I.CMD_PATCHING_SECTOR_NUMBER(iSomewhatAccurateSectorProgress));

                pl.progressUpdate(iSoundUnitsReplaced);
            }
            pl.progressEnd();
        } finally {
            IO.closeSilently(ais, LOG);
        }
    }

    private static class PairCollector implements SquareAudioSectorToSquareAudioSectorPair.Listener,
                                                  Iterator<SquareAudioSectorPair>
    {
        private final LinkedList<SquareAudioSectorPair> _pairs = new LinkedList<SquareAudioSectorPair>();

        @Override
        public boolean hasNext() { return !_pairs.isEmpty(); }
        @Override
        public @Nonnull SquareAudioSectorPair next() throws NoSuchElementException {
            return _pairs.remove();
        }
        @Override
        public void remove() { throw new UnsupportedOperationException(); }

        @Override
        public void pairDone(@Nonnull SquareAudioSectorPair pair, @Nonnull ILocalizedLogger log) {
            _pairs.add(pair);
        }

        @Override
        public void endOfSectors(@Nonnull ILocalizedLogger log) {
            // should not happen
            LOG.warning("Why are we hitting end of sectors while replacing?");
        }
    }

}
