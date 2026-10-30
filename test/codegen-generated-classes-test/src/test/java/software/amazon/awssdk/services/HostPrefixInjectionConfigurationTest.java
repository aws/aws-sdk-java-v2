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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Verifies that the disable host prefix injection setting is resolved from the system property, the environment variable
 * and the profile file, and that a generated client then skips the modeled hostPrefix.
 */
public class HostPrefixInjectionConfigurationTest {
    private static final String PROPERTY = SdkSystemSetting.AWS_DISABLE_HOST_PREFIX_INJECTION.property();

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();
    private MockSyncHttpClient mockHttpClient;
    private MockAsyncHttpClient mockAsyncHttpClient;
    private AtomicReference<String> capturedBody;

    @BeforeEach
    public void setup() {
        System.clearProperty(PROPERTY);
        mockHttpClient = new MockSyncHttpClient();
        mockAsyncHttpClient = new MockAsyncHttpClient();
        mockHttpClient.stubNextResponse(okResponse());
        mockAsyncHttpClient.stubNextResponse(okResponse());
        capturedBody = new AtomicReference<>();
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty(PROPERTY);
        environmentVariableHelper.reset();
        mockAsyncHttpClient.close();
    }

    @Test
    public void nothingSet_hostPrefixIsApplied() {
        syncClient(emptyProfileOverride()).operationWithHostPrefix(b -> b.stringMember("123"));
        assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("123-foo.localhost");

        asyncClient(emptyProfileOverride()).operationWithHostPrefix(b -> b.stringMember("123")).join();
        assertThat(mockAsyncHttpClient.getLastRequest().getUri().getHost()).isEqualTo("123-foo.localhost");
    }

    @Test
    public void systemPropertyTrue_sync_hostPrefixIsNotApplied_memberStillSerialized() {
        System.setProperty(PROPERTY, "true");
        syncClient(emptyProfileOverride()).operationWithHostPrefix(b -> b.stringMember("123"));
        assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("localhost");
        assertThat(capturedBody.get()).contains("\"StringMember\":\"123\"");
    }

    @Test
    public void systemPropertyTrue_async_hostPrefixIsNotApplied() {
        System.setProperty(PROPERTY, "true");
        asyncClient(emptyProfileOverride()).operationWithHostPrefix(b -> b.stringMember("123")).join();
        assertThat(mockAsyncHttpClient.getLastRequest().getUri().getHost()).isEqualTo("localhost");
    }

    @Test
    public void environmentVariableTrue_hostPrefixIsNotApplied() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_DISABLE_HOST_PREFIX_INJECTION, "true");
        syncClient(emptyProfileOverride()).operationWithHostPrefix(b -> b.stringMember("123"));
        assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("localhost");

        asyncClient(emptyProfileOverride()).operationWithHostPrefix(b -> b.stringMember("123")).join();
        assertThat(mockAsyncHttpClient.getLastRequest().getUri().getHost()).isEqualTo("localhost");
    }

    @Test
    public void profileTrue_hostPrefixIsNotApplied() {
        ClientOverrideConfiguration override =
            profileOverride("[profile test]\ndisable_host_prefix_injection = true");
        syncClient(override).operationWithHostPrefix(b -> b.stringMember("123"));
        assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("localhost");
        assertThat(capturedBody.get()).contains("\"StringMember\":\"123\"");

        asyncClient(override).operationWithHostPrefix(b -> b.stringMember("123")).join();
        assertThat(mockAsyncHttpClient.getLastRequest().getUri().getHost()).isEqualTo("localhost");
    }

    @Test
    public void explicitFalse_beatsSystemProperty_hostPrefixIsApplied() {
        System.setProperty(PROPERTY, "true");
        ClientOverrideConfiguration override =
            emptyProfileOverride().toBuilder()
                                  .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, false)
                                  .build();
        syncClient(override).operationWithHostPrefix(b -> b.stringMember("123"));
        assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("123-foo.localhost");

        asyncClient(override).operationWithHostPrefix(b -> b.stringMember("123")).join();
        assertThat(mockAsyncHttpClient.getLastRequest().getUri().getHost()).isEqualTo("123-foo.localhost");
    }

    private ProtocolRestJsonClient syncClient(ClientOverrideConfiguration override) {
        ClientOverrideConfiguration withCapture = override.toBuilder()
                                                          .addExecutionInterceptor(new BodyCapturingInterceptor())
                                                          .build();
        return ProtocolRestJsonClient.builder()
                                     .credentialsProvider(credentials())
                                     .region(Region.US_EAST_1)
                                     .endpointOverride(URI.create("http://localhost"))
                                     .httpClient(mockHttpClient)
                                     .overrideConfiguration(withCapture)
                                     .build();
    }

    private ProtocolRestJsonAsyncClient asyncClient(ClientOverrideConfiguration override) {
        return ProtocolRestJsonAsyncClient.builder()
                                          .credentialsProvider(credentials())
                                          .region(Region.US_EAST_1)
                                          .endpointOverride(URI.create("http://localhost"))
                                          .httpClient(mockAsyncHttpClient)
                                          .overrideConfiguration(override)
                                          .build();
    }

    private static ClientOverrideConfiguration emptyProfileOverride() {
        return profileOverride("");
    }

    private static ClientOverrideConfiguration profileOverride(String content) {
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream(content))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        return ClientOverrideConfiguration.builder()
                                          .defaultProfileFileSupplier(() -> profileFile)
                                          .defaultProfileName("test")
                                          .build();
    }

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }

    private static HttpExecuteResponse okResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                  .build();
    }

    private class BodyCapturingInterceptor implements ExecutionInterceptor {
        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            context.requestBody().ifPresent(body -> capturedBody.set(
                invokeSafely(() -> IoUtils.toUtf8String(body.contentStreamProvider().newStream()))));
        }
    }
}
