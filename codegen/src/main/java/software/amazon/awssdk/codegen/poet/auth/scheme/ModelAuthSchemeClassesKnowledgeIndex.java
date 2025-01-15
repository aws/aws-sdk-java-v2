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

package software.amazon.awssdk.codegen.poet.auth.scheme;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;

/**
 * Knowledge index of the configured auth schemes concrete classes.
 */
public final class ModelAuthSchemeClassesKnowledgeIndex {
    private final Set<Class<?>> serviceConcreteAuthSchemeClasses;

    private ModelAuthSchemeClassesKnowledgeIndex(IntermediateModel intermediateModel) {
        this.serviceConcreteAuthSchemeClasses =
            getServiceConcreteAuthSchemeClasses(
                ModelAuthSchemeKnowledgeIndex.of(intermediateModel).operationsToMetadata(),
                intermediateModel.getCustomizationConfig().isEndpointBasedAuthSchemeParamsLegacy());
    }

    /**
     * Creates a new {@link AuthSchemeCodegenKnowledgeIndex} using the given {@code intermediateModel}..
     */
    public static ModelAuthSchemeClassesKnowledgeIndex of(IntermediateModel intermediateModel) {
        return new ModelAuthSchemeClassesKnowledgeIndex(intermediateModel);
    }

    /**
     * Returns the set of all the service supported concrete auth scheme classes.
     */
    public Set<Class<?>> serviceConcreteAuthSchemeClasses() {
        return serviceConcreteAuthSchemeClasses;
    }

    private static Set<Class<?>> getServiceConcreteAuthSchemeClasses(
        Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToAuthSchemes,
        boolean useEndpointBasedAuthProvider
    ) {
        Set<Class<?>> result = operationsToAuthSchemes
            .values()
            .stream()
            .flatMap(Collection::stream)
            .map(AuthSchemeCodegenMetadata::authSchemeClass)
            .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Class::getSimpleName))));
        if (useEndpointBasedAuthProvider) {
            // sigv4a is not modeled but needed for the endpoints based auth-scheme cases.
            result.add(AwsV4aAuthScheme.class);
        }
        // Make the no-auth scheme available.
        result.add(NoAuthAuthScheme.class);
        return Collections.unmodifiableSet(result);
    }
}
