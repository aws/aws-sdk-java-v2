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

package software.amazon.awssdk.services.s3.signer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;

/**
 * Demonstrates how to exclude headers from being included in signing
 */
public class SigningExcludeHeadersIntegrationTest extends S3IntegrationTestBase  {

    private static final Map<String, List<String>> extraHeaders = new HashMap<>();
    private static final List<String> headersToExclude = new ArrayList<>();
    private CapturingInterceptor recordingInterceptor;

    @BeforeAll
    public static void setupLocal() throws Exception {
        S3IntegrationTestBase.setUp();
        extraHeaders.put("header1", Collections.singletonList("value1"));
        extraHeaders.put("header2", Collections.singletonList("value2"));
        extraHeaders.put("header3", Collections.singletonList("value3"));
        headersToExclude.add("header1");
        headersToExclude.add("header3");
        createBucket("signexclude-integ-test123");
    }

    @AfterAll
    public static void removeLocal() {
        deleteBucketAndAllContents("signexclude-integ-test123");
    }

    /**
     * - Adds a signing attribute HttpSigner.EXCLUDED_HEADERS (String here but should probably be list of string)
     * - User must implement a custom AuthSchemeProvider which sets the signing attribute
     */
    @Test
    public void whenAddingSigningAttributeThroughAuthSchemeProvider_headersCanBeExcluded() {
        recordingInterceptor = new CapturingInterceptor();
        S3Client syncClient = s3ClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(recordingInterceptor))
            .authSchemeProvider(new SignerAttribAuthSchemeProvider(S3AuthSchemeProvider.defaultProvider(),
                                                                   headersToExclude))
            .build();

        AwsRequestOverrideConfiguration.Builder requestOverride = AwsRequestOverrideConfiguration.builder();
        extraHeaders.forEach(requestOverride::putHeader);

        syncClient.putObject(r -> r.bucket("signexclude-integ-test123")
                                   .key("test.txt")
                                   .overrideConfiguration(requestOverride.build()),
                             RequestBody.fromString("test"));

        Map<String, List<String>> sentHeaders = recordingInterceptor.headers;
        assertThat(sentHeaders).containsAllEntriesOf(extraHeaders);
        verifyExcludedHeadersNotSigned(sentHeaders, headersToExclude);
    }

    /**
     * - User adds a wrapping signer that removes and puts back the excluded headers
     * - The user must add this through overriding the standard auth scheme
     */
    @Test
    public void whenOverridingSraSigner_headersCanBeExcluded() {
        recordingInterceptor = new CapturingInterceptor();
        S3Client syncClient = s3ClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(recordingInterceptor))
            .putAuthScheme(authScheme("aws.auth#sigv4", new ModifyHeaderSigner(AwsV4HttpSigner.create(), headersToExclude)))
            .build();

        AwsRequestOverrideConfiguration.Builder requestOverride = AwsRequestOverrideConfiguration.builder();
        extraHeaders.forEach(requestOverride::putHeader);

        syncClient.putObject(r -> r.bucket("signexclude-integ-test123")
                                   .key("test.txt")
                                   .overrideConfiguration(requestOverride.build()),
                             RequestBody.fromString("test"));

        Map<String, List<String>> sentHeaders = recordingInterceptor.headers;
        assertThat(sentHeaders).containsAllEntriesOf(extraHeaders);
        verifyExcludedHeadersNotSigned(sentHeaders, headersToExclude);
    }

    /**
     * - User adds a wrapping signer that removes and puts back the excluded headers - old signer
     * - The user adds the signer through the client override configuration, advanced option, signer
     */
    @Test
    public void whenOverridingOldSigner_headersCanBeExcluded() {
        recordingInterceptor = new CapturingInterceptor();
        ModifyHeaderOldSigner headerModifyingSigner = new ModifyHeaderOldSigner(Aws4Signer.create(), headersToExclude);

        S3Client syncClient = s3ClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(recordingInterceptor)
                                         .putAdvancedOption(SdkAdvancedClientOption.SIGNER, headerModifyingSigner))
            .build();

        AwsRequestOverrideConfiguration.Builder overrideConfiguration = AwsRequestOverrideConfiguration.builder();
        extraHeaders.forEach(overrideConfiguration::putHeader);

        syncClient.putObject(r -> r.bucket("signexclude-integ-test123")
                                   .key("test.txt")
                                   .overrideConfiguration(overrideConfiguration.build()),
                             RequestBody.fromString("test"));

        Map<String, List<String>> sentHeaders = recordingInterceptor.headers;
        assertThat(sentHeaders).containsAllEntriesOf(extraHeaders);
        verifyExcludedHeadersNotSigned(sentHeaders, headersToExclude);
    }

    private void verifyExcludedHeadersNotSigned(Map<String, List<String>> sentHeaders, List<String> headersToExclude) {
        String authorization = sentHeaders.get("Authorization").get(0);
        String signedHeaders = Arrays.stream(authorization.split(","))
                                     .filter(s -> s.contains("SignedHeaders"))
                                     .collect(Collectors.joining());
        String signedHeaders1 = signedHeaders.substring(signedHeaders.indexOf('=') + 1);
        long excludedHeadersSigned = Arrays.stream(signedHeaders1.split(";"))
                                           .filter(headersToExclude::contains)
                                           .count();
        assertThat(excludedHeadersSigned).isEqualTo(0);
    }

    private static AuthScheme<?> authScheme(String schemeId, HttpSigner<AwsCredentialsIdentity> signer) {
        return new AuthScheme<AwsCredentialsIdentity>() {
            @Override
            public String schemeId() {
                return schemeId;
            }

            @Override
            public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
                return providers.identityProvider(AwsCredentialsIdentity.class);
            }

            @Override
            public HttpSigner<AwsCredentialsIdentity> signer() {
                return signer;
            }
        };
    }

    private static class ModifyHeaderSigner implements HttpSigner<AwsCredentialsIdentity> {
        private final HttpSigner<AwsCredentialsIdentity> delegateSigner;
        private List<String> excludedHeaders;

        ModifyHeaderSigner(HttpSigner<AwsCredentialsIdentity> delegateSigner, List<String> excludedHeaders) {
            this.delegateSigner = delegateSigner;
            this.excludedHeaders = excludedHeaders;
        }

        @Override
        public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
            SdkHttpRequest unsignedHttpRequest = request.request();

            Map<String, List<String>> requestHeadersToExclude = new HashMap<>();
            SdkHttpRequest.Builder httpRequestWithRemovedHeaders = unsignedHttpRequest.toBuilder();
            unsignedHttpRequest.headers().forEach((key, value) -> {
                if (excludedHeaders.contains(key)) {
                    requestHeadersToExclude.put(key, value);
                    httpRequestWithRemovedHeaders.removeHeader(key);
                }
            });

            SignRequest<? extends AwsCredentialsIdentity> modifiedRequestToSign =
                request.copy(c -> c.request(httpRequestWithRemovedHeaders.build()));

            SignedRequest signedRequest = delegateSigner.sign(modifiedRequestToSign);

            SdkHttpRequest signedHttpRequest = signedRequest.request();
            SdkHttpRequest.Builder httprequest = signedHttpRequest.toBuilder();
            requestHeadersToExclude.forEach(httprequest::putHeader);

            return signedRequest.copy(signed -> signed.request(httprequest.build()));
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
            return new CompletableFuture<>();
        }
    }

    private static class ModifyHeaderOldSigner implements Signer {
        private final Signer delegateSigner;
        private final List<String> excludedHeaders;

        ModifyHeaderOldSigner(Signer delegateSigner, List<String> excludedHeaders) {
            this.delegateSigner = delegateSigner;
            this.excludedHeaders = excludedHeaders;
        }

        @Override
        public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
            Map<String, List<String>> requestHeadersToExclude = new HashMap<>();
            SdkHttpRequest.Builder httpRequestWithoutHdeaders = request.toBuilder();
            request.headers().forEach((key, value) -> {
                if (excludedHeaders.contains(key)) {
                    requestHeadersToExclude.put(key, value);
                    httpRequestWithoutHdeaders.removeHeader(key);
                }
            });

            SdkHttpRequest requestBeforeSigning = httpRequestWithoutHdeaders.build();

            SdkHttpFullRequest signedRequest = delegateSigner.sign((SdkHttpFullRequest) requestBeforeSigning, executionAttributes);

            SdkHttpRequest.Builder signedRequestBuilder = signedRequest.toBuilder();
            requestHeadersToExclude.forEach(signedRequestBuilder::putHeader);

            return (SdkHttpFullRequest) signedRequestBuilder.build();
        }

        @Override
        public CredentialType credentialType() {
            return Signer.super.credentialType();
        }
    }

    public class SignerAttribAuthSchemeProvider implements S3AuthSchemeProvider {

        private final S3AuthSchemeProvider delegateAuthSchemeProvider;
        private final List<String> excludedHeaders;

        public SignerAttribAuthSchemeProvider(S3AuthSchemeProvider delegateAuthSchemeProvider,
                                              List<String> excludedHeaders) {
            this.delegateAuthSchemeProvider = delegateAuthSchemeProvider;
            this.excludedHeaders = Collections.unmodifiableList(excludedHeaders);
        }

        @Override
        public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
            List<AuthSchemeOption> options = delegateAuthSchemeProvider.resolveAuthScheme(authSchemeParams);

            String excludedHeadersString = String.join(",", excludedHeaders);
            return options.stream()
                          .map(option -> option.toBuilder()
                                               .putSignerProperty(HttpSigner.EXCLUDED_HEADERS, excludedHeadersString)
                                               .build())
                          .collect(Collectors.toList());
        }
    }

    public static final class CapturingInterceptor implements ExecutionInterceptor {
        Map<String, List<String>> headers;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SdkHttpRequest sdkHttpRequest = context.httpRequest();
            headers = sdkHttpRequest.headers();
        }
    }
}