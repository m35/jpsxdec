package jpsxdec.util.mkvwriter;

import org.junit.Assert;
import org.junit.Test;

public class VariableLengthInt_Tests {

    @Test
    public void testVintVarious() {
        Assert.assertArrayEquals(new byte[]{b2b("00100000", 0x13), (byte)0x81, (byte)0x23}, VariableLengthInt.encodeVariableUnsignedInt(0x138123));

        Assert.assertArrayEquals(new byte[]{b2b("00010000"),       (byte)0x23, (byte)0x81, (byte)0x23}, VariableLengthInt.encodeVariableUnsignedInt(  0x238123));
        Assert.assertArrayEquals(new byte[]{b2b("00010000", 0x03), (byte)0x23, (byte)0x81, (byte)0x23}, VariableLengthInt.encodeVariableUnsignedInt(0x03238123));

        Assert.assertArrayEquals(new byte[]{b2b("10000000", 3)}, VariableLengthInt.encodeVariableUnsignedInt(3));

        Assert.assertArrayEquals(new byte[]{b2b("01000000"      ), (byte)129}, VariableLengthInt.encodeVariableUnsignedInt(129));
        Assert.assertArrayEquals(new byte[]{b2b("01000000", 0x12), (byte)0x44}, VariableLengthInt.encodeVariableUnsignedInt(0x1244));

        Assert.assertArrayEquals(new byte[]{b2b("00100000"), (byte)0x81, (byte)0x23}, VariableLengthInt.encodeVariableUnsignedInt(  0x8123));
    }

    @Test
    public void testVintMinMax() {
        Assert.assertArrayEquals(new byte[]{b2b("10000000")}, VariableLengthInt.encodeVariableUnsignedInt(0x00));
        Assert.assertArrayEquals(new byte[]{b2b("10000001")}, VariableLengthInt.encodeVariableUnsignedInt(0x01));
        Assert.assertArrayEquals(new byte[]{b2b("11111111")}, VariableLengthInt.encodeVariableUnsignedInt(0x7f));

        Assert.assertArrayEquals(new byte[]{b2b("01000000"), _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x00ff));
        Assert.assertArrayEquals(new byte[]{b2b("01000001"),   0}, VariableLengthInt.encodeVariableUnsignedInt(0x0100));
        Assert.assertArrayEquals(new byte[]{b2b("01111111"),   0}, VariableLengthInt.encodeVariableUnsignedInt(0x3f00));
        Assert.assertArrayEquals(new byte[]{b2b("01111111"), _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x3fff));

        Assert.assertArrayEquals(new byte[]{b2b("00100000"), _ff,  _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x00ffff));
        Assert.assertArrayEquals(new byte[]{b2b("00100001"),   0,    0}, VariableLengthInt.encodeVariableUnsignedInt(0x010000));
        Assert.assertArrayEquals(new byte[]{b2b("00111111"),   0,    0}, VariableLengthInt.encodeVariableUnsignedInt(0x1f0000));
        Assert.assertArrayEquals(new byte[]{b2b("00111111"), _ff,  _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x1fffff));

        Assert.assertArrayEquals(new byte[]{b2b("00010000"), _ff, _ff, _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x00ffffff));
        Assert.assertArrayEquals(new byte[]{b2b("00010001"),   0,   0,   0}, VariableLengthInt.encodeVariableUnsignedInt(0x01000000));
        Assert.assertArrayEquals(new byte[]{b2b("00011111"),   0,   0,   0}, VariableLengthInt.encodeVariableUnsignedInt(0x0f000000));
        Assert.assertArrayEquals(new byte[]{b2b("00011111"), _ff, _ff, _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x0fffffff));

        Assert.assertArrayEquals(new byte[]{b2b("00001000"), _ff, _ff, _ff, _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x00ffffffffL));
        Assert.assertArrayEquals(new byte[]{b2b("00001001"),   0,   0,   0,   0}, VariableLengthInt.encodeVariableUnsignedInt(0x0100000000L));
        Assert.assertArrayEquals(new byte[]{b2b("00001111"),   0,   0,   0,   0}, VariableLengthInt.encodeVariableUnsignedInt(0x0700000000L));
        Assert.assertArrayEquals(new byte[]{b2b("00001111"), _ff, _ff, _ff, _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x07ffffffffL));

        Assert.assertArrayEquals(new byte[]{b2b("00000100"), _ff, _ff, _ff, _ff, _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x00ffffffffffL));
        Assert.assertArrayEquals(new byte[]{b2b("00000101"),   0,   0,   0,   0,   0}, VariableLengthInt.encodeVariableUnsignedInt(0x010000000000L));
        Assert.assertArrayEquals(new byte[]{b2b("00000111"),   0,   0,   0,   0,   0}, VariableLengthInt.encodeVariableUnsignedInt(0x030000000000L));
        Assert.assertArrayEquals(new byte[]{b2b("00000111"), _ff, _ff, _ff, _ff, _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x03ffffffffffL));

        Assert.assertArrayEquals(new byte[]{b2b("00000010"), _ff, _ff, _ff, _ff, _ff, _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x00ffffffffffffL));
        Assert.assertArrayEquals(new byte[]{b2b("00000011"),   0,   0,   0,   0,   0,   0}, VariableLengthInt.encodeVariableUnsignedInt(0x01000000000000L));
        Assert.assertArrayEquals(new byte[]{b2b("00000011"), _ff, _ff, _ff, _ff, _ff, _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x01ffffffffffffL));

        Assert.assertArrayEquals(new byte[]{b2b("00000001"), (byte)0x80,   0,   0,   0,   0,   0,   0}, VariableLengthInt.encodeVariableUnsignedInt(0x0080000000000000L));
        Assert.assertArrayEquals(new byte[]{b2b("00000001"),        _ff, _ff, _ff, _ff, _ff, _ff, _ff}, VariableLengthInt.encodeVariableUnsignedInt(0x00ffffffffffffffL));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVintTooBig1() {
        VariableLengthInt.encodeVariableUnsignedInt(0x7fffffffffffffffL);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testVintTooBig2() {
        VariableLengthInt.encodeVariableUnsignedInt(0xffffffffffffffffL);
    }

    private static final byte _ff = (byte) 0xff;

    private static byte b2b(String sBits, int iOrBits) {
        int i = b2b(sBits) & 0xff;
        if ((i & iOrBits) != 0)
            throw new RuntimeException();
        return (byte) (i | iOrBits);
    }
    private static byte b2b(String sBits) {
        if (sBits.length() != 8)
            throw new RuntimeException();
        int i = Integer.parseInt(sBits, 2);
        if (i > 255)
            throw new RuntimeException();
        return (byte) i;
    }

    @Test
    public void testVintOtherFunctions() {
        Assert.assertArrayEquals(new byte[]{0}, VariableLengthInt.numberToBEBytes(0, 1));
        Assert.assertArrayEquals(new byte[]{0, (byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef, (byte)0x11}, VariableLengthInt.numberToBEBytes(0xdeadbeef11L, 6));
        Assert.assertArrayEquals(new byte[]{0}, VariableLengthInt.minimalUnsignedIntToBEBytes(0));
        Assert.assertArrayEquals(new byte[]{(byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef, (byte)0x11}, VariableLengthInt.minimalUnsignedIntToBEBytes(0xdeadbeef11L));
    }

}
