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

package jpsxdec.modules.xa;

import argparser.ArgParser;
import argparser.IntHolder;
import argparser.StringHolder;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.swing.JPanel;
import jpsxdec.formats.JavaAudioFormat;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.AudioOutputFileWriter;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.TabularFeedback;

/** Manages possible options for creating a SectorAudioWriter, and
 *  produces independent instances using the current options. */
public class SectorAudioWriterBuilder {

    private final DiscItemAudioStream _audItem;

    public SectorAudioWriterBuilder(DiscItemAudioStream audItem)
    {
        _audItem = audItem;
        setPossibleFormats(JavaAudioFormat.getAudioFormats());
        setFilename(_audItem.getSuggestedBaseName());
    }

    private JavaAudioFormat[] _aoPossibleContainerFormats;
    protected void setPossibleFormats(JavaAudioFormat[] aoPossibleFormats) {
        _aoPossibleContainerFormats = aoPossibleFormats;
        if (_aoPossibleContainerFormats != null && _aoPossibleContainerFormats.length > 0)
            setContainerForamt(_aoPossibleContainerFormats[0]);
        else
            setContainerForamt(null);
    }

    public static final String PROP_FORAMT = "containerFormat";
    private JavaAudioFormat _containerFormat;
    public void setContainerForamt(JavaAudioFormat val) {
        firePropChange(PROP_FORAMT, _containerFormat,
                _containerFormat = val);
    }
    public JavaAudioFormat getContainerFormat() {
        return _containerFormat;
    }

    public static final String PROP_FILENAME = "filename";
    private String _sFilename;
    public void setFilename(String val) {
        firePropChange(PROP_FORAMT, _sFilename,
                _sFilename = val);
    }
    public String getFilename() {
        return _sFilename;
    }

    public static final String PROP_VOLUME = "volume";
    private double _dblVolume = 1.0;
    public void setVolume(double val) {
        firePropChange(PROP_VOLUME, _dblVolume,
                _dblVolume = val);
    }
    public double getVolume() {
        return _dblVolume;
    }

    public JPanel getGui() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs) {
        if (asArgs == null) return asArgs;

        ArgParser parser = new ArgParser("", false);

        IntHolder vol = new IntHolder(-1);
        parser.addOption("-vol %i {[0, 100]}", vol);

        StringHolder audfmt = new StringHolder();
        parser.addOption("-audfmt,-af %s {" + JavaAudioFormat.getCmdLineList() + "}", audfmt);

        String[] asRemain = parser.matchAllArgs(asArgs, 0, 0);

        if (vol.value >= 0 && vol.value <= 100) {
            fbs.printlnNorm("Volume set to " + vol.value + "%");
            setVolume(vol.value / 100.0);
        }

        if (audfmt.value != null) {
            JavaAudioFormat fmt = JavaAudioFormat.fromCmdLine(audfmt.value);
            if (fmt != null) {
                fbs.printlnNorm("Saving as " + fmt.getExtension());
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

    private class JavaFormatWriter implements SectorAudioWriter, IAudioReceiver {

        private final AudioFormat __audioFmt;
        private final String __sOutputFile = _sFilename + "." + _containerFormat.getExtension();
        private final AudioOutputFileWriter __audioWriter;
        private IAudioSectorDecoder __decoder;

        public JavaFormatWriter(double dblVolume) throws IOException {

            __audioFmt = new AudioFormat(
                    _audItem.getSampleRate(),
                    16, _audItem.isStereo() ? 2 : 1,
                    true, true);
            __audioWriter = new AudioOutputFileWriter(
                    new File(__sOutputFile),
                    __audioFmt, _containerFormat.getJavaType());
            __decoder = _audItem.makeDecoder(__audioFmt.isBigEndian(), dblVolume);
            __decoder.open(this);
        }

        @Override
        public void write(AudioFormat inFormat, byte[] abData, int iStart, int iLen, int iPresentationSector) throws IOException {
            __audioWriter.write(inFormat, abData, iStart, iLen);
        }

        public void close() throws IOException {
            __audioWriter.close();
        }

        public String getOutputFile() {
            return __sOutputFile;
        }

        @Override
        public void feedSector(IdentifiedSector sector) throws IOException {
            __decoder.feedSector(sector);
        }

    };

    public SectorAudioWriter getAudioWriter() throws IOException {
        return new JavaFormatWriter(getVolume());
    }


    private final PropertyChangeSupport _propChange = new PropertyChangeSupport(this);
    public void addListener(String sProperty, PropertyChangeListener listener) {
        _propChange.addPropertyChangeListener(sProperty, listener);
    }
    public void removeListener(String sProperty, PropertyChangeListener listener) {
        _propChange.removePropertyChangeListener(sProperty, listener);
    }

    public void firePropChange(String propertyName, Object oldValue, Object newValue)
    {
        if (oldValue != null && newValue != null && oldValue.equals(newValue))
            return;
        _propChange.firePropertyChange(
                new PropertyChangeEvent(this, propertyName, oldValue, newValue));
    }

}
