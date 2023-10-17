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

package jpsxdec.psxvideo.mdec;

import com.mortennobel.imagescaling.ResampleFilter;
import com.mortennobel.imagescaling.ResampleFilters;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;

public enum ChromaUpsample {
    /** i.e. Box */
    NearestNeighbor(I.CHROMA_UPSAMPLE_NEAR_NEIGHBOR_DESCRIPTION(),
                    I.CHROMA_UPSAMPLE_NEAR_NEIGHBOR_CMDLINE(), null),

    /** i.e. Triangle */
    Bilinear(I.CHROMA_UPSAMPLE_BILINEAR_DESCRIPTION(),
             I.CHROMA_UPSAMPLE_BILINEAR_CMDLINE(), null),

    Bicubic(I.CHROMA_UPSAMPLE_BICUBIC_DESCRIPTION(),
            I.CHROMA_UPSAMPLE_BICUBIC_CMDLINE(), ResampleFilters.getBiCubicFilter()),

    Bell(I.CHROMA_UPSAMPLE_BELL_DESCRIPTION(),
         I.CHROMA_UPSAMPLE_BELL_CMDLINE(), ResampleFilters.getBellFilter()),

    Mitchell(I.CHROMA_UPSAMPLE_MITCHELL_DESCRIPTION(),
             I.CHROMA_UPSAMPLE_MITCHELL_CMDLINE(), ResampleFilters.getMitchellFilter()),

    BSpline(I.CHROMA_UPSAMPLE_BSPLINE_DESCRIPTION(),
            I.CHROMA_UPSAMPLE_BSPLINE_CMDLINE(), ResampleFilters.getBSplineFilter()),

    Lanczos3(I.CHROMA_UPSAMPLE_LANCZOS3_DESCRIPTION(),
             I.CHROMA_UPSAMPLE_LANCZOS3_CMDLINE(), ResampleFilters.getLanczos3Filter()),

    Hermite(I.CHROMA_UPSAMPLE_HERMITE_DESCRIPTION(),
            I.CHROMA_UPSAMPLE_HERMITE_CMDLINE(), ResampleFilters.getHermiteFilter());

    @Nonnull
    private final ILocalizedMessage _description;
    private final ILocalizedMessage _cmdLine;
    @CheckForNull
    final ResampleFilter _filter;

    private ChromaUpsample(@Nonnull ILocalizedMessage description,
                      @Nonnull ILocalizedMessage cmdLine,
                      @CheckForNull ResampleFilter filter)
    {
        _description = description;
        _cmdLine = cmdLine;
        _filter = filter;
    }

    public static ChromaUpsample fromCmdLine(String sCmdLine) {
        ChromaUpsample up = null;
        for (ChromaUpsample upsampler : values()) {
            if (upsampler.getCmdLine().equalsIgnoreCase(sCmdLine)) {
                up = upsampler;
                break;
            }
        }
        return up;
    }

    public ILocalizedMessage getDescription() {
        return _description;
    }

    public ILocalizedMessage getCmdLine() {
        return _cmdLine;
    }

    public ILocalizedMessage getCmdLineHelp() {
        if (_cmdLine.equals(_description))
            return _cmdLine;
        else
            return I.CHROMA_UPSAMPLE_CMDLINE_HELP(_cmdLine, _description);
    }

    /** {@inheritDoc}
     *<p>
     * Used in GUI list so must be localized. */
    @Override
    public String toString() {
        return getDescription().getLocalizedMessage();
    }
}
