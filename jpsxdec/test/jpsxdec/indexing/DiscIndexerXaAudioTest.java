/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2023  Michael Sabin
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

import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSector2352;
import jpsxdec.i18n.log.DebugLogger;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.xa.DiscIndexerXaAudio;
import jpsxdec.util.ByteArrayFPIS;
import org.junit.*;


public class DiscIndexerXaAudioTest {

    private static class DummyIdentifiedSector extends IdentifiedSector {

        private static final int[] SECTOR_HEADER = {
            0x00,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0x00, // sync header
            0x00,0x00,0x00, // BCD block address
            2,   // mode 2
            // sub header(1):
            0  , // interleaved
            255, // channel
            0x88, // EOF | DATA
            0x00, // coding info
            // sub header(2):
            0  , // interleaved
            255, // channel
            0x88, // EOF | DATA
            0x00, // coding info
        };

        private static CdSector makeDummySector() {
            byte[] abSectorBytes = new byte[CdSector.SECTOR_SIZE_2352_BIN];
            for (int i = 0; i < SECTOR_HEADER.length; i++) {
                abSectorBytes[i] = (byte)SECTOR_HEADER[i];
            }
            return new CdSector2352(0, abSectorBytes, 0, 0);
        }

        public DummyIdentifiedSector() {
            super(makeDummySector());
        }

        public String getTypeName() {
            return "dummy";
        }

        public int getIdentifiedUserDataSize() {
            return 0;
        }

        public ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(new byte[0]);
        }
    }


    @Test
    public void channel255() {
        DiscIndexerXaAudio xaIndexer = new DiscIndexerXaAudio(DebugLogger.Log);
        // just testing that nothing terrible happens
        DummyIdentifiedSector s = new DummyIdentifiedSector();
        xaIndexer.feedSector(s, DebugLogger.Log);
    }

}
