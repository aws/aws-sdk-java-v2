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
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.rules.Identifier;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public final class AwsEndpointProviderUtils {
    private static final Logger LOG = Logger.loggerFor(AwsEndpointProviderUtils.class);
    private static final String SIGV4_NAME = "sigv4";
    private static final String SIGV4A_NAME = "sigv4a";

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

    public static AwsRequest addHeaders(AwsRequest request, Map<String, List<String>> headers) {
        AwsRequestOverrideConfiguration.Builder configBuilder = request.overrideConfiguration()
                                                                       .map(AwsRequestOverrideConfiguration::toBuilder)
                                                                       .orElseGet(AwsRequestOverrideConfiguration::builder);


        headers.forEach((name, values) -> {
            List<String> existingValues = configBuilder.headers().get(name);
            List<String> updatedValues;

            if (existingValues != null) {
                updatedValues = new ArrayList<>(existingValues);
            } else {
                updatedValues = new ArrayList<>();
            }

            updatedValues.addAll(values);

            configBuilder.putHeader(name, updatedValues);
        });

        return request.toBuilder()
            .overrideConfiguration(configBuilder.build())
            .build();
    }

    /**
     * Per the spec, the auth schemes list is ordered by preference, so we simply iterate over the list until we find an
     * auth scheme we recognize.
     */
    public static EndpointAuthScheme chooseAuthScheme(List<EndpointAuthScheme> authSchemes) {
        for (EndpointAuthScheme authScheme : authSchemes) {
            if (SIGV4_NAME.equals(authScheme.name()) || SIGV4A_NAME.equals(authScheme.name())) {
                return authScheme;
            }
        }
        throw SdkClientException.create("Endpoint did not contain any known auth schemes: " + authSchemes);
    }

    public static void setSigningParams(ExecutionAttributes executionAttributes, EndpointAuthScheme authScheme) {
        if (authScheme instanceof SigV4AuthScheme) {
            setSigV4SigningParams(executionAttributes, (SigV4AuthScheme) authScheme);
        } else if (authScheme instanceof SigV4aAuthScheme) {
            setSigV4aAuthSigningParams(executionAttributes, (SigV4aAuthScheme) authScheme);
        } else {
            throw SdkClientException.create("Don't know how to set signing params for auth scheme: " + authScheme.name());
        }
    }

    public static void setSigV4SigningParams(ExecutionAttributes executionAttributes, SigV4AuthScheme sigV4AuthScheme) {
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE,
                                         !sigV4AuthScheme.disableDoubleEncoding());

        if (sigV4AuthScheme.signingName() != null) {
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, sigV4AuthScheme.signingName());
        }

        if (sigV4AuthScheme.signingRegion() != null) {
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION,
                                             Region.of(sigV4AuthScheme.signingRegion()));
        }
    }

    public static void setSigV4aAuthSigningParams(ExecutionAttributes executionAttributes, SigV4aAuthScheme sigV4aAuthScheme) {
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE,
                                         !sigV4aAuthScheme.disableDoubleEncoding());

        if (sigV4aAuthScheme.signingName() != null) {
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, sigV4aAuthScheme.signingName());
        }

        if (sigV4aAuthScheme.signingRegionSet() != null) {
            if (sigV4aAuthScheme.signingRegionSet().size() > 1) {
                throw SdkClientException.create("Don't know how to set scope of > 1 region");
            }

            if (sigV4aAuthScheme.signingRegionSet().isEmpty()) {
                throw SdkClientException.create("Signing region set is empty");
            }

            String scope = sigV4aAuthScheme.signingRegionSet().get(0);
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE, RegionScope.create(scope));
        }
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
                case SIGV4A_NAME: {
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
                case SIGV4_NAME: {
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
