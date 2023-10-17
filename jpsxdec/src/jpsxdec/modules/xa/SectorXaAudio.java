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

package jpsxdec.modules.xa;

import java.io.PrintStream;
import javax.annotation.Nonnull;
import jpsxdec.adpcm.XaAdpcmDecoder;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.cdreaders.XaAnalysis;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.ByteArrayFPIS;


/** Standard audio sector for XA. */
public class SectorXaAudio extends IdentifiedSector {

    private int _iSamplesPerSecond;
    private int _iBitsPerSample;
    private boolean _blnStereo;
    private int _iErrors;

    public SectorXaAudio(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        XaAnalysis analysis = XaAnalysis.analyze(cdSector);
        if (analysis == null)
            return;

        _blnStereo = analysis.blnStereo;
        _iSamplesPerSecond = analysis.iSamplesPerSecond;
        _iBitsPerSample = analysis.iBitsPerSample;

        _iErrors = analysis.iErrors;

        setProbability(analysis.iProbability);
    }

    public int getFileNumber() {
        CdSectorXaSubHeader sh = getCdSector().getSubHeader();
        assert sh != null;
        return sh.getFileNumber();
    }

    public int getChannel() {
        CdSectorXaSubHeader sh = getCdSector().getSubHeader();
        assert sh != null;
        return sh.getChannel();
    }

    public boolean isStereo() {
        return _blnStereo;
    }

    public int getAdpcmBitsPerSample() {
        return _iBitsPerSample;
    }

    public int getSamplesPerSecond() {
        return _iSamplesPerSecond;
    }

    /** The last 20 bytes of the sector are unused. */
    public int getDemuxPieceSize() {
        return super.getCdSector().getCdUserDataSize() - 20;
    }

    public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                0, getDemuxPieceSize());
    }


    @Override
    public @Nonnull String getTypeName() {
        return "XA Audio";
    }

    public int getSampleFrameCount() {
        return XaAdpcmDecoder.pcmSampleFramesGeneratedFromXaAdpcmSector(_iBitsPerSample, _blnStereo);
    }

    @Override
    public String toString() {
        CdSector cd = getCdSector();
        int iSize = cd.getCdUserDataSize();
        StringBuilder sb = new StringBuilder(String.format("%s %s %s %d bits/sample %d samples/sec "
                + "%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",
                getTypeName(),
                super.toString(),
                _blnStereo ? "Stereo" : "Mono",
                _iBitsPerSample,
                _iSamplesPerSecond,
                cd.readUserDataByte(iSize - 20),
                cd.readUserDataByte(iSize - 19),
                cd.readUserDataByte(iSize - 18),
                cd.readUserDataByte(iSize - 17),
                cd.readUserDataByte(iSize - 16),
                cd.readUserDataByte(iSize - 15),
                cd.readUserDataByte(iSize - 14),
                cd.readUserDataByte(iSize - 13),
                cd.readUserDataByte(iSize - 12),
                cd.readUserDataByte(iSize - 11),
                cd.readUserDataByte(iSize - 10),
                cd.readUserDataByte(iSize - 9),
                cd.readUserDataByte(iSize - 8),
                cd.readUserDataByte(iSize - 7),
                cd.readUserDataByte(iSize - 6),
                cd.readUserDataByte(iSize - 5),
                cd.readUserDataByte(iSize - 4),
                cd.readUserDataByte(iSize - 3),
                cd.readUserDataByte(iSize - 2),
                cd.readUserDataByte(iSize - 1)
                ));
        if (_iErrors > 0)
            sb.append(" {").append(_iErrors).append(" errors}");
        if (isSilent())
            sb.append(" SILENT");
        return sb.toString();
    }

    /** Checks if the sector doesn't generate any sound. Note that this
     *  does not mean, if the sector was decoded as part of an ADPCM stream,
     *  that it would actually produce only silence. Because ADPCM uses prior
     *  samples to determine the current sample, the first 2 or so samples
     *  might have sound. But after that would be silence. This method is
     *  most reliable if this sector is not associated with any other XA
     *  sectors. In that case it really doesn't generate anything but silence.
     *  */
    public boolean isSilent() {
        CdSector cdSector = getCdSector();

        for(int iSndGrp = 0, i = 0;
            iSndGrp < XaAdpcmDecoder.ADPCM_SOUND_GROUPS_PER_SECTOR;
            iSndGrp++, i += XaAdpcmDecoder.SIZEOF_SOUND_GROUP)
        {
            // just check if all ADPCM values are 0
            for (int j = 16; j < XaAdpcmDecoder.SIZEOF_SOUND_GROUP; j++) {
                if (i+j >= cdSector.getCdUserDataSize())
                    return false; // catch toString when probability == 0
                if (cdSector.readUserDataByte(i+j) != 0)
                    return false;
            }
        }
        return true;
    }

    public int getErrorCount() {
        return _iErrors;
    }

    public void printErrors(@Nonnull PrintStream ps) {
        ps.println("Sector " + getSectorNumber() + ": " +_iErrors+ " errors in XA sound parameters");
    }

}
