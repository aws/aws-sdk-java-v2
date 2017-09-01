package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.core.AwsRequest;

@Generated("software.amazon.awssdk:codegen")
public abstract class JsonProtocolTestsRequest extends AwsRequest {
    protected JsonProtocolTestsRequest(Builder builder) {
        super(builder);
    }

    @Override
    public abstract Builder toBuilder();

    public interface Builder extends AwsRequest.Builder {
        @Override
        JsonProtocolTestsRequest build();
    }

    protected abstract static class BuilderImpl extends AwsRequest.BuilderImpl implements Builder {
        protected BuilderImpl() {
        }

        protected BuilderImpl(JsonProtocolTestsRequest request) {
            super(request);
        }
    }
}
