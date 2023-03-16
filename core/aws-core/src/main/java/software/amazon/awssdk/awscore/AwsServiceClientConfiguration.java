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

package software.amazon.awssdk.awscore;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.regions.Region;

/**
 * Class to expose AWS service client settings to the user, e.g., region
 */
@SdkPublicApi
public class AwsServiceClientConfiguration extends SdkServiceClientConfiguration {

    private final Region region;

    public AwsServiceClientConfiguration(SdkClientConfiguration clientConfiguration,
                                         ClientOverrideConfiguration clientOverrideConfiguration) {
        super(clientOverrideConfiguration);
        this.region = clientConfiguration.option(AwsClientOption.AWS_REGION);
    }

    /**
     *
     * @return The configured region of the AwsClient
     */
    public Region region() {
        return this.region;
    }
}
