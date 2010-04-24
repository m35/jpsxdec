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

package jpsxdec.modules.psx.tim;

import jpsxdec.modules.JPSXModule;
import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.modules.IndexingDemuxerIS;
import jpsxdec.modules.DiscItemSerialization;
import jpsxdec.modules.DiscIndex;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.modules.psx.video.bitstreams.BitStreamUncompressor;

/**
 * Searches for TIM images
 */
public class JPSXModuleTIM extends JPSXModule {

    private static final Logger log = Logger.getLogger(JPSXModuleTIM.class.getName());

    private static JPSXModuleTIM SINGLETON;

    public static JPSXModuleTIM getModule() {
        if (SINGLETON == null)
            SINGLETON = new JPSXModuleTIM();
        return SINGLETON;
    }

    private JPSXModuleTIM() {}

    @Override
    public IdentifiedSector identifySector(CDSector oSect) {
        return null;
    }

    @Override
    public void deserialize_lineRead(DiscItemSerialization oSerial) {
        try {
            if (DiscItemTIM.TYPE_ID.equals(oSerial.getType()))
                super.addDiscItem(new DiscItemTIM(oSerial));
        } catch (NotThisTypeException ex) {}
    }

    @Override
    public void indexing_sectorRead(IdentifiedSector oSect) {
    }

    @Override
    public void indexing_endOfDisc() {
    }

    @Override
    public void indexing_static(IndexingDemuxerIS inStream) throws IOException {
        try {
            int iStartSector = inStream.getSectorNumber();
            int iStartOffset = inStream.getSectorPosition();
            Tim t = Tim.read(inStream);
            super.addDiscItem(new DiscItemTIM(
                    iStartSector, inStream.getSectorNumber(),
                    iStartOffset, t.getPaletteCount(), t.getBitsPerPixel()));
        } catch (NotThisTypeException ex) {}
    }

    @Override
    public void mediaListGenerated(DiscIndex index) {
    }

    @Override
    public String getModuleDescription() {
        return "TIM module for jPSXdec by Michael Sabin";
    }

    @Override
    public BitStreamUncompressor identifyVideoFrame(byte[] abHeaderBytes, long lngFrameNum) {
        return null;
    }


}
