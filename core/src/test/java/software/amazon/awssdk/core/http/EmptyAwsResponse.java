package software.amazon.awssdk.core.http;

import software.amazon.awssdk.core.AwsResponse;

public class EmptyAwsResponse extends AwsResponse {
    public EmptyAwsResponse(Builder builder) {
        super(builder);
    }
    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends AwsResponse.Builder {
        @Override
        EmptyAwsResponse build();
    }

    private static class BuilderImpl extends AwsResponse.BuilderImpl implements Builder {

        @Override
        public EmptyAwsResponse build() {
            return new EmptyAwsResponse(this);
        }
    }
}
