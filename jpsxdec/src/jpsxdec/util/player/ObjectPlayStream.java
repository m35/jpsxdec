/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

package jpsxdec.util.player;

import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** Very powerful, thread-safe, blocking queue with the ability to specify 
 * behavior when taking and adding items.  */
class ObjectPlayStream<T> {

    private static final boolean DEBUG = false;

    private static enum WRITE {
        OPEN,
        CLOSED,
    }

    private static enum READ {
        OPEN,
        PAUSED,
        CLOSED,
    }

    private final Object _eventSync = new Object();
    
    private volatile WRITE _eWriteState = WRITE.OPEN;
    private volatile READ _eReadState = READ.PAUSED;

    @Nonnull
    protected final T[] _aoQueue;
    protected int _iHeadPos;
    protected int _iTailPos;
    private int _iSize;

    public ObjectPlayStream(int iCapacity) {
        _aoQueue = (T[]) new Object[iCapacity];
        _iSize = _iHeadPos = _iTailPos = 0;
    }

    //////////////////////////////////

    public @Nonnull Object getSyncObject() {
        return _eventSync;
    }

    void writerOpen() {
        synchronized (_eventSync) {
            _eWriteState = WRITE.OPEN;
        }
    }

    public void writerClose() {
        synchronized (_eventSync) {
            _eWriteState = WRITE.CLOSED;
            _eventSync.notifyAll();
        }
    }

    public void readerPause() {
        synchronized (_eventSync) {
            _eReadState = READ.PAUSED;
        }
    }

    public void readerOpen() {
        synchronized (_eventSync) {
            _eReadState = READ.OPEN;
            _eventSync.notifyAll();
        }
    }

    public void readerClose() {
        synchronized (_eventSync) {
            _eReadState = READ.CLOSED;
            clear();
            _eventSync.notifyAll();
        }
    }
    
    private void clear() {
        Arrays.fill(_aoQueue, null);
        _iSize = _iHeadPos = _iTailPos = 0;
    }

    public boolean isReaderOpen() {
        return _eReadState == READ.OPEN;
    }

    public boolean isReaderOpenPaused() {
        return _eReadState == READ.PAUSED;
    }

    public boolean isReaderClosed() {
        return _eReadState == READ.CLOSED;
    }

    /////////////////////////////////

    /** Returns true if object was added, or false if it wasn't.
     * This method may block. The object must not be null. */
    public boolean write(@Nonnull T o) throws InterruptedException {
        if (o == null)
            throw new IllegalArgumentException();

        if (DEBUG) System.out.println(Thread.currentThread().getName() + " add("+o.toString()+")");

        synchronized (_eventSync) {
            while (true) {
                if (_eWriteState == WRITE.CLOSED || _eReadState == READ.CLOSED) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " closed: returning false");
                    return false;
                } else if (isFull()) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " full: waiting");
                    _eventSync.wait();
                } else {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " writing " + o.toString());
                    enqueue(o);
                    _iSize++;
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " notifying other threads and returning");
                    _eventSync.notifyAll();
                    return true;
                }
            }
        }
    }

    private boolean isFull() {
        return _iSize >= _aoQueue.length;
    }

    protected void enqueue(@Nonnull T o) {
        _aoQueue[_iTailPos] = o;
        _iTailPos++;
        if (_iTailPos >= _aoQueue.length) {
            _iTailPos = 0;
        }
    }

    /** Retrieves the head of the queue. May return null if no object is removed.
     * This method may block. */
    public @CheckForNull T read() throws InterruptedException {
        if (DEBUG) System.out.println(Thread.currentThread().getName() + " enter take()");
        
        synchronized (_eventSync) {
            while (true) {
                if (_eReadState == READ.PAUSED) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " paused: waiting");
                    _eventSync.wait();
                } else if (_eReadState == READ.CLOSED) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " reader closed: returning null");
                    return null;
                } else if (isEmpty()) {
                    if (_eWriteState == WRITE.CLOSED) {
                        if (DEBUG) System.out.println(Thread.currentThread().getName() + " empty & writer closed: closing reader & returning null");
                        _eReadState = READ.CLOSED;
                        return null;
                    } else {
                        if (DEBUG) System.out.println(Thread.currentThread().getName() + " empty: waiting");
                        _eventSync.wait();
                    }
                } else {
                    return dequeue();
                }
            }
        }
    }

    private boolean isEmpty() {
        return _iSize <= 0;
    }

    /** Always called within <code>syncronized (_eventSync)</code> */
    private @Nonnull T dequeue() {
        T o = _aoQueue[_iHeadPos];
        if (DEBUG) System.out.println(Thread.currentThread().getName() + " removing object: " + o.toString());
        _aoQueue[_iHeadPos] = null;
        _iHeadPos++;
        if (_iHeadPos >= _aoQueue.length)
            _iHeadPos = 0;
        _iSize--;
        _eventSync.notifyAll();
        return o;
    }

}
