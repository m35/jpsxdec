/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Add elements to this push iterator, then consumer them by generating
 * one or more read iterators. No IO exceptions can occur in using this class.
 * Does not allow for nulls.
 * This class is pretty sweet.
 */
public class BufferedPushIterator<T> implements Iterable<T> {

    private static class Element<T> {
        @CheckForNull
        public final T _value;
        @CheckForNull
        public Element<T> _next;
        public Element(@CheckForNull T value) {
            _value = value;
            _next = null;
        }

        public boolean hasPrevious() {
            return _value != null;
        }
        public @Nonnull T previous() throws NoSuchElementException {
            if (_value == null)
                throw new NoSuchElementException();
            return _value;
        }
        public boolean hasNext() {
            return _next != null;
        }
        public @Nonnull Element<T> next() throws NoSuchElementException {
            if (!hasNext())
                throw new NoSuchElementException();
            return _next;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(_value);
            if (_next == null)
                sb.append(" no next");
            else
                sb.append(" has next");
            return sb.toString();
        }

    }

    @Nonnull
    private Element<T> _tail;

    public BufferedPushIterator() {
        _tail = new Element<T>(null);
    }

    public void add(@Nonnull T element) {
        Element<T> e = new Element<T>(element);
        _tail._next = e;
        _tail = e;
    }

    /** Any number of iterators can be created to read from the same sequence
     * of elements added to this push iterator. */
    @Override
    public @Nonnull Iter<T> iterator() {
        if (_tail._value == null) {
            return new Iter<T>(_tail);
        } else {
            Element<T> e = new Element<T>(null);
            e._next = _tail;
            return new Iter<T>(e);
        }
    }

    public static class Iter<T> implements Iterator<T>, IOIterator<T> {
        @Nonnull
        private Element<T> _head;

        private Iter(@Nonnull Element<T> head) {
            _head = head;
        }


        public boolean hasPrevious() {
            return _head.hasPrevious();
        }
        public @Nonnull T peekPrevious() throws NoSuchElementException {
            return _head.previous();
        }
        @Override
        public boolean hasNext() {
            return _head.hasNext();
        }
        @Override
        public @Nonnull T next() throws NoSuchElementException {
            _head = _head.next();
            return _head.previous();
        }

        /** Duplicates this iterator into a new independent iterator. */
        public @Nonnull Iter<T> copy() {
            return new Iter<T>(_head);
        }

        @Override
        public void remove() {
            // we have no word like remove in our iterator
            throw new UnsupportedOperationException();
        }


        /** Retrieves all the elements between this element and the other, inclusive.
         * @return empty list if no elements have been added.
         * @throws IllegalArgumentException if the other iterator is not a copy of this one
         *                                  (or visa-versa), or if this iterator is ahead of the other.
         */
        public @Nonnull List<T> getElementSpanTo(@Nonnull BufferedPushIterator.Iter<T> other) {
            ArrayList<T> list = new ArrayList<T>();
            Element<T> head = _head;
            while (head != other._head) {
                if (head._value != null)
                    list.add(head._value);
                head = head._next;
                if (head == null)
                    throw new IllegalArgumentException();
            }
            if (head._value != null)
                list.add(head._value);
            return list;
        }

        @Override
        public String toString() {
            return _head.toString();
        }
    }
}

