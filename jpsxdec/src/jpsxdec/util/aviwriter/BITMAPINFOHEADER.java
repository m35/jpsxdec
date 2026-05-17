/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2026  Michael Sabin
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

package jpsxdec.util.aviwriter;

import java.io.IOException;
import java.io.RandomAccessFile;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;

/**
 * Represents the C BITMAPINFOHEADER structure.
 * https://docs.microsoft.com/en-us/windows/win32/api/wingdi/ns-wingdi-bitmapinfoheader
 * <blockquote>
 * The BITMAPINFOHEADER structure contains information about the dimensions and
 * color format of a device-independent bitmap (DIB).
 * </blockquote>
 */
public class BITMAPINFOHEADER extends AVIstruct {

    public final static int BI_RGB = 0;

    /**
     * Specifies the number of bytes required by the structure. This value does
     * not include the size of the color table or the size of the color masks,
     * if they are appended to the end of structure. See Remarks.
     */
    public final /*DWORD*/ int    biSize           = sizeof();

    /**
     * Specifies the width of the bitmap, in pixels. For information about
     * calculating the stride of the bitmap, see Remarks.
     */
    public       /*LONG */ int    biWidth;

    /**
     * Specifies the height of the bitmap, in pixels.
     *
     * <ul>
     * <li>For uncompressed RGB bitmaps, if biHeight is positive, the bitmap is
     * a bottom-up DIB with the origin at the lower left corner. If biHeight is
     * negative, the bitmap is a top-down DIB with the origin at the upper left
     * corner.
     * <li>For YUV bitmaps, the bitmap is always top-down, regardless of the
     * sign of biHeight. Decoders should offer YUV formats with positive
     * biHeight, but for backward compatibility they should accept YUV formats
     * with either positive or negative biHeight.
     * <li>For compressed formats, biHeight must be positive, regardless of
     * image orientation.
     * <ul>
     */
    public       /*LONG */ int    biHeight;

    /**
     * Specifies the number of planes for the target device. This value must be
     * set to 1.
     */
    public final /*WORD */ short  biPlanes = 1;

    /**
     * Specifies the number of bits per pixel (bpp). For uncompressed formats,
     * this value is the average number of bits per pixel. For compressed
     * formats, this value is the implied bit depth of the uncompressed image,
     * after the image has been decoded.
     */
    public       /*WORD */ short  biBitCount;

    /**
     * For compressed video and YUV formats, this member is a FOURCC code,
     * specified as a DWORD in little-endian order. For example, YUYV video has
     * the FOURCC 'VYUY' or 0x56595559. For more information, see FOURCC Codes.
     */
    public       /*DWORD*/ int    biCompression;

    /**
     * Specifies the size, in bytes, of the image. This can be set to 0 for
     * uncompressed RGB bitmaps.
     */
    public       /*DWORD*/ int    biSizeImage;

    /**
     * Specifies the horizontal resolution, in pixels per meter, of the target
     * device for the bitmap.
     */
    public       /*LONG */ int    biXPelsPerMeter;

    /**
     * Specifies the vertical resolution, in pixels per meter, of the target
     * device for the bitmap.
     */
    public       /*LONG */ int    biYPelsPerMeter;

    /**
     * Specifies the number of color indices in the color table that are
     * actually used by the bitmap. See Remarks for more information.
     */
    public       /*DWORD*/ int    biClrUsed;

    /**
     * Specifies the number of color indices that are considered important for
     * displaying the bitmap. If this value is zero, all colors are important.
     */
    public       /*DWORD*/ int    biClrImportant;

    public @Nonnull byte[] toBytes() {
        byte[] ab = new byte[sizeof()];
        /*DWORD*/ IO.writeInt32LE(ab,  0, biSize         );
        /*LONG */ IO.writeInt32LE(ab,  4, biWidth        );
        /*LONG */ IO.writeInt32LE(ab,  8, biHeight       );
        /*WORD */ IO.writeInt16LE(ab, 12, biPlanes       );
        /*WORD */ IO.writeInt16LE(ab, 14, biBitCount     );
        /*DWORD*/ IO.writeInt32LE(ab, 16, biCompression  );
        /*DWORD*/ IO.writeInt32LE(ab, 20, biSizeImage    );
        /*LONG */ IO.writeInt32LE(ab, 24, biXPelsPerMeter);
        /*LONG */ IO.writeInt32LE(ab, 28, biYPelsPerMeter);
        /*DWORD*/ IO.writeInt32LE(ab, 32, biClrUsed      );
        /*DWORD*/ IO.writeInt32LE(ab, 36, biClrImportant );
        return ab;
    }

    @Override
    public void write(@Nonnull RandomAccessFile raf) throws IOException {
        raf.write(toBytes());
    }

    @Override
    public int sizeof() {
        return 40;
    }

}
