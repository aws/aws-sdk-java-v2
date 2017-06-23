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

package software.amazon.awssdk.auth;

import java.util.Optional;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * {@link AwsCredentialsProvider} implementation that loads credentials from the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and
 * AWS_SESSION_TOKEN environment variables.
 */
public class EnvironmentVariableCredentialsProvider extends SystemSettingsCredentialsProvider {
    @Override
    protected Optional<String> loadSetting(SystemSetting setting) {
        // CHECKSTYLE:OFF - Customers should be able to specify a credentials provider that only looks at the environment
        // variables, but not the system properties. For that reason, we're only checking the environment variable here.
        return Optional.ofNullable(System.getenv(setting.environmentVariable()));
        // CHECKSTYLE:ON
    }
}
