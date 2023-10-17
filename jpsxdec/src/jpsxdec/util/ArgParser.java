/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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

package jpsxdec.util;

import argparser.BooleanHolder;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** Wraps all the common use of {@link argparser.ArgParser} in jPSXdec
 * into a single class. */
public class ArgParser {

    private static final String[] HELPS = {"-?", "-h", "-help"};

    @CheckForNull
    private String[] _asArgs;
    @CheckForNull
    private argparser.ArgParser _currentParser;

    public ArgParser(@CheckForNull String[] asArgs) {
        _asArgs = asArgs;
    }

    /** Checks if a help flag exists in the arguments without removing the flags. */
    public boolean hasHelp() {
        if (_asArgs == null)
            return false;
        argparser.ArgParser ap = new argparser.ArgParser("", false);
        argparser.BooleanHolder help = new argparser.BooleanHolder();
        ap.addOption(Misc.join(HELPS, ",")+" %v", help);
        ap.matchAllArgs(_asArgs, 0, 0);
        return help.value;
    }

    /** Creates a {@link argparser.StringHolder} that will match the given
     * flags. Allows multiple equivalent flags (e.g. -i and -item).
     * All matching arguments will be removed, even if there are duplicate
     * arguments. */
    public @Nonnull argparser.StringHolder addStringOption(@Nonnull String ... asFlags) {
        currentArgParse();
        argparser.StringHolder sh = new argparser.StringHolder();
        _currentParser.addOption(Misc.join(asFlags, ",")+" %s", sh);
        return sh;
    }

    /** Creates a {@link argparser.BooleanHolder} that will match the given
     * flags. Allows multiple equivalent flags (e.g. -i and -item).
     * All matching arguments will be removed, even if there are duplicate
     * arguments. */
    public @Nonnull argparser.BooleanHolder addBoolOption(@Nonnull String ... asFlags) {
        currentArgParse();
        argparser.BooleanHolder bh = new argparser.BooleanHolder();
        _currentParser.addOption(Misc.join(asFlags, ",")+" %v", bh);
        return bh;
    }

    /** Creates a {@link argparser.BooleanHolder} with a default value.
     * @see #addBoolOption(java.lang.String...) */
    public @Nonnull argparser.BooleanHolder addBoolOption(boolean blnDefault, @Nonnull String ... asFlags) {
        currentArgParse();
        argparser.BooleanHolder bh = new argparser.BooleanHolder(blnDefault);
        _currentParser.addOption(Misc.join(asFlags, ",")+" %v", bh);
        return bh;
    }

    /** Starts a new argument {@link argparser.ArgParser} session if there isn't one. */
    private void currentArgParse() {
        if (_currentParser == null)
            _currentParser = new argparser.ArgParser("", false);
    }

    /** Automatically creates a {@link argparser.BooleanHolder} for the
     * default help flags. */
    public @Nonnull BooleanHolder addHelp() {
        return addBoolOption(HELPS);
    }

    /** Performs the actual matching.
     * All created {@link argparser.StringHolder} and {@link argparser.BooleanHolder}
     * since creation or the last call to {@link #match()} will have their values populated.
     * The matched flags will be removed from the original argument list. */
    public void match() {
        if (_currentParser == null)
            throw new IllegalStateException();
        if (_asArgs != null)
            _asArgs = _currentParser.matchAllArgs(_asArgs, 0, 0);
        _currentParser = null;
    }

    /** Returns if any arguments remain that have not been matched. */
    public boolean hasRemaining() {
        return _asArgs != null && _asArgs.length > 0;
    }

    /** Make a copy of this {@link ArgParser} so parameters may be parsed
     * multiple times. */
    public @Nonnull ArgParser copy() {
        return new ArgParser(_asArgs);
    }

}
