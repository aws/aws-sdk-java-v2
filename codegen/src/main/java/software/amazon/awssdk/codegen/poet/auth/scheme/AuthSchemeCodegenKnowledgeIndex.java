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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Knowledge index to get access to the configured service auth schemes and operations overrides. This index is optimized for
 * code generation of switch statements therefore the data is grouped by operations that share the same auth schemes.
 */
public final class AuthSchemeCodegenKnowledgeIndex {
    /**
     * We delegate this value to {@link ModelAuthSchemeKnowledgeIndex#operationsToMetadata()}. We just wrap the results in an
     * interface that easier to use for the layer that does the code generation.
     */
    private final Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToAuthSchemes;

    private AuthSchemeCodegenKnowledgeIndex(IntermediateModel intermediateModel) {
        this.operationsToAuthSchemes = ModelAuthSchemeKnowledgeIndex.of(intermediateModel).operationsToMetadata();
    }

    /**
     * Creates a new {@link AuthSchemeCodegenKnowledgeIndex} using the given {@code intermediateModel}..
     */
    public static AuthSchemeCodegenKnowledgeIndex of(IntermediateModel intermediateModel) {
        return new AuthSchemeCodegenKnowledgeIndex(intermediateModel);
    }

    /**
     * Returns the service defaults auth schemes. These can be overridden by operation.
     *
     * @return the service defaults auth schemes.
     */
    public List<AuthSchemeCodegenMetadata> serviceDefaultAuthSchemes() {
        return operationsToAuthSchemes.get(Collections.emptyList());
    }

    /**
     * Returns true if there are auth scheme overrides per operation.
     *
     * @return true if there are auth scheme overrides per operation
     */
    public boolean hasPerOperationAuthSchemesOverrides() {
        // The map at least contains one key-value pair (keyed with Collections.emptyList()).
        // If we have more than that then we have at least one override.
        return operationsToAuthSchemes.size() > 1;
    }

    /**
     * Traverses each group of operations with the same set of auth schemes.
     *
     * @param consumer The consumer to call for each group of operations with the same set of auth schemes.
     */
    public void forEachOperationsOverridesGroup(BiConsumer<List<String>, List<AuthSchemeCodegenMetadata>> consumer) {
        for (Map.Entry<List<String>, List<AuthSchemeCodegenMetadata>> kvp : operationsToAuthSchemes.entrySet()) {
            if (kvp.getKey().isEmpty()) {
                // We are traversing operation groups, ignore service wide defaults.
                continue;
            }
            consumer.accept(kvp.getKey(), kvp.getValue());
        }
    }
}
