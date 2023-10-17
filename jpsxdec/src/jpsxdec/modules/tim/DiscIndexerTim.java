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

package jpsxdec.modules.tim;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.modules.CdSectorDemuxPiece;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.UnidentifiedSectorStreamListener;
import jpsxdec.tim.Tim;
import jpsxdec.tim.TimInfo;
import jpsxdec.util.DemuxPushInputStream;
import jpsxdec.util.DemuxedData;
import jpsxdec.util.IO;

/** Searches for TIM images. */
public class DiscIndexerTim extends DiscIndexer implements UnidentifiedSectorStreamListener.Listener {

    private static final Logger LOG = Logger.getLogger(DiscIndexerTim.class.getName());

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem serial)
            throws LocalizedDeserializationFail
    {
        if (DiscItemTim.TYPE_ID.equals(serial.getType()))
            return new DiscItemTim(getCd(), serial);
        return null;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        UnidentifiedSectorStreamListener.attachToSectorClaimer(scs, this);
    }

    @CheckForNull
    private DemuxPushInputStream<CdSectorDemuxPiece> _stream;

    @Override
    public void feedSector(@Nonnull CdSector sector) {
        CdSectorDemuxPiece piece = new CdSectorDemuxPiece(sector);
        if (_stream == null)
            _stream = new DemuxPushInputStream<CdSectorDemuxPiece>(piece);
        else
            _stream.addPiece(piece);
        findTims();
    }

    @Override
    public void endOfUnidentified() {
        exhaustStream();
    }

    private void findTims() {
        if (_stream == null)
            return;
        // read loop
        while (_stream.available() > Tim.MINIMUM_TIM_SIZE) {
            // ^ no sense looking for Tims if there isn't enough left to contain one
            _stream.mark(Integer.MAX_VALUE);
            try {
                TimInfo ti = Tim.isTim(_stream);
                if (ti != null) {
                    // found a tim: reset, skip, loop
                    addTim(ti);
                }
                // else, if bin not rec: reset, skip, loop
            } catch (DemuxPushInputStream.NeedsMoreData ex) {
                // if need more: reset, break
                _stream.reset();
                break;
            } catch (EOFException ex) {
                // should not happen
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                // should not happen
                throw new RuntimeException(ex);
            }

            _stream.reset();
            _stream.mark(4); // in case we are unable to skip 4 full bytes
            try {
                IO.skip(_stream, 4);
            } catch (DemuxPushInputStream.NeedsMoreData ex) {
                // if need more: reset, break to try again later
                _stream.reset();
                break;
            } catch (IOException ex) {
                // should not happen
                throw new RuntimeException(ex);
            }
        }
    }

    private void exhaustStream() {
        if (_stream == null)
            return;
        _stream.close();
        // read skip loop until skip throws eof
        while (_stream.available() > 2) {
            // ^ if the first 2 bytes of a Tim are found at the very end of the stream,
            //   it's silly to assume we were in the middle of a possible Tim
            _stream.mark(Integer.MAX_VALUE);
            try {
                TimInfo ti = Tim.isTim(_stream);
                if (ti != null) {
                    // found a tim: reset and skip
                    addTim(ti);
                }
                // else, no problem: reset and skip again
            } catch (EOFException ex) {
                LOG.log(Level.INFO, "Stream ended in the middle of possible Tim", ex);
                // no problem: reset and skip again
            } catch (IOException ex) {
                // should not happen
                throw new RuntimeException(ex);
            }

            _stream.reset();
            try {
                IO.skip(_stream, 4);
            } catch (EOFException ex) {
                // end of stream when skipping: stream exhausted, all done
                break;
            } catch (IOException ex) {
                // should not happen
                throw new RuntimeException(ex);
            }
        }
        _stream = null;
    }


    private void addTim(@Nonnull TimInfo tim) {
        DemuxedData<CdSectorDemuxPiece> demux = _stream.getMarkToReadDemux();
        addDiscItem(new DiscItemTim(getCd(),
                demux.getStartSector(), demux.getEndSector(),
                demux.getStartDataOffset(), tim.iPaletteCount, tim.iBitsPerPixel,
                tim.iPixelWidth, tim.iPixelHeight));
    }

    @Override
    public void listPostProcessing(@Nonnull Collection<DiscItem> allItems) {
    }
    @Override
    public boolean filterChild(DiscItem parent, DiscItem child) {
        return false;
    }
    @Override
    public void indexGenerated(@Nonnull DiscIndex index) {
    }

}
