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

package software.amazon.awssdk.services.polly.internal.presigner;

import static java.util.stream.Collectors.toMap;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.endpoint.AwsClientEndpointProvider;
import software.amazon.awssdk.awscore.endpoint.DualstackEnabledProvider;
import software.amazon.awssdk.awscore.endpoint.FipsEnabledProvider;
import software.amazon.awssdk.awscore.presigner.PresignRequest;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.polly.auth.scheme.PollyAuthSchemeProvider;
import software.amazon.awssdk.services.polly.internal.presigner.model.transform.SynthesizeSpeechRequestMarshaller;
import software.amazon.awssdk.services.polly.model.PollyRequest;
import software.amazon.awssdk.services.polly.presigner.PollyPresigner;
import software.amazon.awssdk.services.polly.presigner.model.PresignedSynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.presigner.model.SynthesizeSpeechPresignRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link PollyPresigner}.
 */
@SdkInternalApi
public final class DefaultPollyPresigner implements PollyPresigner {
    private static final String SIGNING_NAME = "polly";
    private static final String SERVICE_NAME = "polly";
    private final Clock signingClock;
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;
    private final Region region;
    private final IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
    private final URI endpointOverride;
    private final Boolean dualstackEnabled;
    private final Boolean fipsEnabled;

    private DefaultPollyPresigner(BuilderImpl builder) {
        this.signingClock = builder.signingClock != null ? builder.signingClock
                                                         : Clock.systemUTC();
        this.profileFile = ProfileFile::defaultProfileFile;
        this.profileName = ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
        this.region = builder.region != null ? builder.region
                                             : DefaultAwsRegionProviderChain.builder()
                                                                            .profileFile(profileFile)
                                                                            .profileName(profileName)
                                                                            .build()
                                                                            .getRegion();
        this.credentialsProvider = builder.credentialsProvider != null ? builder.credentialsProvider
                                                                       : DefaultCredentialsProvider.builder()
                                                                                                   .profileFile(profileFile)
                                                                                                   .profileName(profileName)
                                                                                                   .build();
        this.endpointOverride = builder.endpointOverride;
        this.dualstackEnabled = builder.dualstackEnabled != null ? builder.dualstackEnabled
                                                                 : DualstackEnabledProvider.builder()
                                                                                           .profileFile(profileFile)
                                                                                           .profileName(profileName)
                                                                                           .build()
                                                                                           .isDualstackEnabled()
                                                                                           .orElse(false);
        this.fipsEnabled = builder.fipsEnabled != null ? builder.fipsEnabled
                                                       : FipsEnabledProvider.builder()
                                                                            .profileFile(profileFile)
                                                                            .profileName(profileName)
                                                                            .build()
                                                                            .isFipsEnabled()
                                                                            .orElse(false);
    }

    IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
        return credentialsProvider;
    }

    @Override
    public void close() {
        IoUtils.closeIfCloseable(credentialsProvider, null);
    }

    // Builder for testing that allows you to set the signing clock.
    @SdkTestInternalApi
    static PollyPresigner.Builder builder(Clock signingClock) {
        return new BuilderImpl()
            .signingClock(signingClock);
    }

    public static PollyPresigner.Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public PresignedSynthesizeSpeechRequest presignSynthesizeSpeech(
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest) {
        return presign(PresignedSynthesizeSpeechRequest.builder(),
                       synthesizeSpeechPresignRequest,
                       synthesizeSpeechPresignRequest.synthesizeSpeechRequest(),
                       SynthesizeSpeechRequestMarshaller.getInstance()::marshall)
            .build();
    }

    private <T extends PollyRequest> SdkHttpFullRequest marshallRequest(
        T request, Function<T, SdkHttpFullRequest.Builder> marshalFn) {
        SdkHttpFullRequest.Builder requestBuilder = marshalFn.apply(request);
        applyOverrideHeadersAndQueryParams(requestBuilder, request);
        applyEndpoint(requestBuilder);
        return requestBuilder.build();
    }

    /**
     * Generate a {@link PresignedRequest} from a {@link PresignedRequest} and {@link PollyRequest}.
     */
    private <T extends PresignedRequest.Builder, U extends PollyRequest> T presign(
        T presignedRequest,
        PresignRequest presignRequest,
        U requestToPresign,
        Function<U, SdkHttpFullRequest.Builder> requestMarshaller
    ) {
        ExecutionAttributes execAttrs = createExecutionAttributes(presignRequest, requestToPresign);
        SdkHttpFullRequest marshalledRequest = marshallRequest(requestToPresign, requestMarshaller);
        Presigner presigner = resolvePresigner(requestToPresign);
        SdkHttpFullRequest signedHttpRequest = null;
        if (presigner != null) {
            signedHttpRequest = presignRequest(presigner, marshalledRequest, execAttrs);
        } else {
            SelectedAuthScheme<AwsCredentialsIdentity> authScheme = selectedAuthScheme(requestToPresign, execAttrs);
            signedHttpRequest = doSraPresign(marshalledRequest, authScheme, presignRequest.signatureDuration());
        }
        initializePresignedRequest(presignedRequest, execAttrs, signedHttpRequest);
        return presignedRequest;
    }

    private void initializePresignedRequest(PresignedRequest.Builder presignedRequest,
                                            ExecutionAttributes execAttrs,
                                            SdkHttpFullRequest signedHttpRequest) {
        List<String> signedHeadersQueryParam = signedHttpRequest.firstMatchingRawQueryParameters("X-Amz-SignedHeaders");

        Map<String, List<String>> signedHeaders =
            signedHeadersQueryParam.stream()
                                   .flatMap(h -> Stream.of(h.split(";")))
                                   .collect(toMap(h -> h, h -> signedHttpRequest.firstMatchingHeader(h)
                                                                                .map(Collections::singletonList)
                                                                                .orElseGet(ArrayList::new)));

        boolean isBrowserExecutable = signedHttpRequest.method() == SdkHttpMethod.GET &&
                                      (signedHeaders.isEmpty() ||
                                       (signedHeaders.size() == 1 && signedHeaders.containsKey("host")));

        presignedRequest.expiration(execAttrs.getAttribute(AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION))
                        .isBrowserExecutable(isBrowserExecutable)
                        .httpRequest(signedHttpRequest)
                        .signedHeaders(signedHeaders);
    }

    private SdkHttpFullRequest presignRequest(Presigner presigner,
                                              SdkHttpFullRequest marshalledRequest,
                                              ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest presigned = presigner.presign(marshalledRequest, executionAttributes);
        List<String> signedHeadersQueryParam = presigned.firstMatchingRawQueryParameters("X-Amz-SignedHeaders");
        Validate.validState(!signedHeadersQueryParam.isEmpty(),
                            "Only SigV4 presigners are supported at this time, but the configured "
                            + "presigner (%s) did not seem to generate a SigV4 signature.", presigner);
        return presigned;
    }

    private <T extends Identity> SdkHttpFullRequest doSraPresign(SdkHttpFullRequest request,
                                                                 SelectedAuthScheme<T> selectedAuthScheme,
                                                                 Duration expirationDuration) {
        CompletableFuture<? extends T> identityFuture = selectedAuthScheme.identity();
        T identity = CompletableFutureUtils.joinLikeSync(identityFuture);

        // presigned url puts auth info in query string, does not sign the payload, and has an expiry.
        SignRequest.Builder<T> signRequestBuilder = SignRequest
            .builder(identity)
            .putProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION, AwsV4FamilyHttpSigner.AuthLocation.QUERY_STRING)
            .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, expirationDuration)
            .putProperty(HttpSigner.SIGNING_CLOCK, signingClock)
            .request(request)
            .payload(request.contentStreamProvider().orElse(null));
        AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();
        authSchemeOption.forEachSignerProperty(signRequestBuilder::putProperty);

        HttpSigner<T> signer = selectedAuthScheme.signer();
        SignedRequest signedRequest = signer.sign(signRequestBuilder.build());
        return toSdkHttpFullRequest(signedRequest);
    }

    private SelectedAuthScheme<AwsCredentialsIdentity> selectedAuthScheme(PollyRequest requestToPresign,
                                                                          ExecutionAttributes attributes) {
        AuthScheme<AwsCredentialsIdentity> authScheme = AwsV4AuthScheme.create();
        AwsCredentialsIdentity credentialsIdentity = resolveCredentials(resolveCredentialsProvider(requestToPresign));
        AuthSchemeOption.Builder optionBuilder = AuthSchemeOption.builder()
                                                                 .schemeId(authScheme.schemeId());
        optionBuilder.putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, SERVICE_NAME);
        String region = attributes.getAttribute(AwsExecutionAttribute.AWS_REGION).id();
        optionBuilder.putSignerProperty(AwsV4HttpSigner.REGION_NAME, region);
        return new SelectedAuthScheme<>(CompletableFuture.completedFuture(credentialsIdentity), authScheme.signer(),
                                        optionBuilder.build());
    }

    private SdkHttpFullRequest toSdkHttpFullRequest(SignedRequest signedRequest) {
        SdkHttpRequest request = signedRequest.request();

        return SdkHttpFullRequest.builder()
                                 .contentStreamProvider(signedRequest.payload().orElse(null))
                                 .protocol(request.protocol())
                                 .method(request.method())
                                 .host(request.host())
                                 .port(request.port())
                                 .encodedPath(request.encodedPath())
                                 .applyMutation(r -> request.forEachHeader(r::putHeader))
                                 .applyMutation(r -> request.forEachRawQueryParameter(r::putRawQueryParameter))
                                 .build();
    }

    private ExecutionAttributes createExecutionAttributes(PresignRequest presignRequest, PollyRequest requestToPresign) {
        // A fixed signingClock is used, so that the current time used by the signing logic, as well as to determine expiration
        // are the same.
        Instant signingInstant = signingClock.instant();
        Clock signingClockOverride = Clock.fixed(signingInstant, ZoneOffset.UTC);
        Duration expirationDuration = presignRequest.signatureDuration();
        Instant signatureExpiration = signingInstant.plus(expirationDuration);

        AwsCredentialsIdentity credentials = resolveCredentials(resolveCredentialsProvider(requestToPresign));
        Validate.validState(credentials != null, "Credential providers must never return null.");

        return new ExecutionAttributes()
            .putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, CredentialUtils.toCredentials(credentials))
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, SIGNING_NAME)
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_CLOCK, signingClockOverride)
            .putAttribute(AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION, signatureExpiration)
            .putAttribute(AwsExecutionAttribute.AWS_REGION, region)
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, region)
            .putAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX, false)
            .putAttribute(SdkExecutionAttribute.CLIENT_TYPE, ClientType.SYNC)
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE_NAME)
            .putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER, PollyAuthSchemeProvider.defaultProvider())
            .putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, authSchemes())
            .putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS,
                          IdentityProviders.builder()
                                           .putIdentityProvider(credentialsProvider())
                                           .build());
    }

    private Map<String, AuthScheme<?>> authSchemes() {
        AwsV4AuthScheme awsV4AuthScheme = AwsV4AuthScheme.create();
        return Collections.singletonMap(awsV4AuthScheme.schemeId(), awsV4AuthScheme);
    }

    private IdentityProvider<? extends AwsCredentialsIdentity> resolveCredentialsProvider(PollyRequest request) {
        return request.overrideConfiguration().flatMap(AwsRequestOverrideConfiguration::credentialsIdentityProvider)
                      .orElse(credentialsProvider);
    }

    private AwsCredentialsIdentity resolveCredentials(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
        return CompletableFutureUtils.joinLikeSync(credentialsProvider.resolveIdentity());
    }

    private Presigner resolvePresigner(PollyRequest request) {
        Signer signer = request.overrideConfiguration().flatMap(AwsRequestOverrideConfiguration::signer)
                               .orElse(null);
        if (signer == null) {
            return null;
        }
        return Validate.isInstanceOf(Presigner.class, signer,
                                     "Signer of type %s given in request override is not a Presigner",
                                     signer.getClass().getName());
    }

    private void applyOverrideHeadersAndQueryParams(SdkHttpFullRequest.Builder httpRequestBuilder, PollyRequest request) {
        request.overrideConfiguration().ifPresent(o -> {
            o.headers().forEach(httpRequestBuilder::putHeader);
            o.rawQueryParameters().forEach(httpRequestBuilder::putRawQueryParameter);
        });
    }

    private void applyEndpoint(SdkHttpFullRequest.Builder httpRequestBuilder) {
        URI uri = resolveEndpoint();
        httpRequestBuilder.protocol(uri.getScheme())
                          .host(uri.getHost())
                          .port(uri.getPort());
    }

    private URI resolveEndpoint() {
        return AwsClientEndpointProvider.builder()
                                        .clientEndpointOverride(endpointOverride)
                                        .serviceEndpointOverrideEnvironmentVariable("AWS_ENDPOINT_URL_POLLY")
                                        .serviceEndpointOverrideSystemProperty("aws.endpointUrlPolly")
                                        .serviceProfileProperty("polly")
                                        .serviceEndpointPrefix(SERVICE_NAME)
                                        .defaultProtocol("https")
                                        .region(region)
                                        .profileFile(profileFile)
                                        .profileName(profileName)
                                        .dualstackEnabled(dualstackEnabled)
                                        .fipsEnabled(fipsEnabled)
                                        .build()
                                        .clientEndpoint();
    }

    public static class BuilderImpl implements PollyPresigner.Builder {
        private Clock signingClock;
        private Region region;
        private IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
        private URI endpointOverride;
        private Boolean dualstackEnabled;
        private Boolean fipsEnabled;

        public Builder signingClock(Clock signingClock) {
            this.signingClock = signingClock;
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            return credentialsProvider((IdentityProvider<? extends AwsCredentialsIdentity>) credentialsProvider);
        }

        @Override
        public Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        @Override
        public Builder dualstackEnabled(Boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return this;
        }

        @Override
        public Builder fipsEnabled(Boolean fipsEnabled) {
            this.fipsEnabled = fipsEnabled;
            return this;
        }

        @Override
        public Builder endpointOverride(URI endpointOverride) {
            this.endpointOverride = endpointOverride;
            return this;
        }

        @Override
        public PollyPresigner build() {
            return new DefaultPollyPresigner(this);
        }
    }
}
