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

package software.amazon.awssdk.awscore.rules;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.rules.Identifier;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public final class AwsEndpointProviderUtils {
    private static final Logger LOG = Logger.loggerFor(AwsEndpointProviderUtils.class);

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
     * Returns the endpoint set on the client. Note that this strips off the query part of the URI because the endpoint rules
     * library, e.g. {@code ParseURL} will return an exception if the URI it parses has query parameters.
     */
    public static String endpointBuiltIn(ExecutionAttributes executionAttributes) {
        if (endpointIsOverridden(executionAttributes)) {
            return invokeSafely(() -> {
                URI endpointOverride = executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT);
                return new URI(endpointOverride.getScheme(), null, endpointOverride.getHost(), endpointOverride.getPort(),
                               endpointOverride.getPath(), null, null).toString();
            });
        }
        return null;
    }

    public static Boolean useGlobalEndpointBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(SdkInternalExecutionAttribute.USE_GLOBAL_ENDPOINT);
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
            Endpoint.Builder builder = Endpoint.builder();
            builder.url(URI.create(endpoint.getUrl()));

            Map<String, List<String>> headers = endpoint.getHeaders();
            if (headers != null) {
                headers.forEach((name, values) -> values.forEach(v -> builder.putHeader(name, v)));
            }

            addKnownProperties(builder, endpoint.getProperties());

            return builder.build();
        } else if (value instanceof Value.Str) {
            String errorMsg = value.expectString();
            throw SdkClientException.create(errorMsg);
        } else {
            throw SdkClientException.create("Rule engine return neither an endpoint result or error value. Returned value was:"
                                            + value);
        }
    }

    /**
     * This sets the request URI to the resolved URI returned by the endpoint provider. There some things to be careful about
     * to make this work properly:
     * <p>
     * If the client endpoint is an endpoint override, it may contain a path. In addition, the request marshaller itself may add
     * components to the path if it's modeled for the operation. Unfortunately, {@link SdkHttpRequest#encodedPath()} returns
     * the combined path from both the endpoint and the request. There is no way to know, just from the HTTP request object,
     * where the override path ends (if it's even there) and where the request path starts. Additionally, the rule itself may
     * also append other parts to the endpoint override path.
     * <p>
     * To solve this issue, we pass in the endpoint set on the path, which allows us to the strip the path from the endpoint
     * override from the request path, and then correctly combine the paths.
     * <p>
     * For example, let's suppose the endpoint override on the client is {@code https://example.com/a}. Then we call an
     * operation {@code Foo()}, that marshalls {@code /c} to the path. The resulting request path is {@code /a/c}. However, we
     * also pass the endpoint to provider as a parameter, and the resolver returns {@code https://example.com/a/b}. This method
     * takes care of combining the paths correctly so that the resulting path is {@code https://example.com/a/b/c}.
     */
    public static SdkHttpRequest setUri(SdkHttpRequest request, URI clientEndpoint, URI resolvedUri) {
        // [client endpoint path]
        String clientEndpointPath = clientEndpoint.getRawPath();

        // [client endpoint path]/[request path]
        String requestPath = request.getUri().getRawPath();

        // [client endpoint path]/[additional path added by resolver]
        String resolvedUriPath = resolvedUri.getRawPath();

        // our goal is to construct [client endpoint path]/[additional path added by resolver]/[request path], so we just need
        // to strip the client endpoint path from the marshalled request path to isolate just the part added by the marshaller
        String requestPathWithClientPathRemoved = StringUtils.replaceOnce(requestPath, clientEndpointPath, "");
        String finalPath = SdkHttpUtils.appendUri(resolvedUriPath, requestPathWithClientPathRemoved);

        return request.toBuilder()
                      .protocol(resolvedUri.getScheme())
                      .host(resolvedUri.getHost())
                      .port(resolvedUri.getPort())
                      .encodedPath(finalPath)
                      .build();
    }

    private static void addKnownProperties(Endpoint.Builder builder, Map<String, Value> properties) {
        properties.forEach((n, v) -> {
            switch (n) {
                case "authSchemes":
                    addAuthSchemes(builder, v);
                    break;
                default:
                    LOG.debug(() -> "Ignoring unknown endpoint property: " + n);
                    break;
            }
        });
    }

    private static void addAuthSchemes(Endpoint.Builder builder, Value authSchemesValue) {
        Value.Array schemesArray = authSchemesValue.expectArray();

        List<EndpointAuthScheme> authSchemes = new ArrayList<>();
        for (int i = 0; i < schemesArray.size(); ++i) {
            Value.Record scheme = schemesArray.get(i).expectRecord();

            String authSchemeName = scheme.get(Identifier.of("name")).expectString();
            switch (authSchemeName) {
                case "sigv4a": {
                    SigV4aAuthScheme.Builder schemeBuilder = SigV4aAuthScheme.builder();

                    Value signingName = scheme.get(Identifier.of("signingName"));
                    if (signingName != null) {
                        schemeBuilder.signingName(signingName.expectString());
                    }

                    Value signingRegionSet = scheme.get(Identifier.of("signingRegionSet"));
                    if (signingRegionSet != null) {
                        Value.Array signingRegionSetArray = signingRegionSet.expectArray();
                        for (int j = 0; j < signingRegionSetArray.size(); ++j) {
                            schemeBuilder.addSigningRegion(signingRegionSetArray.get(j).expectString());
                        }
                    }

                    Value disableDoubleEncoding = scheme.get(Identifier.of("disableDoubleEncoding"));
                    if (disableDoubleEncoding != null) {
                        schemeBuilder.disableDoubleEncoding(disableDoubleEncoding.expectBool());
                    }

                    authSchemes.add(schemeBuilder.build());
                }
                break;
                case "sigv4": {
                    SigV4AuthScheme.Builder schemeBuilder = SigV4AuthScheme.builder();

                    Value signingName = scheme.get(Identifier.of("signingName"));
                    if (signingName != null) {
                        schemeBuilder.signingName(signingName.expectString());
                    }

                    Value signingRegion = scheme.get(Identifier.of("signingRegion"));
                    if (signingRegion != null) {
                        schemeBuilder.signingRegion(signingRegion.expectString());
                    }

                    Value disableDoubleEncoding = scheme.get(Identifier.of("disableDoubleEncoding"));
                    if (disableDoubleEncoding != null) {
                        schemeBuilder.disableDoubleEncoding(disableDoubleEncoding.expectBool());
                    }

                    authSchemes.add(schemeBuilder.build());
                }
                break;
                default:
                    LOG.debug(() -> "Ignoring unknown auth scheme: " + authSchemeName);
                    break;
            }
        }

        builder.putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, authSchemes);
    }
}
