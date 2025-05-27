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

package software.amazon.awssdk.services.environmenttokenprovider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.StaticTokenProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.environmenttokenprovider.auth.scheme.EnvironmentTokenProviderAuthSchemeProvider;
import software.amazon.awssdk.services.environmenttokenprovider.model.OneOperationRequest;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class EnvironmentTokenProviderTest {
    private static final String ENV_NAME = "AWS_BEARER_TOKEN_ENVIRONMENT_TOKEN";
    private static final String SYSTEM_PROPERTY_NAME = "aws.bearerTokenEnvironmentToken";
    public static final String ENV_TOKEN = "env-test-token";
    public static final String SYSTEM_TEST_TOKEN = "system-test-token";

    private MockSyncHttpClient mockHttpClient;
    private String systemPropertyBeforeTest;

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    @BeforeEach
    void setUp() {
        mockHttpClient = new MockSyncHttpClient();
        systemPropertyBeforeTest = System.getProperty(SYSTEM_PROPERTY_NAME);
    }

    @AfterEach
    void tearDown() {
        mockHttpClient.reset();
        environmentVariableHelper.reset();
        if (systemPropertyBeforeTest != null) {
            System.setProperty(SYSTEM_PROPERTY_NAME, systemPropertyBeforeTest);
        } else {
            System.clearProperty(SYSTEM_PROPERTY_NAME);
        }
    }

    @Test
    public void usesSigv4WhenTokenUnset() {
        mockHttpClient.stubNextResponse(mockResponse());

        EnvironmentTokenProviderClient client = EnvironmentTokenProviderClient
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .httpClient(mockHttpClient)
            .build();

        client.oneOperation(b -> {
        });

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        assertThat(loggedRequest.firstMatchingHeader("Authorization").get()).startsWith("AWS4-HMAC-SHA256 Credential=akid/");
    }

    @Test
    public void usesBearerAuthWithTokenFromEnvironmentWhenSet() {
        environmentVariableHelper.set(ENV_NAME, ENV_TOKEN);
        mockHttpClient.stubNextResponse(mockResponse());

        EnvironmentTokenProviderClient client = EnvironmentTokenProviderClient
            .builder()
            .httpClient(mockHttpClient)
            .build();

        client.oneOperation(b -> {
        });

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        assertThat(loggedRequest.firstMatchingHeader("Authorization").get()).isEqualTo(String.format("Bearer %s", ENV_TOKEN));
        assertThat(loggedRequest.firstMatchingHeader("User-Agent").get())
            .matches(".*m\\/[A-Za-z0-9,]+" + BusinessMetricFeatureId.BEARER_SERVICE_ENV_VARS);
    }

    @Test
    public void usesBearerAuthWithTokenPreferredFromSystemProperties() {
        environmentVariableHelper.set(ENV_NAME, ENV_TOKEN);
        System.setProperty(SYSTEM_PROPERTY_NAME, SYSTEM_TEST_TOKEN);


        mockHttpClient.stubNextResponse(mockResponse());

        EnvironmentTokenProviderClient client = EnvironmentTokenProviderClient
            .builder()
            .httpClient(mockHttpClient)
            .build();

        client.oneOperation(b -> {
        });

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        assertThat(loggedRequest.firstMatchingHeader("Authorization").get())
            .isEqualTo(String.format("Bearer %s", SYSTEM_TEST_TOKEN));
    }

    @Test
    public void usesBearerAuthWithTokenFromEnvironmentOverAuthSchemePreference() {
        environmentVariableHelper.set(ENV_NAME, ENV_TOKEN);
        environmentVariableHelper.set(
            SdkSystemSetting.AWS_AUTH_SCHEME_PREFERENCE.environmentVariable(), "sigv4");
        mockHttpClient.stubNextResponse(mockResponse());

        EnvironmentTokenProviderClient client = EnvironmentTokenProviderClient
            .builder()
            .httpClient(mockHttpClient)
            .build();

        client.oneOperation(b -> {
        });

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        assertThat(loggedRequest.firstMatchingHeader("Authorization").get()).isEqualTo(String.format("Bearer %s", ENV_TOKEN));
        assertThat(loggedRequest.firstMatchingHeader("User-Agent").get())
            .matches(".*m\\/[A-Za-z0-9,]+" + BusinessMetricFeatureId.BEARER_SERVICE_ENV_VARS);
    }

    @Test
    public void usesSigv4WhenAuthSchemeProviderIsManuallyConfigured() {
        mockHttpClient.stubNextResponse(mockResponse());
        environmentVariableHelper.set(ENV_NAME, ENV_TOKEN);

        EnvironmentTokenProviderClient client = EnvironmentTokenProviderClient
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .authSchemeProvider(EnvironmentTokenProviderAuthSchemeProvider.defaultProvider())
            .httpClient(mockHttpClient)
            .build();

        client.oneOperation(b -> {
        });

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        assertThat(loggedRequest.firstMatchingHeader("Authorization").get()).startsWith("AWS4-HMAC-SHA256 Credential=akid/");
    }

    @Test
    public void metricNotSetWhenTokenOverriddenOnOperation() {
        environmentVariableHelper.set(ENV_NAME, ENV_TOKEN);
        mockHttpClient.stubNextResponse(mockResponse());

        EnvironmentTokenProviderClient client = EnvironmentTokenProviderClient
            .builder()
            .httpClient(mockHttpClient)
            .build();

        client.oneOperation(OneOperationRequest.builder()
                                               .overrideConfiguration(c -> c.tokenIdentityProvider(
                                                   StaticTokenProvider.create(() -> "operation-token")))
                                               .build());

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        assertThat(loggedRequest.firstMatchingHeader("Authorization").get()).isEqualTo("Bearer operation-token");
        assertThat(loggedRequest.firstMatchingHeader("User-Agent").get())
            .doesNotMatch(".*m\\/[A-Za-z0-9,]+" + BusinessMetricFeatureId.BEARER_SERVICE_ENV_VARS);
    }

    private HttpExecuteResponse mockResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .build();
    }
}
