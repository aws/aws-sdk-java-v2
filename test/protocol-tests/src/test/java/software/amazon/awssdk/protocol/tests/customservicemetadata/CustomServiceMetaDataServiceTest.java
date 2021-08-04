package software.amazon.awssdk.protocol.tests.customservicemetadata;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;


import java.net.URI;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.protocoljsonrpc.ProtocolJsonRpcClient;
import software.amazon.awssdk.services.protocoljsonrpccustomized.ProtocolJsonRpcCustomizedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocoljsonrpccustomized.model.SimpleRequest;
import software.amazon.awssdk.services.protocoljsonrpccustomized.model.SimpleResponse;
import software.amazon.awssdk.services.protocoljsonrpc.model.AllTypesRequest;
import software.amazon.awssdk.services.protocoljsonrpc.model.AllTypesResponse;

public class CustomServiceMetaDataServiceTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(WireMockConfiguration.wireMockConfig()
            .port(0)
            .fileSource(new SingleRootFileSource("src/test/resources")));

    private static final String JSON_BODY = "{\"StringMember\":\"foo\"}";
    private static final String JSON_BODY_GZIP = "compressed_json_body.gz";
    private static final String JSON_BODY_Crc32_CHECKSUM = "3049587505";
    private static final String JSON_BODY_GZIP_Crc32_CHECKSUM = "3023995622";
    private static final StaticCredentialsProvider FAKE_CREDENTIALS_PROVIDER =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"));

    @Test
    public void clientWithCustomizedMetadata_has_contenttype_as_mentioned_customization() {

        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Encoding", "gzip")
                .withHeader("x-amz-crc32", JSON_BODY_GZIP_Crc32_CHECKSUM)
                .withBodyFile(JSON_BODY_GZIP)));

        ProtocolJsonRpcCustomizedClient jsonRpc = ProtocolJsonRpcCustomizedClient.builder()
                .credentialsProvider(FAKE_CREDENTIALS_PROVIDER)
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                .build();
        jsonRpc.simple(SimpleRequest.builder().build());

        WireMock.verify(postRequestedFor(urlPathEqualTo("/"))
                .withHeader("Content-Type", containing("application/json")));
    }

    @Test
    public void clientWithNoCustomizedMetadata_has_default_contenttype() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("x-amz-crc32", JSON_BODY_Crc32_CHECKSUM)
                .withBody(JSON_BODY)));

        ProtocolJsonRpcClient jsonRpc = ProtocolJsonRpcClient.builder()
                .credentialsProvider(FAKE_CREDENTIALS_PROVIDER)
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                .build();

        AllTypesResponse result =
                jsonRpc.allTypes(AllTypesRequest.builder().build());

        WireMock.verify(postRequestedFor(urlPathEqualTo("/"))
                .withHeader("Content-Type", containing("application/x-amz-json-1.1")));
    }
}
