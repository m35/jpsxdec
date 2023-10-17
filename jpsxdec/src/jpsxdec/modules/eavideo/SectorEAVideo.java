/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.modules.eavideo;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.IdentifiedSector;

/** Just a simple sector wrapper for EA videos to claim sectors. */
public class SectorEAVideo extends IdentifiedSector implements Iterable<EAVideoPacketSectors> {

    /** If the sector is at the start, end, or middle (0) of the stream (bit flags). */
    public static final int START = 1, END = 2;

    private final int _iStartEnd;

    @Nonnull
    private List<EAVideoPacketSectors> _packetsEndingInThisSector = Collections.emptyList();

    public SectorEAVideo(@Nonnull CdSector cdSector, int iStartEnd) {
        super(cdSector);
        _iStartEnd = iStartEnd;
        setProbability(100);
    }

    @Override
    public String getTypeName() {
        return "EA video";
    }

    void setPacketsEndingInThisSector(@Nonnull List<EAVideoPacketSectors> packetsEndingInThisSector) {
        _packetsEndingInThisSector = packetsEndingInThisSector;
    }

    @Override
    public @Nonnull Iterator<EAVideoPacketSectors> iterator() {
        return _packetsEndingInThisSector.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTypeName()).append(' ')
          .append(super.toString()).append(' ')
          .append(_packetsEndingInThisSector.size()).append(" finished packets [");
        for (int i = 0; i < _packetsEndingInThisSector.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(_packetsEndingInThisSector.get(i));
        }
        sb.append(']');
        if ((_iStartEnd & START) != 0)
            sb.append(" START");
        if ((_iStartEnd & END) != 0)
            sb.append(" END");
        return sb.toString();
    }
}
