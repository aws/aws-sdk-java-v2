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

package software.amazon.awssdk.core.internal.crac;

import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * Describes the request the CRaC warm-up sends to the resolved endpoint, and converts it to an {@link SdkHttpRequest} bound to
 * that endpoint. The request is never signed and needs no credentials; only its execution (DNS, TLS, request/response I/O)
 * matters for JIT priming.
 */
@SdkInternalApi
public final class WarmUpRequest {

    private final SdkHttpMethod method;
    private final String body;
    private final String contentType;

    private WarmUpRequest(SdkHttpMethod method, String body, String contentType) {
        this.method = method;
        this.body = body;
        this.contentType = contentType;
    }

    /**
     * A bare {@code GET} to the endpoint, with no body. Warms DNS, the TLS handshake, and certificate-chain validation, which
     * are the dominant cold-start costs and are shared across all services and operations.
     */
    public static WarmUpRequest get() {
        return new WarmUpRequest(SdkHttpMethod.GET, null, null);
    }

    /**
     * @retusrn this request as an {@link SdkHttpRequest} targeting {@code endpoint}.
     */
    public SdkHttpRequest toHttpRequest(URI endpoint) {
        SdkHttpRequest.Builder builder = SdkHttpRequest.builder()
                                                       .method(method)
                                                       .uri(endpoint);
        if (body != null) {
            builder.putHeader("Content-Type", contentType)
                   .putHeader("Content-Length", String.valueOf(body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length));
        }
        return builder.build();
    }

    /**
     * @return the request body stream provider, or empty if this request has no body.
     */
    public Optional<ContentStreamProvider> contentStreamProvider() {
        return body == null ? Optional.empty() : Optional.of(ContentStreamProvider.fromUtf8String(body));
    }
}
