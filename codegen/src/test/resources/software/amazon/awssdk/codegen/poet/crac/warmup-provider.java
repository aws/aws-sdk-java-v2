package software.amazon.awssdk.services.query.internal.crac;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.core.internal.crac.CannedResponseHttpClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.query.QueryClient;
import software.amazon.awssdk.services.query.model.GetOperationWithChecksumRequest;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class QueryWarmUpProvider implements SdkWarmUpProvider {
    private static final byte[] CANNED_RESPONSE = "<Response/>".getBytes(StandardCharsets.UTF_8);

    @Override
    public void warmUp() {
        SdkHttpClient httpClient = CannedResponseHttpClient.builder().responseBody(CANNED_RESPONSE).statusCode(200).build();
        try (QueryClient client = QueryClient.builder().httpClient(httpClient)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .region(Region.US_EAST_1).endpointOverride(URI.create("http://localhost")).build()) {
            client.getOperationWithChecksum(GetOperationWithChecksumRequest.builder().build());
        }
    }
}
