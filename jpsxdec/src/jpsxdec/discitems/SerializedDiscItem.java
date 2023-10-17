/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.util.Misc;


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
    private static final String ID_KEY = "ID";
    private static final String TYPE_KEY = "Type";
    private static final String SECTOR_RANGE_KEY = "Sectors";

    private final LinkedHashMap<String, String> _fields = new LinkedHashMap<String, String>();

    /** Creates a new serialization class to accept information.
     * <p>
     * If {@code iIndex} and/or {@code sIndexId} are not provided,
     * this object will still work and can be serialized. However, the resulting
     * serialization cannot be deserialized.
     *
     * @param iIndex if {@code < 0} then index number is not included.
     * @param sIndexId if {@code null} then index id is not included. */
    public SerializedDiscItem(@Nonnull String sType, int iIndex,
                              @CheckForNull String sIndexId,
                              int iSectorStart, int iSectorEnd)
    {
        if (iIndex >= 0)
            addNumberNoKeyNameCheck(INDEX_KEY, iIndex);
        if (sIndexId != null)
            addStringNoKeyNameCheck(ID_KEY, sIndexId);
        addStringNoKeyNameCheck(TYPE_KEY, sType);
        addRangeNoKeyNameCheck(SECTOR_RANGE_KEY, iSectorStart, iSectorEnd);
    }

    /** Parses a serialization string and makes the information available
     *  through the accessors. */
    public SerializedDiscItem(@Nonnull String sSerialized) throws LocalizedDeserializationFail {
        if (sSerialized.matches("^\\s*$"))
            throw new LocalizedDeserializationFail(I.EMPTY_SERIALIZED_STRING());
        String[] asFields = sSerialized.split(Pattern.quote(FIELD_DELIMITER));
        for (String sField : asFields) {
            String[] asParts = sField.split(KEY_VALUE_DELIMITER);
            if (asParts.length != 2)
                throw new LocalizedDeserializationFail(I.SERIALIZATION_FIELD_IMPROPERLY_FORMATTED(sField));
            String sKey = asParts[0];
            String sValue = asParts[1];
            _fields.put(sKey, sValue);
        }
        if (!_fields.containsKey(INDEX_KEY) || !_fields.containsKey(ID_KEY) ||
            !_fields.containsKey(TYPE_KEY)  || !_fields.containsKey(SECTOR_RANGE_KEY))
            throw new LocalizedDeserializationFail(I.SERIALIZATION_MISSING_REQUIRED_FIELDS(sSerialized));
    }

    /** Converts the data into a string. No additional data may be added to the
     *  object without throwing an exception. */
    public @Nonnull String serialize() {

        LinkedHashMap<String, String> fieldsCopy = new LinkedHashMap<String, String>(_fields);

        StringBuilder sb = new StringBuilder();

        // want to handle the required fields first
        if (fieldsCopy.containsKey(INDEX_KEY)) {
            sb.append(INDEX_KEY);
            sb.append(KEY_VALUE_DELIMITER);
            sb.append(fieldsCopy.remove(INDEX_KEY));
            sb.append(FIELD_DELIMITER);
        }
        if (fieldsCopy.containsKey(ID_KEY)) {
            sb.append(ID_KEY);
            sb.append(KEY_VALUE_DELIMITER);
            sb.append(fieldsCopy.remove(ID_KEY));
            sb.append(FIELD_DELIMITER);
        }

        sb.append(SECTOR_RANGE_KEY);
        sb.append(KEY_VALUE_DELIMITER);
        sb.append(fieldsCopy.remove(SECTOR_RANGE_KEY));

        sb.append(FIELD_DELIMITER);

        sb.append(TYPE_KEY);
        sb.append(KEY_VALUE_DELIMITER);
        sb.append(fieldsCopy.remove(TYPE_KEY));

        // add remaining field/value pair
        for (Map.Entry<String, String> entry : fieldsCopy.entrySet()) {
            sb.append(FIELD_DELIMITER);

            sb.append(entry.getKey());
            sb.append(KEY_VALUE_DELIMITER);
            sb.append(entry.getValue());
        }

        return sb.toString();
    }

    // =========================================================================

    final public void addString(@Nonnull String sFieldName, @Nonnull String sValue) {
        checkValidKeyName(sFieldName);
        addStringNoKeyNameCheck(sFieldName, sValue);
    }
    private void addStringNoKeyNameCheck(@Nonnull String sFieldName, @Nonnull String sValue) {
        checkValidKey(sFieldName);
        checkValidValue(sValue);
        _fields.put(sFieldName, sValue);
    }

    final public void addYesNo(@Nonnull String sFieldName, boolean blnYesNo) {
        checkValidKeyName(sFieldName);
        addStringNoKeyNameCheck(sFieldName, blnYesNo ? "Yes" : "No");
    }

    final public void addNumber(@Nonnull String sFieldName, long lngValue) {
        checkValidKeyName(sFieldName);
        addNumberNoKeyNameCheck(sFieldName, lngValue);
    }
    private void addNumberNoKeyNameCheck(@Nonnull String sFieldName, long lngValue) {
        checkValidKey(sFieldName);
        _fields.put(sFieldName, String.format("%d", lngValue));
    }

    /**
     * @param lngStart Must be {@code >= 0}
     * @param lngEnd   Must be {@code >= 0}
     */
    final public void addRange(@Nonnull String sFieldName, long lngStart, long lngEnd) {
        checkValidKeyName(sFieldName);
        addRangeNoKeyNameCheck(sFieldName, lngStart, lngEnd);
    }

    /**
     * @param lngStart Must be {@code >= 0}
     * @param lngEnd   Must be {@code >= 0}
     */
    public void addRangeNoKeyNameCheck(@Nonnull String sFieldName, long lngStart, long lngEnd) {
        if (lngStart < 0 || lngEnd < 0)
            throw new IllegalArgumentException("Range values must be >= 0");
        checkValidKey(sFieldName);
        _fields.put(sFieldName, String.format("%d-%d", lngStart, lngEnd) );
    }

    /**
     * @param lngNumerator   Must be {@code >= 0}
     * @param lngDenominator Must be {@code >= 0}
     */
    public void addFraction(@Nonnull String sFieldName, long lngNumerator, long lngDenominator) {
        if (lngNumerator < 0 || lngDenominator < 0)
            throw new IllegalArgumentException("Fraction values must be >= 0");
        checkValidKey(sFieldName);
        checkValidKeyName(sFieldName);
        _fields.put(sFieldName, String.format("%d/%d", lngNumerator, lngDenominator) );
    }

    private static void checkValidValue(String sValue) {
        if (sValue.matches(".*[:\\|].*"))
            throw new IllegalArgumentException("Keys and Values cannot contain ':' or '|'");
    }
    private void checkValidKeyName(String sKey) {
        if ((INDEX_KEY.equals(sKey) || ID_KEY.equals(sKey) || TYPE_KEY.equals(sKey) || SECTOR_RANGE_KEY.equals(sKey)))
            throw new IllegalArgumentException("Key cannot be one of the default required ones " + sKey);
    }
    private void checkValidKey(String sKey) {
        if (sKey.length() == 0)
            throw new IllegalArgumentException("Empty key or value?");
        if (sKey.matches(".*[:\\|].*"))
            throw new IllegalArgumentException("Keys and Values cannot contain ':' or '|'");
        if (_fields.containsKey(sKey))
            throw new IllegalArgumentException("Key already exists " + sKey);
    }

    // =========================================================================
    // Reading fields

    public boolean hasField(@Nonnull String sFieldName) {
        return _fields.containsKey(sFieldName);
    }

    public @Nonnull String getString(@Nonnull String sFieldName) throws LocalizedDeserializationFail {
        String sValue = _fields.get(sFieldName);
        if (sValue == null)
            throw new LocalizedDeserializationFail(I.SERIALIZATION_FIELD_NOT_FOUND(sFieldName));
        return sValue;
    }

    public @Nonnull boolean getYesNo(@Nonnull String sFieldName) throws LocalizedDeserializationFail {
        String sValue = getString(sFieldName);

        if ("No".equalsIgnoreCase(sValue))
            return false;
        else if ("Yes".equalsIgnoreCase(sValue))
            return true;
        else
            throw new LocalizedDeserializationFail(I.FIELD_HAS_INVALID_VALUE_STR(sFieldName, sValue));
    }

    public long getLong(@Nonnull String sFieldName) throws LocalizedDeserializationFail {
        String sValue = getString(sFieldName);

        try {
            return Long.parseLong(sValue);
        } catch (NumberFormatException e) {
            throw new LocalizedDeserializationFail(I.SERIALIZATION_FAILED_TO_CONVERT_TO_NUMBER(sValue));
        }
    }

    public int getInt(@Nonnull String sFieldName) throws LocalizedDeserializationFail {
        String sValue = getString(sFieldName);

        try {
            return Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
            throw new LocalizedDeserializationFail(I.SERIALIZATION_FAILED_TO_CONVERT_TO_NUMBER(sValue));
        }
    }

    public int getInt(@Nonnull String sFieldName, int iDefault) throws LocalizedDeserializationFail {
        String sValue = _fields.get(sFieldName);
        if (sValue == null)
            return iDefault;

        try {
            return Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
            throw new LocalizedDeserializationFail(I.SERIALIZATION_FAILED_TO_CONVERT_TO_NUMBER(sValue));
        }
    }

    public @Nonnull int[] getIntRange(@Nonnull String sFieldName) throws LocalizedDeserializationFail {
        String sValue = getString(sFieldName);
        int[] ai = Misc.splitInt(sValue, "-");
        if (ai == null || ai.length != 2)
            throw new LocalizedDeserializationFail(I.SERIALIZATION_FAILED_TO_CONVERT_TO_RANGE(sValue));

        return ai;
    }

    public @Nonnull long[] getLongRange(@Nonnull String sFieldName) throws LocalizedDeserializationFail {
        String sValue = getString(sFieldName);
        long[] alng = Misc.splitLong(sValue, "\\D+");
        if (alng == null || alng.length != 2)
            throw new LocalizedDeserializationFail(I.SERIALIZATION_FAILED_TO_CONVERT_TO_RANGE(sValue));

        return alng;
    }

    public @Nonnull long[] getFraction(@Nonnull String sFieldName) throws LocalizedDeserializationFail {
        return getLongRange(sFieldName);
    }

    // -- required fields --------------

    public @Nonnull String getType() {
        return _fields.get(TYPE_KEY);
    }

    public @Nonnull int[] getSectorRange() throws LocalizedDeserializationFail {
        return getIntRange(SECTOR_RANGE_KEY);
    }

    public int getIndex() throws LocalizedDeserializationFail {
        return getInt(INDEX_KEY);
    }

    public @Nonnull String getId() throws LocalizedDeserializationFail {
        return getString(ID_KEY);
    }

    @Override
    public String toString() {
        return _fields.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + (this._fields != null ? this._fields.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final SerializedDiscItem other = (SerializedDiscItem) obj;
        return Misc.objectEquals(_fields, other._fields);
    }

}
