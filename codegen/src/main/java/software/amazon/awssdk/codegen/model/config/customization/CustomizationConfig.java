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

package software.amazon.awssdk.codegen.model.config.customization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.config.templates.CodeGenTemplatesConfig;

public class CustomizationConfig {

    public static final CustomizationConfig DEFAULT = new CustomizationConfig();
    /**
     * List of 'convenience' overloads to generate for model classes. Convenience overloads expose a
     * different type that is adapted to the real type
     */
    private final List<ConvenienceTypeOverload> convenienceTypeOverloads = new ArrayList<>();
    /**
     * Overrides the request-level service name that will be used for request metrics and service
     * exceptions. If not specified, the client will use the service interface name by default.
     *
     * Example: for backwards compatibility, this is set to "AmazonDynamoDBv2" for DynamoDB client.
     *
     * @see {@link software.amazon.awssdk.core.Request#getServiceName()}
     */
    private String customServiceNameForRequest;
    /**
     * True if the generated code should enable client-side validation on required input
     * parameters.
     */
    private boolean requiredParamValidationEnabled;
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
    private CodeGenTemplatesConfig customCodeTemplates;
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
     * Skips generating smoketests if set to true.
     */
    private boolean skipSmokeTests;

    /**
     * Exclude the create() method on a client. This is useful for global services that will need a global region configured to
     * work.
     */
    private boolean excludeClientCreateMethod = false;

    /**
     * A service name that this service client should share models with. The models and non-request marshallers will be generated
     * into the same directory as the provided service's models.
     */
    private String shareModelsWith;

    /**
     * Expression to return a service specific instance of {@link software.amazon.awssdk.http.SdkHttpConfigurationOption}. If
     * present, the client builder will override the hook to return service specific HTTP config and inject this expression into
     * that method. At some point we may want to have a more data driven way to declare these settings but right now we don't
     * have any requirements to necessitate that and referencing handwritten code is simpler. See SWF customization.config
     * for an example.
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

    private String sdkRequestBaseClassName;

    private String sdkResponseBaseClassName;
    private String defaultExceptionUnmarshaller;

    private Map<String, String> modelMarshallerDefaultValueSupplier;

    private boolean useAutoConstructList = true;

    /**
     * Custom Retry Policy
     */
    private String customRetryPolicy;

    private CustomizationConfig() {
    }

    public String getCustomServiceNameForRequest() {
        return customServiceNameForRequest;
    }

    public void setCustomServiceNameForRequest(String customServiceNameForRequest) {
        this.customServiceNameForRequest = customServiceNameForRequest;
    }

    public CodeGenTemplatesConfig getCustomCodeTemplates() {
        return customCodeTemplates;
    }

    public void setCustomCodeTemplates(CodeGenTemplatesConfig customCodeTemplates) {
        this.customCodeTemplates = customCodeTemplates;
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

    public boolean isRequiredParamValidationEnabled() {
        return requiredParamValidationEnabled;
    }

    public void setRequiredParamValidationEnabled(boolean requiredParamValidationEnabled) {
        this.requiredParamValidationEnabled = requiredParamValidationEnabled;
    }

    public String getServiceSpecificClientConfigClass() {
        return serviceSpecificClientConfigClass;
    }

    public void setServiceSpecificClientConfigClass(String serviceSpecificClientConfig) {
        this.serviceSpecificClientConfigClass = serviceSpecificClientConfig;
    }

    /**
     * Customization to generate a method overload for a member setter that takes a string rather
     * than an InputStream. Currently only used by Lambda
     */
    public void setStringOverloadForInputStreamMember(
        StringOverloadForInputStreamMember stringOverloadForInputStreamMember) {
        this.convenienceTypeOverloads
            .add(stringOverloadForInputStreamMember.getConvenienceTypeOverload());
    }

    /**
     * Customization to generate a method overload for a member setter that takes a string rather
     * than an ByteBuffer. Currently only used by Lambda
     */
    public void setStringOverloadForByteBufferMember(
        StringOverloadForByteBufferMember stringOverloadForByteBufferMember) {
        this.convenienceTypeOverloads
            .add(stringOverloadForByteBufferMember.getConvenienceTypeOverload());
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

    public boolean isSkipSmokeTests() {
        return skipSmokeTests;
    }

    public void setSkipSmokeTests(boolean skipSmokeTests) {
        this.skipSmokeTests = skipSmokeTests;
    }

    public boolean isExcludeClientCreateMethod() {
        return excludeClientCreateMethod;
    }

    public void setExcludeClientCreateMethod(boolean excludeClientCreateMethod) {
        this.excludeClientCreateMethod = excludeClientCreateMethod;
    }

    public String getShareModelsWith() {
        return shareModelsWith;
    }

    public void setShareModelsWith(String shareModelsWith) {
        this.shareModelsWith = shareModelsWith;
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

    public String getDefaultExceptionUnmarshaller() {
        return defaultExceptionUnmarshaller;
    }

    public void setDefaultExceptionUnmarshaller(String defaultExceptionUnmarshaller) {
        this.defaultExceptionUnmarshaller = defaultExceptionUnmarshaller;
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

    public String getCustomRetryPolicy() {
        return customRetryPolicy;
    }

    public void setCustomRetryPolicy(String customRetryPolicy) {
        this.customRetryPolicy = customRetryPolicy;

    }
}
