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
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.exception.ILocalizedException;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ConsoleProgressLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.modules.audio.sectorbased.DiscItemSectorBasedAudioStream;
import jpsxdec.modules.tim.DiscItemTim;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.sectorbased.DiscItemSectorBasedVideoStream;
import jpsxdec.modules.xa.DiscItemXaAudioStream;
import jpsxdec.util.ArgParser;
import jpsxdec.util.TaskCanceledException;

/** Handle {@code -i} options. */
class Command_Items {

    private static final Logger LOG = Logger.getLogger(Command_Items.class.getName());

    public static class Command_Item extends Command {

        @CheckForNull
        private String _sItemId;
        private int _iItemNum;

        public Command_Item() {
            super("-i","-item");
        }
        @Override
        protected @CheckForNull ILocalizedMessage validate(@Nonnull String s) {
            try {
                _iItemNum = Integer.parseInt(s);
                if (_iItemNum < 0)
                    return I.CMD_INVALID_VALUE_FOR_CMD(s, "-i,-item");
                else
                    return null;
            } catch (NumberFormatException ex) {
                if (s.contains(" "))
                    return I.CMD_INVALID_VALUE_FOR_CMD(s, "-i,-item");
                _sItemId = s;
                return null;
            }
        }
        @Override
        public void execute(@Nonnull ArgParser ap) throws CommandLineException {
            DiscIndex discIndex = getIndex();

            DiscItem item;
            if (_sItemId != null) {
                item = discIndex.getById(_sItemId);
                if (item == null)
                    throw new CommandLineException(I.CMD_DISC_ITEM_NOT_FOUND_STR(_sItemId));
            } else {
                item = discIndex.getByIndex(_iItemNum);
                if (item == null)
                    throw new CommandLineException(I.CMD_DISC_ITEM_NOT_FOUND_NUM(_iItemNum));
            }

            ConsoleProgressLogger saveLog = new ConsoleProgressLogger(
                    I.SAVE_LOG_FILE_BASE_NAME().getLocalizedMessage(), _fbs.getUnderlyingStream());
            ConsoleProgressLogger replaceLog = new ConsoleProgressLogger(
                    I.REPLACE_LOG_FILE_BASE_NAME().getLocalizedMessage(), _fbs.getUnderlyingStream());
            try {
                handleItem(item, ap, _fbs, saveLog, replaceLog);
            } finally {
                saveLog.close();
                replaceLog.close();
            }
        }
    }

    public static class Command_All extends Command {

        @Nonnull
        private String _sType;

        public Command_All() {
            super("-a","-all");
        }
        @Override
        protected @CheckForNull ILocalizedMessage validate(@Nonnull String s) {
            _sType = s;
            return null;
        }
        @Override
        public void execute(@Nonnull ArgParser ap) throws CommandLineException {

            BooleanHolder includeVideoAudio = ap.addBoolOption(false, "-vidaud");
            ap.match();

            DiscIndex discIndex = getIndex();

            ConsoleProgressLogger saveLog = new ConsoleProgressLogger(
                    I.SAVE_LOG_FILE_BASE_NAME().getLocalizedMessage(), _fbs.getUnderlyingStream());
            ConsoleProgressLogger replaceLog = new ConsoleProgressLogger(
                    I.REPLACE_LOG_FILE_BASE_NAME().getLocalizedMessage(), _fbs.getUnderlyingStream());

            boolean blnFound = false;
            try {
                for (DiscItem item : discIndex) {
                    if (item.getType().getName().equalsIgnoreCase(_sType)) {
                        boolean blnIsVideoAudio = (item instanceof DiscItemSectorBasedAudioStream) &&
                                             ((DiscItemSectorBasedAudioStream)item).isPartOfVideo();
                        if (!blnIsVideoAudio || includeVideoAudio.value) {
                            blnFound = true;
                            handleItem(item, ap.copy(), _fbs, saveLog, replaceLog);
                            _fbs.println(I.CMD_ITEM_COMPLETE());
                            _fbs.println();
                        }
                    }
                }
            } finally {
                saveLog.close();
                replaceLog.close();
            }

            if (!blnFound) {
                _fbs.println(I.CMD_NO_ITEMS_OF_TYPE(_sType));
            } else {
                _fbs.println(I.CMD_ALL_ITEMS_COMPLETE());
            }
        }
    }

    private static void handleItem(@Nonnull DiscItem item,
                                   @Nonnull ArgParser ap,
                                   @Nonnull FeedbackStream fbs,
                                   @Nonnull ConsoleProgressLogger saveLog,
                                   @Nonnull ConsoleProgressLogger replaceLog)
            throws CommandLineException
    {
        BooleanHolder itemHelpArg = ap.addHelp();
        BooleanHolder frameInfoArg = ap.addBoolOption("-frameinfodump");
        StringHolder replaceFrames = ap.addStringOption("-replaceframes");
        StringHolder replaceTim = ap.addStringOption("-replacetim");
        StringHolder replaceAudio = ap.addStringOption("-replaceaudio");
        StringHolder replaceXa = ap.addStringOption("-replacexa");
        StringHolder xaNum = ap.addStringOption("-xa");
        StringHolder directory = ap.addStringOption("-dir");
        ap.match();

        DiscPatcher patcher = null;
        try {
            if (itemHelpArg.value) {
                fbs.println(I.CMD_DETAILED_HELP_FOR());
                fbs.println(new UnlocalizedMessage(item.toString()));
                item.makeSaverBuilder().printHelp(fbs);
            } else if (frameInfoArg.value) {
                if (!(item instanceof DiscItemSectorBasedVideoStream)) {
                    throw new CommandLineException(I.CMD_DISC_ITEM_NOT_STR_VIDEO());
                } else {
                    ((DiscItemSectorBasedVideoStream)item).frameInfoDump(fbs.getUnderlyingStream(),
                                                                         fbs.getLevel() >= FeedbackStream.MORE);
                }
            } else if (replaceFrames.value != null) {
                if (!(item instanceof DiscItemVideoStream)) {
                    throw new CommandLineException(I.CMD_DISC_ITEM_NOT_STR_VIDEO());
                } else if (!(item instanceof DiscItemSectorBasedVideoStream)) {
                    throw new CommandLineException(I.CMD_DISC_ITEM_VIDEO_FRAME_REPLACE_UNSUPPORTED(item.getSerializationTypeId()));
                } else {
                    patcher = new DiscPatcher();
                    ((DiscItemSectorBasedVideoStream)item).replaceFrames(patcher, replaceFrames.value, replaceLog);
                    fbs.printlnWarn(I.CMD_BACKUP_DISC_IMAGE_WARNING());
                    fbs.printlnWarn(I.CMD_REOPENING_DISC_WRITE_ACCESS());
                    patcher.applyPatches((CdFileSectorReader) item.getSourceCd(), replaceLog);
                }
            } else if (replaceTim.value != null) {
                if (!(item instanceof DiscItemTim)) {
                    throw new CommandLineException(I.CMD_DISC_ITEM_NOT_TIM());
                } else {
                    DiscItemTim timItem = (DiscItemTim)item;
                    patcher = new DiscPatcher();
                    timItem.replace(patcher, new File(replaceTim.value), fbs);
                    fbs.printlnWarn(I.CMD_BACKUP_DISC_IMAGE_WARNING());
                    fbs.printlnWarn(I.CMD_REOPENING_DISC_WRITE_ACCESS());
                    patcher.applyPatches((CdFileSectorReader) timItem.getSourceCd(), replaceLog);
                }
            } else if (replaceAudio.value != null) {
                if (!(item instanceof DiscItemSectorBasedAudioStream))
                    throw new CommandLineException(I.CMD_DISC_ITEM_NOT_AUDIO());
                DiscItemSectorBasedAudioStream audioItem = (DiscItemSectorBasedAudioStream)item;

                patcher = new DiscPatcher();
                audioItem.replace(patcher, new File(replaceAudio.value), replaceLog);
                fbs.printlnWarn(I.CMD_BACKUP_DISC_IMAGE_WARNING());
                fbs.printlnWarn(I.CMD_REOPENING_DISC_WRITE_ACCESS());
                patcher.applyPatches((CdFileSectorReader) audioItem.getSourceCd(), replaceLog);
            } else if (replaceXa.value != null) {
                if (xaNum.value == null)
                    throw new CommandLineException(I.CMD_REPLACEXA_MISSING_XA_OPTION());

                if (!(item instanceof DiscItemXaAudioStream))
                    throw new CommandLineException(I.CMD_DISC_ITEM_NOT_XA());
                DiscItemXaAudioStream xaItem = (DiscItemXaAudioStream)item;

                fbs.println(I.CMD_XA_REPLACE_OPENING_PATCH_IDX(replaceXa.value));
                DiscIndex patchIndex = new DiscIndex(replaceXa.value, replaceLog);
                DiscItemXaAudioStream patchXa;
                try {
                    int iPatchXaIndex = Integer.parseInt(xaNum.value);
                    patchXa = (DiscItemXaAudioStream) patchIndex.getByIndex(iPatchXaIndex);
                    if (patchXa == null)
                        throw new NullPointerException();
                } catch (Throwable ex) {
                    throw new CommandLineException(I.CMD_XA_REPLACE_BAD_ITEM_NUM(xaNum.value), ex);
                }
                patcher = new DiscPatcher();
                xaItem.replaceXa(patcher, patchXa, replaceLog);
                fbs.printlnWarn(I.CMD_BACKUP_DISC_IMAGE_WARNING());
                fbs.printlnWarn(I.CMD_REOPENING_DISC_WRITE_ACCESS());
                patcher.applyPatches((CdFileSectorReader) xaItem.getSourceCd(), replaceLog);
            } else {
                File dir;
                if (directory.value != null)
                    dir = new File(directory.value);
                else
                    dir = null;
                // decode/extract the desired disc item
                decodeDiscItem(item, dir, ap, fbs, saveLog);
                fbs.println(I.CMD_PROCESS_COMPLETE());
            }

        } catch (Throwable ex) {
            if (ex instanceof ILocalizedException)
                throw new CommandLineException(((ILocalizedException)ex).getSourceMessage());
            ILocalizedMessage msg = I.CMD_ERR_EX_CLASS(ex, ex.getClass().getSimpleName());
            saveLog.log(Level.SEVERE, msg, ex);
            throw new CommandLineException(msg, ex);
        } finally {
            if (patcher != null)
                patcher.discard();
        }
    }

    private static void decodeDiscItem(@Nonnull DiscItem item, @CheckForNull File dir,
                                       @Nonnull ArgParser ap,
                                       @Nonnull FeedbackStream fbs,
                                       @Nonnull ConsoleProgressLogger cpl)
            throws LoggedFailure
    {

        DiscItemSaverBuilder builder = item.makeSaverBuilder();

        fbs.println(I.CMD_SAVING(item.toString()));

        builder.commandLineOptions(ap, fbs);

        fbs.println();

        builder.printSelectedOptions(fbs.makeLogger());

        long lngStart, lngEnd;
        lngStart = System.currentTimeMillis();
        try {
            cpl.log(Level.INFO, new UnlocalizedMessage(item.getSourceCd().toString()));
            cpl.log(Level.INFO, new UnlocalizedMessage(item.toString()));
            builder.startSave(cpl, dir);
            fbs.println(I.CMD_NUM_FILES_CREATED(builder.getGeneratedFiles().size()));
        } catch (TaskCanceledException ex) {
            LOG.log(Level.SEVERE, "SHOULD NEVER HAPPEN", ex);
        }
        lngEnd = System.currentTimeMillis();
        fbs.println(I.PROCESS_TIME((lngEnd - lngStart) / 1000.0));
    }

}
