/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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

import argparser.ArgParser;
import argparser.IntHolder;
import argparser.StringHolder;
import java.io.File;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.formats.JavaAudioFormat;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.TabularFeedback;

/** Manages possible options for creating a SectorAudioWriter, and
 *  produces independent instances using the current options. */
public class AudioSaverBuilder extends DiscItemSaverBuilder {

    private final DiscItemAudioStream _audItem;
    private final JavaAudioFormat[] _aoPossibleContainerFormats = JavaAudioFormat.getAudioFormats();

    public AudioSaverBuilder(DiscItemAudioStream audItem) {
        _audItem = audItem;
        resetToDefaults();
    }

    public DiscItemSaverBuilderGui getOptionPane() {
        return new AudioSaverBuilderGui(this);
    }

    public void resetToDefaults() {
        if (_aoPossibleContainerFormats != null && _aoPossibleContainerFormats.length > 0)
            setContainerForamt(_aoPossibleContainerFormats[0]);
        else
            setContainerForamt(null);
        setVolume(1);
    }

    @Override
    public boolean copySettingsTo(DiscItemSaverBuilder other) {
        if (other instanceof AudioSaverBuilder) {
            AudioSaverBuilder o = (AudioSaverBuilder) other;
            o.setContainerForamt(getContainerFormat());
            o.setVolume(getVolume());
            return true;
        }
        return false;
    }



    private JavaAudioFormat _containerFormat;
    public void setContainerForamt(JavaAudioFormat val) {
        _containerFormat = val;
        firePossibleChange();
    }
    public JavaAudioFormat getContainerFormat() {
        return _containerFormat;
    }

    public int getContainerFormat_listSize() {
        return JavaAudioFormat.getAudioFormats().length;
    }
    public JavaAudioFormat getContainerFormat_listItem(int i) {
        return JavaAudioFormat.getAudioFormats()[i];
    }

    public String getExtension() {
        return getContainerFormat().getExtension();
    }

    // ....................................................

    public String getFileBaseName() {
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

    public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs) {
        if (asArgs == null) return asArgs;

        ArgParser parser = new ArgParser("", false);

        IntHolder vol = new IntHolder(-1);
        parser.addOption("-vol %i {[0, 100]}", vol);

        StringHolder audfmt = new StringHolder();
        parser.addOption("-audfmt,-af %s {" + JavaAudioFormat.getCmdLineList() + "}", audfmt);

        String[] asRemain = parser.matchAllArgs(asArgs, 0, 0);

        if (vol.value >= 0 && vol.value <= 100) {
            fbs.println("Volume set to " + vol.value + "%");
            setVolume(vol.value / 100.0);
        }

        if (audfmt.value != null) {
            JavaAudioFormat fmt = JavaAudioFormat.fromCmdLine(audfmt.value);
            if (fmt != null) {
                fbs.println("Saving as " + fmt.getExtension());
                setContainerForamt(fmt);
            } else {
                fbs.printlnWarn("Ignoring invalid format " + audfmt.value);
            }
        }

        return asRemain;
    }

    public void printHelp(FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();

        tfb.setRowSpacing(1);

        tfb.print("-audfmt,-af <format>").tab().print("Output audio format (default wav). Options: ")
                .indent().print(JavaAudioFormat.getCmdLineList());
        tfb.newRow();

        tfb.print("-vol <0-100>").tab().print("Adjust volume (default 100).");

        tfb.write(fbs);
    }

    public IDiscItemSaver makeSaver() {
        return new AudioSaver(_audItem,
                new File(_audItem.getSuggestedBaseName().getPath() + "." + getExtension()),
                _containerFormat, _dblVolume);
    }

}
