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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyUserAgentStage.HEADER_USER_AGENT;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;

@RunWith(MockitoJUnitRunner.class)
public class ApplyUserAgentStageTest {
    private static final SelectedAuthScheme<Identity> EMPTY_SELECTED_AUTH_SCHEME =
        new SelectedAuthScheme<>(CompletableFuture.completedFuture(Mockito.mock(Identity.class)),
                                 (HttpSigner<Identity>) Mockito.mock(HttpSigner.class),
                                 AuthSchemeOption.builder().schemeId("mock").build());

    private static final String SDK_UA_STRING = "aws-sdk-java/version vendor/unknown";
    private static final String PROVIDER_SOURCE = "ProcessCredentialsProvider";
    private static final AwsCredentialsIdentity IDENTITY_WITHOUT_SOURCE =
        AwsCredentialsIdentity.create("akid", "secret");

    private static final AwsCredentialsIdentity IDENTITY_WITH_SOURCE =
        AwsSessionCredentialsIdentity.builder().accessKeyId("akid").secretAccessKey("secret").sessionToken("token")
                                     .providerName(PROVIDER_SOURCE).build();

    @Test
    public void when_noAdditionalDataIsPresent_outputStringEqualsInputString() throws Exception {
        String clientBuildTimeUserAgentString = SDK_UA_STRING;

        ApplyUserAgentStage stage = new ApplyUserAgentStage(dependenciesWithUserAgent(clientBuildTimeUserAgentString));

        RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITHOUT_SOURCE), noOpRequest());
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).isEqualTo(SDK_UA_STRING);
    }

    @Test
    public void when_identityContainsProvider_authSourceIsPresent() throws Exception {
        String clientBuildTimeUserAgentString = SDK_UA_STRING;

        ApplyUserAgentStage stage = new ApplyUserAgentStage(dependenciesWithUserAgent(clientBuildTimeUserAgentString));

        RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITH_SOURCE), noOpRequest());
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains("auth-source#proc");
    }

    @Test
    public void when_requestContainsApiName_apiNamesArePresent() throws Exception {
        String clientBuildTimeUserAgentString = SDK_UA_STRING;

        ApplyUserAgentStage stage = new ApplyUserAgentStage(dependenciesWithUserAgent(clientBuildTimeUserAgentString));

        RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITH_SOURCE),
                                                              requestWithApiName("myLib", "1.0"));
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains("myLib/1.0");
    }

    private static HttpClientDependencies dependenciesWithUserAgent(String userAgent) {
        SdkClientConfiguration clientConfiguration = SdkClientConfiguration.builder()
                                                                           .option(SdkClientOption.CLIENT_USER_AGENT, userAgent)
                                                                           .build();
        return HttpClientDependencies.builder()
                                     .clientConfiguration(clientConfiguration)
                                     .build();
    }

    private static SdkRequest noOpRequest() {
        return requestWithOverrideConfig(null);
    }

    private static SdkRequest requestWithApiName(String apiName, String version) {
        SdkRequestOverrideConfiguration requestOverrideConfiguration =
            SdkRequestOverrideConfiguration.builder()
                                           .addApiName(a -> a.name(apiName).version(version))
                                           .build();
        return requestWithOverrideConfig(requestOverrideConfiguration);
    }

    private static SdkRequest requestWithOverrideConfig(SdkRequestOverrideConfiguration overrideConfiguration) {
        return NoopTestRequest.builder()
                              .overrideConfiguration(overrideConfiguration)
                              .build();
    }

    private static ExecutionAttributes executionAttributes(AwsCredentialsIdentity identity) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                         new SelectedAuthScheme<>(CompletableFuture.completedFuture(identity),
                                                         EMPTY_SELECTED_AUTH_SCHEME.signer(),
                                                         EMPTY_SELECTED_AUTH_SCHEME.authSchemeOption()));
        return executionAttributes;
    }

    private RequestExecutionContext requestExecutionContext(ExecutionAttributes executionAttributes,
                                                            SdkRequest request) {
        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .executionAttributes(executionAttributes)
                                                            .build();
        return RequestExecutionContext.builder()
                                      .executionContext(executionContext)
                                      .originalRequest(request).build();

    }
}
