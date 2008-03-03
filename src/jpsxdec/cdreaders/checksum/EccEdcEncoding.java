
package jpsxdec.cdreaders.checksum;

/** This code has been ported from cdrdao: edc_ecc.c
 * http://ftp.fmed.uc.pt/pub/OpenBSD/distfiles/cdrdao-1.1.3.src.tar.gz 
 * Copyright 1998 by Heiko Eissfeldt
 * and the Ubiquitous Amiga Emulator: cdrom.c
 * http://www.angstrom-distribution.org/unstable/sources/e-uae-0.8.28.tar.bz2
 * both of which are released under the GNU GENERAL PUBLIC LICENSE ver 2.
 */
public class EccEdcEncoding implements EncodeTables, CrcTable {
    
    public final static int MODE_0         = 0;
    public final static int MODE_1         = 1;
    public final static int MODE_2         = 2;
    public final static int MODE_2_FORM_1  = 3;
    public final static int MODE_2_FORM_2  = 4;
    
    private final static byte[] SYNCPATTERN = new byte[] 
    {0x00,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff};

    /* Layer 2 Product code en/decoder */
    public static int do_encode_L2(byte inout[/*(12 + 4 + L2_RAW+4+8+L2_Q+L2_P)*/], int sectortype, int sector)
    {
        assert(inout.length == 2352);
        long result;

        /* supply initial sync pattern */
        for (int i=0; i<SYNCPATTERN.length; i++) inout[i] = SYNCPATTERN[i];

        switch (sectortype) {
            case MODE_2_FORM_1:
                result = build_edc(inout, 16, 16+8+2048-1);
                inout[2072+0] = (byte)(result >> 0L);
                inout[2072+1] = (byte)(result >> 8L);
                inout[2072+2] = (byte)(result >> 16L);
                inout[2072+3] = (byte)(result >> 24L);

                /* clear header for P/Q parity calculation */
                inout[12] = 0;
                inout[12+1] = 0;
                inout[12+2] = 0;
                inout[12+3] = 0;
                encode_L2_P(inout, 12);
                encode_L2_Q(inout, 12);
                build_address(inout, sectortype, sector);
                break;
            case MODE_2_FORM_2:
                build_address(inout, sectortype, sector);
                result = build_edc(inout, 16, 16+8+2324-1);
                inout[2348+0] = (byte)(result >> 0L);
                inout[2348+1] = (byte)(result >> 8L);
                inout[2348+2] = (byte)(result >> 16L);
                inout[2348+3] = (byte)(result >> 24L);
                break;
            default:
                return -1;
        }

        return 0;
    }
    

    /* data sector definitions for RSPC */
    /* user data bytes per frame */
    private final static int L2_RAW = (1024*2);
    /* parity bytes for 16 bit units */
    private final static int L2_Q   = (26*2*2);
    private final static int L2_P   = (43*2*2);
    private final static int RS_L12_BITS = 8;

    private static long build_edc(byte inout[], int from, int upto) {
        int p = from;
        long result = 0;

        for (; from <= upto; from++) {
            result = EDC_crctable[(int) ((result ^ inout[p++]) & 0xffL)] ^ (result >> 8);
        }

        return result;
    }
    

    private static void encode_L2_Q(byte inout[], int pinout)
    {
        byte Q[];
        int pQ;
        int i,j;

        Q = inout;
        pQ = pinout + 4 + L2_RAW + 4 + 8 + L2_P;
        //memset(Q, 0, L2_Q);
        for (i=0; i<L2_Q; i++) Q[pQ+i] = 0;
        
        for (j = 0; j < 26; j++) {
            for (i = 0; i < 43; i++) {
                int data;
                /* LSB */
                data = inout[pinout+(j*43*2+i*2*44) % (4 + L2_RAW + 4 + 8 + L2_P)] &0xFF;
                if (data != 0) {
                    int base = rs_l12_log[data];
                    int sum = base + DQ[0][i];
                    if (sum >= ((1 << RS_L12_BITS)-1))
                        sum -= (1 << RS_L12_BITS)-1;
                    Q[pQ+0]    ^= rs_l12_alog[sum];
                    sum = base + DQ[1][i];
                    if (sum >= ((1 << RS_L12_BITS)-1))
                        sum -= (1 << RS_L12_BITS)-1;
                    Q[pQ+26*2] ^= rs_l12_alog[sum];
                }
                /* MSB */
                data = inout[pinout+(j*43*2+i*2*44+1) % (4 + L2_RAW + 4 + 8 + L2_P)] &0xFF;
                if (data != 0) {
                    int base = rs_l12_log[data];
                    int sum = base+DQ[0][i];
                    if (sum >= ((1 << RS_L12_BITS)-1))
                        sum -= (1 << RS_L12_BITS)-1;
                    Q[pQ+1]      ^= rs_l12_alog[sum];
                    sum = base + DQ[1][i];
                    if (sum >= ((1 << RS_L12_BITS)-1))
                        sum -= (1 << RS_L12_BITS)-1;
                    Q[pQ+26*2+1] ^= rs_l12_alog[sum];
                }
            }
            pQ += 2;
        }
    }

    private static void encode_L2_P(byte inout[/*4 + L2_RAW + 4 + 8 + L2_P*/], int pinout)
    {
        byte P[];
        int pP;
        int i,j;

        P = inout;
        pP = pinout + 4 + L2_RAW + 4 + 8;
        //memset(P, 0, L2_P);
        for (i=0; i<L2_P; i++) P[pP+i] = 0;
        
        for (j = 0; j < 43; j++) {
            for (i = 0; i < 24; i++) {
                int data;
                /* LSB */
                data = inout[pinout+i*2*43] &0xFF;
                if (data != 0) {
                    int base = rs_l12_log[data];
                    int sum = base + DP[0][i];
                    if (sum >= ((1 << RS_L12_BITS)-1))
                        sum -= (1 << RS_L12_BITS)-1;
                    P[pP+0]    ^= rs_l12_alog[sum];
                    sum = base + DP[1][i];
                    if (sum >= ((1 << RS_L12_BITS)-1))
                        sum -= (1 << RS_L12_BITS)-1;
                    P[pP+43*2] ^= rs_l12_alog[sum];
                }
                /* MSB */
                data = inout[pinout+i*2*43+1] &0xFF;
                if (data != 0) {
                    int base = rs_l12_log[data];
                    int sum = base + DP[0][i];
                    if (sum >= ((1 << RS_L12_BITS)-1))
                        sum -= (1 << RS_L12_BITS)-1;
                    P[pP+1]      ^= rs_l12_alog[sum];
                    sum = base + DP[1][i];
                    if (sum >= ((1 << RS_L12_BITS)-1))
                        sum -= (1 << RS_L12_BITS)-1;
                    P[pP+43*2+1] ^= rs_l12_alog[sum];
                }
            }
            pP += 2;
            pinout += 2;
        }
    }

    private static int build_address(byte inout[], int sectortype, int sector)
    {
        inout[12] = sectorToBCD( sector / (60*75));
        inout[13] = sectorToBCD((sector / 75) % 60);
        inout[14] = sectorToBCD( sector % 75);
        
        if (sectortype == MODE_0)
            inout[15] = 0;
        else if (sectortype == MODE_1)
            inout[15] = 1;
        else if (sectortype == MODE_2)
            inout[15] = 2;
        else if (sectortype == MODE_2_FORM_1)
            inout[15] = 2;
        else if (sectortype == MODE_2_FORM_2)
            inout[15] = 2;
        else
            return -1;
        
        return 0;
    }

    private static byte sectorToBCD(int v)
    {
        return (byte)( ((v / 10) << 4) | (v % 10) );
    }

    private void encode_l2 (byte p[], int address)
    {
        long v;

        p[0] = 0x00;
        //memset (p + 1, 0xff, 11);
        for (int i=1; i<11; i++) p[i]=(byte)0xff;

        p[12] = sectorToBCD(  address / (60 * 75) );
        p[13] = sectorToBCD( (address / 75) % 60 );
        p[14] = sectorToBCD(  address % 75 );
        p[15] = 1; /* MODE1 */
        v = build_edc(p, 0, 16 + 2048 - 1);
        p[2064 + 0] = (byte) (v >> 0);
        p[2064 + 1] = (byte) (v >> 8);
        p[2064 + 2] = (byte) (v >> 16);
        p[2064 + 3] = (byte) (v >> 24);
        //memset (p + 2064 + 4, 0, 8);
        for (int i=2064 + 4; i<8; i++) p[i]=0;
        encode_L2_P (p, 12);
        encode_L2_Q (p, 12);
    }
    
}
