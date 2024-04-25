import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.utils.ToString;

/**
 * Contains classes used at runtime by the code generator classes for waiter acceptors generated from JMESPath
 * expressions.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class WaitersRuntime {
    /**
     * The default acceptors that should be matched *last* in the list of acceptors used by the SDK client waiters.
     */
    public static final List<WaiterAcceptor<Object>> DEFAULT_ACCEPTORS = Collections.unmodifiableList(defaultAcceptors());

    private WaitersRuntime() {
    }

    private static List<WaiterAcceptor<Object>> defaultAcceptors() {
        return Collections.singletonList(retryOnUnmatchedResponseWaiter());
    }

    private static WaiterAcceptor<Object> retryOnUnmatchedResponseWaiter() {
        return WaiterAcceptor.retryOnResponseAcceptor(r -> true);
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
         * The value if this is an {@link Type#INTEGER} (or null otherwise).
         */
        private Integer integerValue;

        /**
         * The value if this is an {@link Type#STRING} (or null otherwise).
         */
        private String stringValue;

        /**
         * The value if this is an {@link Type#LIST} (or null otherwise).
         */
        private List<Object> listValue;

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
            } else if (value instanceof Integer) {
                this.type = Type.INTEGER;
                this.integerValue = (Integer) value;
            } else if (value instanceof Collection) {
                this.type = Type.LIST;
                this.listValue = new ArrayList<>(((Collection<?>) value));
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
         * Retrieve the actual value that this represents (this will be the same value passed to the constructor).
         */
        public Object value() {
            switch (type) {
                case NULL: return null;
                case POJO: return pojoValue;
                case INTEGER: return integerValue;
                case STRING: return stringValue;
                case BOOLEAN: return booleanValue;
                case LIST: return listValue;
                default: throw new IllegalStateException();
            }
        }

        /**
         * Retrieve the actual value that this represents, as a list.
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

            if (type != Type.POJO) {
                throw new IllegalArgumentException("Cannot flatten a " + type);
            }

            return Value.newProjection(pojoValue.sdkFields().stream()
                                                .map(f -> f.getValueOrDefault(pojoValue))
                                                .filter(Objects::nonNull)
                                                .collect(toList()));
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

            if (type != Type.LIST) {
                throw new IllegalArgumentException("Unsupported type for filter function: " + type);
            }

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

            throw new IllegalArgumentException("Unsupported type for length function: " + type);
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

            if (type == Type.LIST) {
                return new Value(listValue.stream().anyMatch(v -> Objects.equals(v, rhs.value())));
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

            if (type == Type.INTEGER) {
                switch (comparison) {
                    case "<": return new Value(integerValue < rhs.integerValue);
                    case "<=": return new Value(integerValue <= rhs.integerValue);
                    case ">": return new Value(integerValue > rhs.integerValue);
                    case ">=": return new Value(integerValue >= rhs.integerValue);
                    case "==": return new Value(Objects.equals(integerValue, rhs.integerValue));
                    case "!=": return new Value(!Objects.equals(integerValue, rhs.integerValue));
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
                case STRING:
                    return !stringValue.isEmpty();
                case BOOLEAN:
                    return booleanValue;
                default:
                    return false;
            }
        }

        /**
         * Project the provided function across all values in this list. Assumes this is a LIST and isProjection is true.
         */
        private Value project(Function<Value, Value> functionToApply) {
            return new Value(listValue.stream()
                                      .map(Value::new)
                                      .map(functionToApply)
                                      .map(Value::value)
                                      .collect(toList()),
                             true);
        }

        /**
         * The JMESPath type of this value.
         */
        private enum Type {
            POJO,
            LIST,
            BOOLEAN,
            STRING,
            INTEGER,
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

    /**
     * A {@link WaiterAcceptor} implementation that checks for a specific HTTP response status, regardless of whether it's
     * reported by a response or an exception.
     */
    public static final class ResponseStatusAcceptor implements WaiterAcceptor<SdkResponse> {
        private final int statusCode;
        private final WaiterState waiterState;

        public ResponseStatusAcceptor(int statusCode, WaiterState waiterState) {
            this.statusCode = statusCode;
            this.waiterState = waiterState;
        }

        @Override
        public WaiterState waiterState() {
            return waiterState;
        }

        @Override
        public boolean matches(SdkResponse response) {
            return response.sdkHttpResponse() != null &&
                   response.sdkHttpResponse().statusCode() == statusCode;
        }

        @Override
        public boolean matches(Throwable throwable) {
            if (throwable instanceof SdkServiceException) {
                return ((SdkServiceException) throwable).statusCode() == statusCode;
            }

            return false;
        }
    }
}
