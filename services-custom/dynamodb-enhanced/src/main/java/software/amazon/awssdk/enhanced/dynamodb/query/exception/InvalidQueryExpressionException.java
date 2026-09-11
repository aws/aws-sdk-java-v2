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
 * Thrown at build time when a {@link software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec} contains
 * incompatible or missing options (e.g. {@code groupBy} without {@code aggregate}, {@code filterBase} without a join).
 */
@SdkInternalApi
public class InvalidQueryExpressionException extends RuntimeException {

    public InvalidQueryExpressionException(String message) {
        super(message);
    }

    public InvalidQueryExpressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
