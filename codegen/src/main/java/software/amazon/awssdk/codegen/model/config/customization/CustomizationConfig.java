/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Set;
import software.amazon.awssdk.codegen.model.config.ConstructorFormsWrapper;
import software.amazon.awssdk.codegen.model.config.templates.CodeGenTemplatesConfig;

public class CustomizationConfig {

    public static final CustomizationConfig DEFAULT = new CustomizationConfig();
    /**
     * List of 'convenience' overloads to generate for model classes. Convenience overloads expose a
     * different type that is adapted to the real type
     */
    private final List<ConvenienceTypeOverload> convenienceTypeOverloads = new ArrayList<ConvenienceTypeOverload>();
    /**
     * The fully-qualified class name of the custom metric types to be collected by the client.
     *
     * Example: "software.amazon.awssdk.services.dynamodbv2.metrics.DynamoDBRequestMetric"
     */
    private String requestMetrics;
    /**
     * True if auto-construct list is in use; false otherwise.
     */
    private boolean useAutoConstructList;
    /**
     * True if auto-construct map is in use; false otherwise.
     */
    private boolean useAutoConstructMap;
    /**
     * True if we want to apply the ServiceClientHolderInputStream wrapper to all the stream
     * response returned by the client; the purpose is to prevent the client being GCed before the
     * response data is fully consumed.
     */
    private boolean serviceClientHoldInputStream;
    /**
     * The name of the operations where the LengthCheckInputStream wrapper should be applied to the
     * response stream.
     */
    private List<String> operationsWithResponseStreamContentLengthValidation;
    /**
     * If specified the name of the custom exception unmarshaller (e.g. 'LegacyErrorUnmarshaller'
     * for SimpleDB). If not set then the default unmarshaller of the protocol will be used (e.g.
     * StandardErrorUnmarshaller for aws-query and rest-xml). Currently the exception unmarshaller
     * for JSON protocols is not customizable.
     */
    private String customExceptionUnmarshallerImpl;
    /**
     * The name of the custom class returned by the client method getCacheResponseMetadata.
     * Currently it's only set for SimpleDB ("SimpleDBResponseMetadata")
     */
    private String customResponseMetadataClassName;
    /**
     * True if the generated interface should NOT include shutdown() and getCachedResponseData
     * methods. Currently it's only set true for SimpleDB.
     */
    private boolean skipInterfaceAdditions;
    /**
     * Overrides the request-level service name that will be used for request metrics and service
     * exceptions. If not specified, the client will use the service interface name by default.
     *
     * Example: for backwards compatibility, this is set to "AmazonDynamoDBv2" for DynamoDB client.
     *
     * @see {@link software.amazon.awssdk.Request#getServiceName()}
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
     * Specifies the name of the endpoint builder class to use if a service
     * has a specific endpoint builder class. Null if the service does not have
     * a specific endpoint builder.
     */
    private String serviceSpecificEndpointBuilderClass;
    /**
     * Specify additional constructor forms for a given model class.
     */
    private Map<String, ConstructorFormsWrapper> additionalShapeConstructors;
    /**
     * Specify simplified method forms for a given operation API.
     */
    private Map<String, SimpleMethodFormsWrapper> simpleMethods;
    /**
     * Specify shapes to be renamed.
     */
    private Map<String, String> renameShapes;
    /**
     * Specify List member shapes to send empty String when the List is auto-constructed in query
     * protocol. This customization will only affect marshaling when autoConstructList is true.
     * Currently, it's only set in ElasticLoadBalancing service.
     */
    private Map<String, List<String>> sendEmptyAutoConstructedListAsEmptyList;
    /**
     * Marshalls empty lists on the wire. This customization does not send empty lists created by
     * the autoconstruct customization and is only applicable to AWS Query services.
     */
    private boolean sendExplicitlyEmptyListsForQuery;
    /**
     * Configuration for generating policy action enums.
     */
    private AuthPolicyActions authPolicyActions;
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
     * Customization to use the actual shape name of output shapes (as defined in the service model)
     * to name the corresponding Java class. Normally we derive a new name using the operation name
     * (i.e. PutFooResult). This is currently only exercised by SWF and mainly to preserve backwards
     * compatibility due to a bug in the previous code generator. This is similar to the 'wrapper'
     * trait in the normalized model but unlike for Query services, this customization has no affect
     * on how the shape is represented on the wire.
     */
    private boolean useModeledOutputShapeNames;
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
     * Uses the specified SignerProvider implementation for this client.
     */
    private String customSignerProvider;
    /**
     * Service calculates CRC32 checksum from compressed file when Accept-Encoding: gzip header is provided.
     */
    private boolean calculateCrc32FromCompressedData;
    /**
     * Custom file header for all generated Java classes. If not specified uses default Amazon
     * license header.
     */
    private String customFileHeader;
    /**
     * Skips generating smoketests if set to true.
     */
    private boolean skipSmokeTests;

    /**
     * Fully qualified class name of presigner extension class if it exists.
     */
    private String presignersFqcn;

    /**
     * A set of deprecated code that generation can be suppressed for
     */
    private Set<DeprecatedSuppression> deprecatedSuppressions;

    /**
     * A service name that this service client should share models with. The models and non-request marshallers will be generated
     * into the same directory as the provided service's models.
     */
    private String shareModelsWith;

    /**
     * Expression to return a service specific instance of {@link software.amazon.awssdk.http.SdkHttpConfigurationOptions}. If
     * present, the client builder will override the hook to return service specific HTTP config and inject this expression into
     * that method. At some point we may want to have a more data driven way to declare these settings but right now we don't
     * have any requirements to necessitate that and referencing handwritten code is simpler. See SWF customization.config
     * for an example.
     */
    private String serviceSpecificHttpConfig;

    private CustomizationConfig() {
    }

    public String getRequestMetrics() {
        return requestMetrics;
    }

    public void setRequestMetrics(String requestMetrics) {
        this.requestMetrics = requestMetrics;
    }

    public boolean isServiceClientHoldInputStream() {
        return serviceClientHoldInputStream;
    }

    public void setServiceClientHoldInputStream(boolean serviceClientHoldInputStream) {
        this.serviceClientHoldInputStream = serviceClientHoldInputStream;
    }

    public List<String> getOperationsWithResponseStreamContentLengthValidation() {
        return operationsWithResponseStreamContentLengthValidation;
    }

    public void setOperationsWithResponseStreamContentLengthValidation(
            List<String> operationsWithResponseStreamContentLengthValidation) {
        this.operationsWithResponseStreamContentLengthValidation = operationsWithResponseStreamContentLengthValidation;
    }

    public String getCustomExceptionUnmarshallerImpl() {
        return customExceptionUnmarshallerImpl;
    }

    public void setCustomExceptionUnmarshallerImpl(String customExceptionUnmarshallerImpl) {
        this.customExceptionUnmarshallerImpl = customExceptionUnmarshallerImpl;
    }

    public String getCustomResponseMetadataClassName() {
        return customResponseMetadataClassName;
    }

    public void setCustomResponseMetadataClassName(String customResponseMetadataClassName) {
        this.customResponseMetadataClassName = customResponseMetadataClassName;
    }

    public boolean isSkipInterfaceAdditions() {
        return skipInterfaceAdditions;
    }

    public void setSkipInterfaceAdditions(boolean skipInterfaceAdditions) {
        this.skipInterfaceAdditions = skipInterfaceAdditions;
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

    public Map<String, ConstructorFormsWrapper> getAdditionalShapeConstructors() {
        return additionalShapeConstructors;
    }

    public void setAdditionalShapeConstructors(
            Map<String, ConstructorFormsWrapper> additionalConstructors) {
        this.additionalShapeConstructors = additionalConstructors;
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

    public Map<String, List<String>> getSendEmptyAutoConstructedListAsEmptyList() {
        return sendEmptyAutoConstructedListAsEmptyList;
    }

    public void setSendEmptyAutoConstructedListAsEmptyList(
            Map<String, List<String>> sendEmptyAutoConstructedListAsEmptyList) {
        this.sendEmptyAutoConstructedListAsEmptyList = sendEmptyAutoConstructedListAsEmptyList;
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

    public Map<String, SimpleMethodFormsWrapper> getSimpleMethods() {
        return simpleMethods;
    }

    public void setSimpleMethods(Map<String, SimpleMethodFormsWrapper> simpleMethods) {
        this.simpleMethods = simpleMethods;
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

    public AuthPolicyActions getAuthPolicyActions() {
        return authPolicyActions;
    }

    public void setAuthPolicyActions(AuthPolicyActions policyActions) {
        this.authPolicyActions = policyActions;
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

    public String getServiceSpecificEndpointBuilderClass() {
        return serviceSpecificEndpointBuilderClass;
    }

    public void setServiceSpecificEndpointBuilderClass(String serviceSpecificEndpointBuilderClass) {
        this.serviceSpecificEndpointBuilderClass = serviceSpecificEndpointBuilderClass;
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

    public boolean useModeledOutputShapeNames() {
        return useModeledOutputShapeNames;
    }

    public void setUseModeledOutputShapeNames(boolean useModeledOutputShapeNames) {
        this.useModeledOutputShapeNames = useModeledOutputShapeNames;
    }

    public String getSdkModeledExceptionBaseClassName() {
        return sdkModeledExceptionBaseClassName;
    }

    public void setSdkModeledExceptionBaseClassName(String sdkModeledExceptionBaseClassName) {
        this.sdkModeledExceptionBaseClassName = sdkModeledExceptionBaseClassName;
    }

    public String getCustomSignerProvider() {
        return customSignerProvider;
    }

    public void setCustomSignerProvider(String customSignerProvider) {
        this.customSignerProvider = customSignerProvider;
    }

    public boolean isCalculateCrc32FromCompressedData() {
        return calculateCrc32FromCompressedData;
    }

    public void setCalculateCrc32FromCompressedData(
            boolean calculateCrc32FromCompressedData) {
        this.calculateCrc32FromCompressedData = calculateCrc32FromCompressedData;
    }

    public String getCustomFileHeader() {
        return customFileHeader;
    }

    public void setCustomFileHeader(String customFileHeader) {
        this.customFileHeader = customFileHeader;
    }

    public boolean isSkipSmokeTests() {
        return skipSmokeTests;
    }

    public void setSkipSmokeTests(boolean skipSmokeTests) {
        this.skipSmokeTests = skipSmokeTests;
    }

    public boolean isSendExplicitlyEmptyListsForQuery() {
        return sendExplicitlyEmptyListsForQuery;
    }

    public void setSendExplicitlyEmptyListsForQuery(boolean sendExplicitlyEmptyListsForQuery) {
        this.sendExplicitlyEmptyListsForQuery = sendExplicitlyEmptyListsForQuery;
    }

    public String getPresignersFqcn() {
        return presignersFqcn;
    }

    public void setPresignersFqcn(String presignersFqcn) {
        this.presignersFqcn = presignersFqcn;
    }

    public Set<DeprecatedSuppression> getDeprecatedSuppressions() {
        return deprecatedSuppressions;
    }

    public void setDeprecatedSuppressions(Set<DeprecatedSuppression> deprecatedSuppressions) {
        this.deprecatedSuppressions = deprecatedSuppressions;
    }

    public boolean emitClientMutationMethods() {
        return !shouldSuppress(DeprecatedSuppression.ClientMutationMethods);
    }

    public boolean emitClientConstructors() {
        return !shouldSuppress(DeprecatedSuppression.ClientConstructors);
    }

    private boolean shouldSuppress(DeprecatedSuppression suppression) {
        return deprecatedSuppressions != null && deprecatedSuppressions.contains(suppression);
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
}
