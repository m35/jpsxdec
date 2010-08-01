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

package jpsxdec.modules.psx.square;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.swing.JPanel;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.DiscItemSerialization;
import jpsxdec.modules.DiscItemSaver;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.ProgressListener;
import jpsxdec.modules.xa.DiscItemAudioStream;
import jpsxdec.modules.xa.IAudioReceiver;
import jpsxdec.modules.xa.IAudioSectorDecoder;
import jpsxdec.modules.xa.SectorAudioWriter;
import jpsxdec.modules.xa.SectorAudioWriterBuilder;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.NotThisTypeException;

public class DiscItemSquareAudioStream extends DiscItemAudioStream {

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
    public String getTypeId() {
        return TYPE_ID;
    }

    public int getDiscSpeed() {
        return 2;
    }

    @Override
    public int getPresentationStartSector() {
        return getStartSector() + 1;
    }

    public AudioFormat getAudioFormat(boolean blnBigEndian) {
        return new AudioFormat(_iSamplesPerSecond, 16, 2, true, blnBigEndian);
    }

    public IAudioSectorDecoder makeDecoder(boolean blnBigEndian, double dblVolume) {
        return new SquareConverter(new SquareADPCMDecoder(blnBigEndian, dblVolume));
    }

    private class SquareConverter implements IAudioSectorDecoder {

        private final SquareADPCMDecoder __decoder;
        private IAudioReceiver __audioWriter;
        private ISquareAudioSector __leftAudioSector, __rightAudioSector;
        private byte[] __abTempBuffer;
        private AudioFormat __format;

        public SquareConverter(SquareADPCMDecoder decoder) {
            __decoder = decoder;
            __abTempBuffer = new byte[2*2*4000]; // TODO: calculate this as needed
        }

        public void open(IAudioReceiver audioOut) {
            __audioWriter = audioOut;
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
                if (__format == null)
                    __format = __decoder.getOutputFormat(__rightAudioSector.getSamplesPerSecond());
                __audioWriter.write(__format, __abTempBuffer, 0, iSize, __rightAudioSector.getSectorNumber());
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
            return __decoder.getOutputFormat(_iSamplesPerSecond);
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

        public int getPresentationStartSector() {
            return DiscItemSquareAudioStream.this.getPresentationStartSector();
        }
    }


    @Override
    public DiscItemSaver getSaver() {
        return new SquareAudioSaver(this);
    }

    public int getSampleRate() {
        return _iSamplesPerSecond;
    }

    private static class SquareAudioSaver extends DiscItemSaver {

        private final SectorAudioWriterBuilder _builder;
        private final DiscItemSquareAudioStream _audItem;

        public SquareAudioSaver(DiscItemSquareAudioStream audStream) {
            _audItem = audStream;
            _builder = new SectorAudioWriterBuilder(audStream);
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
            SectorAudioWriter audioWriter = _builder.getAudioWriter();
            int iSector = _audItem.getStartSector();
            try {
                final double SECTOR_LENGTH = _audItem.getEndSector() - _audItem.getStartSector();
                pl.progressStart("Writing " + audioWriter.getOutputFile());
                for (; iSector <= _audItem.getEndSector(); iSector++) {
                    CdSector cdSector = _audItem.getSourceCD().getSector(iSector);
                    IdentifiedSector identifiedSect = _audItem.identifySector(cdSector);
                    audioWriter.feedSector(identifiedSect);
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
