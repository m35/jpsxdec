package jpsxdec.plugins.xa;

import jpsxdec.util.Fraction;

public class AudioSync {

    private final Fraction _sectorsPerSecond;
    private final Fraction _samplesPerSecond;
    private final Fraction _samplesPerSector;

    private int _iPreviousAudioSector;
    private long _lngSamplesFromPreviousAudioSector;

    public AudioSync(int iMovieStartSector,
                     int iSectorsPerSecond,
                     float fltSamplesPerSecond)
    {

        _sectorsPerSecond = new Fraction(iSectorsPerSecond, 1);
        _samplesPerSecond = new Fraction(Math.round(fltSamplesPerSecond * 1000), 1000);
        // samples/sector = samples/second / sectors/second
        _samplesPerSector = _samplesPerSecond.divide(_sectorsPerSecond);

        _iPreviousAudioSector = iMovieStartSector;
    }

    public Fraction getSamplesPerSecond() {
        return _samplesPerSecond;
    }

    public Fraction getSamplesPerSector() {
        return _samplesPerSector;
    }

    public Fraction getSectorsPerSecond() {
        return _sectorsPerSecond;
    }

    public long calculateSamples(int iSectorSpan) {
        return Math.round(_samplesPerSector.multiply(iSectorSpan).asDouble());
    }

    public long calculateNeededSilence(int iSector, long lngSampleCount) {

        long lngSamplesNeededForSectorSpan = calculateSamples(iSector - _iPreviousAudioSector);
        long lngSampleDifference = lngSamplesNeededForSectorSpan - _lngSamplesFromPreviousAudioSector;

        _iPreviousAudioSector = iSector;
        _lngSamplesFromPreviousAudioSector = lngSampleCount;

        return lngSampleDifference;
    }

}
