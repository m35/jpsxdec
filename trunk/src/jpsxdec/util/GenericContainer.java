/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * GenericContainer.java
 */

package jpsxdec.util;

/** Just holds an object. Useful when you need to change a final object. */
public class GenericContainer<T> {
    private T o;

    public GenericContainer() {}
    public GenericContainer(T o) {
        this.o = o;
    }
    
    public T get() {
        return this.o;
    }
    
    public T set(T o) {
        this.o = o;
        return this.o;
    }
}
