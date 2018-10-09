package software.amazon.awssdk.services.jsonprotocoltests.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsResponseMetadata;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class JsonProtocolTestsResponseMetadata extends AwsResponseMetadata {
    private static final String REQUEST_ID = "x-foobar-id";

    private static final String FOO_ID = "x-foo-id";

    private static final String BAR_ID = "x-bar-id";

    private JsonProtocolTestsResponseMetadata(AwsResponseMetadata responseMetadata) {
        super(responseMetadata);
    }

    public static JsonProtocolTestsResponseMetadata create(AwsResponseMetadata responseMetadata) {
        return new JsonProtocolTestsResponseMetadata(responseMetadata);
    }

    @Override
    public String requestId() {
        return getValue(REQUEST_ID);
    }

    public String fooId() {
        return getValue(FOO_ID);
    }

    public String barId() {
        return getValue(BAR_ID);
    }
}
