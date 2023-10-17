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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *  Handles running the reader supplied by the user.
 */
class ReaderThread<FRAME_TYPE> implements Runnable {

    private static final boolean DEBUG = false;

    @Nonnull
    private final PlayController _controller;
    @Nonnull
    private final MediaDataWriter<FRAME_TYPE> _readWriter;
    @Nonnull
    private final IMediaDataReadProcessor<FRAME_TYPE> _reader;

    @Nonnull
    private final Thread _thisThread;

    @CheckForNull
    private final AudioPlayer _audioPlayer;
    @CheckForNull
    private final VideoProcessorThread<FRAME_TYPE> _videoProcessorThread;

    public ReaderThread(@Nonnull PlayController controller,
                        @Nonnull MediaDataWriter<FRAME_TYPE> readWriter,
                        @Nonnull IMediaDataReadProcessor<FRAME_TYPE> reader,
                        @CheckForNull AudioPlayer audioPlayer,
                        @CheckForNull VideoProcessorThread<FRAME_TYPE> videoProcessorThread)
    {
        _audioPlayer = audioPlayer;
        _videoProcessorThread = videoProcessorThread;
        _controller = controller;
        _thisThread = new Thread(this, getClass().getName());
        _readWriter = readWriter;
        _reader = reader;
    }

    public void start() {
        if (DEBUG) System.out.println("Starting ReaderThread");
        _thisThread.start();
    }

    @Override
    public void run() {
        try {
            if (DEBUG) System.out.println("ReaderThread started");
            _reader.readerThread(_readWriter);
            if (DEBUG) System.out.println("Reader returned");
        } catch (Throwable ex) {
            if (!ex.getClass().equals(StopPlayingException.class)) {
                // not ok! Something bad happened
                ex.printStackTrace();
            }
            // immediately terminate
            _controller.terminate();
            return;
        }
        if (_audioPlayer != null)
            _audioPlayer.finish();
        if (_videoProcessorThread != null)
            _videoProcessorThread.finish();
    }

}
