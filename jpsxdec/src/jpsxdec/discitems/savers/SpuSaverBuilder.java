/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2017  Michael Sabin
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

package jpsxdec.discitems.savers;

import argparser.StringHolder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.audio.SpuAdpcmDecoder;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.DiscItemSpu;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.formats.JavaAudioFormat;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.LocalizedFileNotFoundException;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.util.ArgParser;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.ILocalizedLogger;
import jpsxdec.util.IO;
import jpsxdec.util.LoggedFailure;
import jpsxdec.util.ProgressLogger;
import jpsxdec.util.TabularFeedback;
import jpsxdec.util.TabularFeedback.Cell;
import jpsxdec.util.TaskCanceledException;


public class SpuSaverBuilder extends DiscItemSaverBuilder {

    private static final Logger LOG = Logger.getLogger(SpuSaverBuilder.class.getName());

    /** Common sample rates of SPU clips (stolen from PSound). */
    private static final int[] DEFAULT_SAMPLE_RATES = {
        8000,
        11025,
        16000,
        18900,
        22050,
        24000,
        32000,
        37800,
        44100,
        48000,
    };

    /** Represents an output format that SPU audio can be saved to. */
    public static class SpuSaverFormat {

        private final @CheckForNull JavaAudioFormat _jFmt;
        private final @Nonnull String _sExtension;
        private final @Nonnull ILocalizedMessage _cmdId;
        private final @Nonnull ILocalizedMessage _guiDotExtension;

        private SpuSaverFormat(@Nonnull String sExtension) {
            _jFmt = null;
            _sExtension = sExtension;
            _cmdId = new UnlocalizedMessage(_sExtension);
            _guiDotExtension = new UnlocalizedMessage("." + _sExtension);
        }
        
        private SpuSaverFormat(@Nonnull JavaAudioFormat jFmt) {
            _jFmt = jFmt;
            _sExtension = null;
            _cmdId = null;
            _guiDotExtension = null;
        }

        public JavaAudioFormat getJavaType() {
            if (_jFmt == null)
                throw new UnsupportedOperationException();
            return _jFmt;
        }

        public @Nonnull String getExtension() {
            if (_jFmt != null)
                return _jFmt.getExtension();
            else
                return _sExtension;
        }

        public String toString() {
            if (_jFmt != null)
                return _jFmt.toString();
            else
                return _guiDotExtension.toString();
        }

        public @Nonnull ILocalizedMessage getCmdId() {
            if (_jFmt != null)
                return _jFmt.getCmdId();
            else
                return _cmdId;
        }

    }

    /** Raw SPU data. */
    private static final SpuSaverFormat SPU = new SpuSaverFormat("spu");
    /** SPU data wrapped in VAG file. */
    private static final SpuSaverFormat VAG = new SpuSaverFormat("vag");
    private static final ArrayList<SpuSaverFormat> AUDIO_FORMATS;
    static {
        // add normal Java audio formats
        JavaAudioFormat[] availableFormats = JavaAudioFormat.getAudioFormats();
        AUDIO_FORMATS = new ArrayList<SpuSaverFormat>(availableFormats.length + 2);
        for (JavaAudioFormat fmt : availableFormats) {
            AUDIO_FORMATS.add(new SpuSaverFormat(fmt));
        }
        // add special handling formats
        AUDIO_FORMATS.add(SPU);
        //AUDIO_FORMATS.add(VAG); // TODO: finish VAG support
    }

    private static SpuSaverFormat fromCmdLine(String s) {
        for (SpuSaverFormat fmt : AUDIO_FORMATS) {
            if (fmt.getCmdId().equalsIgnoreCase(s))
                return fmt;
        }
        return null;
    }

    // =========================================================================

    @Nonnull
    private final DiscItemSpu _spuItem;

    public SpuSaverBuilder(@Nonnull DiscItemSpu spuItem) {
        _spuItem = spuItem;
        resetToDefaults();
    }

    public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
        return SpuSaverBuilderGui.make(this);
    }

    public void resetToDefaults() {
        setContainerForamt(AUDIO_FORMATS.get(0));
        setVolume(1.0);
    }

    @Override
    public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder other) {
        if (other instanceof SpuSaverBuilder) {
            SpuSaverBuilder o = (SpuSaverBuilder) other;
            o.setContainerForamt(getContainerFormat());
            o.setVolume(getVolume());
            o.setSampleRate(getSampleRate());
            return true;
        }
        return false;
    }

    // ....................................................

    public int getSampleRate() {
        return _spuItem.getSampleRate();
    }
    public void setSampleRate(int iSampleRate) {
        _spuItem.setSampleRate(iSampleRate);
        firePossibleChange();
    }
    public int getSampleRate_listSize() {
        return DEFAULT_SAMPLE_RATES.length;
    }
    public int getSampleRate_listItem(int i) {
        return DEFAULT_SAMPLE_RATES[i];
    }

    // ....................................................
    
    @Nonnull
    private SpuSaverFormat _containerFormat = AUDIO_FORMATS.get(0);
    public void setContainerForamt(@Nonnull SpuSaverFormat val) {
        _containerFormat = val;
        firePossibleChange();
    }
    public @Nonnull SpuSaverFormat getContainerFormat() {
        return _containerFormat;
    }

    public int getContainerFormat_listSize() {
        return AUDIO_FORMATS.size();
    }
    public @Nonnull SpuSaverFormat getContainerFormat_listItem(int i) {
        return AUDIO_FORMATS.get(i);
    }

    // ....................................................
    
    public @Nonnull String getExtension() {
        return getContainerFormat().getExtension();
    }

    // ....................................................

    public @Nonnull String getFileBaseName() {
        return _spuItem.getSuggestedBaseName().getPath();
    }

    // ....................................................

    private double _dblVolume = 1.0;
    public void setVolume(double val) {
        _dblVolume = val;
        firePossibleChange();
    }
    public double getVolume() {
        if (getVolume_enabled())
            return _dblVolume;
        else
            return 1.0;
    }
    public boolean getVolume_enabled() {
        return getContainerFormat() != SPU && getContainerFormat() != VAG;
    }

    // ....................................................

    public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs)
    {
        if (!ap.hasRemaining())
            return;

        StringHolder vol = ap.addStringOption("-vol");
        StringHolder audfmt = ap.addStringOption("-audfmt","-af");
        StringHolder sampleRate = ap.addStringOption("-samplerate","-sr");
        ap.match();

        if (vol.value != null) {
            try {
                int iVol = Integer.parseInt(vol.value);
                if (iVol < 0 || iVol > 100)
                    throw new NumberFormatException();
                setVolume(iVol / 100.0);
            } catch (NumberFormatException ex) {
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VOLUME(vol.value));
            }
        }

        if (audfmt.value != null) {
            SpuSaverFormat fmt = fromCmdLine(audfmt.value);
            if (fmt != null) {
                setContainerForamt(fmt);
            } else {
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_FORMAT(audfmt.value));
            }
        }

        if (sampleRate.value != null) {
            // TODO
        }
    }

    @Override
    public void printHelp(@Nonnull FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();

        tfb.setRowSpacing(1);

        tfb.addCell(I.CMD_AUDIO_AF());
        Cell c = new Cell(I.CMD_AUDIO_AF_HELP(AUDIO_FORMATS.get(0).getExtension()));
        for (SpuSaverFormat saveFormat : AUDIO_FORMATS) {
            c.addLine(saveFormat.getCmdId(), 2);
        }
        tfb.addCell(c);
        tfb.newRow();

        tfb.addCell(I.CMD_AUDIO_VOL()).addCell(I.CMD_AUDIO_VOL_HELP(100));

        // TODO add sample rate

        tfb.write(fbs.getUnderlyingStream());
    }

    @Override
    public @Nonnull IDiscItemSaver makeSaver(@CheckForNull File directory) {
        SpuSaverFormat fmt = getContainerFormat();
        if (fmt == SPU) {
            return new SpuSaver(_spuItem, directory,
                    new File(_spuItem.getSuggestedBaseName().getPath() + "." + getExtension()),
                    getContainerFormat());
        } else if (fmt == VAG) {
            throw new UnsupportedOperationException("VAG support not implemented yet");
        } else {
            return new SpuSaverJavaAudio(_spuItem, directory,
                    new File(_spuItem.getSuggestedBaseName().getPath() + "." + getExtension()),
                    getContainerFormat(), getVolume());
        }
    }

    public @Nonnull AudioInputStream getStream() {
        return _spuItem.getAudioStream(getVolume());
    }


    /** Saves to one of the special SPU formats. */
    private static class SpuSaver implements IDiscItemSaver {

        @Nonnull
        private final DiscItemSpu _spuItem;
        @CheckForNull
        private final File _outputDir;
        @Nonnull
        private final SpuSaverFormat _saveFormat;
        @Nonnull
        protected final File _fileRelativePath;
        @CheckForNull
        private File _generatedFile;

        public SpuSaver(@Nonnull DiscItemSpu spuItem,
                        @CheckForNull File outputDir,
                        @Nonnull File fileRelativePath,
                        @Nonnull SpuSaverFormat saveFormat)
        {
            _spuItem = spuItem;
            _outputDir = outputDir;
            _fileRelativePath = fileRelativePath;
            _saveFormat = saveFormat;
        }

        final public void startSave(@Nonnull ProgressLogger pl) throws LoggedFailure, TaskCanceledException {
            pl.progressStart(1);
            File outputFile = new File(_outputDir, _fileRelativePath.getPath());

            try {
                IO.makeDirsForFile(outputFile);
            } catch (LocalizedFileNotFoundException ex) {
                throw new LoggedFailure(pl, Level.SEVERE, ex.getSourceMessage(), ex);
            }
            
            doSave(outputFile, pl);
            _generatedFile = outputFile;
            pl.progressEnd();
        }

        protected void doSave(@Nonnull File outputFile, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(outputFile);
            } catch (FileNotFoundException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(outputFile.toString()), ex);
            }

            InputStream spu = null;
            try {
                spu = _spuItem.getStream();
                byte[] ab = new byte[2048];
                int iBytesToCopy = _spuItem.getSoundUnitCount() * SpuAdpcmDecoder.SIZEOF_SOUND_UNIT;
                while (iBytesToCopy > 0) {
                    int iCopy = Math.min(iBytesToCopy, ab.length);
                    try {
                        IO.readByteArray(spu, ab, 0, iCopy);
                    } catch (IOException ex) {
                        throw new LoggedFailure(log, Level.SEVERE,
                                I.IO_READING_FROM_FILE_ERROR_NAME(_spuItem.getSourceCd().getSourceFile().toString()),
                                ex);
                    }
                    try {
                        fos.write(ab, 0, iCopy);
                    } catch (IOException ex) {
                        throw new LoggedFailure(log, Level.SEVERE,
                                I.IO_WRITING_TO_FILE_ERROR_NAME(outputFile.toString()), ex);
                    }
                    iBytesToCopy -= iCopy;
                }
            } finally {
                IO.closeSilently(spu, LOG);
                IO.closeSilently(fos, LOG);
            }
        }

        public @Nonnull String getInput() {
            return _spuItem.getIndexId().toString();
        }

        public @Nonnull ILocalizedMessage getOutputSummary() {
            return new UnlocalizedMessage(_fileRelativePath.getPath());
        }

        public void printSelectedOptions(@Nonnull FeedbackStream fbs) {
            fbs.println(I.CMD_AUDIO_FORMAT(_saveFormat.getCmdId()));
            fbs.println(I.CMD_FILENAME(_fileRelativePath));
        }

        public @Nonnull DiscItemSpu getDiscItem() {
            return _spuItem;
        }

        public @CheckForNull File[] getGeneratedFiles() {
            if (_generatedFile == null)
                return null;
            else
                return new File[] {_generatedFile};
        }

    }

    /** Saves to a Java audio format.
     * Subclassing is a little sloppy but saves code. */
    private static class SpuSaverJavaAudio extends SpuSaver {

        @Nonnull
        private final JavaAudioFormat _audioFormat;
        private final double _dblVolume;

        public SpuSaverJavaAudio(@Nonnull DiscItemSpu spuItem,
                                 @CheckForNull File outputDir,
                                 @Nonnull File fileRelativePath,
                                 @Nonnull SpuSaverFormat saveFormat,
                                 double dblVolume)
        {
            super(spuItem, outputDir, fileRelativePath, saveFormat);
            _audioFormat = saveFormat.getJavaType();
            _dblVolume = dblVolume;
        }

        @Override
        protected void doSave(@Nonnull File outputFile, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            AudioInputStream ais = getDiscItem().getAudioStream(_dblVolume);
            try {
                AudioSystem.write(ais, _audioFormat.getJavaType(), outputFile);
            } catch (IOException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_WRITING_FILE_ERROR_NAME(outputFile.toString()), ex);
            }
        }

        @Override
        public void printSelectedOptions(@Nonnull FeedbackStream fbs) {
            fbs.println(I.CMD_VOLUME_PERCENT(_dblVolume));
            fbs.println(I.CMD_AUDIO_FORMAT(_audioFormat.getCmdId()));
            fbs.println(I.CMD_FILENAME(_fileRelativePath));
        }

    }

}
