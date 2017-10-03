package software.amazon.awssdk.codegen.poet.common.model;

import java.util.stream.Stream;
import javax.annotation.Generated;

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
}
