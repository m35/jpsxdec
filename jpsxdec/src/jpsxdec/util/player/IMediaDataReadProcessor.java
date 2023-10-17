/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

import javax.annotation.Nonnull;

/**
 * A class provided by the user that will read audio and/or video data
 * in one thread, and process frame data in another thread to an RGB image.
 * Class to write raw frame data to by the {@link IMediaDataReader}.
 *
 * Write raw video frame data to {@link MediaDataWriter#writeFrame(Object, long)}
 * that will later be sent to {@link #processFrameThread(Object, int[])} in another thread.
 * Write PCM data to {@link MediaDataWriter#getAudioPlayerOutputStream()}.
 *
 * The player will start this reader in its own thread, and the reader should
 * loop until data is exhausted. Then the reader should just simply return,
 * which will end the thread and media playback will end once all audio/video has been
 * presented.
 *
 * If an error occurs, the reader can also simply return if it's ok for any
 * pending audio/video to finish playing, or throw {@link StopPlayingException}
 * to immediately terminate all playback and discard any buffered data.
 *
 * @param <FRAME_TYPE> type
 */
public interface IMediaDataReadProcessor<FRAME_TYPE> {
    /**
     * The reader thread is responsible for reading data and writing it to the
     * appropriate functions in {@link MediaDataWriter}.
     */
    void readerThread(@Nonnull MediaDataWriter<FRAME_TYPE> writer) throws StopPlayingException;

    /**
     * The processing thread will receive the frame that was sent from the reader thread.
     * This method is called for each frame sent from the reader thread.
     * Note that if the player is exhausted, then frames will be skipped.
     * So this probably wouldn't work for mpeg P or B frames.
     *
     * @param aiDrawRgb24Here buffer to write frame pixels as used by BI
     *                        The array will have width*height elements.
     * @see java.awt.image.BufferedImage#setRGB(int, int, int, int, int[], int, int)
     */
    /**
     * A class provided by the user that will accept raw frames, process them
     * and then copy the final frame data into the provided int array
     * in xRGB format. Top 8 bits ignored, next 24 bits are red, green, and blue.
     * This will be called for every frame in the same video processing thread.
     * This allows the processor to maintain some kind of state between frames.
     */
    void processFrameThread(@Nonnull FRAME_TYPE frame, @Nonnull int[] aiDrawRgb24Here) throws StopPlayingException;
}
