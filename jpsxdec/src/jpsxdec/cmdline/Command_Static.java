/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2019  Michael Sabin
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

package jpsxdec.cmdline;

import argparser.BooleanHolder;
import argparser.StringHolder;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.UserFriendlyLogger;
import jpsxdec.modules.video.save.MdecDecodeQuality;
import jpsxdec.modules.video.save.VDP;
import jpsxdec.modules.video.save.VideoFileNameFormatter;
import jpsxdec.modules.video.save.VideoFormat;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.tim.Tim;
import jpsxdec.util.ArgParser;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;


class Command_Static extends Command {

    public Command_Static() {
        super("-static");
    }

    private static enum StaticType {
        bs, mdec, tim
    }
    @Nonnull
    private StaticType _eStaticType;

    protected @CheckForNull ILocalizedMessage validate(@Nonnull String s) {
        for (StaticType type : StaticType.values()) {
            if (s.equalsIgnoreCase(type.name())) {
                _eStaticType = type;
                return null;
            }
        }
        return I.CMD_STATIC_TYPE_INVALID(s);
    }

    public void execute(@Nonnull ArgParser ap) throws CommandLineException {
        File inFile = getInFile();
        switch (_eStaticType) {
            case bs:
            case mdec:
                BooleanHolder debug = ap.addBoolOption("-debug");
                StringHolder dimentions = ap.addStringOption("-dim");
                StringHolder quality = ap.addStringOption("-quality","-q");
                StringHolder format = ap.addStringOption("-fmt");
                StringHolder upsample = ap.addStringOption("-up");
                //......
                ap.match();
                //......
                if (dimentions.value == null) {
                    throw new CommandLineException(I.CMD_DIM_OPTION_REQURIED());
                }
                final int iWidth;
                final int iHeight;
                int[] aiDim = Misc.splitInt(dimentions.value, "x");
                iWidth = aiDim[0];
                iHeight = aiDim[1];
                if (debug.value) {
                    BitStreamUncompressor.DEBUG = true;
                    MdecDecoder.DEBUG = true;
                    boolean blnAssertsEnabled = false;
                    assert blnAssertsEnabled = true;
                    if (!blnAssertsEnabled) {
                        _fbs.printlnWarn(I.CMD_ASSERT_DISABLED_NO_DEBUG());
                        _fbs.printlnWarn(I.CMD_ASSERT_DISABLED_NO_DEBUG_USE_EA());
                    }
                }
                _fbs.println(I.CMD_READING_STATIC_FILE(inFile));
                String sFileBaseName = Misc.removeExt(inFile.getName());
                VideoFormat vf = VideoFormat.IMGSEQ_PNG;
                if (format.value != null) {
                    vf = VideoFormat.fromCmdLine(format.value);
                    if (vf == null || vf.isAvi() || vf == VideoFormat.IMGSEQ_BITSTREAM)
                        throw new CommandLineException(I.CMD_FORMAT_INVALID(format.value));
                }
                VDP.IMdecListener mdecOut;
                VideoFileNameFormatter formatter = new VideoFileNameFormatter(null, sFileBaseName, vf, iWidth, iHeight);
                UserFriendlyLogger log = new UserFriendlyLogger("static", _fbs.getUnderlyingStream());
                UserFriendlyLogger.WarnErrCounter warnErrCount = new UserFriendlyLogger.WarnErrCounter();
                log.setListener(warnErrCount);
                if (vf == VideoFormat.IMGSEQ_MDEC) {
                    mdecOut = new VDP.Mdec2File(formatter, iWidth, iHeight, log);
                } else if (vf == VideoFormat.IMGSEQ_JPG) {
                    VDP.Mdec2Jpeg m2j = new VDP.Mdec2Jpeg(formatter, iWidth, iHeight, log);
                    mdecOut = m2j;
                } else {
                    MdecDecodeQuality decQuality = MdecDecodeQuality.HIGH_PLUS;
                    if (quality.value != null) {
                        decQuality = MdecDecodeQuality.fromCmdLine(quality.value);
                        if (decQuality == null)
                            throw new CommandLineException(I.CMD_QUALITY_INVALID(quality.value));
                    }
                    MdecDecoder vidDecoder = decQuality.makeDecoder(iWidth, iHeight);
                    _fbs.println(I.CMD_USING_QUALITY(decQuality.getCmdLine()));
                    if (vidDecoder instanceof MdecDecoder_double_interpolate) {
                        MdecDecoder_double_interpolate.Upsampler up =
                                MdecDecoder_double_interpolate.Upsampler.Bicubic;
                        if (upsample.value != null) {
                            up = MdecDecoder_double_interpolate.Upsampler.fromCmdLine(upsample.value);
                            if (up == null)
                                throw new CommandLineException(I.CMD_UPSAMPLING_INVALID(upsample.value));
                        }
                        _fbs.println(I.CMD_USING_UPSAMPLING(up.getDescription()));
                        ((MdecDecoder_double_interpolate) vidDecoder).setResampler(up);
                    }
                    VDP.Mdec2Decoded m2d = new VDP.Mdec2Decoded(vidDecoder, log);
                    mdecOut = m2d;
                    VDP.Decoded2JavaImage imgOut = new VDP.Decoded2JavaImage(formatter, vf.getImgFmt(), iWidth, iHeight, log);
                    m2d.setDecoded(imgOut);
                }
                _fbs.println(I.CMD_SAVING_AS(formatter.format(null, log)));
                try {
                    byte[] abBitstream = IO.readFile(inFile); // TODO: separate exception for reading here
                    if (_eStaticType == StaticType.bs) {
                        VDP.Bitstream2Mdec b2m = new VDP.Bitstream2Mdec(mdecOut);
                        b2m.bitstream(abBitstream, abBitstream.length, null, new Fraction(-1));
                    } else {
                        mdecOut.mdec(new MdecInputStreamReader(abBitstream), null, new Fraction(-1));
                    }
                    if (warnErrCount.getWarnCount() == 0 && warnErrCount.getErrCount() == 0)
                        _fbs.println(I.CMD_FRAME_CONVERT_OK()); // TODO: have another message saying complete with issues
                    // TODO: how do I know the operation even generated any output?
                } catch (FileNotFoundException ex) {
                    throw new CommandLineException(I.IO_OPENING_FILE_NOT_FOUND_NAME(inFile.toString()), ex);
                } catch (IOException ex) {
                    throw new CommandLineException(I.IO_WRITING_TO_FILE_ERROR_NAME(inFile.toString()), ex);
                } catch (LoggedFailure ex) {
                    _fbs.printErr(ex.getSourceMessage());
                }
                return;
            case tim:
                _fbs.println(I.CMD_READING_TIM(inFile));
                FileInputStream is = null;
                try {
                    is = new FileInputStream(inFile);
                    String sOutBaseName = Misc.removeExt(inFile.getName());
                    Tim tim = Tim.read(is);
                    _fbs.println(new UnlocalizedMessage(tim.toString()));
                    int iDigitCount = String.valueOf(tim.getPaletteCount()).length();
                    for (int i = 0; i < tim.getPaletteCount(); i++) {
                        BufferedImage bi = tim.toBufferedImage(i);
                        String sFileName = String.format("%s_p%0" + iDigitCount + "d.png", sOutBaseName, i);
                        File file = new File(sFileName);
                        _fbs.println(I.IO_WRITING_FILE(file.getName()));
                        ImageIO.write(bi, "png", file);
                    }
                    _fbs.println(I.CMD_IMAGE_CONVERT_OK());
                } catch (BinaryDataNotRecognized ex) {
                    throw new CommandLineException(I.CMD_NOT_TIM(), ex);
                } catch (IOException ex) {
                    throw new CommandLineException(I.CMD_TIM_IO_ERR(), ex);
                } finally {
                    IO.closeSilently(is, Logger.getLogger(Command_Static.class.getName()));
                }
                return;
        }
        throw new RuntimeException("Shouldn't happen");
    }

}
