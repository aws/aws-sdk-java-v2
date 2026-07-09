package software.amazon.awssdk.services.xml.internal.crac;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import software.amazon.awssdk.services.xml.XmlAsyncClient;
import software.amazon.awssdk.services.xml.XmlClient;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class XmlWarmUpProvider implements SdkWarmUpProvider {
    private static final byte[] CANNED_RESPONSE = "<Response/>".getBytes(StandardCharsets.UTF_8);

    @Override
    public String syncClientClassName() {
        return "software.amazon.awssdk.services.xml.XmlClient";
    }

    @Override
    public String asyncClientClassName() {
        return "software.amazon.awssdk.services.xml.XmlAsyncClient";
    }

    @Override
    public void warmUpClient(ClientType clientType) {
        if (clientType == ClientType.SYNC) {
            SdkHttpClient httpClient = CannedResponseHttpClient.builder().responseBody(CANNED_RESPONSE).statusCode(200).build();
            try (XmlClient client = XmlClient.builder().httpClient(httpClient)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                    .region(Region.US_EAST_1).endpointOverride(URI.create("http://localhost")).build()) {
            }
        }
        if (clientType == ClientType.ASYNC) {
            SdkAsyncHttpClient asyncHttpClient = CannedResponseAsyncHttpClient.builder().responseBody(CANNED_RESPONSE)
                    .statusCode(200).build();
            try (XmlAsyncClient asyncClient = XmlAsyncClient.builder().httpClient(asyncHttpClient)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                    .region(Region.US_EAST_1).endpointOverride(URI.create("http://localhost")).build()) {
            }
        }
    }
}
