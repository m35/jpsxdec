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

package jpsxdec.discitems.savers;

import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.discitems.ISectorAudioDecoder;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.formats.JavaAudioFormat;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.AudioOutputFileWriter;
import jpsxdec.util.ProgressListener;

/** Actually performs the saving process using the options selected in
 * {@link AudioSaverBuilder}. */
public class AudioSaver implements IDiscItemSaver  {

    private static final Logger log = Logger.getLogger(AudioSaver.class.getName());

    private final DiscItemAudioStream _audItem;
    private final String _sOutputFile;
    private final AudioOutputFileWriter _audioWriter;
    private final ISectorAudioDecoder _decoder;

    private static final boolean WRITE_BIG_ENDIAN = true;

    public AudioSaver(DiscItemAudioStream audItem, String sFilename,
                       JavaAudioFormat containerFormat, double dblVolume)
            throws IOException
    {
        _audItem = audItem;
        _sOutputFile = sFilename;

        AudioFormat audioFmt = audItem.getAudioFormat(WRITE_BIG_ENDIAN);
        _audioWriter = new AudioOutputFileWriter(new File(_sOutputFile),
                            audioFmt, containerFormat.getJavaType());
        
        _decoder = audItem.makeDecoder(WRITE_BIG_ENDIAN, dblVolume);
        _decoder.open(new ISectorAudioDecoder.ISectorTimedAudioWriter() {
            public void write(AudioFormat format, byte[] abData, int iStart, int iLen, int iPresentationSector) throws IOException {
                _audioWriter.write(format, abData, iStart, iLen);
            }
        });
    }

    public void startSave(ProgressListener pl) throws IOException {
        try {
            final double SECTOR_LENGTH = _audItem.getSectorLength();
            for (int iSector = 0; iSector <= SECTOR_LENGTH; iSector++) {
                IdentifiedSector identifiedSect = _audItem.getRelativeIdentifiedSector(iSector);
                _decoder.feedSector(identifiedSect);
                pl.progressUpdate(iSector / SECTOR_LENGTH);
            }
            pl.progressEnd();
        } finally {
            try {
                _audioWriter.close();
            } catch (Throwable ex) {
                pl.error(ex);
            }
        }
    }

}

