/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2020  Michael Sabin
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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** Handles the running the reader supplied by the user. */
class ReaderThread implements Runnable {

    @CheckForNull
    private final AudioPlayer _audioPlayer;
    @CheckForNull
    private final VideoProcessor _videoProcessor;
    @Nonnull
    private final PlayController _controller;
    @Nonnull
    private final Thread _thread;

    private IMediaDataReader _reader;

    public ReaderThread(@CheckForNull AudioPlayer audioPlayer, 
                        @CheckForNull VideoProcessor videoProcessor,
                        @Nonnull PlayController controller)
    {
        _audioPlayer = audioPlayer;
        _videoProcessor = videoProcessor;
        _controller = controller;
        _thread = new Thread(this, getClass().getName());
    }

    public void setReader(@Nonnull IMediaDataReader reader) {
        _reader = reader;
    }

    public void start() {
        if (_reader == null)
            throw new IllegalStateException();
        _thread.start();
    }

    public void run() {
        try {
            _reader.demuxThread(_controller);
        } catch (StopPlayingException ex) {
            // ok
            // immediately terminate
            _controller.terminate();
            return;
        } catch (Throwable ex) {
            // not ok
            ex.printStackTrace();
            // immediately terminate
            _controller.terminate();
            return;
        } 
        if (_audioPlayer != null)
            _audioPlayer.finish();
        if (_videoProcessor != null)
            _videoProcessor.finish();
    }

}
