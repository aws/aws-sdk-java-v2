package software.amazon.awssdk.services.json.internal.crac;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.core.internal.crac.CannedResponseAsyncHttpClient;
import software.amazon.awssdk.core.internal.crac.CannedResponseHttpClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.json.JsonAsyncClient;
import software.amazon.awssdk.services.json.JsonClient;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class JsonWarmUpProvider implements SdkWarmUpProvider {
    private static final byte[] CANNED_RESPONSE = "{}".getBytes(StandardCharsets.UTF_8);

    @Override
    public void warmUp() {
        SdkHttpClient httpClient = CannedResponseHttpClient.builder().responseBody(CANNED_RESPONSE).statusCode(200).build();
        try (JsonClient client = JsonClient.builder().httpClient(httpClient)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .region(Region.US_EAST_1).endpointOverride(URI.create("http://localhost")).build()) {
        }
        SdkAsyncHttpClient asyncHttpClient = CannedResponseAsyncHttpClient.builder().responseBody(CANNED_RESPONSE)
                .statusCode(200).build();
        try (JsonAsyncClient asyncClient = JsonAsyncClient.builder().httpClient(asyncHttpClient)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .region(Region.US_EAST_1).endpointOverride(URI.create("http://localhost")).build()) {
        }
    }
}
