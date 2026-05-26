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

package software.amazon.awssdk.enhanced.dynamodb.query.exception;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Thrown at runtime when {@link software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode#STRICT_KEY_ONLY}
 * is active and no key condition is provided. This prevents silent full-table scans; callers must either provide a
 * key condition or explicitly opt in via {@link software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode#ALLOW_SCAN}.
 */
@SdkInternalApi
public class MissingKeyConditionException extends RuntimeException {

    public MissingKeyConditionException(String message) {
        super(message);
    }

    public MissingKeyConditionException(String message, Throwable cause) {
        super(message, cause);
    }
}
