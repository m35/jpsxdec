/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins;

import jpsxdec.util.FeedbackStream;

public class ConsoleProgressListener extends ProgressListener {

    private static final int BAR_WIDTH = 30;

    private String _sLastEvent = "";
    private boolean _blnNewLine = false;
    private double _dblNextProgressMark = 0;
    private FeedbackStream _fbs;

    public ConsoleProgressListener(FeedbackStream fbs) {
        _fbs = fbs;
    }

    @Override
    public void error(String sDescription) {
        if (_blnNewLine) {
            _fbs.nl();
            _blnNewLine = false;
        }
        _fbs.printlnErr(sDescription);
    }

    @Override
    public void event(String sDescription) {
        _sLastEvent = sDescription;
    }

    @Override
    public void info(String s) {
        _fbs.printlnNorm(s);
    }


    @Override
    public void progressEnd() {
        _fbs.printlnNorm(buildProgress(1));
        _dblNextProgressMark = 0;
    }

    @Override
    public void progressStart(String s) {
        if (s != null)
            _fbs.printlnNorm(s);
        _dblNextProgressMark = 0;
    }

    @Override
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
        _fbs.printlnNorm(sLine);
        _blnNewLine = true;
        
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

        strBuild.append(String.format("] %4d%% %s", (long)Math.floor(dblPercentComplete * 100), _sLastEvent));

        return strBuild.toString();
    }

    @Override
    public void warning(String sDescription) {
        if (_blnNewLine) {
            _fbs.nl();
            _blnNewLine = false;
        }
        _fbs.printlnWarn(sDescription);
    }

}
