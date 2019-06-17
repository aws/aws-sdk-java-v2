/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;

/**
 * Attributes that can be applied to all sdk requests. Only SDK is allowed to set these values.
 * Customer should not be using this class.
 */
@SdkProtectedApi
public final class SdkInternalExecutionAttribute extends SdkExecutionAttribute {

    /**
     * The key to indicate if the request is for a full duplex operation ie., request and response are sent/received
     * at the same time.
     */
    public static final ExecutionAttribute<Boolean> IS_FULL_DUPLEX = new ExecutionAttribute<>("IsFullDuplex");

    /**
     * The key to store the {@link CompletableFuture} returned by {@link AsyncResponseTransformer#prepare()} method
     * in the first attempt of a request. This is used only for async streaming requests
     */
    public static final ExecutionAttribute<CompletableFuture<?>> ASYNC_RESPONSE_TRANSFORMER_FUTURE =
        new ExecutionAttribute<>("AsyncResponseTransformerFuture");

    private SdkInternalExecutionAttribute() {
    }
}
