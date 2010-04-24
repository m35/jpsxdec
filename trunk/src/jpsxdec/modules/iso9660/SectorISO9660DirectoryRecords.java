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

package jpsxdec.modules.iso9660;

import jpsxdec.modules.UnidentifiedSector;
import jpsxdec.modules.IdentifiedSector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.NotThisTypeException;


public class SectorISO9660DirectoryRecords
        extends UnidentifiedSector
        implements Iterable<DirectoryRecord>
{

    private ArrayList<DirectoryRecord> _dirRecords;
    
    public SectorISO9660DirectoryRecords(CDSector cdSectotr)
            throws NotThisTypeException
    {
        super(cdSectotr);
        DirectoryRecord oFirstRec;
        ByteArrayFPIS oSectStream = cdSectotr.getCDUserDataStream();
        try {
            oFirstRec = new DirectoryRecord(oSectStream);
        } catch (IOException ex) {
            throw new NotThisTypeException();
        }
        
        _dirRecords = new ArrayList<DirectoryRecord>();
        _dirRecords.add(oFirstRec);
        try {
            while (true) {
                _dirRecords.add(new DirectoryRecord(oSectStream));
            }
        } catch (NotThisTypeException ex) {} catch (IOException ex) {}
    }

    @Override
    public int getSectorType() {
        return IdentifiedSector.SECTOR_ISO9660_DR;
    }

    @Override
    public String getTypeName() {
        return "ISO9660 Directory Records";
    }

    public Iterator<DirectoryRecord> iterator() {
        return _dirRecords.iterator();
    }

    public ArrayList<DirectoryRecord> getRecords() {
        return _dirRecords;
    }

    @Override
    public String toString() {
        return String.format("ISO DirRec %s %s",
                super.cdToString(), _dirRecords.toString());
    }
}