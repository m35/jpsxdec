/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.player;


/** Special threadsafe blocking queue that can be in one of several states:
 * Playing but empty - Taking will block until an element is added
 * Playing with one or more elements in queue - Taking returns immediately with an element
 * Paused - Taking will block until playing & not empty
 * Closed - Will not block at all. Ignores adds and returns null.
 * Full -
 * Stop when empty -
 *
 */
public class MultiStateBlockingQueue<T> {

    public static boolean DEBUG = false;

    final private Object _eventSync = new Object();

    private static final int RUNNING = 1;
    private static final int STOPPED = 2;
    private static final int PAUSED = 3;
    private static final int STOPPING_WHEN_EMPTY = 4;
    private static final int OVERWRITE_WHEN_FULL = 5;
    private int _iState = PAUSED;

    private static final int ADD__IGNORE = 0;
    private static final int ADD__BLOCK_WHEN_FULL = 1;
    private static final int ADD__IGNORE_WHEN_FULL = 2;
    private static final int ADD__OVERWRITE_WHEN_FULL = 3;
    private int _iAddingResponse = ADD__BLOCK_WHEN_FULL;

    private static final int TAKE__IGNORE = 10;
    private static final int TAKE__BLOCK = 11;
    private static final int TAKE__BLOCK_WHEN_EMPTY = 12;
    private static final int TAKE__IGNORE_WHEN_EMPTY = 13;
    private int _iTakingResponse = TAKE__BLOCK;

    private boolean _blnChangeToIgnoreWhenEmpty = false;

    private T[] _aoQueue;
    private int _iHeadPos;
    private int _iTailPos;
    private int _iSize;

    public MultiStateBlockingQueue(int iCapacity) {
        _aoQueue = (T[]) new Object[iCapacity];
        _iSize = _iHeadPos = _iTailPos = 0;
    }

    //////////////////////////////////

    public void play() {
        synchronized (_eventSync) {
            _iState = RUNNING;

            _iAddingResponse = ADD__BLOCK_WHEN_FULL;
            _iTakingResponse = TAKE__BLOCK_WHEN_EMPTY;

            _eventSync.notifyAll();
        }
    }

    public void stop() {
        synchronized (_eventSync) {
            _iState = STOPPED;

            _iAddingResponse = ADD__IGNORE;
            _iTakingResponse = TAKE__IGNORE;

            _eventSync.notifyAll();
        }
    }

    public void stopWhenEmpty() {
        synchronized (_eventSync) {
            _iState = STOPPING_WHEN_EMPTY;

            _blnChangeToIgnoreWhenEmpty = true;
            
            _eventSync.notifyAll();
        }
    }

    public void pause() {
        synchronized (_eventSync) {
            _iState = PAUSED;

            _iAddingResponse = ADD__BLOCK_WHEN_FULL;
            _iTakingResponse = TAKE__BLOCK;

            _eventSync.notifyAll();
        }
    }

    /////////////////////////////////


    public boolean add(T o) throws InterruptedException {
        if (o == null)
            throw new IllegalArgumentException();

        if (DEBUG) System.out.println(Thread.currentThread().getName() + " add("+o.toString()+")");

        synchronized (_eventSync) {
            while (true) {
                if (_iState == STOPPED) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " stopped: returning false");
                    return false;
                } else if (isFull() && _iState != OVERWRITE_WHEN_FULL) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " full: waiting");
                    _eventSync.wait();
                } else {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " adding " + o.toString());
                    _aoQueue[_iTailPos] = o;
                    _iTailPos++;
                    if (_iTailPos >= _aoQueue.length) {
                        _iTailPos = 0;
                    }
                    if (_iState != OVERWRITE_WHEN_FULL)
                        _iSize++;
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " notifying other threads and returning");
                    _eventSync.notifyAll();
                    return true;
                }
            }
        }
    }

    public T take() throws InterruptedException {
        if (DEBUG) System.out.println(Thread.currentThread().getName() + " enter take()");
        synchronized (_eventSync) {
            while (true) {
                if (_iState == STOPPED) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " stopped: returning null");
                    return null;
                } else if (_iState == PAUSED) {
                        if (DEBUG) System.out.println(Thread.currentThread().getName() + " paused: waiting");
                        _eventSync.wait();
                } else if (isEmpty()) {
                    if (_iState == STOPPING_WHEN_EMPTY) {
                        _iState = STOPPED;
                        return null;
                    } else {
                        if (DEBUG) System.out.println(Thread.currentThread().getName() + " empty: waiting");
                        _eventSync.wait();
                    }
                } else {
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
        }
    }

    public boolean isFull() {
        synchronized (_eventSync) {
            return _iSize == _aoQueue.length;
        }
    }

    public boolean isEmpty() {
        synchronized (_eventSync) {
            return _iSize == 0;
        }
    }

    public void clear() {
        synchronized (_eventSync) {
            for (int i = 0; i < _aoQueue.length; i++) {
                _aoQueue[i] = null;
            }
            _iSize = _iHeadPos = _iTailPos = 0;
            _eventSync.notifyAll();
        }
    }

    public Object peek() {
        synchronized (_eventSync) {
            if (isEmpty()) {
                return null;
            } else {
                return _aoQueue[_iHeadPos];
            }
        }
    }

    /////////////////////////////////////

    public int size() {
        synchronized (_eventSync) {
            return _iSize;
        }
    }

}
