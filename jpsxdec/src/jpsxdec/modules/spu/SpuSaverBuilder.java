/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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

package jpsxdec.modules.spu;

import argparser.StringHolder;
import java.io.Closeable;
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
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.adpcm.VagWriter;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.formats.JavaAudioFormat;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.TabularFeedback;
import jpsxdec.i18n.TabularFeedback.Cell;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.exception.LocalizedFileNotFoundException;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.util.ArgParser;
import jpsxdec.util.IO;
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
        private final @Nonnull ILocalizedMessage _guiDotExtensionDescription;

        private SpuSaverFormat(@Nonnull String sExtension,
                               @Nonnull ILocalizedMessage guiDotExtensionDescription)
        {
            _jFmt = null;
            _sExtension = sExtension;
            _cmdId = new UnlocalizedMessage(_sExtension);
            _guiDotExtensionDescription = guiDotExtensionDescription;
        }

        private SpuSaverFormat(@Nonnull JavaAudioFormat jFmt) {
            _jFmt = jFmt;
            _sExtension = null;
            _cmdId = null;
            _guiDotExtensionDescription = null;
        }

        public @CheckForNull JavaAudioFormat getJavaType() {
            return _jFmt;
        }

        public @Nonnull String getExtension() {
            if (_jFmt != null)
                return _jFmt.getExtension();
            else
                return _sExtension;
        }

        @Override
        public String toString() {
            if (_jFmt != null)
                return _jFmt.toString();
            else
                return _guiDotExtensionDescription.toString();
        }

        public @Nonnull ILocalizedMessage getCmdId() {
            if (_jFmt != null)
                return _jFmt.getCmdId();
            else
                return _cmdId;
        }

    }

    /** Raw SPU data (a.k.a. 'headerless VAG'). */
    private static final SpuSaverFormat SPU = new SpuSaverFormat("spu", I.SPU_EXTENSION_DESCRIPTION());
    /** SPU data wrapped in VAG file. */
    private static final SpuSaverFormat VAG = new SpuSaverFormat("vag", I.VAG_EXTENSION_DESCRIPTION());
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
        AUDIO_FORMATS.add(VAG);
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

    @Override
    public @Nonnull DiscItemSpu getDiscItem() {
        return _spuItem;
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
        return getContainerFormat().getJavaType() != null;
    }

    // ....................................................

    public @Nonnull AudioInputStream getAudioStream() {
        return _spuItem.getAudioStream(getVolume());
    }

    private @Nonnull File getFileRelativePath() {
        return new File(_spuItem.getSuggestedBaseName().getPath() + "." + getExtension());
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
    public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs) {
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
    public void printSelectedOptions(@Nonnull ILocalizedLogger log) {
        SpuSaverFormat fmt = getContainerFormat();
        JavaAudioFormat jFmt = fmt.getJavaType();
        if (jFmt != null) {
            log.log(Level.INFO, I.CMD_VOLUME_PERCENT(_dblVolume));
            log.log(Level.INFO, I.CMD_AUDIO_FORMAT(jFmt.getCmdId()));
        }
        log.log(Level.INFO, I.CMD_FILENAME(getFileRelativePath()));
    }

    @Override
    public @Nonnull ILocalizedMessage getOutputSummary() {
        return new UnlocalizedMessage(getFileRelativePath().getPath());
    }
    @Override
    public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
        return new SpuSaverBuilderGui(this);
    }


    @Override
    public void startSave(@Nonnull ProgressLogger pl, @CheckForNull File directory)
            throws LoggedFailure, TaskCanceledException
    {
        clearGeneratedFiles();
        printSelectedOptions(pl);
        pl.progressStart(1);
        File outputFile = new File(directory, getFileRelativePath().getPath());

        try {
            IO.makeDirsForFile(outputFile);
        } catch (LocalizedFileNotFoundException ex) {
            throw new LoggedFailure(pl, Level.SEVERE, ex.getSourceMessage(), ex);
        }

        SpuSaverFormat fmt = getContainerFormat();
        JavaAudioFormat jFmt = fmt.getJavaType();
        if (jFmt != null) {
            startSaveJavaAudio(pl, outputFile, jFmt.getJavaType());
        } else {
            startSaveSpu(pl, outputFile, fmt);
        }
        pl.progressEnd();
    }

    private void startSaveJavaAudio(@Nonnull ProgressLogger pl,
                                    @Nonnull File outputFile,
                                    @Nonnull AudioFileFormat.Type audioFileType)
            throws LoggedFailure
    {
        try (AudioInputStream ais = _spuItem.getAudioStream(_dblVolume)) {
            addGeneratedFile(outputFile);
            AudioSystem.write(ais, audioFileType, outputFile);
        } catch (IOException ex) {
            throw new LoggedFailure(pl, Level.SEVERE, I.IO_WRITING_FILE_ERROR_NAME(outputFile.toString()), ex);
        }
    }

    private void startSaveSpu(@Nonnull ProgressLogger pl,
                              @Nonnull File outputFile,
                              @Nonnull SpuSaverFormat fmt)
            throws LoggedFailure
    {
        // Save with a raw SPU format
        ISaveSpu saver;
        if (fmt == SPU) {
            saver = new SaveSpuFile();
        } else if (fmt == VAG) {
            saver = new SaveVagFile();
        } else {
            throw new RuntimeException();
        }
        addGeneratedFile(outputFile);
        saver.open(_spuItem, outputFile, pl);
        InputStream spu = null;
        try {
            spu = _spuItem.getSpuStream();
            byte[] ab = new byte[SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT];
            for (int i = 0; i < _spuItem.getSoundUnitCount(); i++) {
                try {
                    IO.readByteArray(spu, ab);
                } catch (IOException ex) {
                    throw new LoggedFailure(pl, Level.SEVERE,
                            I.IO_READING_FROM_FILE_ERROR_NAME(_spuItem.getSourceCd().getSourceFile().toString()),
                            ex);
                }
                try {
                    saver.writeSoundUnit(ab);
                } catch (IOException ex) {
                    throw new LoggedFailure(pl, Level.SEVERE,
                            I.IO_WRITING_TO_FILE_ERROR_NAME(outputFile.toString()), ex);
                }
            }
        } finally {
            IO.closeSilently(spu, LOG);
            IO.closeSilently(saver, LOG);
        }
    }


    /** Handles shared SPU and VAG behavior. */
    private interface ISaveSpu extends Closeable {
        void open(@Nonnull DiscItemSpu spuItem, @Nonnull File outputFile, @Nonnull ILocalizedLogger log) throws LoggedFailure;
        void writeSoundUnit(@Nonnull byte[] abSoundUnit) throws IOException;
    }

    private static class SaveSpuFile implements ISaveSpu {
        @CheckForNull
        private FileOutputStream _fos;

        @Override
        public void open(@Nonnull DiscItemSpu spuItem, @Nonnull File outputFile, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            try {
                _fos = new FileOutputStream(outputFile);
            } catch (FileNotFoundException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(outputFile.toString()), ex);
            }
        }
        @Override
        public void writeSoundUnit(byte[] abSoundUnit) throws IOException {
            _fos.write(abSoundUnit);
        }
        @Override
        public void close() throws IOException {
            _fos.close();
        }
    }

    private static class SaveVagFile implements ISaveSpu {
        @CheckForNull
        private VagWriter _vag;

        @Override
        public void open(@Nonnull DiscItemSpu spuItem, @Nonnull File outputFile, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            try {
                _vag = new VagWriter(outputFile, makeId(spuItem), spuItem.getSampleRate());
            } catch (FileNotFoundException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(outputFile.toString()), ex);
            } catch (IOException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(outputFile.toString()), ex);
            }
        }
        @Override
        public void writeSoundUnit(byte[] abSoundUnit) throws IOException {
            _vag.writeSoundUnit(abSoundUnit);
        }
        @Override
        public void close() throws IOException {
            _vag.close();
        }

        private static @Nonnull String makeId(@Nonnull DiscItemSpu spu) {
            File baseName = spu.getSuggestedBaseName();
            String sName = baseName.getName();
            String sCleanName = sName.replaceAll("[^\\w\\d]", "");
            return sCleanName.substring(0, Math.min(sCleanName.length(), 16));
        }
    }
}
