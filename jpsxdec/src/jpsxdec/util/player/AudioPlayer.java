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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import jpsxdec.util.IO;

/**
 * Manages writing audio data to the final {@link SourceDataLine}.
 * A buffer sits between the data written to this class and the actual DataLine
 * because DataLine seems to have a very small buffer itself.
 * A thread manages copying the buffer into the DataLine.
 *<p>
 * This also extends VideoTimer that manages the video playback using
 * the audio timer. It conveniently uses the DataLine's listener thread to
 * fire events.
 */
class AudioPlayer extends VideoTimer implements Runnable, LineListener {

    private static final Logger LOG = Logger.getLogger(AudioPlayer.class.getName());
    private static final boolean DEBUG = false;

    private static final boolean IS_WINDOWS;
    static {
        IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static final long NANOS_PER_SECOND = 1000000000;

    /** On Ubuntu (circa 2019) it seems a big buffer causes issues when video is paused.
     * The play time drifts a lot when paused and buffering, so when
     * unpaused the video hangs a lot. Smaller buffer reduces that effect. */
    private static final double SECONDS_OF_BUFFER_NON_WINDOWS = 0.1;
    /** On Windows it seems a small buffer causes stuttering, so use a big one. */
    private static final double SECONDS_OF_BUFFER_WINDOWS = 5;

    @Nonnull
    private final AudioFormat _format;
    @Nonnull
    private final PipedInputStream _pipedInputStream;
    @Nonnull
    private final PipedOutputStream _pipedOutputStream;

    private final double _dblSamplesPerNano;
    private final int _iCopyBufferSize;

    @Nonnull
    private final Thread _thisThread;

    private SourceDataLine _dataLine;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // TODO Experiment with using the system clock and intermittently sync with the audio time
    private boolean _blnUseAudioAndSystemClockTogether = false;
    private long _lngStartTime = -1;
    private long _lngLastSync = -1;
    private static final long RESYNC_EVERY_NANOS = NANOS_PER_SECOND / 2;
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public AudioPlayer(@Nonnull AudioFormat format) {
        _format = format;

        _dblSamplesPerNano = (double)NANOS_PER_SECOND / _format.getSampleRate();

        int iPipeSize = _format.getFrameSize() * Math.round(_format.getSampleRate()) * 5;
        _pipedInputStream = new PipedInputStream(iPipeSize);
        try {
            _pipedOutputStream = new PipedOutputStream(_pipedInputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Should not happen", ex);
        }
        _iCopyBufferSize = (int) (format.getFrameSize() * format.getSampleRate() * 5);

        _thisThread = new Thread(this, getClass().getName());
    }


    @Override
    public synchronized void initPaused() throws PlayerException {
        if (_dataLine != null)
            throw new IllegalStateException();
        try {
            _dataLine = createOpenLine(_format);
            // Important note about dataline listeners:
            // The events are not triggered when there is a buffer under-run
            // although the docs kinda suggest that they are
            _dataLine.addLineListener(this);
            _thisThread.start();
        } catch (LineUnavailableException ex) {
            throw new PlayerException(ex);
        }
    }

    private static @Nonnull SourceDataLine createOpenLine(@Nonnull AudioFormat format)
            throws LineUnavailableException
    {

        final boolean blnUseDefault = true;
        SourceDataLine dataLine = null;
        if (blnUseDefault) {
            dataLine = AudioSystem.getSourceDataLine(format);
        } else {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            Mixer.Info[] aoMixerInfos = AudioSystem.getMixerInfo();
            System.out.println("[AudioPlayer] Available mixers:");
            for (Mixer.Info mixerInfo : aoMixerInfos) {
                System.out.println("[AudioPlayer] " + mixerInfo.getName() + " " + mixerInfo.getDescription());
            }

            for (Mixer.Info mixerInfo : aoMixerInfos) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                try {
                    System.out.println("[AudioPlayer] Trying " + mixerInfo.getName());
                    if (mixerInfo.getName().contains("default"))
                        continue;
                    dataLine = (SourceDataLine)mixer.getLine(info);
                    break;
                } catch (LineUnavailableException | IllegalArgumentException ex) {
                    ex.printStackTrace();
                }
            }
        }

        double dblSeconds;
        if (IS_WINDOWS)
            dblSeconds = SECONDS_OF_BUFFER_WINDOWS;
        else
            dblSeconds = SECONDS_OF_BUFFER_NON_WINDOWS;

        int iRequestedBufferSize = (int) (format.getFrameSize() * format.getSampleRate() * dblSeconds);
        dataLine.open(format, iRequestedBufferSize);
        // for some reason on Ubuntu (circa 2019) the audio starts as soon as data is fed to it
        // no matter how many times .stop() is called
        // somehow calling start() then stop() gets around it
        dataLine.start();
        dataLine.stop();
        int iActualBufferSize = dataLine.getBufferSize();
        System.out.println("Dataline requested buffer size " + iRequestedBufferSize + " actual buffer size " + iActualBufferSize);
        return dataLine;
    }

    @Override
    public void run() {
        try {
            byte[] abCopyBuffer = new byte[_iCopyBufferSize];
            int iBytesToWrite = 0, iBytesWritten = 0;
            while (true) {
                if (!_dataLine.isOpen()) {
                    break;
                }
                System.out.println("Dataline is open");

                if (iBytesWritten < iBytesToWrite) {
                    int iToWrite = iBytesToWrite - iBytesWritten;
                    int iFrameRemainder = iToWrite % _format.getFrameSize();
                    if (iFrameRemainder > 0)
                        iToWrite -= iFrameRemainder;
                    if (DEBUG) System.out.println("Writing " + iToWrite + " bytes of audio");
                    int iWrote = _dataLine.write(abCopyBuffer, iBytesWritten, iToWrite);
                    if (DEBUG) System.out.println("Actually wrote " + iWrote + " bytes of audio");
                    iBytesWritten += iWrote;
                    if (iWrote < iToWrite) {
                        boolean blnDueToPause = false;
                        // possible race condition here if the play state changed between writing audio data and checking state
                        // but that shouldn't cause any problems except trigger the message below
                        synchronized (this) {
                            if (isPaused()) {
                                this.wait();
                                blnDueToPause = true;
                            }
                        }
                        if (!blnDueToPause) {
                            // special to note that the dataline will continually reject audio while it is paused
                            // so no need to log in that case, just wait
                            System.out.println("[AudioPlayer] Only " + iWrote +
                                    " bytes of audio was written. Progress: " +
                                    iBytesWritten + "/" + iBytesToWrite);
                        }

                    }
                } else {
                    iBytesToWrite = _pipedInputStream.read(abCopyBuffer);
                    if (DEBUG) System.out.println("Got " + iBytesToWrite + " bytes of audio");
                    iBytesWritten = 0;
                    if (iBytesToWrite < 0) {
                        break;
                    }
                }
            }
                    System.out.println("[audio] Something changed");
        } catch (IOException ex) {
            // this hopefully only happens because the reader was forcefully closed
            System.out.println("Audio player IOException stop: " + ex.getMessage());
            _dataLine.close();
            super.terminate();
            return;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            _dataLine.close();
            super.terminate();
            return;
        }
        _dataLine.drain();
        _dataLine.close();
        super.terminate();
    }


    public @Nonnull OutputStream getOutputStream() {
        return _pipedOutputStream;
    }

    @Override
    public synchronized void go() {
        if (DEBUG) System.out.println("AudioPlayer go!");
        // synchronized since this is 2 operations
        // and we don't want the player state to change in the middle
        if (_dataLine != null) {
            long lngNow = System.nanoTime();
            _lngLastSync = lngNow;
            _lngStartTime = lngNow - (long)(_dataLine.getLongFramePosition() * _dblSamplesPerNano);
            _dataLine.start();
        }
        super.go();
    }

    @Override
    public synchronized void pause() {
        // synchronized to keep the dataline state in sync with the VideoTimer state
        if (_dataLine != null) {
            //_dataLine.flush();
            _dataLine.stop();
        }
        super.pause();
    }

    public void finish() {
        // it looks like it's thread-safe to close the stream
        IO.closeSilently(_pipedOutputStream, LOG);
    }

    @Override
    public void videoDone() {
        // it doesn't matter if the video is done, we continue playing audio
    }

    @Override
    public synchronized void terminate() {
        // it looks like it's thread-safe to close the streams
        // synchronized to keep the dataline state in sync with the VideoTimer state
        // Make sure to close the player first!
        // Otherwise possible deadlock if the thread is stuck writing when this is called
        if (_dataLine != null) {
            _dataLine.stop();
            _dataLine.close();
        }
        System.out.println("Just closed dataline, closing piped streams");
        IO.closeSilently(_pipedOutputStream, LOG);
        IO.closeSilently(_pipedInputStream, LOG);
        super.terminate();
    }

    @Override
    public synchronized long getNanoTime() {
        if (!_blnUseAudioAndSystemClockTogether || isPaused() || isTerminated()) {
            return (long)(_dataLine.getLongFramePosition() * _dblSamplesPerNano);
        } else {
            long lngNow = System.nanoTime();
            if (lngNow - _lngLastSync > RESYNC_EVERY_NANOS) {
                long lngPlayTime = (long)(_dataLine.getLongFramePosition() * _dblSamplesPerNano);
                long lngOldStartTime = _lngStartTime;
                _lngStartTime = lngNow - lngPlayTime;
                _lngLastSync = lngNow;
                System.out.println("Resyncing from " + lngOldStartTime + " to " + _lngStartTime + " (" + ((_lngStartTime - lngOldStartTime) / (double)NANOS_PER_SECOND) + ")");
                return lngPlayTime;
            } else {
                return lngNow - _lngStartTime;
            }
        }
    }

    /** Translate an audio event to this player's event. */
    @Override
    public void update(LineEvent event) {
        LineEvent.Type type = event.getType();
        PlayController.Event playerEvent;
        if (type == LineEvent.Type.CLOSE)
            playerEvent = PlayController.Event.End;
        else if (type == LineEvent.Type.START)
            playerEvent = PlayController.Event.Play;
        else if (type == LineEvent.Type.STOP)
            playerEvent = PlayController.Event.Pause;
        else
            return;

        fire(playerEvent);
    }
}
