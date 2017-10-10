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

package software.amazon.awssdk.codegen.model.intermediate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.protocol.ProtocolMetadataProvider;
import software.amazon.awssdk.utils.StringUtils;

public class Metadata {

    private String apiVersion;

    private Protocol protocol;

    private ProtocolMetadataProvider protocolMetadataProvider;

    // TODO Not sure if this is needed.Remove if not needed.
    private String checksumFormat;

    private String documentation;

    private String defaultEndpoint;

    private String defaultRegion;

    private String defaultEndpointWithoutHttpProtocol;

    private String syncInterface;

    private String syncClient;

    private String syncBuilderInterface;

    private String syncBuilder;

    private String asyncInterface;

    private String asyncClient;

    private String asyncBuilderInterface;

    private String asyncBuilder;

    private String baseBuilderInterface;

    private String baseBuilder;

    private String rootPackageName;

    private String clientPackageName;

    private String modelPackageName;

    private String transformPackageName;

    private String requestTransformPackageName;

    private String paginatorsPackageName;

    private String authPolicyPackageName;

    private String smokeTestsPackageName;

    private String serviceAbbreviation;

    private String serviceFullName;

    private String baseExceptionName;

    private boolean hasApiWithStreamInput;

    private String contentType;

    private String jsonVersion;

    private String endpointPrefix;

    private String signingName;

    private boolean requiresIamSigners;

    private boolean requiresApiKey;

    private String uid;

    private AuthType authType;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Metadata withApiVersion(String apiVersion) {
        setApiVersion(apiVersion);
        return this;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
        this.protocolMetadataProvider = protocol.getProvider();
    }

    public Metadata withProtocol(Protocol protocol) {
        setProtocol(protocol);
        return this;
    }

    /**
     * @return The default implementation of exception unmarshallers to use when no custom one is
     *     provided through {@link software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig}
     */
    public String getProtocolDefaultExceptionUmarshallerImpl() {
        return protocolMetadataProvider.getExceptionUnmarshallerImpl();
    }

    public String getChecksumFormat() {
        return checksumFormat;
    }

    public void setChecksumFormat(String checksumFormat) {
        this.checksumFormat = checksumFormat;
    }

    public Metadata withChecksumFormat(String checksumFormat) {
        setChecksumFormat(checksumFormat);
        return this;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public Metadata withDocumentation(String documentation) {
        setDocumentation(documentation);
        return this;
    }

    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }

    public void setDefaultEndpoint(String defaultEndpoint) {
        this.defaultEndpoint = defaultEndpoint;
    }

    public Metadata withDefaultEndpoint(String defaultEndpoint) {
        setDefaultEndpoint(defaultEndpoint);
        return this;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    public Metadata withDefaultRegion(String defaultRegion) {
        setDefaultRegion(defaultRegion);
        return this;
    }

    public String getDefaultEndpointWithoutHttpProtocol() {
        return defaultEndpointWithoutHttpProtocol;
    }

    public void setDefaultEndpointWithoutHttpProtocol(
            String defaultEndpointWithoutHttpProtocol) {
        this.defaultEndpointWithoutHttpProtocol = defaultEndpointWithoutHttpProtocol;
    }

    public Metadata withDefaultEndpointWithoutHttpProtocol(
            String defaultEndpointWithoutHttpProtocol) {
        setDefaultEndpointWithoutHttpProtocol(defaultEndpointWithoutHttpProtocol);
        return this;
    }

    public String getSyncInterface() {
        return syncInterface;
    }

    public void setSyncInterface(String syncInterface) {
        this.syncInterface = syncInterface;
    }

    public Metadata withSyncInterface(String syncInterface) {
        setSyncInterface(syncInterface);
        return this;
    }

    @JsonIgnore
    public String getSyncAbstractClass() {
        return syncInterface == null ? null : "Abstract" + syncInterface;
    }

    public String getSyncClient() {
        return syncClient;
    }

    public void setSyncClient(String syncClient) {
        this.syncClient = syncClient;
    }

    public Metadata withSyncClient(String syncClient) {
        setSyncClient(syncClient);
        return this;
    }

    public String getSyncBuilderInterface() {
        return syncBuilderInterface;
    }

    public void setSyncBuilderInterface(String syncBuilderInterface) {
        this.syncBuilderInterface = syncBuilderInterface;
    }

    public Metadata withSyncBuilderInterface(String syncBuilderInterface) {
        this.syncBuilderInterface = syncBuilderInterface;
        return this;
    }

    public String getSyncBuilder() {
        return syncBuilder;
    }

    public void setSyncBuilder(String syncBuilder) {
        this.syncBuilder = syncBuilder;
    }

    public Metadata withSyncBuilder(String syncBuilder) {
        this.syncBuilder = syncBuilder;
        return this;
    }

    public String getAsyncInterface() {
        return asyncInterface;
    }

    public void setAsyncInterface(String asyncInterface) {
        this.asyncInterface = asyncInterface;
    }

    public Metadata withAsyncInterface(String asyncInterface) {
        setAsyncInterface(asyncInterface);
        return this;
    }

    @JsonIgnore
    public String getAsyncAbstractClass() {
        return asyncInterface == null ? null : "Abstract" + asyncInterface;
    }

    public String getAsyncClient() {
        return asyncClient;
    }

    public void setAsyncClient(String asyncClient) {
        this.asyncClient = asyncClient;
    }

    public Metadata withAsyncClient(String asyncClient) {
        setAsyncClient(asyncClient);
        return this;
    }

    public String getAsyncBuilderInterface() {
        return asyncBuilderInterface;
    }

    public void setAsyncBuilderInterface(String asyncBuilderInterface) {
        this.asyncBuilderInterface = asyncBuilderInterface;
    }

    public Metadata withAsyncBuilderInterface(String asyncBuilderInterface) {
        this.asyncBuilderInterface = asyncBuilderInterface;
        return this;
    }

    public String getBaseBuilderInterface() {
        return baseBuilderInterface;
    }

    public void setBaseBuilderInterface(String baseBuilderInterface) {
        this.baseBuilderInterface = baseBuilderInterface;
    }

    public Metadata withBaseBuilderInterface(String baseBuilderInterface) {
        this.baseBuilderInterface = baseBuilderInterface;
        return this;
    }

    public String getBaseBuilder() {
        return baseBuilder;
    }

    public void setBaseBuilder(String baseBuilder) {
        this.baseBuilder = baseBuilder;
    }

    public Metadata withBaseBuilder(String baseBuilder) {
        this.baseBuilder = baseBuilder;
        return this;
    }

    public String getAsyncBuilder() {
        return asyncBuilder;
    }

    public void setAsyncBuilder(String asyncBuilder) {
        this.asyncBuilder = asyncBuilder;
    }

    public Metadata withAsyncBuilder(String asyncBuilder) {
        this.asyncBuilder = asyncBuilder;
        return this;
    }

    public String getBaseExceptionName() {
        return baseExceptionName;
    }

    public void setBaseExceptionName(String baseExceptionName) {
        this.baseExceptionName = baseExceptionName;
    }

    public Metadata withBaseExceptionName(String baseExceptionName) {
        setBaseExceptionName(baseExceptionName);
        return this;
    }

    public String getRootPackageName() {
        return rootPackageName;
    }

    public void setRootPackageName(String rootPackageName) {
        this.rootPackageName = rootPackageName;
    }

    public Metadata withRootPackageName(String rootPackageName) {
        setRootPackageName(rootPackageName);
        return this;
    }

    public String getFullClientPackageName() {
        return joinPackageNames(rootPackageName, getClientPackageName());
    }

    public String getClientPackageName() {
        return clientPackageName;
    }

    public void setClientPackageName(String clientPackageName) {
        this.clientPackageName = clientPackageName;
    }

    public Metadata withClientPackageName(String clientPackageName) {
        setClientPackageName(clientPackageName);
        return this;
    }

    public String getFullModelPackageName() {
        return joinPackageNames(rootPackageName, getModelPackageName());
    }

    public String getModelPackageName() {
        return modelPackageName;
    }

    public void setModelPackageName(String modelPackageName) {
        this.modelPackageName = modelPackageName;
    }

    public Metadata withModelPackageName(String modelPackageName) {
        setModelPackageName(modelPackageName);
        return this;
    }

    public String getFullTransformPackageName() {
        return joinPackageNames(rootPackageName, getTransformPackageName());
    }

    public String getTransformPackageName() {
        return transformPackageName;
    }

    public void setTransformPackageName(String transformPackageName) {
        this.transformPackageName = transformPackageName;
    }

    public Metadata withTransformPackageName(String transformPackageName) {
        setTransformPackageName(transformPackageName);
        return this;
    }

    public String getFullRequestTransformPackageName() {
        return joinPackageNames(rootPackageName, getRequestTransformPackageName());
    }

    public String getRequestTransformPackageName() {
        return requestTransformPackageName;
    }

    public void setRequestTransformPackageName(String requestTransformPackageName) {
        this.requestTransformPackageName = requestTransformPackageName;
    }

    public Metadata withRequestTransformPackageName(String requestTransformPackageName) {
        setRequestTransformPackageName(requestTransformPackageName);
        return this;
    }

    public String getFullPaginatorsPackageName() {
        return joinPackageNames(rootPackageName, getPaginatorsPackageName());
    }

    public String getPaginatorsPackageName() {
        return paginatorsPackageName;
    }

    public void setPaginatorsPackageName(String paginatorsPackageName) {
        this.paginatorsPackageName = paginatorsPackageName;
    }

    public Metadata withPaginatorsPackageName(String paginatorsPackageName) {
        setPaginatorsPackageName(paginatorsPackageName);
        return this;
    }

    public String getFullAuthPolicyPackageName() {
        return joinPackageNames(rootPackageName, getAuthPolicyPackageName());
    }

    public String getAuthPolicyPackageName() {
        return authPolicyPackageName;
    }

    public void setAuthPolicyPackageName(String authPolicyPackageName) {
        this.authPolicyPackageName = authPolicyPackageName;
    }

    public Metadata withAuthPolicyPackageName(String authPolicyPackageName) {
        setSmokeTestsPackageName(authPolicyPackageName);
        return this;
    }

    public String getFullSmokeTestsPackageName() {
        return joinPackageNames(rootPackageName, getSmokeTestsPackageName());
    }

    public String getSmokeTestsPackageName() {
        return smokeTestsPackageName;
    }

    public void setSmokeTestsPackageName(String smokeTestsPackageName) {
        this.smokeTestsPackageName = smokeTestsPackageName;
    }

    public Metadata withSmokeTestsPackageName(String smokeTestsPackageName) {
        setSmokeTestsPackageName(smokeTestsPackageName);
        return this;
    }

    /**
     * @return The class name for service specific ModuleInjector.
     */
    public String getCucumberModuleInjectorClassName() {
        return getSyncInterface() + "ModuleInjector";
    }

    /**
     * Returns an abbreviated name for the service if one is defined in the
     * service model; for example "Amazon EC2". Returns null if no abbreviation
     * is defined.
     */
    public String getServiceAbbreviation() {
        return serviceAbbreviation;
    }

    public void setServiceAbbreviation(String serviceAbbreviation) {
        this.serviceAbbreviation = serviceAbbreviation;
    }

    public Metadata withServiceAbbreviation(String serviceAbbreviation) {
        setServiceAbbreviation(serviceAbbreviation);
        return this;
    }

    /**
     * Returns the full name of the service as defined in the service model;
     * for example "Amazon Elastic Compute Cloud".
     */
    public String getServiceFullName() {
        return serviceFullName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
    }

    public Metadata withServiceFullName(String serviceFullName) {
        setServiceFullName(serviceFullName);
        return this;
    }

    /**
     * Returns a convenient name for the service. If an abbreviated form
     * of the service name is available it will return that, otherwise it
     * will return the full service name.
     * <p>
     * Use me when casually referring to a service in documentation. Use
     * {@code getServiceFullName} if you want to make sure you have the
     * full-on official name of the service.
     */
    public String getServiceName() {
        if (serviceAbbreviation != null) {
            return serviceAbbreviation;
        }
        return serviceFullName;
    }

    public boolean isHasApiWithStreamInput() {
        return hasApiWithStreamInput;
    }

    public void setHasApiWithStreamInput(boolean hasApiWithStreamInput) {
        this.hasApiWithStreamInput = hasApiWithStreamInput;
    }

    public Metadata withHasApiWithStreamInput(boolean hasApiWithStreamInput) {
        setHasApiWithStreamInput(hasApiWithStreamInput);
        return this;
    }

    public String getJsonVersion() {
        return jsonVersion;
    }

    public void setJsonVersion(String jsonVersion) {
        this.jsonVersion = jsonVersion;
    }

    public Metadata withJsonVersion(String jsonVersion) {
        setJsonVersion(jsonVersion);
        return this;
    }

    public boolean isIonProtocol() {
        return protocolMetadataProvider.isIonProtocol();
    }

    public boolean isCborProtocol() {
        return protocolMetadataProvider.isCborProtocol();
    }

    public boolean isJsonProtocol() {
        return protocolMetadataProvider.isJsonProtocol();
    }

    public boolean isXmlProtocol() {
        return protocolMetadataProvider.isXmlProtocol();
    }

    /**
     * @return True for RESTful protocols. False for all other protocols (RPC, Query, etc).
     */
    public static boolean isNotRestProtocol(String protocol) {
        switch (Protocol.fromValue(protocol)) {
            case API_GATEWAY:
            case REST_JSON:
            case REST_XML:
                return false;
            default:
                return true;
        }
    }

    public String getEndpointPrefix() {
        return endpointPrefix;
    }

    public void setEndpointPrefix(String endpointPrefix) {
        this.endpointPrefix = endpointPrefix;
    }

    public Metadata withEndpointPrefix(String endpointPrefix) {
        setEndpointPrefix(endpointPrefix);
        return this;
    }

    public String getSigningName() {
        return signingName;
    }

    public void setSigningName(String signingName) {
        this.signingName = signingName;
    }

    public Metadata withSigningName(String signingName) {
        setSigningName(signingName);
        return this;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        if (contentType != null) {
            return contentType;
        }
        return protocolMetadataProvider.getContentType();
    }

    public String getUnmarshallerContextClassName() {
        return protocolMetadataProvider.getUnmarshallerContextClassName();
    }

    public String getProtocolFactory() {
        return protocolMetadataProvider.getProtocolFactoryImplFqcn();
    }

    public boolean isRequiresIamSigners() {
        return requiresIamSigners;
    }

    public void setRequiresIamSigners(boolean requiresIamSigners) {
        this.requiresIamSigners = requiresIamSigners;
    }

    public String getRequestBaseFqcn() {
        return protocolMetadataProvider.getRequestBaseFqcn();
    }

    public boolean isRequiresApiKey() {
        return requiresApiKey;
    }

    public Metadata withRequiresApiKey(boolean requiresApiKey) {
        this.requiresApiKey = requiresApiKey;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Metadata withUid(String uid) {
        setUid(uid);
        return this;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public Metadata withAuthType(AuthType authType) {
        this.authType = authType;
        return this;
    }

    private String joinPackageNames(String lhs, String rhs) {
        return StringUtils.isBlank(rhs) ? lhs : lhs + '.' + rhs;
    }
}
