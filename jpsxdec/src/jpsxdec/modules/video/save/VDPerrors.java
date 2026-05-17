/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2026  Michael Sabin
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

package jpsxdec.modules.video.save;

import java.io.File;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;

/** Different messages depending on whether the frame number is null or not. */
public class VDPerrors {
    public static @Nonnull ILocalizedMessage JPEG_ENCODER_FRAME_FAIL(
            @CheckForNull FormattedFrameNumber frameNumber)
    {
        if (frameNumber == null)
            return I.JPEG_ENCODER_FRAME_FAIL_NO_FRAME();
        else
            return I.JPEG_ENCODER_FRAME_FAIL(frameNumber.toString());
    }

    public static @Nonnull ILocalizedMessage FRAME_NUM_INCOMPLETE(
            @CheckForNull FormattedFrameNumber frameNumber)
    {
        if (frameNumber == null)
            return I.FRAME_INCOMPLETE();
        else
            return I.FRAME_NUM_INCOMPLETE(frameNumber.getUnpaddedValue());
    }

    public static @Nonnull ILocalizedMessage FRAME_NUM_CORRUPTED(
            @CheckForNull FormattedFrameNumber frameNumber)
    {
        if (frameNumber == null)
            return I.FRAME_CORRUPTED();
        else
            return I.FRAME_NUM_CORRUPTED(frameNumber.getUnpaddedValue());
    }

    public static @Nonnull ILocalizedMessage FRAME_NUM_AHEAD_OF_READING(
            @CheckForNull FormattedFrameNumber frameNumber, int iFrameCount)
    {
        if (frameNumber == null)
            return I.FRAME_AHEAD_OF_READING(iFrameCount);
        else
            return I.FRAME_NUM_AHEAD_OF_READING(frameNumber.getUnpaddedValue(), iFrameCount);
    }

    public static @Nonnull ILocalizedMessage FRAME_WRITE_ERR(
            @Nonnull File f, @CheckForNull FormattedFrameNumber frameNumber)
    {
        if (frameNumber == null)
            return I.IO_WRITING_FILE_ERROR_NAME(f.toString());
        else
            return I.FRAME_WRITE_ERR(f, frameNumber.getUnpaddedValue());
    }

    public static @Nonnull ILocalizedMessage FRAME_FILE_WRITE_UNABLE(
            @Nonnull File f, @CheckForNull FormattedFrameNumber frameNumber)
    {
        if (frameNumber == null)
            return I.IO_WRITING_FILE_ERROR_NAME(f.toString());
        else
            return I.FRAME_FILE_WRITE_UNABLE(f.toString(), frameNumber.getUnpaddedValue());
    }

    public static @Nonnull ILocalizedMessage UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(
            @CheckForNull FormattedFrameNumber frameNumber)
    {
        if (frameNumber == null)
            return I.UNABLE_TO_DETERMINE_FRAME_TYPE();
        else
            return I.UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(frameNumber.getUnpaddedValue());
    }

}
