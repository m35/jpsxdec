/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.psxvideo.mdec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.discitems.savers.VideoSaverBuilder;
import jpsxdec.formats.Rec601YCbCrImage;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_double;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_int;
import jpsxdec.psxvideo.mdec.idct.StephensIDCT;
import jpsxdec.psxvideo.mdec.idct.simple_idct;
import jpsxdec.util.IO;
import jpsxdec.util.ProgressListener;
import jpsxdec.util.SimpleConsoleProgressListener;

/**
 *
 * @author Michael
 */
public class _Generate {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        ProgressListener listener = new SimpleConsoleProgressListener();
        CdFileSectorReader cd = new CdFileSectorReader(new File("D:\\PSXIMG\\ff9disc4.bin"));
        DiscIndex index = new DiscIndex(cd, listener);
        DiscItemVideoStream video = (DiscItemVideoStream) index.getById("SEQ11/FMV056.STR[0]");
        VideoSaverBuilder builder = video.makeSaverBuilder();
        builder.setSaveStartFrame(133);
        builder.setSaveEndFrame(133);
        builder.setVideoFormat(VideoSaverBuilder.VideoFormat.IMGSEQ_MDEC);
        IDiscItemSaver saver =  builder.makeSaver();
        saver.startSave(listener, new File("."));

        final int W = video.getWidth(), H = video.getHeight();
        int[] aiRgb = new int[W*H];
        Rec601YCbCrImage ycbcr = new Rec601YCbCrImage(W, H);

        MdecDecoder_int mdecIntSimple = new MdecDecoder_int(new simple_idct(), W, H);
        MdecDecoder_int mdecIntPsx = new MdecDecoder_int(new PsxMdecIDCT_int(), W, H);
        MdecDecoder_double mdecDblStephen = new MdecDecoder_double(new StephensIDCT(), W, H);
        MdecDecoder_double mdecDblPsx = new MdecDecoder_double(new PsxMdecIDCT_double(), W, H);

        MdecInputStreamReader in;

        File frameFile = saver.getOutputFile(0);
        System.out.println("=================================");
        System.out.println("Using file " + frameFile);
        System.out.println("=================================");

        in = new MdecInputStreamReader(frameFile);
        mdecIntSimple.decode(in);
        mdecIntSimple.readDecodedRgb(W, H, aiRgb);
        IO.writeFileBE("int_simple.rgb", aiRgb);

        in = new MdecInputStreamReader(frameFile);
        mdecIntPsx.decode(in);
        mdecIntPsx.readDecodedRgb(W, H, aiRgb);
        IO.writeFileBE("int_psx.rgb", aiRgb);

        in = new MdecInputStreamReader(frameFile);
        mdecDblStephen.decode(in);
        mdecDblStephen.readDecodedRgb(W, H, aiRgb);
        IO.writeFileBE("dbl_stephen.rgb", aiRgb);
        mdecDblStephen.readDecodedRec601YCbCr420(ycbcr);
        writeYCbCr("dbl_stephen.yuv", ycbcr);

        in = new MdecInputStreamReader(frameFile);
        mdecDblPsx.decode(in);
        mdecDblPsx.readDecodedRgb(W, H, aiRgb);
        IO.writeFileBE("dbl_psx.rgb", aiRgb);
        mdecDblPsx.readDecodedRec601YCbCr420(ycbcr);
        writeYCbCr("dbl_psx.yuv", ycbcr);
    }

    private static void writeYCbCr(String sFile, Rec601YCbCrImage ycbcr) throws IOException {
        FileOutputStream fos = new FileOutputStream(sFile);
        fos.write(ycbcr.getY());
        fos.write(ycbcr.getCb());
        fos.write(ycbcr.getCr());
        fos.close();
    }

}
