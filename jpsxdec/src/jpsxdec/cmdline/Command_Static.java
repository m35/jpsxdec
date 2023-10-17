/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.UserFriendlyLogger;
import jpsxdec.modules.video.save.AutowireVDP;
import jpsxdec.modules.video.save.IOWritingException;
import jpsxdec.modules.video.save.MdecDecodeQuality;
import jpsxdec.modules.video.save.VDP;
import jpsxdec.modules.video.save.VideoFileNameFormatter;
import jpsxdec.modules.video.save.VideoFormat;
import jpsxdec.psxvideo.bitstreams.BitStreamDebugging;
import jpsxdec.psxvideo.mdec.ChromaUpsample;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.tim.Tim;
import jpsxdec.util.ArgParser;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/** Handle {@code -static} option. */
class Command_Static extends Command {

    public Command_Static() {
        super("-static");
    }

    private enum StaticType {
        bs, mdec, tim
    }
    @Nonnull
    private StaticType _eStaticType;

    @Override
    protected @CheckForNull ILocalizedMessage validate(@Nonnull String s) {
        for (StaticType type : StaticType.values()) {
            if (s.equalsIgnoreCase(type.name())) {
                _eStaticType = type;
                return null;
            }
        }
        return I.CMD_INVALID_VALUE_FOR_CMD(s, "-static");
    }

    @Override
    public void execute(@Nonnull ArgParser ap) throws CommandLineException {
        File inFile = getInFile();
        switch (_eStaticType) {
            case bs:
            case mdec:
                BooleanHolder debug = ap.addBoolOption("-debug");
                StringHolder dimensions = ap.addStringOption("-dim");
                StringHolder quality = ap.addStringOption("-quality","-q");
                StringHolder outFormat = ap.addStringOption("-fmt");
                StringHolder upsample = ap.addStringOption("-up");
                //......
                ap.match();
                //......

                // verify dimensions
                if (dimensions.value == null) {
                    throw new CommandLineException(I.CMD_DIM_OPTION_REQUIRED());
                }
                final int iWidth;
                final int iHeight;
                int[] aiDim = Misc.splitInt(dimensions.value, "x");
                if (aiDim == null || aiDim.length != 2)
                    throw new CommandLineException(I.CMD_INVALID_VALUE_FOR_CMD(dimensions.value, "-dim"));
                iWidth = aiDim[0];
                iHeight = aiDim[1];

                // valid output formats
                List<VideoFormat> validOutputFormat = new ArrayList<VideoFormat>(Arrays.asList(
                    VideoFormat.IMGSEQ_BITSTREAM, VideoFormat.IMGSEQ_JPG, VideoFormat.IMGSEQ_BMP, VideoFormat.IMGSEQ_PNG
                ));
                if (_eStaticType == StaticType.bs)
                    validOutputFormat.add(VideoFormat.IMGSEQ_MDEC);

                // verify output format
                VideoFormat outputFormat;
                _fbs.println(I.CMD_READING_STATIC_FILE(inFile));
                if (outFormat.value == null) {
                    outputFormat = VideoFormat.IMGSEQ_PNG;
                } else {
                    outputFormat = VideoFormat.fromCmdLine(outFormat.value);
                    if (!validOutputFormat.contains(outputFormat))
                        throw new CommandLineException(I.CMD_INVALID_VALUE_FOR_CMD(outFormat.value, "-fmt"));
                }

                // enable debugging
                if (debug.value) {
                    BitStreamDebugging.DEBUG = true;
                    MdecDecoder.DEBUG = true;
                    boolean blnAssertsEnabled = false;
                    assert blnAssertsEnabled = true;
                    if (!blnAssertsEnabled) {
                        _fbs.printlnWarn(I.CMD_ASSERT_DISABLED_NO_DEBUG());
                        _fbs.printlnWarn(I.CMD_ASSERT_DISABLED_NO_DEBUG_USE_EA());
                    }
                }

                // make logger
                UserFriendlyLogger log = new UserFriendlyLogger("static", _fbs.getUnderlyingStream());
                FileAndIssueListener fileAndIssueListener = new FileAndIssueListener(_fbs);
                log.setListener(fileAndIssueListener);

                // file listener
                String sFileBaseName = Misc.removeExt(inFile.getName());

                // build pipeline
                AutowireVDP pipeline = setupPipeline(iWidth, iHeight, outputFormat, sFileBaseName, quality.value, upsample.value, log);
                pipeline.setFileListener(fileAndIssueListener);

                // read input file
                byte[] abFileData;
                try {
                    abFileData = IO.readFile(inFile);
                } catch (FileNotFoundException ex) {
                    throw new CommandLineException(I.IO_OPENING_FILE_NOT_FOUND_NAME(inFile.toString()), ex);
                } catch (IOException ex) {
                    throw new CommandLineException(I.IO_READING_FILE_ERROR_NAME(inFile.toString()), ex);
                }

                try {
                    if (_eStaticType == StaticType.bs) {
                        pipeline.setBitstream2Mdec(new VDP.Bitstream2Mdec());
                        pipeline.autowire();
                        // finally convert the file
                        pipeline.getBitstreamListener().bitstream(abFileData, abFileData.length, null, Fraction.ZERO);
                    } else {
                        pipeline.autowire();
                        // finally convert the file
                        pipeline.getMdecListener().mdec(new MdecInputStreamReader(abFileData), null, Fraction.ZERO);
                    }

                    if (!fileAndIssueListener.blnHadIssue) {
                        _fbs.println(I.CMD_FRAME_CONVERT_OK());
                    }
                    _fbs.println(I.CMD_NUM_FILES_CREATED(fileAndIssueListener.genFiles.size()));

                } catch (IOWritingException ex) {
                    _fbs.printErr(I.IO_WRITING_TO_FILE_ERROR_NAME(ex.getOutFile().toString()));
                }
                return;
            case tim:
                saveTim(inFile);
                return;
        }
        throw new RuntimeException("Shouldn't happen");
    }

    private static class FileAndIssueListener implements VDP.GeneratedFileListener, UserFriendlyLogger.OnWarnErr {
        public final List<File> genFiles = new ArrayList<File>();
        public final FeedbackStream fbs;
        public boolean blnHadIssue = false;
        public FileAndIssueListener(FeedbackStream fbs) {
            this.fbs = fbs;
        }
        @Override
        public void fileGenerated(@Nonnull File f) {
            genFiles.add(f);
        }
        @Override
        public void onWarn(@Nonnull ILocalizedMessage msg) {
            fbs.printlnWarn(msg);
            blnHadIssue = true;
        }
        @Override
        public void onErr(@Nonnull ILocalizedMessage msg) {
            fbs.printlnErr(msg);
            blnHadIssue = true;
        }
    }

    private AutowireVDP setupPipeline(
                       int iWidth, int iHeight,
                       @Nonnull VideoFormat outputFormat, @Nonnull String sOutputBaseName,
                       @CheckForNull String sOutputQuality, @CheckForNull String sUpsampleQuality,
                       @Nonnull ILocalizedLogger log)
            throws CommandLineException
    {
        VideoFileNameFormatter formatter = new VideoFileNameFormatter(null, sOutputBaseName, outputFormat, iWidth, iHeight);
        AutowireVDP pipeline = new AutowireVDP();

        switch (outputFormat) {
            case IMGSEQ_MDEC:
                pipeline.setMdec2File(new VDP.Mdec2File(formatter, iWidth, iHeight, log));
                break;
            case IMGSEQ_JPG:
                pipeline.setMdec2File(new VDP.Mdec2Jpeg(formatter, iWidth, iHeight, log));
                break;

            case IMGSEQ_BMP:
            case IMGSEQ_PNG:
                    // made decoder, verify decoding quality
                    MdecDecodeQuality decQuality = MdecDecodeQuality.HIGH;
                    if (sOutputQuality != null) {
                        decQuality = MdecDecodeQuality.fromCmdLine(sOutputQuality);
                        if (decQuality == null)
                            throw new CommandLineException(I.CMD_INVALID_VALUE_FOR_CMD(sOutputQuality, "-q,-quality"));
                    }
                    MdecDecoder vidDecoder = decQuality.makeDecoder(iWidth, iHeight);
                    _fbs.println(I.CMD_USING_QUALITY(decQuality.getCmdLine()));

                    // verify upsample quality
                    ChromaUpsample up = ChromaUpsample.Bicubic;
                    if (vidDecoder instanceof MdecDecoder_double) {
                        if (sUpsampleQuality != null) {
                            up = ChromaUpsample.fromCmdLine(sUpsampleQuality);
                            if (up == null)
                                throw new CommandLineException(I.CMD_INVALID_VALUE_FOR_CMD(sUpsampleQuality, "-up"));
                        }
                        _fbs.println(I.CMD_UPSAMPLE_QUALITY(up.getDescription().getLocalizedMessage()));
                        ((MdecDecoder_double) vidDecoder).setUpsampler(up);
                    }

                    pipeline.setMdec2Decoded(new VDP.Mdec2Decoded(vidDecoder, log));
                    pipeline.setDecoded2File(new VDP.Decoded2JavaImage(formatter, outputFormat.getImgFmt(), iWidth, iHeight, log));
                break;
            default:
                throw new RuntimeException();
        }

        _fbs.println(I.CMD_SAVING_AS(formatter.format(null, log)));

        return pipeline;
    }

    private void saveTim(@Nonnull File inFile) throws CommandLineException {
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
    }

}
