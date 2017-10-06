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

package software.amazon.awssdk.core.profile.path.cred;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.profile.path.AwsDirectoryBasePathProvider;

/**
 * Treats the CLI config file as the source of credentials. We support this for legacy reasons,
 * ideally credentials should be defined in the separate shared credentials file
 * (~/.aws/credentials). Any credentials defined in the shared credentials file take precedence over
 * credentials sourced from here.
 */
@SdkInternalApi
public class CredentialsLegacyConfigLocationProvider extends AwsDirectoryBasePathProvider {

    private static final Logger log = LoggerFactory.getLogger(CredentialsLegacyConfigLocationProvider.class);

    /**
     * File name of the default location of the CLI config file.
     */
    private static final String LEGACY_CONFIG_PROFILES_FILENAME = "config";

    @Override
    public File getLocation() {
        File legacyConfigProfiles = new File(getAwsDirectory(), LEGACY_CONFIG_PROFILES_FILENAME);
        if (legacyConfigProfiles.exists() && legacyConfigProfiles.isFile()) {
            log.warn("Found the legacy config profiles file at [{}]. " +
                     "Please move it to the latest default location [~/.aws/credentials]",
                     legacyConfigProfiles.getAbsolutePath());
            return legacyConfigProfiles;
        }
        return null;
    }
}
