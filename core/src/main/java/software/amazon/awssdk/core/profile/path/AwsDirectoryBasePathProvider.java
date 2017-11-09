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

package software.amazon.awssdk.core.profile.path;

import java.io.File;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.JavaSystemSetting;

/**
 * Base provider for all location providers that source a file from the ~/.aws directory.
 */
@SdkInternalApi
public abstract class AwsDirectoryBasePathProvider implements AwsProfileFileLocationProvider {
    /**
     * @return File of ~/.aws directory.
     */
    protected final File getAwsDirectory() {
        return new File(getHomeDirectory(), ".aws");
    }

    private String getHomeDirectory() {
        return JavaSystemSetting.USER_HOME.getStringValueOrThrow();
    }
}
