/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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
import javax.sound.sampled.AudioFormat;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.discitems.ISectorAudioDecoder;
import jpsxdec.formats.JavaAudioFormat;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.AudioOutputFileWriter;
import jpsxdec.util.IO;
import jpsxdec.util.ProgressListenerLogger;
import jpsxdec.util.TaskCanceledException;

/** Actually performs the saving process using the options selected in
 * {@link AudioSaverBuilder}. */
public class AudioSaver implements IDiscItemSaver  {

    private final DiscItemAudioStream _audItem;
    private final ISectorAudioDecoder _decoder;
    private final File _fileSubPath;
    private final JavaAudioFormat _containerFormat;

    public AudioSaver(DiscItemAudioStream audItem, File fileSubPath,
                       JavaAudioFormat containerFormat, double dblVolume)
    {
        _audItem = audItem;
        _fileSubPath = fileSubPath;
        _decoder = audItem.makeDecoder(dblVolume);
        _containerFormat = containerFormat;
    }

    public String getInput() {
        return _audItem.getIndexId().toString();
    }

    public DiscItemAudioStream getDiscItem() {
        return _audItem;
    }

    public String getOutputSummary() {
        return _fileSubPath.getPath();
    }

    public File getOutputFile(int i) {
        return _fileSubPath;
    }

    public int getOutputFileCount() {
        return 1;
    }

    public void printSelectedOptions(PrintStream ps) {
        ps.println("Format: " + _containerFormat);
        ps.println("Volume: " + Math.round(_decoder.getVolume() * 100) + "%");
        ps.println("Filename: " + _fileSubPath);
    }


    public void startSave(ProgressListenerLogger pl, File dir) throws IOException, TaskCanceledException {

        File outputFile = new File(dir, _fileSubPath.getPath());

        IO.makeDirs(outputFile.getParentFile());

        AudioFormat audioFmt = _decoder.getOutputFormat();
        final AudioOutputFileWriter audioWriter;
        audioWriter = new AudioOutputFileWriter(outputFile,
                            audioFmt, _containerFormat.getJavaType());

        _decoder.setAudioListener(new ISectorAudioDecoder.ISectorTimedAudioWriter() {
            public void write(AudioFormat format, byte[] abData, int iStart, int iLen, int iPresentationSector) throws IOException {
                audioWriter.write(format, abData, iStart, iLen);
            }
        });

        try {
            final int SECTOR_LENGTH = _audItem.getSectorLength();
            for (int iSector = 0; iSector < SECTOR_LENGTH; iSector++) {
                IdentifiedSector identifiedSect = _audItem.getRelativeIdentifiedSector(iSector);
                _decoder.feedSector(identifiedSect, pl);
                pl.progressUpdate(iSector / (double)SECTOR_LENGTH);
            }
            pl.progressEnd();
        } finally {
            try {
                audioWriter.close();
            } catch (Throwable ex) {
                pl.log(Level.SEVERE, "Error closing audio writer", ex);
            }
        }
    }

}

