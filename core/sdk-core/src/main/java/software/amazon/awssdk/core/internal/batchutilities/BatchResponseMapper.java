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

package software.amazon.awssdk.core.internal.batchutilities;

import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Unpacks the batch response, then transforms individual entries to the appropriate response type. Each entry's batch ID
 * is mapped to the individual response entry.
 * @param <BatchResponseT> the type of an outgoing batch response.
 * @param <ResponseT> the type of an outgoing response.
 */
@FunctionalInterface
@SdkProtectedApi
public interface BatchResponseMapper<BatchResponseT, ResponseT> {
    List<IdentifiableMessage<ResponseT>> mapBatchResponse(BatchResponseT batchResponse);
}
