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

package software.amazon.awssdk.services.s3.internal.endpoints;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Endpoint builder for operations specific to S3 Object Lambda.
 */
@SdkInternalApi
public class S3ObjectLambdaOperationEndpointBuilder {
    private String region;
    private String protocol;
    private String domain;

    private S3ObjectLambdaOperationEndpointBuilder() {
    }

    /**
     * Create a new instance of this builder class.
     */
    public static S3ObjectLambdaOperationEndpointBuilder create() {
        return new S3ObjectLambdaOperationEndpointBuilder();
    }


    public S3ObjectLambdaOperationEndpointBuilder region(String region) {
        this.region = region;
        return this;
    }

    public S3ObjectLambdaOperationEndpointBuilder protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public S3ObjectLambdaOperationEndpointBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Generate an endpoint URI with no path that maps to the Object Lambdas Access Point information stored in this builder.
     */
    public URI toUri() {
        Validate.paramNotBlank(protocol, "protocol");
        Validate.paramNotBlank(domain, "domain");
        Validate.paramNotBlank(region, "region");

        String servicePrefix = "s3-object-lambda";

        String uriString = String.format("%s://%s.%s.%s",
                protocol,
                servicePrefix,
                region,
                domain);

        return URI.create(uriString);
    }
}