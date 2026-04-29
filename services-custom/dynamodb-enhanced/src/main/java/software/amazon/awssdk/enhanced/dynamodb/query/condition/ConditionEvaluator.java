/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.query.condition;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Evaluates a {@link Condition} against an item represented as a map of attribute names to values. Used by the query engine to
 * apply in-memory filters. Values in the map are typically Java types (String, Number, Boolean, etc.); comparison uses natural
 * ordering where applicable.
 */
@SdkInternalApi
public final class ConditionEvaluator {

    private ConditionEvaluator() {
    }

    /**
     * Returns true if the given condition is satisfied by the item. If condition is null, returns true.
     *
     * @param condition the condition to evaluate (may be null)
     * @param item      attribute name to value map (Java types)
     * @return true if condition is null or satisfied
     */
    public static boolean evaluate(Condition condition, Map<String, Object> item) {
        if (condition == null) {
            return true;
        }
        if (condition instanceof Condition.Comparator) {
            return evaluateComparator((Condition.Comparator) condition, item);
        }
        if (condition instanceof Condition.Between) {
            return evaluateBetween((Condition.Between) condition, item);
        }
        if (condition instanceof Condition.Function) {
            return evaluateFunction((Condition.Function) condition, item);
        }
        if (condition instanceof Condition.And) {
            Condition.And and = (Condition.And) condition;
            return evaluate(and.left(), item) && evaluate(and.right(), item);
        }
        if (condition instanceof Condition.Or) {
            Condition.Or or = (Condition.Or) condition;
            return evaluate(or.left(), item) || evaluate(or.right(), item);
        }
        if (condition instanceof Condition.Not) {
            return !evaluate(((Condition.Not) condition).inner(), item);
        }
        if (condition instanceof Condition.Group) {
            return evaluate(((Condition.Group) condition).inner(), item);
        }
        return false;
    }

    /**
     * Two-map overload that avoids merging base and joined maps into a combined HashMap. Attribute lookups check {@code primary}
     * first; if the value is null, {@code secondary} is checked. This matches the semantics of
     * {@code Map combined = new HashMap<>(secondary); combined.putAll(primary)}.
     */
    public static boolean evaluate(Condition condition, Map<String, Object> primary, Map<String, Object> secondary) {
        if (condition == null) {
            return true;
        }
        return evaluate(condition, new CombinedMapView(primary, secondary));
    }

    /**
     * Looks up an attribute from two maps (primary wins). Supports dot-path traversal for nested attributes.
     */
    public static Object lookupCombined(String key, Map<String, Object> primary, Map<String, Object> secondary) {
        Object v = resolveAttribute(primary, key);
        return v != null ? v : resolveAttribute(secondary, key);
    }

    /**
     * Resolves an attribute value from a map, supporting dot-separated paths for nested Map attributes. For example,
     * {@code resolveAttribute(item, "address.city")} will look up {@code item.get("address")} and, if that value is a
     * {@code Map}, look up {@code "city"} within it.
     *
     * @param item      the item map
     * @param attribute the attribute name, possibly dot-separated
     * @return the resolved value, or null if not found or any intermediate value is not a Map
     */
    @SuppressWarnings("unchecked")
    public static Object resolveAttribute(Map<String, Object> item, String attribute) {
        if (attribute == null || item == null) {
            return null;
        }
        if (attribute.indexOf('.') < 0) {
            return item.get(attribute);
        }
        String[] parts = attribute.split("\\.");
        Object current = item;
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static boolean evaluateComparator(Condition.Comparator c, Map<String, Object> item) {
        Object actual = resolveAttribute(item, c.attribute());
        Object expected = c.value();
        int cmp = compare(actual, expected);
        switch (c.operator()) {
            case "=":
                return cmp == 0;
            case ">":
                return cmp > 0;
            case ">=":
                return cmp >= 0;
            case "<":
                return cmp < 0;
            case "<=":
                return cmp <= 0;
            default:
                return false;
        }
    }

    private static boolean evaluateBetween(Condition.Between c, Map<String, Object> item) {
        Object actual = resolveAttribute(item, c.attribute());
        if (actual == null) {
            return false;
        }
        int low = compare(actual, c.from());
        int high = compare(actual, c.to());
        return low >= 0 && high <= 0;
    }

    @SuppressWarnings("unchecked")
    private static boolean evaluateFunction(Condition.Function c, Map<String, Object> item) {
        Object actual = resolveAttribute(item, c.attribute());
        Object val = c.value();
        switch (c.function()) {
            case "contains":
                if (actual == null) {
                    return false;
                }
                if (actual instanceof String && val instanceof String) {
                    return ((String) actual).contains((String) val);
                }
                if (actual instanceof java.util.Set) {
                    return ((java.util.Set<?>) actual).contains(val);
                }
                return false;
            case "begins_with":
                if (actual instanceof String && val instanceof String) {
                    return ((String) actual).startsWith((String) val);
                }
                return false;
            default:
                return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        if (a instanceof Number && b instanceof Number) {
            java.math.BigDecimal left = (a instanceof java.math.BigDecimal)
                                        ? (java.math.BigDecimal) a
                                        : new java.math.BigDecimal(a.toString());
            java.math.BigDecimal right = (b instanceof java.math.BigDecimal)
                                         ? (java.math.BigDecimal) b
                                         : new java.math.BigDecimal(b.toString());
            return left.compareTo(right);
        }
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable) a).compareTo(b);
            } catch (ClassCastException e) {
                return a.toString().compareTo(b.toString());
            }
        }
        return a.toString().compareTo(b.toString());
    }

    /**
     * Lightweight read-only view over two maps. {@code get} checks primary first, then secondary. Only {@code get} and
     * {@code containsKey} are supported; this is sufficient for condition evaluation.
     */
    private static final class CombinedMapView extends AbstractMap<String, Object> {
        private final Map<String, Object> primary;
        private final Map<String, Object> secondary;

        CombinedMapView(Map<String, Object> primary, Map<String, Object> secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public Object get(Object key) {
            Object v = primary.get(key);
            return v != null ? v : secondary.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return primary.containsKey(key) || secondary.containsKey(key);
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            throw new UnsupportedOperationException("CombinedMapView does not support entrySet");
        }
    }
}
