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

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.utils.StringUtils;

public class Metadata {

    private String apiVersion;

    private Protocol protocol;

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

    private String waitersPackageName;

    private String serviceAbbreviation;

    private String serviceFullName;

    private String serviceName;

    private String baseExceptionName;

    private String contentType;

    private String jsonVersion;

    private String endpointPrefix;

    private String signingName;

    private boolean requiresIamSigners;

    private boolean requiresApiKey;

    private String uid;

    private AuthType authType;

    private String baseRequestName;

    private String baseResponseName;

    private boolean supportsH2;

    private String serviceId;

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
    }

    public Metadata withProtocol(Protocol protocol) {
        setProtocol(protocol);
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
        setAuthPolicyPackageName(authPolicyPackageName);
        return this;
    }

    public void setServiceAbbreviation(String serviceAbbreviation) {
        this.serviceAbbreviation = serviceAbbreviation;
    }

    public Metadata withServiceAbbreviation(String serviceAbbreviation) {
        setServiceAbbreviation(serviceAbbreviation);
        return this;
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
     */
    public String getDescriptiveServiceName() {
        if (serviceAbbreviation != null) {
            return serviceAbbreviation;
        }
        return serviceFullName;
    }

    /**
     * @return Unique, short name for the service. Suitable for displaying in metadata like {@link AwsErrorDetails} and
     * for use in metrics. Should not be used in documentation, use {@link #getDescriptiveServiceName()} for that.
     */
    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Metadata withServiceName(String serviceName) {
        setServiceName(serviceName);
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
        return protocol == Protocol.ION;
    }

    public boolean isCborProtocol() {
        return protocol == Protocol.CBOR;
    }

    public boolean isJsonProtocol() {
        return protocol == Protocol.CBOR ||
               protocol == Protocol.ION ||
               protocol == Protocol.AWS_JSON ||
               protocol == Protocol.REST_JSON;
    }

    public boolean isXmlProtocol() {
        return protocol == Protocol.EC2 ||
               protocol == Protocol.QUERY ||
               protocol == Protocol.REST_XML;
    }

    public boolean isQueryProtocol() {
        return protocol == Protocol.EC2 ||
               protocol == Protocol.QUERY;
    }

    /**
     * @return True for RESTful protocols. False for all other protocols (RPC, Query, etc).
     */
    public static boolean isNotRestProtocol(String protocol) {
        switch (Protocol.fromValue(protocol)) {
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
        return contentType;
    }

    public boolean isRequiresIamSigners() {
        return requiresIamSigners;
    }

    public void setRequiresIamSigners(boolean requiresIamSigners) {
        this.requiresIamSigners = requiresIamSigners;
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

    public String getBaseRequestName() {
        return baseRequestName;
    }

    public Metadata withBaseRequestName(String baseRequestName) {
        this.baseRequestName = baseRequestName;
        return this;
    }

    public String getBaseResponseName() {
        return baseResponseName;
    }

    public Metadata withBaseResponseName(String baseResponseName) {
        this.baseResponseName = baseResponseName;
        return this;
    }

    private String joinPackageNames(String lhs, String rhs) {
        return StringUtils.isBlank(rhs) ? lhs : lhs + '.' + rhs;
    }

    public boolean supportsH2() {
        return supportsH2;
    }

    public void setSupportsH2(boolean supportsH2) {
        this.supportsH2 = supportsH2;
    }

    public Metadata withSupportsH2(boolean supportsH2) {
        setSupportsH2(supportsH2);
        return this;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Metadata withServiceId(String serviceId) {
        setServiceId(serviceId);
        return this;
    }

    public String getWaitersPackageName() {
        return waitersPackageName;
    }

    public void setWaitersPackageName(String waitersPackageName) {
        this.waitersPackageName = waitersPackageName;
    }

    public Metadata withWaitersPackageName(String waitersPackageName) {
        setWaitersPackageName(waitersPackageName);
        return this;
    }

    public String getFullWaitersPackageName() {
        return joinPackageNames(rootPackageName, getWaitersPackageName());
    }

    public String getFullWaitersInternalPackageName() {
        return joinPackageNames(getFullWaitersPackageName(), "internal");
    }
}
