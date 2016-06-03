/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2016  Michael Sabin
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

package jpsxdec.cmdline;

import argparser.ArgParser;
import argparser.StringHolder;
import java.io.File;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.UserFriendlyLogger;


public abstract class Command {
    @Nonnull
    private final String _sArg;

    /** @param sArg  Option name. */
    public Command(@Nonnull String sArg) {
        _sArg = sArg;
    }

    abstract public void execute(@CheckForNull String[] asRemainingArgs) throws CommandLineException;

    private final StringHolder _receiver = new StringHolder();
    @Nonnull
    private StringHolder inputFileArg, indexFileArg;
    @Nonnull
    protected FeedbackStream _fbs;

    final public Command init(@Nonnull ArgParser ap,
                              @Nonnull StringHolder inputFileArg,
                              @Nonnull StringHolder indexFileArg,
                              @Nonnull FeedbackStream fbs)
    {
        ap.addOption(_sArg + " %s", _receiver);
        this.inputFileArg = inputFileArg;
        this.indexFileArg = indexFileArg;
        _fbs = fbs;
        return this;
    }

    final public boolean found() {
        return _receiver.value != null;
    }

    /** If issue, returns an error message and the caller should fail,
     * otherwise null if there is no issue. */
    final public @CheckForNull ILocalizedMessage validate() {
        return validate(_receiver.value);
    }

    /** Checks that the option value is valid.
        *  Returns {@code null} if OK, or error message if invalid. */
    abstract protected @CheckForNull ILocalizedMessage validate(@Nonnull String sOptionValue);

    protected @Nonnull CdFileSectorReader getCdReader() throws CommandLineException {
        if (inputFileArg.value != null) {
            return CommandLine.loadDisc(inputFileArg.value, _fbs);
        } else if (indexFileArg.value != null) {
            _fbs.println(I.CMD_READING_INDEX_FILE(indexFileArg.value));
            DiscIndex index;
            try {
                index = new DiscIndex(indexFileArg.value, new UserFriendlyLogger(I.INDEX_LOG_FILE_BASE_NAME().getLocalizedMessage()));
            } catch (IOException ex) {
                throw new CommandLineException(I.ERR_LOADING_INDEX_FILE(), ex);
            } catch (NotThisTypeException ex) {
                throw new CommandLineException(I.ERR_LOADING_INDEX_FILE(), ex);
            }
            _fbs.println(I.CMD_ITEMS_LOADED(index.size()));
            return index.getSourceCd();
        }
        throw new CommandLineException(I.CMD_DISC_FILE_REQUIRED());
    }

    // TODO: cleanup
    protected @Nonnull DiscIndex getIndex() throws CommandLineException {
        final DiscIndex index;
        if (indexFileArg.value != null) {
            if (inputFileArg.value != null) {
                CdFileSectorReader cd = CommandLine.loadDisc(inputFileArg.value, _fbs);
                File idxFile = new File(indexFileArg.value);
                if (idxFile.exists()) {
                    _fbs.println(I.CMD_READING_INDEX_FILE(indexFileArg.value));
                    try {
                        index = new DiscIndex(indexFileArg.value, cd, new UserFriendlyLogger(I.INDEX_LOG_FILE_BASE_NAME().getLocalizedMessage()));
                    } catch (IOException ex) {
                        throw new CommandLineException(I.ERR_LOADING_INDEX_FILE(), ex);
                    } catch (NotThisTypeException ex) {
                        throw new CommandLineException(I.ERR_LOADING_INDEX_FILE(), ex);
                    }
                    _fbs.println(I.CMD_USING_SRC_FILE(index.getSourceCd().getSourceFile()));
                    _fbs.println(I.CMD_ITEMS_LOADED(index.size()));
                } else {
                    index = CommandLine.buildIndex(cd, _fbs);
                    CommandLine.saveIndex(index, indexFileArg.value, _fbs);
                }
            } else {
                _fbs.println(I.CMD_READING_INDEX_FILE(indexFileArg.value));
                try {
                    index = new DiscIndex(indexFileArg.value, new UserFriendlyLogger(I.INDEX_LOG_FILE_BASE_NAME().getLocalizedMessage()));
                } catch (IOException ex) {
                    throw new CommandLineException(I.ERR_LOADING_INDEX_FILE(), ex);
                } catch (NotThisTypeException ex) {
                    throw new CommandLineException(I.ERR_LOADING_INDEX_FILE(), ex);
                }
                _fbs.println(I.CMD_USING_SRC_FILE(index.getSourceCd().getSourceFile()));
                _fbs.println(I.CMD_ITEMS_LOADED(index.size()));
            }
        } else {
            if (inputFileArg.value != null) {
                CdFileSectorReader cd = CommandLine.loadDisc(inputFileArg.value, _fbs);
                index = CommandLine.buildIndex(cd, _fbs);
            } else {
                throw new CommandLineException(I.CMD_NEED_INPUT_OR_INDEX());
            }
        }
        return index;
    }

    protected @Nonnull File getInFile() throws CommandLineException {
        if (inputFileArg.value == null)
            throw new CommandLineException(I.CMD_INPUT_FILE_REQUIRED());
        File file = new File(inputFileArg.value);
        if (!file.exists())
            throw new CommandLineException(I.CMD_INPUT_FILE_NOT_FOUND(file));
        return file;
    }

}


