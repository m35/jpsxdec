/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.ArgParser;

/** An abstract command line option. Extend to create a command line option. */
public abstract class Command {
    @Nonnull
    private final String[] _asFlags;

    /** @param asFlags  Option name. */
    public Command(@Nonnull String ... asFlags) {
        _asFlags = asFlags;
    }

    abstract public void execute(@Nonnull ArgParser ap) throws CommandLineException;

    @Nonnull
    private StringHolder _receiver;
    @Nonnull
    private InFileAndIndexArgs _discIndex;
    @Nonnull
    protected FeedbackStream _fbs;

    final public Command init(@Nonnull ArgParser ap,
                              @Nonnull InFileAndIndexArgs discIndex,
                              @Nonnull FeedbackStream fbs)
    {
        _receiver = ap.addStringOption(_asFlags);
        _discIndex = discIndex;
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

    protected @Nonnull ICdSectorReader getCdReader() throws CommandLineException {
        return _discIndex.getCdReader();
    }

    protected @Nonnull DiscIndex getIndex() throws CommandLineException {
        return _discIndex.getIndex();
    }

    protected @Nonnull File getInFile() throws CommandLineException {
        return _discIndex.getInFile();
    }

}


