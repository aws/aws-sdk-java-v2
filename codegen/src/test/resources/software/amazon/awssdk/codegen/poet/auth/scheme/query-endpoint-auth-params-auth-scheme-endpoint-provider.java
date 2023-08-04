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

package software.amazon.awssdk.services.query.auth.scheme.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeParams;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeProvider;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryAuthSchemeProvider implements QueryAuthSchemeProvider {
    private static final DefaultQueryAuthSchemeProvider DEFAULT = new DefaultQueryAuthSchemeProvider();

    private static final QueryAuthSchemeProvider MODELED_RESOLVER = ModeledQueryAuthSchemeProvider.create();

    private static final QueryEndpointProvider DELEGATE = QueryEndpointProvider.defaultProvider();

    private DefaultQueryAuthSchemeProvider() {
    }

    public static QueryAuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(QueryAuthSchemeParams params) {
        QueryEndpointParams endpointParameters = QueryEndpointParams.builder().region(params.region())
                .defaultTrueParam(params.defaultTrueParam()).defaultStringParam(params.defaultStringParam())
                .deprecatedParam(params.deprecatedParam()).booleanContextParam(params.booleanContextParam())
                .stringContextParam(params.stringContextParam()).operationContextParam(params.operationContextParam()).build();
        Endpoint endpoint = DELEGATE.resolveEndpoint(endpointParameters).join();
        List<EndpointAuthScheme> authSchemes = endpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
        if (authSchemes == null) {
            return MODELED_RESOLVER.resolveAuthScheme(params);
        }
        List<AuthSchemeOption> options = new ArrayList<>();
        for (EndpointAuthScheme authScheme : authSchemes) {
            String name = authScheme.name();
            switch (name) {
            case "sigv4":
                SigV4AuthScheme sigv4AuthScheme = Validate.isInstanceOf(SigV4AuthScheme.class, authScheme,
                        "Expecting auth scheme of class SigV4AuthScheme, got instead object of class %s", authScheme.getClass()
                                .getName());
                options.add(AuthSchemeOption
                        .builder()
                        .schemeId("aws.auth#sigv4")
                        .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, sigv4AuthScheme.signingName())
                        .putSignerProperty(AwsV4HttpSigner.REGION_NAME, sigv4AuthScheme.signingRegion())
                        .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, !sigv4AuthScheme.disableDoubleEncoding())
                            .build());
                break;
            case "sigv4a":
                throw new UnsupportedOperationException("SigV4a is not yet supported.");
            default:
                throw new IllegalArgumentException("Unknown auth scheme: " + name);
            }
        }
        return Collections.unmodifiableList(options);
    }
}
