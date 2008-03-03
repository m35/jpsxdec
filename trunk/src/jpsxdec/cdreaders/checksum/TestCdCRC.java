
package jpsxdec.cdreaders.checksum;

import java.io.*;

/** This code has been ported from cdrdao
 * http://ftp.fmed.uc.pt/pub/OpenBSD/distfiles/cdrdao-1.1.3.src.tar.gz 
 * and the Ubiquitous Amiga Emulator
 * http://www.angstrom-distribution.org/unstable/sources/e-uae-0.8.28.tar.bz2
 * both of which are released under the GNU GENERAL PUBLIC LICENSE ver 2.
 */
public class TestCdCRC {

    public static void main(String[] args) throws IOException {
        FileInputStream fis = new FileInputStream("bsector.bin");
        byte[] ab = new byte[2352];
        if (ab.length != fis.read(ab)) throw new IOException("Failed to read file");
        fis.close();
        
        //byte[] ab2 = ab.clone();
        
        FileOutputStream fso;
        
        int iSectorNum = 7621; // 82819;
        int iType = EccEdcEncoding.MODE_2_FORM_2;
        
        if (iType == EccEdcEncoding.MODE_2_FORM_1) {
        
            // clear fields we'll build
            for (int i=0; i<16; i++) ab[i] = 0;
            for (int i=0; i<4 + 276; i++) ab[16+8+2048+i] = 0;

            EccEdcEncoding.do_encode_L2(ab, EccEdcEncoding.MODE_2_FORM_1, iSectorNum);

        } else if (iType == EccEdcEncoding.MODE_2_FORM_2) {
            // clear fields we'll build
            for (int i=0; i<16; i++) ab[i] = 0;
            for (int i=0; i<4; i++) ab[16+8+2324+i] = 0;
            
            EccEdcEncoding.do_encode_L2(ab, EccEdcEncoding.MODE_2_FORM_2, iSectorNum);
        }
        fso = new FileOutputStream("out.bin");
        fso.write(ab);
        fso.close();
    }

}
