/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2019  Michael Sabin
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

package jpsxdec.modules.crusader;

import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.DemuxPushInputStream;
import jpsxdec.util.DemuxedData;
import jpsxdec.util.IO;


/** Unlike other pipelines, there isn't an end-to-end path from sector to
 * presentation. Due to the way Crusader streams are, there really needs
 * to be a bridge that handles {@link SectorCrusader}s as they come
 * identified off the disc. 
 * This object is only valid for 1 movie. Create a new one once this is done. */
public class CrusaderSectorToCrusaderPacket {

    private static final Logger LOG = Logger.getLogger(CrusaderSectorToCrusaderPacket.class.getName());
    
    public interface PacketListener {
        void frame(@Nonnull CrusaderPacketHeaderReader.VideoHeader frame,
                   @Nonnull DemuxedData<CrusaderDemuxPiece> demux,
                   @Nonnull ILocalizedLogger log);

        void audio(@Nonnull CrusaderPacketHeaderReader.AudioHeader audio,
                   @Nonnull DemuxedData<CrusaderDemuxPiece> demux,
                   @Nonnull ILocalizedLogger log);
    }

    @CheckForNull
    private PacketListener _packetListener;

    private int _iPrevCrusaderSector = -1;

    @CheckForNull
    private DemuxPushInputStream<CrusaderDemuxPiece> _stream;
    @CheckForNull
    private CrusaderPacketHeaderReader.Header _header;

    public CrusaderSectorToCrusaderPacket() {
    }
    public CrusaderSectorToCrusaderPacket(@Nonnull PacketListener listener) {
        _packetListener = listener;
    }
    public void setListener(@CheckForNull PacketListener listener) {
        _packetListener = listener;
    }

    /** Returns if the sector was accepted by this movie.
     * A new {@link CrusaderSectorToCrusaderPacket} should be created for
     * each movie. */
    public boolean sectorRead(@Nonnull SectorCrusader sector, @Nonnull ILocalizedLogger log) {
        if (_iPrevCrusaderSector != -1) {
            if (sector.getCrusaderSectorNumber() < _iPrevCrusaderSector)
                return false;
            int iNumberOfSectorsMissing = sector.getCrusaderSectorNumber() - _iPrevCrusaderSector;
            int iPrevCdSector = sector.getSectorNumber() - iNumberOfSectorsMissing;
            for (int iCrusaderSector = _iPrevCrusaderSector+1,
                           iCdSector = iPrevCdSector+1;
                 iCrusaderSector < sector.getCrusaderSectorNumber();
                 iCrusaderSector++, iCdSector++)
            {
                addPiece(new CrusaderDemuxPiece(iCdSector));
            }
        }
        addPiece(new CrusaderDemuxPiece(sector));
        _iPrevCrusaderSector = sector.getCrusaderSectorNumber();
        read(log);
        return true;
    }
    
    private void addPiece(@Nonnull CrusaderDemuxPiece piece) {
        if (_stream == null) {
            _stream = new DemuxPushInputStream<CrusaderDemuxPiece>(piece);
        } else {
            _stream.addPiece(piece);
        }
    }

    /** Tells this to finish off the video and flush any remaining data. */
    public void endVideo(@Nonnull ILocalizedLogger log) {
        _stream.close();
        read(log);
        _stream = null;
    }

    private void read(@Nonnull ILocalizedLogger log) {
        try {
            while (true) {
                if (_header == null) {
                    _stream.mark(16);
                    try {
                        _header = CrusaderPacketHeaderReader.read(_stream);
                    } catch (BinaryDataNotRecognized ex) {
                        _stream.reset();
                        // there are sectors with unallocated data at the end
                        // of videos, so no need to consider this a warning
                        LOG.log(Level.INFO, "Invalid Crusader header in {0} offset {1,number,#}",
                                new Object[]{_stream.getCurrentPiece(), _stream.getOffsetInCurrentPiece()});
                        IO.skip(_stream, 1);
                    } catch (EOFException ex) {
                        // must be closed
                        // not enough data to even hold a header, so we're done
                        _stream = null;
                        return;
                    }
                } else {
                    _stream.mark(_header.getByteSize());
                    int iSkipped = IO.skipMax(_stream, _header.getByteSize());
                    DemuxedData<CrusaderDemuxPiece> demux = _stream.getMarkToReadDemux();

                    if (_header.getByteSize() != demux.getDemuxSize()) {
                        // this is possible if there is an unexpected end
                        // of Crusader sectors. Then the demux data read will
                        // be < what the header says it should be
                        LOG.log(Level.WARNING, "Crusader packet header size {0} != demux size {1}",
                                                new Object[]{_header.getByteSize(), demux.getDemuxSize()});
                    }
                    
                    if (_header instanceof CrusaderPacketHeaderReader.VideoHeader) {
                        if (_packetListener != null)
                            _packetListener.frame((CrusaderPacketHeaderReader.VideoHeader)_header, demux, log);
                    } else if (_header instanceof CrusaderPacketHeaderReader.AudioHeader) {
                        if (_packetListener != null)
                            _packetListener.audio((CrusaderPacketHeaderReader.AudioHeader)_header, demux, log);
                    } else {
                        throw new RuntimeException();
                    }
                    _header = null; // packet done
                }
            }
        } catch (DemuxPushInputStream.NeedsMoreData ex) {
            // ok, wait until another sector is added to try again
            _stream.reset();
        } catch (IOException ex) {
            throw new RuntimeException("Should not happen", ex);
        }
    }

}
