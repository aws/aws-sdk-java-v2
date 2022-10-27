import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class EndpointRule extends Rule {
    private final EndpointResult endpoint;

    protected EndpointRule(Builder builder, EndpointResult endpoint) {
        super(builder);
        this.endpoint = endpoint;
    }

    public EndpointResult getEndpoint() {
        return endpoint;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> visitor) {
        return visitor.visitEndpointRule(this.getEndpoint());
    }

    @Override
    public String toString() {
        return "EndpointRule{" +
               "endpoint=" + endpoint +
               ", conditions=" + conditions +
               ", documentation='" + documentation + '\'' +
               '}';
    }
}
