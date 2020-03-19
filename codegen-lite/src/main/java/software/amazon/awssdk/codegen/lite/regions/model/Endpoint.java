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

package software.amazon.awssdk.codegen.lite.regions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Endpoint configuration.
 */
@SdkInternalApi
public final class Endpoint implements Cloneable {

    private static final String HTTP = "http";

    private static final String HTTPS = "https";

    /**
     * endpoint string.
     */
    private String hostname;

    /**
     * credential scope for the endpoint.
     */
    private CredentialScope credentialScope;

    /**
     * supported schemes for the endpoint.
     */
    private List<String> protocols;

    /**
     * supported signature versions of the endpoint.
     */
    private List<String> signatureVersions;

    /**
     * ssl common name for the endpoint.
     */
    private String sslCommonName;

    public Endpoint() {
    }

    /**
     * Merges the given endpoints and returns the merged one.
     */
    public Endpoint merge(Endpoint higher) {
        if (higher == null) {
            higher = new Endpoint();
        }

        Endpoint merged = this.clone();

        merged.setCredentialScope(higher.getCredentialScope() != null
                                          ? higher.getCredentialScope()
                                          : merged.getCredentialScope());

        merged.setHostname(higher.getHostname() != null
                                   ? higher.getHostname()
                                   : merged.getHostname());

        merged.setSslCommonName(higher.getSslCommonName() != null
                                        ? higher.getSslCommonName()
                                        : merged.getSslCommonName());

        merged.setProtocols(higher.getProtocols() != null
                                    ? higher.getProtocols()
                                    : merged.getProtocols());

        merged.setSignatureVersions(higher.getSignatureVersions() != null
                                            ? higher.getSignatureVersions()
                                            : merged.getSignatureVersions());

        return merged;
    }

    /**
     * returns the endpoint string.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * sets the endpoint string.
     */
    @JsonProperty(value = "hostname")
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * returns credential scope for the endpoint.
     */
    public CredentialScope getCredentialScope() {
        return credentialScope;
    }

    /**
     * sets the credential scope for the endpoint.
     */
    @JsonProperty(value = "credentialScope")
    public void setCredentialScope(CredentialScope credentialScope) {
        this.credentialScope = credentialScope;
    }

    /**
     * returns the supported schemes for the endpoint.
     */
    public List<String> getProtocols() {
        return protocols;
    }

    /**
     * sets the supported schemes for the endpoint.
     */
    public void setProtocols(List<String> protocols) {
        this.protocols = protocols;
    }

    /**
     * returns the supported signature versions of the endpoint.
     */
    public List<String> getSignatureVersions() {
        return signatureVersions;
    }

    /**
     * returns the supported signature versions of the endpoint.
     */
    @JsonProperty(value = "signatureVersions")
    public void setSignatureVersions(List<String> signatureVersions) {
        this.signatureVersions = signatureVersions;
    }

    /**
     * returns the ssl common name for the endpoint.
     */
    public String getSslCommonName() {
        return sslCommonName;
    }

    /**
     * sets the ssl common name for the endpoint.
     */
    @JsonProperty(value = "sslCommonName")
    public void setSslCommonName(String sslCommonName) {
        this.sslCommonName = sslCommonName;
    }

    /**
     * A convenient method that returns true if the endpoint support HTTPS
     * scheme. Returns false otherwise.
     */
    public boolean hasHttpsSupport() {
        return isProtocolSupported(HTTPS);
    }

    /**
     * A convenient method that returns true if the endpoint support HTTP
     * scheme. Returns false otherwise.
     */
    public boolean hasHttpSupport() {
        return isProtocolSupported(HTTP);
    }

    private boolean isProtocolSupported(String protocol) {
        return protocols != null && protocols.contains(protocol);
    }

    @Override
    protected Endpoint clone() {
        try {
            return (Endpoint) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(
                    "Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!", e);
        }
    }
}
