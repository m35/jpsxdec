/**
 * JPEGImage.java
 *
 * The authors make NO WARRANTY or representation, either express or implied,
 * with respect to this software, its quality, accuracy, merchantability, or
 * fitness for a particular purpose.  This software is provided "AS IS", and you,
 * its user, assume the entire risk as to its quality and accuracy.
 *
 * This software is copyright (C) 1991-1998, Thomas G. Lane.
 * All Rights Reserved except as specified below.
 *
 * Permission is hereby granted to use, copy, modify, and distribute this
 * software (or portions thereof) for any purpose, without fee, subject to these
 * conditions:
 * (1) If any part of the source code for this software is distributed, then this
 * README file must be included, with this copyright and no-warranty notice
 * unaltered; and any additions, deletions, or changes to the original files
 * must be clearly indicated in accompanying documentation.
 * (2) If only executable code is distributed, then the accompanying
 * documentation must state that "this software is based in part on the work of
 * the Independent JPEG Group".
 * (3) Permission for use of this software is granted only if the user accepts
 * full responsibility for any undesirable consequences; the authors accept
 * NO LIABILITY for damages of any kind.
 *
 * These conditions apply to any software derived from or based on the IJG code,
 * not just to the unmodified library.  If you use our work, you ought to
 * acknowledge us.
 *
 * Permission is NOT granted for the use of any IJG author's name or company name
 * in advertising or publicity relating to this software or products derived from
 * it.  This software may be referred to only as "the Independent JPEG Group's
 * software".
 *
 * We specifically permit and encourage the use of this software as the basis of
 * commercial products, provided that all warranty or liability claims are
 * assumed by the product vendor.
 */

package com.pdfjet;

import java.lang.*;
import java.io.*;
import java.util.*;


//>>>>pdfjet {
public class JPEGImage {

    static final char M_SOF0  = (char) 0x00C0;  // Start Of Frame N
    static final char M_SOF1  = (char) 0x00C1;  // N indicates which compression process
    static final char M_SOF2  = (char) 0x00C2;  // Only SOF0-SOF2 are now in common use
    static final char M_SOF3  = (char) 0x00C3;
    static final char M_SOF5  = (char) 0x00C5;  // NB: codes C4 and CC are NOT SOF markers
    static final char M_SOF6  = (char) 0x00C6;
    static final char M_SOF7  = (char) 0x00C7;
    static final char M_SOF9  = (char) 0x00C9;
    static final char M_SOF10 = (char) 0x00CA;
    static final char M_SOF11 = (char) 0x00CB;
    static final char M_SOF13 = (char) 0x00CD;
    static final char M_SOF14 = (char) 0x00CE;
    static final char M_SOF15 = (char) 0x00CF;

    static final char M_SOS   = (char) 0x00DA;  // Start Of Scan (begins compressed data)

    int width  = 0;
    int height = 0;
    int colorComponents = 0;

    ByteArrayInputStream bais = null;
    byte[] data;


    public JPEGImage(java.io.InputStream inputStream) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int count;
        while ((count = inputStream.read(buf, 0, buf.length)) != -1) {
            baos.write(buf, 0, count);
        }
        data = baos.toByteArray();
        bais = new ByteArrayInputStream(data);

        char ch1 = (char) bais.read();
        char ch2 = (char) bais.read();
        if (ch1 == 0x00FF && ch2 == 0x00D8) {
            boolean foundSOFn = false;
            for (;;) {
                char ch = nextMarker(inputStream);
                switch (ch) {
                    // Note that marker codes 0xC4, 0xC8, 0xCC are not,
                    // and must not be treated as SOFn. C4 in particular
                    // is actually DHT.
                    case M_SOF0:    // Baseline
                    case M_SOF1:    // Extended sequential, Huffman
                    case M_SOF2:    // Progressive, Huffman
                    case M_SOF3:    // Lossless, Huffman
                    case M_SOF5:    // Differential sequential, Huffman
                    case M_SOF6:    // Differential progressive, Huffman
                    case M_SOF7:    // Differential lossless, Huffman
                    case M_SOF9:    // Extended sequential, arithmetic
                    case M_SOF10:   // Progressive, arithmetic
                    case M_SOF11:   // Lossless, arithmetic
                    case M_SOF13:   // Differential sequential, arithmetic
                    case M_SOF14:   // Differential progressive, arithmetic
                    case M_SOF15:   // Differential lossless, arithmetic
                    // Skip 3 bytes to get to the image height and width
                    bais.read();
                    bais.read();
                    bais.read();
                    height = readTwoBytes(inputStream);
                    width = readTwoBytes(inputStream);
                    colorComponents = bais.read();
                    foundSOFn = true;
                    break;

                    default:
                    skipVariable(inputStream);
                    break;
                }

                if (foundSOFn) break;
            }
        } else {
            throw new Exception();
        }

    }


    public int getWidth() {
        return this.width;
    }


    public int getHeight() {
        return this.height;
    }


    public int getColorComponents() {
        return this.colorComponents;
    }


    public byte[] getData() {
        return this.data;
    }


    private int readTwoBytes(java.io.InputStream inputStream) throws Exception {
        int value = bais.read();
        value <<= 8;
        value |= bais.read();
        return value;
    }


    // Find the next JPEG marker and return its marker code.
    // We expect at least one FF byte, possibly more if the compressor
    // used FFs to pad the file.
    // There could also be non-FF garbage between markers. The treatment
    // of such garbage is unspecified; we choose to skip over it but emit
    // a warning msg.
    // NB: this routine must not be used after seeing SOS marker, since
    // it will not deal correctly with FF/00 sequences in the compressed
    // image data...
    private char nextMarker(java.io.InputStream inputStream) throws Exception {
        int discarded_bytes = 0;
        char ch = ' ';

        // Find 0xFF byte; count and skip any non-FFs.
        ch = (char) bais.read();
        while (ch != 0x00FF) {
            discarded_bytes++;
            ch = (char) bais.read();
        }

        // Get marker code byte, swallowing any duplicate FF bytes. Extra FFs
        // are legal as pad bytes, so don't count them in discarded_bytes.
        do {
            ch = (char) bais.read();
        } while (ch == 0x00FF);

        if (discarded_bytes != 0) {
            throw new Exception();
        }

        return ch;
    }


    // Most types of marker are followed by a variable-length parameter
    // segment. This routine skips over the parameters for any marker we
    // don't otherwise want to process.
    // Note that we MUST skip the parameter segment explicitly in order
    // not to be fooled by 0xFF bytes that might appear within the
    // parameter segment such bytes do NOT introduce new markers.
    private void skipVariable(java.io.InputStream inputStream) throws Exception {
        // Get the marker parameter length count
        int length = readTwoBytes(inputStream);

        // Length includes itself, so must be at least 2
        if (length < 2) {
            throw new Exception();
        }
        length -= 2;
        // Skip over the remaining bytes
        while (length > 0) {
            bais.read();
            length--;
        }
    }

}   // End of JPEGImage.java
//<<<<}
