/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

package jpsxdec.modules.audio;

import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;

/** A packet of decoded audio that can be passed around. */
public class AbstractDecodedAudioPacket {

    /** Channel that this audio packet belongs to.
     * This channel is like the CD sector channel,
     * so not left/right audio channel, but like audio track.
     * Convention is -1 means 'default' channel. */
    private final int _iChannel;
    @Nonnull
    private final AudioFormat _audioFmt;
    private final int _iSampleFrameCount;
    @Nonnull
    private final byte[] _abData;

    /**
     * @param iChannel Not left/right channel, but entirely separate stream.
     *                                         Use -1 for default.
     */
    public AbstractDecodedAudioPacket(int iChannel, @Nonnull AudioFormat audioFmt,
                                      @Nonnull byte[] abData)
    {
        if (abData.length % audioFmt.getFrameSize() != 0)
            throw new IllegalArgumentException();
        _iChannel = iChannel;
        _audioFmt = audioFmt;
        _iSampleFrameCount = abData.length / audioFmt.getFrameSize();
        _abData = abData;
    }

    /** -1 for default audio channel. */
    public int getChannel() {
        return _iChannel;
    }

    public @Nonnull AudioFormat getAudioFormat() {
        return _audioFmt;
    }

    public int getSampleFrameCount() {
        return _iSampleFrameCount;
    }

    public @Nonnull byte[] getData() {
        return _abData;
    }

    @Override
    public String toString() {
        return String.format(
                "Channel %d format %s SampleFrameCount %d Data.length %d",
                _iChannel, _audioFmt, _iSampleFrameCount, _abData.length);
    }

}
