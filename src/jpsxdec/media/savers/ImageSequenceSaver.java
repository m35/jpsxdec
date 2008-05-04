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
 * ImageSequenceSaver.java
 */

package jpsxdec.media.savers;

import java.awt.image.BufferedImage;
import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.media.StopPlayingException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.media.PSXMediaStreaming.IDemuxListener;
import jpsxdec.media.savers.Formats.ImgSeqVidFormat;
import jpsxdec.util.IO;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. Saves a series of images, 
 *  demux, mdec files. */
public class ImageSequenceSaver extends AbstractSaver 
        implements IDemuxListener
{

    private final PSXMediaStreaming m_oMedia;
    private final File m_oFolder;
    private final String m_sFileVidBase;
    private final Formats.ImgSeqVidFormat m_oOutputFormat;
    private final long m_lngEndFrame;
    private boolean m_blnUseDefaultJpeg;
    private float m_fltJpegQuality;
    
    private Decoders.DemuxToRgb oDecoder;
    private Decoders.DemuxToUncompress oUncompressor;
    
    private String m_sFileVidExt;
    
    public ImageSequenceSaver(SavingOptions oOptions) {
        
        m_oMedia = oOptions.getMedia();
        m_oFolder = oOptions.getFolder();
        m_sFileVidBase = oOptions.getVidFilenameBase();
        m_oOutputFormat = (ImgSeqVidFormat)oOptions.getVideoFormat();
        m_sFileVidExt = oOptions.getVidFilenameExt();
        
        oDecoder = Decoders.MakeDemuxToRgb(oOptions.getDecodeQuality());
        oUncompressor = Decoders.MakeDemuxToUncompress(oOptions.getDecodeQuality());

        m_lngEndFrame = oOptions.getEndFrame();
        
        m_oMedia.addVidDemuxListener(this);
    }

    @Override
    public void done() throws IOException {     
        m_oMedia.clearListeners();  //FIXME: This will clear other listeners
    }

    private File FileName(long frame) {
        return new File(m_oFolder, String.format( "%s%04d%s",
                m_sFileVidBase, frame, m_sFileVidExt));
    }
    
    public void event(StrFramePushDemuxer oDemux) throws StopPlayingException, IOException {
        long frame = oDemux.getFrameNumber();
        
        if (m_lngEndFrame >= 0 && frame > m_lngEndFrame) throw new StopPlayingException();
        
        fireProgressUpdate("Reading frame " + frame, 
                    m_oMedia.getStartFrame(),
                    m_oMedia.getEndFrame(),
                    frame);

        if (m_oOutputFormat == Formats.DEMUX) {
            IO.writeIStoFile(oDemux.getStream(), FileName(frame));
        } else if (m_oOutputFormat == Formats.MDEC) {
            InputStream is = oUncompressor.Uncompress(oDemux, super.m_oListener);
            if (is != null)
                IO.writeIStoFile(is, FileName(frame));
        } else {
            BufferedImage bi = oDecoder.UncompressDecodeRgb(oDemux, super.m_oListener);
            if (m_oOutputFormat == Formats.JPEG_IMG_SEQ && !m_blnUseDefaultJpeg) {
                boolean ok = ImageIO.write(bi, m_oOutputFormat.getId(), FileName(frame));
                if (!ok) throw new IOException("Unable to write frames as " + m_oOutputFormat.getDesciption() + " file format.");
            } else {
                // TODO: Write jpg with custom quality
                boolean ok = ImageIO.write(bi, m_oOutputFormat.getId(), FileName(frame));
                if (!ok) throw new IOException("Unable to write frames as " + m_oOutputFormat.getDesciption() + " file format.");
            }
        }
        
        if (m_lngEndFrame >= 0 && frame >= m_lngEndFrame) throw new StopPlayingException();
    }

}
