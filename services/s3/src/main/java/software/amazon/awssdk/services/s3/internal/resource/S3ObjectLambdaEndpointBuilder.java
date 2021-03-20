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

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * This class is used to construct an endpoint for an S3 Object Lambda access point.
 */
@SdkInternalApi
public class S3ObjectLambdaEndpointBuilder {
    private URI endpointOverride;
    private String accessPointName;
    private String region;
    private String accountId;
    private String protocol;
    private String domain;
    private Boolean fipsEnabled;
    private Boolean dualstackEnabled;

    private S3ObjectLambdaEndpointBuilder() {
    }

    /**
     * Create a new instance of this builder class.
     */
    public static S3ObjectLambdaEndpointBuilder create() {
        return new S3ObjectLambdaEndpointBuilder();
    }

    /**
     * The endpoint override configured on the client (null if no endpoint override was set).
     */
    public S3ObjectLambdaEndpointBuilder endpointOverride(URI endpointOverride) {
        this.endpointOverride = endpointOverride;
        return this;
    }

    public S3ObjectLambdaEndpointBuilder accessPointName(String accessPointName) {
        this.accessPointName = accessPointName;
        return this;
    }

    public S3ObjectLambdaEndpointBuilder region(String region) {
        this.region = region;
        return this;
    }

    public S3ObjectLambdaEndpointBuilder accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public S3ObjectLambdaEndpointBuilder protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public S3ObjectLambdaEndpointBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    public S3ObjectLambdaEndpointBuilder fipsEnabled(Boolean fipsEnabled) {
        this.fipsEnabled = fipsEnabled;
        return this;
    }

    public S3ObjectLambdaEndpointBuilder dualstackEnabled(Boolean dualstackEnabled) {
        this.dualstackEnabled = dualstackEnabled;
        return this;
    }

    public URI toUri() {
        validateHostnameCompliant(accountId, "accountId", "object lambda ARN");
        validateHostnameCompliant(accessPointName, "accessPointName", "object lambda ARN");

        String fipsSegment = Boolean.TRUE.equals(fipsEnabled) ? "fips-" : "";

        String uriString;
        if (endpointOverride == null) {
            if (Boolean.TRUE.equals(dualstackEnabled)) {
                throw new IllegalStateException("S3 Object Lambda does not support Dual stack endpoints.");
            }

            uriString = String.format("%s://%s-%s.s3-object-lambda.%s%s.%s",
                                      protocol, accessPointName, accountId, fipsSegment, region, domain);
        } else {
            StringBuilder uriSuffix = new StringBuilder(endpointOverride.getHost());
            if (endpointOverride.getPort() > 0) {
                uriSuffix.append(":").append(endpointOverride.getPort());
            }
            if (endpointOverride.getPath() != null) {
                uriSuffix.append(endpointOverride.getPath());
            }

            uriString = String.format("%s://%s-%s.%s", protocol, accessPointName, accountId, uriSuffix);
        }

        return URI.create(uriString);
    }
}
