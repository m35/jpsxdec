/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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

package jpsxdec.modules.spu;

import java.util.Collection;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.UnidentifiedSectorStreamListener;


public class DiscIndexerSpu extends DiscIndexer implements UnidentifiedSectorStreamListener.Listener {

    public static final boolean ENABLE_SPU_SUPPORT = false;


    private static final Logger LOG = Logger.getLogger(DiscIndexerSpu.class.getName());

    private static final int MIN_SOUND_UNIT_COUNT = 16;

    @Override
    public @CheckForNull DiscItemSpu deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        if (DiscItemSpu.TYPE_ID.equals(fields.getType()))
            return new DiscItemSpu(getCd(), fields);
        return null;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        UnidentifiedSectorStreamListener.attachToSectorClaimer(scs, this);
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

    /**
     * http://problemkaputt.de/psx-spx.htm#soundprocessingunitspu
     *
     * Flag Bits (in 2nd byte of ADPCM Header)
     *
     * 0 Loop End (0=No change, 1=Set ENDX flag and Jump to [1F801C0Eh+N*10h])
     * 1 Loop Repeat (0=Force Release and set ADSR Level to Zero; only if Bit0=1)
     * 2 Loop Start (0=No change, 1=Copy current address to [1F801C0Eh+N*10h])
     * 3-7 Unknown (usually 0)
     *
     * Possible combinations for Bit0-1 are:
     *
     * Code 0 = Normal (continue at next 16-byte block)
     * Code 1 = End+Mute (jump to Loop-address, set ENDX flag, Release, Env=0000h)
     * Code 2 = Ignored (same as Code 0)
     * Code 3 = End+Repeat (jump to Loop-address, set ENDX flag)
     *
     * Looped and One-shot Samples
     * The Loop Start/End flags in the ADPCM Header allow to play one or more
     * sample block(s) in a loop, that can be either  all block(s) endless
     * repeated, or only the last some block(s) of the sample.
     * There's no way to stop the output, so a one-shot sample must be
     * followed by dummy block (with Loop Start/End flags both set, and all data
     * nibbles set to zero; so that the block gets endless repeated, but doesn't
     * produce any sound).
     */
    private class SpuRun {
        private boolean _blnInRun = false;
        private int _iStartSector;
        private int _iStartOffset;

        private int _iEndSector;
        private int _iEndOffset;
        private int _iSoundUnitCount;

        private boolean _blnLastSoundUnit;

        private boolean _blnOnlyZeroes;

        public void addQuad(int iQuadIndex, int iQuad, int iSector, int iOffset) {
            if (iQuadIndex == 0) {
                int iFilterRange = (iQuad >> 24) & 0xff;
                int iFlagBits = (iQuad >> 16) & 0xff;

                int iFilter = (iFilterRange >> 4) & 0xf;
                int iRange = iFilterRange & 0xf;
                boolean blnIsSpuHeader = iFilter <=4 && iRange <= 12 && iFlagBits <= 7;
                boolean blnIsEnd = false;
                if (blnIsSpuHeader) {
                    blnIsEnd = (iFlagBits & 1) != 0;
                }

                if (_blnInRun) {
                    if (!blnIsSpuHeader) {
                        cancelRun();
                    } else if (blnIsEnd) {
                        _blnLastSoundUnit = true;
                    }
                } else if (blnIsSpuHeader && iFilterRange != 0 && !blnIsEnd) {
                    // not in a run and this is a non-zero SPU header
                    startRun(iSector, iOffset);
                    _blnOnlyZeroes = (iQuad & 0xffff) == 0;
                }
            } else if (_blnInRun) {
                _blnOnlyZeroes = _blnOnlyZeroes && iQuad == 0;
                if (iQuadIndex == 3) {
                    _iEndSector = iSector;
                    _iEndOffset = iOffset + 3;
                    _iSoundUnitCount++;
                    if (_blnLastSoundUnit) {
                        endRun();
                    }
                }
            }
        }

        private void cancelRun() {
            _blnInRun = false;
        }

        private void startRun(int iSector, int iOffset) {
            // start a new run
            _iStartSector = iSector;
            _iStartOffset = iOffset;
            _iSoundUnitCount = 0;
            _blnInRun = true;
            _blnLastSoundUnit = false;
        }

        private void endRun() {
            assert _blnInRun;
            //if (_blnOnlyZeroes)
            //    System.out.println("break;");
            if (_iSoundUnitCount >= MIN_SOUND_UNIT_COUNT && !_blnOnlyZeroes) {
                // save the run
                addDiscItem(new DiscItemSpu(getCd(), _iStartSector, _iStartOffset,
                                            _iEndSector, _iEndOffset,
                                            _iSoundUnitCount));
                //System.out.format(
                //    "Found run sector %d offset %d to sector %d offset %d, %d sound units",
                //    _iStartSector, _iStartOffset, _iEndSector, _iEndOffset, _iSoundUnitCount)
                //.println();
            }
            _blnInRun = false;
        }

        public void clearRun() {
            _blnInRun = false;
        }

    }

    private final SpuRun[] _spuRuns = new SpuRun[4];
    private int _iRun = 0;

    public DiscIndexerSpu() {
        for (int i = 0; i < _spuRuns.length; i++) {
            _spuRuns[i] = new SpuRun();
        }
    }

    @Override
    public void feedSector(@Nonnull CdSector cdSector) {
        int iUserDataSize = cdSector.getCdUserDataSize();
        int iSector = cdSector.getSectorIndexFromStart();
        for (int iOfs = 0; iOfs < iUserDataSize; iOfs+=4) {
            if (iUserDataSize - iOfs < 4) {
                LOG.severe("WAT");
                throw new RuntimeException("SPU WIP");
                // end all runs
            }
            int iQuad = cdSector.readSInt32BE(iOfs);

            //final int X = 215376;
            //if (iSector == X / 2048 && iOfs == X % 2048)
            //    System.out.println("break");

            int iRun = _iRun;
            int iQuadIndex = 0;
            for (int i = 0; i < 4; i++) {
                _spuRuns[iRun].addQuad(iQuadIndex, iQuad, iSector, iOfs);
                iRun = (iRun + 1) % _spuRuns.length;
                iQuadIndex--;
                if (iQuadIndex < 0)
                    iQuadIndex = _spuRuns.length - 1;
            }
            _iRun = (_iRun + 1) % _spuRuns.length;
        }
    }

    @Override
    public void endOfUnidentified() {
        for (SpuRun run : _spuRuns) {
            run.clearRun();
        }
    }


}
