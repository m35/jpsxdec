/*
 * jPSXdec Translations
 * Copyright (c) 2015 Michael Sabin, Víctor González
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

import java.util.Date;
import javax.annotation.Nonnull;

public class I {

    private static LocalizedMessage inter(String sKey, String sEnglishDefault, Object ... aoArgs) {
        if (aoArgs.length == 0)
            return new LocalizedMessage(sKey, sEnglishDefault);
        else
            return new LocalizedMessage(sKey, sEnglishDefault, aoArgs);
    }

    /**
    <table border="1"><tr><td>
    <pre>Duplicate header frame number {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>AbstractVideoStreamIndex.java</li>
    </ul>
    */
    public static LocalizedMessage DUP_HDR_FRM_NUM(int a0) {
        return inter("DUP_HDR_FRM_NUM", "Duplicate header frame number {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Format: {0}</pre>
    </td></tr></table>
    <ul>
       <li>AudioSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_AUDIO_FORMAT(@Nonnull jpsxdec.formats.JavaAudioFormat a0) {
        return inter("CMD_AUDIO_FORMAT", "Format: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Volume: {0,number,#%}%</pre>
    </td></tr></table>
    <ul>
       <li>AudioSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VOLUME_PERCENT(double a0) {
        return inter("CMD_VOLUME_PERCENT", "Volume: {0,number,#%}%", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Filename: {0}</pre>
    </td></tr></table>
    <ul>
       <li>AudioSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_FILENAME(@Nonnull java.io.File a0) {
        return inter("CMD_FILENAME", "Filename: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error closing audio writer</pre>
    </td></tr></table>
    <ul>
       <li>AudioSaver.java</li>
    </ul>
    */
    public static LocalizedMessage ERR_CLOSING_AUDIO_WRITER() {
        return inter("ERR_CLOSING_AUDIO_WRITER", "Error closing audio writer");
    }

    /**
    <table border="1"><tr><td>
    <pre>-audfmt,-af &lt;format&gt;</pre>
    </td></tr></table>
    <p>Note that the commands -audfmt and -af are hard-coded</p>
    <ul>
       <li>AudioSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_AUDIO_AF() {
        return inter("CMD_AUDIO_AF", "-audfmt,-af <format>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Output audio format (default {0}). Options:</pre>
    </td></tr></table>
    <ul>
       <li>AudioSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_AUDIO_AF_HELP(@Nonnull String a0) {
        return inter("CMD_AUDIO_AF_HELP", "Output audio format (default {0}). Options:", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>-vol &lt;0-100&gt;</pre>
    </td></tr></table>
    <p>Note that the command -vol is hard-coded</p>
    <ul>
       <li>AudioSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_AUDIO_VOL() {
        return inter("CMD_AUDIO_VOL", "-vol <0-100>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Adjust volume (default {0,number,#}).</pre>
    </td></tr></table>
    <ul>
       <li>AudioSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_AUDIO_VOL_HELP(int a0) {
        return inter("CMD_AUDIO_VOL_HELP", "Adjust volume (default {0,number,#}).", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Ignoring invalid format {0}</pre>
    </td></tr></table>
    <ul>
       <li>AudioSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_IGNORING_INVALID_FORMAT(@Nonnull String a0) {
        return inter("CMD_IGNORING_INVALID_FORMAT", "Ignoring invalid format {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Ignoring invalid volume {0}</pre>
    </td></tr></table>
    <ul>
       <li>AudioSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_IGNORING_INVALID_VOLUME(@Nonnull String a0) {
        return inter("CMD_IGNORING_INVALID_VOLUME", "Ignoring invalid volume {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Volume:</pre>
    </td></tr></table>
    <ul>
       <li>AudioSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_VOLUME_LABEL() {
        return inter("GUI_VOLUME_LABEL", "Volume:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Avi file is closed</pre>
    </td></tr></table>
    <ul>
       <li>AviWriter.java</li>
    </ul>
    */
    public static LocalizedMessage AVI_FILE_IS_CLOSED() {
        return inter("AVI_FILE_IS_CLOSED", "Avi file is closed");
    }

    /**
    <table border="1"><tr><td>
    <pre>JFIF header not found in jpeg data, unable to write frame to AVI.</pre>
    </td></tr></table>
    <ul>
       <li>AviWriterMJPG.java</li>
    </ul>
    */
    public static LocalizedMessage AVI_JPEG_JFIF_HEADER_MISSING() {
        return inter("AVI_JPEG_JFIF_HEADER_MISSING", "JFIF header not found in jpeg data, unable to write frame to AVI.");
    }

    /**
    <table border="1"><tr><td>
    <pre>The file &quot;{0}&quot; already exists!
Do you want to replace it?</pre>
    </td></tr></table>
    <ul>
       <li>BetterFileChooser.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_FILE_EXISTS_REPLACE(@Nonnull String a0) {
        return inter("GUI_FILE_EXISTS_REPLACE", "The file \"{0}\" already exists!\nDo you want to replace it?", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not Iki</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_NOT_IKI() {
        return inter("FRAME_NOT_IKI", "Frame is not Iki");
    }

    /**
    <table border="1"><tr><td>
    <pre>Trying to reduce Qscale of ({0,number,#},{1,number,#}) to {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
    </ul>
    */
    public static LocalizedMessage IKI_REDUCING_QSCALE_OF_MB_TO_VAL(int a0, int a1, int a2) {
        return inter("IKI_REDUCING_QSCALE_OF_MB_TO_VAL", "Trying to reduce Qscale of ({0,number,#},{1,number,#}) to {2,number,#}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Incomplete iki frame header</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
    </ul>
    */
    public static LocalizedMessage IKI_INCOMPLETE_FRM_HDR() {
        return inter("IKI_INCOMPLETE_FRM_HDR", "Incomplete iki frame header");
    }

    /**
    <table border="1"><tr><td>
    <pre>New frame {0} demux size {1,number,#} &gt; max source {2,number,#}, so stopping</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
    </ul>
    */
    public static LocalizedMessage IKI_NEW_FRAME_GT_SRC_STOPPING(@Nonnull jpsxdec.discitems.FrameNumber a0, int a1, int a2) {
        return inter("IKI_NEW_FRAME_GT_SRC_STOPPING", "New frame {0} demux size {1,number,#} > max source {2,number,#}, so stopping", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>New frame {0} demux size {1,number,#} &lt;= max source {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
       <li>BitStreamUncompressor_Lain.java</li>
       <li>BitStreamUncompressor_STRv2.java</li>
    </ul>
    */
    public static LocalizedMessage NEW_FRAME_FITS(@Nonnull jpsxdec.discitems.FrameNumber a0, int a1, int a2) {
        return inter("NEW_FRAME_FITS", "New frame {0} demux size {1,number,#} <= max source {2,number,#}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Trying {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
       <li>BitStreamUncompressor_STRv2.java</li>
    </ul>
    */
    public static LocalizedMessage TRYING_QSCALE(int a0) {
        return inter("TRYING_QSCALE", "Trying {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>End of stream</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Iki.java</li>
       <li>ParsedMdecImage.java</li>
    </ul>
    */
    public static LocalizedMessage END_OF_STREAM() {
        return inter("END_OF_STREAM", "End of stream");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to escape {0}, AC code too large for Lain</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Lain.java</li>
    </ul>
    */
    public static LocalizedMessage AC_CODE_TOO_LARGE_FOR_LAIN(@Nonnull jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode a0) {
        return inter("AC_CODE_TOO_LARGE_FOR_LAIN", "Unable to escape {0}, AC code too large for Lain", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid chroma quantization scale {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Lain.java</li>
    </ul>
    */
    public static LocalizedMessage INVALID_CHROMA_QSCALE(int a0) {
        return inter("INVALID_CHROMA_QSCALE", "Invalid chroma quantization scale {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Inconsistent chroma quantization scale {0,number,#} != {1,number,#}</pre>
    </td></tr></table>
    <p>It would be nice to use &ne;, but probably wouldn't appear correctly</p>
    <ul>
       <li>BitStreamUncompressor_Lain.java</li>
    </ul>
    */
    public static LocalizedMessage INCONSISTENT_CHROMA_QSCALE(int a0, int a1) {
        return inter("INCONSISTENT_CHROMA_QSCALE", "Inconsistent chroma quantization scale {0,number,#} != {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Inconsistent luma quantization scale {0,number,#} != {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Lain.java</li>
    </ul>
    */
    public static LocalizedMessage INCONSISTENT_LUMA_QSCALE(int a0, int a1) {
        return inter("INCONSISTENT_LUMA_QSCALE", "Inconsistent luma quantization scale {0,number,#} != {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Trying luma {0,number,#} chroma {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Lain.java</li>
    </ul>
    */
    public static LocalizedMessage TRYING_LUMA_CHROMA(int a0, int a1) {
        return inter("TRYING_LUMA_CHROMA", "Trying luma {0,number,#} chroma {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid luma quantization scale {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Lain.java</li>
    </ul>
    */
    public static LocalizedMessage INVALID_LUMA_QSCALE(int a0) {
        return inter("INVALID_LUMA_QSCALE", "Invalid luma quantization scale {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not Lain</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_Lain.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_NOT_LAIN() {
        return inter("FRAME_NOT_LAIN", "Frame is not Lain");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not STRv1</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_STRv1.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_NOT_STRV1() {
        return inter("FRAME_NOT_STRV1", "Frame is not STRv1");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not STRv2</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_STRv2.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_NOT_STRV2() {
        return inter("FRAME_NOT_STRV2", "Frame is not STRv2");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is not STRv3</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_STRv3.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_NOT_STRV3() {
        return inter("FRAME_NOT_STRV3", "Frame is not STRv3");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid quantization scale {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_STRv2.java</li>
    </ul>
    */
    public static LocalizedMessage INVALID_QSCALE(int a0) {
        return inter("INVALID_QSCALE", "Invalid quantization scale {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Inconsistent quantization scale {0,number,#} != {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_STRv2.java</li>
    </ul>
    */
    public static LocalizedMessage INCONSISTENT_QSCALE(int a0, int a1) {
        return inter("INCONSISTENT_QSCALE", "Inconsistent quantization scale {0,number,#} != {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error uncompressing macro block {0,number,#}.{1,number,#}: Unknown luma DC variable length code {2}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_STRv3.java</li>
    </ul>
    */
    public static LocalizedMessage STRV3_BLOCK_UNCOMPRESS_ERR_UNKNOWN_LUMA_DC_VLC(int a0, int a1, @Nonnull String a2) {
        return inter("STRV3_BLOCK_UNCOMPRESS_ERR_UNKNOWN_LUMA_DC_VLC", "Error uncompressing macro block {0,number,#}.{1,number,#}: Unknown luma DC variable length code {2}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error uncompressing macro block {0,number,#}.{1,number,#}: Chroma DC out of bounds: {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_STRv3.java</li>
    </ul>
    */
    public static LocalizedMessage STRV3_BLOCK_UNCOMPRESS_ERR_CHROMA_DC_OOB(int a0, int a1, int a2) {
        return inter("STRV3_BLOCK_UNCOMPRESS_ERR_CHROMA_DC_OOB", "Error uncompressing macro block {0,number,#}.{1,number,#}: Chroma DC out of bounds: {2,number,#}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error uncompressing macro block {0,number,#}.{1,number,#}: Unknown chroma DC variable length code {2}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_STRv3.java</li>
    </ul>
    */
    public static LocalizedMessage STRV3_BLOCK_UNCOMPRESS_ERR_UNKNOWN_CHROMA_DC_VLC(int a0, int a1, @Nonnull String a2) {
        return inter("STRV3_BLOCK_UNCOMPRESS_ERR_UNKNOWN_CHROMA_DC_VLC", "Error uncompressing macro block {0,number,#}.{1,number,#}: Unknown chroma DC variable length code {2}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error uncompressing macro block {0,number,#}.{1,number,#}: Luma DC out of bounds: {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor_STRv3.java</li>
    </ul>
    */
    public static LocalizedMessage STRV3_BLOCK_UNCOMPRESS_ERR_LUMA_DC_OOB(int a0, int a1, int a2) {
        return inter("STRV3_BLOCK_UNCOMPRESS_ERR_LUMA_DC_OOB", "Error uncompressing macro block {0,number,#}.{1,number,#}: Luma DC out of bounds: {2,number,#}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unmatched AC variable length code: {0}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor.java</li>
    </ul>
    */
    public static LocalizedMessage UNMATCHED_AC_VLC(@Nonnull String a0) {
        return inter("UNMATCHED_AC_VLC", "Unmatched AC variable length code: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>[MDEC] Run length out of bounds [{0,number,#}] in macroblock {1,number,#} block {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>BitStreamUncompressor.java</li>
    </ul>
    */
    public static LocalizedMessage RLC_OOB_IN_MB_BLOCK(int a0, int a1, int a2) {
        return inter("RLC_OOB_IN_MB_BLOCK", "[MDEC] Run length out of bounds [{0,number,#}] in macroblock {1,number,#} block {2,number,#}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Serialized sector count {0,number,#} does not match actual {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CdFileSectorReader.java</li>
    </ul>
    */
    public static LocalizedMessage SECTOR_COUNT_MISMATCH(int a0, int a1) {
        return inter("SECTOR_COUNT_MISMATCH", "Serialized sector count {0,number,#} does not match actual {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to deserialize CD string: {0}</pre>
    </td></tr></table>
    <ul>
       <li>CdFileSectorReader.java</li>
    </ul>
    */
    public static LocalizedMessage DESERIALIZE_CD_FAIL(@Nonnull String a0) {
        return inter("DESERIALIZE_CD_FAIL", "Failed to deserialize CD string: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>.iso (2048 bytes/sector) format</pre>
    </td></tr></table>
    <ul>
       <li>CdFileSectorReader.java</li>
    </ul>
    */
    public static LocalizedMessage ISO_EXTENSION() {
        return inter("ISO_EXTENSION", ".iso (2048 bytes/sector) format");
    }

    /**
    <table border="1"><tr><td>
    <pre>partial header (2336 bytes/sector) format</pre>
    </td></tr></table>
    <ul>
       <li>CdFileSectorReader.java</li>
    </ul>
    */
    public static LocalizedMessage DISC_FMT_2336() {
        return inter("DISC_FMT_2336", "partial header (2336 bytes/sector) format");
    }

    /**
    <table border="1"><tr><td>
    <pre>BIN/CUE + Sub Channel (2448 bytes/sector) format</pre>
    </td></tr></table>
    <ul>
       <li>CdFileSectorReader.java</li>
    </ul>
    */
    public static LocalizedMessage DISC_FMT_2448() {
        return inter("DISC_FMT_2448", "BIN/CUE + Sub Channel (2448 bytes/sector) format");
    }

    /**
    <table border="1"><tr><td>
    <pre>BIN/CUE (2352 bytes/sector) format</pre>
    </td></tr></table>
    <ul>
       <li>CdFileSectorReader.java</li>
    </ul>
    */
    public static LocalizedMessage DISC_FMT_2352() {
        return inter("DISC_FMT_2352", "BIN/CUE (2352 bytes/sector) format");
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to read at least 1 entire sector.</pre>
    </td></tr></table>
    <ul>
       <li>CdFileSectorReader.java</li>
    </ul>
    */
    public static LocalizedMessage FAILED_TO_READ_1_SECTOR() {
        return inter("FAILED_TO_READ_1_SECTOR", "Failed to read at least 1 entire sector.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1,number,#} bytes in the sync header are corrupted</pre>
    </td></tr></table>
    <ul>
       <li>CdxaHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECTOR_CORRUPTED_SYNC_HEADER(int a0, int a1) {
        return inter("SECTOR_CORRUPTED_SYNC_HEADER", "Sector {0,number,#} {1,number,#} bytes in the sync header are corrupted", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} Mode number is corrupted {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CdxaHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECTOR_CORRUPTED_MODE_NUMBER(int a0, int a1) {
        return inter("SECTOR_CORRUPTED_MODE_NUMBER", "Sector {0,number,#} Mode number is corrupted {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} Seconds number is corrupted {1}</pre>
    </td></tr></table>
    <ul>
       <li>CdxaHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECTOR_CORRUPTED_SECONDS(int a0, @Nonnull String a1) {
        return inter("SECTOR_CORRUPTED_SECONDS", "Sector {0,number,#} Seconds number is corrupted {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} Sectors number is corrupted {1}</pre>
    </td></tr></table>
    <ul>
       <li>CdxaHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECTOR_CORRUPTED_SECTOR_NUMBER(int a0, @Nonnull String a1) {
        return inter("SECTOR_CORRUPTED_SECTOR_NUMBER", "Sector {0,number,#} Sectors number is corrupted {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} Minutes number is corrupted {1}</pre>
    </td></tr></table>
    <ul>
       <li>CdxaHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECTOR_CORRUPTED_MINUTES(int a0, @Nonnull String a1) {
        return inter("SECTOR_CORRUPTED_MINUTES", "Sector {0,number,#} Minutes number is corrupted {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1} corrupted: {2} (bad) != {3} (chose {3})</pre>
    </td></tr></table>
    <p>Parameter {1} will be one of the SUB_HEADER_* values below</p>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECT_CORRUPT_DIFF_1BAD2GOOD(int a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1, @Nonnull Object a2, @Nonnull Object a3) {
        return inter("SECT_CORRUPT_DIFF_1BAD2GOOD", "Sector {0,number,#} {1} corrupted: {2} (bad) != {3} (chose {3})", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1} corrupted: {2} != {3} (chose {2} by default)</pre>
    </td></tr></table>
    <p>Parameter {1} will be one of the SUB_HEADER_* values below</p>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECT_CORRUPT_DIFF_BOTHGOOD_1DEFAULT(int a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1, @Nonnull Object a2, @Nonnull Object a3) {
        return inter("SECT_CORRUPT_DIFF_BOTHGOOD_1DEFAULT", "Sector {0,number,#} {1} corrupted: {2} != {3} (chose {2} by default)", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1} corrupted: {2} != {3} (chose {2} by confidence)</pre>
    </td></tr></table>
    <p>Parameter {1} will be one of the SUB_HEADER_* values below</p>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECT_CORRUPT_DIFF_BOTHGOOD_1CONFIDENCE(int a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1, @Nonnull Object a2, @Nonnull Object a3) {
        return inter("SECT_CORRUPT_DIFF_BOTHGOOD_1CONFIDENCE", "Sector {0,number,#} {1} corrupted: {2} != {3} (chose {2} by confidence)", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1} corrupted: {2} != {3} (bad) (chose {2})</pre>
    </td></tr></table>
    <p>Parameter {1} will be one of the SUB_HEADER_* values below</p>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECT_CORRUPT_DIFF_1GOOD2BAD(int a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1, @Nonnull Object a2, @Nonnull Object a3) {
        return inter("SECT_CORRUPT_DIFF_1GOOD2BAD", "Sector {0,number,#} {1} corrupted: {2} != {3} (bad) (chose {2})", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1} corrupted: {2} (bad) != {3} (bad) (chose {2} by default)</pre>
    </td></tr></table>
    <p>Parameter {1} will be one of the SUB_HEADER_* values below</p>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECT_CORRUPT_DIFF_BOTHBAD_2DEFAULT(int a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1, @Nonnull Object a2, @Nonnull Object a3) {
        return inter("SECT_CORRUPT_DIFF_BOTHBAD_2DEFAULT", "Sector {0,number,#} {1} corrupted: {2} (bad) != {3} (bad) (chose {2} by default)", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1} corrupted: {2} (bad) != {3} (bad) (chose {3} by confidence)</pre>
    </td></tr></table>
    <p>Parameter {1} will be one of the SUB_HEADER_* values below</p>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECT_CORRUPT_DIFF_BOTHBAD_2CONFIDENCE(int a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1, @Nonnull Object a2, @Nonnull Object a3) {
        return inter("SECT_CORRUPT_DIFF_BOTHBAD_2CONFIDENCE", "Sector {0,number,#} {1} corrupted: {2} (bad) != {3} (bad) (chose {3} by confidence)", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1} corrupted: {2} != {3} (chose {3} by confidence)</pre>
    </td></tr></table>
    <p>Parameter {1} will be one of the SUB_HEADER_* values below</p>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECT_CORRUPT_DIFF_BOTHGOOD_2CONFIDENCE(int a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1, @Nonnull Object a2, @Nonnull Object a3) {
        return inter("SECT_CORRUPT_DIFF_BOTHGOOD_2CONFIDENCE", "Sector {0,number,#} {1} corrupted: {2} != {3} (chose {3} by confidence)", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1} corrupted: {2} (bad) != {3} (bad) (chose {2} by confidence)</pre>
    </td></tr></table>
    <p>Parameter {1} will be one of the SUB_HEADER_* values below</p>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECT_CORRUPT_DIFF_BOTHBAD_1CONFIDENCE(int a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1, @Nonnull Object a2, @Nonnull Object a3) {
        return inter("SECT_CORRUPT_DIFF_BOTHBAD_1CONFIDENCE", "Sector {0,number,#} {1} corrupted: {2} (bad) != {3} (bad) (chose {2} by confidence)", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} {1} corrupted: {2} (bad) == {3} (bad)</pre>
    </td></tr></table>
    <p>Parameter {1} will be one of the SUB_HEADER_* values below</p>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SECT_CORRUPT_EQUAL_BOTHBAD(int a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1, @Nonnull Object a2, @Nonnull Object a3) {
        return inter("SECT_CORRUPT_EQUAL_BOTHBAD", "Sector {0,number,#} {1} corrupted: {2} (bad) == {3} (bad)", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>File Number</pre>
    </td></tr></table>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SUB_HEADER_FILE_NUMBER() {
        return inter("SUB_HEADER_FILE_NUMBER", "File Number");
    }

    /**
    <table border="1"><tr><td>
    <pre>Coding Info</pre>
    </td></tr></table>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SUB_HEADER_CODING_INFO() {
        return inter("SUB_HEADER_CODING_INFO", "Coding Info");
    }

    /**
    <table border="1"><tr><td>
    <pre>Channel Number</pre>
    </td></tr></table>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SUB_HEADER_CHANNEL_NUMBER() {
        return inter("SUB_HEADER_CHANNEL_NUMBER", "Channel Number");
    }

    /**
    <table border="1"><tr><td>
    <pre>Submode</pre>
    </td></tr></table>
    <ul>
       <li>CdxaSubHeader.java</li>
    </ul>
    */
    public static LocalizedMessage SUB_HEADER_SUBMODE() {
        return inter("SUB_HEADER_SUBMODE", "Submode");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid sector range: {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_CopySect.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_SECTOR_RANGE_INVALID(@Nonnull String a0) {
        return inter("CMD_SECTOR_RANGE_INVALID", "Invalid sector range: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Copying sectors {0,number,#} - {1,number,#} to {2}</pre>
    </td></tr></table>
    <ul>
       <li>Command_CopySect.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_COPYING_SECTOR(int a0, int a1, @Nonnull String a2) {
        return inter("CMD_COPYING_SECTOR", "Copying sectors {0,number,#} - {1,number,#} to {2}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sorry, couldn''t find any disc items of type {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_NO_ITEMS_OF_TYPE(@Nonnull String a0) {
        return inter("CMD_NO_ITEMS_OF_TYPE", "Sorry, couldn''t find any disc items of type {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Item complete.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ITEM_COMPLETE() {
        return inter("CMD_ITEM_COMPLETE", "Item complete.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Could not find disc item {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_ITEM_NOT_FOUND_NUM(int a0) {
        return inter("CMD_DISC_ITEM_NOT_FOUND_NUM", "Could not find disc item {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Could not find disc item {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_ITEM_NOT_FOUND_STR(@Nonnull String a0) {
        return inter("CMD_DISC_ITEM_NOT_FOUND_STR", "Could not find disc item {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Detailed help for</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DETAILED_HELP_FOR() {
        return inter("CMD_DETAILED_HELP_FOR", "Detailed help for");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid item number: {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ITEM_NUMBER_INVALID(@Nonnull String a0) {
        return inter("CMD_ITEM_NUMBER_INVALID", "Invalid item number: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to replace a TIM image that has multiple palettes.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_UNABLE_TO_REPLACE_MULTI_PAL_TIM() {
        return inter("CMD_UNABLE_TO_REPLACE_MULTI_PAL_TIM", "Unable to replace a TIM image that has multiple palettes.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Reopening disc image with write access.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_REOPENING_DISC_WRITE_ACCESS() {
        return inter("CMD_REOPENING_DISC_WRITE_ACCESS", "Reopening disc image with write access.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc item is not audio or video. Cannot create player.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_ITEM_NOT_AUDIO_VIDEO_NO_PLAYER() {
        return inter("CMD_DISC_ITEM_NOT_AUDIO_VIDEO_NO_PLAYER", "Disc item is not audio or video. Cannot create player.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Time: {0,number,#.##} sec</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PROCESS_TIME(double a0) {
        return inter("CMD_PROCESS_TIME", "Time: {0,number,#.##} sec", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>All index items complete.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ALL_ITEMS_COMPLETE() {
        return inter("CMD_ALL_ITEMS_COMPLETE", "All index items complete.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid item identifier: {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ITEM_ID_INVALID(@Nonnull String a0) {
        return inter("CMD_ITEM_ID_INVALID", "Invalid item identifier: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Hope your disc image is backed up because this is irreversable.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_BACKUP_DISC_IMAGE_WARNING() {
        return inter("CMD_BACKUP_DISC_IMAGE_WARNING", "Hope your disc image is backed up because this is irreversable.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error with player</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PLAYER_ERR() {
        return inter("CMD_PLAYER_ERR", "Error with player");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc item isn't a video.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_ITEM_NOT_VIDEO() {
        return inter("CMD_DISC_ITEM_NOT_VIDEO", "Disc item isn't a video.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc item isn't a XA stream.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_ITEM_NOT_XA() {
        return inter("CMD_DISC_ITEM_NOT_XA", "Disc item isn't a XA stream.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc item isn't a TIM image.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_ITEM_NOT_TIM() {
        return inter("CMD_DISC_ITEM_NOT_TIM", "Disc item isn't a TIM image.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid or missing XA item number {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_XA_REPLACE_BAD_ITEM_NUM(@Nonnull String a0) {
        return inter("CMD_XA_REPLACE_BAD_ITEM_NUM", "Invalid or missing XA item number {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc decoding/extracting complete.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PROCESS_COMPLETE() {
        return inter("CMD_PROCESS_COMPLETE", "Disc decoding/extracting complete.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Creating player for</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_CREATING_PLAYER() {
        return inter("CMD_CREATING_PLAYER", "Creating player for");
    }

    /**
    <table border="1"><tr><td>
    <pre>Opening patch index {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_XA_REPLACE_OPENING_PATCH_IDX(@Nonnull String a0) {
        return inter("CMD_XA_REPLACE_OPENING_PATCH_IDX", "Opening patch index {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_SAVING(@Nonnull jpsxdec.discitems.DiscItem a0) {
        return inter("CMD_SAVING", "Saving {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,choice,0#No files created|1#1 file created|2#{0} files created}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_NUM_FILES_CREATED(int a0) {
        return inter("CMD_NUM_FILES_CREATED", "{0,choice,0#No files created|1#1 file created|2#{0} files created}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>jPSXdec: PSX media decoder (non-commercial) v{0} - Player</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage JPSXDEC_PLAYER_WIN_TITLE_POSTFIX(@Nonnull String a0) {
        return inter("JPSXDEC_PLAYER_WIN_TITLE_POSTFIX", "jPSXdec: PSX media decoder (non-commercial) v{0} - Player", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>save</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
    </ul>
    */
    public static LocalizedMessage SAVE_LOG_FILE_BASE_NAME() {
        return inter("SAVE_LOG_FILE_BASE_NAME", "save");
    }

    /**
    <table border="1"><tr><td>
    <pre>Play</pre>
    </td></tr></table>
    <ul>
       <li>Command_Items.java</li>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_PLAY_BTN() {
        return inter("GUI_PLAY_BTN", "Play");
    }

    /**
    <table border="1"><tr><td>
    <pre>Generating sector list</pre>
    </td></tr></table>
    <ul>
       <li>Command_SectorDump.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_GENERATING_SECTOR_LIST() {
        return inter("CMD_GENERATING_SECTOR_LIST", "Generating sector list");
    }

    /**
    <table border="1"><tr><td>
    <pre>-dim option required</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DIM_OPTION_REQURIED() {
        return inter("CMD_DIM_OPTION_REQURIED", "-dim option required");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid quality {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_QUALITY_INVALID(@Nonnull String a0) {
        return inter("CMD_QUALITY_INVALID", "Invalid quality {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Using quality {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_USING_QUALITY(@Nonnull String a0) {
        return inter("CMD_USING_QUALITY", "Using quality {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error: not a Tim image</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_NOT_TIM() {
        return inter("CMD_NOT_TIM", "Error: not a Tim image");
    }

    /**
    <table border="1"><tr><td>
    <pre>Using upsampling {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_USING_UPSAMPLING(@Nonnull jpsxdec.i18n.LocalizedMessage a0) {
        return inter("CMD_USING_UPSAMPLING", "Using upsampling {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error reading or writing TIM file</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_TIM_IO_ERR() {
        return inter("CMD_TIM_IO_ERR", "Error reading or writing TIM file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid format type {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_FORMAT_INVALID(@Nonnull String a0) {
        return inter("CMD_FORMAT_INVALID", "Invalid format type {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to enable decoding debug because asserts are disabled.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ASSERT_DISABLED_NO_DEBUG() {
        return inter("CMD_ASSERT_DISABLED_NO_DEBUG", "Unable to enable decoding debug because asserts are disabled.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Reading TIM file {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_READING_TIM(@Nonnull java.io.File a0) {
        return inter("CMD_READING_TIM", "Reading TIM file {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid static type: {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_STATIC_TYPE_INVALID(@Nonnull String a0) {
        return inter("CMD_STATIC_TYPE_INVALID", "Invalid static type: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame converted successfully.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_FRAME_CONVERT_OK() {
        return inter("CMD_FRAME_CONVERT_OK", "Frame converted successfully.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Reading static file {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_READING_STATIC_FILE(@Nonnull java.io.File a0) {
        return inter("CMD_READING_STATIC_FILE", "Reading static file {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Image converted successfully</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_IMAGE_CONVERT_OK() {
        return inter("CMD_IMAGE_CONVERT_OK", "Image converted successfully");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid upsampling {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_UPSAMPLING_INVALID(@Nonnull String a0) {
        return inter("CMD_UPSAMPLING_INVALID", "Invalid upsampling {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Start java using the -ea option.</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ASSERT_DISABLED_NO_DEBUG_USE_EA() {
        return inter("CMD_ASSERT_DISABLED_NO_DEBUG_USE_EA", "Start java using the -ea option.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_WRITING(@Nonnull String a0) {
        return inter("CMD_WRITING", "Writing {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving as: {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command_Static.java</li>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_SAVING_AS(@Nonnull java.io.File a0) {
        return inter("CMD_SAVING_AS", "Saving as: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Generating visualization</pre>
    </td></tr></table>
    <ul>
       <li>Command_Visualize.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_GENERATING_VISUALIZATION() {
        return inter("CMD_GENERATING_VISUALIZATION", "Generating visualization");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error creating or writing the visualization</pre>
    </td></tr></table>
    <ul>
       <li>Command_Visualize.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VISUALIZATION_ERR() {
        return inter("CMD_VISUALIZATION_ERR", "Error creating or writing the visualization");
    }

    /**
    <table border="1"><tr><td>
    <pre>Using source file {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_USING_SRC_FILE(@Nonnull java.io.File a0) {
        return inter("CMD_USING_SRC_FILE", "Using source file {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Need a input file and/or index file to load.</pre>
    </td></tr></table>
    <ul>
       <li>Command.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_NEED_INPUT_OR_INDEX() {
        return inter("CMD_NEED_INPUT_OR_INDEX", "Need a input file and/or index file to load.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Input file disc image required for this command.</pre>
    </td></tr></table>
    <ul>
       <li>Command.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_FILE_REQUIRED() {
        return inter("CMD_DISC_FILE_REQUIRED", "Input file disc image required for this command.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Input file is required for this command.</pre>
    </td></tr></table>
    <ul>
       <li>Command.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_INPUT_FILE_REQUIRED() {
        return inter("CMD_INPUT_FILE_REQUIRED", "Input file is required for this command.");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#} items loaded.</pre>
    </td></tr></table>
    <ul>
       <li>Command.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ITEMS_LOADED(int a0) {
        return inter("CMD_ITEMS_LOADED", "{0,number,#} items loaded.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Reading index file {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_READING_INDEX_FILE(@Nonnull String a0) {
        return inter("CMD_READING_INDEX_FILE", "Reading index file {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Input file not found {0}</pre>
    </td></tr></table>
    <ul>
       <li>Command.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_INPUT_FILE_NOT_FOUND(@Nonnull java.io.File a0) {
        return inter("CMD_INPUT_FILE_NOT_FOUND", "Input file not found {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>index</pre>
    </td></tr></table>
    <ul>
       <li>Command.java</li>
       <li>CommandLine.java</li>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_LOG_FILE_BASE_NAME() {
        return inter("INDEX_LOG_FILE_BASE_NAME", "index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error loading index file</pre>
    </td></tr></table>
    <ul>
       <li>Command.java</li>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage ERR_LOADING_INDEX_FILE() {
        return inter("ERR_LOADING_INDEX_FILE", "Error loading index file");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#} items found</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_NUM_ITEMS_FOUND(int a0) {
        return inter("CMD_NUM_ITEMS_FOUND", "{0,number,#} items found", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Building index</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_BUILDING_INDEX() {
        return inter("CMD_BUILDING_INDEX", "Building index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc read error.</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_READ_ERROR() {
        return inter("CMD_DISC_READ_ERROR", "Disc read error.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid verbosity level {0}</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VERBOSE_LVL_INVALID_STR(@Nonnull String a0) {
        return inter("CMD_VERBOSE_LVL_INVALID_STR", "Invalid verbosity level {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Opening {0}</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_OPENING(@Nonnull String a0) {
        return inter("CMD_OPENING", "Opening {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error opening file for saving</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_SAVE_OPEN_ERR() {
        return inter("CMD_SAVE_OPEN_ERR", "Error opening file for saving");
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving index as {0}</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_SAVING_INDEX(@Nonnull String a0) {
        return inter("CMD_SAVING_INDEX", "Saving index as {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Try -? for help.</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_TRY_HELP() {
        return inter("CMD_TRY_HELP", "Try -? for help.");
    }

    /**
    <table border="1"><tr><td>
    <pre>No items found, not saving index file</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_NOT_SAVING_EMPTY_INDEX() {
        return inter("CMD_NOT_SAVING_EMPTY_INDEX", "No items found, not saving index file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error: {0}</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ARGPARSE_ERR(@Nonnull String a0) {
        return inter("CMD_ARGPARSE_ERR", "Error: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>ERROR: {0} ({1})</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ERR_EX_CLASS(@Nonnull java.lang.Throwable a0, @Nonnull String a1) {
        return inter("CMD_ERR_EX_CLASS", "ERROR: {0} ({1})", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Need a main command.</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_NEED_MAIN_COMMAND() {
        return inter("CMD_NEED_MAIN_COMMAND", "Need a main command.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid verbosity level {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VERBOSE_LVL_INVALID_NUM(int a0) {
        return inter("CMD_VERBOSE_LVL_INVALID_NUM", "Invalid verbosity level {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Identified as {0}</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_IDENTIFIED(@Nonnull jpsxdec.i18n.LocalizedMessage a0) {
        return inter("CMD_DISC_IDENTIFIED", "Identified as {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Command needs disc file</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_COMMAND_NEEDS_DISC() {
        return inter("CMD_COMMAND_NEEDS_DISC", "Command needs disc file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error writing index file.</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage WRITING_INDEX_FILE_ERR() {
        return inter("WRITING_INDEX_FILE_ERR", "Error writing index file.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Too many main commands.</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_TOO_MANY_MAIN_COMMANDS() {
        return inter("CMD_TOO_MANY_MAIN_COMMANDS", "Too many main commands.");
    }

    /**
    <table border="1"><tr><td>
    <pre>File not found {0}</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_FILE_NOT_FOUND_FILE(@Nonnull java.io.File a0) {
        return inter("CMD_FILE_NOT_FOUND_FILE", "File not found {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>jPSXdec: PSX media decoder (non-commercial) v{0}</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
       <li>Gui.java</li>
       <li>GuiSettings.java</li>
       <li>Mdec2Jpeg.java</li>
       <li>AviWriter.java</li>
       <li>DebugFormatter.java</li>
       <li>UserFriendlyLogger.java</li>
    </ul>
    */
    public static LocalizedMessage JPSXDEC_VERSION_NON_COMMERCIAL(@Nonnull String a0) {
        return inter("JPSXDEC_VERSION_NON_COMMERCIAL", "jPSXdec: PSX media decoder (non-commercial) v{0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Indexing {0}</pre>
    </td></tr></table>
    <ul>
       <li>CommandLine.java</li>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_GUI_INDEXING(@Nonnull jpsxdec.cdreaders.CdFileSectorReader a0) {
        return inter("CMD_GUI_INDEXING", "Indexing {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1,number,#%}{2,choice,0#|1# '{2,number,#}' warnings}{3,choice,0#|1# '{3,number,#}' errors}</pre>
    </td></tr></table>
    <p>Note the single quotes are necessary inside the choice argument.</p>
    <ul>
       <li>ConsoleProgressListenerLogger.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PROGRESS(@Nonnull String a0, double a1, int a2, int a3) {
        return inter("CMD_PROGRESS", "[{0}] {1,number,#%}{2,choice,0#|1# '{2,number,#}' warnings}{3,choice,0#|1# '{3,number,#}' errors}", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1,number,#%} {2} {3,choice,0#|1# '{3,number,#}' warnings}{4,choice,0#|1# '{4,number,#}' errors}</pre>
    </td></tr></table>
    <p>Note the single quotes are necessary inside the choice argument.</p>
    <ul>
       <li>ConsoleProgressListenerLogger.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PROGRESS_WITH_MSG(@Nonnull String a0, double a1, @Nonnull jpsxdec.i18n.LocalizedMessage a2, int a3, int a4) {
        return inter("CMD_PROGRESS_WITH_MSG", "[{0}] {1,number,#%} {2} {3,choice,0#|1# '{3,number,#}' warnings}{4,choice,0#|1# '{4,number,#}' errors}", a0, a1, a2, a3, a4);
    }

    /**
    <table border="1"><tr><td>
    <pre>Inconsistent width {0,number,#} != {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CrusaderDemuxer.java</li>
    </ul>
    */
    public static LocalizedMessage INCONSISTENT_WIDTH(int a0, int a1) {
        return inter("INCONSISTENT_WIDTH", "Inconsistent width {0,number,#} != {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Embedded Crusader audio {0,number,#}Hz</pre>
    </td></tr></table>
    <ul>
       <li>CrusaderDemuxer.java</li>
    </ul>
    */
    public static LocalizedMessage EMBEDDED_CRUSADER_AUDIO_HZ(int a0) {
        return inter("EMBEDDED_CRUSADER_AUDIO_HZ", "Embedded Crusader audio {0,number,#}Hz", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid presentation sample {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CrusaderDemuxer.java</li>
    </ul>
    */
    public static LocalizedMessage CRUSADER_INVALID_PRESENTATION_SAMPLE(long a0) {
        return inter("CRUSADER_INVALID_PRESENTATION_SAMPLE", "Invalid presentation sample {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Inconsistent height {0,number,#} != {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CrusaderDemuxer.java</li>
    </ul>
    */
    public static LocalizedMessage INCONSISTENT_HEIGHT(int a0, int a1) {
        return inter("INCONSISTENT_HEIGHT", "Inconsistent height {0,number,#} != {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid Crusader audio id {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CrusaderDemuxer.java</li>
    </ul>
    */
    public static LocalizedMessage CRUSADER_INVALID_AUIDO_ID(long a0) {
        return inter("CRUSADER_INVALID_AUIDO_ID", "Invalid Crusader audio id {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame number {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>CrusaderDemuxer.java</li>
    </ul>
    */
    public static LocalizedMessage INVALID_FRAME_NUM(int a0) {
        return inter("INVALID_FRAME_NUM", "Invalid frame number {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame {0} chunk {1,number,#} missing.</pre>
    </td></tr></table>
    <ul>
       <li>DemuxedCrusaderFrame.java</li>
    </ul>
    */
    public static LocalizedMessage MISSING_CHUNK(@Nonnull jpsxdec.discitems.FrameNumber a0, int a1) {
        return inter("MISSING_CHUNK", "Frame {0} chunk {1,number,#} missing.", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Trying to replace a frame with missing chunks??</pre>
    </td></tr></table>
    <ul>
       <li>DemuxedCrusaderFrame.java</li>
       <li>DemuxedStrFrame.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_FRAME_TO_REPLACE_MISSING_CHUNKS() {
        return inter("CMD_FRAME_TO_REPLACE_MISSING_CHUNKS", "Trying to replace a frame with missing chunks??");
    }

    /**
    <table border="1"><tr><td>
    <pre>!!! New frame {0} demux size {1,number,#} &gt; max source {2,number,#} !!!</pre>
    </td></tr></table>
    <ul>
       <li>DemuxedCrusaderFrame.java</li>
       <li>ReplaceFrame.java</li>
       <li>BitStreamUncompressor_Iki.java</li>
       <li>BitStreamUncompressor_Lain.java</li>
       <li>BitStreamUncompressor_STRv2.java</li>
    </ul>
    */
    public static LocalizedMessage NEW_FRAME_DOES_NOT_FIT(@Nonnull jpsxdec.discitems.FrameNumber a0, int a1, int a2) {
        return inter("NEW_FRAME_DOES_NOT_FIT", "!!! New frame {0} demux size {1,number,#} > max source {2,number,#} !!!", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc format does not match what index says &quot;{0}&quot; != &quot;{1}&quot;.</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage DISC_FORMAT_MISMATCH(@Nonnull jpsxdec.cdreaders.CdFileSectorReader a0, @Nonnull String a1) {
        return inter("DISC_FORMAT_MISMATCH", "Disc format does not match what index says \"{0}\" != \"{1}\".", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Missing proper index header.</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_HEADER_MISSING() {
        return inter("INDEX_HEADER_MISSING", "Missing proper index header.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error while indexing disc</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEXING_ERROR() {
        return inter("INDEXING_ERROR", "Error while indexing disc");
    }

    /**
    <table border="1"><tr><td>
    <pre>Non-continuous sector header number: {0,number,#} -&gt; {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_SECTOR_HEADER_NUM_BREAK(int a0, int a1) {
        return inter("INDEX_SECTOR_HEADER_NUM_BREAK", "Non-continuous sector header number: {0,number,#} -> {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to parse line: {0}</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_PARSE_LINE_FAIL(@Nonnull String a0) {
        return inter("INDEX_PARSE_LINE_FAIL", "Failed to parse line: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to do anything with {0}</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_UNHANDLED_LINE(@Nonnull jpsxdec.discitems.SerializedDiscItem a0) {
        return inter("INDEX_UNHANDLED_LINE", "Failed to do anything with {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} / {1,number,#} {2,number,#} items found</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_SECTOR_ITEM_PROGRESS(int a0, int a1, int a2) {
        return inter("INDEX_SECTOR_ITEM_PROGRESS", "Sector {0,number,#} / {1,number,#} {2,number,#} items found", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} Lines that begin with {0} are ignored</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_COMMENT(@Nonnull String a0) {
        return inter("INDEX_COMMENT", "{0} Lines that begin with {0} are ignored", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} rejected {1}</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_REBUILD_PARENT_REJECTED_CHILD(@Nonnull jpsxdec.discitems.DiscItem a0, @Nonnull jpsxdec.discitems.DiscItem a1) {
        return inter("INDEX_REBUILD_PARENT_REJECTED_CHILD", "{0} rejected {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} is Mode 1 found among Mode 2 sectors</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_MODE1_AMONG_MODE2(int a0) {
        return inter("INDEX_MODE1_AMONG_MODE2", "Sector {0,number,#} is Mode 1 found among Mode 2 sectors", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Index is missing source CD file and no source file was supplied.</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndex.java</li>
    </ul>
    */
    public static LocalizedMessage INDEX_NO_CD() {
        return inter("INDEX_NO_CD", "Index is missing source CD file and no source file was supplied.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Ignoring a silent XA audio stream that is only 1 sector long at sector {0,number,#}, channel {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscIndexerXaAudio.java</li>
    </ul>
    */
    public static LocalizedMessage IGNORING_SILENT_XA_SECTOR(int a0, int a1) {
        return inter("IGNORING_SILENT_XA_SECTOR", "Ignoring a silent XA audio stream that is only 1 sector long at sector {0,number,#}, channel {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Video</pre>
    </td></tr></table>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage ITEM_TYPE_VIDEO() {
        return inter("ITEM_TYPE_VIDEO", "Video");
    }

    /**
    <table border="1"><tr><td>
    <pre>File</pre>
    </td></tr></table>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage ITEM_TYPE_FILE() {
        return inter("ITEM_TYPE_FILE", "File");
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio</pre>
    </td></tr></table>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage ITEM_TYPE_AUDIO() {
        return inter("ITEM_TYPE_AUDIO", "Audio");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image</pre>
    </td></tr></table>
    <ul>
       <li>DiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage ITEM_TYPE_IMAGE() {
        return inter("ITEM_TYPE_IMAGE", "Image");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#} fps = {4,time,m:ss}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemCrusader.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_CRUSADER_VID_DETAILS(int a0, int a1, int a2, int a3, @Nonnull Date a4) {
        return inter("GUI_CRUSADER_VID_DETAILS", "{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#} fps = {4,time,m:ss}", a0, a1, a2, a3, a4);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} bytes</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_ISOFILE_DETAILS(long a0) {
        return inter("GUI_ISOFILE_DETAILS", "{0} bytes", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>-iso save as 2048 sectors (default raw 2352 sectors)</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ISOFILE_ISO_HELP() {
        return inter("CMD_ISOFILE_ISO_HELP", "-iso save as 2048 sectors (default raw 2352 sectors)");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save raw:</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_ISOFILE_SAVE_RAW_LABEL() {
        return inter("GUI_ISOFILE_SAVE_RAW_LABEL", "Save raw:");
    }

    /**
    <table border="1"><tr><td>
    <pre>[no options available]</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ISOFILE_HELP_NO_OPTIONS() {
        return inter("CMD_ISOFILE_HELP_NO_OPTIONS", "[no options available]");
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving with raw sectors</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ISOFILE_SAVING_RAW() {
        return inter("CMD_ISOFILE_SAVING_RAW", "Saving with raw sectors");
    }

    /**
    <table border="1"><tr><td>
    <pre>Saving with iso sectors</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ISOFILE_SAVING_ISO() {
        return inter("CMD_ISOFILE_SAVING_ISO", "Saving with iso sectors");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save as:</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemISO9660File.java</li>
       <li>AudioSaverBuilderGui.java</li>
       <li>TimSaverBuilderGui.java</li>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_AS_LABEL() {
        return inter("GUI_SAVE_AS_LABEL", "Save as:");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,time,m:ss}, {1,number,#} Hz Stereo</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemSquareAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SQUARE_AUDIO_DETAILS(@Nonnull Date a0, int a1) {
        return inter("GUI_SQUARE_AUDIO_DETAILS", "{0,time,m:ss}, {1,number,#} Hz Stereo", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss} (or {5,number,#.###} fps = {6,time,m:ss})</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemStrVideoStream.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_STR_VIDEO_DETAILS_UNKNOWN_FPS(int a0, int a1, int a2, double a3, @Nonnull Date a4, double a5, @Nonnull Date a6) {
        return inter("GUI_STR_VIDEO_DETAILS_UNKNOWN_FPS", "{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss} (or {5,number,#.###} fps = {6,time,m:ss})", a0, a1, a2, a3, a4, a5, a6);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemStrVideoStream.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_STR_VIDEO_DETAILS(int a0, int a1, int a2, double a3, @Nonnull Date a4) {
        return inter("GUI_STR_VIDEO_DETAILS", "{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#.###} fps = {4,time,m:ss}", a0, a1, a2, a3, a4);
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing {0,number,#} bytes to sector {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemTim.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_TIM_REPLACE_SECTOR_BYTES(int a0, int a1) {
        return inter("CMD_TIM_REPLACE_SECTOR_BYTES", "Writing {0,number,#} bytes to sector {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}, Palettes: {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemTim.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_TIM_IMAGE_DETAILS(int a0, int a1, int a2) {
        return inter("GUI_TIM_IMAGE_DETAILS", "{0,number,#}x{1,number,#}, Palettes: {2,number,#}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>New TIM dimensions ({0,number,#}x{1,number,#}) do not match existing TIM dimensions ({2,number,#}x{3,number,#}).</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemTim.java</li>
    </ul>
    */
    public static LocalizedMessage TIM_REPLACE_DIMENSIONS_MISMATCH(int a0, int a1, int a2, int a3) {
        return inter("TIM_REPLACE_DIMENSIONS_MISMATCH", "New TIM dimensions ({0,number,#}x{1,number,#}) do not match existing TIM dimensions ({2,number,#}x{3,number,#}).", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to replace a multi-paletted TIM with a simple image</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemTim.java</li>
    </ul>
    */
    public static LocalizedMessage TIM_REPLACE_MULTI_CLUT_UNABLE() {
        return inter("TIM_REPLACE_MULTI_CLUT_UNABLE", "Unable to replace a multi-paletted TIM with a simple image");
    }

    /**
    <table border="1"><tr><td>
    <pre>Patching sector</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PATCHING_SECTOR() {
        return inter("CMD_PATCHING_SECTOR", "Patching sector");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} field has invalid value: {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage FIELD_HAS_INVALID_VALUE_NUM(@Nonnull String a0, int a1) {
        return inter("FIELD_HAS_INVALID_VALUE_NUM", "{0} field has invalid value: {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} field has invalid value: {1}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage FIELD_HAS_INVALID_VALUE_STR(@Nonnull String a0, @Nonnull String a1) {
        return inter("FIELD_HAS_INVALID_VALUE_STR", "{0} field has invalid value: {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>with sector</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PATCHING_WITH_SECTOR() {
        return inter("CMD_PATCHING_WITH_SECTOR", "with sector");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,time,m:ss}, {1,number,#} Hz {2,choice,1#Mono|2#Stereo}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_XA_DESCRIPTION(@Nonnull Date a0, int a1, int a2) {
        return inter("GUI_XA_DESCRIPTION", "{0,time,m:ss}, {1,number,#} Hz {2,choice,1#Mono|2#Stereo}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Patching</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PATCHING_DISC_ITEM() {
        return inter("CMD_PATCHING_DISC_ITEM", "Patching");
    }

    /**
    <table border="1"><tr><td>
    <pre>with</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PATCHING_WITH_DISC_ITEM() {
        return inter("CMD_PATCHING_WITH_DISC_ITEM", "with");
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio file sample rate {0} does not match XA audio rate {1}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage XA_COPY_REPLACE_SAMPLE_RATE_MISMATCH(float a0, int a1) {
        return inter("XA_COPY_REPLACE_SAMPLE_RATE_MISMATCH", "Audio file sample rate {0} does not match XA audio rate {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio file is {0,choice,1#Mono|2#Stereo} and does not match XA audio {1,choice,1#Mono|2#Stereo}</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage XA_COPY_REPLACE_CHANNEL_MISMATCH(int a0, int a1) {
        return inter("XA_COPY_REPLACE_CHANNEL_MISMATCH", "Audio file is {0,choice,1#Mono|2#Stereo} and does not match XA audio {1,choice,1#Mono|2#Stereo}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Formats not equal</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage XA_ENCODE_REPLACE_FORMAT_MISMATCH() {
        return inter("XA_ENCODE_REPLACE_FORMAT_MISMATCH", "Formats not equal");
    }

    /**
    <table border="1"><tr><td>
    <pre>Source audio is exhaused, writing silence</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage XA_ENCODE_REPLACE_SRC_AUDIO_EXHAUSTED() {
        return inter("XA_ENCODE_REPLACE_SRC_AUDIO_EXHAUSTED", "Source audio is exhaused, writing silence");
    }

    /**
    <table border="1"><tr><td>
    <pre>End of XA used to replace, stopping</pre>
    </td></tr></table>
    <ul>
       <li>DiscItemXaAudioStream.java</li>
    </ul>
    */
    public static LocalizedMessage XA_COPY_REPLACE_SRC_XA_EXHAUSTED() {
        return inter("XA_COPY_REPLACE_SRC_XA_EXHAUSTED", "End of XA used to replace, stopping");
    }

    /**
    <table border="1"><tr><td>
    <pre>Chunks in frame {0} changed from {1,number,#} to {2,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>FrameDemuxer.java</li>
    </ul>
    */
    public static LocalizedMessage DEMUX_FRAME_CHUNKS_CHANGED_FROM_TO(@Nonnull jpsxdec.discitems.FrameNumber a0, int a1, int a2) {
        return inter("DEMUX_FRAME_CHUNKS_CHANGED_FROM_TO", "Chunks in frame {0} changed from {1,number,#} to {2,number,#}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Chunk number {0,number,#} is &gt;= chunks in frame {1}</pre>
    </td></tr></table>
    <ul>
       <li>FrameDemuxer.java</li>
    </ul>
    */
    public static LocalizedMessage DEMUX_CHUNK_NUM_GTE_CHUNKS_IN_FRAME(int a0, @Nonnull jpsxdec.discitems.FrameNumber a1) {
        return inter("DEMUX_CHUNK_NUM_GTE_CHUNKS_IN_FRAME", "Chunk number {0,number,#} is >= chunks in frame {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame number {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumber.java</li>
    </ul>
    */
    public static LocalizedMessage INVALID_FRAME_NUMBER(@Nonnull String a0) {
        return inter("INVALID_FRAME_NUMBER", "Invalid frame number {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame range {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumber.java</li>
    </ul>
    */
    public static LocalizedMessage INVALID_FRAME_RANGE(@Nonnull String a0) {
        return inter("INVALID_FRAME_RANGE", "Invalid frame range {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Index</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumberFormat.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_NUM_FMT_INDEX() {
        return inter("FRAME_NUM_FMT_INDEX", "Index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumberFormat.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_NUM_FMT_SECTOR() {
        return inter("FRAME_NUM_FMT_SECTOR", "Sector");
    }

    /**
    <table border="1"><tr><td>
    <pre>Header</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumberFormat.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_NUM_FMT_HEADER() {
        return inter("FRAME_NUM_FMT_HEADER", "Header");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumberFormatter.java</li>
    </ul>
    */
    public static LocalizedMessage FRM_NUM_FMTR_SECTOR(@Nonnull String a0) {
        return inter("FRM_NUM_FMTR_SECTOR", "Sector {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumberFormatter.java</li>
    </ul>
    */
    public static LocalizedMessage FRM_NUM_FMTR_FRAME(@Nonnull String a0) {
        return inter("FRM_NUM_FMTR_FRAME", "Frame {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame number format {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumberFormat.java</li>
    </ul>
    */
    public static LocalizedMessage INVALID_FRAME_NUMBER_FORMAT(@Nonnull String a0) {
        return inter("INVALID_FRAME_NUMBER_FORMAT", "Invalid frame number format {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Not expecting duplicate frame number {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumberFormatter.java</li>
    </ul>
    */
    public static LocalizedMessage NOT_EXPECTING_DUP_FRM_NUM(@Nonnull jpsxdec.discitems.FrameNumber a0) {
        return inter("NOT_EXPECTING_DUP_FRM_NUM", "Not expecting duplicate frame number {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Not expecting duplicate sector number {0}</pre>
    </td></tr></table>
    <ul>
       <li>FrameNumberFormatter.java</li>
    </ul>
    */
    public static LocalizedMessage NOT_EXPECTING_DUP_SECT_NUM(@Nonnull jpsxdec.discitems.FrameNumber a0) {
        return inter("NOT_EXPECTING_DUP_SECT_NUM", "Not expecting duplicate sector number {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>*Unsaved*</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_TITLE_UNSAVED_INDEX() {
        return inter("GUI_TITLE_UNSAVED_INDEX", "*Unsaved*");
    }

    /**
    <table border="1"><tr><td>
    <pre>Open and Analyze File</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_OPEN_ANALYZE_DISC_BTN() {
        return inter("GUI_OPEN_ANALYZE_DISC_BTN", "Open and Analyze File");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc image does not have raw headers -- audio may not be detected.</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_DISC_NO_RAW_HEADERS_WARNING() {
        return inter("GUI_DISC_NO_RAW_HEADERS_WARNING", "Disc image does not have raw headers -- audio may not be detected.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Select disc image or media file</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_OPEN_DISC_DIALOG_TITLE() {
        return inter("GUI_OPEN_DISC_DIALOG_TITLE", "Select disc image or media file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Open Index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_OPEN_INDEX_BTN() {
        return inter("GUI_OPEN_INDEX_BTN", "Open Index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Load index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_LOAD_INDEX_FILE_DIALOG_TITLE() {
        return inter("GUI_LOAD_INDEX_FILE_DIALOG_TITLE", "Load index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Opening index {0}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_OPENING_INDEX(@Nonnull java.io.File a0) {
        return inter("GUI_OPENING_INDEX", "Opening index {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to open index file</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage OPENING_INDEX_FAILED() {
        return inter("OPENING_INDEX_FAILED", "Unable to open index file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Issues loading index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_LOAD_ISSUES_DIALOG_TITLE() {
        return inter("GUI_INDEX_LOAD_ISSUES_DIALOG_TITLE", "Issues loading index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Loaded {0,number,#} items, but with issues.</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_LOAD_ISSUES(int a0) {
        return inter("GUI_INDEX_LOAD_ISSUES", "Loaded {0,number,#} items, but with issues.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Warnings: {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_LOAD_ISSUES_WARNINGS(int a0) {
        return inter("GUI_INDEX_LOAD_ISSUES_WARNINGS", "Warnings: {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Errors: {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_LOAD_ISSUES_ERRORS(int a0) {
        return inter("GUI_INDEX_LOAD_ISSUES_ERRORS", "Errors: {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>See {0} for details.</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_LOAD_ISSUES_SEE_FILE(@Nonnull String a0) {
        return inter("GUI_INDEX_LOAD_ISSUES_SEE_FILE", "See {0} for details.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Save Index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_INDEX_BTN() {
        return inter("GUI_SAVE_INDEX_BTN", "Save Index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_INDEX_FILE_DIALOG_TITLE() {
        return inter("GUI_SAVE_INDEX_FILE_DIALOG_TITLE", "Save index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error saving index</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_INDEX_ERR() {
        return inter("GUI_SAVE_INDEX_ERR", "Error saving index");
    }

    /**
    <table border="1"><tr><td>
    <pre>Directory:</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_DIRECTORY_LABEL() {
        return inter("GUI_DIRECTORY_LABEL", "Directory:");
    }

    /**
    <table border="1"><tr><td>
    <pre>...</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_DIR_CHOOSER_BTN() {
        return inter("GUI_DIR_CHOOSER_BTN", "...");
    }

    /**
    <table border="1"><tr><td>
    <pre>Select ...</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SELECT_BTN() {
        return inter("GUI_SELECT_BTN", "Select ...");
    }

    /**
    <table border="1"><tr><td>
    <pre>Collapse All</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_COLLAPSE_ALL_BTN() {
        return inter("GUI_COLLAPSE_ALL_BTN", "Collapse All");
    }

    /**
    <table border="1"><tr><td>
    <pre>Expand All</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_EXPAND_ALL_BTN() {
        return inter("GUI_EXPAND_ALL_BTN", "Expand All");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save All Selected</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_ALL_SELECTED_BTN() {
        return inter("GUI_SAVE_ALL_SELECTED_BTN", "Save All Selected");
    }

    /**
    <table border="1"><tr><td>
    <pre>Nothing is marked for saving.</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_NOTHING_IS_MARKED_FOR_SAVING() {
        return inter("GUI_NOTHING_IS_MARKED_FOR_SAVING", "Nothing is marked for saving.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Apply to all {0}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_APPLY_TO_ALL_BTN(@Nonnull jpsxdec.i18n.LocalizedMessage a0) {
        return inter("GUI_APPLY_TO_ALL_BTN", "Apply to all {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Applied settings to {0,number,#} items.</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_APPLIED_SETTINGS(int a0) {
        return inter("GUI_APPLIED_SETTINGS", "Applied settings to {0,number,#} items.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>The index has not been saved. Save index?</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_INDEX_PROMPT() {
        return inter("GUI_SAVE_INDEX_PROMPT", "The index has not been saved. Save index?");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save index?</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_INDEX_PROMPT_TITLE() {
        return inter("GUI_SAVE_INDEX_PROMPT_TITLE", "Save index?");
    }

    /**
    <table border="1"><tr><td>
    <pre>    Play    </pre>
    </td></tr></table>
    <p>Extra space to give the tab some width</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage PLAY_TAB() {
        return inter("PLAY_TAB", "    Play    ");
    }

    /**
    <table border="1"><tr><td>
    <pre>    Save    </pre>
    </td></tr></table>
    <p>Extra space to give the tab some width</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage SAVE_TAB() {
        return inter("SAVE_TAB", "    Save    ");
    }

    /**
    <table border="1"><tr><td>
    <pre>Pause</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_PAUSE_BTN() {
        return inter("GUI_PAUSE_BTN", "Pause");
    }

    /**
    <table border="1"><tr><td>
    <pre>File not found</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_FILE_NOT_FOUND() {
        return inter("GUI_FILE_NOT_FOUND", "File not found");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to open {0}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_UNABLE_TO_OPEN_FILE(@Nonnull java.io.File a0) {
        return inter("GUI_UNABLE_TO_OPEN_FILE", "Unable to open {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to open {0}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_UNABLE_TO_OPEN_STR(@Nonnull String a0) {
        return inter("GUI_UNABLE_TO_OPEN_STR", "Unable to open {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to read file</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_FAILED_TO_READ_FILE() {
        return inter("GUI_FAILED_TO_READ_FILE", "Failed to read file");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error opening {0}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_ERR_OPENING(@Nonnull String a0) {
        return inter("GUI_ERR_OPENING", "Error opening {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error opening {0}: {1}</pre>
    </td></tr></table>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_ERR_OPENING_EXCEPTION(@Nonnull String a0, @Nonnull String a1) {
        return inter("GUI_ERR_OPENING_EXCEPTION", "Error opening {0}: {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Bad error</pre>
    </td></tr></table>
    <p>Combine with GUI_UNHANDLED_ERROR?</p>
    <ul>
       <li>Gui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_BAD_ERROR() {
        return inter("GUI_BAD_ERROR", "Bad error");
    }

    /**
    <table border="1"><tr><td>
    <pre>CD images (*.iso, *.bin, *.img, *.mdf)</pre>
    </td></tr></table>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_CD_IMAGE_EXTENSIONS() {
        return inter("GUI_CD_IMAGE_EXTENSIONS", "CD images (*.iso, *.bin, *.img, *.mdf)");
    }

    /**
    <table border="1"><tr><td>
    <pre>Index files (*.idx)</pre>
    </td></tr></table>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_EXTENSION() {
        return inter("GUI_INDEX_EXTENSION", "Index files (*.idx)");
    }

    /**
    <table border="1"><tr><td>
    <pre>PlayStation video (*.str, *.mov, *.iki, *.ik2)</pre>
    </td></tr></table>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_PSX_VIDEO_EXTENSIONS() {
        return inter("GUI_PSX_VIDEO_EXTENSIONS", "PlayStation video (*.str, *.mov, *.iki, *.ik2)");
    }

    /**
    <table border="1"><tr><td>
    <pre>All compatible types</pre>
    </td></tr></table>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_ALL_COMPATIBLE_EXTENSIONS() {
        return inter("GUI_ALL_COMPATIBLE_EXTENSIONS", "All compatible types");
    }

    /**
    <table border="1"><tr><td>
    <pre>PlayStation/CD-i audio (*.xa, *.xai)</pre>
    </td></tr></table>
    <ul>
       <li>GuiFileFilters.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_XA_EXTENSION() {
        return inter("GUI_XA_EXTENSION", "PlayStation/CD-i audio (*.xa, *.xai)");
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
    public static LocalizedMessage GUI_TREE_NAME_COLUMN() {
        return inter("GUI_TREE_NAME_COLUMN", "");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sectors</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SECTORS_COLUMN() {
        return inter("GUI_SECTORS_COLUMN", "Sectors");
    }

    /**
    <table border="1"><tr><td>
    <pre>Type</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_TREE_TYPE_COLUMN() {
        return inter("GUI_TREE_TYPE_COLUMN", "Type");
    }

    /**
    <table border="1"><tr><td>
    <pre>none</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SELECT_NONE() {
        return inter("GUI_SELECT_NONE", "none");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Video</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SELECT_ALL_VIDEO() {
        return inter("GUI_SELECT_ALL_VIDEO", "all Video");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Files</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SELECT_ALL_FILES() {
        return inter("GUI_SELECT_ALL_FILES", "all Files");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Audio (excluding video audio)</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SELECT_ALL_AUIO_EX_VID() {
        return inter("GUI_SELECT_ALL_AUIO_EX_VID", "all Audio (excluding video audio)");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Audio (including video audio)</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SELECT_ALL_AUDIO_INC_VID() {
        return inter("GUI_SELECT_ALL_AUDIO_INC_VID", "all Audio (including video audio)");
    }

    /**
    <table border="1"><tr><td>
    <pre>all Images</pre>
    </td></tr></table>
    <ul>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SELECT_ALL_IMAGES() {
        return inter("GUI_SELECT_ALL_IMAGES", "all Images");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unnamed</pre>
    </td></tr></table>
    <ul>
       <li>IndexId.java</li>
    </ul>
    */
    public static LocalizedMessage UNNAMED_DISC_ITEM() {
        return inter("UNNAMED_DISC_ITEM", "Unnamed");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid id format: {0}</pre>
    </td></tr></table>
    <ul>
       <li>IndexId.java</li>
    </ul>
    */
    public static LocalizedMessage ID_FORMAT_INVALID(@Nonnull String a0) {
        return inter("ID_FORMAT_INVALID", "Invalid id format: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Warnings:</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_WARNINGS_LABEL() {
        return inter("GUI_INDEX_WARNINGS_LABEL", "Warnings:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Failure - See {0} for details</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_RESULT_FAILURE(@Nonnull String a0) {
        return inter("GUI_INDEX_RESULT_FAILURE", "Failure - See {0} for details", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Canceled</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_RESULT_CANCELED() {
        return inter("GUI_INDEX_RESULT_CANCELED", "Canceled");
    }

    /**
    <table border="1"><tr><td>
    <pre>Progress...</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_TITLE() {
        return inter("GUI_INDEX_TITLE", "Progress...");
    }

    /**
    <table border="1"><tr><td>
    <pre>Exception</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_EXCEPTION_DIALOG_TITLE() {
        return inter("GUI_INDEX_EXCEPTION_DIALOG_TITLE", "Exception");
    }

    /**
    <table border="1"><tr><td>
    <pre>Success with messages - See {0} for details</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_RESULT_OK_MSGS(@Nonnull String a0) {
        return inter("GUI_INDEX_RESULT_OK_MSGS", "Success with messages - See {0} for details", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Indexing:</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEXING_LABEL() {
        return inter("GUI_INDEXING_LABEL", "Indexing:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Errors:</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_ERRORS_LABEL() {
        return inter("GUI_INDEX_ERRORS_LABEL", "Errors:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Success!</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_INDEX_RESULT_SUCCESS() {
        return inter("GUI_INDEX_RESULT_SUCCESS", "Success!");
    }

    /**
    <table border="1"><tr><td>
    <pre>Cancel</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
       <li>SavingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_CANCEL_BTN() {
        return inter("GUI_CANCEL_BTN", "Cancel");
    }

    /**
    <table border="1"><tr><td>
    <pre>Close</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
       <li>SavingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_CLOSE_BTN() {
        return inter("GUI_CLOSE_BTN", "Close");
    }

    /**
    <table border="1"><tr><td>
    <pre>Start</pre>
    </td></tr></table>
    <ul>
       <li>IndexingGui.java</li>
       <li>SavingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_START_BTN() {
        return inter("GUI_START_BTN", "Start");
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
    public static LocalizedMessage GUI_UNHANDLED_ERROR() {
        return inter("GUI_UNHANDLED_ERROR", "Unhandled error");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unexpected end of file in {0}</pre>
    </td></tr></table>
    <ul>
       <li>IO.java</li>
    </ul>
    */
    public static LocalizedMessage UNEXPECTE_EOF_IN_FUNCTION(@Nonnull String a0) {
        return inter("UNEXPECTE_EOF_IN_FUNCTION", "Unexpected end of file in {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Folder {0} does not exist.</pre>
    </td></tr></table>
    <ul>
       <li>IO.java</li>
    </ul>
    */
    public static LocalizedMessage DIR_DOES_NOT_EXIST(@Nonnull String a0) {
        return inter("DIR_DOES_NOT_EXIST", "Folder {0} does not exist.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Cannot create directory over a file {0}</pre>
    </td></tr></table>
    <ul>
       <li>IO.java</li>
    </ul>
    */
    public static LocalizedMessage CANNOT_CREATE_DIR_OVER_FILE(@Nonnull java.io.File a0) {
        return inter("CANNOT_CREATE_DIR_OVER_FILE", "Cannot create directory over a file {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to create directory {0}</pre>
    </td></tr></table>
    <ul>
       <li>IO.java</li>
    </ul>
    */
    public static LocalizedMessage UNABLE_TO_CREATE_DIR(@Nonnull java.io.File a0) {
        return inter("UNABLE_TO_CREATE_DIR", "Unable to create directory {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>[MDEC] Run length out of bounds [{0,number,#}] in macroblock {1,number,#} ({2,number,#}, {3,number,#}) block {4,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>Mdec2Jpeg.java</li>
    </ul>
    */
    public static LocalizedMessage RLC_OOB_IN_MB_XY_BLOCK(int a0, int a1, int a2, int a3, int a4) {
        return inter("RLC_OOB_IN_MB_XY_BLOCK", "[MDEC] Run length out of bounds [{0,number,#}] in macroblock {1,number,#} ({2,number,#}, {3,number,#}) block {4,number,#}", a0, a1, a2, a3, a4);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error reading macro block {0,number,#} ({1,number,#},{2,number,#}) block {3,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>Mdec2Jpeg.java</li>
    </ul>
    */
    public static LocalizedMessage JPEG_ERR_READING_MB(int a0, int a1, int a2, int a3) {
        return inter("JPEG_ERR_READING_MB", "Error reading macro block {0,number,#} ({1,number,#},{2,number,#}) block {3,number,#}", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>low</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static LocalizedMessage QUALITY_FAST_COMMAND() {
        return inter("QUALITY_FAST_COMMAND", "low");
    }

    /**
    <table border="1"><tr><td>
    <pre>High quality (slower)</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static LocalizedMessage QUALITY_HIGH_DESCRIPTION() {
        return inter("QUALITY_HIGH_DESCRIPTION", "High quality (slower)");
    }

    /**
    <table border="1"><tr><td>
    <pre>Fast (lower quality)</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static LocalizedMessage QUALITY_FAST_DESCRIPTION() {
        return inter("QUALITY_FAST_DESCRIPTION", "Fast (lower quality)");
    }

    /**
    <table border="1"><tr><td>
    <pre>psx</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static LocalizedMessage QUALITY_PSX_COMMAND() {
        return inter("QUALITY_PSX_COMMAND", "psx");
    }

    /**
    <table border="1"><tr><td>
    <pre>Emulate PSX (low) quality</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static LocalizedMessage QUALITY_PSX_DESCRIPTION() {
        return inter("QUALITY_PSX_DESCRIPTION", "Emulate PSX (low) quality");
    }

    /**
    <table border="1"><tr><td>
    <pre>high</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecodeQuality.java</li>
    </ul>
    */
    public static LocalizedMessage QUALITY_HIGH_COMMAND() {
        return inter("QUALITY_HIGH_COMMAND", "high");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bicubic</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_BICUBIC_DESCRIPTION() {
        return inter("CHROMA_UPSAMPLE_BICUBIC_DESCRIPTION", "Bicubic");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bicubic</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_BICUBIC_CMDLINE() {
        return inter("CHROMA_UPSAMPLE_BICUBIC_CMDLINE", "Bicubic");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bell</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_BELL_DESCRIPTION() {
        return inter("CHROMA_UPSAMPLE_BELL_DESCRIPTION", "Bell");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bell</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_BELL_CMDLINE() {
        return inter("CHROMA_UPSAMPLE_BELL_CMDLINE", "Bell");
    }

    /**
    <table border="1"><tr><td>
    <pre>Nearest Neighbor</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_NEAR_NEIGHBOR_DESCRIPTION() {
        return inter("CHROMA_UPSAMPLE_NEAR_NEIGHBOR_DESCRIPTION", "Nearest Neighbor");
    }

    /**
    <table border="1"><tr><td>
    <pre>NearestNeighbor</pre>
    </td></tr></table>
    <p>1 word (no spaces) user can type on command-line</p>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_NEAR_NEIGHBOR_CMDLINE() {
        return inter("CHROMA_UPSAMPLE_NEAR_NEIGHBOR_CMDLINE", "NearestNeighbor");
    }

    /**
    <table border="1"><tr><td>
    <pre>Lanczos3</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_LANCZOS3_DESCRIPTION() {
        return inter("CHROMA_UPSAMPLE_LANCZOS3_DESCRIPTION", "Lanczos3");
    }

    /**
    <table border="1"><tr><td>
    <pre>Lanczos3</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_LANCZOS3_CMDLINE() {
        return inter("CHROMA_UPSAMPLE_LANCZOS3_CMDLINE", "Lanczos3");
    }

    /**
    <table border="1"><tr><td>
    <pre>Mitchell</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_MITCHELL_DESCRIPTION() {
        return inter("CHROMA_UPSAMPLE_MITCHELL_DESCRIPTION", "Mitchell");
    }

    /**
    <table border="1"><tr><td>
    <pre>Mitchell</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_MITCHELL_CMDLINE() {
        return inter("CHROMA_UPSAMPLE_MITCHELL_CMDLINE", "Mitchell");
    }

    /**
    <table border="1"><tr><td>
    <pre>Hermite</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_HERMITE_DESCRIPTION() {
        return inter("CHROMA_UPSAMPLE_HERMITE_DESCRIPTION", "Hermite");
    }

    /**
    <table border="1"><tr><td>
    <pre>Hermite</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_HERMITE_CMDLINE() {
        return inter("CHROMA_UPSAMPLE_HERMITE_CMDLINE", "Hermite");
    }

    /**
    <table border="1"><tr><td>
    <pre>BSpline</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_BSPLINE_DESCRIPTION() {
        return inter("CHROMA_UPSAMPLE_BSPLINE_DESCRIPTION", "BSpline");
    }

    /**
    <table border="1"><tr><td>
    <pre>BSpline</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_BSPLINE_CMDLINE() {
        return inter("CHROMA_UPSAMPLE_BSPLINE_CMDLINE", "BSpline");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bilinear</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_BILINEAR_DESCRIPTION() {
        return inter("CHROMA_UPSAMPLE_BILINEAR_DESCRIPTION", "Bilinear");
    }

    /**
    <table border="1"><tr><td>
    <pre>Bilinear</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_BILINEAR_CMDLINE() {
        return inter("CHROMA_UPSAMPLE_BILINEAR_CMDLINE", "Bilinear");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} ({1})</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double_interpolate.java</li>
    </ul>
    */
    public static LocalizedMessage CHROMA_UPSAMPLE_CMDLINE_HELP(@Nonnull jpsxdec.i18n.LocalizedMessage a0, @Nonnull jpsxdec.i18n.LocalizedMessage a1) {
        return inter("CHROMA_UPSAMPLE_CMDLINE_HELP", "{0} ({1})", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error decoding macro block {0,number,#} block {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double.java</li>
       <li>MdecDecoder_int.java</li>
    </ul>
    */
    public static LocalizedMessage BLOCK_DECODE_ERR(int a0, int a1) {
        return inter("BLOCK_DECODE_ERR", "Error decoding macro block {0,number,#} block {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>[MDEC] Run length out of bounds [{0,number,#}] in macroblock {1,number,#} ({2,number,#}, {3,number,#}) block {4,number,#} ({5})</pre>
    </td></tr></table>
    <ul>
       <li>MdecDecoder_double.java</li>
       <li>MdecDecoder_int.java</li>
    </ul>
    */
    public static LocalizedMessage RLC_OOB_IN_BLOCK_NAME(int a0, int a1, int a2, int a3, int a4, @Nonnull String a5) {
        return inter("RLC_OOB_IN_BLOCK_NAME", "[MDEC] Run length out of bounds [{0,number,#}] in macroblock {1,number,#} ({2,number,#}, {3,number,#}) block {4,number,#} ({5})", a0, a1, a2, a3, a4, a5);
    }

    /**
    <table border="1"><tr><td>
    <pre>Run length out of bounds: {0,number,#}</pre>
    </td></tr></table>
    <p>Currently unused</p>
    */
    public static LocalizedMessage RLC_OOB(int a0) {
        return inter("RLC_OOB", "Run length out of bounds: {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unexpected end of stream in block {0,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>MdecInputStreamReader.java</li>
    </ul>
    */
    public static LocalizedMessage UNEXPECTED_STREAM_END_IN_BLOCK(int a0) {
        return inter("UNEXPECTED_STREAM_END_IN_BLOCK", "Unexpected end of stream in block {0,number,#}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacement frame dimensions do not match frame to replace: {0,number,#}x{1,number} != {2,number,#}x{3,number}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrame.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_FRAME_DIMENSIONS_MISMATCH(int a0, int a1, int a2, int a3) {
        return inter("REPLACE_FRAME_DIMENSIONS_MISMATCH", "Replacement frame dimensions do not match frame to replace: {0,number,#}x{1,number} != {2,number,#}x{3,number}", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to find {0}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrame.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_UNABLE_TO_FIND_FILE(@Nonnull java.io.File a0) {
        return inter("REPLACE_UNABLE_TO_FIND_FILE", "Unable to find {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to read {0} as an image. Did you forget ''format''?</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrame.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_FILE_NOT_JAVA_IMAGE(@Nonnull java.io.File a0) {
        return inter("REPLACE_FILE_NOT_JAVA_IMAGE", "Unable to read {0} as an image. Did you forget ''format''?", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to identify frame type</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrame.java</li>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_UNABLE_TO_IDENTIFY_FRAME_TYPE() {
        return inter("CMD_UNABLE_TO_IDENTIFY_FRAME_TYPE", "Unable to identify frame type");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to compress frame {0} small enough to fit in {1,number,#} bytes!!!</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrame.java</li>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_UNABLE_TO_COMPRESS_FRAME_SMALL_ENOUGH(@Nonnull jpsxdec.discitems.FrameNumber a0, int a1) {
        return inter("CMD_UNABLE_TO_COMPRESS_FRAME_SMALL_ENOUGH", "Unable to compress frame {0} small enough to fit in {1,number,#} bytes!!!", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>No differences found, skipping.</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_NO_DIFFERENCE_SKIPPING() {
        return inter("CMD_NO_DIFFERENCE_SKIPPING", "No differences found, skipping.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Found {0,number,#} different macroblocks (16x16)</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_REPLACE_FOUND_DIFFERENT_MACRO_BLOCKS(int a0) {
        return inter("CMD_REPLACE_FOUND_DIFFERENT_MACRO_BLOCKS", "Found {0,number,#} different macroblocks (16x16)", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Warning: Entire frame is different.</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_ENTIRE_FRAME_DIFFERENT() {
        return inter("CMD_ENTIRE_FRAME_DIFFERENT", "Warning: Entire frame is different.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to load {0} as image</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_UNABLE_READ_IMAGE(@Nonnull java.io.File a0) {
        return inter("REPLACE_UNABLE_READ_IMAGE", "Unable to load {0} as image", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacement frame dimensions smaller than source frame</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFramePartial.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_FRAME_DIMENSIONS_TOO_SMALL() {
        return inter("REPLACE_FRAME_DIMENSIONS_TOO_SMALL", "Replacement frame dimensions smaller than source frame");
    }

    /**
    <table border="1"><tr><td>
    <pre>Replacing with {0}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrames.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_REPLACING_FRAME_WITH_FILE(@Nonnull java.io.File a0) {
        return inter("CMD_REPLACING_FRAME_WITH_FILE", "Replacing with {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame {0}:</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrames.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_REPLACING_FRAME_NUM(@Nonnull jpsxdec.discitems.FrameNumber a0) {
        return inter("CMD_REPLACING_FRAME_NUM", "Frame {0}:", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid root node {0}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrames.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_REPLACE_XML_INVALID_ROOT_NODE(@Nonnull String a0) {
        return inter("CMD_REPLACE_XML_INVALID_ROOT_NODE", "Invalid root node {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid version {0}</pre>
    </td></tr></table>
    <ul>
       <li>ReplaceFrames.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_REPLACE_XML_INVALID_VERSION(@Nonnull String a0) {
        return inter("CMD_REPLACE_XML_INVALID_VERSION", "Invalid version {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Complete | See {0} for details</pre>
    </td></tr></table>
    <ul>
       <li>SavingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_STATUS_OVERALL_COMPLETE(@Nonnull String a0) {
        return inter("GUI_SAVE_STATUS_OVERALL_COMPLETE", "Complete | See {0} for details", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Canceled | See {0} for details</pre>
    </td></tr></table>
    <ul>
       <li>SavingGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_STATUS_OVERALL_CANCELED(@Nonnull String a0) {
        return inter("GUI_SAVE_STATUS_OVERALL_CANCELED", "Canceled | See {0} for details", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed!</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_STATUS_FAILED() {
        return inter("GUI_SAVE_STATUS_FAILED", "Failed!");
    }

    /**
    <table border="1"><tr><td>
    <pre>Canceled</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_STATUS_CANCELED() {
        return inter("GUI_SAVE_STATUS_CANCELED", "Canceled");
    }

    /**
    <table border="1"><tr><td>
    <pre>Source</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SRC_COLUMN() {
        return inter("GUI_SRC_COLUMN", "Source");
    }

    /**
    <table border="1"><tr><td>
    <pre>Err</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_ERR_COLUMN() {
        return inter("GUI_ERR_COLUMN", "Err");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save As</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_AS_COLUMN() {
        return inter("GUI_SAVE_AS_COLUMN", "Save As");
    }

    /**
    <table border="1"><tr><td>
    <pre>Waiting</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_STATUS_WAITING() {
        return inter("GUI_SAVE_STATUS_WAITING", "Waiting");
    }

    /**
    <table border="1"><tr><td>
    <pre>Message</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_MESSAGE_COLUMN() {
        return inter("GUI_MESSAGE_COLUMN", "Message");
    }

    /**
    <table border="1"><tr><td>
    <pre>Done</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_STATUS_DONE() {
        return inter("GUI_SAVE_STATUS_DONE", "Done");
    }

    /**
    <table border="1"><tr><td>
    <pre>Progress</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_PROGRESS_COLUMN() {
        return inter("GUI_PROGRESS_COLUMN", "Progress");
    }

    /**
    <table border="1"><tr><td>
    <pre>Warn</pre>
    </td></tr></table>
    <ul>
       <li>SavingGuiTable.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_WARN_COLUMN() {
        return inter("GUI_WARN_COLUMN", "Warn");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame type is not v2 or v3</pre>
    </td></tr></table>
    <ul>
       <li>SectorAbstractVideo.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_FRAME_TYPE_NOT_V2_V3() {
        return inter("REPLACE_FRAME_TYPE_NOT_V2_V3", "Frame type is not v2 or v3");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame type is not v2</pre>
    </td></tr></table>
    <ul>
       <li>SectorFF9.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_FRAME_TYPE_NOT_V2() {
        return inter("REPLACE_FRAME_TYPE_NOT_V2", "Frame type is not v2");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame type is not iki</pre>
    </td></tr></table>
    <ul>
       <li>SectorIkiVideo.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_FRAME_TYPE_NOT_IKI() {
        return inter("REPLACE_FRAME_TYPE_NOT_IKI", "Frame type is not iki");
    }

    /**
    <table border="1"><tr><td>
    <pre>Iki frame dimentions do not match sector dimensions: {0,number,#}x{1,number,#} != {2,number,#}x{3,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>SectorIkiVideo.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_FRAME_IKI_DIMENSIONS_MISMATCH(int a0, int a1, int a2, int a3) {
        return inter("REPLACE_FRAME_IKI_DIMENSIONS_MISMATCH", "Iki frame dimentions do not match sector dimensions: {0,number,#}x{1,number,#} != {2,number,#}x{3,number,#}", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>Incompatible frame data for Lain</pre>
    </td></tr></table>
    <ul>
       <li>SectorLainVideo.java</li>
    </ul>
    */
    public static LocalizedMessage REPLACE_FRAME_TYPE_NOT_LAIN() {
        return inter("REPLACE_FRAME_TYPE_NOT_LAIN", "Incompatible frame data for Lain");
    }

    /**
    <table border="1"><tr><td>
    <pre>Empty serialized string</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage EMPTY_SERIALIZED_STRING() {
        return inter("EMPTY_SERIALIZED_STRING", "Empty serialized string");
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to convert serialized field to int: {0}</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage SERIALIZATION_FAILED_TO_CONVERT_TO_INT(@Nonnull String a0) {
        return inter("SERIALIZATION_FAILED_TO_CONVERT_TO_INT", "Failed to convert serialized field to int: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to convert serialized field to long: {0}</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage SERIALIZATION_FAILED_TO_CONVERT_TO_LONG(@Nonnull String a0) {
        return inter("SERIALIZATION_FAILED_TO_CONVERT_TO_LONG", "Failed to convert serialized field to long: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Improperly formatted field serialization: {0}</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage SERIALIZATION_FIELD_IMPROPERLY_FORMATTED(@Nonnull String a0) {
        return inter("SERIALIZATION_FIELD_IMPROPERLY_FORMATTED", "Improperly formatted field serialization: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Failed to convert serialized value to range: {0}</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage SERIALIZATION_FAILED_TO_CONVERT_TO_RANGE(@Nonnull String a0) {
        return inter("SERIALIZATION_FAILED_TO_CONVERT_TO_RANGE", "Failed to convert serialized value to range: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Line missing vital fields {0}</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage SERIALIZATION_MISSING_REQUIRED_FIELDS(@Nonnull String a0) {
        return inter("SERIALIZATION_MISSING_REQUIRED_FIELDS", "Line missing vital fields {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0} field not found.</pre>
    </td></tr></table>
    <ul>
       <li>SerializedDiscItem.java</li>
    </ul>
    */
    public static LocalizedMessage FIELD_NOT_FOUND(@Nonnull String a0) {
        return inter("FIELD_NOT_FOUND", "{0} field not found.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Square ADPCM Sound Parameter Filter Index &gt; 4 ({0,number,#}) [sound parameter 0x{1} at {2,number,#}]</pre>
    </td></tr></table>
    <ul>
       <li>SquareAdpcmDecoder.java</li>
    </ul>
    */
    public static LocalizedMessage SQUARE_ADPCM_FILTER_IDX_GT_4_FP(int a0, @Nonnull String a1, long a2) {
        return inter("SQUARE_ADPCM_FILTER_IDX_GT_4_FP", "Square ADPCM Sound Parameter Filter Index > 4 ({0,number,#}) [sound parameter 0x{1} at {2,number,#}]", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Square ADPCM Sound Parameter Filter Index &gt; 4 ({0,number,#}) [sound parameter 0x{1}]</pre>
    </td></tr></table>
    <ul>
       <li>SquareAdpcmDecoder.java</li>
    </ul>
    */
    public static LocalizedMessage SQUARE_ADPCM_FILTER_IDX_GT_4(int a0, @Nonnull String a1) {
        return inter("SQUARE_ADPCM_FILTER_IDX_GT_4", "Square ADPCM Sound Parameter Filter Index > 4 ({0,number,#}) [sound parameter 0x{1}]", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unexpected end of audio data</pre>
    </td></tr></table>
    <ul>
       <li>SquareAdpcmDecoder.java</li>
       <li>XaAdpcmDecoder.java</li>
    </ul>
    */
    public static LocalizedMessage UNEXPECTED_END_OF_AUDIO() {
        return inter("UNEXPECTED_END_OF_AUDIO", "Unexpected end of audio data");
    }

    /**
    <table border="1"><tr><td>
    <pre>Copy to clipboard</pre>
    </td></tr></table>
    <ul>
       <li>TimPaletteSelector.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_COPY_TO_CLIPBOARD_TOOLTIP() {
        return inter("GUI_COPY_TO_CLIPBOARD_TOOLTIP", "Copy to clipboard");
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to write image file {0} for palette {1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PALETTE_IMAGE_SAVE_FAIL(@Nonnull java.io.File a0, int a1) {
        return inter("CMD_PALETTE_IMAGE_SAVE_FAIL", "Unable to write image file {0} for palette {1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Format: {0}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_TIM_SAVE_FORMAT(@Nonnull String a0) {
        return inter("CMD_TIM_SAVE_FORMAT", "Format: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid format {0}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_TIM_SAVE_FORMAT_INVALID(@Nonnull String a0) {
        return inter("CMD_TIM_SAVE_FORMAT_INVALID", "Invalid format {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid list of palettes {0}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PALETTE_LIST_INVALID(@Nonnull String a0) {
        return inter("CMD_PALETTE_LIST_INVALID", "Invalid list of palettes {0}", a0);
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
    public static LocalizedMessage CMD_TIM_PAL() {
        return inter("CMD_TIM_PAL", "-pal <#,#-#>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Palettes to save (default all).</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_TIM_PAL_HELP() {
        return inter("CMD_TIM_PAL_HELP", "Palettes to save (default all).");
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
    public static LocalizedMessage CMD_TIM_IF() {
        return inter("CMD_TIM_IF", "-imgfmt,-if <format>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Output image format (default {0}). Options:</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_TIM_IF_HELP(@Nonnull String a0) {
        return inter("CMD_TIM_IF_HELP", "Output image format (default {0}). Options:", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Palette files: {0}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_PALETTE_FILES(@Nonnull jpsxdec.i18n.LocalizedMessage a0) {
        return inter("CMD_PALETTE_FILES", "Palette files: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#} files between {1}-{2}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage TIM_OUTPUT_FILES(int a0, @Nonnull String a1, @Nonnull String a2) {
        return inter("TIM_OUTPUT_FILES", "{0,number,#} files between {1}-{2}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>None</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage TIM_OUTPUT_FILES_NONE() {
        return inter("TIM_OUTPUT_FILES_NONE", "None");
    }

    /**
    <table border="1"><tr><td>
    <pre>Format:</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_TIM_SAVE_FORMAT_LABEL() {
        return inter("GUI_TIM_SAVE_FORMAT_LABEL", "Format:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Error reading TIM preview
{0}</pre>
    </td></tr></table>
    <ul>
       <li>TimSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_ERR_READING_TIM_PREVIEW(@Nonnull String a0) {
        return inter("GUI_ERR_READING_TIM_PREVIEW", "Error reading TIM preview\n{0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1}</pre>
    </td></tr></table>
    <ul>
       <li>UserFriendlyLogger.java</li>
    </ul>
    */
    public static LocalizedMessage USER_LOG_MESSAGE(@Nonnull String a0, @Nonnull String a1) {
        return inter("USER_LOG_MESSAGE", "[{0}] {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1} {2}</pre>
    </td></tr></table>
    <ul>
       <li>UserFriendlyLogger.java</li>
    </ul>
    */
    public static LocalizedMessage USER_LOG_MESSAGE_EXCEPTION(@Nonnull String a0, @Nonnull String a1, @Nonnull String a2) {
        return inter("USER_LOG_MESSAGE_EXCEPTION", "[{0}] {1} {2}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1} {2} : {3}</pre>
    </td></tr></table>
    <ul>
       <li>UserFriendlyLogger.java</li>
    </ul>
    */
    public static LocalizedMessage USER_LOG_MESSAGE_EXCEPTION_MSG(@Nonnull String a0, @Nonnull String a1, @Nonnull String a2, @Nonnull String a3) {
        return inter("USER_LOG_MESSAGE_EXCEPTION_MSG", "[{0}] {1} {2} : {3}", a0, a1, a2, a3);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1}</pre>
    </td></tr></table>
    <ul>
       <li>UserFriendlyLogger.java</li>
    </ul>
    */
    public static LocalizedMessage USER_LOG_EXCEPTION(@Nonnull String a0, @Nonnull String a1) {
        return inter("USER_LOG_EXCEPTION", "[{0}] {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>[{0}] {1} : {2}</pre>
    </td></tr></table>
    <ul>
       <li>UserFriendlyLogger.java</li>
    </ul>
    */
    public static LocalizedMessage USER_LOG_EXCEPTION_MSG(@Nonnull String a0, @Nonnull String a1, @Nonnull String a2) {
        return inter("USER_LOG_EXCEPTION_MSG", "[{0}] {1} : {2}", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing {0,number,#} samples of silence to align audio/video playback.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage WRITING_SILECE_TO_SYNC_AV(long a0) {
        return inter("WRITING_SILECE_TO_SYNC_AV", "Writing {0,number,#} samples of silence to align audio/video playback.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error with frame {0}: Unable to determine frame type.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(@Nonnull jpsxdec.discitems.FrameNumber a0) {
        return inter("UNABLE_TO_DETERMINE_FRAME_TYPE_FRM", "Error with frame {0}: Unable to determine frame type.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame {0} is ahead of reading by {1,number,#} frame(s).</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_NUM_AHEAD_OF_READING(@Nonnull jpsxdec.discitems.FrameNumber a0, int a1) {
        return inter("FRAME_NUM_AHEAD_OF_READING", "Frame {0} is ahead of reading by {1,number,#} frame(s).", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame is ahead of reading by {0,number,#} frame(s).</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_AHEAD_OF_READING(int a0) {
        return inter("FRAME_AHEAD_OF_READING", "Frame is ahead of reading by {0,number,#} frame(s).", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Unable to write frame file {0} for frame {1}</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_FILE_WRITE_UNABLE(@Nonnull java.io.File a0, @Nonnull jpsxdec.discitems.FrameNumber a1) {
        return inter("FRAME_FILE_WRITE_UNABLE", "Unable to write frame file {0} for frame {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error closing file {0} for frame {1}</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_FILE_CLOSE_ERR(@Nonnull java.io.File a0, @Nonnull jpsxdec.discitems.FrameNumber a1) {
        return inter("FRAME_FILE_CLOSE_ERR", "Error closing file {0} for frame {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Video format identified as {0}</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage VIDEO_FMT_IDENTIFIED(@Nonnull String a0) {
        return inter("VIDEO_FMT_IDENTIFIED", "Video format identified as {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error uncompressing frame {0}</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_UNCOMPRESS_ERR(@Nonnull jpsxdec.discitems.FrameNumber a0) {
        return inter("FRAME_UNCOMPRESS_ERR", "Error uncompressing frame {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Writing {0,number,#} blank frame(s) to align audio/video playback.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage WRITING_BLANK_FRAMES_TO_ALIGN_AV(int a0) {
        return inter("WRITING_BLANK_FRAMES_TO_ALIGN_AV", "Writing {0,number,#} blank frame(s) to align audio/video playback.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error writing file {0} for frame {1}</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage FRAME_WRITE_ERR(@Nonnull java.io.File a0, @Nonnull jpsxdec.discitems.FrameNumber a1) {
        return inter("FRAME_WRITE_ERR", "Error writing file {0} for frame {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Adding {0,number,#} samples to keep audio in sync.</pre>
    </td></tr></table>
    <ul>
       <li>VDP.java</li>
    </ul>
    */
    public static LocalizedMessage WRITING_SILENCE_TO_KEEP_AV_SYNCED(long a0) {
        return inter("WRITING_SILENCE_TO_KEEP_AV_SYNCED", "Adding {0,number,#} samples to keep audio in sync.", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>AVI: Compressed (MJPG)</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_AVI_MJPG_DESCRIPTION() {
        return inter("VID_AVI_MJPG_DESCRIPTION", "AVI: Compressed (MJPG)");
    }

    /**
    <table border="1"><tr><td>
    <pre>png</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_PNG_COMMAND() {
        return inter("VID_IMG_SEQ_PNG_COMMAND", "png");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: png</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_PNG_DESCRIPTION() {
        return inter("VID_IMG_SEQ_PNG_DESCRIPTION", "Image sequence: png");
    }

    /**
    <table border="1"><tr><td>
    <pre>avi:mjpg</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_AVI_MJPG_COMMAND() {
        return inter("VID_AVI_MJPG_COMMAND", "avi:mjpg");
    }

    /**
    <table border="1"><tr><td>
    <pre>avi:rgb</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_AVI_RGB_COMMAND() {
        return inter("VID_AVI_RGB_COMMAND", "avi:rgb");
    }

    /**
    <table border="1"><tr><td>
    <pre>AVI: Uncompressed RGB</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_AVI_RGB_DESCRIPTION() {
        return inter("VID_AVI_RGB_DESCRIPTION", "AVI: Uncompressed RGB");
    }

    /**
    <table border="1"><tr><td>
    <pre>bmp</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_BMP_COMMAND() {
        return inter("VID_IMG_SEQ_BMP_COMMAND", "bmp");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: bmp</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_BMP_DESCRIPTION() {
        return inter("VID_IMG_SEQ_BMP_DESCRIPTION", "Image sequence: bmp");
    }

    /**
    <table border="1"><tr><td>
    <pre>mdec</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_MDEC_COMMAND() {
        return inter("VID_IMG_SEQ_MDEC_COMMAND", "mdec");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: mdec</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_MDEC_DESCRIPTION() {
        return inter("VID_IMG_SEQ_MDEC_DESCRIPTION", "Image sequence: mdec");
    }

    /**
    <table border="1"><tr><td>
    <pre>avi:jyuv</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_AVI_JYUV_COMMAND() {
        return inter("VID_AVI_JYUV_COMMAND", "avi:jyuv");
    }

    /**
    <table border="1"><tr><td>
    <pre>AVI: YUV with [0-255] range</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_AVI_JYUV_DESCRIPTION() {
        return inter("VID_AVI_JYUV_DESCRIPTION", "AVI: YUV with [0-255] range");
    }

    /**
    <table border="1"><tr><td>
    <pre>jpg</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_JPG_COMMAND() {
        return inter("VID_IMG_SEQ_JPG_COMMAND", "jpg");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: jpg</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_JPG_DESCRIPTION() {
        return inter("VID_IMG_SEQ_JPG_DESCRIPTION", "Image sequence: jpg");
    }

    /**
    <table border="1"><tr><td>
    <pre>bs</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_BS_COMMAND() {
        return inter("VID_IMG_SEQ_BS_COMMAND", "bs");
    }

    /**
    <table border="1"><tr><td>
    <pre>Image sequence: bitstream</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_IMG_SEQ_BS_DESCRIPTION() {
        return inter("VID_IMG_SEQ_BS_DESCRIPTION", "Image sequence: bitstream");
    }

    /**
    <table border="1"><tr><td>
    <pre>avi:yuv</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_AVI_YUV_COMMAND() {
        return inter("VID_AVI_YUV_COMMAND", "avi:yuv");
    }

    /**
    <table border="1"><tr><td>
    <pre>AVI: YUV</pre>
    </td></tr></table>
    <ul>
       <li>VideoFormat.java</li>
    </ul>
    */
    public static LocalizedMessage VID_AVI_YUV_DESCRIPTION() {
        return inter("VID_AVI_YUV_DESCRIPTION", "AVI: YUV");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0}-{1}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage VID_RANGE_OF_FILES_TO_SAVE(@Nonnull java.io.File a0, @Nonnull java.io.File a1) {
        return inter("VID_RANGE_OF_FILES_TO_SAVE", "{0}-{1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>Decode quality: {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DECODE_QUALITY(@Nonnull jpsxdec.discitems.savers.MdecDecodeQuality a0) {
        return inter("CMD_DECODE_QUALITY", "Decode quality: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Output files: {0}-{1}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_OUTPUT_FILES(@Nonnull java.io.File a0, @Nonnull java.io.File a1) {
        return inter("CMD_OUTPUT_FILES", "Output files: {0}-{1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>With audio item(s):</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_SAVING_WITH_AUDIO_ITEMS() {
        return inter("CMD_SAVING_WITH_AUDIO_ITEMS", "With audio item(s):");
    }

    /**
    <table border="1"><tr><td>
    <pre>Emulate PSX audio/video sync: {0,choice,0#No|1#Yes}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_EMULATE_PSX_AV_SYNC_NY(int a0) {
        return inter("CMD_EMULATE_PSX_AV_SYNC_NY", "Emulate PSX audio/video sync: {0,choice,0#No|1#Yes}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>No audio</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_NO_AUDIO() {
        return inter("CMD_NO_AUDIO", "No audio");
    }

    /**
    <table border="1"><tr><td>
    <pre>Skip frames before {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_FRAME_RANGE_BEFORE(@Nonnull jpsxdec.discitems.savers.FrameLookup a0) {
        return inter("CMD_FRAME_RANGE_BEFORE", "Skip frames before {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Chroma upsampling: {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_UPSAMPLE_QUALITY(@Nonnull jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate.Upsampler a0) {
        return inter("CMD_UPSAMPLE_QUALITY", "Chroma upsampling: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Video format: {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_FORMAT(@Nonnull jpsxdec.discitems.savers.VideoFormat a0) {
        return inter("CMD_VIDEO_FORMAT", "Video format: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Skip frames after {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_FRAME_RANGE_AFTER(@Nonnull jpsxdec.discitems.savers.FrameLookup a0) {
        return inter("CMD_FRAME_RANGE_AFTER", "Skip frames after {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Cropping: {0,choice,0#No|1#Yes}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_CROPPING(int a0) {
        return inter("CMD_CROPPING", "Cropping: {0,choice,0#No|1#Yes}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error closing AVI</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage AVI_CLOSE_ERR() {
        return inter("AVI_CLOSE_ERR", "Error closing AVI");
    }

    /**
    <table border="1"><tr><td>
    <pre>Output file: {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_OUTPUT_FILE(@Nonnull java.io.File a0) {
        return inter("CMD_OUTPUT_FILE", "Output file: {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc speed: {0,choice,1#1x|2#2x} ({1,number,#.###} fps)</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaver.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DISC_SPEED(int a0, double a1) {
        return inter("CMD_DISC_SPEED", "Disc speed: {0,choice,1#1x|2#2x} ({1,number,#.###} fps)", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>-ds &lt;disc speed&gt;</pre>
    </td></tr></table>
    <p>Note that the command -ds is hard-coded</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_DS() {
        return inter("CMD_VIDEO_DS", "-ds <disc speed>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Specify 1 or 2 if disc speed is unknown.</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_DS_HELP() {
        return inter("CMD_VIDEO_DS_HELP", "Specify 1 or 2 if disc speed is unknown.");
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
    public static LocalizedMessage CMD_VIDEO_UP() {
        return inter("CMD_VIDEO_UP", "-up <upsampling>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Chroma upsampling method
(default {0}). Options:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_UP_HELP(@Nonnull jpsxdec.i18n.LocalizedMessage a0) {
        return inter("CMD_VIDEO_UP_HELP", "Chroma upsampling method\n(default {0}). Options:", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid upsample quality {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_UPSAMPLE_QUALITY_INVALID(@Nonnull String a0) {
        return inter("CMD_UPSAMPLE_QUALITY_INVALID", "Invalid upsample quality {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid decode quality {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_DECODE_QUALITY_INVALID(@Nonnull String a0) {
        return inter("CMD_DECODE_QUALITY_INVALID", "Invalid decode quality {0}", a0);
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
    public static LocalizedMessage CMD_VIDEO_QUALITY() {
        return inter("CMD_VIDEO_QUALITY", "-quality,-q <quality>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Decoding quality (default {0}). Options:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_QUALITY_HELP(@Nonnull jpsxdec.i18n.LocalizedMessage a0) {
        return inter("CMD_VIDEO_QUALITY_HELP", "Decoding quality (default {0}). Options:", a0);
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
    public static LocalizedMessage CMD_VIDEO_NOCROP() {
        return inter("CMD_VIDEO_NOCROP", "-nocrop");
    }

    /**
    <table border="1"><tr><td>
    <pre>Don't crop data around unused frame edges.</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_NOCROP_HELP() {
        return inter("CMD_VIDEO_NOCROP_HELP", "Don't crop data around unused frame edges.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame number type {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_FRAME_NUMBER_TYPE_INVALID(@Nonnull String a0) {
        return inter("CMD_FRAME_NUMBER_TYPE_INVALID", "Invalid frame number type {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>-frame,-frames # or #-#</pre>
    </td></tr></table>
    <p>Note that the commands -frame and -frames are hard-coded</p>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_FRAMES() {
        return inter("CMD_VIDEO_FRAMES", "-frame,-frames # or #-#");
    }

    /**
    <table border="1"><tr><td>
    <pre>Process only frames in range.</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_FRAMES_HELP() {
        return inter("CMD_VIDEO_FRAMES_HELP", "Process only frames in range.");
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
    public static LocalizedMessage CMD_VIDEO_NUM() {
        return inter("CMD_VIDEO_NUM", "-num <type>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Frame number to use when saving image sequence
(default {0}). Options:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_NUM_HELP(@Nonnull jpsxdec.i18n.LocalizedMessage a0) {
        return inter("CMD_VIDEO_NUM_HELP", "Frame number to use when saving image sequence\n(default {0}). Options:", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid video format {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_FORMAT_INVALID(@Nonnull String a0) {
        return inter("CMD_VIDEO_FORMAT_INVALID", "Invalid video format {0}", a0);
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
    public static LocalizedMessage CMD_VIDEO_VF() {
        return inter("CMD_VIDEO_VF", "-vidfmt,-vf <format>");
    }

    /**
    <table border="1"><tr><td>
    <pre>Output video format (default {0}).
Options:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_VF_HELP(@Nonnull jpsxdec.i18n.LocalizedMessage a0) {
        return inter("CMD_VIDEO_VF_HELP", "Output video format (default {0}).\nOptions:", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>Invalid frame(s) {0}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilder.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_FRAME_RANGE_INVALID(@Nonnull String a0) {
        return inter("CMD_FRAME_RANGE_INVALID", "Invalid frame(s) {0}", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>-noaud</pre>
    </td></tr></table>
    <p>Note that the command -noaud is hard-coded</p>
    <ul>
       <li>VideoSaverBuilderCrusader.java</li>
       <li>VideoSaverBuilderStr.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_NOAUD() {
        return inter("CMD_VIDEO_NOAUD", "-noaud");
    }

    /**
    <table border="1"><tr><td>
    <pre>Don't save audio.</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderCrusader.java</li>
       <li>VideoSaverBuilderStr.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_NOAUD_HELP() {
        return inter("CMD_VIDEO_NOAUD_HELP", "Don't save audio.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save audio:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderCrusaderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_SAVE_AUDIO_LABEL() {
        return inter("GUI_SAVE_AUDIO_LABEL", "Save audio:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Decode quality:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_DECODE_QUALITY_LABEL() {
        return inter("GUI_DECODE_QUALITY_LABEL", "Decode quality:");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#} fps</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_FPS_LABLE_WHOLE_NUMBER(long a0) {
        return inter("GUI_FPS_LABLE_WHOLE_NUMBER", "{0,number,#} fps", a0);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#}x{1,number,#}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_DIMENSIONS_WIDTH_X_HEIGHT_LABEL(int a0, int a1) {
        return inter("GUI_DIMENSIONS_WIDTH_X_HEIGHT_LABEL", "{0,number,#}x{1,number,#}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>{0}
to: {1}</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_OUTPUT_VIDEO_FILE_RANGE(@Nonnull java.io.File a0, @Nonnull java.io.File a1) {
        return inter("GUI_OUTPUT_VIDEO_FILE_RANGE", "{0}\nto: {1}", a0, a1);
    }

    /**
    <table border="1"><tr><td>
    <pre>2x</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage DISC_SPEED_2X() {
        return inter("DISC_SPEED_2X", "2x");
    }

    /**
    <table border="1"><tr><td>
    <pre>Disc speed:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_DISC_SPEED_LABEL() {
        return inter("GUI_DISC_SPEED_LABEL", "Disc speed:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Dimensions:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_DIMENSIONS_LABEL() {
        return inter("GUI_DIMENSIONS_LABEL", "Dimensions:");
    }

    /**
    <table border="1"><tr><td>
    <pre>1x</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage DISC_SPEED_1X() {
        return inter("DISC_SPEED_1X", "1x");
    }

    /**
    <table border="1"><tr><td>
    <pre>Audio volume:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_AUDIO_VOLUME_LABEL() {
        return inter("GUI_AUDIO_VOLUME_LABEL", "Audio volume:");
    }

    /**
    <table border="1"><tr><td>
    <pre>Video format:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_VIDEO_FORMAT_LABEL() {
        return inter("GUI_VIDEO_FORMAT_LABEL", "Video format:");
    }

    /**
    <table border="1"><tr><td>
    <pre>{0,number,#.###} ({1,number,#}/{2,number,#}) fps</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_FPS_LABEL_FRACTION(double a0, long a1, long a2) {
        return inter("GUI_FPS_LABEL_FRACTION", "{0,number,#.###} ({1,number,#}/{2,number,#}) fps", a0, a1, a2);
    }

    /**
    <table border="1"><tr><td>
    <pre>Crop</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_CROP_CHECKBOX() {
        return inter("GUI_CROP_CHECKBOX", "Crop");
    }

    /**
    <table border="1"><tr><td>
    <pre>Chroma upsampling:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_CHROMA_UPSAMPLING_LABEL() {
        return inter("GUI_CHROMA_UPSAMPLING_LABEL", "Chroma upsampling:");
    }

    /**
    <table border="1"><tr><td>
    <pre>-psxav</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderStr.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_PSXAV() {
        return inter("CMD_VIDEO_PSXAV", "-psxav");
    }

    /**
    <table border="1"><tr><td>
    <pre>Emulate PSX audio/video timing.</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderStr.java</li>
    </ul>
    */
    public static LocalizedMessage CMD_VIDEO_PSXAV_HELP() {
        return inter("CMD_VIDEO_PSXAV_HELP", "Emulate PSX audio/video timing.");
    }

    /**
    <table border="1"><tr><td>
    <pre>Emulate PSX a/v sync:</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderStrGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_EMULATE_PSX_AV_SYNC_LABEL() {
        return inter("GUI_EMULATE_PSX_AV_SYNC_LABEL", "Emulate PSX a/v sync:");
    }

    /**
    <table border="1"><tr><td>
    <pre></pre>
    </td></tr></table>
    <p>Column name is empty in English</p>
    <ul>
       <li>VideoSaverBuilderStrGui.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_VID_AUDIO_SAVE_ID_COLUMN() {
        return inter("GUI_VID_AUDIO_SAVE_ID_COLUMN", "");
    }

    /**
    <table border="1"><tr><td>
    <pre>Details</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderStrGui.java</li>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_TREE_DETAILS_COLUMN() {
        return inter("GUI_TREE_DETAILS_COLUMN", "Details");
    }

    /**
    <table border="1"><tr><td>
    <pre>Save</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderStrGui.java</li>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_TREE_SAVE_COLUMN() {
        return inter("GUI_TREE_SAVE_COLUMN", "Save");
    }

    /**
    <table border="1"><tr><td>
    <pre>#</pre>
    </td></tr></table>
    <ul>
       <li>VideoSaverBuilderStrGui.java</li>
       <li>GuiTree.java</li>
    </ul>
    */
    public static LocalizedMessage GUI_TREE_INDEX_NUMBER_COLUMN() {
        return inter("GUI_TREE_INDEX_NUMBER_COLUMN", "#");
    }

    /**
    <table border="1"><tr><td>
    <pre>Sector {0,number,#} sound parameter corrupted: [{1,number,#}{2,choice,0# (bad)|1#}, {3,number,#}{4,choice,0# (bad)|1#}, {5,number,#}{6,choice,0# (bad)|1#}, {7,number,#}{8,choice,0# (bad)|1#}]. Chose {9,number,#}. Affects samples starting at {10,number,#}.</pre>
    </td></tr></table>
    <p>Sorry for the nasty list of choice, but it's simpler than having dozens of strings for the different combinations</p>
    <ul>
       <li>XaAdpcmDecoder.java</li>
    </ul>
    */
    public static LocalizedMessage XA_SOUND_PARAMETER_CORRUPTED(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, long a10) {
        return inter("XA_SOUND_PARAMETER_CORRUPTED", "Sector {0,number,#} sound parameter corrupted: [{1,number,#}{2,choice,0# (bad)|1#}, {3,number,#}{4,choice,0# (bad)|1#}, {5,number,#}{6,choice,0# (bad)|1#}, {7,number,#}{8,choice,0# (bad)|1#}]. Chose {9,number,#}. Affects samples starting at {10,number,#}.", a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
    }

    /**
    <table border="1"><tr><td>
    <pre>Error writing frame file {0}</pre>
    </td></tr></table>
    <p>Currently unused</p>
    */
    public static LocalizedMessage FRAME_FILE_WRITE_ERR(@Nonnull java.io.File a0) {
        return inter("FRAME_FILE_WRITE_ERR", "Error writing frame file {0}", a0);
    }

}