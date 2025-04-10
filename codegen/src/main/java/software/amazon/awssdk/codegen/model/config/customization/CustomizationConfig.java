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

package software.amazon.awssdk.codegen.model.config.customization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.model.service.CustomOperationContextParam;
import software.amazon.awssdk.codegen.model.service.PreClientExecutionRequestCustomizer;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * {@code service-2.json} models can be manually modified via defining properties in an associated {@code customization.config}
 * file. This class defines the Java bean representation that will be used to parse the JSON customization file. The bean can
 * then be later queried in the misc. codegen steps.
 */
public class CustomizationConfig {

    public enum LegacyEventGenerationMode {
        DISABLED,
        NO_ES_EVENT_IMPL, // old legacy
        TOP_LEVEL_ES_INTERFACE // new legacy
    }

    /**
     * List of 'convenience' overloads to generate for model classes. Convenience overloads expose a
     * different type that is adapted to the real type
     */
    private final List<ConvenienceTypeOverload> convenienceTypeOverloads = new ArrayList<>();

    /**
     * Configuration object for service-specific configuration options.
     */
    private ServiceConfig serviceConfig = new ServiceConfig();

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
    private CustomSdkShapes customSdkShapes;
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
    private List<String> excludedSimpleMethods = new ArrayList<>();

    /**
     * APIs that have no required arguments in their model but can't be called via a simple method.
     * Superseded by {@link #excludedSimpleMethods}
     */
    @Deprecated
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

    /**
     * Custom Retry Policy
     */
    private String customRetryPolicy;

    /**
     * Custom Retry strategy
     */
    private String customRetryStrategy;

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

    /**
     * Config to generate a additional Builder methods in the client interface.
     */
    private List<AdditionalBuilderMethod> additionalBuilderMethods;

    /**
     * Force generation of deprecated client builder method 'enableEndpointDiscovery'. Only services that already had
     * this method when it was deprecated require this flag to be set.
     */
    private boolean enableEndpointDiscoveryMethodRequired = false;

    /**
     * Arnable fields used in s3 control
     */
    private Map<String, S3ArnableFieldConfig> s3ArnableFields;

    /**
     * Allow a customer to set an endpoint override AND bypass endpoint discovery on their client even when endpoint discovery
     * enabled is true and endpoint discovery is required for an operation. This customization should almost never be "true"
     * because it creates a confusing customer experience.
     */
    private boolean allowEndpointOverrideForEndpointDiscoveryRequiredOperations = false;

    /**
     * Customization to instruct the code generator to use the legacy model generation scheme for the given events.
     * <p>
     * <b>NOTE</b>This customization is primarily here to preserve backwards compatibility with existing code before the
     * generation scheme for the visitor methods was changed. There should be no good reason to use this customization
     * for any other purpose.
     */
    @JsonDeserialize(using = UseLegacyEventSchemeDeserializer.class)
    private Map<String, LegacyEventGenerationMode> useLegacyEventGenerationScheme = new HashMap<>();

    /**
     * How the code generator should behave when it encounters shapes with underscores in the name.
     */
    private UnderscoresInNameBehavior underscoresInNameBehavior;

    private String userAgent;

    private RetryMode defaultRetryMode;

    /**
     * Whether to generate an abstract decorator class that delegates to the async service client
     */
    private boolean delegateAsyncClientClass;

    /**
     * Whether to generate an abstract decorator class that delegates to the sync service client
     */
    private boolean delegateSyncClientClass;

    /**
     * Fully qualified name of a class that given the default sync client instance can return the final client instance,
     * for instance by decorating the client with specific-purpose implementations of the client interface.
     * See S3 customization.config for an example.
     */
    private String syncClientDecorator;

    /**
     * Fully qualified name of a class that given the default async client instance can return the final client instance,
     * for instance by decorating the client with specific-purpose implementations of the client interface.
     * See S3 customization.config for an example.
     */
    private String asyncClientDecorator;

    /**
     * Only for s3. A set of customization to related to multipart operations.
     */
    private MultipartCustomization multipartCustomization;

    /**
     * Whether to skip generating endpoint tests from endpoint-tests.json
     */
    private boolean skipEndpointTestGeneration;

    /**
     * Whether to generate client-level endpoint tests; overrides test case criteria such as operation inputs.
     */
    private boolean generateEndpointClientTests;

    /**
     * Whether to use prior knowledge protocol negotiation for H2
     */
    private boolean usePriorKnowledgeForH2;

    /**
     * A mapping from the skipped test's description to the reason why it's being skipped.
     */
    private Map<String, String> skipEndpointTests;

    private boolean useGlobalEndpoint;

    private boolean useS3ExpressSessionAuth;

    private List<String> interceptors = new ArrayList<>();

    private List<String> internalPlugins = new ArrayList<>();

    /**
     * Whether marshallers perform validations against members marked with RequiredTrait.
     */
    private boolean requiredTraitValidationEnabled = false;

    /**
     * Whether SRA based auth logic should be used.
     */
    private boolean useSraAuth = true;

    /**
     * Whether to generate auth scheme params based on endpoint params.
     */
    private boolean enableEndpointAuthSchemeParams = false;

    /**
     * List of endpoint params to be used for the auth scheme params
     */
    private List<String> allowedEndpointAuthSchemeParams = Collections.emptyList();

    /**
     * Whether the list of allowed endpoint auth scheme params was explicitly configured.
     */
    private boolean allowedEndpointAuthSchemeParamsConfigured = false;

    /**
     * Customization to attach map of Custom client param configs that can be set on a client builder.
     */
    private Map<String, ClientContextParam> customClientContextParams;

    private boolean s3ExpressAuthSupport;

    /**
     * Set to true to enable compiled endpoint rules. Currently defaults to false.
     */
    private boolean enableGenerateCompiledEndpointRules = false;

    /**
     * Customization related to auth scheme derived from endpoints.
     */
    private EndpointAuthSchemeConfig endpointAuthSchemeConfig;

    /**
     * Customization to change the root package name.
     * By default, it's "software.amazon.awssdk.services.[serviceId]"
     */
    private String rootPackageName;

    /**
     * Special case for a service where model changes for endpoint params were not updated .
     * This should be removed once the service updates its models
     */
    private Map<String, ParameterModel> endpointParameters;

    private List<CustomOperationContextParam> customOperationContextParams;

    /**
     * A map that associates API names with their respective custom request transformers.
     * The {@link PreClientExecutionRequestCustomizer} allows for dynamic and specific handling of API requests,
     * ensuring that each request that requires custom handling can be appropriately transformed based on its corresponding
     * API name.
     */
    private Map<String, PreClientExecutionRequestCustomizer> preClientExecutionRequestCustomizer;

    /**
     * A boolean flag to indicate if Automatic Batch Request is supported.
     */
    private boolean batchManagerSupported;

    /**
     * A boolean flag to indicate if the fast unmarshaller code path is enabled.
     */
    private boolean enableFastUnmarshaller;

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

    public CustomSdkShapes getCustomSdkShapes() {
        return customSdkShapes;
    }

    public void setCustomSdkShapes(CustomSdkShapes customSdkShapes) {
        this.customSdkShapes = customSdkShapes;
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

    public List<String> getExcludedSimpleMethods() {
        return excludedSimpleMethods;
    }

    public void setExcludedSimpleMethods(List<String> excludedSimpleMethods) {
        this.excludedSimpleMethods = excludedSimpleMethods;
    }

    /**
     * Use {@link #getExcludedSimpleMethods()}
     */
    @Deprecated
    public List<String> getBlacklistedSimpleMethods() {
        return blacklistedSimpleMethods;
    }

    /**
     * Use {@link #setExcludedSimpleMethods(List)}
     */
    @Deprecated
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

    public String getCustomRetryPolicy() {
        return customRetryPolicy;
    }

    public String getCustomRetryStrategy() {
        return customRetryStrategy;
    }

    public void setCustomRetryPolicy(String customRetryPolicy) {
        this.customRetryPolicy = customRetryPolicy;
    }

    public void setCustomRetryStrategy(String customRetryStrategy) {
        this.customRetryStrategy = customRetryStrategy;
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


    public List<AdditionalBuilderMethod> getAdditionalBuilderMethods() {
        return additionalBuilderMethods;
    }

    public void setAdditionalBuilderMethods(List<AdditionalBuilderMethod> additionalBuilderMethods) {
        this.additionalBuilderMethods = additionalBuilderMethods;
    }

    public boolean isEnableEndpointDiscoveryMethodRequired() {
        return enableEndpointDiscoveryMethodRequired;
    }

    public void setEnableEndpointDiscoveryMethodRequired(boolean enableEndpointDiscoveryMethodRequired) {
        this.enableEndpointDiscoveryMethodRequired = enableEndpointDiscoveryMethodRequired;
    }

    public Map<String, S3ArnableFieldConfig> getS3ArnableFields() {
        return s3ArnableFields;
    }

    public CustomizationConfig withS3ArnableFields(Map<String, S3ArnableFieldConfig> s3ArnableFields) {
        this.s3ArnableFields = s3ArnableFields;
        return this;
    }

    public void setS3ArnableFields(Map<String, S3ArnableFieldConfig> s3ArnableFields) {
        this.s3ArnableFields = s3ArnableFields;
    }

    public boolean allowEndpointOverrideForEndpointDiscoveryRequiredOperations() {
        return allowEndpointOverrideForEndpointDiscoveryRequiredOperations;
    }

    public void setAllowEndpointOverrideForEndpointDiscoveryRequiredOperations(
        boolean allowEndpointOverrideForEndpointDiscoveryRequiredOperations) {
        this.allowEndpointOverrideForEndpointDiscoveryRequiredOperations =
            allowEndpointOverrideForEndpointDiscoveryRequiredOperations;
    }

    public Map<String, LegacyEventGenerationMode> getUseLegacyEventGenerationScheme() {
        return useLegacyEventGenerationScheme;
    }

    public void setUseLegacyEventGenerationScheme(Map<String, LegacyEventGenerationMode> useLegacyEventGenerationScheme) {
        this.useLegacyEventGenerationScheme = useLegacyEventGenerationScheme;
    }

    public UnderscoresInNameBehavior getUnderscoresInNameBehavior() {
        return underscoresInNameBehavior;
    }

    public void setUnderscoresInNameBehavior(UnderscoresInNameBehavior behavior) {
        this.underscoresInNameBehavior = behavior;
    }

    public CustomizationConfig withUnderscoresInShapeNameBehavior(UnderscoresInNameBehavior behavior) {
        this.underscoresInNameBehavior = behavior;
        return this;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public CustomizationConfig withUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public RetryMode getDefaultRetryMode() {
        return defaultRetryMode;
    }

    public void setDefaultRetryMode(RetryMode defaultRetryMode) {
        this.defaultRetryMode = defaultRetryMode;
    }

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public boolean isDelegateAsyncClientClass() {
        return delegateAsyncClientClass;
    }

    public void setDelegateAsyncClientClass(boolean delegateAsyncClientClass) {
        this.delegateAsyncClientClass = delegateAsyncClientClass;
    }

    public String getSyncClientDecorator() {
        return syncClientDecorator;
    }

    public void setSyncClientDecorator(String syncClientDecorator) {
        this.syncClientDecorator = syncClientDecorator;
    }

    public String getAsyncClientDecorator() {
        return asyncClientDecorator;
    }

    public void setAsyncClientDecorator(String asyncClientDecorator) {
        this.asyncClientDecorator = asyncClientDecorator;
    }

    public boolean isDelegateSyncClientClass() {
        return delegateSyncClientClass;
    }

    public void setDelegateSyncClientClass(boolean delegateSyncClientClass) {
        this.delegateSyncClientClass = delegateSyncClientClass;
    }

    public boolean isSkipEndpointTestGeneration() {
        return skipEndpointTestGeneration;
    }

    public void setSkipEndpointTestGeneration(boolean skipEndpointTestGeneration) {
        this.skipEndpointTestGeneration = skipEndpointTestGeneration;
    }

    public boolean isGenerateEndpointClientTests() {
        return generateEndpointClientTests;
    }

    public void setGenerateEndpointClientTests(boolean generateEndpointClientTests) {
        this.generateEndpointClientTests = generateEndpointClientTests;
    }

    public boolean isUsePriorKnowledgeForH2() {
        return usePriorKnowledgeForH2;
    }

    public void setUsePriorKnowledgeForH2(boolean usePriorKnowledgeForH2) {
        this.usePriorKnowledgeForH2 = usePriorKnowledgeForH2;
    }

    public boolean useGlobalEndpoint() {
        return useGlobalEndpoint;
    }

    public void setUseGlobalEndpoint(boolean useGlobalEndpoint) {
        this.useGlobalEndpoint = useGlobalEndpoint;
    }

    public boolean useS3ExpressSessionAuth() {
        return useS3ExpressSessionAuth;
    }

    public void setUseS3ExpressSessionAuth(boolean useS3ExpressSessionAuth) {
        this.useS3ExpressSessionAuth = useS3ExpressSessionAuth;
    }

    public boolean isEnableGenerateCompiledEndpointRules() {
        return enableGenerateCompiledEndpointRules;
    }

    public void setEnableGenerateCompiledEndpointRules(boolean enableGenerateCompiledEndpointRules) {
        this.enableGenerateCompiledEndpointRules = enableGenerateCompiledEndpointRules;
    }

    public Map<String, String> getSkipEndpointTests() {
        return skipEndpointTests;
    }

    public void setSkipEndpointTests(Map<String, String> skipEndpointTests) {
        this.skipEndpointTests = skipEndpointTests;
    }

    public List<String> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<String> interceptors) {
        this.interceptors = interceptors;
    }

    public List<String> getInternalPlugins() {
        return internalPlugins;
    }

    public void setInternalPlugins(List<String> internalPlugins) {
        this.internalPlugins = internalPlugins;
    }

    public boolean isRequiredTraitValidationEnabled() {
        return requiredTraitValidationEnabled;
    }

    public void setRequiredTraitValidationEnabled(boolean requiredTraitValidationEnabled) {
        this.requiredTraitValidationEnabled = requiredTraitValidationEnabled;
    }

    public void setUseSraAuth(boolean useSraAuth) {
        this.useSraAuth = useSraAuth;
    }

    // TODO(post-sra-identity-auth): Remove this customization and all related switching logic, keeping only the
    //  useSraAuth==true branch going forward.
    public boolean useSraAuth() {
        return useSraAuth;
    }

    public void setEnableEndpointAuthSchemeParams(boolean enableEndpointAuthSchemeParams) {
        this.enableEndpointAuthSchemeParams = enableEndpointAuthSchemeParams;
    }

    public boolean isEnableEndpointAuthSchemeParams() {
        return enableEndpointAuthSchemeParams;
    }

    public void setAllowedEndpointAuthSchemeParams(List<String> allowedEndpointAuthSchemeParams) {
        this.allowedEndpointAuthSchemeParamsConfigured = true;
        this.allowedEndpointAuthSchemeParams = allowedEndpointAuthSchemeParams;
    }

    public List<String> getAllowedEndpointAuthSchemeParams() {
        return this.allowedEndpointAuthSchemeParams;
    }

    public boolean getAllowedEndpointAuthSchemeParamsConfigured() {
        return allowedEndpointAuthSchemeParamsConfigured;
    }

    public Map<String, ClientContextParam> getCustomClientContextParams() {
        return customClientContextParams;
    }

    public void setCustomClientContextParams(Map<String, ClientContextParam> customClientContextParams) {
        this.customClientContextParams = customClientContextParams;
    }

    public boolean getS3ExpressAuthSupport() {
        return s3ExpressAuthSupport;
    }

    public void setS3ExpressAuthSupport(boolean s3ExpressAuthSupport) {
        this.s3ExpressAuthSupport = s3ExpressAuthSupport;
    }

    public MultipartCustomization getMultipartCustomization() {
        return this.multipartCustomization;
    }

    public void setMultipartCustomization(MultipartCustomization multipartCustomization) {
        this.multipartCustomization = multipartCustomization;
    }

    public EndpointAuthSchemeConfig getEndpointAuthSchemeConfig() {
        return endpointAuthSchemeConfig;
    }

    public void setEndpointAuthSchemeConfig(EndpointAuthSchemeConfig endpointAuthSchemeConfig) {
        this.endpointAuthSchemeConfig = endpointAuthSchemeConfig;
    }

    public String getRootPackageName() {
        return rootPackageName;
    }

    public void setRootPackageName(String rootPackageName) {
        this.rootPackageName = rootPackageName;
    }

    public CustomizationConfig withRootPackageName(String packageName) {
        this.rootPackageName = packageName;
        return this;
    }

    public Map<String, ParameterModel> getEndpointParameters() {
        return endpointParameters;
    }

    public void setEndpointParameters(Map<String, ParameterModel> endpointParameters) {
        this.endpointParameters = endpointParameters;
    }

    public List<CustomOperationContextParam> getCustomOperationContextParams() {
        return customOperationContextParams;
    }

    public void setCustomOperationContextParams(List<CustomOperationContextParam> customOperationContextParams) {
        this.customOperationContextParams = customOperationContextParams;
    }

    public Map<String, PreClientExecutionRequestCustomizer> getPreClientExecutionRequestCustomizer() {
        return preClientExecutionRequestCustomizer;
    }

    public void setPreClientExecutionRequestCustomizer(Map<String, PreClientExecutionRequestCustomizer>
                                                           preClientExecutionRequestCustomizer) {
        this.preClientExecutionRequestCustomizer = preClientExecutionRequestCustomizer;
    }

    public boolean getBatchManagerSupported() {
        return batchManagerSupported;
    }

    public void setBatchManagerSupported(boolean batchManagerSupported) {
        this.batchManagerSupported = batchManagerSupported;
    }

    public boolean getEnableFastUnmarshaller() {
        return enableFastUnmarshaller;
    }

    public void setEnableFastUnmarshaller(boolean enableFastUnmarshaller) {
        this.enableFastUnmarshaller = enableFastUnmarshaller;
    }
}
