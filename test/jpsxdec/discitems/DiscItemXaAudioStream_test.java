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

import java.io.File;
import java.io.IOException;
import jpsxdec.audio.XaAdpcmDecoder;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.ConsoleProgressListenerLogger;
import jpsxdec.util.IO;
import jpsxdec.util.TaskCanceledException;
import static org.junit.Assert.*;
import org.junit.*;
import static testutil.Util.resourceAsFile;
import static testutil.Util.resourceAsTempFile;

public class DiscItemXaAudioStream_test {
    
    public DiscItemXaAudioStream_test() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void sectorStride1() {
        final int BITS_PER_SAMPLE = 4;
        DiscItemXaAudioStream item = new DiscItemXaAudioStream(
                0, 1, // 2 sectors long
                0, // channel
                XaAdpcmDecoder.pcmSamplesGeneratedFromXaAdpcmSector(BITS_PER_SAMPLE) * 2,
                37800, // samples/second
                false, // mono
                BITS_PER_SAMPLE, // bits/sample
                1); // sector stride
        
        assertTrue(true);
    }
    
    @Test
    public void replaceXa() throws IOException, TaskCanceledException {
        //final File EXPECTED = resourceAsTempFile(getClass(), "ORIGINALlain+REPLACE-PNG.STR");
        final File SAMPLES = new File("Worms126918-126950.bin");
        resourceAsFile(getClass(), "Worms126918-126950.bin", SAMPLES);
        final File EXPECTED = resourceAsTempFile(getClass(), "Worms126918-126950-replaced.bin");
        CdFileSectorReader cd = new CdFileSectorReader(SAMPLES, true);
        DiscIndex index = new DiscIndex(cd, new ConsoleProgressListenerLogger("test", System.out));
        DiscItemXaAudioStream xa1 = (DiscItemXaAudioStream) index.getByIndex(0);
        DiscItemXaAudioStream xa2 = (DiscItemXaAudioStream) index.getByIndex(1);
        try {
            xa1.replaceXa(System.out, xa2);
            assertTrue("Expected exception", false);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            assertEquals(ex.getMessage(), "Sector index out of bounds of this disc item");
        }
        cd.close();
        assertArrayEquals(IO.readFile(EXPECTED), IO.readFile(SAMPLES));
        SAMPLES.delete();
    }
    
}
