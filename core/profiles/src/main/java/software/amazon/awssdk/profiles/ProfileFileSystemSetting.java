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

package software.amazon.awssdk.profiles;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * System settings for loading configuration from profile files.
 */
@SdkProtectedApi
public enum ProfileFileSystemSetting implements SystemSetting {
    /**
     * Configure the default configuration file used in the ProfileFile. When not explicitly
     * overridden in a client (eg. by specifying the region or credentials provider), this will be the location used when an
     * AWS client is created.
     *
     * See http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html for more information on configuring the
     * SDK via a configuration file.
     */
    AWS_CONFIG_FILE("aws.configFile", null),

    /**
     * Configure the default credentials file used in the ProfileFile. When not explicitly
     * overridden in a client (eg. by specifying the region or credentials provider), this will be the location used when an
     * AWS client is created.
     *
     * See http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html for more information on configuring the
     * SDK via a credentials file.
     */
    AWS_SHARED_CREDENTIALS_FILE("aws.sharedCredentialsFile", null),

    /**
     * Configure the default profile that should be loaded from the {@link #AWS_CONFIG_FILE}
     *
     * @see #AWS_CONFIG_FILE
     */
    AWS_PROFILE("aws.profile", "default");

    private final String systemProperty;
    private final String defaultValue;

    ProfileFileSystemSetting(String systemProperty, String defaultValue) {
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
