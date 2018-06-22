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

package software.amazon.awssdk.auth.credentials.internal;

import static software.amazon.awssdk.utils.StringUtils.trim;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * Loads credentials providers from the {@link SdkSystemSetting#AWS_ACCESS_KEY_ID},
 * {@link SdkSystemSetting#AWS_SECRET_ACCESS_KEY}, and {@link SdkSystemSetting#AWS_SESSION_TOKEN} system settings.
 *
 * This does not load the credentials directly. Instead, the actual mapping of setting to credentials is done by child classes.
 * This allows us to separately load the credentials from system properties and environment variables so that customers can
 * remove one or the other from their credential chain, or build a different chain with these pieces of functionality separated.
 *
 * @see EnvironmentVariableCredentialsProvider
 * @see SystemPropertyCredentialsProvider
 */
@SdkInternalApi
public abstract class SystemSettingsCredentialsProvider implements AwsCredentialsProvider {
    @Override
    public AwsCredentials resolveCredentials() {
        String accessKey = trim(loadSetting(SdkSystemSetting.AWS_ACCESS_KEY_ID).orElse(null));
        String secretKey = trim(loadSetting(SdkSystemSetting.AWS_SECRET_ACCESS_KEY).orElse(null));
        String sessionToken = trim(loadSetting(SdkSystemSetting.AWS_SESSION_TOKEN).orElse(null));

        if (StringUtils.isEmpty(accessKey)) {
            throw new SdkClientException(
                    String.format("Unable to load credentials from system settings. Access key must be specified either via "
                                  + "environment variable (%s) or system property (%s).",
                                  SdkSystemSetting.AWS_ACCESS_KEY_ID.environmentVariable(),
                                  SdkSystemSetting.AWS_ACCESS_KEY_ID.property()));
        }

        if (StringUtils.isEmpty(secretKey)) {
            throw new SdkClientException(
                    String.format("Unable to load credentials from system settings. Secret key must be specified either via "
                                  + "environment variable (%s) or system property (%s).",
                                  SdkSystemSetting.AWS_SECRET_ACCESS_KEY.environmentVariable(),
                                  SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property()));
        }

        return sessionToken == null ? AwsBasicCredentials.create(accessKey, secretKey)
                                    : AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
    }

    /**
     * Implemented by child classes to load the requested setting.
     */
    protected abstract Optional<String> loadSetting(SystemSetting setting);
}
