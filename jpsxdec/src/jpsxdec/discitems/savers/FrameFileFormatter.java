/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2015  Michael Sabin
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


package jpsxdec.discitems.savers;

import java.io.File;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.FrameNumberFormat;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.FrameNumberFormatter;

/** Generates video filenames. */
public class FrameFileFormatter {
    // avi
    // sequence + header
    // sequence + index
    // sequence + sector

    /** Single file format, never includes the frame number. */
    public static @Nonnull File makeFile(@CheckForNull File directory, @Nonnull VideoFormat vf,
                                         @Nonnull DiscItemVideoStream vid)
    {
        File baseName = vid.getSuggestedBaseName();
        StringBuilder sb = new StringBuilder(baseName.getName());
        if (vf.needsDims())
            sb.append('_').append(vid.getWidth()).append('x').append(vid.getHeight());
        sb.append(vf.getExtension());
        String sBaseNameParent = baseName.getParent();
        if (sBaseNameParent == null)
            return new File(directory, sb.toString());
        else
            return new File(new File(directory, sBaseNameParent), sb.toString());
    }

    /** Formatter that only generates a single file, regardless of frame number. */
    public static @Nonnull FrameFileFormatter makeFormatter(@Nonnull String sBaseName,
                                                            @Nonnull VideoFormat vf,
                                                            int iWidth, int iHeight)
    {
        StringBuilder sb = new StringBuilder(sBaseName.replace("%", "%%"));
        if (vf.needsDims())
            sb.append('_').append(iWidth).append('x').append(iHeight);
        sb.append(vf.getExtension());
        File full = new File(sb.toString());
        return new FrameFileFormatter(full.getParentFile(), full.getName(), null);
    }
    
    // -------------------------------------------------------------------------


    public static @Nonnull FrameFileFormatter makeFormatter(@Nonnull VideoFormat vf,
                                                            @Nonnull DiscItemVideoStream videoItem,
                                                            @Nonnull FrameNumberFormat.Type type)
    {
        File fmtFile = makeFormat(videoItem, vf);
        File dir = fmtFile.getParentFile();
        if (dir == null)
            return new FrameFileFormatter(null, fmtFile.getName(), videoItem.getFrameNumberFormat().makeFormatter(type));
        else
            return new FrameFileFormatter(dir, fmtFile.getName(), videoItem.getFrameNumberFormat().makeFormatter(type));
    }
    public static @Nonnull FrameFileFormatter makeFormatter(@Nonnull File directory,
                                                            @Nonnull VideoFormat vf,
                                                            @Nonnull DiscItemVideoStream videoItem,
                                                            @Nonnull FrameNumberFormat.Type type)
    {
        File fmtFile = makeFormat(videoItem, vf);
        String sFmtDir = fmtFile.getParent();
        if (sFmtDir == null)
            return new FrameFileFormatter(directory, fmtFile.getName(), videoItem.getFrameNumberFormat().makeFormatter(type));
        else
            return new FrameFileFormatter(new File(directory, sFmtDir), fmtFile.getName(), videoItem.getFrameNumberFormat().makeFormatter(type));
    }
    
    private static @Nonnull File makeFormat(@Nonnull DiscItemVideoStream vid,
                                            @Nonnull VideoFormat vf)
    {
        File baseName = vid.getSuggestedBaseName();
        StringBuilder sb = new StringBuilder(baseName.getName().replace("%", "%%"));
        if (vf.needsDims())
            sb.append('_').append(vid.getWidth()).append('x').append(vid.getHeight());
        sb.append("[%s]");
        sb.append(vf.getExtension());
        return new File(baseName.getParent(), sb.toString());
    }

    //--------------------------------------------------------------------------

    @CheckForNull
    private final File _directory;
    @Nonnull
    private final String _sFileNameFormat;
    @CheckForNull
    private final FrameNumberFormatter _numberFormatter;

    private FrameFileFormatter(@CheckForNull File directory, @Nonnull String sFileNameFormat, 
                               @CheckForNull FrameNumberFormatter numberFormatter)
    {
        _directory = directory;
        _sFileNameFormat = sFileNameFormat;
        _numberFormatter = numberFormatter;
    }

    /** Generates a file name for the given frame number.
     * Note that if this instance was created using
     * {@link #makeFormatter(java.lang.String, jpsxdec.discitems.savers.VideoFormat, int, int) }
     * then the frameNumber argument is ignored.
     * @throws NullPointerException if frameNumber is null and this formatter needs it.
     */
    public @Nonnull File format(@CheckForNull FrameNumber frameNumber,
                                @CheckForNull Logger log)
    {
        if (_numberFormatter == null)
            return new File(_directory, _sFileNameFormat);
        else if (frameNumber == null)
            throw new NullPointerException("Frame number required for this formatter");
        else
            return new File(_directory,
                            String.format(_sFileNameFormat,
                                          _numberFormatter.formatNumber(frameNumber, log)));
    }
}
