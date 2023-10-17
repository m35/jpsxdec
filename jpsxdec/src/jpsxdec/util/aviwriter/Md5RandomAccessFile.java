/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2023  Michael Sabin
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

package jpsxdec.util.aviwriter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.Md5OutputStream;

/**
 * Calculates the md5 of all the written data.
 * Backed by a {@link Md5OutputStream}.
 *
 * Note that if any file seeking happens, the md5 will be different than if the md5
 * was calculated on the generated file.
 *
 * By necessity, creates a single empty file in the system's temp directory
 * at the first call of {@link #newThreadMd5Raf()}. It will be automatically deleted
 * when the program ends.
 *
 * ONLY the following methods can be used without error:
 * <ul>
 * <li>{@link #write(int)}
 * <li>{@link #write(byte[])} )}
 * <li>{@link #write(byte[], int, int)}
 * <li>{@link #skipBytes(int)}
 * <li>{@link #seek(long)}
 * <li>{@link #getFilePointer()}
 * <li>{@link #setLength(long)}
 * </ul>
 *
 * All other methods will throw {@link UnsupportedOperationException}
 * (for non-final methods that could be overridden) or {@link IOException}
 * (because the underlying file is closed).
 */
public class Md5RandomAccessFile extends RandomAccessFile {

    private static final ThreadLocal<AtomicLong> _threadFilePointer = ThreadLocal.withInitial(new Supplier<AtomicLong>() {
        @Override
        public AtomicLong get() {
            return new AtomicLong();
        }
    });

    /**
     * Common singleton {@link Md5RandomAccessFile} used by all threads. The thread specific
     * difference is in the thread's {@link #_threadFilePointer} and the thread's
     * {@link Md5OutputStream}.
     * Nicer to have a single {@link RandomAccessFile} so only 1 temp file must be created
     * during the entire run of the program.
     */
    @CheckForNull
    private static Md5RandomAccessFile _md5RafSingleton;

    /**
     * Technically doesn't create a new instance, but the returned object will behave as a new instance.
     *
     * Only 1 instance of this class should be used at a time on a given thread!
     * If multiple are used, the MD5 calculation and file pointer will be a mix of all.
     */
    public static @Nonnull Md5RandomAccessFile newThreadMd5Raf() throws IOException {
        synchronized (Md5RandomAccessFile.class) {

            if (_md5RafSingleton == null) {
                File tempFile = File.createTempFile(Md5RandomAccessFile.class.getSimpleName(), null);
                tempFile.deleteOnExit();
                _md5RafSingleton = new Md5RandomAccessFile(tempFile);
            }

            // reset the position in the file as if a new instance of this class was created
            _threadFilePointer.get().set(0);
            return _md5RafSingleton;
        }
    }


    private Md5RandomAccessFile(@Nonnull File tempFile) throws IOException {
        super(tempFile, "r");
        super.close();
    }

    @Override
    public int read() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] b, int off, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long length() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int skipBytes(int n) {
        _threadFilePointer.get().addAndGet(n);
        return n;
    }

    @Override
    public void write(int b) {
        Md5OutputStream.getThreadMd5OutputStream().write(b);
        _threadFilePointer.get().addAndGet(1);
    }

    @Override
    public void write(byte[] b) {
        Md5OutputStream.getThreadMd5OutputStream().write(b);
        _threadFilePointer.get().addAndGet(b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        Md5OutputStream.getThreadMd5OutputStream().write(b, off, len);
        _threadFilePointer.get().addAndGet(len);
    }

    @Override
    public long getFilePointer() {
        return _threadFilePointer.get().get();
    }

    @Override
    public void seek(long pos) {
        _threadFilePointer.get().set(pos);
    }

    @Override
    public void setLength(long newLength) {
    }

    @Override
    public void close() {
        Md5OutputStream.getThreadMd5OutputStream().close();
    }
}