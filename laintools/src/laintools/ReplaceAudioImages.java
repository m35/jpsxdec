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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import javax.imageio.ImageIO;
import jpsxdec.tim.Tim;
import jpsxdec.util.IO;

public class ReplaceAudioImages {

    public static void main(String[] args) throws Exception {

        if (args.length < 5) {
            System.out.println("Requires 5 arguments");
            System.out.println("   <original SITEA.BIN> <original SITEB.BIN> <existing SLPS file to edit> <SITEA.BIN file to create> <SITEB.BIN file to create>");
            System.out.println();
            System.out.println("Also requires SiteA and SiteB directories to exist full of files to insert");
        }
        
        String sSlpsFile = args[2], 
               sSiteAFile = args[3], 
               sSiteBFile = args[4];
        
        System.out.println("SiteA");
        SiteImageProvider siteAFiles = new SiteImageProvider('A', new File("SiteA"), new File(args[0]));
        System.out.println("SiteB");
        SiteImageProvider siteBFiles = new SiteImageProvider('B', new File("SiteB"), new File(args[1]));


        System.out.println("Hunting for node images");
        SiteImage[][] aaoNodeImage = new SiteImage[NODES_WITH_IMAGES.length][];
        for (int i = 0; i < NODES_WITH_IMAGES.length; i++) {
            aaoNodeImage[i] = NODES_WITH_IMAGES[i].loadImages(siteAFiles, siteBFiles);
        }

        if (!new File(sSlpsFile).exists())
            throw new FileNotFoundException(sSlpsFile);
        System.out.println("Opening existing SLPS file " + sSlpsFile);
        RandomAccessFile slps = new RandomAccessFile(sSlpsFile, "rw");

        System.out.println("Generating new site files and site index tables");
        FileOutputStream fos = new FileOutputStream(sSiteAFile);
        siteAFiles.write(slps, fos, true);
        fos.close();
        fos = new FileOutputStream(sSiteBFile);
        siteBFiles.write(slps, fos, true);
        fos.close();

        System.out.println("Updating SLPS node table indexes");
        for (int i = 0; i < NODES_WITH_IMAGES.length; i++) {
            NODES_WITH_IMAGES[i].write(slps, aaoNodeImage[i]);
        }

        slps.close();

    }
    

    public static class NodeInfo {
        private static final int NONE = -1;

        private final String _sName;
        private final int _iOffset;
        private final char _cSite;
        final protected int[] _aiSiteImgIndexes = new int[3];

        @Override
        public String toString() {
            return "NodeImageReplacer{" + _sName + " @" + _iOffset + " Site" + _cSite + " " + Arrays.toString(_aiSiteImgIndexes) + "}";
        }

        

        public NodeInfo(String sName, int iOffset, char cSite, int iImg1, int iImg2, int iImg3) {
            _sName = sName;
            _iOffset = iOffset;
            _cSite = cSite;
            _aiSiteImgIndexes[0] = iImg1;
            _aiSiteImgIndexes[1] = iImg2;
            _aiSiteImgIndexes[2] = iImg3;
        }

        void write(RandomAccessFile slps, SiteImage[] aoImages) throws IOException {
            slps.seek(_iOffset+22);
            System.out.println("@" + (_iOffset+22) + " " + _sName +"-0  --> " + aoImages[0]);
            IO.writeInt16LE(slps, (short)aoImages[0].getIndex());
            System.out.println("@" + (_iOffset+24) + " " + _sName +"-1  --> " + aoImages[0]);
            IO.writeInt16LE(slps, (short)(aoImages[1] == null ? NONE : aoImages[1].getIndex()));
            System.out.println("@" + (_iOffset+26) + " " + _sName +"-2  --> " + aoImages[0]);
            IO.writeInt16LE(slps, (short)(aoImages[2] == null ? NONE : aoImages[2].getIndex()));
        }


        SiteImage[] loadImages(SiteImageProvider imgProvider, int[] aiImg012Indexes) {
            int iImg012 = 0;
            SiteImage[] aoRet = new SiteImage[3];
            System.out.print(_sName + "-" + iImg012 + ": ");
            SiteImage image = imgProvider.findFile(_sName + "-" + iImg012, aiImg012Indexes[iImg012]);
            if (image == null) {
                // use SITE*.BIN for images
                for (; iImg012 < 3 && aiImg012Indexes[iImg012] != NONE; iImg012++) {
                    if (iImg012 > 0)
                        System.out.print(_sName + "-" + iImg012 + ": ");
                    image = imgProvider.getSiteImage(aiImg012Indexes[iImg012]);
                    image.addUsedBy(_sName + "-" + iImg012);
                    aoRet[iImg012] = image;
                }
            } else {
                // use files for images
                while (true) {
                    image.addUsedBy(_sName + "-" + iImg012);
                    aoRet[iImg012] = image;
                    iImg012++;
                    if (iImg012 >= 3)
                        break;
                    System.out.print(_sName + "-" + iImg012 + ": ");
                    image = imgProvider.findFile(_sName + "-" + iImg012, aiImg012Indexes[iImg012]);
                    if (image == null) {
                        System.out.println("no image found, so done with the node");
                        break;
                    }
                } 
            }
            return aoRet;
        }
        
        
        SiteImage[] loadImages(SiteImageProvider siteAprovider, SiteImageProvider siteBprovider) throws IOException {
            if (_cSite == 'A') {
                return loadImages(siteAprovider, _aiSiteImgIndexes);
            } else {
                return loadImages(siteBprovider, _aiSiteImgIndexes);
            }
        }

        public String sites() {
            return String.valueOf(_cSite);
        }

        public String name() {
            return _sName;
        }

    }
    private static final byte[] image2pkTim(File png) throws Exception {
        if (!png.exists())
            throw new FileNotFoundException(png.toString());
        BufferedImage bi = ImageIO.read(png);
        return image2pkTim(bi);
    }

    private static final byte[] image2pkTim(BufferedImage bi) throws Exception {
        // convert to tim
        Tim newTim = Tim.create(bi, 8);
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        // write it out
        newTim.write(temp);
        // and compress it
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("napk".getBytes());
        Lain_Pk.compress(temp.toByteArray(), out);
        return out.toByteArray();
    }

    private static class EnvNodeImage extends NodeInfo {

        private final int _iEnvOffsetA;
        private final int _iEnvOffsetB;

        private final int[] _iImgSiteBImgIndexes = new int[3];


        public EnvNodeImage(String sName, int iOffset, int iImgA1, int iImgA2, int iImgA3,
                                                       int iImgB1, int iImgB2, int iImgB3,
                                                       int iEnvOffsetA, int iEnvOffsetB)
        {
            super(sName, iOffset, 'E', iImgA1, iImgA2, iImgA3);
            _iEnvOffsetA = iEnvOffsetA;
            _iEnvOffsetB = iEnvOffsetB;
            _iImgSiteBImgIndexes[0] = iImgB1;
            _iImgSiteBImgIndexes[1] = iImgB2;
            _iImgSiteBImgIndexes[2] = iImgB3;
        }

        public void write(RandomAccessFile slps, SiteImage[] aoImages) throws IOException {
            super.write(slps, aoImages);
            slps.seek(_iEnvOffsetA);
            IO.writeInt16LE(slps, (short)aoImages[0].getIndex());
            IO.writeInt16LE(slps, (short)aoImages[1].getIndex());
            IO.writeInt16LE(slps, (short)aoImages[2].getIndex());
            slps.seek(_iEnvOffsetB);
            IO.writeInt16LE(slps, (short)aoImages[3].getIndex());
            IO.writeInt16LE(slps, (short)aoImages[4].getIndex());
            IO.writeInt16LE(slps, (short)aoImages[5].getIndex());
        }

        @Override
        public SiteImage[] loadImages(SiteImageProvider siteAdir, SiteImageProvider siteBdir) throws IOException {
            SiteImage[] a = loadImages(siteAdir, _aiSiteImgIndexes);
            SiteImage[] b = loadImages(siteBdir, _iImgSiteBImgIndexes);
            return new SiteImage[] {
                a[0], a[1], a[2],
                b[0], b[1], b[2]
            };
        }

        @Override
        public String sites() {
            return "AB";
        }
    }

    private static abstract class SiteImage implements Comparable<SiteImage> {
        
        public static final int NEW = -2;
        
        private final char _cSite;
        private int _iIndex = NEW;
        private final ArrayList<String> _usedBy = new ArrayList<String>();

        private SiteImage(char cSite) {
            _cSite = cSite;
        }

        public void setSiteTableIndex(int iSiteTableIndex) {
            _iIndex = iSiteTableIndex;
        }
        
        abstract public byte[] readData() throws Exception;

        public int getIndex() {
            return _iIndex;
        }

        public int compareTo(SiteImage o) {
            return Integer.valueOf(_iIndex).compareTo(o._iIndex);
        }
        
        public void addUsedBy(String sNode) {
            _usedBy.add(sNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SiteImage other = (SiteImage) obj;
            if (this._iIndex != other._iIndex) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + this._iIndex;
            return hash;
        }
        public String toString() {
            return "Site"+_cSite + " image " + (_iIndex == NEW ? "NEW" : _iIndex) + " used by " + _usedBy;
        }
    }
    
    private static class SiteImageFromFile extends SiteImage {
        private File _source;

        private SiteImageFromFile(char cSite, File source) {
            super(cSite);
            _source = source;
        }

        public byte[] readData() throws Exception {
            if (_source.getName().endsWith(".png")) {
                return image2pkTim(_source);
            } else {
                return IO.readFile(_source);
            }
        }

        @Override
        public String toString() {
            return "SiteImageFromFile{" + super.toString() + " from "+_source + "}";
        }

    }
    private static class SiteImageFromSITE extends SiteImage {

        private final int _iSiteFileSectorOffset;
        private final int _iDataSize;
        private final File _siteFile;
        private final RandomAccessFile _siteRaf;
        
        private SiteImageFromSITE(char cSite, int iSiteIndex, File siteFile, RandomAccessFile raf, int iFileSectorOffset, int iDataSize) {
            super(cSite);
            setSiteTableIndex(iSiteIndex);
            _iSiteFileSectorOffset = iFileSectorOffset;
            _iDataSize = iDataSize;
            _siteFile = siteFile;
            _siteRaf = raf;
        }
        
        @Override
        public byte[] readData() throws IOException {
            _siteRaf.seek(_iSiteFileSectorOffset * 2048);
            return IO.readByteArray(_siteRaf, _iDataSize);
        }
        
        @Override
        public String toString() {
            return "SiteImageFromSITE{" + super.toString() + " from "+ _siteFile +" offset " + 
                                    _iSiteFileSectorOffset + " size " + _iDataSize + "}";
        }
    }
    

    private static class SiteImageProvider {
        private final File _dir;
        private final File[] _aoFiles;
        private final char _cSite;
        private static byte[] _abDummy;
        private final int _iOffsetInSlps;
        
        private final SiteImage[] _aoImages;
        private final ArrayList<SiteImage> _newImages = new ArrayList<SiteImage>();
        private final SiteImageFromSITE[] _aoSiteTable;
        

        public SiteImageProvider(char cSite, File imgageDir, File siteFile) throws Exception {
            _cSite = cSite;            
            _dir = imgageDir;
            _aoFiles = imgageDir.listFiles();
            if (_cSite == 'A') {
                _aoSiteTable = new SiteImageFromSITE[790];
                _aoImages = new SiteImage[790];
                _iOffsetInSlps = 568416;
            } else {
                _aoSiteTable = new SiteImageFromSITE[558];
                _aoImages = new SiteImage[558];
                _iOffsetInSlps = 574736;
            }
            if (_abDummy == null) {
                System.out.println("Loading dummy error placeholder");
                _abDummy = image2pkTim(new File("dummy.png"));
            }
            loadSiteTable(siteFile);
        }
        
        private void loadSiteTable(File siteFile) throws IOException {
            RandomAccessFile siteRaf =  new RandomAccessFile(siteFile, "r");
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    getClass().getResourceAsStream("SITE"+_cSite+"-image-offset-table.txt")));
            for (int iSiteIndex = 0; iSiteIndex < _aoSiteTable.length; iSiteIndex++) {
                String sLine = br.readLine();
                String[] asParts = sLine.split("\\s+");
                if (iSiteIndex != Integer.parseInt(asParts[0]))
                    throw new RuntimeException();
                int iFileOffset = Integer.parseInt(asParts[1]);
                int iDataSize = Integer.parseInt(asParts[2]);
                _aoSiteTable[iSiteIndex] = new SiteImageFromSITE(_cSite, iSiteIndex, siteFile, siteRaf, iFileOffset, iDataSize);
                if (sLine.contains("Reserved")) {
                    _aoImages[iSiteIndex] = _aoSiteTable[iSiteIndex];
                }
                    
            }
            br.close();
        }

        private HashMap<File, SiteImage> _cache = new HashMap<File, SiteImage>();
        
        private SiteImage findFile(String sNamePart, int iSiteTableIndex) {
            
            for (File file : _aoFiles) {
                if (file.getName().contains(sNamePart)) {
                    SiteImage image = _cache.get(file);
                    if (image != null) {
                        System.out.println("Site" + _cSite +": Reusing from cache " + image);
                        return image;
                    }
                    image = new SiteImageFromFile(_cSite, file);
                    if (iSiteTableIndex != -1 && _aoImages[iSiteTableIndex] == null) {
                        image.setSiteTableIndex(iSiteTableIndex);
                        System.out.println("Site" + _cSite +": Using " + image);
                        _aoImages[iSiteTableIndex] = image;
                    } else {
                        System.out.println("Site" + _cSite +": Queing new image to add later " + image);
                        _newImages.add(image);
                    }
                    _cache.put(file, image);
                    return image;
                }
            }
            return null;
        }
        
        public SiteImageFromSITE getSiteImage(int iSiteTableIndex) {
            if (_aoImages[iSiteTableIndex] != null) {
                if (_aoImages[iSiteTableIndex] != _aoSiteTable[iSiteTableIndex])
                    throw new RuntimeException();
                System.out.println("Site" + _cSite +": Reusing " + _aoSiteTable[iSiteTableIndex]);
            } else {
                System.out.println("Site" + _cSite +": Using " + _aoSiteTable[iSiteTableIndex]);
            }
            _aoImages[iSiteTableIndex] = _aoSiteTable[iSiteTableIndex];
            return _aoSiteTable[iSiteTableIndex];
        }

        public void write(RandomAccessFile slps, OutputStream os, boolean blnReal) throws Exception {
            
            int iOutOffset = 0, iDummyOffset = -1;
            System.out.println("Site" + _cSite +": Seeking to offset of site table in slps " + _iOffsetInSlps);
            if (blnReal) slps.seek(_iOffsetInSlps);
            Iterator<SiteImage> newImages = _newImages.iterator();

            for (int iIndex = 0; iIndex < _aoImages.length; iIndex++) {
                SiteImage image = _aoImages[iIndex];
                if (image == null && newImages.hasNext()) {
                    image = newImages.next();
                    image.setSiteTableIndex(iIndex);
                    _aoImages[iIndex] = image;
                }

                if (image == null && iDummyOffset >= 0) {
                    System.out.println("Site" + _cSite + ": #" + iIndex + " is unused, and there are no more images to add, so point to dummy @" +(iDummyOffset/2048) + " ("+iDummyOffset+")");
                    if (blnReal) IO.writeInt32LE(slps, iDummyOffset / 2048);
                    if (blnReal) IO.writeInt32LE(slps, _abDummy.length);
                } else {
                    byte[] abData;
                    if (image == null) {
                        System.out.println("Site" + _cSite + ": Site table index " + iIndex + " is unused, and there are no more images to add so...");
                        System.out.print("Site" + _cSite + ": #" + iIndex + " writing dummy image for the first time");
                        iDummyOffset = iOutOffset;
                        abData = _abDummy;
                    } else {
                        System.out.print("Site" + _cSite + ": #" + iIndex + " writing " + image);
                        abData = image.readData();
                    }
                    
                    if (iOutOffset % 2048 != 0)
                        throw new RuntimeException();
                    
                    if (blnReal) IO.writeInt32LE(slps, iOutOffset / 2048);
                    if (blnReal) IO.writeInt32LE(slps, abData.length);

                    int iTotalToWrite = (abData.length + 2047) & ~2047;
                    System.out.println(" @"+(iOutOffset / 2048)+" ("+iOutOffset+") size " + abData.length + " + " + (iTotalToWrite - abData.length) + " padding");
                    if (blnReal) os.write(abData);
                    if (blnReal) for (int i = abData.length; i < iTotalToWrite; i++)
                        os.write(0);

                    iOutOffset += iTotalToWrite;
                }
            }
            if (newImages.hasNext()) {
                throw new RuntimeException("Unable to add all new images to Site" +_cSite + "!");
            }
                
        }

        
    }

    // ########################################################################

    // <editor-fold defaultstate="collapsed" desc="Node table">

    private static NodeInfo A(int iOffset, String sName, int iImg1, int iImg2, int iImg3) {
        return new NodeInfo(sName, iOffset, 'A', iImg1, iImg2, iImg3);
    }
    private static NodeInfo B(int iOffset, String sName, int iImg1, int iImg2, int iImg3) {
        return new NodeInfo(sName, iOffset, 'B', iImg1, iImg2, iImg3);
    }
    private static EnvNodeImage AB(int iNodeOffset, String sName,
            int iEnvOffsetA, int iImgA1, int iImgA2, int iImgA3,
            int iEnvOffsetB, int iImgB1, int iImgB2, int iImgB3)
    {
        return new EnvNodeImage(sName, iNodeOffset,
                                        iImgA1, iImgA2, iImgA3,
                                        iImgB1, iImgB2, iImgB3,
                                        iEnvOffsetA, iEnvOffsetB);
    }

    public static final NodeInfo[] NODES_WITH_IMAGES = {
        A(413824, "Cou001", 31 , 584, 607),
        A(413864, "Cou002", 577, 583, 492),
        A(413904, "Cou003", 495, 490, 551),
        A(413944, "Cou004", 546, 529, 531),
        A(413984, "Cou005", 443, 445, 447),
        A(414024, "Cou006", 442, 439, 446),
        A(414064, "Cou007", 464, 461, 406),
        A(414104, "Cou008", 408, 409, 418),
        A(414144, "Cou009", 412, 545, 536),
        A(414184, "Cou010", 608, 586, 618),
        A(414224, "Cou011", 392, 395, 394),
        A(414264, "Cou012", 349, 257, 252),
        A(414304, "Cou013", 29 , 253, 370),
        A(414344, "Cou014", 0  , -1 , -1 ),
        A(414384, "Cou015", 258, 179, 32 ),
        A(414424, "Cou016", 180, 259, 46 ),
        A(414464, "Cou017", 181, 49 , 260),
        A(414504, "Cou018", 50 , 182, 261),
        A(414544, "Cou019", 52 , 262, 183),
        A(414584, "Cou020", 263, 63 , 184),
        A(414624, "Cou021", 264, 185, 75 ),
        A(414664, "Cou022", 186, 268, 81 ),
        A(414704, "Cou023", 190, 85 , 272),
        A(414744, "Cou024", 87 , 194, 282),
        A(414784, "Cou025", 88 , 291, 196),
        A(414824, "Cou026", 323, 92 , 197),
        A(414864, "Cou027", 326, 198, 103),
        A(414904, "Cou028", 199, 327, 104),
        A(414944, "Cou029", 200, 106, 328),
        A(414984, "Cou030", 107, 201, 332),
        A(415024, "Cou031", 108, 333, 202),
        A(415064, "Cou032", 334, 109, 236),
        A(415104, "Cou033", 336, 244, 110),
        A(415144, "Cou034", 246, 338, 111),
        A(415184, "Cou035", 247, 112, 339),
        A(415224, "Cou036", 122, 248, 340),
        A(415264, "Cou037", 140, 341, 250),
        A(415304, "Cou038", 35 , 145, 251),
        B(415344, "Cou039", 126, 77 , 23 ),
        B(415384, "Cou040", 78 , 138, 24 ),
        B(415424, "Cou041", 79 , 25 , 139),
        B(415464, "Cou042", 26 , 80 , 140),
        B(415504, "Cou043", 27 , 141, 81 ),
        B(415544, "Cou044", 142, 30 , 82 ),
        B(415584, "Cou045", 144, 84 , 31 ),
        B(415624, "Cou046", 85 , 145, 32 ),
        B(415664, "Cou047", 86 , 33 , 146),
        B(415704, "Cou048", 34 , 87 , 147),
        B(415744, "Cou049", 37 , 148, 88 ),
        B(415784, "Cou050", 149, 38 , 89 ),
        B(415824, "Cou051", 151, 90 , 45 ),
        B(415864, "Cou052", 91 , 158, 46 ),
        B(415904, "Cou053", 36 , 39 , 49 ),
        A(415944, "Dia001", 265, 54 , 57 ),
        A(415984, "Dia002", 43 , 44 , 30 ),
        A(416024, "Dia003", 28 , 53 , 86 ),
        A(416064, "Dia004", 105, 249, 254),
        A(416104, "Dia005", 256, 55 , 321),
        A(416144, "Dia006", 322, 324, 329),
        A(416184, "Dia007", 331, 330, 376),
        A(416224, "Dia008", 670, 347, 381),
        A(416264, "Dia009", 267, 348, 375),
        A(416304, "Dia010", 380, 390, 391),
        A(416344, "Dia011", 393, 427, 417),
        A(416384, "Dia012", 455, 421, 530),
        A(416424, "Dia013", 428, 465, 501),
        A(416464, "Dia014", 502, 429, 533),
        A(416504, "Dia015", 371, 491, 407),
        A(416544, "Dia016", 516, 372, 410),
        A(416584, "Dia017", 517, 411, 373),
        A(416624, "Dia018", 413, 519, 374),
        A(416664, "Dia019", 414, 377, 520),
        A(416704, "Dia020", 378, 415, 522),
        A(416744, "Dia021", 379, 523, 419),
        A(416784, "Dia022", 532, 382, 431),
        A(416824, "Dia023", 535, 432, 383),
        A(416864, "Dia024", 433, 537, 384),
        A(416904, "Dia025", 434, 385, 540),
        A(416944, "Dia026", 386, 435, 557),
        A(416984, "Dia027", 387, 567, 438),
        A(417024, "Dia028", 568, 388, 440),
        A(417064, "Dia029", 585, 441, 389),
        B(417104, "Dia030", 266, 311, 161),
        B(417144, "Dia031", 267, 171, 312),
        B(417184, "Dia032", 178, 270, 313),
        B(417224, "Dia033", 181, 314, 271),
        B(417264, "Dia034", 315, 185, 272),
        B(417304, "Dia035", 316, 273, 186),
        B(417344, "Dia036", 274, 317, 187),
        B(417384, "Dia037", 275, 189, 318),
        B(417424, "Dia038", 190, 276, 319),
        B(417464, "Dia039", 191, 325, 277),
        B(417504, "Dia040", 328, 192, 417),
        B(417544, "Dia041", 330, 286, 193),
        B(417584, "Dia042", 287, 331, 195),
        B(417624, "Dia043", 288, 196, 332),
        B(417664, "Dia044", 199, 290, 335),
        B(417704, "Dia045", 200, 338, 291),
        B(417744, "Dia046", 340, 235, 292),
        B(417784, "Dia047", 341, 293, 236),
        B(417824, "Dia048", 529, 530, 48 ),
        B(417864, "Eda001", 502, 507, 504),
        B(417904, "Eda002", 499, 500, 501),
        B(417944, "Eda003", 486, 486, 480), // interesting
        B(417984, "Eda004", 503, 506, 505),
        B(418024, "Eda005", 481, 482, 517),
        A(418064, "Ekm001", 680, 692, 682),
        A(418104, "Ekm002", 693, 628, 694),
        A(418144, "Ekm003", 188, 174, 178),
        A(418184, "Ekm004", 163, 171, 176),
        A(418224, "Ekm005", 173, 177, 175),
        A(418264, "Ekm006", 702, 661, 770),
        AB(418304, "Env001",  408056, 401, 404, 344,  408116, 220, 232, 132),
        AB(418344, "Env002",  408062, 320, 346, 706,  408122, 122, 136, 464),
        AB(418384, "Env004",  408068, 668, 667, 669,  408128, 394, 389, 401),
        AB(418424, "Env005",  408074, 664, 647, 655,  408134, 387, 345, 350),
        AB(418464, "Env006",  408080, 658, 630, 596,  408140, 353, 333, 320),
        AB(418504, "Env007",  408086, 504, 514, 539,  408146, 294, 296, 298),
        AB(418544, "Env008",  408092, 524, 476, 452,  408152, 297, 289, 283),
        AB(418584, "Env010",  408098, 400, 396, 405,  408158, 218, 209, 234),
        AB(418624, "Env011",  408104, 399, 398, 403,  408164, 217, 214, 228),
        AB(418664, "Env012",  408110, 402, 397, 416,  408170, 224, 212, 242),
        B(418704, "Ere001", 425, 421, 323),
        B(418744, "Ere002", 321, 308, 305),
        B(418784, "Ere003", 301, 302, 280),
        B(418824, "Ere004", 304, 300, 306),
        B(418864, "Ere005", 303, 281, 282),
        B(418904, "Ere006", 307, 309, 310),
        B(418944, "Ere007", 278, 263, 265),
        B(418984, "Ere008", 264, 269, 279),
        B(419024, "Ere009", 166, 165, 169),
        B(419064, "Ere010", 159, 143, 162),
        A(419104, "Lda001", 1  , -1 , -1 ),
        A(419144, "Lda002", 238, 220, 235),
        A(419184, "Lda003", 2  , -1 , -1 ),
        A(419224, "Lda004", 191, 193, 195),
        A(419264, "Lda005", 240, 150, 144),
        A(419304, "Lda006", 133, 117, -1 ),
        A(419344, "Lda007", 99 , 420, 62 ),
        A(419384, "Lda008", 36 , 703, 696),
        A(419424, "Lda009", 154, 187, 613),
        A(419464, "Lda010", 3  , -1 , -1 ),
        A(419504, "Lda011", 593, 578, 437),
        A(419544, "Lda012", 4  , -1 , -1 ),
        A(419584, "Lda013", 5  , -1 , -1 ),
        A(419624, "Lda014", 6  , -1 , -1 ),
        A(419664, "Lda015", 7  , -1 , -1 ),
        A(419704, "Lda016", 8  , -1 , -1 ),
        A(419744, "Lda017", 687, 686, 704),
        A(419784, "Lda018", 691, 701, 648),
        A(419824, "Lda019", 674, 673, 430),
        A(419864, "Lda020", 155, 518, 37 ),
        A(419904, "Lda021", 688, 769, 689),
        A(419944, "Lda022", 21 , 425, 26 ),
        A(419984, "Lda023", 22 , 27 , 426),
        A(420024, "Lda024", 9  , -1 , -1 ),
        A(420064, "Lda025", 20 , 448, 23 ),
        A(420104, "Lda026", 449, 164, 24 ),
        A(420144, "Lda027", 451, 25 , 165),
        A(420184, "Lda028", 342, 453, 166),
        A(420224, "Lda029", 45 , -1 , -1 ),
        A(420264, "Lda030", 167, -1 , -1 ),
        A(420304, "Lda031", 170, 456, 47 ),
        A(420344, "Lda032", 457, 189, 48 ),
        A(420384, "Lda033", 458, 51 , 237),
        A(420424, "Lda034", 56 , 33 , 239),
        A(420464, "Lda035", 59 , 241, 466),
        A(420504, "Lda036", 242, 60 , 34 ),
        A(420544, "Lda037", 243, 460, 61 ),
        A(420584, "Lda038", 470, 255, 64 ),
        A(420624, "Lda039", 471, 65 , 311),
        A(420664, "Lda040", 73 , 472, 312),
        A(420704, "Lda041", 77 , 313, 473),
        A(420744, "Lda042", 314, 78 , 474),
        A(420784, "Lda043", 315, 477, 84 ),
        A(420824, "Lda044", 478, 316, 89 ),
        A(420864, "Lda045", 624, 90 , 317),
        A(420904, "Lda046", 91 , 278, 318),
        A(420944, "Lda047", 96 , 319, 481),
        A(420984, "Lda048", 345, 97 , 482),
        A(421024, "Lda049", 10 , -1 , -1 ),
        A(421064, "Lda050", 483, 436, 100),
        A(421104, "Lda051", 484, 101, 444),
        A(421144, "Lda052", 113, 485, 459),
        A(421184, "Lda053", 120, 288, 486),
        A(421224, "Lda054", 626, 127, 489),
        A(421264, "Lda055", 784, 493, 128),
        A(421304, "Lda056", 494, 420, 129),
        A(421344, "Lda057", 232, 130, 671),
        A(421384, "Lda058", 131, 497, 672),
        A(421424, "Lda059", 132, 675, 499),
        A(421464, "Lda060", 676, 134, 500),
        A(421504, "Lda061", 681, 503, 135),
        A(421544, "Lda062", 11 , -1 , -1 ),
        A(421584, "Lda063", 505, 136, 683),
        A(421624, "Lda064", 137, 506, 684),
        A(421664, "Lda065", 138, 685, 508),
        A(421704, "Lda066", 690, 139, 509),
        A(421744, "Lda067", 695, 510, 141),
        A(421784, "Lda068", 343, 697, 142),
        A(421824, "Lda069", 511, 143, 698),
        A(421864, "Lda070", 146, 512, 699),
        A(421904, "Lda071", 147, 700, 513),
        A(421944, "Lda072", 705, 148, 515),
        A(421984, "Lda073", 12 , -1 , -1 ),
        A(422024, "Lda074", 525, 707, -1 ),
        A(422064, "Lda075", 149, 526, 708),
        A(422104, "Lda076", 151, 709, 527),
        A(422144, "Lda077", 710, 152, 530),
        A(422184, "Lda078", 711, 534, 153),
        A(422224, "Lda079", 538, 712, 156),
        A(422264, "Lda080", 422, 157, 713),
        A(422304, "Lda081", 158, 541, 714),
        A(422344, "Lda082", 159, 715, 542),
        A(422384, "Lda083", 716, 160, 543),
        A(422424, "Lda084", 13 , -1 , -1 ),
        A(422464, "Lda085", 552, 717, 161),
        A(422504, "Lda086", 554, 162, 718),
        A(422544, "Lda087", 168, 560, 719),
        A(422584, "Lda088", 423, 720, 563),
        A(422624, "Lda089", 721, 169, 573),
        A(422664, "Lda090", 722, 574, 172),
        A(422704, "Lda091", 575, 723, 203),
        A(422744, "Lda092", 576, 207, 724),
        A(422784, "Lda093", 208, 581, 725),
        A(422824, "Lda094", 424, 726, 587),
        A(422864, "Lda095", 727, 209, 588),
        A(422904, "Lda096", 728, 589, 210),
        A(422944, "Lda097", 590, 729, 212),
        A(422984, "Lda098", 591, 213, 730),
        A(423024, "Lda099", 214, -1 , -1 ),
        A(423064, "Lda100", 215, 732, 597),
        A(423104, "Lda101", 302, 216, 598),
        A(423144, "Lda102", 733, 599, 217),
        A(423184, "Lda103", 601, 734, 218),
        A(423224, "Lda104", 602, 219, 735),
        A(423264, "Lda105", 221, 603, 736),
        A(423304, "Lda106", 222, 737, 604),
        A(423344, "Lda107", 738, 223, 606),
        A(423384, "Lda108", 739, 609, 224),
        A(423424, "Lda109", 610, 740, 225),
        A(423464, "Lda110", 611, 226, 741),
        A(423504, "Lda111", 227, 612, 742),
        A(423544, "Lda112", 228, 743, 614),
        A(423584, "Lda113", 731, 229, 615),
        A(423624, "Lda114", 745, 616, 230),
        A(423664, "Lda115", 617, 746, 233),
        A(423704, "Lda116", 619, 234, 747),
        A(423744, "Lda117", 270, 620, 748),
        A(423784, "Lda118", 271, 749, 621),
        A(423824, "Lda119", 750, 273, 622),
        A(423864, "Lda120", 751, 623, 274),
        A(423904, "Lda121", 479, 752, 275),
        A(423944, "Lda122", 625, 276, 753),
        A(423984, "Lda123", 480, 627, 754),
        A(424024, "Lda124", 283, 755, 632),
        A(424064, "Lda125", 756, 284, 633),
        A(424104, "Lda126", 757, 634, 285),
        A(424144, "Lda127", 635, 758, 286),
        A(424184, "Lda128", 636, 287, 759),
        A(424224, "Lda129", 628, 637, 760),
        A(424264, "Lda130", 289, 761, 638),
        A(424304, "Lda131", 762, 290, 639),
        A(424344, "Lda132", 763, 640, 292),
        A(424384, "Lda133", 641, 764, 293),
        A(424424, "Lda134", 642, 294, 765),
        A(424464, "Lda135", 295, 643, 766),
        A(424504, "Lda136", 296, 767, 644),
        A(424544, "Lda137", 768, 297, 645),
        A(424584, "Lda138", 629, 646, 298),
        A(424624, "Lda139", 649, 771, 299),
        A(424664, "Lda140", 663, 300, 772),
        A(424704, "Lda141", 301, 650, 773),
        A(424744, "Lda142", 231, 774, 652),
        A(424784, "Lda143", 775, 303, 653),
        A(424824, "Lda144", 776, 654, 304),
        A(424864, "Lda145", 656, 777, 305),
        A(424904, "Lda146", 657, 306, 778),
        A(424944, "Lda147", 307, 659, 779),
        A(424984, "Lda148", 308, 780, -1 ),
        A(425024, "Lda149", 781, 309, 660),
        A(425064, "Lda150", 782, 662, 310),
        B(425104, "Lda151", 361, 508, 92 ),
        B(425144, "Lda152", 362, 93 , 509),
        B(425184, "Lda153", 94 , 363, 510),
        B(425224, "Lda154", 95 , 511, 364),
        B(425264, "Lda155", 512, 96 , 366),
        B(425304, "Lda156", 513, 367, 97 ),
        B(425344, "Lda157", 368, 514, 98 ),
        B(425384, "Lda158", 369, 99 , 515),
        B(425424, "Lda159", 100, 346, 516),
        B(425464, "Lda160", 101, 536, 370),
        B(425504, "Lda161", 537, 102, 371),
        B(425544, "Lda162", 538, 372, 103),
        B(425584, "Lda163", 373, 539, 104),
        B(425624, "Lda164", 374, -1 , -1 ),
        B(425664, "Lda165", 105, 375, 540),
        B(425704, "Lda166", 106, 541, 376),
        B(425744, "Lda167", 542, 107, 380),
        B(425784, "Lda168", 543, 381, 108),
        B(425824, "Lda169", 382, 544, 109),
        B(425864, "Lda170", 383, 110, 545),
        B(425904, "Lda171", 0  , -1 , -1 ),
        B(425944, "Lda172", 111, 546, 385),
        B(425984, "Lda173", 547, 112, 386),
        B(426024, "Lda174", 548, 388, 113),
        B(426064, "Lda175", 392, 549, 114),
        B(426104, "Lda176", 393, 115, 83 ),
        B(426144, "Lda177", 1  , -1 , -1 ),
        B(426184, "Lda178", 116, 124, 395),
        B(426224, "Lda179", 170, 117, 396),
        B(426264, "Lda180", 194, 397, 118),
        B(426304, "Lda181", 400, 284, 119),
        B(426344, "Lda182", 402, 120, 285),
        B(426384, "Lda183", 121, 403, 326),
        B(426424, "Lda184", 123, 327, 404),
        B(426464, "Lda185", 409, 125, 405),
        B(426504, "Lda186", 410, 407, 295),
        B(426544, "Lda187", 408, 411, 127),
        B(426584, "Lda188", 438, 128, 415),
        B(426624, "Lda189", 129, 439, 416),
        B(426664, "Lda190", 130, 419, 459),
        B(426704, "Lda191", 420, 131, 462),
        B(426744, "Lda192", 2  , -1 , -1 ),
        B(426784, "Lda193", 422, 463, 133),
        B(426824, "Lda194", 470, 423, 134),
        B(426864, "Lda195", 471, 135, 426),
        B(426904, "Lda196", 137, 472, 427),
        B(426944, "Lda197", 150, 428, 473),
        B(426984, "Lda198", 429, 154, 475),
        B(427024, "Lda199", 430, 476, 155),
        B(427064, "Lda200", 3  , -1 , -1 ),
        B(427104, "Lda201", 483, 156, 431),
        B(427144, "Lda202", 163, 484, 432),
        B(427184, "Lda203", 164, 433, 485),
        B(427224, "Lda204", 434, 203, 487),
        B(427264, "Lda205", 435, 488, 204),
        B(427304, "Lda206", 489, 436, 205),
        B(427344, "Lda207", 490, 211, 437),
        B(427384, "Lda208", 213, 491, 440),
        B(427424, "Lda209", 215, 441, 492),
        B(427464, "Lda210", 442, 216, 493),
        B(427504, "Lda211", 446, 494, 219),
        B(427544, "Lda212", 4  , -1 , -1 ),
        B(427584, "Lda213", 495, 221, 447),
        B(427624, "Lda214", 222, 496, 448),
        B(427664, "Lda215", 223, 449, 497),
        B(427704, "Lda216", 450, 225, 498),
        B(427744, "Lda217", 451, 518, 229),
        B(427784, "Lda218", 519, 452, 230),
        B(427824, "Lda219", 520, 231, 453),
        B(427864, "Lda220", 233, 521, 454),
        B(427904, "Lda221", 455, 239, 522),
        B(427944, "Lda222", 458, 523, 241),
        B(427984, "Lda223", 5  , -1 , -1 ),
        B(428024, "Lda224", 524, 243, 460),
        B(428064, "Lda225", 244, 525, 461),
        B(428104, "Lda226", 246, 465, 526),
        B(428144, "Lda227", 466, 247, 527),
        B(428184, "Lda228", 467, 528, 248),
        B(428224, "Lda229", 6  , -1 , -1 ),
        B(428264, "Lda230", 7  , -1 , -1 ),
        B(428304, "Lda231", 299, 531, 469),
        B(428344, "Lda232", 8  , -1 , -1 ),
        B(428384, "Lda233", 474, 76 , 532),
        B(428424, "Lda234", 477, 533, 250),
        B(428464, "Lda235", 534, 478, 251),
        B(428504, "Lda236", 535, 252, 479),
        B(428544, "Lda237", 556, 557, -1 ),
        A(428584, "Tda001", 266, 665, 677),
        A(428624, "Tda002", 666, 592, 192),
        A(428664, "Tda003", 462, 678, 679),
        A(428704, "Tda004", 463, 204, 450),
        A(428744, "Tda005", 467, 454, 205),
        A(428784, "Tda006", 468, 38 , 206),
        A(428824, "Tda007", 469, 211, 39 ),
        A(428864, "Tda008", 488, 475, 40 ),
        A(428904, "Tda009", 496, 41 , 487),
        A(428944, "Tda010", 594, 245, 498),
        A(428984, "Tda011", 14 , -1 , -1 ),
        A(429024, "Tda012", 507, 58 , 269),
        A(429064, "Tda013", 521, 277, 66 ),
        A(429104, "Tda014", 15 , -1 , -1 ),
        A(429144, "Tda015", 279, 67 , 528),
        A(429184, "Tda016", 68 , 280, 544),
        A(429224, "Tda017", 69 , 547, 281),
        A(429264, "Tda018", 548, 70 , 325),
        A(429304, "Tda019", 549, 335, 71 ),
        A(429344, "Tda020", 337, 550, 72 ),
        A(429384, "Tda021", 350, 74 , 553),
        A(429424, "Tda022", 76 , 351, 555),
        A(429464, "Tda023", 79 , 556, 352),
        A(429504, "Tda024", 16 , -1 , -1 ),
        A(429544, "Tda025", 558, 353, 80 ),
        A(429584, "Tda026", 354, 559, 82 ),
        A(429624, "Tda027", 355, 83 , 561),
        A(429664, "Tda028", 93 , 356, 562),
        A(429704, "Tda029", 94 , 564, 783),
        A(429744, "Tda030", 565, 95 , 358),
        A(429784, "Tda031", 566, 359, 98 ),
        A(429824, "Tda032", 360, 569, 102),
        A(429864, "Tda033", 361, 114, 570),
        A(429904, "Tda034", 115, 362, 571),
        A(429944, "Tda035", 116, 572, 363),
        A(429984, "Tda036", 579, 118, 364),
        A(430024, "Tda037", 580, 365, 119),
        A(430064, "Tda038", 17 , -1 , -1 ),
        A(430104, "Tda039", 366, 121, 582),
        A(430144, "Tda040", 18 , -1 , -1 ),
        A(430184, "Tda041", 123, 42 , -1 ),
        A(430224, "Tda042", 595, 124, 367),
        A(430264, "Tda043", 600, 368, 125),
        A(430304, "Tda044", 369, 605, 126),
        B(430344, "Tda045", 152, 14 , 322),
        B(430384, "Tda046", 15 , 153, 324),
        B(430424, "Tda047", 16 , 329, 157),
        B(430464, "Tda048", 334, 17 , 160),
        B(430504, "Tda049", 336, 167, 18 ),
        B(430544, "Tda050", 168, 337, 19 ),
        B(430584, "Tda051", 172, 20 , -1 ),
        B(430624, "Tda052", 21 , 173, 339),
        B(430664, "Tda053", 22 , 342, 174),
        B(430704, "Tda054", 343, 28 , 175),
        B(430744, "Tda055", 344, 176, 29 ),
        B(430784, "Tda056", 9  , -1 , -1 ),
        B(430824, "Tda057", 177, 35 , 347),
        B(430864, "Tda058", 40 , 179, 348),
        B(430904, "Tda059", 41 , 349, 180),
        B(430944, "Tda060", 351, 42 , 182),
        B(430984, "Tda061", 352, 183, 43 ),
        B(431024, "Tda062", 184, 354, 44 ),
        B(431064, "Tda063", 188, 47 , 355),
        B(431104, "Tda064", 50 , 197, 356),
        B(431144, "Tda065", 51 , 357, 198),
        B(431184, "Tda066", 10 , -1 , -1 ),
        B(431224, "Tda067", 358, 201, 52 ),
        B(431264, "Tda068", 202, 359, 53 ),
        B(431304, "Tda069", 206, 54 , 360),
        B(431344, "Tda070", 55 , 207, 365),
        B(431384, "Tda071", 56 , 377, 208),
        B(431424, "Tda072", 378, 57 , 210),
        B(431464, "Tda073", 379, 226, 58 ),
        B(431504, "Tda074", 227, 384, 59 ),
        B(431544, "Tda075", 237, 60 , 390),
        B(431584, "Tda076", 61 , 238, 391),
        B(431624, "Tda077", 62 , 398, 240),
        B(431664, "Tda078", 399, 63 , 245),
        B(431704, "Tda079", 11 , -1 , -1 ),
        B(431744, "Tda080", 249, 406, 64 ),
        B(431784, "Tda081", 253, 65 , 412),
        B(431824, "Tda082", 66 , 254, 413),
        B(431864, "Tda083", 67 , 414, 255),
        B(431904, "Tda084", 418, 68 , 256),
        B(431944, "Tda085", 424, 257, 69 ),
        B(431984, "Tda086", 258, 443, 70 ),
        B(432024, "Tda087", 259, 71 , 444),
        B(432064, "Tda088", 72 , 260, 445),
        B(432104, "Tda089", 73 , 456, 261),
        B(432144, "Tda090", 457, 74 , 262),
        B(432184, "Tda091", 468, 268,  75),
        B(432224, "Tda092", 12,   -1,  -1),
    };
    // </editor-fold>

}
