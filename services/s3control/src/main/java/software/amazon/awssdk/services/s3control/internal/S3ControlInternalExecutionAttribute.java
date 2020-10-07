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

package software.amazon.awssdk.services.s3control.internal;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

@SdkProtectedApi
public final class S3ControlInternalExecutionAttribute extends SdkExecutionAttribute {

    /**
     * The optional value contains metadata for a request with a field that contains an ARN
     */
    public static final ExecutionAttribute<S3ArnableField> S3_ARNABLE_FIELD = new ExecutionAttribute<>("S3_ARNABLE_FIELD");
}
