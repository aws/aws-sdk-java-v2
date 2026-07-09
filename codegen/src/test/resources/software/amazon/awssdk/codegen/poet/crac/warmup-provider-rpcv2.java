package software.amazon.awssdk.services.smithyrpcv2protocol.internal.crac;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.core.crac.http.CannedResponseAsyncHttpClient;
import software.amazon.awssdk.core.crac.http.CannedResponseHttpClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.smithyrpcv2protocol.SmithyRpcV2ProtocolAsyncClient;
import software.amazon.awssdk.services.smithyrpcv2protocol.SmithyRpcV2ProtocolClient;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class SmithyRpcV2ProtocolWarmUpProvider implements SdkWarmUpProvider {
    private static final byte[] CANNED_RESPONSE = new byte[] { (byte) 0xA0 };

    @Override
    public String syncClientClassName() {
        return "software.amazon.awssdk.services.smithyrpcv2protocol.SmithyRpcV2ProtocolClient";
    }

    @Override
    public String asyncClientClassName() {
        return "software.amazon.awssdk.services.smithyrpcv2protocol.SmithyRpcV2ProtocolAsyncClient";
    }

    @Override
    public void warmUpClient(ClientType clientType) {
        if (clientType == ClientType.SYNC) {
            SdkHttpClient httpClient = CannedResponseHttpClient.builder().responseBody(CANNED_RESPONSE).statusCode(200).build();
            try (SmithyRpcV2ProtocolClient client = SmithyRpcV2ProtocolClient.builder().httpClient(httpClient)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                    .region(Region.US_EAST_1).endpointOverride(URI.create("http://localhost")).build()) {
            }
        }
        if (clientType == ClientType.ASYNC) {
            SdkAsyncHttpClient asyncHttpClient = CannedResponseAsyncHttpClient.builder().responseBody(CANNED_RESPONSE)
                    .statusCode(200).build();
            try (SmithyRpcV2ProtocolAsyncClient asyncClient = SmithyRpcV2ProtocolAsyncClient.builder()
                    .httpClient(asyncHttpClient)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                    .region(Region.US_EAST_1).endpointOverride(URI.create("http://localhost")).build()) {
            }
        }
    }
}
