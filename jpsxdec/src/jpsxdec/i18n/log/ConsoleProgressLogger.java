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

package jpsxdec.i18n.log;

import java.io.PrintStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;

public class ConsoleProgressLogger extends ProgressLogger implements UserFriendlyLogger.OnWarnErr {

    private static final int BAR_WIDTH = 20;

    @CheckForNull
    private ILocalizedMessage _lastEvent = null;
    private double _dblNextProgressMark = 0;
    private int _iWarnCount = 0;
    private int _iErrCount = 0;
    /** Different from the logging stream.  */
    @Nonnull
    private final PrintStream _progressStream;

    public ConsoleProgressLogger(@Nonnull String sBaseName, @Nonnull PrintStream progressStream) {
        super(sBaseName);
        _progressStream = progressStream;
        setListener(this);
    }

    @Override
    public void onWarn(@Nonnull ILocalizedMessage msg) {
        _iWarnCount++;
    }
    @Override
    public void onErr(@Nonnull ILocalizedMessage msg) {
        _iErrCount++;
    }

    @Override
    public void event(@Nonnull ILocalizedMessage msg) {
        _lastEvent = msg;
    }

    @Override
    protected void handleProgressEnd() {
        _progressStream.println(buildProgress(1));
        _dblNextProgressMark = 0;
    }

    @Override
    protected void handleProgressStart() {
        _dblNextProgressMark = 0;
        _iWarnCount = 0;
        _iErrCount = 0;
        _lastEvent = null;
    }

    @Override
    protected void handleProgressUpdate(double dblPercentComplete) {

        if (dblPercentComplete < _dblNextProgressMark) {
            return;
        }

        if (dblPercentComplete > 0.91) {
            return;
        }

        ILocalizedMessage line = buildProgress(dblPercentComplete);

        // a carriage return after the string \r
        // resets the cursor position back to the beginning of the line
        // but for now just do normal new line
        _progressStream.println(line);

        _dblNextProgressMark = Math.round((dblPercentComplete + 0.05) * 10.0) / 10.0;
    }

    private @Nonnull ILocalizedMessage buildProgress(double dblPercentComplete) {

        StringBuilder progressBar = new StringBuilder();
        for (double dblProgress = 0; dblProgress < 1.0; dblProgress += 1.0/BAR_WIDTH) {
            if (dblProgress < dblPercentComplete)
                progressBar.append('=');
            else
                progressBar.append('.');
        }

        // stupid DecimalFormat has no way to pad numbers with spaces like String.format() does
        if (_lastEvent== null)
            return I.CMD_PROGRESS(progressBar.toString(), dblPercentComplete, _iWarnCount, _iErrCount);
        else
            return I.CMD_PROGRESS_WITH_MSG(progressBar.toString(), dblPercentComplete, _lastEvent, _iWarnCount, _iErrCount);
    }

    @Override
    public boolean isSeekingEvent() { return true; }

}
