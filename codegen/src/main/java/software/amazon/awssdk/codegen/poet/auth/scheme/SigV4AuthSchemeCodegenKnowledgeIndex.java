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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeCodegenMetadata.SignerPropertyValueProvider;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;

/**
 * Knowledge index to compute the sets of operations that share the same set of sigv4 overrides.
 */
public final class SigV4AuthSchemeCodegenKnowledgeIndex {
    private final Map<List<String>, AuthSchemeCodegenMetadata> operationsToSigv4AuthScheme;

    private SigV4AuthSchemeCodegenKnowledgeIndex(IntermediateModel intermediateModel) {
        this.operationsToSigv4AuthScheme =
            operationsToSigv4AuthScheme(ModelAuthSchemeKnowledgeIndex.of(intermediateModel).operationsToMetadata());
    }

    /**
     * Creates a new knowledge index from the given model.
     */
    public static SigV4AuthSchemeCodegenKnowledgeIndex of(IntermediateModel intermediateModel) {
        return new SigV4AuthSchemeCodegenKnowledgeIndex(intermediateModel);
    }

    /**
     * Returns the service overrides for sigv4. This method returns null if there are none configured. The service may or may not
     * support sigv4 regardless.
     */
    public AuthSchemeCodegenMetadata serviceSigV4Overrides() {
        return operationsToSigv4AuthScheme.get(Collections.emptyList());
    }

    /**
     * Returns true if there are any sigv4 overrides per operation.
     *
     * @return true if there are auth scheme overrides per operation
     */
    public boolean hasPerOperationSigV4Overrides() {
        if (operationsToSigv4AuthScheme.containsKey(Collections.emptyList())) {
            return operationsToSigv4AuthScheme.size() > 1;
        }
        return !operationsToSigv4AuthScheme.isEmpty();
    }

    /**
     * Returns true if there are any service wide sigv4 overrides.
     */
    public boolean hasServiceSigV4Overrides() {
        return serviceSigV4Overrides() != null;
    }

    /**
     * Returns true if there are sigv4 signer overrides in the model.
     */
    public boolean hasSigV4Overrides() {
        return hasServiceSigV4Overrides() || hasPerOperationSigV4Overrides();
    }


    /**
     * Traverses each group of operations with the same set of auth schemes.
     *
     * @param consumer The consumer to call for each group of operations with the same set of auth schemes.
     */
    public void forEachOperationsOverridesGroup(BiConsumer<List<String>, AuthSchemeCodegenMetadata> consumer) {
        for (Map.Entry<List<String>, AuthSchemeCodegenMetadata> kvp : operationsToSigv4AuthScheme.entrySet()) {
            if (kvp.getKey().isEmpty()) {
                // Ignore service wide defaults.
                continue;
            }
            consumer.accept(kvp.getKey(), kvp.getValue());
        }
    }

    /**
     * Returns a map that groups all operations that share the ame set of sigv4 signer properties with override values. The
     * service wide default values are encoded using {@link Collections#emptyList()} as a key and the value may be null.
     */
    private Map<List<String>, AuthSchemeCodegenMetadata> operationsToSigv4AuthScheme(
        Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToMetadata
    ) {
        Map<List<String>, AuthSchemeCodegenMetadata> result = new HashMap<>();
        for (Map.Entry<List<String>, List<AuthSchemeCodegenMetadata>> kvp : operationsToMetadata.entrySet()) {
            AuthSchemeCodegenMetadata sigv4 = sigV4AuthSchemeWithConstantOverrides(kvp.getValue());
            if (sigv4 != null) {
                result.put(kvp.getKey(), sigv4);
            }
        }
        return result;
    }

    /**
     * Finds the sigv4 auth scheme from the list and transforms it to remove any signer property that does not have a constant
     * value. Returns null if there are no signer properties with constant values or if the sigv4 auth scheme is not found.
     */
    private AuthSchemeCodegenMetadata sigV4AuthSchemeWithConstantOverrides(List<AuthSchemeCodegenMetadata> authSchemes) {
        AuthSchemeCodegenMetadata sigv4 = findSigV4AuthScheme(authSchemes);
        if (sigv4 == null) {
            return null;
        }
        List<SignerPropertyValueProvider> signerPropertiesWithConstantValues =
            filterSignerPropertiesWithConstantValues(sigv4.properties());

        // No signer properties with overrides, we return null: we are only
        // interested when there are any properties with constant values for codegen.
        if (signerPropertiesWithConstantValues.isEmpty()) {
            return null;
        }
        // Return the auth scheme but only retain the properties with constant values.
        return sigv4.toBuilder()
                    .properties(signerPropertiesWithConstantValues)
                    .build();
    }

    /**
     * Returns a new list of signer properties with only those properties that use a constant value.
     */
    private List<SignerPropertyValueProvider> filterSignerPropertiesWithConstantValues(
        List<SignerPropertyValueProvider> properties
    ) {
        List<SignerPropertyValueProvider> result = null;
        for (SignerPropertyValueProvider property : properties) {
            if (property.isConstant()) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(property);
            }
        }
        if (result != null) {
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Filters out the auth scheme with scheme id "aws.auth#sigv4". Returns {@code null} if not found.
     */
    private AuthSchemeCodegenMetadata findSigV4AuthScheme(List<AuthSchemeCodegenMetadata> authSchemes) {
        for (AuthSchemeCodegenMetadata metadata : authSchemes) {
            if (metadata.schemeId().equals(AwsV4AuthScheme.SCHEME_ID)) {
                return metadata;
            }
        }
        return null;
    }
}
