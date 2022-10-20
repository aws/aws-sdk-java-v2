import java.util.Locale;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public enum ParameterType {
    STRING("String"),
    BOOLEAN("Boolean"),
    ;

    private final String name;

    ParameterType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static ParameterType fromNode(JsonNode node) {
        return fromValue(node.asString());
    }

    public static ParameterType fromValue(String value) {
        switch (value.toLowerCase(Locale.ENGLISH)) {
            case "string":
                return STRING;
            case "boolean":
                return BOOLEAN;
            default:
                throw SdkClientException.create("Unknown parameter type: " + value);
        }
    }
}
