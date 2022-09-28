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

@SdkInternalApi
public final class AwsProviderUtils {
    private static final Logger LOG = Logger.loggerFor(AwsProviderUtils.class);

    private AwsProviderUtils() {
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

    public static String endpointBuiltIn(ExecutionAttributes executionAttributes) {
        if (endpointIsOverridden(executionAttributes)) {
            return executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT).toString();
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

    public static SdkHttpRequest setUri(SdkHttpRequest request, URI newUri) {
        String newPath = newUri.getRawPath();
        String existingPath = request.getUri().getRawPath();

        if (newPath.endsWith("/") || existingPath.startsWith("/")) {
            newPath += existingPath;
        } else {
            newPath = newPath + "/" + existingPath;
        }

        return request.toBuilder()
                      .protocol(newUri.getScheme())
                      .host(newUri.getHost())
                      .port(newUri.getPort())
                      .encodedPath(newPath)
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
