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

package jpsxdec.modules.video.framenumber;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;

/** A frame number consisting of a frame index, and the start sector of that frame. */
public class IndexSectorFrameNumber extends FrameNumber {

    private static final Logger LOG = Logger.getLogger(IndexSectorFrameNumber.class.getName());

    private IndexSectorFrameNumber(int iFrameIndex, int iFrameCount,
                                   @Nonnull FrameNumberNumber sectorNumber)
    {
        super(iFrameIndex, iFrameCount, sectorNumber);
    }

    public static class Format {

        public static class Builder {
            private int _iFrameCount;

            @Nonnull
            private final FrameNumberNumber.Format.Builder _sectorNumberBuilder;

            public Builder(int iFirstFrameStartSector) {
                _sectorNumberBuilder = new FrameNumberNumber.Format.Builder(iFirstFrameStartSector);
                _iFrameCount = 1;
            }

            public void addFrameStartSector(int iSector) {
                _sectorNumberBuilder.addNumber(iSector);
                _iFrameCount++;
            }

            public @Nonnull Format makeFormat() {
                return new Format(_iFrameCount, _sectorNumberBuilder.makeFormat());
            }
        }


        private static final String FRAME_COUNT_KEY = "Frame Count";
        private final int _iFrameCount;

        private static final String SECTOR_FORMAT_KEY = "Sector Frames";
        @Nonnull
        private final FrameNumberNumber.Format _sectorNumberFormat;

        private Format(int iFrameCount, @Nonnull FrameNumberNumber.Format sectorNumberFormat) {
            _iFrameCount = iFrameCount;
            _sectorNumberFormat = sectorNumberFormat;
        }

        public Format(@Nonnull SerializedDiscItem serial) throws LocalizedDeserializationFail {
            _iFrameCount = serial.getInt(FRAME_COUNT_KEY);
            _sectorNumberFormat = new FrameNumberNumber.Format(serial.getString(SECTOR_FORMAT_KEY));
        }

        public void serialize(@Nonnull SerializedDiscItem serial) {
            serial.addNumber(FRAME_COUNT_KEY, _iFrameCount);
            serial.addString(SECTOR_FORMAT_KEY, _sectorNumberFormat.serialize());
        }

        public int getFrameCount() {
            return _iFrameCount;
        }

        public @Nonnull IndexSectorFrameNumber getStartFrame() {
            return new IndexSectorFrameNumber(0, _iFrameCount, _sectorNumberFormat.getStart());
        }

        public @Nonnull IndexSectorFrameNumber getEndFrame() {
            return new IndexSectorFrameNumber(_iFrameCount - 1, _iFrameCount, _sectorNumberFormat.getEnd());
        }


        public @Nonnull IFrameNumberFormatter makeFormatter() {
            return makePrivateTypeFormatter();
        }
        // for header to use
        @Nonnull Formatter makePrivateTypeFormatter() {
            return new Formatter(_iFrameCount, _sectorNumberFormat.makeFormatter());
        }

        @Override
        public String toString() {
            return _iFrameCount + " frames " + _sectorNumberFormat;
        }
    }

    static class Formatter implements IFrameNumberFormatter {
        private final int _iExpectedMaxFrameCount;
        private int _iCurrentFrameIndex = 0;

        @Nonnull
        private final FrameNumberNumber.Formatter _sectorFormatter;

        Formatter(int iExpectedMaxFrameCount,
                  @Nonnull FrameNumberNumber.Formatter sectorFormatter)
        {
            _iExpectedMaxFrameCount = iExpectedMaxFrameCount;
            _sectorFormatter = sectorFormatter;
        }

        private void warnIndexDigitsOutOfBounds(@Nonnull ILocalizedLogger log) {
            if (_iCurrentFrameIndex >= _iExpectedMaxFrameCount) {
                log.log(Level.WARNING, I.FRAMES_UNEXPECTED_NUMBER());
                LOG.log(Level.WARNING, "Expected no more than {0} frames but got {1}",
                                       new Object[]{_iExpectedMaxFrameCount - 1, _iCurrentFrameIndex});
            }
        }

        private static void warnSectorFrameNumberIssues(@Nonnull FrameNumberNumber sectorNumber,
                                                        @Nonnull ILocalizedLogger log)
        {
            if (sectorNumber.frameNumberOutOfMaxBounds() || sectorNumber.frameNumberOutOfMinBounds()) {
                log.log(Level.WARNING, I.FRAMES_UNEXPECTED_NUMBER());
                LOG.log(Level.WARNING,
                       "Frame start sector {0} out of expected bounds: {1} to {2}",
                       new Object[]{sectorNumber.getFrameValue(),
                                    sectorNumber.getExpectedFrameMinValue(),
                                    sectorNumber.getExpectedFrameMaxValue()});
            }
            if (sectorNumber.tooManyDuplicates()) {
                log.log(Level.WARNING, I.FRAMES_UNEXPECTED_NUMBER());
                LOG.log(Level.WARNING,
                        "Found {0} frames starting in sector {1}, but expected no more than {2}",
                        new Object[]{sectorNumber.getDuplicateIndex(),
                                     sectorNumber.getFrameValue(),
                                     sectorNumber.getExpectedDuplicateMax()});
            }
        }


        @Override
        public @Nonnull IndexSectorFrameNumber next(int iSector, @Nonnull ILocalizedLogger log) {
            warnIndexDigitsOutOfBounds(log);
            FrameNumberNumber sectorNumber = _sectorFormatter.next(iSector);
            warnSectorFrameNumberIssues(sectorNumber, log);
            IndexSectorFrameNumber fn = new IndexSectorFrameNumber(_iCurrentFrameIndex, _iExpectedMaxFrameCount, sectorNumber);
            _iCurrentFrameIndex++;
            return fn;
        }
    }

    public static @Nonnull IFrameNumberFormatter makeSimpleFormatter(
            int iFrameCount,
            int iSectorMinValue, int iSectorMaxValue, int iSectorDuplicateMax)
    {
        // set the end duplicate = max duplicate for simplicity
        FrameNumberNumber.Format snf = new FrameNumberNumber.Format(
                iSectorMinValue, iSectorMaxValue, iSectorDuplicateMax, iSectorDuplicateMax);
        FrameNumberNumber.Formatter sf = new FrameNumberNumber.Formatter(snf);

        IndexSectorFrameNumber.Formatter sect = new IndexSectorFrameNumber.Formatter(iFrameCount, sf);
        return sect;
    }

}
