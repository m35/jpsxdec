/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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

package jpsxdec.discitems;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.ByteArrayFPIS;

/** Demuxes a series of CdSectors into a solid stream. */
public class DemuxedSectorInputStream extends SequenceInputStream {

    private static final Logger LOG = Logger.getLogger(DemuxedSectorInputStream.class.getName());
    
    private static class SectorEnumerator implements Enumeration<InputStream> {
        public final CdFileSectorReader _cd;
        private final int _iOffset;
        private final int _iStartSector;
        private final int _iEndSector;
        public int _iSector;
        private ByteArrayFPIS _currentStream;

        public SectorEnumerator(CdFileSectorReader cd, int iSector, int iOffset, int iEndSector) {
            _cd = cd;
            _iStartSector = _iSector = iSector;
            _iOffset = iOffset;
            _iEndSector = iEndSector;
        }
        
        public boolean hasMoreElements() {
            return _iSector <= _iEndSector;
        }

        public InputStream nextElement() {
            if (!hasMoreElements())
                throw new NoSuchElementException();
            try {
                // TODO: What to do with CD or form 2 sectors?
                CdSector sector = _cd.getSector(_iSector);
                if (sector.isCdAudioSector())
                    LOG.warning("Reading CD sector intermingled with Form 1 sectors.");
                else if (sector.hasSubHeader() && sector.getSubMode().getForm() == 2)
                    LOG.warning("Reading form 2 sector intermingled with Form 1 sectors.");
                _currentStream = sector.getCdUserDataStream();
                if (_iSector == _iStartSector) {
                    _currentStream.skip(_iOffset);
                }
                _iSector++;
                return _currentStream;
            } catch (final IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
                _currentStream = null;
                return new InputStream() {
                    public int read() throws IOException { throw ex; }
                };
            }
        }
        
    }

    public DemuxedSectorInputStream(CdFileSectorReader cd, int iStartSector, int iOffset) {
        this(cd, iStartSector, iOffset, cd.getLength()-1);
    }
    
    public DemuxedSectorInputStream(CdFileSectorReader cd, int iStartSector, int iOffset, int iEndSector) {
        super(new SectorEnumerator(cd, iStartSector, iOffset, iEndSector));
    }
    
    @Override
    public void close() throws IOException {
        // no need to do anything
        // and definitely don't want to do what SequenceInputStream does
    }
    
}
