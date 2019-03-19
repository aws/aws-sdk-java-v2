/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.model.config.customization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.utils.AttributeMap;

public class CustomizationConfig {

    /**
     * List of 'convenience' overloads to generate for model classes. Convenience overloads expose a
     * different type that is adapted to the real type
     */
    private final List<ConvenienceTypeOverload> convenienceTypeOverloads = new ArrayList<>();

    /**
     * Specifies the name of the client configuration class to use if a service
     * has a specific advanced client configuration class. Null if the service
     * does not have advanced configuration.
     */
    private String serviceSpecificClientConfigClass;
    /**
     * Specify shapes to be renamed.
     */
    private Map<String, String> renameShapes;

    /**
     * Custom service and intermediate model metadata properties.
     */
    private MetadataConfig customServiceMetadata;
    /**
     * Codegen customization mechanism shared by the .NET SDK
     */
    private Map<String, OperationModifier> operationModifiers;
    private Map<String, ShapeSubstitution> shapeSubstitutions;
    private Map<String, ShapeModifier> shapeModifiers;
    /**
     * Sets the custom field name that identifies the type of modeled exception for JSON protocols.
     * Normally this is '__type' but Glacier has a custom error code field named simply 'code'.
     */
    private String customErrorCodeFieldName;
    /**
     * Service specific base class for all modeled exceptions. By default this is syncInterface +
     * Exception (i.e. AmazonSQSException). Currently only DynamoDB Streams utilizes this
     * customization since it shares exception types with the DynamoDB client.
     *
     * <p>This customization should only provide the simple class name. The typical model package
     * will be used when fully qualifying references to this exception</p>
     *
     * <p><b>Note:</b> that if a custom base class is provided the generator will not generate one.
     * We assume it already exists.</p>
     */
    private String sdkModeledExceptionBaseClassName;
    /**
     * Service calculates CRC32 checksum from compressed file when Accept-Encoding: gzip header is provided.
     */
    private boolean calculateCrc32FromCompressedData;

    /**
     * Exclude the create() method on a client. This is useful for global services that will need a global region configured to
     * work.
     */
    private boolean excludeClientCreateMethod = false;

    /**
     * Configurations for the service that share model with other services. The models and non-request marshallers will be
     * generated into the same directory as the provided service's models.
     */
    private ShareModelConfig shareModelConfig;

    /**
     * Fully qualified name of the class that contains the custom http config. The class should expose a public static method
     * with name "defaultHttpConfig" that returns an {@link AttributeMap} containing the desired http config defaults.
     *
     * See SWF customization.config for an example.
     */
    private String serviceSpecificHttpConfig;

    /**
     * APIs that have no required arguments in their model but can't be called via a simple method
     */
    private List<String> blacklistedSimpleMethods = new ArrayList<>();

    /**
     * APIs that are not Get/List/Describe APIs that have been verified that they are able
     * to be used through simple methods
     */
    private List<String> verifiedSimpleMethods = new ArrayList<>();

    /**
     * If a service isn't in the endpoints.json, the region that should be used for simple method integration tests.
     */
    private String defaultSimpleMethodTestRegion;

    private List<String> deprecatedOperations = new ArrayList<>();

    private List<String> deprecatedShapes = new ArrayList<>();

    private String sdkRequestBaseClassName;

    private String sdkResponseBaseClassName;

    private Map<String, String> modelMarshallerDefaultValueSupplier = new HashMap<>();

    private boolean useAutoConstructList = true;

    private boolean useAutoConstructMap = true;

    /**
     * Custom Retry Policy
     */
    private String customRetryPolicy;

    private boolean skipSyncClientGeneration;

    /**
     * Customization to attach the {@link PayloadTrait} to a member. Currently this is only used for
     * S3 which doesn't model a member as a payload trait even though it is.
     */
    private Map<String, String> attachPayloadTraitToMember = new HashMap<>();

    /**
     * Custom Response metadata
     */
    private Map<String, String> customResponseMetadata;

    /**
     * Custom protocol factory implementation. Currently this is only respected by the REST-XML protocol as only S3
     * needs a custom factory.
     */
    private String customProtocolFactoryFqcn;

    /**
     * Map of paginated operations that use custom class generation.
     * Key - c2j operation name
     * Value - indicates the type of pagination strategy to use
     */
    private Map<String, String> paginationCustomization;

    /**
     * Config to generate a utilities() in the low-level client
     */
    private UtilitiesMethod utilitiesMethod;

    private CustomizationConfig() {
    }

    public static CustomizationConfig create() {
        return new CustomizationConfig();
    }

    public Map<String, OperationModifier> getOperationModifiers() {
        return operationModifiers;
    }

    public void setOperationModifiers(Map<String, OperationModifier> operationModifiers) {
        this.operationModifiers = operationModifiers;
    }

    public Map<String, String> getRenameShapes() {
        return renameShapes;
    }

    public void setRenameShapes(Map<String, String> renameShapes) {
        this.renameShapes = renameShapes;
    }

    public Map<String, ShapeSubstitution> getShapeSubstitutions() {
        return shapeSubstitutions;
    }

    public void setShapeSubstitutions(Map<String, ShapeSubstitution> shapeSubstitutions) {
        this.shapeSubstitutions = shapeSubstitutions;
    }

    public Map<String, ShapeModifier> getShapeModifiers() {
        return shapeModifiers;
    }

    public void setShapeModifiers(Map<String, ShapeModifier> shapeModifiers) {
        this.shapeModifiers = shapeModifiers;
    }

    public String getServiceSpecificClientConfigClass() {
        return serviceSpecificClientConfigClass;
    }

    public void setServiceSpecificClientConfigClass(String serviceSpecificClientConfig) {
        this.serviceSpecificClientConfigClass = serviceSpecificClientConfig;
    }

    public List<ConvenienceTypeOverload> getConvenienceTypeOverloads() {
        return this.convenienceTypeOverloads;
    }

    public void setConvenienceTypeOverloads(List<ConvenienceTypeOverload> convenienceTypeOverloads) {
        this.convenienceTypeOverloads.addAll(convenienceTypeOverloads);
    }

    public MetadataConfig getCustomServiceMetadata() {
        return customServiceMetadata;
    }

    public void setCustomServiceMetadata(MetadataConfig metadataConfig) {
        this.customServiceMetadata = metadataConfig;
    }

    public String getCustomErrorCodeFieldName() {
        return customErrorCodeFieldName;
    }

    public void setCustomErrorCodeFieldName(String customErrorCodeFieldName) {
        this.customErrorCodeFieldName = customErrorCodeFieldName;
    }

    public String getSdkModeledExceptionBaseClassName() {
        return sdkModeledExceptionBaseClassName;
    }

    public void setSdkModeledExceptionBaseClassName(String sdkModeledExceptionBaseClassName) {
        this.sdkModeledExceptionBaseClassName = sdkModeledExceptionBaseClassName;
    }

    public boolean isCalculateCrc32FromCompressedData() {
        return calculateCrc32FromCompressedData;
    }

    public void setCalculateCrc32FromCompressedData(
        boolean calculateCrc32FromCompressedData) {
        this.calculateCrc32FromCompressedData = calculateCrc32FromCompressedData;
    }

    public boolean isExcludeClientCreateMethod() {
        return excludeClientCreateMethod;
    }

    public void setExcludeClientCreateMethod(boolean excludeClientCreateMethod) {
        this.excludeClientCreateMethod = excludeClientCreateMethod;
    }

    public ShareModelConfig getShareModelConfig() {
        return shareModelConfig;
    }

    public void setShareModelConfig(ShareModelConfig shareModelConfig) {
        this.shareModelConfig = shareModelConfig;
    }

    public String getServiceSpecificHttpConfig() {
        return serviceSpecificHttpConfig;
    }

    public void setServiceSpecificHttpConfig(String serviceSpecificHttpConfig) {
        this.serviceSpecificHttpConfig = serviceSpecificHttpConfig;
    }

    public List<String> getBlacklistedSimpleMethods() {
        return blacklistedSimpleMethods;
    }

    public void setBlacklistedSimpleMethods(List<String> blackListedSimpleMethods) {
        this.blacklistedSimpleMethods = blackListedSimpleMethods;
    }

    public List<String> getVerifiedSimpleMethods() {
        return verifiedSimpleMethods;
    }

    public void setVerifiedSimpleMethods(List<String> verifiedSimpleMethods) {
        this.verifiedSimpleMethods = verifiedSimpleMethods;
    }

    public String getDefaultSimpleMethodTestRegion() {
        return defaultSimpleMethodTestRegion;
    }

    public void setDefaultSimpleMethodTestRegion(String defaultSimpleMethodTestRegion) {
        this.defaultSimpleMethodTestRegion = defaultSimpleMethodTestRegion;
    }

    public List<String> getDeprecatedOperations() {
        return deprecatedOperations;
    }

    public void setDeprecatedOperations(List<String> deprecatedOperations) {
        this.deprecatedOperations = deprecatedOperations;
    }

    public List<String> getDeprecatedShapes() {
        return deprecatedShapes;
    }

    public void setDeprecatedShapes(List<String> deprecatedShapes) {
        this.deprecatedShapes = deprecatedShapes;
    }

    public String getSdkRequestBaseClassName() {
        return sdkRequestBaseClassName;
    }

    public void setSdkRequestBaseClassName(String sdkRequestBaseClassName) {
        this.sdkRequestBaseClassName = sdkRequestBaseClassName;
    }

    public String getSdkResponseBaseClassName() {
        return sdkResponseBaseClassName;
    }

    public void setSdkResponseBaseClassName(String sdkResponseBaseClassName) {
        this.sdkResponseBaseClassName = sdkResponseBaseClassName;
    }

    public Map<String, String> getModelMarshallerDefaultValueSupplier() {
        return modelMarshallerDefaultValueSupplier;
    }

    public void setModelMarshallerDefaultValueSupplier(Map<String, String> modelMarshallerDefaultValueSupplier) {
        this.modelMarshallerDefaultValueSupplier = modelMarshallerDefaultValueSupplier;
    }

    public boolean isUseAutoConstructList() {
        return useAutoConstructList;
    }

    public void setUseAutoConstructList(boolean useAutoConstructList) {
        this.useAutoConstructList = useAutoConstructList;
    }

    public boolean isUseAutoConstructMap() {
        return useAutoConstructMap;
    }

    public void setUseAutoConstructMap(boolean useAutoConstructMap) {
        this.useAutoConstructMap = useAutoConstructMap;
    }

    public String getCustomRetryPolicy() {
        return customRetryPolicy;
    }

    public void setCustomRetryPolicy(String customRetryPolicy) {
        this.customRetryPolicy = customRetryPolicy;
    }

    public boolean isSkipSyncClientGeneration() {
        return skipSyncClientGeneration;
    }

    public void setSkipSyncClientGeneration(boolean skipSyncClientGeneration) {
        this.skipSyncClientGeneration = skipSyncClientGeneration;
    }

    public Map<String, String> getAttachPayloadTraitToMember() {
        return attachPayloadTraitToMember;
    }

    public void setAttachPayloadTraitToMember(Map<String, String> attachPayloadTraitToMember) {
        this.attachPayloadTraitToMember = attachPayloadTraitToMember;
    }

    public Map<String, String> getCustomResponseMetadata() {
        return customResponseMetadata;
    }

    public void setCustomResponseMetadata(Map<String, String> customResponseMetadata) {
        this.customResponseMetadata = customResponseMetadata;
    }

    public String getCustomProtocolFactoryFqcn() {
        return customProtocolFactoryFqcn;
    }

    public void setCustomProtocolFactoryFqcn(String customProtocolFactoryFqcn) {
        this.customProtocolFactoryFqcn = customProtocolFactoryFqcn;
    }

    public Map<String, String> getPaginationCustomization() {
        return paginationCustomization;
    }

    public void setPaginationCustomization(Map<String, String> paginationCustomization) {
        this.paginationCustomization = paginationCustomization;
    }

    public UtilitiesMethod getUtilitiesMethod() {
        return utilitiesMethod;
    }

    public void setUtilitiesMethod(UtilitiesMethod utilitiesMethod) {
        this.utilitiesMethod = utilitiesMethod;
    }
}
