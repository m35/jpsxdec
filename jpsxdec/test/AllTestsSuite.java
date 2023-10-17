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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    jpsxdec.TestLog.class,
    jpsxdec.adpcm.SpuDecodeCorruption.class,
    jpsxdec.adpcm.XaDecodeCorruption.class,
    jpsxdec.cmdline.Command_StaticTest.class,
    jpsxdec.discitems.DiscItemTest.class,
    jpsxdec.discitems.SerializedDiscItemTest.class,
    jpsxdec.formats.YCbCrImageTest.class,
    jpsxdec.indexing.DiscIndexerXaAudioTest.class,
    jpsxdec.modules.video.sectorbased.fps.Fps.class,
    jpsxdec.modules.xa.DiscItemXaAudioStreamTest.class,
    jpsxdec.psxvideo.PsxYCbCrTest.class,
    jpsxdec.psxvideo.bitstreams.BitReader.class,
    jpsxdec.psxvideo.bitstreams.Iki.class,
    jpsxdec.psxvideo.bitstreams.STRv2.class,
    jpsxdec.psxvideo.bitstreams.STRv3.class,
    jpsxdec.psxvideo.mdec.tojpeg.Mdec2JpegTest.class,
    jpsxdec.util.ArgParserTest.class,
    jpsxdec.util.DemuxPushInputStreamTest.class,
    jpsxdec.util.DemuxedDataTest.class,
    jpsxdec.util.FractionTest.class,
    jpsxdec.util.IOTest.class,
    jpsxdec.util.MiscTest.class,
    jpsxdec.util.player.ClosableBoundedBlockingQueueTest.class
})
public class AllTestsSuite {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

}
