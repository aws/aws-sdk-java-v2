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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class DefaultEndpointAuthSchemeStrategy implements EndpointAuthSchemeStrategy {
    private static final Logger LOG = Logger.loggerFor(DefaultEndpointAuthSchemeStrategy.class);

    private final Map<String, Function<Value.Record, EndpointAuthScheme>> knownAuthSchemesMapping;

    public DefaultEndpointAuthSchemeStrategy(
        Map<String, Function<Value.Record, EndpointAuthScheme>> knownAuthSchemesMapping) {
        this.knownAuthSchemesMapping = knownAuthSchemesMapping;
    }

    @Override
    public EndpointAuthScheme chooseAuthScheme(List<EndpointAuthScheme> authSchemes) {
        Supplier<SdkClientException> failure =
            () -> SdkClientException.create("Endpoint did not contain any known auth schemes: " + authSchemes);
        return authSchemes.stream()
                          .filter(scheme -> knownAuthSchemesMapping.containsKey(scheme.name()))
                          .findFirst()
                          .orElseThrow(failure);
    }

    @Override
    public List<EndpointAuthScheme> createAuthSchemes(Value authSchemesValue) {
        Value.Array schemesArray = authSchemesValue.expectArray();
        List<EndpointAuthScheme> authSchemes = new ArrayList<>();
        for (int i = 0; i < schemesArray.size(); ++i) {
            Value.Record scheme = schemesArray.get(i).expectRecord();
            String authSchemeName = scheme.get(Identifier.of("name")).expectString();
            Function<Value.Record, EndpointAuthScheme> mapper = knownAuthSchemesMapping.get(authSchemeName);
            if (mapper == null) {
                LOG.debug(() -> "Ignoring unknown auth scheme: " + authSchemeName);
                continue;
            }
            authSchemes.add(mapper.apply(scheme));
        }
        return authSchemes;
    }
}