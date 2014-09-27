/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

import java.io.PrintStream;
import java.util.logging.LogRecord;
import jpsxdec.LocalizedMessage;

public class ConsoleProgressListenerLogger extends ProgressListenerLogger {

    private static final int BAR_WIDTH = 30;

    private LocalizedMessage _lastEvent = null;
    private double _dblNextProgressMark = 0;
    private int _iWarnCount = 0;
    private int _iErrCount = 0;
    /** Different from the logging stream.  */
    private final PrintStream _progressStream;

    public ConsoleProgressListenerLogger(String sBaseName, PrintStream progressStream) {
        super(sBaseName);
        _progressStream = progressStream;
        setListener(new OnWarnErr() {
            public void onErr(LogRecord record) {
                _iErrCount++;
            }
            public void onWarn(LogRecord record) {
                _iWarnCount++;
            }
        });
    }

    public void event(LocalizedMessage msg) {
        _lastEvent = msg;
    }

    public void progressInfo(LocalizedMessage msg) {
        _progressStream.println(msg.getLocalizedMessage());
    }

    public void progressEnd() {
        _progressStream.println(buildProgress(1));
        _dblNextProgressMark = 0;
    }

    public void progressStart() { progressStart(null); }
    public void progressStart(LocalizedMessage msg) {
        if (msg != null)
            _progressStream.println(msg.getLocalizedMessage());
        _dblNextProgressMark = 0;
        _iWarnCount = 0;
        _iErrCount = 0;
        _lastEvent = null;
    }

    public void progressUpdate(double dblPercentComplete) {

        if (dblPercentComplete < _dblNextProgressMark) {
            return;
        }

        if (dblPercentComplete > 0.91) {
            return;
        }

        String sLine = buildProgress(dblPercentComplete);

        // a carriage return after the string \r
        // resets the cursor position back to the beginning of the line
        // but for now just do normal new line
        _progressStream.println(sLine);
        
        _dblNextProgressMark = Math.round((dblPercentComplete + 0.05) * 10.0) / 10.0;
    }

    private String buildProgress(double dblPercentComplete) {
        StringBuilder strBuild = new StringBuilder("[");

        for (double dblProgress = 0; dblProgress < 1.0; dblProgress += 1.0/BAR_WIDTH) {
            if (dblProgress < dblPercentComplete)
                strBuild.append('=');
            else
                strBuild.append('.');
        }

        long lngPercent = (long)Math.floor(dblPercentComplete * 100);
        if (_lastEvent== null)
            strBuild.append(String.format("] %4d%%", lngPercent));
        else
            strBuild.append(String.format("] %4d%% %s", lngPercent, _lastEvent.getLocalizedMessage()));

        if (_iWarnCount > 0)
            strBuild.append(" ").append(_iWarnCount).append(" warnings"); // I18N
        if (_iErrCount > 0)
            strBuild.append(" ").append(_iErrCount).append(" errors"); // I18N

        return strBuild.toString();
    }

    public boolean seekingEvent() { return true; }

}
