/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2016  Michael Sabin
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


package jpsxdec.discitems;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;

/** Formats a {@link FrameNumber} for consistent display.
 * Adds padding zeros as needed to consistently keep the same character length.
 * Basically only used when generating file names. */
public abstract class FrameNumberFormatter {
    private static final Logger LOG = Logger.getLogger(FrameNumberFormatter.class.getName());

    abstract public @Nonnull String formatNumber(@Nonnull FrameNumber frameNumber, @CheckForNull Logger log);
    abstract public @Nonnull String getNumber(@Nonnull FrameNumber frameNumber);
    abstract public @Nonnull ILocalizedMessage getDescription(@Nonnull FrameNumber frameNumber);

    static class Index extends FrameNumberFormatter {
        @Nonnull
        private final String _sFormat;

        Index(int iIndexDigitCount) {
            _sFormat = "%0" + iIndexDigitCount + 'd';
        }

        @Override
        public @Nonnull String getNumber(@Nonnull FrameNumber frameNumber) {
            return frameNumber.getIndexString();
        }

        @Override
        public @Nonnull String formatNumber(@Nonnull FrameNumber frameNumber, @CheckForNull Logger log) {
            return String.format(_sFormat, frameNumber.getIndex());
        }

        @Override
        public @Nonnull ILocalizedMessage getDescription(@Nonnull FrameNumber frameNumber) {
            return I.FRM_NUM_FMTR_FRAME(getNumber(frameNumber));
        }
    }

    static class Sector extends FrameNumberFormatter {
        @Nonnull
        private final String _sFormat;
        private final int _iSectorDigitCount, _iSectorDuplicateDigitCount;

        public Sector(int iSectorDigitCount, int iSectorDuplicateDigitCount) {
            _iSectorDigitCount = iSectorDigitCount;
            _iSectorDuplicateDigitCount = iSectorDuplicateDigitCount;

            if (_iSectorDuplicateDigitCount > 0) {
                _sFormat = "%0" + _iSectorDigitCount + "d.%0" + _iSectorDuplicateDigitCount + 'd';
            } else {
                _sFormat = "%0" + _iSectorDigitCount + 'd';
            }
        }

        @Override
        public @Nonnull String getNumber(@Nonnull FrameNumber frameNumber) {
            return frameNumber.getSectorString();
        }

        @Override
        public @Nonnull String formatNumber(@Nonnull FrameNumber frameNumber, @CheckForNull Logger log) {
            if (frameNumber.getSectorDuplicate()> 0) {
                if (_iSectorDuplicateDigitCount > 0) {
                    return String.format(_sFormat, frameNumber.getSector(), frameNumber.getSectorDuplicate());
                } else {
                    if (log == null)
                        log = LOG;
                    I.NOT_EXPECTING_DUP_SECT_NUM(frameNumber).log(log, Level.WARNING);
                    return String.format("%0" + _iSectorDigitCount + "d.%d",
                            frameNumber.getSector(), frameNumber.getSectorDuplicate());
                }
            } else {
                return String.format(_sFormat, frameNumber.getSector());
            }
        }

        @Override
        public @Nonnull ILocalizedMessage getDescription(@Nonnull FrameNumber frameNumber) {
            return I.FRM_NUM_FMTR_SECTOR(getNumber(frameNumber));
        }
    }

    static class Header extends FrameNumberFormatter {
        @Nonnull
        private final String _sFormat;
        private final int _iHeaderFrameDigitCount, _iHeaderDuplicateDigitCount;

        public Header(int iHeaderFrameDigitCount, int iHeaderDuplicateDigitCount) {
            _iHeaderFrameDigitCount = iHeaderFrameDigitCount;
            _iHeaderDuplicateDigitCount = iHeaderDuplicateDigitCount;

            if (_iHeaderDuplicateDigitCount > 0) {
                _sFormat = "%0" + _iHeaderFrameDigitCount + "d.%0" + _iHeaderDuplicateDigitCount + 'd';
            } else {
                _sFormat = "%0" + _iHeaderFrameDigitCount + 'd';
            }
        }

        @Override
        public @Nonnull String getNumber(@Nonnull FrameNumber frameNumber) {
            return frameNumber.getHeaderFrameString();
        }

        @Override
        public @Nonnull String formatNumber(@Nonnull FrameNumber frameNumber, @CheckForNull Logger log) {
            if (frameNumber.getHeaderFrameDuplicate() > 0) {
                if (_iHeaderDuplicateDigitCount > 0) {
                    return String.format(_sFormat, frameNumber.getHeaderFrameNumber(), frameNumber.getHeaderFrameDuplicate());
                } else {
                    if (log == null)
                        log = LOG;
                    I.NOT_EXPECTING_DUP_FRM_NUM(frameNumber).log(log, Level.WARNING);
                    return String.format("%0" + _iHeaderFrameDigitCount + "d.%d",
                            frameNumber.getHeaderFrameNumber(), frameNumber.getHeaderFrameDuplicate());
                }
            } else {
                return String.format(_sFormat, frameNumber.getHeaderFrameNumber());
            }
        }

        @Override
        public @Nonnull ILocalizedMessage getDescription(@Nonnull FrameNumber frameNumber) {
            return I.FRM_NUM_FMTR_FRAME(getNumber(frameNumber));
        }
    }

}
