/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

import argparser.StringHolder;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.CdOpener;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ConsoleProgressLogger;
import jpsxdec.i18n.log.UserFriendlyLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.ArgParser;
import jpsxdec.util.IO;
import jpsxdec.util.TaskCanceledException;

/** Manages the input file and index file arguments on the command line. */
public class InFileAndIndexArgs {

    private static final Logger LOG = Logger.getLogger(InFileAndIndexArgs.class.getName());

    @Nonnull
    private final StringHolder _inputFileArg;
    @Nonnull
    private final StringHolder _indexFileArg;
    @Nonnull
    private final StringHolder _cdSectorSize;
    @Nonnull
    private final FeedbackStream _fbs;

    public InFileAndIndexArgs(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs) {
        _inputFileArg = ap.addStringOption("-f","-file");
        _indexFileArg = ap.addStringOption("-x","-index");
        _cdSectorSize = ap.addStringOption("-sectorsize");
        _fbs = fbs;
    }

    // for testing
    public InFileAndIndexArgs(@CheckForNull String sInputFile,
                              @CheckForNull String sIndexFile,
                              @Nonnull FeedbackStream fbs)
    {
        _inputFileArg = new StringHolder(sInputFile);
        _indexFileArg = new StringHolder(sIndexFile);
        _cdSectorSize = new StringHolder();
        _fbs = fbs;
    }


    public @Nonnull File getInFile() throws CommandLineException {
        if (_inputFileArg.value == null)
            throw new CommandLineException(I.CMD_INPUT_FILE_REQUIRED());
        File file = new File(_inputFileArg.value);
        if (!file.exists())
            throw new CommandLineException(I.CMD_INPUT_FILE_NOT_FOUND(file));
        return file;
    }

    /** Reads the input file as a disc image, or if missing, loads index
     * and returns the disc image used by it. */
    public @Nonnull ICdSectorReader getCdReader() throws CommandLineException {
        if (_inputFileArg.value != null) {
            // input file provided on command line
            return loadDisc();
        } else if (_indexFileArg.value != null) {
            // index file provided on command line
            // read index, then get the CD image used by the index
            _fbs.println(I.CMD_READING_INDEX_FILE(_indexFileArg.value));
            UserFriendlyLogger log = new UserFriendlyLogger(I.INDEX_LOG_FILE_BASE_NAME().getLocalizedMessage());
            DiscIndex index = loadIndex(_indexFileArg.value, null, log);
            _fbs.println(I.CMD_ITEMS_LOADED(index.size()));
            return index.getSourceCd();
        }
        throw new CommandLineException(I.CMD_DISC_FILE_REQUIRED());
    }

    /** If both an input file and index file are provided, reads the CD, indexes
     * it, then saves the index and returns true. Returns false if no input file
     * and index file are provided. */
    public boolean createAndSaveIndexIfProvided() throws CommandLineException {
        if (_inputFileArg.value == null && _indexFileArg.value == null)
            return false;

        ICdSectorReader cd = loadDisc();
        try {
            DiscIndex index = buildIndex(cd, _fbs);
            saveIndex(index, _indexFileArg.value, _fbs);
        } finally {
            IO.closeSilently(cd, LOG);
        }

        return true;
    }

    private static void saveIndex(@Nonnull DiscIndex index, @Nonnull String sIndexFile,
                                  @Nonnull FeedbackStream Feedback)
            throws CommandLineException
    {
        if (index.size() < 1) {
            Feedback.println(I.CMD_NOT_SAVING_EMPTY_INDEX());
        } else {
            Feedback.println(I.CMD_SAVING_INDEX(sIndexFile));
            try {
                index.serializeIndex(new File(sIndexFile));
            } catch (FileNotFoundException ex) {
                throw new CommandLineException(I.IO_OPENING_FILE_NOT_FOUND_NAME(sIndexFile), ex);
            }
        }
    }

    /** If index file provided on the command line, reads and returns it.
     * If not, reads the input file as a disc image and indexes it.
     * If neither provided, throws exception. If both provided, loads the index
     * but replaces the index's disc image with the disc image in the command
     * line. */
    public @Nonnull DiscIndex getIndex() throws CommandLineException {
        final DiscIndex index;
        if (_indexFileArg.value != null) {
            if (_inputFileArg.value != null) {
                // both disc image and index provided
                // load the disc
                ICdSectorReader cd = loadDisc();
                File idxFile = new File(_indexFileArg.value);
                // then load the index, but replace the index CD with the one
                // on the command line
                if (idxFile.exists()) {
                    _fbs.println(I.CMD_READING_INDEX_FILE(_indexFileArg.value));
                    UserFriendlyLogger log = new UserFriendlyLogger(I.INDEX_LOG_FILE_BASE_NAME().getLocalizedMessage());
                    index = loadIndex(_indexFileArg.value, cd, log);
                    _fbs.println(I.CMD_USING_SRC_FILE(index.getSourceCd().getSourceFile()));
                    _fbs.println(I.CMD_ITEMS_LOADED(index.size()));
                } else {
                    // index file not found, index the CD instead
                    // TODO print a warning or fail
                    index = buildIndex(cd, _fbs);
                    saveIndex(index, _indexFileArg.value, _fbs);
                }
            } else {
                // only index file provided
                // load it
                _fbs.println(I.CMD_READING_INDEX_FILE(_indexFileArg.value));
                UserFriendlyLogger log = new UserFriendlyLogger(I.INDEX_LOG_FILE_BASE_NAME().getLocalizedMessage());
                index = loadIndex(_indexFileArg.value, null, log);
                _fbs.println(I.CMD_USING_SRC_FILE(index.getSourceCd().getSourceFile()));
                _fbs.println(I.CMD_ITEMS_LOADED(index.size()));
            }
        } else {
            // only input file provided
            // open it then index it
            if (_inputFileArg.value != null) {
                ICdSectorReader cd = loadDisc();
                index = buildIndex(cd, _fbs);
            } else {
                throw new CommandLineException(I.CMD_NEED_INPUT_OR_INDEX());
            }
        }
        return index;
    }

    /** Reads the input file as a disc image, or if missing, throws an exception. */
    private @Nonnull ICdSectorReader loadDisc() throws CommandLineException {
        if (_inputFileArg.value == null) {
            // input file not provided on command line
            throw new CommandLineException(I.CMD_COMMAND_NEEDS_DISC());
        }

        Integer oiCdSectorSize = null;
        if (_cdSectorSize.value != null) {
            try {
                oiCdSectorSize = Integer.parseInt(_cdSectorSize.value);
            } catch (NumberFormatException ex) {
                throw new CommandLineException(I.CMD_INVALID_SECTOR_SIZE(_cdSectorSize.value), ex);
            }
        }

        _fbs.println(I.IO_OPENING_FILE(_inputFileArg.value));

        try {
            ICdSectorReader cd;
            if (oiCdSectorSize == null)
                cd = CdOpener.open(_inputFileArg.value);
            else
                cd = CdOpener.openWithSectorSize(_inputFileArg.value, oiCdSectorSize.intValue());
            _fbs.println(I.CMD_DISC_IDENTIFIED(cd.getTypeDescription()));
            return cd;
        } catch (CdException ex) {
            throw new CommandLineException(ex.getSourceMessage(), ex);
        }
    }

    private static @Nonnull DiscIndex buildIndex(@Nonnull ICdSectorReader cd,
                                                 @Nonnull FeedbackStream fbs)
            throws CommandLineException
    {
        fbs.println(I.CMD_BUILDING_INDEX());
        DiscIndex index;
        ConsoleProgressLogger cpl = new ConsoleProgressLogger(
                I.INDEX_LOG_FILE_BASE_NAME().getLocalizedMessage(), fbs.getUnderlyingStream());
        try {
            cpl.log(Level.INFO, I.CMD_GUI_INDEXING(cd.toString()));
            try {
                index = new DiscIndex(cd, cpl);
            } catch (CdException.Read ex) {
                throw new CommandLineException(I.IO_READING_FROM_FILE_ERROR_NAME(ex.getFile().toString()), ex);
            } catch (LoggedFailure ex) {
                throw new CommandLineException(ex.getSourceMessage(), ex);
            }
        } catch (TaskCanceledException ex) {
            throw new RuntimeException("Impossible TaskCanceledException during commandline indexing", ex);
        } finally {
            cpl.close();
        }
        fbs.println(I.CMD_NUM_ITEMS_FOUND(index.size()));
        fbs.println();
        return index;
    }

    private static @Nonnull DiscIndex loadIndex(@Nonnull String sIndexFile,
                                                @CheckForNull ICdSectorReader cd,
                                                @Nonnull UserFriendlyLogger log)
            throws CommandLineException
    {
        DiscIndex index;
        try {
            if (cd == null)
                index = new DiscIndex(sIndexFile, log);
            else
                index = new DiscIndex(sIndexFile, cd, log);
        } catch (DiscIndex.IndexNotFoundException ex) {
            throw new CommandLineException(I.IO_OPENING_FILE_ERROR_NAME(ex.getFile().toString()), ex);
        } catch (DiscIndex.IndexReadException ex) {
            throw new CommandLineException(I.IO_READING_FROM_FILE_ERROR_NAME(ex.getFile().toString()), ex);
        } catch (LocalizedDeserializationFail ex) {
            throw new CommandLineException(I.ERR_LOADING_INDEX_FILE_REASON(ex.getSourceMessage()), ex);
        } catch (CdException ex) {
            throw new CommandLineException(ex.getSourceMessage(), ex);
        } finally {
            log.close();
        }
        return index;
    }
}
