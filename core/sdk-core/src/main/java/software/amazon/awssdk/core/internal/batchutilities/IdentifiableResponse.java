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

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Wrapper class for a response and its associated batch id.
 * @param <ResponseT> the response
 */
@SdkProtectedApi
// TODO Can combine this with IdentifiableRequest (They are literally the same, just remove one).
public final class IdentifiableResponse<ResponseT> {

    private final String id;
    private final ResponseT response;

    public IdentifiableResponse(String id, ResponseT response) {
        this.id = id;
        this.response = response;
    }

    public String id() {
        return id;
    }

    public ResponseT response() {
        return response;
    }
}
