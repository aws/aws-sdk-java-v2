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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.endpoints.EndpointAttributeKey;

@SdkInternalApi
public final class KnownS3ExpressEndpointProperty {

    /**
     * An attribute to represent a service component
     */
    public static final EndpointAttributeKey<String> BACKEND =
        new EndpointAttributeKey<>("Backend", String.class);

    public static final List<EndpointAttributeProvider<?>> KNOWN_S3_ENDPOINT_PROPERTIES = Collections.unmodifiableList(
        Arrays.asList(
            new AuthSchemesProperty(),
            new BackendProperty()
        )
    );

    private KnownS3ExpressEndpointProperty() {
    }

    private static class AuthSchemesProperty implements EndpointAttributeProvider<List<EndpointAuthScheme>> {
        @Override
        public String propertyName() {
            return "authSchemes";
        }

        @Override
        public EndpointAttributeKey<List<EndpointAuthScheme>> attributeKey() {
            return AwsEndpointAttribute.AUTH_SCHEMES;
        }

        @Override
        public List<EndpointAuthScheme> attributeValue(Value value) {
            EndpointAuthSchemeStrategyFactory endpointAuthSchemeStrategyFactory = new S3EndpointAuthSchemeStrategyFactory();
            EndpointAuthSchemeStrategy strategy = endpointAuthSchemeStrategyFactory.endpointAuthSchemeStrategy();
            return strategy.createAuthSchemes(value);
        }
    }

    private static class BackendProperty implements EndpointAttributeProvider<String> {
        @Override
        public String propertyName() {
            return "backend";
        }

        @Override
        public EndpointAttributeKey<String> attributeKey() {
            return BACKEND;
        }

        @Override
        public String attributeValue(Value value) {
            return value.expectString();
        }
    }
}
