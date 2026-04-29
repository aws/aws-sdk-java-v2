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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.endpoint.EndpointResolver;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.auth.AuthSchemeResolver;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Pipeline stage that resolves the endpoint using the service's endpoint rules engine.
 * <p>
 * This stage runs after all interceptors (including {@code modifyHttpRequest}) and after
 * {@link AuthSchemeResolutionStage}. It calls a service-specific callback to resolve the endpoint,
 * then applies the resolved URL, headers, and metrics.
 * <p>
 */
@SdkInternalApi
public final class EndpointResolutionStage implements MutableRequestToRequestPipeline {

    public EndpointResolutionStage(HttpClientDependencies dependencies) {
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        ExecutionAttributes attrs = context.executionAttributes();

        if (Boolean.TRUE.equals(attrs.getAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT))) {
            return request;
        }

        EndpointResolver resolver = attrs.getAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER);
        if (resolver == null) {
            return request;
        }

        SdkRequest sdkRequest = context.executionContext().interceptorContext().request();

        long resolveEndpointStart = System.nanoTime();
        Endpoint endpoint = resolver.resolve(sdkRequest, attrs);
        Duration resolveEndpointDuration = Duration.ofNanos(System.nanoTime() - resolveEndpointStart);

        // The endpoint resolver callback (generated per-service) may overwrite SELECTED_AUTH_SCHEME with
        // endpoint-resolved signer properties. Re-apply any interceptor-modified properties so they take precedence.
        reapplyInterceptorModifiedAuthProperties(attrs);

        MetricCollector metricCollector = attrs.getAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);
        if (metricCollector != null) {
            metricCollector.reportMetric(CoreMetric.ENDPOINT_RESOLVE_DURATION, resolveEndpointDuration);
        }

        attrs.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, endpoint);

        // Copy endpoint headers onto HTTP request
        Map<String, List<String>> headers = endpoint.headers();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((name, values) ->
                values.forEach(v -> request.appendHeader(name, v)));
        }

        // Apply resolved endpoint URL, unless a customer interceptor modified the URL in modifyHttpRequest()
        ClientEndpointProvider clientEndpointProvider =
            attrs.getAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER);
        if (interceptorModifiedEndpoint(request, attrs)) {
            applyResolvedPath(request, clientEndpointProvider.clientEndpoint(), endpoint.url());
            return request;
        }
        return setUri(request, clientEndpointProvider.clientEndpoint(), endpoint.url());
    }

    /**
     * Detects if an interceptor modified the HTTP request URL in modifyHttpRequest().
     * Compares the current request's host and scheme against the snapshot taken before interceptors ran.
     */
    private static boolean interceptorModifiedEndpoint(SdkHttpFullRequest.Builder request, ExecutionAttributes attrs) {
        URI preModifyUri = attrs.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY);
        if (preModifyUri == null) {
            return false;
        }
        String requestHost = request.host();
        Integer requestPort = request.port();
        return requestHost != null
            && (!requestHost.equals(preModifyUri.getHost())
                || !String.valueOf(request.protocol()).equals(preModifyUri.getScheme())
                || (requestPort != null && requestPort != preModifyUri.getPort()));
    }

    /**
     * Applies the resolved endpoint URL to the HTTP request, merging three path components:
     * client endpoint path + rules engine additions + marshaller operation path.
     */
    private static SdkHttpFullRequest.Builder setUri(SdkHttpFullRequest.Builder request,
                                                      URI clientEndpoint,
                                                      URI resolvedUri) {
        applyResolvedPath(request, clientEndpoint, resolvedUri);
        return request.protocol(resolvedUri.getScheme())
                      .host(resolvedUri.getHost())
                      .port(resolvedUri.getPort());
    }

    private static void applyResolvedPath(SdkHttpFullRequest.Builder request,
                                           URI clientEndpoint,
                                           URI resolvedUri) {
        String clientEndpointPath = clientEndpoint.getRawPath();
        String requestPath = request.encodedPath();
        String resolvedUriPath = resolvedUri.getRawPath();

        if (!resolvedUriPath.equals(clientEndpointPath)) {
            request.encodedPath(combinePath(clientEndpointPath, requestPath, resolvedUriPath));
        }
    }

    private static String combinePath(String clientEndpointPath, String requestPath, String resolvedUriPath) {
        String requestPathWithClientPathRemoved = StringUtils.replaceOnce(requestPath, clientEndpointPath, "");
        return SdkHttpUtils.appendUri(resolvedUriPath, requestPathWithClientPathRemoved);
    }

    /**
     * Re-applies interceptor-modified signer properties after the endpoint resolver callback may have overwritten them.
     * Compares the pre-interceptor snapshot with the post-interceptor (pre-resolution) state to find what interceptors
     * changed, then force-overwrites those properties onto the current auth scheme.
     */
    private static void reapplyInterceptorModifiedAuthProperties(ExecutionAttributes attrs) {
        SelectedAuthScheme<?> currentScheme = attrs.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (currentScheme == null) {
            return;
        }
        SelectedAuthScheme<?> beforeInterceptors =
            attrs.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_BEFORE_INTERCEPTORS);

        SelectedAuthScheme<?> afterInterceptors =
            attrs.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_AFTER_INTERCEPTORS);
        if (afterInterceptors == null) {
            return;
        }

        AuthSchemeResolver.applyInterceptorModifiedProperties(currentScheme, beforeInterceptors, afterInterceptors, attrs);
    }
}
