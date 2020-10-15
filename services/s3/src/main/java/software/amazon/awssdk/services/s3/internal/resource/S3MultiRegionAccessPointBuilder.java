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
 * This class is used to construct an endpoint for an S3 global, multi-region access point.
 */
@SdkInternalApi
public final class S3MultiRegionAccessPointBuilder {

    private String accessPointName;
    private String accountId;
    private String protocol;
    private String domain;

    private S3MultiRegionAccessPointBuilder() {
    }

    /**
     * Create a new instance of this builder class.
     */
    public static S3MultiRegionAccessPointBuilder create() {
        return new S3MultiRegionAccessPointBuilder();
    }

    public S3MultiRegionAccessPointBuilder accessPointName(String accessPointName) {
        this.accessPointName = accessPointName;
        return this;
    }

    public S3MultiRegionAccessPointBuilder accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public S3MultiRegionAccessPointBuilder protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public S3MultiRegionAccessPointBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Generate an endpoint URI with no path that maps to the Multi-Region Access Point information stored in this builder.
     */
    public URI toUri() {
        validateHostnameCompliant(accountId, "accountId", "multi-region ARN");
        validateHostnameCompliant(accessPointName, "accessPointName", "multi-region ARN");

        String uriString = String.format("%s://%s.%s.mrap.global-s3.%s", protocol, accessPointName, accountId, domain);
        return URI.create(uriString);
    }
}
