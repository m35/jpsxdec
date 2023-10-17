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

package jpsxdec.formats;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

/** Keeps track of Java framework's image formats. */
public enum JavaImageFormat {

    PNG("png",   "png", "png", true, true),
    BMP("bmp",   "bmp", "bmp", true, false), // Java doesn't support bmp with alpha channel
    GIF("gif",   "gif", "gif", false, false),
    TIFF("tiff", "tif", "tif", true, false);

    @Nonnull
    private final String _sUiId;
    @Nonnull
    private final String _sImageIOid;
    @Nonnull
    private final String _sExtension;
    private final boolean _blnTrueColor;
    private final boolean _blnAlpha;
    private final boolean _blnAvailable;

    JavaImageFormat(@Nonnull String sUiId, @Nonnull String sImageIOid, @Nonnull String sExtension, boolean blnTrueColor, boolean blnAlpha) {
        _sUiId = sUiId;
        _sImageIOid = sImageIOid;
        _sExtension = sExtension;

        _blnTrueColor = blnTrueColor;
        _blnAlpha = blnAlpha;

        String[] asValues = ImageIO.getWriterFormatNames();

        boolean blnAvailable = false;
        for (String s : asValues) {
            if (s.equalsIgnoreCase(_sImageIOid)) {
                blnAvailable = true;
                break;
            }
        }
        _blnAvailable = blnAvailable;
    }

    public @Nonnull String getUiId() {
        return _sUiId;
    }

    /** Unique id identifying this image format.
     * Also the id used for ImageIO operations. */
    public @Nonnull String getImageIOid() {
        return _sImageIOid;
    }

    public @Nonnull String getExtension() {
        return _sExtension;
    }

    public boolean hasTrueColor() {
        return _blnTrueColor;
    }

    public boolean isAvailable() {
        return _blnAvailable;
    }

    public boolean hasAlpha() {
        return _blnAlpha;
    }

    @Override
    public String toString() {
        return _sUiId;
    }

    public static @Nonnull List<JavaImageFormat> getAvailable() {
        JavaImageFormat[] aeFormats = JavaImageFormat.values();
        ArrayList<JavaImageFormat> available = new ArrayList<JavaImageFormat>(aeFormats.length);
        for (JavaImageFormat fmt : aeFormats) {
            if (fmt.isAvailable())
                available.add(fmt);
        }
        return available;
    }

}
