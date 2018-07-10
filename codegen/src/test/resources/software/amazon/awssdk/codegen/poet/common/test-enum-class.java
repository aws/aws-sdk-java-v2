package software.amazon.awssdk.codegen.poet.common.model;

import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;

/**
 * Some comment on the class itself
 */
@Generated("software.amazon.awssdk:codegen")
public enum TestEnumClass {
    AVAILABLE("available"),

    PERMANENT_FAILURE("permanent-failure"),

    UNKNOWN_TO_SDK_VERSION(null);

    private final String value;

    private TestEnumClass(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Use this in place of valueOf to convert the raw string returned by the service into the enum value.
     *
     * @param value
     *        real value
     * @return TestEnumClass corresponding to the value
     */
    public static TestEnumClass fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Stream.of(TestEnumClass.values()).filter(e -> e.toString().equals(value)).findFirst().orElse(UNKNOWN_TO_SDK_VERSION);
    }

    /**
     * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK.
     * This will return all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
     *
     * @return a {@link Set} of known {@link TestEnumClass}s
     */
    public static Set<TestEnumClass> knownValues() {
        return Stream.of(values()).filter(v -> v != UNKNOWN_TO_SDK_VERSION).collect(toSet());
    }
}
