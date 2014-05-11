/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2014  Michael Sabin
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
import argparser.BooleanHolder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jpsxdec.discitems.AudioStreamsCombiner;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.DiscItemStrVideoStream;
import jpsxdec.discitems.ISectorAudioDecoder;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.TabularFeedback;


public class VideoSaverBuilderStr extends VideoSaverBuilder {

    /** Hacky workaround to prevent constructor superclass resetting defaults. */
    private boolean _blnAudioInit = false;
    private final DiscItemStrVideoStream _sourceVidItem;

    public VideoSaverBuilderStr(DiscItemStrVideoStream vidItem) {
        super(vidItem);
        _sourceVidItem = vidItem;
        if (vidItem.hasAudio()) {
            _parallelAudio = vidItem.getParallelAudioStreams();
            _ablnParallelAudio = new boolean[vidItem.getChildCount()];
        } else {
            _parallelAudio = new ArrayList<DiscItemAudioStream>(0);
            _ablnParallelAudio = new boolean[0];
        }
        _blnAudioInit = true;
        resetToDefaults();
    }

    @Override
    public void resetToDefaults() {
        if (!_blnAudioInit) // wait until this class is constructed to reset
            return;
        super.resetToDefaults();

        if (_sourceVidItem.hasAudio()) {
            List<DiscItemAudioStream> defaultAud = _sourceVidItem.getLongestNonIntersectingAudioStreams();
            for (int i = 0; i < _ablnParallelAudio.length; i++) {
                _ablnParallelAudio[i] = defaultAud.contains(_parallelAudio.get(i));
            }
        }
        setEmulatePsxAVSync(false);
    }

    @Override
    public boolean copySettingsTo(DiscItemSaverBuilder otherBuilder) {
        if (super.copySettingsTo(otherBuilder)) {
            if (otherBuilder instanceof VideoSaverBuilderStr) {
                VideoSaverBuilderStr other = (VideoSaverBuilderStr) otherBuilder;
                //other.setParallelAudio(getParallelAudio());
                if (getEmulatePsxAVSync_enabled())
                    other.setEmulatePsxAVSync(getEmulatePsxAvSync());
                if (hasAudio() && !getSavingAudio())
                    other.setParallelAudioNone();
            }
            return true;
        } else {
            return false;
        }
    }

    public DiscItemSaverBuilderGui getOptionPane() {
        return new VideoSaverBuilderStrGui(this);
    }

    // .........................................................................


    public boolean getAudioVolume_enabled() {
        return getSavingAudio();
    }

    // .........................................................................

    private final List<DiscItemAudioStream> _parallelAudio;
    private final boolean[] _ablnParallelAudio;

    public int getParallelAudioCount() {
        return _sourceVidItem.getChildCount();
    }
    public DiscItemAudioStream getParallelAudio(int i) {
        return _parallelAudio.get(i);
    }

    public boolean getParallelAudio_selected(int i) {
        if (getParallelAudio_enabled())
            return _ablnParallelAudio[i];
        else
            return false;
    }

    public void setParallelAudio(DiscItemAudioStream parallelAudio, boolean blnSelected) {
        if (!_sourceVidItem.hasAudio())
            return;

        setParallelAudio(_parallelAudio.indexOf(parallelAudio), blnSelected);
    }

    public void setParallelAudio(int iIndex, boolean blnSelected) {
        if (!_sourceVidItem.hasAudio())
            return;

        if (iIndex < 0 || iIndex >= _sourceVidItem.getChildCount())
            return;

        DiscItemAudioStream aud = _parallelAudio.get(iIndex);
        for (int i = 0; i < _ablnParallelAudio.length; i++) {
            if (_ablnParallelAudio[i]) {
                DiscItemAudioStream other = _parallelAudio.get(i);
                // if it overlaps or has a different format
                if (aud.overlaps(other) || !aud.hasSameFormat(other)) {
                    // disable it
                    _ablnParallelAudio[i] = false;
                }
            }
        }

        _ablnParallelAudio[iIndex] = blnSelected;
        firePossibleChange();
    }

    public void setParallelAudioNone() {
        for (int i = 0; i < getParallelAudioCount(); i++) {
            setParallelAudio(i, false);
        }
    }

    public boolean getParallelAudio_enabled() {
        return getVideoFormat().isAvi();
    }

    public boolean hasAudio() {
        return _sourceVidItem.hasAudio();
    }

    public boolean getSavingAudio() {
        if (!getVideoFormat().isAvi())
            return false;
        
        for (boolean b : _ablnParallelAudio) {
            if (b)
                return true;
        }
        return false;
    }

    // .........................................................................

    private boolean _blnEmulatePsxAVSync = false;
    public boolean getEmulatePsxAvSync() {
        if (getEmulatePsxAVSync_enabled())
            return _blnEmulatePsxAVSync;
        return false;
    }
    public void setEmulatePsxAVSync(boolean val) {
        _blnEmulatePsxAVSync = val;
        firePossibleChange();
    }

    public boolean getEmulatePsxAVSync_enabled() {
        return getSavingAudio();
    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs) {
        asArgs = super.commandLineOptions(asArgs, fbs);
        if (asArgs == null) return null;
        
        ArgParser parser = new ArgParser("", false);

        //...........

        BooleanHolder noaud = new BooleanHolder(false);
        parser.addOption("-noaud %v", noaud); // Only with AVI & audio

        BooleanHolder emulateav = new BooleanHolder(false);
        parser.addOption("-psxav %v", emulateav); // Only with AVI & audio

        // -------------------------
        String[] asRemain = parser.matchAllArgs(asArgs, 0, 0);
        // -------------------------

        setEmulatePsxAVSync(emulateav.value);

        if (noaud.value)
            setParallelAudioNone();

        return asRemain;
    }

    @Override
    protected void makeHelpTable(TabularFeedback tfb) {
        super.makeHelpTable(tfb);

        if (_sourceVidItem.hasAudio()) {
            tfb.newRow();
            tfb.print("-noaud").tab().print("Don't save audio."); // I18N
        }

        if (_sourceVidItem.hasAudio()) {
            tfb.newRow();
            tfb.print("-psxav").tab().print("Emulate PSX audio/video timing."); // I18N
        }
    }

    @Override
    protected SectorFeeder makeFeeder() {
        ISectorAudioDecoder audDecoder;
        if (getSavingAudio()) {
            ArrayList<DiscItemAudioStream> parallelAudio = new ArrayList<DiscItemAudioStream>();
            for (int i = 0; i < _ablnParallelAudio.length; i++) {
                if (_ablnParallelAudio[i])
                    parallelAudio.add(_parallelAudio.get(i));
            }
            if (parallelAudio.size() == 1)
                audDecoder = parallelAudio.get(0).makeDecoder(getAudioVolume());
            else
                audDecoder = new AudioStreamsCombiner(parallelAudio, getAudioVolume());
        } else {
            audDecoder = null;
        }

        return new StrSectorFeeder(_sourceVidItem.makeDemuxer(), audDecoder);
    }

    private static class StrSectorFeeder extends SectorFeeder {

        public StrSectorFeeder(ISectorFrameDemuxer vidDemuxer, ISectorAudioDecoder audDecoder) {
            videoDemuxer = vidDemuxer;
            audioDecoder = audDecoder;
        }

        @Override
        public void feedSector(IdentifiedSector sector, Logger log) throws IOException {
            videoDemuxer.feedSector(sector, log);
            if (audioDecoder != null)
                audioDecoder.feedSector(sector, log);
        }
    }

}
