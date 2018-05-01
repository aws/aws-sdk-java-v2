/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.codegen.internal.Constants.APPROVED_SIMPLE_METHOD_VERBS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.core.ResponseMetadata;
import software.amazon.awssdk.utils.IoUtils;

public final class IntermediateModel {

    /**
     * This is used for all service clients unless overridden in the Customizations file.
     */
    private static final String DEFAULT_CLIENT_CONFIG_FACTORY = "LegacyClientConfigurationFactory";

    private final Metadata metadata;

    private final Map<String, OperationModel> operations;

    private final Map<String, ShapeModel> shapes;

    private final CustomizationConfig customizationConfig;

    private final ServiceExamples examples;

    private final Map<String, AuthorizerModel> customAuthorizers;

    @JsonIgnore
    private final Map<String, PaginatorDefinition> paginators;

    @JsonCreator
    public IntermediateModel(
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("operations") Map<String, OperationModel> operations,
        @JsonProperty("shapes") Map<String, ShapeModel> shapes,
        @JsonProperty("customizationConfig") CustomizationConfig customizationConfig,
        @JsonProperty("serviceExamples") ServiceExamples examples) {

        this(metadata, operations, shapes, customizationConfig, examples, Collections.emptyMap(), Collections.emptyMap());
    }

    public IntermediateModel(
        Metadata metadata,
        Map<String, OperationModel> operations,
        Map<String, ShapeModel> shapes,
        CustomizationConfig customizationConfig,
        ServiceExamples examples,
        Map<String, AuthorizerModel> customAuthorizers,
        Map<String, PaginatorDefinition> paginators) {
        this.metadata = metadata;
        this.operations = operations;
        this.shapes = shapes;
        this.customizationConfig = customizationConfig;
        this.examples = examples;
        this.customAuthorizers = customAuthorizers;
        this.paginators = paginators;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Map<String, OperationModel> getOperations() {
        return operations;
    }

    public OperationModel getOperation(String operationName) {
        return getOperations().get(operationName);
    }

    public Map<String, ShapeModel> getShapes() {
        return shapes;
    }

    public ShapeModel getShapeByC2jName(String c2jName) {
        return Utils.findShapeModelByC2jName(this, c2jName);
    }

    public CustomizationConfig getCustomizationConfig() {
        return customizationConfig;
    }

    public ServiceExamples getExamples() {
        return examples;
    }

    public Map<String, PaginatorDefinition> getPaginators() {
        return paginators;
    }

    /**
     * @return Exception unmarshaller implementation to use. Currently only needed by XML based
     * protocols.
     */
    public String getExceptionUnmarshallerImpl() {
        if (customizationConfig.getDefaultExceptionUnmarshaller() != null) {
            return customizationConfig.getDefaultExceptionUnmarshaller();
        } else {
            return metadata.getProtocolDefaultExceptionUmarshallerImpl();
        }
    }

    public String getServiceBaseExceptionFqcn() {
        // TODO Move this into Metadata
        return metadata.getProtocol().getProvider().getBaseExceptionFqcn();
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

    public String getFileHeader() throws IOException {
        return loadDefaultFileHeader();
    }

    private String loadDefaultFileHeader() throws IOException {
        try (InputStream inputStream = getClass()
            .getResourceAsStream("/software/amazon/awssdk/codegen/DefaultFileHeader.txt")) {
            return IoUtils.toString(inputStream)
                          .replaceFirst("%COPYRIGHT_DATE_RANGE%", getCopyrightDateRange());
        }
    }

    private String getCopyrightDateRange() {
        final int currentYear = ZonedDateTime.now().getYear();
        final int copyrightStartYear = currentYear - 5;
        return String.format("%d-%d", copyrightStartYear, currentYear);
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
        return ResponseMetadata.class.getName();
    }

    @JsonIgnore
    public List<OperationModel> simpleMethodsRequiringTesting() {
        return getOperations().values().stream()
                              .filter(v -> v.getInputShape().isSimpleMethod())
                              .filter(v -> !getCustomizationConfig().getVerifiedSimpleMethods().contains(v.getMethodName()))
                              .filter(v -> v.getMethodName().matches(APPROVED_SIMPLE_METHOD_VERBS))
                              .collect(Collectors.toList());
    }

    public Map<String, AuthorizerModel> getCustomAuthorizers() {
        return customAuthorizers;
    }

    public boolean hasPaginators() {
        return paginators.size() > 0;
    }
}
