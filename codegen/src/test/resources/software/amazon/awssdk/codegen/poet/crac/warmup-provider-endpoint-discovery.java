package software.amazon.awssdk.services.endpointdiscoverytest.internal.crac;

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
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestAsyncClient;
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestClient;
import software.amazon.awssdk.services.endpointdiscoverytest.model.DescribeEndpointsRequest;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class EndpointDiscoveryTestWarmUpProvider implements SdkWarmUpProvider {
  private static final byte[] CANNED_RESPONSE = "{}".getBytes(StandardCharsets.UTF_8);

  @Override
  public String syncClientClassName() {
    return "software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestClient";
  }

  @Override
  public String asyncClientClassName() {
    return "software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestAsyncClient";
  }

  @Override
  public void warmUpClient(ClientType clientType) {
    if (clientType == ClientType.SYNC) {
      SdkHttpClient httpClient = CannedResponseHttpClient.builder().responseBody(CANNED_RESPONSE).statusCode(200).build();
      try (EndpointDiscoveryTestClient client = EndpointDiscoveryTestClient.builder()
      .httpClient(httpClient)
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
      .endpointDiscoveryEnabled(false)
      .region(Region.US_EAST_1)
      .endpointOverride(URI.create("http://localhost"))
      .build()) {
        client.describeEndpoints(DescribeEndpointsRequest.builder().build());
      }
    }
    if (clientType == ClientType.ASYNC) {
      SdkAsyncHttpClient asyncHttpClient = CannedResponseAsyncHttpClient.builder().responseBody(CANNED_RESPONSE).statusCode(200).build();
      try (EndpointDiscoveryTestAsyncClient asyncClient = EndpointDiscoveryTestAsyncClient.builder()
      .httpClient(asyncHttpClient)
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
      .endpointDiscoveryEnabled(false)
      .region(Region.US_EAST_1)
      .endpointOverride(URI.create("http://localhost"))
      .build()) {
        asyncClient.describeEndpoints(DescribeEndpointsRequest.builder().build()).join();
      }
    }
  }
}
