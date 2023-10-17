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

package jpsxdec.modules.iso9660;

import java.io.EOFException;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.iso9660.VolumePrimaryDescriptor;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.ByteArrayFPIS;

/** Sector containing the ISO9660 Volume Primary Descriptor. I believe
 * this usually falls at sector 16 in the disc image. */
public class SectorISO9660VolumePrimaryDescriptor extends IdentifiedSector {

    @CheckForNull
    private VolumePrimaryDescriptor _primaryDescriptor;

    public SectorISO9660VolumePrimaryDescriptor(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        int iSectNum = cdSector.getSectorIndexFromStart();
        if (iSectNum > 16) return;
        try {
            _primaryDescriptor = new VolumePrimaryDescriptor(cdSector.getCdUserDataStream());
        } catch (EOFException ex) {
            return;
        } catch (IOException ex) {
            // the sector should have enough data to read a VPD, but if not, return
            return;
        } catch (BinaryDataNotRecognized ex) {
            return;
        }
        setProbability(100);
    }

    public int getIdentifiedUserDataSize() {
        return getCdSector().getCdUserDataSize();
    }

    public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
        return getCdSector().getCdUserDataStream();
    }

    @Override
    public @Nonnull String getTypeName() {
        return "ISO9660 Volume Primary Descriptor";
    }

    public @Nonnull VolumePrimaryDescriptor getVPD() {
        if (_primaryDescriptor == null)
            throw new IllegalStateException();
        return _primaryDescriptor;
    }

    @Override
    public String toString() {
        return String.format("ISO PriDesc %s %s",
                super.toString(), _primaryDescriptor);
    }

}
