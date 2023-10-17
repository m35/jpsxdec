/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;

/** Generates a file name for a given frame number.
 * Combines the path, base name, frame number, and extension of a file
 * into a string of a file name that can be used to save a video frame.
 *
 * mdec and bs formats will also include the frame dimensions in the filename.
 */
public class VideoFileNameFormatter {

    private static final Logger LOG = Logger.getLogger(VideoFileNameFormatter.class.getName());

    /** When you just want a file without a number in the name (e.g. avi). */
    public static @Nonnull File singleFile(@CheckForNull File directory,
                                           @Nonnull DiscItemVideoStream vid,
                                           @Nonnull VideoFormat vf)
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

    @CheckForNull
    private final File _directory;
    @Nonnull
    private final String _sBaseName;
    @CheckForNull
    private final String _sDims;
    @Nonnull
    private final String _sExtension;

    private final boolean _blnIgnoreFrameNumbers;

    /** Makes a formatter that ignores frame numbers. */
    public VideoFileNameFormatter(@CheckForNull File directory,
                                  @Nonnull String sBaseName,
                                  @Nonnull VideoFormat vf,
                                  int iWidth, int iHeight)
    {
        File basePath = new File(sBaseName);
        String sParent = basePath.getParent();
        if (sParent != null)
            _directory = new File(directory, sParent);
        else
            _directory = directory;

        _sBaseName = basePath.getName();

        if (vf.needsDims())
            _sDims = "_" + iWidth + "x" + iHeight;
        else
            _sDims = null;
        _sExtension = vf.getExtension();

        _blnIgnoreFrameNumbers = true;
    }

    /** Make a formatter based on the directory, video item details, and
     * output format.
     * @param blnIgnoreFrameNumbers Whether to exclude frame numbers in the file
     *                              names generated. In essence, it will just
     *                              keep generating the same file name
     *                              over and over for each frame.
     */
    public VideoFileNameFormatter(@CheckForNull File directory,
                                  @Nonnull DiscItemVideoStream videoItem,
                                  @Nonnull VideoFormat vf,
                                  boolean blnIgnoreFrameNumbers)
    {
        File basePath = videoItem.getSuggestedBaseName();
        String sParent = basePath.getParent();
        if (sParent != null)
            _directory = new File(directory, sParent);
        else
            _directory = directory;

        _sBaseName = basePath.getName();

        if (vf.needsDims())
            _sDims = "_" + videoItem.getWidth() + "x" + videoItem.getHeight();
        else
            _sDims = null;
        _sExtension = vf.getExtension();

        _blnIgnoreFrameNumbers = blnIgnoreFrameNumbers;
    }

    public @Nonnull File format(@CheckForNull FormattedFrameNumber frameNumber, @CheckForNull ILocalizedLogger log) {
        if (!_blnIgnoreFrameNumbers && frameNumber == null && log != null) {
            throw new IllegalArgumentException("Programmer error: formatter expected a frame");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(_sBaseName);

        if (_sDims != null)
            sb.append(_sDims);

        if (!_blnIgnoreFrameNumbers && frameNumber != null)
            sb.append('[').append(frameNumber).append(']');

        sb.append(_sExtension);

        if (log != null && frameNumber != null) {
            // warn if digits are out of bounds, therefore file name will be inconsistent
            if (frameNumber.digitsAreOutOfBounds()) {
                log.log(Level.WARNING, I.FRAMES_UNEXPECTED_NUMBER());
                LOG.log(Level.WARNING,
                        "Frame number {0} is greater than expected {1}, resulting frame file name {2} will have an inconsistent format",
                        new Object[]{frameNumber.getUnpaddedValue(), frameNumber.getMaxFormat(), sb});
            }

        }

        return new File(_directory, sb.toString());
    }

}
