import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class JmesPathRuntime {

    private JmesPathRuntime() {
    }

    /**
     * An intermediate value for JMESPath expressions, encapsulating the different data types supported by JMESPath and the
     * operations on that data.
     */
    public static final class Value {
        /**
         * A null value.
         */
        private static final Value NULL_VALUE = new Value(null);

        /**
         * The type associated with this value.
         */
        private final Type type;

        /**
         * Whether this value is a "projection" value. Projection values are LIST values where certain operations are performed
         * on each element of the list, instead of on the entire list.
         */
        private final boolean isProjection;

        /**
         * The value if this is a {@link Type#POJO} (or null otherwise).
         */
        private SdkPojo pojoValue;

        /**
         * The value if this is an {@link Type#NUMBER} (or null otherwise).
         */
        private BigDecimal numberValue;

        /**
         * The value if this is an {@link Type#STRING} (or null otherwise).
         */
        private String stringValue;

        /**
         * The value if this is an {@link Type#LIST} (or null otherwise).
         */
        private List<Object> listValue;

        /**
         * The value if this is an {@link Type#MAP} (or null otherwise).
         */
        private Map<Object, Object> mapValue;

        /**
         * The value if this is an {@link Type#BOOLEAN} (or null otherwise).
         */
        private Boolean booleanValue;

        /**
         * Create a LIST value, specifying whether this is a projection. This is private and is usually invoked by
         * {@link #newProjection(Collection)}.
         */
        private Value(Collection<?> value, boolean projection) {
            this.type = Type.LIST;
            this.listValue = new ArrayList<>(value);
            this.isProjection = projection;
        }

        /**
         * Create a MAP value, specifying whether this is a projection. This is private and is usually invoked by
         * {@link #newProjection(Map)}.
         */
        private Value(Map<?, ?> value, boolean projection) {
            this.type = Type.MAP;
            this.mapValue = new HashMap<>(value);
            this.isProjection = projection;
        }

        /**
         * Create a non-projection value, where the value type is determined reflectively.
         */
        public Value(Object value) {
            this.isProjection = false;

            if (value == null) {
                this.type = Type.NULL;
            } else if (value instanceof SdkPojo) {
                this.type = Type.POJO;
                this.pojoValue = (SdkPojo) value;
            } else if (value instanceof String) {
                this.type = Type.STRING;
                this.stringValue = (String) value;
            } else if (value instanceof BigDecimal) {
                this.type = Type.NUMBER;
                this.numberValue = (BigDecimal) value;
            } else if (value instanceof Collection) {
                this.type = Type.LIST;
                this.listValue = new ArrayList<>(((Collection<?>) value));
            } else if (value instanceof Map) {
                this.type = Type.MAP;
                this.mapValue = new HashMap<>((Map<?, ?>) value);
            } else if (value instanceof Boolean) {
                this.type = Type.BOOLEAN;
                this.booleanValue = (Boolean) value;
            } else {
                throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
            }
        }

        /**
         * Create a {@link Type#LIST} with a {@link #isProjection} of true.
         */
        private static Value newProjection(Collection<?> values) {
            return new Value(values, true);
        }

        /**
         * Create a {@link Type#MAP} with a {@link #isProjection} of true.
         */
        private static Value newProjection(Map<?, ?> values) {
            return new Value(values, true);
        }

        /**
         * Retrieve the actual value that this represents (this will be the same value passed to the constructor).
         */
        public Object value() {
            switch (type) {
                case NULL: return null;
                case POJO: return pojoValue;
                case NUMBER: return numberValue;
                case STRING: return stringValue;
                case BOOLEAN: return booleanValue;
                case LIST: return listValue;
                case MAP: return mapValue;
                default: throw new IllegalStateException();
            }
        }

        /**
         * Retrieve the actual value that this represents, as a list of object.
         */
        public List<Object> values() {
            if (type == Type.NULL) {
                return Collections.emptyList();
            }

            if (type == Type.LIST) {
                return listValue;
            }

            return Collections.singletonList(value());
        }

        /**
         * Retrieve the actual value that this represents, as a map of objects.
         */
        private Map<Object, Object> mapValues() {
            if (type == Type.NULL) {
                return Collections.emptyMap();
            }

            if (type == Type.MAP) {
                return mapValue;
            }

            throw new IllegalStateException("Must be MAP type to get map values");
        }

       /**
         * Retrieve the actual value that this represents, as a Boolean.
         * Note that only null, boolean and string types are supported.
         */
        public Boolean booleanValue() {
            switch (type) {
                case NULL:
                    return null;
                case STRING:
                    return Boolean.parseBoolean(stringValue);
                case BOOLEAN:
                    return booleanValue;
                default:
                    throw new IllegalStateException(String.format("Cannot convert type %s to Boolean.", type));
            }
        }

       /**
         * Retrieve the actual value that this represents, as a String.
         * Note that collection types are not supported.
         */
        public String stringValue() {
            switch (type) {
                case NULL:
                    return null;
                case NUMBER:
                    return numberValue.toString();
                case STRING:
                    return stringValue;
                case BOOLEAN:
                    return booleanValue.toString();
                default:
                    throw new IllegalStateException(String.format("Cannot convert type %s to String.", type));
            }
        }

       /**
         * Retrieve the actual value that this represents, as a list of String.
         * Note that if the contents of the list is not String, an exception is thrown.
         * If the value has a different type, the code makes a best effort to return a single element
         * list of String. See {@code stringValue}.
         */
        public List<String> stringValues() {
            if (type == Type.NULL) {
                return Collections.emptyList();
            }

            if (type == Type.LIST) {
                List<String> result = new ArrayList<>();
                for (Object listEntry : listValue) {
                    Value entryAsValue = new Value(listEntry);
                    if (entryAsValue.type == Type.NULL) {
                        continue;
                    }
                    if (entryAsValue.type != Type.STRING) {
                        throw new IllegalStateException("Expected list elements to be of type String, but were "
                        + entryAsValue.type);
                    }
                    result.add(entryAsValue.stringValue);
                }
                return result;
            }

            return Collections.singletonList(stringValue());
        }

       /**
         * Retrieve the actual value that this represents, as a map of Strings.
         * Note that if the contents of the map are not String, or
         * if the value has a different type, an exception is thrown.
         */
        public Map<String, String> stringValuesMap() {
            if (type == Type.NULL) {
                return Collections.emptyMap();
            }

            if (type == Type.MAP) {
                Map<String, String> result = new HashMap<>();
                mapValue.forEach((key, value) -> {
                    Value keyAsValue = new Value(key);
                    Value entryAsValue = new Value(value);
                    if (keyAsValue.type != Type.NULL) {
                        if (!isStringType(keyAsValue) || !isStringType(entryAsValue)) {
                            throw new IllegalStateException("Keys and values must be String type");
                        }
                        result.put(keyAsValue.stringValue, entryAsValue.stringValue);
                    }
                });

                return result;
            }

            throw new IllegalArgumentException("Not of type MAP");
        }

        private boolean isStringType(Value value) {
            return value.type == Type.STRING;
        }

        /**
         * Convert this value to a new constant value, discarding the current value.
         */
        public Value constant(Value value) {
            return value;
        }

        /**
         * Convert this value to a new constant value, discarding the current value.
         */
        public Value constant(Object constant) {
            return new Value(constant);
        }

        /**
         * Execute a wildcard expression on this value: https://jmespath.org/specification.html#wildcard-expressions
         */
        public Value wildcard() {
            if (type == Type.NULL) {
                return NULL_VALUE;
            }

            if (type == Value.Type.LIST) {
                return newProjection(listValue);
            }

            if (type == Type.MAP) {
                return newProjection(mapValue);
            }

            if (type == Value.Type.POJO) {
                return newProjection(pojoValue.sdkFields().stream().map(f -> f.getValueOrDefault(pojoValue))
                                              .filter(Objects::nonNull).collect(toList()));
            }

            throw new IllegalArgumentException("Cannot use a wildcard expression on a " + type);
        }

        /**
         * Execute a flattening expression on this value: https://jmespath.org/specification.html#flatten-operator
         */
        public Value flatten() {
            if (type == Type.NULL) {
                return NULL_VALUE;
            }

            if (type != Type.LIST) {
                throw new IllegalArgumentException("Cannot flatten a " + type);
            }

            List<Object> result = new ArrayList<>();
            for (Object listEntry : listValue) {
                Value listValue = new Value(listEntry);
                if (listValue.type != Type.LIST) {
                    result.add(listEntry);
                } else {
                    result.addAll(listValue.listValue);
                }
            }

            return Value.newProjection(result);
        }

        /**
         * Retrieve an identifier from this value: https://jmespath.org/specification.html#identifiers
         */
        public Value field(String fieldName) {
            if (isProjection) {
                return project(v -> v.field(fieldName));
            }

            if (type == Type.NULL) {
                return NULL_VALUE;
            }

            if (type == Type.POJO) {
                return pojoValue.sdkFields()
                                .stream()
                                .filter(f -> f.memberName().equals(fieldName))
                                .map(f -> f.getValueOrDefault(pojoValue))
                                .map(Value::new)
                                .findAny()
                                .orElseThrow(() -> new IllegalArgumentException("No such field: " + fieldName));
            }

            throw new IllegalArgumentException("Cannot get a field from a " + type);
        }

        /**
         * Filter this value: https://jmespath.org/specification.html#filter-expressions
         */
        public Value filter(Function<Value, Value> predicate) {
            if (isProjection) {
                return project(f -> f.filter(predicate));
            }

            if (type == Type.NULL) {
                return NULL_VALUE;
            }

            if (type == Type.LIST) {
                List<Object> results = new ArrayList<>();
                listValue.forEach(entry -> {
                    Value entryValue = new Value(entry);
                    Value predicateResult = predicate.apply(entryValue);
                    if (predicateResult.isTrue()) {
                        results.add(entry);
                    }
                });
                return new Value(results);
            }

            if (type == Type.MAP) {
                Map<Object, Object> results = new HashMap<>();
                mapValue.forEach((key, entry) -> {
                    Value entryValue = new Value(entry);
                    Value predicateResult = predicate.apply(entryValue);
                    if (predicateResult.isTrue()) {
                        results.put(key, entry);
                    }
                });
                return new Value(results);
            }

            throw new IllegalArgumentException("Unsupported type for filter function: " + type);
        }

        /**
         * Execute the length function, with this value as the first parameter: https://jmespath.org/specification.html#length
         */
        public Value length() {
            if (type == Type.NULL) {
                return NULL_VALUE;
            }

            if (type == Type.STRING) {
                return new Value(stringValue.length());
            }

            if (type == Type.POJO) {
                return new Value(pojoValue.sdkFields().size());
            }

            if (type == Type.LIST) {
                return new Value(Math.toIntExact(listValue.size()));
            }

            if (type == Type.MAP) {
                return new Value(Math.toIntExact(mapValue.size()));
            }

            throw new IllegalArgumentException("Unsupported type for length function: " + type);
        }

        public Value keys() {
            if (type == Type.NULL) {
                return new Value(Collections.emptyList(), false);
            }

            if (type == Type.POJO) {
                return new Value(pojoValue.sdkFields().stream().map(SdkField::memberName).collect(toList()));
            }

            if (type == Type.MAP) {
                return new Value(mapValue.keySet());
            }

            throw new IllegalArgumentException("Unsupported type for keys function: " + type);
        }

        /**
         * Execute the contains function, with this value as the first parameter: https://jmespath.org/specification.html#contains
         */
        public Value contains(Value rhs) {
            if (type == Type.NULL) {
                return NULL_VALUE;
            }

            if (type == Type.STRING) {
                if (rhs.type != Type.STRING) {
                    // Unclear from the spec whether we can check for a boolean in a string, for example...
                    return new Value(false);
                }

                return new Value(stringValue.contains(rhs.stringValue));
            }

            Object value = rhs.value();

            if (type == Type.LIST) {
                return new Value(listValue.stream().anyMatch(v -> Objects.equals(v, value)));
            }

            if (type == Type.MAP) {
                return new Value(mapValue.containsValue(value));
            }

            throw new IllegalArgumentException("Unsupported type for contains function: " + type);
        }

        /**
         * Compare this value to another value, using the specified comparison operator:
         * https://jmespath.org/specification.html#comparison-operators
         */
        public Value compare(String comparison, Value rhs) {
            if (type != rhs.type) {
                return new Value(false);
            }

            if (type == Type.NUMBER) {
                switch (comparison) {
                    case "<": return new Value(numberValue.compareTo(rhs.numberValue) < 0);
                    case "<=": return new Value(numberValue.compareTo(rhs.numberValue) <= 0);
                    case ">": return new Value(numberValue.compareTo(rhs.numberValue) > 0);
                    case ">=": return new Value(numberValue.compareTo(rhs.numberValue) >= 0);
                    case "==": return new Value(numberValue.compareTo(rhs.numberValue) == 0);
                    case "!=": return new Value(numberValue.compareTo(rhs.numberValue) != 0);
                    default: throw new IllegalArgumentException("Unsupported comparison: " + comparison);
                }
            }

            if (type == Type.NULL || type == Type.STRING || type == Type.BOOLEAN) {
                switch (comparison) {
                    case "<":
                    case "<=":
                    case ">":
                    case ">=":
                        return NULL_VALUE; // Invalid comparison, spec says to treat as null.
                    case "==": return new Value(Objects.equals(value(), rhs.value()));
                    case "!=": return new Value(!Objects.equals(value(), rhs.value()));
                    default: throw new IllegalArgumentException("Unsupported comparison: " + comparison);
                }
            }

            throw new IllegalArgumentException("Unsupported type in comparison: " + type);
        }

        /**
         * Perform a multi-select list expression on this value: https://jmespath.org/specification.html#multiselect-list
         */
        @SafeVarargs
        public final Value multiSelectList(Function<Value, Value>... functions) {
            if (isProjection) {
                return project(v -> v.multiSelectList(functions));
            }
            if (type == Type.NULL) {
                return NULL_VALUE;
            }

            List<Object> result = new ArrayList<>();
            for (Function<Value, Value> function : functions) {
                result.add(function.apply(this).value());
            }
            return new Value(result);
        }

        /**
         * Perform a multi-select hash expression on this value:
         * https://jmespath.org/specification.html#multiselect-hash
         */
        public final Value multiSelectHash(Map<String, Function<Value, Value>> selections) {
            if (isProjection) {
                return project(v -> v.multiSelectHash(selections));
            }
            if (type == Type.NULL) {
                return NULL_VALUE;
            }
            if (type != Type.MAP) {
                throw new IllegalArgumentException("Multi-select map operation is only supported for maps");
            }

            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, Function<Value, Value>> entry : selections.entrySet()) {
                String key = entry.getKey();
                Function<Value, Value> function = entry.getValue();
                Value selectedValue = function.apply(new Value(mapValue.get(key)));
                result.put(key, selectedValue.value());
            }

            return new Value(result);
        }

        /**
         * Perform an OR comparison between this value and another one: https://jmespath.org/specification.html#or-expressions
         */
        public Value or(Value rhs) {
            if (isTrue()) {
                return this;
            } else {
                return rhs.isTrue() ? rhs : NULL_VALUE;
            }
        }

        /**
         * Perform an AND comparison between this value and another one: https://jmespath.org/specification.html#or-expressions
         */
        public Value and(Value rhs) {
            return isTrue() ? rhs : this;
        }

        /**
         * Perform a NOT conversion on this value: https://jmespath.org/specification.html#not-expressions
         */
        public Value not() {
            return new Value(!isTrue());
        }

        /**
         * Returns true is this value is "true-like" (or false otherwise): https://jmespath.org/specification.html#or-expressions
         */
        private boolean isTrue() {
            switch (type) {
                case POJO:
                    return !pojoValue.sdkFields().isEmpty();
                case LIST:
                    return !listValue.isEmpty();
                case MAP:
                   return !mapValue.isEmpty();
                case STRING:
                    return !stringValue.isEmpty();
                case BOOLEAN:
                    return booleanValue;
                default:
                    return false;
            }
        }

        /**
         * Project the provided function across all values in this list. Assumes this is a LIST or MAP and isProjection
         * is true.
         */
        private Value project(Function<Value, Value> functionToApply) {
            if (type == Type.LIST) {
                return new Value(listValue.stream().map(Value::new).map(functionToApply).map(Value::value)
                                          .filter(Objects::nonNull).collect(toList()), true);
            }

            if (type == Type.MAP) {
                return new Value(mapValue.values().stream().map(Value::new).map(functionToApply).map(Value::value)
                                         .filter(Objects::nonNull).collect(toList()), true);
            }

            throw new IllegalArgumentException("Can only project on List or Map types");
        }

        /**
         * The JMESPath type of this value.
         */
        private enum Type {
            POJO,
            LIST,
            MAP,
            BOOLEAN,
            STRING,
            NUMBER,
            NULL
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Value value = (Value) o;

            return type == value.type && Objects.equals(value(), value.value());
        }

        @Override
        public int hashCode() {
            Object value = value();

            int result = type.hashCode();
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return ToString.builder("Value")
                           .add("type", type)
                           .add("value", value())
                           .build();
        }
    }
}
