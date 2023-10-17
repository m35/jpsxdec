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

package jpsxdec.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/** Inverts the file writing process from pulling data from an
 * {@link AudioInputStream} to pushing the data. */
public class AudioOutputFileWriter implements Runnable, Closeable {

    private static final Logger LOG = Logger.getLogger(AudioOutputFileWriter.class.getName());

    /** {@link PipedInputStream} internally has {@link notify()} triggers
     * so it can be relied on for synchronization purposes. */
    @Nonnull
    private final PipedInputStream _threadInputStream;
    @Nonnull
    private final AudioInputStream _threadAudioStream;
    @Nonnull
    private final Thread _writingThread;

    @Nonnull
    private final File _outFile;
    @Nonnull
    private final PipedOutputStream _feedStream;
    @Nonnull
    private final AudioFormat _format;
    @Nonnull
    private final AudioFileFormat.Type _eFileFormat;
    @CheckForNull
    private Throwable _writingError;

    public AudioOutputFileWriter(@Nonnull String sFile, @Nonnull AudioFormat format,
                                 @Nonnull AudioFileFormat.Type eFileFormat)
            throws IOException
    {
        this(new File(sFile), format, eFileFormat);
    }

    public AudioOutputFileWriter(@Nonnull File file, @Nonnull AudioFormat format,
                                 @Nonnull AudioFileFormat.Type eFileFormat)
             throws IOException
    {
        // set everything up
        _format = format;
        _eFileFormat = eFileFormat;
        _outFile = file;
        _threadInputStream = new PipedInputStream();
        _feedStream = new PipedOutputStream(_threadInputStream);
        _threadAudioStream = new AudioInputStream(_threadInputStream, format, AudioSystem.NOT_SPECIFIED);

        _writingThread = new Thread(this, AudioOutputFileWriter.class.getSimpleName() + " " + _outFile);
        // start the writing thread
        _writingThread.start();
        try {
            synchronized (_threadInputStream) {

                // wait until reading has started
                // PipedInputStream _threadInputStream internally will notify
                // the object when read is called
                // the pending notify() will be devoured here
                _threadInputStream.wait();
                // the wait will end when the other thread is either
                // waiting for data or there was an error

                // check if there was an error
                if (_writingError != null) {
                    if (_writingError instanceof IOException)
                        throw (IOException)_writingError;
                    else
                        throw new RuntimeException(_writingError);
                }
            }
        } catch (InterruptedException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {
        try {
            // start writing the audio file
            // PipedInputStream _threadInputStream internally will notify
            // the object when read is called
            AudioSystem.write(_threadAudioStream, _eFileFormat, _outFile);
        } catch (Throwable ex) {
            // if there's an error, save it and notify the main thread
            // in case it is waiting after startup
            // XXX: If the PipedInputStream and AudioSystem.write()
            //      implementations trigger the notify before the exception,
            //      this could be a race condition
            synchronized (_threadInputStream) {
                _writingError = ex;
                // fire the notification in case reading didn't call it
                _threadInputStream.notifyAll();
            }
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public void write(@Nonnull byte[] abData, int iOffset, int iByteLength)
            throws IOException
    {
        // check if there has been an error in the writing thread
        synchronized (_threadInputStream) {
            if (_writingError != null) {
                if (_writingError instanceof IOException)
                    throw (IOException)_writingError;
                else
                    throw new RuntimeException(_writingError);
            }
        }

        _feedStream.write(abData, iOffset, iByteLength);

        // again check if there has been an error in the writing thread
        synchronized (_threadInputStream) {
            if (_writingError != null) {
                if (_writingError instanceof IOException)
                    throw (IOException)_writingError;
                else
                    throw new RuntimeException(_writingError);
            }
        }
    }

    public @Nonnull AudioFormat getFormat() {
        return _format;
    }

    @Override
    public void close() throws IOException {

        // flush out the rest of the data written before closing the stream
        _feedStream.flush();
        try {
            _feedStream.close(); // expose close exception
        } finally {
            try {
                // important to wait until the writing thread is done in case
                // System.exit() is called before the writer is completely finished
                _writingThread.join();
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }
}
