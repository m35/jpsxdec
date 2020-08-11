/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2020  Michael Sabin
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

package testutil;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jpsxdec.util.Misc;

public class PairList<T1, T2> extends AbstractList<Map.Entry<T1, T2>> {
    
    private static class Pair<T1, T2> implements Map.Entry<T1, T2> {
        
        private final T1 _key;
        private T2 _value;

        public Pair(T1 key, T2 value) {
            _key = key;
            _value = value;
        }
        
        public T1 getKey() {
            return _key;
        }

        public T2 getValue() {
            return _value;
        }

        public T2 setValue(T2 value) {
            T2 old = _value;
            _value = value;
            return old;
        }
        
        @Override
	public boolean equals(Object o) {
	    if (!(o instanceof Pair))
		return false;
	    Pair<?, ?> p = (Pair<?, ?>)o;
	    return Misc.objectEquals(_key  , p.getKey()  ) &&
                   Misc.objectEquals(_value, p.getValue());
	}

        @Override
	public int hashCode() {
	    return (_key   == null ? 0 :   _key.hashCode()) ^
		   (_value == null ? 0 : _value.hashCode());
	}

        @Override
	public String toString() {
	    return _key + "=" + _value;
	}       
    }

    private final List<Pair<T1, T2>> _pairs = new ArrayList<Pair<T1, T2>>();

    public void add(T1 o1, T2 o2) {
        _pairs.add(new Pair<T1, T2>(o1, o2));
    }

    @Override
    public Iterator<Map.Entry<T1, T2>> iterator() {
        return (Iterator)_pairs.iterator();
    }

    @Override
    public Map.Entry<T1, T2> get(int index) {
        return _pairs.get(index);
    }

    @Override
    public int size() {
        return _pairs.size();
    }
}
