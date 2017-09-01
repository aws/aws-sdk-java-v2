package software.amazon.awssdk.core.http;

import software.amazon.awssdk.core.SdkRequestOverrideConfig;
import software.amazon.awssdk.core.SdkRequest;

import java.util.Optional;

public class NoopTestRequest extends SdkRequest {
    private NoopTestRequest() {

    }

    @Override
    public Optional<? extends SdkRequestOverrideConfig> requestOverrideConfig() {
        return Optional.empty();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkRequest.Builder {
        @Override
        NoopTestRequest build();
    }

    private static class BuilderImpl implements SdkRequest.Builder, Builder {

        @Override
        public SdkRequestOverrideConfig requestOverrideConfig() {
            return null;
        }

        @Override
        public NoopTestRequest build() {
            return new NoopTestRequest();
        }
    }
}
