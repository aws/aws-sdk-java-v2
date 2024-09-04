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

package software.amazon.awssdk.services.rds.internal;

import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.endpoint.AwsClientEndpointProvider;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.protocols.query.AwsQueryProtocolFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.model.RdsRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Abstract pre-sign handler that follows the pre-signing scheme outlined in the 'RDS Presigned URL for Cross-Region Copying'
 * SEP.
 *
 * @param <T> The request type.
 */
@SdkInternalApi
public abstract class RdsPresignInterceptor<T extends RdsRequest> implements ExecutionInterceptor {

    private static final ClientEndpointProvider CUSTOM_ENDPOINT_PROVIDER_LOCALHOST =
        ClientEndpointProvider.forEndpointOverride(URI.create("http://localhost"));

    protected static final AwsQueryProtocolFactory PROTOCOL_FACTORY = AwsQueryProtocolFactory
        .builder()
        // Need an endpoint to marshall but this will be overwritten in modifyHttpRequest
        .clientConfiguration(SdkClientConfiguration.builder()
                                                   .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                                                           CUSTOM_ENDPOINT_PROVIDER_LOCALHOST)
                                                   .build())
        .build();

    private static final String SERVICE_NAME = "rds";
    private static final String PARAM_SOURCE_REGION = "SourceRegion";
    private static final String PARAM_DESTINATION_REGION = "DestinationRegion";
    private static final String PARAM_PRESIGNED_URL = "PreSignedUrl";


    public interface PresignableRequest {
        String getSourceRegion();

        SdkHttpFullRequest marshall();
    }

    private final Class<T> requestClassToPreSign;

    private final Clock signingClockOverride;

    protected RdsPresignInterceptor(Class<T> requestClassToPreSign) {
        this(requestClassToPreSign, null);
    }

    protected RdsPresignInterceptor(Class<T> requestClassToPreSign, Clock signingClockOverride) {
        this.requestClassToPreSign = requestClassToPreSign;
        this.signingClockOverride = signingClockOverride;
    }

    @Override
    public final SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                                  ExecutionAttributes executionAttributes) {
        SdkHttpRequest request = context.httpRequest();
        PresignableRequest presignableRequest = toPresignableRequest(request, context);
        if (presignableRequest == null) {
            return request.toBuilder().removeQueryParameter(PARAM_SOURCE_REGION).build();
        }

        SelectedAuthScheme<?> selectedAuthScheme = executionAttributes.getAttribute(SELECTED_AUTH_SCHEME);
        String sourceRegion = presignableRequest.getSourceRegion();
        String destinationRegion = selectedAuthScheme.authSchemeOption().signerProperty(AwsV4HttpSigner.REGION_NAME);
        URI endpoint = createEndpoint(sourceRegion, SERVICE_NAME, executionAttributes);
        SdkHttpFullRequest.Builder marshalledRequest = presignableRequest.marshall().toBuilder().uri(endpoint);

        SdkHttpFullRequest requestToPresign =
            marshalledRequest.method(SdkHttpMethod.GET)
                             .putRawQueryParameter(PARAM_DESTINATION_REGION, destinationRegion)
                             .removeQueryParameter(PARAM_SOURCE_REGION)
                             .build();

        requestToPresign = sraPresignRequest(executionAttributes, requestToPresign, sourceRegion);

        String presignedUrl = requestToPresign.getUri().toString();

        return request.toBuilder()
                      .putRawQueryParameter(PARAM_PRESIGNED_URL, presignedUrl)
                      // Remove the unmodeled params to stop them getting onto the wire
                      .removeQueryParameter(PARAM_SOURCE_REGION)
                      .build();
    }

    /**
     * Adapts the request to the {@link PresignableRequest}.
     *
     * @param originalRequest the original request
     * @return a PresignableRequest
     */
    protected abstract PresignableRequest adaptRequest(T originalRequest);

    /**
     * Converts the request to a PresignableRequest if possible.
     */
    private PresignableRequest toPresignableRequest(SdkHttpRequest request, Context.ModifyHttpRequest context) {
        SdkRequest originalRequest = context.request();
        if (!requestClassToPreSign.isInstance(originalRequest)) {
            return null;
        }
        if (request.firstMatchingRawQueryParameter(PARAM_PRESIGNED_URL).isPresent()) {
            return null;
        }

        PresignableRequest presignableRequest = adaptRequest(requestClassToPreSign.cast(originalRequest));
        String sourceRegion = presignableRequest.getSourceRegion();
        if (sourceRegion == null) {
            return null;
        }
        return presignableRequest;
    }

    /**
     * Presign the provided HTTP request using SRA HttpSigner
     */
    private SdkHttpFullRequest sraPresignRequest(ExecutionAttributes executionAttributes, SdkHttpFullRequest request,
                                                 String signingRegion) {
        SelectedAuthScheme<?> selectedAuthScheme = executionAttributes.getAttribute(SELECTED_AUTH_SCHEME);
        Instant signingInstant;
        if (signingClockOverride != null) {
            signingInstant = signingClockOverride.instant();
        } else {
            signingInstant = Instant.now();
        }
        // A fixed signing clock is used so that the current time used by the signing logic, as well as to
        // determine expiration are the same.
        Clock signingClock = Clock.fixed(signingInstant, ZoneOffset.UTC);
        Duration expirationDuration = Duration.ofDays(7);
        return doSraPresign(request, selectedAuthScheme, signingRegion, signingClock, expirationDuration);
    }

    private <T extends Identity> SdkHttpFullRequest doSraPresign(SdkHttpFullRequest request,
                                                                 SelectedAuthScheme<T> selectedAuthScheme,
                                                                 String signingRegion,
                                                                 Clock signingClock,
                                                                 Duration expirationDuration) {
        CompletableFuture<? extends T> identityFuture = selectedAuthScheme.identity();
        T identity = CompletableFutureUtils.joinLikeSync(identityFuture);

        // Pre-signed URL puts auth info in query string, does not sign the payload, and has an expiry.
        SignRequest.Builder<T> signRequestBuilder = SignRequest
            .builder(identity)
            .putProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION, AwsV4FamilyHttpSigner.AuthLocation.QUERY_STRING)
            .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, expirationDuration)
            .putProperty(HttpSigner.SIGNING_CLOCK, signingClock)
            .request(request)
            .payload(request.contentStreamProvider().orElse(null));
        AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();
        authSchemeOption.forEachSignerProperty(signRequestBuilder::putProperty);
        // Override the region
        signRequestBuilder.putProperty(AwsV4HttpSigner.REGION_NAME, signingRegion);
        HttpSigner<T> signer = selectedAuthScheme.signer();
        SignedRequest signedRequest = signer.sign(signRequestBuilder.build());
        return toSdkHttpFullRequest(signedRequest);
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
                                 .removeQueryParameter(PARAM_SOURCE_REGION)
                                 .build();
    }

    private URI createEndpoint(String regionName, String serviceName, ExecutionAttributes attributes) {
        return AwsClientEndpointProvider.builder()
                                        .serviceEndpointPrefix(SERVICE_NAME)
                                        .defaultProtocol(Protocol.HTTPS.toString())
                                        .region(Region.of(regionName))
                                        .profileFile(attributes.getAttribute(SdkExecutionAttribute.PROFILE_FILE_SUPPLIER))
                                        .profileName(attributes.getAttribute(SdkExecutionAttribute.PROFILE_NAME))
                                        .dualstackEnabled(
                                            attributes.getAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED))
                                        .fipsEnabled(attributes.getAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED))
                                        .build()
                                        .clientEndpoint();
    }
}
