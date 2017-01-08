/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import argparser.StringHolder;
import java.io.File;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.formats.JavaAudioFormat;
import jpsxdec.i18n.I;
import jpsxdec.util.ArgParser;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.TabularFeedback;
import jpsxdec.util.TabularFeedback.Cell;

/** Manages possible options for creating a {@link AudioSaver}, and
 *  produces independent instances using the current options. */
public class AudioSaverBuilder extends DiscItemSaverBuilder {

    @Nonnull
    private final DiscItemAudioStream _audItem;

    public AudioSaverBuilder(@Nonnull DiscItemAudioStream audItem) {
        _audItem = audItem;
        resetToDefaults();
    }

    public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
        return new AudioSaverBuilderGui(this);
    }

    public void resetToDefaults() {
        JavaAudioFormat[] fmts = JavaAudioFormat.getAudioFormats();
        if (fmts.length > 0)
            setContainerForamt(fmts[0]);
        else
            setContainerForamt(null);
        setVolume(1.0);
    }

    @Override
    public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder other) {
        if (other instanceof AudioSaverBuilder) {
            AudioSaverBuilder o = (AudioSaverBuilder) other;
            o.setContainerForamt(getContainerFormat());
            o.setVolume(getVolume());
            return true;
        }
        return false;
    }


    @Nonnull
    private JavaAudioFormat _containerFormat;
    public void setContainerForamt(@Nonnull JavaAudioFormat val) {
        _containerFormat = val;
        firePossibleChange();
    }
    public @Nonnull JavaAudioFormat getContainerFormat() {
        return _containerFormat;
    }

    public int getContainerFormat_listSize() {
        return JavaAudioFormat.getAudioFormats().length;
    }
    public @Nonnull JavaAudioFormat getContainerFormat_listItem(int i) {
        return JavaAudioFormat.getAudioFormats()[i];
    }

    public @Nonnull String getExtension() {
        return getContainerFormat().getExtension();
    }

    // ....................................................

    public @Nonnull String getFileBaseName() {
        return _audItem.getSuggestedBaseName().getPath();
    }

    // ....................................................

    private double _dblVolume = 1.0;
    public void setVolume(double val) {
        _dblVolume = val;
        firePossibleChange();
    }
    public double getVolume() {
        return _dblVolume;
    }

    // ....................................................

    public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs)
    {
        if (!ap.hasRemaining())
            return;

        StringHolder vol = ap.addStringOption("-vol");
        StringHolder audfmt = ap.addStringOption("-audfmt","-af");
        ap.match();

        if (vol.value != null) {
            try {
                int iVol = Integer.parseInt(vol.value);
                if (iVol < 0 || iVol > 100)
                    throw new NumberFormatException();
                setVolume(iVol / 100.0);
            } catch (NumberFormatException ex) {
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VOLUME(vol.value));
            }
        }

        if (audfmt.value != null) {
            JavaAudioFormat fmt = JavaAudioFormat.fromCmdLine(audfmt.value);
            if (fmt != null) {
                setContainerForamt(fmt);
            } else {
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_FORMAT(audfmt.value));
            }
        }
    }

    public void printHelp(@Nonnull FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();

        tfb.setRowSpacing(1);

        tfb.addCell(I.CMD_AUDIO_AF());
        Cell cell = new Cell(I.CMD_AUDIO_AF_HELP(JavaAudioFormat.getAudioFormats()[0].getExtension()));
        for (JavaAudioFormat audioFormat : JavaAudioFormat.getAudioFormats()) {
            cell.addLine(audioFormat.getCmdId(), 2);
        }
        tfb.addCell(cell);
        tfb.newRow();

        tfb.addCell(I.CMD_AUDIO_VOL()).addCell(I.CMD_AUDIO_VOL_HELP(100));

        tfb.write(fbs.getUnderlyingStream());
    }

    public @Nonnull AudioSaver makeSaver(@CheckForNull File directory) {
        return new AudioSaver(_audItem, directory,
                new File(_audItem.getSuggestedBaseName().getPath() + "." + getExtension()),
                _containerFormat, _dblVolume);
    }

}
