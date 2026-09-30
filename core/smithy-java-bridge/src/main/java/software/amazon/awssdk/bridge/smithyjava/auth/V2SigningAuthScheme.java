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

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.smithy.java.auth.api.Signer;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.client.core.auth.scheme.AuthScheme;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * smithy-java's {@code aws.auth#sigv4} auth scheme, signing with AWS SDK v2's own {@code AwsV4HttpSigner}.
 *
 * <h2>Why the signer is bridged</h2>
 *
 * <p>For most services SigV4 is SigV4 and smithy-java's {@code SigV4Signer} would do. For S3 it is not,
 * because v2 puts a large amount of S3 behavior <em>in its signer</em>, all of it driven by signer
 * properties that v2's auth-scheme provider resolves per operation:
 *
 * <ul>
 *   <li><b>Flexible checksums.</b> The request checksum (CRC32 by default, or whatever algorithm the
 *       caller chose) is computed by the signer, as a header for a non-streaming body and as an
 *       {@code aws-chunked} trailer for a streaming one. smithy-java 1.6.1 implements neither; its only
 *       checksum support is {@code Content-MD5} for {@code @httpChecksumRequired}. Without it, S3's
 *       checksum-required operations — {@code PutObjectTagging}, {@code DeleteObjects},
 *       {@code PutBucketPolicy} and the rest — are rejected outright (ledger 12.9, 13.3).</li>
 *   <li><b>Chunked signing.</b> Over plain HTTP v2 signs a streaming body chunk by chunk
 *       ({@code STREAMING-AWS4-HMAC-SHA256-PAYLOAD}); smithy-java does not implement it and refuses the
 *       request (13.2).</li>
 *   <li><b>{@code UNSIGNED-PAYLOAD}</b> over HTTPS for S3, where smithy-java hashes every body (12.8).</li>
 *   <li><b>{@code DOUBLE_URL_ENCODE} and {@code NORMALIZE_PATH}</b>, both off for S3, which changes the
 *       canonical request for any key with characters that need encoding.</li>
 * </ul>
 *
 * <p>All four are properties of v2's signer and v2's auth-scheme resolution, not of smithy-java's
 * pipeline, so bridging the signer is the faithful fix: the bytes are v2's by construction. The rest of
 * the pipeline — identity resolution, endpoint resolution, retries, serde — stays smithy-java's; the
 * signer is called at the point smithy-java would call its own.
 *
 * <h2>How the signer gets its properties</h2>
 *
 * <p>{@code V2EndpointResolverBridge} already builds the v2 {@link ExecutionAttributes} the rules engine
 * needs, once per attempt, with the call's input in hand. It now also writes the operation's checksum
 * metadata ({@code V2OperationMetadata}) into them and resolves v2's {@link AuthSchemeOption} with v2's
 * own auth-scheme provider, and attaches both to the resolved endpoint as {@link SigningInputs}.
 * Endpoint resolution precedes signing in {@code ClientPipeline}, and the pipeline keeps the endpoint in
 * the call context ({@code CallContext.ENDPOINT}), so they are there when {@link V2SignerBridge} runs.
 * {@link #getSignerProperties} hands the signer the live call context to read them from.
 */
@SdkProtectedApi
public final class V2SigningAuthScheme implements AuthScheme<HttpRequest, AwsCredentialsIdentity> {

    /** What {@code V2EndpointResolverBridge} attaches to the endpoint for the signer, per attempt. */
    public static final Context.Key<SigningInputs> SIGNING_INPUTS = Context.key("v2 signing inputs");

    /** The call context, passed to the signer through its properties; see {@link #getSignerProperties}. */
    static final Context.Key<Context> CALL_CONTEXT = Context.key("v2 signing call context");

    private static final ShapeId SIGV4 = ShapeId.from("aws.auth#sigv4");

    private final V2SignerBridge signer;

    /**
     * @param fallbackSigningName signing name for a call that reached the signer without
     *                            {@link SigningInputs}, which should not happen on a bridged client.
     * @param fallbackRegion      signing region, likewise.
     */
    public V2SigningAuthScheme(String fallbackSigningName, String fallbackRegion) {
        this(fallbackSigningName, fallbackRegion, null);
    }

    /**
     * @param clientSigner a client-level legacy {@code Signer} override, or null.
     */
    @SuppressWarnings("deprecation")
    public V2SigningAuthScheme(String fallbackSigningName, String fallbackRegion,
                               software.amazon.awssdk.core.signer.Signer clientSigner) {
        this.signer = new V2SignerBridge(fallbackSigningName, fallbackRegion, clientSigner);
    }

    @Override
    public ShapeId schemeId() {
        return SIGV4;
    }

    @Override
    public Class<HttpRequest> requestClass() {
        return HttpRequest.class;
    }

    @Override
    public Class<AwsCredentialsIdentity> identityClass() {
        return AwsCredentialsIdentity.class;
    }

    /**
     * Passes the call context itself, by reference.
     *
     * <p>This is called while the auth scheme is resolved, <em>before</em> the endpoint is — so the
     * signing inputs do not exist yet. A reference to the live context, rather than a copy of it, is what
     * lets the signer read what endpoint resolution writes afterwards.
     */
    @Override
    public Context getSignerProperties(Context context) {
        return Context.create().put(CALL_CONTEXT, context);
    }

    @Override
    public Signer<HttpRequest, AwsCredentialsIdentity> signer() {
        return signer;
    }

    /**
     * Everything v2's signing stage would have had for this attempt.
     *
     * @param authSchemeOption the option v2's auth-scheme provider selected, carrying v2's signer
     *                         properties (signing name and region, double-encoding, payload signing,
     *                         chunk encoding).
     * @param attributes       the attempt's v2 execution attributes, including the operation's checksum
     *                         metadata and the client's checksum configuration.
     */
    public record SigningInputs(AuthSchemeOption authSchemeOption, ExecutionAttributes attributes) {
    }
}
