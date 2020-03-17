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

package software.amazon.awssdk.core.client.handler;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Client interface to invoke an API.
 */
@SdkProtectedApi
public interface SyncClientHandler extends SdkAutoCloseable {

    /**
     * Execute's a web service request. Handles marshalling and unmarshalling of data and making the
     * underlying HTTP call(s).
     *
     * @param executionParams Parameters specific to this invocation of an API.
     * @param <InputT> Input POJO type
     * @param <OutputT> Output POJO type
     * @return Unmarshalled output POJO type.
     */
    <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
        ClientExecutionParams<InputT, OutputT> executionParams);

    /**
     * Execute's a streaming web service request. Handles marshalling and unmarshalling of data and making the
     * underlying HTTP call(s).
     *
     * @param executionParams Parameters specific to this invocation of an API.
     * @param responseTransformer Response handler for a streaming response. Receives unmarshalled POJO and input stream and
     * returns a transformed result.
     * @param <InputT> Input POJO type
     * @param <OutputT> Output POJO type
     * @param <ReturnT> Transformed result returned by responseTransformer. Returned by this method.
     * @return Transformed result as returned by responseTransformer.
     */
    <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ResponseTransformer<OutputT, ReturnT> responseTransformer);
}
