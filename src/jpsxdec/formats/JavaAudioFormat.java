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
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

/** Keeps track of Java framework's audio formats. */
public class JavaAudioFormat {

    private final AudioFileFormat.Type _audioType;
    private final String _sId;
    private final String _sDescription;
    private final String _sExtension;
    private final boolean _blnIsAvailable;

    public final static JavaAudioFormat WAVE = new JavaAudioFormat(AudioFileFormat.Type.WAVE);
    private final static JavaAudioFormat AIFF = new JavaAudioFormat(AudioFileFormat.Type.AIFF);
    private final static JavaAudioFormat AU   = new JavaAudioFormat(AudioFileFormat.Type.AU);
    private final static JavaAudioFormat AIFC = new JavaAudioFormat(AudioFileFormat.Type.AIFC);
    private final static JavaAudioFormat SND  = new JavaAudioFormat(AudioFileFormat.Type.SND);

    private JavaAudioFormat(AudioFileFormat.Type oAudioType) {
        _audioType = oAudioType;
        _sId = oAudioType.toString();
        _sDescription = oAudioType.toString().toLowerCase();
        _sExtension = oAudioType.getExtension();
        _blnIsAvailable = AudioSystem.isFileTypeSupported(oAudioType);
    }

    private boolean isAvailable() {
        return _blnIsAvailable;
    }

    @Override
    public String toString() {
        return "." + _sExtension;
    }

    public AudioFileFormat.Type getJavaType() {
        return _audioType;
    }
    
    //-------------------------------

    public static JavaAudioFormat getDefaultAudioFormat() {
        return AUDIO_FORMATS[0];
    }

    private static JavaAudioFormat[] AUDIO_FORMATS;
    public static JavaAudioFormat[] getAudioFormats() {
        if (AUDIO_FORMATS != null) return AUDIO_FORMATS;
        
        ArrayList<JavaAudioFormat> javaAudFmts = new ArrayList<JavaAudioFormat>();
        
        if (WAVE.isAvailable())
            javaAudFmts.add(WAVE);
        if (AIFF.isAvailable())
            javaAudFmts.add(AIFF);
        if (AU.isAvailable())
            javaAudFmts.add(AU);
        if (AIFC.isAvailable())
            javaAudFmts.add(AIFC);
        if (SND.isAvailable())
            javaAudFmts.add(SND);

        return AUDIO_FORMATS = javaAudFmts.toArray(new JavaAudioFormat[javaAudFmts.size()]);
    }

    public static String getCmdLineList() {
        StringBuilder sb = new StringBuilder();
        for (JavaAudioFormat fmt : getAudioFormats()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(fmt.getExtension());
        }
        return sb.toString();
    }

    public static JavaAudioFormat fromCmdLine(String s) {
        for (JavaAudioFormat fmt : getAudioFormats()) {
            if (fmt.getExtension().equalsIgnoreCase(s))
                return fmt;
        }
        return null;
    }

    public String getExtension() {
        return _sExtension;
    }

}
