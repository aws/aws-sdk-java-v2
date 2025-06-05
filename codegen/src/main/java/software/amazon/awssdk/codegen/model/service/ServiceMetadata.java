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

package software.amazon.awssdk.codegen.model.service;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.utils.ProtocolUtils;

public class ServiceMetadata {

    private String apiVersion;

    private String endpointPrefix;

    private String signingName;

    private String serviceAbbreviation;

    private String serviceFullName;

    private String serviceId;

    private String xmlNamespace;

    private String protocol;

    private List<String> protocols;

    private String jsonVersion;

    private Map<String, String> awsQueryCompatible;

    private boolean resultWrapped;

    private String signatureVersion;

    private String targetPrefix;

    private String uid;

    private List<String> auth;

    private Map<String, String> protocolSettings;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getEndpointPrefix() {
        return endpointPrefix;
    }

    public void setEndpointPrefix(String endpointPrefix) {
        this.endpointPrefix = endpointPrefix;
    }

    public String getSigningName() {
        if (signingName == null) {
            setSigningName(endpointPrefix);
        }
        return signingName;
    }

    public void setSigningName(String signingName) {
        this.signingName = signingName;
    }

    public String getServiceAbbreviation() {
        return serviceAbbreviation;
    }

    public void setServiceAbbreviation(String serviceAbbreviation) {
        this.serviceAbbreviation = serviceAbbreviation;
    }

    public String getServiceFullName() {
        return serviceFullName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
    }

    public String getXmlNamespace() {
        return xmlNamespace;
    }

    public void setXmlNamespace(String xmlNamespace) {
        this.xmlNamespace = xmlNamespace;
    }

    /**
     * {@code protocol} superseded by {@code protocols} field, resolved in {@link ProtocolUtils#resolveProtocol(ServiceMetadata)}
     */
    @Deprecated
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public List<String> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<String> protocols) {
        this.protocols = protocols;
    }

    public String getJsonVersion() {
        return jsonVersion;
    }

    public void setJsonVersion(String jsonVersion) {
        this.jsonVersion = jsonVersion;
    }

    public Map<String, String> getAwsQueryCompatible() {
        return awsQueryCompatible;
    }

    public void setAwsQueryCompatible(Map<String, String> awsQueryCompatible) {
        this.awsQueryCompatible = awsQueryCompatible;
    }

    public boolean isResultWrapped() {
        return resultWrapped;
    }

    public void setResultWrapped(boolean resultWrapped) {
        this.resultWrapped = resultWrapped;
    }

    public String getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(String signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    public String getTargetPrefix() {
        return targetPrefix;
    }

    public void setTargetPrefix(String targetPrefix) {
        this.targetPrefix = targetPrefix;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<String> getAuth() {
        return auth;
    }

    public void setAuth(List<String> auth) {
        this.auth = auth;
    }

    public Map<String, String> getProtocolSettings() {
        return protocolSettings;
    }

    public void setProtocolSettings(Map<String, String> protocolSettings) {
        this.protocolSettings = protocolSettings;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
}
