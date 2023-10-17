/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

package jpsxdec.util;

import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

/**
 * Calculates the MD5 of all the written data.
 *
 * Only 1 instance of this class should be used at a time on a given thread!
 * If multiple are used, the MD5 calculation will be a mix of all.
 *
 * Since this will be used to mock an {@link OutputStream} as used normally in the code,
 * there's no way to access the {@link Md5OutputStream} that's created inside the code being tested.
 * The workaround is to store the calculation in thread local storage.
 * So the testing code can run existing code as usual,
 * then get the result from the thread local storage.
 */
public class Md5OutputStream extends OutputStream {

    private static final ThreadLocal<Md5OutputStream> _threadMd5 = ThreadLocal.withInitial(new Supplier<Md5OutputStream>() {
        @Override
        public Md5OutputStream get() {
            return new Md5OutputStream();
        }
    });

    public static @Nonnull Md5OutputStream getThreadMd5OutputStream() {
        return _threadMd5.get();
    }

    public static @Nonnull String getAndResetThreadMd5() {
        return getThreadMd5OutputStream().getAndResetMd5();
    }

    public @Nonnull String getAndResetMd5() {
        synchronized (_md) {
            BigInteger number = new BigInteger(1, _md.digest());
            String sHashText = number.toString(16);
            String sPaddedHash = Misc.zeroPadString(sHashText, 32, false);
            return sPaddedHash;
        }
    }
    @Override
    public void write(@Nonnull byte[] b, int off, int len) {
        synchronized (_md) {
            _md.update(b, off, len);
        }
    }


    @Nonnull
    private final MessageDigest _md;

    private Md5OutputStream() {
        try {
            _md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void write(int b) {
        write(new byte[] {(byte) b});
    }

    @Override
    public void write(@Nonnull byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }
}
