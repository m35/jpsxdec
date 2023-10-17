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

package jpsxdec.formats;

import java.util.ArrayList;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;

/** Keeps track of Java framework's audio formats. */
public class JavaAudioFormat {

    @Nonnull
    private final AudioFileFormat.Type _audioType;
    @Nonnull
    private final String _sExtension;
    @Nonnull
    private final UnlocalizedMessage _localizedCmdId;
    @Nonnull
    private final UnlocalizedMessage _localizedGuiDotExtension;
    private final boolean _blnIsAvailable;

    private final static JavaAudioFormat WAVE = new JavaAudioFormat(AudioFileFormat.Type.WAVE);
    private final static JavaAudioFormat AIFF = new JavaAudioFormat(AudioFileFormat.Type.AIFF);
    private final static JavaAudioFormat AU   = new JavaAudioFormat(AudioFileFormat.Type.AU);
    private final static JavaAudioFormat AIFC = new JavaAudioFormat(AudioFileFormat.Type.AIFC);
    private final static JavaAudioFormat SND  = new JavaAudioFormat(AudioFileFormat.Type.SND);

    private JavaAudioFormat(@Nonnull AudioFileFormat.Type audioType) {
        _audioType = audioType;
        _sExtension = audioType.getExtension();
        _localizedCmdId = new UnlocalizedMessage(_sExtension);
        _localizedGuiDotExtension = new UnlocalizedMessage("." + _sExtension);
        _blnIsAvailable = AudioSystem.isFileTypeSupported(audioType);
    }

    private boolean isAvailable() {
        return _blnIsAvailable;
    }

    public @Nonnull AudioFileFormat.Type getJavaType() {
        return _audioType;
    }

    public @Nonnull String getExtension() {
        return _sExtension;
    }

    public @Nonnull ILocalizedMessage getCmdId() {
        return _localizedCmdId;
    }

    @Override
    public String toString() {
        return _localizedGuiDotExtension.toString();
    }

    //-----------------------------------------------------------------
    // static

    public static @Nonnull JavaAudioFormat getDefaultAudioFormat() {
        return getAudioFormats()[0];
    }

    @CheckForNull
    private static JavaAudioFormat[] AUDIO_FORMATS;
    /** The length of returned array is guaranteed to not be empty.. */
    public static @Nonnull JavaAudioFormat[] getAudioFormats() {
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

        if (javaAudFmts.isEmpty())
            throw new ExceptionInInitializerError("JVM unable to save any audio formats");

        return AUDIO_FORMATS = javaAudFmts.toArray(new JavaAudioFormat[javaAudFmts.size()]);
    }

    public static @CheckForNull JavaAudioFormat fromCmdLine(@Nonnull String s) {
        for (JavaAudioFormat fmt : getAudioFormats()) {
            if (fmt.getCmdId().equalsIgnoreCase(s))
                return fmt;
        }
        return null;
    }

}
