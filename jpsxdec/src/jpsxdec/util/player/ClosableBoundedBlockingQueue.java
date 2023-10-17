/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Normal bounded blocking queue but with the ability to 'poison' it or 'close' it immediately.
 * <p>
 * {@link #closeNow()}. will immediately unblock any waiting threads.
 * {@link #closeWhenEmpty()} will unblock any writers and will reject anything
 * else being added. When the queue is empty, it will be flagged as closed.
 * <p>
 * Items can be added in two ways. {@link #add(Object)} will block if the queue
 * is full and wait until it is no longer full, or the queue is closed.
 * {@link #addWithCapacityCheck(Object)} will never block, but if the queue
 * is full it will throw {@link IllegalStateException}. This 2nd method
 * is useful when you want an effectively unbounded queue and don't expect
 * the queue size to grow beyond its capacity. You could set the capacity
 * to {@link Integer#MAX_VALUE} which would be effectively unbounded,
 * or just some large number that should never happen, but if it does, something
 * bad has happened.
 */
public class ClosableBoundedBlockingQueue<T> {


    /**
     * The so-called "poison pill" object to indicate the end of the queue.
     */
    private static final Object POISON_PILL = new Object();

    @Nonnull
    private final Queue<Object> _queue;
    private final int _iCapacity;

    @Nonnull
    private final ReentrantLock _lock;
    @Nonnull
    private final Condition _notEmpty;
    @Nonnull
    private final Condition _notFull;
    private boolean _blnIsClosed = false;
    private boolean _blnIsPoisoned = false;

    /**
     * The last thread used to add an entry to the queue.
     * {@link #take()} will check this periodically to see if the adding thread died.
     * Hopefully will minimize the likelihood of {@link #take()} blocking a thread forever.
     * Idea borrowed from PipedInputStream.
     */
    @CheckForNull
    private Thread _addingThread;
    @CheckForNull
    private Thread _takingThread;

    public ClosableBoundedBlockingQueue(int iCapacity) {
        _iCapacity = iCapacity;
        _queue = new ArrayDeque<Object>();
        _lock = new ReentrantLock();
        _notEmpty = _lock.newCondition();
        _notFull  = _lock.newCondition();
    }

    /**
     * Blocks only if the queue is full, and the queue is not closed or poisoned.
     * @return true if the queue is still open and not poisoned, otherwise false
     * @throws NullPointerException  if the argument is null
     */
    public boolean add(@Nonnull T entry) throws InterruptedException {
        return innerAdd(entry, false);
    }

    /**
     * Adds to the queue, but instead of blocking when the size limit is
     * reached, close the queue and blow up with an exception.
     * @return true if the queue is still open and not poisoned, otherwise false
     * @throws NullPointerException  if the argument is null
     * @throws IllegalStateException if the queue is full
     */
    public boolean addWithCapacityCheck(@Nonnull T entry) {
        try {
            return innerAdd(entry, true);
        } catch (InterruptedException ex) {
            throw new RuntimeException("should not happen", ex);
        }
    }

    private boolean innerAdd(@Nonnull Object entry, boolean blnFailIfTooBig) throws InterruptedException {
        // the queue doesn't allow null entries
        // plus we use null to indicate take() should not wait
        if (entry == null)
            throw new NullPointerException();

        _lock.lock();
        try {
            // grab the current thread for take() to check
            _addingThread = Thread.currentThread();

            // always loop when dealing with thread notifications
            while (true) {

                // easy check, is this closed or poisoned?
                if (_blnIsClosed || _blnIsPoisoned) {
                    return false;

                } else {
                    int iQueueSize = _queue.size();
                    // next easy check, is there room to add?
                    if (iQueueSize < _iCapacity) {
                        _queue.add(entry);
                        _notEmpty.signalAll();
                        return true;
                    } else {

                        if (blnFailIfTooBig) {
                            closeNow();
                            throw new IllegalStateException("Queue is too big: " + iQueueSize + "/" + _iCapacity);
                        }

                        // sanity check
                        checkOtherThreadBeforeWaiting(_takingThread);

                        // all else failed, now wait for room in the queue
                        // but don't wait forever, and do all checks again in case the other thread died
                        boolean dontCareWhy = _notEmpty.await(1, TimeUnit.SECONDS);
                    }
                }
            }

        } finally {
            _lock.unlock();
        }
    }

    /**
     * Blocks until:
     * - something is available
     * - the queues is closed
     * @return null if the queue is closed or the poison pill has been encountered, thus closing the queue.
     */
    public @CheckForNull T take() throws InterruptedException {
        _lock.lock();
        try {

            // grab the current thread for add() to check
            _takingThread = Thread.currentThread();

            // always loop when dealing with thread notifications
            while (true) {

                // easy check, is this closed?
                if (_blnIsClosed) {
                    return null;
                } else {

                    // next easy check, something in the queue?
                    if (!_queue.isEmpty()) {
                        Object entry = _queue.remove();
                        _notFull.signalAll();
                        if (entry == POISON_PILL) {
                            closeNow();
                            return null;
                        } else {
                            // we know the queue only contains elements of type T or the poison pill
                            @SuppressWarnings("unchecked")
                            T suppressed = (T) entry;
                            return suppressed;
                        }
                    } else {

                        checkOtherThreadBeforeWaiting(_addingThread);

                        // all else failed, now wait for something in the queue
                        // but don't wait forever, and do all checks again in case the other thread died
                        boolean dontCareWhy = _notEmpty.await(1, TimeUnit.SECONDS);
                    }
                }
            }
        } finally {
            _lock.unlock();
        }
    }

    private static void checkOtherThreadBeforeWaiting(@CheckForNull Thread otherThread) {
        // now a thread sanity check to avoid deadlocks
        if (otherThread != null) {
            if (!otherThread.isAlive()) {
                throw new IllegalStateException(otherThread.getName() + " thread is dead, throwing exception to make sure this thread (" +
                                                Thread.currentThread().getName() + ") won't block forever");
            } else if (otherThread == Thread.currentThread()) {
                throw new IllegalStateException("The last thread to add entries was THIS thread (" +
                                                Thread.currentThread().getName() +
                                                "), throwing exception to make sure this won't block forever");
            }
        }
    }

    /**
     * Closes the queue and returns immediately.
     * Calling multiple times will only notify the threads again.
     */
    public void closeNow() {
        _lock.lock();
        try {
            _blnIsClosed = true;
            _queue.clear();
            _notEmpty.signalAll();
            _notFull.signalAll();
        } finally {
            _lock.unlock();
        }
    }

    /**
     * Blocks adding elements to this queue.
     * Calling multiple times will only notify the threads again.
     */
    public void closeWhenEmpty() {
        _lock.lock();
        try {
            if (_blnIsClosed)
                return;
            if (_blnIsPoisoned)
                return;
            _queue.add(POISON_PILL); // add at end, even if it makes the queue > max
            _notEmpty.signalAll();
            _blnIsPoisoned = true;
        } finally {
            _lock.unlock();
        }
    }

    public boolean isClosed() {
        return _blnIsClosed;
    }

}
