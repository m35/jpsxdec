/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2025-2026  Michael Sabin
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

package jpsxdec.util.mkvwriter;


import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.formats.Signed16bitLittleEndianLinearPcmAudioInputStream;
import jpsxdec.formats.YCbCrImageTest;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import org.junit.Assert;
import org.junit.Test;
import testutil.Util;

public class MkvWriter_Tests {

    private static final Logger LOG = Logger.getLogger(MkvWriter_Tests.class.getName());

    /*
Everything to test

/All formats
    /With and without _audioReader
        /Different _audioReader sample rates
        /no _audioReader/stereo/mono
            /Different frame rates
                /constant/variable frame rate

*/

    private static final boolean GENERATE_EXPECTED = false;

    private static final String AUDIO_TEST_FILE = "MkvWriter_Tests.wav";
    private static final int WIDTH = 128;
    private static final int HEIGHT = 128;
    private static final Fraction[] VARIABLE_FRAME_DURATIONS = {
        new Fraction(1, 15),
        new Fraction(1, 10),
        new Fraction(1, 5),
        new Fraction(1, 12),
        new Fraction(1, 9),
        new Fraction(1, 6),
    };
    private static final int AUDIO_SAMPLE_RATE = 44100 / 2;

    private static final Fraction[] FPSS = {
            null,
            new Fraction(15, 1),
    };
    private static final int FRAMES_TO_GENERATE = 60;

    private static final IAVWriteStrategy[] WRITE_STRATEGIES = {
            new WriteOnlyVideo(),
            new WriteNothing(),
            new WriteOnlyAudio(),
            new WriteSameTimes(),
            new WriteOneFrame(),
            new WriteTimestampsTooClose(),
            new WriteVideoThenAudio(),
            new WriteAudioThenVideo(),
            new WriteWhateverTimes(),
    };

    //-------------------------------------------------------------------------
    private static class MkvAndFile {
        final File _file;
        final IMkvWriter _mkv;

        public MkvAndFile(File file, IMkvWriter mkv) {
            _file = file;
            _mkv = mkv;
        }
    }

    private void deleteTestFiles(MkvFactory factory) throws Exception {
        TreeSet<File> filesToDelete = new TreeSet<File>();
        for (MkvAndFile mkvAndFile : factory._filesCreated) {
            mkvAndFile._mkv.close();
            filesToDelete.add(mkvAndFile._file);
        }

        if (!GENERATE_EXPECTED) {
            ArrayList<File> deleteFailed = new ArrayList<File>();
            for (File file : filesToDelete) {
                if (!file.delete())
                    deleteFailed.add(file);
            }
            Assert.assertEquals(deleteFailed.toString(), 0, deleteFailed.size());
        }
    }
    //-------------------------------------------------------------------------

    @Test
    public void test1() throws Exception {

        for (Fraction fps : FPSS) {

            for (int iAudioChannels = 0; iAudioChannels < 3; iAudioChannels++) {
                for (IAVWriteStrategy writeStrategy : WRITE_STRATEGIES) {
                    MkvFactory[] FACTORIES = { new Png(), new Yuv(), new Mjpg(), };
                    for (MkvFactory factory : FACTORIES) {

                        MkvTestCase testCase = new MkvTestCase(factory, fps, iAudioChannels, writeStrategy, AUDIO_SAMPLE_RATE);
                        System.out.println();
                        System.out.println("Testing " + testCase);
                        doTest(testCase);
                        deleteTestFiles(factory);
                    }
                }
            }
        }
    }

    private static class MkvTestCase {
        final MkvFactory _factory;
        final Fraction _fps;
        final int _iAudioChannels;
        final IAVWriteStrategy _writeStrategy;
        final int _iAudioSampleRate;

        public MkvTestCase(MkvFactory factory, Fraction fps, int iAudioChannels, IAVWriteStrategy writeStrategy, int iAudioSampleRate) {
            _factory = factory;
            _fps = fps;
            _iAudioChannels = iAudioChannels;
            _writeStrategy = writeStrategy;
            _iAudioSampleRate = iAudioSampleRate;
        }

        @Override
        public String toString() {
            return String.format("%s %s %s %s",
                    _factory.getClass().getSimpleName(),
                    _fps, _iAudioChannels,
                    _writeStrategy.getClass().getSimpleName());
        }

        @Nonnull
        private File mkvFileName() {
            String sFormat = _factory.getClass().getSimpleName();

            String sFps;
            if (_fps == null) {
                sFps = "_vfr";
            } else {
                sFps = "_" + _fps.getNumerator() + "." + _fps.getDenominator();
            }

            String sStrategy = "_" + _writeStrategy.getClass().getSimpleName();

            String sChannels = "";
            String sSampleRate = "";
            if (_iAudioChannels > 0) {
                sSampleRate = "_" + _iAudioSampleRate;
                sChannels = "_" + (_iAudioChannels == 1 ? "Mono" : "Stereo");
            }

            String sMkvFile = String.format("%s%s%s%s%s.mkv", sFormat, sFps, sChannels, sSampleRate, sStrategy);
            return new File(sMkvFile);
        }

        public void makeBadMkvs() throws IOException {
            // test invalid dimensions
            int[][] aaiDims = {
                {0, 0},
                {0, 1},
                {1, 0},
                {-1, -1},
                {-1, 1},
                {1, -1},
            };
            File mkvFile = mkvFileName();
            for (int[] aiDims : aaiDims) {
                try {
                    _factory.makeMkv(mkvFile, aiDims[0], aiDims[1], _fps);
                    Assert.fail("Expected " + IllegalArgumentException.class + " for " + aiDims[0] + " x " + aiDims[1]);
                } catch (IllegalArgumentException ex) {}

                try {
                    _factory.makeMkvWithAudio(mkvFile, aiDims[0], aiDims[1], _fps, _iAudioChannels, _iAudioSampleRate);
                    Assert.fail("Expected " + IllegalArgumentException.class + " for " + aiDims[0] + " x " + aiDims[1]);
                } catch (IllegalArgumentException ex) {}
            }

            // test 0 sample rate
            try {
                _factory.makeMkvWithAudio(mkvFile, WIDTH, HEIGHT, _fps, _iAudioChannels, 0);
                Assert.fail("Expected " + IllegalArgumentException.class);
            } catch (IllegalArgumentException ex) {}

            // test negative sample rate
            try {
                _factory.makeMkvWithAudio(mkvFile, WIDTH, HEIGHT, _fps, _iAudioChannels, -1);
                Assert.fail("Expected " + IllegalArgumentException.class);
            } catch (IllegalArgumentException ex) {}

            // test massive audio sample rate
            makeMkvWithInsaneAudio().close();

        }

        public IMkvWriter makeMkvWithAudio() throws IOException {
            return _factory.makeMkvWithAudio(mkvFileName(), WIDTH, HEIGHT, _fps, _iAudioChannels, _iAudioSampleRate);
        }

        public IMkvWriter makeMkvWithInsaneAudio() throws IOException {
            // TODO test this
            long lngRate = MkvTimestampScale.getNanosPerTick() * 4;
            Assert.assertTrue(""+lngRate,lngRate <= Integer.MAX_VALUE);
            return _factory.makeMkvWithAudio(mkvFileName(), WIDTH, HEIGHT, _fps, _iAudioChannels, (int)lngRate);
        }

        public IMkvWriter makeMkv() throws IOException {
            return _factory.makeMkv(mkvFileName(), WIDTH, HEIGHT, _fps);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /*

/vfr random frame times
/Need stereo _audioReader clip
/Different sound in each channel

Everything to test

Audio and video packets landing at same time
    Multiple frames, then multiple _audioReader, and visa-versa
        Kinda already handled by the string test
                inconsistent frame times when cfr
                    frames and/or _audioReader out of order

gap in _audioReader
write silence
yuv odd width/height
video with _audioReader but no _audioReader written?
!!empty mkv file
!!A single frame video fails to open/play in some programs

 */


     private static void doTest(MkvTestCase testCase) throws Exception {

        // .........................................................

         String sExistingMeta = null;
         IMkvWriter mkvWriter = null;
         AudioReader audioReader = null;
         try {
             sExistingMeta = System.setProperty("meta", "mkv test");

             testCase.makeBadMkvs();

             if (testCase._iAudioChannels > 0) {
                 audioReader = new AudioReader(testCase._iAudioSampleRate, testCase._iAudioChannels);
                 mkvWriter = testCase.makeMkvWithAudio();
             } else { // vfr
                 mkvWriter = testCase.makeMkv();
             }

             testCase._writeStrategy.write(audioReader, new ImageGetter(), new FrameTimeIterator(testCase._fps), mkvWriter);

         } finally {
             if (sExistingMeta == null) {
                 System.clearProperty("meta");
             } else {
                 System.setProperty("meta", sExistingMeta);
             }
             IO.closeSilently(mkvWriter, LOG);

             IO.closeSilently(audioReader, LOG);
         }

         if (true || GENERATE_EXPECTED) {
             // Data is generated, copy to test dir
             System.out.println("Copy test file " + testCase.mkvFileName() + " to test directory");
             //Assert.fail("Copy test file " + sMkvFile);
             // Run ffmpeg to extract audio and frames, compare results with inputs
             // run video through mkv validator
         } else {
             byte[] abExpected = Util.readResource(MkvWriter_Tests.class, testCase.mkvFileName().getName());
             byte[] abActual = IO.readFile(testCase.mkvFileName());

             Assert.assertArrayEquals(abExpected, abActual);

         }

    }

    private interface IAVWriteStrategy {
        void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception;
    }

    private static class WriteNothing implements IAVWriteStrategy {
        @Override
        public void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception {
            Fraction lastTime = null;
            Fraction time = frameTimeIter.next()[0];

            for (int i = 0; i < FRAMES_TO_GENERATE; i++) {
                BufferedImage image = imageGetter.nextImage();

                testErrors(audioReader != null, frameTimeIter.isCfr(), mkvWriter, image, lastTime, time, null);

            }
        }
    }

    private static class WriteOnlyVideo implements IAVWriteStrategy {
        @Override
        public void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception {

            Fraction lastTime = null;

            for (int i = 0; i < FRAMES_TO_GENERATE; i++) {
                BufferedImage image = imageGetter.nextImage();
                Fraction time = frameTimeIter.next()[0];

                testErrors(audioReader != null, frameTimeIter.isCfr(), mkvWriter, image, lastTime, time, null);

                // Properly write a frame
                mkvWriter.writeImage(image, time);

                lastTime = time;
            }
        }
    }

    private static class WriteOnlyAudio implements IAVWriteStrategy {
        @Override
        public void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception {

            Fraction lastTime = null;
            Fraction time = frameTimeIter.next()[0];
            Fraction lastAudioTime = null;

            for (int i = 0; i < FRAMES_TO_GENERATE; i++) {
                BufferedImage image = imageGetter.nextImage();

                testErrors(audioReader != null, frameTimeIter.isCfr(), mkvWriter, image, lastTime, time, lastAudioTime);

                if (audioReader != null) {
                    lastAudioTime = mkvWriter.getAudioTime();
                    mkvWriter.writeAudio(audioReader.readSeconds(new Fraction(1, 25)));
                }

            }
        }
    }

    // worms video all 3 formats

    private static class WriteWhateverTimes implements IAVWriteStrategy {
        // frame frame, audio
        // audio, audio, frame

        @Override
        public void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception {
            // TODO
        }
    }

    private static class WriteTimestampsTooClose implements IAVWriteStrategy {
        // audio or frame time CLOSE to zero -- too small to differentiate with the default tick size
        @Override
        public void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception {
            // TODO test video
            // TODO audio may not actually be testable, the audio Hz would have to be HUGE

            BufferedImage image = imageGetter.nextImage();

            mkvWriter.writeImage(image, Fraction.ZERO);

        }
    }

    private static class WriteVideoThenAudio implements IAVWriteStrategy {
        @Override
        public void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception {
            new WriteOnlyVideo().write(audioReader, imageGetter, frameTimeIter, mkvWriter);
            new WriteOnlyAudio().write(audioReader, imageGetter, frameTimeIter, mkvWriter);
        }
    }

    private static class WriteAudioThenVideo implements IAVWriteStrategy {
        @Override
        public void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception {
            new WriteOnlyAudio().write(audioReader, imageGetter, frameTimeIter, mkvWriter);
            new WriteOnlyVideo().write(audioReader, imageGetter, frameTimeIter, mkvWriter);
        }
    }

    private static class WriteOneFrame implements IAVWriteStrategy {
        @Override
        public void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception {

            BufferedImage image = imageGetter.nextImage();
            Fraction time = frameTimeIter.next()[0];

            testErrors(audioReader != null, frameTimeIter.isCfr(), mkvWriter, image, null, time, null);

            // Properly write a frame
            mkvWriter.writeImage(image, time);
        }
    }


    private static class WriteSameTimes implements IAVWriteStrategy {
        @Override
        public void write(AudioReader audioReader, ImageGetter imageGetter, FrameTimeIterator frameTimeIter, IMkvWriter mkvWriter) throws Exception {

            Fraction lastTime = null;
            Fraction lastAudioTime = null;

            for (int i = 0; i < FRAMES_TO_GENERATE; i++) {
                BufferedImage image = imageGetter.nextImage();
                Fraction[] times = frameTimeIter.next();


                Fraction frameTime;
                if (audioReader != null) {

                    frameTime = mkvWriter.getAudioTime();
                    testErrors(audioReader != null, frameTimeIter.isCfr(), mkvWriter, image, lastTime, frameTime, lastAudioTime);

                    Fraction roundedDuration = audioReader.roundSeconds(times[1]);
                    //System.out.println("Writing " + times[1] + " -> " + roundedDuration + " seconds at " + mkvWriter.getAudioTime());
                    byte[] abAudio = audioReader.readSeconds(roundedDuration);
                    lastAudioTime = mkvWriter.getAudioTime();
                    mkvWriter.writeAudio(abAudio);
                } else {
                    testErrors(audioReader != null, frameTimeIter.isCfr(), mkvWriter, image, lastTime, times[0], lastAudioTime);
                    frameTime = times[0];
                }

                // Properly write a frame
                mkvWriter.writeImage(image, frameTime);

                lastTime = frameTime;
            }
        }
    }

    private static void testErrors(boolean hasAudio, boolean isCfr, IMkvWriter mkvWriter, BufferedImage image, Fraction lastTime, Fraction time, Fraction lastAudioTime) throws IOException {

        try { // Negative frame time
            mkvWriter.writeImage(image, new Fraction(-1));
            Assert.fail("Expected " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            // expected
        }

        if (lastTime != null) {

            try { // fractionally tiny difference in frame time will result in equal tick time
                mkvWriter.writeImage(image, lastTime.add(new Fraction(1, 10000000000L)));
                Assert.fail("Expected " + IllegalArgumentException.class);
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }

            try { // Write frame with the same timestamp
                mkvWriter.writeImage(image, lastTime);
                Assert.fail("Expected " + IllegalArgumentException.class);
            } catch (IllegalArgumentException ex) {
                // expected
            }

            try { // Write frame a little before the last frame
                mkvWriter.writeImage(image, lastTime.subtract(new Fraction(1, 100)));
                Assert.fail("Expected " + IllegalArgumentException.class);
            } catch (IllegalArgumentException ex) {
                // expected
            }
        }

        if (isCfr) {
            try { // Write frame off the constant frame rate
                mkvWriter.writeImage(image, time.add(new Fraction(1, 100)));
                Assert.fail("Expected " + IllegalArgumentException.class);
            } catch (IllegalArgumentException ex) {
                // expected
            }
        }

        if (!hasAudio) {
            try { // Write audio in video-only mkv
                mkvWriter.writeAudio(new byte[8], new Fraction(1));
                Assert.fail("Expected " + UnsupportedOperationException.class);
            } catch (UnsupportedOperationException ex) {
                // expected
            }
        } else {
            try { // Null audio
                mkvWriter.writeAudio(new byte[8], null);
                Assert.fail("Expected " + NullPointerException.class);
            } catch (NullPointerException ex) {
                System.out.println(ex.getMessage());
            }

            // Write no audio
            mkvWriter.writeAudio(new byte[0]);

            if (lastAudioTime == null) {
                try { // Write audio with the wrong timestamp at the start (negative)
                    mkvWriter.writeAudio(new byte[16], new Fraction(-1)); // should be at 0 seconds
                    Assert.fail("Expected " + IllegalArgumentException.class);
                } catch (IllegalArgumentException ex) {
                    // expected
                }

                try { // Write audio with the wrong timestamp at the start (positive)
                    mkvWriter.writeAudio(new byte[16], new Fraction(1, 3)); // should be at 0 seconds
                    Assert.fail("Expected " + IllegalArgumentException.class);
                } catch (IllegalArgumentException ex) {
                    // expected
                }
            }
        }
    }


    private static class FrameTimeIterator {
        private int i = 0;
        private final Fraction _secPerFrame;
        private Fraction _currentTime = Fraction.ZERO;

        private FrameTimeIterator(Fraction cfr) {
            if (cfr == null) {
                _secPerFrame = null;
            } else {
                _secPerFrame = cfr.reciprocal();
            }
        }

        public boolean isCfr() {
            return _secPerFrame != null;
        }

        public Fraction[] next() {
            Fraction nextFrameDuration;
            if (_secPerFrame == null) {
                if (i >= VARIABLE_FRAME_DURATIONS.length)
                    i = 0;
                nextFrameDuration = VARIABLE_FRAME_DURATIONS[i++];
            } else {
                nextFrameDuration = _secPerFrame;
            }
            Fraction thisTime = _currentTime;
            _currentTime = _currentTime.add(nextFrameDuration);
            return new Fraction[] {thisTime, nextFrameDuration};
        }
    }

    private abstract static class MkvFactory {
        final ArrayList<MkvAndFile> _filesCreated = new ArrayList<MkvAndFile>();
        abstract protected IMkvWriter makeMkv(File file, int iWidth, int iHeight, Fraction fps) throws IOException;
        abstract protected IMkvWriter makeMkvWithAudio(File file, int iWidth, int iHeight, Fraction fps, int iAudioChannels, int iSampleRate) throws IOException;
    }

    private class Png extends MkvFactory {
        @Override
        protected IMkvWriter makeMkv(File file, int iWidth, int iHeight, Fraction fps) throws IOException {
            IMkvWriter m = new MkvPngWriter(file, iWidth, iHeight, fps);
            _filesCreated.add(new MkvAndFile(file, m));
            return m;
        }

        @Override
        protected IMkvWriter makeMkvWithAudio(File file, int iWidth, int iHeight, Fraction fps, int iAudioChannels, int iSampleRate) throws IOException {
            IMkvWriter m = new MkvPngWriter(file, iWidth, iHeight, fps, iAudioChannels == 2, iSampleRate);
            _filesCreated.add(new MkvAndFile(file, m));
            return m;
        }
    }
    private class Yuv extends MkvFactory {
        @Override
        protected IMkvWriter makeMkv(File file, int iWidth, int iHeight, Fraction fps) throws IOException {
            IMkvWriter m = new MkvJYuvWriter(file, iWidth, iHeight, fps);
            _filesCreated.add(new MkvAndFile(file, m));
            return m;
        }

        @Override
        protected IMkvWriter makeMkvWithAudio(File file, int iWidth, int iHeight, Fraction fps, int iAudioChannels, int iSampleRate) throws IOException {
            IMkvWriter m = new MkvJYuvWriter(file, iWidth, iHeight, fps, iAudioChannels == 2, iSampleRate);
            _filesCreated.add(new MkvAndFile(file, m));
            return m;
        }
    }
    private class Mjpg extends MkvFactory {
        @Override
        protected IMkvWriter makeMkv(File file, int iWidth, int iHeight, Fraction fps) throws IOException {
            IMkvWriter m = new MkvMjpegWriter(file, iWidth, iHeight, fps);
            _filesCreated.add(new MkvAndFile(file, m));
            return m;
        }

        @Override
        protected IMkvWriter makeMkvWithAudio(File file, int iWidth, int iHeight, Fraction fps, int iAudioChannels, int iSampleRate) throws IOException {
            IMkvWriter m =  new MkvMjpegWriter(file, iWidth, iHeight, fps, iAudioChannels == 2, iSampleRate);
            _filesCreated.add(new MkvAndFile(file, m));
            return m;
        }
    }


    private static class ImageGetter {
        private int i = 0;
        BufferedImage testImage = YCbCrImageTest.readTestImage();

        public BufferedImage nextImage() {
            BufferedImage sub = testImage.getSubimage(i, i, WIDTH, HEIGHT);
            i++;
            return sub;
        }
    }

    private static class AudioReader implements Closeable {
        private final InputStream stream;
        private final int iSampleRate;
        private final int iChannels;

        public AudioReader(int iSampleRate, int iChannels) throws Exception {
            this.iSampleRate = iSampleRate;
            this.iChannels = iChannels;
            if (iChannels == 2) {
                stream = AudioSystem.getAudioInputStream(new BufferedInputStream(MkvWriter_Tests.class.getResourceAsStream(AUDIO_TEST_FILE)));
            } else {
                short[] asiLeftChannel;
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(MkvWriter_Tests.class.getResourceAsStream(AUDIO_TEST_FILE)))) {

                    long lngFrameLength = ais.getFrameLength();
                    Assert.assertTrue(lngFrameLength <= Integer.MAX_VALUE);
                    Signed16bitLittleEndianLinearPcmAudioInputStream ais16 = new Signed16bitLittleEndianLinearPcmAudioInputStream(ais);
                    asiLeftChannel = ais16.readSampleFrames((int) lngFrameLength)[0];
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream(asiLeftChannel.length * 2);
                for (short siSample : asiLeftChannel) {
                    IO.writeInt16LE(baos, siSample);
                }
                stream = new ByteArrayInputStream(baos.toByteArray());
            }

        }

        public byte[] readSampleFrames(int iCount) throws Exception {
            return IO.readByteArray(stream, iCount * iChannels * 2);
        }

        public byte[] readSeconds(Fraction seconds) throws Exception {
            Fraction count = secondsToSamples(seconds);
            Assert.assertTrue(seconds + " -> " + count + " is not whole number", count.isWholeNumber());
            Assert.assertTrue(count.getNumerator() <= Integer.MAX_VALUE);
            return readSampleFrames((int) count.getNumerator());
        }

        public Fraction secondsToSamples(Fraction seconds) {
            // seconds * samples/seconds = samples
            Fraction count = seconds.multiply(iSampleRate);
            return count;
        }

        public Fraction samplesToSeconds(int iSamples) {
            return new Fraction(iSamples, iSampleRate);
        }

        public Fraction roundSeconds(Fraction seconds) {
            Fraction samples = secondsToSamples(seconds);
            long lngRoundedSamples = samples.asLong();
            // samples / samples/seconds = seconds
            return new Fraction(lngRoundedSamples, iSampleRate);
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

}
