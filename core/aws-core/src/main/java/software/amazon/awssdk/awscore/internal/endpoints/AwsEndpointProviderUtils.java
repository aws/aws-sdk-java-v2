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

package software.amazon.awssdk.awscore.internal.endpoints;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Shared utility methods for endpoint resolution across all AWS services.
 * Previously this was generated per-service as an identical copy; now de-duplicated here.
 */
@SdkInternalApi
public final class AwsEndpointProviderUtils {

    private AwsEndpointProviderUtils() {
    }

    public static Region regionBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION);
    }

    public static Boolean dualStackEnabledBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED);
    }

    public static Boolean fipsEnabledBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED);
    }

    /**
     * Returns the endpoint set on the client. Note that this strips off the query part of the URI because the endpoint
     * rules library, e.g. {@code ParseURL} will return an exception if the URI it parses has query parameters.
     */
    public static String endpointBuiltIn(ExecutionAttributes executionAttributes) {
        if (endpointIsOverridden(executionAttributes)) {
            executionAttributes.getOptionalAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS).ifPresent(
                metric -> metric.addMetric(BusinessMetricFeatureId.ENDPOINT_OVERRIDE.value()));
            return invokeSafely(() -> {
                URI endpointOverride = executionAttributes.getAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER)
                                                          .clientEndpoint();
                return new URI(endpointOverride.getScheme(), null, endpointOverride.getHost(), endpointOverride.getPort(),
                        endpointOverride.getPath(), null, endpointOverride.getFragment()).toString();
            });
        }
        return null;
    }

    /**
     * True if the {@link SdkInternalExecutionAttribute#CLIENT_ENDPOINT_PROVIDER}'s endpoint is overridden.
     */
    public static boolean endpointIsOverridden(ExecutionAttributes attrs) {
        return attrs.getAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER).isEndpointOverridden();
    }

    /**
     * True if the {@link SdkInternalExecutionAttribute#IS_DISCOVERED_ENDPOINT} attribute is present and its value
     * is {@code true}, {@code false} otherwise.
     */
    public static boolean endpointIsDiscovered(ExecutionAttributes attrs) {
        return attrs.getOptionalAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT).orElse(false);
    }

    /**
     * True if the {@link SdkInternalExecutionAttribute#DISABLE_HOST_PREFIX_INJECTION} attribute is present and its
     * value is {@code true}, {@code false} otherwise.
     */
    public static boolean disableHostPrefixInjection(ExecutionAttributes attrs) {
        return attrs.getOptionalAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION).orElse(false);
    }

    /**
     * Apply the given endpoint prefix to the endpoint.
     */
    public static Endpoint addHostPrefix(Endpoint endpoint, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return endpoint;
        }
        validatePrefixIsHostNameCompliant(prefix);
        URI originalUrl = endpoint.url();
        String newHost = prefix + endpoint.url().getHost();
        URI newUrl = invokeSafely(() -> new URI(originalUrl.getScheme(), null, newHost, originalUrl.getPort(),
                originalUrl.getPath(), originalUrl.getQuery(), originalUrl.getFragment()));
        return endpoint.toBuilder().url(newUrl).build();
    }

    /**
     * This sets the request URI to the resolved URI returned by the endpoint provider. There are some things to be
     * careful about to make this work properly:
     * <p>
     * If the client endpoint is an endpoint override, it may contain a path. In addition, the request marshaller itself
     * may add components to the path if it's modeled for the operation. Unfortunately,
     * {@link SdkHttpRequest#encodedPath()} returns the combined path from both the endpoint and the request. There is
     * no way to know, just from the HTTP request object, where the override path ends (if it's even there) and where
     * the request path starts. Additionally, the rule itself may also append other parts to the endpoint override path.
     * <p>
     * To solve this issue, we pass in the endpoint set on the path, which allows us to the strip the path from the
     * endpoint override from the request path, and then correctly combine the paths.
     * <p>
     * For example, let's suppose the endpoint override on the client is {@code https://example.com/a}. Then we call an
     * operation {@code Foo()}, that marshalls {@code /c} to the path. The resulting request path is {@code /a/c}.
     * However, we also pass the endpoint to provider as a parameter, and the resolver returns
     * {@code https://example.com/a/b}. This method takes care of combining the paths correctly so that the resulting
     * path is {@code https://example.com/a/b/c}.
     */
    public static SdkHttpRequest setUri(SdkHttpRequest request, URI clientEndpoint, URI resolvedUri) {
        String clientEndpointPath = clientEndpoint.getRawPath();
        String requestPath = request.encodedPath();
        String resolvedUriPath = resolvedUri.getRawPath();

        String finalPath = requestPath;
        if (!resolvedUriPath.equals(clientEndpointPath)) {
            finalPath = combinePath(clientEndpointPath, requestPath, resolvedUriPath);
        }

        return request.toBuilder().protocol(resolvedUri.getScheme()).host(resolvedUri.getHost()).port(resolvedUri.getPort())
                .encodedPath(finalPath).build();
    }

    /**
     * Constructs [resolved URI path]/[request path without client endpoint path]. Strips the client endpoint path from
     * the marshalled request path to isolate just the part added by the marshaller, then appends it to the resolved path.
     */
    private static String combinePath(String clientEndpointPath, String requestPath, String resolvedUriPath) {
        String requestPathWithClientPathRemoved = StringUtils.replaceOnce(requestPath, clientEndpointPath, "");
        return SdkHttpUtils.appendUri(resolvedUriPath, requestPathWithClientPathRemoved);
    }

    private static void validatePrefixIsHostNameCompliant(String prefix) {
        String[] components = prefix.split("\\.");
        for (String component : components) {
            HostnameValidator.validateHostnameCompliant(component, component, "request");
        }
    }
}
