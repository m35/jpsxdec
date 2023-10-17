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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * This class is an iterator itself and can be copied to create additional
 * iterators off the same source iterator, all of which reading from
 * the source iterator only once (yes it's kinda hard to explain).
 * Also provides the ability to look back at the last element provided.
 */
public class BufferedIOIterator<T> implements IOIterator<T> {

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
        public boolean hasNext(@Nonnull IOIterator<T> source) {
            if (_next == null)
                return source.hasNext();
            return true;
        }
        public @Nonnull Element<T> next(@Nonnull IOIterator<T> source)
                throws NoSuchElementException, IOException
        {
            if (_next == null)
                _next = new Element<T>(source.next());
            return _next;
        }
        private boolean hasBufferedNext() {
            return _next != null;
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

    //  -------------------------------------------------------

    @Nonnull
    private final IOIterator<T> _source;
    @Nonnull
    private Element<T> _head;

    public BufferedIOIterator(@Nonnull IOIterator<T> iterator) {
        _source = iterator;
        _head = new Element<T>(null);
    }
    /** Copy constructor. */
    private BufferedIOIterator(@Nonnull IOIterator<T> iterator,
                               @Nonnull Element<T> head)
    {
        _source = iterator;
        _head = head;
    }

    /** Returns true if at least one {@link #next()} has been called. */
    public boolean hasPrevious() {
        return _head.hasPrevious();
    }
    /** Returns the result of the last {@link #next()} call. */
    public @Nonnull T peekPrevious() throws NoSuchElementException {
        return _head.previous();
    }
    @Override
    public boolean hasNext() {
        return _head.hasNext(_source);
    }
    @Override
    public @Nonnull T next() throws NoSuchElementException, IOException {
        _head = _head.next(_source);
        return _head.previous();
    }

    /** Like {@link #hasNext()}, except returns if the {@link #next()} will
     * come form a previously buffered read, or from the source iterator. */
    public boolean isNextBuffered() {
        return _head.hasBufferedNext();
    }

    /** Duplicates this consumer into a new independent consumer. */
    public @Nonnull BufferedIOIterator<T> copy() {
        return new BufferedIOIterator<T>(_source, _head);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + _source.hashCode();
        hash = 29 * hash + _head.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final BufferedIOIterator<?> other = (BufferedIOIterator<?>) obj;
        if (_source != other._source)
            return false;
        if (_head != other._head)
            return false;
        return true;
    }

    /** Retrieves all the elements between this element and the other, inclusive.
     * @throws IllegalArgumentException if the other iterator is not a copy of this one
     *                                  (or visa-versa), or if this iterator is ahead of the other.
     */
    public @Nonnull List<T> getElementSpanTo(@Nonnull BufferedIOIterator<T> other) {
        ArrayList<T> list = new ArrayList<T>();
        Element<T> h = _head;
        if (h._value != null)
            list.add(h._value);
        while (h != other._head) {
            Element<T> next = h._next;
            if (next == null)
                throw new IllegalArgumentException();
            h = next;
            if (h._value == null)
                throw new IllegalStateException();
            list.add(h._value);
        }
        return list;
    }

    @Override
    public String toString() {
        return _head.toString();
    }

}

