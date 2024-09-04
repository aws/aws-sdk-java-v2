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

package software.amazon.awssdk.core.interceptor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.SdkProtocolMetadata;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Attributes that can be applied to all sdk requests. Only generated code from the SDK clients should set these values.
 */
@SdkProtectedApi
public final class SdkInternalExecutionAttribute extends SdkExecutionAttribute {

    /**
     * The key to indicate if the request is for a full duplex operation ie., request and response are sent/received
     * at the same time.
     */
    public static final ExecutionAttribute<Boolean> IS_FULL_DUPLEX = new ExecutionAttribute<>("IsFullDuplex");

    /**
     * If true, indicates that this is an event streaming request being sent over RPC, and therefore the serialized
     * request object is encapsulated as an event of type {@code initial-request}.
     */
    public static final ExecutionAttribute<Boolean> HAS_INITIAL_REQUEST_EVENT = new ExecutionAttribute<>(
        "HasInitialRequestEvent");

    public static final ExecutionAttribute<HttpChecksumRequired> HTTP_CHECKSUM_REQUIRED =
        new ExecutionAttribute<>("HttpChecksumRequired");

    /**
     * Whether host prefix injection has been disabled on the client.
     * See {@link software.amazon.awssdk.core.client.config.SdkAdvancedClientOption#DISABLE_HOST_PREFIX_INJECTION}
     */
    public static final ExecutionAttribute<Boolean> DISABLE_HOST_PREFIX_INJECTION =
            new ExecutionAttribute<>("DisableHostPrefixInjection");

    /**
     * Key to indicate if the Http Checksums that are valid for an operation.
     */
    public static final ExecutionAttribute<HttpChecksum> HTTP_CHECKSUM =
        new ExecutionAttribute<>("HttpChecksum");

    /**
     * The SDK HTTP attributes that can be passed to the HTTP client
     */
    public static final ExecutionAttribute<SdkHttpExecutionAttributes> SDK_HTTP_EXECUTION_ATTRIBUTES =
        new ExecutionAttribute<>("SdkHttpExecutionAttributes");

    public static final ExecutionAttribute<Boolean> IS_NONE_AUTH_TYPE_REQUEST =
        new ExecutionAttribute<>("IsNoneAuthTypeRequest");

    /**
     * The endpoint provider used to resolve the endpoint of the client. This will be overridden during the request
     * pipeline by the {@link #ENDPOINT_PROVIDER}.
     */
    public static final ExecutionAttribute<ClientEndpointProvider> CLIENT_ENDPOINT_PROVIDER =
        new ExecutionAttribute<>("ClientEndpointProvider");

    /**
     * The endpoint provider used to resolve the destination endpoint for a request.
     */
    public static final ExecutionAttribute<EndpointProvider> ENDPOINT_PROVIDER =
        new ExecutionAttribute<>("EndpointProvider");

    /**
     * The resolved endpoint as computed by the client's configured {@link EndpointProvider}.
     */
    public static final ExecutionAttribute<Endpoint> RESOLVED_ENDPOINT =
        new ExecutionAttribute<>("ResolvedEndpoint");

    /**
     * The values of client context params declared for this service. Client contet params are one possible source of inputs into
     * the endpoint provider for the client.
     */
    public static final ExecutionAttribute<AttributeMap> CLIENT_CONTEXT_PARAMS =
        new ExecutionAttribute<>("ClientContextParams");

    /**
     * Whether the endpoint on the request is the result of Endpoint Discovery.
     */
    public static final ExecutionAttribute<Boolean> IS_DISCOVERED_ENDPOINT =
        new ExecutionAttribute<>("IsDiscoveredEndpoint");

    /**
     * The nano time that the current API call attempt began.
     */
    public static final ExecutionAttribute<Long> API_CALL_ATTEMPT_START_NANO_TIME =
        new ExecutionAttribute<>("ApiCallAttemptStartNanoTime");

    /**
     * The nano time that reading the response headers is complete.
     */
    public static final ExecutionAttribute<Long> HEADERS_READ_END_NANO_TIME =
        new ExecutionAttribute<>("HeadersReadEndNanoTime");

    /**
     * The running count of bytes in the response body that have been read by the client. This is updated atomically as the
     * response is being read.
     * <p>
     * This attribute is set before every API call attempt.
     */
    public static final ExecutionAttribute<AtomicLong> RESPONSE_BYTES_READ =
        new ExecutionAttribute<>("ResponseBytesRead");

    /**
     * The auth scheme provider used to resolve the auth scheme for a request.
     */
    public static final ExecutionAttribute<AuthSchemeProvider> AUTH_SCHEME_RESOLVER =
        new ExecutionAttribute<>("AuthSchemeProvider");

    /**
     * The auth schemes available for a request.
     */
    public static final ExecutionAttribute<Map<String, AuthScheme<?>>> AUTH_SCHEMES = new ExecutionAttribute<>("AuthSchemes");

    /**
     * The {@link IdentityProviders} for a request.
     */
    public static final ExecutionAttribute<IdentityProviders> IDENTITY_PROVIDERS = new ExecutionAttribute<>("IdentityProviders");

    /**
     * The selected auth scheme for a request.
     */
    public static final ExecutionAttribute<SelectedAuthScheme<?>> SELECTED_AUTH_SCHEME =
        new ExecutionAttribute<>("SelectedAuthScheme");

    /**
     * The supported compression algorithms for an operation, and whether the operation is streaming or not.
     */
    public static final ExecutionAttribute<RequestCompression> REQUEST_COMPRESSION =
        new ExecutionAttribute<>("RequestCompression");

    /**
     * The key under which the protocol metadata is stored.
     */
    public static final ExecutionAttribute<SdkProtocolMetadata> PROTOCOL_METADATA =
        new ExecutionAttribute<>("ProtocolMetadata");

    public static final ExecutionAttribute<SdkClient> SDK_CLIENT =
        new ExecutionAttribute<>("SdkClient");

    /**
     * The backing attribute for RESOLVED_CHECKSUM_SPECS.
     * This holds the real ChecksumSpecs value, and is used to map to the ChecksumAlgorithm signer property
     * in the SELECTED_AUTH_SCHEME execution attribute.
     */
    static final ExecutionAttribute<ChecksumSpecs> INTERNAL_RESOLVED_CHECKSUM_SPECS =
        new ExecutionAttribute<>("InternalResolvedChecksumSpecs");

    private SdkInternalExecutionAttribute() {
    }
}
