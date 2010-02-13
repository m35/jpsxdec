package jpsxdec.plugins.psx.str;

import java.util.logging.Logger;
import jpsxdec.util.Fraction;

public class VideoSync {

    private static final Logger log = Logger.getLogger(VideoSync.class.getName());

    private final int _iSectorsPerSecond;
    private final Fraction _sectorsPerFrame;
    private final Fraction _secondsPerFrame;
    private final int _iFirstPresentationSector;
    
    public VideoSync(int iFirstPresentationSector,
                     int iSectorsPerSecond,
                     Fraction sectorsPerFrame)
    {
        _iFirstPresentationSector = iFirstPresentationSector;
        _iSectorsPerSecond = iSectorsPerSecond;
        _sectorsPerFrame = sectorsPerFrame;

        _secondsPerFrame = _sectorsPerFrame.divide(_iSectorsPerSecond);
    }

    public Fraction getSecondsPerFrame() {
        return _secondsPerFrame;
    }

    public Fraction getSectorsPerFrame() {
        return _sectorsPerFrame;
    }

    public int getSectorsPerSecond() {
        return _iSectorsPerSecond;
    }

    private static Fraction Point5 = new Fraction(1, 2);
    private static Fraction NegPoint5 = Point5.negative();

    public int calculateFramesToCatchup(int iSector, long lngFramesWritten) {

        Fraction presentationTime = new Fraction(iSector - _iFirstPresentationSector, _iSectorsPerSecond);
        Fraction movieTime = _secondsPerFrame.multiply(lngFramesWritten);
        Fraction timeDiff = presentationTime.subtract(movieTime);
        Fraction framesDiff = timeDiff.divide(_secondsPerFrame);

        int iFrameCatchupNeeded = 0;

        if (framesDiff.compareTo(NegPoint5) > 0) { // presentation time is equal, or ahead of movie time
            iFrameCatchupNeeded = (int)Math.round(framesDiff.asDouble());
        } else { // movie time is ahead of disc time
            log.warning(String.format("Frame is written %1.3f seconds ahead.", -timeDiff.asDouble()));
            // return the negative number
            iFrameCatchupNeeded = (int)Math.round(framesDiff.asDouble());
        }

        return iFrameCatchupNeeded;
    }

    public int getInitialVideo() {
        return 0;
    }

    public long getFpsNum() {
        return getSecondsPerFrame().getDenominator();
    }

    public long getFpsDenom() {
        return getSecondsPerFrame().getNumerator();
    }
}
