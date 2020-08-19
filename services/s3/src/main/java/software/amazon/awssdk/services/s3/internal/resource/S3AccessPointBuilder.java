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

import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * This class is used to construct an endpoint host for an S3 access point.
 */
@SdkInternalApi
public class S3AccessPointBuilder {
    private static final Pattern HOSTNAME_COMPLIANT_PATTERN = Pattern.compile("[A-Za-z0-9\\-]+");
    private static final int HOSTNAME_MAX_LENGTH = 63;

    private Boolean dualstackEnabled;
    private String accessPointName;
    private String region;
    private String accountId;
    private String protocol;
    private String domain;
    private Boolean fipsEnabled;

    /**
     * Create a new instance of this builder class.
     */
    public static S3AccessPointBuilder create() {
        return new S3AccessPointBuilder();
    }

    /**
     * Enable DualStack endpoint.
     */
    public S3AccessPointBuilder dualstackEnabled(Boolean dualstackEnabled) {
        this.dualstackEnabled = dualstackEnabled;
        return this;
    }

    /**
     * Enable fips in endpoint.
     */
    public S3AccessPointBuilder fipsEnabled(Boolean fipsEnabled) {
        this.fipsEnabled = fipsEnabled;
        return this;
    }

    /**
     * The S3 Access Point name.
     */
    public S3AccessPointBuilder accessPointName(String accessPointName) {
        this.accessPointName = accessPointName;
        return this;
    }

    /**
     * The AWS region hosting the Access Point.
     */
    public S3AccessPointBuilder region(String region) {
        this.region = region;
        return this;
    }

    /**
     * The ID of the AWS Account the Access Point is associated with.
     */
    public S3AccessPointBuilder accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    /**
     * The protocol to be used with the endpoint URI.
     */
    public S3AccessPointBuilder protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * The TLD for the access point.
     */
    public S3AccessPointBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Generate an endpoint URI with no path that maps to the Access Point information stored in this builder.
     */
    public URI toUri() {
        validateHostnameCompliant(accountId, "accountId");
        validateHostnameCompliant(accessPointName, "accessPointName");

        String fipsSegment = Boolean.TRUE.equals(fipsEnabled) ? "fips-" : "";

        String dualStackSegment = Boolean.TRUE.equals(dualstackEnabled) ? ".dualstack" : "";
        String uriString = String.format("%s://%s-%s.s3-accesspoint%s.%s%s.%s", protocol, urlEncode(accessPointName), accountId,
                                         dualStackSegment, fipsSegment, region, domain);
        URI uri = URI.create(uriString);
        if (uri.getHost() == null) {
            throw SdkClientException.create("ARN region (" + region + ") resulted in an invalid URI:" + uri);
        }
        return uri;
    }

    private static void validateHostnameCompliant(String hostnameComponent, String paramName) {
        if (hostnameComponent.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("An S3 Access Point ARN has been passed that is not valid: the required '%s' "
                              + "component is missing.", paramName));
        }

        if (hostnameComponent.length() > HOSTNAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                String.format("An S3 Access Point ARN has been passed that is not valid: the '%s' "
                              + "component exceeds the maximum length of %d characters.", paramName, HOSTNAME_MAX_LENGTH));
        }

        Matcher m = HOSTNAME_COMPLIANT_PATTERN.matcher(hostnameComponent);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                String.format("An S3 Access Point ARN has been passed that is not valid: the '%s' "
                              + "component must only contain alphanumeric characters and dashes.", paramName));
        }
    }
}