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

package software.amazon.awssdk.services.dynamodb;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Tests that verify AccountId from IMDS credentials flows into DynamoDB account-based endpoint resolution.
 */
class ImdsAccountIdEndpointTest {

    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String PROFILE_NAME = "test-role";
    private static final String TOKEN_STUB = "test-token";
    private static final String ACCOUNT_ID = "123456789012";

    private static final String STUB_CREDENTIALS_WITH_ACCOUNT_ID =
        "{\"AccessKeyId\":\"AKIAIOSFODNN7EXAMPLE\","
        + "\"SecretAccessKey\":\"wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\","
        + "\"Token\":\"FwoGZXIvYXdzEBYaDHqa0AP9P9R3JATObiExample\","
        + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1))) + "\","
        + "\"AccountId\":\"" + ACCOUNT_ID + "\"}";

    private static final String STUB_CREDENTIALS_WITHOUT_ACCOUNT_ID =
        "{\"AccessKeyId\":\"AKIAIOSFODNN7EXAMPLE\","
        + "\"SecretAccessKey\":\"wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\","
        + "\"Token\":\"FwoGZXIvYXdzEBYaDHqa0AP9P9R3JATObiExample\","
        + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1))) + "\"}";

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
                                                               .options(wireMockConfig().dynamicPort())
                                                               .configureStaticDsl(true)
                                                               .build();

    private MockSyncHttpClient mockHttpClient;

    @BeforeEach
    void setup() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                           "http://localhost:" + wireMockServer.getPort());
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                           .response(SdkHttpResponse.builder().statusCode(200).build())
                                                           .responseBody(AbortableInputStream.create(
                                                               new StringInputStream("{}")))
                                                           .build());
    }

    @AfterAll
    static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
    }

    private void stubImdsResponse(String credentialsBody) {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                    .willReturn(aResponse().withBody(TOKEN_STUB)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                    .willReturn(aResponse().withBody(PROFILE_NAME)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + PROFILE_NAME))
                    .willReturn(aResponse().withBody(credentialsBody)));
    }

    private DynamoDbClient createClient(InstanceProfileCredentialsProvider credentialsProvider) {
        return DynamoDbClient.builder()
                             .region(Region.US_EAST_1)
                             .credentialsProvider(credentialsProvider)
                             .httpClient(mockHttpClient)
                             .build();
    }

    @Test
    void imdsWithAccountId_usesAccountBasedEndpoint() {
        stubImdsResponse(STUB_CREDENTIALS_WITH_ACCOUNT_ID);

        try (InstanceProfileCredentialsProvider credentialsProvider =
                 InstanceProfileCredentialsProvider.builder().build();
             DynamoDbClient client = createClient(credentialsProvider)) {

            client.listTables();
        }

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest.host()).contains(ACCOUNT_ID + ".ddb.");
    }

    @Test
    void imdsWithoutAccountId_usesStandardEndpoint() {
        stubImdsResponse(STUB_CREDENTIALS_WITHOUT_ACCOUNT_ID);

        try (InstanceProfileCredentialsProvider credentialsProvider =
                 InstanceProfileCredentialsProvider.builder().build();
             DynamoDbClient client = createClient(credentialsProvider)) {

            client.listTables();
        }

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest.host()).isEqualTo("dynamodb.us-east-1.amazonaws.com");
    }
}
