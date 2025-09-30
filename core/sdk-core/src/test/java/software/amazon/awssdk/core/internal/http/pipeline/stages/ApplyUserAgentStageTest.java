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
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_PREFIX;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_SUFFIX;
import static software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyUserAgentStage.HEADER_USER_AGENT;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.HTTP;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.IO;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.RETRY_MODE;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.SPACE;

import java.util.Arrays;
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
import software.amazon.awssdk.core.internal.useragent.SdkClientUserAgentProperties;
import software.amazon.awssdk.core.internal.useragent.SdkUserAgentBuilder;
import software.amazon.awssdk.core.useragent.AdditionalMetadata;
import software.amazon.awssdk.core.util.SystemUserAgent;
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

    private static final String PROVIDER_SOURCE = "ProcessCredentialsProvider";
    private static final AwsCredentialsIdentity IDENTITY_WITHOUT_SOURCE =
        AwsCredentialsIdentity.create("akid", "secret");

    private static final AwsCredentialsIdentity IDENTITY_WITH_SOURCE =
        AwsSessionCredentialsIdentity.builder().accessKeyId("akid").secretAccessKey("secret").sessionToken("token")
                                     .providerName(PROVIDER_SOURCE).build();

    @Test
    public void when_noAdditionalDataIsPresent_userAgentOnlyHasSdkValues() throws Exception {
        ApplyUserAgentStage stage = new ApplyUserAgentStage(dependencies(clientUserAgent()));

        RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITHOUT_SOURCE), noOpRequest());
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        String userAgentString = userAgentHeaders.get(0);
        assertThat(userAgentString).startsWith("aws-sdk-java");
    }

    @Test
    public void when_userPrefixIsPresent_itIsAddedToUserAgent() throws Exception {
        String prefix = "Some completely opaque user prefix";
        ApplyUserAgentStage stage = new ApplyUserAgentStage(dependencies(clientUserAgent(), prefix, null));

        RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITHOUT_SOURCE), noOpRequest());
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        String userAgentString = userAgentHeaders.get(0);
        assertThat(userAgentString).startsWith(prefix + SPACE);
    }

    @Test
    public void when_userSuffixIsPresent_itIsAddedToUserAgent() throws Exception {
        String suffix = "Some completely opaque user suffix";
        ApplyUserAgentStage stage = new ApplyUserAgentStage(dependencies(clientUserAgent(), null, suffix));

        RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITHOUT_SOURCE), noOpRequest());
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        String userAgentString = userAgentHeaders.get(0);
        assertThat(userAgentString).startsWith("aws-sdk-java").endsWith(SPACE + suffix);
    }

    @Test
    public void when_requestContainsApiName_apiNamesArePresent() throws Exception {
        ApplyUserAgentStage stage = new ApplyUserAgentStage(dependencies(clientUserAgent()));

        RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITH_SOURCE),
                                                              requestWithApiName("myLib", "1.0"));
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains("myLib/1.0");
    }

    @Test
    public void when_requestContainsMetadata_metadataIsPresent() throws Exception {
        ApplyUserAgentStage stage = new ApplyUserAgentStage(dependencies(clientUserAgent()));

        RequestExecutionContext ctx = requestExecutionContext(
            executionAttributes(Arrays.asList(
                AdditionalMetadata.builder().name("name1").value("value1").build(),
                AdditionalMetadata.builder().name("name2").value("value2").build()
            )),
            noOpRequest());
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains("md/name1#value1");
        assertThat(userAgentHeaders.get(0)).contains("md/name2#value2");
    }

    @Test
    public void when_identityContainsProvider_authSourceIsPresent() throws Exception {
        ApplyUserAgentStage stage = new ApplyUserAgentStage(dependencies(clientUserAgent()));

        RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITH_SOURCE), noOpRequest());
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains("auth-source#proc");
    }

    private static HttpClientDependencies dependencies(String clientUserAgent) {
        return dependencies(clientUserAgent, null, null);
    }

    private static HttpClientDependencies dependencies(String clientUserAgent, String prefix, String suffix) {
        SdkClientConfiguration clientConfiguration =
            SdkClientConfiguration.builder()
                                  .option(SdkClientOption.CLIENT_USER_AGENT, clientUserAgent)
                                  .option(USER_AGENT_PREFIX, prefix)
                                  .option(USER_AGENT_SUFFIX, suffix)
                                  .build();
        return HttpClientDependencies.builder()
                                     .clientConfiguration(clientConfiguration)
                                     .build();
    }

    private String clientUserAgent() {
        SdkClientUserAgentProperties clientProperties = new SdkClientUserAgentProperties();

        clientProperties.putProperty(RETRY_MODE, "standard");
        clientProperties.putProperty(IO, "async");
        clientProperties.putProperty(HTTP, "netty");

        return SdkUserAgentBuilder.buildClientUserAgentString(SystemUserAgent.getOrCreate(), clientProperties);
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

    private static ExecutionAttributes executionAttributes(List<AdditionalMetadata> metadata) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.USER_AGENT_METADATA, metadata);
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
