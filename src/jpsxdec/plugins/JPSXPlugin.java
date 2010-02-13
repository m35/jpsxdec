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

package jpsxdec.plugins;

import java.io.InputStream;
import jpsxdec.plugins.psx.str.JPSXPluginVideo;
import jpsxdec.plugins.xa.JPSXPluginXAAudio;
import jpsxdec.plugins.psx.square.JPSXPluginSquare;
import jpsxdec.plugins.iso9660.JPSXPluginISO9660;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.plugins.psx.alice.JPSXPluginAlice;
import jpsxdec.plugins.psx.lain.JPSXPluginLain;
import jpsxdec.plugins.psx.tim.JPSXPluginTIM;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;


public abstract class JPSXPlugin {

    private static final Logger log = Logger.getLogger(JPSXPlugin.class.getName());

    private static final JPSXPlugin[] aoPlugins = new JPSXPlugin[] {
        JPSXPluginISO9660.getPlugin(),
        JPSXPluginVideo.getPlugin(),
        JPSXPluginXAAudio.getPlugin(),
        JPSXPluginTIM.getPlugin(),
        JPSXPluginSquare.getPlugin(),
        JPSXPluginLain.getPlugin(),
        JPSXPluginAlice.getPlugin(),
    };

    public static JPSXPlugin[] getPlugins() {
        return aoPlugins;
    }

    /** Identify the type of the supplied sector. */
    public static IdentifiedSector identifyPluginSector(CDSector cdSector) {
        for (JPSXPlugin oPlugin : aoPlugins) {
            IdentifiedSector oPSXSect = oPlugin.identifySector(cdSector);
            if (oPSXSect != null)
                return oPSXSect;
        }
        return null;
    }

    public static DemuxFrameUncompressor identifyUncompressor(byte[] abDemuxBuf, int iStart, int iFrame) {
        for (JPSXPlugin plugin : aoPlugins) {
            if (iStart != 0)
                throw new RuntimeException("oops");
            DemuxFrameUncompressor oUncompressor =
                plugin.identifyVideoFrame(abDemuxBuf, iFrame);
            if (oUncompressor != null)
                return oUncompressor;
        }
        return null;
    }

    public static void identifyStatic(InputStream inStream) {
        for (JPSXPlugin plugin : aoPlugins) {
            //plugin.indexing_static(inStream);
        }
    }

    private List<DiscItem> _mediaList;
    private CDSectorReader _cd;

    abstract public void indexing_sectorRead(IdentifiedSector sector);

    abstract public void indexing_endOfDisc();

    abstract public void indexing_static(IndexingDemuxerIS inStream) throws IOException;

    abstract public void deserialize_lineRead(DiscItemSerialization serial);

    public void putYourCompletedMediaItemsHere(List<DiscItem> items) {
        _mediaList = items;
    }

    public void thisIsTheSourceCD(CDSectorReader cdReader) {
        _cd = cdReader;
    }

    protected void addDiscItem(DiscItem discItem) {
        if (discItem == null) {
            log.log(Level.WARNING, "Something tried to add a null disc item.", new Exception());
            return;
        }
        if (log.isLoggable(Level.INFO))
            log.info("Adding media item " + discItem.toString());
        discItem.setSourceCD(_cd);
        _mediaList.add(discItem);
    }

    protected CDSectorReader getSourceCD() { return _cd; }

    abstract public IdentifiedSector identifySector(CDSector cdSector);

    abstract public DemuxFrameUncompressor identifyVideoFrame(byte[] abHeaderBytes, long lngFrameNum);

    /** Override to add behavior. */
    public void mediaListGenerated(DiscIndex discIndex) {
    }

    /** Return HTML for an 'About' type dialog. */
    abstract public String getPluginDescription();

}
