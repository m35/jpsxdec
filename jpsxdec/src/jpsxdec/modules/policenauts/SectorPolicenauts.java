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

package jpsxdec.modules.policenauts;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.IdentifiedSector;

/** Just a simple sector wrapper for Policenauts to claim sectors.
 * Holds the packets that end in this sector, and a flag if it's the last sector
 * of the video. */
public class SectorPolicenauts extends IdentifiedSector implements Iterable<SPacketData> {

    private boolean _blnIsEnd;

    @Nonnull
    private List<SPacketData> _packetsEndingInThisSector = Collections.emptyList();

    public SectorPolicenauts(@Nonnull CdSector cdSector, boolean blnIsEnd) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;
        _blnIsEnd = blnIsEnd;
        setProbability(100);
    }

    @Override
    public String getTypeName() {
        return "Policenauts";
    }

    void setPacketsEndingInThisSector(@Nonnull List<SPacketData> packetsEndingInThisSector) {
        assert endsInThisSector(packetsEndingInThisSector);
        _packetsEndingInThisSector = packetsEndingInThisSector;
    }

    private boolean endsInThisSector(@Nonnull List<SPacketData> packetsEndingInThisSector) {
        for (SPacketData sPacketData : packetsEndingInThisSector) {
            if (sPacketData.getEndSectorInclusive() != getSectorNumber())
                return false;
        }
        return true;
    }

    @Override
    public @Nonnull Iterator<SPacketData> iterator() {
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
        if (_blnIsEnd)
            sb.append(" END");
        return sb.toString();
    }
}
