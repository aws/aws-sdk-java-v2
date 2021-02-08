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

package software.amazon.awssdk.services.s3.internal.resource;

import static software.amazon.awssdk.utils.HostnameValidator.validateHostnameCompliant;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * This class is used to construct an endpoint for an S3 outpost access point.
 */
@SdkInternalApi
public final class S3OutpostAccessPointBuilder {

    private URI endpointOverride;
    private String accessPointName;
    private String outpostId;
    private String region;
    private String accountId;
    private String protocol;
    private String domain;

    private S3OutpostAccessPointBuilder() {
    }

    /**
     * Create a new instance of this builder class.
     */
    public static S3OutpostAccessPointBuilder create() {
        return new S3OutpostAccessPointBuilder();
    }

    /**
     * The endpoint override configured on the client (null if no endpoint override was set).
     */
    public S3OutpostAccessPointBuilder endpointOverride(URI endpointOverride) {
        this.endpointOverride = endpointOverride;
        return this;
    }

    public S3OutpostAccessPointBuilder accessPointName(String accessPointName) {
        this.accessPointName = accessPointName;
        return this;
    }

    public S3OutpostAccessPointBuilder region(String region) {
        this.region = region;
        return this;
    }

    public S3OutpostAccessPointBuilder accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public S3OutpostAccessPointBuilder outpostId(String outpostId) {
        this.outpostId = outpostId;
        return this;
    }

    public S3OutpostAccessPointBuilder protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public S3OutpostAccessPointBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Generate an endpoint URI with no path that maps to the Outpost Access Point information stored in this builder.
     */
    public URI toUri() {
        validateHostnameCompliant(outpostId, "outpostId", "outpost ARN");
        validateHostnameCompliant(accountId, "accountId", "outpost ARN");
        validateHostnameCompliant(accessPointName, "accessPointName", "outpost ARN");

        String uri;
        if (endpointOverride == null) {
            uri = String.format("%s://%s-%s.%s.s3-outposts.%s.%s", protocol, accessPointName, accountId, outpostId,
                                region, domain);
        } else {
            StringBuilder uriSuffix = new StringBuilder(endpointOverride.getHost());
            if (endpointOverride.getPort() > 0) {
                uriSuffix.append(":").append(endpointOverride.getPort());
            }
            if (endpointOverride.getPath() != null) {
                uriSuffix.append(endpointOverride.getPath());
            }

            uri = String.format("%s://%s-%s.%s.%s", protocol, urlEncode(accessPointName), accountId, outpostId, uriSuffix);
        }

        URI result = URI.create(uri);
        if (result.getHost() == null) {
            throw SdkClientException.create("Request resulted in an invalid URI: " + result);
        }

        return result;
    }
}
