/*
 * LainTools: PSX Serial Experiments Lain Hacking and Translation Tools
 * Copyright (C) 2011  Michael Sabin
 *
 * Redistribution and use of the LainTools code or any derivative works are
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

package laintools;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import jpsxdec.tim.Tim;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.BinaryDataNotRecognized;

/** Data structures found in the game executable SLPS_016.03 or SLPS_016.04. */
public class LainDataStructures {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Expecting 3 or 4 parameters: <path to SLPS_016.03 or SLPS_016.04> <path to SITEA.BIN> <path to SITEB.BIN> [images]");
            return;
        }
        boolean blnDumpImages = false;
        if (args.length == 4) {
            if (!args[3].equals("images")) {
                System.out.println("Optional 4th parameter must be 'images'");
                return;
            }
            blnDumpImages = true;
        }

        dumpTsvNodeTableAndStringsAndImages(args[0], args[1], args[2], blnDumpImages);
    }

    /**
     * Dumps all the Lain data structures in the form of 2 files:
     * NODE_TABLE.tsv and STRING_TABLE.tsv (tsv = tab separated values).
     * Optionally also dump all the images displayed when listening to
     * audio clips. The file names will be {@code <site>\<node id>-#.png} for
     * example "A\Tou001-1.png"
     *
     * @param SLPS_016_0x_path Path to SLPS_016.03 or SLPS_016.04 file
     * @param SITEA_BIN_path Path to SITEA.BIN
     * @param SITEB_BIN_path Path to SITEB.BIN
     * @param blnDumpImages Also dump all the images displayed when listening to
     *                      audio clips
     */
    public static void dumpTsvNodeTableAndStringsAndImages(String SLPS_016_0x_path, String SITEA_BIN_path, String SITEB_BIN_path, boolean blnDumpImages) throws Exception {

        NodeTable nodeTable = new NodeTable(SLPS_016_0x_path, SITEA_BIN_path, SITEB_BIN_path);

        nodeTable.toTsv("NODE_TABLE.tsv");
        nodeTable._stringTable.toTsv("STRING_TABLE.tsv");
        if (blnDumpImages) {
            nodeTable._siteImageTableA.dumpImages(new File("A"));
            nodeTable._siteImageTableB.dumpImages(new File("B"));
        }
    }

    /** Core data structure of the game. */
    public static class NodeTable extends AbstractList<NodeTableItem> {

        public final static int SLPS_016_0x_NODE_TABLE_START = 411544;

        private final NodeTableItem[] _nodeList = new NodeTableItem[716];
        public final SiteImageTable _siteImageTableA;
        public final SiteImageTable _siteImageTableB;
        
        private final EnvEquivalenceTables _envEquivalenceTables;
        private final FileTable _fileTable;

        private final MediaFileTable _mediaTable;
        
        private final StringTable _stringTable;

        public NodeTable(String sSlps, String sSiteA, String sSiteB) throws IOException {

            RandomAccessFile slps0160xRaf = new RandomAccessFile(sSlps, "r");
            _siteImageTableA = new SiteImageTable(slps0160xRaf, true, new RandomAccessFile(sSiteA, "r"));
            _siteImageTableB = new SiteImageTable(slps0160xRaf, false, new RandomAccessFile(sSiteB, "r"));

            _envEquivalenceTables = new EnvEquivalenceTables(slps0160xRaf);

            _fileTable = new FileTable(slps0160xRaf);

            _mediaTable = new MediaFileTable(slps0160xRaf, _fileTable);

            _stringTable = new StringTable(slps0160xRaf);

            slps0160xRaf.seek(SLPS_016_0x_NODE_TABLE_START);
            for (int i = 0; i < _nodeList.length; i++) {
                NodeTableItem nodeItem = new NodeTableItem(i, slps0160xRaf, _siteImageTableA, _siteImageTableB, _envEquivalenceTables, _mediaTable, _stringTable);
                _nodeList[i] = nodeItem;
            }
            
            slps0160xRaf.close();
        }

        @Override
        public NodeTableItem get(int index) {
            return _nodeList[index];
        }

        @Override
        public int size() {
            return _nodeList.length;
        }

        public void print(PrintStream out) {
            _siteImageTableA.print(out);
            out.println();
            _siteImageTableB.print(out);
            out.println();
            _stringTable.print(out);
            out.println();
            _fileTable.print(out);
            out.println();
            _mediaTable.print(out);
            out.println();
            out.println("Env image equivalents (SiteA=SiteB)");
            _envEquivalenceTables.print(out);
            out.println();
            for (NodeTableItem nodeTableItem : _nodeList) {
                out.println(nodeTableItem);
            }
        }
        
        public void toTsv(String sFile) throws FileNotFoundException {
            PrintStream out = new PrintStream(sFile);
            NodeTableItem.toTsvHeader(out);
            for (NodeTableItem nodeTableItem : _nodeList) {
                nodeTableItem.toTsv(this, out);
            }
            out.close();
        }
    }


    public static class NodeTableItem implements Comparable<NodeTableItem> {
        public static final int SIZEOF = 40;

        private final int _iIndex;
        private final long _lngAt;

        // Size
        /* 8     */ private final String _sNodeName;
        /* 2     */ private final int _iProtocolLine1StringTableIndex417;
        /* 2     */ private final int _iProtocolLine2StringTableIndex417;
        /* 2     */ private final int _iProtocolLine3StringTableIndex417;
        /* 2     */ private final int _iProtocolLine4StringTableIndex417;
        /* 2     */ private final int _iWord1StringTableIndex;
        /* 2     */ private final int _iWord2StringTableIndex;
        /* 2     */ private final int _iWord3StringTableIndex;
        /* 2,2,2 */ private final int[] _aiSiteImageTableIndexes = new int[3];
        /* 2     */ private final int _iMediaFileTableIndex;
        /* 2     */ private final int _iSite_Level_Position;
        /* 2     */ private final int _iZero1;
        /* 1     */ private final short _siType_UpgradeReq;
        /* 1     */ private final short _siGameStateBitFlags;
        /* Bits
           0: Is currently visible
           1: Is visible in new game
           2: Has been viewed
           3: Triggers final video after
           4-7: Number of times the final video must be viewed before this node can be opened
        */
        /* 2     */ private final int _iUnlockedByNodeTableIndex;
        /* 2     */ private final int _iZero2;
        
        private final MediaFileTableItem _mediaFile;

        private int[] _aiEnvSiteBTableIndexes;

        private final SiteImageTable _siteImageTable;

        private final StringTable _stringTable;

        public NodeTableItem(int iIndex, RandomAccessFile slps0160xRaf,
                             SiteImageTable siteTableA, SiteImageTable siteTableB,
                             EnvEquivalenceTables envEquivalenceTables, 
                             MediaFileTable mediaTable, StringTable stringTable)
                 throws IOException
        {
            _iIndex = iIndex;
            _lngAt = slps0160xRaf.getFilePointer();

            _sNodeName = readName(slps0160xRaf);
            _iProtocolLine1StringTableIndex417 = IO.readSInt16LE(slps0160xRaf);
            _iProtocolLine2StringTableIndex417 = IO.readSInt16LE(slps0160xRaf);
            _iProtocolLine3StringTableIndex417 = IO.readSInt16LE(slps0160xRaf);
            _iProtocolLine4StringTableIndex417 = IO.readSInt16LE(slps0160xRaf);
            _iWord1StringTableIndex = IO.readSInt16LE(slps0160xRaf);
            _iWord2StringTableIndex = IO.readSInt16LE(slps0160xRaf);
            _iWord3StringTableIndex = IO.readSInt16LE(slps0160xRaf);
            _aiSiteImageTableIndexes[0] = IO.readSInt16LE(slps0160xRaf);
            _aiSiteImageTableIndexes[1] = IO.readSInt16LE(slps0160xRaf);
            _aiSiteImageTableIndexes[2] = IO.readSInt16LE(slps0160xRaf);
            _iMediaFileTableIndex = IO.readSInt16LE(slps0160xRaf);
            _iSite_Level_Position = IO.readSInt16LE(slps0160xRaf);
            _iZero1 = IO.readSInt16LE(slps0160xRaf);
            _siType_UpgradeReq = (short) slps0160xRaf.readUnsignedByte();
            _siGameStateBitFlags = (short) slps0160xRaf.readUnsignedByte();
            _iUnlockedByNodeTableIndex = IO.readSInt16LE(slps0160xRaf);
            _iZero2 = IO.readSInt16LE(slps0160xRaf);
            
            _mediaFile = mediaTable.get(_iMediaFileTableIndex);

            if (_iZero1 != 0)
                throw new RuntimeException("Zero1=" + _iZero1);
            if (_iZero2 != 0)
                throw new RuntimeException("Zero2=" + _iZero2);

            // sanity check
            if ((_siGameStateBitFlags & 1) != ((_siGameStateBitFlags >> 1) & 1))
                throw new RuntimeException(Misc.bitsToString(_siGameStateBitFlags, 8) + " last 2 bits should be equal");

            if (isSiteA()) {
                _siteImageTable = siteTableA;
                if (_sNodeName.startsWith("Env")) {
                    _aiEnvSiteBTableIndexes = new int[3];
                    _aiEnvSiteBTableIndexes[0] = envEquivalenceTables.aToB(_aiSiteImageTableIndexes[0]);
                    _aiEnvSiteBTableIndexes[1] = envEquivalenceTables.aToB(_aiSiteImageTableIndexes[1]);
                    _aiEnvSiteBTableIndexes[2] = envEquivalenceTables.aToB(_aiSiteImageTableIndexes[2]);
                }
            } else {
                _siteImageTable = siteTableB;
            }

            for (int i : _aiSiteImageTableIndexes) {
                if (i >= 0) {
                    _siteImageTable.get(i).addOwnedBy(this);
                }
            }
            if (_aiEnvSiteBTableIndexes != null) {
                for (int i : _aiEnvSiteBTableIndexes) {
                    if (i >= 0) {
                        siteTableB.get(i).addOwnedBy(this);
                    }
                }
            }

            _stringTable = stringTable;
        }

        private static String readName(RandomAccessFile slps0160xRaf) throws IOException {
            byte[] abNTS = IO.readByteArray(slps0160xRaf, 8);
            int iLen = 0;
            for (; iLen < abNTS.length; iLen++) {
                if (abNTS[iLen] == 0)
                    break;
            }
            return Misc.asciiToString(abNTS, 0, iLen);
        }

        final public boolean isSiteA() {
            // additional santiy check
            boolean blnMustBeSiteA = (_aiSiteImageTableIndexes[0] >= SiteImageTable.SITEB_TABLE_COUNT) ||
                                     (_aiSiteImageTableIndexes[1] >= SiteImageTable.SITEB_TABLE_COUNT) ||
                                     (_aiSiteImageTableIndexes[2] >= SiteImageTable.SITEB_TABLE_COUNT);
            boolean blnMightBeSiteA = (_iSite_Level_Position & 0x8000) == 0;
            if (blnMustBeSiteA && !blnMightBeSiteA)
                throw new RuntimeException("Site A is not site A " + _iIndex);
            return blnMightBeSiteA;
        }

        public int getPosition() {
            int iPos = _iSite_Level_Position & 0x7fff;
            return (iPos - 8) % 24;
        }
        public int getPositionX() {
            return getPosition() % 8;
        }
        public int getPositionY() {
            return getPosition() / 8;
        }
        
        public int getLevel() {
            int iPos = _iSite_Level_Position & 0x7fff;
            return (iPos - 8) / 24 + 1;
        }
        
        public int getSiteTableIndex(int i) {
            return _aiSiteImageTableIndexes[i];
        }

        public int getProtocolLine1StringTableIndex() {
            return _iProtocolLine1StringTableIndex417 + 417;
        }
        public String getProtocolLine1String() {
            return _stringTable.getString(getProtocolLine1StringTableIndex());
        }

        public int getProtocolLine2StringTableIndex() {
            return _iProtocolLine2StringTableIndex417 + 417;
        }
        public String getProtocolLine2String() {
            return _stringTable.getString(getProtocolLine2StringTableIndex());
        }

        public int getProtocolLine3StringTableIndex() {
            return _iProtocolLine3StringTableIndex417 + 417;
        }
        public String getProtocolLine3String() {
            return _stringTable.getString(getProtocolLine3StringTableIndex());
        }

        public int getProtocolLine4StringTableIndex() {
            return _iProtocolLine4StringTableIndex417 + 417;
        }
        public String getProtocolLine4String() {
            return _stringTable.getString(getProtocolLine4StringTableIndex());
        }

        public String getWord1() {
            return _iWord1StringTableIndex >= 0 ? _stringTable.getString(_iWord1StringTableIndex) : null;
        }

        public String getWord2() {
            return _iWord2StringTableIndex >= 0 ? _stringTable.getString(_iWord2StringTableIndex) : null;
        }

        public String getWord3() {
            return _iWord3StringTableIndex >= 0 ? _stringTable.getString(_iWord3StringTableIndex) : null;
        }

        public String getName() {
            return _sNodeName;
        }

        public BufferedImage getImage(int i) throws IOException {
            return _aiSiteImageTableIndexes[i] >= 0 ? _siteImageTable.getImage(_aiSiteImageTableIndexes[i]) : null;
        }
        
        public int getType() {
            return _siType_UpgradeReq >> 4;
        }

        public int getUpgradeReq() {
            return _siType_UpgradeReq & 0xf;
        }

        public int getIndex() {
            return _iMediaFileTableIndex;
        }

        public boolean isCurrentlyVisible() {
            return (_siGameStateBitFlags & 1) != 0;
        }

        public boolean isVisibleInNewGame() {
            return (_siGameStateBitFlags & 2) != 0;
        }

        public boolean hasBeenViewed() {
            return (_siGameStateBitFlags & 4) != 0;
        }

        public boolean triggersFinalVideo() {
            return (_siGameStateBitFlags & 8) != 0;
        }

        public int getRequiredFinalVideoViewings() {
            return (_siGameStateBitFlags >> 4) & 0xf;
        }

        // ---------------------------------------------------------------------

        public int getEnvTableIndex(int i) {
            if (_aiEnvSiteBTableIndexes == null)
                return -1;
            else
                return _aiEnvSiteBTableIndexes[i];
        }

        // ---------------------------------------------------------------------
        @Override
        public int compareTo(NodeTableItem o) {
            Integer i = Integer.valueOf(_iIndex);
            return i.compareTo(o._iIndex);
        }
        @Override
        public boolean equals(Object obj) {
            return (obj instanceof NodeTableItem) && compareTo((NodeTableItem) obj) == 0;
        }
        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }
        // ---------------------------------------------------------------------

        private static final String[] TABLE_HEADER = {
            "Index",
            "SLPS_016_0x offset",

            "Node Name",
            "Protocol Line 1 (string table index)",
            "Protocol Line 2 (string table index)",
            "Protocol Line 3 (string table index)",
            "Protocol Line 4 (string table index)",
            "Word 1 (string table index)",
            "Word 2 (string table index)",
            "Word 3 (string table index)",
            "Image Table Index 1",
            "Image Table Index 2",
            "Image Table Index 3",
            "Media File",
            "Site",
            "Level",
            "Position X",
            "Position Y",
            "Type",
            "SSkn Upgrade Requirement",
            "Is Visible",
            "Triggers Final Video",
            "Required Final Video Viewings",
            "Unlocked By Index",
        };

        public static void toTsvHeader(PrintStream out) {
            out.println(Misc.join(TABLE_HEADER, "\t"));
        }

        public void toTsv(NodeTable nodeTable, PrintStream out) {
            Object ao[] = {
                _iIndex,
                _lngAt,

                _sNodeName,
                getProtocolLine1StringTableIndex(),
                getProtocolLine2StringTableIndex(),
                getProtocolLine3StringTableIndex(),
                getProtocolLine4StringTableIndex(),
                _iWord1StringTableIndex,
                _iWord2StringTableIndex,
                _iWord3StringTableIndex,
                _aiSiteImageTableIndexes[0],
                _aiSiteImageTableIndexes[1],
                _aiSiteImageTableIndexes[2],
                _mediaFile.getFile(),
                isSiteA() ? "A" : "B",
                getLevel(),
                getPositionX(),
                getPositionY(),
                getType(),
                getUpgradeReq(),
                isVisibleInNewGame(),
                triggersFinalVideo(),
                getRequiredFinalVideoViewings(),
                _iUnlockedByNodeTableIndex,
            };
            if (ao.length != TABLE_HEADER.length)
                throw new RuntimeException();
            out.println(Misc.join(ao, "\t"));
        }

        @Override
        public String toString() {
            String s = String.format(
                "%d: @%d %s Flags %s %s MediaTblIdx %d (%s) Level %d.%d Imgs(%d, %d, %d) Words(%d=%s, %d=%s, %d=%s) UnlockedBy %d Type %d Upgrade %d Protocol(%d=%s %d=%s %d=%s %d=%s)",
                _iIndex,
                _lngAt,
                _sNodeName,
                Misc.bitsToString(_siGameStateBitFlags, 8) ,
                isSiteA() ? "SiteA" : "SiteB",
                _iMediaFileTableIndex,
                _mediaFile,
                getLevel(),
                getPosition(),
                _aiSiteImageTableIndexes[0],
                _aiSiteImageTableIndexes[1],
                _aiSiteImageTableIndexes[2],
                _iWord1StringTableIndex, getWord1(),
                _iWord2StringTableIndex, getWord2(),
                _iWord3StringTableIndex, getWord3(),
                _iUnlockedByNodeTableIndex,
                getType(),
                getUpgradeReq(),
                getProtocolLine1StringTableIndex(), getProtocolLine1String(),
                getProtocolLine2StringTableIndex(), getProtocolLine2String(),
                getProtocolLine3StringTableIndex(), getProtocolLine3String(),
                getProtocolLine4StringTableIndex(), getProtocolLine4String()
            );
            if (_aiEnvSiteBTableIndexes == null)
                return s;
            else
                return s + String.format(" SiteB Imgs(%d, %d, %d)",
                        _aiEnvSiteBTableIndexes[0],
                        _aiEnvSiteBTableIndexes[1],
                        _aiEnvSiteBTableIndexes[2]);
        }

    }

    // ========================================================================


   public static class SiteImageTable {

        private static final int SITEA_TABLE_OFFSET = 568416;
        private static final int SITEA_TABLE_COUNT = 790;

        private static final int SITEB_TABLE_OFFSET = 574736;
        private static final int SITEB_TABLE_COUNT = 558;

        private final SiteImageTableItem[] _siteList;

        private final char _cSite;

        public SiteImageTable(RandomAccessFile slps0160xRaf, boolean blnSiteA, RandomAccessFile site) throws IOException {
            if (blnSiteA) {
                slps0160xRaf.seek(SITEA_TABLE_OFFSET);
                _siteList = new SiteImageTableItem[SITEA_TABLE_COUNT];
                _cSite = 'A';
            } else {
                slps0160xRaf.seek(SITEB_TABLE_OFFSET);
                _siteList = new SiteImageTableItem[SITEB_TABLE_COUNT];
                _cSite = 'B';
            }

            for (int i = 0; i < _siteList.length; i++) {
                _siteList[i] = new SiteImageTableItem(slps0160xRaf, site, i);
            }
        }

        public BufferedImage getImage(int index) throws IOException {
            return _siteList[index].readImg();
        }

        public SiteImageTableItem get(int index) {
            return _siteList[index];
        }

        public int size() {
            return _siteList.length;
        }

        public String getSite() {
            return "Site" + _cSite;
        }

        public void print(PrintStream ps) {
            ps.println("Site " + _cSite + " " + _siteList.length + " items");
            for (int i = 0; i < _siteList.length; i++) {
                ps.println(_siteList[i]);
            }
        }

        public void dumpImages(File directory) throws IOException {
            for (SiteImageTableItem item : _siteList) {
                String sFile = String.format("Site%c-%03d.png", _cSite, item._iIndex);
                BufferedImage bi = item.readImg();
                File file = new File(directory, sFile);
                if (!ImageIO.write(bi, "png", file))
                    throw new RuntimeException();
            }
        }
    }

   public static class SiteImageTableItem implements Comparable<SiteImageTableItem> {
        public static final int SIZEOF = 8;

        // Size
        /* 4 */ private final long _lngSectorOffset;
        /* 4 */ private final int _iDataSize;

        private final int _iIndex;
        private final boolean _blnUncompressed;
        private final ArrayList<NodeTableItem> _ownedBy = new ArrayList<NodeTableItem>();

        private final RandomAccessFile _siteRaf;

        public SiteImageTableItem(RandomAccessFile slps0160xRaf, RandomAccessFile siteRaf, int iIndex) throws IOException {
            _lngSectorOffset = IO.readUInt32LE(slps0160xRaf);
            _iDataSize = IO.readSInt32LE(slps0160xRaf);
            if (_iDataSize < 1)
                throw new RuntimeException();

            _siteRaf = siteRaf;
            _iIndex = iIndex;

            // check if image is compressed
            _siteRaf.seek(_lngSectorOffset * 2048);
            byte[] ab = IO.readByteArray(_siteRaf, 4);
            if (Misc.asciiToString(ab).equals(BINextrator.NAPK)) {
                _blnUncompressed = false;
            } else if (Arrays.equals(ab, BINextrator.TIM_MAGIC)) {
                _blnUncompressed = true;
            } else {
                throw new RuntimeException("Bad type");
            }

        }

        public String toString() {
            return String.format("%d: %s Offset %d (%d) Size %d %s",
                    _iIndex,
                    _blnUncompressed ? "Uncompressed" : "Compressed",
                    _lngSectorOffset,
                    _lngSectorOffset * 2048,
                    _iDataSize,
                    _ownedBy
                    );
        }

        public int getIndex() {
            return _iIndex;
        }

        public List<NodeTableItem> ownedBy() {
            return _ownedBy;
        }
        
        public boolean isUncompressed() {
            return _blnUncompressed;
        }

        // .....................

        public byte[] readRaw() throws IOException {
            _siteRaf.seek(_lngSectorOffset * 2048);
            return IO.readByteArray(_siteRaf, _iDataSize);
        }

        public Tim readTim() throws IOException {

            try {
                byte[] abTim;
                if (_blnUncompressed) {
                    abTim = readRaw();
                } else {
                    _siteRaf.seek(_lngSectorOffset * 2048 + 4);
                    abTim = Lain_Pk.decompress(_siteRaf);
                }
                return Tim.read(new ByteArrayInputStream(abTim));
            } catch (BinaryDataNotRecognized e) {
                throw new RuntimeException(e);
            }
        }

        public BufferedImage readImg() throws IOException {
            return readTim().toBufferedImage(0);
        }

        // ...............

        public void addOwnedBy(NodeTableItem nodeItem) {
            _ownedBy.add(nodeItem);
        }

        public int compareTo(SiteImageTableItem o) {
            Integer i = Integer.valueOf(_iIndex);
            return i.compareTo(o._iIndex);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SiteImageTableItem other = (SiteImageTableItem) obj;
            if (this._iIndex != other._iIndex) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + this._iIndex;
            return hash;
        }

    }

    // ========================================================================

    public static class StringTable {

        public static final int COUNT = 234;//209;
        public static final int SIZEOF = StringTableItem.SIZEOF * COUNT;
        private static final int STRING_INDEX_TABLE_START = 409672;

        private final StringTableItem[] _aoStrings = new StringTableItem[COUNT];

        public StringTable(RandomAccessFile slps0160xRaf) throws IOException {
            for (int i = 0; i < COUNT; i++) {
                slps0160xRaf.seek(STRING_INDEX_TABLE_START +  i * StringTableItem.SIZEOF);
                _aoStrings[i] = new StringTableItem(slps0160xRaf, i);
            }
            for (StringTableItem item : _aoStrings) {
                item.loadStrings(slps0160xRaf);
            }
        }

        public void print(PrintStream out) {
            for (StringTableItem item : _aoStrings) {
                item.print(out);
            }
        }
        
        public String getString(int i) {
            StringTableItem item = _aoStrings[i/2];
            if ((i % 2) == 0)
                return item._sString1;
            else
                return item._sString2;
        }

        public void toTsv(String sFile) throws FileNotFoundException {
            PrintStream out = new PrintStream(sFile);
            StringTableItem.toTsvHeader(out);
            for (StringTableItem str : _aoStrings) {
                str.toTsv(out);
            }
            out.close();
        }

    }

    public static class StringTableItem {

        public static final int SIZEOF = 8;
        
        private final static int PSX_RAM_SLPS0160x_FILE_DIFF = 63488;
        
        private final int _iIndex;

        // Size
                /** Address to the string in PSX ram. */
        /* 4 */ private final long _lngString1Offset;
                /** Address to the string in PSX ram. */
        /* 4 */ private final long _lngString2Offset;
        
        private String _sString1;
        private String _sString2;

        public StringTableItem(RandomAccessFile slps0160xRaf, int iIndex) throws IOException {
            _iIndex = iIndex;
            _lngString1Offset = IO.readUInt32LE(slps0160xRaf);
            _lngString2Offset = IO.readUInt32LE(slps0160xRaf);
        }
        
        public void loadStrings(RandomAccessFile slps0160xRaf) throws IOException {
            _sString1 = loadString(slps0160xRaf, _lngString1Offset);
            _sString2 = loadString(slps0160xRaf, _lngString2Offset);
        }
        
        private String loadString(RandomAccessFile slps0160xRaf, long lngPsxRamPos) throws IOException {
            slps0160xRaf.seek(psx2file(lngPsxRamPos));
            StringBuilder sb = new StringBuilder();
            int b;
            while (sb.length() <= 25 && (b = slps0160xRaf.read()) != 0) {
                if (b < 32 || b > 128)
                    throw new RuntimeException(b+" shouldn't happen for " + _iIndex);
                sb.append((char)b);
            }
            return sb.toString();
        }

        /**  Convert PSX ram address to location in SLPS_016.0x file. */
        private static int psx2file(long lngPsxRamPos) {
            return (int)((lngPsxRamPos & ~0x80000000) - PSX_RAM_SLPS0160x_FILE_DIFF);
        }
        
        public String toString() {
            return String.format("@%d:%s @%d:%s", psx2file(_lngString1Offset), _sString1, psx2file(_lngString2Offset), _sString2);
        }
        
        public void print(PrintStream out) {
            out.println((_iIndex*2)+": "+_sString1+" @"+psx2file(_lngString1Offset));
            out.println((_iIndex*2+1)+": "+_sString2+" @"+psx2file(_lngString2Offset));
        }

        public static void toTsvHeader(PrintStream out) {
            out.println("Index\tString");
        }

        public void toTsv(PrintStream out) {
            out.println((_iIndex*2)+"\t"+_sString1);
            out.println((_iIndex*2+1)+"\t"+_sString2);
        }
    }

   
    // ========================================================================

    public static class EnvEquivalenceTables {
        public static final int COUNT = 10;
        public static final int SIZEOF = EnvEquivalenceTableItem.SIZEOF * COUNT;

        private static final int SITE_A_TABLE_START = 408056;
        private static final int SITE_B_TABLE_START = 408116;

        private final EnvEquivalenceTableItem[] _aoSiteA = new EnvEquivalenceTableItem[COUNT];
        private final EnvEquivalenceTableItem[] _aoSiteB = new EnvEquivalenceTableItem[COUNT];

        public EnvEquivalenceTables(RandomAccessFile slps0160xRaf) throws IOException {
            
            slps0160xRaf.seek(SITE_A_TABLE_START);
            for (int i = 0; i < _aoSiteA.length; i++) {
                _aoSiteA[i] = new EnvEquivalenceTableItem(slps0160xRaf);
            }

            slps0160xRaf.seek(SITE_B_TABLE_START);
            for (int i = 0; i < _aoSiteB.length; i++) {
                _aoSiteB[i] = new EnvEquivalenceTableItem(slps0160xRaf);
            }
        }

        public void print(PrintStream out) {
            for (int i = 0; i < COUNT; i++) {
                out.format("%d=%d, %d=%d, %d=%d",
                        _aoSiteA[i]._aiSiteIndex[0], _aoSiteB[i]._aiSiteIndex[0],
                        _aoSiteA[i]._aiSiteIndex[1], _aoSiteB[i]._aiSiteIndex[1],
                        _aoSiteA[i]._aiSiteIndex[2], _aoSiteB[i]._aiSiteIndex[2])
                        .println();
            }
        }

        public int aToB(int iSiteAIndex) {
            for (int i = 0; i < _aoSiteA.length; i++) {
                for (int j = 0; j < _aoSiteA[i]._aiSiteIndex.length; j++) {
                    if (_aoSiteA[i]._aiSiteIndex[j] == iSiteAIndex)
                        return _aoSiteB[i]._aiSiteIndex[j];
                }
            }
            return -1;
        }
    }

    public static class EnvEquivalenceTableItem {
        public static final int SIZEOF = 6;

        private final int[] _aiSiteIndex = new int[3];

        public EnvEquivalenceTableItem(RandomAccessFile slps0160xRaf) throws IOException {
            _aiSiteIndex[0] = IO.readSInt16LE(slps0160xRaf);
            _aiSiteIndex[1] = IO.readSInt16LE(slps0160xRaf);
            _aiSiteIndex[2] = IO.readSInt16LE(slps0160xRaf);
        }
    }

    // -------------------------------------------------------------------------

    public static class FileTable  {
        private static final long FILE_TABLE_START = 401016;

        private static final long ALT_SITEA_TABLE_START = 400664;

        private static final int FILE_COUNT = SITEA_FILES.size() + SITEB_FILES.size();
        private static final int SITEB_START = SITEA_FILES.size();

        private final FileTableItem[] _aoItems;

        public FileTable(RandomAccessFile slps0160xRaf) throws IOException {
            slps0160xRaf.seek(FILE_TABLE_START);
            _aoItems = new FileTableItem[FILE_COUNT];
            for (int i = 0; i < _aoItems.length; i++) {
                _aoItems[i] = new FileTableItem(i, slps0160xRaf);
            }
        }

        public void print(PrintStream out) {
            for (FileTableItem xaFileTableItem : _aoItems) {
                out.println(xaFileTableItem);
            }
        }

        public FileTableItem get(int i) {
            return _aoItems[i];
        }
    }

    public static class FileTableItem {
        public static final int SIZEOF = 8;

        // Size
        /* 4 */ private final long _lngStartSector;
        /* 4 */ private final long _lngSize;

        private final int _iIndex;

        public FileTableItem(int iIndex, RandomAccessFile slps0160xRaf) throws IOException {
            _iIndex = iIndex;
            _lngStartSector = IO.readUInt32LE(slps0160xRaf);
            _lngSize = IO.readUInt32LE(slps0160xRaf);
        }
        
        public LainFile getFile() {
            return _iIndex < FileTable.SITEB_START ? SITEA_FILES.get((int)_lngStartSector)
                                                   : SITEB_FILES.get((int)_lngStartSector);
        }

        public String toString() {
            return String.format("%d: Site%s Sector start %d Size %d (%s)",
                    _iIndex, _iIndex < FileTable.SITEB_START ? "A" : "B",
                    _lngStartSector, _lngSize,
                    getFile()
                    );
        }
    }

    //--------------------------------------------------------------------------

    public static class MediaFileTable {
        private final MediaFileTableItem[] _items = new MediaFileTableItem[734];

        public MediaFileTable(RandomAccessFile slps, FileTable fileTable) throws IOException {
            slps.seek(402024);

            for (int i = 0; i < _items.length; i++) {
                if (i < 57 || (i >= 709 && i <= 732))
                    _items[i] = new MediaTableItemStr(i, slps, fileTable);
                else
                    _items[i] = new MediaTableItemXa(i, slps);
            }
        }

        public void print(PrintStream out) {
            for (MediaFileTableItem item : _items) {
                out.println(item);
            }
        }

        public MediaFileTableItem get(int i) {
            return _items[i];
        }
    }


    public static abstract class MediaFileTableItem {
        public static final int SIZEOF = 8;

        private final int _iIndex;
        private final long _iAt;

        public MediaFileTableItem(int iIndex, long at) {
            _iIndex = iIndex;
            _iAt = at;
        }

        @Override
        public String toString() {
            return _iIndex + ": @" + _iAt;
        }

        abstract public String getFile();
    }

    public static class MediaTableItemStr extends MediaFileTableItem {

        // Size
        /* 2 */ private final int _iZero;
        /* 2 */ private final int _iFileTableIndex;
        /* 4 */ private final long _lngFrameCount;

        private final FileTableItem _fileItem;

        public MediaTableItemStr(int iIndex, RandomAccessFile slps, FileTable fileTable) throws IOException {
            super(iIndex, slps.getFilePointer());
            _iZero = IO.readUInt16LE(slps);
            _iFileTableIndex = IO.readUInt16LE(slps);
            _lngFrameCount = IO.readUInt32LE(slps);

            _fileItem = fileTable.get(_iFileTableIndex);
        }
        
        @Override
        public String toString() {
            return super.toString() + 
                   String.format(" Zero=%d FileTableIndex=%d (%s) FrameCount=%d",
                   _iZero, _iFileTableIndex, _fileItem, _lngFrameCount
                   );
        }

        @Override
        public String getFile() {
            return _fileItem.getFile().getPath();
        }
        
        
    }

    public static class MediaTableItemXa extends MediaFileTableItem {

        // Size
        /* 2 */ private final int _iXaFile;
        /* 2 */ private final int _iChannel;
        /** The data size of all the Mode 2 Form 2 sectors that make up this stream.
         * In other words: (the number of sectors) * 2336 */
        /* 4 */ private final long _lngByteSizeMultOf2336;

        public MediaTableItemXa(int iIndex, RandomAccessFile slps) throws IOException {
            super(iIndex, slps.getFilePointer());
            _iXaFile = IO.readUInt16LE(slps);
            _iChannel = IO.readUInt16LE(slps);
            _lngByteSizeMultOf2336 = IO.readUInt32LE(slps);
        }

        @Override
        public String toString() {
            return super.toString() + 
                    String.format(" %s Sectors data size %d (%s sectors)",
                    getFile(), _lngByteSizeMultOf2336, Double.toString(_lngByteSizeMultOf2336 / 2336.f));
        }

        @Override
        public String getFile() {
            return String.format("LAIN%02d.XA[%d]", _iXaFile, _iChannel);
        }
        
    }


    // ------------------------------------------------------------------------

    public static class LainFile extends File {

        private final int _iSize, _iStartSector, _iEndSector;

        public LainFile(String sPath, int iSize, int iStartSector, int iEndSector) {
            super(sPath);
            _iSize = iSize;
            _iStartSector = iStartSector;
            _iEndSector = iEndSector;
        }

        @Override
        public String toString() {
            return super.toString() + " " + _iSize + " @" + _iStartSector + "-" + _iEndSector;
        }

    }


    private static final HashMap<Integer, LainFile> SITEA_FILES = new HashMap<Integer, LainFile>();
    private static final HashMap<Integer, LainFile> SITEB_FILES = new HashMap<Integer, LainFile>();
    
    // <editor-fold defaultstate="collapsed" desc="SITE*_FILES init">
    static {
        SITEA_FILES.put(23    , new LainFile("SLPS_016.03"    , 618496  , 23    , 324));
        SITEA_FILES.put(325   , new LainFile("SYSTEM.CNF"     , 69      , 325   , 325));
        SITEA_FILES.put(326   , new LainFile("LAIN_1.INF"     , 18      , 326   , 326));
        SITEA_FILES.put(327   , new LainFile("BIN.BIN"        , 256000  , 327   , 451));
        SITEA_FILES.put(452   , new LainFile("LAPKS.BIN"      , 13936640, 452   , 7256));
        SITEA_FILES.put(7257  , new LainFile("SND.BIN"        , 438272  , 7257  , 7470));
        SITEA_FILES.put(7471  , new LainFile("SITEA.BIN"      , 7057408 , 7471  , 10916));
        SITEA_FILES.put(10917 , new LainFile("VOICE.BIN"      , 14690304, 10917 , 18089));
        SITEA_FILES.put(18092 , new LainFile("MOVIE/INS16.STR", 1376256 , 18092 , 18763));
        SITEA_FILES.put(18764 , new LainFile("MOVIE/INS17.STR", 1376256 , 18764 , 19435));
        SITEA_FILES.put(19436 , new LainFile("MOVIE/INS18.STR", 1507328 , 19436 , 20171));
        SITEA_FILES.put(20172 , new LainFile("MOVIE/INS19.STR", 1376256 , 20172 , 20843));
        SITEA_FILES.put(20844 , new LainFile("MOVIE/INS20.STR", 2293760 , 20844 , 21963));
        SITEA_FILES.put(21964 , new LainFile("MOVIE/INS21.STR", 1179648 , 21964 , 22539));
        SITEA_FILES.put(22540 , new LainFile("MOVIE/INS22.STR", 1376256 , 22540 , 23211));
        SITEA_FILES.put(23212 , new LainFile("MOVIE/INS01.STR", 1048576 , 23212 , 23723));
        SITEA_FILES.put(23724 , new LainFile("MOVIE/INS02.STR", 1048576 , 23724 , 24235));
        SITEA_FILES.put(24236 , new LainFile("MOVIE/INS03.STR", 589824  , 24236 , 24523));
        SITEA_FILES.put(24524 , new LainFile("MOVIE/INS04.STR", 1048576 , 24524 , 25035));
        SITEA_FILES.put(25036 , new LainFile("MOVIE/INS05.STR", 720896  , 25036 , 25387));
        SITEA_FILES.put(25388 , new LainFile("MOVIE/INS06.STR", 1376256 , 25388 , 26059));
        SITEA_FILES.put(26060 , new LainFile("MOVIE/INS07.STR", 1638400 , 26060 , 26859));
        SITEA_FILES.put(26860 , new LainFile("MOVIE/INS08.STR", 1638400 , 26860 , 27659));
        SITEA_FILES.put(27660 , new LainFile("MOVIE/INS09.STR", 1638400 , 27660 , 28459));
        SITEA_FILES.put(28460 , new LainFile("MOVIE/INS10.STR", 1507328 , 28460 , 29195));
        SITEA_FILES.put(29196 , new LainFile("MOVIE/INS11.STR", 1376256 , 29196 , 29867));
        SITEA_FILES.put(29868 , new LainFile("MOVIE/INS12.STR", 1638400 , 29868 , 30667));
        SITEA_FILES.put(30668 , new LainFile("MOVIE/INS13.STR", 1507328 , 30668 , 31403));
        SITEA_FILES.put(31404 , new LainFile("MOVIE/INS14.STR", 1376256 , 31404 , 32075));
        SITEA_FILES.put(32076 , new LainFile("MOVIE/INS15.STR", 1966080 , 32076 , 33035));
        SITEA_FILES.put(33036 , new LainFile("MOVIE/F001.STR" , 1245184 , 33036 , 33643));
        SITEA_FILES.put(33644 , new LainFile("MOVIE/F002.STR" , 5242880 , 33644 , 36203));
        SITEA_FILES.put(36204 , new LainFile("MOVIE/F003.STR" , 2031616 , 36204 , 37195));
        SITEA_FILES.put(37196 , new LainFile("MOVIE/F004.STR" , 2031616 , 37196 , 38187));
        SITEA_FILES.put(38188 , new LainFile("MOVIE/F006.STR" , 3801088 , 38188 , 40043));
        SITEA_FILES.put(40044 , new LainFile("MOVIE/F008.STR" , 2949120 , 40044 , 41483));
        SITEA_FILES.put(41484 , new LainFile("MOVIE/F010.STR" , 2949120 , 41484 , 42923));
        SITEA_FILES.put(42924 , new LainFile("MOVIE/F012.STR" , 7733248 , 42924 , 46699));
        SITEA_FILES.put(46700 , new LainFile("MOVIE/F013.STR" , 3735552 , 46700 , 48523));
        SITEA_FILES.put(48524 , new LainFile("MOVIE/F014.STR" , 2818048 , 48524 , 49899));
        SITEA_FILES.put(49900 , new LainFile("MOVIE/F015.STR" , 3440640 , 49900 , 51579));
        SITEA_FILES.put(51580 , new LainFile("MOVIE/F020.STR" , 10354688, 51580 , 56635));
        SITEA_FILES.put(56636 , new LainFile("MOVIE/F022.STR" , 8650752 , 56636 , 60859));
        SITEA_FILES.put(60860 , new LainFile("MOVIE/F024.STR" , 9437184 , 60860 , 65467));
        SITEA_FILES.put(65468 , new LainFile("MOVIE/F025.STR" , 5439488 , 65468 , 68123));
        SITEA_FILES.put(68124 , new LainFile("MOVIE/F026.STR" , 2359296 , 68124 , 69275));
        SITEA_FILES.put(69276 , new LainFile("MOVIE/F027.STR" , 5111808 , 69276 , 71771));
        SITEA_FILES.put(71772 , new LainFile("MOVIE/F029.STR" , 11862016, 71772 , 77563));
        SITEA_FILES.put(77564 , new LainFile("MOVIE/F033.STR" , 3706880 , 77564 , 79373));
        SITEA_FILES.put(79374 , new LainFile("MOVIE/F034.STR" , 6488064 , 79374 , 82541));
        SITEA_FILES.put(82542 , new LainFile("MOVIE/F035.STR" , 7995392 , 82542 , 86445));
        SITEA_FILES.put(86446 , new LainFile("MOVIE/F036.STR" , 4653056 , 86446 , 88717));
        SITEA_FILES.put(88718 , new LainFile("MOVIE/F037.STR" , 5046272 , 88718 , 91181));
        SITEA_FILES.put(91182 , new LainFile("MOVIE/F038.STR" , 4915200 , 91182 , 93581));
        SITEA_FILES.put(93582 , new LainFile("MOVIE/F042.STR" , 3735552 , 93582 , 95405));
        SITEA_FILES.put(95406 , new LainFile("MOVIE/F043.STR" , 12910592, 95406 , 101709));
        SITEA_FILES.put(101711, new LainFile("XA/LAIN02.XA"   , 62390272, 101711, 132174));
        SITEA_FILES.put(132175, new LainFile("XA/LAIN03.XA"   , 14942208, 132175, 139470));
        SITEA_FILES.put(139471, new LainFile("XA/LAIN04.XA"   , 15335424, 139471, 146958));
        SITEA_FILES.put(146959, new LainFile("XA/LAIN05.XA"   , 19988480, 146959, 156718));
        SITEA_FILES.put(156719, new LainFile("XA/LAIN06.XA"   , 17235968, 156719, 165134));
        SITEA_FILES.put(165135, new LainFile("XA/LAIN07.XA"   , 20971520, 165135, 175374));
        SITEA_FILES.put(175375, new LainFile("XA/LAIN08.XA"   , 35323904, 175375, 192622));
        SITEA_FILES.put(192623, new LainFile("XA/LAIN09.XA"   , 25559040, 192623, 205102));
        SITEA_FILES.put(205103, new LainFile("XA/LAIN10.XA"   , 2490368 , 205103, 206318));
        SITEA_FILES.put(206319, new LainFile("XA/LAIN11.XA"   , 1835008 , 206319, 207214));
        SITEA_FILES.put(207215, new LainFile("XA/LAIN12.XA"   , 2818048 , 207215, 208590));
        SITEA_FILES.put(208591, new LainFile("XA/LAIN13.XA"   , 14286848, 208591, 215566));
        SITEA_FILES.put(215567, new LainFile("XA/LAIN01.XA"   , 66060288, 215567, 247822));

        SITEB_FILES.put(23    , new LainFile("SLPS_016.04"        , 618496  , 23    , 324));
        SITEB_FILES.put(325   , new LainFile("SYSTEM.CNF"         , 69      , 325   , 325));
        SITEB_FILES.put(326   , new LainFile("LAIN_2.INF"         , 18      , 326   , 326));
        SITEB_FILES.put(327   , new LainFile("BIN.BIN"            , 256000  , 327   , 451));
        SITEB_FILES.put(452   , new LainFile("LAPKS.BIN"          , 13936640, 452   , 7256));
        SITEB_FILES.put(7257  , new LainFile("SND.BIN"            , 438272  , 7257  , 7470));
        SITEB_FILES.put(7471  , new LainFile("SITEB.BIN"          , 5328896 , 7471  , 10072));
        SITEB_FILES.put(10073 , new LainFile("VOICE.BIN"          , 14690304, 10073 , 17245));
        SITEB_FILES.put(17248 , new LainFile("MOVIE/INS16.STR"    , 1376256 , 17248 , 17919));
        SITEB_FILES.put(17920 , new LainFile("MOVIE/INS17.STR"    , 1376256 , 17920 , 18591));
        SITEB_FILES.put(18592 , new LainFile("MOVIE/INS18.STR"    , 1507328 , 18592 , 19327));
        SITEB_FILES.put(19328 , new LainFile("MOVIE/INS19.STR"    , 1376256 , 19328 , 19999));
        SITEB_FILES.put(20000 , new LainFile("MOVIE/INS20.STR"    , 2293760 , 20000 , 21119));
        SITEB_FILES.put(21120 , new LainFile("MOVIE/INS21.STR"    , 1179648 , 21120 , 21695));
        SITEB_FILES.put(21696 , new LainFile("MOVIE/INS22.STR"    , 1376256 , 21696 , 22367));
        SITEB_FILES.put(22368 , new LainFile("MOVIE/PO1.STR"      , 512000  , 22368 , 22617));
        SITEB_FILES.put(22618 , new LainFile("MOVIE/PO2.STR"      , 1536000 , 22618 , 23367));
        SITEB_FILES.put(23368 , new LainFile("MOVIE/F039.STR"     , 4587520 , 23368 , 25607));
        SITEB_FILES.put(25608 , new LainFile("MOVIE/F041.STR"     , 3407872 , 25608 , 27271));
        SITEB_FILES.put(27272 , new LainFile("MOVIE/F045.STR"     , 8388608 , 27272 , 31367));
        SITEB_FILES.put(31368 , new LainFile("MOVIE/F047.STR"     , 4521984 , 31368 , 33575));
        SITEB_FILES.put(33576 , new LainFile("MOVIE/F049.STR"     , 7733248 , 33576 , 37351));
        SITEB_FILES.put(37352 , new LainFile("MOVIE/F052.STR"     , 6815744 , 37352 , 40679));
        SITEB_FILES.put(40680 , new LainFile("MOVIE/F054.STR"     , 3080192 , 40680 , 42183));
        SITEB_FILES.put(42184 , new LainFile("MOVIE/F055.STR"     , 4653056 , 42184 , 44455));
        SITEB_FILES.put(44456 , new LainFile("MOVIE/F056.STR"     , 13565952, 44456 , 51079));
        SITEB_FILES.put(51080 , new LainFile("MOVIE/F057.STR"     , 11075584, 51080 , 56487));
        SITEB_FILES.put(56488 , new LainFile("MOVIE/F058.STR"     , 5439488 , 56488 , 59143));
        SITEB_FILES.put(59144 , new LainFile("MOVIE/F059.STR"     , 10682368, 59144 , 64359));
        SITEB_FILES.put(64360 , new LainFile("MOVIE/F061.STR"     , 29020160, 64360 , 78529));
        SITEB_FILES.put(78530 , new LainFile("MOVIE/F065.STR"     , 6291456 , 78530 , 81601));
        SITEB_FILES.put(81602 , new LainFile("MOVIE/F066.STR"     , 7864320 , 81602 , 85441));
        SITEB_FILES.put(85442 , new LainFile("MOVIE/F068.STR"     , 4784128 , 85442 , 87777));
        SITEB_FILES.put(87778 , new LainFile("MOVIE/F069.STR"     , 5111808 , 87778 , 90273));
        SITEB_FILES.put(90274 , new LainFile("MOVIE/F072.STR"     , 1769472 , 90274 , 91137));
        SITEB_FILES.put(91138 , new LainFile("MOVIE/F073.STR"     , 3735552 , 91138 , 92961));
        SITEB_FILES.put(92962 , new LainFile("MOVIE/F075.STR"     , 5242880 , 92962 , 95521));
        SITEB_FILES.put(95522 , new LainFile("MOVIE/F076.STR"     , 14942208, 95522 , 102817));
        SITEB_FILES.put(102818, new LainFile("MOVIE/F079.STR"     , 18481152, 102818, 111841));
        SITEB_FILES.put(111842, new LainFile("MOVIE/F082.STR"     , 3735552 , 111842, 113665));
        SITEB_FILES.put(113666, new LainFile("MOVIE/F083.STR"     , 10567680, 113666, 118825));
        SITEB_FILES.put(118826, new LainFile("MOVIE/F085.STR"     , 13041664, 118826, 125193));
        SITEB_FILES.put(125194, new LainFile("MOVIE/F088.STR"     , 9568256 , 125194, 129865));
        SITEB_FILES.put(129866, new LainFile("MOVIE/F090.STR"     , 11075584, 129866, 135273));
        SITEB_FILES.put(135274, new LainFile("MOVIE/F094.STR"     , 11730944, 135274, 141001));
        SITEB_FILES.put(141002, new LainFile("MOVIE/F096.STR"     , 8650752 , 141002, 145225));
        SITEB_FILES.put(145226, new LainFile("MOVIE/F098.STR"     , 11141120, 145226, 150665));
        SITEB_FILES.put(150666, new LainFile("MOVIE/F103.STR"     , 9895936 , 150666, 155497));
        SITEB_FILES.put(155499, new LainFile("XA/LAIN14.XA"       , 54001664, 155499, 181866));
        SITEB_FILES.put(181867, new LainFile("XA/LAIN15.XA"       , 67043328, 181867, 214602));
        SITEB_FILES.put(214603, new LainFile("XA/LAIN16.XA"       , 21037056, 214603, 224874));
        SITEB_FILES.put(224875, new LainFile("XA/LAIN17.XA"       , 13762560, 224875, 231594));
        SITEB_FILES.put(231595, new LainFile("XA/LAIN18.XA"       , 29753344, 231595, 246122));
        SITEB_FILES.put(246123, new LainFile("XA/LAIN19.XA"       , 30801920, 246123, 261162));
        SITEB_FILES.put(261163, new LainFile("XA/LAIN20.XA"       , 2490368 , 261163, 262378));
        SITEB_FILES.put(262379, new LainFile("XA/LAIN21.XA"       , 14286848, 262379, 269354));
        SITEB_FILES.put(269356, new LainFile("MOVIE2/ENDROLL1.STR", 97779712, 269356, 317099));

    }
    // </editor-fold>

    
}
