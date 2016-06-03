/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

package jpsxdec.discitems.savers;

import javax.annotation.Nonnull;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


public abstract class FrameLookup {

    public static @Nonnull FrameLookup deserialize(@Nonnull String s) throws NotThisTypeException {
        String[] as = Misc.regex("^(["+FrameNumber.SECTOR_PREFIX+FrameNumber.HEADER_PREFIX+"])?(\\d+)$", s);
        if (as == null)
            throw new NotThisTypeException();
        try {
            if (as[1] == null) {
                return new Index(Integer.parseInt(as[2]));
            } else {
                switch (as[1].charAt(0)) {
                    case FrameNumber.SECTOR_PREFIX:
                        return new Sector(Integer.parseInt(s.substring(1)));
                    case FrameNumber.HEADER_PREFIX:
                        return new Header(Integer.parseInt(s.substring(1)));
                    default:
                        throw new RuntimeException();
                }
            }
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException(ex);
        }
    }

    public static @Nonnull FrameLookup[] parseRange(@Nonnull String sRange) throws NotThisTypeException {
        String[] as = sRange.split("\\-", 2);
        FrameLookup[] ret;
        if (as.length > 2)
            ret = new FrameLookup[2];
        else
            ret = new FrameLookup[as.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = deserialize(as[i]);            
        }
        return ret;
    }

    public static @Nonnull FrameLookup byHeader(int iFrame) {
        return new Header(iFrame);
    }

    //==========================================================================

    private static class Index extends FrameLookup {
        private final int _iIndex;

        public Index(int iIndex) {
            _iIndex = iIndex;
        }

        @Override
        public int compareTo(@Nonnull FrameNumber fn) {
            return Misc.intCompare(_iIndex, fn.getIndex());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + _iIndex;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass())
                return false;
            final Index other = (Index) obj;
            return _iIndex == other._iIndex;
        }

        @Override
        public String toString() {
            return String.valueOf(_iIndex);
        }
    }

    public static class Header extends FrameLookup {
        private final int _iHeaderNumber;

        public Header(int iHeaderNumber) {
            if (iHeaderNumber < 0)
                throw new IllegalArgumentException();
            _iHeaderNumber = iHeaderNumber;
        }

        @Override
        public int compareTo(@Nonnull FrameNumber fn) {
            return Misc.intCompare(_iHeaderNumber, fn.getHeaderFrameNumber());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + _iHeaderNumber;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass())
                return false;
            final Header other = (Header) obj;
            return _iHeaderNumber == other._iHeaderNumber;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append(FrameNumber.HEADER_PREFIX)
                    .append(_iHeaderNumber).toString();
        }
    }

    private static class Sector extends FrameLookup {
        private final int _iSector;

        public Sector(int iHeaderNumber) {
            _iSector = iHeaderNumber;
        }
        @Override
        public int compareTo(@Nonnull FrameNumber fn) {
            return Misc.intCompare(_iSector, fn.getSector());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + _iSector;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass())
                return false;
            final Sector other = (Sector) obj;
            return _iSector == other._iSector;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append(FrameNumber.SECTOR_PREFIX)
                    .append(_iSector).toString();
        }
    }

    abstract public int compareTo(@Nonnull FrameNumber fn);

    @Override
    abstract public boolean equals(Object o);

    @Override
    abstract public int hashCode();

    @Override
    abstract public String toString();
}
