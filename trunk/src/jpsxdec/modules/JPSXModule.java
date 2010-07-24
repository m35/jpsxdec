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

package jpsxdec.modules;

import java.io.InputStream;
import jpsxdec.modules.psx.str.JPSXModuleVideo;
import jpsxdec.modules.xa.JPSXModuleXAAudio;
import jpsxdec.modules.psx.square.JPSXModuleSquare;
import jpsxdec.modules.iso9660.JPSXModuleISO9660;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.modules.psx.alice.JPSXModuleAlice;
import jpsxdec.modules.psx.lain.JPSXModuleLain;
import jpsxdec.modules.psx.tim.JPSXModuleTIM;
import jpsxdec.modules.psx.video.bitstreams.BitStreamUncompressor;


public abstract class JPSXModule {

    private static final Logger log = Logger.getLogger(JPSXModule.class.getName());

    private static final JPSXModule[] _aoModules = new JPSXModule[] {
        JPSXModuleISO9660.getModule(),
        JPSXModuleVideo.getModule(),
        JPSXModuleXAAudio.getModule(),
        JPSXModuleTIM.getModule(),
        JPSXModuleSquare.getModule(),
        JPSXModuleLain.getModule(),
        JPSXModuleAlice.getModule(),
    };

    public static JPSXModule[] getModules() {
        return _aoModules;
    }

    /** Identify the type of the supplied sector. */
    public static IdentifiedSector identifyModuleSector(CdSector cdSector) {
        for (JPSXModule module : _aoModules) {
            IdentifiedSector oPSXSect = module.identifySector(cdSector);
            if (oPSXSect != null)
                return oPSXSect;
        }
        return null;
    }

    public static BitStreamUncompressor identifyUncompressor(byte[] abDemuxBuf, int iStart, int iFrame) {
        for (JPSXModule module : _aoModules) {
            if (iStart != 0)
                throw new RuntimeException("oops");
            BitStreamUncompressor oUncompressor =
                module.identifyVideoFrame(abDemuxBuf, iFrame);
            if (oUncompressor != null)
                return oUncompressor;
        }
        return null;
    }

    public static void identifyStatic(InputStream inStream) {
        for (JPSXModule moldule : _aoModules) {
            //moldule.indexing_static(inStream);
        }
    }

    private List<DiscItem> _mediaList;
    private CDFileSectorReader _cd;

    abstract public void indexing_sectorRead(IdentifiedSector sector);

    abstract public void indexing_endOfDisc();

    abstract public void indexing_static(IndexingDemuxerIS inStream) throws IOException;

    abstract public void deserialize_lineRead(DiscItemSerialization serial);

    public void putYourCompletedMediaItemsHere(List<DiscItem> items) {
        _mediaList = items;
    }

    public void thisIsTheSourceCD(CDFileSectorReader cdReader) {
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

    protected CDFileSectorReader getSourceCD() { return _cd; }

    abstract public IdentifiedSector identifySector(CdSector cdSector);

    abstract public BitStreamUncompressor identifyVideoFrame(byte[] abHeaderBytes, long lngFrameNum);

    /** Override to add behavior. */
    public void mediaListGenerated(DiscIndex discIndex) {
    }

    /** Return HTML for an 'About' type dialog. */
    abstract public String getModuleDescription();

}
