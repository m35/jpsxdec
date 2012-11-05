/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import jpsxdec.util.player.VideoPlayer.VideoFrame;

/** Manages writing audio data to the final SourceDataLine. */
public class AudioPlayer implements IVideoTimer {

    private static final Logger log = Logger.getLogger(AudioPlayer.class.getName());
    private static final boolean DEBUG = false;

    private static final int SECONDS_OF_BUFFER = 5;

    private SourceDataLine _dataLine;
    private final PlayingState _state = new PlayingState(PlayingState.State.STOPPED);

    private final AudioFormat _format;
    private final double _dblTimeConvert;
    private final PlayController _controller;

    AudioPlayer(AudioFormat format, PlayController controller) throws LineUnavailableException {
        _format = format;
        _controller = controller;
        _dblTimeConvert = 1000000000. / _format.getSampleRate();
    }

    private static SourceDataLine createOpenLine(AudioFormat format) throws LineUnavailableException {
        
        final boolean blnUseDefault = true;
        SourceDataLine dataLine;
        if (blnUseDefault) {
            dataLine = AudioSystem.getSourceDataLine(format);
        } else {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            Mixer.Info[] aoMixerInfos = AudioSystem.getMixerInfo();
            System.out.println("[AudioPlayer] Available mixers:");
            for (Mixer.Info mixerInfo : aoMixerInfos) {
                System.out.println("[AudioPlayer] " + mixerInfo.getName());
            }

            Mixer mixer = AudioSystem.getMixer(aoMixerInfos[0]);
            dataLine = (SourceDataLine)mixer.getLine(info);
        }
        /*  Start and Stop events are useless to me because they do not
         *  occur when playing is stopped due to no data in the buffer.
        dataLine.addLineListener(new LineListener() {
            public void update(LineEvent event) {
                System.out.println(event);
            }
        });
        */

        dataLine.open(format, format.getFrameSize() * (int)format.getSampleRate() * SECONDS_OF_BUFFER);
        return dataLine;
    }

    /** Will block until all audio was written or there is a player state change. */
    public void write(byte[] abData, int iStart, int iLength) {
        try {
            int iTotalWritten = 0;
            while (iTotalWritten < iLength) {
                if (_state.get() == PlayingState.State.STOPPED) {
                    _dataLine.close();
                    return;
                }
                int iWritten = _dataLine.write(abData, iTotalWritten, iLength - iTotalWritten);
                iTotalWritten += iWritten;
                if (iTotalWritten < iLength) {
                    System.out.println("[AudioPlayer] Only " + iWritten + " bytes of audio was written, "
                                      + "progress " + iTotalWritten + "/" + iLength);
                    synchronized (_state) {
                        if (_state.get() == PlayingState.State.PAUSED)
                            _state.waitForChange();
                    }
                }
            }
        } catch (Throwable ex) {
            log.log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            _state.set(PlayingState.State.STOPPED);
        }
    }

    /** Buffer of zeros for writing lots of zeros. */
    private byte[] _abZeroBuff;

    public void writeSilence(long lngSamples) {
        try {
            if (_abZeroBuff == null) {
                _abZeroBuff = new byte[_format.getFrameSize() * 2048];
            }
            long lngBytesLeft = lngSamples * _format.getFrameSize();
            final long lngBytesToWrite = lngBytesLeft;
            while (lngBytesLeft > 0) {
                synchronized (_state) {
                    if (_state.get() == PlayingState.State.STOPPED) {
                        _dataLine.close();
                        return;
                    }
                }
                int iWritten = _dataLine.write(_abZeroBuff, 0, (int)Math.min(lngBytesLeft, _abZeroBuff.length));
                lngBytesLeft -= iWritten;
                if (lngBytesLeft > 0) {
                    System.out.println("[AudioPlayer] Only " + iWritten + " bytes of silence was written, "
                                      + "progress " + (lngBytesToWrite - lngBytesLeft) + "/" + lngBytesToWrite);
                    synchronized (_state) {
                        if (_state.get() == PlayingState.State.PAUSED)
                            _state.waitForChange();
                    }
                }
            }
        } catch (Throwable ex) {
            log.log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            _state.set(PlayingState.State.STOPPED);
        }
    }

    void blockUntilEndThenStop() {
        _dataLine.drain();
        stop();
    }
    
    void play() throws LineUnavailableException {
        synchronized (_state) {
            switch (_state.get()) {
                case PAUSED:
                    _dataLine.start();
                    _state.set(PlayingState.State.PLAYING);
                    break;
                case STOPPED:
                    _dataLine = createOpenLine(_format);
                    _dataLine.start();
                    _state.set(PlayingState.State.PLAYING);
                    break;
            }
        }
    }

    void pause() throws LineUnavailableException {
        synchronized (_state) {
            switch (_state.get()) {
                case PLAYING:
                    _dataLine.stop();
                    _state.set(PlayingState.State.PAUSED);
                    break;
                case STOPPED:
                    _dataLine = createOpenLine(_format);
                    _state.set(PlayingState.State.PAUSED);
                    break;
            }
        }
    }

    void stop() {
        synchronized (_state) {
            switch (_state.get()) {
                case PLAYING:
                case PAUSED:
                    _dataLine.close();
                    _state.set(PlayingState.State.STOPPED);
                    _controller.fireStopped();
                    break;
            }
        }
    }

    public long getPlayTime() {
        return (long)(_dataLine.getLongFramePosition() * _dblTimeConvert);
    }

    public AudioFormat getFormat() {
        return _format;
    }

    public boolean shouldBeProcessed(long lngPresentationTime) {
        synchronized (_state) {
            switch (_state.get()) {
                case PAUSED:
                case PLAYING:
                    long lngPlayTime = getPlayTime();
                    if (DEBUG) System.out.println("[AudioPlayer] Play time = " + lngPlayTime + " vs. Pres time = " + lngPresentationTime);
                    return lngPresentationTime >= lngPlayTime;
                case STOPPED:
                    return false;
                default:
                    throw new RuntimeException("Should never happen");
            }
        }
    }

    public boolean waitToPresent(VideoFrame frame) {
        try {
            synchronized (_state) {
                while (true) {
                    switch (_state.get()) {
                        case PAUSED:
                            if (DEBUG) System.out.println("[AudioPlayer] AudioPlayer timer, waiting to present");
                            _state.waitForChange();
                            if (DEBUG) System.out.println("[AudioPlayer] AudioPlayer timer, not waiting anymore");
                            break; // loop again to see the new state
                        case STOPPED:
                            return false;
                        case PLAYING:
                            long lngPos = getPlayTime();
                            long lngSleepTime;
                            if ((lngSleepTime = frame.PresentationTime - lngPos) > 10000) {
                                lngSleepTime -= 10000;
                                _state.wait(lngSleepTime / 1000000, (int)(lngSleepTime % 1000000));
                                break;
                            } else {
                                return true;
                            }
                        default:
                            throw new RuntimeException("Should never happen");
                    }
                }
            }
        } catch (Throwable ex) {
            log.log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            return false;
        }
    }

    public void drain() {
        if (_dataLine != null)
            _dataLine.drain();
    }


}
