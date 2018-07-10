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

package software.amazon.awssdk.services.dynamodb.document.internal;

import static software.amazon.awssdk.utils.BinaryUtils.copyAllBytesFrom;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.services.dynamodb.document.AttributeUpdate;
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.IncompatibleTypeException;
import software.amazon.awssdk.services.dynamodb.document.Item;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;

/**
 * Internal utilities.  Not meant for general use.  May change without notice.
 */
public final class InternalUtils {

    private InternalUtils() {
    }

    /**
     * Returns a non-null list of <code>Item</code>'s given the low level
     * list of item information.
     */
    public static List<Item> toItemList(List<Map<String, AttributeValue>> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        List<Item> result = new ArrayList<Item>(items.size());
        for (Map<String, AttributeValue> item : items) {
            result.add(Item.fromMap(toSimpleMapValue(item)));
        }
        return result;
    }

    /**
     * Converts an <code>Item</code> into the low-level representation;
     * or null if the input is null.
     */
    public static Map<String, AttributeValue> toAttributeValues(Item item) {
        if (item == null) {
            return null;
        }
        // row with multiple attributes
        Map<String, AttributeValue> result = new LinkedHashMap<String, AttributeValue>();
        for (Map.Entry<String, Object> entry : item.attributes()) {
            result.put(entry.getKey(), toAttributeValue(entry.getValue()));
        }
        return result;
    }

    /**
     * Converts a map of string to simple objects into the low-level
     * representation; or null if the input is null.
     */
    public static Map<String, AttributeValue> fromSimpleMap(
            Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        // row with multiple attributes
        Map<String, AttributeValue> result = new LinkedHashMap<String, AttributeValue>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), toAttributeValue(entry.getValue()));
        }
        return result;
    }

    /**
     * Converts a list of <code>AttributeUpdate</code> into the low-level
     * representation; or null if the input is null.
     */
    public static Map<String, AttributeValueUpdate> toAttributeValueUpdate(
            List<AttributeUpdate> attributesToUpdate) {
        if (attributesToUpdate == null) {
            return null;
        }

        Map<String, AttributeValueUpdate> result = new LinkedHashMap<String, AttributeValueUpdate>();

        for (AttributeUpdate attribute : attributesToUpdate) {
            AttributeValueUpdate.Builder attributeToUpdateBuilder = AttributeValueUpdate.builder()
                    .action(attribute.getAction());
            if (attribute.value() != null) {
                attributeToUpdateBuilder.value(toAttributeValue(attribute.value()));
            } else if (attribute.getAttributeValues() != null) {
                attributeToUpdateBuilder.value(toAttributeValue(attribute
                                                                     .getAttributeValues()));
            }
            result.put(attribute.getAttributeName(), attributeToUpdateBuilder.build());
        }

        return result;
    }

    /**
     * Converts a simple value into the low-level {@code <AttributeValue/>}
     * representation.
     *
     * @param value
     *            the given value which can be one of the followings:
     * <ul>
     * <li>String</li>
     * <li>Set&lt;String></li>
     * <li>Number (including any subtypes and primitive types)</li>
     * <li>Set&lt;Number></li>
     * <li>byte[]</li>
     * <li>Set&lt;byte[]></li>
     * <li>ByteBuffer</li>
     * <li>Set&lt;ByteBuffer></li>
     * <li>Boolean or boolean</li>
     * <li>null</li>
     * <li>Map&lt;String,T>, where T can be any type on this list but must not
     * induce any circular reference</li>
     * <li>List&lt;T>, where T can be any type on this list but must not induce
     * any circular reference</li>
     * </ul>
     * @return a non-null low level representation of the input object value
     *
     * @throws UnsupportedOperationException
     *             if the input object type is not supported
     */
    public static AttributeValue toAttributeValue(Object value) {
        AttributeValue.Builder resultBuilder = AttributeValue.builder();
        if (value == null) {
            return resultBuilder.nul(Boolean.TRUE).build();
        } else if (value instanceof Boolean) {
            return resultBuilder.bool((Boolean) value).build();
        } else if (value instanceof String) {
            return resultBuilder.s((String) value).build();
        } else if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) value;
            return resultBuilder.n(bd.toPlainString()).build();
        } else if (value instanceof Number) {
            return resultBuilder.n(value.toString()).build();
        } else if (value instanceof byte[]) {
            return resultBuilder.b(SdkBytes.fromByteArray((byte[]) value)).build();
        } else if (value instanceof ByteBuffer) {
            return resultBuilder.b(SdkBytes.fromByteBuffer((ByteBuffer) value)).build();
        } else if (value instanceof Set) {
            // default to an empty string set if there is no element
            @SuppressWarnings("unchecked")
            Set<Object> set = (Set<Object>) value;
            if (set.size() == 0) {
                resultBuilder.ss(new ArrayList<>());
                return resultBuilder.build();
            }
            Object element = set.iterator().next();
            if (element instanceof String) {
                @SuppressWarnings("unchecked")
                Set<String> ss = (Set<String>) value;
                resultBuilder.ss(new ArrayList<String>(ss));
            } else if (element instanceof Number) {
                @SuppressWarnings("unchecked")
                Set<Number> in = (Set<Number>) value;
                List<String> out = new ArrayList<String>(set.size());
                for (Number n : in) {
                    BigDecimal bd = InternalUtils.toBigDecimal(n);
                    out.add(bd.toPlainString());
                }
                resultBuilder.ns(out);
            } else if (element instanceof byte[]) {
                @SuppressWarnings("unchecked")
                Set<byte[]> in = (Set<byte[]>) value;
                List<SdkBytes> out = new ArrayList<>(set.size());
                for (byte[] buf : in) {
                    out.add(SdkBytes.fromByteArray(buf));
                }
                resultBuilder.bs(out);
            } else if (element instanceof ByteBuffer) {
                @SuppressWarnings("unchecked")
                Set<ByteBuffer> in = (Set<ByteBuffer>) value;
                List<SdkBytes> out = new ArrayList<>(set.size());
                for (ByteBuffer buf : in) {
                    out.add(SdkBytes.fromByteBuffer(buf));
                }
                resultBuilder.bs(out);
            } else {
                throw new UnsupportedOperationException("element type: "
                                                        + element.getClass());
            }
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> in = (List<Object>) value;
            List<AttributeValue> out = new ArrayList<AttributeValue>();
            for (Object v : in) {
                out.add(toAttributeValue(v));
            }
            resultBuilder.l(out);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> in = (Map<String, Object>) value;
            Map<String, AttributeValue> attrs = new HashMap<>();
            for (Map.Entry<String, Object> e : in.entrySet()) {
                attrs.put(e.getKey(), toAttributeValue(e.getValue()));
                //resultBuilder.addMEntry(e.getKey(), toAttributeValue(e.getValue()));
            }
            resultBuilder.m(attrs);
        } else {
            throw new UnsupportedOperationException("value type: "
                                                    + value.getClass());
        }
        return resultBuilder.build();
    }

    /**
     * Converts a list of low-level <code>AttributeValue</code> into a list of
     * simple values. Each value in the returned list can be one of the
     * followings:
     *
     * <ul>
     * <li>String</li>
     * <li>Set&lt;String></li>
     * <li>Number (including any subtypes and primitive types)</li>
     * <li>Set&lt;Number></li>
     * <li>byte[]</li>
     * <li>Set&lt;byte[]></li>
     * <li>ByteBuffer</li>
     * <li>Set&lt;ByteBuffer></li>
     * <li>Boolean or boolean</li>
     * <li>null</li>
     * <li>Map&lt;String,T>, where T can be any type on this list but must not
     * induce any circular reference</li>
     * <li>List&lt;T>, where T can be any type on this list but must not induce
     * any circular reference</li>
     * </ul>
     */
    public static List<Object> toSimpleList(List<AttributeValue> attrValues) {
        if (attrValues == null) {
            return null;
        }
        List<Object> result = new ArrayList<Object>(attrValues.size());
        for (AttributeValue attrValue : attrValues) {
            Object value = toSimpleValue(attrValue);
            result.add(value);
        }
        return result;
    }

    /**
     * Convenient method to convert a list of low-level
     * <code>AttributeValue</code> into a list of values of the same type T.
     * Each value in the returned list can be one of the followings:
     * <ul>
     * <li>String</li>
     * <li>Set&lt;String></li>
     * <li>Number (including any subtypes and primitive types)</li>
     * <li>Set&lt;Number></li>
     * <li>byte[]</li>
     * <li>Set&lt;byte[]></li>
     * <li>ByteBuffer</li>
     * <li>Set&lt;ByteBuffer></li>
     * <li>Boolean or boolean</li>
     * <li>null</li>
     * <li>Map&lt;String,T>, where T can be any type on this list but must not
     * induce any circular reference</li>
     * <li>List&lt;T>, where T can be any type on this list but must not induce
     * any circular reference</li>
     * </ul>
     */
    public static <T> List<T> toSimpleListValue(List<AttributeValue> values) {
        if (values == null) {
            return null;
        }

        List<T> result = new ArrayList<T>(values.size());
        for (AttributeValue v : values) {
            T t = toSimpleValue(v);
            result.add(t);
        }
        return result;
    }

    public static <T> Map<String, T> toSimpleMapValue(
            Map<String, AttributeValue> values) {
        if (values == null) {
            return null;
        }

        Map<String, T> result = new LinkedHashMap<String, T>(values.size());
        for (Map.Entry<String, AttributeValue> entry : values.entrySet()) {
            T t = toSimpleValue(entry.getValue());
            result.put(entry.getKey(), t);
        }
        return result;
    }

    /**
     * Returns the string representation of the given value; or null if the
     * value is null. For <code>BigDecimal</code> it will be the string
     * representation without an exponent field.
     */
    public static String valToString(Object val) {
        if (val instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) val;
            return bd.toPlainString();
        }
        if (val == null) {
            return null;
        }
        if (val instanceof String
            || val instanceof Boolean
            || val instanceof Number) {
            return val.toString();
        }
        throw new IncompatibleTypeException("Cannot convert " + val.getClass() + " into a string");
    }

    /**
     * Converts a low-level <code>AttributeValue</code> into a simple value,
     * which can be one of the followings:
     *
     * <ul>
     * <li>String</li>
     * <li>Set&lt;String></li>
     * <li>Number (including any subtypes and primitive types)</li>
     * <li>Set&lt;Number></li>
     * <li>byte[]</li>
     * <li>Set&lt;byte[]></li>
     * <li>ByteBuffer</li>
     * <li>Set&lt;ByteBuffer></li>
     * <li>Boolean or boolean</li>
     * <li>null</li>
     * <li>Map&lt;String,T>, where T can be any type on this list but must not
     * induce any circular reference</li>
     * <li>List&lt;T>, where T can be any type on this list but must not induce
     * any circular reference</li>
     * </ul>
     *
     * @throws IllegalArgumentException
     *             if an empty <code>AttributeValue</code> value is specified
     */
    static <T> T toSimpleValue(AttributeValue value) {
        if (value == null) {
            return null;
        }
        if (Boolean.TRUE.equals(value.nul())) {
            return null;
        } else if (Boolean.FALSE.equals(value.nul())) {
            throw new UnsupportedOperationException("False-NULL is not supported in DynamoDB");
        } else if (value.bool() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) value.bool();
            return t;
        } else if (value.s() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) value.s();
            return t;
        } else if (value.n() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) new BigDecimal(value.n());
            return t;
        } else if (value.b() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) value.b().asByteArray();
            return t;
        } else if (value.ss() != null && !(value.ss() instanceof SdkAutoConstructList)) {
            @SuppressWarnings("unchecked")
            T t = (T) new LinkedHashSet<String>(value.ss());
            return t;
        } else if (value.ns() != null && !(value.ns() instanceof SdkAutoConstructList)) {
            Set<BigDecimal> set = new LinkedHashSet<BigDecimal>(value.ns().size());
            for (String s : value.ns()) {
                set.add(new BigDecimal(s));
            }
            @SuppressWarnings("unchecked")
            T t = (T) set;
            return t;
        } else if (value.bs() != null && !(value.bs() instanceof SdkAutoConstructList)) {
            Set<byte[]> set = new LinkedHashSet<byte[]>(value.bs().size());
            for (SdkBytes bb : value.bs()) {
                set.add(copyAllBytesFrom(bb.asByteBuffer()));
            }
            @SuppressWarnings("unchecked")
            T t = (T) set;
            return t;
        } else if (value.l() != null && !(value.l() instanceof SdkAutoConstructList)) {
            @SuppressWarnings("unchecked")
            T t = (T) toSimpleList(value.l());
            return t;
        } else if (value.m() != null && !(value.m() instanceof SdkAutoConstructMap)) {
            @SuppressWarnings("unchecked")
            T t = (T) toSimpleMapValue(value.m());
            return t;
        } else {
            throw new IllegalArgumentException(
                    "Attribute value must not be empty: " + value);
        }
    }

    /**
     * Returns the minimum of the two input integers taking null into account.
     * Returns null if both integers are null. Otherwise, a null Integer is
     * treated as infinity.
     */
    public static Integer minimum(Integer one, Integer two) {
        if (one == null) {
            return two;
        } else if (two == null) {
            return one;
        } else if (one < two) {
            return one;
        } else {
            return two;
        }
    }

    /**
     * Returns the low level representation of a collection of <code>Expected</code>.
     */
    public static Map<String, ExpectedAttributeValue> toExpectedAttributeValueMap(
            Collection<Expected> expectedSet) {
        if (expectedSet == null) {
            return null;
        }
        Map<String, ExpectedAttributeValue> expectedMap =
                new LinkedHashMap<String, ExpectedAttributeValue>();
        for (Expected expected : expectedSet) {
            final String attr = expected.getAttribute();
            final Object[] values = expected.values();
            ExpectedAttributeValue.Builder eavBuilder = ExpectedAttributeValue.builder();
            if (values != null) {
                if (values.length > 0) {
                    // convert from list of object values to list of AttributeValues
                    AttributeValue[] avs = InternalUtils.toAttributeValues(values);
                    eavBuilder.attributeValueList(avs);
                } else {
                    throw new IllegalStateException("Bug!");
                }
            }
            ComparisonOperator op = expected.getComparisonOperator();
            if (op == null) {
                throw new IllegalArgumentException(
                        "Comparison operator for attribute " + expected.getAttribute()
                        + " must be specified");
            }
            eavBuilder.comparisonOperator(op);
            expectedMap.put(attr, eavBuilder.build());
        }
        if (expectedSet.size() != expectedMap.size()) {
            throw new IllegalArgumentException("duplicates attribute names not allowed in input");
        }
        return Collections.unmodifiableMap(expectedMap);
    }

    /**
     * Returns the low level representation of a collection of <code>Filter</code>.
     */
    public static Map<String, Condition> toAttributeConditionMap(Collection<? extends Filter<?>> filters) {
        if (filters == null) {
            return null;
        }
        Map<String, Condition> conditionMap = new LinkedHashMap<String, Condition>();
        for (Filter<?> filter : filters) {
            final String attr = filter.getAttribute();
            final Object[] values = filter.values();
            Condition.Builder conditionBuilder = Condition.builder();
            if (values != null) {
                if (values.length > 0) {
                    // convert from list of object values to list of AttributeValues
                    AttributeValue[] avs = InternalUtils.toAttributeValues(values);
                    conditionBuilder.attributeValueList(avs);
                } else {
                    throw new IllegalStateException("Bug!");
                }
            }
            ComparisonOperator op = filter.getComparisonOperator();
            if (op == null) {
                throw new IllegalArgumentException(
                        "Comparison operator for attribute " + filter.getAttribute()
                        + " must be specified");
            }
            conditionBuilder.comparisonOperator(op);
            conditionMap.put(attr, conditionBuilder.build());
        }
        if (filters.size() != conditionMap.size()) {
            throw new IllegalArgumentException("duplicates attribute names not allowed in input");
        }
        return Collections.unmodifiableMap(conditionMap);
    }

    /**
     * Converts the input array of values into an array of low level
     * representation of those values.
     *
     * A value in the input array can be one of the followings:
     *
     * <ul>
     * <li>String</li>
     * <li>Set&lt;String></li>
     * <li>Number (including any subtypes and primitive types)</li>
     * <li>Set&lt;Number></li>
     * <li>byte[]</li>
     * <li>Set&lt;byte[]></li>
     * <li>ByteBuffer</li>
     * <li>Set&lt;ByteBuffer></li>
     * <li>Boolean or boolean</li>
     * <li>null</li>
     * <li>Map&lt;String,T>, where T can be any type on this list but must not
     * induce any circular reference</li>
     * <li>List&lt;T>, where T can be any type on this list but must not induce
     * any circular reference</li>
     * </ul>
     */
    public static AttributeValue[] toAttributeValues(Object[] values) {
        AttributeValue[] attrValues = new AttributeValue[values.length];
        for (int i = 0; i < values.length; i++) {
            attrValues[i] = InternalUtils.toAttributeValue(values[i]);
        }
        return attrValues;
    }

    /**
     * Converts the specified primary key into the low-level representation.
     */
    public static Map<String, AttributeValue> toAttributeValueMap(
            Collection<KeyAttribute> primaryKey) {
        if (primaryKey == null) {
            return null;
        }
        Map<String, AttributeValue> keys = new LinkedHashMap<String, AttributeValue>();
        for (KeyAttribute keyAttr : primaryKey) {
            keys.put(keyAttr.name(),
                     InternalUtils.toAttributeValue(keyAttr.value()));
        }
        return Collections.unmodifiableMap(keys);
    }

    /**
     * Converts the specified primary key into the low-level representation.
     */
    public static Map<String, AttributeValue> toAttributeValueMap(
            PrimaryKey primaryKey) {
        if (primaryKey == null) {
            return null;
        }
        return toAttributeValueMap(primaryKey.getComponents());
    }

    /**
     * Converts the specified primary key into the low-level representation.
     */
    public static Map<String, AttributeValue> toAttributeValueMap(
            KeyAttribute... primaryKey) {
        if (primaryKey == null) {
            return null;
        }
        return toAttributeValueMap(Arrays.asList(primaryKey));
    }

    /**
     * Converts a number into BigDecimal representation.
     */
    public static BigDecimal toBigDecimal(Number n) {
        if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        }
        return new BigDecimal(n.toString());
    }

    public static Set<BigDecimal> toBigDecimalSet(Number... val) {
        Set<BigDecimal> set = new LinkedHashSet<BigDecimal>(val.length);
        for (Number n : val) {
            set.add(InternalUtils.toBigDecimal(n));
        }
        return set;
    }

    public static Set<BigDecimal> toBigDecimalSet(Set<Number> vals) {
        Set<BigDecimal> set = new LinkedHashSet<BigDecimal>(vals.size());
        for (Number n : vals) {
            set.add(InternalUtils.toBigDecimal(n));
        }
        return set;
    }

    /**
     * Append the custom user-agent string.
     */
    public static <X extends AwsRequest> X applyUserAgent(X request) {
        final AwsRequestOverrideConfiguration newCfg = request.overrideConfiguration()
                                                              .map(AwsRequestOverrideConfiguration::toBuilder)
                                                              .orElse(AwsRequestOverrideConfiguration.builder())
                                                              .addApiName(apiName -> apiName.name("dynamodb-table-api").version(VersionInfo.SDK_VERSION))
                                                              .build();

        return (X) request.toBuilder()
                .overrideConfiguration(newCfg)
                .build();
    }

    public static void rejectNullValue(Object val) {
        if (val == null) {
            throw new IllegalArgumentException("Input value must not be null");
        }
    }

    public static void rejectNullInput(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("Input must not be null");
        }
    }

    public static void rejectEmptyInput(Object[] input) {
        if (input.length == 0) {
            throw new IllegalArgumentException("At least one input must be specified");
        }
    }

    public static void rejectNullOrEmptyInput(Object[] input) {
        rejectNullInput(input);
        rejectEmptyInput(input);
    }

    public static void checkInvalidAttrName(String attrName) {
        if (attrName == null || attrName.trim().length() == 0) {
            throw new IllegalArgumentException("Attribute name must not be null or empty");
        }
    }

    public static void checkInvalidAttribute(String attrName, Object val) {
        checkInvalidAttrName(attrName);
        rejectNullValue(val);
    }
}
