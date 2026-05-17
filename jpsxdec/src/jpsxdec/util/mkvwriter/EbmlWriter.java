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
import java.io.RandomAccessFile;
import java.util.Stack;
import javax.annotation.Nonnull;
import jpsxdec.util.Misc;

/**
 * A simple Extensible Binary Meta Language (EBML) writer.
 * <a href="https://datatracker.ietf.org/doc/rfc8794/">Specification (as of 2024).</a>
 *<p>
 * I think this could generally be used for any EBML writing, but can't confirm.
 * But its use by {@link BasicMkvWriter} is correct.
 *<p>
 * To understand mkv, you need to understand EBML. To understand EBML, you need
 * to understand variable length ints. See {@link VariableLengthInt}.
 * <p>
 * EBML is described as a "binary derivative of XML". I find it more closely resembles
 * the RIFF binary format. The file is broken up into "element" blocks. Each element has
 * this format:
 * <ul>
 *   <li>An identifier of the block, encoded as a variable length int
 *   <li>The size of the contents of the block, encoded as a variable length int.
 *   Note that the size excludes the id and this size.
 *   <li>Contents of the element.
 * </ul>
 * <p>
 * There are several predetermined "element" types. There are "master" elements
 * that contain other elements. Other element types include unsigned integers,
 * floats, strings, and binary bytes.
 * <p>
 * Due to this way vints are encoded, it allows you to represent the same value
 * with any number of bytes you want. 0 could be encoded with one byte as
 * {@code 10000000}, or even with 2 bytes as {@code 010000000 00000000}. This is
 * important because often you won't know the size of the contents of an element
 * until after the contents have been written. By using extra bytes at the start
 * of an element for its length, it makes room for both small and large values.
 */
class EbmlWriter implements Closeable {

    private static final boolean DEBUG = false;


    public static class EbmlElement {
        private final long _lngTagId;
        /** The tag integer value is actually a variable length int so
         * the length could be found using var int logic. But it's just easier
         * to predefine the length of the value. */
        private final int _iIdLength;
        /** Mostly for debugging. */
        @Nonnull
        private final String _sIdString;

        protected EbmlElement(long lngTagId, int iIdLength, @Nonnull String sIdString) {
            _lngTagId = lngTagId & 0xffffffff;
            _iIdLength = iIdLength;
            _sIdString = sIdString;
        }

        public @Nonnull byte[] toBytes() {
            return VariableLengthInt.numberToBEBytes(_lngTagId, _iIdLength);
        }

        @Override
        public String toString() {
            return String.format("%s (%0"+(_iIdLength*2)+"x)", _sIdString, _lngTagId);
        }
    }

    public static class EbmlMaster extends EbmlElement {
        public EbmlMaster(int iId, int iIdLength, @Nonnull String sId) {
            super(iId, iIdLength, sId);
        }
    }

    public static class EbmlUint extends EbmlElement {
        public EbmlUint(int iId, int iIdLength, @Nonnull String sId) {
            super(iId, iIdLength, sId);
        }
    }

    public static class EbmlAscii extends EbmlElement {
        public EbmlAscii(int iId, int iIdLength, @Nonnull String sId) {
            super(iId, iIdLength, sId);
        }
    }

    public static class EbmlBinary extends EbmlElement {
        public EbmlBinary(int iId, int iIdLength, @Nonnull String sId) {
            super(iId, iIdLength, sId);
        }
    }

    public static class EbmlFloat extends EbmlElement {
        public EbmlFloat(int iId, int iIdLength, @Nonnull String sId) {
            super(iId, iIdLength, sId);
        }
    }

    // ...................................................................................

    /** Wrap the {@link RandomAccessFile} with simpler functions and to capture
     * all output for debugging. */
    private static class RafWrapper {
        @Nonnull
        private final RandomAccessFile ___raf___;

        public RafWrapper(@Nonnull File f) throws FileNotFoundException, IOException {
            ___raf___ = new RandomAccessFile(f, "rw");
            ___raf___.setLength(0);
        }

        public long getFilePointer() throws IOException {
            return ___raf___.getFilePointer();
        }

        public void write(@Nonnull byte[] ab) throws IOException {
            if (DEBUG)
                debug(ab);
            if (ab.length == 0) {
                return;
            }
            ___raf___.write(ab);
        }

        public void writeByte(int v) throws IOException {
            if (DEBUG)
                debug(v);
            ___raf___.writeByte(v);
        }

        private void debug(int v) throws IOException {
            debug(new byte[]{(byte)v});
        }
        private void debug(byte[] ab) throws IOException {
            long fp = ___raf___.getFilePointer();
            System.out.println("Writing " + ab.length + " bytes at " + fp + ": " + b2h(ab));
        }
        private static @Nonnull String b2h(@Nonnull byte[] ab) {
            StringBuilder sb = new StringBuilder();
            int iMaxLen = ab.length;
            String sEllipsis = "";
            if (iMaxLen > 100) {
                iMaxLen = 100;
                sEllipsis = "...(truncated)";
            }
            for (int i = 0; i < iMaxLen; i++) {
                byte b = ab[i];
                sb.append(String.format("%02x", b));
            }
            sb.append(sEllipsis);
            return sb.toString();
        }

        /** @see BackWritePlaceholder */
        public long writePlaceholder(int iLengthInBytes) throws IOException {
            long lngCurrentPos = ___raf___.getFilePointer();
            write(new byte[iLengthInBytes]);
            return lngCurrentPos;
        }

        /** @see BackWritePlaceholder */
        public void backWrite(long lngPos, @Nonnull byte[] ab) throws IOException {
            if (ab.length == 0)
                return;
            long lngCurrentPos = ___raf___.getFilePointer();
            ___raf___.seek(lngPos);
            write(ab);
            ___raf___.seek(lngCurrentPos);
        }

        public void close() throws IOException {
            ___raf___.close();
        }
    }

    // ...................................................................................

    /** Some values won't be known until later in the writing process
     * (offsets and sizes of some blocks). This holds the offset and size in the file
     * so it can seek back to the position and write the actual value later. */
    public static abstract class BackWritePlaceholder {
        protected final int _iSpaceSize;
        protected long _lngFilePos;

        private BackWritePlaceholder(int iSize) {
            _iSpaceSize = iSize;
        }
    }

    public static class BackWriteUIntPlaceholder extends BackWritePlaceholder {
        private static final int SIZE = 8;

        private BackWriteUIntPlaceholder() {
            super(SIZE);
        }
    }

    public static class BackWriteFloatPlaceholder extends BackWritePlaceholder {

        private BackWriteFloatPlaceholder() {
            super(Float.BYTES);
        }
    }

    public @Nonnull BackWriteUIntPlaceholder writeElementPlaceholder(@Nonnull EbmlUint e) throws IOException {
        return initPlaceholder(e, new BackWriteUIntPlaceholder());
    }
    public void backWrite(@Nonnull BackWriteUIntPlaceholder placeholder, long lngValue) throws IOException {
        doBackWrite(placeholder, lngValue);
    }

    public @Nonnull BackWriteFloatPlaceholder writeElementPlaceholder(@Nonnull EbmlFloat e) throws IOException {
        return initPlaceholder(e, new BackWriteFloatPlaceholder());
    }
    public void backWrite(@Nonnull BackWriteFloatPlaceholder placeholder, float fltValue) throws IOException {
        int iFloat = Float.floatToIntBits(fltValue);
        doBackWrite(placeholder, iFloat);
    }

    private <T extends BackWritePlaceholder> T initPlaceholder(@Nonnull EbmlElement e, @Nonnull T placeholder) throws IOException {
        _raf.write(e.toBytes());
        writeVarUnsignedInt(placeholder._iSpaceSize);
        placeholder._lngFilePos = _raf.writePlaceholder(placeholder._iSpaceSize);
        return placeholder;
    }

    private void doBackWrite(@Nonnull BackWritePlaceholder placeholder, long lngValue) throws IOException {
        byte[] ab = VariableLengthInt.numberToBEBytes(lngValue, placeholder._iSpaceSize);
        _raf.backWrite(placeholder._lngFilePos, ab);
    }

    // ...................................................................................

    /** Holds the master type and position in the file where the length of the
     * master should be written after its length is determined. Somewhat similar
     * to {@link BackWritePlaceholder}. */
    private static class MasterStartPosition {
        @Nonnull
        private final EbmlMaster _master;
        private final long _lngLengthFilePointer;

        public MasterStartPosition(@Nonnull EbmlMaster master, long lngFilePosition) {
            _master = master;
            _lngLengthFilePointer = lngFilePosition;
        }
    }

    // ===================================================================================
    // ===================================================================================

    @Nonnull
    private final RafWrapper _raf;

    /** "Master" elements can contain other master elements, and some file offsets need
     * to be calculated based on the position relative to the current master. */
    private final Stack<MasterStartPosition> _masterStack = new Stack<MasterStartPosition>();

    public EbmlWriter(@Nonnull File file) throws FileNotFoundException, IOException {
        _raf = new RafWrapper(file);
    }

    // ...................................................................................

    private static final int MASTER_LENGTH_BYTE_SIZE = 8;
    /** Because the length will always be 8, the mask allowing for
     * 56 bits for the int will always be used.
     * @see VariableLengthInt#encodeVariableUnsignedInt(long) */
    private static final long MASK = 1L << 56;

    public void startMaster(@Nonnull EbmlMaster master) throws IOException {
        _raf.write(master.toBytes());
        MasterStartPosition masterPos = new MasterStartPosition(master, _raf.writePlaceholder(MASTER_LENGTH_BYTE_SIZE));
        _masterStack.push(masterPos);
    }

    public long getOffsetInCurrentMasterBlock() throws IOException {
        // It's relative to the end of the block's header
        return _raf.getFilePointer() - _masterStack.peek()._lngLengthFilePointer - MASTER_LENGTH_BYTE_SIZE;
    }

    public void endMaster(@Nonnull EbmlMaster master) throws IOException {
        long lngLength = getOffsetInCurrentMasterBlock();
        MasterStartPosition top = _masterStack.pop();
        if (top._master != master)
            throw new IllegalStateException();
        if ((lngLength & 0xff00000000000000L) != 0)
            throw new IllegalStateException("Master block length is too large to fit in the header " + lngLength);
        // add the mask saying the value will be 8 bytes
        byte[] ab = VariableLengthInt.numberToBEBytes(lngLength | MASK, MASTER_LENGTH_BYTE_SIZE);
        _raf.backWrite(top._lngLengthFilePointer, ab);
    }

    // ...................................................................................

    public void writeElement(@Nonnull EbmlUint e, long lngValue) throws IOException {
        _raf.write(e.toBytes());
        byte[] ab = VariableLengthInt.minimalUnsignedIntToBEBytes(lngValue);
        writeBytesWithVarIntLength(ab);
    }

    public void writeElement(@Nonnull EbmlUint e, long lngValue, int iByteSize) throws IOException {
        _raf.write(e.toBytes());
        byte[] ab = VariableLengthInt.numberToBEBytes(lngValue, iByteSize);
        writeBytesWithVarIntLength(ab);
    }

    public void writeElement(@Nonnull EbmlBinary e, @Nonnull byte[] ab) throws IOException {
        _raf.write(e.toBytes());
        writeBytesWithVarIntLength(ab);
    }

    public void writeElement(@Nonnull EbmlAscii e, @Nonnull String s) throws IOException {
        _raf.write(e.toBytes());
        byte[] ab = Misc.stringToAscii(s);
        writeBytesWithVarIntLength(ab);
    }

    public void writeElement(@Nonnull EbmlFloat e, float f) throws IOException {
        _raf.write(e.toBytes());
        int iFloat = Float.floatToIntBits(f);
        byte[] ab = VariableLengthInt.numberToBEBytes(iFloat, 4);
        writeBytesWithVarIntLength(ab);
    }

    private void writeBytesWithVarIntLength(@Nonnull byte[] ab) throws IOException {
        writeVarUnsignedInt(ab.length);
        _raf.write(ab);
    }

    // ...................................................................................

    public void writeVarUnsignedInt(long lngValue) throws IOException {
        byte[] ab = VariableLengthInt.encodeVariableUnsignedInt(lngValue);
        _raf.write(ab);
    }

    public void writeRawBytes(@Nonnull byte[] b) throws IOException {
        _raf.write(b);
    }

    public void writeSigned16(short si) throws IOException {
        int ui = si & 0xffff;
        byte[] ab = VariableLengthInt.numberToBEBytes(ui, 2);
        _raf.write(ab);
    }

    public void writeUnsigned8(int i) throws IOException {
        _raf.writeByte(i & 0xff);
    }

    @Override
    public void close() throws IOException {
        _raf.close();
    }

}
