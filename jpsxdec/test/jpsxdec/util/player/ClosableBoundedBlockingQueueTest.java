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

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class ClosableBoundedBlockingQueueTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10); // 10 seconds max per method tested

    private static final String OBJECT1 = "OBJECT1";

    @Test
    public void testAdd() throws Exception {
        ClosableBoundedBlockingQueue<String> queue = new ClosableBoundedBlockingQueue<String>(1);

        assertTrue(queue.add(OBJECT1));
        assertFalse(queue.isClosed());
    }

    @Test
    public void testAddTakeSameThread() throws Exception {
        ClosableBoundedBlockingQueue<String> queue = new ClosableBoundedBlockingQueue<String>(1);

        assertTrue(queue.add(OBJECT1));
        String actual = queue.take();
        assertSame(OBJECT1, actual);
        assertFalse(queue.isClosed());
    }

    @Test
    public void testAddTake2SameThread() throws Exception {
        ClosableBoundedBlockingQueue<String> queue = new ClosableBoundedBlockingQueue<String>(1);

        assertTrue(queue.add(OBJECT1));
        String actual = queue.take();
        assertSame(OBJECT1, actual);
        try {
            queue.take();
            fail("Expected " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
        }
    }

    @Test
    public void testClosed() throws Exception {
        ClosableBoundedBlockingQueue<String> queue = new ClosableBoundedBlockingQueue<String>(1);

        queue.closeNow();
        assertFalse(queue.add(OBJECT1));
        String actual = queue.take();
        assertNull(actual);
        assertTrue(queue.isClosed());
    }

    @Test
    public void testCloseNow() throws Exception {
        ClosableBoundedBlockingQueue<String> queue = new ClosableBoundedBlockingQueue<String>(3);

        assertTrue(queue.add(OBJECT1));
        assertTrue(queue.add(OBJECT1));
        queue.closeNow();
        assertFalse(queue.add(OBJECT1));
        String actual = queue.take();
        assertNull(actual);
        assertTrue(queue.isClosed());
    }

    @Test
    public void testPoison() throws Exception {
        ClosableBoundedBlockingQueue<String> queue = new ClosableBoundedBlockingQueue<String>(3);

        assertTrue(queue.add(OBJECT1));
        queue.closeWhenEmpty();
        assertFalse(queue.isClosed());
        assertFalse(queue.add(OBJECT1));
        assertFalse(queue.isClosed());
        String actual = queue.take();
        assertFalse(queue.isClosed());
        assertSame(OBJECT1, actual);
        assertFalse(queue.isClosed());
        actual = queue.take();
        assertNull(actual);
        assertTrue(queue.isClosed());
    }

    @Test
    public void testTakeTakeAdd() throws Exception {
        ClosableBoundedBlockingQueue<String> queue = new ClosableBoundedBlockingQueue<String>(1);

        OtherTake otherTake = new OtherTake(queue);
        otherTake.start();
        try {
            while (otherTake.isAlive() && otherTake.getState() != Thread.State.TIMED_WAITING) {
                Thread.sleep(100);
            }

            assertEquals(Thread.State.TIMED_WAITING, otherTake.getState());
            assertTrue(otherTake.isAlive());
            // check the thread stopped before setting these
            assertNull(otherTake._ex);
            assertNull(otherTake._taken);

            // add an object to the queue
            assertTrue(queue.add(OBJECT1));
            // that will unblock the thread and it will exit
            // wait for it to exit
            otherTake.join(3000);
            // fail if it didn't exit
            assertFalse(otherTake.isAlive());

            // make sure there was no exception
            assertNull(otherTake._ex);
            // and finally check that the object made it there
            assertSame(OBJECT1, otherTake._taken);

            assertFalse(queue.isClosed());
        } finally {
            otherTake.interrupt();
        }
    }


    @Test
    public void testCloseThread() throws Exception {
        ClosableBoundedBlockingQueue<String> queue = new ClosableBoundedBlockingQueue<String>(1);

        OtherTake otherTake = new OtherTake(queue);
        otherTake.start();
        try {
            while (otherTake.isAlive() && otherTake.getState() != Thread.State.TIMED_WAITING) {
                Thread.sleep(100);
            }

            assertEquals(Thread.State.TIMED_WAITING, otherTake.getState());
            assertTrue(otherTake.isAlive());
            // check the thread stopped before setting these
            assertNull(otherTake._ex);
            assertNull(otherTake._taken);

            // close both
            queue.closeNow();
            // that will unblock the thread and it will exit
            // wait for it to exit
            otherTake.join(3000);
            // fail if it didn't exit
            assertFalse(otherTake.isAlive());

            // make sure there was no exception
            assertNull(otherTake._ex);
            // and finally check that nothing was taken
            assertNull(otherTake._taken);

            assertTrue(queue.isClosed());
        } finally {
            otherTake.interrupt();
        }
    }

    @Test
    public void testPoisonThread() throws Exception {
        ClosableBoundedBlockingQueue<String> queue = new ClosableBoundedBlockingQueue<String>(1);

        OtherTake otherTake = new OtherTake(queue);
        otherTake.start();
        try {
            while (otherTake.isAlive() && otherTake.getState() != Thread.State.TIMED_WAITING) {
                Thread.sleep(100);
            }

            assertEquals(Thread.State.TIMED_WAITING, otherTake.getState());
            assertTrue(otherTake.isAlive());
            // check the thread stopped before setting these
            assertNull(otherTake._ex);
            assertNull(otherTake._taken);

            // close
            queue.closeWhenEmpty();
            // that will unblock the thread and it will exit
            // wait for it to exit
            otherTake.join(3000);
            // fail if it didn't exit
            assertFalse(otherTake.isAlive());

            // make sure there was no exception
            assertNull(otherTake._ex);
            // and finally check that nothing was taken
            assertNull(otherTake._taken);

            assertTrue(queue.isClosed());
        } finally {
            otherTake.interrupt();
        }
    }

    private static class OtherTake extends Thread {
        private final ClosableBoundedBlockingQueue<String> _otherMine;
        private transient String _taken;
        private transient Exception _ex;
        public OtherTake(ClosableBoundedBlockingQueue<String> otherMine) {
            _otherMine = otherMine;
        }
        @Override
        public void run() {
            System.out.println("Thread started");
            try {
                _taken = _otherMine.take();
            } catch (Exception ex) {
                _ex = ex;
            }
            System.out.println("Thread ending");
        }
    }

}
