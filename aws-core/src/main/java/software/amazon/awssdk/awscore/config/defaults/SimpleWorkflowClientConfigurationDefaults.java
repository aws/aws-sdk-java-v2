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

package software.amazon.awssdk.awscore.config.defaults;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.config.AwsClientConfiguration;
import software.amazon.awssdk.core.config.defaults.GlobalClientConfigurationDefaults;

/**
 * A Simple Workflow-specific decorator for a {@link AwsClientConfiguration} that adds default values optimal for communicating
 * with Simple Workflow, assuming the customer hasn't attempted to override the defaults. This is a higher-priority
 * configuration than the {@link GlobalClientConfigurationDefaults}, and a lower-priority configuration than the
 * customer-provided configuration.
 */
@SdkInternalApi
@ReviewBeforeRelease("Hook up service specific defaults")
public class SimpleWorkflowClientConfigurationDefaults extends AwsClientConfigurationDefaults {
}
