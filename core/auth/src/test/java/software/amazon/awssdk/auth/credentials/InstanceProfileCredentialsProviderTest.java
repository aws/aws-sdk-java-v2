/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.auth.credentials;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.UserAgentUtils;
import software.amazon.awssdk.utils.DateUtils;

public class InstanceProfileCredentialsProviderTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String STUB_CREDENTIALS = "{\"AccessKeyId\":\"ACCESS_KEY_ID\",\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
            + "\"}";
    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public WireMockRule mockMetadataEndpoint = new WireMockRule();

    @Before
    public void methodSetup() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:" + mockMetadataEndpoint.port());
    }

    @AfterClass
    public static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
    }

    @Test
    public void resolveCredentials_metadataLookupDisabled_throws() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property(), "true");
        thrown.expect(SdkClientException.class);
        thrown.expectMessage("Loading credentials from local endpoint is disabled");
        try {
            InstanceProfileCredentialsProvider.builder().build().resolveCredentials();
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property());
        }
    }

    @Test
    public void resolveCredentials_requestsIncludeUserAgent() {
        String stubToken = "some-token";
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(stubToken)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        String userAgentHeader = "User-Agent";
        String userAgent = UserAgentUtils.getUserAgent();
        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).withHeader(userAgentHeader, equalTo(userAgent)));
    }

    @Test
    public void resolveCredentials_queriesTokenResource() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_includedInCredentialsRequests() {
        String stubToken = "some-token";
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(stubToken)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).withHeader(TOKEN_HEADER, equalTo(stubToken)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).withHeader(TOKEN_HEADER, equalTo(stubToken)));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_403Error_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(403).withBody("oops")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_404Error_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(404).withBody("oops")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_405Error_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(405).withBody("oops")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_400Error_throws() {
        thrown.expect(SdkClientException.class);
        thrown.expectMessage("token");

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(400).withBody("oops")));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();
    }

    @Test
    public void resolveCredentials_queriesTokenResource_socketTimeout_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token").withFixedDelay(Integer.MAX_VALUE)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    public void resolveCredentials_endpointSettingEmpty_throws() {
        thrown.expect(SdkClientException.class);

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "");
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();
    }

    @Test
    public void resolveCredentials_endpointSettingHostNotExists_throws() {
        thrown.expect(SdkClientException.class);

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "some-host-that-does-not-exist");
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();
    }
}
