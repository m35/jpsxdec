/*
 * LainTools: PSX Serial Experiments Lain Hacking and Translation Tools
 * Copyright (C) 2020-2023  Michael Sabin
 *
 * Redistribution and use of the LainTools code or any derivative works are
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

import java.io.File;
import java.util.BitSet;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.i18n.ILocalizedMessage;
import org.junit.*;
import static org.junit.Assert.*;

public class DiscItemXaAudioStreamTest {

    private static class MockSectorReader implements ICdSectorReader {
        @Override
        public int getSectorCount() {
            return 100;
        }

        public void close() { throw new AssertionError(); }
        public int getRawSectorSize() { throw new AssertionError(); }
        public CdSector getSector(int iSector) { throw new AssertionError(); }
        public ILocalizedMessage getTypeDescription() { throw new AssertionError(); }
        public boolean hasSectorHeader() { throw new AssertionError(); }
        public File getSourceFile() { throw new AssertionError(); }
        public boolean matchesSerialization(@Nonnull String sSerialization) { throw new AssertionError(); }
        public String serialize() { throw new AssertionError(); }
    }


    @Test
    public void testSilentSectors() {
        MockSectorReader cd = new MockSectorReader();

        // Disc is 100 sectors long
        // 5 XA sectors, 8 sectors apart: 10, 18, 26, 34, 42, 50
        XaAudioFormat format = new XaAudioFormat(1, 0, 18900, 8, true);

        DiscItemXaAudioStream xa = new DiscItemXaAudioStream(cd, 10, 50, format, 8, null);
        assertFalse(xa.isConfirmedToBeSilent());

        BitSet sectorsWithAudio = new BitSet();
        xa = new DiscItemXaAudioStream(cd, 10, 50, format, 8, sectorsWithAudio);
        assertTrue(xa.isConfirmedToBeSilent());

        sectorsWithAudio = new BitSet();
        sectorsWithAudio.set(0);
        xa = new DiscItemXaAudioStream(cd, 10, 50, format, 8, sectorsWithAudio);
        assertFalse(xa.isConfirmedToBeSilent());

        sectorsWithAudio = new BitSet();
        sectorsWithAudio.set(0, 5);
        xa = new DiscItemXaAudioStream(cd, 10, 50, format, 8, sectorsWithAudio);
        assertFalse(xa.isConfirmedToBeSilent());

        DiscItemXaAudioStream[] split = xa.split(25);
        assertEquals(10, split[0].getStartSector());
        assertEquals(18, split[0].getEndSector());
        assertFalse(split[0].isConfirmedToBeSilent());

        assertEquals(26, split[1].getStartSector());
        assertEquals(50, split[1].getEndSector());
        assertFalse(split[1].isConfirmedToBeSilent());


        sectorsWithAudio = new BitSet();
        sectorsWithAudio.set(0);
        xa = new DiscItemXaAudioStream(cd, 10, 50, format, 8, sectorsWithAudio);
        assertFalse(xa.isConfirmedToBeSilent());
        split = xa.split(25);
        assertFalse(split[0].isConfirmedToBeSilent());
        assertTrue(split[1].isConfirmedToBeSilent());

        sectorsWithAudio = new BitSet();
        sectorsWithAudio.set(4);
        xa = new DiscItemXaAudioStream(cd, 10, 50, format, 8, sectorsWithAudio);
        assertFalse(xa.isConfirmedToBeSilent());
        split = xa.split(25);
        assertTrue(split[0].isConfirmedToBeSilent());
        assertFalse(split[1].isConfirmedToBeSilent());
    }

}
