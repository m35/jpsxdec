/*
 * $Id: StringValues.java,v 1.4 2009/03/11 13:45:10 kleopatra Exp $
 *
 * Copyright 2008 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.renderer;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;

import javax.swing.filechooser.FileSystemView;

/**
 * A collection of common {@code StringValue} implementations.
 * 
 * @author Karl George Schaefer
 * @author Jeanette Winzenburg
 */
public final class StringValues {
    /**
     * A {@code StringValue} that always presents an empty string.
     */
    @SuppressWarnings("serial")
    public final static StringValue EMPTY = new StringValue() {
        public String getString(Object value) {
            return "";
        }
    };
    
    /**
     * A {@code StringValue} that presents a {@link Object#toString() toString}
     * value for the given object. If the value passed is {@code null}, this has
     * the same effect as {@link StringValues#EMPTY}.
     */
    @SuppressWarnings("serial")
    public final static StringValue TO_STRING = new StringValue() {
        public String getString(Object value) {
            return (value != null) ? value.toString() : StringValues.EMPTY.getString(value);
        }
    };

    /**
     * A {@code StringValue} that presents the current L&F display name for a
     * given file. If the value passed to {@code FILE_NAME} is not a
     * {@link File}, this has the same effect as {@link StringValues#TO_STRING}.
     */
    @SuppressWarnings("serial")
    public static final StringValue FILE_NAME = new StringValue() {
        public String getString(Object value) {
            if (value instanceof File) {
                FileSystemView fsv = FileSystemView.getFileSystemView();

                return fsv.getSystemDisplayName((File) value);
            }

            return StringValues.TO_STRING.getString(value);
        }
    };

    /**
     * A {@code StringValue} that presents the current L&F type name for a
     * given file. If the value passed to {@code FILE_TYPE} is not a
     * {@link File}, this has the same effect as {@link StringValues#TO_STRING}.
     */
    @SuppressWarnings("serial")
    public static final StringValue FILE_TYPE = new StringValue() {
        public String getString(Object value) {
            if (value instanceof File) {
                FileSystemView fsv = FileSystemView.getFileSystemView();
                
                return fsv.getSystemTypeDescription((File) value);
            }
            
            return StringValues.TO_STRING.getString(value);
        }
    };

    /**
     * Default converter for <code>Date</code> types. Uses the default format
     * as returned from <code>DateFormat</code>.
     */
    @SuppressWarnings("serial")
    public final static FormatStringValue DATE_TO_STRING = new FormatStringValue() {
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String getString(Object value) {
            if (format == null) {
                format = DateFormat.getDateInstance();
            }
            return super.getString(value);
        }
        
    };

    /**
     * Default converter for <code>Number</code> types. Uses the default format
     * as returned from <code>NumberFormat</code>.
     */
    @SuppressWarnings("serial")
    public final static FormatStringValue NUMBER_TO_STRING = new FormatStringValue() {
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String getString(Object value) {
            if (format == null) {
                format = NumberFormat.getNumberInstance();
            }
            return super.getString(value);
        }
        
    };
    
    private StringValues() {
        // does nothing
    }
}
