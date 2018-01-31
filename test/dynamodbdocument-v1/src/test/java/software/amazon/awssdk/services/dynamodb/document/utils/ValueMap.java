/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb.document.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.core.util.json.JacksonUtils;
import software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils;
import software.amazon.awssdk.services.dynamodb.document.internal.ItemValueConformer;


/**
 * Utility class for value maps.
 */
public class ValueMap extends FluentHashMap<String, Object> {
    private static final long serialVersionUID = 1L;
    private static final ItemValueConformer VALUE_CONFORMER = new ItemValueConformer();

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withString(String key, String val) {
        super.put(key, val);
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withNumber(String key, BigDecimal val) {
        super.put(key, val);
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withNumber(String key, Number val) {
        super.put(key, InternalUtils.toBigDecimal(val));
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withInt(String key, int val) {
        return withNumber(key, Integer.valueOf(val));
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withLong(String key, long val) {
        return withNumber(key, Long.valueOf(val));
    }


    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withBinary(String key, byte[] val) {
        super.put(key, val);
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withStringSet(String key, Set<String> val) {
        super.put(key, val);
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withStringSet(String key, String... val) {
        super.put(key, new LinkedHashSet<String>(Arrays.asList(val)));
        return this;
    }


    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withNumberSet(String key, Set<BigDecimal> val) {
        super.put(key, val);
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withNumberSet(String key, BigDecimal... val) {
        super.put(key, new LinkedHashSet<BigDecimal>(Arrays.asList(val)));
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withNumberSet(String key, Number... val) {
        super.put(key, InternalUtils.toBigDecimalSet(val));
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withBinarySet(String key, Set<byte[]> val) {
        super.put(key, val);
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withBinarySet(String key, byte[]... val) {
        super.put(key, new LinkedHashSet<byte[]>(Arrays.asList(val)));
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withList(String key, List<?> val) {
        super.put(key, val == null ? null : new ArrayList<Object>(val));
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given values as a list.
     */
    public ValueMap withList(String key, Object... vals) {
        super.put(key,
                  vals == null ? null : new ArrayList<Object>(Arrays.asList(vals)));
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * given value.
     */
    public ValueMap withMap(String key, Map<String, ?> val) {
        super.put(key, val);
        return this;
    }

    /**
     * Sets the value of the specified key in the current ValueMap to the
     * boolean value.
     */
    public ValueMap withBoolean(String key, boolean val) {
        super.put(key, Boolean.valueOf(val));
        return this;
    }

    /**
     * Sets the value of the specified key to null.
     */
    public ValueMap withNull(String key) {
        super.put(key, null);
        return this;
    }

    /**
     * Sets the value of the specified key to an object represented by the JSON
     * structure passed.
     */
    public ValueMap withJson(String key, String jsonValue) {
        super.put(key, VALUE_CONFORMER.transform(JacksonUtils.fromJsonString(jsonValue, Object.class)));
        return this;
    }

    /**
     * Sets the value of the specified key to the given value. A
     * value can be a
     * <ul>
     * <li>Number</li>
     * <li>String</li>DefaultMetricCollectorFactory
     * <li>binary (ie byte array or byte buffer)</li>
     * <li>boolean</li>
     * <li>null</li>
     * <li>list (of any of the types on this list)</li>
     * <li>map (with string key to value of any of the types on this list)</li>
     * <li>set (of any of the types on this list)</li>
     * </ul>
     */
    public ValueMap with(String key, Object val) {
        if (val == this) {
            throw new IllegalArgumentException("Self reference is not allowed");
        }
        // TODO: fail fast if val is not a supported type
        super.put(key, val);
        return this;
    }
}
