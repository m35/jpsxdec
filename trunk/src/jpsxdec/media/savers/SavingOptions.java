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
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioSystem;
import jpsxdec.media.PSXMedia;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.util.Fraction;
import jpsxdec.util.Misc;


public class SavingOptions {

    /* -----------------------------------------------------------------------*/
    /* -- Video formats ------------------------------------------------------*/
    /* -----------------------------------------------------------------------*/
    
    // <editor-fold defaultstate="collapsed" desc="Video formats">
    
    public static class VideoFormat {
        
        private final String id;
        private final String description;
        private final String ext;
        private final String altExt;

        public VideoFormat(String id, String description, String ext, String altExt) {
            this.id = id;
            this.description = description;
            this.ext = ext;
            this.altExt = altExt;
        }

        public VideoFormat(String id, String description, String ext) {
            this.id = id;
            this.description = description;
            this.ext = ext;
            this.altExt = null;
        }
        
        public String getId() {
            return id;
        }
        
        public String getExt() {
            return ext;
        }
        
        public boolean hasAltExt() {
            return altExt != null;
        }
        
        public String getAltExt() {
            return altExt;
        }
        
        public boolean equals(Object o) {
            if (o instanceof String) {
                return id.equals(o) ||
                        ext.equals(o) ||
                       (altExt != null && altExt.equals(o));
            } else if (o instanceof VideoFormat) {
                return id.equals(((VideoFormat)o).id);
            } else {
                return false;
            }
        }
        
        public String toString() {
            return description;
        }
        
    }
    
    /**
     * Output will be sorted as:
     * <ul>
     * <li> "AVI" avi
     * <li> png
     * <li> "jpeg" jpg*, jpeg
     * <li> <<others sorted by description>>
     * <li> "yuv4mpeg2" yuv*, y4m
     * <li> mdec
     * <li> demux
     * </ul>
     */
    public static Vector<VideoFormat> GetImageFormats(boolean withAvi) {
        Vector<VideoFormat> oValidFormats = new Vector<VideoFormat>();
        
        Vector<String> oFmts = Misc.GetJavaImageFormats();

        if (withAvi)
            oValidFormats.add(AVI_FORMAT); // avi first
        
        png(oFmts, oValidFormats); // then png
        jpg(oFmts, oValidFormats); // then jpg
        
        Collections.sort(oFmts); // sort the rest
        // and add them
        for (String fmt : oFmts) {
            oValidFormats.add(new VideoFormat(fmt, fmt, fmt));
        }

        // remaining
        oValidFormats.add(new VideoFormat("yuv", "yuv4mpeg2", "yuv", "y4m"));
        oValidFormats.add(new VideoFormat("mdec", "mdec", "mdec"));
        oValidFormats.add(new VideoFormat("demux", "demux", "demux"));
        
        return oValidFormats;
    }
    
    private static void jpg(List<String> ls, Vector<VideoFormat> vv) {
        String j1 = null;
        String j2 = null;
        
        int i;
        if ((i = ls.indexOf("jpg")) >= 0) {
            j1 = "jpg";
            ls.remove(i);
        }
        if ((i = ls.indexOf("jpeg")) >= 0) {
            if (j1 == null)
                j1 = "jpeg";
            else
                j2 = "jpeg";
            ls.remove(i);
        }
        
        if (j1 != null) {
            vv.add(new VideoFormat("jpg", "jpeg", j1, j2));
        }
        
    }
    
    private static void png(List<String> ls, Vector<VideoFormat> vv) {
        int i;
        if ((i = ls.indexOf("png")) >= 0) {
            vv.add(new VideoFormat("png", "png", "png"));
            ls.remove(i);
        }
    }
    
    final public static VideoFormat AVI_FORMAT = new VideoFormat("avi", "AVI", "avi");
    // </editor-fold>
    
    /* -----------------------------------------------------------------------*/
    /* -- Audio formats ------------------------------------------------------*/
    /* -----------------------------------------------------------------------*/
    
    // <editor-fold defaultstate="collapsed" desc="Audio formats">

    public static class AudioFormat {
        
        private final String id;
        private final String description;
        private final String ext;
        private final Type type;

        public AudioFormat(String id, String description, String ext, Type type) {
            this.id = id;
            this.description = description;
            this.ext = ext;
            this.type = type;
        }

        public String getId() {
            return id;
        }
        
        public String getExt() {
            return ext;
        }
        
        public Type getType() {
            return type;
        }

        public boolean equals(Object o) {
            if (o instanceof String) {
                return id.equals(o) ||
                        ext.equals(o);
            } else if (o instanceof AudioFormat) {
                return id.equals(((AudioFormat)o).id);
            } else if (type != null && (o instanceof Type)) {
                return type.equals(o);
            } else {
                return false;
            }
        }
        
        public String toString() {
            return description;
        }
        
    }
    
    final public static AudioFormat AVI_AUDIO = new AudioFormat("AVI", "part of AVI", "avi", null);
    
    public static Vector<AudioFormat> GetAudioFormats(boolean withAvi) {
        Vector<AudioFormat> v = new Vector<AudioFormat>();
        Vector<Type> at = new Vector<Type>();
        for (Type type : AudioSystem.getAudioFileTypes()) {
            at.add(type);
        }
        if (withAvi)
            v.add(AVI_AUDIO);
        
        wav(at, v);
        
        for (Type t : at) {
            v.add(new AudioFormat(t.toString(), t.toString().toLowerCase(), t.getExtension().toLowerCase(), t));
        }

        return v;                
    }
    
    private static void wav(Vector<Type> at, Vector<AudioFormat> v) {

        for (int i = 0; i < at.size(); i++) {
            Type t = at.get(i);
            if (t.toString().equals("WAVE")) {
                v.add(new AudioFormat(t.toString(), t.toString().toLowerCase(), t.getExtension().toLowerCase(), t));
                at.remove(i);
                break;
            }
        }

    }
    
    // </editor-fold>
    
    /* -----------------------------------------------------------------------*/
    /* -- AVI Codecs ---------------------------------------------------------*/
    /* -----------------------------------------------------------------------*/
    
    // <editor-fold defaultstate="collapsed" desc="AVI Codecs">
    
    public static class AviCodec {
        
        private final String description;
        private final String id;

        public AviCodec( String id,String name) {
            this.id = id;
            this.description = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getId() {
            return id;
        }
        
        public boolean equals(Object o) {
            if (o instanceof String) {
                return description.equals(o) ||
                        id.equals(o);
            } else if (o instanceof AviCodec) {
                return description.equals(((AviCodec)o).description);
            } else {
                return false;
            }
        }
        
        public String toString() {
            return description;
        }
        
    }
    
    final public static Vector<AviCodec> AVI_CODECS = GetAviCodecs();
    
    private static Vector<AviCodec> GetAviCodecs() {
        Vector<AviCodec> v = new Vector<SavingOptions.AviCodec>(2);
        v.add(new AviCodec("DIB", "Uncompressed"));
        v.add(new AviCodec("MJPG", "Compressed (MJPG)"));
        return v;
    }
    
    // </editor-fold>
    
    /* -----------------------------------------------------------------------*/
    /* -- SavingOptions -------------------------------------------------------*/
    /* -----------------------------------------------------------------------*/
    
     private static String videoFormatExt(String s, Vector<VideoFormat> oVidFormats) {
        for (VideoFormat fmt : oVidFormats) {
            if (fmt.equals(s)) return fmt.getExt();
        }
        return null;
    }
   
    static String audioFormatExt(String s, Vector<AudioFormat> AUDIO_FORMATS) {
        for (AudioFormat fmt : AUDIO_FORMATS) {
            if (fmt.equals(s)) return fmt.getExt();
        }
        return null;
    }
    
    
    public SavingOptions(PSXMediaStreaming media, String filenameBase, boolean decodeVideo, boolean decodeAudio) {
        
        this.media = media;
        this.videoFilenameBase = filenameBase;
        this.decodeAudio = decodeAudio;
        this.decodeVideo = decodeVideo;
        
        folder = new File(".");
        
        videoFormat = "avi";
        videoFilenameExt = videoFormatExt(videoFormat, GetImageFormats(true));
        if (decodeVideo) {
            framesPerSecond = media.getPossibleFPS()[0];
            startFrame = media.getStartFrame();
            endFrame = media.getEndFrame();
        }
        
        aviCodec = "MJPG";
        
        useDefaultJpegQuality = true;
        jpegQuality = 0.75f;


        doNotCrop = false;

        audioFormat = AVI_FORMAT.equals(videoFormat) ? "avi" : "wav";
        audioFilenameBase = filenameBase;
        audioFilenameExt = audioFormatExt(audioFormat, GetAudioFormats(false));
    }
    
    private PSXMediaStreaming media;      // *
    private String videoFilenameBase; // *
    
    private boolean decodeVideo;
    private String videoFormat;
    private String videoFilenameExt;
    
    private Fraction framesPerSecond;
    private String aviCodec;
    private boolean useDefaultJpegQuality;
    private float jpegQuality;
    
    private long startFrame;
    private long endFrame;
    
    private boolean doNotCrop;
    
    private boolean decodeAudio;
    private String audioFormat;
    private String audioFilenameBase;
    private String audioFilenameExt;
    
    private File folder;

    public File getFolder() {
        return folder;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public boolean decodeVideo() {
        return decodeVideo;
    }

    public void setAudioFormat(AudioFormat audioFormat) {
        this.audioFormat = audioFormat.getId();
    }

    public void setAviCodec(AviCodec aviCodec) {
        this.aviCodec = aviCodec.getId();
    }

    public void setDecodeVideo(boolean decodeVideo) {
        this.decodeVideo = decodeVideo;
    }

    public String getAudioFilenameBase() {
        return audioFilenameBase;
    }

    public void setAudioFilenameBase(String audioFilenameBase) {
        this.audioFilenameBase = audioFilenameBase;
    }

    public String getAudioFilenameExt() {
        return audioFilenameExt;
    }

    public void setAudioFilenameExt(String audioFilenameExt) {
        this.audioFilenameExt = audioFilenameExt;
    }

    public String getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }

    public String getAviCodec() {
        return aviCodec;
    }

    public void setAviCodec(String aviCodec) {
        this.aviCodec = aviCodec;
    }

    public boolean decodeAudio() {
        return decodeAudio;
    }

    public void setDecodeAudio(boolean decodeAudio) {
        this.decodeAudio = decodeAudio;
    }

    public boolean doNotCrop() {
        return doNotCrop;
    }

    public void setDoNotCrop(boolean doNotCrop) {
        this.doNotCrop = doNotCrop;
    }

    public long getEndFrame() {
        return endFrame;
    }

    public void setEndFrame(long endFrame) {
        this.endFrame = endFrame;
    }

    public Fraction getFramesPerSecond() {
        return framesPerSecond;
    }

    public void setFramesPerSecond(Fraction framesPerSecond) {
        this.framesPerSecond = framesPerSecond;
    }

    public float getJpegQuality() {
        return jpegQuality;
    }

    public void setJpegQuality(float jpegQuality) {
        this.jpegQuality = jpegQuality;
    }

    public PSXMediaStreaming getMedia() {
        return media;
    }

    public void setMedia(PSXMediaStreaming media) {
        this.media = media;
    }

    public long getStartFrame() {
        return startFrame;
    }

    public void setStartFrame(long startFrame) {
        this.startFrame = startFrame;
    }


    public boolean useDefaultJpegQuality() {
        return useDefaultJpegQuality;
    }

    public void setUseDefaultJpegQuality(boolean useDefaultJpegQuality) {
        this.useDefaultJpegQuality = useDefaultJpegQuality;
    }

    public String getVideoFilenameBase() {
        return videoFilenameBase;
    }

    public void setVideoFilenameBase(String videoFilenameBase) {
        this.videoFilenameBase = videoFilenameBase;
    }

    public String getVideoFilenameExt() {
        return videoFilenameExt;
    }

    public void setVideoFilenameExt(String videoFilenameExt) {
        this.videoFilenameExt = videoFilenameExt;
    }

    public String getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(String videoFormat) {
        this.videoFormat = videoFormat;
    }

    public void setVideoFormat(VideoFormat videoFormat) {
        this.videoFormat = videoFormat.getId();
    }

    
}
