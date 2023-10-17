/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2023  Michael Sabin
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

package jpsxdec.psxvideo.mdec;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/** Filters out MDEC codes where AC=0.
 * Known to happen in FF7 and Judge Dredd videos, but could happen anywhere.
 * MDEC codes where AC=0 are redundant and just waste space.
 * This will merge those codes with adjacent codes by extending zero-run-length.
 */
public class Ac0Checker implements MdecInputStream {

    private static final Logger LOG = Logger.getLogger(Ac0Checker.class.getName());

    public static Ac0Checker wrapWithChecker(@Nonnull MdecInputStream source, boolean blnAlsoClean) {
        if (source instanceof Ac0Checker)
            return (Ac0Checker)source;
        else
            return new Ac0Checker(source, blnAlsoClean);
    }

    private final MdecInputStream _source;
    private final boolean _blnAlsoClean;

    private boolean _blnNextCodeIsQscaleDC = true;
    private int _iEscapeCodeAc0Count = 0;

    public Ac0Checker(MdecInputStream source, boolean blnAlsoClean) {
        _source = source;
        _blnAlsoClean = blnAlsoClean;
    }

    final public int get0AcCoefficientCount() {
        return _iEscapeCodeAc0Count;
    }

    final public void logIfAny0AcCoefficient() {
        if (_iEscapeCodeAc0Count > 0) {
            LOG.log(Level.INFO, "Frame had {0} codes 0 AC coefficient", _iEscapeCodeAc0Count);
        }
    }

    @Override
    public boolean readMdecCode(MdecCode code)
            throws MdecException.EndOfStream, MdecException.ReadCorruption
    {
        boolean blnEod = _source.readMdecCode(code);

        if (_blnNextCodeIsQscaleDC) {
            _blnNextCodeIsQscaleDC = false;
            if (blnEod) // there's something very strange going on if this happens
                LOG.log(Level.WARNING, "(qscale,DC) code says it's EOD");
        } else if (blnEod) {
            _blnNextCodeIsQscaleDC = true;
        } else if (code.getBottom10Bits() == 0) {
            _iEscapeCodeAc0Count++;
            LOG.log(Level.FINE, "Found AC=0 code {0}", code);
            if (_blnAlsoClean)
                blnEod = filter0(code);
        }
        return blnEod;
    }

    private boolean filter0(MdecCode code)
            throws MdecException.EndOfStream, MdecException.ReadCorruption
    {
        boolean blnEod;
        final MdecCode nextCode = new MdecCode();
        do {
            blnEod = _source.readMdecCode(nextCode);
            if (blnEod) {
                LOG.log(Level.FINE, "Discarding merge {0} at EOD", code);
                code.setFrom(nextCode);
                _blnNextCodeIsQscaleDC = true;
                break;
            }
            code.setTop6Bits(code.getTop6Bits() + nextCode.getTop6Bits() + 1);
            code.setBottom10Bits(nextCode.getBottom10Bits());
            LOG.log(Level.FINE, "Merged code {0} into prev AC=0 code to form {1}",
                    new Object[] {nextCode, code});
        } while (code.getBottom10Bits() == 0);

        return blnEod;
    }

}
