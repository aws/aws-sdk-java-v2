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

package software.amazon.awssdk.auth.profile.internal.path.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.profile.internal.path.AwsDirectoryBasePathProvider;

/**
 * Checks if there is a config file present at the default location (~/.aws/config).
 */
@SdkInternalApi
public class SharedConfigDefaultLocationProvider extends AwsDirectoryBasePathProvider {

    /**
     * Default name of the shared AWS config file.
     */
    private static final String DEFAULT_CONFIG_FILE_NAME = "config";

    @Override
    public Optional<Path> getLocation() {
        Path file = getAwsDirectory().resolve(DEFAULT_CONFIG_FILE_NAME);
        if (Files.isRegularFile(file)) {
            return Optional.of(file);
        }
        return Optional.empty();
    }
}
