
package jpsxdec.util.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import jpsxdec.util.player.VideoPlayer.VideoFrame;

public class AudioPlayer implements IVideoTimer {

    private static final boolean DEBUG = false;

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
            System.out.println("Available mixers:");
            for (Mixer.Info mixerInfo : aoMixerInfos) {
                System.out.println(mixerInfo.getName());
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

        dataLine.open(format);
        return dataLine;
    }

    public void write(byte[] abData, int iStart, int iLength) {
        try {
            int i = 0;
            while (i < iLength) {
                synchronized (_state) {
                    if (_state.get() == PlayingState.State.PAUSED)
                        _state.waitForChange();
                    if (_state.get() == PlayingState.State.STOPPED) {
                        _dataLine.close();
                        return;
                    }
                }
                i += _dataLine.write(abData, i, iLength - i);
                if (i < iLength) System.out.println("Not all audio data was written");
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            _state.set(PlayingState.State.STOPPED);
        }
    }

    private byte[] _abZeroBuff;

    public void writeSilence(long lngSamples) {
        try {
            if (_abZeroBuff == null) {
                _abZeroBuff = new byte[_format.getFrameSize() * 2048];
            }
            long lngBytesToWrite = lngSamples * _format.getFrameSize();
            while (lngBytesToWrite > 0) {
                synchronized (_state) {
                    if (_state.get() == PlayingState.State.PAUSED)
                        _state.waitForChange();
                    if (_state.get() == PlayingState.State.STOPPED) {
                        _dataLine.close();
                        return;
                    }
                }
                lngBytesToWrite -= _dataLine.write(_abZeroBuff, 0, (int)Math.min(lngBytesToWrite, _abZeroBuff.length));
                if (lngBytesToWrite > 0) System.out.println("Not all audio data was written");
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            _state.set(PlayingState.State.STOPPED);
        }
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

    private long _lngContiguousPlayId;

    public long getContiguousPlayId() {
        synchronized (_state) {
            return _lngContiguousPlayId;
        }
    }

    public boolean shouldBeProcessed(long lngContiguousPlayId, long lngPresentationTime) {
        synchronized (_state) {
            if (lngContiguousPlayId != _lngContiguousPlayId)
                return false;
            switch (_state.get()) {
                case PAUSED:
                case PLAYING:
                    long lngPlayTime = getPlayTime();
                    if (DEBUG) System.out.println("Play time = " + lngPlayTime + " vs. Pres time = " + lngPresentationTime);
                    return lngPresentationTime > lngPlayTime;
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
                    if (frame.ContigusPlayUniqueId != _lngContiguousPlayId)
                        return false;
                    switch (_state.get()) {
                        case PAUSED:
                            if (DEBUG) System.out.println("AudioPlayer timer, waiting to present");
                            _state.waitForChange();
                            if (DEBUG) System.out.println("AudioPlayer timer, not waiting anymore");
                            break; // loop again to see the new state, or new contiguous id
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
                        case STOPPED:
                            return false;
                        default:
                            throw new RuntimeException("Should never happen");
                    }
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void drain() {
        if (_dataLine != null)
            _dataLine.drain();
    }


}
