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

package software.amazon.awssdk.awscore.internal.protocol.xml;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.http.response.StaxResponseHandler;

/**
 * Contains information needed to create a {@link StaxResponseHandler}.
 */
@SdkProtectedApi
public final class StaxOperationMetadata {

    private boolean hasStreamingSuccessResponse;

    public StaxOperationMetadata withHasStreamingSuccessResponse(
        boolean hasStreamingSuccessResponse) {
        this.hasStreamingSuccessResponse = hasStreamingSuccessResponse;
        return this;
    }

    public boolean isHasStreamingSuccessResponse() {
        return hasStreamingSuccessResponse;
    }
}
