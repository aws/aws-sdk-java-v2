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

package software.amazon.awssdk.core.rules;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;

@SdkInternalApi
public final class ProviderUtils {
    private ProviderUtils() {
    }

    /**
     * True if the the {@link SdkExecutionAttribute#ENDPOINT_OVERRIDDEN} attribute is present and its value is
     * {@code true}, {@code false} otherwise.
     */
    public static boolean endpointIsOverridden(ExecutionAttributes attrs) {
        return attrs.getOptionalAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN).orElse(false);
    }

    /**
     * True if the the {@link SdkInternalExecutionAttribute#IS_DISCOVERED_ENDPOINT} attribute is present and its value is
     * {@code true}, {@code false} otherwise.
     */
    public static boolean endpointIsDiscovered(ExecutionAttributes attrs) {
        return attrs.getOptionalAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT).orElse(false);
    }

    public static Endpoint valueAsEndpointOrThrow(Value value) {
        if (value instanceof Value.Endpoint) {
            Value.Endpoint endpoint = value.expectEndpoint();
            return Endpoint.builder()
                           .url(URI.create(endpoint.getUrl()))
                           .build();
        } else if (value instanceof Value.Str) {
            String errorMsg = value.expectString();
            throw SdkClientException.create(errorMsg);
        } else {
            throw SdkClientException.create("Rule engine return neither an endpoint result or error value. Returned value was:"
                                            + value);
        }
    }

    public static SdkHttpRequest setUri(SdkHttpRequest request, URI newUri) {
        return request.toBuilder()
            .protocol(newUri.getScheme())
            .host(newUri.getHost())
            .port(newUri.getPort())
            .build();
    }
}
