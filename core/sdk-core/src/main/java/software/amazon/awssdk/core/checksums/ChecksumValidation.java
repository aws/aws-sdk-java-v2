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

package software.amazon.awssdk.core.checksums;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

/**
 * Enum to indicate the checksum validation is performed on a Response that supports Http Checksum Validation.
 * The status of Checksum validation can be obtained by accessing {@link SdkExecutionAttribute#HTTP_RESPONSE_CHECKSUM_VALIDATION}
 * on an execution context by using
 * {@link ExecutionInterceptor#afterExecution(Context.AfterExecution, ExecutionAttributes)}.
 * Possible Checksum Validations
 * {@link #VALIDATED}
 * {@link #FORCE_SKIP}
 * {@link #CHECKSUM_ALGORITHM_NOT_FOUND}
 * {@link #CHECKSUM_RESPONSE_NOT_FOUND}
 *
 */
@SdkPublicApi
public enum ChecksumValidation {
    /**
     * Checksum validation was performed on the response.
     */
    VALIDATED,

    /**
     * Checksum validation was skipped since response has customization for specific checksum values.
     */
    FORCE_SKIP,

    /**
     * Response has checksum mode enabled but Algorithm was not found in client.
     */
    CHECKSUM_ALGORITHM_NOT_FOUND,

    /**
     * Response has checksum mode enabled but response header did not have checksum.
     */
    CHECKSUM_RESPONSE_NOT_FOUND,
}
