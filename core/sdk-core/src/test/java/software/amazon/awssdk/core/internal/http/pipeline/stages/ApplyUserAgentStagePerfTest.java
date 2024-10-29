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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.ApiName;
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
import software.amazon.awssdk.core.util.SystemUserAgent;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;

@RunWith(MockitoJUnitRunner.class)
public class ApplyUserAgentStagePerfTest {
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

        RequestExecutionContext ctx = requestExecutionContext(
            executionAttributes(IDENTITY_WITH_SOURCE),
            requestWithApiName(Collections.singletonList(ApiName.builder().name("myLib").version("1.0").build())));
        SdkHttpFullRequest.Builder request = stage.execute(SdkHttpFullRequest.builder(), ctx);

        List<String> userAgentHeaders = request.headers().get(HEADER_USER_AGENT);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains("myLib/1.0");
    }

    @Test
    public void when_requestContainsApiName_apiNamesArePresentAndTime() throws Exception {
        List<List<Long>> times = new ArrayList<>();

        List<Long> noApiNames = new ArrayList<>();
        List<Long> fiveApiNames = new ArrayList<>();
        List<Long> hundredApiNames = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            ApplyUserAgentStage stage = new ApplyUserAgentStage(dependencies(clientUserAgent()));
            List<ApiName> apiNames = IntStream.range(0, 100).mapToObj(this::createApiName).collect(Collectors.toList());

            RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITH_SOURCE),
                                                                  requestWithApiName(apiNames));

            long start = System.nanoTime();
            stage.execute(SdkHttpFullRequest.builder(), ctx);
            long end = System.nanoTime() - start;
            hundredApiNames.add(end);
        }

        for (int i = 0; i < 50; i++) {
            ApplyUserAgentStage stage = new ApplyUserAgentStage(dependencies(clientUserAgent()));
            List<ApiName> apiNames = IntStream.range(0, 5).mapToObj(this::createApiName).collect(Collectors.toList());

            RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITH_SOURCE),
                                                                  requestWithApiName(apiNames));

            long start = System.nanoTime();
            stage.execute(SdkHttpFullRequest.builder(), ctx);
            long end = System.nanoTime() - start;
            fiveApiNames.add(end);
        }

        for (int i = 0; i < 50; i++) {
            ApplyUserAgentStage stage = new ApplyUserAgentStage(dependencies(clientUserAgent()));
            RequestExecutionContext ctx = requestExecutionContext(executionAttributes(IDENTITY_WITH_SOURCE),
                                                                  noOpRequest());

            long start = System.nanoTime();
            stage.execute(SdkHttpFullRequest.builder(), ctx);
            long end = System.nanoTime() - start;
            noApiNames.add(end);
        }

        System.out.println("100 ApiNames, 50 iterations");
        System.out.println("Min: " + stream(hundredApiNames).min().getAsLong());
        System.out.println("Max: " + stream(hundredApiNames).max().getAsLong());
        double median = median(stream(hundredApiNames).sorted(), hundredApiNames.size());
        System.out.println("Median: " + median);
        System.out.println("Median (ms): " + median / 1000000);

        System.out.println(" --- ");

        System.out.println("5 ApiNames, 50 iterations");
        System.out.println("Min: " + stream(fiveApiNames).min().getAsLong());
        System.out.println("Max: " + stream(fiveApiNames).max().getAsLong());
        median = median(stream(fiveApiNames).sorted(), fiveApiNames.size());
        System.out.println("Median: " + median);
        System.out.println("Median (ms): + " + median / 1000000);

        System.out.println(" --- ");

        System.out.println("0 ApiNames, 50 iterations");
        System.out.println("Min: " + stream(noApiNames).min().getAsLong());
        System.out.println("Max: " + stream(noApiNames).max().getAsLong());
        median = median(stream(noApiNames).sorted(), noApiNames.size());
        System.out.println("Median: " + median);
        System.out.println("Median (ms): " + median / 1000000);

        System.out.println(" --- ");
    }

    private LongStream stream(List<Long> longList) {
        return longList.stream().mapToLong(Long::valueOf);
    }

    private double median(LongStream sortedStream, int listSize) {
        return listSize%2 == 0?
               sortedStream.skip(listSize/2-1).limit(2).average().getAsDouble():
               sortedStream.skip(listSize/2).findFirst().getAsLong();
    }

    private ApiName createApiName(int num) {
        String name = num % 2 == 0 ? "sdk-metrics" : "customer-metric" + num;
        String version = num % 2 == 0 ? "a" + num : "customer-metric-value" + num;
        return ApiName.builder().name(name).version(version).build();
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

    @Test
    public void when_apiNamesAreSorted_IttakesNoTime() throws Exception {
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

    private static SdkRequest requestWithApiName(Iterable<ApiName> apiNames) {
        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        apiNames.forEach(builder::addApiName);
        return requestWithOverrideConfig(builder.build());
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
