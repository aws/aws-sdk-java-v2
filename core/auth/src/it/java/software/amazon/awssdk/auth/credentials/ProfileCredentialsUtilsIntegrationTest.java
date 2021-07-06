package software.amazon.awssdk.auth.credentials;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.core.internal.util.UserAgentUtils;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.DateUtils;

public class ProfileCredentialsUtilsIntegrationTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String STUB_CREDENTIALS = "{\"AccessKeyId\":\"ACCESS_KEY_ID\",\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
            + "\"}";

    @Test
    public void profileWithCredentialSourceUsingEc2InstanceMetadataAndCustomEndpoint_usesEndpointInSourceProfile() {
        String testFileContentsTemplate = "" +
                "[profile a]\n" +
                "role_arn=arn:aws:iam::123456789012:role/testRole3\n" +
                "credential_source = ec2instancemetadata\n" +
                "ec2_metadata_service_endpoint = http://localhost:%d\n";

        WireMockServer mockMetadataEndpoint = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockMetadataEndpoint.start();
        try {
            String profileFileContents = String.format(testFileContentsTemplate, mockMetadataEndpoint.port());

            ProfileFile profileFile = ProfileFile.builder()
                    .type(ProfileFile.Type.CONFIGURATION)
                    .content(new ByteArrayInputStream(profileFileContents.getBytes(StandardCharsets.UTF_8)))
                    .build();

            ProfileCredentialsUtils credentialsUtils = new ProfileCredentialsUtils(profileFile, profileFile.profile("a").get(), profileFile::profile);

            String stubToken = "some-token";
            mockMetadataEndpoint.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(stubToken)));
            mockMetadataEndpoint.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
            mockMetadataEndpoint.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

            Optional<AwsCredentialsProvider> awsCredentialsProvider = credentialsUtils.credentialsProvider();

            awsCredentialsProvider.get().resolveCredentials();

            // all requests should have gone to the second server, and none to the other one
//            mockMetadataEndpoint.verify(0, RequestPatternBuilder.allRequests());

        } catch (Throwable e) {
            e.printStackTrace();
        }

        String userAgentHeader = "User-Agent";
        String userAgent = UserAgentUtils.getUserAgent();
        mockMetadataEndpoint.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
        mockMetadataEndpoint.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
        mockMetadataEndpoint.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).withHeader(userAgentHeader, equalTo(userAgent)));
    }
}
