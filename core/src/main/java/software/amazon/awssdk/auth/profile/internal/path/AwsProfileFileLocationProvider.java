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

package software.amazon.awssdk.auth.profile.internal.path;

import java.nio.file.Path;
import java.util.Optional;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Provides the location of both the AWS Shared credentials file (~/.aws/credentials) or the AWS
 * Shared config file (~/.aws/config).
 */
@SdkInternalApi
@FunctionalInterface
public interface AwsProfileFileLocationProvider {
    /**
     * Location provider for the shared AWS credentials file. Checks the environment variable and system property overrides
     * first, falling back to the default location (~/.aws/credentials) if they were not configured.
     */
    AwsProfileFileLocationProvider DEFAULT_CREDENTIALS_LOCATION_PROVIDER = new CredentialsSystemSettingsLocationProvider();

    /**
     * Location provider for the shared AWS config file. Checks the environment variable and system property overrides
     * first, falling back to the default location (~/.aws/config) if they were not configured.
     */
    AwsProfileFileLocationProvider DEFAULT_CONFIG_LOCATION_PROVIDER = new SystemSettingsProfileLocationProvider();

    /**
     * @return Location of file containing profile data. Optional.empty if the file could not be located.
     */
    Optional<Path> getLocation();
}
