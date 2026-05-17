/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2026  Michael Sabin
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

package jpsxdec.util.mkvwriter;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.Version;
import jpsxdec.i18n.I;
import jpsxdec.util.IO;
import jpsxdec.util.mkvwriter.EbmlWriter.BackWriteFloatPlaceholder;
import jpsxdec.util.mkvwriter.EbmlWriter.BackWriteUIntPlaceholder;
import jpsxdec.util.mkvwriter.EbmlWriter.EbmlAscii;
import jpsxdec.util.mkvwriter.EbmlWriter.EbmlBinary;
import jpsxdec.util.mkvwriter.EbmlWriter.EbmlFloat;
import jpsxdec.util.mkvwriter.EbmlWriter.EbmlMaster;
import jpsxdec.util.mkvwriter.EbmlWriter.EbmlUint;

/**
 * A minimal but compliant Matroska (MKV) audio/video container writer.
 *<p>
 * Audio is limited to signed 16-bit little-endian PCM.
 * It supports only a few video codecs.
 *<p>
 * Compliance was determined by the MKV validation tools, and testing
 * in several players.
 *<p>
 * Written blocks are not checked for correctness. That is the caller's responsibility.
 * The content of blocks may cause the resulting mkv to not be compliant.
 *<p>
 * Possible improvement:
 *<ul>
 *<li> Construct the smaller master elements in memory so the lengths would be known
 * before writing, thus making them shorter in length, and avoids seeking back to write the
 * lengths.
 *</ul>
 */
final class BasicMkvWriter implements Closeable {

    private static final Logger LOG = Logger.getLogger(BasicMkvWriter.class.getName());

    // Setting the system property "meta" allows you to create byte identical .mkv files (for testing)
    // It serves the same purpose as "mkvmerge --deterministic" option.
    // https://mkvtoolnix.download/doc/mkvmerge.html#mkvmerge.description.deterministic
    // https://gitlab.com/mbunkus/mkvtoolnix/-/wikis/Creating-byte-identical-files

    /** This metadata value is written to the mkv header (MuxingApp and WritingApp).
     * For testing, it can be set to a constant value. */
    private static @Nonnull String getMeta() {
        String sMeta = System.getProperty("meta");
        if (sMeta == null)
            sMeta = I.JPSXDEC_VERSION_NON_COMMERCIAL(Version.Version).getEnglishMessage();
        return sMeta;
    }

    /** Used to generate random UIDs in the mkv header. */
    private static @Nonnull Random getRandom() {
        String sMeta = System.getProperty("meta");
        Random random;
        if (sMeta == null) {
            random = new Random();
        } else {
            // Seed the random with the hash of the meta string to produce deterministic values (for testing purposes)
            random = new Random(sMeta.hashCode());
        }
        return random;
    }

    // EBML tags as defined in the MKV spec https://www.matroska.org/technical/elements.html
    // with a few necessary tags from the EBML specification https://datatracker.ietf.org/doc/rfc8794/
    // Only the necessary tags for this implementation are declared here

    private static final int EBML_MAX_ID_LENGTH = 4;
    // TODO need to go back through these
    // make sure they haven't changed in the spec
    // and can I get away from using version 4?
    // also check dbmp fourcc. in theory windows should support it
    // and it says some are required but never show up
    // and some as optional, but I'm including them cuz ffmpeg
    private static final EbmlMaster EBML                 = new EbmlMaster( 0x1A45DFA3 ,4, "EBML");
    private static final   EbmlUint   EBMLVersion        = new EbmlUint  (     0x4286 ,2, "EBMLVersion");
    private static final   EbmlUint   EBMLReadVersion    = new EbmlUint  (     0x42F7 ,2, "EBMLReadVersion");
    private static final   EbmlUint   EBMLMaxIDLength    = new EbmlUint  (     0x42F2 ,2, "EBMLMaxIDLength");
    private static final   EbmlUint   EBMLMaxSizeLength  = new EbmlUint  (     0x42F3 ,2, "EBMLMaxSizeLength");
    private static final   EbmlAscii  DocType            = new EbmlAscii (     0x4282 ,2, "DocType");
    private static final   EbmlUint   DocTypeVersion     = new EbmlUint  (     0x4287 ,2, "DocTypeVersion");
    private static final   EbmlUint   DocTypeReadVersion = new EbmlUint  (     0x4285 ,2, "DocTypeReadVersion");
    // -------------------------------------------------------------------------------------------------
    private static final EbmlMaster Segment              = new EbmlMaster( 0x18538067 ,4, "Segment");
    // -------------------------------------------------------------------------------------------------
    private static final EbmlMaster SeekHead             = new EbmlMaster( 0x114D9B74 ,4, "SeekHead");
    private static final   EbmlMaster Seek               = new EbmlMaster(     0x4DBB ,2, "Seek");
    private static final   EbmlBinary SeekID             = new EbmlBinary(     0x53AB ,2, "SeekID");
    private static final   EbmlUint   SeekPosition       = new EbmlUint  (     0x53AC ,2, "SeekPosition");
    private static final EbmlBinary Ebml_Void            = new EbmlBinary(       0xEC ,1, "Void");
    // -------------------------------------------------------------------------------------------------
    private static final EbmlMaster Info                 = new EbmlMaster( 0x1549A966 ,4, "Info");
    private static final EbmlAscii    MuxingApp          = new EbmlAscii (     0x4D80 ,2, "MuxingApp");
    private static final EbmlAscii    WritingApp         = new EbmlAscii (     0x5741 ,2, "WritingApp");
    private static final EbmlUint     TimestampScale     = new EbmlUint  (   0x2AD7B1 ,3, "TimestampScale");
    private static final EbmlBinary   SegmentUID         = new EbmlBinary(     0x73A4 ,2, "SegmentUID");
    private static final EbmlFloat    Duration           = new EbmlFloat (     0x4489 ,2, "Duration");
    // -------------------------------------------------------------------------------------------------
    private static final EbmlMaster Tracks               = new EbmlMaster( 0x1654AE6B ,4, "Tracks");
    private static final EbmlMaster   TrackEntry         = new EbmlMaster(       0xAE ,1, "TrackEntry");
    private static final EbmlUint       TrackNumber      = new EbmlUint  (       0xD7 ,1, "TrackNumber");
    private static final EbmlUint       TrackUID         = new EbmlUint  (     0x73C5 ,2, "TrackUID");
    private static final EbmlUint       TrackType        = new EbmlUint  (       0x83 ,1, "TrackType");
    private static final EbmlUint       FlagLacing       = new EbmlUint  (       0x9C ,1, "FlagLacing");
    private static final EbmlUint       DefaultDuration  = new EbmlUint  (   0x23E383 ,3, "DefaultDuration");
    private static final EbmlAscii  CodecID              = new EbmlAscii (       0x86 ,1, "CodecID");
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    private static final EbmlMaster Video                  = new EbmlMaster(       0xE0 ,1, "Video");
    private static final EbmlUint     PixelWidth           = new EbmlUint  (       0xB0 ,1, "PixelWidth");
    private static final EbmlUint     PixelHeight          = new EbmlUint  (       0xBA ,1, "PixelHeight");
    private static final EbmlAscii    UncompressedFourCC   = new EbmlAscii (   0x2EB524 ,3, "UncompressedFourCC");
    private static final EbmlUint     FlagInterlaced       = new EbmlUint  (       0x9A ,1, "FlagInterlaced");
    private static final EbmlBinary   CodecPrivate         = new EbmlBinary(     0x63A2 ,2, "CodecPrivate");
    private static final EbmlMaster   Colour               = new EbmlMaster(     0x55B0 ,2, "Colour");
    private static final EbmlUint       MatrixCoefficients = new EbmlUint  (     0x55B1 ,2, "MatrixCoefficients");
    private static final EbmlUint       Range              = new EbmlUint  (     0x55B9 ,2, "Range");
    private static final EbmlUint       ChromaSitingHorz   = new EbmlUint  (     0x55B7 ,2, "ChromaSitingHorz");
    private static final EbmlUint       ChromaSitingVert   = new EbmlUint  (     0x55B8 ,2, "ChromaSitingVert");
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    private static final EbmlMaster Audio                  = new EbmlMaster(       0xE1 ,1, "Audio");
    private static final EbmlUint     BitDepth             = new EbmlUint  (     0x6264 ,2, "BitDepth");
    private static final EbmlUint     Channels             = new EbmlUint  (       0x9F ,1, "Channels");
    private static final EbmlFloat    SamplingFrequency    = new EbmlFloat (       0xB5 ,1, "SamplingFrequency");
    // -------------------------------------------------------------------------------------------------
    private static final EbmlMaster Cluster                = new EbmlMaster( 0x1F43B675 ,4, "Cluster");
    private static final EbmlUint     Timestamp            = new EbmlUint  (       0xE7 ,1, "Timestamp");
    private static final   EbmlMaster BlockGroup           = new EbmlMaster(       0xA0 ,1, "BlockGroup");
    private static final   EbmlUint     BlockDuration      = new EbmlUint  (       0x9B ,1, "BlockDuration");
    private static final EbmlMaster   SimpleBlock          = new EbmlMaster(       0xA3 ,1, "SimpleBlock");
    // -------------------------------------------------------------------------------------------------
    private static final EbmlMaster Cues                      = new EbmlMaster( 0x1C53BB6B ,4, "Cues");
    private static final EbmlMaster   CuePoint                = new EbmlMaster(       0xBB ,1, "CuePoint");
    private static final EbmlUint       CueTime               = new EbmlUint  (       0xB3 ,1, "CueTime");
    private static final EbmlUint       CueTrack              = new EbmlUint  (       0xF7 ,1, "CueTrack");
    private static final EbmlMaster     CueTrackPositions     = new EbmlMaster(       0xB7 ,1, "CueTrackPositions");
    private static final EbmlUint         CueClusterPosition  = new EbmlUint  (       0xF1 ,1, "CueClusterPosition");
    private static final EbmlUint         CueRelativePosition = new EbmlUint  (       0xF0 ,1, "CueRelativePosition");
    // -------------------------------------------------------------------------------------------------

    /**
     * Collects the information about each frame to be later written into the {@link #Cues} element.
     */
    private static class CueEntry {
        /** @see #CueTime */
        long lngCueTime;
        /** @see #CueTrack */
        int iCueTrack;
        /** @see #CueClusterPosition */
        long lngCueClusterPosition;
        /** @see #CueRelativePosition */
        long lngCueRelativePosition;
    }

    /** Arbitrary number, but the convention is 1. */
    private static final int VIDEO_TRACK_NUM = 1;
    /** Arbitrary number, but the convention is 2. */
    private static final int AUDIO_TRACK_NUM = 2;

    @Nonnull
    private final File _mkvFile;
    @Nonnull
    private final EbmlWriter _ebmlWriter;

    private final ArrayList<CueEntry> _cues = new ArrayList<CueEntry>();

    @Nonnull
    private final BackWriteUIntPlaceholder _Info_offsetPlaceholder;
    @Nonnull
    private final BackWriteUIntPlaceholder _Cues_offsetPlaceholder;
    @Nonnull
    private final BackWriteUIntPlaceholder _Tracks_offsetPlaceholder;
    @Nonnull
    private final BackWriteFloatPlaceholder _Duration_offsetPlaceholder;

    /** Used to generate UIDs when the mvk is created. */
    private final Random _random = getRandom();

    private boolean _blnFinished = false;

    public BasicMkvWriter(@Nonnull File mkvFile,
                          @Nonnull MkvVideoFormat videoFormat,
                          @CheckForNull MkvAudioFormat audioFormat)
            throws FileNotFoundException, IOException
    {
        if (videoFormat.getWidth() < 1 || videoFormat.getHeight() < 1)
            throw new IllegalArgumentException("Invalid dimensions: " + videoFormat.getWidth() + " x " + videoFormat.getHeight());

        _mkvFile = mkvFile;

        _ebmlWriter = new EbmlWriter(_mkvFile);

        boolean blnExceptionThrown = true;
        try {
            _ebmlWriter.startMaster(EBML);
                _ebmlWriter.writeElement(EBMLVersion, 1);
                _ebmlWriter.writeElement(EBMLReadVersion, 1);
                _ebmlWriter.writeElement(EBMLMaxIDLength, EBML_MAX_ID_LENGTH);
                _ebmlWriter.writeElement(EBMLMaxSizeLength, VariableLengthInt.EBML_MAX_SIZE_LENGTH);
                _ebmlWriter.writeElement(DocType, "matroska");
                _ebmlWriter.writeElement(DocTypeVersion, 4); // v4 needed for UncompressedFourCC
                _ebmlWriter.writeElement(DocTypeReadVersion, 2);
            _ebmlWriter.endMaster(EBML);

            _ebmlWriter.startMaster(Segment); // top level segment

            _ebmlWriter.startMaster(SeekHead);
                _ebmlWriter.startMaster(Seek);
                    _ebmlWriter.writeElement(SeekID, Info.toBytes());
                    _Info_offsetPlaceholder = _ebmlWriter.writeElementPlaceholder(SeekPosition);
                _ebmlWriter.endMaster(Seek);
                _ebmlWriter.startMaster(Seek);
                    _ebmlWriter.writeElement(SeekID, Tracks.toBytes());
                    _Tracks_offsetPlaceholder = _ebmlWriter.writeElementPlaceholder(SeekPosition);
                _ebmlWriter.endMaster(Seek);
                _ebmlWriter.startMaster(Seek);
                    _ebmlWriter.writeElement(SeekID, Cues.toBytes());
                    _Cues_offsetPlaceholder = _ebmlWriter.writeElementPlaceholder(SeekPosition);
                _ebmlWriter.endMaster(Seek);
            _ebmlWriter.endMaster(SeekHead);

            // mkv spec recommends adding void data here so other programs can expand the SeekHead.
            // ffmpeg (or was it MKVToolNix?) uses 81 bytes
            _ebmlWriter.writeElement(Ebml_Void, new byte[81]);

            byte[] abSegmentUid = new byte[16];
            _random.nextBytes(abSegmentUid);

            _ebmlWriter.backWrite(_Info_offsetPlaceholder, _ebmlWriter.getOffsetInCurrentMasterBlock());
            _ebmlWriter.startMaster(Info);
                String sMeta = getMeta();
                _ebmlWriter.writeElement(MuxingApp, sMeta);
                _ebmlWriter.writeElement(WritingApp, sMeta);
                // "Timestamp scale in nanoseconds"
                // (e.g. 1,000,000 means all timestamps in the Segment are expressed in milliseconds).
                _ebmlWriter.writeElement(TimestampScale, MkvTimestampScale.getNanosPerTick());
                // "A randomly generated unique ID to identify the Segment amongst many others (128 bits)."
                _ebmlWriter.writeElement(SegmentUID, abSegmentUid); // random UUID

                // "Duration of the Segment in nanoseconds based on TimestampScale"
                // Optional, but VirtualDub needs it, and other players' timelines work better
                _Duration_offsetPlaceholder = _ebmlWriter.writeElementPlaceholder(Duration);
            _ebmlWriter.endMaster(Info);

            writeTracks(videoFormat, audioFormat);

            // Here ffmpeg adds a "Tags" element with tag names "ENCODER" and "DURATION" for each track.
            // mkvtoolnix-gui.exe Multiplexer adds its own choice of tags at the end of the file.
            // I couldn't find any media player that requires the "Tags" elements, so not including it here.

            blnExceptionThrown = false;
        } finally {
            if (blnExceptionThrown)
                IO.closeSilently(_ebmlWriter, LOG);
        }

    }

    private void writeTracks(@Nonnull MkvVideoFormat videoFormat, @CheckForNull MkvAudioFormat audioFormat) throws IOException {

        final int UID_BYTE_SIZE = 4; // Always use a fixed size so I can compare mkv easily
        long lngVideoTrackUid = _random.nextInt() & 0xffffffffL;

        _ebmlWriter.backWrite(_Tracks_offsetPlaceholder, _ebmlWriter.getOffsetInCurrentMasterBlock());
        _ebmlWriter.startMaster(Tracks);
            _ebmlWriter.startMaster(TrackEntry);
                _ebmlWriter.writeElement(TrackNumber, VIDEO_TRACK_NUM);
                _ebmlWriter.writeElement(TrackUID, lngVideoTrackUid, UID_BYTE_SIZE); // "A unique ID to identify the Track."
                _ebmlWriter.writeElement(TrackType, 1); // 1 = video
                _ebmlWriter.writeElement(FlagLacing, 0); // "Set to 1 if the track **MAY** contain blocks using lacing."
                // "When no lacing is used, the number of frames in the lace is ommitted
                // and only one frame can be stored in the Block. The bits 5-6 of the
                // Block Header flags are set to 00."
                // https://matroska.org/technical/notes.html#no-lacing

                if (videoFormat.hasConstantFrameRate()) {
                    // "Number of nanoseconds (not scaled via TimestampScale) per frame"
                    //
                    // Will only write this if the FPS will always adhere to a set interval,
                    // otherwise let the player assume the frame rate is variable.
                    _ebmlWriter.writeElement(DefaultDuration, videoFormat.getDefaultDuration());
                }

                // "An ID corresponding to the codec, see Matroska codec RFC for more info."
                _ebmlWriter.writeElement(CodecID, videoFormat.getCodecId());

                if (videoFormat.getPrivateBytes() != null)
                    _ebmlWriter.writeElement(CodecPrivate, videoFormat.getPrivateBytes()); // "Private data only known to the codec."

                _ebmlWriter.startMaster(Video);
                    _ebmlWriter.writeElement(PixelWidth, videoFormat.getWidth());
                    _ebmlWriter.writeElement(PixelHeight, videoFormat.getHeight());

                    if (videoFormat.getUncompressedFourCC() != null) {
                        /*
                        Specify the uncompressed pixel format used for the Track's data as a FourCC. This value
                        is similar in scope to the biCompression value of AVI's `BITMAPINFO` [@?AVIFormat].
                        There is no definitive list of FourCC values, nor an official registry. Some common
                        values for YUV pixel formats can be found at [@?MSYUV8], [@?MSYUV16] and
                        [@?FourCC-YUV]. Some common values for uncompressed RGB pixel formats can be found at
                        [@?MSRGB] and [@?FourCC-RGB]. UncompressedFourCC **MUST** be set in TrackEntry, when
                        the CodecID Element of the TrackEntry is set to "V_UNCOMPRESSED".
                        */
                        _ebmlWriter.writeElement(UncompressedFourCC, videoFormat.getUncompressedFourCC());
                    }

                    // "Specify whether the video frames in this track are interlaced."
                    _ebmlWriter.writeElement(FlagInterlaced, 2); // 2=progressive

                    if (videoFormat.hasFullRangeYuvColor()) {
                        _ebmlWriter.startMaster(Colour);
                            // this is set by ffmpeg, so I guess I'll do the same
                            _ebmlWriter.writeElement(MatrixCoefficients, 5); // 5=ITU-R BT.470BG
                            // JFIF YUV is 4:2:0 full range with chroma in the middle
                            _ebmlWriter.writeElement(Range, 2); // 2=full range, 3=defined by MatrixCoefficients / TransferCharacteristics
                            _ebmlWriter.writeElement(ChromaSitingHorz, 2); // 2=half
                            _ebmlWriter.writeElement(ChromaSitingVert, 2); // 2=half
                        _ebmlWriter.endMaster(Colour);
                    }
                _ebmlWriter.endMaster(Video);
            _ebmlWriter.endMaster(TrackEntry);

        if (audioFormat != null) {
            long lngAudioTrackUid = _random.nextInt() & 0xffffffffL;

            _ebmlWriter.startMaster(TrackEntry);
                _ebmlWriter.writeElement(TrackNumber, AUDIO_TRACK_NUM);
                _ebmlWriter.writeElement(TrackUID, lngAudioTrackUid, UID_BYTE_SIZE); // "A unique ID to identify the Track"
                _ebmlWriter.writeElement(TrackType, 2); // 2 = audio
                _ebmlWriter.writeElement(FlagLacing, 0); // "Set to 1 if the track **MAY** contain blocks using lacing."

                /*
                A_PCM/INT/LIT
                Codec Name: PCM Integer Little Endian
                Description: The audio bit depth MUST be read and set from the BitDepth
                Element. Audio samples MUST be considered as signed values, except if
                the audio bit depth is 8 which MUST be interpreted as unsigned values.
                Corresponding ACM wFormatTag : 0x0001
                https://www.matroska.org/technical/codec_specs.html#a_pcmintlit
                */
                _ebmlWriter.writeElement(CodecID, "A_PCM/INT/LIT");

                // (ffmpeg here writes that the track is 'eng' language, but that's optional and is the default value)

                _ebmlWriter.startMaster(Audio);
                    _ebmlWriter.writeElement(BitDepth, 16);
                    _ebmlWriter.writeElement(Channels, audioFormat.getAudioChannels());
                    _ebmlWriter.writeElement(SamplingFrequency, (float)audioFormat.getSampleFramesPerSecond()); // "Sampling frequency in Hz."
                _ebmlWriter.endMaster(Audio);
            _ebmlWriter.endMaster(TrackEntry);
        }

        _ebmlWriter.endMaster(Tracks);
    }

    /** Written at the end of the mkv. List of offsets in the file where every frame cluster is located. */
    private void writeCues() throws IOException {
        if (_cues.isEmpty())
            return;

        _ebmlWriter.backWrite(_Cues_offsetPlaceholder, _ebmlWriter.getOffsetInCurrentMasterBlock());
        _ebmlWriter.startMaster(Cues);
        for (int i = 0; i < _cues.size(); i++) {
            CueEntry cue = _cues.get(i);
            _ebmlWriter.startMaster(CuePoint);
                _ebmlWriter.writeElement(CueTime, cue.lngCueTime); // "Absolute timestamp according to the Segment time base."
                _ebmlWriter.startMaster(CueTrackPositions);
                    _ebmlWriter.writeElement(CueTrack, cue.iCueTrack);
                    _ebmlWriter.writeElement(CueClusterPosition, cue.lngCueClusterPosition);
                    _ebmlWriter.writeElement(CueRelativePosition, cue.lngCueRelativePosition);
                _ebmlWriter.endMaster(CueTrackPositions);
            _ebmlWriter.endMaster(CuePoint);
        }
        _ebmlWriter.endMaster(Cues);

        _cues.clear();
    }


    /**
     * Must call this before {@link #close()}, otherwise the mkv will be left
     * in an incomplete and probably corrupted state.
     *
     * @param fltDurationInTicks
     *         "Duration of the Segment in nanoseconds based on TimestampScale"
     *          To be written to the MKV file in the {@link #Duration} element
     */
    public void finish(float fltDurationInTicks) throws IOException {

        // If something goes wrong while wrapping up the mkv, then the mkv is already
        // corrupted so don't try to write the ending again
        if (_blnFinished) {
            LOG.severe("BasicMkvWriter.finish() called more than once");
            return;
        }
        _blnFinished = true;

        writeCues();

        _ebmlWriter.endMaster(Segment); // end top level segment

        /*
        "Duration of the Segment in nanoseconds based on TimestampScale"

        I'm not really sure how to exactly calculate it. I just track
        the max value of the Cluster timestamps + their duration.
        But when I remux with ffmpeg or MKVToolNix, they sometimes have different values.
        */
        _ebmlWriter.backWrite(_Duration_offsetPlaceholder, fltDurationInTicks);
    }

    /**
     * Must call {@link #finish(float)} before this.
     */
    @Override
    public void close() throws IOException {
        if (!_blnFinished) {
            LOG.severe("Closing mkv without finish(), mkv will be corrupted");
        }
        _ebmlWriter.close();
    }

    /**
     * Write a video and/or audio block in a cluster.
     * Use the {@code byte[]} data to indicate which is being written (as null or non-null).
     * If both are being written, the timestamps MUST be within
     * {@link Short#MIN_VALUE} and {@link Short#MAX_VALUE} ticks.
     */
    public void writeCluster(@CheckForNull byte[] abFrame, long lngFrameTimestampInTicks,
                             @CheckForNull byte[] abAudio, long lngAudioTimestampInTicks)
            throws IOException
    {
        if (abFrame == null && abAudio == null)
            throw new IllegalArgumentException("No frame or audio cluster in the parameters");

        long lngClusterPresentationTime;
        short siAudioOffsetTime = 0;

        // Cues are only used to track video frames
        // I didn't see it in the spec, but ffmpeg and MKVToolNix never put
        // more than 1 frame in a cluster, but may put multiple audio
        // blocks in a cluster
        CueEntry frameCue;

        if (abFrame != null) {

            if (abAudio != null) {

                long lngAudioOffsetTime = lngAudioTimestampInTicks - lngFrameTimestampInTicks;
                if (lngAudioOffsetTime < Short.MIN_VALUE || lngAudioOffsetTime > Short.MAX_VALUE) {
                    // uh oh
                    throw new UnsupportedOperationException(
                            "The frame ("+lngFrameTimestampInTicks+") and audio ("+lngAudioOffsetTime+
                                    ") timestamps difference is too big to fit in the 16-bit timestamp relative to cluster");
                }
                siAudioOffsetTime = (short)lngAudioOffsetTime;
            }

            frameCue = new CueEntry();
            frameCue.iCueTrack = VIDEO_TRACK_NUM;
            frameCue.lngCueClusterPosition = _ebmlWriter.getOffsetInCurrentMasterBlock();
            frameCue.lngCueTime = lngFrameTimestampInTicks; // "Absolute timestamp according to the Segment time base."
            lngClusterPresentationTime = lngFrameTimestampInTicks;
        } else {
            frameCue = null;
            lngClusterPresentationTime = lngAudioTimestampInTicks;
        }

        _ebmlWriter.startMaster(Cluster);
            _ebmlWriter.writeElement(Timestamp, lngClusterPresentationTime); // "Absolute timestamp of the cluster (based on TimestampScale)"
            if (frameCue != null) {
                frameCue.lngCueRelativePosition = _ebmlWriter.getOffsetInCurrentMasterBlock();
            }

            /* Maybe add this? (ffmpeg and MKVToolNix don't)
            <BlockGroup> (optional)
                Basic container of information containing a single Block and information specific to that Block.

                <BlockDuration> (required)
                    The duration of the Block, expressed in Track Ticks; see timestamp-ticks. The
                    BlockDuration Element can be useful at the end of a Track to define the duration of
                    the last frame (as there is no subsequent Block available), or when there is a
                    break in a track like for subtitle tracks. When not written and with no
                    DefaultDuration, the value is assumed to be the difference between the timestamp
                    of this Block and the timestamp of the next Block in "display" order (not coding
                    order). BlockDuration **MUST** be set if the associated TrackEntry stores a
                    DefaultDuration value.
                </BlockDuration>
            </BlockGroup>
            */

            if (abFrame != null) {
                writeSimpleBlock(abFrame, VIDEO_TRACK_NUM, (short)0);
            }

            if (abAudio != null) {
                writeSimpleBlock(abAudio, AUDIO_TRACK_NUM, siAudioOffsetTime);
            }
        _ebmlWriter.endMaster(Cluster);

        if (frameCue != null)
            _cues.add(frameCue);
    }

    private void writeSimpleBlock(byte[] abData, int iTrackNumber, short siTimestampRelativeToCluster) throws IOException {
        _ebmlWriter.startMaster(SimpleBlock);

        _ebmlWriter.writeVarUnsignedInt(iTrackNumber);

        // Timestamp, relative to the Cluster Timestamp (used in cases when there's multiple blocks in a cluster)
        // This is a subtle limitation of the TimestampScale value.
        // The audio and video timestamp difference must be smaller than a 16 bit value
        // or they won't be able to share the same Cluster. If the TimestampScale
        // value is tiny, then the audio/video must have very close Timestamps
        // to fit (could also apply to having multiple audio blocks in the same cluster).
        _ebmlWriter.writeSigned16(siTimestampRelativeToCluster);

        /* Flags:
        MSB
        0   Keyframe, set when the Block contains only keyframes
        1-3 Reserved, set to 0
        4   Invisible, the codec SHOULD decode this frame but not display it
        5-6 Lacing: 00=no lacing, 01=Xiph lacing, 11=EBML lacing, 10=fixed-size lacing
        7   Discardable, the frames of the Block can be discarded during playing if needed
        */
        _ebmlWriter.writeUnsigned8(0x80); // flags (only the Keyframe bit is set)

        _ebmlWriter.writeRawBytes(abData);

        _ebmlWriter.endMaster(SimpleBlock);
    }

}
