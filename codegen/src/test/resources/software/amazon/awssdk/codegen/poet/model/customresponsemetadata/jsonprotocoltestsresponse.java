package software.amazon.awssdk.services.jsonprotocoltests.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;

@Generated("software.amazon.awssdk:codegen")
public abstract class JsonProtocolTestsResponse extends AwsResponse {
    private final JsonProtocolTestsResponseMetadata responseMetadata;

    protected JsonProtocolTestsResponse(Builder builder) {
        super(builder);
        this.responseMetadata = builder.responseMetadata();
    }

    @Override
    public JsonProtocolTestsResponseMetadata responseMetadata() {
        return responseMetadata;
    }

    public interface Builder extends AwsResponse.Builder {
        @Override
        JsonProtocolTestsResponse build();

        @Override
        JsonProtocolTestsResponseMetadata responseMetadata();

        @Override
        Builder responseMetadata(AwsResponseMetadata metadata);
    }

    protected abstract static class BuilderImpl extends AwsResponse.BuilderImpl implements Builder {
        private JsonProtocolTestsResponseMetadata responseMetadata;

        protected BuilderImpl() {
        }

        protected BuilderImpl(JsonProtocolTestsResponse response) {
            super(response);
            this.responseMetadata = response.responseMetadata();
        }

        @Override
        public JsonProtocolTestsResponseMetadata responseMetadata() {
            return responseMetadata;
        }

        @Override
        public Builder responseMetadata(AwsResponseMetadata responseMetadata) {
            this.responseMetadata = JsonProtocolTestsResponseMetadata.create(responseMetadata);
            return this;
        }
    }
}
