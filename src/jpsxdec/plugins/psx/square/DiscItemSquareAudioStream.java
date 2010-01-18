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

package jpsxdec.plugins.psx.square;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.swing.JPanel;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.DiscItemStreaming;
import jpsxdec.plugins.DiscItemSerialization;
import jpsxdec.plugins.DiscItemSaver;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.plugins.ProgressListener;
import jpsxdec.plugins.xa.IDiscItemAudioStream;
import jpsxdec.plugins.xa.IDiscItemAudioSectorDecoder;
import jpsxdec.plugins.xa.PCM16bitAudioWriter;
import jpsxdec.plugins.xa.PCM16bitAudioWriterBuilder;
import jpsxdec.util.AudioOutputStream;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.NotThisTypeException;

public class DiscItemSquareAudioStream extends DiscItemStreaming implements IDiscItemAudioStream {

    public static final String TYPE_ID = "SquareAudio";

    private static final String SAMPLE_COUNT_LEFT_KEY = "Left samples";
    private final long _lngLeftSampleCount;
    
    private static final String SAMPLE_COUNT_RIGHT_KEY = "Right samples";
    private final long _lngRightSampleCount;
    
    private static final String SAMPLES_PER_SEC_KEY = "Samples/Sec";
    private final int _iSamplesPerSecond;

    private static final String SECTORS_PAST_END_KEY = "Sectors past end";
    private final int _iSectorsPastEnd;
    
    public DiscItemSquareAudioStream(int iStartSector, int iEndSector,
            long lngLeftSampleCount, long lngRightSampleCount, 
            int iSamplesPerSecond, int iSectorsPastEnd)
    {
        super(iStartSector, iEndSector);

        _lngLeftSampleCount = lngLeftSampleCount;
        _lngRightSampleCount = lngRightSampleCount;
        _iSamplesPerSecond = iSamplesPerSecond;
        _iSectorsPastEnd = iSectorsPastEnd;
    }
    
    public DiscItemSquareAudioStream(DiscItemSerialization fields) throws NotThisTypeException
    {
        super(fields);
        
        _iSamplesPerSecond = fields.getInt(SAMPLES_PER_SEC_KEY);
        _lngLeftSampleCount = fields.getLong(SAMPLE_COUNT_LEFT_KEY);
        _lngRightSampleCount = fields.getLong(SAMPLE_COUNT_RIGHT_KEY);
        _iSectorsPastEnd = fields.getInt(SECTORS_PAST_END_KEY);
    }
    
    @Override
    public DiscItemSerialization serialize() {
        DiscItemSerialization fields = super.superSerial(TYPE_ID);
        fields.addNumber(SAMPLES_PER_SEC_KEY, _iSamplesPerSecond);
        fields.addNumber(SAMPLE_COUNT_LEFT_KEY, _lngLeftSampleCount);
        fields.addNumber(SAMPLE_COUNT_RIGHT_KEY, _lngRightSampleCount);
        fields.addNumber(SECTORS_PAST_END_KEY, _iSectorsPastEnd);
        return fields;
    }
    
    public boolean isStereo() {
        return true;
    }

    public int getSectorsPastEnd() {
        return _iSectorsPastEnd;
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_AUDIO;
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int getDiscSpeed() {
        return 2;
    }

    public AudioFormat getAudioFormat(boolean blnBigEndian) {
        return new AudioFormat(_iSamplesPerSecond, 16, 2, true, blnBigEndian);
    }

    public IDiscItemAudioSectorDecoder makeDecoder(AudioOutputStream outStream, boolean blnBigEndian, double dblVolume) {
        return new SquareConverter(new SquareADPCMDecoder(blnBigEndian, dblVolume), outStream);
    }

    private class SquareConverter implements IDiscItemAudioSectorDecoder {

        private final SquareADPCMDecoder __decoder;
        private final AudioOutputStream __audioWriter;
        private ISquareAudioSector __leftAudioSector, __rightAudioSector;
        private byte[] __abTempBuffer;

        public SquareConverter(SquareADPCMDecoder decoder, AudioOutputStream outStream) {
            __decoder = decoder;
            __audioWriter = outStream;
            __abTempBuffer = new byte[100000]; // TODO: calculate this as needed
        }

        public void feedSector(IdentifiedSector sector) throws IOException {
            if (!(sector instanceof ISquareAudioSector))
                return;
            
            ISquareAudioSector audSector = (ISquareAudioSector) sector;
            if (audSector.getAudioChannel() == 0) {
                __leftAudioSector = audSector;
            } else if (audSector.getAudioChannel() == 1) {
                __rightAudioSector = audSector;

                if (__leftAudioSector.getAudioDataSize() != __rightAudioSector.getAudioDataSize() ||
                    __leftAudioSector.getSamplesPerSecond() != __rightAudioSector.getSamplesPerSecond())
                    throw new RuntimeException("Left/Right audio does not match.");

                int iSize = __decoder.decode(__leftAudioSector.getIdentifiedUserDataStream(),
                        __rightAudioSector.getIdentifiedUserDataStream(),
                        audSector.getAudioDataSize(), __abTempBuffer);
                __audioWriter.write(__decoder.getOutputFormat(__rightAudioSector.getSamplesPerSecond()),
                        __abTempBuffer, 0, iSize);
            } else {
                throw new RuntimeException("Invalid audio channel " + audSector.getAudioChannel());
            }
        }

        public double getVolume() {
            return __decoder.getVolume();
        }

        public void setVolume(double dblVolume) {
            __decoder.setVolume(dblVolume);
        }

        public AudioFormat getOutputFormat() {
            return __audioWriter.getFormat();
        }

        public void reset() {
            __decoder.resetContext();
        }

        public int getEndSector() {
            return DiscItemSquareAudioStream.this.getEndSector();
        }

        public int getStartSector() {
            return DiscItemSquareAudioStream.this.getStartSector();
        }

    }


    @Override
    public long calclateTime(int iSect) {
        if (iSect < getStartSector() || iSect > getEndSector())
            throw new IllegalArgumentException("Sector number is out of media item bounds.");
        return ( iSect - getStartSector() ) * 2 * 75;
    }

    @Override
    public DiscItemSaver getSaver() {
        return new SquareAudioSaver(this);
    }

    public int getSampleRate() {
        return _iSamplesPerSecond;
    }

    private static class SquareAudioSaver extends DiscItemSaver {

        private final PCM16bitAudioWriterBuilder _builder;
        private final DiscItemSquareAudioStream _audItem;

        public SquareAudioSaver(DiscItemSquareAudioStream audStream) {
            _audItem = audStream;
            _builder = new PCM16bitAudioWriterBuilder(
                    audStream.isStereo(), audStream.getSampleRate(),
                    audStream.getSuggestedBaseName());
        }

        @Override
        public String[] commandLineOptions(String[] asArgs, FeedbackStream infoStream) {
            return _builder.commandLineOptions(asArgs, infoStream);
        }

        @Override
        public void printHelp(FeedbackStream fbs) {
            _builder.printHelp(fbs);
        }

        @Override
        public JPanel getOptionPane() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void startSave(ProgressListener pl) throws IOException {
            PCM16bitAudioWriter audioWriter = _builder.getAudioWriter();
            int iSector = _audItem.getStartSector();
            IDiscItemAudioSectorDecoder decoder = _audItem.makeDecoder(audioWriter, audioWriter.getFormat().isBigEndian(), audioWriter.getVolume());
            try {
                final double SECTOR_LENGTH = _audItem.getEndSector() - _audItem.getStartSector();
                pl.progressStart("Writing " + audioWriter.getOutputFile());
                audioWriter.open();
                for (; iSector <= _audItem.getEndSector(); iSector++) {
                    CDSector cdSector = _audItem.getSourceCD().getSector(iSector);
                    IdentifiedSector identifiedSect = _audItem.identifySector(cdSector);
                    decoder.feedSector(identifiedSect);
                    pl.progressUpdate((iSector - _audItem.getStartSector()) / SECTOR_LENGTH);
                }
                pl.progressEnd();
            } finally {
                try {
                    audioWriter.close();
                } catch (Throwable ex) {
                    pl.error(ex);
                }
            }
        }

    }

}
