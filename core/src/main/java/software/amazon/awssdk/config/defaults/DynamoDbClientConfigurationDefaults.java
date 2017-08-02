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

package software.amazon.awssdk.config.defaults;

import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.retry.RetryPolicyAdapter;

/**
 * A DynamoDB-specific decorator for a {@link ClientConfiguration} that adds default values optimal for communicating with
 * DynamoDB, assuming the customer hasn't attempted to override the defaults. This is a higher-priority configuration than the
 * {@link GlobalClientConfigurationDefaults}, and a lower-priority configuration than the customer-provided configuration.
 */
@SdkInternalApi
public class DynamoDbClientConfigurationDefaults extends ClientConfigurationDefaults {
    @Override
    protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
        ClientOverrideConfiguration configuration = builder.build();
        builder.retryPolicy(applyDefault(configuration.retryPolicy(), () ->
                new RetryPolicyAdapter(PredefinedRetryPolicies.DYNAMODB_DEFAULT)));
    }
}
