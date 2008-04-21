/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * SavingOptions.java
 */

package jpsxdec.media.savers;

import java.io.File;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.util.Fraction;
import jpsxdec.util.Misc;


public class SavingOptions {
    
    
    /* -----------------------------------------------------------------------*/
    /* -- SavingOptions -------------------------------------------------------*/
    /* -----------------------------------------------------------------------*/
    
    public SavingOptions(PSXMediaStreaming media) {
        this.media = media;
    }
    
    private PSXMediaStreaming media;
    public PSXMediaStreaming getMedia() {
        return media;
    }
    
    
    private File folder;
    public void setFolder(File folder) {
        this.folder = folder;
    }
    public File getFolder() {
        if (folder == null)
            return new File(".").getAbsoluteFile();
        else
            return folder;
    }


    private String videoFilenameBase;
    private String videoFilenameExt;
    public void setVidFilename(String s) {
        String[] as = Misc.parseFilename(s);
        videoFilenameBase = as[0];
        videoFilenameExt = as[1];
    }
    public void setVideoFilenameBase(String s) {
        videoFilenameBase = s;
    }
    public void setVideoFilenameExt(String s) {
        videoFilenameExt = s;
    }
    public String getVidFilenameBase() {
        if (videoFilenameBase == null)
            return media.getSuggestedName();
        else
            return videoFilenameBase;
    }
    public String getVidFilenameExt() {
        if (videoFilenameExt == null)
            if (media.hasVideo())
                return "." + getVideoFormat().getExt();
            else
                return null;
        else
            return videoFilenameExt;
    }
    
    
    private Boolean decodeVideo;
    public void setDecodeVideo(boolean b) {
        decodeVideo = b;
    }
    public boolean getDecodeVideo() {
        if (media.hasVideo())
            if (decodeVideo == null)
                return true;
            else
                return decodeVideo;
        else
            return false;
    }
    
    
    private Formats.Format videoFormat;
    public void setVideoFormat(Formats.Format fmt) {
        if (fmt instanceof Formats.AviVidFormat ||
            fmt instanceof Formats.ImgSeqVidFormat)
            videoFormat = fmt;
        else
            throw new IllegalArgumentException();
    }
    public Formats.Format getVideoFormat() {
        if (videoFormat == null)
            if (media.hasVideo())
                return Formats.MJPG_AVI;
            else
                return null;
        else
            return videoFormat;
    }
    
    
    private Fraction framesPerSecond;
    public void setFps(Fraction f) {
        framesPerSecond = f;
    }
    public Fraction getFps() {
        if (framesPerSecond == null) {
            try {
                if (media.getPossibleFPS() == null)
                    return null;
                else
                    return media.getPossibleFPS()[0];
            } catch (UnsupportedOperationException ex) {
                return null;
            }
        }
        else
            return framesPerSecond;
    }
    
    
    private boolean useDefaultJpegQuality = true;
    public void setUseDefaultJpegQuality(boolean b) {
        useDefaultJpegQuality = b;
    }
    public boolean getUseDefaultJpegQuality() {
        return useDefaultJpegQuality;
    }
    
    private float jpegQuality = -1;
    public void setJpegQuality(float q) {
        jpegQuality = q;
    }
    public float getJpegQuality() {
        if (jpegQuality < 0)
            return 0.75f;
        else
            return jpegQuality;
    }
    
    
    private long startFrame = -1;
    public void setStartFrame(long l) {
        startFrame = l;
    }
    public long getStartFrame() {
        if (startFrame < 0)
            return media.getStartFrame();
        else
            return startFrame;
    }
            
    
    private long endFrame = -1;
    public void setEndFrame(long l) {
        endFrame = l;
    }
    public long getEndFrame() {
        if (endFrame < 0) {
            try {
                return media.getEndFrame();
            } catch (UnsupportedOperationException ex) {
                return -1;
            }
        } else
            return endFrame;
    }
    
    
    private boolean doNotCrop = false;
    public void setDoNotCrop(boolean b) {
        doNotCrop = b;
    }
    public boolean getDoNotCrop() {
        return doNotCrop;
    }
    
    private Boolean decodeAudio;
    public void setDecodeAudio(boolean b) {
        decodeAudio = b;
    }
    public boolean getDecodeAudio() {
        if (media.hasAudio())
            if (decodeAudio == null)
                return true;
            else
                return decodeAudio;
        else
            return false;
    }
    
    
    private String audioFilenameBase;
    private String audioFilenameExt;
    public void setAudioFilename(String s) {
        String[] as = Misc.parseFilename(s);
        audioFilenameBase = as[0];
        audioFilenameExt = as[1];
    }
    public void setAudioFilenameBase(String s) {
        audioFilenameBase = s;
    }
    public void setAudioFilenameExt(String s) {
        audioFilenameExt = s;
    }
    public String getAudioFilenameBase() {
        if (audioFilenameBase == null) 
            return media.getSuggestedName();
        else
            return audioFilenameBase;
    }
    public String getAudioFilenameExt() {
        if (audioFilenameExt == null)
            return "." + getAudioFormat().getExt();
        else
            return audioFilenameExt;
    }
    
    
    private Formats.Format audioFormat;
    public void setAudioFormat(Formats.Format fmt) {
        if (fmt instanceof Formats.AudFormat ||
            fmt instanceof Formats.AviAudFormat)
            audioFormat = fmt;
        else
            throw new IllegalArgumentException();
    }
    public Formats.Format getAudioFormat() {
        if (audioFormat == null)
            if (getVideoFormat() instanceof Formats.AviVidFormat)
                return Formats.AVI_WAV;
            else
                return Formats.WAVE; // TODO: Make sure we can actually save wav
        else
            return audioFormat;
    }
    
    
    private float volume = -1;
    public void setVolume(float v) {
        volume = v;
    }
    public float getVolume() {
        if (volume < 0)
            return 1.0f;
        else
            return volume;
    }
    
    
    
}
