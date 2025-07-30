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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Functional tests to verify DynamoDB client retry behavior with different retry modes.
 */
@WireMockTest
class DynamoDbDefaultRetryFunctionalTest {

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();
    private DynamoDbClient dynamoDbClient;

    @AfterEach
    void tearDown() {
        environmentVariableHelper.reset();
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"adaptive", "legacy", "standard"})
    void listTables_whenRetryModeSet_shouldAttempt9Times(String retryMode, WireMockRuntimeInfo wm) {
        // Set the retry mode environment variable
        environmentVariableHelper.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), retryMode);

        // Build the DynamoDB client here instead of setup so that environment variable options gets picked for each tests
        dynamoDbClient = DynamoDbClient.builder()
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .credentialsProvider(StaticCredentialsProvider.create(
                                           AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .build();

        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(503)));

        assertThatExceptionOfType(DynamoDbException.class)
            .isThrownBy(() -> dynamoDbClient.listTables());

        int actualAttempts = wm.getWireMock().getAllServeEvents().size();
        assertThat(actualAttempts)
            .as("Retry mode '%s' should result in 9 total attempts (1 initial + 8 retries)", retryMode)
            .isEqualTo(9);
    }

    @Test
    void listTables_whenUsingDefaultRetryMode_shouldAttempt9Times(WireMockRuntimeInfo wm) {
        dynamoDbClient = DynamoDbClient.builder()
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .credentialsProvider(StaticCredentialsProvider.create(
                                           AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .build();
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(503)));
        assertThatExceptionOfType(DynamoDbException.class)
            .isThrownBy(() -> dynamoDbClient.listTables());

        int actualAttempts = wm.getWireMock().getAllServeEvents().size();
        assertThat(actualAttempts)
            .as("Default retry mode should result in 9 total attempts (1 initial + 8 retries)")
            .isEqualTo(9);
    }

}
