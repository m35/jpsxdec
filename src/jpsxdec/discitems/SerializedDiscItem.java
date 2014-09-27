/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

package jpsxdec.discitems;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import jpsxdec.util.NotThisTypeException;


/** Handles the serialization and deserialization of a {@link DiscItem}.
 * <p>
 * For serialization, this class will accept various bits of information,
 * and it will combine it into a human-readable, one-line string.
 * <p>
 * For deserialization, this class will parse a one-line string into the
 * various bits of information for easy access to the {@link DiscItem}.
 */
public class SerializedDiscItem {
    
    private static final String KEY_VALUE_DELIMITER = ":";
    private static final String FIELD_DELIMITER = "|";

    private static final String INDEX_KEY = "#";
    private static final String ID_KEY = "Id";
    private static final String TYPE_KEY = "Type";
    private static final String SECTOR_RANGE_KEY = "Sectors";

    private LinkedHashMap<String, String> _fields = new LinkedHashMap<String, String>();
    private String _sSerizedString = null;
    
    /** Creates a new serialization class to accept information. */
    public SerializedDiscItem(String sType, int iIndex, String sId, int iSectorStart, int iSectorEnd) {
        if (iIndex >= 0)
            addNumber(INDEX_KEY, iIndex);
        if (sId != null)
            addString(ID_KEY, sId);
        addString(TYPE_KEY, sType);
        addRange(SECTOR_RANGE_KEY, iSectorStart, iSectorEnd);
    }
    
    /** Parses a serialization string and makes the information available
     *  through the accessors. */
    public SerializedDiscItem(String sSerialized) throws NotThisTypeException {
        if (sSerialized.matches("^\\s*$"))
            throw new NotThisTypeException("Empty serialized string"); // I18N
        // TODO: For 1.0 make index and id normal keys: #:1|Id:FISH.STR|...
        String[] asFields = sSerialized.split(Pattern.quote(FIELD_DELIMITER));
        String[] asIndexId = asFields[0].split(" ");
        if (asIndexId.length > 2)
            throw new NotThisTypeException("Improperly formatted serialization: {0}", sSerialized); // I18N
        try {
            // remove leading #
            int iIndex = Integer.parseInt(asIndexId[0].substring(1));
            addNumber(INDEX_KEY, iIndex);
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException("Improperly formatted serialization: {0}", sSerialized); // I18N
        }
        if (asIndexId.length > 1)
            addString(ID_KEY, asIndexId[1]);

        for (int i = 1; i < asFields.length; i++) {
            deserializeField(asFields[i]);
        }
    }

    private void deserializeField(String sSerialized) throws NotThisTypeException {
        String[] asParts = sSerialized.split(KEY_VALUE_DELIMITER);
        if (asParts.length != 2)
            throw new NotThisTypeException("Improperly formatted field serialization: {0}", sSerialized); // I18N
        String sKey = asParts[0];
        String sValue = asParts[1];
        _fields.put(sKey, sValue);
    }
    
    /** Converts the data into a string. No additional data may be added to the
     *  object without throwing an exception. */
    public String serialize() {
        
        if (_sSerizedString != null) return _sSerizedString;
        
        StringBuilder sb = new StringBuilder();
        if (_fields.containsKey(ID_KEY))
            sb.append(INDEX_KEY).append(_fields.remove(INDEX_KEY));
        if (_fields.containsKey(ID_KEY))
            sb.append(' ').append(_fields.remove(ID_KEY));
        if (sb.length() > 0)
            sb.append(FIELD_DELIMITER);

        // want to handle the required fields first
        sb.append(SECTOR_RANGE_KEY);
        sb.append(KEY_VALUE_DELIMITER);
        sb.append(_fields.remove(SECTOR_RANGE_KEY));

        sb.append(FIELD_DELIMITER);

        sb.append(TYPE_KEY);
        sb.append(KEY_VALUE_DELIMITER);
        sb.append(_fields.remove(TYPE_KEY));

        // add each field/value pair
        for (Iterator<String> it = _fields.keySet().iterator(); it.hasNext();) {
            String sKey = it.next();
            
            sb.append(FIELD_DELIMITER);
            
            sb.append(sKey);
            sb.append(KEY_VALUE_DELIMITER);
            sb.append(_fields.get(sKey));
        }
        
        _sSerizedString = sb.toString();
        
        _fields = null; // save some memory
        
        return _sSerizedString;
    }
    
    final public void addString(String sFieldName, String sValue) {
        if (_sSerizedString != null) throw new IllegalStateException("Serialization object locked.");
        if (sValue != null) {
            if (sValue.contains(":")) throw new IllegalArgumentException(
                    String.format(
                    "String cannot contain '%s' or '%s', they are used as delimters.",
                    FIELD_DELIMITER, KEY_VALUE_DELIMITER ));

            _fields.put(sFieldName, sValue);
        }
    }
    
    final public void addNumber(String sFieldName, long lngValue) {
        if (_sSerizedString != null) throw new IllegalStateException("Serialization object locked.");
        _fields.put(sFieldName, String.format("%d", lngValue));
    }
    
    final public void addRange(String sFieldName, long lngStart, long lngEnd) {
        if (_sSerizedString != null) throw new IllegalStateException("Serialization object locked.");
        if (lngStart < 0 || lngEnd < 0) throw new IllegalArgumentException("Range values must be >= 0");
        _fields.put(sFieldName, String.format("%d-%d", lngStart, lngEnd) );
    }

    public void addFraction(String sFieldName, long lngNumerator, long lngDenominator) {
        if (_sSerizedString != null) throw new IllegalStateException("Serialization object locked.");
        _fields.put(sFieldName, String.format("%d/%d", lngNumerator, lngDenominator) );
    }
    
    public void addDimensions(String sFieldName, long lngWidth, long lngHeight) {
        if (_sSerizedString != null) throw new IllegalStateException("Serialization object locked.");
        if (lngWidth < 0 || lngHeight < 0) throw new IllegalArgumentException("Range values must be >= 0");
        _fields.put(sFieldName, String.format("%dx%d", lngWidth, lngHeight) );
    }
    
    public String getString(String sFieldName) {
        return _fields.get(sFieldName);
    }
    
    public long getLong(String sFieldName) throws NotThisTypeException {
        String sValue = _fields.get(sFieldName);
        if (sValue == null) throw new NotThisTypeException("{0} field not found.", sFieldName); // I18N
        
        try {
            return Long.parseLong(sValue);
        } catch (NumberFormatException e) {
            throw new NotThisTypeException("Failed to convert serialized field to long: {0}", sValue); // I18N
        }
    }
    
    public int getInt(String sFieldName) throws NotThisTypeException {
        String sValue = _fields.get(sFieldName);
        if (sValue == null) throw new NotThisTypeException("{0} field not found.", sFieldName); // I18N
        
        try {
            return Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
            throw new NotThisTypeException("Failed to convert serialized field to long: {0}", sValue); // I18N
        }
    }

    public int getInt(String sFieldName, int iDefault) throws NotThisTypeException {
        String sValue = _fields.get(sFieldName);
        if (sValue == null)
            return iDefault;

        try {
            return Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
            throw new NotThisTypeException("Failed to convert serialized field to long: {0}", sValue); // I18N
        }
    }

    public int[] getIntRange(String sFieldName) throws NotThisTypeException {
        String sValue = _fields.get(sFieldName);
        int[] ai = jpsxdec.util.Misc.splitInt(sValue, "\\D+");
        if (ai == null || ai.length != 2) throw new NotThisTypeException(
                "Failed to convert serialized value to range: {0}", sValue); // I18N

        return ai;
    }

    public long[] getLongRange(String sFieldName) throws NotThisTypeException {
        String sValue = _fields.get(sFieldName);
        long[] alng = jpsxdec.util.Misc.splitLong(sValue, "\\D+");
        if (alng == null || alng.length != 2) throw new NotThisTypeException(
                "Failed to convert serialized value to range: {0}", sValue); // I18N

        return alng;
    }

    public int[] getDimensions(String sFieldName) throws NotThisTypeException {
        return getIntRange(sFieldName);
    }

    public long[] getFraction(String sFieldName) throws NotThisTypeException {
        return getLongRange(sFieldName);
    }

    // -- required fields --------------
    
    public String getType() {
        return getString(TYPE_KEY);
    }

    public int[] getSectorRange() throws NotThisTypeException {
        return getIntRange(SECTOR_RANGE_KEY);
    }

    public int getIndex() throws NotThisTypeException {
        return getInt(INDEX_KEY);
    }

    public String getId() {
        return getString(ID_KEY);
    }

    @Override
    public String toString() {
        return _fields.toString();
    }

}
