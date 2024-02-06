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

package software.amazon.awssdk.services.s3.endpoints.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.services.s3.endpoints.authscheme.S3ExpressEndpointAuthScheme;

@SdkInternalApi
public final class S3EndpointAuthSchemeStrategyFactory implements EndpointAuthSchemeStrategyFactory {

    public static final String SIGNING_NAME_ID = "signingName";
    public static final String SIGNING_REGION_SET_ID = "signingRegionSet";
    public static final String DISABLE_DOUBLE_ENCODING_ID = "disableDoubleEncoding";
    public static final String SIGNING_REGION_ID = "signingRegion";

    private static final String SIGV4_NAME = "sigv4";
    private static final String SIGV4A_NAME = "sigv4a";
    private static final String S3EXPRESS_NAME = "sigv4-s3express";

    @Override
    public EndpointAuthSchemeStrategy endpointAuthSchemeStrategy() {
        Map<String, Function<Value.Record, EndpointAuthScheme>> knownAuthSchemesMapping = new HashMap<>();
        knownAuthSchemesMapping.put(SIGV4A_NAME, this::sigV4A);
        knownAuthSchemesMapping.put(SIGV4_NAME, this::sigV4);
        knownAuthSchemesMapping.put(S3EXPRESS_NAME, this::s3Express);
        return new DefaultEndpointAuthSchemeStrategy(knownAuthSchemesMapping);
    }

    private EndpointAuthScheme sigV4A(Value.Record scheme) {
        SigV4aAuthScheme.Builder schemeBuilder = SigV4aAuthScheme.builder();

        Value signingName = scheme.get(Identifier.of(SIGNING_NAME_ID));
        if (signingName != null) {
            schemeBuilder.signingName(signingName.expectString());
        }

        Value signingRegionSet = scheme.get(Identifier.of(SIGNING_REGION_SET_ID));
        if (signingRegionSet != null) {
            Value.Array signingRegionSetArray = signingRegionSet.expectArray();
            for (int j = 0; j < signingRegionSetArray.size(); ++j) {
                schemeBuilder.addSigningRegion(signingRegionSetArray.get(j).expectString());
            }
        }

        Value disableDoubleEncoding = scheme.get(Identifier.of(DISABLE_DOUBLE_ENCODING_ID));
        if (disableDoubleEncoding != null) {
            schemeBuilder.disableDoubleEncoding(disableDoubleEncoding.expectBool());
        }

        return schemeBuilder.build();
    }

    private EndpointAuthScheme sigV4(Value.Record scheme) {
        SigV4AuthScheme.Builder schemeBuilder = SigV4AuthScheme.builder();

        Value signingName = scheme.get(Identifier.of(SIGNING_NAME_ID));
        if (signingName != null) {
            schemeBuilder.signingName(signingName.expectString());
        }

        Value signingRegion = scheme.get(Identifier.of(SIGNING_REGION_ID));
        if (signingRegion != null) {
            schemeBuilder.signingRegion(signingRegion.expectString());
        }

        Value disableDoubleEncoding = scheme.get(Identifier.of(DISABLE_DOUBLE_ENCODING_ID));
        if (disableDoubleEncoding != null) {
            schemeBuilder.disableDoubleEncoding(disableDoubleEncoding.expectBool());
        }

        return schemeBuilder.build();
    }

    private EndpointAuthScheme s3Express(Value.Record scheme) {
        S3ExpressEndpointAuthScheme.Builder schemeBuilder = S3ExpressEndpointAuthScheme.builder();

        Value signingName = scheme.get(Identifier.of(SIGNING_NAME_ID));
        if (signingName != null) {
            schemeBuilder.signingName(signingName.expectString());
        }

        Value signingRegion = scheme.get(Identifier.of(SIGNING_REGION_ID));
        if (signingRegion != null) {
            schemeBuilder.signingRegion(signingRegion.expectString());
        }

        Value disableDoubleEncoding = scheme.get(Identifier.of(DISABLE_DOUBLE_ENCODING_ID));
        if (disableDoubleEncoding != null) {
            schemeBuilder.disableDoubleEncoding(disableDoubleEncoding.expectBool());
        }

        return schemeBuilder.build();
    }
}
