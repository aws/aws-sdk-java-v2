/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.internal.client.config;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOptionValidation;

/**
 * A set of static methods used to validate that a {@link SdkClientConfiguration} contains all of
 * the values required for the AWS client handlers to function.
 */
@SdkInternalApi
public final class AwsClientOptionValidation extends SdkClientOptionValidation {
    private AwsClientOptionValidation() {
    }

    public static void validateAsyncClientOptions(SdkClientConfiguration c) {
        validateClientOptions(c);
    }

    public static void validateSyncClientOptions(SdkClientConfiguration c) {
        validateClientOptions(c);
    }

    private static void validateClientOptions(SdkClientConfiguration c) {
        require("credentialsProvider", c.option(AwsClientOption.CREDENTIALS_PROVIDER));

        require("overrideConfiguration.advancedOption[AWS_REGION]", c.option(AwsClientOption.AWS_REGION));
        require("overrideConfiguration.advancedOption[SIGNING_REGION]", c.option(AwsClientOption.SIGNING_REGION));
        require("overrideConfiguration.advancedOption[SERVICE_SIGNING_NAME]",
                c.option(AwsClientOption.SERVICE_SIGNING_NAME));
        require("overrideConfiguration.advancedOption[ENABLE_DEFAULT_REGION_DETECTION]",
                c.option(AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION));
    }
}
