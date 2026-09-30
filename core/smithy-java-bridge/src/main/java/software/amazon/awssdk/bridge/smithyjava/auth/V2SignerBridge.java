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

package software.amazon.awssdk.bridge.smithyjava.auth;

import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.bridge.smithyjava.client.V2RequestOverrides;
import software.amazon.awssdk.bridge.smithyjava.streaming.V2DataStreams;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.util.HttpChecksumResolver;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.smithy.java.auth.api.SignResult;
import software.amazon.smithy.java.auth.api.Signer;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * A smithy-java {@link Signer} that signs with v2's {@link AwsV4HttpSigner}. See
 * {@link V2SigningAuthScheme} for why, and for where its inputs come from.
 *
 * <p>This is v2's {@code SigningStage} plus the checksum half of {@code HttpChecksumStage}, reduced to
 * what a single signing call needs. The checksum logic is not reimplemented: the stage's decision
 * ({@code HttpChecksumUtils.isHttpChecksumCalculationNeeded}) and the spec resolution
 * ({@code HttpChecksumResolver}) are v2's own static methods, run against the attempt's v2 attributes, and
 * the only lines here are the ones {@code HttpChecksumStage#sraChecksum} wraps around them. The computing
 * — header or trailer, and whatever {@code aws-chunked} framing a trailer needs — is v2's signer.
 */
@SdkInternalApi
final class V2SignerBridge implements Signer<HttpRequest, AwsCredentialsIdentity> {

    private final AwsV4HttpSigner v2Signer = AwsV4HttpSigner.create();
    private final AuthSchemeOption fallbackOption;

    @SuppressWarnings("deprecation")
    private final software.amazon.awssdk.core.signer.Signer clientSigner;

    @SuppressWarnings("deprecation")
    V2SignerBridge(String fallbackSigningName, String fallbackRegion, software.amazon.awssdk.core.signer.Signer clientSigner) {
        this.clientSigner = clientSigner;
        this.fallbackOption = AuthSchemeOption.builder()
                                              .schemeId(AwsV4HttpSigner.class.getName())
                                              .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, fallbackSigningName)
                                              .putSignerProperty(AwsV4HttpSigner.REGION_NAME, fallbackRegion)
                                              .build();
    }

    @Override
    public SignResult<HttpRequest> sign(HttpRequest request, AwsCredentialsIdentity identity, Context properties) {
        software.amazon.awssdk.identity.spi.AwsCredentialsIdentity v2Identity = V2IdentityResolver.toV2(identity);
        SdkHttpFullRequest.Builder v2Request = toV2(request);

        AuthSchemeOption option = resolveOption(properties, v2Request, v2Identity);
        DataStream body = request.body();

        Context call = properties.get(V2SigningAuthScheme.CALL_CONTEXT);
        RequestOverrideConfiguration overrides = call == null ? null : call.get(V2RequestOverrides.KEY);
        if (overrides != null && overrides.signer().isPresent()) {
            return new SignResult<>(signLegacy(request, v2Request, v2Identity, option, body, overrides.signer().get()));
        }
        if (clientSigner != null) {
            return new SignResult<>(signLegacy(request, v2Request, v2Identity, option, body, clientSigner));
        }

        if (body instanceof V2DataStreams.AsyncBody asyncBody) {
            return new SignResult<>(signAsync(request, v2Request, v2Identity, option, asyncBody));
        }
        return new SignResult<>(signSync(request, v2Request, v2Identity, option, body));
    }

    /**
     * v2's auth option for this attempt, with {@code CHECKSUM_ALGORITHM} decided the way
     * {@code HttpChecksumStage#sraChecksum} decides it.
     */
    private AuthSchemeOption resolveOption(Context properties,
                                           SdkHttpFullRequest.Builder v2Request,
                                           software.amazon.awssdk.identity.spi.AwsCredentialsIdentity v2Identity) {
        Context call = properties.get(V2SigningAuthScheme.CALL_CONTEXT);
        Endpoint endpoint = call == null ? null : call.get(CallContext.ENDPOINT);
        V2SigningAuthScheme.SigningInputs inputs =
            endpoint == null ? null : endpoint.property(V2SigningAuthScheme.SIGNING_INPUTS);
        if (inputs == null) {
            return fallbackOption;
        }

        ExecutionAttributes attributes = inputs.attributes();
        // No checksum metadata on the operation (every DynamoDB operation): v2's checksum logic cannot
        // decide anything, so skip building the selected scheme and specs it would read. ~2% of a small call.
        if (attributes.getAttribute(software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.HTTP_CHECKSUM) == null
            && attributes.getAttribute(software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute
                                           .HTTP_CHECKSUM_REQUIRED) == null) {
            return inputs.authSchemeOption();
        }
        // RESOLVED_CHECKSUM_SPECS is a view onto SELECTED_AUTH_SCHEME's CHECKSUM_ALGORITHM signer property
        // (SdkExecutionAttribute's read/write mappings), so the selected scheme has to be in place first;
        // after that, writing the spec is what sets the signer property.
        attributes.putAttribute(SELECTED_AUTH_SCHEME,
                                new SelectedAuthScheme<>(CompletableFuture.completedFuture(v2Identity),
                                                         v2Signer,
                                                         inputs.authSchemeOption()));
        attributes.putAttribute(RESOLVED_CHECKSUM_SPECS, HttpChecksumResolver.resolveChecksumSpecs(attributes));

        if (HttpChecksumUtils.isHttpChecksumCalculationNeeded(v2Request, attributes)) {
            ChecksumSpecs specs = attributes.getAttribute(RESOLVED_CHECKSUM_SPECS);
            if (specs == null || specs.algorithmV2() == null) {
                specs = (specs == null ? ChecksumSpecs.builder() : specs.toBuilder())
                    .algorithmV2(DefaultChecksumAlgorithm.CRC32)
                    .headerName(ChecksumUtil.checksumHeaderName(DefaultChecksumAlgorithm.CRC32))
                    .build();
                if (specs.requestAlgorithmHeader() != null) {
                    v2Request.putHeader(specs.requestAlgorithmHeader(), DefaultChecksumAlgorithm.CRC32.algorithmId());
                }
            }
            attributes.putAttribute(RESOLVED_CHECKSUM_SPECS, specs);
        }
        return attributes.getAttribute(SELECTED_AUTH_SCHEME).authSchemeOption();
    }

    private HttpRequest signSync(HttpRequest original, SdkHttpFullRequest.Builder v2Request,
                                 software.amazon.awssdk.identity.spi.AwsCredentialsIdentity v2Identity,
                                 AuthSchemeOption option, DataStream body) {
        ContentStreamProvider payload = toContentStreamProvider(body);
        SignRequest.Builder<software.amazon.awssdk.identity.spi.AwsCredentialsIdentity> signRequest =
            SignRequest.builder(v2Identity).request(v2Request.build()).payload(payload);
        option.forEachSignerProperty(signRequest::putProperty);

        SignedRequest signed = v2Signer.sign(signRequest.build());

        ContentStreamProvider signedPayload = signed.payload().orElse(null);
        DataStream signedBody = signedPayload == payload || signedPayload == null
                                ? body
                                : V2DataStreams.fromContentStreamProvider(signedPayload,
                                                                         body == null ? null : body.contentType(),
                                                                         contentLength(signed.request()));
        return toSmithy(original, signed.request(), signedBody);
    }

    private HttpRequest signAsync(HttpRequest original, SdkHttpFullRequest.Builder v2Request,
                                  software.amazon.awssdk.identity.spi.AwsCredentialsIdentity v2Identity,
                                  AuthSchemeOption option, V2DataStreams.AsyncBody body) {
        Publisher<ByteBuffer> payload = body.asyncRequestBody();
        AsyncSignRequest.Builder<software.amazon.awssdk.identity.spi.AwsCredentialsIdentity> signRequest =
            AsyncSignRequest.builder(v2Identity).request(v2Request.build()).payload(payload);
        option.forEachSignerProperty(signRequest::putProperty);

        // join is fine: this runs inside Client#call, which on an async client is the call's virtual thread,
        // and v2's async signing completes without I/O -- the payload transformation is lazy.
        AsyncSignedRequest signed = v2Signer.signAsync(signRequest.build()).join();

        Publisher<ByteBuffer> signedPayload = signed.payload().orElse(null);
        DataStream signedBody = signedPayload == payload || signedPayload == null
                                ? body
                                : DataStream.ofPublisher(FlowAdapters.toFlowPublisher(signedPayload),
                                                         body.contentType(),
                                                         contentLength(signed.request()),
                                                         body.isReplayable());
        return toSmithy(original, signed.request(), signedBody);
    }

    /**
     * A request-level legacy {@code Signer} override, run in place of v2's HTTP signer, as v2's
     * {@code SigningStage} runs it: with a full request carrying the body, and the legacy execution
     * attributes an AWS4 signer reads (credentials, signing name, signing region). ledger 2.3.
     */
    @SuppressWarnings("deprecation")
    private static HttpRequest signLegacy(HttpRequest original, SdkHttpFullRequest.Builder v2Request,
                                          software.amazon.awssdk.identity.spi.AwsCredentialsIdentity v2Identity,
                                          AuthSchemeOption option, DataStream body,
                                          software.amazon.awssdk.core.signer.Signer signer) {
        ContentStreamProvider payload = toContentStreamProvider(body);
        if (payload != null) {
            v2Request.contentStreamProvider(payload);
        }
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                                software.amazon.awssdk.auth.credentials.CredentialUtils.toCredentials(v2Identity));
        String signingName = option.signerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME);
        String region = option.signerProperty(AwsV4HttpSigner.REGION_NAME);
        if (signingName != null) {
            attributes.putAttribute(software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME,
                                    signingName);
        }
        if (region != null) {
            attributes.putAttribute(software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SIGNING_REGION,
                                    software.amazon.awssdk.regions.Region.of(region));
        }
        SdkHttpFullRequest signed = signer.sign(v2Request.build(), attributes);
        ContentStreamProvider signedPayload = signed.contentStreamProvider().orElse(null);
        DataStream signedBody = signedPayload == null || signedPayload == payload
                                ? body
                                : V2DataStreams.fromContentStreamProvider(signedPayload,
                                                                         body == null ? null : body.contentType(),
                                                                         contentLength(signed));
        return toSmithy(original, signed, signedBody);
    }

    private static ContentStreamProvider toContentStreamProvider(DataStream body) {
        if (body == null || (body.hasKnownLength() && body.contentLength() == 0)) {
            return null;
        }
        if (body.isReplayable()) {
            return ContentStreamProvider.fromInputStreamSupplier(body::asInputStream);
        }
        return ContentStreamProvider.fromInputStream(body.asInputStream());
    }

    private static long contentLength(SdkHttpRequest signed) {
        return signed.firstMatchingHeader("Content-Length").map(Long::parseLong).orElse(-1L);
    }

    private static SdkHttpFullRequest.Builder toV2(HttpRequest request) {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                                                               .uri(request.uri().toURI())
                                                               .method(SdkHttpMethod.fromValue(request.method()));
        for (Map.Entry<String, List<String>> e : request.headers().map().entrySet()) {
            builder.putHeader(e.getKey(), e.getValue());
        }
        return builder;
    }

    /**
     * The signed request, back in smithy's model. Headers are replaced wholesale — the signer adds
     * {@code Authorization}, {@code X-Amz-Date}, {@code x-amz-content-sha256}, {@code Host}, and for a
     * trailer the {@code aws-chunked} framing headers — and the URI is taken from the signed request in
     * case signing changed it.
     */
    private static HttpRequest toSmithy(HttpRequest original, SdkHttpRequest signed, DataStream body) {
        ModifiableHttpRequest out = original.toModifiableCopy();
        // Only what signing changed: the signer adds a handful of headers (Authorization, X-Amz-Date,
        // x-amz-content-sha256, Host, framing headers for a trailer) and normally leaves the URI alone.
        // Rewriting every header and re-parsing the URI was ~3% of a small call.
        java.net.URI signedUri = signed.getUri();
        if (!signedUri.equals(original.uri().toURI())) {
            out.setUri(signedUri);
        }
        software.amazon.smithy.java.http.api.HttpHeaders before = original.headers();
        for (Map.Entry<String, List<String>> header : signed.headers().entrySet()) {
            if (!header.getValue().equals(before.allValues(header.getKey()))) {
                out.setHeader(header.getKey(), header.getValue());
            }
        }
        if (body != null) {
            out.setBody(body);
        }
        return out;
    }
}
