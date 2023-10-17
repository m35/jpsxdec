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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.iso9660.DirectoryRecord;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.ByteArrayFPIS;

/** Sectors containing ISO9660 Directory Records. */
public class SectorISO9660DirectoryRecords extends IdentifiedSector
        implements Iterable<DirectoryRecord>
{

    @CheckForNull
    private ArrayList<DirectoryRecord> _dirRecords;

    public SectorISO9660DirectoryRecords(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        DirectoryRecord firstRec;
        ByteArrayFPIS sectStream = cdSector.getCdUserDataStream();
        try {
            firstRec = new DirectoryRecord(sectStream);
        } catch (IOException | BinaryDataNotRecognized ex) {
            return;
        }

        _dirRecords = new ArrayList<DirectoryRecord>();
        _dirRecords.add(firstRec);
        try {
            while (true) {
                _dirRecords.add(new DirectoryRecord(sectStream));
            }
        } catch (BinaryDataNotRecognized | IOException ex) {}

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
        return "ISO9660 Directory Records";
    }

    @Override
    public @Nonnull Iterator<DirectoryRecord> iterator() {
        if (_dirRecords == null)
            throw new IllegalStateException();
        return _dirRecords.iterator();
    }

    public @Nonnull ArrayList<DirectoryRecord> getRecords() {
        if (_dirRecords == null)
            throw new IllegalStateException();
        return _dirRecords;
    }

    @Override
    public String toString() {
        return String.format("ISO DirRec %s %s",
                super.toString(), _dirRecords);
    }
}
