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
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.awscore.presigner.PresignRequest;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.LazyAwsRegionProvider;
import software.amazon.awssdk.services.polly.internal.presigner.model.transform.SynthesizeSpeechRequestMarshaller;
import software.amazon.awssdk.services.polly.model.PollyRequest;
import software.amazon.awssdk.services.polly.presigner.PollyPresigner;
import software.amazon.awssdk.services.polly.presigner.model.PresignedSynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.presigner.model.SynthesizeSpeechPresignRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link PollyPresigner}.
 */
@SdkInternalApi
public final class DefaultPollyPresigner implements PollyPresigner {
    private static final String SIGNING_NAME = "polly";
    private static final String SERVICE_NAME = "polly";
    private static final AwsRegionProvider DEFAULT_REGION_PROVIDER =
            new LazyAwsRegionProvider(DefaultAwsRegionProviderChain::new);
    private static final AwsCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER =
            DefaultCredentialsProvider.create();
    private static final Aws4Signer DEFAULT_SIGNER = Aws4Signer.create();

    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;
    private final URI endpointOverride;

    private DefaultPollyPresigner(BuilderImpl builder) {
        this.region = builder.region != null ? builder.region : DEFAULT_REGION_PROVIDER.getRegion();
        this.credentialsProvider = builder.credentialsProvider != null
                ? builder.credentialsProvider : DEFAULT_CREDENTIALS_PROVIDER;
        this.endpointOverride = builder.endpointOverride;
    }

    public Region region() {
        return region;
    }

    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    public URI endpointOverride() {
        return endpointOverride;
    }

    @Override
    public void close() {
        IoUtils.closeIfCloseable(credentialsProvider, null);
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
    private <T extends PresignedRequest.Builder, U extends PollyRequest> T presign(T presignedRequest,
                                                              PresignRequest presignRequest,
                                                              U requestToPresign,
                                                              Function<U, SdkHttpFullRequest.Builder> requestMarshaller) {
        ExecutionAttributes execAttrs = createExecutionAttributes(presignRequest, requestToPresign);

        SdkHttpFullRequest marshalledRequest = marshallRequest(requestToPresign, requestMarshaller);
        SdkHttpFullRequest signedHttpRequest = presignRequest(requestToPresign, marshalledRequest, execAttrs);
        initializePresignedRequest(presignedRequest, execAttrs, signedHttpRequest);
        return presignedRequest;
    }

    private void initializePresignedRequest(PresignedRequest.Builder presignedRequest,
                                            ExecutionAttributes execAttrs,
                                            SdkHttpFullRequest signedHttpRequest) {
        List<String> signedHeadersQueryParam = signedHttpRequest.rawQueryParameters().get("X-Amz-SignedHeaders");

        Map<String, List<String>> signedHeaders =
                signedHeadersQueryParam.stream()
                        .flatMap(h -> Stream.of(h.split(";")))
                        .collect(toMap(h -> h, h -> signedHttpRequest.firstMatchingHeader(h)
                                .map(Collections::singletonList)
                                .orElseGet(ArrayList::new)));

        boolean isBrowserExecutable = signedHttpRequest.method() == SdkHttpMethod.GET &&
                (signedHeaders.isEmpty() ||
                        (signedHeaders.size() == 1 && signedHeaders.containsKey("host")));

        presignedRequest.expiration(execAttrs.getAttribute(PRESIGNER_EXPIRATION))
                .isBrowserExecutable(isBrowserExecutable)
                .httpRequest(signedHttpRequest)
                .signedHeaders(signedHeaders);
    }

    private SdkHttpFullRequest presignRequest(PollyRequest requestToPresign,
                                              SdkHttpFullRequest marshalledRequest,
                                              ExecutionAttributes executionAttributes) {
        Presigner presigner = resolvePresigner(requestToPresign);
        SdkHttpFullRequest presigned = presigner.presign(marshalledRequest, executionAttributes);
        List<String> signedHeadersQueryParam = presigned.rawQueryParameters().get("X-Amz-SignedHeaders");
        Validate.validState(signedHeadersQueryParam != null,
                "Only SigV4 presigners are supported at this time, but the configured "
                        + "presigner (%s) did not seem to generate a SigV4 signature.", presigner);
        return presigned;
    }

    private ExecutionAttributes createExecutionAttributes(PresignRequest presignRequest, PollyRequest requestToPresign) {
        Instant signatureExpiration = Instant.now().plus(presignRequest.signatureDuration());
        AwsCredentials credentials = resolveCredentialsProvider(requestToPresign).resolveCredentials();
        Validate.validState(credentials != null, "Credential providers must never return null.");

        return new ExecutionAttributes()
                .putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, credentials)
                .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, SIGNING_NAME)
                .putAttribute(AwsExecutionAttribute.AWS_REGION, region())
                .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, region())
                .putAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX, false)
                .putAttribute(SdkExecutionAttribute.CLIENT_TYPE, ClientType.SYNC)
                .putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE_NAME)
                .putAttribute(PRESIGNER_EXPIRATION, signatureExpiration);
    }

    private AwsCredentialsProvider resolveCredentialsProvider(PollyRequest request) {
        return request.overrideConfiguration().flatMap(AwsRequestOverrideConfiguration::credentialsProvider)
                .orElse(credentialsProvider());
    }

    private Presigner resolvePresigner(PollyRequest request) {
        Signer signer = request.overrideConfiguration().flatMap(AwsRequestOverrideConfiguration::signer)
                .orElse(DEFAULT_SIGNER);

        return Validate.isInstanceOf(Presigner.class, signer,
                "Signer of type %s given in request override is not a Presigner", signer.getClass().getSimpleName());
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
        if (endpointOverride() != null) {
            return endpointOverride();
        }

        return new DefaultServiceEndpointBuilder(SERVICE_NAME, "https")
                .withRegion(region())
                .getServiceEndpoint();
    }

    public static class BuilderImpl implements PollyPresigner.Builder {
        private Region region;
        private AwsCredentialsProvider credentialsProvider;
        private URI endpointOverride;

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
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
