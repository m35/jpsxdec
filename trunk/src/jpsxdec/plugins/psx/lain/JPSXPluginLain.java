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

package jpsxdec.plugins.psx.lain;

import java.io.IOException;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.DiscItemSerialization;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.plugins.IndexingDemuxerIS;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor_Lain;
import jpsxdec.util.NotThisTypeException;


public class JPSXPluginLain extends JPSXPlugin {

    private static JPSXPluginLain SINGLETON;

    public static JPSXPluginLain getPlugin() {
        if (SINGLETON == null)
            SINGLETON = new JPSXPluginLain();
        return SINGLETON;
    }

    private JPSXPluginLain() {
    }

    @Override
    public void indexing_sectorRead(IdentifiedSector sector) {
    }

    @Override
    public void indexing_endOfDisc() {
    }

    @Override
    public void indexing_static(IndexingDemuxerIS inStream) throws IOException {
        // todo: identify Lain data files
    }

    @Override
    public void deserialize_lineRead(DiscItemSerialization serial) {
    }

    @Override
    public IdentifiedSector identifySector(CDSector cdSector) {
        try { return new SectorLainVideo(cdSector); }
        catch (NotThisTypeException e) {}
        return null;
    }

    @Override
    public DemuxFrameUncompressor identifyVideoFrame(byte[] abHeaderBytes, long lngFrameNum) {
        if (DemuxFrameUncompressor_Lain.checkHeader(abHeaderBytes))
            return new DemuxFrameUncompressor_Lain();
        else
            return null;
    }

    @Override
    public String getPluginDescription() {
        return "Serial Experiments Lain game handling plugin for jPSXdec by Michael Sabin";
    }

}
