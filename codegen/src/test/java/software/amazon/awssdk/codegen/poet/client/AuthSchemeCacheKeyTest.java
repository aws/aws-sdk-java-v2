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

package software.amazon.awssdk.codegen.poet.client;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.customPackageModels;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.opsWithSigv4a;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeSpecUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;

/**
 * Verifies that every AwsExecutionAttribute used in auth scheme params building is also present in the cache key.
 * If a new attribute is added to the params without updating the cache key, this test will fail.
 */
public class AuthSchemeCacheKeyTest {

    private static final Pattern AWS_EXEC_ATTR_PATTERN =
        Pattern.compile("AwsExecutionAttribute\\.(\\w+)");

    @Test
    public void restJson_cacheKeyCoversAllParamAttributes() {
        verifyConsistency(restJsonServiceModels());
    }

    @Test
    public void sigv4a_cacheKeyCoversAllParamAttributes() {
        verifyConsistency(opsWithSigv4a());
    }

    @Test
    public void uniformAuth_cacheKeyCoversAllParamAttributes() {
        verifyConsistency(customPackageModels());
    }

    private void verifyConsistency(IntermediateModel model) {
        AuthSchemeSpecUtils authSchemeSpecUtils = new AuthSchemeSpecUtils(model);
        EndpointRulesSpecUtils endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);

        String source = ClientClassUtils.resolveAuthSchemeOptionsMethod(authSchemeSpecUtils, endpointRulesSpecUtils)
                                        .toString();

        int resolveCallIndex = source.indexOf(".resolveAuthScheme(");
        assertThat(resolveCallIndex).as("resolveAuthScheme call should exist in generated method").isGreaterThan(0);

        int cacheKeyStart = source.indexOf("cacheKey =");
        if (cacheKeyStart < 0) {
            // No cache — endpoint-based service, nothing to verify
            return;
        }
        int cacheKeyEnd = source.indexOf(";", cacheKeyStart);

        int paramsStart = source.indexOf("paramsBuilder");
        String paramsSection = source.substring(paramsStart, resolveCallIndex);

        Set<String> paramsAttributes = extractAttributes(paramsSection);
        assertThat(paramsAttributes).as("Expected auth params to reference AwsExecutionAttributes").isNotEmpty();
        String cacheKeySection = source.substring(cacheKeyStart, cacheKeyEnd);
        Set<String> cacheKeyAttributes = extractAttributes(cacheKeySection);

        assertThat(cacheKeyAttributes)
            .as("Cache key must include all AwsExecutionAttributes used in params building. "
                + "If you added a new attribute to params, add it to addAuthSchemeCacheLookup() too.")
            .containsAll(paramsAttributes);
    }

    private Set<String> extractAttributes(String section) {
        Set<String> attributes = new HashSet<>();
        Matcher matcher = AWS_EXEC_ATTR_PATTERN.matcher(section);
        while (matcher.find()) {
            attributes.add(matcher.group(1));
        }
        return attributes;
    }
}
