/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.modules.aconcagua;

import java.util.List;
import javax.annotation.Nonnull;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.sectorbased.ISelfDemuxingVideoSector;
import jpsxdec.modules.video.sectorbased.IVideoSectorWithFrameNumber;
import jpsxdec.modules.video.sectorbased.VideoSectorWithFrameNumberDemuxer;


public class AconcaguaDemuxer extends VideoSectorWithFrameNumberDemuxer {

    private final int _iQuantizationScale;
    private final boolean _blnIsIntroVideo;

    public AconcaguaDemuxer(@Nonnull SectorAconcaguaVideo firstChunk,
                            @Nonnull ILocalizedLogger log)
    {
        super(firstChunk, log);
        _iQuantizationScale = firstChunk.getQuantizationScale();
        _blnIsIntroVideo = firstChunk.isIntroVideo();
    }

    @Override
    public boolean addSectorIfPartOfFrame(@Nonnull ISelfDemuxingVideoSector sector) {
        if (!(sector instanceof SectorAconcaguaVideo))
            return false;
        SectorAconcaguaVideo ac = (SectorAconcaguaVideo) sector;
        if (ac.getQuantizationScale() != _iQuantizationScale || ac.isIntroVideo() != _blnIsIntroVideo) {
            return false;
        }
        return super.addSectorIfPartOfFrame(sector);
    }

    @Override
    public @Nonnull DemuxedAconcaguaFrame finishFrame(@Nonnull ILocalizedLogger log) {
        List<IVideoSectorWithFrameNumber> nonNullChunks = getNonNullChunks(log);

        // only Aconcagua sectors could have been added to the list
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<SectorAconcaguaVideo> list = (List<SectorAconcaguaVideo>)(List)nonNullChunks;
        return new DemuxedAconcaguaFrame(getWidth(), getHeight(), getHeaderFrameNumber(), list,
                                         _iQuantizationScale, _blnIsIntroVideo);
    }
}
