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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.AuthType;

/**
 * Knowledge index to get access to the configured service auth schemes and operations overrides. This index is optimized for
 * code generation of switch statements therefore the data is grouped by operations that share the same auth schemes. This
 * index is a building block for {@link AuthSchemeCodegenKnowledgeIndex} and {@link SigV4AuthSchemeCodegenKnowledgeIndex}
 * indexes that have a friendly interface for the codegen use cases.
 */
public final class ModelAuthSchemeKnowledgeIndex {
    private final IntermediateModel intermediateModel;

    private ModelAuthSchemeKnowledgeIndex(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
    }

    /**
     * Creates a new knowledge index using the given model.
     */
    public static ModelAuthSchemeKnowledgeIndex of(IntermediateModel intermediateModel) {
        return new ModelAuthSchemeKnowledgeIndex(intermediateModel);
    }

    /**
     * Returns a map from a list of operations to all the auth schemes that the operations accept.
     *
     * @return a map from a list of operations to all the auth schemes that the operations accept
     */
    public Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToMetadata() {
        List<AuthType> serviceDefaults = serviceDefaultAuthTypes();
        if (serviceDefaults.size() == 1) {
            String authTypeName = serviceDefaults.get(0).value();
            SigV4SignerDefaults defaults = AuthTypeToSigV4Default.authTypeToDefaults().get(authTypeName);
            if (areServiceWide(defaults)) {
                return operationsToModeledMetadataFormSigV4Defaults(defaults);
            }
        }
        return operationsToModeledMetadata();
    }

    /**
     * Computes a map from operations to codegen metadata objects. The intermediate model is used to compute mappings to
     * {@link AuthType} values for the service and for each operation that has an override. Then we group all the operations that
     * share the same set of auth types together and finally convert the auth types to their corresponding codegen metadata
     * instances that then we can use to codegen switch statements. The service wide codegen metadata instances are keyed using
     * {@link Collections#emptyList()}.
     */
    private Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToModeledMetadata() {
        Map<List<String>, List<AuthType>> operationsToAuthType = operationsToAuthType();
        Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToMetadata = new LinkedHashMap<>();
        operationsToAuthType.forEach((k, v) -> operationsToMetadata.put(k, authTypeToCodegenMetadata(v)));
        return operationsToMetadata;
    }

    /**
     * Returns a map from list of operations to the list of auth-types modeled for those operations. The values are taken directly
     * from the model {@link OperationModel#getAuth()} method.
     */
    private Map<List<String>, List<AuthType>> operationsToAuthType() {
        Map<List<AuthType>, List<String>> authSchemesToOperations =
            intermediateModel.getOperations()
                             .entrySet()
                             .stream()
                             .filter(kvp -> !kvp.getValue().getAuth().isEmpty())
                             .collect(Collectors.groupingBy(kvp -> kvp.getValue().getAuth(),
                                                            Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        Map<List<String>, List<AuthType>> operationsToAuthType = authSchemesToOperations
            .entrySet()
            .stream()
            .sorted(Comparator.comparing(kvp -> kvp.getValue().get(0)))
            .collect(Collectors.toMap(Map.Entry::getValue,
                                      Map.Entry::getKey, (a, b) -> b,
                                      LinkedHashMap::new));

        List<AuthType> serviceDefaults = serviceDefaultAuthTypes();

        // Get the list of operations that share the same auth schemes as the system defaults and remove it from the result. We
        // will take care of all of these in the fallback `default` case.
        List<String> operationsWithDefaults = authSchemesToOperations.remove(serviceDefaults);
        operationsToAuthType.remove(operationsWithDefaults);
        operationsToAuthType.put(Collections.emptyList(), serviceDefaults);
        return operationsToAuthType;
    }


    /**
     * Similar to {@link #operationsToModeledMetadata()} computes a map from operations to codegen metadata objects. The service
     * default list of codegen metadata is keyed with {@link Collections#emptyList()}.
     */
    private Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToModeledMetadataFormSigV4Defaults(
        SigV4SignerDefaults defaults
    ) {
        Map<SigV4SignerDefaults, List<String>> defaultsToOperations =
            defaults.operations()
                    .entrySet()
                    .stream()
                    .collect(Collectors.groupingBy(Map.Entry::getValue,
                                                   Collectors.mapping(Map.Entry::getKey,
                                                                      Collectors.toList())));

        Map<List<String>, SigV4SignerDefaults> operationsToDefaults =
            defaultsToOperations.entrySet()
                                .stream()
                                .sorted(Comparator.comparing(left -> left.getValue().get(0)))
                                .collect(Collectors.toMap(Map.Entry::getValue,
                                                          Map.Entry::getKey, (a, b) -> b,
                                                          LinkedHashMap::new));

        Map<List<String>, List<AuthSchemeCodegenMetadata>> result = new LinkedHashMap<>();
        for (Map.Entry<List<String>, SigV4SignerDefaults> kvp : operationsToDefaults.entrySet()) {
            result.put(kvp.getKey(),
                       Arrays.asList(AuthSchemeCodegenMetadataExt.fromConstants(kvp.getValue())));
        }
        result.put(Collections.emptyList(), Arrays.asList(AuthSchemeCodegenMetadataExt.fromConstants(defaults)));
        return result;
    }

    /**
     * Returns the list of modeled top-level auth-types.
     */
    private List<AuthType> serviceDefaultAuthTypes() {
        List<AuthType> modeled = intermediateModel.getMetadata().getAuth();
        if (!modeled.isEmpty()) {
            return modeled;
        }
        return Collections.singletonList(intermediateModel.getMetadata().getAuthType());
    }

    private List<AuthSchemeCodegenMetadata> authTypeToCodegenMetadata(List<AuthType> authTypes) {
        return authTypes.stream().map(AuthSchemeCodegenMetadataExt::fromAuthType).collect(Collectors.toList());
    }

    private boolean areServiceWide(SigV4SignerDefaults defaults) {
        return defaults != null
               && defaults.isServiceOverrideAuthScheme()
               && Objects.equals(intermediateModel.getMetadata().getServiceName(), defaults.service());
    }
}
