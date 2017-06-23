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

package software.amazon.awssdk;

import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.AwsSessionCredentials;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.ElasticContainerCredentialsProvider;
import software.amazon.awssdk.auth.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.client.builder.ClientBuilder;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.profile.path.config.SystemSettingsProfileLocationProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;
import software.amazon.awssdk.regions.providers.SystemSettingsRegionProvider;
import software.amazon.awssdk.utils.JavaSystemSetting;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * The system properties and environment variables supported by the AWS SDK.
 *
 * @see JavaSystemSetting
 */
@ReviewBeforeRelease("Do we need all of these, or can they be controlled in the client configuration?")
public enum AwsSystemSetting implements SystemSetting {
    /**
     * Configure the AWS access key ID used in the {@link EnvironmentVariableCredentialsProvider} and
     * {@link SystemPropertyCredentialsProvider}. This value is checked by the {@link DefaultCredentialsProvider}, which is used
     * when clients are created with no credential provider specified via
     * {@link ClientBuilder#credentialsProvider(AwsCredentialsProvider)}.
     *
     * This value will not be ignored if the {@link #AWS_SECRET_ACCESS_KEY} is not specified.
     *
     * @see AwsCredentials
     */
    AWS_ACCESS_KEY_ID("aws.accessKeyId", null),

    /**
     * Configure the AWS secret access key used in the {@link EnvironmentVariableCredentialsProvider} and
     * {@link SystemPropertyCredentialsProvider}. This value is checked by the {@link DefaultCredentialsProvider}, which is used
     * when clients are created with no credential provider specified via
     * {@link ClientBuilder#credentialsProvider(AwsCredentialsProvider)}.
     *
     * This value will not be ignored if the {@link #AWS_ACCESS_KEY_ID} is not specified.
     *
     * @see AwsCredentials
     */
    AWS_SECRET_ACCESS_KEY("aws.secretAccessKey", null),

    /**
     * Configure the AWS session token used in the {@link EnvironmentVariableCredentialsProvider} and
     * {@link SystemPropertyCredentialsProvider}. This value is checked by the {@link DefaultCredentialsProvider}, which is used
     * when clients are created with no credential provider specified via
     * {@link ClientBuilder#credentialsProvider(AwsCredentialsProvider)}.
     *
     * This value will not be ignored if the {@link #AWS_ACCESS_KEY_ID} and {@link #AWS_SECRET_ACCESS_KEY} are not specified.
     *
     * @see AwsSessionCredentials
     */
    AWS_SESSION_TOKEN("aws.sessionToken", null),

    /**
     * Configure the default region used in the {@link SystemSettingsRegionProvider}. This value is checked by the
     * {@link ClientBuilder} when no region is specified via {@link ClientBuilder#region(Region)}.
     *
     * @see Region
     */
    AWS_REGION("aws.region", null),

    /**
     * Configure the default configuration file used in the {@link SystemSettingsProfileLocationProvider}. This value is
     * checked by the {@link DefaultCredentialsProvider}, which is used when clients are created with no credential provider
     * specified via {@link ClientBuilder#credentialsProvider(AwsCredentialsProvider)}.
     *
     * See http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html for more information on configuring the
     * SDK via a configuration file.
     *
     * @see ProfileCredentialsProvider
     */
    @ReviewBeforeRelease("The code relies on this being null, so it's treated as an override. When we refactor the profile stuff"
                         + "we should consider adding a default to this value and simplifying the location finding.")
    AWS_CONFIG_FILE("aws.configFile", null),

    /**
     * Configure the default profile that should be loaded from the {@link #AWS_CONFIG_FILE} when using configuration files for
     * configuring the SDK.
     *
     * @see #AWS_CONFIG_FILE
     */
    AWS_DEFAULT_PROFILE("aws.defaultProfile", "default"),

    /**
     * Whether the default configuration applied to AWS clients should be optimized for services within the same region.
     * This will usually include lower request timeouts because requests do not need to travel outside of the AWS network.
     */
    AWS_IN_REGION_OPTIMIZATION_ENABLED("aws.inRegionOptimizationEnabled", "false"),

    /**
     * The execution environment of the SDK user. This is automatically set in certain environments by the underlying AWS service.
     * For example, AWS Lambda will automatically specify a runtime indicating that the SDK is being used within Lambda.
     */
    AWS_EXECUTION_ENV("aws.executionEnvironment", null),

    /**
     * The EC2 instance metadata service endpoint that should be called by the {@link InstanceProfileCredentialsProvider} and
     * {@link InstanceProfileRegionProvider} when loading data from the EC2 instance metadata service.
     *
     * This allows a service running in EC2 to automatically load its credentials and region without needing to configure them
     * in the {@link ClientBuilder}.
     */
    AWS_EC2_METADATA_SERVICE_ENDPOINT("aws.ec2MetadataServiceEndpoint", "http://169.254.169.254"),

    /**
     * The elastic container metadata service endpoint that should be called by the {@link ElasticContainerCredentialsProvider}
     * when loading data from the container metadata service.
     *
     * This allows a service running in an elastic container to automatically load its credentials without needing to configure
     * them in the {@link ClientBuilder}.
     *
     * This is not used if the {@link #AWS_CONTAINER_CREDENTIALS_PATH} is not specified.
     */
    AWS_CONTAINER_SERVICE_ENDPOINT("aws.containerServiceEndpoint", "http://169.254.170.2"),

    /**
     * The elastic container metadata service path that should be called by the {@link ElasticContainerCredentialsProvider} when
     * loading credentials form the container metadata service. If this is not specified, credentials will not be automatically
     * loaded from the container metadata service.
     *
     * @see #AWS_CONTAINER_SERVICE_ENDPOINT
     */
    AWS_CONTAINER_CREDENTIALS_PATH("aws.containerCredentialsPath", null),

    /**
     * Whether CBOR optimization should automatically be used if its support is found on the classpath and the service supports
     * CBOR-formatted JSON.
     */
    @ReviewBeforeRelease("This shouldn't be AWS-branded if CBOR is a core SDK feature.")
    AWS_CBOR_ENABLED("aws.cborEnabled", "true"),

    /**
     * Whether binary ION representation optimization should automatically be used if the service supports ION.
     */
    @ReviewBeforeRelease("This shouldn't be AWS-branded if ION is a core SDK feature.")
    AWS_BINARY_ION_ENABLED("aws.binaryIonEnabled", "true"),

    /**
     * @see AwsSdkMetrics
     */
    @ReviewBeforeRelease("This shouldn't be AWS-branded if metrics are a core SDK feature.")
    AWS_DEFAULT_METRICS("aws.defaultMetrics", null);

    private final String systemProperty;
    private final String defaultValue;

    AwsSystemSetting(String systemProperty, String defaultValue) {
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
