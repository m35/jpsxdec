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

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorClaimSystem;

/** Watches for XA audio streams.
 * Tracks the channel numbers and maintains all the XA streams.
 * Adds them to the media list as they end. */
public class DiscIndexerXaAudio extends DiscIndexer implements IdentifiedSectorListener<IIdentifiedSector> {

    @Nonnull
    private final ILocalizedLogger _errLog;

    public DiscIndexerXaAudio(@Nonnull ILocalizedLogger errLog) {
        _errLog = errLog;
    }

    @Override
    public void listPostProcessing(Collection<DiscItem> allItems) {
    }
    @Override
    public boolean filterChild(DiscItem parent, DiscItem child) {
        return false;
    }
    @Override
    public void indexGenerated(@Nonnull DiscIndex index) {
    }

    /** Tracks the indexing of one audio stream in one channel. */
    private static class AudioStreamIndex {

        /** First sector of the audio stream. */
        private final int _iStartSector;

        @Nonnull
        private final XaAudioFormat _format;

        private final BitSet _sectorsWithAudio = new BitSet();
        private int _iSectorCount;
        private int _iEndSector;

        /** Number of sectors between XA sectors that are part of this stream.
         * Should only ever be 1, 2, 4, 8, 16, or 32.
         * Is -1 until 2nd sector is discovered. */
        private int _iAudioStride = -1;

        @Nonnull
        private final ILocalizedLogger _errLog;

        public AudioStreamIndex(@Nonnull SectorXaAudio first, @Nonnull ILocalizedLogger errLog) {
            _format = new XaAudioFormat(first);
            _iEndSector = _iStartSector = first.getSectorNumber();
            _iSectorCount = 1;
            if (!first.isSilent())
                _sectorsWithAudio.set(0);
            _errLog = errLog;
        }

        /**
         * @return true if the sector was accepted as part of this stream,
         *         or false if the stream is finished.
         */
        public boolean sectorRead(@Nonnull SectorXaAudio nextXa) {

            if (!_format.equals(new XaAudioFormat(nextXa)))
                return false;

            int iStride = nextXa.getSectorNumber() - _iEndSector;

            if (iStride != 1) {
                DiscSpeed discSpeed = _format.calculateDiscSpeed(iStride);
                if (discSpeed == null)
                    return false;
            }

            // check the stride
            if (_iAudioStride < 0)
                _iAudioStride = iStride;
            else if (iStride != _iAudioStride)
                return false;

            _iEndSector = nextXa.getSectorNumber();
            if (!nextXa.isSilent())
                _sectorsWithAudio.set(_iSectorCount);

            _iSectorCount++;

            return true; // the sector was accepted
        }

        public void createMediaItem(@Nonnull DiscIndexerXaAudio adder) {
            if (_iSectorCount == 1 && _sectorsWithAudio.isEmpty()) {
                _errLog.log(Level.INFO, I.IGNORING_SILENT_XA_SECTOR(_iStartSector, _format.iChannel));
                return;
            }
            adder.addDiscItem(new DiscItemXaAudioStream(
                              adder.getCd(),
                              _iStartSector, _iEndSector,
                              _format,
                              _iAudioStride,
                              _sectorsWithAudio));
        }

        public boolean ended(int iSectorNum) {
            return (_iAudioStride >= 0) &&
                   (iSectorNum > _iEndSector + _iAudioStride);
        }
    }

    private static class FileChannel implements Comparable<FileChannel> {
        public final int iFileNumber;
        public final int iChanel;

        public FileChannel(int iFileNumber, int iChanel) {
            this.iFileNumber = iFileNumber;
            this.iChanel = iChanel;
        }

        public FileChannel(@Nonnull SectorXaAudio xaSector) {
            this.iFileNumber = xaSector.getFileNumber();
            this.iChanel = xaSector.getChannel();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + this.iFileNumber;
            hash = 89 * hash + this.iChanel;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            FileChannel other = (FileChannel) obj;
            return this.iFileNumber == other.iFileNumber &&
                   this.iChanel     == other.iChanel;
        }

        @Override
        public int compareTo(FileChannel o) {
            int i = Integer.compare(iFileNumber, o.iFileNumber);
            return i != 0 ? i : Integer.compare(iChanel, o.iChanel);
        }

        @Override
        public String toString() {
            return "File:" + iFileNumber + " Channel:" + iChanel;
        }

    }

    private final TreeMap<FileChannel, AudioStreamIndex> _channels
            = new TreeMap<FileChannel, AudioStreamIndex>();

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem serial)
            throws LocalizedDeserializationFail
    {
        if (DiscItemXaAudioStream.TYPE_ID.equals(serial.getType()))
            return new DiscItemXaAudioStream(getCd(), serial);
        return null;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        scs.addIdListener(this);
    }

    @Override
    public @Nonnull Class<IIdentifiedSector> getListeningFor() {
        return IIdentifiedSector.class;
    }

    @Override
    public void feedSector(@Nonnull IIdentifiedSector idSector, @Nonnull ILocalizedLogger log) {
        if (idSector instanceof SectorXaAudio) {
            SectorXaAudio xaSector = (SectorXaAudio) idSector;
            FileChannel fc = new FileChannel(xaSector);
            AudioStreamIndex audStream = _channels.get(fc);
            if (audStream == null) {
                _channels.put(fc, new AudioStreamIndex(xaSector, _errLog));
            } else if (!audStream.sectorRead(xaSector)) {
                audStream.createMediaItem(this);
                _channels.put(fc, new AudioStreamIndex(xaSector, _errLog));
            }
        }

        // check for streams that are beyond their stride and close them
        Iterator<Map.Entry<FileChannel, AudioStreamIndex>> it = _channels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FileChannel, AudioStreamIndex> channelStream = it.next();
            AudioStreamIndex s = channelStream.getValue();
            if (s != null && s.ended(idSector.getSectorNumber())) {
                s.createMediaItem(this);
                it.remove();
            }
        }

        // if the sector's EOF bit was set, that channel's stream is closed
        // this is important for many games
        CdSectorXaSubHeader sh = idSector.getCdSector().getSubHeader();
        if (sh != null && sh.getSubMode().getEndOfFile()) {
            FileChannel fc = new FileChannel(sh.getFileNumber(), sh.getChannel());
            AudioStreamIndex audStream = _channels.get(fc);
            if (audStream != null) {
                audStream.createMediaItem(this);
                _channels.remove(fc);
            }
        }
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) {
        for (AudioStreamIndex audStream : _channels.values()) {
            audStream.createMediaItem(this);
        }
        _channels.clear();
    }

}
