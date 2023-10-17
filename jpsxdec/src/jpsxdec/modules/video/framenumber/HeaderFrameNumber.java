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

/** A frame number that contains an index, start sector, and header frame number. */
public class HeaderFrameNumber extends FrameNumber {

    private static final Logger LOG = Logger.getLogger(HeaderFrameNumber.class.getName());

    /** Adds header frame number to an existing index+sector frame number. */
    private HeaderFrameNumber(@Nonnull IndexSectorFrameNumber indexSectorFrameNumber,
                              @Nonnull FrameNumberNumber headerNumber)
    {
        super(indexSectorFrameNumber._iFrameIndex,
              indexSectorFrameNumber._iFrameCount,
              indexSectorFrameNumber._sectorNumber,
              headerNumber);
    }

    /** Serialize/deserialize Index+Sector+Header frame number format.
     * Generates formatters. */
    public static class Format {

        public static class Builder {
            @Nonnull
            private final FrameNumberNumber.Format.Builder _headerNumberBuilder;

            public Builder(int iFirstFrameHeaderNumber) {
                _headerNumberBuilder = new FrameNumberNumber.Format.Builder(iFirstFrameHeaderNumber);
            }

            public void addHeaderFrameNumber(int iHeaderFrameNumber) {
                _headerNumberBuilder.addNumber(iHeaderFrameNumber);
            }

            public int getStartFrameNumber() {
                return _headerNumberBuilder.getStartFrameValue();
            }

            public int getLastFrameNumber() {
                return _headerNumberBuilder.getLastFrameValue();
            }

            public @Nonnull Format makeFormat() {
                return new Format(_headerNumberBuilder.makeFormat());
            }
        }


        public static final String HEADER_FORMAT_KEY = "Header Frames";
        @Nonnull
        private final FrameNumberNumber.Format _headerNumberFormat;

        private Format(@Nonnull FrameNumberNumber.Format headerNumberFormat) {
            _headerNumberFormat = headerNumberFormat;
        }

        public Format(@Nonnull SerializedDiscItem serial) throws LocalizedDeserializationFail {
            _headerNumberFormat = new FrameNumberNumber.Format(serial.getString(HEADER_FORMAT_KEY));
        }

        public void serialize(@Nonnull SerializedDiscItem serial) {
            serial.addString(HEADER_FORMAT_KEY, _headerNumberFormat.serialize());
        }

        public @Nonnull HeaderFrameNumber getStartFrame(@Nonnull IndexSectorFrameNumber.Format indexSectorFormat)
        {
            return new HeaderFrameNumber(indexSectorFormat.getStartFrame(),
                                         _headerNumberFormat.getStart());
        }

        public @Nonnull HeaderFrameNumber getEndFrame(@Nonnull IndexSectorFrameNumber.Format indexSectorFormat)
        {
            return new HeaderFrameNumber(indexSectorFormat.getEndFrame(),
                                         _headerNumberFormat.getEnd());
        }

        public @Nonnull IFrameNumberFormatterWithHeader makeFormatter(@Nonnull IndexSectorFrameNumber.Format indexSectorFormat) {
            return new Formatter(indexSectorFormat.makePrivateTypeFormatter(), _headerNumberFormat.makeFormatter());
        }

        @Override
        public String toString() {
            return _headerNumberFormat.toString();
        }
    }

    /** Generates frame numbers with index, sector, and header. */
    private static class Formatter implements IFrameNumberFormatterWithHeader {

        @Nonnull
        private final IndexSectorFrameNumber.Formatter _indexSectorFrameNumberFormatter;
        @Nonnull
        private final FrameNumberNumber.Formatter _headerFormatter;

        private Formatter(@Nonnull IndexSectorFrameNumber.Formatter indexSectorFrameNumberFormatter,
                          @Nonnull FrameNumberNumber.Formatter headerFormatter)
        {
            _indexSectorFrameNumberFormatter = indexSectorFrameNumberFormatter;
            _headerFormatter = headerFormatter;
        }

        private static void warnHeaderFrameNumberIssues(@Nonnull FrameNumberNumber headerNumber,
                                                        @Nonnull ILocalizedLogger log)
        {
            if (headerNumber.frameNumberOutOfMaxBounds() || headerNumber.frameNumberOutOfMinBounds()) {
                log.log(Level.WARNING, I.FRAMES_UNEXPECTED_NUMBER());
                LOG.log(Level.WARNING,
                        "Expected frame numbers between {0} and {1} but found one with frame number {2}",
                        new Object[]{headerNumber.getExpectedFrameMinValue(),
                                     headerNumber.getExpectedFrameMaxValue(),
                                     headerNumber.getFrameValue()});
            }
            if (headerNumber.tooManyDuplicates()) {
                log.log(Level.WARNING, I.FRAMES_UNEXPECTED_NUMBER());
                LOG.log(Level.WARNING,
                        "Expected only {0} frames with the header frame number {1}, but found {2}",
                        new Object[]{headerNumber.getExpectedDuplicateMax(),
                                     headerNumber.getFrameValue(),
                                     headerNumber.getDuplicateIndex()});
            }
        }

        @Override
        public @Nonnull HeaderFrameNumber next(int iSector, int iHeaderFrame, @Nonnull ILocalizedLogger log) {
            IndexSectorFrameNumber f1 = _indexSectorFrameNumberFormatter.next(iSector, log);
            FrameNumberNumber hnn = _headerFormatter.next(iHeaderFrame);
            warnHeaderFrameNumberIssues(hnn, log);
            return new HeaderFrameNumber(f1, hnn);
        }

    }

    public static @Nonnull IFrameNumberFormatterWithHeader makeSimpleFormatter(
            int iFrameCount,
            int iSectorMinValue, int iSectorMaxValue, int iSectorDuplicateMax,
            int iHeaderMinValue, int iHeaderMaxValue, int iHeaderDuplicateMax)
    {
        // set the end duplicate = max duplicate for simplicity
        FrameNumberNumber.Format snf = new FrameNumberNumber.Format(
                iSectorMinValue, iSectorMaxValue, iSectorDuplicateMax, iSectorDuplicateMax);
        FrameNumberNumber.Format hnf = new FrameNumberNumber.Format(
                iHeaderMinValue, iHeaderMaxValue, iHeaderDuplicateMax, iHeaderDuplicateMax);
        FrameNumberNumber.Formatter sf = new FrameNumberNumber.Formatter(snf);
        FrameNumberNumber.Formatter hf = new FrameNumberNumber.Formatter(hnf);

        IndexSectorFrameNumber.Formatter sect = new IndexSectorFrameNumber.Formatter(iFrameCount, sf);
        Formatter head = new Formatter(sect, hf);
        return head;
    }

}
