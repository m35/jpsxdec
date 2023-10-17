/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

package jpsxdec.cdreaders;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.ILocalizedException;

/** CD image file reading exceptions grouped into one file. */
public abstract class CdException extends IOException implements ILocalizedException {

    @Nonnull
    private final ILocalizedMessage _message;

    public CdException(@Nonnull ILocalizedMessage message) {
        super(message.getEnglishMessage());
        _message = message;
    }

    public CdException(@Nonnull ILocalizedMessage message, @Nonnull Throwable cause) {
        super(message.getEnglishMessage(), cause);
        _message = message;
    }

    @Override
    final public @Nonnull ILocalizedMessage getSourceMessage() {
        return _message;
    }

    @Override
    final public @Nonnull String getMessage() {
        return _message.getEnglishMessage();
    }

    @Override
    final public @Nonnull String getLocalizedMessage() {
        return _message.getLocalizedMessage();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getMessage();
    }

    //====================================================================================


    /** Exception if a CD file is not found or cannot be opened. */
    public static class FileNotFound extends CdException {
        @Nonnull
        private final File _file;

        public FileNotFound(@Nonnull File file, @Nonnull FileNotFoundException ex) {
            super(I.IO_OPENING_FILE_NOT_FOUND_NAME(file.toString()), ex);
            _file = file;
        }

        public @Nonnull File getFile() {
            return _file;
        }
    }

    /** Exception if the source CD file is too small to be identified (like {@code < 2048 bytes}) */
    public static class FileTooSmallToIdentify extends CdException {
        @Nonnull
        private final File _file;
        private final long _lngFileSize;
        private final int _iMinimumSize;

        public FileTooSmallToIdentify(@Nonnull File file, long lngFileSize, int iMinimumSize) {
            super(I.CD_FILE_TOO_SMALL(file.toString(), lngFileSize, iMinimumSize));
            _file = file;
            _lngFileSize = lngFileSize;
            _iMinimumSize = iMinimumSize;
        }

        public @Nonnull File getFile() {
            return _file;
        }

        public long getFileSize() {
            return _lngFileSize;
        }

        public int getMinimumSize() {
            return _iMinimumSize;
        }
    }

    /** Discs can't be larger than {@link Integer#MAX_VALUE} (which is 2gb) in order to
     * support loading a full disc in RAM. */
    public static class FileTooLarge extends CdException {
        @Nonnull
        private final File _file;
        private final long _lngFileSize;
        private final int _iMaxSize;

        public FileTooLarge(@Nonnull File file, long lngFileSize, int iMaxSize) {
            super(I.CD_FILE_TOO_LARGE(file.toString(), lngFileSize, iMaxSize));
            _file = file;
            _lngFileSize = lngFileSize;
            _iMaxSize = iMaxSize;
        }

        public @Nonnull File getFile() {
            return _file;
        }

        public long getFileSize() {
            return _lngFileSize;
        }

        public int getMaxSize() {
            return _iMaxSize;
        }
    }

    /** Exception if there is an error reading from the CD. */
    public static class Read extends CdException {
        @Nonnull
        private final File _file;
        @Nonnull
        private final IOException _ioCause;

        public Read(@Nonnull File file, @Nonnull IOException ex) {
            super(I.IO_READING_FROM_FILE_ERROR_NAME(file.toString()), ex);
            _file = file;
            _ioCause = ex;
        }

        public @Nonnull File getFile() {
            return _file;
        }

        public @Nonnull IOException getIoCause() {
            return _ioCause;
        }
    }

}
