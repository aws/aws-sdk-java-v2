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

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Wrapper class for a request and its associated batch id.
 * @param <T> The request
 */
@SdkInternalApi
public class IdentifiedRequest<T> {

    private final String id;
    private final T request;

    public IdentifiedRequest(String id, T request) {
        this.id = id;
        this.request = request;
    }

    public String getId() {
        return id;
    }

    public T getRequest() {
        return request;
    }
}
