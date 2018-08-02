/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * System properties to configure the SDK runtime.
 */
@SdkProtectedApi
public enum SdkSystemSetting implements SystemSetting {
    /**
     * Configure the AWS access key ID.
     *
     * This value will not be ignored if the {@link #AWS_SECRET_ACCESS_KEY} is not specified.
     */
    AWS_ACCESS_KEY_ID("aws.accessKeyId", null),

    /**
     * Configure the AWS secret access key.
     *
     * This value will not be ignored if the {@link #AWS_ACCESS_KEY_ID} is not specified.
     */
    AWS_SECRET_ACCESS_KEY("aws.secretAccessKey", null),

    /**
     * Configure the AWS session token.
     */
    AWS_SESSION_TOKEN("aws.sessionToken", null),

    /**
     * Configure the default region.
     */
    AWS_REGION("aws.region", null),

    /**
     * Whether to load information such as credentials, regions from EC2 Metadata instance service.
     */
    AWS_EC2_METADATA_DISABLED("aws.disableEc2Metadata", "false"),

    /**
     * The EC2 instance metadata service endpoint.
     *
     * This allows a service running in EC2 to automatically load its credentials and region without needing to configure them
     * in the SdkClientBuilder.
     */
    AWS_EC2_METADATA_SERVICE_ENDPOINT("aws.ec2MetadataServiceEndpoint", "http://169.254.169.254"),

    /**
     * The elastic container metadata service endpoint that should be called by the ContainerCredentialsProvider
     * when loading data from the container metadata service.
     *
     * This allows a service running in an elastic container to automatically load its credentials without needing to configure
     * them in the SdkClientBuilder.
     *
     * This is not used if the {@link #AWS_CONTAINER_CREDENTIALS_RELATIVE_URI} is not specified.
     */
    AWS_CONTAINER_SERVICE_ENDPOINT("aws.containerServiceEndpoint", "http://169.254.170.2"),

    /**
     * The elastic container metadata service path that should be called by the ContainerCredentialsProvider when
     * loading credentials form the container metadata service. If this is not specified, credentials will not be automatically
     * loaded from the container metadata service.
     *
     * @see #AWS_CONTAINER_SERVICE_ENDPOINT
     */
    AWS_CONTAINER_CREDENTIALS_RELATIVE_URI("aws.containerCredentialsPath", null),

    /**
     * The full URI path to a localhost metadata service to be used.
     */
    AWS_CONTAINER_CREDENTIALS_FULL_URI("aws.containerCredentialsFullUri", null),

    /**
     * An authorization token to pass to a container metadata service, only used when {@link #AWS_CONTAINER_CREDENTIALS_FULL_URI}
     * is specified.
     *
     * @see #AWS_CONTAINER_CREDENTIALS_FULL_URI
     */
    AWS_CONTAINER_AUTHORIZATION_TOKEN("aws.containerAuthorizationToken", null),

    /**
     * Explicitly identify the default synchronous HTTP implementation the SDK will use. Useful
     * when there are multiple implementations on the classpath or as a performance optimization
     * since implementation discovery requires classpath scanning.
     */
    SYNC_HTTP_SERVICE_IMPL("software.amazon.awssdk.http.service.impl", null),

    /**
     * Explicitly identify the default Async HTTP implementation the SDK will use. Useful
     * when there are multiple implementations on the classpath or as a performance optimization
     * since implementation discovery requires classpath scanning.
     */
    ASYNC_HTTP_SERVICE_IMPL("software.amazon.awssdk.http.async.service.impl", null),

    /**
     * Whether CBOR optimization should automatically be used if its support is found on the classpath and the service supports
     * CBOR-formatted JSON.
     */
    CBOR_ENABLED("aws.cborEnabled", "true"),

    /**
     * Whether binary ION representation optimization should automatically be used if the service supports ION.
     */
    BINARY_ION_ENABLED("aws.binaryIonEnabled", "true"),

    /**
     * The execution environment of the SDK user. This is automatically set in certain environments by the underlying AWS service.
     * For example, AWS Lambda will automatically specify a runtime indicating that the SDK is being used within Lambda.
     */
    AWS_EXECUTION_ENV("aws.executionEnvironment", null);

    private final String systemProperty;
    private final String defaultValue;

    SdkSystemSetting(String systemProperty, String defaultValue) {
        this.systemProperty = systemProperty;
        this.defaultValue = defaultValue;
    }

    @Override
    public String property() {
        return systemProperty;
    }

    @Override
    public String environmentVariable() {
        return name();
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }
}
