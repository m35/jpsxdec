/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014  Michael Sabin
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
import jpsxdec.discitems.DiscItemVideoStream;

/** Generates video filenames. */
public class FrameFormatter {
    // avi
    // sequence + header
    // sequence + index
    // sequence + sector

    public static File makeFile(VideoFormat vf, DiscItemVideoStream videoItem) {
        return makeFormat(videoItem, vf);
    }
    public static File makeFile(File directory, VideoFormat vf, DiscItemVideoStream vid) {
        File baseName = vid.getSuggestedBaseName();
        String sName = baseName.getName() +
                vf.makePostfix(vid.getWidth(), vid.getHeight());
        String sBaseNameParent = baseName.getParent();
        if (sBaseNameParent == null)
            return new File(directory, sName);
        else
            return new File(new File(directory, sBaseNameParent), sName);
    }


    private static File makeFormat(DiscItemVideoStream vid, VideoFormat vf) {
        File baseName = vid.getSuggestedBaseName();
        String sName = baseName.getName().replace("%", "%%") +
                       vf.makePostfix(vid.getWidth(), vid.getHeight()) +
                       '[' + vid.getFrameNumberFormat() + ']';
        return new File(baseName.getParent(), sName);
    }

    public static FrameFormatter makeFormatter(VideoFormat vf, DiscItemVideoStream videoItem) {
        File fmtFile = makeFormat(videoItem, vf);
        File fmtParent = fmtFile.getParentFile();
        if (fmtParent == null)
            return new FrameFormatter(null, fmtFile.getName(), true);
        else
            return new FrameFormatter(fmtParent, fmtFile.getName(), true);
    }
    public static FrameFormatter makeFormatter(File directory, VideoFormat vf, DiscItemVideoStream videoItem) {
        File fmtFile = makeFormat(videoItem, vf);
        String sFmtParent = fmtFile.getParent();
        if (sFmtParent == null)
            return new FrameFormatter(directory, fmtFile.getName(), true);
        else
            return new FrameFormatter(new File(directory, sFmtParent), fmtFile.getName(), true);
    }

    public static FrameFormatter makeFormatter(String sBaseName, VideoFormat vf, int iWidth, int iHeight) {
        File full = new File(sBaseName + vf.makePostfix(iWidth, iHeight));
        return new FrameFormatter(full.getParentFile(), full.getName(), false);
    }

    //--------------------------------------------------------------------------

    private final File _directory;
    private final String _sFormat;
    private final boolean _blnHasFrameFormat;

    public FrameFormatter(File directory, String sFormat, boolean blnHasFrameFormat) {
        _directory = directory;
        _sFormat = sFormat;
        _blnHasFrameFormat = blnHasFrameFormat;
    }

    public File format(int iHeaderFrameNumber) {
        if (_blnHasFrameFormat)
            return new File(_directory, String.format(_sFormat, iHeaderFrameNumber));
        else
            return new File(_directory, _sFormat);
    }
}
