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

package software.amazon.awssdk.utils;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * <p>This class assists in validating arguments. The validation methods are
 * based along the following principles:
 * <ul>
 *   <li>An invalid {@code null} argument causes a {@link NullPointerException}.</li>
 *   <li>A non-{@code null} argument causes an {@link IllegalArgumentException}.</li>
 *   <li>An invalid index into an array/collection/map/string causes an {@link IndexOutOfBoundsException}.</li>
 * </ul>
 *
 * <p>All exceptions messages are
 * <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax">format strings</a>
 * as defined by the Java platform. For example:</p>
 *
 * <pre>
 * Validate.isTrue(i &gt; 0, "The value must be greater than zero: %d", i);
 * Validate.notNull(surname, "The surname must not be %s", null);
 * </pre>
 *
 * <p>This class's source was modified from the Apache commons-lang library: https://github.com/apache/commons-lang/</p>
 *
 * <p>#ThreadSafe#</p>
 * @see java.lang.String#format(String, Object...)
 */
@SdkProtectedApi
public final class Validate {
    private static final String DEFAULT_IS_NULL_EX_MESSAGE = "The validated object is null";

    private Validate() {
    }

    // isTrue
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the argument condition is {@code true}; otherwise
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating a
     * primitive number or using your own custom validation expression.</p>
     *
     * <pre>
     * Validate.isTrue(i &gt;= min &amp;&amp; i &lt;= max, "The value must be between &#37;d and &#37;d", min, max);
     * Validate.isTrue(myObject.isOk(), "The object is not okay");</pre>
     *
     * @param expression  the boolean expression to check
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @throws IllegalArgumentException if expression is {@code false}
     */
    public static void isTrue(final boolean expression, final String message, final Object... values) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }

    // notNull
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument is not {@code null};
     * otherwise throwing an exception with the specified message.
     *
     * <pre>Validate.notNull(myObject, "The object must not be null");</pre>
     *
     * @param <T> the object type
     * @param object  the object to check
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message
     * @return the validated object (never {@code null} for method chaining)
     * @throws NullPointerException if the object is {@code null}
     */
    public static <T> T notNull(final T object, final String message, final Object... values) {
        if (object == null) {
            throw new NullPointerException(String.format(message, values));
        }
        return object;
    }

    /**
     * <p>Validate that the specified field/param is not {@code null};
     * otherwise throwing an exception with a precanned message that includes the parameter name.
     *
     * <pre>Validate.paramNotNull(myObject, "myObject");</pre>
     *
     * @param <T> the object type
     * @param object  the object to check
     * @param paramName  The name of the param or field being checked.
     * @return the validated object (never {@code null} for method chaining)
     * @throws NullPointerException if the object is {@code null}
     */
    public static <T> T paramNotNull(final T object, final String paramName) {
        if (object == null) {
            throw new NullPointerException(String.format("%s must not be null.", paramName));
        }
        return object;
    }

    /**
     * <p>Validate that the specified char sequence is neither
     * {@code null}, a length of zero (no characters), empty nor
     * whitespace; otherwise throwing an exception with the specified
     * message.
     *
     * <pre>Validate.paramNotBlank(myCharSequence, "myCharSequence");</pre>
     *
     * @param <T> the char sequence type
     * @param chars  the character sequence to check
     * @param paramName  The name of the param or field being checked.
     * @return the validated char sequence (never {@code null} for method chaining)
     * @throws NullPointerException if the char sequence is {@code null}
     */
    public static <T extends CharSequence> T paramNotBlank(final T chars, final String paramName) {
        if (chars == null) {
            throw new NullPointerException(String.format("%s must not be null.", paramName));
        }
        if (StringUtils.isBlank(chars)) {
            throw new IllegalArgumentException(String.format("%s must not be blank or empty.", paramName));
        }
        return chars;
    }

    /**
     * <p>Validate the stateful predicate is true for the given object and return the object;
     * otherwise throw an exception with the specified message.</p>
     *
     * {@code String value = Validate.validState(someString, s -> s.length() == 0, "must be blank got: %s", someString);}
     *
     *
     * @param <T> the object type
     * @param object  the object to check
     * @param test  the predicate to apply, will return true if the object is valid
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message
     * @return the validated object
     * @throws NullPointerException if the object is {@code null}
     */
    public static <T> T validState(final T object, final Predicate<T> test, final String message, final Object... values) {
        if (!test.test(object)) {
            throw new IllegalStateException(String.format(message, values));
        }
        return object;
    }

    // notEmpty array
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument array is neither {@code null}
     * nor a length of zero (no elements); otherwise throwing an exception
     * with the specified message.
     *
     * <pre>Validate.notEmpty(myArray, "The array must not be empty");</pre>
     *
     * @param <T> the array type
     * @param array  the array to check, validated not null by this method
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @return the validated array (never {@code null} method for chaining)
     * @throws NullPointerException if the array is {@code null}
     * @throws IllegalArgumentException if the array is empty
     */
    public static <T> T[] notEmpty(final T[] array, final String message, final Object... values) {
        if (array == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (array.length == 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return array;
    }

    // notEmpty collection
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument collection is neither {@code null}
     * nor a size of zero (no elements); otherwise throwing an exception
     * with the specified message.
     *
     * <pre>Validate.notEmpty(myCollection, "The collection must not be empty");</pre>
     *
     * @param <T> the collection type
     * @param collection  the collection to check, validated not null by this method
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @return the validated collection (never {@code null} method for chaining)
     * @throws NullPointerException if the collection is {@code null}
     * @throws IllegalArgumentException if the collection is empty
     */
    public static <T extends Collection<?>> T notEmpty(final T collection, final String message, final Object... values) {
        if (collection == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return collection;
    }

    // notEmpty map
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument map is neither {@code null}
     * nor a size of zero (no elements); otherwise throwing an exception
     * with the specified message.
     *
     * <pre>Validate.notEmpty(myMap, "The map must not be empty");</pre>
     *
     * @param <T> the map type
     * @param map  the map to check, validated not null by this method
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @return the validated map (never {@code null} method for chaining)
     * @throws NullPointerException if the map is {@code null}
     * @throws IllegalArgumentException if the map is empty
     */
    public static <T extends Map<?, ?>> T notEmpty(final T map, final String message, final Object... values) {
        if (map == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return map;
    }

    // notEmpty string
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument character sequence is
     * neither {@code null} nor a length of zero (no characters);
     * otherwise throwing an exception with the specified message.
     *
     * <pre>Validate.notEmpty(myString, "The string must not be empty");</pre>
     *
     * @param <T> the character sequence type
     * @param chars  the character sequence to check, validated not null by this method
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @return the validated character sequence (never {@code null} method for chaining)
     * @throws NullPointerException if the character sequence is {@code null}
     * @throws IllegalArgumentException if the character sequence is empty
     */
    public static <T extends CharSequence> T notEmpty(final T chars, final String message, final Object... values) {
        if (chars == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (chars.length() == 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return chars;
    }

    // notBlank string
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument character sequence is
     * neither {@code null}, a length of zero (no characters), empty
     * nor whitespace; otherwise throwing an exception with the specified
     * message.
     *
     * <pre>Validate.notBlank(myString, "The string must not be blank");</pre>
     *
     * @param <T> the character sequence type
     * @param chars  the character sequence to check, validated not null by this method
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @return the validated character sequence (never {@code null} method for chaining)
     * @throws NullPointerException if the character sequence is {@code null}
     * @throws IllegalArgumentException if the character sequence is blank
     */
    public static <T extends CharSequence> T notBlank(final T chars, final String message, final Object... values) {
        if (chars == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (StringUtils.isBlank(chars)) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return chars;
    }

    // noNullElements array
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument array is neither
     * {@code null} nor contains any elements that are {@code null};
     * otherwise throwing an exception with the specified message.
     *
     * <pre>Validate.noNullElements(myArray, "The array is null or contains null.");</pre>
     *
     * @param <T> the array type
     * @param array  the array to check, validated not null by this method
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message
     * @return the validated array (never {@code null} method for chaining)
     * @throws NullPointerException if the array is {@code null}
     * @throws IllegalArgumentException if an element is {@code null}
     */
    public static <T> T[] noNullElements(final T[] array, final String message, final Object... values) {
        Validate.notNull(array, message);
        for (T anArray : array) {
            if (anArray == null) {
                throw new IllegalArgumentException(String.format(message, values));
            }
        }
        return array;
    }

    // noNullElements iterable
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument iterable is neither
     * {@code null} nor contains any elements that are {@code null};
     * otherwise throwing an exception with the specified message.
     *
     * <pre>Validate.noNullElements(myCollection, "The collection is null or contains null.");</pre>
     *
     * @param <T> the iterable type
     * @param iterable  the iterable to check, validated not null by this method
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message.
     * @return the validated iterable (never {@code null} method for chaining)
     * @throws NullPointerException if the array is {@code null}
     * @throws IllegalArgumentException if an element is {@code null}
     */
    public static <T extends Iterable<?>> T noNullElements(final T iterable, final String message, final Object... values) {
        Validate.notNull(iterable, DEFAULT_IS_NULL_EX_MESSAGE);
        int i = 0;
        for (Iterator<?> it = iterable.iterator(); it.hasNext(); i++) {
            if (it.next() == null) {
                throw new IllegalArgumentException(String.format(message, values));
            }
        }
        return iterable;
    }

    // validState
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the stateful condition is {@code true}; otherwise
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating a
     * primitive number or using your own custom validation expression.</p>
     *
     * <pre>Validate.validState(this.isOk(), "The state is not OK: %s", myObject);</pre>
     *
     * @param expression  the boolean expression to check
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @throws IllegalStateException if expression is {@code false}
     */
    public static void validState(final boolean expression, final String message, final Object... values) {
        if (!expression) {
            throw new IllegalStateException(String.format(message, values));
        }
    }

    // inclusiveBetween
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument object fall between the two
     * inclusive values specified; otherwise, throws an exception with the
     * specified message.</p>
     *
     * <pre>Validate.inclusiveBetween(0, 2, 1, "Not in boundaries");</pre>
     *
     * @param <T> the type of the argument object
     * @param start  the inclusive start value, not null
     * @param end  the inclusive end value, not null
     * @param value  the object to validate, not null
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @throws IllegalArgumentException if the value falls outside the boundaries
     */
    public static <T extends Comparable<U>, U> T inclusiveBetween(final U start, final U end, final T value,
                                                                  final String message, final Object... values) {
        if (value.compareTo(start) < 0 || value.compareTo(end) > 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return value;
    }

    /**
    * Validate that the specified primitive value falls between the two
    * inclusive values specified; otherwise, throws an exception with the
    * specified message.
    *
    * <pre>Validate.inclusiveBetween(0, 2, 1, "Not in range");</pre>
    *
    * @param start the inclusive start value
    * @param end   the inclusive end value
    * @param value the value to validate
    * @param message the exception message if invalid, not null
    *
    * @throws IllegalArgumentException if the value falls outside the boundaries
    */
    public static long inclusiveBetween(final long start, final long end, final long value, final String message) {
        if (value < start || value > end) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
    * Validate that the specified primitive value falls between the two
    * inclusive values specified; otherwise, throws an exception with the
    * specified message.
    *
    * <pre>Validate.inclusiveBetween(0.1, 2.1, 1.1, "Not in range");</pre>
    *
    * @param start the inclusive start value
    * @param end   the inclusive end value
    * @param value the value to validate
    * @param message the exception message if invalid, not null
    *
    * @throws IllegalArgumentException if the value falls outside the boundaries
    */
    public static double inclusiveBetween(final double start, final double end, final double value, final String message) {
        if (value < start || value > end) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // exclusiveBetween
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument object fall between the two
     * exclusive values specified; otherwise, throws an exception with the
     * specified message.</p>
     *
     * <pre>Validate.exclusiveBetween(0, 2, 1, "Not in boundaries");</pre>
     *
     * @param <T> the type of the argument object
     * @param start  the exclusive start value, not null
     * @param end  the exclusive end value, not null
     * @param value  the object to validate, not null
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @throws IllegalArgumentException if the value falls outside the boundaries
     */
    public static <T extends Comparable<U>, U> T exclusiveBetween(final U start, final U end, final T value,
                                                                     final String message, final Object... values) {
        if (value.compareTo(start) <= 0 || value.compareTo(end) >= 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return value;
    }

    /**
    * Validate that the specified primitive value falls between the two
    * exclusive values specified; otherwise, throws an exception with the
    * specified message.
    *
    * <pre>Validate.exclusiveBetween(0, 2, 1, "Not in range");</pre>
    *
    * @param start the exclusive start value
    * @param end   the exclusive end value
    * @param value the value to validate
    * @param message the exception message if invalid, not null
    *
    * @throws IllegalArgumentException if the value falls outside the boundaries
    */
    public static long exclusiveBetween(final long start, final long end, final long value, final String message) {
        if (value <= start || value >= end) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
    * Validate that the specified primitive value falls between the two
    * exclusive values specified; otherwise, throws an exception with the
    * specified message.
    *
    * <pre>Validate.exclusiveBetween(0.1, 2.1, 1.1, "Not in range");</pre>
    *
    * @param start the exclusive start value
    * @param end   the exclusive end value
    * @param value the value to validate
    * @param message the exception message if invalid, not null
    *
    * @throws IllegalArgumentException if the value falls outside the boundaries
    */
    public static double exclusiveBetween(final double start, final double end, final double value, final String message) {
        if (value <= start || value >= end) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // isInstanceOf
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the argument is an instance of the specified class; otherwise
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary class</p>
     *
     * <pre>Validate.isInstanceOf(OkClass.class, object, "Wrong class, object is of class %s",
     *   object.getClass().getName());</pre>
     *
     * @param type  the class the object must be validated against, not null
     * @param obj  the object to check, null throws an exception
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @throws IllegalArgumentException if argument is not of specified class
     */
    public static <T, U> U isInstanceOf(final Class<U> type, final T obj, final String message, final Object... values) {
        if (!type.isInstance(obj)) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return type.cast(obj);
    }

    // isAssignableFrom
    //---------------------------------------------------------------------------------

    /**
     * Validates that the argument can be converted to the specified class, if not throws an exception.
     *
     * <p>This method is useful when validating if there will be no casting errors.</p>
     *
     * <pre>Validate.isAssignableFrom(SuperClass.class, object.getClass());</pre>
     *
     * <p>The message of the exception is &quot;The validated object can not be converted to the&quot;
     * followed by the name of the class and &quot;class&quot;</p>
     *
     * @param superType  the class the class must be validated against, not null
     * @param type  the class to check, not null
     * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @throws IllegalArgumentException if argument can not be converted to the specified class
     */
    public static <T> Class<? extends T> isAssignableFrom(final Class<T> superType, final Class<?> type,
                                        final String message, final Object... values) {
        if (!superType.isAssignableFrom(type)) {
            throw new IllegalArgumentException(String.format(message, values));
        }

        return (Class<? extends T>) type;
    }

    /**
     * Asserts that the given number is positive (non-negative and non-zero).
     *
     * @param num Number to validate
     * @param fieldName Field name to display in exception message if not positive.
     * @return Number if positive.
     */
    public static int isPositive(int num, String fieldName) {
        if (num <= 0) {
            throw new IllegalArgumentException(String.format("%s must be positive", fieldName));
        }
        return num;
    }

    /**
     * Asserts that the given number is positive (non-negative and non-zero).
     *
     * @param num Number to validate
     * @param fieldName Field name to display in exception message if not positive.
     * @return Number if positive.
     */
    public static long isPositive(long num, String fieldName) {
        if (num <= 0) {
            throw new IllegalArgumentException(String.format("%s must be positive", fieldName));
        }
        return num;
    }

    public static int isNotNegative(int num, String fieldName) {

        if (num < 0) {
            throw new IllegalArgumentException(String.format("%s must not be negative", fieldName));
        }

        return num;
    }

    /**
     * Asserts that the given duration is positive (non-negative and non-zero).
     *
     * @param duration Number to validate
     * @param fieldName Field name to display in exception message if not positive.
     * @return Duration if positive.
     */
    public static Duration isPositive(Duration duration, String fieldName) {
        if (duration == null) {
            throw new IllegalArgumentException(String.format("%s cannot be null", fieldName));
        }

        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(String.format("%s must be positive", fieldName));
        }
        return duration;
    }

    /**
     * Asserts that the given duration is positive (non-negative and non-zero) or null.
     *
     * @param duration Number to validate
     * @param fieldName Field name to display in exception message if not positive.
     * @return Duration if positive or null.
     */
    public static Duration isPositiveOrNull(Duration duration, String fieldName) {
        if (duration == null) {
            return null;
        }

        return isPositive(duration, fieldName);
    }

    /**
     * Asserts that the given boxed integer is positive (non-negative and non-zero) or null.
     *
     * @param num Boxed integer to validate
     * @param fieldName Field name to display in exception message if not positive.
     * @return Duration if positive or null.
     */
    public static Integer isPositiveOrNull(Integer num, String fieldName) {
        if (num == null) {
            return null;
        }

        return isPositive(num, fieldName);
    }

    /**
     * Asserts that the given boxed long is positive (non-negative and non-zero) or null.
     *
     * @param num Boxed long to validate
     * @param fieldName Field name to display in exception message if not positive.
     * @return Duration if positive or null.
     */
    public static Long isPositiveOrNull(Long num, String fieldName) {
        if (num == null) {
            return null;
        }

        return isPositive(num, fieldName);
    }

    /**
     * Asserts that the given duration is positive (non-negative and non-zero).
     *
     * @param duration Number to validate
     * @param fieldName Field name to display in exception message if not positive.
     * @return Duration if positive.
     */
    public static Duration isNotNegative(Duration duration, String fieldName) {
        if (duration == null) {
            throw new IllegalArgumentException(String.format("%s cannot be null", fieldName));
        }

        if (duration.isNegative()) {
            throw new IllegalArgumentException(String.format("%s must not be negative", fieldName));
        }

        return duration;
    }

    /**
     * Returns the param if non null, otherwise gets a default value from the provided {@link Supplier}.
     *
     * @param param Param to return if non null.
     * @param defaultValue Supplier of default value.
     * @param <T> Type of value.
     * @return Value of param or default value if param was null.
     */
    public static <T> T getOrDefault(T param, Supplier<T> defaultValue) {
        paramNotNull(defaultValue, "defaultValue");
        return param != null ? param : defaultValue.get();
    }

    /**
     * Verify that only one of the objects is non null. If all objects are null this method
     * does not throw.
     *
     * @param message Error message if more than one object is non-null.
     * @param objs Objects to validate.
     * @throws IllegalArgumentException if more than one of the objects was non-null.
     */
    public static void mutuallyExclusive(String message, Object... objs) {
        boolean oneProvided = false;
        for (Object o : objs) {
            if (o != null) {
                if (oneProvided) {
                    throw new IllegalArgumentException(message);
                } else {
                    oneProvided = true;
                }
            }
        }
    }
}
