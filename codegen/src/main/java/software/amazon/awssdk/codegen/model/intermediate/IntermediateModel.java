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

package software.amazon.awssdk.codegen.model.intermediate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.utils.IoUtils;

public final class IntermediateModel {
    private static final String FILE_HEADER;

    private Metadata metadata;

    private Map<String, OperationModel> operations;

    private Map<String, ShapeModel> shapes;

    private CustomizationConfig customizationConfig;

    private Map<String, AuthorizerModel> customAuthorizers;

    private Optional<OperationModel> endpointOperation;

    private Map<String, PaginatorDefinition> paginators;

    private Map<String, WaiterDefinition> waiters;

    @JsonIgnore
    private NamingStrategy namingStrategy;

    static {
        FILE_HEADER = loadDefaultFileHeader();
    }

    public IntermediateModel() {
        this.endpointOperation = Optional.empty();
        this.paginators = Collections.emptyMap();
        this.waiters = Collections.emptyMap();
        this.namingStrategy = null;
    }

    public IntermediateModel(Metadata metadata,
                             Map<String, OperationModel> operations,
                             Map<String, ShapeModel> shapes,
                             CustomizationConfig customizationConfig) {
        this(metadata, operations, shapes, customizationConfig, null,
             Collections.emptyMap(), Collections.emptyMap(), null, Collections.emptyMap());
    }

    public IntermediateModel(
        Metadata metadata,
        Map<String, OperationModel> operations,
        Map<String, ShapeModel> shapes,
        CustomizationConfig customizationConfig,
        OperationModel endpointOperation,
        Map<String, AuthorizerModel> customAuthorizers,
        Map<String, PaginatorDefinition> paginators,
        NamingStrategy namingStrategy,
        Map<String, WaiterDefinition> waiters) {
        this.metadata = metadata;
        this.operations = operations;
        this.shapes = shapes;
        this.customizationConfig = customizationConfig;
        this.endpointOperation = Optional.ofNullable(endpointOperation);
        this.customAuthorizers = customAuthorizers;
        this.paginators = paginators;
        this.namingStrategy = namingStrategy;
        this.waiters = waiters;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Map<String, OperationModel> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, OperationModel> operations) {
        this.operations = operations;
    }

    public OperationModel getOperation(String operationName) {
        return getOperations().get(operationName);
    }

    public Map<String, ShapeModel> getShapes() {
        return shapes;
    }

    public void setShapes(Map<String, ShapeModel> shapes) {
        this.shapes = shapes;
    }

    /**
     * Looks up a shape by name and verifies that the expected C2J name matches
     * @param shapeName the name of the shape in the intermediate model
     * @param shapeC2jName C2J's name for the shape
     * @return the ShapeModel
     * @throws IllegalArgumentException if no matching shape is found
     */
    public ShapeModel getShapeByNameAndC2jName(String shapeName, String shapeC2jName) {
        for (ShapeModel sm : getShapes().values()) {
            if (shapeName.equals(sm.getShapeName()) && shapeC2jName.equals(sm.getC2jName())) {
                return sm;
            }
        }
        throw new IllegalArgumentException("C2J shape " + shapeC2jName + " with shape name " + shapeName + " does not exist in "
                                           + "the intermediate model.");
    }

    public CustomizationConfig getCustomizationConfig() {
        return customizationConfig;
    }

    public void setCustomizationConfig(CustomizationConfig customizationConfig) {
        this.customizationConfig = customizationConfig;
    }

    public Map<String, PaginatorDefinition> getPaginators() {
        return paginators;
    }

    public Map<String, WaiterDefinition> getWaiters() {
        return waiters;
    }

    public void setPaginators(Map<String, PaginatorDefinition> paginators) {
        this.paginators = paginators;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public String getCustomRetryPolicy() {
        return customizationConfig.getCustomRetryPolicy();
    }

    public String getSdkModeledExceptionBaseFqcn() {
        return String.format("%s.%s",
                             metadata.getFullModelPackageName(),
                             getSdkModeledExceptionBaseClassName());
    }

    public String getSdkModeledExceptionBaseClassName() {
        if (customizationConfig.getSdkModeledExceptionBaseClassName() != null) {
            return customizationConfig.getSdkModeledExceptionBaseClassName();
        } else {
            return metadata.getBaseExceptionName();
        }
    }

    public String getSdkRequestBaseClassName() {
        if (customizationConfig.getSdkRequestBaseClassName() != null) {
            return customizationConfig.getSdkRequestBaseClassName();
        } else {
            return metadata.getBaseRequestName();
        }
    }

    public String getSdkResponseBaseClassName() {
        if (customizationConfig.getSdkResponseBaseClassName() != null) {
            return customizationConfig.getSdkResponseBaseClassName();
        } else {
            return metadata.getBaseResponseName();
        }
    }

    public String getFileHeader() {
        return FILE_HEADER;
    }

    private static String loadDefaultFileHeader() {
        try (InputStream inputStream =
                 IntermediateModel.class.getResourceAsStream("/software/amazon/awssdk/codegen/DefaultFileHeader.txt")) {
            return IoUtils.toUtf8String(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getSdkBaseResponseFqcn() {
        if (metadata.getProtocol() == Protocol.API_GATEWAY) {
            return "software.amazon.awssdk.opensdk.BaseResult";
        } else {
            return String.format("%s<%s>",
                                 AwsResponse.class.getName(),
                                 getResponseMetadataClassName());
        }
    }

    private String getResponseMetadataClassName() {
        return AwsResponseMetadata.class.getName();
    }

    @JsonIgnore
    public List<OperationModel> simpleMethodsRequiringTesting() {
        return getOperations().values().stream()
                              .filter(v -> v.getInputShape().isSimpleMethod())
                              .collect(Collectors.toList());
    }

    public Map<String, AuthorizerModel> getCustomAuthorizers() {
        return customAuthorizers;
    }

    public void setCustomAuthorizers(Map<String, AuthorizerModel> customAuthorizers) {
        this.customAuthorizers = customAuthorizers;
    }

    public Optional<OperationModel> getEndpointOperation() {
        return endpointOperation;
    }

    public void setEndpointOperation(OperationModel endpointOperation) {
        this.endpointOperation = Optional.ofNullable(endpointOperation);
    }

    public boolean hasPaginators() {
        return paginators.size() > 0;
    }

    public boolean hasWaiters() {
        return waiters.size() > 0;
    }

    public boolean containsRequestSigners() {
        return getShapes().values().stream()
                          .anyMatch(ShapeModel::isRequestSignerAware);
    }

    public boolean containsRequestEventStreams() {
        return getOperations().values().stream()
                              .anyMatch(OperationModel::hasEventStreamInput);
    }
}
