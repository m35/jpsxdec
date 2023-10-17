/*
 * jPSXdec Translations
 * Copyright (c) 2015-2019
 * Michael Sabin, Víctor González, Sergi Medina, Gianluigi "Infrid" Cusimano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jpsxdec.i18n;

import javax.annotation.Nonnull;

public class I {

    private static ILocalizedMessage msg(String sKey, String sEnglishDefault, Object ... args) {
        if (args.length == 0)
            return new LocalizedMessage(sKey, sEnglishDefault);
        else
            return new LocalizedMessage(sKey, sEnglishDefault, args);
    }

    /**
    <table border="1"><tr><td>
    <pre>jPSXdec: PSX media decoder (non-commercial) v{0}</pre>
    </td></tr></table>
    <ul>
       <li>AviWriter.java</li>
       <li>CommandLine.java</li>
       <li>DebugFormatter.java</li>
       <li>Gui.java</li>
       <li>GuiSettings.java</li>
       <li>Mdec2Jpeg.java</li>
       <li>UserFriendlyLogger.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage JPSXDEC_VERSION_NON_COMMERCIAL(@Nonnull String versionNumber) {
        return msg("JPSXDEC_VERSION_NON_COMMERCIAL", "jPSXdec: PSX media decoder (non-commercial) v{0}", versionNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Using source file {0}</pre>
    </td></tr></table>
    <p>The file provided by the -f command-line option</p>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_USING_SRC_FILE(@Nonnull java.io.File sourceFileName) {
        return msg("CMD_USING_SRC_FILE", "Using source file {0}", sourceFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Need a input file and/or index file to load.</pre>
    </td></tr></table>
    <p>If neither -f or -x command-line flags are used</p>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_NEED_INPUT_OR_INDEX() {
        return msg("CMD_NEED_INPUT_OR_INDEX", "Need a input file and/or index file to load.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Input file disc image required for this command.</pre>
    </td></tr></table>
    <p>-f command is required</p>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_FILE_REQUIRED() {
        return msg("CMD_DISC_FILE_REQUIRED", "Input file disc image required for this command.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Input file is required for this command.</pre>
    </td></tr></table>
    <p>-f command is required</p>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_INPUT_FILE_REQUIRED() {
        return msg("CMD_INPUT_FILE_REQUIRED", "Input file is required for this command.");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#} items loaded.</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ITEMS_LOADED(int itemCount) {
        return msg("CMD_ITEMS_LOADED", "{0,number,#} items loaded.", itemCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Reading index file {0}</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_READING_INDEX_FILE(@Nonnull String fileName) {
        return msg("CMD_READING_INDEX_FILE", "Reading index file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Input file not found {0}</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_INPUT_FILE_NOT_FOUND(@Nonnull java.io.File fileName) {
        return msg("CMD_INPUT_FILE_NOT_FOUND", "Input file not found {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid item number: {0}</pre>
    </td></tr></table>
    <p>Trying to look-up an item by its numeric index and the number is invalid (probably negative)</p>
    */
    public static @Nonnull ILocalizedMessage CMD_ITEM_NUMBER_INVALID(@Nonnull String badItemNumber) {
        return msg("CMD_ITEM_NUMBER_INVALID", "Invalid item number: {0}", badItemNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid item identifier: {0}</pre>
    </td></tr></table>
    <p>Trying to look-up an item by its string index identifier that is invalid (probably contains space)</p>
    */
    public static @Nonnull ILocalizedMessage CMD_ITEM_ID_INVALID(@Nonnull String badItemIdentifier) {
        return msg("CMD_ITEM_ID_INVALID", "Invalid item identifier: {0}", badItemIdentifier);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sorry, could not find any disc items of type &quot;{0}&quot;</pre>
    </td></tr></table>
    <p>Type will be like &quot;audio&quot; or &quot;video&quot; or &quot;image&quot; etc</p>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_NO_ITEMS_OF_TYPE(@Nonnull String discItemType) {
        return msg("CMD_NO_ITEMS_OF_TYPE", "Sorry, could not find any disc items of type \"{0}\"", discItemType);
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc item is not audio or video. Cannot create player.</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_ITEM_NOT_AUDIO_VIDEO_NO_PLAYER() {
        return msg("CMD_DISC_ITEM_NOT_AUDIO_VIDEO_NO_PLAYER", "Disc item is not audio or video. Cannot create player.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc item is not a standard STR video</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_ITEM_NOT_STR_VIDEO() {
        return msg("CMD_DISC_ITEM_NOT_STR_VIDEO", "Disc item is not a standard STR video");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc item is not a XA stream</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_ITEM_NOT_XA() {
        return msg("CMD_DISC_ITEM_NOT_XA", "Disc item is not a XA stream");
    }

    /**
    <table border="1"><tr><td>
    <pre>-replacexa option is missing -xa option</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_REPLACEXA_MISSING_XA_OPTION() {
        return msg("CMD_REPLACEXA_MISSING_XA_OPTION", "-replacexa option is missing -xa option");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc item is not an audio stream</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_ITEM_NOT_AUDIO() {
        return msg("CMD_DISC_ITEM_NOT_AUDIO", "Disc item is not an audio stream");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc item is not a TIM image.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_ITEM_NOT_TIM() {
        return msg("CMD_DISC_ITEM_NOT_TIM", "Disc item is not a TIM image.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Could not find disc item {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_ITEM_NOT_FOUND_NUM(int discItemIndex) {
        return msg("CMD_DISC_ITEM_NOT_FOUND_NUM", "Could not find disc item {0,number,#}", discItemIndex);
    }

    /**
    <table border="1"><tr><td>
    <pre>Could not find disc item {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_ITEM_NOT_FOUND_STR(@Nonnull String discItemId) {
        return msg("CMD_DISC_ITEM_NOT_FOUND_STR", "Could not find disc item {0}", discItemId);
    }

    /**
    <table border="1"><tr><td>
    <pre>Detailed help for</pre>
    </td></tr></table>
    <p>The next line printed to the console after this will be the description of the item</p>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DETAILED_HELP_FOR() {
        return msg("CMD_DETAILED_HELP_FOR", "Detailed help for");
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacing frames of video type &quot;{0}&quot; is not supported</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_ITEM_VIDEO_FRAME_REPLACE_UNSUPPORTED(@Nonnull String discItemTypeName) {
        return msg("CMD_DISC_ITEM_VIDEO_FRAME_REPLACE_UNSUPPORTED", "Replacing frames of video type \"{0}\" is not supported", discItemTypeName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_SAVING(@Nonnull String discItemDescription) {
        return msg("CMD_SAVING", "Saving {0}", discItemDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Item complete.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ITEM_COMPLETE() {
        return msg("CMD_ITEM_COMPLETE", "Item complete.");
    }

    /**
    <table border="1"><tr><td>
    <pre>All index items complete.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ALL_ITEMS_COMPLETE() {
        return msg("CMD_ALL_ITEMS_COMPLETE", "All index items complete.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc decoding/extracting complete.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_PROCESS_COMPLETE() {
        return msg("CMD_PROCESS_COMPLETE", "Disc decoding/extracting complete.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Time: {0,number,#.##} sec</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage PROCESS_TIME(double durationInSeconds) {
        return msg("PROCESS_TIME", "Time: {0,number,#.##} sec", durationInSeconds);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,choice,0#No files created|1#1 file created|2#'{0,number,#}' files created}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_NUM_FILES_CREATED(int fileCount) {
        return msg("CMD_NUM_FILES_CREATED", "{0,choice,0#No files created|1#1 file created|2#'{0,number,#}' files created}", fileCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Reopening disc image with write access.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_REOPENING_DISC_WRITE_ACCESS() {
        return msg("CMD_REOPENING_DISC_WRITE_ACCESS", "Reopening disc image with write access.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Hope your disc image is backed up because this is irreversible.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_BACKUP_DISC_IMAGE_WARNING() {
        return msg("CMD_BACKUP_DISC_IMAGE_WARNING", "Hope your disc image is backed up because this is irreversible.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid or missing XA item number {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_XA_REPLACE_BAD_ITEM_NUM(@Nonnull String badItemNumber) {
        return msg("CMD_XA_REPLACE_BAD_ITEM_NUM", "Invalid or missing XA item number {0}", badItemNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Creating player for</pre>
    </td></tr></table>
    <p>The next line will display the item info</p>
    */
    public static @Nonnull ILocalizedMessage CMD_CREATING_PLAYER() {
        return msg("CMD_CREATING_PLAYER", "Creating player for");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error with player</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_PLAYER_ERR() {
        return msg("CMD_PLAYER_ERR", "Error with player");
    }

    /**
    <table border="1"><tr><td>
    <pre>Opening patch index {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_XA_REPLACE_OPENING_PATCH_IDX(@Nonnull String patchIndexFileName) {
        return msg("CMD_XA_REPLACE_OPENING_PATCH_IDX", "Opening patch index {0}", patchIndexFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>save</pre>
    </td></tr></table>
    <p>The base name of the log file that will be created, e.g. &quot;save.log&quot;</p>
    <ul>
       <li>Command_Items.java</li>
       <li>SavingGuiTask.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage SAVE_LOG_FILE_BASE_NAME() {
        return msg("SAVE_LOG_FILE_BASE_NAME", "save");
    }

    /**
    <table border="1"><tr><td>
    <pre>index</pre>
    </td></tr></table>
    <p>The base name of the log file that will be created, e.g. &quot;index.log&quot;</p>
    <ul>
       <li>Gui.java</li>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_LOG_FILE_BASE_NAME() {
        return msg("INDEX_LOG_FILE_BASE_NAME", "index");
    }

    /**
    <table border="1"><tr><td>
    <pre>replace</pre>
    </td></tr></table>
    <p>The base name of the log file that will be created, e.g. &quot;replace.log&quot;</p>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_LOG_FILE_BASE_NAME() {
        return msg("REPLACE_LOG_FILE_BASE_NAME", "replace");
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1}</pre>
    </td></tr></table>
    <p>e.g. [INFO] Some message</p>
    <ul>
       <li>UserFriendlyLogger.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage USER_LOG_MESSAGE(@Nonnull String logLevel, @Nonnull String logMessage) {
        return msg("USER_LOG_MESSAGE", "[{0}] {1}", logLevel, logMessage);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1} {2}</pre>
    </td></tr></table>
    <p>e.g. [WARN] Some message BadException</p>
    */
    public static @Nonnull ILocalizedMessage USER_LOG_MESSAGE_EXCEPTION(@Nonnull String logLevel, @Nonnull String logMessage, @Nonnull String exceptionName) {
        return msg("USER_LOG_MESSAGE_EXCEPTION", "[{0}] {1} {2}", logLevel, logMessage, exceptionName);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1} {2} : {3}</pre>
    </td></tr></table>
    <p>e.g. [WARN] Some message BadException: Something bad happened</p>
    */
    public static @Nonnull ILocalizedMessage USER_LOG_MESSAGE_EXCEPTION_MSG(@Nonnull String logLevel, @Nonnull String logMessage, @Nonnull String exceptionName, @Nonnull String exceptionMessage) {
        return msg("USER_LOG_MESSAGE_EXCEPTION_MSG", "[{0}] {1} {2} : {3}", logLevel, logMessage, exceptionName, exceptionMessage);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1}</pre>
    </td></tr></table>
    <p>e.g. [WARN] BadException</p>
    */
    public static @Nonnull ILocalizedMessage USER_LOG_EXCEPTION(@Nonnull String logLevel, @Nonnull String exceptionName) {
        return msg("USER_LOG_EXCEPTION", "[{0}] {1}", logLevel, exceptionName);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1} : {2}</pre>
    </td></tr></table>
    <p>e.g. [WARN] BadException: Something bad happened</p>
    */
    public static @Nonnull ILocalizedMessage USER_LOG_EXCEPTION_MSG(@Nonnull String logLevel, @Nonnull String exceptionName, @Nonnull String exceptionMessage) {
        return msg("USER_LOG_EXCEPTION_MSG", "[{0}] {1} : {2}", logLevel, exceptionName, exceptionMessage);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid sector range: {0}</pre>
    </td></tr></table>
    <p>Sector range should be in the format &quot;start-end&quot;</p>
    */
    public static @Nonnull ILocalizedMessage CMD_SECTOR_RANGE_INVALID(@Nonnull String badSectorRangeString) {
        return msg("CMD_SECTOR_RANGE_INVALID", "Invalid sector range: {0}", badSectorRangeString);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid sector size &quot;{0}&quot;</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_INVALID_SECTOR_SIZE(@Nonnull String badSectorSizeString) {
        return msg("CMD_INVALID_SECTOR_SIZE", "Invalid sector size \"{0}\"", badSectorSizeString);
    }

    /**
    <table border="1"><tr><td>
    <pre>Copying sectors {0,number,#} - {1,number,#} to {2}</pre>
    </td></tr></table>
    <ul>
       <li>Command_CopySect.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_COPYING_SECTOR(int startSector, int endSector, @Nonnull String destinationFile) {
        return msg("CMD_COPYING_SECTOR", "Copying sectors {0,number,#} - {1,number,#} to {2}", startSector, endSector, destinationFile);
    }

    /**
    <table border="1"><tr><td>
    <pre>Generating sector list</pre>
    </td></tr></table>
    <ul>
       <li>Command_SectorDump.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_GENERATING_SECTOR_LIST() {
        return msg("CMD_GENERATING_SECTOR_LIST", "Generating sector list");
    }

    /**
    <table border="1"><tr><td>
    <pre>-dim option required</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DIM_OPTION_REQUIRED() {
        return msg("CMD_DIM_OPTION_REQUIRED", "-dim option required");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid dimension format &quot;{0}&quot;</pre>
    </td></tr></table>
    <ul>
       <li>Dimensions.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_INVALID_DIMENSIONS(@Nonnull String badDimensionsString) {
        return msg("CMD_INVALID_DIMENSIONS", "Invalid dimension format \"{0}\"", badDimensionsString);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid quality {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_QUALITY_INVALID(@Nonnull String badQuality) {
        return msg("CMD_QUALITY_INVALID", "Invalid quality {0}", badQuality);
    }

    /**
    <table border="1"><tr><td>
    <pre>Using quality {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_USING_QUALITY(@Nonnull ILocalizedMessage qualityName) {
        return msg("CMD_USING_QUALITY", "Using quality {0}", qualityName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error: not a Tim image</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_NOT_TIM() {
        return msg("CMD_NOT_TIM", "Error: not a Tim image");
    }

    /**
    <table border="1"><tr><td>
    <pre>Using upsampling {0}</pre>
    </td></tr></table>
    <p>See CHROMA_UPSAMPLE_*_DESCRIPTION</p>
    */
    public static @Nonnull ILocalizedMessage CMD_USING_UPSAMPLING(@Nonnull ILocalizedMessage upsampleDescription) {
        return msg("CMD_USING_UPSAMPLING", "Using upsampling {0}", upsampleDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error reading or writing TIM file</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_IO_ERR() {
        return msg("CMD_TIM_IO_ERR", "Error reading or writing TIM file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid format type {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_FORMAT_INVALID(@Nonnull String badFormat) {
        return msg("CMD_FORMAT_INVALID", "Invalid format type {0}", badFormat);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to enable decoding debug because asserts are disabled.</pre>
    </td></tr></table>
    <p>Right after this string, the next string (CMD_ASSERT_DISABLED_NO_DEBUG_USE_EA) is shown [TODO: combine these strings into 1]</p>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ASSERT_DISABLED_NO_DEBUG() {
        return msg("CMD_ASSERT_DISABLED_NO_DEBUG", "Unable to enable decoding debug because asserts are disabled.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Start java using the -ea option.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ASSERT_DISABLED_NO_DEBUG_USE_EA() {
        return msg("CMD_ASSERT_DISABLED_NO_DEBUG_USE_EA", "Start java using the -ea option.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Reading TIM file {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_READING_TIM(@Nonnull java.io.File timFileName) {
        return msg("CMD_READING_TIM", "Reading TIM file {0}", timFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid static type: {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_STATIC_TYPE_INVALID(@Nonnull String badStaticTypeName) {
        return msg("CMD_STATIC_TYPE_INVALID", "Invalid static type: {0}", badStaticTypeName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame converted successfully.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_FRAME_CONVERT_OK() {
        return msg("CMD_FRAME_CONVERT_OK", "Frame converted successfully.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Reading static file {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_READING_STATIC_FILE(@Nonnull java.io.File fileName) {
        return msg("CMD_READING_STATIC_FILE", "Reading static file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Image converted successfully</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_IMAGE_CONVERT_OK() {
        return msg("CMD_IMAGE_CONVERT_OK", "Image converted successfully");
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving as: {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_SAVING_AS(@Nonnull java.io.File fileName) {
        return msg("CMD_SAVING_AS", "Saving as: {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Generating visualization</pre>
    </td></tr></table>
    <ul>
       <li>Command_Visualize.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_GENERATING_VISUALIZATION() {
        return msg("CMD_GENERATING_VISUALIZATION", "Generating visualization");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error creating or writing the visualization</pre>
    </td></tr></table>
    <ul>
       <li>Command_Visualize.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VISUALIZATION_ERR() {
        return msg("CMD_VISUALIZATION_ERR", "Error creating or writing the visualization");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error loading index file: {0}</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ERR_LOADING_INDEX_FILE_REASON(@Nonnull ILocalizedMessage localizedDetails) {
        return msg("ERR_LOADING_INDEX_FILE_REASON", "Error loading index file: {0}", localizedDetails);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#} items found</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_NUM_ITEMS_FOUND(int itemCount) {
        return msg("CMD_NUM_ITEMS_FOUND", "{0,number,#} items found", itemCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Building index</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_BUILDING_INDEX() {
        return msg("CMD_BUILDING_INDEX", "Building index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc read error.</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_READ_ERROR() {
        return msg("CMD_DISC_READ_ERROR", "Disc read error.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid verbosity level {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_VERBOSE_LVL_INVALID_STR(@Nonnull String badVerbosityLevel) {
        return msg("CMD_VERBOSE_LVL_INVALID_STR", "Invalid verbosity level {0}", badVerbosityLevel);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid verbosity level {0,number,#}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_VERBOSE_LVL_INVALID_NUM(int badVerbosityNumber) {
        return msg("CMD_VERBOSE_LVL_INVALID_NUM", "Invalid verbosity level {0,number,#}", badVerbosityNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving index as {0}</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_SAVING_INDEX(@Nonnull String fileName) {
        return msg("CMD_SAVING_INDEX", "Saving index as {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Try -? for help.</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_TRY_HELP() {
        return msg("CMD_TRY_HELP", "Try -? for help.");
    }

    /**
    <table border="1"><tr><td>
    <pre>No items found, not saving index file</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_NOT_SAVING_EMPTY_INDEX() {
        return msg("CMD_NOT_SAVING_EMPTY_INDEX", "No items found, not saving index file");
    }

    /**
    <table border="1"><tr><td>
    <pre>ERROR: {0} ({1})</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
       <li>Command_Items.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ERR_EX_CLASS(@Nonnull java.lang.Throwable errorMessage, @Nonnull String exceptionType) {
        return msg("CMD_ERR_EX_CLASS", "ERROR: {0} ({1})", errorMessage, exceptionType);
    }

    /**
    <table border="1"><tr><td>
    <pre>Need a main command.</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_NEED_MAIN_COMMAND() {
        return msg("CMD_NEED_MAIN_COMMAND", "Need a main command.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Identified as {0}</pre>
    </td></tr></table>
    <p>See DISC_FMT_*</p>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_IDENTIFIED(@Nonnull ILocalizedMessage discFormatDescription) {
        return msg("CMD_DISC_IDENTIFIED", "Identified as {0}", discFormatDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Command needs disc file</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_COMMAND_NEEDS_DISC() {
        return msg("CMD_COMMAND_NEEDS_DISC", "Command needs disc file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Too many main commands.</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_TOO_MANY_MAIN_COMMANDS() {
        return msg("CMD_TOO_MANY_MAIN_COMMANDS", "Too many main commands.");
    }

    /**
    <table border="1"><tr><td>
    <pre>File not found {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_FILE_NOT_FOUND_FILE(@Nonnull String fileName) {
        return msg("CMD_FILE_NOT_FOUND_FILE", "File not found {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Indexing {0}</pre>
    </td></tr></table>
    <ul>
       <li>InFileAndIndexArgs.java</li>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_GUI_INDEXING(@Nonnull String cdFileDescription) {
        return msg("CMD_GUI_INDEXING", "Indexing {0}", cdFileDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1,number,#%}{2,choice,0#|1# '{2,number,#}' '{2,choice,0#warnings|1#warning|1&lt;warnings}'}{3,choice,0#|1# '{3,number,#}' '{3,choice,0#errors|1#error|1&lt;errors}'}</pre>
    </td></tr></table>
    <p>Note the single quotes are necessary inside the choice argument.</p>
    <ul>
       <li>ConsoleProgressLogger.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_PROGRESS(@Nonnull String progressBar, double percentComplete, int warningCount, int errorCount) {
        return msg("CMD_PROGRESS", "[{0}] {1,number,#%}{2,choice,0#|1# '{2,number,#}' '{2,choice,0#warnings|1#warning|1<warnings}'}{3,choice,0#|1# '{3,number,#}' '{3,choice,0#errors|1#error|1<errors}'}", progressBar, percentComplete, warningCount, errorCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1,number,#%} {2} {3,choice,0#|1# '{3,number,#}' '{3,choice,0#warnings|1#warning|1&lt;warnings}'}{4,choice,0#|1# '{4,number,#}' '{4,choice,0#errors|1#error|1&lt;errors}'}</pre>
    </td></tr></table>
    <p>Note the single quotes are necessary inside the choice argument.</p>
    <ul>
       <li>ConsoleProgressLogger.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_PROGRESS_WITH_MSG(@Nonnull String progressBar, double percentComplete, @Nonnull ILocalizedMessage message, int warningCount, int errorCount) {
        return msg("CMD_PROGRESS_WITH_MSG", "[{0}] {1,number,#%} {2} {3,choice,0#|1# '{3,number,#}' '{3,choice,0#warnings|1#warning|1<warnings}'}{4,choice,0#|1# '{4,number,#}' '{4,choice,0#errors|1#error|1<errors}'}", progressBar, percentComplete, message, warningCount, errorCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} size is {1} bytes, which is too small to identify (needs to be at least {2} bytes)</pre>
    </td></tr></table>
    <p>This occurs when trying to open a file and it's too small to recognize what it is</p>
    <ul>
       <li>CdException.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CD_FILE_TOO_SMALL(@Nonnull String fileName, long fileSize, int minimumSize) {
        return msg("CD_FILE_TOO_SMALL", "{0} size is {1} bytes, which is too small to identify (needs to be at least {2} bytes)", fileName, fileSize, minimumSize);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} size is {1} bytes which is larger than the allowed size of {2} bytes</pre>
    </td></tr></table>
    <ul>
       <li>CdException.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CD_FILE_TOO_LARGE(@Nonnull String fileName, long fileSize, int maximumSize) {
        return msg("CD_FILE_TOO_LARGE", "{0} size is {1} bytes which is larger than the allowed size of {2} bytes", fileName, fileSize, maximumSize);
    }

    /**
    <table border="1"><tr><td>
    <pre>*Unsaved*</pre>
    </td></tr></table>
    <p>Shows in the window title in place of the file name when the index is not saved</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_TITLE_UNSAVED_INDEX() {
        return msg("GUI_TITLE_UNSAVED_INDEX", "*Unsaved*");
    }

    /**
    <table border="1"><tr><td>
    <pre>Open and Analyze File</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_OPEN_ANALYZE_DISC_BTN() {
        return msg("GUI_OPEN_ANALYZE_DISC_BTN", "Open and Analyze File");
    }

    /**
    <table border="1"><tr><td>
    <pre>File does not contain raw sector headers -- audio may not be detected. See manual for details.</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_DISC_NO_RAW_HEADERS_WARNING() {
        return msg("GUI_DISC_NO_RAW_HEADERS_WARNING", "File does not contain raw sector headers -- audio may not be detected. See manual for details.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Could not identify anything in file {0}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_DIALOG_COULD_NOT_IDENTIFY_ANYTHING(@Nonnull String fileName) {
        return msg("GUI_DIALOG_COULD_NOT_IDENTIFY_ANYTHING", "Could not identify anything in file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Select disc image or media file</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_OPEN_DISC_DIALOG_TITLE() {
        return msg("GUI_OPEN_DISC_DIALOG_TITLE", "Select disc image or media file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Play</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_PLAY_BTN() {
        return msg("GUI_PLAY_BTN", "Play");
    }

    /**
    <table border="1"><tr><td>
    <pre>Open Index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_OPEN_INDEX_BTN() {
        return msg("GUI_OPEN_INDEX_BTN", "Open Index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Load index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_LOAD_INDEX_FILE_DIALOG_TITLE() {
        return msg("GUI_LOAD_INDEX_FILE_DIALOG_TITLE", "Load index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Issues loading index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_LOAD_ISSUES_DIALOG_TITLE() {
        return msg("GUI_INDEX_LOAD_ISSUES_DIALOG_TITLE", "Issues loading index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Loaded {0,number,#} items, but with issues.</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_LOAD_ISSUES(int itemCount) {
        return msg("GUI_INDEX_LOAD_ISSUES", "Loaded {0,number,#} items, but with issues.", itemCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Warnings: {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_LOAD_ISSUES_WARNINGS(int warningCount) {
        return msg("GUI_INDEX_LOAD_ISSUES_WARNINGS", "Warnings: {0,number,#}", warningCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Errors: {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_LOAD_ISSUES_ERRORS(int errorCount) {
        return msg("GUI_INDEX_LOAD_ISSUES_ERRORS", "Errors: {0,number,#}", errorCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>See {0} for details.</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_LOAD_ISSUES_SEE_FILE(@Nonnull String logFileName) {
        return msg("GUI_INDEX_LOAD_ISSUES_SEE_FILE", "See {0} for details.", logFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Save Index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_INDEX_BTN() {
        return msg("GUI_SAVE_INDEX_BTN", "Save Index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_INDEX_FILE_DIALOG_TITLE() {
        return msg("GUI_SAVE_INDEX_FILE_DIALOG_TITLE", "Save index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error saving index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_INDEX_ERR() {
        return msg("GUI_SAVE_INDEX_ERR", "Error saving index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Directory:</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_DIRECTORY_LABEL() {
        return msg("GUI_DIRECTORY_LABEL", "Directory:");
    }

    /**
    <table border="1"><tr><td>
    <pre>...</pre>
    </td></tr></table>
    <p>Button</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_DIR_CHOOSER_BTN() {
        return msg("GUI_DIR_CHOOSER_BTN", "...");
    }

    /**
    <table border="1"><tr><td>
    <pre>Select ...</pre>
    </td></tr></table>
    <p>Button</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SELECT_BTN() {
        return msg("GUI_SELECT_BTN", "Select ...");
    }

    /**
    <table border="1"><tr><td>
    <pre>Collapse All</pre>
    </td></tr></table>
    <p>Button</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_COLLAPSE_ALL_BTN() {
        return msg("GUI_COLLAPSE_ALL_BTN", "Collapse All");
    }

    /**
    <table border="1"><tr><td>
    <pre>Expand All</pre>
    </td></tr></table>
    <p>Button</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_EXPAND_ALL_BTN() {
        return msg("GUI_EXPAND_ALL_BTN", "Expand All");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save All Selected</pre>
    </td></tr></table>
    <p>Button</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_ALL_SELECTED_BTN() {
        return msg("GUI_SAVE_ALL_SELECTED_BTN", "Save All Selected");
    }

    /**
    <table border="1"><tr><td>
    <pre>Nothing is marked for saving.</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_NOTHING_IS_MARKED_FOR_SAVING() {
        return msg("GUI_NOTHING_IS_MARKED_FOR_SAVING", "Nothing is marked for saving.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Apply to all {0}</pre>
    </td></tr></table>
    <p>See ITEM_TYPE_*_APPLY</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_APPLY_TO_ALL_BTN(@Nonnull ILocalizedMessage itemTypeName) {
        return msg("GUI_APPLY_TO_ALL_BTN", "Apply to all {0}", itemTypeName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Applied settings to {0,number,#} items.</pre>
    </td></tr></table>
    <p>Dialog box</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_APPLIED_SETTINGS(int itemCount) {
        return msg("GUI_APPLIED_SETTINGS", "Applied settings to {0,number,#} items.", itemCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>The index has not been saved. Save index?</pre>
    </td></tr></table>
    <p>Dialog box</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_INDEX_PROMPT() {
        return msg("GUI_SAVE_INDEX_PROMPT", "The index has not been saved. Save index?");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save index?</pre>
    </td></tr></table>
    <p>Dialog box</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_INDEX_PROMPT_TITLE() {
        return msg("GUI_SAVE_INDEX_PROMPT_TITLE", "Save index?");
    }

    /**
    <table border="1"><tr><td>
    <pre>Play</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_PLAY_TAB() {
        return msg("GUI_PLAY_TAB", "Play");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_TAB() {
        return msg("GUI_SAVE_TAB", "Save");
    }

    /**
    <table border="1"><tr><td>
    <pre>Pause</pre>
    </td></tr></table>
    <p>Button</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_PAUSE_BTN() {
        return msg("GUI_PAUSE_BTN", "Pause");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bad error</pre>
    </td></tr></table>
    <p>I may combine with GUI_UNHANDLED_ERROR at some point</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_BAD_ERROR() {
        return msg("GUI_BAD_ERROR", "Bad error");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error loading index file</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ERR_LOADING_INDEX_FILE() {
        return msg("ERR_LOADING_INDEX_FILE", "Error loading index file");
    }

    /**
    <table border="1"><tr><td>
    <pre>CD images (*.iso, *.bin, *.img, *.mdf)</pre>
    </td></tr></table>
    <p>File dialog format</p>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_CD_IMAGE_EXTENSIONS() {
        return msg("GUI_CD_IMAGE_EXTENSIONS", "CD images (*.iso, *.bin, *.img, *.mdf)");
    }

    /**
    <table border="1"><tr><td>
    <pre>Index files (*.idx)</pre>
    </td></tr></table>
    <p>File dialog format</p>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_EXTENSION() {
        return msg("GUI_INDEX_EXTENSION", "Index files (*.idx)");
    }

    /**
    <table border="1"><tr><td>
    <pre>PlayStation video (*.str, *.mov, *.iki, *.ik2)</pre>
    </td></tr></table>
    <p>File dialog format</p>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_PSX_VIDEO_EXTENSIONS() {
        return msg("GUI_PSX_VIDEO_EXTENSIONS", "PlayStation video (*.str, *.mov, *.iki, *.ik2)");
    }

    /**
    <table border="1"><tr><td>
    <pre>All compatible types</pre>
    </td></tr></table>
    <p>File dialog format</p>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_ALL_COMPATIBLE_EXTENSIONS() {
        return msg("GUI_ALL_COMPATIBLE_EXTENSIONS", "All compatible types");
    }

    /**
    <table border="1"><tr><td>
    <pre>PlayStation/CD-i audio (*.xa, *.xai)</pre>
    </td></tr></table>
    <p>File dialog format</p>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_XA_EXTENSION() {
        return msg("GUI_XA_EXTENSION", "PlayStation/CD-i audio (*.xa, *.xai)");
    }

    /**
    <table border="1"><tr><td>
    <pre>The file &quot;{0}&quot; already exists!
Do you want to replace it?</pre>
    </td></tr></table>
    <p>Dialog</p>
    <ul>
       <li>BetterFileChooser.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_FILE_EXISTS_REPLACE(@Nonnull String fileName) {
        return msg("GUI_FILE_EXISTS_REPLACE", "The file \"{0}\" already exists!\nDo you want to replace it?", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre></pre>
    </td></tr></table>
    <p>Name column is blank in English</p>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_TREE_NAME_COLUMN() {
        return msg("GUI_TREE_NAME_COLUMN", "");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sectors</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SECTORS_COLUMN() {
        return msg("GUI_SECTORS_COLUMN", "Sectors");
    }

    /**
    <table border="1"><tr><td>
    <pre>Type</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_TREE_TYPE_COLUMN() {
        return msg("GUI_TREE_TYPE_COLUMN", "Type");
    }

    /**
    <table border="1"><tr><td>
    <pre>none</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SELECT_NONE() {
        return msg("GUI_SELECT_NONE", "none");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Videos</pre>
    </td></tr></table>
    <p>Drop-down</p>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SELECT_ALL_VIDEO() {
        return msg("GUI_SELECT_ALL_VIDEO", "all Videos");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Files</pre>
    </td></tr></table>
    <p>Drop-down</p>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SELECT_ALL_FILES() {
        return msg("GUI_SELECT_ALL_FILES", "all Files");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Audio (excluding video audio)</pre>
    </td></tr></table>
    <p>Drop-down</p>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SELECT_ALL_AUIO_EX_VID() {
        return msg("GUI_SELECT_ALL_AUIO_EX_VID", "all Audio (excluding video audio)");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Audio (including video audio)</pre>
    </td></tr></table>
    <p>Drop-down</p>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SELECT_ALL_AUDIO_INC_VID() {
        return msg("GUI_SELECT_ALL_AUDIO_INC_VID", "all Audio (including video audio)");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Images</pre>
    </td></tr></table>
    <p>Drop-down</p>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SELECT_ALL_IMAGES() {
        return msg("GUI_SELECT_ALL_IMAGES", "all Images");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Sound clips</pre>
    </td></tr></table>
    <p>Drop-down</p>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SELECT_ALL_SOUNDS() {
        return msg("GUI_SELECT_ALL_SOUNDS", "all Sound clips");
    }

    /**
    <table border="1"><tr><td>
    <pre>Details</pre>
    </td></tr></table>
    <p>Column name</p>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_TREE_DETAILS_COLUMN() {
        return msg("GUI_TREE_DETAILS_COLUMN", "Details");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save</pre>
    </td></tr></table>
    <p>Column name</p>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_TREE_SAVE_COLUMN() {
        return msg("GUI_TREE_SAVE_COLUMN", "Save");
    }

    /**
    <table border="1"><tr><td>
    <pre>#</pre>
    </td></tr></table>
    <p>Column name</p>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
       <li>GuiTree.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_TREE_INDEX_NUMBER_COLUMN() {
        return msg("GUI_TREE_INDEX_NUMBER_COLUMN", "#");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unnamed</pre>
    </td></tr></table>
    <ul>
       <li>IndexId.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage UNNAMED_DISC_ITEM() {
        return msg("UNNAMED_DISC_ITEM", "Unnamed");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid id format: {0}</pre>
    </td></tr></table>
    <ul>
       <li>IndexId.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ID_FORMAT_INVALID(@Nonnull String badIndexId) {
        return msg("ID_FORMAT_INVALID", "Invalid id format: {0}", badIndexId);
    }

    /**
    <table border="1"><tr><td>
    <pre>Warnings:</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_WARNINGS_LABEL() {
        return msg("GUI_INDEX_WARNINGS_LABEL", "Warnings:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Failure - See {0} for details</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_RESULT_FAILURE(@Nonnull String logFileName) {
        return msg("GUI_INDEX_RESULT_FAILURE", "Failure - See {0} for details", logFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Canceled</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_RESULT_CANCELED() {
        return msg("GUI_INDEX_RESULT_CANCELED", "Canceled");
    }

    /**
    <table border="1"><tr><td>
    <pre>Progress...</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_TITLE() {
        return msg("GUI_INDEX_TITLE", "Progress...");
    }

    /**
    <table border="1"><tr><td>
    <pre>Exception</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_EXCEPTION_DIALOG_TITLE() {
        return msg("GUI_INDEX_EXCEPTION_DIALOG_TITLE", "Exception");
    }

    /**
    <table border="1"><tr><td>
    <pre>Success with messages - See {0} for details</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_RESULT_OK_MSGS(@Nonnull String logFileName) {
        return msg("GUI_INDEX_RESULT_OK_MSGS", "Success with messages - See {0} for details", logFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Indexing:</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEXING_LABEL() {
        return msg("GUI_INDEXING_LABEL", "Indexing:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Errors:</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_ERRORS_LABEL() {
        return msg("GUI_INDEX_ERRORS_LABEL", "Errors:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Success!</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_INDEX_RESULT_SUCCESS() {
        return msg("GUI_INDEX_RESULT_SUCCESS", "Success!");
    }

    /**
    <table border="1"><tr><td>
    <pre>Cancel</pre>
    </td></tr></table>
    <p>Button</p>
    <ul>
       <li>IndexingGui.java</li>
       <li>SavingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_CANCEL_BTN() {
        return msg("GUI_CANCEL_BTN", "Cancel");
    }

    /**
    <table border="1"><tr><td>
    <pre>Close</pre>
    </td></tr></table>
    <p>Button</p>
    <ul>
       <li>IndexingGui.java</li>
       <li>SavingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_CLOSE_BTN() {
        return msg("GUI_CLOSE_BTN", "Close");
    }

    /**
    <table border="1"><tr><td>
    <pre>Start</pre>
    </td></tr></table>
    <p>Button</p>
    <ul>
       <li>IndexingGui.java</li>
       <li>SavingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_START_BTN() {
        return msg("GUI_START_BTN", "Start");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unhandled error</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
       <li>SavingGuiTask.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_UNHANDLED_ERROR() {
        return msg("GUI_UNHANDLED_ERROR", "Unhandled error");
    }

    /**
    <table border="1"><tr><td>
    <pre>Complete | See {0} for details</pre>
    </td></tr></table>
    <ul>
       <li>SavingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_STATUS_OVERALL_COMPLETE(@Nonnull String fileName) {
        return msg("GUI_SAVE_STATUS_OVERALL_COMPLETE", "Complete | See {0} for details", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Canceled | See {0} for details</pre>
    </td></tr></table>
    <ul>
       <li>SavingGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_STATUS_OVERALL_CANCELED(@Nonnull String fileName) {
        return msg("GUI_SAVE_STATUS_OVERALL_CANCELED", "Canceled | See {0} for details", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed!</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_STATUS_FAILED() {
        return msg("GUI_SAVE_STATUS_FAILED", "Failed!");
    }

    /**
    <table border="1"><tr><td>
    <pre>Canceled</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_STATUS_CANCELED() {
        return msg("GUI_SAVE_STATUS_CANCELED", "Canceled");
    }

    /**
    <table border="1"><tr><td>
    <pre>Source</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SRC_COLUMN() {
        return msg("GUI_SRC_COLUMN", "Source");
    }

    /**
    <table border="1"><tr><td>
    <pre>Err</pre>
    </td></tr></table>
    <p>Column header for the number of errors in the GUI</p>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_ERR_COLUMN() {
        return msg("GUI_ERR_COLUMN", "Err");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save As</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_AS_COLUMN() {
        return msg("GUI_SAVE_AS_COLUMN", "Save As");
    }

    /**
    <table border="1"><tr><td>
    <pre>Waiting</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_STATUS_WAITING() {
        return msg("GUI_SAVE_STATUS_WAITING", "Waiting");
    }

    /**
    <table border="1"><tr><td>
    <pre>Message</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_MESSAGE_COLUMN() {
        return msg("GUI_MESSAGE_COLUMN", "Message");
    }

    /**
    <table border="1"><tr><td>
    <pre>Done</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_STATUS_DONE() {
        return msg("GUI_SAVE_STATUS_DONE", "Done");
    }

    /**
    <table border="1"><tr><td>
    <pre>Progress</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_PROGRESS_COLUMN() {
        return msg("GUI_PROGRESS_COLUMN", "Progress");
    }

    /**
    <table border="1"><tr><td>
    <pre>Warn</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_WARN_COLUMN() {
        return msg("GUI_WARN_COLUMN", "Warn");
    }

    /**
    <table border="1"><tr><td>
    <pre>Serialized sector count {0,number,#} does not match actual {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CdOpener.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CD_SECTOR_COUNT_MISMATCH(int serializedCount, long actualCount) {
        return msg("CD_SECTOR_COUNT_MISMATCH", "Serialized sector count {0,number,#} does not match actual {1,number,#}", serializedCount, actualCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to deserialize CD string: {0}</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDisc.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CD_DESERIALIZE_FAIL(@Nonnull String badSerializedString) {
        return msg("CD_DESERIALIZE_FAIL", "Failed to deserialize CD string: {0}", badSerializedString);
    }

    /**
    <table border="1"><tr><td>
    <pre>.iso (2048 bytes/sector) format</pre>
    </td></tr></table>
    <ul>
       <li>SectorFactory.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CD_FORMAT_2048() {
        return msg("CD_FORMAT_2048", ".iso (2048 bytes/sector) format");
    }

    /**
    <table border="1"><tr><td>
    <pre>partial header (2336 bytes/sector) format</pre>
    </td></tr></table>
    <ul>
       <li>SectorFactory.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CD_FORMAT_2336() {
        return msg("CD_FORMAT_2336", "partial header (2336 bytes/sector) format");
    }

    /**
    <table border="1"><tr><td>
    <pre>BIN/CUE + Sub Channel (2448 bytes/sector) format</pre>
    </td></tr></table>
    <ul>
       <li>SectorFactory.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CD_FORMAT_2448() {
        return msg("CD_FORMAT_2448", "BIN/CUE + Sub Channel (2448 bytes/sector) format");
    }

    /**
    <table border="1"><tr><td>
    <pre>BIN/CUE (2352 bytes/sector) format</pre>
    </td></tr></table>
    <ul>
       <li>SectorFactory.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CD_FORMAT_2352() {
        return msg("CD_FORMAT_2352", "BIN/CUE (2352 bytes/sector) format");
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to read at least 1 entire sector.</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage FAILED_TO_READ_1_SECTOR() {
        return msg("FAILED_TO_READ_1_SECTOR", "Failed to read at least 1 entire sector.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Empty serialized string</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage EMPTY_SERIALIZED_STRING() {
        return msg("EMPTY_SERIALIZED_STRING", "Empty serialized string");
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to convert serialized field to int: {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage SERIALIZATION_FAILED_TO_CONVERT_TO_INT(@Nonnull String badNumber) {
        return msg("SERIALIZATION_FAILED_TO_CONVERT_TO_INT", "Failed to convert serialized field to int: {0}", badNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to convert serialized field to long: {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage SERIALIZATION_FAILED_TO_CONVERT_TO_LONG(@Nonnull String badNumber) {
        return msg("SERIALIZATION_FAILED_TO_CONVERT_TO_LONG", "Failed to convert serialized field to long: {0}", badNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to convert text to number: {0}</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage SERIALIZATION_FAILED_TO_CONVERT_TO_NUMBER(@Nonnull String badNumber) {
        return msg("SERIALIZATION_FAILED_TO_CONVERT_TO_NUMBER", "Failed to convert text to number: {0}", badNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Improperly formatted field serialization: {0}</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage SERIALIZATION_FIELD_IMPROPERLY_FORMATTED(@Nonnull String lineFromIndexFile) {
        return msg("SERIALIZATION_FIELD_IMPROPERLY_FORMATTED", "Improperly formatted field serialization: {0}", lineFromIndexFile);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to convert serialized value to range: {0}</pre>
    </td></tr></table>
    <p>This is when trying to parse a &quot;range&quot; value, which is supposed to like &quot;number-number&quot; (e.g. &quot;7-14&quot;), but in this case the &quot;range&quot; format isn't right</p>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage SERIALIZATION_FAILED_TO_CONVERT_TO_RANGE(@Nonnull String badRange) {
        return msg("SERIALIZATION_FAILED_TO_CONVERT_TO_RANGE", "Failed to convert serialized value to range: {0}", badRange);
    }

    /**
    <table border="1"><tr><td>
    <pre>Line missing vital fields {0}</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage SERIALIZATION_MISSING_REQUIRED_FIELDS(@Nonnull String lineFromIndexFile) {
        return msg("SERIALIZATION_MISSING_REQUIRED_FIELDS", "Line missing vital fields {0}", lineFromIndexFile);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} field not found.</pre>
    </td></tr></table>
    <p>A .idx line is missing a field, e.g. a video is missing &quot;dimensions&quot;</p>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage SERIALIZATION_FIELD_NOT_FOUND(@Nonnull String missingField) {
        return msg("SERIALIZATION_FIELD_NOT_FOUND", "{0} field not found.", missingField);
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc format &quot;{0}&quot; does not match the format in the index file &quot;{1}&quot;</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CD_FORMAT_MISMATCH(@Nonnull String actualFormatDescription, @Nonnull String expectedFormatDescription) {
        return msg("CD_FORMAT_MISMATCH", "Disc format \"{0}\" does not match the format in the index file \"{1}\"", actualFormatDescription, expectedFormatDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Missing proper index header.</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_HEADER_MISSING() {
        return msg("INDEX_HEADER_MISSING", "Missing proper index header.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error while indexing disc</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage INDEXING_ERROR() {
        return msg("INDEXING_ERROR", "Error while indexing disc");
    }

    /**
    <table border="1"><tr><td>
    <pre>Detected corruption in sector {0,number,#}. This may affect identification and conversion.</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_SECTOR_CORRUPTED(int sectorNumber) {
        return msg("INDEX_SECTOR_CORRUPTED", "Detected corruption in sector {0,number,#}. This may affect identification and conversion.", sectorNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Detected corruption at sector {0,number,#}. This may affect identification and conversion.</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_SECTOR_CORRUPTED_AT(int sectorNumber) {
        return msg("INDEX_SECTOR_CORRUPTED_AT", "Detected corruption at sector {0,number,#}. This may affect identification and conversion.", sectorNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Non-continuous sector header number: {0,number,#} -&gt; {1,number,#}</pre>
    </td></tr></table>
    <p>Sector header numbers should always be sequential. If some number is skipped, this error appears.</p>
    */
    public static @Nonnull ILocalizedMessage INDEX_SECTOR_HEADER_NUM_BREAK(int previousSectorNumber, int currentSectorNumber) {
        return msg("INDEX_SECTOR_HEADER_NUM_BREAK", "Non-continuous sector header number: {0,number,#} -> {1,number,#}", previousSectorNumber, currentSectorNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} is Mode 1 found among Mode 2 sectors</pre>
    </td></tr></table>
    <p>This is another case where these is inconsistencies in the sequence of sectors. &quot;Mode 1&quot; and &quot;Mode 2&quot; are sector types, and should never be mixed together.</p>
    */
    public static @Nonnull ILocalizedMessage INDEX_MODE1_AMONG_MODE2(int sectorNumber) {
        return msg("INDEX_MODE1_AMONG_MODE2", "Sector {0,number,#} is Mode 1 found among Mode 2 sectors", sectorNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to parse &quot;{0}&quot; because &quot;{1}&quot;</pre>
    </td></tr></table>
    <p>When trying to open a .idx file, if there is a line that it doesn't recognize or has an error, this is the message that is logged. So 0 is the text of the line, and 1 is the error message. For a silly example:</p>
    <p>Failed to parse &quot;bad line&quot; because &quot;That line makes no sense&quot;</p>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_PARSE_LINE_FAIL(@Nonnull String lineFromIndexFile, @Nonnull ILocalizedMessage localizedErrorMessage) {
        return msg("INDEX_PARSE_LINE_FAIL", "Failed to parse \"{0}\" because \"{1}\"", lineFromIndexFile, localizedErrorMessage);
    }

    /**
    <table border="1"><tr><td>
    <pre>Line not recognized {0}</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_UNHANDLED_LINE(@Nonnull String lineFromIndexFile) {
        return msg("INDEX_UNHANDLED_LINE", "Line not recognized {0}", lineFromIndexFile);
    }

    /**
    <table border="1"><tr><td>
    <pre>Index contains multiple lines that start with &quot;{0}&quot;</pre>
    </td></tr></table>
    <p>The .idx file has a line that described the source disc file format. The line starts with &quot;{0}&quot;. There should only be 1 line of this type in a .idx file. In this case, there are more than one, which is weird.</p>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_MULTIPLE_CD(@Nonnull String discFileIdentifier) {
        return msg("INDEX_MULTIPLE_CD", "Index contains multiple lines that start with \"{0}\"", discFileIdentifier);
    }

    /**
    <table border="1"><tr><td>
    <pre>Index is missing a line that starts with &quot;{0}&quot;, and no source file was supplied.</pre>
    </td></tr></table>
    <p>The line it's talking about is the line indicating the source disc file</p>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_NO_CD(@Nonnull String discFileIdentifier) {
        return msg("INDEX_NO_CD", "Index is missing a line that starts with \"{0}\", and no source file was supplied.", discFileIdentifier);
    }

    /**
    <table border="1"><tr><td>
    <pre>Found inconsistencies in the index, has it been modified?</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_INCONSTSTENCIES() {
        return msg("INDEX_INCONSTSTENCIES", "Found inconsistencies in the index, has it been modified?");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} / {1,number,#} {2,number,#} items found</pre>
    </td></tr></table>
    <p>Progress bar string</p>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_SECTOR_ITEM_PROGRESS(int currentSectorNumber, int totalSectorCount, int itemsFound) {
        return msg("INDEX_SECTOR_ITEM_PROGRESS", "Sector {0,number,#} / {1,number,#} {2,number,#} items found", currentSectorNumber, totalSectorCount, itemsFound);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} Lines that begin with {0} are ignored</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage INDEX_COMMENT(@Nonnull String lineCommentCharacter) {
        return msg("INDEX_COMMENT", "{0} Lines that begin with {0} are ignored", lineCommentCharacter);
    }

    /**
    <table border="1"><tr><td>
    <pre>Ignoring a silent XA audio stream that is only 1 sector long at sector {0,number,#}, channel {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndexerXaAudio.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IGNORING_SILENT_XA_SECTOR(int sectorNumber, int channelNumber) {
        return msg("IGNORING_SILENT_XA_SECTOR", "Ignoring a silent XA audio stream that is only 1 sector long at sector {0,number,#}, channel {1,number,#}", sectorNumber, channelNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Video</pre>
    </td></tr></table>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_VIDEO() {
        return msg("ITEM_TYPE_VIDEO", "Video");
    }

    /**
    <table border="1"><tr><td>
    <pre>Videos</pre>
    </td></tr></table>
    <p>Variation for &quot;Apply to all Videos&quot;</p>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_VIDEO_APPLY() {
        return msg("ITEM_TYPE_VIDEO_APPLY", "Videos");
    }

    /**
    <table border="1"><tr><td>
    <pre>File</pre>
    </td></tr></table>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_FILE() {
        return msg("ITEM_TYPE_FILE", "File");
    }

    /**
    <table border="1"><tr><td>
    <pre>Files</pre>
    </td></tr></table>
    <p>Variation for &quot;Apply to all Files&quot;</p>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_FILE_APPLY() {
        return msg("ITEM_TYPE_FILE_APPLY", "Files");
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio</pre>
    </td></tr></table>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_AUDIO() {
        return msg("ITEM_TYPE_AUDIO", "Audio");
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio</pre>
    </td></tr></table>
    <p>Variation for &quot;Apply to all Audio&quot;</p>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_AUDIO_APPLY() {
        return msg("ITEM_TYPE_AUDIO_APPLY", "Audio");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image</pre>
    </td></tr></table>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_IMAGE() {
        return msg("ITEM_TYPE_IMAGE", "Image");
    }

    /**
    <table border="1"><tr><td>
    <pre>Images</pre>
    </td></tr></table>
    <p>Variation for &quot;Apply to all Images&quot;</p>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_IMAGE_APPLY() {
        return msg("ITEM_TYPE_IMAGE_APPLY", "Images");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sound clip</pre>
    </td></tr></table>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_SOUND() {
        return msg("ITEM_TYPE_SOUND", "Sound clip");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sound clips</pre>
    </td></tr></table>
    <p>Variation for &quot;Apply to all Sound clips&quot;</p>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ITEM_TYPE_SOUND_APPLY() {
        return msg("ITEM_TYPE_SOUND_APPLY", "Sound clips");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemPacketBasedVideoStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_PACKET_BASED_VID_DETAILS(int videoWidth, int videoHeight, int frameCount, double framesPerSecond, @Nonnull java.util.Date duration) {
        return msg("GUI_PACKET_BASED_VID_DETAILS", "{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss}", videoWidth, videoHeight, frameCount, framesPerSecond, duration);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss}, {5,number,#} Hz</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemPacketBasedVideoStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_PACKET_BASED_VID_DETAILS_WITH_AUDIO(int videoWidth, int videoHeight, int frameCount, double framesPerSecond, @Nonnull java.util.Date duration, int audioHz) {
        return msg("GUI_PACKET_BASED_VID_DETAILS_WITH_AUDIO", "{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss}, {5,number,#} Hz", videoWidth, videoHeight, frameCount, framesPerSecond, duration, audioHz);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,time,m:ss}, {1,number,#} Hz Stereo</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage GUI_SQUARE_AUDIO_DETAILS(@Nonnull java.util.Date duration, int sampleRate) {
        return msg("GUI_SQUARE_AUDIO_DETAILS", "{0,time,m:ss}, {1,number,#} Hz Stereo", duration, sampleRate);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss} (or {5,number,#.###} fps = {6,time,m:ss})</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemSectorBasedVideoStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_STR_VIDEO_DETAILS_UNKNOWN_FPS(int videoWidth, int videoHeight, int frameCount, double doubleSpeedFramesPerSecond, @Nonnull java.util.Date doubleSpeedDuration, double singleSpeedFramesPerSecond, @Nonnull java.util.Date singleSpeedDuration) {
        return msg("GUI_STR_VIDEO_DETAILS_UNKNOWN_FPS", "{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss} (or {5,number,#.###} fps = {6,time,m:ss})", videoWidth, videoHeight, frameCount, doubleSpeedFramesPerSecond, doubleSpeedDuration, singleSpeedFramesPerSecond, singleSpeedDuration);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemSectorBasedVideoStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_STR_VIDEO_DETAILS(int videoWidth, int videoHeight, int frameCount, double framesPerSecond, @Nonnull java.util.Date duration) {
        return msg("GUI_STR_VIDEO_DETAILS", "{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss}", videoWidth, videoHeight, frameCount, framesPerSecond, duration);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} bytes</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_ISOFILE_DETAILS(long fileSize) {
        return msg("GUI_ISOFILE_DETAILS", "{0} bytes", fileSize);
    }

    /**
    <table border="1"><tr><td>
    <pre>-raw  save with raw {0,number,#} bytes/sectors (default is 2048 bytes/sector)</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ISOFILE_ISO_HELP(int rawBytesPerSector) {
        return msg("CMD_ISOFILE_ISO_HELP", "-raw  save with raw {0,number,#} bytes/sectors (default is 2048 bytes/sector)", rawBytesPerSector);
    }

    /**
    <table border="1"><tr><td>
    <pre>[no options available]</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ISOFILE_HELP_NO_OPTIONS() {
        return msg("CMD_ISOFILE_HELP_NO_OPTIONS", "[no options available]");
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving with raw {0,number,#} bytes/sector</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ISOFILE_SAVING_RAW(int rawBytesPerSector) {
        return msg("CMD_ISOFILE_SAVING_RAW", "Saving with raw {0,number,#} bytes/sector", rawBytesPerSector);
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving with normal 2048 bytes/sector</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ISOFILE_SAVING_2048() {
        return msg("CMD_ISOFILE_SAVING_2048", "Saving with normal 2048 bytes/sector");
    }

    /**
    <table border="1"><tr><td>
    <pre>Normal 2048 bytes/sector</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_ISOFILE_SAVE_2048() {
        return msg("GUI_ISOFILE_SAVE_2048", "Normal 2048 bytes/sector");
    }

    /**
    <table border="1"><tr><td>
    <pre>Raw</pre>
    </td></tr></table>
    <p>This is like the &quot;Raw&quot; below, but without knowing the bytes/sector</p>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_ISOFILE_SAVE_RAW() {
        return msg("GUI_ISOFILE_SAVE_RAW", "Raw");
    }

    /**
    <table border="1"><tr><td>
    <pre>Raw {0,number,#} bytes/sector</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_ISOFILE_SAVE_RAW_SIZE(int rawSectorSize) {
        return msg("GUI_ISOFILE_SAVE_RAW_SIZE", "Raw {0,number,#} bytes/sector", rawSectorSize);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} is not completely in the bounds of the CD/file, extracting it will cause errors.</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
       <li>DiscIndexerISO9660.java</li>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage NOT_CONTAINED_IN_DISC(@Nonnull String itemDescription) {
        return msg("NOT_CONTAINED_IN_DISC", "{0} is not completely in the bounds of the CD/file, extracting it will cause errors.", itemDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc file {0} information is corrupted, ignoring</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndexerISO9660.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage ISO_FILE_CORRUPTED_IGNORING(@Nonnull String fileName) {
        return msg("ISO_FILE_CORRUPTED_IGNORING", "Disc file {0} information is corrupted, ignoring", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Save as:</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
       <li>SectorBasedAudioSaverBuilderGui.java</li>
       <li>SpuSaverBuilderGui.java</li>
       <li>TimSaverBuilderGui.java</li>
       <li>VideoSaverPanel.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_AS_LABEL() {
        return msg("GUI_SAVE_AS_LABEL", "Save as:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Format: {0}</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedAudioSaverBuilder.java</li>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_AUDIO_FORMAT(@Nonnull ILocalizedMessage audioFormat) {
        return msg("CMD_AUDIO_FORMAT", "Format: {0}", audioFormat);
    }

    /**
    <table border="1"><tr><td>
    <pre>Volume: {0,number,#%}</pre>
    </td></tr></table>
    <p>Value between 0.0 and 1.0</p>
    <ul>
       <li>SectorBasedAudioSaverBuilder.java</li>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VOLUME_PERCENT(double volumeLevelPercent) {
        return msg("CMD_VOLUME_PERCENT", "Volume: {0,number,#%}", volumeLevelPercent);
    }

    /**
    <table border="1"><tr><td>
    <pre>Filename: {0}</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedAudioSaverBuilder.java</li>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_FILENAME(@Nonnull java.io.File fileName) {
        return msg("CMD_FILENAME", "Filename: {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>-audfmt,-af &lt;format&gt;</pre>
    </td></tr></table>
    <p>Note that the commands -audfmt and -af are hard-coded</p>
    <ul>
       <li>SectorBasedAudioSaverBuilder.java</li>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_AUDIO_AF() {
        return msg("CMD_AUDIO_AF", "-audfmt,-af <format>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Output audio format (default {0}). Options:</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedAudioSaverBuilder.java</li>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_AUDIO_AF_HELP(@Nonnull String defaultAudioFormatName) {
        return msg("CMD_AUDIO_AF_HELP", "Output audio format (default {0}). Options:", defaultAudioFormatName);
    }

    /**
    <table border="1"><tr><td>
    <pre>-vol &lt;0-100&gt;</pre>
    </td></tr></table>
    <p>Note that the command -vol is hard-coded</p>
    <ul>
       <li>SectorBasedAudioSaverBuilder.java</li>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_AUDIO_VOL() {
        return msg("CMD_AUDIO_VOL", "-vol <0-100>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Adjust volume (default {0,number,#}).</pre>
    </td></tr></table>
    <p>Value between 0 and 100</p>
    <ul>
       <li>SectorBasedAudioSaverBuilder.java</li>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_AUDIO_VOL_HELP(int defaultVolumeLevel) {
        return msg("CMD_AUDIO_VOL_HELP", "Adjust volume (default {0,number,#}).", defaultVolumeLevel);
    }

    /**
    <table border="1"><tr><td>
    <pre>Ignoring invalid format {0}</pre>
    </td></tr></table>
    <ul>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_IGNORING_INVALID_FORMAT(@Nonnull String invalidFormatName) {
        return msg("CMD_IGNORING_INVALID_FORMAT", "Ignoring invalid format {0}", invalidFormatName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Ignoring invalid volume {0}</pre>
    </td></tr></table>
    <ul>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_IGNORING_INVALID_VOLUME(@Nonnull String invalidVolume) {
        return msg("CMD_IGNORING_INVALID_VOLUME", "Ignoring invalid volume {0}", invalidVolume);
    }

    /**
    <table border="1"><tr><td>
    <pre>Ignoring invalid disc speed {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_IGNORING_INVALID_DISC_SPEED(@Nonnull String badDiscSpeed) {
        return msg("CMD_IGNORING_INVALID_DISC_SPEED", "Ignoring invalid disc speed {0}", badDiscSpeed);
    }

    /**
    <table border="1"><tr><td>
    <pre>Volume:</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage GUI_VOLUME_LABEL() {
        return msg("GUI_VOLUME_LABEL", "Volume:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Avi file is closed</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage AVI_FILE_IS_CLOSED() {
        return msg("AVI_FILE_IS_CLOSED", "Avi file is closed");
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing samples starting at {0,number,#} to sector {1}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
       <li>SquareAudioSectorPair.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage WRITING_SAMPLES_TO_SECTOR(long startOfSamples, @Nonnull String sectorDescription) {
        return msg("WRITING_SAMPLES_TO_SECTOR", "Writing samples starting at {0,number,#} to sector {1}", startOfSamples, sectorDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Patching sector {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemSquareAudioStream.java</li>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_PATCHING_SECTOR_NUMBER(int sectorNumber) {
        return msg("CMD_PATCHING_SECTOR_NUMBER", "Patching sector {0,number,#}", sectorNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Patching sector {0}</pre>
    </td></tr></table>
    <p>TODO Combined with next string</p>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_PATCHING_SECTOR_DESCRIPTION(@Nonnull String sectorDescription) {
        return msg("CMD_PATCHING_SECTOR_DESCRIPTION", "Patching sector {0}", sectorDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>with sector {0}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_PATCHING_WITH_SECTOR_DESCRIPTION(@Nonnull String otherSectorDescription) {
        return msg("CMD_PATCHING_WITH_SECTOR_DESCRIPTION", "with sector {0}", otherSectorDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} field has invalid value: {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
       <li>XaAudioFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FIELD_HAS_INVALID_VALUE_NUM(@Nonnull String fieldName, int badFieldNumberValue) {
        return msg("FIELD_HAS_INVALID_VALUE_NUM", "{0} field has invalid value: {1,number,#}", fieldName, badFieldNumberValue);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} field has invalid value: {1}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FIELD_HAS_INVALID_VALUE_STR(@Nonnull String fieldName, @Nonnull String badFieldStringValue) {
        return msg("FIELD_HAS_INVALID_VALUE_STR", "{0} field has invalid value: {1}", fieldName, badFieldStringValue);
    }

    /**
    <table border="1"><tr><td>
    <pre>XA audio corrupted at sector {0,number,#} affecting samples after {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>SectorXaAudioToAudioPacket.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage XA_AUDIO_CORRUPTED(int sectorNumber, long firstBadSample) {
        return msg("XA_AUDIO_CORRUPTED", "XA audio corrupted at sector {0,number,#} affecting samples after {1,number,#}", sectorNumber, firstBadSample);
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio corrupted near sector {0,number,#} affecting samples after {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CrusaderPacketToFrameAndAudio.java</li>
       <li>SquareAudioSectorPairToAudioPacket.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage SPU_ADPCM_CORRUPTED(int approximateSectorNumber, long firstBadSample) {
        return msg("SPU_ADPCM_CORRUPTED", "Audio corrupted near sector {0,number,#} affecting samples after {1,number,#}", approximateSectorNumber, firstBadSample);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,time,m:ss}, {1,number,#} Hz {2,choice,1#Mono|2#Stereo}</pre>
    </td></tr></table>
    <p>Audio channel count is 1=Mono, 2=Stereo</p>
    <ul>
       <li>DiscItemSquareAudioStream.java</li>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_AUDIO_DESCRIPTION(@Nonnull java.util.Date duration, int audioSampleRate, int audioChannelCount) {
        return msg("GUI_AUDIO_DESCRIPTION", "{0,time,m:ss}, {1,number,#} Hz {2,choice,1#Mono|2#Stereo}", duration, audioSampleRate, audioChannelCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Patching {0}</pre>
    </td></tr></table>
    <p>This line comes before the next line</p>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_PATCHING_DISC_ITEM(@Nonnull String discItemDescription) {
        return msg("CMD_PATCHING_DISC_ITEM", "Patching {0}", discItemDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>with {0}</pre>
    </td></tr></table>
    <p>This line comes after the previous line</p>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_PATCHING_WITH_DISC_ITEM(@Nonnull String otherDiscItemDescription) {
        return msg("CMD_PATCHING_WITH_DISC_ITEM", "with {0}", otherDiscItemDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>XA audio mismatch: new XA audio ({0,number,#} bits/sample, {1,number} {2,choice,1#Mono|2#Stereo} samples at {3,number}Hz) does not match existing XA audio ({4,number,#} bits/sample, {5,number} {6,choice,1#Mono|2#Stereo} samples at {7,number}Hz)</pre>
    </td></tr></table>
    <p>1 = Mono, 2 = Stereo</p>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage XA_REPLACE_FORMAT_MISMATCH(int newBitsPerSample, long newSampleCount, int newChannelCount, int newSamplesPerSecond, int existingBitsPerSample, long existingSampleCount, int existingChannelCount, int existingSamplesPerSecond) {
        return msg("XA_REPLACE_FORMAT_MISMATCH", "XA audio mismatch: new XA audio ({0,number,#} bits/sample, {1,number} {2,choice,1#Mono|2#Stereo} samples at {3,number}Hz) does not match existing XA audio ({4,number,#} bits/sample, {5,number} {6,choice,1#Mono|2#Stereo} samples at {7,number}Hz)", newBitsPerSample, newSampleCount, newChannelCount, newSamplesPerSecond, existingBitsPerSample, existingSampleCount, existingChannelCount, existingSamplesPerSecond);
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio mismatch: new audio format ({0,number,#.#}Hz {1,choice,1#Mono|2#Stereo}) does not match existing audio ({2,number,#.#}Hz {3,choice,1#Mono|2#Stereo})</pre>
    </td></tr></table>
    <p>1 = Mono, 2 = Stereo</p>
    <ul>
       <li>DiscItemSectorBasedAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage AUDIO_REPLACE_FORMAT_MISMATCH(int newSampleRate, int newChannelCount, int existingSampleRate, int existingChannelCount) {
        return msg("AUDIO_REPLACE_FORMAT_MISMATCH", "Audio mismatch: new audio format ({0,number,#.#}Hz {1,choice,1#Mono|2#Stereo}) does not match existing audio ({2,number,#.#}Hz {3,choice,1#Mono|2#Stereo})", newSampleRate, newChannelCount, existingSampleRate, existingChannelCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Input audio format is not signed little-endian 16-bit PCM with a whole number sample rate</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemSectorBasedAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage AUDIO_INVALID_FORMAT() {
        return msg("AUDIO_INVALID_FORMAT", "Input audio format is not signed little-endian 16-bit PCM with a whole number sample rate");
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacement audio sample length {0} is too small to fill existing sample length {1}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemSquareAudioStream.java</li>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_REPLACE_AUDIO_TOO_SHORT(long audioSampleLength, long existingAudioSampleLength) {
        return msg("CMD_REPLACE_AUDIO_TOO_SHORT", "Replacement audio sample length {0} is too small to fill existing sample length {1}", audioSampleLength, existingAudioSampleLength);
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacement audio sample length is larger than existing sample length {0}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemSquareAudioStream.java</li>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_REPLACE_AUDIO_TOO_LONG(long existingAudioSampleLength) {
        return msg("CMD_REPLACE_AUDIO_TOO_LONG", "Replacement audio sample length is larger than existing sample length {0}", existingAudioSampleLength);
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio file sample rate {0} does not match XA audio rate {1}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage XA_COPY_REPLACE_SAMPLE_RATE_MISMATCH(float incompatibleAudioSampleRate, int xaAudioSampleRate) {
        return msg("XA_COPY_REPLACE_SAMPLE_RATE_MISMATCH", "Audio file sample rate {0} does not match XA audio rate {1}", incompatibleAudioSampleRate, xaAudioSampleRate);
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio file is {0,choice,1#Mono|2#Stereo} and does not match XA audio {1,choice,1#Mono|2#Stereo}</pre>
    </td></tr></table>
    <p>1 = Mono, 2 = Stereo</p>
    */
    public static @Nonnull ILocalizedMessage XA_COPY_REPLACE_CHANNEL_MISMATCH(int replaceChannelCount, int sourceChannelCount) {
        return msg("XA_COPY_REPLACE_CHANNEL_MISMATCH", "Audio file is {0,choice,1#Mono|2#Stereo} and does not match XA audio {1,choice,1#Mono|2#Stereo}", replaceChannelCount, sourceChannelCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Source audio is exhausted, writing silence</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage XA_ENCODE_REPLACE_SRC_AUDIO_EXHAUSTED() {
        return msg("XA_ENCODE_REPLACE_SRC_AUDIO_EXHAUSTED", "Source audio is exhausted, writing silence");
    }

    /**
    <table border="1"><tr><td>
    <pre>End of source XA, stopping</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage XA_COPY_REPLACE_SRC_XA_EXHAUSTED() {
        return msg("XA_COPY_REPLACE_SRC_XA_EXHAUSTED", "End of source XA, stopping");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame number {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameLookup.java</li>
       <li>FrameNumberNumber.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_INVALID(@Nonnull String badFrameNumberString) {
        return msg("FRAME_NUM_INVALID", "Invalid frame number {0}", badFrameNumberString);
    }

    /**
    <table border="1"><tr><td>
    <pre>Index</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumber.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_FORMAT_INDEX() {
        return msg("FRAME_NUM_FORMAT_INDEX", "Index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumber.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_FORMAT_SECTOR() {
        return msg("FRAME_NUM_FORMAT_SECTOR", "Sector");
    }

    /**
    <table border="1"><tr><td>
    <pre>Header</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumber.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_FORMAT_HEADER() {
        return msg("FRAME_NUM_FORMAT_HEADER", "Header");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumber.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_FORMATTER_SECTOR(@Nonnull String formattedSectorNumber) {
        return msg("FRAME_NUM_FORMATTER_SECTOR", "Sector {0}", formattedSectorNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumber.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_FORMATTER_FRAME(@Nonnull String formattedFrameNumber) {
        return msg("FRAME_NUM_FORMATTER_FRAME", "Frame {0}", formattedFrameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame number format {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_FORMAT_INVALID(@Nonnull String badFrameNumberFormat) {
        return msg("FRAME_NUM_FORMAT_INVALID", "Invalid frame number format {0}", badFrameNumberFormat);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame {0,number,#} missing frame number information</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_MISSING_FRAME_NUMBER_HEADER(int frameNumber) {
        return msg("FRAME_MISSING_FRAME_NUMBER_HEADER", "Frame {0,number,#} missing frame number information", frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Found an unexpected number of frames, the frames may be saved in an inconsistent order.</pre>
    </td></tr></table>
    <p>When the file is indexed, the number of frames in a video are saved. When saving as a sequence of imgaes, that number is necessary to properly format the file names with extra zeroes. e.g. a video with 99 frames needs all frame numbers to be 2 digits, but a video with 100 frames, needs frame numbers to be 3 digits. If the index says there are 99 frames, but there are actually 100, then the file name &quot;videoframe[100].png&quot; will come before &quot;videoframe[99].png&quot;.</p>
    <ul>
       <li>HeaderFrameNumber.java</li>
       <li>IndexSectorFrameNumber.java</li>
       <li>VideoFileNameFormatter.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAMES_UNEXPECTED_NUMBER() {
        return msg("FRAMES_UNEXPECTED_NUMBER", "Found an unexpected number of frames, the frames may be saved in an inconsistent order.");
    }

    /**
    <table border="1"><tr><td>
    <pre>High quality (slower)</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage QUALITY_HIGH_DESCRIPTION() {
        return msg("QUALITY_HIGH_DESCRIPTION", "High quality (slower)");
    }

    /**
    <table border="1"><tr><td>
    <pre>high</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage QUALITY_HIGH_COMMAND() {
        return msg("QUALITY_HIGH_COMMAND", "high");
    }

    /**
    <table border="1"><tr><td>
    <pre>Fast (lower quality)</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage QUALITY_FAST_DESCRIPTION() {
        return msg("QUALITY_FAST_DESCRIPTION", "Fast (lower quality)");
    }

    /**
    <table border="1"><tr><td>
    <pre>low</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage QUALITY_FAST_COMMAND() {
        return msg("QUALITY_FAST_COMMAND", "low");
    }

    /**
    <table border="1"><tr><td>
    <pre>Emulate PSX (low) quality</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage QUALITY_PSX_DESCRIPTION() {
        return msg("QUALITY_PSX_DESCRIPTION", "Emulate PSX (low) quality");
    }

    /**
    <table border="1"><tr><td>
    <pre>psx</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage QUALITY_PSX_COMMAND() {
        return msg("QUALITY_PSX_COMMAND", "psx");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bicubic</pre>
    </td></tr></table>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_BICUBIC_DESCRIPTION() {
        return msg("CHROMA_UPSAMPLE_BICUBIC_DESCRIPTION", "Bicubic");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bicubic</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_BICUBIC_CMDLINE() {
        return msg("CHROMA_UPSAMPLE_BICUBIC_CMDLINE", "Bicubic");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bell</pre>
    </td></tr></table>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_BELL_DESCRIPTION() {
        return msg("CHROMA_UPSAMPLE_BELL_DESCRIPTION", "Bell");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bell</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_BELL_CMDLINE() {
        return msg("CHROMA_UPSAMPLE_BELL_CMDLINE", "Bell");
    }

    /**
    <table border="1"><tr><td>
    <pre>Nearest Neighbor</pre>
    </td></tr></table>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_NEAR_NEIGHBOR_DESCRIPTION() {
        return msg("CHROMA_UPSAMPLE_NEAR_NEIGHBOR_DESCRIPTION", "Nearest Neighbor");
    }

    /**
    <table border="1"><tr><td>
    <pre>NearestNeighbor</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_NEAR_NEIGHBOR_CMDLINE() {
        return msg("CHROMA_UPSAMPLE_NEAR_NEIGHBOR_CMDLINE", "NearestNeighbor");
    }

    /**
    <table border="1"><tr><td>
    <pre>Lanczos3</pre>
    </td></tr></table>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_LANCZOS3_DESCRIPTION() {
        return msg("CHROMA_UPSAMPLE_LANCZOS3_DESCRIPTION", "Lanczos3");
    }

    /**
    <table border="1"><tr><td>
    <pre>Lanczos3</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_LANCZOS3_CMDLINE() {
        return msg("CHROMA_UPSAMPLE_LANCZOS3_CMDLINE", "Lanczos3");
    }

    /**
    <table border="1"><tr><td>
    <pre>Mitchell</pre>
    </td></tr></table>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_MITCHELL_DESCRIPTION() {
        return msg("CHROMA_UPSAMPLE_MITCHELL_DESCRIPTION", "Mitchell");
    }

    /**
    <table border="1"><tr><td>
    <pre>Mitchell</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_MITCHELL_CMDLINE() {
        return msg("CHROMA_UPSAMPLE_MITCHELL_CMDLINE", "Mitchell");
    }

    /**
    <table border="1"><tr><td>
    <pre>Hermite</pre>
    </td></tr></table>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_HERMITE_DESCRIPTION() {
        return msg("CHROMA_UPSAMPLE_HERMITE_DESCRIPTION", "Hermite");
    }

    /**
    <table border="1"><tr><td>
    <pre>Hermite</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_HERMITE_CMDLINE() {
        return msg("CHROMA_UPSAMPLE_HERMITE_CMDLINE", "Hermite");
    }

    /**
    <table border="1"><tr><td>
    <pre>BSpline</pre>
    </td></tr></table>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_BSPLINE_DESCRIPTION() {
        return msg("CHROMA_UPSAMPLE_BSPLINE_DESCRIPTION", "BSpline");
    }

    /**
    <table border="1"><tr><td>
    <pre>BSpline</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_BSPLINE_CMDLINE() {
        return msg("CHROMA_UPSAMPLE_BSPLINE_CMDLINE", "BSpline");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bilinear</pre>
    </td></tr></table>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_BILINEAR_DESCRIPTION() {
        return msg("CHROMA_UPSAMPLE_BILINEAR_DESCRIPTION", "Bilinear");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bilinear</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_BILINEAR_CMDLINE() {
        return msg("CHROMA_UPSAMPLE_BILINEAR_CMDLINE", "Bilinear");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} ({1})</pre>
    </td></tr></table>
    <p>See CHROMA_UPSAMPLE_*_DESCRIPTION and CHROMA_UPSAMPLE_*_CMDLINE</p>
    <ul>
       <li>ChromaUpsample.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CHROMA_UPSAMPLE_CMDLINE_HELP(@Nonnull ILocalizedMessage commandLineId, @Nonnull ILocalizedMessage interplationName) {
        return msg("CHROMA_UPSAMPLE_CMDLINE_HELP", "{0} ({1})", commandLineId, interplationName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacement frame file {0} dimensions {1,number,#}x{2,number,#} do not match frame to replace dimensions {3,number,#}x{4,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrameFull.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_FRAME_DIMENSIONS_MISMATCH(@Nonnull String imageFile, int sourceWidth, int sourceHeight, int replaceWidth, int replaceHeight) {
        return msg("REPLACE_FRAME_DIMENSIONS_MISMATCH", "Replacement frame file {0} dimensions {1,number,#}x{2,number,#} do not match frame to replace dimensions {3,number,#}x{4,number,#}", imageFile, sourceWidth, sourceHeight, replaceWidth, replaceHeight);
    }

    /**
    <table border="1"><tr><td>
    <pre>Bitstream frame file {0} type does not match existing frame type</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrameFull.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_BITSTREAM_MISMATCH(@Nonnull java.io.File bitstreamFile) {
        return msg("REPLACE_BITSTREAM_MISMATCH", "Bitstream frame file {0} type does not match existing frame type", bitstreamFile);
    }

    /**
    <table border="1"><tr><td>
    <pre>Incompatible mdec file {0} for frame {1}</pre>
    </td></tr></table>
    <p>&quot;mdec&quot; is a file type</p>
    <ul>
       <li>ReplaceFrameFull.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_INCOMPATIBLE_MDEC(@Nonnull String mdecFileName, @Nonnull String frameNumber) {
        return msg("REPLACE_INCOMPATIBLE_MDEC", "Incompatible mdec file {0} for frame {1}", mdecFileName, frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Incomplete mdec file {0} for frame {1}</pre>
    </td></tr></table>
    <p>&quot;mdec&quot; is a file type</p>
    <ul>
       <li>ReplaceFrameFull.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_INCOMPLETE_MDEC(@Nonnull String mdecFileName, @Nonnull String frameNumber) {
        return msg("REPLACE_INCOMPLETE_MDEC", "Incomplete mdec file {0} for frame {1}", mdecFileName, frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Corrupted mdec file {0} for frame {1}</pre>
    </td></tr></table>
    <p>&quot;mdec&quot; is a file type</p>
    <ul>
       <li>ReplaceFrameFull.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_CORRUPTED_MDEC(@Nonnull String mdecFileName, @Nonnull String frameNumber) {
        return msg("REPLACE_CORRUPTED_MDEC", "Corrupted mdec file {0} for frame {1}", mdecFileName, frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid replacement image format {0}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrameFull.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_INVALID_IMAGE_FORMAT(@Nonnull String badFormatName) {
        return msg("REPLACE_INVALID_IMAGE_FORMAT", "Invalid replacement image format {0}", badFormatName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to read {0} as an image. Did you forget ''format'' option in the XML?</pre>
    </td></tr></table>
    <p>&quot;format&quot; is an XML tag so should not be translated</p>
    <ul>
       <li>ReplaceFrameFull.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_FILE_NOT_JAVA_IMAGE(@Nonnull java.io.File fileName) {
        return msg("REPLACE_FILE_NOT_JAVA_IMAGE", "Unable to read {0} as an image. Did you forget ''format'' option in the XML?", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to identify frame type</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_UNABLE_TO_IDENTIFY_FRAME_TYPE() {
        return msg("CMD_UNABLE_TO_IDENTIFY_FRAME_TYPE", "Unable to identify frame type");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to compress frame {0} small enough to fit in {1,number,#} bytes</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrameFull.java</li>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_UNABLE_TO_COMPRESS_FRAME_SMALL_ENOUGH(@Nonnull String frameNumber, int maxSize) {
        return msg("CMD_UNABLE_TO_COMPRESS_FRAME_SMALL_ENOUGH", "Unable to compress frame {0} small enough to fit in {1,number,#} bytes", frameNumber, maxSize);
    }

    /**
    <table border="1"><tr><td>
    <pre>No differences found in frame {0}, skipping.</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_NO_DIFFERENCE_SKIPPING(@Nonnull String frameNumber) {
        return msg("CMD_NO_DIFFERENCE_SKIPPING", "No differences found in frame {0}, skipping.", frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Found {0,number,#} different macroblocks (16x16) out of {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_REPLACE_FOUND_DIFFERENT_MACRO_BLOCKS(int differenceCount, int total) {
        return msg("CMD_REPLACE_FOUND_DIFFERENT_MACRO_BLOCKS", "Found {0,number,#} different macroblocks (16x16) out of {1,number,#}", differenceCount, total);
    }

    /**
    <table border="1"><tr><td>
    <pre>Warning: Entire frame is different.</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_ENTIRE_FRAME_DIFFERENT() {
        return msg("CMD_ENTIRE_FRAME_DIFFERENT", "Warning: Entire frame is different.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to load {0} as image</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_UNABLE_READ_IMAGE(@Nonnull String fileName) {
        return msg("REPLACE_UNABLE_READ_IMAGE", "Unable to load {0} as image", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacement frame dimensions {0,number,#}x{1,number,#} are smaller than source frame {2,number,#}x{3,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_FRAME_DIMENSIONS_TOO_SMALL(int newWidth, int newHeight, int existingWidth, int existingHeight) {
        return msg("REPLACE_FRAME_DIMENSIONS_TOO_SMALL", "Replacement frame dimensions {0,number,#}x{1,number,#} are smaller than source frame {2,number,#}x{3,number,#}", newWidth, newHeight, existingWidth, existingHeight);
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacing frame {0} with {1}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrames.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_REPLACING_FRAME_WITH_FILE(@Nonnull String frameNumber, @Nonnull java.io.File fileName) {
        return msg("CMD_REPLACING_FRAME_WITH_FILE", "Replacing frame {0} with {1}", frameNumber, fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error with frame replacement xml: {0}</pre>
    </td></tr></table>
    <p>Unfortunately the description of the error in the xml file is only available in English</p>
    <ul>
       <li>ReplaceFrames.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_FRAME_XML_ERROR(@Nonnull String xmlErrorInEnglish) {
        return msg("REPLACE_FRAME_XML_ERROR", "Error with frame replacement xml: {0}", xmlErrorInEnglish);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid size &quot;{0}&quot;, expected &quot;{1}&quot;</pre>
    </td></tr></table>
    <p>Currently the only valid value is &quot;original non-zero&quot; but may allow any number in the future</p>
    <ul>
       <li>ReplaceFrameFull.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_XML_INVALID_SIZE_LIMIT(@Nonnull String invalidSize, @Nonnull String expectedSize) {
        return msg("REPLACE_XML_INVALID_SIZE_LIMIT", "Invalid size \"{0}\", expected \"{1}\"", invalidSize, expectedSize);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid root node {0}</pre>
    </td></tr></table>
    <p>The root xml tag should be &quot;str-replace&quot;, but it's &quot;{0}&quot;</p>
    <ul>
       <li>ReplaceFrames.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_REPLACE_XML_INVALID_ROOT_NODE(@Nonnull String xmlRootNodeName) {
        return msg("CMD_REPLACE_XML_INVALID_ROOT_NODE", "Invalid root node {0}", xmlRootNodeName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid version {0}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrames.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_REPLACE_XML_INVALID_VERSION(@Nonnull String versionNumber) {
        return msg("CMD_REPLACE_XML_INVALID_VERSION", "Invalid version {0}", versionNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame type is not STRv2 or STRv3</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage REPLACE_FRAME_TYPE_NOT_V2_V3() {
        return msg("REPLACE_FRAME_TYPE_NOT_V2_V3", "Frame type is not STRv2 or STRv3");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame type is not STRv2</pre>
    </td></tr></table>
    <ul>
       <li>SectorFF9.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage REPLACE_FRAME_TYPE_NOT_V2() {
        return msg("REPLACE_FRAME_TYPE_NOT_V2", "Frame type is not STRv2");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame type is not iki</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage REPLACE_FRAME_TYPE_NOT_IKI() {
        return msg("REPLACE_FRAME_TYPE_NOT_IKI", "Frame type is not iki");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not Iki format</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage FRAME_NOT_IKI() {
        return msg("FRAME_NOT_IKI", "Frame is not Iki format");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not Lain format</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage FRAME_NOT_LAIN() {
        return msg("FRAME_NOT_LAIN", "Frame is not Lain format");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not STRv1 format</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage FRAME_NOT_STRV1() {
        return msg("FRAME_NOT_STRV1", "Frame is not STRv1 format");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not STRv2 format</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage FRAME_NOT_STRV2() {
        return msg("FRAME_NOT_STRV2", "Frame is not STRv2 format");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not STRv3 format</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage FRAME_NOT_STRV3() {
        return msg("FRAME_NOT_STRV3", "Frame is not STRv3 format");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not {0} format</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage FRAME_IS_NOT_BITSTREAM_FORMAT(@Nonnull String frameFormatName) {
        return msg("FRAME_IS_NOT_BITSTREAM_FORMAT", "Frame is not {0} format", frameFormatName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Iki frame dimensions do not match sector dimensions: {0,number,#}x{1,number,#} != {2,number,#}x{3,number,#}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage REPLACE_FRAME_IKI_DIMENSIONS_MISMATCH(int sourceWidth, int sourceHeight, int replaceWidth, int replaceHeight) {
        return msg("REPLACE_FRAME_IKI_DIMENSIONS_MISMATCH", "Iki frame dimensions do not match sector dimensions: {0,number,#}x{1,number,#} != {2,number,#}x{3,number,#}", sourceWidth, sourceHeight, replaceWidth, replaceHeight);
    }

    /**
    <table border="1"><tr><td>
    <pre>Incompatible frame data for Lain</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage REPLACE_FRAME_TYPE_NOT_LAIN() {
        return msg("REPLACE_FRAME_TYPE_NOT_LAIN", "Incompatible frame data for Lain");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unexpected end of audio data</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage UNEXPECTED_END_OF_AUDIO() {
        return msg("UNEXPECTED_END_OF_AUDIO", "Unexpected end of audio data");
    }

    /**
    <table border="1"><tr><td>
    <pre>Trying to reduce quantization scale of macroblock ({0,number,#},{1,number,#}) to {2,number,#}</pre>
    </td></tr></table>
    <p>Overly technical message when trying to replace a video frame. I don't really expect anyone to understand what it means. Probably should repalce it with something more generic.</p>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IKI_REDUCING_QSCALE_OF_MB_TO_VAL(int macroBlockX, int macroBlockY, int quantizationScale) {
        return msg("IKI_REDUCING_QSCALE_OF_MB_TO_VAL", "Trying to reduce quantization scale of macroblock ({0,number,#},{1,number,#}) to {2,number,#}", macroBlockX, macroBlockY, quantizationScale);
    }

    /**
    <table border="1"><tr><td>
    <pre>New frame {0} replacement size {1,number,#} fits within the existing available size {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
       <li>BitStreamUncompressor_Lain.java</li>
       <li>CommonBitStreamCompressing.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage NEW_FRAME_FITS(@Nonnull String frameNumber, int demuxSize, int sourceSize) {
        return msg("NEW_FRAME_FITS", "New frame {0} replacement size {1,number,#} fits within the existing available size {2,number,#}", frameNumber, demuxSize, sourceSize);
    }

    /**
    <table border="1"><tr><td>
    <pre>Trying quantization scale {0,number,#} (smaller value is better quality but needs more space)</pre>
    </td></tr></table>
    <ul>
       <li>CommonBitStreamCompressing.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage TRYING_QSCALE(int quantizationScale) {
        return msg("TRYING_QSCALE", "Trying quantization scale {0,number,#} (smaller value is better quality but needs more space)", quantizationScale);
    }

    /**
    <table border="1"><tr><td>
    <pre>End of stream</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage END_OF_STREAM() {
        return msg("END_OF_STREAM", "End of stream");
    }

    /**
    <table border="1"><tr><td>
    <pre>Trying to compress with luma quantization scale {0,number,#} and chroma quantization scale {1,number,#}</pre>
    </td></tr></table>
    <p>Overly technical message when trying to replace a video frame. I don't really expect anyone to understand what it means. Probably should repalce it with something more generic.</p>
    <ul>
       <li>BitStreamUncompressor_Lain.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage TRYING_LUMA_CHROMA(int lumaQuantizationScale, int chromaQuantizationScale) {
        return msg("TRYING_LUMA_CHROMA", "Trying to compress with luma quantization scale {0,number,#} and chroma quantization scale {1,number,#}", lumaQuantizationScale, chromaQuantizationScale);
    }

    /**
    <table border="1"><tr><td>
    <pre>New frame {0} replacement size {1,number,#} does not fit in the existing available size {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
       <li>BitStreamUncompressor_Lain.java</li>
       <li>CommonBitStreamCompressing.java</li>
       <li>ReplaceFrameFull.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage NEW_FRAME_DOES_NOT_FIT(@Nonnull String frameNumber, int newFrameSize, int sourceFrameSize) {
        return msg("NEW_FRAME_DOES_NOT_FIT", "New frame {0} replacement size {1,number,#} does not fit in the existing available size {2,number,#}", frameNumber, newFrameSize, sourceFrameSize);
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacement image for frame {0} is too detailed to compress</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Lain.java</li>
       <li>CommonBitStreamCompressing.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage COMPRESS_TOO_MUCH_ENERGY(@Nonnull String frameNumber) {
        return msg("COMPRESS_TOO_MUCH_ENERGY", "Replacement image for frame {0} is too detailed to compress", frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Inconsistent width {0,number,#} != {1,number,#}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage INCONSISTENT_WIDTH(int currentWidth, int newWidth) {
        return msg("INCONSISTENT_WIDTH", "Inconsistent width {0,number,#} != {1,number,#}", currentWidth, newWidth);
    }

    /**
    <table border="1"><tr><td>
    <pre>Inconsistent height {0,number,#} != {1,number,#}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage INCONSISTENT_HEIGHT(int currentHeight, int newHeight) {
        return msg("INCONSISTENT_HEIGHT", "Inconsistent height {0,number,#} != {1,number,#}", currentHeight, newHeight);
    }

    /**
    <table border="1"><tr><td>
    <pre>Embedded audio {0,number,#} Hz</pre>
    </td></tr></table>
    <ul>
       <li>PacketBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_EMBEDDED_PACKET_BASED_AUDIO_HZ(int audioSampleRate) {
        return msg("CMD_EMBEDDED_PACKET_BASED_AUDIO_HZ", "Embedded audio {0,number,#} Hz", audioSampleRate);
    }

    /**
    <table border="1"><tr><td>
    <pre>Policenauts data is corrupted</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndexerPolicenauts.java</li>
       <li>SectorClaimToPolicenauts.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage POLICENAUTS_DATA_CORRUPTION() {
        return msg("POLICENAUTS_DATA_CORRUPTION", "Policenauts data is corrupted");
    }

    /**
    <table border="1"><tr><td>
    <pre>RoadRash data corruption</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage ROADRASH_DATA_CORRUPTION() {
        return msg("ROADRASH_DATA_CORRUPTION", "RoadRash data corruption");
    }

    /**
    <table border="1"><tr><td>
    <pre>Electronic Arts video data is corrupted</pre>
    </td></tr></table>
    <p>Several games made by Electronic Arts used the same video format</p>
    <ul>
       <li>SectorClaimToEAVideo.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage EA_VIDEO_DATA_CORRUPTION() {
        return msg("EA_VIDEO_DATA_CORRUPTION", "Electronic Arts video data is corrupted");
    }

    /**
    <table border="1"><tr><td>
    <pre>N-Gauge Unten Kibun Game - Gatan Goton data is corrupted</pre>
    </td></tr></table>
    <p>&quot;N-Gauge Unten Kibun Game - Gatan Goton&quot; is the name of a game</p>
    <ul>
       <li>NGaugeSectorToFrame.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage N_GAUGE_DATA_CORRUPTION() {
        return msg("N_GAUGE_DATA_CORRUPTION", "N-Gauge Unten Kibun Game - Gatan Goton data is corrupted");
    }

    /**
    <table border="1"><tr><td>
    <pre>Crusader: No Remorse data is corrupted</pre>
    </td></tr></table>
    <p>&quot;Crusader: No Remorse&quot; is the name of a game</p>
    <ul>
       <li>DiscIndexerCrusader.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CRUSADER_DATA_CORRUPTED() {
        return msg("CRUSADER_DATA_CORRUPTED", "Crusader: No Remorse data is corrupted");
    }

    /**
    <table border="1"><tr><td>
    <pre>Crusader: No Remorse video is corrupted</pre>
    </td></tr></table>
    <p>&quot;Crusader: No Remorse&quot; is the name of a game</p>
    */
    public static @Nonnull ILocalizedMessage CRUSADER_VIDEO_CORRUPTED() {
        return msg("CRUSADER_VIDEO_CORRUPTED", "Crusader: No Remorse video is corrupted");
    }

    /**
    <table border="1"><tr><td>
    <pre>Crusader: No Remorse audio is corrupted</pre>
    </td></tr></table>
    <p>&quot;Crusader: No Remorse&quot; is the name of a game</p>
    */
    public static @Nonnull ILocalizedMessage CRUSADER_AUDIO_CORRUPTED() {
        return msg("CRUSADER_AUDIO_CORRUPTED", "Crusader: No Remorse audio is corrupted");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame {0} chunk {1,number,#} missing.</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage MISSING_CHUNK(@Nonnull String frameNumber, int chunkNumber) {
        return msg("MISSING_CHUNK", "Frame {0} chunk {1,number,#} missing.", frameNumber, chunkNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame in sectors {0,number,#}-{1,number,#} is missing chunk {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedFrameBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage MISSING_CHUNK_FRAME_IN_SECTORS(int frameStartSector, int frameEndSector, int chunkNumber) {
        return msg("MISSING_CHUNK_FRAME_IN_SECTORS", "Frame in sectors {0,number,#}-{1,number,#} is missing chunk {2,number,#}", frameStartSector, frameEndSector, chunkNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Trying to replace an existing corrupted frame</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedFrameReplace.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_FRAME_TO_REPLACE_MISSING_CHUNKS() {
        return msg("CMD_FRAME_TO_REPLACE_MISSING_CHUNKS", "Trying to replace an existing corrupted frame");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error with frame {0}: Frame is corrupted</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
       <li>ReplaceFrameFull.java</li>
       <li>ReplaceFramePartial.java</li>
       <li>SectorBasedFrameBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_CORRUPTED(@Nonnull String frameNumber) {
        return msg("FRAME_NUM_CORRUPTED", "Error with frame {0}: Frame is corrupted", frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error: Frame is corrupted</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_CORRUPTED() {
        return msg("FRAME_CORRUPTED", "Error: Frame is corrupted");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error with frame {0}: Frame is incomplete</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrameFull.java</li>
       <li>ReplaceFramePartial.java</li>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_INCOMPLETE(@Nonnull String frameNumber) {
        return msg("FRAME_NUM_INCOMPLETE", "Error with frame {0}: Frame is incomplete", frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error: Frame is incomplete</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_INCOMPLETE() {
        return msg("FRAME_INCOMPLETE", "Error: Frame is incomplete");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error with frame {0}: Unable to determine frame type.</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrameFull.java</li>
       <li>ReplaceFramePartial.java</li>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(@Nonnull String frameNumber) {
        return msg("UNABLE_TO_DETERMINE_FRAME_TYPE_FRM", "Error with frame {0}: Unable to determine frame type.", frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error: Unable to determine frame type.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage UNABLE_TO_DETERMINE_FRAME_TYPE() {
        return msg("UNABLE_TO_DETERMINE_FRAME_TYPE", "Error: Unable to determine frame type.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Presentation time of frame {0} in video file will be {1,number,#} {1,choice,1#frame|2#frames} ahead of original timing</pre>
    </td></tr></table>
    <p>Message when saving an .avi and the frames are slightly out of sync with the audio</p>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_NUM_AHEAD_OF_READING(@Nonnull String frameNumber, int frameCount) {
        return msg("FRAME_NUM_AHEAD_OF_READING", "Presentation time of frame {0} in video file will be {1,number,#} {1,choice,1#frame|2#frames} ahead of original timing", frameNumber, frameCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Presentation time of frame in video file will be {0,number,#} {0,choice,1#frame|2#frames} ahead of original timing</pre>
    </td></tr></table>
    <p>Message when saving an .avi and the frames are slightly out of sync with the audio</p>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_AHEAD_OF_READING(int frameCount) {
        return msg("FRAME_AHEAD_OF_READING", "Presentation time of frame in video file will be {0,number,#} {0,choice,1#frame|2#frames} ahead of original timing", frameCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to write frame file {0} for frame {1}</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_FILE_WRITE_UNABLE(@Nonnull String fileName, @Nonnull String frameNumber) {
        return msg("FRAME_FILE_WRITE_UNABLE", "Unable to write frame file {0} for frame {1}", fileName, frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Video format identified as {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage VIDEO_FMT_IDENTIFIED(@Nonnull String formatIdentifier) {
        return msg("VIDEO_FMT_IDENTIFIED", "Video format identified as {0}", formatIdentifier);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error uncompressing frame {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage FRAME_UNCOMPRESS_ERR(@Nonnull String frameNumber) {
        return msg("FRAME_UNCOMPRESS_ERR", "Error uncompressing frame {0}", frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>The simple jPSXdec JPEG encoder cannot convert frame {0}. Please save in a different format.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage JPEG_ENCODER_FRAME_FAIL(@Nonnull String frameNumber) {
        return msg("JPEG_ENCODER_FRAME_FAIL", "The simple jPSXdec JPEG encoder cannot convert frame {0}. Please save in a different format.", frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>The simple jPSXdec JPEG encoder cannot convert frame. Please save in a different format.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage JPEG_ENCODER_FRAME_FAIL_NO_FRAME() {
        return msg("JPEG_ENCODER_FRAME_FAIL_NO_FRAME", "The simple jPSXdec JPEG encoder cannot convert frame. Please save in a different format.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing {0,number,#} blank {0,choice,1#frame|2#frames} to align audio/video playback.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage WRITING_BLANK_FRAMES_TO_ALIGN_AV(int frameCount) {
        return msg("WRITING_BLANK_FRAMES_TO_ALIGN_AV", "Writing {0,number,#} blank {0,choice,1#frame|2#frames} to align audio/video playback.", frameCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing {0,number,#} duplicate {0,choice,1#frame|2#frames} to align audio/video playback.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage WRITING_DUP_FRAMES_TO_ALIGN_AV(int frameCount) {
        return msg("WRITING_DUP_FRAMES_TO_ALIGN_AV", "Writing {0,number,#} duplicate {0,choice,1#frame|2#frames} to align audio/video playback.", frameCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error writing file {0} for frame {1}</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage FRAME_WRITE_ERR(@Nonnull java.io.File fileName, @Nonnull String frameNumber) {
        return msg("FRAME_WRITE_ERR", "Error writing file {0} for frame {1}", fileName, frameNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing {0,number,#} samples of silence to align audio/video playback.</pre>
    </td></tr></table>
    <p>TODO combine with next line</p>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage WRITING_SILECE_TO_SYNC_AV(long sampleCount) {
        return msg("WRITING_SILECE_TO_SYNC_AV", "Writing {0,number,#} samples of silence to align audio/video playback.", sampleCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Adding {0,number,#} samples to keep audio in sync.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage WRITING_SILENCE_TO_KEEP_AV_SYNCED(long sampleCount) {
        return msg("WRITING_SILENCE_TO_KEEP_AV_SYNCED", "Adding {0,number,#} samples to keep audio in sync.", sampleCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: png</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_PNG_DESCRIPTION() {
        return msg("VID_IMG_SEQ_PNG_DESCRIPTION", "Image sequence: png");
    }

    /**
    <table border="1"><tr><td>
    <pre>png</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_PNG_COMMAND() {
        return msg("VID_IMG_SEQ_PNG_COMMAND", "png");
    }

    /**
    <table border="1"><tr><td>
    <pre>AVI: Compressed (MJPG)</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_AVI_MJPG_DESCRIPTION() {
        return msg("VID_AVI_MJPG_DESCRIPTION", "AVI: Compressed (MJPG)");
    }

    /**
    <table border="1"><tr><td>
    <pre>avi:mjpg</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    */
    public static @Nonnull ILocalizedMessage VID_AVI_MJPG_COMMAND() {
        return msg("VID_AVI_MJPG_COMMAND", "avi:mjpg");
    }

    /**
    <table border="1"><tr><td>
    <pre>AVI: Uncompressed RGB</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_AVI_RGB_DESCRIPTION() {
        return msg("VID_AVI_RGB_DESCRIPTION", "AVI: Uncompressed RGB");
    }

    /**
    <table border="1"><tr><td>
    <pre>avi:rgb</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    */
    public static @Nonnull ILocalizedMessage VID_AVI_RGB_COMMAND() {
        return msg("VID_AVI_RGB_COMMAND", "avi:rgb");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: bmp</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_BMP_DESCRIPTION() {
        return msg("VID_IMG_SEQ_BMP_DESCRIPTION", "Image sequence: bmp");
    }

    /**
    <table border="1"><tr><td>
    <pre>bmp</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_BMP_COMMAND() {
        return msg("VID_IMG_SEQ_BMP_COMMAND", "bmp");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: tiff</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_TIFF_DESCRIPTION() {
        return msg("VID_IMG_SEQ_TIFF_DESCRIPTION", "Image sequence: tiff");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: mdec</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_MDEC_DESCRIPTION() {
        return msg("VID_IMG_SEQ_MDEC_DESCRIPTION", "Image sequence: mdec");
    }

    /**
    <table border="1"><tr><td>
    <pre>mdec</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_MDEC_COMMAND() {
        return msg("VID_IMG_SEQ_MDEC_COMMAND", "mdec");
    }

    /**
    <table border="1"><tr><td>
    <pre>AVI: YUV with [0-255] range</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_AVI_JYUV_DESCRIPTION() {
        return msg("VID_AVI_JYUV_DESCRIPTION", "AVI: YUV with [0-255] range");
    }

    /**
    <table border="1"><tr><td>
    <pre>avi:jyuv</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    */
    public static @Nonnull ILocalizedMessage VID_AVI_JYUV_COMMAND() {
        return msg("VID_AVI_JYUV_COMMAND", "avi:jyuv");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: jpg</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_JPG_DESCRIPTION() {
        return msg("VID_IMG_SEQ_JPG_DESCRIPTION", "Image sequence: jpg");
    }

    /**
    <table border="1"><tr><td>
    <pre>jpg</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_JPG_COMMAND() {
        return msg("VID_IMG_SEQ_JPG_COMMAND", "jpg");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: bitstream</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_BS_DESCRIPTION() {
        return msg("VID_IMG_SEQ_BS_DESCRIPTION", "Image sequence: bitstream");
    }

    /**
    <table border="1"><tr><td>
    <pre>bs</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    */
    public static @Nonnull ILocalizedMessage VID_IMG_SEQ_BS_COMMAND() {
        return msg("VID_IMG_SEQ_BS_COMMAND", "bs");
    }

    /**
    <table border="1"><tr><td>
    <pre>AVI: YUV</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_AVI_YUV_DESCRIPTION() {
        return msg("VID_AVI_YUV_DESCRIPTION", "AVI: YUV");
    }

    /**
    <table border="1"><tr><td>
    <pre>avi:yuv</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line. Not case sensitive</p>
    */
    public static @Nonnull ILocalizedMessage VID_AVI_YUV_COMMAND() {
        return msg("VID_AVI_YUV_COMMAND", "avi:yuv");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0}-{1}</pre>
    </td></tr></table>
    <p>Display the range of frame files that will be saved. e.g. &quot;frame[001].png-frame[077].png&quot;</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VID_RANGE_OF_FILES_TO_SAVE(@Nonnull java.io.File startFileName, @Nonnull java.io.File endFileName) {
        return msg("VID_RANGE_OF_FILES_TO_SAVE", "{0}-{1}", startFileName, endFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>.spu (vag without header = raw SPU data)</pre>
    </td></tr></table>
    <ul>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage SPU_EXTENSION_DESCRIPTION() {
        return msg("SPU_EXTENSION_DESCRIPTION", ".spu (vag without header = raw SPU data)");
    }

    /**
    <table border="1"><tr><td>
    <pre>.vag (''Very Audio Good'' format)</pre>
    </td></tr></table>
    <ul>
       <li>SpuSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage VAG_EXTENSION_DESCRIPTION() {
        return msg("VAG_EXTENSION_DESCRIPTION", ".vag (''Very Audio Good'' format)");
    }

    /**
    <table border="1"><tr><td>
    <pre>Decode quality: {0}</pre>
    </td></tr></table>
    <p>See QUALITY_*_DESCRIPTION</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DECODE_QUALITY(@Nonnull String qualityDescription) {
        return msg("CMD_DECODE_QUALITY", "Decode quality: {0}", qualityDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Output files: {0}-{1}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_OUTPUT_FILES(@Nonnull java.io.File startFileName, @Nonnull java.io.File endFileName) {
        return msg("CMD_OUTPUT_FILES", "Output files: {0}-{1}", startFileName, endFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>With audio item(s):</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_SAVING_WITH_AUDIO_ITEMS() {
        return msg("CMD_SAVING_WITH_AUDIO_ITEMS", "With audio item(s):");
    }

    /**
    <table border="1"><tr><td>
    <pre>Emulate PSX audio/video sync: {0,choice,0#No|1#Yes}</pre>
    </td></tr></table>
    <p>0 = No, 1 = Yes</p>
    <ul>
       <li>SectorBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_EMULATE_PSX_AV_SYNC_NY(int willEmulate) {
        return msg("CMD_EMULATE_PSX_AV_SYNC_NY", "Emulate PSX audio/video sync: {0,choice,0#No|1#Yes}", willEmulate);
    }

    /**
    <table border="1"><tr><td>
    <pre>No audio</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_NO_AUDIO() {
        return msg("CMD_NO_AUDIO", "No audio");
    }

    /**
    <table border="1"><tr><td>
    <pre>Chroma upsampling: {0}</pre>
    </td></tr></table>
    <p>See CHROMA_UPSAMPLE_*_DESCRIPTION</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
       <li>Command_Static.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_UPSAMPLE_QUALITY(@Nonnull String upsampleDescription) {
        return msg("CMD_UPSAMPLE_QUALITY", "Chroma upsampling: {0}", upsampleDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Video format: {0}</pre>
    </td></tr></table>
    <p>See VID_*_DESCRIPTION</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_FORMAT(@Nonnull String videoFormatDescription) {
        return msg("CMD_VIDEO_FORMAT", "Video format: {0}", videoFormatDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Skip frames before {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_FRAME_RANGE_BEFORE(@Nonnull String startFrame) {
        return msg("CMD_FRAME_RANGE_BEFORE", "Skip frames before {0}", startFrame);
    }

    /**
    <table border="1"><tr><td>
    <pre>Skip frames after {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_FRAME_RANGE_AFTER(@Nonnull String endFrame) {
        return msg("CMD_FRAME_RANGE_AFTER", "Skip frames after {0}", endFrame);
    }

    /**
    <table border="1"><tr><td>
    <pre>Cropping: {0,choice,0#No|1#Yes}</pre>
    </td></tr></table>
    <p>0 = No, 1 = Yes</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_CROPPING(int willCrop) {
        return msg("CMD_CROPPING", "Cropping: {0,choice,0#No|1#Yes}", willCrop);
    }

    /**
    <table border="1"><tr><td>
    <pre>Video must have even dimensions to save as {0}, increasing size by 1 pixel</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_MUST_HAVE_EVEN_DIMS(@Nonnull String videoFormatDescription) {
        return msg("CMD_VIDEO_MUST_HAVE_EVEN_DIMS", "Video must have even dimensions to save as {0}, increasing size by 1 pixel", videoFormatDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Dimensions: {0,number,#}x{1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DIMENSIONS(int width, int height) {
        return msg("CMD_DIMENSIONS", "Dimensions: {0,number,#}x{1,number,#}", width, height);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error closing AVI</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage AVI_CLOSE_ERR() {
        return msg("AVI_CLOSE_ERR", "Error closing AVI");
    }

    /**
    <table border="1"><tr><td>
    <pre>Output file: {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_OUTPUT_FILE(@Nonnull java.io.File fileName) {
        return msg("CMD_OUTPUT_FILE", "Output file: {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc speed: {0,choice,1#1x|2#2x} ({1,number,#.###} fps)</pre>
    </td></tr></table>
    <p>discSpeed is 1 or 2</p>
    <ul>
       <li>SectorBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_DISC_SPEED(int discSpeed, double framesPerSecond) {
        return msg("CMD_DISC_SPEED", "Disc speed: {0,choice,1#1x|2#2x} ({1,number,#.###} fps)", discSpeed, framesPerSecond);
    }

    /**
    <table border="1"><tr><td>
    <pre>-ds &lt;disc speed&gt;</pre>
    </td></tr></table>
    <p>Note that the command -ds is hard-coded</p>
    <ul>
       <li>SectorBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_DS() {
        return msg("CMD_VIDEO_DS", "-ds <disc speed>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Specify 1 or 2 if disc speed is unknown.</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_DS_HELP() {
        return msg("CMD_VIDEO_DS_HELP", "Specify 1 or 2 if disc speed is unknown.");
    }

    /**
    <table border="1"><tr><td>
    <pre>-up &lt;upsampling&gt;</pre>
    </td></tr></table>
    <p>Note that the command -up is hard-coded</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_UP() {
        return msg("CMD_VIDEO_UP", "-up <upsampling>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Chroma upsampling method
(default {0}). Options:</pre>
    </td></tr></table>
    <p>See CHROMA_UPSAMPLE_*</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_UP_HELP(@Nonnull ILocalizedMessage defaultUpsamplingMethod) {
        return msg("CMD_VIDEO_UP_HELP", "Chroma upsampling method\n(default {0}). Options:", defaultUpsamplingMethod);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid upsample quality {0}</pre>
    </td></tr></table>
    <p>TODO replace this and similar lines with &quot;invalid option/value for {-command}&quot;</p>
    */
    public static @Nonnull ILocalizedMessage CMD_UPSAMPLE_QUALITY_INVALID(@Nonnull String badQualityName) {
        return msg("CMD_UPSAMPLE_QUALITY_INVALID", "Invalid upsample quality {0}", badQualityName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid decode quality {0}</pre>
    </td></tr></table>
    <p>TODO replace this and similar lines with &quot;invalid option/value for {-command}&quot;</p>
    */
    public static @Nonnull ILocalizedMessage CMD_DECODE_QUALITY_INVALID(@Nonnull String badQualityName) {
        return msg("CMD_DECODE_QUALITY_INVALID", "Invalid decode quality {0}", badQualityName);
    }

    /**
    <table border="1"><tr><td>
    <pre>-quality,-q &lt;quality&gt;</pre>
    </td></tr></table>
    <p>Note that the commands -quality and -q are hard-coded</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_QUALITY() {
        return msg("CMD_VIDEO_QUALITY", "-quality,-q <quality>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Decoding quality (default {0}). Options:</pre>
    </td></tr></table>
    <p>See QUALITY_*</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_QUALITY_HELP(@Nonnull ILocalizedMessage defaultQuality) {
        return msg("CMD_VIDEO_QUALITY_HELP", "Decoding quality (default {0}). Options:", defaultQuality);
    }

    /**
    <table border="1"><tr><td>
    <pre>-nocrop</pre>
    </td></tr></table>
    <p>Note that the command -nocrop is hard-coded</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_NOCROP() {
        return msg("CMD_VIDEO_NOCROP", "-nocrop");
    }

    /**
    <table border="1"><tr><td>
    <pre>Do not crop data around unused frame edges.</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_NOCROP_HELP() {
        return msg("CMD_VIDEO_NOCROP_HELP", "Do not crop data around unused frame edges.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame number type {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_FRAME_NUMBER_TYPE_INVALID(@Nonnull String badFrameNumberType) {
        return msg("CMD_FRAME_NUMBER_TYPE_INVALID", "Invalid frame number type {0}", badFrameNumberType);
    }

    /**
    <table border="1"><tr><td>
    <pre>-start #, -end #</pre>
    </td></tr></table>
    <p>Note that the commands -start and -end are hard-coded</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_FRAMES() {
        return msg("CMD_VIDEO_FRAMES", "-start #, -end #");
    }

    /**
    <table border="1"><tr><td>
    <pre>Process only frames in range.</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_FRAMES_HELP() {
        return msg("CMD_VIDEO_FRAMES_HELP", "Process only frames in range.");
    }

    /**
    <table border="1"><tr><td>
    <pre>-num &lt;type&gt;</pre>
    </td></tr></table>
    <p>Note that the command -num is hard-coded</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_NUM() {
        return msg("CMD_VIDEO_NUM", "-num <type>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame number to use when saving image sequence
(default {0}). Options:</pre>
    </td></tr></table>
    <p>See FRAME_NUM_FMT_*</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_NUM_HELP(@Nonnull ILocalizedMessage frameNumberType) {
        return msg("CMD_VIDEO_NUM_HELP", "Frame number to use when saving image sequence\n(default {0}). Options:", frameNumberType);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid video format {0}</pre>
    </td></tr></table>
    <p>TODO replace this and similar lines with &quot;invalid option/value for {-command}&quot;</p>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_FORMAT_INVALID(@Nonnull String badFormatString) {
        return msg("CMD_VIDEO_FORMAT_INVALID", "Invalid video format {0}", badFormatString);
    }

    /**
    <table border="1"><tr><td>
    <pre>-vidfmt,-vf &lt;format&gt;</pre>
    </td></tr></table>
    <p>Note that the commands -vidfmt and -vf are hard-coded</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_VF() {
        return msg("CMD_VIDEO_VF", "-vidfmt,-vf <format>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Output video format (default {0}).
Options:</pre>
    </td></tr></table>
    <p>See VID_*</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_VF_HELP(@Nonnull String defaultVideoFormat) {
        return msg("CMD_VIDEO_VF_HELP", "Output video format (default {0}).\nOptions:", defaultVideoFormat);
    }

    /**
    <table border="1"><tr><td>
    <pre>Video does not support indexing frames by header frame number, -start -end -num ignored</pre>
    </td></tr></table>
    <p>-start -end and -num are hard-coded command-line options </p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_HEADER_FRAME_NUMBER_UNSUPPORTED() {
        return msg("CMD_VIDEO_HEADER_FRAME_NUMBER_UNSUPPORTED", "Video does not support indexing frames by header frame number, -start -end -num ignored");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame(s) {0}</pre>
    </td></tr></table>
    <p>TODO replace this and similar lines with &quot;invalid option/value for {-command}&quot;</p>
    */
    public static @Nonnull ILocalizedMessage CMD_FRAME_RANGE_INVALID(@Nonnull String badFrameNumberString) {
        return msg("CMD_FRAME_RANGE_INVALID", "Invalid frame(s) {0}", badFrameNumberString);
    }

    /**
    <table border="1"><tr><td>
    <pre>-noaud</pre>
    </td></tr></table>
    <p>Note that the command -noaud is hard-coded</p>
    <ul>
       <li>PacketBasedVideoSaverBuilder.java</li>
       <li>SectorBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_NOAUD() {
        return msg("CMD_VIDEO_NOAUD", "-noaud");
    }

    /**
    <table border="1"><tr><td>
    <pre>Don''t save audio.</pre>
    </td></tr></table>
    <ul>
       <li>PacketBasedVideoSaverBuilder.java</li>
       <li>SectorBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_NOAUD_HELP() {
        return msg("CMD_VIDEO_NOAUD_HELP", "Don''t save audio.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save audio:</pre>
    </td></tr></table>
    <ul>
       <li>PacketBasedVideoSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_SAVE_AUDIO_LABEL() {
        return msg("GUI_SAVE_AUDIO_LABEL", "Save audio:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Decode quality:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverPanel.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_DECODE_QUALITY_LABEL() {
        return msg("GUI_DECODE_QUALITY_LABEL", "Decode quality:");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#} fps</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_FPS_LABLE_WHOLE_NUMBER(long framesPerSecond) {
        return msg("GUI_FPS_LABLE_WHOLE_NUMBER", "{0,number,#} fps", framesPerSecond);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#.###} ({1,number,#}/{2,number,#}) fps</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_FPS_LABEL_FRACTION(double decimalFramesPerSecond, long framesPerSecondNumerator, long framesPerSecondDenominator) {
        return msg("GUI_FPS_LABEL_FRACTION", "{0,number,#.###} ({1,number,#}/{2,number,#}) fps", decimalFramesPerSecond, framesPerSecondNumerator, framesPerSecondDenominator);
    }

    /**
    <table border="1"><tr><td>
    <pre>Dimensions:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverPanel.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_DIMENSIONS_LABEL() {
        return msg("GUI_DIMENSIONS_LABEL", "Dimensions:");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverPanel.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_DIMENSIONS_WIDTH_X_HEIGHT_LABEL(int width, int height) {
        return msg("GUI_DIMENSIONS_WIDTH_X_HEIGHT_LABEL", "{0,number,#}x{1,number,#}", width, height);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0}
to: {1}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverPanel.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_OUTPUT_VIDEO_FILE_RANGE(@Nonnull java.io.File startFileName, @Nonnull java.io.File endFileName) {
        return msg("GUI_OUTPUT_VIDEO_FILE_RANGE", "{0}\nto: {1}", startFileName, endFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc speed:</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_DISC_SPEED_LABEL() {
        return msg("GUI_DISC_SPEED_LABEL", "Disc speed:");
    }

    /**
    <table border="1"><tr><td>
    <pre>1x</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage DISC_SPEED_1X() {
        return msg("DISC_SPEED_1X", "1x");
    }

    /**
    <table border="1"><tr><td>
    <pre>2x</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage DISC_SPEED_2X() {
        return msg("DISC_SPEED_2X", "2x");
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio volume:</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage GUI_AUDIO_VOLUME_LABEL() {
        return msg("GUI_AUDIO_VOLUME_LABEL", "Audio volume:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Video format:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverPanel.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_VIDEO_FORMAT_LABEL() {
        return msg("GUI_VIDEO_FORMAT_LABEL", "Video format:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Crop</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverPanel.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_CROP_CHECKBOX() {
        return msg("GUI_CROP_CHECKBOX", "Crop");
    }

    /**
    <table border="1"><tr><td>
    <pre>Chroma upsampling:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverPanel.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_CHROMA_UPSAMPLING_LABEL() {
        return msg("GUI_CHROMA_UPSAMPLING_LABEL", "Chroma upsampling:");
    }

    /**
    <table border="1"><tr><td>
    <pre>-psxav</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_PSXAV() {
        return msg("CMD_VIDEO_PSXAV", "-psxav");
    }

    /**
    <table border="1"><tr><td>
    <pre>Emulate PSX audio/video timing.</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_VIDEO_PSXAV_HELP() {
        return msg("CMD_VIDEO_PSXAV_HELP", "Emulate PSX audio/video timing.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Emulate PSX a/v sync:</pre>
    </td></tr></table>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_EMULATE_PSX_AV_SYNC_LABEL() {
        return msg("GUI_EMULATE_PSX_AV_SYNC_LABEL", "Emulate PSX a/v sync:");
    }

    /**
    <table border="1"><tr><td>
    <pre></pre>
    </td></tr></table>
    <p>Column name is empty in English</p>
    <ul>
       <li>SectorBasedVideoSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_VID_AUDIO_SAVE_ID_COLUMN() {
        return msg("GUI_VID_AUDIO_SAVE_ID_COLUMN", "");
    }

    /**
    <table border="1"><tr><td>
    <pre>Copy to clipboard</pre>
    </td></tr></table>
    <ul>
       <li>TimPaletteSelector.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_COPY_TO_CLIPBOARD_TOOLTIP() {
        return msg("GUI_COPY_TO_CLIPBOARD_TOOLTIP", "Copy to clipboard");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to write image file {0} for palette {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_PALETTE_IMAGE_SAVE_FAIL(@Nonnull java.io.File outputFile, int paletteIndex) {
        return msg("CMD_PALETTE_IMAGE_SAVE_FAIL", "Unable to write image file {0} for palette {1,number,#}", outputFile, paletteIndex);
    }

    /**
    <table border="1"><tr><td>
    <pre>Format: {0}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_SAVE_FORMAT(@Nonnull String fileFormat) {
        return msg("CMD_TIM_SAVE_FORMAT", "Format: {0}", fileFormat);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid format {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_SAVE_FORMAT_INVALID(@Nonnull String badFileFormat) {
        return msg("CMD_TIM_SAVE_FORMAT_INVALID", "Invalid format {0}", badFileFormat);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid list of palettes {0}</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_PALETTE_LIST_INVALID(@Nonnull String badPaletteList) {
        return msg("CMD_TIM_PALETTE_LIST_INVALID", "Invalid list of palettes {0}", badPaletteList);
    }

    /**
    <table border="1"><tr><td>
    <pre>-pal &lt;#,#-#&gt;</pre>
    </td></tr></table>
    <p>Note that the command -pal is hard-coded</p>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_PAL() {
        return msg("CMD_TIM_PAL", "-pal <#,#-#>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Palettes to save (default all).</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_PAL_HELP() {
        return msg("CMD_TIM_PAL_HELP", "Palettes to save (default all).");
    }

    /**
    <table border="1"><tr><td>
    <pre>-imgfmt,-if &lt;format&gt;</pre>
    </td></tr></table>
    <p>Note that the commands -imgfmt and -if are hard-coded</p>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_IF() {
        return msg("CMD_TIM_IF", "-imgfmt,-if <format>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Output image format (default {0}). Options:</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_IF_HELP(@Nonnull String defaultImageFormat) {
        return msg("CMD_TIM_IF_HELP", "Output image format (default {0}). Options:", defaultImageFormat);
    }

    /**
    <table border="1"><tr><td>
    <pre>Palette files: {0}</pre>
    </td></tr></table>
    <p>See TIM_OUTPUT_FILES*</p>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_PALETTE_FILES(@Nonnull ILocalizedMessage ouputFiles) {
        return msg("CMD_TIM_PALETTE_FILES", "Palette files: {0}", ouputFiles);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#} files between {1}-{2}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage TIM_OUTPUT_FILES(int fileCount, @Nonnull String startFileName, @Nonnull String endFileName) {
        return msg("TIM_OUTPUT_FILES", "{0,number,#} files between {1}-{2}", fileCount, startFileName, endFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>None</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage TIM_OUTPUT_FILES_NONE() {
        return msg("TIM_OUTPUT_FILES_NONE", "None");
    }

    /**
    <table border="1"><tr><td>
    <pre>TIM image data not found</pre>
    </td></tr></table>
    <p>If TIM image data is not found where it should be on the disc</p>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage TIM_DATA_NOT_FOUND() {
        return msg("TIM_DATA_NOT_FOUND", "TIM image data not found");
    }

    /**
    <table border="1"><tr><td>
    <pre>Format:</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_TIM_SAVE_FORMAT_LABEL() {
        return msg("GUI_TIM_SAVE_FORMAT_LABEL", "Format:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error reading TIM preview
{0}</pre>
    </td></tr></table>
    <p>{0} is a technical multi-line error (exception stack trace)</p>
    <ul>
       <li>TimSaverBuilderGui.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_TIM_ERR_READING_PREVIEW(@Nonnull String listOfSourceCodeLineNumbers) {
        return msg("GUI_TIM_ERR_READING_PREVIEW", "Error reading TIM preview\n{0}", listOfSourceCodeLineNumbers);
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing {0,number,#} bytes to sector {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemTim.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_TIM_REPLACE_SECTOR_BYTES(int byteCount, int sectorNumber) {
        return msg("CMD_TIM_REPLACE_SECTOR_BYTES", "Writing {0,number,#} bytes to sector {1,number,#}", byteCount, sectorNumber);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}, Palettes: {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemTim.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage GUI_TIM_IMAGE_DETAILS(int timWidth, int timHeight, int paletteCount) {
        return msg("GUI_TIM_IMAGE_DETAILS", "{0,number,#}x{1,number,#}, Palettes: {2,number,#}", timWidth, timHeight, paletteCount);
    }

    /**
    <table border="1"><tr><td>
    <pre>New TIM format &quot;{0}&quot; does not match existing TIM format &quot;{1}&quot;</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemTim.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage TIM_INCOMPATIBLE(@Nonnull String newTimFormatDescription, @Nonnull String existingFormatDescription) {
        return msg("TIM_INCOMPATIBLE", "New TIM format \"{0}\" does not match existing TIM format \"{1}\"", newTimFormatDescription, existingFormatDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>This Tim image contains inconsistent data, but can still be extracted: {0}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage TIM_HAS_ISSUES_CAN_BE_EXTRACTED(@Nonnull String timDescription) {
        return msg("TIM_HAS_ISSUES_CAN_BE_EXTRACTED", "This Tim image contains inconsistent data, but can still be extracted: {0}", timDescription);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to replace a multi-paletted TIM with a simple image</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage TIM_REPLACE_MULTI_CLUT_UNABLE() {
        return msg("TIM_REPLACE_MULTI_CLUT_UNABLE", "Unable to replace a multi-paletted TIM with a simple image");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid value &quot;{0}&quot; for {1}</pre>
    </td></tr></table>
    <p>'command' means the command-line flag, for example &quot;-quality&quot;</p>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_INVALID_VALUE_FOR_CMD(@Nonnull String invalidValue, @Nonnull String command) {
        return msg("CMD_INVALID_VALUE_FOR_CMD", "Invalid value \"{0}\" for {1}", invalidValue, command);
    }

    /**
    <table border="1"><tr><td>
    <pre>Ignoring invalid value &quot;{0}&quot; for {1}</pre>
    </td></tr></table>
    <p>'command' means the command-line flag, for example &quot;-quality&quot;</p>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CMD_IGNORING_INVALID_VALUE_FOR_CMD(@Nonnull String invalidValue, @Nonnull String command) {
        return msg("CMD_IGNORING_INVALID_VALUE_FOR_CMD", "Ignoring invalid value \"{0}\" for {1}", invalidValue, command);
    }

    /**
    <table border="1"><tr><td>
    <pre>Opening file {0}</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_OPENING_FILE(@Nonnull String fileName) {
        return msg("IO_OPENING_FILE", "Opening file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>File not found</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage IO_OPENING_FILE_NOT_FOUND() {
        return msg("IO_OPENING_FILE_NOT_FOUND", "File not found");
    }

    /**
    <table border="1"><tr><td>
    <pre>File not found {0}</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_OPENING_FILE_NOT_FOUND_NAME(@Nonnull String fileName) {
        return msg("IO_OPENING_FILE_NOT_FOUND_NAME", "File not found {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to open file</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_OPENING_FILE_ERROR() {
        return msg("IO_OPENING_FILE_ERROR", "Failed to open file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to open file {0}</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_OPENING_FILE_ERROR_NAME(@Nonnull String fileName) {
        return msg("IO_OPENING_FILE_ERROR_NAME", "Failed to open file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error reading file</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage IO_READING_FILE_ERROR() {
        return msg("IO_READING_FILE_ERROR", "Error reading file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error reading file {0}</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_READING_FILE_ERROR_NAME(@Nonnull String fileName) {
        return msg("IO_READING_FILE_ERROR_NAME", "Error reading file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error reading from file</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_READING_FROM_FILE_ERROR() {
        return msg("IO_READING_FROM_FILE_ERROR", "Error reading from file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error reading from file {0}</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_READING_FROM_FILE_ERROR_NAME(@Nonnull String fileName) {
        return msg("IO_READING_FROM_FILE_ERROR_NAME", "Error reading from file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing file {0}</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_WRITING_FILE(@Nonnull String fileName) {
        return msg("IO_WRITING_FILE", "Writing file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error writing file</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage IO_WRITING_FILE_ERROR() {
        return msg("IO_WRITING_FILE_ERROR", "Error writing file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error writing file {0}</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_WRITING_FILE_ERROR_NAME(@Nonnull String fileName) {
        return msg("IO_WRITING_FILE_ERROR_NAME", "Error writing file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error writing to file</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage IO_WRITING_TO_FILE_ERROR() {
        return msg("IO_WRITING_TO_FILE_ERROR", "Error writing to file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error writing to file {0}</pre>
    </td></tr></table>
    <ul>
       <li>*</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage IO_WRITING_TO_FILE_ERROR_NAME(@Nonnull String fileName) {
        return msg("IO_WRITING_TO_FILE_ERROR_NAME", "Error writing to file {0}", fileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Cannot create directory over a file {0}</pre>
    </td></tr></table>
    <ul>
       <li>IO.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage CANNOT_CREATE_DIR_OVER_FILE(@Nonnull java.io.File existingFileName) {
        return msg("CANNOT_CREATE_DIR_OVER_FILE", "Cannot create directory over a file {0}", existingFileName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to create directory {0}</pre>
    </td></tr></table>
    <ul>
       <li>IO.java</li>
    </ul>
    */
    public static @Nonnull ILocalizedMessage UNABLE_TO_CREATE_DIR(@Nonnull java.io.File directoryName) {
        return msg("UNABLE_TO_CREATE_DIR", "Unable to create directory {0}", directoryName);
    }

    /**
    <table border="1"><tr><td>
    <pre>Directory {0} does not exist.</pre>
    </td></tr></table>
    */
    public static @Nonnull ILocalizedMessage DIR_DOES_NOT_EXIST(@Nonnull String directoryName) {
        return msg("DIR_DOES_NOT_EXIST", "Directory {0} does not exist.", directoryName);
    }

}