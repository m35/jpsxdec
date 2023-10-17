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

package jpsxdec.indexing;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.UnidentifiedSector;

/** Feed identified (or unidentified) sectors or their type names to this class
 * and it will count how many it sees of each type. */
public class SectorTypeCounter implements Iterable<Map.Entry<String, Integer>> {

    public static boolean LOG_SECTOR_COUNT_IN_SEPARATE_LINES = true;

    private static final Logger LOG = Logger.getLogger(SectorTypeCounter.class.getName());

    private final TreeMap<String, Integer> _sectorCounts =
            new TreeMap<String, Integer>();

    public void increment(@CheckForNull IIdentifiedSector idSect) {
        if (idSect == null)
            incrementName(UnidentifiedSector.TYPE_NAME);
        else
            incrementName(idSect.getTypeName());
    }

    private void incrementName(@Nonnull String sName) {
        Integer oiCount = _sectorCounts.get(sName);
        if (oiCount == null) {
            _sectorCounts.put(sName, 1);
        } else {
            _sectorCounts.put(sName, oiCount + 1);
        }
    }

    public void put(@Nonnull String sName, int iCount) {
        _sectorCounts.put(sName, iCount);
    }

    public void logCount() {

        if (LOG.isLoggable(Level.INFO)) {
            if (LOG_SECTOR_COUNT_IN_SEPARATE_LINES) {
                for (Map.Entry<String, Integer> entry : this) {
                    String sLog = entry.getKey() + " " + entry.getValue();
                    LOG.info(sLog);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Integer> entry : this) {
                    sb.append('[').append(entry.getKey()).append(':').append(entry.getValue()).append("] ");
                }
                LOG.info(sb.toString().trim());
            }
        }
    }

    @Override
    public @Nonnull Iterator<Map.Entry<String, Integer>> iterator() {
        return _sectorCounts.entrySet().iterator();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (_sectorCounts != null ? _sectorCounts.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SectorTypeCounter other = (SectorTypeCounter) obj;
        if (_sectorCounts != other._sectorCounts && !_sectorCounts.equals(other._sectorCounts)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : this) {
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }
}
