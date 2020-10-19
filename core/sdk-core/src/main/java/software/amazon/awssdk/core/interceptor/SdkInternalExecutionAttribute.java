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

package software.amazon.awssdk.core.interceptor;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;

/**
 * Attributes that can be applied to all sdk requests. Only generated code from the SDK clients should set these values.
 */
@SdkProtectedApi
public final class SdkInternalExecutionAttribute extends SdkExecutionAttribute {

    /**
     * The key to indicate if the request is for a full duplex operation ie., request and response are sent/received
     * at the same time.
     */
    public static final ExecutionAttribute<Boolean> IS_FULL_DUPLEX = new ExecutionAttribute<>("IsFullDuplex");

    public static final ExecutionAttribute<HttpChecksumRequired> HTTP_CHECKSUM_REQUIRED =
        new ExecutionAttribute<>("HttpChecksumRequired");

    private SdkInternalExecutionAttribute() {
    }
}
