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

import static software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeCodegenMetadataExt.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.checksum.HttpChecksum;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;

/**
 * Knowledge index to get access to the configured service auth schemes and operations overrides. This index is optimized for code
 * generation of switch statements therefore the data is grouped by operations that share the same auth schemes. This index is a
 * building block for {@link AuthSchemeCodegenKnowledgeIndex} and {@link SigV4AuthSchemeCodegenKnowledgeIndex} indexes that have a
 * friendly interface for the codegen use cases.
 */
public final class ModelAuthSchemeKnowledgeIndex {
    private final IntermediateModel intermediateModel;
    private final List<AuthType> serviceDefaultAuthType;

    private ModelAuthSchemeKnowledgeIndex(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.serviceDefaultAuthType = serviceDefaultAuthTypes();
    }

    /**
     * Creates a new knowledge index using the given model.
     */
    public static ModelAuthSchemeKnowledgeIndex of(IntermediateModel intermediateModel) {
        return new ModelAuthSchemeKnowledgeIndex(intermediateModel);
    }

    /**
     * Returns a map from a list of operations to the list of auth-types modeled for those operations. The {@link AuthTrait}
     * values are taken directly from the {@link OperationModel}.
     *
     * <p>This method groups operations by their authentication requirements and chunked encoding needs:
     * <ul>
     *   <li>Operations with identical auth traits are grouped together</li>
     *   <li>Operations requiring chunked encoding (streaming operations with HTTP checksum traits - either
     *       requestAlgorithmMember or isRequestChecksumRequired) are separated from regular operations and marked 
     *       with CHUNK_ENCODING_ENABLED property. This ensures they get distinct auth scheme metadata even if they 
     *       share the same base auth traits.</li>
     *   <li>Operations using service defaults are keyed with an empty list</li>
     * </ul>
     *
     * <p>Processing flow:
     * <ol>
     *   <li>Identify all operations requiring chunked encoding
     *     <br>Example: PutObject (streaming + checksum) → chunked encoding needed</li>
     *   <li>Get operations with custom auth traits (validation applied during this step:
     *     WriteGetObjectResponse is filtered out for services with custom auth scheme overrides, 
     *     other operations with custom auth in those services throw exception)
     *     <br>Example: Operations with bearer auth → grouped by auth trait</li>
     *   <li>Process each operation group:
     *     <ul>
     *       <li>Split into regular and chunked encoding operations
     *         <br>Example: [GetObject, PutObject] with SigV4 → [GetObject] regular, [PutObject] chunked</li>
     *       <li>Add regular operations with their auth metadata
     *         <br>Example: [GetObject] → SigV4 metadata</li>
     *       <li>Add chunked operations with CHUNK_ENCODING_ENABLED property
     *         <br>Example: [PutObject] → SigV4 metadata + CHUNK_ENCODING_ENABLED</li>
     *     </ul>
     *   </li>
     *   <li>Process remaining chunked encoding operations that use service defaults
     *     <br>Example: [UploadPart] (chunked, no custom auth) → service defaults + CHUNK_ENCODING_ENABLED</li>
     *   <li>Add service-wide defaults with empty list key
     *     <br>Example: [] → SigV4 metadata (fallback for all other operations)</li>
     * </ol>
     *
     * @return Map where keys are lists of operation names and values are their auth scheme metadata. Empty list key represents
     * service-wide defaults.
     */
    public Map<List<String>, List<AuthSchemeCodegenMetadata>> operationsToMetadata() {

        Set<String> chunkedEncodingOps = operationsShouldUseChunkedEncoding();
        List<AuthSchemeCodegenMetadata> serviceDefaults = serviceDefaultAuthSchemeCodeGenMetadata();

        Map<List<String>, List<AuthSchemeCodegenMetadata>> result = new LinkedHashMap<>();
        Set<String> processedChunkedOps = new HashSet<>();

        processOperationsWithAuthTraits(chunkedEncodingOps, serviceDefaults, result, processedChunkedOps);

        // Handle chunked encoding operations that use service defaults
        processRemainingChunkedEncodingOperations(result, chunkedEncodingOps, processedChunkedOps, serviceDefaults);

        // Add service-wide defaults
        result.put(Collections.emptyList(), serviceDefaults);
        return result;
    }

    private boolean shouldUseChunkedEncoding(OperationModel opModel) {
        if (!opModel.isStreaming()) {
            return false;
        }

        HttpChecksum httpChecksum = opModel.getHttpChecksum();
        if (httpChecksum == null) {
            return false;
        }

        return httpChecksum.getRequestAlgorithmMember() != null || httpChecksum.isRequestChecksumRequired();
    }

    private Set<String> operationsShouldUseChunkedEncoding() {
        return intermediateModel.getOperations().entrySet().stream()
                                .filter(entry -> shouldUseChunkedEncoding(entry.getValue()))
                                .map(v -> v.getKey())
                                .collect(Collectors.toSet());
    }

    private Map<List<String>, List<AuthTrait>> operationsToAuthOptions() {
        // Group operations by their shared AuthTraits.
        // The map's keys are AuthTrait lists, and the values are lists of operation names.
        Map<List<AuthTrait>, List<String>> authSchemesToOperations =
            intermediateModel.getOperations()
                             .entrySet()
                             .stream()
                             .filter(this::hasAuthTrait)
                             .collect(Collectors.groupingBy(
                                 kvp -> toAuthTrait(kvp.getValue()),
                                 Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                             ));

        // Convert the map to have operation names as keys and AuthTrait options as values,
        // sorted by the first operation name in each group.
        Map<List<String>, List<AuthTrait>> operationsToAuthTrait = authSchemesToOperations
            .entrySet()
            .stream()
            .sorted(Comparator.comparing(kvp -> kvp.getValue().get(0)))
            .collect(Collectors.toMap(Map.Entry::getValue,
                                      Map.Entry::getKey, (a, b) -> b,
                                      LinkedHashMap::new));

        List<AuthTrait> serviceDefaults = serviceDefaultAuthTrait();

        // Get the list of operations that share the same auth schemes as the system defaults and remove it from the result. We
        // will take care of all of these in the fallback `default` case.
        List<String> operationsWithDefaults = authSchemesToOperations.remove(serviceDefaults);
        if (operationsWithDefaults != null) {
            operationsToAuthTrait.remove(operationsWithDefaults);
        }

        return operationsToAuthTrait;
    }

    /**
     * Determines if an operation should be included based on whether it has auth traits.
     *
     * <p>Returns false if:
     * <ul>
     *   <li>The operation has no modeled auth traits (empty auth list)</li>
     * </ul>
     *
     * <p>Throws UnsupportedOperationException if:
     * <ul>
     *   <li>The service has hardcoded auth scheme overrides defined in {@link SigV4SignerDefaults}. The
     *   exception is WriteGetObjectResponse, which has "v4-unsigned-body" auth type. It is already handled by the
     *   S3 default auth schemes, so there is no need to add operation override</li>
     * </ul>
     */
    private boolean hasAuthTrait(Map.Entry<String, OperationModel> op) {
        if (op.getValue().getAuth().isEmpty()) {
            return false;
        }

        if (!hasCustomServiceAuthSchemeOverride()) {
            return true;
        }

        if (op.getKey().equals("WriteGetObjectResponse")) {
            return false;
        }

        throw new UnsupportedOperationException(
            String.format("Operation %s has auth trait and requires special handling: ", op.getKey()));
    }

    private void processOperationsWithAuthTraits(Set<String> chunkedEncodingOps,
                                                 List<AuthSchemeCodegenMetadata> serviceDefaults,
                                                 Map<List<String>, List<AuthSchemeCodegenMetadata>> result,
                                                 Set<String> processedChunkedOps) {
        Map<List<String>, List<AuthTrait>> operationsToAuthOption = operationsToAuthOptions();
        for (Map.Entry<List<String>, List<AuthTrait>> entry : operationsToAuthOption.entrySet()) {
            List<String> allOperations = entry.getKey();
            List<AuthTrait> authTraits = entry.getValue();

            // Split operations into regular and chunked encoding
            List<String> regularOps = new ArrayList<>();
            List<String> chunkedOps = new ArrayList<>();
            for (String op : allOperations) {
                if (chunkedEncodingOps.contains(op)) {
                    chunkedOps.add(op);
                    processedChunkedOps.add(op);
                } else {
                    regularOps.add(op);
                }
            }

            List<AuthSchemeCodegenMetadata> metadata = determineAuthScheme(authTraits, serviceDefaults);

            if (!regularOps.isEmpty()) {
                result.put(regularOps, metadata);
            }

            if (!chunkedOps.isEmpty()) {
                result.put(chunkedOps, addChunkedEncodingEnabledProperty(metadata));
            }
        }
    }

    private List<AuthSchemeCodegenMetadata> determineAuthScheme(List<AuthTrait> authTraits,
                                                                List<AuthSchemeCodegenMetadata> serviceDefaults) {
        if (authTraits.isEmpty()) {
            return serviceDefaults;
        }
        return authOptionToCodegenMetadata(authTraits);
    }

    private boolean hasCustomServiceAuthSchemeOverride() {
        if (serviceDefaultAuthType.size() == 1) {
            String authTypeName = serviceDefaultAuthType.get(0).value();
            SigV4SignerDefaults defaults = AuthTypeToSigV4Default.authTypeToDefaults().get(authTypeName);
            return defaults != null
                   && defaults.isServiceOverrideAuthScheme()
                   && Objects.equals(intermediateModel.getMetadata().getServiceName(), defaults.service());
        }
        return false;
    }

    private void processRemainingChunkedEncodingOperations(Map<List<String>, List<AuthSchemeCodegenMetadata>> result,
                                                           Set<String> allChunkedOps,
                                                           Set<String> processedChunkedOps,
                                                           List<AuthSchemeCodegenMetadata> serviceDefaults) {
        Set<String> unprocessedOps = new HashSet<>(allChunkedOps);
        unprocessedOps.removeAll(processedChunkedOps);
        if (!unprocessedOps.isEmpty()) {
            result.put(new ArrayList<>(unprocessedOps), addChunkedEncodingEnabledProperty(serviceDefaults));
        }
    }

    private static List<AuthSchemeCodegenMetadata> addChunkedEncodingEnabledProperty(
        List<AuthSchemeCodegenMetadata> authSchemeCodegenMetadata) {
        return
            authSchemeCodegenMetadata.stream()
                                     .map(metadata ->
                                              metadata.toBuilder()
                                                      .addProperty(from(
                                                          "CHUNK_ENCODING_ENABLED",
                                                          () -> true,
                                                          AwsV4HttpSigner.class)).build()
                                     ).collect(Collectors.toList());
    }

    /**
     * Converts an {@link OperationModel} to a list of {@link AuthTrait} instances based on the authentication related traits
     * defined in the {@link OperationModel}.
     */
    private List<AuthTrait> toAuthTrait(OperationModel operation) {
        return operation.getAuth().stream()
                        .map(authType -> AuthTrait.builder()
                                                  .authType(authType)
                                                  .unsignedPayload(operation.isUnsignedPayload())
                                                  .build())
                        .collect(Collectors.toList());
    }

    /**
     * Returns the list of modeled top-level auth-types.
     */
    private List<AuthType> serviceDefaultAuthTypes() {

        // First, look at legacy signature versions
        if (legacySignatureVersion()) {
            return Collections.singletonList(intermediateModel.getMetadata().getAuthType());
        }

        List<AuthType> modeled = intermediateModel.getMetadata().getAuth();
        if (!modeled.isEmpty()) {
            return modeled;
        }
        return Collections.singletonList(intermediateModel.getMetadata().getAuthType());
    }

    /**
     * Legacy signature version, i.e, s3, s3 control
     */
    private boolean legacySignatureVersion() {
        return intermediateModel.getMetadata().getAuthType() != null
               && intermediateModel.getMetadata().getAuthType() != AuthType.V4;
    }

    private List<AuthTrait> serviceDefaultAuthTrait() {
        return serviceDefaultAuthType.stream()
                                     .map(t -> AuthTrait.builder().authType(t).build())
                                     .collect(Collectors.toList());
    }

    private List<AuthSchemeCodegenMetadata> serviceDefaultAuthSchemeCodeGenMetadata() {
        return serviceDefaultAuthTrait().stream()
                                        .map(AuthSchemeCodegenMetadataExt::fromAuthType)
                                        .collect(Collectors.toList());
    }

    private List<AuthSchemeCodegenMetadata> authOptionToCodegenMetadata(List<AuthTrait> authTypes) {
        return authTypes.stream().map(AuthSchemeCodegenMetadataExt::fromAuthType).collect(Collectors.toList());
    }
}
