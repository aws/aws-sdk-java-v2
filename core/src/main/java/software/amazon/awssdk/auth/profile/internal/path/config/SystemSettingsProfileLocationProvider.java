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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.profile.internal.path.AwsProfileFileLocationProvider;

/**
 * If the 'AWS_CONFIG_FILE' environment variable or 'aws.configFile' system property is set, then we source the config file from
 * the location specified. If both are specified, the system property is used.
 */
@SdkInternalApi
public class SystemSettingsProfileLocationProvider implements AwsProfileFileLocationProvider {
    @Override
    public Optional<Path> getLocation() {
        return AwsSystemSetting.AWS_CONFIG_FILE.getStringValue()
                                               .map(Paths::get);
    }
}
