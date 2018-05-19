package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.awscore.AwsResponse;

@Generated("software.amazon.awssdk:codegen")
public abstract class JsonProtocolTestsResponse extends AwsResponse {
    protected JsonProtocolTestsResponse(Builder builder) {
        super(builder);
    }

    public interface Builder extends AwsResponse.Builder {
        @Override
        JsonProtocolTestsResponse build();
    }

    protected abstract static class BuilderImpl extends AwsResponse.BuilderImpl implements Builder {
        protected BuilderImpl() {
        }

        protected BuilderImpl(JsonProtocolTestsResponse response) {
            super(response);
        }
    }
}
