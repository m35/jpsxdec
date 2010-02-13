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

package jpsxdec.plugins.xa;

import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.JPanel;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.DiscItemSaver;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.plugins.ProgressListener;
import jpsxdec.util.FeedbackStream;

public class XAAudioItemSaver extends DiscItemSaver {

    private static final Logger log = Logger.getLogger(XAAudioItemSaver.class.getName());

    private SectorAudioWriterBuilder _audWriterBuilder;
    private DiscItemXAAudioStream _xaItem;

    public XAAudioItemSaver(DiscItemXAAudioStream xaItem) {
        _audWriterBuilder = new SectorAudioWriterBuilder(xaItem);
        _xaItem = xaItem;
    }

    @Override
    public String[] commandLineOptions(String[] asArgs, FeedbackStream infoStream) {
        return _audWriterBuilder.commandLineOptions(asArgs, infoStream);
    }

    @Override
    public void printHelp(FeedbackStream fbs) {
        _audWriterBuilder.printHelp(fbs);
    }

    @Override
    public JPanel getOptionPane() {
        return _audWriterBuilder.getGui();
    }

    @Override
    public void startSave(ProgressListener pl) throws IOException {
        SectorAudioWriter audioWriter = _audWriterBuilder.getAudioWriter();
        int iSector = _xaItem.getStartSector();
        try {
            final double SECTOR_LENGTH = _xaItem.getSectorLength();
            pl.progressStart("Writing " + audioWriter.getOutputFile());
            for (; iSector <= _xaItem.getEndSector(); iSector++) {
                CDSector cdSector = _xaItem.getSourceCD().getSector(iSector);
                IdentifiedSector identifiedSect = _xaItem.identifySector(cdSector);
                audioWriter.feedSector(identifiedSect);
                pl.progressUpdate((iSector - _xaItem.getStartSector()) / SECTOR_LENGTH);
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
