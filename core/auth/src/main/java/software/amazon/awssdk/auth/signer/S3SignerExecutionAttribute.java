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

package software.amazon.awssdk.auth.signer;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

/**
 * S3-specific signing attributes attached to the execution.
 */
@SdkProtectedApi
public final class S3SignerExecutionAttribute extends SdkExecutionAttribute {

    /**
     * The key to specify whether to enable chunked encoding or not
     */
    public static final ExecutionAttribute<Boolean> ENABLE_CHUNKED_ENCODING = new ExecutionAttribute<>("ChunkedEncoding");

    /**
     * The key to specify whether to enable payload signing or not
     */
    public static final ExecutionAttribute<Boolean> ENABLE_PAYLOAD_SIGNING = new ExecutionAttribute<>("PayloadSigning");

    private S3SignerExecutionAttribute() {
    }
}
