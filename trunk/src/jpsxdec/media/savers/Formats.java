/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
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
 * Formats.java
 */

package jpsxdec.media.savers;

import java.util.Vector;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioSystem;

public class Formats {
    
    public static abstract class Format {
        protected String id;
        protected String description;
        
        public String getId() {
            return id;
        }
        
        public String getDesciption() {
            return description;
        }
        
        public abstract String getExt();

        @Override
        public String toString() {
            return description;
        }
    }
    
    public static interface HasJpeg {
        boolean hasJpeg();
    }
    
    public static class AviVidFormat extends Format implements HasJpeg {

        private boolean hasJpeg;
        
        private AviVidFormat(String id, String description) {
            this(id, description, false);
        }
        private AviVidFormat(String id, String description, boolean jpeg) {
            this.id = id;
            this.description = description;
            hasJpeg = jpeg;
        }
        
        public boolean hasJpeg() {
            return hasJpeg;
        }

        @Override
        public String getExt() {
            return "avi";
        }
    }
    
    public static class AviAudFormat extends Format {

        private AviAudFormat(String id, String description) {
            this.id = id;
            this.description = description;
        }

        @Override
        public String getExt() {
            return "";
        }
    }
    
    public static class ImgSeqVidFormat extends Format implements HasJpeg {
        private String[] ext;
        private boolean hasJpeg;

        private ImgSeqVidFormat(String id, String description, String ... ext) {
            this.id = id;
            this.description = description;
            this.ext = ext;
            hasJpeg = false;
        }
        private ImgSeqVidFormat(String id, String description, boolean jpeg, String ... ext) {
            this.id = id;
            this.description = description;
            this.ext = ext;
            hasJpeg = jpeg;
        }

        public boolean hasJpeg() {
            return hasJpeg;
        }

        @Override
        public String getExt() {
            return ext[0];
        }
    }
    
    public static class AudFormat extends Format {
        private String[] ext;
        private Type type;

        private AudFormat(Type type) {
            id = type.toString();
            description = type.toString().toLowerCase();
            ext = new String[] {type.getExtension()};
            this.type = type;
        }

        public boolean equals(Object o) {
            if (o instanceof AudFormat) {
                return this == o;
            } else if (o instanceof String) {
                if (id.equals(o)) return true;
                if (description.equals(o)) return true;
                for (String s : ext) {
                    if (s.equals(o)) return true;
                }
                return false;
            } else {
                return false;
            }
        }
        
        public Type getType() {
            return type;
        }
        
        @Override
        public String getExt() {
            return ext[0];
        }
    }
    
    /////////////////////////////////////////////////////////////////////////////
    
    public static final AviVidFormat UNCOMPRESSED_AVI = 
            new AviVidFormat("DIB", "Uncompressed AVI");
    
    public static final AviVidFormat MJPG_AVI = 
            new AviVidFormat("MJPG", "Compressed AVI (MJPG)", true);
    
    private static Vector<AviVidFormat> m_oAviVidFmts;
    public static Vector<AviVidFormat> getAviVidFormats() {
        if (m_oAviVidFmts != null) return m_oAviVidFmts;
        
        String[] asReaderFormats = ImageIO.getReaderFormatNames();

        boolean blnHasJpg = false;
        boolean blnHasBmp = false;
        
        for (String sFmt : asReaderFormats) {
            sFmt = sFmt.toLowerCase();
            if (sFmt.equals("jpg") || sFmt.equals("jpeg")) {
                blnHasJpg = true;
            } else if (sFmt.equals("bmp")) {
                blnHasBmp = true;
            }
        }

        m_oAviVidFmts = new Vector<AviVidFormat>();
        
        if (blnHasJpg)
            m_oAviVidFmts.add(MJPG_AVI);
        
        if (blnHasBmp)
            m_oAviVidFmts.add(UNCOMPRESSED_AVI);
        
        return m_oAviVidFmts;
    }
    
    
    public static final AviAudFormat AVI_WAV =
            new AviAudFormat("avi", "part of AVI");
    
    private static Vector<AviAudFormat> m_oAviAudFmts;
    public static Vector<AviAudFormat> getAviAudFormats() {
        if (m_oAviAudFmts != null) return m_oAviAudFmts;
        
        m_oAviAudFmts = new Vector<AviAudFormat>();
        
        m_oAviAudFmts.add(AVI_WAV);
        
        return m_oAviAudFmts;
    }
    
    public final static ImgSeqVidFormat JPEG_IMG_SEQ = 
            new ImgSeqVidFormat("jpg", "jpeg", true, "jpg", "jpeg");
    
    // TODO: Change this to a more general "Default lossless format"
    // which could be png or bmp, or whatever the platform has
    public static ImgSeqVidFormat PNG_IMG_SEQ = 
            new ImgSeqVidFormat("png", "png", "png");
    
    private static Vector<ImgSeqVidFormat> m_oAllJavaImgFormats;
    public static Vector<ImgSeqVidFormat> getAllJavaImgFormats() {
        if (m_oAllJavaImgFormats != null) return m_oAllJavaImgFormats;
        
        String[] asReaderFormats = ImageIO.getReaderFormatNames();

        boolean blnHasJpg = false;
        boolean blnHasBmp = false;
        boolean blnHasPng = false;
        Vector<String> oOtherFmts = new Vector<String>();
        
        for (String sFmt : asReaderFormats) {
            sFmt = sFmt.toLowerCase();
            if (sFmt.equals("jpg") || sFmt.equals("jpeg")) {
                blnHasJpg = true;
            } else if (sFmt.equals("bmp")) {
                blnHasBmp = true;
            } else if (sFmt.equals("png")) {
                blnHasPng = true;
            } else if (!oOtherFmts.contains(sFmt)) {
                oOtherFmts.add(sFmt);
            }
        }
        
        m_oAllJavaImgFormats = new Vector<ImgSeqVidFormat>();
        
        if (blnHasPng)
            m_oAllJavaImgFormats.add(PNG_IMG_SEQ);
        
        if (blnHasJpg)
            m_oAllJavaImgFormats.add(JPEG_IMG_SEQ);
        
        if (blnHasBmp)
            m_oAllJavaImgFormats.add(new ImgSeqVidFormat("bmp", "bmp", "bmp"));
        
        for (String sUnknFmt : oOtherFmts) {
            m_oAllJavaImgFormats.add(new ImgSeqVidFormat(sUnknFmt, sUnknFmt, sUnknFmt));
        }
        
        return m_oAllJavaImgFormats;
    }
    
    private static Vector<ImgSeqVidFormat> m_oVidCompatableImgFmts;
    public static Vector<ImgSeqVidFormat> getVidCompatableImgFmts() {
        if (m_oVidCompatableImgFmts != null) return m_oVidCompatableImgFmts;
        
        m_oVidCompatableImgFmts = new Vector<Formats.ImgSeqVidFormat>();
        
        for (ImgSeqVidFormat fmt : getAllJavaImgFormats()) {
            if (!fmt.getExt().equals("gif") && !fmt.getExt().equals("wbmp"))
                m_oVidCompatableImgFmts.add(fmt);
        }

        return m_oVidCompatableImgFmts;
    }
    
    public static ImgSeqVidFormat DEMUX =
            new ImgSeqVidFormat("demux", "Demuxed frame", "demux");
    
    public static ImgSeqVidFormat MDEC =
            new ImgSeqVidFormat("mdec", "Uncompressed frame", "mdec");
    
    public static ImgSeqVidFormat YUV =
            new ImgSeqVidFormat("yuv", "yuv4mpeg2", "yuv", "y4m");
    
    private static Vector<ImgSeqVidFormat> m_oExtentedFormats;
    public static Vector<ImgSeqVidFormat> getExtendedSeqFormats() {
        if (m_oExtentedFormats != null) return m_oExtentedFormats;
        
        m_oExtentedFormats = new Vector<Formats.ImgSeqVidFormat>(3);
        
        //m_oExtentedFormats.add(YUV); // yuv is not an img sequence format
        m_oExtentedFormats.add(DEMUX);
        m_oExtentedFormats.add(MDEC);
        
        return m_oExtentedFormats;
    }
    
    public final static AudFormat WAVE =
            new AudFormat(Type.WAVE);

    private static Vector<AudFormat> m_oAudFormats;
    public static Vector<AudFormat> getJavaAudFormats() {
        if (m_oAudFormats != null) return m_oAudFormats;
        
        Type[] aoTypes = AudioSystem.getAudioFileTypes();
        
        m_oAudFormats = new Vector<Formats.AudFormat>(aoTypes.length);
        
        for (Type type : aoTypes) {
            if (type.equals(Type.WAVE))
                m_oAudFormats.add(0, WAVE);
            else
                m_oAudFormats.add(new AudFormat(type));
        }
        
        return m_oAudFormats;
    }
    
    
    
    /*
     * ::IMAGE SEQUENCE::
     * 
     * These formats are known to handle true-color images
     *   ID    Description   Extentions
     *   bmp   Windows bmp   bmp
     *   jpg   jpeg          jpg*, jpeg
     *   png   png           png
     *
     * These can't save true-color images. Ignore if found.
     *   gif
     *   wbmp
     * 
     * We also have our own formats
     *   ID     Description           Extentions
     *   demux  Demuxed frame         demux
     *   mdec   For sending to MDEC   mdec
     *   yuv    yuv4mpeg2             yuv*, y4m
     * 
     * And then if there are formats not hangled, just include them
     *  ID       Description  Extentions
     *  <name>   <name>       <name>
     * 
     * 
     * ::AVI::
     * Avi can have two formats
     * ID     Description           Extention
     * DIB    Uncompressed AVI      avi
     * MJPG   Compressed AVI (MJPG) avi
     * 
     * ::PLAYER::
     * 
     * ::AUDIO::
     * ID
     * 
     * 
     */
    

}
