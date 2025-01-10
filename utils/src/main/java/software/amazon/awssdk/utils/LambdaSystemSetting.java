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

package software.amazon.awssdk.utils;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * The system properties provided by aws lambda environment.
 */
@SdkProtectedApi
public enum LambdaSystemSetting implements SystemSetting {
    AWS_LAMBDA_FUNCTION_NAME("aws.lambda.functionName", "AWS_LAMBDA_FUNCTION_NAME", null);

    private final String property;
    private final String envVar;
    private final String defaultValue;

    LambdaSystemSetting(String property, String environmentVariable, String defaultValue) {
        this.property = property;
        this.envVar = environmentVariable;
        this.defaultValue = defaultValue;
    }

    @Override
    public String property() {
        return property;
    }

    @Override
    public String environmentVariable() {
        return envVar;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }
}
