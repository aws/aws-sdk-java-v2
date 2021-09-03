package software.amazon.awssdk.codegen.poet.common.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * Some comment on the class itself
 */
@Generated("software.amazon.awssdk:codegen")
public enum TestEnumClass {
    AVAILABLE("available"),

    PERMANENT_FAILURE("permanent-failure"),

    UNKNOWN_TO_SDK_VERSION(null);

    private static final Map<String, TestEnumClass> VALUE_MAP = EnumUtils.uniqueIndex(TestEnumClass.class, TestEnumClass::toString);

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
        return VALUE_MAP.getOrDefault(value, UNKNOWN_TO_SDK_VERSION);
    }

    /**
     * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK. This will return
     * all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
     *
     * @return a {@link Set} of known {@link TestEnumClass}s
     */
    public static Set<TestEnumClass> knownValues() {
        Set<TestEnumClass> knownValues = EnumSet.allOf(TestEnumClass.class);
        knownValues.remove(UNKNOWN_TO_SDK_VERSION);
        return knownValues;
    }
}

