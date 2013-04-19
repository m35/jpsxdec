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

package jpsxdec.discitems.psxvideoencode;

import java.io.File;
import java.io.IOException;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItemStrVideoStream;
import jpsxdec.discitems.FrameDemuxer;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.ConsoleProgressListenerLogger;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.TaskCanceledException;
import org.junit.*;
import static org.junit.Assert.assertArrayEquals;
import testutil.Util;
import static testutil.Util.resourceAsFile;
import static testutil.Util.resourceAsTempFile;

public class Replace {

    public Replace() {
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
    public void image_v2() throws IOException, TaskCanceledException {
        final File EXPECTED = resourceAsTempFile(getClass(), "ORIGINALv2+REPLACE-PNG.STR");
        final File REPLACEMENT_FRAME = resourceAsTempFile(getClass(), "REPLACE320x240.PNG");

        final File ACTUAL = new File("ACTUAL-image_v2.STR");
        resourceAsFile(getClass(), "ORIGINALv2.STR", ACTUAL);

        final ReplaceFrame rf = new ReplaceFrame(1);
        rf.setImageFile(REPLACEMENT_FRAME);

        doReplace(rf, ACTUAL);

        // now compare
        assertArrayEquals(IO.readFile(EXPECTED), IO.readFile(ACTUAL));
        ACTUAL.delete();
    }

    @Test
    public void image_v3() throws IOException, TaskCanceledException {
        final File EXPECTED = resourceAsTempFile(getClass(), "ORIGINALv3+REPLACE-PNG.STR");
        final File REPLACEMENT_FRAME = resourceAsTempFile(getClass(), "REPLACE320x160.PNG");

        final File ACTUAL = new File("ACTUAL-image_v3.STR");
        Util.resourceAsFile(getClass(), "ORIGINALv3.STR", ACTUAL);

        final ReplaceFrame rf = new ReplaceFrame(1);
        rf.setImageFile(REPLACEMENT_FRAME);

        doReplace(rf, ACTUAL);

        // now compare
        assertArrayEquals(IO.readFile(EXPECTED), IO.readFile(ACTUAL));
        ACTUAL.delete();
    }

    @Test
    public void image_lain() throws IOException, TaskCanceledException {
        final File EXPECTED = resourceAsTempFile(getClass(), "ORIGINALlain+REPLACE-PNG.STR");
        final File REPLACEMENT_FRAME = resourceAsTempFile(getClass(), "REPLACE320x240.PNG");

        final File ACTUAL = new File("ACTUAL-image_lain.STR");
        resourceAsFile(getClass(), "ORIGINALlain.STR", ACTUAL);

        final ReplaceFrame rf = new ReplaceFrame(1);
        rf.setImageFile(REPLACEMENT_FRAME);

        doReplace(rf, ACTUAL);

        // now compare
        assertArrayEquals(IO.readFile(EXPECTED), IO.readFile(ACTUAL));
        ACTUAL.delete();
    }



    @Test
    public void bitstream_v2() throws IOException, TaskCanceledException {
        final File ORIGINAL = resourceAsTempFile(getClass(), "ORIGINALv2.STR");
        final File REPLACEMENT_FRAME = resourceAsTempFile(getClass(), "ORIGINALv2-frame1-320x240.bs");

        final File ACTUAL = new File("ACTUAL-bs_v2.STR");
        resourceAsFile(getClass(), "ORIGINALv2.STR", ACTUAL);

        final ReplaceFrame rf = new ReplaceFrame(1);
        rf.setImageFile(REPLACEMENT_FRAME);
        rf.setFormat("bs");

        doReplace(rf, ACTUAL);
        // now compare
        assertArrayEquals(IO.readFile(ORIGINAL), IO.readFile(ACTUAL));
        ACTUAL.delete();
    }

    @Test
    public void bitstream_lain() throws IOException, TaskCanceledException {
        final File ORIGINAL = resourceAsTempFile(getClass(), "ORIGINALlain.STR");
        final File REPLACEMENT_FRAME = resourceAsTempFile(getClass(), "ORIGINALlain-frame4-320x240.bs");

        final File ACTUAL = new File("ACTUAL-bs_lain.STR");
        resourceAsFile(getClass(), "ORIGINALlain.STR", ACTUAL);

        final ReplaceFrame rf = new ReplaceFrame(4);
        rf.setImageFile(REPLACEMENT_FRAME);
        rf.setFormat("bs");

        doReplace(rf, ACTUAL);
        // now compare
        assertArrayEquals(IO.readFile(ORIGINAL), IO.readFile(ACTUAL));
        ACTUAL.delete();
    }


    private static void doReplace(final ReplaceFrame rf, File strFile)
            throws IOException, TaskCanceledException
    {
        final CdFileSectorReader cd = new CdFileSectorReader(strFile, true);
        ConsoleProgressListenerLogger log = new ConsoleProgressListenerLogger("test", System.out);
        DiscIndex index = new DiscIndex(cd, log);
        DiscItemStrVideoStream vid = (DiscItemStrVideoStream) index.getByIndex(0);
        final FrameDemuxer demuxer = new FrameDemuxer(vid.getWidth(), vid.getHeight(), vid.getStartSector(), vid.getEndSector());
        demuxer.setFrameListener(new ISectorFrameDemuxer.ICompletedFrameListener() {
            public void frameComplete(IDemuxedFrame frame) throws IOException {
                try {
                    if (frame.getFrame() == rf.getFrame())
                        rf.replace(frame, cd, new FeedbackStream());
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }
        });

        for (int i = 0; i < vid.getSectorLength(); i++) {
            IdentifiedSector sect = vid.getRelativeIdentifiedSector(i);
            if (sect != null)
                demuxer.feedSector(sect, log);
        }
        demuxer.flush(log);

        cd.close();
    }

}