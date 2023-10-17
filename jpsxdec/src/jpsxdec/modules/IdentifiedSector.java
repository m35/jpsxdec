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

package jpsxdec.modules;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader;

/** Abstract base class for all identified sector types. Encapsulates CD sectors
 *  with special meaning. */
public abstract class IdentifiedSector implements IIdentifiedSector {

    /** Convenience function to check that the sector has a sub header,
     *  and the submode bits are as expected. */
    final protected boolean subModeExistsAndMaskEquals(int iMask, int iExpected) {
        CdSectorXaSubHeader sh = _sourceCdSector.getSubHeader();
        if (sh == null)
            return false;
        return sh.getSubMode().mask(iMask) == iExpected;
    }

    /** Convenience function to check that the sector has a sub header,
     *  and the submode bits are NOT as expected. */
    final protected boolean subModeExistsAndMaskDoesNotEqual(int iMask, int iExpected) {
        CdSectorXaSubHeader sh = _sourceCdSector.getSubHeader();
        if (sh == null)
            return false;
        return sh.getSubMode().mask(iMask) != iExpected;
    }


    @Nonnull
    private final CdSector _sourceCdSector;
    private int _iProbability = 1;

    public IdentifiedSector(@Nonnull CdSector cdSector) {
        _sourceCdSector = cdSector;
    }

    /** Returns true if the super-class is definitely not a match (probability=0).
     * If it returns false, then it also resets probability to 0 for the child-class
     * to determine its own probability. */
    final protected boolean isSuperInvalidElseReset() {
        if (_iProbability == 0)
            return true;
        _iProbability = 0;
        return false;
    }
    /** Between 0 and 100. */
    final protected void setProbability(int iProbability) {
        _iProbability = iProbability;
    }

    @Override
    final public int getProbability() {
        return _iProbability;
    }

    /** Returns a string description of the sector type. */
    @Override
    public String toString() {
        return _sourceCdSector.toString();
    }

    /** @return The sector number from the start of the source file. */
    @Override
    public int getSectorNumber() {
        return _sourceCdSector.getSectorIndexFromStart();
    }

    @Override
    final public @Nonnull CdSector getCdSector() {
        return _sourceCdSector;
    }

    /** Returns just the {@link CdSector#toString()}, useful when constructing
     * toString()s in child classes. */
    final protected String cdToString() {
        return _sourceCdSector.toString();
    }
}


