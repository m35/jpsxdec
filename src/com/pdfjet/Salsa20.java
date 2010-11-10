/**
 *
 * The Salsa20 encryption function was designed and originally implemented
 * by Daniel J. Bernstein:
 *
 * http://cr.yp.to/salsa20.html
 * http://cr.yp.to/snuffle/ip.pdf
 *
 * The following Java implementation encrypts the system time and returns
 * the first 32 characters of the hash.
 *
 */

package com.pdfjet;

import java.lang.*;


public class Salsa20 {

    private String id = null;


    public Salsa20() {

/*
The Original Specification:

    #define R(a,b) (((a) << (b)) | ((a) >> (32 - (b))))
    void salsa20_word_specification(uint32 out[16],uint32 in[16]) {
        int i;
        uint32 x[16];
        for (i = 0;i < 16;++i) x[i] = in[i];
        for (i = 20;i > 0;i -= 2) {
            x[ 4] ^= R(x[ 0]+x[12], 7);  x[ 8] ^= R(x[ 4]+x[ 0], 9);
            x[12] ^= R(x[ 8]+x[ 4],13);  x[ 0] ^= R(x[12]+x[ 8],18);
            x[ 9] ^= R(x[ 5]+x[ 1], 7);  x[13] ^= R(x[ 9]+x[ 5], 9);
            x[ 1] ^= R(x[13]+x[ 9],13);  x[ 5] ^= R(x[ 1]+x[13],18);
            x[14] ^= R(x[10]+x[ 6], 7);  x[ 2] ^= R(x[14]+x[10], 9);
            x[ 6] ^= R(x[ 2]+x[14],13);  x[10] ^= R(x[ 6]+x[ 2],18);
            x[ 3] ^= R(x[15]+x[11], 7);  x[ 7] ^= R(x[ 3]+x[15], 9);
            x[11] ^= R(x[ 7]+x[ 3],13);  x[15] ^= R(x[11]+x[ 7],18);
            x[ 1] ^= R(x[ 0]+x[ 3], 7);  x[ 2] ^= R(x[ 1]+x[ 0], 9);
            x[ 3] ^= R(x[ 2]+x[ 1],13);  x[ 0] ^= R(x[ 3]+x[ 2],18);
            x[ 6] ^= R(x[ 5]+x[ 4], 7);  x[ 7] ^= R(x[ 6]+x[ 5], 9);
            x[ 4] ^= R(x[ 7]+x[ 6],13);  x[ 5] ^= R(x[ 4]+x[ 7],18);
            x[11] ^= R(x[10]+x[ 9], 7);  x[ 8] ^= R(x[11]+x[10], 9);
            x[ 9] ^= R(x[ 8]+x[11],13);  x[10] ^= R(x[ 9]+x[ 8],18);
            x[12] ^= R(x[15]+x[14], 7);  x[13] ^= R(x[12]+x[15], 9);
            x[14] ^= R(x[13]+x[12],13);  x[15] ^= R(x[14]+x[13],18);
        }
        for (i = 0;i < 16;++i) out[i] = x[i] + in[i];
    }

Test input:
        int[] a_in = new int[] {
                0x61707865, 0x04030201, 0x08070605, 0x0c0b0a09, 
                0x100f0e0d, 0x3320646e, 0x01040103, 0x06020905, 
                0x00000007, 0x00000000, 0x79622d32, 0x14131211, 
                0x18171615, 0x1c1b1a19, 0x201f1e1d, 0x6b206574};

The expected output:
0xb9a205a3,
0x0695e150,
0xaa94881a,
0xadb7b12c,
0x798942d4,
0x26107016,
0x64edb1a4,
0x2d27173f,
0xb1c7f1fa,
0x62066edc,
0xe035fa23,
0xc4496f04,
0x2131e6b3,
0x810bde28,
0xf62cb407,
0x6bdede3d.
*/

        int[] a_in = new int[16];

        StringBuilder buf = new StringBuilder(
                Long.toHexString(System.currentTimeMillis()));
        int len = 128 - buf.length();
        for (int i = 0; i < len; i++) {
            buf.append('0');
        }

        for (int i = 0; i < 128; i += 8) { 
            a_in[i/8] = (int) Long.parseLong(buf.substring(i, i + 8), 16);
        }


        id = bin2hex(salsa20_word_specification(a_in));
        // System.out.println(id);
    }


    // Use unsigned right shift >>> !
    private int R(int a, int b) {
        return (a << b) | (a >>> (32 - b));
    }


    private int[] salsa20_word_specification(int[] a_in) {

        int[] a_out = new int[16];
        int[] x = new int[16];

        for (int i = 0; i < 16; ++i) {
            x[i] = a_in[i];
        }

        for (int i = 20;i > 0;i -= 2) {
            x[ 4] ^= R(x[ 0]+x[12], 7);  x[ 8] ^= R(x[ 4]+x[ 0], 9);
            x[12] ^= R(x[ 8]+x[ 4],13);  x[ 0] ^= R(x[12]+x[ 8],18);
            x[ 9] ^= R(x[ 5]+x[ 1], 7);  x[13] ^= R(x[ 9]+x[ 5], 9);
            x[ 1] ^= R(x[13]+x[ 9],13);  x[ 5] ^= R(x[ 1]+x[13],18);
            x[14] ^= R(x[10]+x[ 6], 7);  x[ 2] ^= R(x[14]+x[10], 9);
            x[ 6] ^= R(x[ 2]+x[14],13);  x[10] ^= R(x[ 6]+x[ 2],18);
            x[ 3] ^= R(x[15]+x[11], 7);  x[ 7] ^= R(x[ 3]+x[15], 9);
            x[11] ^= R(x[ 7]+x[ 3],13);  x[15] ^= R(x[11]+x[ 7],18);
            x[ 1] ^= R(x[ 0]+x[ 3], 7);  x[ 2] ^= R(x[ 1]+x[ 0], 9);
            x[ 3] ^= R(x[ 2]+x[ 1],13);  x[ 0] ^= R(x[ 3]+x[ 2],18);
            x[ 6] ^= R(x[ 5]+x[ 4], 7);  x[ 7] ^= R(x[ 6]+x[ 5], 9);
            x[ 4] ^= R(x[ 7]+x[ 6],13);  x[ 5] ^= R(x[ 4]+x[ 7],18);
            x[11] ^= R(x[10]+x[ 9], 7);  x[ 8] ^= R(x[11]+x[10], 9);
            x[ 9] ^= R(x[ 8]+x[11],13);  x[10] ^= R(x[ 9]+x[ 8],18);
            x[12] ^= R(x[15]+x[14], 7);  x[13] ^= R(x[12]+x[15], 9);
            x[14] ^= R(x[13]+x[12],13);  x[15] ^= R(x[14]+x[13],18);
        }

        for (int i = 0; i < 16; ++i) {
            a_out[i] = x[i] + a_in[i];
        }

        return a_out;
    }


    private String bin2hex(int[] binarray) {

        String table = "0123456789abcdef";
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < binarray.length; i++) {
            int a = binarray[i];
            buf.append(table.charAt(a>>28 & 0x0000000f));
            buf.append(table.charAt(a>>24 & 0x0000000f));
            buf.append(table.charAt(a>>20 & 0x0000000f));
            buf.append(table.charAt(a>>16 & 0x0000000f));
            buf.append(table.charAt(a>>12 & 0x0000000f));
            buf.append(table.charAt(a>> 8 & 0x0000000f));
            buf.append(table.charAt(a>> 4 & 0x0000000f));
            buf.append(table.charAt(a>> 0 & 0x0000000f));
        }

        return buf.substring(0, 32);

    }


    public String getID() {
        return id;
    }


    public static void main(String[] args) {
        new Salsa20();
    }

}
