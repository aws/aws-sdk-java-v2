/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.client.config;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;

/**
 * A set of static methods used to validate that a {@link SdkClientConfiguration} contains all of
 * the values required for the SDK to function.
 */
@SdkProtectedApi
public class SdkClientOptionValidation {
    protected SdkClientOptionValidation() {}

    public static void validateAsyncClientOptions(SdkClientConfiguration c) {
        require("asyncConfiguration.advancedOption[FUTURE_COMPLETION_EXECUTOR]",
                c.option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR));
        require("asyncHttpClient", c.option(SdkClientOption.ASYNC_HTTP_CLIENT));

        validateClientOptions(c);
    }

    public static void validateSyncClientOptions(SdkClientConfiguration c) {
        require("syncHttpClient", c.option(SdkClientOption.SYNC_HTTP_CLIENT));

        validateClientOptions(c);
    }

    private static void validateClientOptions(SdkClientConfiguration c) {
        require("endpoint", c.option(SdkClientOption.ENDPOINT));

        require("overrideConfiguration.additionalHttpHeaders", c.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS));
        require("overrideConfiguration.executionInterceptors", c.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        require("overrideConfiguration.retryPolicy", c.option(SdkClientOption.RETRY_POLICY));

        require("overrideConfiguration.advancedOption[SIGNER]", c.option(SdkAdvancedClientOption.SIGNER));
        require("overrideConfiguration.advancedOption[USER_AGENT_PREFIX]",
                c.option(SdkAdvancedClientOption.USER_AGENT_PREFIX));
        require("overrideConfiguration.advancedOption[USER_AGENT_SUFFIX]",
                c.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX));
        require("overrideConfiguration.advancedOption[CRC32_FROM_COMPRESSED_DATA_ENABLED]",
                c.option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED));
    }

    /**
     * Validate that the customer set the provided field.
     */
    protected static <U> U require(String field, U required) {
        return Validate.notNull(required, "The '%s' must be configured in the client builder.", field);
    }
}
