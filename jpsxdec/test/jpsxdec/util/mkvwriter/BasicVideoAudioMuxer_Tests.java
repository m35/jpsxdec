/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2025-2026  Michael Sabin
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

package jpsxdec.util.mkvwriter;

import org.junit.Assert;
import org.junit.Test;

public class BasicVideoAudioMuxer_Tests {

    @Test
    public void testFlushSequence123() {
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("a"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("f"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("aa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("af"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("fa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("ff"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("aaa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("aaf"));
        Assert.assertEquals(1, BasicVideoAudioMuxer.countBlocksToFlush("afa")); // ****
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("aff"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("faa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("faf"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("ffa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("fff"));
    }

    @Test
    public void testFlushSequence4() {
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("aaaa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("aaaf"));
        Assert.assertEquals(2, BasicVideoAudioMuxer.countBlocksToFlush("aafa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("aaff"));
        Assert.assertEquals(1, BasicVideoAudioMuxer.countBlocksToFlush("afaa"));
        Assert.assertEquals(1, BasicVideoAudioMuxer.countBlocksToFlush("afaf"));
        Assert.assertEquals(2, BasicVideoAudioMuxer.countBlocksToFlush("affa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("afff"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("faaa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("faaf"));
        Assert.assertEquals(2, BasicVideoAudioMuxer.countBlocksToFlush("fafa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("faff"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("ffaa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("ffaf"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("fffa"));
        Assert.assertEquals(0, BasicVideoAudioMuxer.countBlocksToFlush("ffff"));
    }
}
