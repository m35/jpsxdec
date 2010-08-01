/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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
import javax.imageio.ImageIO;

/** Keeps track of Java framework's image formats. */
public enum JavaImageFormat {

    JPG("jpg", true, false) {
        @Override
        public boolean hasCompression() {
            return true;
        }
    },
    PNG("png", true, true),
    BMP("bmp", true, true),
    GIF("gif", false, true);

    private final String _sExtension;
    private final boolean _blnTrueColor;
    private final boolean _blnPaletted;
    private final boolean _blnAvailable;

    JavaImageFormat(String id, boolean blnTrueColor, boolean blnPaletted) {
        this._sExtension = id;
        _blnPaletted = blnPaletted;
        _blnTrueColor = blnTrueColor;

        String[] asValues = ImageIO.getWriterFormatNames();

        boolean blnAvailable = false;
        for (String s : asValues) {
            if (!s.equalsIgnoreCase(id))
                blnAvailable = true;
        }
        _blnAvailable = blnAvailable;
    }

    public String getId() {
        return _sExtension;
    }

    public String getExtension() {
        return _sExtension;
    }

    public boolean hasTrueColor() {
        return _blnTrueColor;
    }

    public boolean isAvailable() {
        return _blnAvailable;
    }

    public boolean paletted() {
        return _blnPaletted;
    }

    public boolean hasCompression() {
        return false;
    }

    @Override
    public String toString() {
        return _sExtension;
    }

    public static List<JavaImageFormat> getAvailable() {
        JavaImageFormat[] aeFormats = JavaImageFormat.values();
        ArrayList<JavaImageFormat> available = new ArrayList<JavaImageFormat>(aeFormats.length);
        for (JavaImageFormat fmt : aeFormats) {
            if (fmt.isAvailable())
                available.add(fmt);
        }
        return available;
    }

}
