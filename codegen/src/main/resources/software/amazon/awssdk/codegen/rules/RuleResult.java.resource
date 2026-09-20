import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class RuleResult {
    private static final RuleResult CARRY_ON = new RuleResult();
    private final String error;
    private final Endpoint endpoint;

    private RuleResult() {
        this.error = null;
        this.endpoint = null;
    }

    private RuleResult(String error) {
        this.error = Validate.paramNotNull(error, "error");
        this.endpoint = null;
    }

    private RuleResult(Endpoint endpoint) {
        this.error = null;
        this.endpoint = Validate.paramNotNull(endpoint, "endpoint");
    }

    public boolean canContinue() {
        return error == null && endpoint == null;
    }

    public boolean isResolved() {
        return !isUnresolved();
    }

    public boolean isUnresolved() {
        return error == null && endpoint == null;
    }

    public boolean isError() {
        return error != null;
    }

    public boolean isEndpoint() {
        return endpoint != null;
    }

    public String error() {
        return error;
    }

    public Endpoint endpoint() {
        return endpoint;
    }

    public static RuleResult error(String error) {
        return new RuleResult(error);
    }

    public static RuleResult carryOn() {
        return CARRY_ON;
    }

    public static RuleResult endpoint(Endpoint endpoint) {
        return new RuleResult(endpoint);
    }
}
