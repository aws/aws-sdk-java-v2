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

package software.amazon.awssdk.core.internal.batchmanager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Takes a list of identified requests in addition to a destination and batches the requests into a batch request.
 * It then sends the batch request and returns a CompletableFuture of the response.
 * @param <RequestT> the type of an outgoing request.
 * @param <BatchResponseT> the type of an outgoing batch response.
 */
@FunctionalInterface
@SdkProtectedApi
public interface BatchAndSend<RequestT, BatchResponseT> {
    CompletableFuture<BatchResponseT> batchAndSend(List<IdentifiableMessage<RequestT>> identifiedRequests, String batchGroupId);
}
