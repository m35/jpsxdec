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

package jpsxdec.modules.xa;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.modules.IdentifiedSector;

/** The "Green Book" standard describes what it calls the "Empty Sector".
 * This is commonly used as filling for an XA audio channel when it
 * ends before other parallel channels (or video).
 * From the Green Book:
 * <blockquote>
 *   An empty sector may be Form 1 or Form 2 and does not contain any CD-I data. They
 *   may be used in the lead-in and/or lead-out areas. In the program area empty sectors
 *   can be used to fill up file space particularly for real-time files.
 *   The subheader of an Empty sector has the following restrictions:
 *   (1) Channel number must be zero.
 *   (2) The Video, Audio and Data bits in the submode byte must be zero.
 *   (3) The Coding Information byte must be zero.
 *   It is recommended that empty sectors are Form 2 and that the data bytes are zero.
 * </blockquote>
 */
public class SectorXaEmpty extends IdentifiedSector {

    public SectorXaEmpty(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        CdSectorXaSubHeader sh = cdSector.getSubHeader();
        // if it doesn't have a raw sector header, empty sectors can't be detected
        if (sh == null) return;

        SubMode sm = sh.getSubMode();
        // if it's not a Form 2 sector, then technically it still could be an empty sector
        // but in practice there should ONLY be Form 2 sectors on PlayStation discs
        if (sm.getForm() != 2) return;

        if (sm.getAudio() || sm.getVideo() || sm.getData()) // must not be set
                return;
        if (sh.getChannel() != 0) // must be 0
                return;

        setProbability(100);
    }

    @Override
    public String toString() {
        return getTypeName() + " " + super.toString();
    }

    @Override
    public @Nonnull String getTypeName() {
        return "XA Empty";
    }

}
