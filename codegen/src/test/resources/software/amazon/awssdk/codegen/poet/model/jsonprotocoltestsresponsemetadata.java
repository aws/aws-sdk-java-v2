package software.amazon.awssdk.services.jsonprotocoltests.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsResponseMetadata;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class JsonProtocolTestsResponseMetadata extends AwsResponseMetadata {
    private JsonProtocolTestsResponseMetadata(AwsResponseMetadata responseMetadata) {
        super(responseMetadata);
    }

    public static JsonProtocolTestsResponseMetadata create(AwsResponseMetadata responseMetadata) {
        return new JsonProtocolTestsResponseMetadata(responseMetadata);
    }
}
