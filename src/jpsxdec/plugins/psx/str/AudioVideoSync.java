package jpsxdec.plugins.psx.str;

import jpsxdec.plugins.xa.AudioSync;
import jpsxdec.util.Fraction;

public class AudioVideoSync extends VideoSync {

    private AudioSync _audSync;
    private final Fraction _samplesPerFrame;
    private final int _iInitialFrameDelay;
    private final long _lngInitialSampleDelay;

    public AudioVideoSync(int iFirstVideoPresentationSector,
                          int iSectorsPerSecond,
                          Fraction sectorsPerFrame,
                          int iFirstAudioPresentationSector,
                          float fltSamplesPerSecond,
                          boolean blnPreciseAv)
    {
        super(iFirstVideoPresentationSector,
                iSectorsPerSecond, sectorsPerFrame);
        _audSync = new AudioSync(iFirstAudioPresentationSector,
                iSectorsPerSecond, fltSamplesPerSecond);

        _samplesPerFrame = _audSync.getSamplesPerSecond().multiply(super.getSecondsPerFrame());

        if (blnPreciseAv) {

            int iPresentationSectorDiff = iFirstAudioPresentationSector - iFirstVideoPresentationSector;

            Fraction initialSampleDelay = _audSync.getSamplesPerSecond().divide(getSectorsPerSecond()).multiply(iPresentationSectorDiff);
            if (initialSampleDelay.compareTo(0) < 0) {
                _iInitialFrameDelay = -(int) Math.floor(initialSampleDelay.divide(_samplesPerFrame).asDouble());
                _lngInitialSampleDelay = Math.round(initialSampleDelay.add(_samplesPerFrame.multiply(_iInitialFrameDelay)).asDouble());
            } else {
                _lngInitialSampleDelay = Math.round(initialSampleDelay.asDouble());
                _iInitialFrameDelay = 0;
            }
        } else {
            _lngInitialSampleDelay = 0;
            _iInitialFrameDelay = 0;
        }

    }

    @Override
    public int calculateFramesToCatchup(int iSector, long lngFramesWritten) {
        return super.calculateFramesToCatchup(iSector, lngFramesWritten - getInitialVideo());
    }

    public Fraction getSamplesPerSector() {
        return _audSync.getSamplesPerSector();
    }

    public Fraction getSamplesPerSecond() {
        return _audSync.getSamplesPerSecond();
    }

    public long getInitialAudio() {
        return _lngInitialSampleDelay;
    }

    @Override
    public int getInitialVideo() {
        return _iInitialFrameDelay;
    }

    public long calculateNeededSilence(int iSector, long lngSampleCount) {
        return _audSync.calculateNeededSilence(iSector, lngSampleCount);
    }
}
