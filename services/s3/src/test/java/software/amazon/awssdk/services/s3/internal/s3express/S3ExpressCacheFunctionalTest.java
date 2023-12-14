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

package software.amazon.awssdk.services.s3.internal.s3express;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@WireMockTest(httpsEnabled = true)
public class S3ExpressCacheFunctionalTest {

    private static final Function<WireMockRuntimeInfo, URI> WM_HTTPS_ENDPOINT = wm -> URI.create(wm.getHttpsBaseUrl());
    private static final PathStyleEnforcingInterceptor PATH_STYLE_INTERCEPTOR = new PathStyleEnforcingInterceptor();
    private static final String S3EXPRESS_BUCKET_1 = "s3express-cache-1--use1-az1--x-s3";
    private static final String S3EXPRESS_BUCKET_2 = "s3express-cache-2--use1-az1--x-s3";
    private static final AwsCredentialsProvider CLIENT_CREDENTIALS_PROVIDER;
    private static final AwsBasicCredentials CLIENT_CREDENTIALS;

    private static final Map<Integer, AwsCredentialsProvider> REQUEST_PROVIDERS;
    private static final Map<Integer, AwsCredentialsIdentity> REQUEST_CREDENTIALS;

    static {
        CLIENT_CREDENTIALS = AwsBasicCredentials.create("akid_client", "skid_client");
        CLIENT_CREDENTIALS_PROVIDER = StaticCredentialsProvider.create(CLIENT_CREDENTIALS);

        REQUEST_PROVIDERS = new HashMap<>();
        REQUEST_CREDENTIALS = new HashMap<>();

        IntStream.range(0, 10).forEach(i -> {
            AwsBasicCredentials credentials = AwsBasicCredentials.create("akid_request_" + i, "skid_request_" + i);
            REQUEST_CREDENTIALS.put(i, credentials);
            REQUEST_PROVIDERS.put(i, StaticCredentialsProvider.create(credentials));
        });
    }

    private static CapturingInterceptor capturingInterceptor = new CapturingInterceptor();

    private static final String CREATE_SESSION_RESPONSE = String.format(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<ConnectResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n"
        + "<Credentials>\n"
        + "<SessionToken>%s</SessionToken>\n"
        + "<SecretAccessKey>%s</SecretAccessKey>\n"
        + "<AccessKeyId>%s</AccessKeyId>"
        + "</Credentials>\n"
        + "</ConnectResult>", "TheToken", "TheSecret", "TheAccessKey");

    private S3Client mockS3SyncClient;
    private S3AsyncClient mockS3AsyncClient;
    private static final String GET_BODY = "Hello world!";

    @BeforeEach
    public void methodSetup() {
        mockS3SyncClient = Mockito.mock(S3Client.class);
        mockS3AsyncClient = Mockito.mock(S3AsyncClient.class);

        stubFor(get(urlMatching("/.*session")).atPriority(1).willReturn(aResponse()
                                                                            .withStatus(200)
                                                                            .withBody(CREATE_SESSION_RESPONSE)));
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(200)));
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(GET_BODY)));
    }

    @AfterEach
    public void reset() {
        capturingInterceptor = new CapturingInterceptor();
    }

    @Test
    void clientCredentialsOnly_cacheUsesClientCredentialsForMainCallAndCreateSession(WireMockRuntimeInfo wm) {
        S3Client s3Client = getS3ClientBuilder(wm).build();
        s3Client.putObject(r -> r.bucket(S3EXPRESS_BUCKET_1).key("key"), RequestBody.fromString("tmp"));

        List<IdentityProvider<AwsCredentialsIdentity>> sessionProviderList = capturingInterceptor.sessionCredentialsProviders();
        IdentityProvider<AwsCredentialsIdentity> awsCredentialsIdentityIdentityProvider = sessionProviderList.get(0);
        assertThat(awsCredentialsIdentityIdentityProvider.resolveIdentity().join()).isEqualTo(CLIENT_CREDENTIALS);

        List<CompletableFuture<?>> createSessionIdentities = capturingInterceptor.sessionCredentials();
        assertThat(createSessionIdentities).hasSize(1);
        AwsBasicCredentials credentialsForSessionCall = basicCredentialsFromFuture(createSessionIdentities.get(0));
        assertThat(credentialsForSessionCall).isEqualTo(CLIENT_CREDENTIALS);

        List<IdentityProvider<AwsCredentialsIdentity>> apiCallProviderList = capturingInterceptor.apiCredentialsProviders();
        IdentityProvider<AwsCredentialsIdentity> credentialsProviderForApiCall = apiCallProviderList.get(0);
        assertThat(credentialsProviderForApiCall).isEqualTo(CLIENT_CREDENTIALS_PROVIDER);

        List<CompletableFuture<?>> apiCallIdentities = capturingInterceptor.apiCredentials();
        assertThat(apiCallIdentities).hasSize(1);
        S3ExpressSessionCredentials apiCallCredentials = s3ExpressCredentialsFromFuture(apiCallIdentities.get(0));
        assertThat(apiCallCredentials.accessKeyId()).isEqualTo("TheAccessKey");
        assertThat(apiCallCredentials.secretAccessKey()).isEqualTo("TheSecret");
        assertThat(apiCallCredentials.sessionToken()).isEqualTo("TheToken");
    }

    @Test
    void requestOverrideCredentials_cacheUsesOverrideCredentialsForMainCallAndCreateSession(WireMockRuntimeInfo wm) {
        S3Client s3Client = getS3ClientBuilder(wm).build();

        putObject(s3Client, S3EXPRESS_BUCKET_1, REQUEST_PROVIDERS.get(0));

        List<IdentityProvider<AwsCredentialsIdentity>> sessionProviderList = capturingInterceptor.sessionCredentialsProviders();
        IdentityProvider<AwsCredentialsIdentity> awsCredentialsIdentityIdentityProvider = sessionProviderList.get(0);
        assertThat(awsCredentialsIdentityIdentityProvider.resolveIdentity().join()).isEqualTo(REQUEST_CREDENTIALS.get(0));

        List<CompletableFuture<?>> createSessionIdentities = capturingInterceptor.sessionCredentials();
        assertThat(createSessionIdentities).hasSize(1);
        AwsBasicCredentials credentialsForSessionCall = basicCredentialsFromFuture(createSessionIdentities.get(0));
        assertThat(credentialsForSessionCall).isEqualTo(REQUEST_CREDENTIALS.get(0));

        List<IdentityProvider<AwsCredentialsIdentity>> apiCallProviderList = capturingInterceptor.apiCredentialsProviders();
        IdentityProvider<AwsCredentialsIdentity> credentialsProviderForApiCall = apiCallProviderList.get(0);
        assertThat(credentialsProviderForApiCall).isEqualTo(REQUEST_PROVIDERS.get(0));

        List<CompletableFuture<?>> apiCallIdentities = capturingInterceptor.apiCredentials();
        assertThat(apiCallIdentities).hasSize(1);
        S3ExpressSessionCredentials apiCallCredentials = s3ExpressCredentialsFromFuture(apiCallIdentities.get(0));
        assertThat(apiCallCredentials.accessKeyId()).isEqualTo("TheAccessKey");
        assertThat(apiCallCredentials.secretAccessKey()).isEqualTo("TheSecret");
        assertThat(apiCallCredentials.sessionToken()).isEqualTo("TheToken");
    }

    @Test
    void differentBucketSameRequestOverrideCredentials_cacheUsesOverrideCredentials(WireMockRuntimeInfo wm) {
        S3Client s3Client = getS3ClientBuilder(wm).build();

        putObject(s3Client, S3EXPRESS_BUCKET_1, REQUEST_PROVIDERS.get(0));
        putObject(s3Client, S3EXPRESS_BUCKET_2, REQUEST_PROVIDERS.get(0));
        putObject(s3Client, S3EXPRESS_BUCKET_1, REQUEST_PROVIDERS.get(0));
        putObject(s3Client, S3EXPRESS_BUCKET_1, REQUEST_PROVIDERS.get(0));

        int numUniqueKeys = 2;

        assertThat(capturingInterceptor.sessionRequests).isEqualTo(numUniqueKeys);

        List<CompletableFuture<?>> createSessionIdentities = capturingInterceptor.sessionCredentials();
        assertThat(createSessionIdentities).hasSize(numUniqueKeys);

        List<CompletableFuture<?>> apiCallIdentities = capturingInterceptor.apiCredentials();
        assertThat(apiCallIdentities).hasSize(4);
    }

    @Test
    void sameBucketDifferentRequestOverrideCredentials_cacheUsesOverrideCredentials(WireMockRuntimeInfo wm) {
        S3Client s3Client = getS3ClientBuilder(wm).build();

        putObject(s3Client, S3EXPRESS_BUCKET_1, REQUEST_PROVIDERS.get(0));
        putObject(s3Client, S3EXPRESS_BUCKET_1, REQUEST_PROVIDERS.get(1));
        putObject(s3Client, S3EXPRESS_BUCKET_1, REQUEST_PROVIDERS.get(0));
        putObject(s3Client, S3EXPRESS_BUCKET_1, REQUEST_PROVIDERS.get(0));

        int numUniqueKeys = 2;

        assertThat(capturingInterceptor.sessionRequests).isEqualTo(numUniqueKeys);

        List<CompletableFuture<?>> createSessionIdentities = capturingInterceptor.sessionCredentials();
        assertThat(createSessionIdentities).hasSize(2);

        List<CompletableFuture<?>> apiCallIdentities = capturingInterceptor.apiCredentials();
        assertThat(apiCallIdentities).hasSize(4);
    }

    private S3ExpressSessionCredentials s3ExpressCredentialsFromFuture(CompletableFuture<?> credentialsFuture) {
        Object o = CompletableFutureUtils.joinLikeSync(credentialsFuture);
        assertThat(o).isInstanceOf(S3ExpressSessionCredentials.class);
        return (S3ExpressSessionCredentials) o;
    }

    private AwsBasicCredentials basicCredentialsFromFuture(CompletableFuture<?> credentialsFuture) {
        Object o = CompletableFutureUtils.joinLikeSync(credentialsFuture);
        assertThat(o).isInstanceOf(AwsBasicCredentials.class);
        return (AwsBasicCredentials) o;
    }

    private void putObject(S3Client s3Client, String bucket, IdentityProvider<AwsCredentialsIdentity> provider) {
        s3Client.putObject(r -> r.bucket(bucket)
                                 .key("key")
                                 .overrideConfiguration(o -> o.credentialsProvider(provider)),
                           RequestBody.fromString("tmp"));
    }

    private S3ClientBuilder getS3ClientBuilder(WireMockRuntimeInfo wm) {
        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .overrideConfiguration(c -> c.addExecutionInterceptor(capturingInterceptor)
                                                    .addExecutionInterceptor(PATH_STYLE_INTERCEPTOR))
                       .credentialsProvider(CLIENT_CREDENTIALS_PROVIDER)
                       .endpointOverride(WM_HTTPS_ENDPOINT.apply(wm))
                       .httpClient(ApacheHttpClient.builder()
                                                   .buildWithDefaults(AttributeMap.builder()
                                                                                  .put(TRUST_ALL_CERTIFICATES, TRUE)
                                                                                  .build()));
    }

    private static final class CapturingInterceptor implements ExecutionInterceptor {

        private int sessionRequests = 0;
        private final List<CompletableFuture<?>> sessionCredentials = new ArrayList<>();
        private final List<CompletableFuture<?>> apiCredentials = new ArrayList<>();

        private final List<IdentityProvider<AwsCredentialsIdentity>> sessionCredentialsProvider = new ArrayList<>();
        private final List<IdentityProvider<AwsCredentialsIdentity>> apiCredentialsProvider = new ArrayList<>();

        public List<CompletableFuture<?>> sessionCredentials() {
            return Collections.unmodifiableList(sessionCredentials);
        }

        public List<CompletableFuture<?>> apiCredentials() {
            return Collections.unmodifiableList(apiCredentials);
        }

        public List<IdentityProvider<AwsCredentialsIdentity>> sessionCredentialsProviders() {
            return Collections.unmodifiableList(sessionCredentialsProvider);
        }

        public List<IdentityProvider<AwsCredentialsIdentity>> apiCredentialsProviders() {
            return Collections.unmodifiableList(apiCredentialsProvider);
        }

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            IdentityProviders providers = executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);
            IdentityProvider<AwsCredentialsIdentity> awsCredentialsIdentityIdentityProvider =
                providers.identityProvider(AwsCredentialsIdentity.class);

            String operationName = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
            if (operationName.equalsIgnoreCase("createsession")) {
                sessionRequests++;
                sessionCredentialsProvider.add(awsCredentialsIdentityIdentityProvider);
            } else {
                apiCredentialsProvider.add(awsCredentialsIdentityIdentityProvider);
            }
        }

        @Override
        public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
            SelectedAuthScheme<?> attribute = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
            CompletableFuture<?> identity = attribute.identity();

            String operationName = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
            if (operationName.equalsIgnoreCase("createsession")) {
                sessionCredentials.add(identity);
            } else {
                apiCredentials.add(identity);
            }
        }
    }

    private static final class PathStyleEnforcingInterceptor implements ExecutionInterceptor {

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            SdkHttpRequest sdkHttpRequest = context.httpRequest();
            String host = sdkHttpRequest.host();
            String bucket = host.substring(0, host.indexOf(".localhost"));

            return sdkHttpRequest.toBuilder().host("localhost")
                                 .encodedPath(SdkHttpUtils.appendUri(bucket, sdkHttpRequest.encodedPath()))
                                 .build();
        }
    }
}
