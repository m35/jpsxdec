/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

package jpsxdec.sectors;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode;
import jpsxdec.discitems.DemuxedStrFrame;
import jpsxdec.discitems.DiscItemStrVideoWithFrame;
import jpsxdec.discitems.FrameDemuxer;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_Lain;
import jpsxdec.psxvideo.mdec.MdecException.Read;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;

/** Judge Dredd video sector. 
 * <p>
 * Judge Dredd does not make video sector identification easy. Without
 * contextual information about the surrounding sectors, and access to the
 * ISO9660 file system, its impossible to uniquely determine if a sector really 
 * is a video sector.
 * <p>
 * So to properly identify them on the fly would require actively checking
 * the surrounding sectors and file-system. This would probably prevent
 * random-access sector reading.
 * <p>
 * To speed things up and simplify, generating that contextual meta-data could
 * be constructed when the disc is first loaded.
 * <p>
 * To speed it up and simplify it even more, that meta-data could be generated
 * once and stored. This is the approach I used. 
 * It also allows us to be able to identify Judge Dredd video sectors in 
 * the absence of any ISO9660 file system. Unfortunately it will prevent
 * identifying Judge Dredd sectors on other disc images (which probably has
 * happened). */
public class SectorDreddVideo extends SectorAbstractVideo implements IVideoSectorWithFrameNumber {

    private static final Logger LOG = Logger.getLogger(SectorDreddVideo.class.toString());

    /** Firsts sector of the first video frame on the Judge Dredd disc. */
    private static final int FIRST_VID_SECTOR = 2721;
    /** Last sector of the last video frame on the Judge Dredd disc. */
    private static final int LAST_VID_SECTOR = 318686;
    
    /** First sector where a "Type B" video sector occurs. */
    private static final int FIRST_SECTOR_TYPE_B = 230016;

    /** A sorted list of sectors containing Judge Dredd video data. */
    private static final int[] SECTOR_LIST;
    /** The corresponding frame numbers of the sectors found in {@link #SECTOR_LIST}. */
    private static final short[] FRAME_LIST;
    /** The corresponding chunk numbers of the sectors found in {@link #SECTOR_LIST}. */
    private static final byte[] CHUNK_LIST;
    
    static {
        // attempts to load the human reable textual sector lookup table first
        InputStream is = SectorDreddVideo.class.getResourceAsStream("SectorDreddVideo.log");
        if (is != null) {
            try {
                // allocate worst-case table sizes
                int[] aiSectorList = new int[LAST_VID_SECTOR - FIRST_VID_SECTOR];
                short[] asiFrameList = new short[aiSectorList.length];
                byte[] abChunkList = new byte[aiSectorList.length];
                // read and parse the lines and store the results in these
                // temporary tables
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String sLine;
                int iSize = 0;
                while ((sLine = reader.readLine()) != null) {
                    int[] aiParts = Misc.splitInt(sLine, "\t");
                    aiSectorList[iSize] = aiParts[0];
                    asiFrameList[iSize] = (short) aiParts[1];
                    abChunkList[iSize] = (byte) aiParts[2];
                    iSize++;
                }
                // now copy just the amount we need
                SECTOR_LIST = new int[iSize];
                FRAME_LIST = new short[iSize];
                CHUNK_LIST = new byte[iSize];
                System.arraycopy(aiSectorList, 0, SECTOR_LIST, 0, iSize);
                System.arraycopy(asiFrameList, 0, FRAME_LIST, 0, iSize);
                System.arraycopy(abChunkList, 0, CHUNK_LIST, 0, iSize);
            } catch (IOException ex) {
                throw new RuntimeException("Error loading Judge Dredd textual lookup table", ex);
            } finally {
                try { is.close(); } catch (IOException ex) {
                    Logger.getLogger(SectorDreddVideo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            // if it can't find the textual lookup table, 
            // it tries to load the binary lookup table
            is = SectorDreddVideo.class.getResourceAsStream("SectorDreddVideo.dat");
            if (is == null) {
                Logger.getLogger(SectorDreddVideo.class.getName()).severe("Unable to load Dredd lookup table.");
                SECTOR_LIST = null;
                FRAME_LIST = null;
                CHUNK_LIST = null;
            } else {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
                try {
                    final int iSize = dis.readInt();
                    SECTOR_LIST = new int[iSize];
                    FRAME_LIST = new short[iSize];
                    CHUNK_LIST = new byte[iSize];
                    short siFrame = 0;
                    byte bChunk = 0;
                    for (int i = 0; i < iSize; i++) {
                        int iSector = dis.readInt();
                        if ((iSector & 0x80000000) != 0) {
                            siFrame = 0;
                            bChunk = 0;
                            iSector &= ~0x80000000;
                        }
                        SECTOR_LIST[i] = iSector;
                        FRAME_LIST[i] = siFrame;
                        CHUNK_LIST[i] = bChunk;
                        bChunk++;
                        if (bChunk >= (iSector < FIRST_SECTOR_TYPE_B ? 9 : 10)) {
                            siFrame++;
                            bChunk = 0;
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    try { is.close(); } catch (IOException ex) {
                        Logger.getLogger(SectorDreddVideo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
    
    /** Frame number. */
    private int _iFrame;
    /** Frame chunk number. */
    private int _iChunk;
    /** Dredd frame height is either 352 or 240. */
    private int _iHeight;
    /** Dredd chunk count is either 9 or 10. */
    private int _iChunkCount;
    /** Dredd sector header size is either 4 or 44. */
    private int _iHeaderSize;

    public SectorDreddVideo(CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;
        
        // if the lookup table is unavailable, Dredd identification is impossible
        if (SECTOR_LIST == null)
            return;

        // Judge Dredd sectors have no frame or chunk count information
        // so it's nearly impossible to determine the frame number or 
        // chunk count without at least knowing where on the disc you are.
        // so we will be requiring the raw sector header to properly identify
        // these sectors
        if (!cdSector.hasHeaderSectorNumber() || !cdSector.hasSubHeader())
            return;

        if (cdSector.getSubHeaderFile() != 1 || cdSector.getSubHeaderChannel() != 2)
            return;

        if (cdSector.subModeMask(~SubMode.MASK_EOF_MARKER) != SubMode.MASK_DATA)
            return;

        int iSector = cdSector.getHeaderSectorNumber();

        if (iSector < FIRST_VID_SECTOR || iSector > LAST_VID_SECTOR)
            return;

        if (iSector < FIRST_SECTOR_TYPE_B) {
            // width is always 320
            _iHeight = 352;
            _iChunkCount = 9;
        } else {
            // width is always 320
            _iHeight = 240;
            _iChunkCount = 10;
        }

        _iChunk = cdSector.readSInt32LE(0);
        if (_iChunk < 0 || _iChunk >= _iChunkCount)
            return;

        // lookup the sector number in the table and fail if it's not found
        int iIndex = Arrays.binarySearch(SECTOR_LIST, iSector);
        if (iIndex < 0)
            return;

        if (_iChunk != CHUNK_LIST[iIndex])
            return;

        _iFrame = FRAME_LIST[iIndex];

        if (iSector < FIRST_SECTOR_TYPE_B && _iChunk == 0)
            _iHeaderSize = 44;
        else
            _iHeaderSize = 4;

        setProbability(50);
    }

    public String getTypeName() {
        return "Dredd";
    }

    @Override
    protected int getSectorHeaderSize() {
        return _iHeaderSize;
    }

    public int getChunkNumber() {
        return _iChunk;
    }

    public int getChunksInFrame() {
        return _iChunkCount;
    }

    public int getFrameNumber() {
        return _iFrame;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getWidth() {
        return 320;
    }

    /** {@inheritDoc}
     * <p>
     * For Judge Dredd sectors, returns {@link #SPLIT_XA_AUDIO_PREVIOUS} at the
     * start of new videos. */
    public int splitXaAudio() {
        return (getFrameNumber() == 0 && getChunkNumber() == 0) ?
            SPLIT_XA_AUDIO_PREVIOUS : SPLIT_XA_AUDIO_NONE;
    }

    @Override
    public String toString() {
        String s = String.format("%s %s frame:%d 320x%d chunk:%d/%d",
                getTypeName(), cdToString(), 
                _iFrame, _iHeight, _iChunk, _iChunkCount);
        if (_iHeaderSize == 4)
            return s;
        else
            return s + " + Unknown data";
    }

    // #########################################################################
    // #########################################################################
    // #########################################################################

    /** Run once with the path to the Judge Dredd disc image to generate
     * a large but human readable sector lookup table. Copy that table into this
     * class path and run a second time to generate an optimized binary lookup 
     * table. */
    public static void main(String[] args) throws IOException {

        if (SECTOR_LIST == null) {
            if (args.length < 1) {
                System.out.println("Need path to the Judge Dredd image.");
                return;
            }
            System.out.println("Generating textual lookup table.");
            buildTextLookup(args[0]);
        } else {
            System.out.println("Converting the loaded table to binary.");
            convertToBinary();
        }
    }
    
    /** Video sector used for generating the Judge Dredd sector lookup table. */
    private static class DummyDreddVid extends SectorAbstractVideo {

        private final int _iChunk, _iFrame;
        private final boolean _blnTypeA;

        public DummyDreddVid(CdSector cdSector, int iChunk, int iFrame, boolean blnTypeA) {
            super(cdSector);
            _iChunk = iChunk;
            _iFrame = iFrame;
            _blnTypeA = blnTypeA;
        }

        @Override
        public String toString() { return super.cdToString(); }

        public String getTypeName() { return "DreddDummy"; }

        @Override
        protected int getSectorHeaderSize() {
            if (_iChunk == 0 && _blnTypeA)
                return 44;
            else
                return 4;
        }

        public int getChunkNumber() { return _iChunk; }
        public int getChunksInFrame() { return _blnTypeA ? 9 : 10; }
        public int getFrameNumber() { return _iFrame; }
        public int getHeight() { return _blnTypeA? 352 : 240; }
        public int getWidth() { return 320; }

        public int splitXaAudio() {
            throw new UnsupportedOperationException("Should never be called.");
        }

    }

    /** Build a human-readable lookup table containing a list of every video 
     * sector plus its frame and chunk number.
     * <p>
     * The format is:
     * <pre>sector_num\tframe_num\tchunk_num</pre>
     */
    private static void buildTextLookup(String sDreddImage) throws IOException {

        //<editor-fold defaultstate="collapsed" desc="Start/end sector pairs">
        final int[] VIDEO_SECTORS = {
            // 9 chunks
            2721,   16319,
            16321,  21820,
            22084,  33023,
            33252,  51351,
            51483,  64432,
            64738,  72037,
            72349,  89048,
            89366,  104465,
            104686, 116585,
            116926, 135025,
            135365, 153464,
            153466, 163464,
            163799, 180298,
            180588, 197687,
            197957, 215056,
            215331, 229009,
            229131, 229261,
            // 10 chunks
            230016, 232749,
            232752, 238605,
            238608, 243298,
            243301, 252298,
            252301, 256727,
            256730, 275412,
            275415, 276553,
            276556, 291470,
            291473, 292214,
            292217, 293438,
            293441, 318686,
        };
        //</editor-fold>

        CdFileSectorReader cd = new CdFileSectorReader(new File(sDreddImage));

        final PrintStream out = new PrintStream("SectorDreddVideo.log");
        final int[] aiSkippedPossibleVidSectors = {0, 0};

        for (int iVideo = 0; iVideo < VIDEO_SECTORS.length; iVideo+=2) {
            final boolean blnIsTypeA = VIDEO_SECTORS[iVideo] < FIRST_SECTOR_TYPE_B;
            
            FrameDemuxer demux = new DiscItemStrVideoWithFrame.Demuxer(VIDEO_SECTORS[iVideo], VIDEO_SECTORS[iVideo+1],
                                                                       320, blnIsTypeA ? 352 : 240);
            demux.setFrameListener(new ISectorFrameDemuxer.ICompletedFrameListener() {
                public void frameComplete(IDemuxedFrame f) throws IOException {
                    String ok = validateFrame(f.copyDemuxData(null), blnIsTypeA);
                    if (ok != null) {
                        for (int iChunk = 0; iChunk < (blnIsTypeA ? 9 : 10); iChunk++) {
                            DummyDreddVid vid = (DummyDreddVid) ((DemuxedStrFrame)f).getChunk(iChunk);
                            if (vid == null)
                                continue;
                            if (ok.length() > 0)
                                aiSkippedPossibleVidSectors[1]++;
                            out.println(String.format(ok + "%d\t%d\t%d",
                                    vid.getCdSector().getHeaderSectorNumber(),
                                    vid.getFrameNumber(),
                                    iChunk));
                        }
                    } else {
                        aiSkippedPossibleVidSectors[0]++;
                    }
                }
            });

            int iFrame = -1, iLastChunk = 99;
            for (int iSector = VIDEO_SECTORS[iVideo]; iSector <= VIDEO_SECTORS[iVideo+1]; iSector++) {
                CdSector sect = cd.getSector(iSector);
                if (sect.getHeaderSectorNumber() != iSector)
                    throw new RuntimeException();
                SectorXaAudio xa = new SectorXaAudio(sect);
                if (xa.getProbability() == 100)
                    continue;
                if (xa.getProbability() > 0)
                    throw new RuntimeException();
                if (sect.getErrorCount() > 0)
                    continue;
                int iChunk = sect.readSInt32LE(0);
                if (iChunk < 0 || iChunk >= (blnIsTypeA ? 9 : 10))
                    continue;

                if (iChunk < iLastChunk)
                    iFrame++;
                else if(iChunk == iLastChunk) {
                    demux.flush(LOG);
                    iFrame++;
                }
                iLastChunk = iChunk;

                demux.feedSector(new DummyDreddVid(sect, iChunk, iFrame, blnIsTypeA), LOG);
            }
            demux.flush(LOG);
        }
        out.close();
        System.out.println(aiSkippedPossibleVidSectors[0] + " potential video sectors failed to generate valid frames");
        System.out.println(aiSkippedPossibleVidSectors[1] + " sectors need reviwing in the output (search for '?')");
    }

    /** Make sure the frame data actually generates a valid frame. 
     * @return "" for a keeper, null if to discard, 
     *         or "?" if needs to be checked after generation */
    private static String validateFrame(byte[] abFrame, boolean blnTypeA) {
        char cType = analyzeFrameContent(abFrame);
        if (cType == 'n')
            return null;

        BitStreamUncompressor uncom = BitStreamUncompressor.identifyUncompressor(abFrame);
        if (uncom == null) {
            return null;
        } else if (uncom instanceof BitStreamUncompressor_Lain) {
            return null;
        }
        try {
            uncom.reset(abFrame, abFrame.length);
            uncom.readToEnd(320, blnTypeA ? 352 : 240);
        } catch (Read ex) {
            return "?";
        } catch (NotThisTypeException ex) {
            throw new RuntimeException(ex);
        }
        return "";
    }

    /** Check the frame content for generally invalid data. 
     * @return "" if worth keeping. */
    private static char analyzeFrameContent(byte[] abFrame) {
        int iConsecutiveZeroCount = 0;
        int iMaxConsecutiveZero = 0;
        int iNegitive1Count = 0;
        for (int i = 0; i < abFrame.length; i++) {
            if (abFrame[i] == 0) {
                iConsecutiveZeroCount++;
            } else {
                if (iConsecutiveZeroCount > iMaxConsecutiveZero)
                    iMaxConsecutiveZero = iConsecutiveZeroCount;
                iConsecutiveZeroCount = 0;
                if (abFrame[i] == -1) {
                    iNegitive1Count++;
                }
            }
        }
        if (iMaxConsecutiveZero == abFrame.length - 4)
            return 'z';
        else if (iNegitive1Count == abFrame.length)
            return 'n';
        else if (iMaxConsecutiveZero > 5)
            return 'x';
        else
            return ' ';
    }

    //##########################################################################
    
    /** Converts the loaded sector lookup tables {@link #SECTOR_LIST} to a
     * single binary table for fastest loading. 
     * <p>
     * The format is:
     * <pre>4 bytes: number of entries in the table
     * For each entry:
     *    4 bytes: sector number with the high bit set if it is the start of a video
     * </pre>
     */
    private static void convertToBinary() throws IOException {
        int iLastFrame = Integer.MAX_VALUE;
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream("SectorDreddVideo.dat"));
            dos.writeInt(SECTOR_LIST.length);
            for (int i = 0; i < SECTOR_LIST.length; i++) {
                int iSector = SECTOR_LIST[i];
                if (FRAME_LIST[i] < iLastFrame)
                    iSector |= 0x80000000;
                dos.writeInt(iSector);
                iLastFrame = FRAME_LIST[i];
            }
        } finally {
            if (dos != null)
                dos.close();
        }
    }
}
