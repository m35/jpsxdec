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

package jpsxdec.util.player;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import javax.annotation.Nonnull;

/**
 * Holds frame information through the playing process.
 * Holds the frame received from the {@link IMediaDataReadProcessor} along with its presentation time.
 * That is discarded once it has been processed and written to {@link MediaDataWriter#writeFrame(Object, long)}
 * It is held here until the presentation time.
 */
class VideoFrame<FRAME_TYPE> {

    /** The time the frame should be displayed, in nano-seconds
     *  from the beginning of the movie. */
    public final long lngPresentationNanos;

    public FRAME_TYPE frame;

    public BufferedImage image;

    public VideoFrame(long lngPresentationNanos, @Nonnull FRAME_TYPE frame) {
        this.lngPresentationNanos = lngPresentationNanos;
        this.frame = frame;
    }

    public void setDecoded(int[] aiDrawHere, int iWidth, int iHeight) {
        frame = null; // don't need original frame anymore
        // TODO experiment with how much time it takes to create this
        // and if there are other, faster ways to get decoded video data to the screen
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        image = gc.createCompatibleImage(iWidth, iHeight, Transparency.OPAQUE);
        image.setRGB(0, 0, iWidth, iHeight, aiDrawHere, 0, iWidth);
    }

}
