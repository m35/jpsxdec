/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

package jpsxdec.discitems;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import jpsxdec.sectors.IdentifiedSector;


public class AudioStreamsCombiner implements ISectorAudioDecoder {

    private final AudioFormat _outFormat;
    private int _iStartSector;
    private int _iEndSector;
    private int _iPresStartSector;

    private final ISectorAudioDecoder[] _aoDecoders;

    public AudioStreamsCombiner(List<DiscItemAudioStream> audStreams, double dblVolume)
    {
        if (thereIsOverlap(audStreams))
            throw new IllegalArgumentException("Streams are not mutually exclusive.");

        _aoDecoders = new ISectorAudioDecoder[audStreams.size()];

        final boolean blnIsStereo;
        final int iSampleRate;

        blnIsStereo = audStreams.get(0).isStereo();
        iSampleRate = audStreams.get(0).getSampleRate();
        _iStartSector = audStreams.get(0).getStartSector();
        _iEndSector = audStreams.get(0).getEndSector();
        _iPresStartSector = audStreams.get(0).getPresentationStartSector();
        _aoDecoders[0] = audStreams.get(0).makeDecoder(dblVolume);
        for (int i = 1; i < _aoDecoders.length; i++) {
            DiscItemAudioStream aud = audStreams.get(i);

            if (!aud.hasSameFormat(audStreams.get(0)))
                throw new IllegalArgumentException("Different format audio.");

            _iStartSector = Math.min(_iStartSector, aud.getStartSector());
            _iEndSector = Math.max(_iEndSector, aud.getEndSector());
            _iPresStartSector = Math.min(_iPresStartSector, aud.getPresentationStartSector());

            _aoDecoders[i] = aud.makeDecoder(dblVolume);
        }

         _outFormat = new AudioFormat(iSampleRate, 16, blnIsStereo ? 2 : 1,
                                      true, false);
    }

    private static boolean thereIsOverlap(List<DiscItemAudioStream> audStreams) {
        for (int i = 0; i < audStreams.size(); i++) {
            for (int j = i+1; j < audStreams.size(); j++) {
                if (audStreams.get(i).overlaps(audStreams.get(j)))
                    return true;
            }
        }
        return false;
    }

    public void setAudioListener(ISectorTimedAudioWriter audioOut) {
         for (ISectorAudioDecoder decoder : _aoDecoders) {
            decoder.setAudioListener(audioOut);
         }
    }

    public double getVolume() {
        // assume the volume is the same for all decoders
        return _aoDecoders[0].getVolume();
    }

    public void setVolume(double dblVolume) {
        for (ISectorAudioDecoder decoder : _aoDecoders) {
            decoder.setVolume(dblVolume);
        }
    }

    public AudioFormat getOutputFormat() {
        return _outFormat;
    }

    public void reset() {
        for (ISectorAudioDecoder decoder : _aoDecoders) {
            decoder.reset();
        }
    }

    public int getStartSector() {
        return _iStartSector;
    }

    public int getEndSector() {
        return _iEndSector;
    }

    public int getPresentationStartSector() {
        return _iPresStartSector;
    }

    public void feedSector(IdentifiedSector sector) throws IOException {
        int iSector = sector.getSectorNumber();
        for (ISectorAudioDecoder decoder : _aoDecoders) {
            if (decoder.getStartSector() <= iSector &&
                iSector <= decoder.getEndSector())
            {
                decoder.feedSector(sector);
                break;
            }
        }
    }

    public DiscItemAudioStream[] getSourceItems() {
        // TODO: maybe there's a better way to do this
        DiscItemAudioStream[] items = new DiscItemAudioStream[_aoDecoders.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = _aoDecoders[i].getSourceItems()[0];
        }
        return items;
    }
}
