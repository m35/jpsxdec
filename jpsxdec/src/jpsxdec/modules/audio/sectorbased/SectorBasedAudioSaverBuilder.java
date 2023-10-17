/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.modules.audio.sectorbased;

import argparser.StringHolder;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.cdreaders.CdException;
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
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.ArgParser;
import jpsxdec.util.AudioOutputFileWriter;
import jpsxdec.util.IO;
import jpsxdec.util.TaskCanceledException;

/** Manages possible options for saving an audio stream. */
public class SectorBasedAudioSaverBuilder extends DiscItemSaverBuilder {

    private static final Logger LOG = Logger.getLogger(SectorBasedAudioSaverBuilder.class.getName());

    @Nonnull
    private final DiscItemSectorBasedAudioStream _audItem;

    public SectorBasedAudioSaverBuilder(@Nonnull DiscItemSectorBasedAudioStream audItem) {
        _audItem = audItem;
        JavaAudioFormat[] fmts = JavaAudioFormat.getAudioFormats();
        if (fmts.length > 0)
            _containerFormat = fmts[0];
        else
            _containerFormat = null;
    }

    @Override
    public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder other) {
        if (other instanceof SectorBasedAudioSaverBuilder) {
            SectorBasedAudioSaverBuilder o = (SectorBasedAudioSaverBuilder) other;
            o.setContainerFormat(getContainerFormat());
            o.setVolume(getVolume());
            return true;
        }
        return false;
    }

    @Override
    public @Nonnull DiscItemSectorBasedAudioStream getDiscItem() {
        return _audItem;
    }

    // ....................................................

    @Nonnull
    private JavaAudioFormat _containerFormat;
    public void setContainerFormat(@Nonnull JavaAudioFormat val) {
        _containerFormat = val;
        firePossibleChange();
    }
    public @Nonnull JavaAudioFormat getContainerFormat() {
        return _containerFormat;
    }

    public int getContainerFormat_listSize() {
        return JavaAudioFormat.getAudioFormats().length;
    }
    public @Nonnull JavaAudioFormat getContainerFormat_listItem(int i) {
        return JavaAudioFormat.getAudioFormats()[i];
    }

    public @Nonnull String getExtension() {
        return getContainerFormat().getExtension();
    }

    // ....................................................

    public @Nonnull String getFileBaseName() {
        return _audItem.getSuggestedBaseName().getPath();
    }

    // ....................................................

    private double _dblVolume = 1.0;
    public void setVolume(double val) {
        _dblVolume = val;
        firePossibleChange();
    }
    public double getVolume() {
        return _dblVolume;
    }

    // ....................................................

    private @Nonnull File getFileRelativePath() {
        return new File(_audItem.getSuggestedBaseName().getPath() + "." + getExtension());
    }

    @Override
    public void printHelp(@Nonnull FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();

        tfb.setRowSpacing(1);

        tfb.addCell(I.CMD_AUDIO_AF());
        Cell cell = new Cell(I.CMD_AUDIO_AF_HELP(JavaAudioFormat.getAudioFormats()[0].getExtension()));
        for (JavaAudioFormat audioFormat : JavaAudioFormat.getAudioFormats()) {
            cell.addLine(audioFormat.getCmdId(), 2);
        }
        tfb.addCell(cell);
        tfb.newRow();

        tfb.addCell(I.CMD_AUDIO_VOL()).addCell(I.CMD_AUDIO_VOL_HELP(100));

        tfb.write(fbs.getUnderlyingStream());
    }
    @Override
    public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs) {
        if (!ap.hasRemaining())
            return;

        StringHolder vol = ap.addStringOption("-vol");
        StringHolder audfmt = ap.addStringOption("-audfmt","-af");
        ap.match();

        if (vol.value != null) {
            try {
                int iVol = Integer.parseInt(vol.value);
                if (iVol < 0 || iVol > 100)
                    throw new NumberFormatException();
                setVolume(iVol / 100.0);
            } catch (NumberFormatException ex) {
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VALUE_FOR_CMD(vol.value, "-vol"));
            }
        }

        if (audfmt.value != null) {
            JavaAudioFormat fmt = JavaAudioFormat.fromCmdLine(audfmt.value);
            if (fmt != null) {
                setContainerFormat(fmt);
            } else {
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VALUE_FOR_CMD(audfmt.value, "-af,-audfmt"));
            }
        }
    }
    @Override
    public void printSelectedOptions(@Nonnull ILocalizedLogger log) {
        log.log(Level.INFO, I.CMD_AUDIO_FORMAT(_containerFormat.getCmdId()));
        log.log(Level.INFO, I.CMD_VOLUME_PERCENT(getVolume()));
        log.log(Level.INFO, I.CMD_FILENAME(getFileRelativePath()));
    }

    @Override
    public @Nonnull ILocalizedMessage getOutputSummary() {
        return new UnlocalizedMessage(getFileRelativePath().getPath());
    }
    @Override
    public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
        return new SectorBasedAudioSaverBuilderGui(this);
    }


    @Override
    public void startSave(final @Nonnull ProgressLogger pl, @CheckForNull File outputDir)
            throws LoggedFailure, TaskCanceledException
    {
        clearGeneratedFiles();
        printSelectedOptions(pl);

        final File outputFile = new File(outputDir, getFileRelativePath().getPath());

        try {
            IO.makeDirsForFile(outputFile);
        } catch (LocalizedFileNotFoundException ex) {
            throw new LoggedFailure(pl, Level.SEVERE, ex.getSourceMessage(), ex);
        }

        // SectorClaimSystem -> ISectorClaimToSectorBasedDecodedAudio -> SectorBasedDecodedAudioPacket -> AudioOutputFileWriter

        ISectorClaimToSectorBasedDecodedAudio decoder = _audItem.makeDecoder(getVolume());
        AudioFormat audioFmt = decoder.getOutputFormat();
        final AudioOutputFileWriter audioWriter;
        try {
            audioWriter = new AudioOutputFileWriter(outputFile,
                                audioFmt, _containerFormat.getJavaType());
            addGeneratedFile(outputFile);
        } catch (IOException ex) {
            throw new LoggedFailure(pl, Level.SEVERE, I.IO_WRITING_FILE_ERROR_NAME(outputFile.toString()), ex);
        }

        decoder.setSectorBasedAudioListener(new SectorBasedDecodedAudioPacket.Listener() {
            @Override
            public void audioPacketComplete(@Nonnull SectorBasedDecodedAudioPacket packet,
                                            @Nonnull ILocalizedLogger log)
            {
                try {
                    // Indexing should have ensured there are no breaks in the audio stream
                    // so there's no need to check for gaps... but the image may
                    // have changed since the index was generated, so I suppose it's possible
                    // TODO warn user if there are audio breaks and maybe fill them with silence
                    if (!packet.getAudioFormat().matches(audioWriter.getFormat()))
                        throw new IllegalArgumentException();
                    byte[] abData = packet.getData();
                    audioWriter.write(abData, 0, abData.length);
                } catch (IOException ex) {
                    // intercept the exception, then unwind outside of the pipeline
                    throw new UnwindException(
                        new LoggedFailure(pl, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(outputFile.toString()), ex));
                }
            }
        });

        try {
            SectorClaimSystem it = _audItem.createClaimSystem();
            decoder.attachToSectorClaimer(it);
            pl.progressStart(_audItem.getSectorLength());
            for (int iSector = 0; it.hasNext(); iSector++) {
                try {
                    IIdentifiedSector identifiedSect = it.next(pl);
                } catch (CdException.Read ex) {
                    throw new LoggedFailure(pl, Level.SEVERE,
                            I.IO_READING_FROM_FILE_ERROR_NAME(ex.getFile().toString()), ex);
                }
                pl.progressUpdate(iSector);
            }
            it.flush(pl);
            pl.progressEnd();
        } finally {
            IO.closeSilently(audioWriter, LOG);
        }
    }


    /** Captures any output {@link LoggedFailure} and unwinds the stack so the
     * {@link LoggedFailure} can be thrown outside the pipeline. */
    private static class UnwindException extends RuntimeException {
        @Nonnull
        private final LoggedFailure _fail;
        public UnwindException(@Nonnull LoggedFailure fail) {
            _fail = fail;
        }
        public LoggedFailure getFailure() {
            return _fail;
        }
    }

}
