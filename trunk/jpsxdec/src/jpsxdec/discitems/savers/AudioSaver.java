/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.discitems.ISectorAudioDecoder;
import jpsxdec.formats.JavaAudioFormat;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.util.AudioOutputFileWriter;
import jpsxdec.util.IO;
import jpsxdec.util.ProgressListenerLogger;
import jpsxdec.util.TaskCanceledException;

/** Actually performs the saving process using the options selected in
 * {@link AudioSaverBuilder}. */
public class AudioSaver implements IDiscItemSaver  {

    @Nonnull
    private final DiscItemAudioStream _audItem;
    @Nonnull
    private final ISectorAudioDecoder _decoder;
    @CheckForNull
    private final File _outputDir;
    @Nonnull
    private final File _fileRelativePath;
    @Nonnull
    private final JavaAudioFormat _containerFormat;
    @CheckForNull
    private File _generatedFile;

    public AudioSaver(@Nonnull DiscItemAudioStream audItem,
                      @CheckForNull File outputDir, @Nonnull File fileRelativePath,
                      @Nonnull JavaAudioFormat containerFormat, double dblVolume)
    {
        _audItem = audItem;
        _outputDir = outputDir;
        _fileRelativePath = fileRelativePath;
        _decoder = audItem.makeDecoder(dblVolume);
        _containerFormat = containerFormat;
    }

    public @Nonnull String getInput() {
        return _audItem.getIndexId().toString();
    }

    public @Nonnull DiscItemAudioStream getDiscItem() {
        return _audItem;
    }

    public @Nonnull ILocalizedMessage getOutputSummary() {
        return new UnlocalizedMessage(_fileRelativePath.getPath());
    }

    public void printSelectedOptions(@Nonnull PrintStream ps) {
        ps.println(I.CMD_AUDIO_FORMAT(_containerFormat));
        ps.println(I.CMD_VOLUME_PERCENT(_decoder.getVolume()));
        ps.println(I.CMD_FILENAME(_fileRelativePath));
    }


    public void startSave(@Nonnull ProgressListenerLogger pll) throws IOException, TaskCanceledException {

        File outputFile = new File(_outputDir, _fileRelativePath.getPath());

        IO.makeDirsForFile(outputFile);

        AudioFormat audioFmt = _decoder.getOutputFormat();
        final AudioOutputFileWriter audioWriter;
        audioWriter = new AudioOutputFileWriter(outputFile,
                            audioFmt, _containerFormat.getJavaType());
        _generatedFile = outputFile;
        _decoder.setAudioListener(new ISectorAudioDecoder.ISectorTimedAudioWriter() {
            public void write(AudioFormat format, byte[] abData, int iStart, int iLen, int iPresentationSector) throws IOException {
                audioWriter.write(format, abData, iStart, iLen);
            }
        });

        try {
            final double dblSectorLength = _audItem.getSectorLength();
            IdentifiedSectorIterator it = _audItem.identifiedSectorIterator();
            for (int iSector = 0; it.hasNext(); iSector++) {
                IdentifiedSector identifiedSect = it.next();
                if (identifiedSect != null)
                    _decoder.feedSector(identifiedSect, pll);
                pll.progressUpdate(iSector / dblSectorLength);
            }
            pll.progressEnd();
        } finally {
            try {
                audioWriter.close();
            } catch (Throwable ex) {
                I.ERR_CLOSING_AUDIO_WRITER().log(pll, Level.SEVERE, ex);
            }
        }
    }

    public @CheckForNull File[] getGeneratedFiles() {
        if (_generatedFile == null)
            return null;
        else
            return new File[] {_generatedFile};
    }    
}

