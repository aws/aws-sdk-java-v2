package software.amazon.awssdk.core.http;

import software.amazon.awssdk.core.AwsRequest;
import software.amazon.awssdk.core.AwsRequestOverrideConfig;

public class NoopTestAwsRequest extends AwsRequest {
    private NoopTestAwsRequest(Builder builder) {
        super(builder);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends AwsRequest.Builder {
        @Override
        NoopTestAwsRequest build();

        @Override
        Builder requestOverrideConfig(AwsRequestOverrideConfig awsRequestOverrideConfig);
    }

    private static class BuilderImpl extends AwsRequest.BuilderImpl implements Builder {

        @Override
        public NoopTestAwsRequest build() {
            return new NoopTestAwsRequest(this);
        }

        @Override
        public Builder requestOverrideConfig(AwsRequestOverrideConfig awsRequestOverrideConfig) {
            super.requestOverrideConfig(awsRequestOverrideConfig);
            return this;
        }
    }
}
