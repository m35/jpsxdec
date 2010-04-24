/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/** Inverts the file writing process from pulling data from an AudioInputStream
 *  to pushing the data. */
public class AudioOutputFileWriter implements Runnable {

    private final PipedInputStream _threadStream;
    private final AudioInputStream _threadAudioStream;
    private final PipedOutputStream _feedStream;
    private final AudioFormat _format;
    private final Thread _writingThread;
    private final AudioFileFormat.Type _eFileFormat;
    private final File _outFile;
    private Throwable _writingError;

    public AudioOutputFileWriter(String sFile, AudioFormat format,
                                 AudioFileFormat.Type eFileFormat)
            throws IOException
    {
        this(new File(sFile), format, eFileFormat);
    }

    public AudioOutputFileWriter(File file, AudioFormat format,
                                 AudioFileFormat.Type eFileFormat)
             throws IOException
    {
        // set everything up
        _format = format;
        _eFileFormat = eFileFormat;
        _outFile = file;
        _threadStream = new PipedInputStream();
        _feedStream = new PipedOutputStream(_threadStream);
        _threadAudioStream = new AudioInputStream(_threadStream, format, AudioSystem.NOT_SPECIFIED);

        // start the writing thread
        _writingThread = new Thread(this);
        _writingThread.start();
        try {
            // wait until the thread has started
            // this wait will end when the other thread is either
            // waiting for data or there was an error
            synchronized (_threadStream) {
                _threadStream.wait();

                // check if there was an error
                if (_writingError != null) {
                    if (_writingError instanceof IOException)
                        throw (IOException)_writingError;
                    else
                        throw new RuntimeException(_writingError);
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(AudioOutputFileWriter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public AudioFormat getFormat() {
        return _format;
    }

    public void write(AudioFormat inFormat, byte[] abData,
                      int iOffset, int iLength)
            throws IOException
    {
        if (!inFormat.matches(_format))
            throw new IllegalArgumentException("Incompatable audio format.");

        // check if there has been an error in the writing thread
        synchronized (_threadStream) {
            if (_writingError != null) {
                if (_writingError instanceof IOException)
                    throw (IOException)_writingError;
                else
                    throw new RuntimeException(_writingError);
            }
        }

        _feedStream.write(abData, iOffset, iLength);

        // again check if there has been an error in the writing thread
        synchronized (_threadStream) {
            if (_writingError != null) {
                if (_writingError instanceof IOException)
                    throw (IOException)_writingError;
                else
                    throw new RuntimeException(_writingError);
            }
        }
    }

    @Override
    public void run() {
        try {
            // start writing the audio file
            AudioSystem.write(_threadAudioStream, _eFileFormat, _outFile);
        } catch (IOException ex) {
            // if there's an error, save it and notify the main thread
            // in case it is waiting after startup
            synchronized (_threadStream) {
                _writingError = ex;
                _threadStream.notifyAll();
            }
            Logger.getLogger(AudioOutputFileWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void close() throws IOException {

        // flush out the rest of the data written before closing the stream
        _feedStream.flush();
        _feedStream.close();

        try {
            // important to wait until the writing thread is done in case
            // System.exit() is called before the writer is completely finished
            _writingThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(AudioOutputFileWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
